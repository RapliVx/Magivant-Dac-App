package com.rapli.magivant.ui

import android.app.Activity
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness1
import androidx.compose.material.icons.filled.Brightness3
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.materialkolor.rememberDynamicColorScheme
import com.rapli.magivant.R
import com.rapli.magivant.ui.components.ExpressiveDropdownItem
import com.rapli.magivant.ui.components.ExpressiveList
import com.rapli.magivant.ui.components.MagivantSlider
import com.rapli.magivant.ui.components.MagivantTab
import com.rapli.magivant.ui.components.ExpressiveListItem
import com.rapli.magivant.ui.components.ExpressiveSwitchItem
import com.rapli.magivant.ui.theme.AppSettings
import com.rapli.magivant.ui.theme.ColorMode
import com.rapli.magivant.ui.theme.keyColorOptions
import com.rapli.magivant.ui.util.clearCustomHeaderImage
import com.rapli.magivant.ui.util.saveHeaderImage
import com.rapli.magivant.usb.Cs43198Spec
import com.yalantis.ucrop.UCrop
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MagivantSettingsScreen(
    appSettings: AppSettings,
    uiState: DacUiState,
    presets: List<DacPreset>,
    volumeTable: List<Int>,
    bottomInnerPadding: Dp,
    onUpdateThemeMode: (ColorMode) -> Unit,
    onUpdateKeyColor: (Int) -> Unit,
    onUpdateDpiScale: (Float) -> Unit,
    onUpdateFloatingNavbar: (Boolean) -> Unit,
    onUpdateFloatingGesture: (Boolean) -> Unit,
    onUpdateClassicBanner: (Boolean) -> Unit,
    onUpdateCustomHeaderUri: (String?) -> Unit,
    onUpdateMaxVolumeLimit: (Int) -> Unit,
    onUpdateStartupPreset: (String) -> Unit,
    onReportError: (String) -> Unit
) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val systemDark = isSystemInDarkTheme()
    val isDark = appSettings.colorMode.getDarkThemeValue(systemDark)
    val isAmoled = appSettings.colorMode == ColorMode.DARKAMOLED

    val scope = rememberCoroutineScope()
    var isSavingHeader by remember { mutableStateOf(false) }
    var pendingCropPath by rememberSaveable { mutableStateOf<String?>(null) }

    val cropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val cropPath = pendingCropPath
        pendingCropPath = null
        scope.launch {
            isSavingHeader = true
            try {
                if (result.resultCode == Activity.RESULT_OK) {
                    val uri = requireNotNull(result.data?.let { UCrop.getOutput(it) }) { "The cropped image is unavailable." }
                    onUpdateCustomHeaderUri(context.saveHeaderImage(uri.toString()))
                } else if (result.resultCode == UCrop.RESULT_ERROR) {
                    onReportError(result.data?.let { UCrop.getError(it) }?.message ?: "Cannot crop the selected image.")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onReportError("Cannot save header image: ${e.message}")
            } finally {
                withContext(kotlinx.coroutines.NonCancellable + Dispatchers.IO) { cropPath?.let { File(it).delete() } }
                isSavingHeader = false
            }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) scope.launch {
            isSavingHeader = true
            try {
                val mimeType = withContext(Dispatchers.IO) { context.contentResolver.getType(uri) }
                if (mimeType?.contains("gif", ignoreCase = true) == true) {
                    onUpdateCustomHeaderUri(context.saveHeaderImage(uri.toString()))
                } else {
                    val destination = withContext(Dispatchers.IO) {
                        File.createTempFile("cropped_header_", ".jpg", context.cacheDir)
                    }
                    pendingCropPath = destination.absolutePath
                    val options = UCrop.Options().apply {
                        setToolbarColor(cs.surface.toArgb())
                        setToolbarWidgetColor(cs.onSurface.toArgb())
                        setActiveControlsWidgetColor(cs.primary.toArgb())
                        setRootViewBackgroundColor(cs.surface.toArgb())
                        setCropFrameColor(cs.primary.toArgb())
                        setFreeStyleCropEnabled(true)
                    }
                    val uCrop = UCrop.of(uri, Uri.fromFile(destination))
                        .withAspectRatio(16f, 9f)
                        .withMaxResultSize(1600, 900)
                        .withOptions(options)
                    cropLauncher.launch(uCrop.getIntent(context))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onReportError("Cannot open header image: ${e.message}")
                withContext(Dispatchers.IO) { pendingCropPath?.let { File(it).delete() } }
                pendingCropPath = null
            } finally {
                isSavingHeader = false
            }
        }
    }

    val themeOptions = listOf(
        ColorMode.SYSTEM to "System",
        ColorMode.LIGHT to "Light",
        ColorMode.DARK to "Dark",
        ColorMode.DARKAMOLED to "AMOLED"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = bottomInnerPadding + 24.dp
            )
    ) {
        ThemePreviewCard(
            keyColor = appSettings.keyColor,
            isDark = isDark,
            isAmoled = isAmoled,
            enableFloatingNav = appSettings.isFloatingNavbar,
            useClassicLayout = appSettings.isClassicBanner
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Color Palette",
            style = MaterialTheme.typography.titleSmall,
            color = cs.primary,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ColorButton(
                    color = Color.Unspecified,
                    isSelected = appSettings.keyColor == 0,
                    isDark = isDark,
                    isAmoled = isAmoled,
                    onClick = { onUpdateKeyColor(0) }
                )
            }

            keyColorOptions.forEach { option ->
                val colorInt = option.color.toArgb()
                ColorButton(
                    color = option.color,
                    isSelected = appSettings.keyColor == colorInt,
                    isDark = isDark,
                    isAmoled = isAmoled,
                    onClick = { onUpdateKeyColor(colorInt) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ExpressiveList(
            title = "Theme Mode",
            content = listOf {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                ) {
                    themeOptions.forEachIndexed { index, (mode, label) ->
                        ToggleButton(
                            checked = appSettings.colorMode == mode,
                            onCheckedChange = {
                                if (it) onUpdateThemeMode(mode)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .semantics { role = Role.RadioButton },
                            shapes = when (index) {
                                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                themeOptions.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                            }
                        ) {
                            Icon(
                                imageVector = when (mode) {
                                    ColorMode.SYSTEM -> Icons.Filled.Brightness4
                                    ColorMode.LIGHT -> Icons.Filled.Brightness7
                                    ColorMode.DARK -> Icons.Filled.Brightness3
                                    ColorMode.DARKAMOLED -> Icons.Filled.Brightness1
                                },
                                contentDescription = label
                            )
                        }
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        ExpressiveList(
            title = "Layout & Navigation",
            content = buildList {
                add {
                    ExpressiveSwitchItem(
                        icon = Icons.Filled.Image,
                        title = "Custom Header Image",
                        summary = if (appSettings.customHeaderUri != null) {
                            "Custom image or GIF active"
                        } else {
                            "Default header image"
                        },
                        checked = appSettings.customHeaderUri != null,
                        enabled = !isSavingHeader,
                        onCheckedChange = { checked ->
                            if (checked) {
                                imagePickerLauncher.launch("image/*")
                            } else {
                                scope.launch {
                                    isSavingHeader = true
                                    try {
                                        context.clearCustomHeaderImage()
                                        onUpdateCustomHeaderUri(null)
                                    } catch (e: CancellationException) {
                                        throw e
                                    } catch (e: Exception) {
                                        onReportError("Cannot clear header image: ${e.message}")
                                    } finally {
                                        isSavingHeader = false
                                    }
                                }
                            }
                        }
                    )
                }

                if (appSettings.customHeaderUri != null) {
                    add {
                        ExpressiveListItem(
                            onClick = {
                                if (!isSavingHeader) imagePickerLauncher.launch("image/*")
                            },
                            headlineContent = { Text("Select Image or GIF", color = cs.onSurface) },
                            supportingContent = { Text("16:9 crop or animated GIF", color = cs.onSurfaceVariant) },
                            leadingContent = {
                                Icon(Icons.Filled.Image, contentDescription = "Select Banner", tint = cs.primary)
                            }
                        )
                    }
                }

                add {
                    ExpressiveSwitchItem(
                        icon = Icons.Filled.ViewAgenda,
                        title = "Classic Banner Layout",
                        summary = if (appSettings.isClassicBanner) {
                            "Full-width header above status cards"
                        } else {
                            "Split header beside status cards"
                        },
                        checked = appSettings.isClassicBanner,
                        onCheckedChange = onUpdateClassicBanner
                    )
                }

                add {
                    ExpressiveSwitchItem(
                        icon = Icons.Filled.ViewCarousel,
                        title = "Floating Navigation Bar",
                        summary = "Show compact bottom bar",
                        checked = appSettings.isFloatingNavbar,
                        onCheckedChange = onUpdateFloatingNavbar
                    )
                }

                if (appSettings.isFloatingNavbar) {
                    add {
                        ExpressiveSwitchItem(
                            icon = Icons.Filled.Swipe,
                            title = "Swipe to Switch Tabs",
                            summary = "Drag across the floating bar to switch tabs",
                            checked = appSettings.isFloatingNavbarGesture,
                            onCheckedChange = onUpdateFloatingGesture
                        )
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        val maxLimitReg = volumeTable.getOrElse(uiState.maxVolumeIndex) { 0x00 }
        val maxLimitDb = Cs43198Spec.formatDbFs(maxLimitReg)
        val startupChoices = listOf("Keep Hardware State") + presets.map { it.name }
        val currentStartupIdx = if (uiState.startupPresetName.isEmpty()) {
            0
        } else {
            (presets.indexOfFirst { it.name == uiState.startupPresetName } + 1).coerceAtLeast(0)
        }

        ExpressiveList(
            title = "Volume Limit & Startup",
            content = listOf(
                {
                    ExpressiveListItem(
                        headlineContent = { Text("Max Volume Limit", color = cs.onSurface) },
                        supportingContent = {
                            Column {
                                Text(
                                    text = "Limit: ${uiState.maxVolumeIndex}/60 ($maxLimitDb)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = cs.onSurfaceVariant
                                )
                                MagivantSlider(
                                    value = uiState.maxVolumeIndex.toFloat(),
                                    onValueChange = { onUpdateMaxVolumeLimit(it.roundToInt()) },
                                    steps = 49,
                                    valueRange = 10f..60f
                                )
                            }
                        },
                        leadingContent = {
                            Icon(Icons.Filled.Hearing, contentDescription = "Volume Limit", tint = cs.primary)
                        }
                    )
                },
                {
                    ExpressiveDropdownItem(
                        icon = Icons.Filled.PlayCircleOutline,
                        title = "Startup Preset",
                        summary = "Apply a preset when the DAC connects",
                        items = startupChoices,
                        selectedIndex = currentStartupIdx,
                        onItemSelected = { index ->
                            val selectedName = if (index == 0) "" else startupChoices.getOrElse(index) { "" }
                            onUpdateStartupPreset(selectedName)
                        }
                    )
                }
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        ExpressiveList(
            title = "Interface Scale",
            content = listOf {
                InterfaceScaleItem(
                    dpiScale = appSettings.dpiScale,
                    onUpdateDpiScale = onUpdateDpiScale
                )
            }
        )
    }
}

@Composable
internal fun InterfaceScaleItem(
    dpiScale: Float,
    onUpdateDpiScale: (Float) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    var tempDpiScale by remember(dpiScale) { mutableFloatStateOf(dpiScale) }

    ExpressiveListItem(
        headlineContent = { Text("UI Scale", color = cs.onSurface) },
        supportingContent = {
            Column {
                Text("${(tempDpiScale * 100).roundToInt()}%", color = cs.onSurfaceVariant)
                MagivantSlider(
                    value = tempDpiScale,
                    onValueChange = { tempDpiScale = it },
                    onValueChangeFinished = {
                        // Keep density and the slider's bounds stable until the gesture ends.
                        val scale = ((tempDpiScale * 20).roundToInt() / 20f).coerceIn(0.75f, 1.25f)
                        if (scale != dpiScale) onUpdateDpiScale(scale)
                    },
                    steps = 9,
                    valueRange = 0.75f..1.25f
                )
            }
        },
        leadingContent = {
            Icon(Icons.Filled.FormatSize, contentDescription = "UI Scale", tint = cs.primary)
        }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ColorButton(
    color: Color,
    isSelected: Boolean,
    isDark: Boolean,
    isAmoled: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val motion = MaterialTheme.motionScheme
    val colorScheme = if (color == Color.Unspecified) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (isDark) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        } else {
            MaterialTheme.colorScheme
        }
    } else {
        rememberDynamicColorScheme(
            seedColor = color,
            isDark = isDark,
            isAmoled = isAmoled
        )
    }

    val innerCornerRadius by animateDpAsState(
        targetValue = if (isSelected) 24.dp else 16.dp,
        animationSpec = motion.fastSpatialSpec(),
        label = "innerCornerRadius"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = colorScheme.surfaceContainer,
        modifier = Modifier.size(72.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(innerCornerRadius))
            ) {
                drawRect(
                    color = colorScheme.primaryContainer,
                    topLeft = Offset(0f, 0f),
                    size = Size(size.width, size.height / 2)
                )
                drawRect(
                    color = colorScheme.tertiaryContainer,
                    topLeft = Offset(0f, size.height / 2),
                    size = Size(size.width, size.height / 2)
                )
            }

            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.1f else 1.0f,
                animationSpec = motion.fastSpatialSpec(),
                label = "colorButtonScale"
            )
            Box(
                modifier = Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
                contentAlignment = Alignment.Center
            ) {
                AnimatedVisibility(
                    visible = isSelected,
                    enter = fadeIn(motion.fastEffectsSpec()) + scaleIn(motion.fastSpatialSpec(), initialScale = 0.8f),
                    exit = fadeOut(motion.fastEffectsSpec()) + scaleOut(motion.fastSpatialSpec(), targetScale = 0.8f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .border(2.dp, colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                AnimatedVisibility(
                    visible = !isSelected,
                    enter = fadeIn(motion.fastEffectsSpec()) + scaleIn(motion.fastSpatialSpec(), initialScale = 0.8f),
                    exit = fadeOut(motion.fastEffectsSpec()) + scaleOut(motion.fastSpatialSpec(), targetScale = 0.8f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(colorScheme.primary, CircleShape)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ThemePreviewCard(
    keyColor: Int,
    isDark: Boolean,
    isAmoled: Boolean,
    enableFloatingNav: Boolean,
    useClassicLayout: Boolean
) {
    val windowSize = LocalWindowInfo.current.containerSize
    val screenRatio = windowSize.width.coerceAtLeast(1).toFloat() / windowSize.height.coerceAtLeast(1)
    val colorScheme = when {
        keyColor == 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            rememberDynamicColorScheme(
                seedColor = colorResource(id = android.R.color.system_accent1_500),
                isDark = isDark,
                isAmoled = isAmoled
            )
        }
        keyColor != 0 -> {
            rememberDynamicColorScheme(
                seedColor = Color(keyColor),
                isDark = isDark,
                isAmoled = isAmoled
            )
        }
        else -> {
            if (isDark) darkColorScheme() else expressiveLightColorScheme()
        }
    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.48f)
                .aspectRatio(screenRatio),
            color = colorScheme.surface,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(2.dp, color = MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = 8.dp
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .height(56.dp)
                        .fillMaxWidth()
                        .background(colorScheme.surfaceContainer),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "Magivant DAC",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(colorScheme.surface),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (useClassicLayout) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Surface(
                                    color = colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth().height(72.dp)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.BottomStart) {
                                        Surface(color = colorScheme.primary, shape = RoundedCornerShape(4.dp), modifier = Modifier.width(36.dp).height(8.dp)) {}
                                    }
                                }
                                Surface(
                                    color = colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth().height(56.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize().padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(modifier = Modifier.size(24.dp).background(colorScheme.primaryContainer, CircleShape))
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Surface(color = colorScheme.onSurfaceVariant.copy(alpha = 0.8f), shape = RoundedCornerShape(2.dp), modifier = Modifier.width(48.dp).height(6.dp)) {}
                                            Surface(color = colorScheme.onSurfaceVariant.copy(alpha = 0.4f), shape = RoundedCornerShape(2.dp), modifier = Modifier.width(32.dp).height(4.dp)) {}
                                        }
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth().height(72.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    color = colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.weight(0.6f).fillMaxHeight()
                                ) {
                                    Box(modifier = Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.BottomStart) {
                                        Surface(color = colorScheme.primary, shape = RoundedCornerShape(4.dp), modifier = Modifier.width(36.dp).height(8.dp)) {}
                                    }
                                }
                                Column(
                                    modifier = Modifier.weight(0.4f).fillMaxHeight(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(color = colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f).fillMaxWidth()) {}
                                    Surface(color = colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f).fillMaxWidth()) {}
                                }
                            }

                            Surface(
                                color = colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth().height(56.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(modifier = Modifier.size(24.dp).background(colorScheme.primaryContainer, CircleShape))
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Surface(color = colorScheme.onSurfaceVariant.copy(alpha = 0.8f), shape = RoundedCornerShape(2.dp), modifier = Modifier.width(48.dp).height(6.dp)) {}
                                        Surface(color = colorScheme.onSurfaceVariant.copy(alpha = 0.4f), shape = RoundedCornerShape(2.dp), modifier = Modifier.width(32.dp).height(4.dp)) {}
                                    }
                                }
                            }
                        }
                    }
                }

                ThemePreviewNavigation(
                    colorScheme = colorScheme,
                    isFloating = enableFloatingNav
                )
            }
        }
    }
}

@Composable
internal fun ThemePreviewNavigation(
    colorScheme: ColorScheme,
    isFloating: Boolean
) {
    Surface(
        modifier = Modifier
            .padding(horizontal = if (isFloating) 12.dp else 0.dp)
            .padding(bottom = if (isFloating) 12.dp else 0.dp)
            .fillMaxWidth()
            .height(if (isFloating) 40.dp else 48.dp),
        shape = if (isFloating) RoundedCornerShape(50) else RoundedCornerShape(0.dp),
        color = colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (isFloating) 8.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MagivantTab.entries.forEach { destination ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    val isSelected = destination == MagivantTab.HOME
                    val iconSize = if (isFloating) 14.dp else 16.dp
                    if (isSelected) {
                        Surface(
                            modifier = Modifier
                                .width(if (isFloating) 40.dp else 48.dp)
                                .height(if (isFloating) 24.dp else 28.dp),
                            shape = RoundedCornerShape(50),
                            color = colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = destination.iconSelected,
                                    contentDescription = destination.label,
                                    tint = colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(iconSize)
                                )
                            }
                        }
                    } else {
                        Icon(
                            imageVector = destination.iconNotSelected,
                            contentDescription = destination.label,
                            tint = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(iconSize)
                        )
                    }
                }
            }
        }
    }
}
