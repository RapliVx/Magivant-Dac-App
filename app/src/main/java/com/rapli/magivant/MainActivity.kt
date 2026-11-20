package com.rapli.magivant

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rapli.magivant.ui.MagivantScreen
import com.rapli.magivant.ui.MagivantViewModel
import com.rapli.magivant.ui.PresetManager
import com.rapli.magivant.ui.theme.MAGIVANTDACTheme
import com.rapli.magivant.ui.theme.ThemeController
import com.rapli.magivant.usb.DndManager
import com.rapli.magivant.usb.UsbDacManager

class MainActivity : ComponentActivity() {

    companion object {
        private const val ACTION_USB_PERMISSION = "com.rapli.magivant.ACTION_USB_PERMISSION"
    }

    private lateinit var usbManager: UsbManager
    private var permissionDeviceName: String? = null
    private val viewModel by viewModels<MagivantViewModel> {
        viewModelFactory {
            initializer {
                val application = requireNotNull(this[APPLICATION_KEY])
                MagivantViewModel(
                    application,
                    UsbDacManager(application.getSystemService(Context.USB_SERVICE) as UsbManager),
                    PresetManager(application),
                    DndManager(application),
                    createSavedStateHandle()
                )
            }
        }
    }

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_USB_PERMISSION -> {
                    permissionDeviceName = null
                    val device = intent.usbDevice()
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false) &&
                        device != null && usbManager.hasPermission(device)
                    ) {
                        viewModel.connectToDevice(device)
                    } else {
                        viewModel.showError("USB permission denied. Tap Scan USB to try again.")
                    }
                }
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> checkUsbDevice()
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    intent.usbDevice()?.let { device ->
                        if (permissionDeviceName == device.deviceName) permissionDeviceName = null
                        viewModel.onDeviceDetached(device)
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false
        window.decorView.isForceDarkAllowed = false
        super.onCreate(savedInstanceState)

        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        permissionDeviceName = savedInstanceState?.getString("usb_permission_device")

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
            val prefs = remember {
                context.getSharedPreferences(ThemeController.PREFS_NAME, Context.MODE_PRIVATE)
            }
            var appSettings by remember {
                mutableStateOf(ThemeController.getAppSettings(context))
            }

            DisposableEffect(prefs) {
                val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
                    appSettings = ThemeController.getAppSettings(context)
                }
                prefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose {
                    prefs.unregisterOnSharedPreferenceChangeListener(listener)
                }
            }

            val density = LocalDensity.current
            val customDensity = remember(density, appSettings.dpiScale) {
                Density(
                    density = density.density * appSettings.dpiScale,
                    fontScale = density.fontScale
                )
            }

            MAGIVANTDACTheme(appSettings = appSettings) {
                CompositionLocalProvider(LocalDensity provides customDensity) {
                    MagivantScreen(
                        viewModel = viewModel,
                        appSettings = appSettings,
                        onUpdateAppSettings = { updated ->
                            appSettings = updated
                        },
                        onCheckDndPermission = { enable ->
                            viewModel.toggleDndMode(enable)
                        },
                        onScanUsb = {
                            checkUsbDevice()
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkUsbDevice(requestPermission = false)
        viewModel.syncDndState()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        checkUsbDevice()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("usb_permission_device", permissionDeviceName)
        super.onSaveInstanceState(outState)
    }

    private fun Intent.usbDevice(): UsbDevice? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(UsbManager.EXTRA_DEVICE)
        }

    private fun checkUsbDevice(requestPermission: Boolean = true) {
        val target = usbManager.deviceList.values.find(UsbDacManager::isSupportedDevice)
        if (target == null) {
            permissionDeviceName = null
            viewModel.onDeviceDisconnected()
            return
        }
        if (usbManager.hasPermission(target)) {
            permissionDeviceName = null
            viewModel.connectToDevice(target)
        } else if (requestPermission && permissionDeviceName != target.deviceName) {
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            val permissionIntent = Intent(ACTION_USB_PERMISSION).setPackage(packageName)
            val pendingIntent = PendingIntent.getBroadcast(this, 0, permissionIntent, flags)
            try {
                permissionDeviceName = target.deviceName
                usbManager.requestPermission(target, pendingIntent)
            } catch (e: Exception) {
                permissionDeviceName = null
                viewModel.showError("Cannot request USB permission: ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        unregisterReceiver(usbReceiver)
        super.onDestroy()
    }
}
