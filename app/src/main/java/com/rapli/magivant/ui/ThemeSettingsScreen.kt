package com.rapli.magivant.ui

import android.app.Activity
import android.content.Intent
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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.materialkolor.rememberDynamicColorScheme
import com.rapli.magivant.R
import com.rapli.magivant.ui.components.ExpressiveDropdownItem
import com.rapli.magivant.ui.components.ExpressiveList
import com.rapli.magivant.ui.components.ExpressiveListItem
import com.rapli.magivant.ui.components.ExpressiveSwitchItem
import com.rapli.magivant.ui.theme.AppSettings
import com.rapli.magivant.ui.theme.ColorMode
import com.rapli.magivant.ui.theme.keyColorOptions
import com.rapli.magivant.ui.util.clearCustomHeaderImage
import com.rapli.magivant.ui.util.copyUriToInternalStorage
import com.rapli.magivant.ui.util.saveHeaderImage
import com.rapli.magivant.usb.Cs43198Spec
import com.yalantis.ucrop.UCrop
import java.io.File
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
    onUpdateStartupPreset: (String) -> Unit
) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val systemDark = isSystemInDarkTheme()
    val isDark = appSettings.colorMode.getDarkThemeValue(systemDark)
    val isAmoled = appSettings.colorMode == ColorMode.DARKAMOLED
    var tempDpiScale by remember(appSettings.dpiScale) { mutableFloatStateOf(appSettings.dpiScale) }

    val cropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val resultUri = result.data?.let { UCrop.getOutput(it) }
            resultUri?.let {
                context.saveHeaderImage(it.toString())
                onUpdateCustomHeaderUri(it.toString())
            }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val mimeType = context.contentResolver.getType(it)
            if (mimeType?.contains("gif", ignoreCase = true) == true) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    context.saveHeaderImage(it.toString())
                    onUpdateCustomHeaderUri(it.toString())
                } catch (e: Exception) {
                    val internalUri = context.copyUriToInternalStorage(it)
                    internalUri?.let { savedUri ->
                        context.saveHeaderImage(savedUri.toString())
                        onUpdateCustomHeaderUri(savedUri.toString())
                    }
                }
            } else {
                val destinationUri = Uri.fromFile(
                    File(context.filesDir, "cropped_header_${System.currentTimeMillis()}.jpg")
                )
                val options = UCrop.Options().apply {
                    setToolbarColor(cs.surface.toArgb())
                    setToolbarWidgetColor(cs.onSurface.toArgb())
                    setActiveControlsWidgetColor(cs.primary.toArgb())
                    setRootViewBackgroundColor(cs.surface.toArgb())
                    setCropFrameColor(cs.primary.toArgb())
                    setFreeStyleCropEnabled(true)
                }
                val uCrop = UCrop.of(it, destinationUri)
                    .withAspectRatio(16f, 9f)
                    .withMaxResultSize(1600, 900)
                    .withOptions(options)
                cropLauncher.launch(uCrop.getIntent(context))
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
            useClassicLayout = appSettings.isClassicBanner,
            dpiScale = appSettings.dpiScale
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
                        onCheckedChange = { checked ->
                            if (checked) {
                                imagePickerLauncher.launch("image/*")
                            } else {
                                context.clearCustomHeaderImage()
                                onUpdateCustomHeaderUri(null)
                            }
                        }
                    )
                }

                if (appSettings.customHeaderUri != null) {
                    add {
                        ExpressiveListItem(
                            onClick = {
                                imagePickerLauncher.launch("image/*")
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
                                val maxVolumeSliderState = rememberSliderState(
                                    value = uiState.maxVolumeIndex.toFloat(),
                                    steps = 49,
                                    trackRange = 10f..60f
                                )
                                Slider(
                                    state = maxVolumeSliderState,
                                    onValueChange = { onUpdateMaxVolumeLimit(it.roundToInt()) }
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
                ExpressiveListItem(
                    headlineContent = { Text("UI Scale", color = cs.onSurface) },
                    supportingContent = {
                        Column {
                            Text("${(tempDpiScale * 100).toInt()}%", color = cs.onSurfaceVariant)
                            val scaleSliderState = rememberSliderState(
                                value = tempDpiScale,
                                steps = 9,
                                trackRange = 0.75f..1.25f
                            )
                            Slider(
                                state = scaleSliderState,
                                onValueChange = { tempDpiScale = it }
                            )
                        }
                    },
                    leadingContent = {
                        Icon(Icons.Filled.FormatSize, contentDescription = "UI Scale", tint = cs.primary)
                    },
                    trailingContent = {
                        if (tempDpiScale != appSettings.dpiScale) {
                            FilledTonalButton(
                                onClick = { onUpdateDpiScale(tempDpiScale) }
                            ) {
                                Text("Apply", color = cs.onSecondaryContainer)
                            }
                        }
                    }
                )
            }
        )
    }
}

@Composable
private fun ColorButton(
    color: Color,
    isSelected: Boolean,
    isDark: Boolean,
    isAmoled: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
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
                    enter = fadeIn() + scaleIn(initialScale = 0.8f),
                    exit = fadeOut() + scaleOut(targetScale = 0.8f)
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
                    enter = fadeIn() + scaleIn(initialScale = 0.8f),
                    exit = fadeOut() + scaleOut(targetScale = 0.8f)
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
    useClassicLayout: Boolean,
    dpiScale: Float
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.toFloat()
    val screenHeight = configuration.screenHeightDp.toFloat()
    val screenRatio = screenWidth / screenHeight
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

    val systemDensity = androidx.compose.ui.platform.LocalDensity.current
    val customDensity = remember(systemDensity, dpiScale) {
        androidx.compose.ui.unit.Density(
            density = systemDensity.density * dpiScale,
            fontScale = systemDensity.fontScale * dpiScale
        )
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
            CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides customDensity) {
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

                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                        if (enableFloatingNav) {
                            Surface(
                                modifier = Modifier.padding(bottom = 12.dp).height(40.dp).padding(horizontal = 24.dp),
                                shape = RoundedCornerShape(50),
                                color = colorScheme.surfaceContainerHigh
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Surface(shape = RoundedCornerShape(50), color = colorScheme.primaryContainer, modifier = Modifier.height(24.dp).width(40.dp)) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = colorScheme.onPrimaryContainer, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                    Box(modifier = Modifier.size(14.dp).background(colorScheme.onSurfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(4.dp)))
                                    Box(modifier = Modifier.size(14.dp).background(colorScheme.onSurfaceVariant.copy(alpha = 0.5f), CircleShape))
                                    Icon(Icons.Outlined.Tune, contentDescription = null, tint = colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                                }
                            }
                        } else {
                            Surface(
                                color = colorScheme.surfaceContainerHigh,
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Surface(shape = RoundedCornerShape(50), color = colorScheme.primaryContainer, modifier = Modifier.height(28.dp).width(48.dp)) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = colorScheme.onPrimaryContainer, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    Box(modifier = Modifier.size(14.dp).background(colorScheme.onSurfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(4.dp)))
                                    Box(modifier = Modifier.size(14.dp).background(colorScheme.onSurfaceVariant.copy(alpha = 0.5f), CircleShape))
                                    Icon(Icons.Outlined.Tune, contentDescription = null, tint = colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
