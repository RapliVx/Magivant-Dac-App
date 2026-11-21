package com.rapli.magivant.ui

data class DacUiState(
    val isConnected: Boolean = false,
    val hasDeviceState: Boolean = false,
    val firmwareVersion: String = "",
    val sampleRateText: String = "Standby",
    val sampleRateRawCode: Int = 0,
    val bridgeModeText: String = "Unknown",
    val volumeIndex: Int = 36,
    val rawVolumeReg: Int = 0x30,
    val balanceBaseValue: Int = 0,
    val isVolumePending: Boolean = false,
    val isBalancePending: Boolean = false,
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
