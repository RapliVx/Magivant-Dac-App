package com.rapli.magivant.desktop

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class LinuxDacTransportTest {
    @Test
    fun loadsBundledUsbLibraryWithoutRequiringAConnectedDac() {
        val transport = LinuxDacTransport()
        try {
            transport.initialize()
        } finally {
            transport.close()
        }
    }

    @Test
    fun commandPayloadMatchesTheSharedDacProtocol() {
        assertArrayEquals(
            byteArrayOf(0xC1.toByte(), 0xA7.toByte(), 4, 60, 0, 0, 0xA8.toByte()),
            LinuxDacTransport.createPayload(4, 60)
        )
    }
}
