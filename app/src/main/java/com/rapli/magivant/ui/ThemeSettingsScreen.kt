package com.rapli.magivant.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.requiredSize
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.rapli.magivant.ui.components.ExpressiveColumn
import com.rapli.magivant.ui.components.ExpressiveDropdownItem
import com.rapli.magivant.ui.components.ExpressiveList
import com.rapli.magivant.ui.components.ExpressiveListItem
import com.rapli.magivant.ui.components.ExpressiveSwitchItem
import com.rapli.magivant.ui.theme.AppSettings
import com.rapli.magivant.ui.theme.ColorMode
import com.rapli.magivant.ui.theme.KeyColorOption
import com.rapli.magivant.ui.theme.isInDarkTheme
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
            if (mimeType == "image/gif") {
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
                val uCrop = UCrop.of(it, destinationUri)
                    .withAspectRatio(16f, 9f)
                    .withMaxResultSize(1600, 900)
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
            uiState = uiState
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
        ) {
            themeOptions.forEachIndexed { index, (label, mode, icon) ->
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ExpressiveColumn(
            title = "Color Palette",
            content = buildList {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    add {
                        ExpressiveSwitchItem(
                            icon = Icons.Filled.ColorLens,
                            title = "Dynamic Wallpaper Color (Monet)",
                            summary = "Derive surface and accent tones from Android 12+ system wallpaper",
                            checked = appSettings.keyColor == 0,
                            onCheckedChange = { enabled ->
                                val targetColor = if (enabled) {
                                    0
                                } else {
                                    keyColorOptions.first().color.toArgb()
                                }
                                onUpdateKeyColor(targetColor)
                            }
                        )
                    }
                }

                add {
                    AnimatedVisibility(
                        visible = appSettings.keyColor != 0 || Build.VERSION.SDK_INT < Build.VERSION_CODES.S,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        val isDark = isInDarkTheme(appSettings.colorMode.value)
                        val isAmoled = appSettings.colorMode == ColorMode.DARKAMOLED
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        ) {
                            val selectedName = keyColorOptions.find {
                                it.color.toArgb() == appSettings.keyColor
                            }?.name ?: "Custom Seed"
                            Text(
                                text = "Active Seed: $selectedName",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                keyColorOptions.forEach { option ->
                                    ColorButton(
                                        option = option,
                                        isSelected = appSettings.keyColor == option.color.toArgb(),
                                        isDark = isDark,
                                        isAmoled = isAmoled,
                                        onClick = {
                                            onUpdateKeyColor(option.color.toArgb())
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        ExpressiveList(
            title = "Banner & Navigation Style",
            content = buildList {
                add {
                    ExpressiveSwitchItem(
                        icon = Icons.Filled.Image,
                        title = "Custom Header Banner",
                        summary = if (appSettings.customHeaderUri != null) {
                            "Using custom image or animated GIF banner"
                        } else {
                            "Using default Leteciel Magivant banner"
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
                            headlineContent = { Text("Change Banner Image or GIF") },
                            supportingContent = { Text("Supports 16:9 cropped photos (UCrop) and looping animated GIFs") },
                            leadingContent = {
                                Icon(Icons.Filled.Image, contentDescription = "Change Banner")
                            }
                        )
                    }
                }

                add {
                    ExpressiveSwitchItem(
                        icon = Icons.Filled.ViewAgenda,
                        title = "Classic Stacked Banner",
                        summary = if (appSettings.isClassicBanner) {
                            "Full-width banner card on top with two telemetry cards below"
                        } else {
                            "Modern split layout (60% banner left, 40% telemetry cards right)"
                        },
                        checked = appSettings.isClassicBanner,
                        onCheckedChange = onUpdateClassicBanner
                    )
                }

                add {
                    ExpressiveSwitchItem(
                        icon = Icons.Filled.ViewCarousel,
                        title = "Floating Pill Navbar",
                        summary = "Use elevated pill navigation bar instead of full-width bottom bar",
                        checked = appSettings.isFloatingNavbar,
                        onCheckedChange = onUpdateFloatingNavbar
                    )
                }

                if (appSettings.isFloatingNavbar) {
                    add {
                        ExpressiveSwitchItem(
                            icon = Icons.Filled.Swipe,
                            title = "Floating Navbar Slide Gesture",
                            summary = "Drag horizontally across the floating pill to switch tabs with spring physics",
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
            title = "CS43198 Hardware Protection & Automation",
            content = listOf(
                {
                    ExpressiveListItem(
                        headlineContent = { Text("Hardware Volume Ceiling") },
                        supportingContent = {
                            Column {
                                Text(
                                    text = "Max Step: ${uiState.maxVolumeIndex}/60 ($maxLimitDb) • Protects sensitive IEMs",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            Icon(Icons.Filled.Hearing, contentDescription = "Volume Ceiling")
                        }
                    )
                },
                {
                    ExpressiveDropdownItem(
                        icon = Icons.Filled.PlayCircleOutline,
                        title = "Auto-Apply Profile on USB Connect",
                        summary = "Automatically write a saved profile to the CS43198 when plugged in",
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
            title = "Display Density",
            content = listOf {
                ExpressiveListItem(
                    headlineContent = { Text("App UI Scale") },
                    supportingContent = {
                        Column {
                            Text("${(tempDpiScale * 100).toInt()}%")
                            Slider(
                                value = tempDpiScale,
                                onValueChange = { tempDpiScale = it },
                                valueRange = 0.75f..1.25f,
                                steps = 9
                            )
                        }
                    },
                    leadingContent = {
                        Icon(Icons.Filled.FormatSize, contentDescription = "UI Scale")
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
    option: KeyColorOption,
    isSelected: Boolean,
    isDark: Boolean,
    isAmoled: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = if (option.color == Color.Unspecified) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (isDark) androidx.compose.material3.dynamicDarkColorScheme(context)
            else androidx.compose.material3.dynamicLightColorScheme(context)
        } else {
            MaterialTheme.colorScheme
        }
    } else {
        rememberDynamicColorScheme(
            seedColor = option.color,
            isDark = isDark,
            isAmoled = isAmoled
        )
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = colorScheme.surfaceContainer,
        modifier = Modifier.size(72.dp)
    ) {
        val scale by animateFloatAsState(
            targetValue = if (isSelected) 1.1f else 1.0f,
            animationSpec = spring(stiffness = Spring.StiffnessLow),
            label = "colorButtonScale"
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
        ) {
            Canvas(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .border(2.dp, colorScheme.outlineVariant, CircleShape)
            ) {
                drawArc(
                    color = colorScheme.primaryContainer,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true
                )
                drawArc(
                    color = colorScheme.tertiaryContainer,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = true
                )
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = option.name,
                        tint = colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemePreviewCard(
    appSettings: AppSettings,
    uiState: DacUiState
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Live Theme & Layout Preview",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.background,
                        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                    )
                    .padding(top = 14.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 14.dp)
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
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.GraphicEq,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Column {
                                            Text(
                                                text = if (uiState.isConnected) "Connected" else "Magivant DAC",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                            Text(
                                                text = uiState.sampleRateText,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 10.dp),
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = Cs43198Spec.formatDbFs(uiState.rawVolumeReg),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = Cs43198Spec.formatVrms(uiState.rawVolumeReg, uiState.isHighGain),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                        .weight(0.6f)
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
                                        .weight(0.4f)
                                        .fillMaxHeight(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 10.dp),
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = if (uiState.isConnected) "CS43198 Live" else "CS43198 Ready",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                            Text(
                                                text = uiState.sampleRateText,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                                            )
                                        }
                                    }

                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 10.dp),
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = Cs43198Spec.formatDbFs(uiState.rawVolumeReg),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = Cs43198Spec.formatVrms(uiState.rawVolumeReg, uiState.isHighGain),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                                tonalElevation = 3.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(MaterialTheme.colorScheme.secondaryContainer)
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Filled.GraphicEq,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "DAC Control",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                    }

                                    Icon(
                                        imageVector = Icons.Outlined.Tune,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .padding(horizontal = 8.dp)
                                            .size(16.dp)
                                    )

                                    Icon(
                                        imageVector = Icons.Outlined.Palette,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .padding(horizontal = 8.dp)
                                            .size(16.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        NavigationBar(
                            modifier = Modifier.height(52.dp),
                            windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
                        ) {
                            NavigationBarItem(
                                selected = true,
                                onClick = {},
                                icon = {
                                    Icon(
                                        Icons.Filled.GraphicEq,
                                        contentDescription = null,
                                        modifier = Modifier.requiredSize(18.dp)
                                    )
                                }
                            )
                            NavigationBarItem(
                                selected = false,
                                onClick = {},
                                icon = {
                                    Icon(
                                        Icons.Outlined.Tune,
                                        contentDescription = null,
                                        modifier = Modifier.requiredSize(18.dp)
                                    )
                                }
                            )
                            NavigationBarItem(
                                selected = false,
                                onClick = {},
                                icon = {
                                    Icon(
                                        Icons.Outlined.Palette,
                                        contentDescription = null,
                                        modifier = Modifier.requiredSize(18.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
