package com.rapli.magivant

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.rapli.magivant.ui.components.ExpressiveDropdownItem
import com.rapli.magivant.ui.theme.AppSettings
import com.rapli.magivant.ui.theme.ColorMode
import com.rapli.magivant.ui.theme.MAGIVANTDACTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
class DropdownInteractionTest {
    @get:Rule
    val compose = createComposeRule()

    private val options = mutableStateOf(listOf("Follow Sample Rate", "Always Off", "Always Red"))
    private val selected = mutableIntStateOf(1)
    private val enabled = mutableStateOf(true)
    private val direction = mutableStateOf(LayoutDirection.Ltr)
    private val fontScale = mutableFloatStateOf(1f)
    private val viewportWidth = mutableStateOf(320.dp)
    private val changes = mutableListOf<Int>()
    private val menuItem = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)

    @Test
    fun selection_marksTheCurrentItemAndDispatchesOnlyNewChoices() {
        setControl()
        trigger().performClick()
        option("Always Off").assertIsSelected().performClick()
        compose.onNode(isPopup()).assertDoesNotExist()
        compose.runOnIdle { assertTrue(changes.isEmpty()) }

        trigger().performClick()
        option("Always Red").performClick()
        compose.onNode(isPopup()).assertDoesNotExist()
        compose.runOnIdle { assertEquals(listOf(2), changes) }
        trigger().performClick()
        option("Always Red").assertIsSelected()
    }

    @Test
    fun disablingAnOpenControl_closesTheMenuAndDoesNotReopenIt() {
        setControl()
        trigger().performClick()
        compose.onNode(isPopup()).assertExists()
        compose.runOnIdle { enabled.value = false }
        compose.onNode(isPopup()).assertDoesNotExist()
        trigger().assertIsNotEnabled().performTouchInput { click() }
        compose.onNode(isPopup()).assertDoesNotExist()
        compose.runOnIdle { enabled.value = true }
        compose.onNode(isPopup()).assertDoesNotExist()
        compose.runOnIdle { assertTrue(changes.isEmpty()) }
    }

    @Test
    fun removingAllOptions_closesTheMenuAndDisablesTheTrigger() {
        setControl()
        trigger().performClick()
        compose.runOnIdle { options.value = emptyList() }
        compose.onNode(isPopup()).assertDoesNotExist()
        trigger().assertIsNotEnabled().performTouchInput { click() }
        compose.onNodeWithText("No options available", useUnmergedTree = true).assertExists()
        compose.runOnIdle { options.value = listOf("Follow Sample Rate") }
        compose.onNode(isPopup()).assertDoesNotExist()
        compose.runOnIdle { assertTrue(changes.isEmpty()) }
    }

    @Test
    fun invalidSelection_doesNotPretendThatTheFirstOptionIsSelected() {
        selected.intValue = 99
        setControl()
        compose.onNodeWithText("Choose an option", useUnmergedTree = true).assertExists()
        trigger().performClick()
        compose.onAllNodes(menuItem and SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
            .assertCountEquals(0)
        option("Always Red").performClick()
        compose.runOnIdle { assertEquals(listOf(2), changes) }
    }

    @Test
    fun longSelection_staysInsideNarrowLayoutsWithLargeFontsAndRtl() {
        val label = "Keep the current hardware settings for this listening preset"
        options.value = listOf(label, "Balanced IEM", "Desktop Reference")
        selected.intValue = 0
        viewportWidth.value = 240.dp
        fontScale.floatValue = 1.5f
        setControl()

        for (layout in listOf(LayoutDirection.Ltr, LayoutDirection.Rtl)) {
            compose.runOnIdle { direction.value = layout }
            val layouts = mutableListOf<TextLayoutResult>()
            compose.onNodeWithText(label, useUnmergedTree = true)
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertTrue(layouts.single().lineCount <= 2)
            val titleLayouts = mutableListOf<TextLayoutResult>()
            compose.onNodeWithText("Indicator LED", useUnmergedTree = true)
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(titleLayouts) }
            assertFalse(titleLayouts.single().didOverflowWidth)

            val anchor = compose.onNodeWithTag("dropdown").fetchSemanticsNode().boundsInRoot
            saveScreenshot("dropdown-trigger-$layout.png", compose.onNodeWithTag("dropdown").captureToImage().asAndroidBitmap())
            trigger().performClick()
            option(label).assertIsSelected().assertIsDisplayed()
            val popup = compose.onNode(isPopup()).fetchSemanticsNode().boundsInRoot
            assertTrue("Menu must fit the narrow control", popup.width <= anchor.width + 1f)
            saveScreenshot("dropdown-menu-$layout.png", compose.onNode(isPopup()).captureToImage().asAndroidBitmap())
            option(label).performClick()
            compose.onNode(isPopup()).assertDoesNotExist()
        }
    }

    @Test
    fun longMenus_scrollTheSelectedItemIntoViewOnOpen() {
        options.value = List(40) { "Listening preset $it" }
        selected.intValue = 39
        setControl()
        trigger().performClick()
        option("Listening preset 39").assertIsSelected().assertIsDisplayed()
        option("Listening preset 0").performScrollTo().performClick()
        compose.onNode(isPopup()).assertDoesNotExist()
        compose.runOnIdle { assertEquals(listOf(0), changes) }
    }

    private fun trigger() = compose.onNode(hasText("Indicator LED") and hasClickAction())

    private fun option(label: String) = compose.onNode(menuItem and hasText(label))

    private fun setControl() {
        compose.setContent {
            MAGIVANTDACTheme(AppSettings(colorMode = ColorMode.LIGHT)) {
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(density.density, fontScale.floatValue),
                    LocalLayoutDirection provides direction.value
                ) {
                    Box(Modifier.width(viewportWidth.value).testTag("dropdown")) {
                        ExpressiveDropdownItem(
                            icon = Icons.Filled.LightMode,
                            title = "Indicator LED",
                            summary = "Choose how the indicator behaves",
                            items = options.value,
                            itemDescriptions = listOf("Match the active sample rate", "Turn off the indicator", "Keep the indicator red"),
                            enabled = enabled.value,
                            selectedIndex = selected.intValue,
                            onItemSelected = { changes += it; selected.intValue = it }
                        )
                    }
                }
            }
        }
    }

    private fun saveScreenshot(name: String, bitmap: Bitmap) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.filesDir, "dropdown-screenshots").apply {
            check(isDirectory || mkdirs()) { "Cannot create the screenshot directory." }
        }
        File(directory, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
