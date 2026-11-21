package com.rapli.magivant.desktop

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import com.rapli.magivant.usb.DacProtocol
import org.usb4java.Context
import org.usb4java.DeviceDescriptor
import org.usb4java.DeviceHandle
import org.usb4java.DeviceList
import org.usb4java.LibUsb
import org.usb4java.Loader
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

data class ConnectedDac(val productId: Int) {
    val name: String
        get() = when (productId) {
            0xF13A -> "Magivant DAC (F13A)"
            else -> "Magivant DAC (F13B)"
        }
}

class LinuxDacTransport : AutoCloseable {
    private val context = Context()
    private val transferMutex = Mutex()
    private val initialized = AtomicBoolean(false)
    private var handle: DeviceHandle? = null

    fun initialize() {
        if (initialized.get()) return
        synchronized(this) {
            if (initialized.get()) return
            try {
                Loader.load()
            } catch (error: UnsatisfiedLinkError) {
                throw IOException("Cannot load the bundled Linux USB library: ${error.message}", error)
            }
            val result = LibUsb.init(context)
            if (result != LibUsb.SUCCESS) throw IOException("Cannot initialize USB access: ${LibUsb.errorName(result)}")
            initialized.set(true)
        }
    }

    suspend fun connect(): ConnectedDac? = withContext(Dispatchers.IO) {
        transferMutex.withLock {
            if (handle != null) return@withLock ConnectedDac(activeProductId)
            initialize()

            val devices = DeviceList()
            val count = LibUsb.getDeviceList(context, devices)
            if (count < 0) throw usbError("Cannot scan USB devices", count)

            var hasSupportedDevice = false
            var accessError: IOException? = null
            try {
                for (device in devices) {
                    val descriptor = DeviceDescriptor()
                    val descriptorResult = LibUsb.getDeviceDescriptor(device, descriptor)
                    if (descriptorResult != LibUsb.SUCCESS) continue

                    val vendorId = descriptor.idVendor().toInt() and 0xFFFF
                    val productId = descriptor.idProduct().toInt() and 0xFFFF
                    if (!DacProtocol.isSupported(vendorId, productId)) continue
                    hasSupportedDevice = true

                    val candidate = DeviceHandle()
                    val openResult = LibUsb.open(device, candidate)
                    if (openResult == LibUsb.SUCCESS) {
                        handle = candidate
                        activeProductId = productId
                        return@withLock ConnectedDac(productId)
                    }
                    accessError = usbError("Cannot open Magivant DAC (VID:PID %04X:%04X)".format(vendorId, productId), openResult)
                }
            } finally {
                LibUsb.freeDeviceList(devices, true)
            }

            if (hasSupportedDevice) {
                throw accessError ?: IOException("Cannot access the Magivant DAC USB interface.")
            }
            null
        }
    }

    @Volatile
    private var activeProductId: Int = 0xF13B

    suspend fun sendCommand(commandId: Byte, value1: Byte, value2: Byte = 0, value3: Byte = 0) =
        withContext(Dispatchers.IO) {
            transferMutex.withLock {
                val payload = createPayload(commandId, value1, value2, value3)
                val result = LibUsb.controlTransfer(
                    requireHandle(), OUT_REQUEST_TYPE, WRITE_REQUEST, 0.toShort(), REQUEST_INDEX,
                    ByteBuffer.allocateDirect(payload.size).put(payload).apply { flip() }, TIMEOUT_MS
                )
                if (result != payload.size) throw usbError("DAC command failed (${result.coerceAtLeast(0)}/${payload.size} bytes)", result)
            }
        }

    suspend fun readData(commandId: Byte): ByteArray = withContext(Dispatchers.IO) {
        transferMutex.withLock {
            val payload = createPayload(commandId)
            val written = LibUsb.controlTransfer(
                requireHandle(), OUT_REQUEST_TYPE, WRITE_REQUEST, 0.toShort(), REQUEST_INDEX,
                ByteBuffer.allocateDirect(payload.size).put(payload).apply { flip() }, TIMEOUT_MS
            )
            if (written != payload.size) throw usbError("DAC read request failed", written)
            Thread.sleep(10)

            val buffer = ByteBuffer.allocateDirect(READ_SIZE)
            val received = LibUsb.controlTransfer(
                requireHandle(), IN_REQUEST_TYPE, READ_REQUEST, 0.toShort(), REQUEST_INDEX, buffer, TIMEOUT_MS
            )
            if (received != READ_SIZE) throw usbError("DAC response failed ($received/$READ_SIZE bytes)", received)
            ByteArray(READ_SIZE).also { buffer.position(0); buffer.get(it) }
        }
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        transferMutex.withLock { closeHandle() }
    }

    private fun requireHandle(): DeviceHandle = handle ?: throw IOException("The DAC is disconnected.")

    private fun closeHandle() {
        handle?.let(LibUsb::close)
        handle = null
    }

    private fun usbError(message: String, result: Int) = IOException(
        when (result) {
            LibUsb.ERROR_ACCESS -> "$message: permission denied. Install the Magivant udev rule, then reconnect the DAC."
            else -> "$message: ${LibUsb.errorName(result)}"
        }
    )

    override fun close() {
        if (!initialized.get()) return
        synchronized(this) {
            if (initialized.get()) {
                handle?.let(LibUsb::close)
                handle = null
                LibUsb.exit(context)
                initialized.set(false)
            }
        }
    }

    companion object {
        private val OUT_REQUEST_TYPE = 0x43.toByte()
        private val IN_REQUEST_TYPE = 0xC3.toByte()
        private const val WRITE_REQUEST = 0xA0.toByte()
        private const val READ_REQUEST = 0xA1.toByte()
        private const val REQUEST_INDEX: Short = 2464
        private const val TIMEOUT_MS = 3000L
        private const val READ_SIZE = 7

        fun createPayload(commandId: Byte, value1: Byte = 0, value2: Byte = 0, value3: Byte = 0) =
            com.rapli.magivant.usb.DacProtocol.createPayload(commandId, value1, value2, value3)
    }
}
