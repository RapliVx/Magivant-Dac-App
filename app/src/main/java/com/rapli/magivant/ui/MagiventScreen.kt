package com.rapli.magivant.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SnippetFolder
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rapli.magivant.R
import com.rapli.magivant.ui.components.NewPresetInputDialog
import com.rapli.magivant.ui.components.PresetListDialog
import kotlin.math.abs

enum class SheetType { NONE, FILTER }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MagivantScreen(viewModel: MagivantViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val presetList by viewModel.presets.collectAsState()

    val scrollState = rememberScrollState()
    var activeSheet by remember { mutableStateOf(SheetType.NONE) }

    var showPresetListDialog by remember { mutableStateOf(false) }
    var showNewPresetDialog by remember { mutableStateOf(false) }
    var presetToExport by remember { mutableStateOf<DacPreset?>(null) }

    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.exportPresetToUri(context, it, presetToExport) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.importPresetFromUri(context, it)
            showPresetListDialog = true
        }
    }

    val appVersion = remember(context) {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(id = R.string.app_name),
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Version $appVersion",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    if (uiState.isConnected) {
                        IconButton(onClick = { showPresetListDialog = true }) {
                            Icon(
                                imageVector = Icons.Outlined.SnippetFolder,
                                contentDescription = "Presets",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.statusBarsPadding()
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            ElevatedCard(
                shape = RoundedCornerShape(32.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.magivant_banner),
                        contentDescription = null,
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.Crop
                    )
                    val monetScrimColor = MaterialTheme.colorScheme.surface
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        monetScrimColor.copy(alpha = 0.4f),
                                        monetScrimColor.copy(alpha = 0.95f)
                                    ),
                                    startY = 0f,
                                    endY = Float.POSITIVE_INFINITY
                                )
                            )
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Leteciel Magivant",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = if (uiState.isConnected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.errorContainer,
                                    contentColor = if (uiState.isConnected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onErrorContainer
                                ) {
                                    Text(
                                        text = if (uiState.isConnected) "Connected" else "Disconnected",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            if (uiState.isConnected) {
                                Text(
                                    text = "Firmware Version: ${uiState.firmwareVersion}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            if (uiState.isConnected) {
                ElevatedCard(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            "Volume Index: ${uiState.volumeIndex}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = uiState.volumeIndex.toFloat(),
                            onValueChange = { viewModel.setVolume(it.toInt()) },
                            valueRange = 0f..60f,
                            steps = 59,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                activeTickColor = MaterialTheme.colorScheme.onPrimary,
                                inactiveTickColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        val balText = when {
                            uiState.balanceBaseValue < 0 -> "Left ${abs(uiState.balanceBaseValue)}"
                            uiState.balanceBaseValue > 0 -> "Right ${uiState.balanceBaseValue}"
                            else -> "Balanced"
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(
                                imageVector = Icons.Rounded.GraphicEq,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Channel Balance: $balText",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Slider(
                            value = uiState.balanceBaseValue.toFloat(),
                            onValueChange = { viewModel.setBalance(it.toInt()) },
                            valueRange = -50f..50f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.secondary,
                                activeTrackColor = MaterialTheme.colorScheme.secondary,
                                inactiveTrackColor = MaterialTheme.colorScheme.secondaryContainer,
                                activeTickColor = MaterialTheme.colorScheme.onSecondary,
                                inactiveTickColor = MaterialTheme.colorScheme.secondary
                            )
                        )
                    }
                }

                ElevatedCard(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (uiState.isHighGain) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    ListItem(
                        headlineContent = {
                            Text(
                                "Gain Mode: ${if (uiState.isHighGain) "High" else "Low"}",
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.isHighGain) MaterialTheme.colorScheme.onErrorContainer
                                else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        },
                        supportingContent = {
                            Text(
                                if (uiState.isHighGain) "Extra power for headphones"
                                else "Clean output for IEMs",
                                color = if (uiState.isHighGain) MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = uiState.isHighGain,
                                onCheckedChange = { viewModel.setGain(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.error,
                                    checkedTrackColor = MaterialTheme.colorScheme.onError,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.primary,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }

                Text(
                    "Hardware Configuration",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp)
                )

                ElevatedCard(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    val filterList = listOf("Fast LL", "Fast PC", "Slow LL", "Slow PC", "NOS")
                    val currentFilterName = filterList.getOrElse(uiState.digitalFilterPos) { "Unknown" }

                    Box(modifier = Modifier.padding(8.dp)) {
                        ConfigSelectorItem(
                            icon = Icons.Rounded.FilterList,
                            label = "Digital Filter",
                            currentValue = currentFilterName,
                            onClick = { activeSheet = SheetType.FILTER }
                        )
                    }
                }

                ElevatedCard(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    val ledOptions = listOf("On", "Off (No Save)", "Off (Save)")
                    Box(modifier = Modifier.padding(8.dp)) {
                        LEDDropdownSelector(
                            options = ledOptions,
                            selectedPos = uiState.ledPos,
                            onSelected = { viewModel.setLed(it) }
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(32.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Usb,
                        contentDescription = "USB Disconnected",
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Device Not Found",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Please connect your Leteciel Magivant DAC\nvia USB to access hardware settings.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        if (activeSheet == SheetType.FILTER) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

            ModalBottomSheet(
                onDismissRequest = { activeSheet = SheetType.NONE },
                sheetState = sheetState,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Column(modifier = Modifier.padding(bottom = 40.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.FilterList, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text("Select Digital Filter", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider()

                    val filters = listOf(
                        "Fast roll-off Low latency",
                        "Fast roll-off Phase-compensated",
                        "Slow roll-off Low latency",
                        "Slow roll-off Phase-compensated",
                        "Non over-sampling"
                    )

                    LazyColumn {
                        itemsIndexed(filters) { i, name ->
                            val isSelected = i == uiState.digitalFilterPos
                            ListItem(
                                headlineContent = {
                                    Text(
                                        name,
                                        fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if(isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                leadingContent = {
                                    if (isSelected) Icon(Icons.Rounded.GraphicEq, null, tint = MaterialTheme.colorScheme.primary)
                                    else Spacer(Modifier.size(24.dp))
                                },
                                modifier = Modifier.clickable {
                                    viewModel.setDigitalFilter(i)
                                    activeSheet = SheetType.NONE
                                },
                                colors = ListItemDefaults.colors(
                                    containerColor = if(isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                )
                            )
                        }
                    }
                }
            }
        }

        if (showPresetListDialog && uiState.isConnected) {
            PresetListDialog(
                presetList = presetList,
                onDismiss = { showPresetListDialog = false },
                onPresetSelected = { preset ->
                    viewModel.applyPreset(preset)
                    showPresetListDialog = false
                },
                onNewClicked = {
                    showPresetListDialog = false
                    showNewPresetDialog = true
                },
                onImportClicked = {
                    showPresetListDialog = false
                    importLauncher.launch(arrayOf("application/json", "*/*"))
                },
                onExportClicked = { preset ->
                    showPresetListDialog = false
                    presetToExport = preset
                    val fileName = preset?.let { "${it.name}.json" } ?: "Magivant_Config.json"
                    exportLauncher.launch(fileName)
                },
                onDeleteClicked = { presets ->
                    viewModel.deletePresets(presets)
                }
            )
        }

        if (showNewPresetDialog && uiState.isConnected) {
            NewPresetInputDialog(
                onDismiss = { showNewPresetDialog = false },
                onSave = { newName ->
                    viewModel.saveCurrentStateAsPreset(newName)
                    showNewPresetDialog = false
                    showPresetListDialog = true
                }
            )
        }
    }
}

@Composable
fun ConfigSelectorItem(icon: ImageVector, label: String, currentValue: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Text(currentValue, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
        ElevatedAssistChip(
            onClick = onClick,
            label = { Text("Change") },
            colors = AssistChipDefaults.elevatedAssistChipColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                labelColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            shape = CircleShape
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LEDDropdownSelector(options: List<String>, selectedPos: Int, onSelected: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val currentLabel = options.getOrElse(selectedPos) { "Unknown" }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .clip(RoundedCornerShape(16.dp))
                .padding(vertical = 12.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Palette, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("LED Settings", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Text(currentLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
        }

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            options.forEachIndexed { index, option ->
                val isSelected = index == selectedPos
                DropdownMenuItem(
                    text = {
                        Text(
                            option,
                            fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if(isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        onSelected(index)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    modifier = Modifier.background(if(isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                )
            }
        }
    }
}