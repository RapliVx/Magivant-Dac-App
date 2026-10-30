package com.rapli.magivant.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

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
    val tabs = MagivantTab.entries

    if (isFloating) {
        val coroutineScope = rememberCoroutineScope()
        val dragOffset = remember { Animatable(0f) }
        var rowWidthPx by remember { mutableIntStateOf(0) }
        val latestCurrentTab by rememberUpdatedState(currentTab)
        val latestOnTabSelected by rememberUpdatedState(onTabSelected)

        val cs = MaterialTheme.colorScheme
        val isLight = cs.background.luminance() > 0.5f

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(
                    WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                )
                .padding(bottom = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = dragOffset.value
                    }
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(50),
                        spotColor = cs.scrim.copy(alpha = 0.3f)
                    ),
                shape = RoundedCornerShape(50),
                color = if (isLight) cs.surfaceColorAtElevation(6.dp) else cs.surfaceColorAtElevation(3.dp),
                contentColor = cs.onSurface,
                tonalElevation = if (isLight) 6.dp else 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .onSizeChanged { rowWidthPx = it.width }
                        .then(
                            if (isFloatingGestureEnabled) {
                                Modifier.pointerInput(tabs.size, isFloatingGestureEnabled) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        var overSlop = 0f
                                        val dragStart = awaitHorizontalTouchSlopOrCancellation(down.id) { change, over ->
                                            overSlop = over
                                            change.consume()
                                        }

                                        if (dragStart != null) {
                                            fun selectAt(x: Float) {
                                                if (rowWidthPx <= 0 || tabs.isEmpty()) return
                                                val fraction = (x / rowWidthPx.toFloat()).coerceIn(0f, 0.9999f)
                                                val targetIndex = (fraction * tabs.size).toInt().coerceIn(0, tabs.lastIndex)
                                                val targetTab = tabs[targetIndex]
                                                if (targetTab != latestCurrentTab) {
                                                    latestOnTabSelected(targetTab)
                                                }
                                            }

                                            var accumulatedDx = overSlop
                                            selectAt(dragStart.position.x)

                                            horizontalDrag(dragStart.id) { change ->
                                                val dx = change.positionChange().x
                                                accumulatedDx += dx
                                                val maxShift = 28.dp.toPx()
                                                val dampedShift = (accumulatedDx * 0.25f).coerceIn(-maxShift, maxShift)
                                                coroutineScope.launch {
                                                    dragOffset.snapTo(dampedShift)
                                                }
                                                selectAt(change.position.x)
                                                change.consume()
                                            }

                                            coroutineScope.launch {
                                                dragOffset.animateTo(
                                                    targetValue = 0f,
                                                    animationSpec = spring(
                                                        dampingRatio = 0.72f,
                                                        stiffness = 320f
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                Modifier
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEach { tab ->
                        FloatingNavItem(
                            tab = tab,
                            isSelected = currentTab == tab,
                            onClick = { onTabSelected(tab) }
                        )
                    }
                }
            }
        }
    } else {
        val cs = MaterialTheme.colorScheme
        val isLight = cs.background.luminance() > 0.5f
        NavigationBar(
            modifier = Modifier.windowInsetsPadding(
                WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal)
            ),
            containerColor = if (isLight) cs.surfaceColorAtElevation(4.dp) else cs.surfaceColorAtElevation(2.dp),
            contentColor = cs.onSurface
        ) {
            tabs.forEach { tab ->
                val selected = currentTab == tab
                NavigationBarItem(
                    selected = selected,
                    onClick = { onTabSelected(tab) },
                    icon = {
                        Icon(
                            imageVector = if (selected) tab.iconSelected else tab.iconNotSelected,
                            contentDescription = tab.label
                        )
                    },
                    label = {
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = cs.onSecondaryContainer,
                        selectedTextColor = cs.onSurface,
                        indicatorColor = cs.secondaryContainer,
                        unselectedIconColor = cs.onSurfaceVariant,
                        unselectedTextColor = cs.onSurfaceVariant
                    ),
                    alwaysShowLabel = false
                )
            }
        }
    }
}

@Composable
private fun FloatingNavItem(
    tab: MagivantTab,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        Color.Transparent
    }

    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val horizontalPadding by animateDpAsState(
        targetValue = if (isSelected) 20.dp else 14.dp,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 380f),
        label = "navHorizontalPadding"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = horizontalPadding, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isSelected) tab.iconSelected else tab.iconNotSelected,
                contentDescription = tab.label,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )

            AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn(
                    animationSpec = spring(stiffness = Spring.StiffnessMedium)
                ) + expandHorizontally(
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 380f),
                    expandFrom = Alignment.Start
                ),
                exit = fadeOut(
                    animationSpec = spring(stiffness = Spring.StiffnessMedium)
                ) + shrinkHorizontally(
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f),
                    shrinkTowards = Alignment.Start
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}
