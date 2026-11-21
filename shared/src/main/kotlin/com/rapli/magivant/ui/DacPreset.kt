package com.rapli.magivant.ui

import com.google.gson.annotations.SerializedName
import com.rapli.magivant.usb.DacVolume

data class DacPreset(
    @SerializedName("name")
    val name: String,
    @SerializedName("volumeIndex")
    val volumeIndex: Int,
    @SerializedName("balanceBaseValue")
    val balanceBaseValue: Int,
    @SerializedName("isHighGain")
    val isHighGain: Boolean,
    @SerializedName("digitalFilterPos")
    val digitalFilterPos: Int,
    @SerializedName("ledPos")
    val ledPos: Int,
    @SerializedName("rawVolumeReg")
    val rawVolumeReg: Int? = null,
    @SerializedName("schemaVersion")
    val schemaVersion: Int = 1
) {
    val volumeRegister: Int
        get() = rawVolumeReg ?: DacVolume.registerValues[volumeIndex]
}
