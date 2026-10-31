package com.rapli.magivant.ui

import android.content.Context
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.LaunchedEffect
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
import com.rapli.magivant.ui.components.TonalCard
import com.rapli.magivant.ui.theme.AppSettings
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
    "Minimum phase, sharp stopband attenuation, no pre-ringing",
    "Linear phase, sharp stopband attenuation, symmetric impulse",
    "Minimum phase, gradual stopband roll-off, low group delay",
    "Linear phase, gradual stopband roll-off, symmetric impulse",
    "Emulates non-oversampling response with minimal digital filtering"
)

private val ledModeTitles = listOf(
    "Follow Sample Rate",
    "Always Off",
    "Always Red"
)

private val ledModeDescriptions = listOf(
    "Indicator color follows active PCM or DSD sample rate",
    "Turns off the indicator LED",
    "Keeps the indicator LED solid red"
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
    val cs = MaterialTheme.colorScheme

    var currentTab by rememberSaveable { mutableStateOf(MagivantTab.HOME) }
    var isScrollingDown by remember { mutableStateOf(false) }
    var showNewPresetDialog by remember { mutableStateOf(false) }
    var presetToExport by remember { mutableStateOf<DacPreset?>(null) }

    LaunchedEffect(currentTab) {
        isScrollingDown = false
    }

    BackHandler(enabled = currentTab != MagivantTab.HOME) {
        currentTab = MagivantTab.HOME
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            var scrollAccumulator = 0f

            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (available.y > 0f) {
                    if (scrollAccumulator < 0f) {
                        scrollAccumulator = 0f
                    }
                    scrollAccumulator = (scrollAccumulator + available.y).coerceIn(0f, 200f)
                    if (scrollAccumulator > 20f) {
                        isScrollingDown = false
                        scrollAccumulator = 0f
                    }
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (consumed.y < 0f) {
                    if (scrollAccumulator > 0f) {
                        scrollAccumulator = 0f
                    }
                    scrollAccumulator = (scrollAccumulator + consumed.y).coerceIn(-200f, 0f)
                    if (scrollAccumulator < -80f) {
                        isScrollingDown = true
                        scrollAccumulator = 0f
                    }
                } else if (consumed.y > 0f || available.y > 0f) {
                    isScrollingDown = false
                    scrollAccumulator = 0f
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
            message = state.syncMessage.ifEmpty { "Syncing..." }
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
                                MagivantTab.HOME -> "Magivant DAC"
                                MagivantTab.PRESETS -> "Presets"
                                MagivantTab.SETTINGS -> "Settings"
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
                                "USB Disconnected (2FC6:F13B)"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (state.isConnected) {
                                cs.primary
                            } else {
                                cs.onSurfaceVariant
                            }
                        )
                    }
                },
                actions = {
                    if (state.isConnected) {
                        IconButton(onClick = { viewModel.manualResync() }) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Refresh Status"
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
                            Text(
                                text = "Scan USB",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (!appSettings.isFloatingNavbar) {
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) + expandVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) + shrinkVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                ) {
                    MagivantBottomBar(
                        currentTab = currentTab,
                        onTabSelected = { currentTab = it },
                        isFloating = false,
                        isFloatingGestureEnabled = appSettings.isFloatingNavbarGesture
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
        )
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
        ) {
            val bottomPadding = if (appSettings.isFloatingNavbar) 96.dp else 16.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
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
                        (fadeIn(animationSpec = tween(220, easing = LinearOutSlowInEasing)) +
                            scaleIn(
                                initialScale = 0.92f,
                                animationSpec = spring(dampingRatio = 0.8f, stiffness = 350f)
                            )) togetherWith
                            (fadeOut(animationSpec = tween(200)) +
                                scaleOut(
                                    targetScale = 0.96f,
                                    animationSpec = tween(200)
                                ))
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

            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                AnimatedVisibility(
                    visible = appSettings.isFloatingNavbar && !isScrollingDown,
                    enter = slideInVertically(
                        initialOffsetY = { it * 2 },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + fadeIn(tween(400)),
                    exit = slideOutVertically(
                        targetOffsetY = { it * 2 },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + fadeOut(tween(400))
                ) {
                    MagivantBottomBar(
                        currentTab = currentTab,
                        onTabSelected = { currentTab = it },
                        isFloating = true,
                        isFloatingGestureEnabled = appSettings.isFloatingNavbarGesture
                    )
                }
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
    TonalCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.errorContainer
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
            title = "Hardware Settings",
            content = listOf(
                {
                    ExpressiveSwitchItem(
                        icon = Icons.Filled.Bolt,
                        title = "High Gain (2.0 Vrms)",
                        summary = if (uiState.isHighGain) {
                            "2.0 Vrms (0 dB) output level"
                        } else {
                            "1.0 Vrms (-6 dB) output level"
                        },
                        checked = uiState.isHighGain,
                        enabled = uiState.isConnected,
                        onCheckedChange = onGainChanged
                    )
                },
                {
                    ExpressiveDropdownItem(
                        icon = Icons.Filled.GraphicEq,
                        title = "Digital Filter",
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
                        title = "Indicator LED",
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
                        title = "Auto Do Not Disturb",
                        summary = "Mute system notifications while the DAC is connected",
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
    val cs = MaterialTheme.colorScheme
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

    // Always use secondaryContainer for the primary status card so Light Mode retains rich accent color.
    val statusContainerColor = cs.secondaryContainer
    val statusContentColor = cs.onSecondaryContainer
    val telemetryContainerColor = cs.surfaceColorAtElevation(2.dp)

    if (appSettings.isClassicBanner) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TonalCard(
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
                TonalCard(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(20.dp),
                    containerColor = statusContainerColor
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                if (!uiState.isConnected) onScanUsb()
                            }
                    ) {
                        Icon(
                            imageVector = if (uiState.isConnected) Icons.Filled.GraphicEq else Icons.Filled.Usb,
                            contentDescription = null,
                            tint = statusContentColor.copy(alpha = 0.12f),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(64.dp)
                                .offset(x = 8.dp, y = 8.dp)
                        )
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
                                color = statusContentColor.copy(alpha = 0.78f)
                            )
                        }
                    }
                }

                TonalCard(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(20.dp),
                    containerColor = telemetryContainerColor
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { onOpenPresetsTab() }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = cs.primary.copy(alpha = 0.10f),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(64.dp)
                                .offset(x = 8.dp, y = 8.dp)
                        )
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = Cs43198Spec.formatDbFs(uiState.rawVolumeReg),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = cs.onSurface
                            )
                            Text(
                                text = "Output: ${Cs43198Spec.formatVrms(uiState.rawVolumeReg, uiState.isHighGain)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (uiState.activePresetName.isNotEmpty()) {
                                    "Profile: ${uiState.activePresetName}"
                                } else {
                                    if (uiState.isHighGain) "High Gain (2Vrms)" else "Low Gain (1Vrms)"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = cs.outline,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
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
            TonalCard(
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
                TonalCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    containerColor = statusContainerColor
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                if (!uiState.isConnected) onScanUsb()
                            }
                    ) {
                        Icon(
                            imageVector = if (uiState.isConnected) Icons.Filled.GraphicEq else Icons.Filled.Usb,
                            contentDescription = null,
                            tint = statusContentColor.copy(alpha = 0.12f),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(54.dp)
                                .offset(x = 6.dp, y = 6.dp)
                        )
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
                                    text = if (uiState.isConnected) "Connected" else "Disconnected",
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
                                color = statusContentColor.copy(alpha = 0.78f),
                                maxLines = 1
                            )
                        }
                    }
                }

                TonalCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    containerColor = telemetryContainerColor
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { onOpenPresetsTab() }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = cs.primary.copy(alpha = 0.10f),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(54.dp)
                                .offset(x = 6.dp, y = 6.dp)
                        )
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
                                color = cs.onSurface,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = Cs43198Spec.formatVrms(uiState.rawVolumeReg, uiState.isHighGain),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = cs.primary,
                                maxLines = 1
                            )
                            Text(
                                text = if (uiState.isHighGain) "High Gain (2Vrms)" else "Low Gain (1Vrms)",
                                style = MaterialTheme.typography.labelSmall,
                                color = cs.outline,
                                maxLines = 1
                            )
                        }
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
    val cs = MaterialTheme.colorScheme
    TonalCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
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
                tint = cs.primary,
                modifier = Modifier.size(28.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "DAC Disconnected",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onSurface
                )
                Text(
                    text = "Connect the Magivant USB DAC and tap Scan USB.",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.outline
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
    val cs = MaterialTheme.colorScheme
    val dbText = Cs43198Spec.formatDbFs(uiState.rawVolumeReg)
    val vrmsText = Cs43198Spec.formatVrms(uiState.rawVolumeReg, uiState.isHighGain)
    val regHex = String.format(Locale.US, "0x%02X", uiState.rawVolumeReg and 0xFF)

    ExpressiveList(
        title = "Volume & Channel Balance",
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
                                contentDescription = "Volume",
                                tint = cs.primary
                            )
                            Column {
                                Text(
                                    text = "Volume (${uiState.volumeIndex} / ${uiState.maxVolumeIndex})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = cs.onSurface
                                )
                                Text(
                                    text = "$dbText • $vrmsText • Reg $regHex",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = cs.onSurfaceVariant
                                )
                            }
                        }

                        TextButton(onClick = onQuickSavePreset) {
                            Text("Save Preset")
                        }
                    }

                    Slider(
                        value = uiState.volumeIndex.toFloat(),
                        onValueChange = { onVolumeChanged(it.roundToInt()) },
                        valueRange = 0f..uiState.maxVolumeIndex.toFloat(),
                        steps = (uiState.maxVolumeIndex - 1).coerceAtLeast(0),
                        enabled = uiState.isConnected
                    )

                    val stepButtonEnabled = uiState.isConnected && uiState.rawVolumeReg < 0xFF
                    val boostButtonEnabled = uiState.isConnected && uiState.rawVolumeReg > 0x00
                    val muteButtonEnabled = uiState.isConnected && uiState.rawVolumeReg != 0xFF
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onAdjustHalfDb(-1) },
                            enabled = stepButtonEnabled,
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
                            enabled = muteButtonEnabled,
                            modifier = Modifier.weight(0.9f)
                        ) {
                            Text("Mute")
                        }

                        OutlinedButton(
                            onClick = { onAdjustHalfDb(1) },
                            enabled = boostButtonEnabled,
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
                                fontWeight = FontWeight.SemiBold,
                                color = cs.onSurface
                            )
                            Text(
                                text = Cs43198Spec.formatBalanceDb(uiState.balanceBaseValue),
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurfaceVariant
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

                    val leftEnabled = uiState.isConnected && uiState.balanceBaseValue > -10
                    val centerEnabled = uiState.isConnected && uiState.balanceBaseValue != 0
                    val rightEnabled = uiState.isConnected && uiState.balanceBaseValue < 10
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onBalanceChanged((uiState.balanceBaseValue - 1).coerceAtLeast(-10)) },
                            enabled = leftEnabled,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Left Bias")
                        }
                        FilledTonalButton(
                            onClick = { onBalanceChanged(0) },
                            enabled = centerEnabled,
                            modifier = Modifier.weight(0.9f)
                        ) {
                            Text("0.0 dB")
                        }
                        OutlinedButton(
                            onClick = { onBalanceChanged((uiState.balanceBaseValue + 1).coerceAtMost(10)) },
                            enabled = rightEnabled,
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
