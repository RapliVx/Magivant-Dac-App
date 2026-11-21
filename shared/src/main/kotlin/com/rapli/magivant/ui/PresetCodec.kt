package com.rapli.magivant.ui

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.Strictness
import com.rapli.magivant.usb.DacVolume
import java.io.InputStream

object PresetCodec {
    private const val MAX_FILE_BYTES = 64 * 1024
    private val gson = GsonBuilder().setStrictness(Strictness.STRICT).setPrettyPrinting().create()

    fun validate(preset: DacPreset): DacPreset {
        require(preset.name.isNotBlank() && preset.name.length <= 80) { "Preset name must contain 1–80 characters." }
        require(preset.name.none { it == '/' || it == '\\' || it.isISOControl() }) { "Invalid preset name." }
        require(preset.name != "." && preset.name != "..") { "Invalid preset name." }
        require(preset.schemaVersion in 1..2) { "Unsupported preset version." }
        require(preset.volumeIndex in 0..60) { "Volume index must be between 0 and 60." }
        require(preset.balanceBaseValue in -10..10) { "Balance must be between -10 and 10." }
        require(preset.digitalFilterPos in 0..4) { "Invalid digital filter." }
        require(preset.ledPos in 0..2) { "Invalid LED mode." }
        require(preset.rawVolumeReg == null || preset.rawVolumeReg in 0..255) { "Invalid volume register." }
        require(preset.rawVolumeReg == null || DacVolume.closestIndex(preset.rawVolumeReg) == preset.volumeIndex) {
            "Volume index does not match the volume register."
        }
        require(preset.schemaVersion != 2 || preset.rawVolumeReg != null) { "Volume register is required." }
        return preset
    }

    fun decode(json: String): DacPreset {
        require(json.toByteArray(Charsets.UTF_8).size <= MAX_FILE_BYTES) { "Preset file is too large." }
        val data = requireNotNull(gson.fromJson(json, JsonObject::class.java)) { "Preset must be a JSON object." }
        fun integer(key: String): Int {
            val value = data.get(key)
            require(value?.isJsonPrimitive == true && value.asJsonPrimitive.isNumber) { "Missing or invalid '$key'." }
            return requireNotNull(value.asString.toIntOrNull()) { "Invalid integer '$key'." }
        }
        val name = data.get("name")
        require(name?.isJsonPrimitive == true && name.asJsonPrimitive.isString) { "Preset name is required." }
        val gain = data.get("isHighGain")
        require(gain?.isJsonPrimitive == true && gain.asJsonPrimitive.isBoolean) { "Gain must be a boolean." }
        return validate(
            DacPreset(
                name = name.asString.trim(),
                volumeIndex = integer("volumeIndex"),
                balanceBaseValue = integer("balanceBaseValue"),
                isHighGain = gain.asBoolean,
                digitalFilterPos = integer("digitalFilterPos"),
                ledPos = integer("ledPos"),
                rawVolumeReg = if (data.has("rawVolumeReg")) integer("rawVolumeReg") else null,
                schemaVersion = if (data.has("schemaVersion")) integer("schemaVersion") else 1
            )
        )
    }

    fun encode(preset: DacPreset): String = gson.toJson(validate(preset))

    fun read(input: InputStream): DacPreset {
        val bytes = ByteArray(MAX_FILE_BYTES + 1)
        var count = 0
        while (count < bytes.size) {
            val read = input.read(bytes, count, bytes.size - count)
            if (read < 0) break
            count += read
        }
        require(count <= MAX_FILE_BYTES) { "Preset file is too large." }
        return decode(String(bytes, 0, count, Charsets.UTF_8))
    }
}
