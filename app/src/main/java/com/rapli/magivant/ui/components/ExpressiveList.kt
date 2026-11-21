package com.rapli.magivant.ui.components

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private val largeCorner = 20.dp
private val smallCorner = 4.dp

private val topShape = RoundedCornerShape(
    topStart = largeCorner,
    topEnd = largeCorner,
    bottomStart = smallCorner,
    bottomEnd = smallCorner
)
private val middleShape = RoundedCornerShape(smallCorner)
private val bottomShape = RoundedCornerShape(
    topStart = smallCorner,
    topEnd = smallCorner,
    bottomStart = largeCorner,
    bottomEnd = largeCorner
)
private val singleShape = RoundedCornerShape(largeCorner)

@Composable
fun expressiveSurfaceColor(): Color {
    return MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
}

@Composable
fun TonalCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
    shape: Shape = RoundedCornerShape(20.dp),
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = shape
    ) {
        content()
    }
}

// List Composable
@Composable
fun ExpressiveList(
    modifier: Modifier = Modifier,
    title: String = "",
    content: List<@Composable () -> Unit>,
) {
    if (content.isEmpty()) return

    Column(modifier = modifier) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
            )
        }
        Column(
            modifier = Modifier.clip(
                if (content.size == 1) singleShape else RoundedCornerShape(largeCorner)
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
                Column(
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp), shape)
                ) {
                    itemContent()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> ExpressiveLazyList(
    modifier: Modifier = Modifier,
    items: List<T>,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    title: String = "",
    key: ((item: T) -> Any)? = null,
    header: (@Composable () -> Unit)? = null,
    itemContent: @Composable (index: Int, item: T) -> Unit
) {
    val motion = MaterialTheme.motionScheme
    Column(modifier = modifier) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
            )
        }
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = contentPadding
        ) {
            if (header != null) {
                item {
                    Box(modifier = Modifier.padding(bottom = 6.dp)) {
                        header()
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
                    index == items.lastIndex -> bottomShape
                    else -> middleShape
                }
                Column(
                    modifier = Modifier
                        .animateItem(
                            fadeInSpec = motion.fastEffectsSpec(),
                            placementSpec = motion.defaultSpatialSpec(),
                            fadeOutSpec = motion.fastEffectsSpec()
                        )
                        .clip(shape)
                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                ) {
                    itemContent(index, item)
                }
            }
        }
    }
}

