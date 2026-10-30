package com.rapli.magivant.ui

import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.UsbOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.rapli.magivant.R
import com.rapli.magivant.ui.components.ExpressiveDropdownItem
import com.rapli.magivant.ui.components.ExpressiveList
import com.rapli.magivant.ui.components.ExpressiveLoadingDialog
import com.rapli.magivant.ui.components.ExpressiveSwitchItem
import com.rapli.magivant.ui.components.MagivantBottomBar
import com.rapli.magivant.ui.components.MagivantPresetScreen
import com.rapli.magivant.ui.components.MagivantTab
import com.rapli.magivant.ui.components.NewPresetInputDialog
import com.rapli.magivant.ui.theme.AppSettings
import com.rapli.magivant.ui.theme.ColorMode
import com.rapli.magivant.usb.Cs43198Spec
import java.util.Locale
import kotlin.math.roundToInt

private val cs43198FilterTitles = listOf(
    "Fast Roll-Off, Low Latency",
    "Fast Roll-Off, Phase Compensated",
    "Slow Roll-Off, Low Latency",
    "Slow Roll-Off, Phase Compensated",
    "Non-Oversampling (NOS)"
)

private val cs43198FilterDescriptions = listOf(
    "Minimum phase filter with zero pre-ringing and tight transient attack",
    "Linear phase filter preserving exact time alignment and studio imaging",
    "Minimum phase with gentle ultrasonic attenuation and natural decay",
    "Linear phase with smooth high-frequency roll-off and wide soundstage",
    "Bypasses digital interpolation filter for unsmoothed R2R-style transients"
)

private val ledModeTitles = listOf(
    "Follow Sample Rate & Playback",
    "Always Off (Stealth Mode)",
    "Always Red"
)

