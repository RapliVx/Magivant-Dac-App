package com.rapli.magivant.usb

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class UsbDacManager(private val usbManager: UsbManager) {

    private val c1: Byte = 0xC1.toByte()
    private val a7: Byte = 0xA7.toByte()

    var targetDevice: UsbDevice? = null

    suspend fun sendCommand(commandId: Byte, value1: Byte, value2: Byte = 0, value3: Byte = 0) {
        withContext(Dispatchers.IO) {
            val device = targetDevice ?: return@withContext
            var connection: UsbDeviceConnection? = null

            try {
                connection = usbManager.openDevice(device)
                if (connection != null) {
                    val payload = ByteArray(7)
                    payload[0] = c1
                    payload[1] = a7
                    payload[2] = commandId
                    payload[3] = value1
                    payload[4] = value2
                    payload[5] = value3

                    var sum = 0
                    for (i in 0..5) sum += payload[i]
                    payload[6] = (sum and 0xFF).toByte()

                    connection.controlTransfer(67, 160, 0, 2464, payload, 7, 3000)
                }
            } finally {
                connection?.close()
            }
        }
    }

    suspend fun readData(commandId: Byte): ByteArray? = withContext(Dispatchers.IO) {
        val device = targetDevice ?: return@withContext null
        var connection: UsbDeviceConnection? = null
        var resultData: ByteArray? = null

        try {
            connection = usbManager.openDevice(device)
            if (connection != null) {
                val payload = byteArrayOf(c1, a7, commandId, 0, 0, 0, 0)
                var sum = 0
                for (i in 0..5) sum += payload[i]
                payload[6] = (sum and 0xFF).toByte()

                connection.controlTransfer(67, 160, 0, 2464, payload, 7, 3000)
                delay(10)
                val buffer = ByteArray(7)
                val bytesRead = connection.controlTransfer(195, 161, 0, 2464, buffer, 7, 3000)

                if (bytesRead == 7) {
                    resultData = buffer
                }
            }
        } finally {
            connection?.close()
        }
        return@withContext resultData
    }
}