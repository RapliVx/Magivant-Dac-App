package com.rapli.magivant

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import com.rapli.magivant.ui.components.ExpressiveSwitchItem
import com.rapli.magivant.ui.components.MagivantSlider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ControlInteractionTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun slider_followsExternalStateAndUpdatedLimits() {
        val value = mutableFloatStateOf(36f)
        val max = mutableIntStateOf(60)
        compose.setContent {
            MaterialTheme {
                MagivantSlider(value.floatValue, { value.floatValue = it }, valueRange = 0f..max.intValue.toFloat(), steps = max.intValue - 1)
            }
        }
        compose.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo(36f, 0f..60f, 59))).assertExists()
        compose.runOnIdle { value.floatValue = 20f; max.intValue = 30 }
        compose.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo(20f, 0f..30f, 29))).assertExists()
    }

    @Test
    fun disabledSwitch_doesNotInvokeItsRowCallback() {
        var changes = 0
        compose.setContent {
            MaterialTheme {
                Column {
                    ExpressiveSwitchItem(title = "Gain", checked = false, enabled = false, onCheckedChange = { changes++ })
                }
            }
        }
        compose.onNodeWithText("Gain").performTouchInput { click() }
        compose.runOnIdle { assertEquals(0, changes) }
    }
}
