package com.rapli.magivant.ui

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PresetStoreTest {
    private val directory = Files.createTempDirectory("magivant-presets-").toFile()
    private val store = PresetStore(directory)
    private val preset = DacPreset("Desktop IEM", 36, -2, true, 4, 1, 47, 2)

    @After
    fun cleanUp() {
        directory.deleteRecursively()
    }

    @Test
    fun saveReplaceDeleteAndImportUseTheSameJsonModel() {
        store.savePreset(preset)
        store.savePreset(preset.copy(balanceBaseValue = 3))
        assertEquals(listOf(preset.copy(balanceBaseValue = 3)), store.getAllPresets())

        val exported = ByteArrayOutputStream()
        store.exportTo(exported, preset)
        assertEquals(preset, store.importAndSave(ByteArrayInputStream(exported.toByteArray())))

        store.deletePreset(preset.name)
        assertTrue(store.getAllPresets().isEmpty())
    }
}
