package com.rapli.magivant.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.UsbOff
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rapli.magivant.ui.DacPreset
import com.rapli.magivant.ui.DacUiState
import com.rapli.magivant.usb.Cs43198Spec
import com.rapli.magivant.usb.DacVolume
import java.awt.FileDialog as AwtFileDialog
import java.awt.Frame
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt

private enum class DesktopPage(val title: String, val icon: ImageVector) {
    DAC("DAC controls", Icons.Outlined.GraphicEq),
    PRESETS("Presets", Icons.Outlined.Tune),
    SETTINGS("Settings", Icons.Outlined.Palette)
}

@Composable
fun DesktopApp(
    controller: DesktopDacController,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val state by controller.uiState.collectAsState()
    val presets by controller.presets.collectAsState()
    var page by remember { mutableStateOf(DesktopPage.DAC) }
    var selectedPreset by remember { mutableStateOf<DacPreset?>(null) }

    BoxWithConstraints(modifier.background(MaterialTheme.colorScheme.surface)) {
        val compactNavigation = maxWidth < 1080.dp
        Row(Modifier.fillMaxSize()) {
            NavigationPanel(page, compactNavigation) { page = it }
            Column(Modifier.weight(1f).fillMaxHeight()) {
                WindowHeader(
                    state = state,
                    onConnect = controller::connect,
                    onDisconnect = controller::disconnect
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

                Column(
                    modifier = Modifier.fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(if (compactNavigation) 20.dp else 32.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    when (page) {
                        DesktopPage.DAC -> DacPage(state, presets, controller)
                        DesktopPage.PRESETS -> PresetsPage(
                            state = state,
                            presets = presets,
                            selectedPreset = selectedPreset,
                            onSelectPreset = { selectedPreset = it },
                            onSavePreset = controller::saveCurrentPreset,
                            onApplyPreset = { controller.applyPreset(it) },
                            onDeletePreset = { controller.deletePreset(it) },
                            onImportPreset = { controller.importPreset(it) },
                            onExportPreset = { file, preset -> controller.exportPreset(file, preset) }
                        )
                        DesktopPage.SETTINGS -> SettingsPage(
                            state = state,
                            isDarkTheme = isDarkTheme,
                            onToggleTheme = onToggleTheme,
                            onUpdateVolumeLimit = controller::setMaxVolumeLimit,
                            onSetStartupPreset = controller::setStartupPreset,
                            presets = presets
                        )
                    }
                    state.errorMessage?.let { ErrorBanner(it, controller::clearError) }
                }
            }
        }
    }
}

@Composable
private fun NavigationPanel(page: DesktopPage, compact: Boolean, onPageSelected: (DesktopPage) -> Unit) {
    Surface(
        modifier = Modifier.width(if (compact) 84.dp else 232.dp).fillMaxHeight(),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            Modifier.fillMaxSize().padding(if (compact) 12.dp else 20.dp),
            horizontalAlignment = if (compact) Alignment.CenterHorizontally else Alignment.Start
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(
                        Icons.Filled.GraphicEq,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(12.dp).size(28.dp)
                    )
                }
                if (!compact) {
                    Column {
                        Text("MAGIVANT", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("DAC control", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(if (compact) 28.dp else 40.dp))
            if (!compact) {
                Text("DEVICE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 12.dp, bottom = 8.dp))
            }
            DesktopPage.entries.forEach { destination ->
                val selected = destination == page
                Surface(
                    onClick = { onPageSelected(destination) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = if (compact) 0.dp else 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            destination.icon,
                            contentDescription = if (compact) destination.title else null,
                            tint = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!compact) {
                            Text(
                                destination.title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))
            if (!compact) {
            Text("3.7-Dekstop-Expressive", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(12.dp))
            }
        }
    }
}

@Composable
private fun WindowHeader(state: DacUiState, onConnect: () -> Unit, onDisconnect: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                when {
                    state.isSyncing -> "Connecting to DAC"
                    state.isConnected -> "${state.bridgeModeText} connected"
                    else -> "Your listening setup"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                if (state.isConnected) "Firmware ${state.firmwareVersion}  •  ${state.sampleRateText}" else "Connect a Magivant DAC to adjust its hardware settings",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (state.isConnected) {
            OutlinedButton(onClick = onDisconnect) {
                Icon(Icons.Filled.UsbOff, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Disconnect")
            }
        } else {
            Button(onClick = onConnect, enabled = !state.isSyncing) {
                Icon(Icons.Filled.Usb, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (state.isSyncing) "Scanning…" else "Scan and connect")
            }
        }
    }
}

@Composable
private fun DacPage(state: DacUiState, presets: List<DacPreset>, controller: DesktopDacController) {
    Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
        Column {
            Text("DAC controls", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Tune output level, channel balance and hardware processing.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (!state.hasDeviceState) {
            EmptyConnectionCard(state.isSyncing)
        } else {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                if (maxWidth >= 1080.dp) {
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1.25f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            VolumeCard(state, controller)
                            BalanceCard(state, controller)
                        }
                        Column(Modifier.weight(0.85f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            HardwareCard(state, controller)
                            PresetQuickCard(state, presets, controller)
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        VolumeCard(state, controller)
                        BalanceCard(state, controller)
                        HardwareCard(state, controller)
                        PresetQuickCard(state, presets, controller)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyConnectionCard(isSyncing: Boolean) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                Icon(Icons.Filled.Usb, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(18.dp).size(32.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(if (isSyncing) "Looking for your DAC…" else "No DAC connected", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("Connect a supported Magivant DAC and scan to load its current settings.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun VolumeCard(state: DacUiState, controller: DesktopDacController) {
    var sliderValue by remember(state.volumeIndex) { mutableFloatStateOf(state.volumeIndex.toFloat()) }
    val db = Cs43198Spec.formatDbFs(state.rawVolumeReg)
    val vrms = Cs43198Spec.formatVrms(state.rawVolumeReg, state.isHighGain)

    AppCard {
        SectionTitle(
            icon = if (state.rawVolumeReg == 0xFF) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
            title = "Output volume",
            supporting = "${state.volumeIndex} / ${state.maxVolumeIndex}  •  $db  •  $vrms"
        )
        Slider(
            value = sliderValue.coerceIn(0f, state.maxVolumeIndex.toFloat()),
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { controller.setVolume(sliderValue.roundToInt()) },
            valueRange = 0f..state.maxVolumeIndex.toFloat(),
            steps = (state.maxVolumeIndex - 1).coerceAtLeast(0),
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 8.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = { controller.adjustVolumeByHalfDb(-1) },
                enabled = state.rawVolumeReg < 0xFF,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Remove, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("−0.5 dB")
            }
            FilledTonalButton(
                onClick = { controller.setVolume(0) },
                enabled = state.rawVolumeReg != 0xFF,
                modifier = Modifier.weight(0.86f)
            ) { Text("Mute") }
            OutlinedButton(
                onClick = { controller.adjustVolumeByHalfDb(1) },
                enabled = state.rawVolumeReg > DacVolume.registerValues[state.maxVolumeIndex],
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("+0.5 dB")
            }
        }
    }
}

@Composable
private fun BalanceCard(state: DacUiState, controller: DesktopDacController) {
    var sliderValue by remember(state.balanceBaseValue) { mutableFloatStateOf(state.balanceBaseValue.toFloat()) }
    AppCard {
        SectionTitle(Icons.Filled.Tune, "Channel balance", Cs43198Spec.formatBalanceDb(state.balanceBaseValue))
        Slider(
            value = sliderValue.coerceIn(-10f, 10f),
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { controller.setBalance(sliderValue.roundToInt()) },
            valueRange = -10f..10f,
            steps = 19,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 6.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { controller.setBalance(-10) }, enabled = state.balanceBaseValue > -10, modifier = Modifier.weight(1f)) { Text("Left bias") }
            FilledTonalButton(onClick = { controller.setBalance(0) }, enabled = state.balanceBaseValue != 0, modifier = Modifier.weight(0.8f)) { Text("Center") }
            OutlinedButton(onClick = { controller.setBalance(10) }, enabled = state.balanceBaseValue < 10, modifier = Modifier.weight(1f)) { Text("Right bias") }
        }
    }
}

@Composable
private fun HardwareCard(state: DacUiState, controller: DesktopDacController) {
    AppCard {
        Text("Hardware processing", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        DropdownSetting(
            title = "Gain",
            value = if (state.isHighGain) "High gain · 2 Vrms" else "Low gain · 1 Vrms",
            options = listOf("Low gain · 1 Vrms", "High gain · 2 Vrms"),
            onSelected = { controller.setGain(it.startsWith("High")) }
        )
        HorizontalDivider(Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        DropdownSetting(
            title = "Digital filter",
            value = filterNames.getOrElse(state.digitalFilterPos) { "Unknown" },
            options = filterNames,
            onSelected = { controller.setDigitalFilter(filterNames.indexOf(it)) }
        )
        HorizontalDivider(Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        DropdownSetting(
            title = "Indicator LED",
            value = ledNames.getOrElse(state.ledPos) { "Unknown" },
            options = ledNames,
            onSelected = { controller.setLed(ledNames.indexOf(it)) }
        )
    }
}

@Composable
private fun PresetQuickCard(state: DacUiState, presets: List<DacPreset>, controller: DesktopDacController) {
    AppCard {
        SectionTitle(Icons.Filled.CheckCircle, "Saved preset", if (state.activePresetName.isNotEmpty()) state.activePresetName else "Current hardware settings")
        Spacer(Modifier.height(12.dp))
        DropdownSetting(
            title = "Apply preset",
            value = state.startupPresetName.ifEmpty { "Choose a preset" },
            options = presets.map { it.name }.ifEmpty { listOf("No saved presets") },
            onSelected = { name -> presets.firstOrNull { it.name == name }?.let(controller::applyPreset) }
        )
    }
}

@Composable
private fun PresetsPage(
    state: DacUiState,
    presets: List<DacPreset>,
    selectedPreset: DacPreset?,
    onSelectPreset: (DacPreset?) -> Unit,
    onSavePreset: (String) -> Unit,
    onApplyPreset: (DacPreset) -> Unit,
    onDeletePreset: (DacPreset) -> Unit,
    onImportPreset: (File) -> Unit,
    onExportPreset: (File, DacPreset) -> Unit
) {
    var newPresetName by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Column {
            Text("Presets", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Save, organize and restore DAC settings.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        AppCard {
            Text("Save current settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.OutlinedTextField(
                    value = newPresetName,
                    onValueChange = { newPresetName = it.take(80); error = null },
                    modifier = Modifier.weight(1f),
                    label = { Text("Preset name") },
                    singleLine = true,
                    enabled = state.hasDeviceState
                )
                Button(
                    onClick = {
                        if (newPresetName.isBlank()) error = "Enter a name for this preset."
                        else { onSavePreset(newPresetName.trim()); newPresetName = "" }
                    },
                    enabled = state.hasDeviceState
                ) { Text("Save preset") }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
        }

        AppCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Your presets", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("${presets.size} saved", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { chooseFile("Import Magivant preset", AwtFileDialog.LOAD, "json")?.let(onImportPreset) }) {
                        Icon(Icons.Filled.FolderOpen, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Import JSON")
                    }
                    OutlinedButton(onClick = { chooseFile("Export Magivant preset", AwtFileDialog.SAVE, "json")?.let { file ->
                        (selectedPreset ?: presets.firstOrNull())?.let { onExportPreset(file.withExtensionIfMissing("json"), it) }
                    } }, enabled = selectedPreset != null || presets.size == 1) {
                        Text("Export selected")
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            if (presets.isEmpty()) {
                Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("No presets yet", style = MaterialTheme.typography.titleMedium)
                        Text("Save your connected DAC settings or import a JSON preset.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                presets.forEach { preset ->
                    PresetRow(
                        preset = preset,
                        selected = selectedPreset?.name == preset.name,
                        active = state.activePresetName == preset.name,
                        onSelect = { onSelectPreset(if (selectedPreset?.name == preset.name) null else preset) },
                        onApply = { onApplyPreset(preset) },
                        onDelete = { onDeletePreset(preset); if (selectedPreset?.name == preset.name) onSelectPreset(null) }
                    )
                    if (preset != presets.last()) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
            }
        }
    }
}

@Composable
private fun PresetRow(preset: DacPreset, selected: Boolean, active: Boolean, onSelect: () -> Unit, onApply: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            onClick = onSelect,
            shape = RoundedCornerShape(18.dp),
            color = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
            modifier = Modifier.weight(1f)
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(preset.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (active) Text("ACTIVE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                Text(
                    "${preset.volumeIndex}/60 · ${Cs43198Spec.formatBalanceDb(preset.balanceBaseValue)} · ${if (preset.isHighGain) "High gain" else "Low gain"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        OutlinedButton(onClick = onApply) { Text("Apply") }
        IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete ${preset.name}", tint = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun SettingsPage(
    state: DacUiState,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onUpdateVolumeLimit: (Int) -> Unit,
    onSetStartupPreset: (String) -> Unit,
    presets: List<DacPreset>
) {
    var limit by remember(state.maxVolumeIndex) { mutableFloatStateOf(state.maxVolumeIndex.toFloat()) }
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Column {
            Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Configure the desktop interface and safe output limits.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        AppCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Dark appearance", style = MaterialTheme.typography.titleMedium)
                    Text("Use the dark expressive colour scheme.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = isDarkTheme, onCheckedChange = { onToggleTheme() })
            }
            HorizontalDivider(Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Column {
                Text("Maximum volume · ${limit.roundToInt()} / 60", style = MaterialTheme.typography.titleMedium)
                Slider(
                    value = limit,
                    onValueChange = { limit = it },
                    onValueChangeFinished = { onUpdateVolumeLimit(limit.roundToInt()) },
                    valueRange = 10f..60f,
                    steps = 49
                )
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            DropdownSetting(
                title = "Startup preset",
                value = state.startupPresetName.ifEmpty { "Keep hardware state" },
                options = listOf("Keep hardware state") + presets.map { it.name },
                onSelected = { onSetStartupPreset(if (it == "Keep hardware state") "" else it) }
            )
        }
        AppCard {
            SectionTitle(Icons.Filled.Usb, "USB access", "Linux requires permission to access the DAC control interface.")
            Spacer(Modifier.height(12.dp))
            Text("Install the supplied udev rule, then reconnect the DAC. Playback remains managed by ALSA / PipeWire.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Supported IDs: 2FC6:F13A and 2FC6:F13B", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 10.dp))
        }
    }
}

@Composable
private fun DropdownSetting(title: String, value: String, options: List<String>, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Surface(onClick = { expanded = true }, shape = RoundedCornerShape(16.dp), color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
                Icon(Icons.Filled.Tune, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { expanded = false; onSelected(option) })
            }
        }
    }
}

@Composable
private fun SectionTitle(icon: ImageVector, title: String, supporting: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.padding(11.dp).size(21.dp))
        }
        Column {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AppCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.24f))
    ) {
        Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
private fun ErrorBanner(message: String, onDismiss: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(16.dp), onClick = onDismiss) {
        Text(message, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.fillMaxWidth().padding(16.dp))
    }
}

private fun chooseFile(title: String, mode: Int, extension: String): File? {
    val dialog = AwtFileDialog(null as Frame?, title, mode).apply {
        isMultipleMode = false
        if (mode == AwtFileDialog.SAVE) file = "preset.$extension"
    }
    dialog.isVisible = true
    return dialog.file?.let { File(dialog.directory ?: ".", it) }
}

private fun File.withExtensionIfMissing(extension: String): File =
    if (name.substringAfterLast('.', missingDelimiterValue = "").isNotEmpty()) this else File(parentFile, "$name.$extension")

private val filterNames = listOf(
    "Fast · low latency",
    "Fast · phase compensated",
    "Slow · low latency",
    "Slow · phase compensated",
    "Non-oversampling"
)

private val ledNames = listOf("Follow sample rate", "Always off", "Always red")
