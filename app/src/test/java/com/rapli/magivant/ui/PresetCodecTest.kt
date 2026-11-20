package com.rapli.magivant.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream

class PresetCodecTest {
    private val legacy = """{"name":"IEM","volumeIndex":36,"balanceBaseValue":0,"isHighGain":false,"digitalFilterPos":4,"ledPos":0}"""

    @Test
    fun legacyPreset_retainsItsVolumeAndSettings() {
        val preset = PresetCodec.decode(legacy)
        assertEquals(1, preset.schemaVersion)
        assertEquals(48, preset.volumeRegister)
        assertEquals(4, preset.digitalFilterPos)
    }

    @Test
    fun precisePreset_roundTripRetainsHalfDbStep() {
        val preset = PresetCodec.decode(legacy).copy(schemaVersion = 2, rawVolumeReg = 47)
        assertEquals(preset, PresetCodec.decode(PresetCodec.encode(preset)))
        assertEquals(47, PresetCodec.decode(PresetCodec.encode(preset)).volumeRegister)
    }

    @Test
    fun quietUnmutedPreset_doesNotBecomeMutedAfterRoundTrip() {
        val preset = PresetCodec.decode(legacy).copy(schemaVersion = 2, volumeIndex = 0, rawVolumeReg = 254)
        assertEquals(254, PresetCodec.decode(PresetCodec.encode(preset)).volumeRegister)
    }

    @Test
    fun missingFields_areRejected() {
        listOf("{}", "null", "[]", legacy.replace("\"ledPos\":0", "\"unused\":0")).forEach {
            assertThrows(Exception::class.java) { PresetCodec.decode(it) }
        }
    }

    @Test
    fun invalidTypes_areRejected() {
        listOf(
            legacy.replace("\"volumeIndex\":36", "\"volumeIndex\":\"36\""),
            legacy.replace("\"volumeIndex\":36", "\"volumeIndex\":36.5"),
            legacy.replace("\"isHighGain\":false", "\"isHighGain\":\"false\""),
            legacy.replace("\"name\":\"IEM\"", "\"name\":null")
        ).forEach { assertThrows(Exception::class.java) { PresetCodec.decode(it) } }
    }

    @Test
    fun outOfRangeValues_areRejected() {
        listOf(
            "volumeIndex" to 61, "volumeIndex" to -1, "balanceBaseValue" to 11,
            "balanceBaseValue" to -11, "digitalFilterPos" to 5, "ledPos" to 3
        ).forEach { (key, value) ->
            val invalid = legacy.replace(Regex("\"$key\":-?\\d+"), "\"$key\":$value")
            assertThrows(IllegalArgumentException::class.java) { PresetCodec.decode(invalid) }
        }
    }

    @Test
    fun unsupportedVersionAndInconsistentRegister_areRejected() {
        val preset = PresetCodec.decode(legacy)
        listOf(
            preset.copy(schemaVersion = 3), preset.copy(schemaVersion = 2),
            preset.copy(rawVolumeReg = 256), preset.copy(rawVolumeReg = 0)
        ).forEach { assertThrows(IllegalArgumentException::class.java) { PresetCodec.encode(it) } }
    }

    @Test
    fun unsafeNames_areRejected() {
        val preset = PresetCodec.decode(legacy)
        listOf("../outside", "..\\outside", "", " ", ".", "..", "line\nfeed", "x".repeat(81)).forEach {
            assertThrows(IllegalArgumentException::class.java) { PresetCodec.encode(preset.copy(name = it)) }
        }
    }

    @Test
    fun oversizedInput_isRejectedBeforeItCanBeSaved() {
        val bytes = ByteArray(64 * 1024 + 1) { ' '.code.toByte() }
        assertThrows(IllegalArgumentException::class.java) { PresetCodec.read(ByteArrayInputStream(bytes)) }
    }

    @Test
    fun malformedAndTrailingJson_areRejected() {
        listOf("{", "$legacy {}", legacy.replace("\"ledPos\":0", "\"ledPos\":0,")).forEach {
            assertThrows(Exception::class.java) { PresetCodec.decode(it) }
        }
    }
}
