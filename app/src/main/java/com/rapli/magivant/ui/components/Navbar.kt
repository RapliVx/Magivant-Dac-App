package com.rapli.magivant.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class MagivantTab(
    val label: String,
    val iconSelected: ImageVector,
    val iconNotSelected: ImageVector
) {
    HOME("DAC", Icons.Filled.GraphicEq, Icons.Outlined.GraphicEq),
    PRESETS("Presets", Icons.Filled.Tune, Icons.Outlined.Tune),
    SETTINGS("Settings", Icons.Filled.Palette, Icons.Outlined.Palette)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MagivantBottomBar(
    currentTab: MagivantTab,
    onTabSelected: (MagivantTab) -> Unit,
    isFloating: Boolean,
    isFloatingGestureEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val visibleTabs = MagivantTab.entries
    val latestOnTabSelected by rememberUpdatedState(onTabSelected)
    val motion = MaterialTheme.motionScheme
    val effectsSpec = motion.fastEffectsSpec<Float>()
    val colorSpec = motion.fastEffectsSpec<Color>()
    val sizeSpec = motion.fastSpatialSpec<IntSize>()
    val boundsSpec = motion.fastSpatialSpec<Rect>()
    val offsetSpec = motion.fastSpatialSpec<Float>()
    val insets = if (isFloating) {
        WindowInsets(0, 0, 0, 0)
    } else {
        WindowInsets.systemBars.union(WindowInsets.displayCutout).only(
            WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(if (isFloating) Modifier.windowInsetsPadding(WindowInsets.navigationBars) else Modifier),
        contentAlignment = Alignment.BottomCenter
    ) {
        if (isFloating) {
            val indicatorBounds = remember { mutableStateMapOf<Int, Rect>() }
            val selectedIndex = visibleTabs.indexOf(currentTab)
            val activeRect = indicatorBounds[selectedIndex] ?: Rect.Zero
            val latestActiveRect by rememberUpdatedState(activeRect)
            val indicator = remember { Animatable(Rect.Zero, Rect.VectorConverter) }
            val navbarOffset = remember { Animatable(0f) }
            val velocityTracker = remember { VelocityTracker() }
            var isGestureActive by remember { mutableStateOf(false) }
            var dragPositionX by remember { mutableFloatStateOf(0f) }
            var hoveredIndex by remember { mutableIntStateOf(-1) }
            var releasedRect by remember { mutableStateOf<Rect?>(null) }
            var releasedOffset by remember { mutableFloatStateOf(0f) }
            var releasedVelocity by remember { mutableFloatStateOf(0f) }
            val density = LocalDensity.current
            val maxShiftPx = with(density) { 10.dp.toPx() }

            fun dragBounds(): Rect {
                val minimum = indicatorBounds.values.minOfOrNull { it.left } ?: 0f
                val maximum = indicatorBounds.values.maxOfOrNull { it.right } ?: minimum
                val width = latestActiveRect.width.coerceIn(0f, maximum - minimum)
                val center = dragPositionX.coerceIn(minimum + width / 2, maximum - width / 2)
                return Rect(center - width / 2, 0f, center + width / 2, 1f)
            }

            fun dragOffset(): Float {
                val minimum = indicatorBounds.values.minOfOrNull { it.left } ?: return 0f
                val maximum = indicatorBounds.values.maxOfOrNull { it.right } ?: return 0f
                val halfWidth = (maximum - minimum) / 2
                if (halfWidth <= 0f) return 0f
                return ((dragPositionX - (minimum + maximum) / 2) / halfWidth)
                    .coerceIn(-1f, 1f) * maxShiftPx
            }

            fun finishDrag(commit: Boolean) {
                if (!isGestureActive) return
                releasedRect = dragBounds()
                releasedOffset = dragOffset()
                val maximumVelocity = (indicatorBounds.values.maxOfOrNull { it.right } ?: 0f) * 3
                releasedVelocity = if (commit) {
                    velocityTracker.calculateVelocity().x.coerceIn(-maximumVelocity, maximumVelocity)
                } else {
                    0f
                }
                val destination = hoveredIndex
                isGestureActive = false
                hoveredIndex = -1
                if (commit && destination in visibleTabs.indices && destination != selectedIndex) {
                    latestOnTabSelected(visibleTabs[destination])
                }
            }

            LaunchedEffect(activeRect, isGestureActive, isFloatingGestureEnabled) {
                if (isGestureActive || activeRect.width <= 0f) return@LaunchedEffect
                val start = releasedRect
                val velocity = releasedVelocity
                releasedRect = null
                if (start != null) {
                    indicator.snapTo(start)
                    navbarOffset.snapTo(releasedOffset)
                } else if (indicator.value.width <= 0f) {
                    indicator.snapTo(activeRect)
                }
                coroutineScope {
                    launch { navbarOffset.animateTo(0f, offsetSpec) }
                    if (start != null) {
                        indicator.animateTo(activeRect, boundsSpec, initialVelocity = Rect(velocity, 0f, velocity, 0f))
                    } else {
                        indicator.animateTo(activeRect, boundsSpec)
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .graphicsLayer {
                        translationX = if (isGestureActive) dragOffset() else navbarOffset.value
                    },
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                tonalElevation = 0.dp
            ) {
                val pillColor = MaterialTheme.colorScheme.primaryContainer
                Row(
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .height(48.dp)
                        .drawBehind {
                            val bounds = if (isGestureActive) dragBounds() else indicator.value
                            val left = bounds.left.coerceIn(0f, size.width)
                            val right = bounds.right.coerceIn(left, size.width)
                            if (right > left) {
                                drawRoundRect(
                                    color = pillColor,
                                    topLeft = androidx.compose.ui.geometry.Offset(left, 0f),
                                    size = Size(right - left, size.height),
                                    cornerRadius = CornerRadius(size.height / 2, size.height / 2)
                                )
                            }
                        }
                        .pointerInput(isFloatingGestureEnabled, currentTab, maxShiftPx) {
                            if (isFloatingGestureEnabled) {
                                detectHorizontalDragGestures(
                                    onDragStart = { position ->
                                        velocityTracker.resetTracking()
                                        dragPositionX = position.x
                                        hoveredIndex = selectedIndex
                                        isGestureActive = true
                                    },
                                    onDragEnd = { finishDrag(commit = true) },
                                    onDragCancel = { finishDrag(commit = false) },
                                    onHorizontalDrag = { change, _ ->
                                        change.consume()
                                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                                        dragPositionX = change.position.x
                                        hoveredIndex = indicatorBounds.minByOrNull {
                                            abs(it.value.center.x - dragPositionX)
                                        }?.key ?: selectedIndex
                                    }
                                )
                            }
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    visibleTabs.forEachIndexed { index, destination ->
                        val isCurrent = destination == currentTab
                        val isActive = if (isGestureActive) hoveredIndex == index else isCurrent
                        val contentColor by animateColorAsState(
                            targetValue = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            animationSpec = colorSpec,
                            label = "navigationContent"
                        )
                        val interactionSource = remember { MutableInteractionSource() }
                        Row(
                            modifier = Modifier
                                .fillMaxHeight()
                                .onPlaced { coordinates ->
                                    val left = coordinates.positionInParent().x
                                    val bounds = Rect(left, 0f, left + coordinates.size.width, 1f)
                                    if (indicatorBounds[index] != bounds) indicatorBounds[index] = bounds
                                }
                                .clip(RoundedCornerShape(50))
                                .selectable(
                                    selected = isCurrent,
                                    role = Role.Tab,
                                    interactionSource = interactionSource,
                                    indication = if (isFloatingGestureEnabled) null else LocalIndication.current,
                                    onClick = { if (!isCurrent) latestOnTabSelected(destination) }
                                )
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isActive) destination.iconSelected else destination.iconNotSelected,
                                contentDescription = destination.label,
                                tint = contentColor
                            )
                            // Keep hit targets stable while the finger is down.
                            AnimatedVisibility(
                                visible = isCurrent,
                                enter = expandHorizontally(sizeSpec, expandFrom = Alignment.Start) + fadeIn(effectsSpec),
                                exit = shrinkHorizontally(sizeSpec, shrinkTowards = Alignment.Start) + fadeOut(effectsSpec)
                            ) {
                                Text(
                                    text = destination.label,
                                    color = contentColor,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            NavigationBar(
                windowInsets = insets,
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                tonalElevation = 0.dp
            ) {
                visibleTabs.forEach { destination ->
                    val isCurrent = destination == currentTab
                    NavigationBarItem(
                        selected = isCurrent,
                        onClick = { if (!isCurrent) latestOnTabSelected(destination) },
                        icon = {
                            Icon(
                                imageVector = if (isCurrent) destination.iconSelected else destination.iconNotSelected,
                                contentDescription = destination.label
                            )
                        },
                        label = {
                            Text(destination.label, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium)
                        },
                        alwaysShowLabel = false
                    )
                }
            }
        }
    }
}
