package com.rapli.magivant.usb

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.pow

class UsbDacManager(private val usbManager: UsbManager) {
    private val c1: Byte = 0xC1.toByte()
    private val a7: Byte = 0xA7.toByte()
    private val transferMutex = Mutex()

    var targetDevice: UsbDevice? = null
    private var activeConnection: UsbDeviceConnection? = null

    val isOpened: Boolean
        get() = activeConnection != null

    fun openConnection(): Boolean {
        val device = targetDevice ?: return false
        if (activeConnection == null) {
            try {
                activeConnection = usbManager.openDevice(device)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return activeConnection != null
    }

    fun closeConnection() {
        try {
            activeConnection?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            activeConnection = null
            targetDevice = null
        }
    }

    // Serialize two-step vendor control transfers on wIndex 0x09A0 so live telemetry polling never collides with register writes.
    suspend fun sendCommand(commandId: Byte, value1: Byte, value2: Byte = 0, value3: Byte = 0): Boolean =
        transferMutex.withLock {
            withContext(Dispatchers.IO) {
                val connection = activeConnection ?: return@withContext false

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

                val transferred = connection.controlTransfer(67, 160, 0, 2464, payload, 7, 3000)
                transferred >= 0
            }
        }

    suspend fun readData(commandId: Byte): ByteArray? =
        transferMutex.withLock {
            withContext(Dispatchers.IO) {
                val connection = activeConnection ?: return@withContext null
                var resultData: ByteArray? = null

                val payload = byteArrayOf(c1, a7, commandId, 0, 0, 0, 0)
                var sum = 0
                for (i in 0..5) sum += payload[i]
                payload[6] = (sum and 0xFF).toByte()

                val writeResult = connection.controlTransfer(67, 160, 0, 2464, payload, 7, 3000)

                if (writeResult >= 0) {
                    delay(10)
                    val buffer = ByteArray(7)
                    val bytesRead = connection.controlTransfer(195, 161, 0, 2464, buffer, 7, 3000)

                    if (bytesRead == 7) {
                        resultData = buffer
                    }
                }

                return@withContext resultData
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
