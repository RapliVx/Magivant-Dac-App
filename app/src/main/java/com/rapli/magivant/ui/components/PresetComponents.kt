package com.rapli.magivant.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SnippetFolder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rapli.magivant.ui.DacPreset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetListDialog(
    presetList: List<DacPreset>,
    onDismiss: () -> Unit,
    onPresetSelected: (DacPreset) -> Unit,
    onNewClicked: () -> Unit,
    onImportClicked: () -> Unit,
    onExportClicked: (DacPreset?) -> Unit,
    onDeleteClicked: (List<DacPreset>) -> Unit
) {
    var selectedItems by remember { mutableStateOf(setOf<DacPreset>()) }
    val isSelectionMode = selectedItems.isNotEmpty()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isSelectionMode) "${selectedItems.size} Selected" else "Presets",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (isSelectionMode) {
                        TextButton(onClick = { selectedItems = emptySet() }) {
                            Text("Cancel", color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        TextButton(onClick = onImportClicked) {
                            Text("Import", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = presetList,
                        key = { preset -> preset.name }
                    ) { preset ->
                        val isSelected = selectedItems.contains(preset)
                        PresetItemCard(
                            preset = preset,
                            isSelected = isSelected,
                            isSelectionMode = isSelectionMode,
                            onClick = {
                                if (isSelectionMode) {
                                    selectedItems = if (isSelected) selectedItems - preset else selectedItems + preset
                                } else {
                                    onPresetSelected(preset)
                                }
                            },
                            onLongClick = {
                                if (!isSelectionMode) {
                                    selectedItems = setOf(preset)
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSelectionMode) {
                        TextButton(
                            onClick = {
                                onDeleteClicked(selectedItems.toList())
                                selectedItems = emptySet()
                            }
                        ) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }

                        if (selectedItems.size == 1) {
                            TextButton(onClick = {
                                onExportClicked(selectedItems.first())
                                selectedItems = emptySet()
                            }) {
                                Text("Export", color = MaterialTheme.colorScheme.primary)
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = { onExportClicked(null) }) {
                                Text("Export Current", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = onNewClicked) {
                                Text("New", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        TextButton(onClick = onDismiss) {
                            Text("Close", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PresetItemCard(
    preset: DacPreset,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.SnippetFolder,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = preset.name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = null
                )
            }
        }
    }
}

@Composable
fun NewPresetInputDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var presetName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save New Preset") },
        text = {
            OutlinedTextField(
                value = presetName,
                onValueChange = { presetName = it },
                label = { Text("Preset Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (presetName.isNotBlank()) onSave(presetName)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}