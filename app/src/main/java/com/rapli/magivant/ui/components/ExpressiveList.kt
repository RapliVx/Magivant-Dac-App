package com.rapli.magivant.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

private val largeRadius = 20.dp
private val smallRadius = 4.dp

private val topShape = RoundedCornerShape(
    topStart = largeRadius,
    topEnd = largeRadius,
    bottomStart = smallRadius,
    bottomEnd = smallRadius
)
private val middleShape = RoundedCornerShape(smallRadius)
private val bottomShape = RoundedCornerShape(
    topStart = smallRadius,
    topEnd = smallRadius,
    bottomStart = largeRadius,
    bottomEnd = largeRadius
)
private val singleShape = RoundedCornerShape(largeRadius)

// Uses tinted surfaceContainer in Light Mode so segmented cards have clear pastel color separation against the light background.
@Composable
fun expressiveSurfaceColor(): Color {
    val cs = MaterialTheme.colorScheme
    return if (cs.background.luminance() > 0.5f) {
        cs.surfaceContainer
    } else {
        cs.surfaceColorAtElevation(1.dp)
    }
}

@Composable
fun ExpressiveList(
    modifier: Modifier = Modifier,
    title: String = "",
    content: List<@Composable () -> Unit>
) {
    if (content.isEmpty()) return

    val containerColor = expressiveSurfaceColor()
    val contentColor = MaterialTheme.colorScheme.onSurface

    Column(modifier = modifier) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp, top = 4.dp)
            )
        }

        Column(
            modifier = Modifier.clip(
                if (content.size == 1) singleShape else RoundedCornerShape(largeRadius)
            ),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            content.forEachIndexed { index, itemContent ->
                val shape = when {
                    content.size == 1 -> singleShape
                    index == 0 -> topShape
                    index == content.size - 1 -> bottomShape
                    else -> middleShape
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(containerColor)
                ) {
                    CompositionLocalProvider(LocalContentColor provides contentColor) {
                        itemContent()
                    }
                }
            }
        }
    }
}

