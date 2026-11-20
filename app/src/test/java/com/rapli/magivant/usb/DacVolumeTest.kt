package com.rapli.magivant.usb

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DacVolumeTest {
    @Test
    fun registerTable_isStrictlyDescendingFromMuteToFullScale() {
        assertEquals(61, DacVolume.registerValues.size)
        assertEquals(255, DacVolume.registerValues.first())
        assertEquals(0, DacVolume.registerValues.last())
        assertTrue(DacVolume.registerValues.zipWithNext().all { (left, right) -> left > right })
    }

    @Test
    fun volumeLimit_usesTheRegisterInsteadOfRoundedIndex() {
        assertEquals(36, DacVolume.closestIndex(47))
        assertEquals(48, DacVolume.limit(47, 36))
        assertEquals(255, DacVolume.limit(255, 36))
        assertEquals(80, DacVolume.limit(80, 36))
    }

    @Test
    fun everyRegister_isClampedAtEverySupportedLimit() {
        for (limit in 10..60) {
            for (raw in 0..255) {
                assertTrue(DacVolume.limit(raw, limit) >= DacVolume.registerValues[limit])
            }
        }
    }

    @Test
    fun commandPayload_hasSevenBytesAndUnsignedChecksum() {
        val payload = UsbDacManager.createPayload(4, 0xFF.toByte())
        assertEquals(7, payload.size)
        assertEquals(0xC1, payload[0].toInt() and 0xFF)
        assertEquals(0xA7, payload[1].toInt() and 0xFF)
        assertEquals(4, payload[2].toInt())
        assertEquals((0xC1 + 0xA7 + 4 + 0xFF) and 0xFF, payload[6].toInt() and 0xFF)
    }
}
