package com.rapli.magivant.usb

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException

class UsbDacManager(private val usbManager: UsbManager) : DacConnection {
    private val transferMutex = Mutex()
    private var activeConnection: UsbDeviceConnection? = null
    private var activeSession: Long? = null

    override suspend fun openConnection(device: UsbDevice, sessionId: Long) {
        transferMutex.withLock {
            withContext(Dispatchers.IO) {
                try {
                    activeConnection?.close()
                } finally {
                    activeConnection = null
                    activeSession = null
                }
                activeConnection = usbManager.openDevice(device)
                    ?: throw IOException("Cannot open the DAC. Check USB permission and reconnect.")
                activeSession = sessionId
            }
        }
    }

    override suspend fun closeConnection(sessionId: Long) {
        transferMutex.withLock {
            withContext(Dispatchers.IO) {
                if (activeSession == sessionId) {
                    try {
                        activeConnection?.close()
                    } finally {
                        activeConnection = null
                        activeSession = null
                    }
                }
            }
        }
    }

    // Opening, closing, and both steps of a read share the same transfer lock.
    override suspend fun sendCommand(
        sessionId: Long,
        commandId: Byte,
        value1: Byte,
        value2: Byte,
        value3: Byte
    ) {
        transferMutex.withLock {
            withContext(Dispatchers.IO) {
                val connection = requireConnection(sessionId)
                val payload = createPayload(commandId, value1, value2, value3)
                val transferred = connection.controlTransfer(67, 160, 0, 2464, payload, payload.size, 3000)
                if (transferred != payload.size) throw IOException("USB command failed ($transferred/7 bytes).")
            }
        }
    }

    override suspend fun readData(sessionId: Long, commandId: Byte): ByteArray =
        transferMutex.withLock {
            withContext(Dispatchers.IO) {
                val connection = requireConnection(sessionId)
                val payload = createPayload(commandId)
                val written = connection.controlTransfer(67, 160, 0, 2464, payload, payload.size, 3000)
                if (written != payload.size) throw IOException("USB read request failed ($written/7 bytes).")
                delay(10)
                val buffer = ByteArray(7)
                val bytesRead = connection.controlTransfer(195, 161, 0, 2464, buffer, buffer.size, 3000)
                if (bytesRead != buffer.size) throw IOException("USB response failed ($bytesRead/7 bytes).")
                buffer
            }
        }

    private fun requireConnection(sessionId: Long): UsbDeviceConnection {
        if (activeSession != sessionId) throw IOException("The USB session has ended.")
        return activeConnection ?: throw IOException("The DAC is disconnected.")
    }

    companion object {
        fun isSupportedDevice(device: UsbDevice): Boolean =
            DacProtocol.isSupported(device.vendorId, device.productId)

        internal fun createPayload(
            commandId: Byte,
            value1: Byte = 0,
            value2: Byte = 0,
            value3: Byte = 0
        ): ByteArray = DacProtocol.createPayload(commandId, value1, value2, value3)
    }
}