// Item Composable
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpressiveListItem(
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    headlineContent: @Composable () -> Unit,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
    supportingContent: @Composable (() -> Unit)? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let {
                if (onClick != null || onLongClick != null) {
                    it.combinedClickable(onClick = onClick ?: {}, onLongClick = onLongClick)
                } else {
                    it
                }
            }
            .then(modifier)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingContent != null) {
            Box(
                modifier = Modifier.padding(end = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                leadingContent()
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp)
        ) {
            headlineContent()
            if (supportingContent != null) {
                CompositionLocalProvider(
                    LocalContentColor provides MaterialTheme.colorScheme.outline
                ) {
                    ProvideTextStyle(value = MaterialTheme.typography.bodySmall) {
                        supportingContent()
                    }
                }
            }
        }
        if (trailingContent != null) {
            Box(
                modifier = Modifier.padding(start = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                ProvideTextStyle(value = MaterialTheme.typography.bodySmall) {
                    trailingContent()
                }
            }
        }
    }
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
    val interactionSource = remember { MutableInteractionSource() }

    ExpressiveListItem(
        modifier = Modifier.toggleable(
            value = checked,
            interactionSource = interactionSource,
            role = Role.Switch,
            enabled = enabled,
            indication = LocalIndication.current,
            onValueChange = onCheckedChange
        ),
        headlineContent = { Text(title) },
        leadingContent = icon?.let { { Icon(it, title) } },
        trailingContent = {
            Switch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = null,
                interactionSource = interactionSource
            )
        },
        supportingContent = summary?.let { { Text(it) } }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
    var expanded by remember { mutableStateOf(false) }
    var anchorWidth by remember { mutableIntStateOf(0) }
    val cs = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val density = LocalDensity.current
    val scrollState = rememberScrollState()
    val selectedItemRequester = remember { BringIntoViewRequester() }
    val hasItems = items.isNotEmpty()
    val canOpen = enabled && hasItems
    val isExpanded = expanded && canOpen
    val selectedLabel = items.getOrNull(selectedIndex) ?: if (hasItems) "Choose an option" else "No options available"
    val contentColor = if (canOpen) cs.onSurface else cs.onSurface.copy(alpha = 0.38f)
    val supportingColor = if (canOpen) cs.onSurfaceVariant else cs.onSurface.copy(alpha = 0.38f)
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = motion.fastSpatialSpec(),
        label = "dropdownChevron"
    )
    val menuWidth = with(density) { anchorWidth.toDp() }.coerceAtMost(360.dp)

    LaunchedEffect(canOpen) {
        if (!canOpen) expanded = false
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { anchorWidth = it.width }
    ) {
        ExpressiveListItem(
            modifier = Modifier
                .clickable(enabled = canOpen, role = Role.Button) { expanded = !expanded }
                .semantics { stateDescription = if (isExpanded) "Expanded" else "Collapsed" },
            leadingContent = icon?.let {
                { Icon(imageVector = it, contentDescription = null, tint = supportingColor) }
            },
            headlineContent = { Text(text = title, color = contentColor) },
            supportingContent = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    AnimatedContent(
                        targetState = selectedLabel,
                        transitionSpec = {
                            (fadeIn(motion.fastEffectsSpec()) togetherWith fadeOut(motion.fastEffectsSpec()))
                                .using(SizeTransform(sizeAnimationSpec = { _, _ -> motion.fastSpatialSpec() }))
                        },
                        label = "dropdownSelection"
                    ) { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (canOpen) cs.primary else supportingColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    summary?.let { Text(text = it, color = supportingColor) }
                }
            },
            trailingContent = {
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = supportingColor,
                    modifier = Modifier.graphicsLayer { rotationZ = chevronRotation }
                )
            }
        )

        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(menuWidth).heightIn(max = 360.dp),
            scrollState = scrollState,
            shape = MenuDefaults.standaloneGroupShape,
            containerColor = cs.surfaceContainerHigh,
            tonalElevation = 0.dp,
            shadowElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items.forEachIndexed { index, text ->
                    val isSelected = index == selectedIndex
                    if (isSelected) {
                        LaunchedEffect(isExpanded, index) {
                            if (isExpanded) {
                                withFrameNanos { }
                                selectedItemRequester.bringIntoView()
                            }
                        }
                    }
                    SelectableDropdownMenuItem(
                        selected = isSelected,
                        onClick = {
                            expanded = false
                            if (canOpen && index in items.indices && index != selectedIndex) onItemSelected(index)
                        },
                        text = { Text(text) },
                        shapes = MenuDefaults.itemShape(index, items.size),
                        modifier = if (isSelected) Modifier.bringIntoViewRequester(selectedItemRequester) else Modifier,
                        enabled = canOpen,
                        leadingIcon = { Spacer(Modifier.size(MenuDefaults.LeadingIconSize)) },
                        selectedLeadingIcon = {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(MenuDefaults.LeadingIconSize))
                        },
                        supportingText = itemDescriptions?.getOrNull(index)?.let { description ->
                            { Text(description) }
                        },
                        colors = MenuDefaults.selectableItemColors(
                            containerColor = Color.Transparent,
                            selectedContainerColor = cs.secondaryContainer,
                            selectedTextColor = cs.onSecondaryContainer,
                            selectedLeadingIconColor = cs.onSecondaryContainer
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun ExpressiveRadioItem(
    title: String,
    summary: String? = null,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    ExpressiveListItem(
        modifier = Modifier.selectable(
            selected = selected,
            onClick = onClick,
            enabled = enabled,
            role = Role.RadioButton
        ),
        headlineContent = { Text(title) },
        leadingContent = {
            RadioButton(
                selected = selected,
                onClick = null,
                enabled = enabled
            )
        },
        supportingContent = summary?.let { { Text(it) } }
    )
}

@Composable
fun ExpressiveCheckboxItem(
    title: String,
    summary: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    ExpressiveListItem(
        modifier = Modifier.toggleable(
            value = checked,
            interactionSource = interactionSource,
            role = Role.Checkbox,
            enabled = enabled,
            indication = LocalIndication.current,
            onValueChange = onCheckedChange
        ),
        headlineContent = { Text(title) },
        leadingContent = {
            Checkbox(
                checked = checked,
                enabled = enabled,
                onCheckedChange = null,
                interactionSource = interactionSource,
                modifier = Modifier.size(24.dp)
            )
        },
        supportingContent = summary?.let { { Text(it) } }
    )
}
