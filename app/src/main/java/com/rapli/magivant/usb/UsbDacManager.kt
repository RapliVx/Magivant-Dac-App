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
import java.util.Locale
import kotlin.math.pow

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
            device.vendorId == 0x2FC6 && device.productId in listOf(0xF13A, 0xF13B)

        internal fun createPayload(
            commandId: Byte,
            value1: Byte = 0,
            value2: Byte = 0,
            value3: Byte = 0
        ): ByteArray {
            val payload = byteArrayOf(0xC1.toByte(), 0xA7.toByte(), commandId, value1, value2, value3, 0)
            payload[6] = (payload.take(6).sumOf { it.toInt() } and 0xFF).toByte()
            return payload
        }
    }
}

object Cs43198Spec {
    // CS43198 registers 0x90001 (VOL_A) and 0x90002 (VOL_B) attenuate in 0.5 dB steps: 0x00 = 0.0 dBFS, 0xFE = -127.0 dBFS, 0xFF = Muted.
    fun rawRegToDbFs(rawVolumeReg: Int): Float {
        val clamped = rawVolumeReg.coerceIn(0x00, 0xFF)
        return -(clamped * 0.5f)
    }

    fun formatDbFs(rawVolumeReg: Int): String {
        val clamped = rawVolumeReg.coerceIn(0x00, 0xFF)
        if (clamped == 0xFF) return "Muted (-127.5 dBFS)"
        val db = rawRegToDbFs(clamped)
        return if (clamped == 0) "0.0 dBFS" else String.format(Locale.US, "%.1f dBFS", db)
    }

    // Full-scale output on CS43198 is 2.0 Vrms in High Gain (0 dB analog) and 1.0 Vrms in Low Gain (-6 dB analog).
    fun estimateOutputVrms(rawVolumeReg: Int, isHighGain: Boolean): Float {
        val clamped = rawVolumeReg.coerceIn(0x00, 0xFF)
        if (clamped == 0xFF) return 0f
        val fullScaleVrms = if (isHighGain) 2.0f else 1.0f
        val attenuationDb = rawRegToDbFs(clamped)
        return (fullScaleVrms * 10.0.pow(attenuationDb / 20.0)).toFloat()
    }

    fun formatVrms(rawVolumeReg: Int, isHighGain: Boolean): String {
        val vrms = estimateOutputVrms(rawVolumeReg, isHighGain)
        return when {
            vrms <= 0f -> "0.00 Vrms"
            vrms < 0.01f -> String.format(Locale.US, "%.1f mVrms", vrms * 1000f)
            else -> String.format(Locale.US, "%.2f Vrms", vrms)
        }
    }

    fun formatBalanceDb(balanceStep: Int): String {
        if (balanceStep == 0) return "Center (0.0 dB)"
        val attenuatedChannel = if (balanceStep > 0) "L" else "R"
        val biasDirection = if (balanceStep > 0) "R" else "L"
        val db = kotlin.math.abs(balanceStep) * 0.5f
        return String.format(Locale.US, "%s bias (%s -%.1f dB)", biasDirection, attenuatedChannel, db)
    }

    // Decodes Comtrue CT7601/CS43198 stream status byte returned by read command 0xA1 (-95).
    fun decodeSampleRateCode(code: Int): String = when (code and 0xFF) {
        0x00 -> "Standby"
        0x01, 0x10 -> "PCM 44.1 kHz"
        0x02, 0x11 -> "PCM 48.0 kHz"
        0x03, 0x12 -> "PCM 88.2 kHz"
        0x04, 0x13 -> "PCM 96.0 kHz"
        0x05, 0x14 -> "PCM 176.4 kHz"
        0x06, 0x15 -> "PCM 192.0 kHz"
        0x07, 0x16 -> "PCM 352.8 kHz"
        0x08, 0x17 -> "PCM 384.0 kHz"
        0x20 -> "DSD64 (2.82 MHz)"
        0x21 -> "DSD128 (5.64 MHz)"
        0x22 -> "DSD256 (11.29 MHz)"
        else -> String.format(Locale.US, "PCM/DSD (0x%02X)", code and 0xFF)
    }
}
