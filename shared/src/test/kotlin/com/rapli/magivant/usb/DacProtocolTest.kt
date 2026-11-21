package com.rapli.magivant.usb

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DacProtocolTest {
    @Test
    fun recognizesOnlySupportedMagivantUsbIds() {
        assertTrue(DacProtocol.isSupported(0x2FC6, 0xF13A))
        assertTrue(DacProtocol.isSupported(0x2FC6, 0xF13B))
        assertFalse(DacProtocol.isSupported(0x2FC6, 0xF13C))
        assertFalse(DacProtocol.isSupported(0x1234, 0xF13B))
    }

    @Test
    fun commandPayloadMatchesTheAndroidUsbProtocol() {
        assertArrayEquals(
            byteArrayOf(0xC1.toByte(), 0xA7.toByte(), 4, 0xFF.toByte(), 0, 0, 0x6B),
            DacProtocol.createPayload(4, 0xFF.toByte())
        )
    }
}
