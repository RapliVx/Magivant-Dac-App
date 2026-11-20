package com.rapli.magivant.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.concurrent.Executors

class PresetManagerTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()
    private val preset = DacPreset("IEM", 36, 0, false, 4, 0, 47, 2)

    @Test
    fun saveAndReplace_preservesExactValuesWithoutDuplicateFiles() {
        val folder = temporaryFolder.newFolder()
        val manager = PresetManager(folder)
        manager.savePreset(preset)
        val updated = preset.copy(balanceBaseValue = -3)
        manager.savePreset(updated)
        assertEquals(listOf(updated), manager.getAllPresets())
        assertEquals(1, folder.listFiles()!!.size)
        assertFalse(folder.listFiles()!!.single().name.contains(preset.name))
    }

    @Test
    fun legacyFile_isMigratedOnlyAfterReplacementIsSaved() {
        val folder = temporaryFolder.newFolder()
        val oldFile = File(folder, "old-name.json").apply { writeText(PresetCodec.encode(preset.copy(rawVolumeReg = null, schemaVersion = 1))) }
        val manager = PresetManager(folder)
        assertEquals(1, manager.getAllPresets().size)
        assertTrue(oldFile.exists())
        manager.savePreset(preset)
        assertFalse(oldFile.exists())
        assertEquals(listOf(preset), manager.getAllPresets())
    }

    @Test
    fun corruptFiles_doNotHideValidPresets() {
        val folder = temporaryFolder.newFolder()
        File(folder, "corrupt.json").writeText("{}")
        val manager = PresetManager(folder)
        manager.savePreset(preset)
        assertEquals(listOf(preset), manager.getAllPresets())
    }

    @Test
    fun invalidReplacement_keepsPreviousFile() {
        val manager = PresetManager(temporaryFolder.newFolder())
        manager.savePreset(preset)
        assertThrows(IllegalArgumentException::class.java) { manager.savePreset(preset.copy(volumeIndex = 99)) }
        assertEquals(listOf(preset), manager.getAllPresets())
    }

    @Test
    fun deletingByDisplayName_doesNotResolveUserSuppliedPaths() {
        val root = temporaryFolder.newFolder()
        val folder = File(root, "presets")
        val outside = File(root, "outside.json").apply { writeText("keep") }
        val manager = PresetManager(folder)
        manager.savePreset(preset)
        manager.deletePreset("../outside")
        assertEquals("keep", outside.readText())
        manager.deletePreset(preset.name)
        assertTrue(manager.getAllPresets().isEmpty())
    }

    @Test
    fun concurrentSaves_keepEveryPresetAndLeaveNoTemporaryFiles() {
        val folder = temporaryFolder.newFolder()
        val manager = PresetManager(folder)
        val executor = Executors.newFixedThreadPool(4)
        try {
            val jobs = (0..15).map { index -> executor.submit { manager.savePreset(preset.copy(name = "Preset $index")) } }
            jobs.forEach { it.get() }
        } finally {
            executor.shutdownNow()
        }
        assertEquals(16, manager.getAllPresets().size)
        assertTrue(folder.listFiles()!!.all { it.extension == "json" })
    }

    @Test
    fun invalidStorageLocation_reportsFailure() {
        val manager = PresetManager(temporaryFolder.newFile())
        assertThrows(Exception::class.java) { manager.savePreset(preset) }
    }
}
