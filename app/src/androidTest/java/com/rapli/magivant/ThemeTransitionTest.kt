package com.rapli.magivant

import android.app.Application
import android.hardware.usb.UsbDevice
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import com.materialkolor.rememberDynamicColorScheme
import com.rapli.magivant.ui.DacUiState
import com.rapli.magivant.ui.MagivantSettingsScreen
import com.rapli.magivant.ui.MagivantScreen
import com.rapli.magivant.ui.MagivantViewModel
import com.rapli.magivant.ui.PresetManager
import com.rapli.magivant.ui.theme.AppSettings
import com.rapli.magivant.ui.theme.ColorMode
import com.rapli.magivant.ui.theme.MAGIVANTDACTheme
import com.rapli.magivant.usb.DacVolume
import com.rapli.magivant.usb.DacConnection
import com.rapli.magivant.usb.DndControl
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
class ThemeTransitionTest {
    @get:Rule
    val compose = createComposeRule()

    private val settings = mutableStateOf(AppSettings(colorMode = ColorMode.LIGHT, keyColor = 0xFF6750A4.toInt()))
    private lateinit var colors: ColorScheme
    private lateinit var targetColors: ColorScheme
    private val store = ViewModelStore()

    @After
    fun tearDown() {
        store.clear()
    }

    @Test
    fun appBarAndNavigation_followTheThemeWithoutASecondColorAnimation() {
        setSettingsContent(includeChrome = true)
        val navigation = hasContentDescription("Presets") and hasClickAction()
        compose.onNode(navigation).performClick()
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { settings.value = settings.value.copy(colorMode = ColorMode.DARK) }
        advanceTransition(48)

        val title = compose.onNode(hasText("Presets") and !hasClickAction())
        val titlePixels = title.captureToImage().toPixelMap()
        assertEquals(colors.surface.toArgb(), titlePixels[0, 0].toArgb())

        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNode(navigation).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertEquals(colors.onPrimaryContainer.toArgb(), layouts.single().layoutInput.style.color.toArgb())
        advanceTransition(2000)
    }

    @Test
    fun preview_usesTheSameIntermediateColorsAsTheApp() {
        setSettingsContent()
        val initialSurface = colors.surface.toArgb()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithContentDescription("Dark").performClick()
        advanceTransition(48)

        val intermediate = colors.surface.toArgb()
        assertNotEquals(initialSurface, intermediate)
        assertNotEquals(targetColors.surface.toArgb(), intermediate)
        assertPreviewColorMatchesTheme()

        advanceTransition(2000)
        assertEquals(targetColors.surface.toArgb(), colors.surface.toArgb())
        assertPreviewColorMatchesTheme()
    }

    @Test
    fun rapidModeAndPaletteChanges_finishAtTheLatestAmoledScheme() {
        setSettingsContent()
        compose.mainClock.autoAdvance = false
        val targets = listOf(
            ColorMode.DARK to 0xFF6750A4.toInt(),
            ColorMode.LIGHT to 0xFF006D3B.toInt(),
            ColorMode.DARK to 0xFF8B5000.toInt(),
            ColorMode.DARKAMOLED to 0xFFBA1A1A.toInt()
        )
        for ((mode, seed) in targets) {
            compose.runOnIdle { settings.value = settings.value.copy(colorMode = mode, keyColor = seed) }
            advanceTransition(48)
            assertPreviewColorMatchesTheme()
        }
        advanceTransition(2000)
        assertEquals(Color.Black.toArgb(), colors.surface.toArgb())
        assertEquals(targetColors.primary.toArgb(), colors.primary.toArgb())
        assertEquals(targetColors.onSurface.toArgb(), colors.onSurface.toArgb())
        assertPreviewColorMatchesTheme()
    }

    private fun advanceTransition(milliseconds: Long) {
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(milliseconds)
        compose.waitForIdle()
    }

    private fun assertPreviewColorMatchesTheme() {
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText("Magivant DAC", useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertEquals(colors.onSurface.toArgb(), layouts.single().layoutInput.style.color.toArgb())
    }

    private fun setSettingsContent(includeChrome: Boolean = false) {
        val model = if (includeChrome) {
            val application = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application
            MagivantViewModel(application, NoUsbConnection(), PresetManager(application), NoDndControl(), SavedStateHandle())
                .also { store.put("theme", it) }
        } else null
        compose.setContent {
            MAGIVANTDACTheme(settings.value) {
                val scheme = MaterialTheme.colorScheme
                val target = rememberDynamicColorScheme(
                    seedColor = Color(settings.value.keyColor),
                    isDark = settings.value.colorMode.getDarkThemeValue(false),
                    isAmoled = settings.value.colorMode == ColorMode.DARKAMOLED
                )
                SideEffect { colors = scheme; targetColors = target }
                if (model != null) {
                    MagivantScreen(model, settings.value, { settings.value = it }, {}, {})
                } else MagivantSettingsScreen(
                    appSettings = settings.value,
                    uiState = DacUiState(),
                    presets = emptyList(),
                    volumeTable = DacVolume.registerValues,
                    bottomInnerPadding = 0.dp,
                    onUpdateThemeMode = { settings.value = settings.value.copy(colorMode = it) },
                    onUpdateKeyColor = { settings.value = settings.value.copy(keyColor = it) },
                    onUpdateDpiScale = {},
                    onUpdateFloatingNavbar = {},
                    onUpdateFloatingGesture = {},
                    onUpdateClassicBanner = {},
                    onUpdateCustomHeaderUri = {},
                    onUpdateMaxVolumeLimit = {},
                    onUpdateStartupPreset = {},
                    onReportError = {}
                )
            }
        }
    }

    private class NoUsbConnection : DacConnection {
        override suspend fun openConnection(device: UsbDevice, sessionId: Long) = error("No hardware in theme tests")
        override suspend fun closeConnection(sessionId: Long) = Unit
        override suspend fun sendCommand(sessionId: Long, commandId: Byte, value1: Byte, value2: Byte, value3: Byte) = error("No hardware in theme tests")
        override suspend fun readData(sessionId: Long, commandId: Byte): ByteArray = error("No hardware in theme tests")
    }

    private class NoDndControl : DndControl {
        override fun hasDndPermission() = false
        override fun requestDndPermission() = false
        override fun setDndActive(activate: Boolean) = !activate
    }
}
