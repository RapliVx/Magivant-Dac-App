package com.rapli.magivant.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val volumeIndex: Int = 0,
    val balanceBaseValue: Int = 0,
    val isHighGain: Boolean = false,
    val digitalFilterPos: Int = 0,
    val ledPos: Int = 0
)

class MagivantViewModel(
    private val usbManager: UsbDacManager,
    private val presetManager: PresetManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(DacUiState())
    val uiState = _uiState.asStateFlow()
    private val _presets = MutableStateFlow<List<DacPreset>>(emptyList())
    val presets = _presets.asStateFlow()

    private var pollingJob: Job? = null

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
    }

    private fun refreshPresets() {
        _presets.value = presetManager.getAllPresets()
    }

    fun onDeviceConnected() {
        _uiState.value = _uiState.value.copy(isConnected = true)
        viewModelScope.launch {
            syncFirmware()
            delay(100)
            syncVolume()
            delay(100)
            syncOtherInfo1()
            startPollingLoop()
        }
    }

    fun onDeviceDisconnected() {
        pollingJob?.cancel()
        _uiState.value = DacUiState(isConnected = false)
    }

    private fun startPollingLoop() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                delay(500)
                syncVolume()
                delay(100)
                syncOtherInfo1()
            }
        }
    }

    private suspend fun syncFirmware() {
        val data = usbManager.readData((-96).toByte()) ?: return
        val fw = "${data[3].toInt() and 0xFF}.${data[4].toInt() and 0xFF}.${data[5].toInt() and 0xFF}"
        _uiState.value = _uiState.value.copy(firmwareVersion = fw)
    }

    private suspend fun syncVolume() {
        val data = usbManager.readData((-94).toByte()) ?: return
        val volIndex = appVolumeIntList.indexOf(data[4].toInt() and 0xFF).takeIf { it >= 0 } ?: 0
        val left = data[5].toInt() and 0xFF
        val right = data[6].toInt() and 0xFF
        val balance = if (left > 0) left else if (right > 0) -right else 0
        _uiState.value = _uiState.value.copy(volumeIndex = volIndex, balanceBaseValue = balance)
    }

    private suspend fun syncOtherInfo1() {
        val data = usbManager.readData((-93).toByte()) ?: return
        _uiState.value = _uiState.value.copy(
            digitalFilterPos = data[3].toInt() and 0xFF,
            isHighGain = (data[4].toInt() and 0xFF) == 1,
            ledPos = data[5].toInt() and 0xFF
        )
    }

    fun setVolume(index: Int) {
        _uiState.value = _uiState.value.copy(volumeIndex = index)
        viewModelScope.launch { usbManager.sendCommand(4, appVolumeIntList[index].toByte()) }
    }

    fun setBalance(balance: Int) {
        _uiState.value = _uiState.value.copy(balanceBaseValue = balance)
        val absVal = abs(balance).toByte()
        viewModelScope.launch {
            if (balance < 0) usbManager.sendCommand(5, 0, absVal)
            else if (balance > 0) usbManager.sendCommand(5, absVal, 0)
            else usbManager.sendCommand(5, 0, 0)
        }
    }

    fun setGain(isHigh: Boolean) {
        _uiState.value = _uiState.value.copy(isHighGain = isHigh)
        viewModelScope.launch {
            usbManager.sendCommand(2, if (isHigh) 1 else 0)
        }
    }

    fun setDigitalFilter(pos: Int) = viewModelScope.launch { usbManager.sendCommand(1, pos.toByte()) }
    fun setLed(pos: Int) = viewModelScope.launch { usbManager.sendCommand(6, pos.toByte()) }

    fun saveCurrentStateAsPreset(presetName: String) {
        val state = _uiState.value
        val newPreset = DacPreset(
            name = presetName,
            volumeIndex = state.volumeIndex,
            balanceBaseValue = state.balanceBaseValue,
            isHighGain = state.isHighGain,
            digitalFilterPos = state.digitalFilterPos,
            ledPos = state.ledPos
        )

        _presets.value = (_presets.value + newPreset).sortedBy { it.name }

        viewModelScope.launch(Dispatchers.IO) {
            presetManager.savePreset(newPreset)
            refreshPresets()
        }
    }

    fun applyPreset(preset: DacPreset) {
        if (!_uiState.value.isConnected) return
        viewModelScope.launch {
            setVolume(preset.volumeIndex)
            delay(100)
            setBalance(preset.balanceBaseValue)
            delay(100)
            setGain(preset.isHighGain)
            delay(100)
            setDigitalFilter(preset.digitalFilterPos)
            delay(100)
            setLed(preset.ledPos)
        }
    }

    fun deletePresets(presetsToDelete: List<DacPreset>) {
        _presets.value = _presets.value.filterNot { it in presetsToDelete }

        viewModelScope.launch(Dispatchers.IO) {
            presetsToDelete.forEach { preset ->
                presetManager.deletePreset(preset.name)
            }
            delay(150)
            refreshPresets()
        }
    }

    fun importPresetFromUri(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            presetManager.importAndSaveFromUri(context, uri)
            refreshPresets()
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
            presetManager.exportToUri(context, uri, preset)
        }
    }
}