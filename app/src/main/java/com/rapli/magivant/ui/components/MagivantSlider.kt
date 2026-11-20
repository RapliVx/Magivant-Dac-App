package com.rapli.magivant.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@Composable
fun MagivantSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0
) {
    val state = remember(steps, valueRange) { SliderState(value, steps, valueRange) }
    val interactionSource = remember { MutableInteractionSource() }
    val isDragged by interactionSource.collectIsDraggedAsState()
    LaunchedEffect(value, isDragged, state) {
        if (!isDragged) state.value = value
    }
    Slider(
        state = state,
        onValueChange = {
            if (isDragged) state.value = it
            onValueChange(it)
        },
        modifier = modifier,
        enabled = enabled,
        interactionSource = interactionSource
    )
}