private val ledModeDescriptions = listOf(
    "Changes chassis LED color dynamically based on active PCM or DSD rate",
    "Turns off chassis LED completely for dark-room listening",
    "Keeps the chassis indicator illuminated in solid red"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MagivantScreen(
    viewModel: MagivantViewModel,
    appSettings: AppSettings,
    onUpdateAppSettings: (AppSettings) -> Unit,
    onCheckDndPermission: (Boolean) -> Unit,
    onScanUsb: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    val state by viewModel.uiState.collectAsState()
    val presetList by viewModel.presets.collectAsState()

    var currentTab by rememberSaveable { mutableStateOf(MagivantTab.HOME) }
    var showBottomBar by remember { mutableStateOf(true) }
    var showNewPresetDialog by remember { mutableStateOf(false) }
    var presetToExport by remember { mutableStateOf<DacPreset?>(null) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta < -5f) {
                    showBottomBar = false
                } else if (delta > 5f) {
                    showBottomBar = true
                }
                return Offset.Zero
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let { viewModel.importPresetFromUri(context, it) }
        }
    )

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
        onResult = { uri ->
            uri?.let {
                viewModel.exportPresetToUri(context, it, presetToExport)
                presetToExport = null
            }
        }
    )

    if (state.isSyncing) {
        ExpressiveLoadingDialog(
            message = state.syncMessage.ifEmpty { "Syncing CS43198 registers..." }
        )
    }

    if (showNewPresetDialog) {
        NewPresetInputDialog(
            onDismiss = { showNewPresetDialog = false },
            onSave = { name ->
                viewModel.saveCurrentStateAsPreset(name)
                showNewPresetDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = when (currentTab) {
                                MagivantTab.HOME -> "Leteciel Magivant DAC"
                                MagivantTab.PRESETS -> "CS43198 Profiles"
                                MagivantTab.SETTINGS -> "Theme & Hardware Setup"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (state.isConnected) {
                                "CS43198 • FW v${state.firmwareVersion.ifEmpty { "3.0.0" }} • ${state.sampleRateText}"
                            } else {
                                "USB DAC Disconnected (VID 2FC6 : PID F13B)"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (state.isConnected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                },
                actions = {
                    if (state.isConnected) {
                        IconButton(onClick = { viewModel.manualResync() }) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Refresh CS43198 Telemetry"
                            )
                        }
                    } else {
                        FilledTonalButton(
                            onClick = onScanUsb,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Usb,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scan USB", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
        )
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .nestedScroll(nestedScrollConnection)
        ) {
            val bottomPadding = if (appSettings.isFloatingNavbar) 96.dp else 80.dp

            Column(modifier = Modifier.fillMaxSize()) {
                AnimatedVisibility(
                    visible = state.errorMessage != null,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    state.errorMessage?.let { errMsg ->
                        ErrorStatusBanner(
                            message = errMsg,
                            isConnected = state.isConnected,
                            onRetry = {
                                if (state.isConnected) viewModel.manualResync() else onScanUsb()
                            },
                            onDismiss = { viewModel.clearError() }
                        )
                    }
                }

                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                    },
                    label = "magivantTabContent",
                    modifier = Modifier.weight(1f)
                ) { tab ->
                    when (tab) {
                        MagivantTab.HOME -> {
                            MagivantHomeScreen(
                                uiState = state,
                                appSettings = appSettings,
                                bottomInnerPadding = bottomPadding,
                                onScanUsb = onScanUsb,
                                onVolumeChanged = { viewModel.setVolume(it) },
                                onAdjustHalfDb = { viewModel.adjustVolumeByHalfDb(it) },
                                onBalanceChanged = { viewModel.setBalance(it) },
                                onGainChanged = { viewModel.setGain(it) },
                                onFilterChanged = { viewModel.setDigitalFilter(it) },
                                onLedChanged = { viewModel.setLed(it) },
                                onDndChanged = { onCheckDndPermission(it) },
                                onOpenPresetsTab = { currentTab = MagivantTab.PRESETS },
                                onQuickSavePreset = { showNewPresetDialog = true }
                            )
                        }
                        MagivantTab.PRESETS -> {
                            MagivantPresetScreen(
                                uiState = state,
                                presetList = presetList,
                                volumeTable = viewModel.appVolumeIntList,
                                bottomInnerPadding = bottomPadding,
                                onPresetSelected = { preset ->
                                    viewModel.applyPreset(preset)
                                },
                                onNewClicked = { showNewPresetDialog = true },
                                onImportClicked = {
                                    importLauncher.launch(arrayOf("application/json", "*/*"))
                                },
                                onExportClicked = { preset ->
                                    presetToExport = preset
                                    val defaultFileName = preset?.let { "${it.name}.json" } ?: "Magivant_Custom.json"
                                    exportLauncher.launch(defaultFileName)
                                },
                                onDeleteClicked = { toDelete ->
                                    viewModel.deletePresets(toDelete)
                                },
                                onSetStartupPreset = { name ->
                                    viewModel.setStartupPresetName(name)
                                }
                            )
                        }
                        MagivantTab.SETTINGS -> {
                            MagivantSettingsScreen(
                                appSettings = appSettings,
                                uiState = state,
                                presets = presetList,
                                volumeTable = viewModel.appVolumeIntList,
                                bottomInnerPadding = bottomPadding,
                                onUpdateThemeMode = { mode ->
                                    prefs.edit().putInt("color_mode", mode.value).apply()
                                    onUpdateAppSettings(appSettings.copy(colorMode = mode))
                                },
                                onUpdateKeyColor = { colorInt ->
                                    prefs.edit().putInt("key_color", colorInt).apply()
                                    onUpdateAppSettings(appSettings.copy(keyColor = colorInt))
                                },
                                onUpdateDpiScale = { scale ->
                                    prefs.edit().putFloat("app_dpi", scale).apply()
                                    onUpdateAppSettings(appSettings.copy(dpiScale = scale))
                                },
                                onUpdateFloatingNavbar = { floating ->
                                    prefs.edit().putBoolean("floating_navbar", floating).apply()
                                    onUpdateAppSettings(appSettings.copy(isFloatingNavbar = floating))
                                },
                                onUpdateFloatingGesture = { gesture ->
                                    prefs.edit().putBoolean("floating_navbar_gesture", gesture).apply()
                                    onUpdateAppSettings(appSettings.copy(isFloatingNavbarGesture = gesture))
                                },
                                onUpdateClassicBanner = { classic ->
                                    prefs.edit().putBoolean("classic_banner", classic).apply()
                                    onUpdateAppSettings(appSettings.copy(isClassicBanner = classic))
                                },
                                onUpdateCustomHeaderUri = { uri ->
                                    onUpdateAppSettings(appSettings.copy(customHeaderUri = uri))
                                },
                                onUpdateMaxVolumeLimit = { maxVol ->
                                    viewModel.setMaxVolumeLimit(maxVol)
                                },
                                onUpdateStartupPreset = { presetName ->
                                    viewModel.setStartupPresetName(presetName)
                                }
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = showBottomBar,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                MagivantBottomBar(
                    currentTab = currentTab,
                    onTabSelected = { currentTab = it },
                    isFloating = appSettings.isFloatingNavbar,
                    isFloatingGestureEnabled = appSettings.isFloatingNavbarGesture
                )
            }
        }
    }
}

@Composable
private fun ErrorStatusBanner(
    message: String,
    isConnected: Boolean,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.ErrorOutline,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onRetry) {
                Text(
                    text = if (isConnected) "Retry" else "Scan",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Bold
                )
            }
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Dismiss",
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun MagivantHomeScreen(
    uiState: DacUiState,
    appSettings: AppSettings,
    bottomInnerPadding: Dp,
    onScanUsb: () -> Unit,
    onVolumeChanged: (Int) -> Unit,
    onAdjustHalfDb: (Int) -> Unit,
    onBalanceChanged: (Int) -> Unit,
    onGainChanged: (Boolean) -> Unit,
    onFilterChanged: (Int) -> Unit,
    onLedChanged: (Int) -> Unit,
    onDndChanged: (Boolean) -> Unit,
    onOpenPresetsTab: () -> Unit,
    onQuickSavePreset: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = bottomInnerPadding + 24.dp
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        MagivantStatusBanner(
            uiState = uiState,
            appSettings = appSettings,
            onScanUsb = onScanUsb,
            onOpenPresetsTab = onOpenPresetsTab
        )

        if (!uiState.isConnected) {
            DisconnectedQuickCard(onScanUsb = onScanUsb)
        }

        VolumeAndBalanceCard(
            uiState = uiState,
            onVolumeChanged = onVolumeChanged,
            onAdjustHalfDb = onAdjustHalfDb,
            onBalanceChanged = onBalanceChanged,
            onQuickSavePreset = onQuickSavePreset
        )

        ExpressiveList(
            title = "CS43198 Hardware Configuration",
            content = listOf(
                {
                    ExpressiveSwitchItem(
                        icon = Icons.Filled.Bolt,
                        title = "High Gain Output Mode (2.0 Vrms)",
                        summary = if (uiState.isHighGain) {
                            "Active: 2.0 Vrms (0 dB analog rail) for high-impedance headphones"
                        } else {
                            "Active: 1.0 Vrms (-6 dB analog rail) for sensitive IEMs"
                        },
                        checked = uiState.isHighGain,
                        enabled = uiState.isConnected,
                        onCheckedChange = onGainChanged
                    )
                },
                {
                    ExpressiveDropdownItem(
                        icon = Icons.Filled.GraphicEq,
                        title = "Digital Interpolation Filter",
                        summary = cs43198FilterDescriptions.getOrElse(uiState.digitalFilterPos) { "" },
                        items = cs43198FilterTitles,
                        itemDescriptions = cs43198FilterDescriptions,
                        enabled = uiState.isConnected,
                        selectedIndex = uiState.digitalFilterPos,
                        onItemSelected = onFilterChanged
                    )
                },
                {
                    ExpressiveDropdownItem(
                        icon = Icons.Filled.LightMode,
                        title = "Chassis Status LED",
                        summary = ledModeDescriptions.getOrElse(uiState.ledPos) { "" },
                        items = ledModeTitles,
                        itemDescriptions = ledModeDescriptions,
                        enabled = uiState.isConnected,
                        selectedIndex = uiState.ledPos,
                        onItemSelected = onLedChanged
                    )
                },
                {
                    ExpressiveSwitchItem(
                        icon = Icons.Filled.DoNotDisturbOn,
                        title = "Auto Do Not Disturb on USB Connect",
                        summary = "Silence system notifications automatically while listening through Magivant DAC",
                        checked = uiState.isDndEnabled,
                        enabled = true,
                        onCheckedChange = onDndChanged
                    )
                }
            )
        )
    }
}

@Composable
private fun MagivantStatusBanner(
    uiState: DacUiState,
    appSettings: AppSettings,
    onScanUsb: () -> Unit,
    onOpenPresetsTab: () -> Unit
) {
    val context = LocalContext.current
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    val statusContainerColor = if (uiState.isConnected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
    }
    val statusContentColor = if (uiState.isConnected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    if (appSettings.isClassicBanner) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(appSettings.customHeaderUri ?: R.drawable.magivant_banner)
                        .crossfade(true)
                        .build(),
                    imageLoader = imageLoader,
                    contentDescription = "Leteciel Magivant Banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable {
                            if (!uiState.isConnected) onScanUsb()
                        },
                    shape = RoundedCornerShape(20.dp),
                    color = statusContainerColor
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (uiState.isConnected) Icons.Filled.CheckCircle else Icons.Filled.UsbOff,
                                contentDescription = null,
                                tint = statusContentColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (uiState.isConnected) "Connected" else "Disconnected",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = statusContentColor
                            )
                        }
                        Text(
                            text = if (uiState.isConnected) uiState.sampleRateText else "Tap to scan USB",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = statusContentColor.copy(alpha = 0.9f)
                        )
                        Text(
                            text = if (uiState.isConnected) {
                                "FW v${uiState.firmwareVersion.ifEmpty { "3.0.0" }} • ${uiState.bridgeModeText}"
                            } else {
                                "Comtrue 2FC6:F13B"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = statusContentColor.copy(alpha = 0.75f)
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onOpenPresetsTab() },
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = Cs43198Spec.formatDbFs(uiState.rawVolumeReg),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Output: ${Cs43198Spec.formatVrms(uiState.rawVolumeReg, uiState.isHighGain)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (uiState.activePresetName.isNotEmpty()) {
                                "Profile: ${uiState.activePresetName}"
                            } else {
                                if (uiState.isHighGain) "High Gain (2Vrms)" else "Low Gain (1Vrms)"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(0.58f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(20.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(appSettings.customHeaderUri ?: R.drawable.magivant_banner)
                        .crossfade(true)
                        .build(),
                    imageLoader = imageLoader,
                    contentDescription = "Leteciel Magivant Banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(
                modifier = Modifier
                    .weight(0.42f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clickable {
                            if (!uiState.isConnected) onScanUsb()
                        },
                    shape = RoundedCornerShape(20.dp),
                    color = statusContainerColor
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (uiState.isConnected) Icons.Filled.CheckCircle else Icons.Filled.UsbOff,
                                contentDescription = null,
                                tint = statusContentColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (uiState.isConnected) "CS43198 Live" else "Disconnected",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = statusContentColor,
                                maxLines = 1
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (uiState.isConnected) uiState.sampleRateText else "Tap to scan USB",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = statusContentColor.copy(alpha = 0.9f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (uiState.isConnected) {
                                "FW v${uiState.firmwareVersion.ifEmpty { "3.0.0" }}"
                            } else {
                                "VID 2FC6 : PID F13B"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = statusContentColor.copy(alpha = 0.75f),
                            maxLines = 1
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clickable { onOpenPresetsTab() },
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = Cs43198Spec.formatDbFs(uiState.rawVolumeReg),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = Cs43198Spec.formatVrms(uiState.rawVolumeReg, uiState.isHighGain),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                        Text(
                            text = if (uiState.isHighGain) "High Gain (2Vrms)" else "Low Gain (1Vrms)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DisconnectedQuickCard(
    onScanUsb: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.UsbOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Leteciel Magivant Not Connected",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Plug in your Magivant USB DAC and tap Scan USB to unlock CS43198 register controls.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(onClick = onScanUsb) {
                Text("Scan USB")
            }
        }
    }
}

@Composable
private fun VolumeAndBalanceCard(
    uiState: DacUiState,
    onVolumeChanged: (Int) -> Unit,
    onAdjustHalfDb: (Int) -> Unit,
    onBalanceChanged: (Int) -> Unit,
    onQuickSavePreset: () -> Unit
) {
    val dbText = Cs43198Spec.formatDbFs(uiState.rawVolumeReg)
    val vrmsText = Cs43198Spec.formatVrms(uiState.rawVolumeReg, uiState.isHighGain)
    val regHex = String.format(Locale.US, "0x%02X", uiState.rawVolumeReg and 0xFF)

    ExpressiveList(
        title = "Precision Attenuation & Channel Balance",
        content = listOf(
            {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (uiState.rawVolumeReg == 0xFF) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Master Volume",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = "Master Volume (${uiState.volumeIndex} / ${uiState.maxVolumeIndex})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "$dbText • $vrmsText • Reg $regHex",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        TextButton(onClick = onQuickSavePreset) {
                            Text("Save Profile")
                        }
                    }

                    Slider(
                        value = uiState.volumeIndex.toFloat(),
                        onValueChange = { onVolumeChanged(it.roundToInt()) },
                        valueRange = 0f..uiState.maxVolumeIndex.toFloat(),
                        steps = (uiState.maxVolumeIndex - 1).coerceAtLeast(0),
                        enabled = uiState.isConnected
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onAdjustHalfDb(-1) },
                            enabled = uiState.isConnected && uiState.rawVolumeReg < 0xFF,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Remove,
                                contentDescription = "Attenuate 0.5 dB",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("-0.5 dB")
                        }

                        FilledTonalButton(
                            onClick = { onVolumeChanged(0) },
                            enabled = uiState.isConnected && uiState.rawVolumeReg != 0xFF,
                            modifier = Modifier.weight(0.9f)
                        ) {
                            Text("Mute")
                        }

                        OutlinedButton(
                            onClick = { onAdjustHalfDb(1) },
                            enabled = uiState.isConnected && uiState.rawVolumeReg > 0x00,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Boost 0.5 dB",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+0.5 dB")
                        }
                    }
                }
            },
            {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "L / R Channel Trim",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = Cs43198Spec.formatBalanceDb(uiState.balanceBaseValue),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (uiState.balanceBaseValue != 0) {
                            TextButton(
                                onClick = { onBalanceChanged(0) },
                                enabled = uiState.isConnected
                            ) {
                                Text("Center (0 dB)")
                            }
                        }
                    }

                    Slider(
                        value = uiState.balanceBaseValue.toFloat(),
                        onValueChange = { onBalanceChanged(it.roundToInt()) },
                        valueRange = -10f..10f,
                        steps = 19,
                        enabled = uiState.isConnected
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onBalanceChanged((uiState.balanceBaseValue - 1).coerceAtLeast(-10)) },
                            enabled = uiState.isConnected && uiState.balanceBaseValue > -10,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Left Bias")
                        }
                        FilledTonalButton(
                            onClick = { onBalanceChanged(0) },
                            enabled = uiState.isConnected && uiState.balanceBaseValue != 0,
                            modifier = Modifier.weight(0.9f)
                        ) {
                            Text("0.0 dB")
                        }
                        OutlinedButton(
                            onClick = { onBalanceChanged((uiState.balanceBaseValue + 1).coerceAtMost(10)) },
                            enabled = uiState.isConnected && uiState.balanceBaseValue < 10,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Right Bias")
                        }
                    }
                }
            }
        )
    )
}
