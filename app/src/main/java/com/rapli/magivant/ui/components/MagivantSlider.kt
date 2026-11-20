package com.rapli.magivant.ui.components

import androidx.compose.animation.core.animate
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlin.math.abs

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MagivantSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    isPending: Boolean = false
) {
    val state = remember(steps, valueRange) { SliderState(value, steps, valueRange) }
    val interactionSource = remember { MutableInteractionSource() }
    val isDragged by interactionSource.collectIsDraggedAsState()
    var interactionVersion by remember { mutableIntStateOf(0) }
    val spatialSpec = MaterialTheme.motionScheme.fastSpatialSpec<Float>()

    LaunchedEffect(value, isDragged, isPending, enabled, state, interactionVersion) {
        val target = value.coerceIn(valueRange.start, valueRange.endInclusive)
        if (!enabled) {
            state.value = target
            return@LaunchedEffect
        }
        if (isDragged || isPending) return@LaunchedEffect
        if (abs(state.value - target) < 0.001f) {
            state.value = target
            return@LaunchedEffect
        }
        state.shouldAutoSnap = false
        try {
            // Only move the visual thumb; animation frames never dispatch USB commands.
            animate(state.value, target, animationSpec = spatialSpec) { position, _ ->
                state.value = position.coerceIn(valueRange.start, valueRange.endInclusive)
            }
            state.value = target
        } finally {
            state.shouldAutoSnap = true
        }
    }
    Slider(
        state = state,
        onValueChange = {
            state.value = it
            interactionVersion++
            onValueChange(it)
        },
        modifier = modifier,
        enabled = enabled,
        interactionSource = interactionSource
    )
}
