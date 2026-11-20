package com.rapli.magivant

import android.app.Application
import android.hardware.usb.UsbDevice
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.longClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import com.rapli.magivant.ui.DacPreset
import com.rapli.magivant.ui.DacUiState
import com.rapli.magivant.ui.MagivantScreen
import com.rapli.magivant.ui.MagivantViewModel
import com.rapli.magivant.ui.PresetManager
import com.rapli.magivant.ui.components.ExpressiveLoadingDialog
import com.rapli.magivant.ui.components.MagivantBottomBar
import com.rapli.magivant.ui.components.MagivantPresetScreen
import com.rapli.magivant.ui.components.MagivantSlider
import com.rapli.magivant.ui.components.MagivantTab
import com.rapli.magivant.ui.theme.AppSettings
import com.rapli.magivant.ui.theme.ColorMode
import com.rapli.magivant.ui.theme.MAGIVANTDACTheme
import com.rapli.magivant.usb.DacConnection
import com.rapli.magivant.usb.DndControl
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
class MotionInteractionTest {
    @get:Rule
    val compose = createComposeRule()
    private val store = ViewModelStore()

    @After
    fun tearDown() {
        store.clear()
    }

    @Test
    fun navigationDrag_selectsTheTabInBothLayoutDirections() {
        val current = mutableStateOf(MagivantTab.HOME)
        val direction = mutableStateOf(LayoutDirection.Ltr)
        compose.setContent {
            MAGIVANTDACTheme(AppSettings(colorMode = ColorMode.LIGHT)) {
                CompositionLocalProvider(LocalLayoutDirection provides direction.value) {
                    MagivantBottomBar(current.value, { current.value = it }, true, true, Modifier.testTag("navigation"))
                }
            }
        }
        for (layout in listOf(LayoutDirection.Ltr, LayoutDirection.Rtl)) {
            compose.runOnIdle { direction.value = layout; current.value = MagivantTab.HOME }
            val root = compose.onNodeWithTag("navigation").fetchSemanticsNode().boundsInRoot
            val start = compose.onNode(hasContentDescription("DAC") and hasClickAction()).fetchSemanticsNode().boundsInRoot.center
            val end = compose.onNode(hasContentDescription("Settings") and hasClickAction()).fetchSemanticsNode().boundsInRoot.center
            compose.onNodeWithTag("navigation").performTouchInput {
                swipe(Offset(start.x - root.left, start.y - root.top), Offset(end.x - root.left, end.y - root.top), 240)
            }
            compose.onNode(hasContentDescription("Settings") and hasClickAction()).assertIsSelected()
            compose.runOnIdle { assertEquals(MagivantTab.SETTINGS, current.value) }
        }
    }

    @Test
    fun rapidNavigation_canRetargetAnUnfinishedAnimation() {
        val current = mutableStateOf(MagivantTab.HOME)
        compose.setContent {
            MAGIVANTDACTheme(AppSettings(colorMode = ColorMode.LIGHT)) {
                MagivantBottomBar(current.value, { current.value = it }, true, true)
            }
        }
        compose.mainClock.autoAdvance = false
        compose.onNode(hasContentDescription("Presets") and hasClickAction()).performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.onNode(hasContentDescription("Settings") and hasClickAction()).performClick()
        compose.mainClock.advanceTimeBy(2000)
        compose.onNode(hasContentDescription("Settings") and hasClickAction()).assertIsSelected()
        compose.onNodeWithText("Settings").assertExists()
        compose.onNodeWithText("Presets").assertDoesNotExist()
    }

    @Test
    fun disabledNavigationGesture_keepsTheCurrentTab() {
        val current = mutableStateOf(MagivantTab.HOME)
        compose.setContent {
            MAGIVANTDACTheme(AppSettings(colorMode = ColorMode.LIGHT)) {
                MagivantBottomBar(current.value, { current.value = it }, true, false, Modifier.testTag("navigation"))
            }
        }
        compose.onNodeWithTag("navigation").performTouchInput { swipe(centerLeft, centerRight, 240) }
        compose.runOnIdle { assertEquals(MagivantTab.HOME, current.value) }
        compose.onNode(hasContentDescription("Settings") and hasClickAction()).performClick()
        compose.onNode(hasContentDescription("Settings") and hasClickAction()).assertIsSelected()
    }

