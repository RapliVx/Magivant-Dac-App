package com.rapli.magivant

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.rapli.magivant.ui.InterfaceScaleItem
import com.rapli.magivant.ui.theme.AppSettings
import com.rapli.magivant.ui.theme.ColorMode
import com.rapli.magivant.ui.theme.MAGIVANTDACTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
class InterfaceScaleTest {
    @get:Rule
    val compose = createComposeRule()

    private val scale = mutableFloatStateOf(1f)
    private val width = mutableStateOf(240.dp)
    private val fontScale = mutableFloatStateOf(1f)
    private val appliedScales = mutableListOf<Float>()

    @Test
    fun drag_keepsNarrowLayoutStableAndAppliesOnceOnRelease() {
        setControl()
        for ((viewportWidth, textScale) in listOf(240.dp to 1.5f, 320.dp to 1f)) {
            compose.runOnIdle {
                width.value = viewportWidth
                fontScale.floatValue = textScale
                scale.floatValue = 1f
                appliedScales.clear()
            }
            val slider = compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))
            val initialBounds = slider.fetchSemanticsNode().boundsInRoot
            slider.performTouchInput { down(center) }
            for (fraction in listOf(0.85f, 0.5f, 0.15f, 1f)) {
                slider.performTouchInput {
                    moveTo(Offset((width * fraction).coerceAtMost(width - 1f), centerY), delayMillis = 160)
                }
                compose.runOnIdle {
                    assertEquals(1f, scale.floatValue)
                    assertTrue("Density must not change while dragging", appliedScales.isEmpty())
                }
                val bounds = slider.fetchSemanticsNode().boundsInRoot
                assertEquals(initialBounds.left, bounds.left, 1f)
                assertEquals(initialBounds.width, bounds.width, 1f)
                compose.onNodeWithText("Apply").assertDoesNotExist()
            }
            slider.performTouchInput { up() }
            compose.runOnIdle { assertEquals(listOf(1.25f), appliedScales) }
            compose.onNodeWithText("125%").assertExists()
        }
    }

    @Test
    fun accessibilityAndKeyboard_applyAutomaticallyWithoutWritingExternalChangesBack() {
        setControl()
        val slider = compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))
        slider.performSemanticsAction(SemanticsActions.SetProgress) { it(1.1f) }
        compose.onNodeWithText("110%").assertExists()
        compose.runOnIdle { assertEquals(listOf(1.1f), appliedScales) }

        slider.performSemanticsAction(SemanticsActions.RequestFocus) { assertTrue(it()) }
        slider.performKeyInput { pressKey(Key.DirectionRight) }
        compose.onNodeWithText("115%").assertExists()
        compose.runOnIdle { assertEquals(listOf(1.1f, 1.15f), appliedScales) }

        compose.runOnIdle { scale.floatValue = 0.75f }
        compose.onNodeWithText("75%").assertExists()
        compose.runOnIdle { scale.floatValue = 1.25f }
        compose.onNodeWithText("125%").assertExists()
        compose.runOnIdle { assertEquals(listOf(1.1f, 1.15f), appliedScales) }
        compose.onNodeWithText("Apply").assertDoesNotExist()
    }

    private fun setControl() {
        compose.setContent {
            MAGIVANTDACTheme(AppSettings(colorMode = ColorMode.LIGHT)) {
                val density = LocalDensity.current
                // Match MainActivity: physical bounds stay fixed while app density changes.
                Box(Modifier.width(width.value)) {
                    CompositionLocalProvider(
                        LocalDensity provides Density(density.density * scale.floatValue, fontScale.floatValue)
                    ) {
                        InterfaceScaleItem(scale.floatValue) {
                            appliedScales += it
                            scale.floatValue = it
                        }
                    }
                }
            }
        }
    }
}
