package com.rapli.magivant

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.rapli.magivant.ui.MagivantScreen
import com.rapli.magivant.ui.MagivantViewModel
import com.rapli.magivant.ui.PresetManager
import com.rapli.magivant.usb.UsbDacManager

class MainActivity : ComponentActivity() {

    companion object {
        private const val ACTION_USB_PERMISSION = "com.rapli.magivant.ACTION_USB_PERMISSION"
    }

    private lateinit var usbManager: UsbManager
    private lateinit var usbDacManager: UsbDacManager
    private lateinit var presetManager: PresetManager
    private lateinit var viewModel: MagivantViewModel

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_USB_PERMISSION -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        device?.let { connectToDevice(it) }
                    }
                }
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> checkUsbDevice()
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    usbDacManager.closeConnection()
                    viewModel.onDeviceDisconnected()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        usbDacManager = UsbDacManager(usbManager)

        presetManager = PresetManager(applicationContext)
        viewModel = MagivantViewModel(usbDacManager, presetManager)

        val filter = IntentFilter().apply {
            addAction(ACTION_USB_PERMISSION)
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }

        ContextCompat.registerReceiver(
            this,
            usbReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        checkUsbDevice()

        setContent {
            val context = LocalContext.current
            val isDarkTheme = isSystemInDarkTheme()

            val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

            val colorScheme = when {
                supportsDynamicColor && isDarkTheme -> dynamicDarkColorScheme(context)
                supportsDynamicColor && !isDarkTheme -> dynamicLightColorScheme(context)
                isDarkTheme -> darkColorScheme()
                else -> lightColorScheme()
            }

            MaterialTheme(colorScheme = colorScheme) {
                MagivantScreen(viewModel = viewModel)
            }
        }
    }

    private fun checkUsbDevice() {
        val deviceList = usbManager.deviceList
        val vId = 0x2FC6
        val pId1 = 0xF13A
        val pId2 = 0xF13B

        val target = deviceList.values.find {
            it.vendorId == vId && (it.productId == pId1 || it.productId == pId2)
        }

        target?.let { device ->
            if (usbManager.hasPermission(device)) {
                connectToDevice(device)
            } else {
                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_MUTABLE
                } else {
                    0
                }

                val permissionIntent = Intent(ACTION_USB_PERMISSION).apply {
                    setPackage(packageName)
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    this,
                    0,
                    permissionIntent,
                    flags
                )

                usbManager.requestPermission(device, pendingIntent)
            }
        }
    }

    private fun connectToDevice(device: UsbDevice) {
        usbDacManager.targetDevice = device
        usbDacManager.openConnection()
        viewModel.onDeviceConnected()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(usbReceiver)
        } catch (e: Exception) {
            // Safe unregister
        }
        usbDacManager.closeConnection()
    }
}