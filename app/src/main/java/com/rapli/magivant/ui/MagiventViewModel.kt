package com.rapli.magivant.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rapli.magivant.usb.Cs43198Spec
import com.rapli.magivant.usb.DndManager
import com.rapli.magivant.usb.UsbDacManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

data class DacUiState(
    val isConnected: Boolean = false,
    val firmwareVersion: String = "",
    val sampleRateText: String = "Standby",
    val sampleRateRawCode: Int = 0,
    val bridgeModeText: String = "UAC 2.0",
    val volumeIndex: Int = 36,
    val rawVolumeReg: Int = 0x30,
    val balanceBaseValue: Int = 0,
    val isHighGain: Boolean = false,
    val digitalFilterPos: Int = 0,
    val ledPos: Int = 0,
    val isDndEnabled: Boolean = false,
    val maxVolumeIndex: Int = 60,
    val startupPresetName: String = "",
    val activePresetName: String = "",
    val isSyncing: Boolean = false,
    val syncMessage: String = "",
    val errorMessage: String? = null
)

class MagivantViewModel(
    application: Application,
    private val usbManager: UsbDacManager,
    private val presetManager: PresetManager,
    private val dndManager: DndManager
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(DacUiState())
    val uiState = _uiState.asStateFlow()
    private val _presets = MutableStateFlow<List<DacPreset>>(emptyList())
    val presets = _presets.asStateFlow()

    private var pollingJob: Job? = null

    private val prefs: SharedPreferences =
        application.getSharedPreferences("MagivantSettings", Context.MODE_PRIVATE)

    // Maps 0..60 slider steps to CS43198 VOL_A/VOL_B attenuation register bytes (0xFF = Muted, 0x00 = 0.0 dBFS).
    val appVolumeIntList = listOf(
        0xFF, 0xC8, 0xB4, 0xAA, 0xA0, 0x96, 0x8C, 0x82, 0x7A, 0x74,
        0x6E, 0x6A, 0x66, 0x62, 0x5E, 0x5A, 0x58, 0x56, 0x54, 0x52,
        0x50, 0x4E, 0x4C, 0x4A, 0x48, 0x46, 0x44, 0x42, 0x40, 0x3E,
        0x3C, 0x3A, 0x38, 0x36, 0x34, 0x32, 0x30, 0x2E, 0x2C, 0x2A,
        0x28, 0x26, 0x24, 0x22, 0x20, 0x1E, 0x1C, 0x1A, 0x18, 0x16,
        0x14, 0x12, 0x10, 0x0E, 0x0C, 0x0A, 0x08, 0x06, 0x04, 0x02, 0x00
    )

    init {
        refreshPresets()

        val savedDndState = prefs.getBoolean("dnd_enabled", false)
        val savedMaxVol = prefs.getInt("max_volume_index", 60).coerceIn(10, 60)
        val savedStartupPreset = prefs.getString("startup_preset_name", "") ?: ""
        _uiState.value = _uiState.value.copy(
            isDndEnabled = savedDndState,
            maxVolumeIndex = savedMaxVol,
            startupPresetName = savedStartupPreset
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun syncDndState() {
        if (dndManager.hasDndPermission()) {
            val systemState = dndManager.isDndActive()
            if (_uiState.value.isDndEnabled != systemState) {
                _uiState.value = _uiState.value.copy(isDndEnabled = systemState)
                prefs.edit().putBoolean("dnd_enabled", systemState).apply()
            }
        }
    }

    private fun refreshPresets() {
        _presets.value = presetManager.getAllPresets()
    }

    fun onDeviceConnected() {
        _uiState.value = _uiState.value.copy(
            isConnected = true,
            isSyncing = true,
            syncMessage = "Reading device state...",
            errorMessage = null
        )

        if (_uiState.value.isDndEnabled) {
            dndManager.setDndActive(true)
            syncDndState()
        }

        viewModelScope.launch {
            syncFirmware()
            delay(60)
            syncSampleRate()
            delay(60)
            syncBridgeStatus()
            delay(60)
            syncVolume()
            delay(60)
            syncOtherInfo1()
            _uiState.value = _uiState.value.copy(isSyncing = false, syncMessage = "")

            val startupName = _uiState.value.startupPresetName
            if (startupName.isNotEmpty()) {
                val targetPreset = _presets.value.find { it.name == startupName }
                if (targetPreset != null) {
                    applyPreset(targetPreset)
                }
            }

            startPollingLoop()
        }
    }

    fun manualResync() {
        if (!_uiState.value.isConnected) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSyncing = true,
                syncMessage = "Refreshing...",
                errorMessage = null
            )
            syncFirmware()
            delay(50)
            syncSampleRate()
            delay(50)
            syncBridgeStatus()
            delay(50)
            syncVolume()
            delay(50)
            syncOtherInfo1()
            _uiState.value = _uiState.value.copy(isSyncing = false, syncMessage = "")
        }
    }

    fun onDeviceDisconnected() {
        pollingJob?.cancel()

        val current = _uiState.value
        _uiState.value = DacUiState(
            isConnected = false,
            isDndEnabled = current.isDndEnabled,
            maxVolumeIndex = current.maxVolumeIndex,
            startupPresetName = current.startupPresetName
        )

        dndManager.setDndActive(false)
    }

    fun toggleDndMode(enabled: Boolean) {
        if (enabled && !dndManager.hasDndPermission()) return

        if (_uiState.value.isConnected) {
            dndManager.setDndActive(enabled)
            syncDndState()
        } else {
            _uiState.value = _uiState.value.copy(isDndEnabled = enabled)
            prefs.edit().putBoolean("dnd_enabled", enabled).apply()
        }
    }

    fun setMaxVolumeLimit(maxIndex: Int) {
        val clamped = maxIndex.coerceIn(10, 60)
        prefs.edit().putInt("max_volume_index", clamped).apply()
        _uiState.value = _uiState.value.copy(maxVolumeIndex = clamped)
        if (_uiState.value.isConnected && _uiState.value.volumeIndex > clamped) {
            setVolume(clamped)
        }
    }

    fun setStartupPresetName(presetName: String) {
        prefs.edit().putString("startup_preset_name", presetName).apply()
        _uiState.value = _uiState.value.copy(startupPresetName = presetName)
    }

    private fun startPollingLoop() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                delay(750)
                syncSampleRate()
                delay(80)
                syncVolume()
                delay(80)
                syncOtherInfo1()
            }
        }
    }

    private suspend fun syncFirmware() {
        val data = usbManager.readData((-96).toByte()) ?: return
        val fw = "${data[3].toInt() and 0xFF}.${data[4].toInt() and 0xFF}.${data[5].toInt() and 0xFF}"
        _uiState.value = _uiState.value.copy(firmwareVersion = fw)
    }

    private suspend fun syncSampleRate() {
        val data = usbManager.readData((-95).toByte()) ?: return
        val code = data[3].toInt() and 0xFF
        val decoded = Cs43198Spec.decodeSampleRateCode(code)
        _uiState.value = _uiState.value.copy(
            sampleRateRawCode = code,
            sampleRateText = decoded
        )
    }

    private suspend fun syncBridgeStatus() {
        val data = usbManager.readData((-92).toByte()) ?: return
        val modeFlag = data[4].toInt() and 0xFF
        val modeLabel = if (modeFlag == 1) "UAC 2.0" else "UAC Mode $modeFlag"
        _uiState.value = _uiState.value.copy(bridgeModeText = modeLabel)
    }

    private suspend fun syncVolume() {
        val data = usbManager.readData((-94).toByte()) ?: return
        val rawReg = data[4].toInt() and 0xFF
        val closestIndex = appVolumeIntList.indices.minByOrNull {
            abs(appVolumeIntList[it] - rawReg)
        } ?: 0
        val left = data[5].toInt() and 0xFF
        val right = data[6].toInt() and 0xFF
        val balance = if (left > 0) left else if (right > 0) -right else 0
        _uiState.value = _uiState.value.copy(
            volumeIndex = closestIndex,
            rawVolumeReg = rawReg,
            balanceBaseValue = balance
        )
    }

    private suspend fun syncOtherInfo1() {
        val data = usbManager.readData((-93).toByte()) ?: return
        _uiState.value = _uiState.value.copy(
            digitalFilterPos = (data[3].toInt() and 0xFF).coerceIn(0, 4),
            isHighGain = (data[4].toInt() and 0xFF) == 1,
            ledPos = (data[5].toInt() and 0xFF).coerceIn(0, 2)
        )
    }

    fun setVolume(index: Int) {
        val maxAllowed = _uiState.value.maxVolumeIndex
        val safeIndex = index.coerceIn(0, maxAllowed)
        val regValue = appVolumeIntList[safeIndex]
        _uiState.value = _uiState.value.copy(
            volumeIndex = safeIndex,
            rawVolumeReg = regValue
        )
        viewModelScope.launch {
            val ok = usbManager.sendCommand(4, regValue.toByte())
            if (!ok && _uiState.value.isConnected) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to set volume over USB."
                )
            }
        }
    }

    // Steps hardware volume register directly by 0.5 dB per step (register 0x00 is loudest, 0xFF is muted).
    fun adjustVolumeByHalfDb(stepDelta: Int) {
        val maxIndex = _uiState.value.maxVolumeIndex
        val minAllowedReg = appVolumeIntList[maxIndex]
        val currentReg = _uiState.value.rawVolumeReg
        val nextReg = (currentReg - stepDelta).coerceIn(minAllowedReg, 0xFF)
        val closestIndex = appVolumeIntList.indices.minByOrNull {
            abs(appVolumeIntList[it] - nextReg)
        } ?: 0

        _uiState.value = _uiState.value.copy(
            rawVolumeReg = nextReg,
            volumeIndex = closestIndex.coerceAtMost(maxIndex)
        )
        viewModelScope.launch {
            usbManager.sendCommand(4, nextReg.toByte())
        }
    }

    fun setBalance(balance: Int) {
        val clamped = balance.coerceIn(-10, 10)
        _uiState.value = _uiState.value.copy(balanceBaseValue = clamped)
        val absVal = abs(clamped).toByte()
        viewModelScope.launch {
            if (clamped < 0) usbManager.sendCommand(5, 0, absVal)
            else if (clamped > 0) usbManager.sendCommand(5, absVal, 0)
            else usbManager.sendCommand(5, 0, 0)
        }
    }

    fun setGain(isHigh: Boolean) {
        _uiState.value = _uiState.value.copy(isHighGain = isHigh)
        viewModelScope.launch {
            usbManager.sendCommand(2, if (isHigh) 1 else 0)
        }
    }

    fun setDigitalFilter(pos: Int) {
        val clamped = pos.coerceIn(0, 4)
        _uiState.value = _uiState.value.copy(digitalFilterPos = clamped)
        viewModelScope.launch {
            usbManager.sendCommand(1, clamped.toByte())
        }
    }

    fun setLed(pos: Int) {
        val clamped = pos.coerceIn(0, 2)
        _uiState.value = _uiState.value.copy(ledPos = clamped)
        viewModelScope.launch {
            usbManager.sendCommand(6, clamped.toByte())
        }
    }

    fun saveCurrentStateAsPreset(presetName: String) {
        val trimmed = presetName.trim()
        if (trimmed.isEmpty()) return
        val state = _uiState.value
        val newPreset = DacPreset(
            name = trimmed,
            volumeIndex = state.volumeIndex,
            balanceBaseValue = state.balanceBaseValue,
            isHighGain = state.isHighGain,
            digitalFilterPos = state.digitalFilterPos,
            ledPos = state.ledPos
        )

        _presets.value = (_presets.value.filterNot { it.name == trimmed } + newPreset).sortedBy { it.name }
        _uiState.value = _uiState.value.copy(activePresetName = trimmed)

        viewModelScope.launch(Dispatchers.IO) {
            presetManager.savePreset(newPreset)
            refreshPresets()
        }
    }

    fun applyPreset(preset: DacPreset) {
        if (!_uiState.value.isConnected) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Connect the DAC before applying '${preset.name}'."
            )
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSyncing = true,
                syncMessage = "Applying '${preset.name}'...",
                activePresetName = preset.name,
                errorMessage = null
            )
            setVolume(preset.volumeIndex)
            delay(90)
            setBalance(preset.balanceBaseValue)
            delay(90)
            setGain(preset.isHighGain)
            delay(90)
            setDigitalFilter(preset.digitalFilterPos)
            delay(90)
            setLed(preset.ledPos)
            delay(60)
            _uiState.value = _uiState.value.copy(
                isSyncing = false,
                syncMessage = ""
            )
        }
    }

    fun deletePresets(presetsToDelete: List<DacPreset>) {
        val namesToDelete = presetsToDelete.map { it.name }.toSet()
        _presets.value = _presets.value.filterNot { it.name in namesToDelete }
        if (_uiState.value.startupPresetName in namesToDelete) {
            setStartupPresetName("")
        }

        viewModelScope.launch(Dispatchers.IO) {
            presetsToDelete.forEach { preset ->
                presetManager.deletePreset(preset.name)
            }
            delay(100)
            refreshPresets()
        }
    }

    fun importPresetFromUri(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val imported = presetManager.importAndSaveFromUri(context, uri)
            refreshPresets()
            if (imported == null) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Invalid preset JSON file."
                )
            }
        }
    }

    private fun getFileNameFromUri(context: Context, uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        result = cursor.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result?.substringBeforeLast(".") ?: "Magivant_Preset"
    }

    fun exportPresetToUri(context: Context, uri: Uri, presetToExport: DacPreset?) {
        viewModelScope.launch(Dispatchers.IO) {
            val actualFileName = getFileNameFromUri(context, uri)
            val preset = presetToExport?.copy(name = actualFileName) ?: DacPreset(
                name = actualFileName,
                volumeIndex = _uiState.value.volumeIndex,
                balanceBaseValue = _uiState.value.balanceBaseValue,
                isHighGain = _uiState.value.isHighGain,
                digitalFilterPos = _uiState.value.digitalFilterPos,
                ledPos = _uiState.value.ledPos
            )
            val ok = presetManager.exportToUri(context, uri, preset)
            if (!ok) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to export preset file."
                )
            }
        }
    }
}