    @Test
    fun slider_waitsForReadbackAndRollsBackWithoutRedispatching() {
        val value = mutableFloatStateOf(20f)
        val pending = mutableStateOf(false)
        var commands = 0
        compose.setContent {
            MAGIVANTDACTheme(AppSettings(colorMode = ColorMode.LIGHT)) {
                MagivantSlider(
                    value = value.floatValue,
                    onValueChange = { pending.value = true; commands++ },
                    valueRange = 0f..60f,
                    steps = 59,
                    isPending = pending.value,
                    modifier = Modifier.testTag("volume")
                )
            }
        }
        compose.onNodeWithTag("volume").performSemanticsAction(SemanticsActions.SetProgress) { it(40f) }
        compose.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo(40f, 0f..60f, 59))).assertExists()
        compose.runOnIdle { assertEquals(1, commands); assertEquals(20f, value.floatValue) }
        // A failed write clears pending while the last confirmed value stays at 20.
        compose.runOnIdle { pending.value = false }
        compose.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo(20f, 0f..60f, 59))).assertExists()
        compose.runOnIdle { assertEquals(1, commands) }
        compose.onNodeWithTag("volume").performSemanticsAction(SemanticsActions.SetProgress) { it(40f) }
        compose.runOnIdle { value.floatValue = 40f; pending.value = false }
        compose.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo(40f, 0f..60f, 59))).assertExists()
        compose.runOnIdle { assertEquals(2, commands) }
    }

    @Test
    fun loadingDialog_finishesItsExitAndCanReopen() {
        val visible = mutableStateOf(false)
        compose.setContent {
            MAGIVANTDACTheme(AppSettings(colorMode = ColorMode.LIGHT)) {
                ExpressiveLoadingDialog(message = "Reading DAC", visible = visible.value)
            }
        }
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { visible.value = true }
        finishTransition()
        compose.onNodeWithText("Reading DAC").assertExists()
        compose.runOnIdle { visible.value = false }
        finishTransition()
        compose.onNodeWithText("Reading DAC").assertDoesNotExist()
        compose.runOnIdle { visible.value = true }
        finishTransition()
        compose.onNodeWithText("Reading DAC").assertExists()
        compose.runOnIdle { visible.value = false }
        finishTransition()
    }

    @Test
    fun presetSelectionToolbar_keepsItsActionsAfterTransition() {
        compose.setContent {
            MAGIVANTDACTheme(AppSettings(colorMode = ColorMode.LIGHT)) {
                Box(Modifier.width(360.dp)) {
                    MagivantPresetScreen(
                        DacUiState(isConnected = true, hasDeviceState = true),
                        listOf(DacPreset("IEM", 36, 0, false, 0, 0)),
                        0.dp, {}, {}, {}, {}, {}
                    )
                }
            }
        }
        compose.onNodeWithText("IEM").performTouchInput { longClick() }
        compose.onNodeWithText("1 Selected").assertExists()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Current Settings").assertExists()
        compose.onNodeWithText("1 Selected").assertDoesNotExist()
    }

    @Test
    fun switchingNavigationModes_keepsOneSelectedDestination() {
        val application = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application
        val settings = mutableStateOf(AppSettings(colorMode = ColorMode.LIGHT))
        val model = MagivantViewModel(application, NoUsbConnection(), PresetManager(application), NoDndControl(), SavedStateHandle())
        store.put("motion", model)
        compose.setContent {
            MAGIVANTDACTheme(settings.value) {
                MagivantScreen(model, settings.value, { settings.value = it }, {}, {})
            }
        }
        compose.onNode(hasContentDescription("Presets") and hasClickAction()).performClick()
        compose.mainClock.autoAdvance = false
        for (floating in listOf(false, true, false)) {
            compose.runOnIdle { settings.value = settings.value.copy(isFloatingNavbar = floating) }
            finishTransition()
            // Material's selected standard item exposes its visible label instead of an icon description.
            val presetTab = (hasContentDescription("Presets") or hasText("Presets")) and hasClickAction()
            compose.onAllNodes(presetTab).assertCountEquals(1)
            compose.onNode(presetTab).assertIsSelected().assertIsDisplayed()
        }
    }

    private fun finishTransition() {
        // Dialog windows and Scaffold measure new content on the Android frame clock.
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(2000)
        compose.waitForIdle()
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
    }

    private class NoUsbConnection : DacConnection {
        override suspend fun openConnection(device: UsbDevice, sessionId: Long) = error("No hardware in motion tests")
        override suspend fun closeConnection(sessionId: Long) = Unit
        override suspend fun sendCommand(sessionId: Long, commandId: Byte, value1: Byte, value2: Byte, value3: Byte) = error("No hardware in motion tests")
        override suspend fun readData(sessionId: Long, commandId: Byte): ByteArray = error("No hardware in motion tests")
    }

    private class NoDndControl : DndControl {
        override fun hasDndPermission() = false
        override fun requestDndPermission() = false
        override fun setDndActive(activate: Boolean) = !activate
    }
}
