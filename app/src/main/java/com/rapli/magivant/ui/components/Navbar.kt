package com.rapli.magivant.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class MagivantTab(
    val label: String,
    val iconSelected: ImageVector,
    val iconNotSelected: ImageVector
) {
    HOME("DAC", Icons.Filled.GraphicEq, Icons.Outlined.GraphicEq),
    PRESETS("Presets", Icons.Filled.Tune, Icons.Outlined.Tune),
    SETTINGS("Settings", Icons.Filled.Palette, Icons.Outlined.Palette)
}

@Composable
fun MagivantBottomBar(
    currentTab: MagivantTab,
    onTabSelected: (MagivantTab) -> Unit,
    isFloating: Boolean,
    isFloatingGestureEnabled: Boolean
) {
    val visibleTabs = MagivantTab.entries
    val latestOnTabSelected by rememberUpdatedState(onTabSelected)

    val insets = if (isFloating) {
        WindowInsets(0, 0, 0, 0)
    } else {
        WindowInsets.systemBars.union(WindowInsets.displayCutout).only(
            WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isFloating) Modifier.windowInsetsPadding(WindowInsets.navigationBars) else Modifier),
        contentAlignment = Alignment.BottomCenter
    ) {
        if (isFloating) {
            val enableGesture = isFloatingGestureEnabled
            val indicatorBounds = remember { mutableStateMapOf<Int, Rect>() }
            var selectedIndex by remember { mutableIntStateOf(0) }

            var isGestureActive by remember { mutableStateOf(false) }
            var dragPositionX by remember { mutableFloatStateOf(0f) }
            var rawDragX by remember { mutableFloatStateOf(0f) }
            var hoveredIndex by remember { mutableIntStateOf(-1) }

            visibleTabs.forEachIndexed { index, destination ->
                if (destination == currentTab) {
                    selectedIndex = index
                }
            }

            val density = LocalDensity.current
            val maxShiftPx = with(density) { 10.dp.toPx() }
            val firstTab = indicatorBounds[0]
            val lastTab = indicatorBounds[visibleTabs.size - 1]
            val totalTabsWidth = if (firstTab != null && lastTab != null) lastTab.right - firstTab.left else 0f
            val centerTabsX = if (firstTab != null && lastTab != null) (firstTab.left + lastTab.right) / 2f else 0f

            val targetNavbarOffset = if (isGestureActive && enableGesture && totalTabsWidth > 0f) {
                val progress = ((rawDragX - centerTabsX) / (totalTabsWidth / 2f)).coerceIn(-1.25f, 1.25f)
                progress * maxShiftPx
            } else {
                0f
            }

            val animatedNavbarOffset by animateFloatAsState(
                targetValue = targetNavbarOffset,
                animationSpec = spring(
                    dampingRatio = 0.72f,
                    stiffness = 320f
                ),
                label = "navbarOffset"
            )

            Surface(
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .graphicsLayer {
                        translationX = animatedNavbarOffset
                    },
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                tonalElevation = 0.dp
            ) {
                val radiusPx = with(density) { 24.dp.toPx() }
                val activeRect = indicatorBounds[selectedIndex] ?: Rect.Zero

                val targetLeft = if (isGestureActive) dragPositionX - radiusPx else activeRect.left
                val targetRight = if (isGestureActive) dragPositionX + radiusPx else activeRect.right

                val animatedLeft by animateFloatAsState(
                    targetValue = targetLeft,
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = 300f),
                    label = "indicatorLeft"
                )
                val animatedRight by animateFloatAsState(
                    targetValue = targetRight,
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = 300f),
                    label = "indicatorRight"
                )
                val pillColor = MaterialTheme.colorScheme.primaryContainer

                Row(
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .height(48.dp)
                        .drawBehind {
                            if (enableGesture && animatedRight > animatedLeft) {
                                drawRoundRect(
                                    color = pillColor,
                                    topLeft = Offset(animatedLeft, 0f),
                                    size = Size(animatedRight - animatedLeft, size.height),
                                    cornerRadius = CornerRadius(size.height / 2, size.height / 2)
                                )
                            }
                        }
                        .pointerInput(enableGesture) {
                            if (enableGesture) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        isGestureActive = true
                                        rawDragX = offset.x
                                        dragPositionX = offset.x
                                        hoveredIndex = selectedIndex
                                    },
                                    onDragEnd = {
                                        isGestureActive = false
                                        rawDragX = 0f
                                        if (hoveredIndex != -1 && hoveredIndex != selectedIndex) {
                                            val dest = visibleTabs[hoveredIndex]
                                            latestOnTabSelected(dest)
                                        }
                                        hoveredIndex = -1
                                    },
                                    onDragCancel = {
                                        isGestureActive = false
                                        rawDragX = 0f
                                        hoveredIndex = -1
                                    },
                                    onDrag = { change, _ ->
                                        rawDragX = change.position.x
                                        val minX = indicatorBounds[0]?.left ?: 0f
                                        val maxX = indicatorBounds[visibleTabs.size - 1]?.right ?: size.width.toFloat()
                                        dragPositionX = rawDragX.coerceIn(minX, maxX)

                                        val newTarget = indicatorBounds.entries.find {
                                            it.value.left <= dragPositionX && it.value.right >= dragPositionX
                                        }?.key
                                        if (newTarget != null) {
                                            hoveredIndex = newTarget
                                        }
                                    }
                                )
                            }
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    visibleTabs.forEachIndexed { index, destination ->
                        val isCurrent = destination == currentTab

                        val isHovered = isGestureActive && hoveredIndex == index
                        val isActive = (!isGestureActive && isCurrent) || isHovered

                        val animationSpec = tween<Color>(durationMillis = 300, easing = FastOutSlowInEasing)

                        val bgColor by animateColorAsState(
                            targetValue = if (!enableGesture && isActive) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                Color.Transparent
                            },
                            animationSpec = animationSpec,
                            label = "bgColor"
                        )

                        val contentColor by animateColorAsState(
                            targetValue = if (isActive) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            animationSpec = animationSpec,
                            label = "contentColor"
                        )

                        val interactionSource = remember { MutableInteractionSource() }

                        Row(
                            modifier = Modifier
                                .fillMaxHeight()
                                .onPlaced { coords ->
                                    val x = coords.positionInParent().x
                                    val width = coords.size.width.toFloat()
                                    indicatorBounds[index] = Rect(x, 0f, x + width, 0f)
                                }
                                .clip(RoundedCornerShape(50))
                                .background(bgColor)
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = if (enableGesture || isGestureActive) null else LocalIndication.current
                                ) {
                                    if (!isCurrent) {
                                        latestOnTabSelected(destination)
                                    }
                                }
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isActive) destination.iconSelected else destination.iconNotSelected,
                                contentDescription = destination.label,
                                tint = contentColor
                            )

                            AnimatedVisibility(
                                visible = isCurrent && !isGestureActive,
                                enter = expandHorizontally(
                                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                                    expandFrom = Alignment.Start
                                ) + fadeIn(animationSpec = tween(300)),
                                exit = shrinkHorizontally(
                                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                                    shrinkTowards = Alignment.Start
                                ) + fadeOut(animationSpec = tween(300))
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
                        onClick = {
                            if (!isCurrent) {
                                latestOnTabSelected(destination)
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (isCurrent) destination.iconSelected else destination.iconNotSelected,
                                contentDescription = destination.label
                            )
                        },
                        label = {
                            Text(
                                text = destination.label,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        alwaysShowLabel = false
                    )
                }
            }
        }
    }
}
