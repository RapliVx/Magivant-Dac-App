package com.rapli.magivant

import com.rapli.magivant.usb.Cs43198Spec
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun cs43198AttenuationMath_isAccurate() {
        assertEquals(0.0f, Cs43198Spec.rawRegToDbFs(0x00), 0.001f)
        assertEquals(-24.0f, Cs43198Spec.rawRegToDbFs(0x30), 0.001f)
        assertEquals(-127.0f, Cs43198Spec.rawRegToDbFs(0xFE), 0.001f)
        assertEquals("0.0 dBFS", Cs43198Spec.formatDbFs(0x00))
        assertEquals("-24.0 dBFS", Cs43198Spec.formatDbFs(0x30))
        assertEquals("Muted (-127.5 dBFS)", Cs43198Spec.formatDbFs(0xFF))
    }

    @Test
    fun cs43198VrmsEstimation_matchesGainRail() {
        assertEquals(2.0f, Cs43198Spec.estimateOutputVrms(0x00, isHighGain = true), 0.001f)
        assertEquals(1.0f, Cs43198Spec.estimateOutputVrms(0x00, isHighGain = false), 0.001f)
        assertEquals(0.0f, Cs43198Spec.estimateOutputVrms(0xFF, isHighGain = true), 0.001f)
        assertEquals("2.00 Vrms", Cs43198Spec.formatVrms(0x00, isHighGain = true))
        assertEquals("1.00 Vrms", Cs43198Spec.formatVrms(0x00, isHighGain = false))
    }

    @Test
    fun cs43198SampleRateDecoder_handlesPcmAndDsd() {
        assertEquals("Idle / Standby", Cs43198Spec.decodeSampleRateCode(0x00))
        assertEquals("PCM 44.1 kHz", Cs43198Spec.decodeSampleRateCode(0x10))
        assertEquals("PCM 384.0 kHz", Cs43198Spec.decodeSampleRateCode(0x17))
        assertEquals("DSD256 (11.29 MHz)", Cs43198Spec.decodeSampleRateCode(0x22))
    }
}
