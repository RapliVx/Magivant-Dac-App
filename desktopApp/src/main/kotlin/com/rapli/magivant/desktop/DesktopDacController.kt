package com.rapli.magivant.desktop

import com.rapli.magivant.ui.DacPreset
import com.rapli.magivant.ui.DacUiState
import com.rapli.magivant.ui.PresetStore
import com.rapli.magivant.usb.Cs43198Spec
import com.rapli.magivant.usb.DacVolume
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.IOException
import java.util.prefs.Preferences
import kotlin.math.abs

class DesktopDacController(
    private val transport: LinuxDacTransport = LinuxDacTransport(),
    private val presetStore: PresetStore = PresetStore(presetDirectory()),
    private val preferences: Preferences = Preferences.userRoot().node("com/rapli/magivant-dac")
) : AutoCloseable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val operationMutex = Mutex()
    private val _uiState = MutableStateFlow(
        DacUiState(
            maxVolumeIndex = preferences.getInt("max_volume_index", 60).coerceIn(10, 60),
            startupPresetName = preferences.get("startup_preset_name", "")
        )
    )
    val uiState: StateFlow<DacUiState> = _uiState.asStateFlow()

    private val _presets = MutableStateFlow<List<DacPreset>>(emptyList())
    val presets: StateFlow<List<DacPreset>> = _presets.asStateFlow()
    private val presetLoadJob = scope.launch {
        runCatching { presetStore.getAllPresets() }
            .onSuccess { _presets.value = it }
            .onFailure { showError(it.message ?: "Cannot read saved presets.") }
    }
    private var activeDevice: ConnectedDac? = null
    private var monitorJob: Job? = null

    fun connect() {
        scope.launch {
            presetLoadJob.join()
            operationMutex.withLock {
                if (_uiState.value.isConnected || _uiState.value.isSyncing) return@withLock
                _uiState.update { it.copy(isSyncing = true, syncMessage = "Searching for Magivant DAC…", errorMessage = null) }
                try {
                    val device = transport.connect()
                        ?: throw IOException("No Magivant DAC found. Connect a supported DAC and try again.")
                    activeDevice = device
                    _uiState.update {
                        it.copy(
                            isConnected = true,
                            syncMessage = "Reading DAC settings…",
                            bridgeModeText = device.name
                        )
                    }
                    syncHardware(includeFirmware = true)
                    _uiState.update { it.copy(hasDeviceState = true, isSyncing = false, syncMessage = "") }
                    val startupPreset = _uiState.value.startupPresetName
                    _presets.value.firstOrNull { it.name == startupPreset }?.let { applyPreset(it) }
                    monitorJob = scope.launch { monitorDevice() }
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    activeDevice = null
                    transport.disconnect()
                    _uiState.update {
                        it.copy(
                            isConnected = false,
                            hasDeviceState = false,
                            isSyncing = false,
                            syncMessage = "",
                            errorMessage = error.message ?: "Cannot connect to the DAC."
                        )
                    }
                }
            }
        }
    }

    fun disconnect() {
        monitorJob?.cancel()
        monitorJob = null
        scope.launch {
            operationMutex.withLock {
                transport.disconnect()
                activeDevice = null
                _uiState.update {
                    DacUiState(
                        maxVolumeIndex = it.maxVolumeIndex,
                        startupPresetName = it.startupPresetName
                    )
                }
            }
        }
    }

    private suspend fun monitorDevice() {
        while (activeDevice != null) {
            delay(1000)
            try {
                operationMutex.withLock { syncHardware() }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                activeDevice = null
                transport.disconnect()
                _uiState.update {
                    it.copy(
                        isConnected = false,
                        hasDeviceState = false,
                        isVolumePending = false,
                        isBalancePending = false,
                        errorMessage = "The DAC stopped responding: ${error.message}"
                    )
                }
            }
        }
    }

    private suspend fun syncHardware(includeFirmware: Boolean = false) {
        if (includeFirmware) {
            val firmware = transport.readData(0xA0.toByte())
            val version = "${firmware.u8(3)}.${firmware.u8(4)}.${firmware.u8(5)}"
            _uiState.update { it.copy(firmwareVersion = version) }

            val bridge = transport.readData(0xA4.toByte()).u8(4)
            _uiState.update { it.copy(bridgeModeText = if (bridge == 1) "UAC 2.0" else "UAC Mode $bridge") }
        }

        val sampleRate = transport.readData(0xA1.toByte()).u8(3)
        _uiState.update {
            it.copy(sampleRateRawCode = sampleRate, sampleRateText = Cs43198Spec.decodeSampleRateCode(sampleRate))
        }
        syncVolume()

        val settings = transport.readData(0xA3.toByte())
        val filter = settings.u8(3)
        val gain = settings.u8(4)
        val led = settings.u8(5)
        if (filter !in 0..4 || gain !in 0..1 || led !in 0..2) {
            throw IOException("The DAC returned invalid settings.")
        }
        _uiState.update { it.copy(digitalFilterPos = filter, isHighGain = gain == 1, ledPos = led) }
        updateActivePreset()
    }

    private suspend fun syncVolume() {
        var response = transport.readData(0xA2.toByte())
        var rawVolume = response.u8(4)
        val limitedVolume = DacVolume.limit(rawVolume, _uiState.value.maxVolumeIndex)
        if (rawVolume != limitedVolume) {
            sendCommand(4, limitedVolume.toByte())
            response = transport.readData(0xA2.toByte())
            rawVolume = response.u8(4)
            if (rawVolume < limitedVolume) throw IOException("The DAC rejected the volume limit.")
        }
        val leftBalance = response.u8(5)
        val rightBalance = response.u8(6)
        _uiState.update {
            it.copy(
                volumeIndex = DacVolume.closestIndex(rawVolume),
                rawVolumeReg = rawVolume,
                balanceBaseValue = if (leftBalance > 0) leftBalance else -rightBalance
            )
        }
    }

    private suspend fun sendCommand(commandId: Int, value1: Byte, value2: Byte = 0) {
        transport.sendCommand(commandId.toByte(), value1, value2)
        delay(60)
    }

    fun setVolume(index: Int) = launchDeviceOperation {
        val clampedIndex = index.coerceIn(0, _uiState.value.maxVolumeIndex)
        sendCommand(4, DacVolume.registerValues[clampedIndex].toByte())
        syncVolume()
        updateActivePreset()
    }

    fun adjustVolumeByHalfDb(stepDelta: Int) = launchDeviceOperation {
        val state = _uiState.value
        val raw = DacVolume.limit(state.rawVolumeReg - stepDelta.coerceIn(-1, 1), state.maxVolumeIndex)
        sendCommand(4, raw.toByte())
        syncVolume()
        updateActivePreset()
    }

    fun setBalance(balance: Int) = launchDeviceOperation {
        val safeBalance = balance.coerceIn(-10, 10)
        val amount = abs(safeBalance).toByte()
        sendCommand(5, if (safeBalance > 0) amount else 0, if (safeBalance < 0) amount else 0)
        syncVolume()
        updateActivePreset()
    }

    fun setGain(isHigh: Boolean) = launchDeviceOperation {
        sendCommand(2, if (isHigh) 1 else 0)
        syncHardware()
    }

    fun setDigitalFilter(position: Int) = launchDeviceOperation {
        sendCommand(1, position.coerceIn(0, 4).toByte())
        syncHardware()
    }

    fun setLed(position: Int) = launchDeviceOperation {
        sendCommand(6, position.coerceIn(0, 2).toByte())
        syncHardware()
    }

    fun setMaxVolumeLimit(index: Int) {
        val safeIndex = index.coerceIn(10, 60)
        preferences.putInt("max_volume_index", safeIndex)
        _uiState.update { it.copy(maxVolumeIndex = safeIndex) }
        if (_uiState.value.hasDeviceState) {
            launchDeviceOperation {
                syncVolume()
                updateActivePreset()
            }
        }
    }

    fun setStartupPreset(name: String) {
        if (name.isNotEmpty() && _presets.value.none { it.name == name }) return
        preferences.put("startup_preset_name", name)
        _uiState.update { it.copy(startupPresetName = name) }
    }

    fun saveCurrentPreset(name: String) {
        val state = _uiState.value
        if (!state.hasDeviceState) return showError("Connect the DAC before saving a preset.")
        val preset = state.toPreset(name)
        launchPresetOperation {
            presetStore.savePreset(preset)
            refreshPresets()
            _uiState.update { it.copy(activePresetName = preset.name) }
        }
    }

    fun applyPreset(preset: DacPreset) = launchDeviceOperation {
        _uiState.update { it.copy(activePresetName = "") }
        sendCommand(4, 0xFF.toByte())
        val balance = preset.balanceBaseValue.coerceIn(-10, 10)
        val amount = abs(balance).toByte()
        sendCommand(5, if (balance > 0) amount else 0, if (balance < 0) amount else 0)
        sendCommand(2, if (preset.isHighGain) 1 else 0)
        sendCommand(1, preset.digitalFilterPos.coerceIn(0, 4).toByte())
        sendCommand(6, preset.ledPos.coerceIn(0, 2).toByte())
        val volume = DacVolume.limit(preset.volumeRegister, _uiState.value.maxVolumeIndex)
        sendCommand(4, volume.toByte())
        syncHardware()
        val expected = preset.copy(volumeIndex = DacVolume.closestIndex(volume), rawVolumeReg = volume)
        if (!matchesCurrentState(expected)) throw IOException("The DAC did not confirm every preset setting.")
        _uiState.update { it.copy(activePresetName = preset.name) }
    }

    fun deletePreset(preset: DacPreset) = launchPresetOperation {
        presetStore.deletePreset(preset.name)
        if (_uiState.value.activePresetName == preset.name) _uiState.update { it.copy(activePresetName = "") }
        refreshPresets()
    }

    fun importPreset(file: File) = launchPresetOperation {
        presetStore.importAndSave(file)
        refreshPresets()
    }

    fun exportPreset(file: File, preset: DacPreset) = launchPresetOperation {
        presetStore.exportTo(file, preset)
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    private fun showError(message: String) = _uiState.update { it.copy(errorMessage = message) }

    private fun launchDeviceOperation(operation: suspend () -> Unit): Job = scope.launch {
        if (!_uiState.value.hasDeviceState) {
            showError("Connect and sync the DAC first.")
            return@launch
        }
        operationMutex.withLock {
            try {
                operation()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _uiState.update { it.copy(activePresetName = "", errorMessage = error.message ?: "DAC command failed.") }
            }
        }
    }

    private fun launchPresetOperation(operation: suspend () -> Unit) = scope.launch {
        try {
            operation()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            showError(error.message ?: "Cannot update presets.")
        }
    }

    private suspend fun refreshPresets() {
        _presets.value = presetStore.getAllPresets()
        updateActivePreset()
    }

    private fun matchesCurrentState(preset: DacPreset): Boolean {
        val state = _uiState.value
        return state.rawVolumeReg == preset.volumeRegister && state.balanceBaseValue == preset.balanceBaseValue &&
            state.isHighGain == preset.isHighGain && state.digitalFilterPos == preset.digitalFilterPos &&
            state.ledPos == preset.ledPos
    }

    private fun updateActivePreset() {
        val activePreset = _presets.value.firstOrNull { it.name == _uiState.value.activePresetName }
        if (activePreset != null && !matchesCurrentState(activePreset)) {
            _uiState.update { it.copy(activePresetName = "") }
        }
    }

    private fun ByteArray.u8(index: Int): Int = get(index).toInt() and 0xFF

    override fun close() {
        monitorJob?.cancel()
        scope.cancel()
        runBlocking(Dispatchers.IO) {
            transport.disconnect()
            transport.close()
        }
        preferences.flush()
    }

    companion object {
        private fun presetDirectory(): File {
            val dataHome = System.getenv("XDG_DATA_HOME")?.takeIf(String::isNotBlank)
                ?: File(System.getProperty("user.home"), ".local/share").absolutePath
            return File(dataHome, "magivant-dac/presets")
        }
    }
}

private fun DacUiState.toPreset(name: String): DacPreset = DacPreset(
    name = name.trim(),
    volumeIndex = volumeIndex,
    balanceBaseValue = balanceBaseValue,
    isHighGain = isHighGain,
    digitalFilterPos = digitalFilterPos,
    ledPos = ledPos,
    rawVolumeReg = rawVolumeReg,
    schemaVersion = 2
)
