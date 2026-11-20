package com.rapli.magivant

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.rapli.magivant.ui.DacUiState
import com.rapli.magivant.ui.ThemePreviewNavigation
import com.rapli.magivant.ui.VolumeAndBalanceCard
import com.rapli.magivant.ui.components.MagivantPresetScreen
import com.rapli.magivant.ui.theme.AppSettings
import com.rapli.magivant.ui.theme.ColorMode
import com.rapli.magivant.ui.theme.MAGIVANTDACTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
class LayoutRegressionTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun savePreset_staysOnOneLineOnNarrowScreensAndLargeFonts() {
        var saves = 0
        val fontScale = mutableStateOf(1f)
        compose.setContent {
            MAGIVANTDACTheme(AppSettings(colorMode = ColorMode.LIGHT)) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale.value)) {
                    Box(modifier = Modifier.width(320.dp).padding(16.dp).testTag("volume_card")) {
                        VolumeAndBalanceCard(
                            uiState = DacUiState(isConnected = true, hasDeviceState = true, rawVolumeReg = 0xFF, volumeIndex = 0),
                            onVolumeChanged = {},
                            onAdjustHalfDb = {},
                            onBalanceChanged = {},
                            onQuickSavePreset = { saves++ }
                        )
                    }
                }
            }
        }
        for (scale in listOf(1f, 1.3f, 1.5f)) {
            compose.runOnIdle { fontScale.value = scale }
            val layouts = mutableListOf<TextLayoutResult>()
            compose.onNodeWithText("Save Preset", useUnmergedTree = true)
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            saveScreenshot("volume_card", "save-preset-font-$scale.png")
            assertEquals(1, layouts.single().lineCount)
            val layout = layouts.single()
            val lastGlyph = layout.getBoundingBox(layout.layoutInput.text.lastIndex)
            assertTrue("Save Preset must fit at font scale $scale", lastGlyph.right <= layout.size.width + 1f)
            val button = compose.onNodeWithText("Save Preset").fetchSemanticsNode().boundsInRoot
            val card = compose.onNodeWithTag("volume_card").fetchSemanticsNode().boundsInRoot
            assertTrue(button.left >= card.left && button.right <= card.right)
            compose.onNodeWithText("Save Preset").performClick()
        }
        compose.runOnIdle { assertEquals(3, saves) }
        saveScreenshot("volume_card", "save-preset-large-font.png")
    }

    @Test
    fun previewNavigation_keepsAllThreeTabsInsideBothLayouts() {
        val width = mutableStateOf(120.dp)
        val floating = mutableStateOf(true)
        compose.setContent {
            MAGIVANTDACTheme(AppSettings(colorMode = ColorMode.LIGHT)) {
                Box(modifier = Modifier.width(width.value).testTag("navigation_preview")) {
                    ThemePreviewNavigation(
                        colorScheme = MaterialTheme.colorScheme,
                        isFloating = floating.value
                    )
                }
            }
        }
        for (isFloating in listOf(true, false)) {
            for (previewWidth in listOf(200.dp, 144.dp, 120.dp)) {
                compose.runOnIdle {
                    floating.value = isFloating
                    width.value = previewWidth
                }
                val preview = compose.onNodeWithTag("navigation_preview").fetchSemanticsNode().boundsInRoot
                val icons = listOf("DAC", "Presets", "Settings").map {
                    compose.onNodeWithContentDescription(it).fetchSemanticsNode().boundsInRoot
                }
                icons.forEach { icon ->
                    assertTrue("Preview icon must remain inside the navbar", icon.left >= preview.left && icon.right <= preview.right)
                    assertTrue(icon.top >= preview.top && icon.bottom <= preview.bottom)
                }
                assertTrue(icons.zipWithNext().all { (left, right) -> left.right <= right.left })
            }
            saveScreenshot("navigation_preview", if (isFloating) "preview-floating-navbar.png" else "preview-standard-navbar.png")
        }
    }

    @Test
    fun presetActionLabel_staysOnOneLineOnNarrowScreens() {
        compose.setContent {
            MAGIVANTDACTheme(AppSettings(colorMode = ColorMode.LIGHT)) {
                Box(modifier = Modifier.width(320.dp).testTag("preset_page")) {
                    MagivantPresetScreen(
                        uiState = DacUiState(isConnected = true, hasDeviceState = true),
                        presetList = emptyList(),
                        bottomInnerPadding = 0.dp,
                        onPresetSelected = {},
                        onNewClicked = {},
                        onImportClicked = {},
                        onExportClicked = {},
                        onDeleteClicked = {}
                    )
                }
            }
        }
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText("Save Preset", useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        saveScreenshot("preset_page", "preset-actions-narrow.png")
        assertEquals(1, layouts.single().lineCount)
        val layout = layouts.single()
        val lastGlyph = layout.getBoundingBox(layout.layoutInput.text.lastIndex)
        assertTrue(lastGlyph.right <= layout.size.width + 1f)
    }

    private fun saveScreenshot(tag: String, name: String) {
        val bitmap = compose.onNodeWithTag(tag).captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.filesDir, "layout-screenshots").apply {
            check(isDirectory || mkdirs()) { "Cannot create the screenshot directory." }
        }
        File(directory, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
