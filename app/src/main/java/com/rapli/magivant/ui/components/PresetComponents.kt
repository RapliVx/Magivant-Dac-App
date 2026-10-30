package com.rapli.magivant.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rapli.magivant.ui.DacPreset
import com.rapli.magivant.ui.DacUiState
import com.rapli.magivant.usb.Cs43198Spec

private val filterShortLabels = listOf(
    "Fast Low-Lat",
    "Fast Phase-Comp",
    "Slow Low-Lat",
    "Slow Phase-Comp",
    "NOS (Non-Oversampling)"
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MagivantPresetScreen(
    uiState: DacUiState,
    presetList: List<DacPreset>,
    volumeTable: List<Int>,
    bottomInnerPadding: Dp,
    onPresetSelected: (DacPreset) -> Unit,
    onNewClicked: () -> Unit,
    onImportClicked: () -> Unit,
    onExportClicked: (DacPreset?) -> Unit,
    onDeleteClicked: (List<DacPreset>) -> Unit,
    onSetStartupPreset: (String) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    var selectedItems by remember(presetList) { mutableStateOf(setOf<DacPreset>()) }
    val isSelectionMode = selectedItems.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        PresetActionHeader(
            uiState = uiState,
            isSelectionMode = isSelectionMode,
            selectedCount = selectedItems.size,
            onClearSelection = { selectedItems = emptySet() },
            onDeleteSelected = {
                onDeleteClicked(selectedItems.toList())
                selectedItems = emptySet()
            },
            onExportSelected = {
                if (selectedItems.size == 1) {
                    onExportClicked(selectedItems.first())
                    selectedItems = emptySet()
                }
            },
            onNewClicked = onNewClicked,
            onImportClicked = onImportClicked,
            onExportCurrentClicked = { onExportClicked(null) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (presetList.isEmpty()) {
            EmptyPresetStateCard(
                onNewClicked = onNewClicked,
                onImportClicked = onImportClicked
            )
        } else {
            ExpressiveLazyList(
                modifier = Modifier.weight(1f),
                items = presetList,
                key = { it.name },
                contentPadding = PaddingValues(bottom = bottomInnerPadding + 24.dp),
                header = {
                    Text(
                        text = "Saved Presets (${presetList.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = cs.primary,
                        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                    )
                }
            ) { _, preset ->
                val isSelected = selectedItems.contains(preset)
                val isActive = uiState.activePresetName == preset.name
                val isStartup = uiState.startupPresetName == preset.name
                val regByte = volumeTable.getOrElse(preset.volumeIndex) { 0x30 }
                val dbText = Cs43198Spec.formatDbFs(regByte)
                val gainText = if (preset.isHighGain) "High Gain (2Vrms)" else "Low Gain (1Vrms)"
                val filterText = filterShortLabels.getOrElse(preset.digitalFilterPos) { "Filter ${preset.digitalFilterPos}" }

                ExpressiveListItem(
                    onClick = {
                        if (isSelectionMode) {
                            selectedItems = if (isSelected) selectedItems - preset else selectedItems + preset
                        } else {
                            onPresetSelected(preset)
                        }
                    },
                    onLongClick = {
                        selectedItems = if (isSelected) selectedItems - preset else selectedItems + preset
                    },
                    leadingContent = {
                        Icon(
                            imageVector = if (isActive) Icons.Filled.CheckCircle else Icons.Outlined.Tune,
                            contentDescription = preset.name,
                            tint = if (isActive) cs.primary else cs.onSurfaceVariant
                        )
                    },
                    headlineContent = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = preset.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                color = cs.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (isStartup) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = cs.secondaryContainer
                                ) {
                                    Text(
                                        text = "Auto-Start",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = cs.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    },
                    supportingContent = {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Vol ${preset.volumeIndex}/60 ($dbText) • $gainText",
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurfaceVariant
                            )
                            Text(
                                text = "$filterText • Balance ${Cs43198Spec.formatBalanceDb(preset.balanceBaseValue)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurfaceVariant
                            )
                        }
                    },
                    trailingContent = {
                        if (isSelectionMode) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    selectedItems = if (checked) selectedItems + preset else selectedItems - preset
                                }
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = { onExportClicked(preset) },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.FileUpload,
                                        contentDescription = "Export ${preset.name}",
                                        tint = cs.onSurfaceVariant
                                    )
                                }
                                FilledTonalButton(
                                    onClick = { onPresetSelected(preset) },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = if (isActive) "Active" else "Apply",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun PresetActionHeader(
    uiState: DacUiState,
    isSelectionMode: Boolean,
    selectedCount: Int,
    onClearSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
    onExportSelected: () -> Unit,
    onNewClicked: () -> Unit,
    onImportClicked: () -> Unit,
    onExportCurrentClicked: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = expressiveSurfaceColor(),
            contentColor = cs.onSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isSelectionMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$selectedCount Selected",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = cs.onSurface
                    )
                    TextButton(onClick = onClearSelection) {
                        Text("Cancel", color = cs.primary)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onDeleteSelected,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete")
                    }
                    if (selectedCount == 1) {
                        OutlinedButton(
                            onClick = onExportSelected,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FileUpload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export JSON")
                        }
                    }
                }
            } else {
                val currentDb = Cs43198Spec.formatDbFs(uiState.rawVolumeReg)
                val currentGain = if (uiState.isHighGain) "High Gain (2Vrms)" else "Low Gain (1Vrms)"
                Text(
                    text = "Current Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onSurface
                )
                Text(
                    text = "Vol ${uiState.volumeIndex}/60 ($currentDb) • $currentGain • ${filterShortLabels.getOrElse(uiState.digitalFilterPos) { "Filter 0" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onNewClicked,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Preset")
                    }
                    FilledTonalButton(
                        onClick = onImportClicked
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FileDownload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import")
                    }
                    OutlinedButton(
                        onClick = onExportCurrentClicked
                    ) {
                        Text("Export")
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyPresetStateCard(
    onNewClicked: () -> Unit,
    onImportClicked: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = expressiveSurfaceColor(),
            contentColor = cs.onSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.GraphicEq,
                contentDescription = null,
                tint = cs.primary,
                modifier = Modifier.size(44.dp)
            )
            Text(
                text = "No Saved Presets",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = cs.onSurface
            )
            Text(
                text = "Save your current volume, balance, gain, and filter settings, or import a JSON file.",
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(onClick = onNewClicked) {
                    Text("Save Current State")
                }
                OutlinedButton(onClick = onImportClicked) {
                    Text("Import JSON")
                }
            }
        }
    }
}

@Composable
fun NewPresetInputDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    var presetName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = cs.surfaceContainerHigh,
        titleContentColor = cs.onSurface,
        textContentColor = cs.onSurfaceVariant,
        title = { Text("Save Preset", color = cs.onSurface) },
        text = {
            OutlinedTextField(
                value = presetName,
                onValueChange = { presetName = it },
                label = { Text("Preset Name") },
                placeholder = { Text("e.g. IEM Low Gain") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (presetName.isNotBlank()) onSave(presetName.trim())
                },
                enabled = presetName.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = cs.primary)
            }
        }
    )
}
