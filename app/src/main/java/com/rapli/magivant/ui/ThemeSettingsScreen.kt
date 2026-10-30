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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.runtime.Composable
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
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme
import com.rapli.magivant.R
import com.rapli.magivant.ui.components.ExpressiveDropdownItem
import com.rapli.magivant.ui.components.ExpressiveList
import com.rapli.magivant.ui.components.ExpressiveListItem
import com.rapli.magivant.ui.components.ExpressiveSwitchItem
import com.rapli.magivant.ui.components.expressiveSurfaceColor
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
                    setStatusBarColor(cs.surface.toArgb())
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
        Triple("System", ColorMode.SYSTEM, Icons.Filled.BrightnessAuto),
        Triple("Light", ColorMode.LIGHT, Icons.Filled.Brightness7),
        Triple("Dark", ColorMode.DARK, Icons.Filled.Brightness4),
        Triple("AMOLED", ColorMode.DARKAMOLED, Icons.Filled.Brightness1)
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
            appSettings = appSettings,
            uiState = uiState,
            isDark = isDark,
            isAmoled = isAmoled
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
                    themeOptions.forEachIndexed { index, (label, mode, icon) ->
                        val isChecked = appSettings.colorMode == mode
                        ToggleButton(
                            checked = isChecked,
                            onCheckedChange = {
                                if (it) onUpdateThemeMode(mode)
                            },
                            colors = ToggleButtonDefaults.toggleButtonColors(
                                containerColor = cs.surfaceContainerHighest,
                                contentColor = cs.onSurface,
                                checkedContainerColor = cs.primary,
                                checkedContentColor = cs.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .semantics { role = Role.RadioButton },
                            shapes = when (index) {
                                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                themeOptions.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                            }
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (isChecked) cs.onPrimary else cs.onSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isChecked) cs.onPrimary else cs.onSurface,
                                    maxLines = 1
                                )
                            }
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
                                Slider(
                                    value = uiState.maxVolumeIndex.toFloat(),
                                    onValueChange = { onUpdateMaxVolumeLimit(it.roundToInt()) },
                                    valueRange = 10f..60f,
                                    steps = 49
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
                            Slider(
                                value = tempDpiScale,
                                onValueChange = { tempDpiScale = it },
                                valueRange = 0.75f..1.25f,
                                steps = 9
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
                                Text("Apply")
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
    val colorScheme = if (color == Color.Unspecified) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            rememberDynamicColorScheme(
                seedColor = colorResource(id = android.R.color.system_accent1_500),
                isDark = isDark,
                isAmoled = isAmoled,
                style = PaletteStyle.TonalSpot
            )
        } else {
            MaterialTheme.colorScheme
        }
    } else {
        rememberDynamicColorScheme(
            seedColor = color,
            isDark = isDark,
            isAmoled = isAmoled,
            style = PaletteStyle.TonalSpot
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
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) colorScheme.primary else colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
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
    appSettings: AppSettings,
    uiState: DacUiState,
    isDark: Boolean,
    isAmoled: Boolean
) {
    val context = LocalContext.current
    val previewScheme = when {
        appSettings.keyColor == 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            rememberDynamicColorScheme(
                seedColor = colorResource(id = android.R.color.system_accent1_500),
                isDark = isDark,
                isAmoled = isAmoled,
                style = PaletteStyle.TonalSpot
            )
        }
        appSettings.keyColor != 0 -> {
            rememberDynamicColorScheme(
                seedColor = Color(appSettings.keyColor),
                isDark = isDark,
                isAmoled = isAmoled,
                style = PaletteStyle.TonalSpot
            )
        }
        else -> {
            if (isDark) darkColorScheme() else expressiveLightColorScheme()
        }
    }

    val previewBg = previewScheme.surfaceContainerLowest
    val previewCardSurface = previewScheme.surfaceContainerHigh
    val previewText = previewScheme.onSurface
    val previewSubText = previewScheme.onSurfaceVariant

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

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = expressiveSurfaceColor()
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Preview",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                color = previewBg,
                border = BorderStroke(1.dp, previewScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (appSettings.isClassicBanner) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(74.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(appSettings.customHeaderUri ?: R.drawable.magivant_banner)
                                        .crossfade(true)
                                        .build(),
                                    imageLoader = imageLoader,
                                    contentDescription = "Banner Preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    color = previewScheme.secondaryContainer
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.GraphicEq,
                                            contentDescription = null,
                                            tint = previewScheme.onSecondaryContainer,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Column {
                                            Text(
                                                text = if (uiState.isConnected) "Connected" else "Magivant DAC",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = previewScheme.onSecondaryContainer
                                            )
                                            Text(
                                                text = uiState.sampleRateText,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = previewScheme.onSecondaryContainer.copy(alpha = 0.85f)
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    color = previewCardSurface
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 10.dp),
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = Cs43198Spec.formatDbFs(uiState.rawVolumeReg),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = previewText
                                        )
                                        Text(
                                            text = Cs43198Spec.formatVrms(uiState.rawVolumeReg, uiState.isHighGain),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = previewSubText
                                        )
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Card(
                                    modifier = Modifier
                                        .weight(0.58f)
                                        .height(94.dp),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(appSettings.customHeaderUri ?: R.drawable.magivant_banner)
                                            .crossfade(true)
                                            .build(),
                                        imageLoader = imageLoader,
                                        contentDescription = "Banner Preview",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Column(
                                    modifier = Modifier
                                        .weight(0.42f)
                                        .fillMaxHeight(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        shape = RoundedCornerShape(14.dp),
                                        color = previewScheme.secondaryContainer
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 10.dp),
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = if (uiState.isConnected) "Connected" else "Standby",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = previewScheme.onSecondaryContainer
                                            )
                                            Text(
                                                text = uiState.sampleRateText,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = previewScheme.onSecondaryContainer.copy(alpha = 0.85f)
                                            )
                                        }
                                    }

                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        shape = RoundedCornerShape(14.dp),
                                        color = previewCardSurface
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 10.dp),
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = Cs43198Spec.formatDbFs(uiState.rawVolumeReg),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = previewText
                                            )
                                            Text(
                                                text = Cs43198Spec.formatVrms(uiState.rawVolumeReg, uiState.isHighGain),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = previewSubText
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (appSettings.isFloatingNavbar) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = previewCardSurface
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(previewScheme.secondaryContainer)
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Filled.GraphicEq,
                                                contentDescription = null,
                                                tint = previewScheme.onSecondaryContainer,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "DAC",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = previewScheme.onSecondaryContainer
                                            )
                                        }
                                    }

                                    Icon(
                                        imageVector = Icons.Outlined.Tune,
                                        contentDescription = null,
                                        tint = previewSubText,
                                        modifier = Modifier
                                            .padding(horizontal = 8.dp)
                                            .size(16.dp)
                                    )

                                    Icon(
                                        imageVector = Icons.Outlined.Palette,
                                        contentDescription = null,
                                        tint = previewSubText,
                                        modifier = Modifier
                                            .padding(horizontal = 8.dp)
                                            .size(16.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Surface(
                            color = previewCardSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(previewScheme.secondaryContainer)
                                        .padding(horizontal = 14.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.GraphicEq,
                                        contentDescription = null,
                                        tint = previewScheme.onSecondaryContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Outlined.Tune,
                                    contentDescription = null,
                                    tint = previewSubText,
                                    modifier = Modifier.size(16.dp)
                                )
                                Icon(
                                    imageVector = Icons.Outlined.Palette,
                                    contentDescription = null,
                                    tint = previewSubText,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
