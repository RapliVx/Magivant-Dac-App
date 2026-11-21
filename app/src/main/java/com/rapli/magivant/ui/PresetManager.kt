package com.rapli.magivant.ui

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.IOException

class PresetManager private constructor(private val store: PresetStore) {
    internal constructor(presetFolder: File) : this(PresetStore(presetFolder))
    constructor(context: Context) : this(PresetStore(File(context.filesDir, "presets")))

    fun savePreset(preset: DacPreset) = store.savePreset(preset)

    fun getAllPresets(): List<DacPreset> = store.getAllPresets()

    fun deletePreset(presetName: String) = store.deletePreset(presetName)

    fun exportToUri(context: Context, uri: Uri, preset: DacPreset) {
        val output = context.contentResolver.openOutputStream(uri, "wt")
            ?: throw IOException("Cannot open the destination file.")
        store.exportTo(output, preset)
    }

    fun importAndSaveFromUri(context: Context, uri: Uri): DacPreset {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Cannot open the preset file.")
        return store.importAndSave(input)
    }
}