@Composable
fun <T> ExpressiveLazyList(
    modifier: Modifier = Modifier,
    items: List<T>,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    key: ((item: T) -> Any)? = null,
    header: (@Composable () -> Unit)? = null,
    itemContent: @Composable (index: Int, item: T) -> Unit
) {
    val containerColor = expressiveSurfaceColor()
    val contentColor = MaterialTheme.colorScheme.onSurface

    LazyColumn(
        state = state,
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (header != null) {
            item {
                Box(modifier = Modifier.padding(bottom = 6.dp)) {
                    CompositionLocalProvider(LocalContentColor provides contentColor) {
                        header()
                    }
                }
            }
        }
        itemsIndexed(
            items = items,
            key = if (key != null) { _, item -> key(item) } else null
        ) { index, item ->
            val shape = when {
                items.size == 1 -> singleShape
                index == 0 -> topShape
                index == items.size - 1 -> bottomShape
                else -> middleShape
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(containerColor)
            ) {
                CompositionLocalProvider(LocalContentColor provides contentColor) {
                    itemContent(index, item)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpressiveColumn(
    modifier: Modifier = Modifier,
    title: String = "",
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: List<@Composable () -> Unit>
) {
    if (content.isEmpty()) return

    val containerColor = expressiveSurfaceColor()
    val contentColor = MaterialTheme.colorScheme.onSurface

    Column(modifier = modifier) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp, top = 4.dp)
            )
        }

        Column(
            modifier = Modifier.clip(
                if (content.size == 1) singleShape else RoundedCornerShape(largeRadius)
            ),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            content.forEachIndexed { index, itemContent ->
                val shape = when {
                    content.size == 1 -> singleShape
                    index == 0 -> topShape
                    index == content.size - 1 -> bottomShape
                    else -> middleShape
                }
                val itemModifier = if (enabled && (onClick != null || onLongClick != null)) {
                    Modifier.combinedClickable(
                        onClick = { onClick?.invoke() },
                        onLongClick = onLongClick
                    )
                } else {
                    Modifier
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(containerColor)
                        .then(itemModifier)
                ) {
                    CompositionLocalProvider(LocalContentColor provides contentColor) {
                        itemContent()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpressiveListItem(
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    headlineContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    supportingContent: (@Composable () -> Unit)? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val cs = MaterialTheme.colorScheme
    val clickModifier = if (onClick != null || onLongClick != null) {
        Modifier.combinedClickable(
            onClick = { onClick?.invoke() },
            onLongClick = onLongClick
        )
    } else {
        Modifier
    }
    ListItem(
        modifier = modifier.then(clickModifier),
        headlineContent = {
            CompositionLocalProvider(LocalContentColor provides cs.onSurface) {
                headlineContent()
            }
        },
        supportingContent = supportingContent?.let {
            {
                CompositionLocalProvider(LocalContentColor provides cs.onSurfaceVariant) {
                    ProvideTextStyle(MaterialTheme.typography.bodySmall) {
                        it()
                    }
                }
            }
        },
        leadingContent = leadingContent?.let {
            {
                CompositionLocalProvider(LocalContentColor provides cs.primary) {
                    it()
                }
            }
        },
        trailingContent = trailingContent?.let {
            {
                CompositionLocalProvider(LocalContentColor provides cs.onSurfaceVariant) {
                    it()
                }
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
            headlineColor = cs.onSurface,
            supportingColor = cs.onSurfaceVariant,
            leadingIconColor = cs.primary,
            trailingIconColor = cs.onSurfaceVariant
        )
    )
}

@Composable
fun ExpressiveSwitchItem(
    icon: ImageVector? = null,
    title: String,
    summary: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }

    ListItem(
        modifier = Modifier.toggleable(
            value = checked,
            interactionSource = interactionSource,
            role = Role.Switch,
            enabled = enabled,
            indication = LocalIndication.current,
            onValueChange = onCheckedChange
        ),
        headlineContent = {
            Text(
                text = title,
                color = if (enabled) cs.onSurface else cs.onSurface.copy(alpha = 0.45f)
            )
        },
        leadingContent = icon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = title,
                    tint = if (enabled) cs.primary else cs.onSurface.copy(alpha = 0.38f)
                )
            }
        },
        trailingContent = {
            Switch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = onCheckedChange,
                interactionSource = interactionSource
            )
        },
        supportingContent = summary?.let {
            {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) cs.onSurfaceVariant else cs.onSurfaceVariant.copy(alpha = 0.45f)
                )
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
            headlineColor = cs.onSurface,
            supportingColor = cs.onSurfaceVariant
        )
    )
}

@Composable
fun ExpressiveRadioItem(
    title: String,
    summary: String? = null,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }

    ListItem(
        modifier = Modifier.toggleable(
            value = selected,
            interactionSource = interactionSource,
            role = Role.RadioButton,
            enabled = enabled,
            indication = LocalIndication.current,
            onValueChange = { onClick() }
        ),
        headlineContent = {
            Text(
                text = title,
                color = cs.onSurface
            )
        },
        leadingContent = {
            RadioButton(
                selected = selected,
                enabled = enabled,
                onClick = onClick,
                interactionSource = interactionSource
            )
        },
        supportingContent = summary?.let {
            {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant
                )
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
            headlineColor = cs.onSurface,
            supportingColor = cs.onSurfaceVariant
        )
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpressiveDropdownItem(
    icon: ImageVector? = null,
    title: String,
    summary: String? = null,
    items: List<String>,
    itemDescriptions: List<String>? = null,
    enabled: Boolean = true,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    var showDialog by remember { mutableStateOf(false) }

    val hasValidSelection = selectedIndex in items.indices
    val currentSelectionText = if (hasValidSelection) items[selectedIndex] else ""

    ListItem(
        modifier = if (enabled) {
            Modifier.combinedClickable(onClick = { showDialog = true })
        } else {
            Modifier
        },
        leadingContent = icon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = title,
                    tint = if (enabled) cs.primary else cs.onSurface.copy(alpha = 0.38f)
                )
            }
        },
        headlineContent = {
            Text(
                text = title,
                color = if (enabled) cs.onSurface else cs.onSurface.copy(alpha = 0.45f)
            )
        },
        supportingContent = summary?.let {
            {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) cs.onSurfaceVariant else cs.onSurfaceVariant.copy(alpha = 0.45f)
                )
            }
        },
        trailingContent = {
            Text(
                text = currentSelectionText,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) cs.primary else cs.onSurfaceVariant.copy(alpha = 0.45f)
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
            headlineColor = cs.onSurface,
            supportingColor = cs.onSurfaceVariant
        )
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            containerColor = cs.surfaceContainerHigh,
            titleContentColor = cs.onSurface,
            textContentColor = cs.onSurfaceVariant,
            title = { Text(text = title, color = cs.onSurface) },
            text = {
                ExpressiveList(
                    content = items.mapIndexed { index, text ->
                        {
                            ExpressiveRadioItem(
                                title = text,
                                summary = itemDescriptions?.getOrNull(index),
                                selected = index == selectedIndex,
                                onClick = {
                                    onItemSelected(index)
                                    showDialog = false
                                }
                            )
                        }
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(text = "Cancel", color = cs.primary)
                }
            }
        )
    }
}

@Composable
fun ExpressiveCheckboxItem(
    title: String,
    summary: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                interactionSource = interactionSource,
                role = Role.Checkbox,
                enabled = enabled,
                indication = LocalIndication.current,
                onValueChange = onCheckedChange
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Checkbox(
            checked = checked,
            enabled = enabled,
            onCheckedChange = null,
            interactionSource = interactionSource,
            modifier = Modifier.size(24.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) cs.onSurface else cs.onSurface.copy(alpha = 0.45f)
            )
            if (summary != null) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled) cs.onSurfaceVariant else cs.onSurfaceVariant.copy(alpha = 0.45f)
                )
            }
        }
    }
}
