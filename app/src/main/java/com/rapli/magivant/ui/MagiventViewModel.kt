package com.rapli.magivant.ui

import android.app.Application
import android.content.Context
import android.hardware.usb.UsbDevice
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.rapli.magivant.usb.Cs43198Spec
import com.rapli.magivant.usb.DacVolume
import com.rapli.magivant.usb.DndControl
import com.rapli.magivant.usb.DacConnection
import com.rapli.magivant.usb.UsbDacManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.coroutines.coroutineContext
import kotlin.math.abs

class MagivantViewModel(
    application: Application,
    private val usbManager: DacConnection,
    private val presetManager: PresetManager,
    private val dndManager: DndControl,
    private val savedStateHandle: SavedStateHandle,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(DacUiState())
    val uiState = _uiState.asStateFlow()
    private val _presets = MutableStateFlow<List<DacPreset>>(emptyList())
    val presets = _presets.asStateFlow()
    val appVolumeIntList = DacVolume.registerValues

    private val operationMutex = Mutex()
    private val presetMutex = Mutex()
    private val presetsLoaded = CompletableDeferred<Unit>()
    private var sessionId = 0L
    private var sessionJob: Job? = null
    private var deviceScope: CoroutineScope? = null
    private var nextControlRequestId = 0L
    private var volumeRequestId = 0L
    private var balanceRequestId = 0L
    private var volumeJob: Job? = null
    private var balanceJob: Job? = null
    private var limitJob: Job? = null
    private var connectedDeviceName: String? = null
    private val prefs = application.getSharedPreferences("MagivantSettings", Context.MODE_PRIVATE)

    init {
        _uiState.update {
            it.copy(
                isDndEnabled = prefs.getBoolean("dnd_enabled", false),
                maxVolumeIndex = prefs.getInt("max_volume_index", 60).coerceIn(10, 60),
                startupPresetName = prefs.getString("startup_preset_name", "") ?: ""
            )
        }
        viewModelScope.launch {
            try {
                presetMutex.withLock { refreshPresets() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showError("Cannot load presets: ${e.message}")
            } finally {
                presetsLoaded.complete(Unit)
            }
        }
    }

    fun showError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun syncDndState() {
        val state = _uiState.value
        if (!dndManager.setDndActive(state.isDndEnabled && state.isConnected)) {
            showError("Cannot update Auto DND. Check notification policy access and the Magivant DAC rule.")
        }
    }

    fun toggleDndMode(enabled: Boolean) {
        prefs.edit().putBoolean("dnd_enabled", enabled).apply()
        _uiState.update { it.copy(isDndEnabled = enabled) }
        if (enabled && !dndManager.hasDndPermission()) {
            if (!dndManager.requestDndPermission()) showError("Cannot open DND permission settings.")
        } else {
            syncDndState()
        }
    }

    fun connectToDevice(device: UsbDevice) {
        if (!UsbDacManager.isSupportedDevice(device) || connectedDeviceName == device.deviceName) return
        onDeviceDisconnected()
        val id = sessionId
        connectedDeviceName = device.deviceName
        _uiState.update { it.copy(isSyncing = true, syncMessage = "Reading device state...", errorMessage = null) }
        sessionJob = viewModelScope.launch {
            var failure: String? = null
            try {
                usbManager.openConnection(device, id)
                coroutineScope {
                    deviceScope = this
                    _uiState.update { it.copy(isConnected = true) }
                    syncDndState()
                    operationMutex.withLock {
                        syncHardware(id, includeFirmware = true)
                        _uiState.update { it.copy(hasDeviceState = true) }
                        presetsLoaded.await()
                        val startupName = _uiState.value.startupPresetName
                        _presets.value.find { it.name == startupName }?.let { applyPresetToDevice(id, it) }
                    }
                    _uiState.update { it.copy(isSyncing = false, syncMessage = "") }
                    launch { pollDevice(id) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failure = "DAC connection failed: ${e.message}"
            } finally {
                withContext(NonCancellable) {
                    try {
                        usbManager.closeConnection(id)
                    } catch (e: Exception) {
                        failure = failure ?: "Cannot close the DAC: ${e.message}"
                    }
                    if (id == sessionId) resetDeviceState(failure)
                }
            }
        }
    }

    fun onDeviceDetached(device: UsbDevice) {
        if (device.deviceName == connectedDeviceName) onDeviceDisconnected()
    }

    fun onDeviceDisconnected() {
        if (connectedDeviceName == null && sessionJob == null) {
            dndManager.setDndActive(false)
            return
        }
        sessionId++
        sessionJob?.cancel()
        sessionJob = null
        volumeJob = null
        balanceJob = null
        limitJob = null
        resetDeviceState()
    }

    private fun resetDeviceState(errorMessage: String? = null) {
        connectedDeviceName = null
        sessionJob = null
        deviceScope = null
        _uiState.update {
            DacUiState(
                isDndEnabled = it.isDndEnabled,
                maxVolumeIndex = it.maxVolumeIndex,
                startupPresetName = it.startupPresetName,
                errorMessage = errorMessage
            )
        }
        dndManager.setDndActive(false)
    }

    override fun onCleared() {
        onDeviceDisconnected()
    }

    fun manualResync() {
        launchDeviceOperation("Refreshing...") { id -> syncHardware(id, includeFirmware = true) }
    }

    private suspend fun pollDevice(id: Long) {
        var failures = 0
        while (coroutineContext.isActive) {
            delay(750)
            try {
                operationMutex.withLock { syncHardware(id) }
                failures = 0
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failures++
                showError("Cannot refresh DAC state: ${e.message}")
                if (failures >= 3) throw IOException("The DAC stopped responding. Reconnect it.", e)
            }
        }
    }

    private fun launchDeviceOperation(
        message: String = "",
        operation: suspend (Long) -> Unit
    ): Job? {
        if (!_uiState.value.hasDeviceState) return null
        val id = sessionId
        return deviceScope?.launch {
            operationMutex.withLock {
                ensureActive()
                if (message.isNotEmpty()) {
                    _uiState.update { it.copy(isSyncing = true, syncMessage = message, errorMessage = null) }
                }
                try {
                    operation(id)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _uiState.update { it.copy(activePresetName = "", errorMessage = "DAC operation failed: ${e.message}") }
                } finally {
                    if (id == sessionId && message.isNotEmpty()) {
                        _uiState.update { it.copy(isSyncing = false, syncMessage = "") }
                    }
                }
            }
        }
    }

    private suspend fun sendCommand(id: Long, command: Byte, value: Byte, value2: Byte = 0) {
        usbManager.sendCommand(id, command, value, value2)
        delay(60)
    }

    private suspend fun syncHardware(id: Long, includeFirmware: Boolean = false) {
        if (includeFirmware) {
            val firmware = usbManager.readData(id, 0xA0.toByte())
            val version = "${firmware[3].toInt() and 0xFF}.${firmware[4].toInt() and 0xFF}.${firmware[5].toInt() and 0xFF}"
            _uiState.update { it.copy(firmwareVersion = version) }
            delay(50)
            val bridge = usbManager.readData(id, 0xA4.toByte())
            val flag = bridge[4].toInt() and 0xFF
            _uiState.update { it.copy(bridgeModeText = if (flag == 1) "UAC 2.0" else "UAC Mode $flag") }
            delay(50)
        }
        val sampleRate = usbManager.readData(id, 0xA1.toByte())[3].toInt() and 0xFF
        _uiState.update { it.copy(sampleRateRawCode = sampleRate, sampleRateText = Cs43198Spec.decodeSampleRateCode(sampleRate)) }
        delay(50)
        syncVolume(id)
        delay(50)
        val data = usbManager.readData(id, 0xA3.toByte())
        val filter = data[3].toInt() and 0xFF
        val gain = data[4].toInt() and 0xFF
        val led = data[5].toInt() and 0xFF
        if (filter !in 0..4 || gain !in 0..1 || led !in 0..2) throw IOException("Invalid DAC settings response.")
        _uiState.update { it.copy(digitalFilterPos = filter, isHighGain = gain == 1, ledPos = led) }
        updateActivePreset()
    }

    private suspend fun syncVolume(id: Long) {
        var data = usbManager.readData(id, 0xA2.toByte())
        var raw = data[4].toInt() and 0xFF
        val limited = DacVolume.limit(raw, _uiState.value.maxVolumeIndex)
        if (raw != limited) {
            sendCommand(id, 4, limited.toByte())
            data = usbManager.readData(id, 0xA2.toByte())
            raw = data[4].toInt() and 0xFF
            if (raw < limited) throw IOException("The DAC did not accept the volume limit.")
        }
        val left = data[5].toInt() and 0xFF
        val right = data[6].toInt() and 0xFF
        _uiState.update {
            it.copy(
                volumeIndex = DacVolume.closestIndex(raw),
                rawVolumeReg = raw,
                balanceBaseValue = if (left > 0) left else -right
            )
        }
    }

    fun setMaxVolumeLimit(maxIndex: Int) {
        val clamped = maxIndex.coerceIn(10, 60)
        prefs.edit().putInt("max_volume_index", clamped).apply()
        _uiState.update { it.copy(maxVolumeIndex = clamped) }
        volumeJob?.cancel()
        limitJob?.cancel()
        limitJob = launchDeviceOperation { id ->
            syncVolume(id)
            updateActivePreset()
        }
    }

    fun setStartupPresetName(presetName: String) {
        if (presetName.isNotEmpty() && _presets.value.none { it.name == presetName }) return
        prefs.edit().putString("startup_preset_name", presetName).apply()
        _uiState.update { it.copy(startupPresetName = presetName) }
    }

    private fun queueControlUpdate(isVolume: Boolean, operation: suspend (Long) -> Unit) {
        if (!_uiState.value.hasDeviceState) return
        val requestId = ++nextControlRequestId
        if (isVolume) {
            volumeRequestId = requestId
            _uiState.update { it.copy(isVolumePending = true) }
            volumeJob?.cancel()
        } else {
            balanceRequestId = requestId
            _uiState.update { it.copy(isBalancePending = true) }
            balanceJob?.cancel()
        }
        val job = launchDeviceOperation(operation = operation)
        fun finishRequest() {
            viewModelScope.launch {
                val latestId = if (isVolume) volumeRequestId else balanceRequestId
                if (latestId == requestId) {
                    _uiState.update {
                        if (isVolume) it.copy(isVolumePending = false) else it.copy(isBalancePending = false)
                    }
                }
            }
        }
        if (job == null) finishRequest() else job.invokeOnCompletion { finishRequest() }
        if (isVolume) volumeJob = job else balanceJob = job
    }

    fun setVolume(index: Int) {
        queueControlUpdate(isVolume = true) { id ->
            val raw = appVolumeIntList[index.coerceIn(0, _uiState.value.maxVolumeIndex)]
            sendCommand(id, 4, raw.toByte())
            syncVolume(id)
            updateActivePreset()
        }
    }

    fun adjustVolumeByHalfDb(stepDelta: Int) {
        launchDeviceOperation { id ->
            val state = _uiState.value
            val raw = DacVolume.limit(state.rawVolumeReg - stepDelta.coerceIn(-1, 1), state.maxVolumeIndex)
            sendCommand(id, 4, raw.toByte())
            syncVolume(id)
            updateActivePreset()
        }
    }

    private suspend fun sendBalance(id: Long, balance: Int) {
        val amount = abs(balance).toByte()
        sendCommand(id, 5, if (balance > 0) amount else 0, if (balance < 0) amount else 0)
    }

    fun setBalance(balance: Int) {
        queueControlUpdate(isVolume = false) { id ->
            sendBalance(id, balance.coerceIn(-10, 10))
            syncVolume(id)
            updateActivePreset()
        }
    }

    fun setGain(isHigh: Boolean) {
        launchDeviceOperation { id ->
            sendCommand(id, 2, if (isHigh) 1 else 0)
            syncHardware(id)
        }
    }

    fun setDigitalFilter(pos: Int) {
        launchDeviceOperation { id ->
            sendCommand(id, 1, pos.coerceIn(0, 4).toByte())
            syncHardware(id)
        }
    }

    fun setLed(pos: Int) {
        launchDeviceOperation { id ->
            sendCommand(id, 6, pos.coerceIn(0, 2).toByte())
            syncHardware(id)
        }
    }

    private fun currentPreset(name: String): DacPreset {
        val state = _uiState.value
        check(state.hasDeviceState) { "Connect and sync the DAC before saving or exporting settings." }
        return DacPreset(
            name = name,
            volumeIndex = state.volumeIndex,
            balanceBaseValue = state.balanceBaseValue,
            isHighGain = state.isHighGain,
            digitalFilterPos = state.digitalFilterPos,
            ledPos = state.ledPos,
            rawVolumeReg = state.rawVolumeReg,
            schemaVersion = 2
        )
    }

    private fun matchesCurrentState(preset: DacPreset): Boolean {
        val state = _uiState.value
        return state.hasDeviceState && preset.volumeRegister == state.rawVolumeReg &&
            preset.balanceBaseValue == state.balanceBaseValue && preset.isHighGain == state.isHighGain &&
            preset.digitalFilterPos == state.digitalFilterPos && preset.ledPos == state.ledPos
    }

    private fun updateActivePreset() {
        val active = _presets.value.find { it.name == _uiState.value.activePresetName }
        if (active == null || !matchesCurrentState(active)) _uiState.update { it.copy(activePresetName = "") }
    }

    fun saveCurrentStateAsPreset(presetName: String) {
        val preset = try {
            PresetCodec.validate(currentPreset(presetName.trim()))
        } catch (e: Exception) {
            showError(e.message ?: "Invalid preset.")
            return
        }
        launchPresetOperation("Cannot save preset") {
            withContext(ioDispatcher) { presetManager.savePreset(preset) }
            refreshPresets()
            if (matchesCurrentState(preset)) _uiState.update { it.copy(activePresetName = preset.name) }
        }
    }

    fun applyPreset(preset: DacPreset) {
        if (!_uiState.value.hasDeviceState) {
            showError("Connect and sync the DAC before applying '${preset.name}'.")
            return
        }
        volumeJob?.cancel()
        balanceJob?.cancel()
        limitJob?.cancel()
        launchDeviceOperation("Applying '${preset.name}'...") { id -> applyPresetToDevice(id, preset) }
    }

    private suspend fun applyPresetToDevice(id: Long, preset: DacPreset) {
        PresetCodec.validate(preset)
        _uiState.update { it.copy(activePresetName = "") }
        // Keep the output muted while changing gain and channel settings.
        sendCommand(id, 4, 0xFF.toByte())
        sendBalance(id, preset.balanceBaseValue)
        sendCommand(id, 2, if (preset.isHighGain) 1 else 0)
        sendCommand(id, 1, preset.digitalFilterPos.toByte())
        sendCommand(id, 6, preset.ledPos.toByte())
        val raw = DacVolume.limit(preset.volumeRegister, _uiState.value.maxVolumeIndex)
        sendCommand(id, 4, raw.toByte())
        syncHardware(id)
        val expected = preset.copy(volumeIndex = DacVolume.closestIndex(raw), rawVolumeReg = raw)
        if (!matchesCurrentState(expected)) throw IOException("The DAC did not confirm all preset settings.")
        _uiState.update { it.copy(activePresetName = if (matchesCurrentState(preset)) preset.name else "") }
    }

    private suspend fun refreshPresets() {
        _presets.value = withContext(ioDispatcher) { presetManager.getAllPresets() }
        updateActivePreset()
    }

    private fun launchPresetOperation(message: String, operation: suspend () -> Unit) {
        viewModelScope.launch {
            presetMutex.withLock {
                try {
                    operation()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    showError("$message: ${e.message}")
                }
            }
        }
    }

    fun deletePresets(presetsToDelete: List<DacPreset>) {
        launchPresetOperation("Cannot delete presets") {
            try {
                withContext(ioDispatcher) { presetsToDelete.forEach { presetManager.deletePreset(it.name) } }
            } finally {
                refreshPresets()
                if (_presets.value.none { it.name == _uiState.value.startupPresetName }) setStartupPresetName("")
            }
        }
    }

    fun importPresetFromUri(context: Context, uri: Uri) {
        val appContext = context.applicationContext
        launchPresetOperation("Cannot import preset") {
            withContext(ioDispatcher) { presetManager.importAndSaveFromUri(appContext, uri) }
            refreshPresets()
        }
    }

    fun preparePresetExport(preset: DacPreset?): Boolean = try {
        savedStateHandle["pending_preset_export"] = PresetCodec.encode(preset ?: currentPreset("Magivant_Custom"))
        true
    } catch (e: Exception) {
        showError(e.message ?: "Cannot prepare preset export.")
        false
    }

    fun exportPreparedPreset(context: Context, uri: Uri?) {
        val json = savedStateHandle.remove<String>("pending_preset_export") ?: return
        if (uri == null) return
        val appContext = context.applicationContext
        launchPresetOperation("Cannot export preset") {
            val preset = PresetCodec.decode(json)
            withContext(ioDispatcher) { presetManager.exportToUri(appContext, uri, preset) }
        }
    }
}
