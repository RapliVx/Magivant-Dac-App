package com.rapli.magivant.usb

import android.hardware.usb.UsbDevice

interface DacConnection {
    suspend fun openConnection(device: UsbDevice, sessionId: Long)
    suspend fun closeConnection(sessionId: Long)
    suspend fun sendCommand(sessionId: Long, commandId: Byte, value1: Byte, value2: Byte = 0, value3: Byte = 0)
    suspend fun readData(sessionId: Long, commandId: Byte): ByteArray
}
