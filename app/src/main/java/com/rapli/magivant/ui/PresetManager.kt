package com.rapli.magivant.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.annotation.Keep
import com.google.gson.GsonBuilder
import java.io.File

@Keep
data class DacPreset(
    val name: String,
    val volumeIndex: Int,
    val balanceBaseValue: Int,
    val isHighGain: Boolean,
    val digitalFilterPos: Int,
    val ledPos: Int
)

class PresetManager(context: Context) {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val presetFolder = File(context.filesDir, "presets").apply {
        if (!exists()) mkdirs()
    }

    fun savePreset(preset: DacPreset) {
        try {
            val file = File(presetFolder, "${preset.name}.json")
            file.writeText(gson.toJson(preset))
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun getAllPresets(): List<DacPreset> {
        val files = presetFolder.listFiles { file -> file.extension == "json" } ?: return emptyList()
        return files.mapNotNull { file ->
            try { gson.fromJson(file.readText(), DacPreset::class.java) }
            catch (e: Exception) { null }
        }.sortedBy { it.name }
    }

    fun deletePreset(presetName: String) {
        try {
            val file = File(presetFolder, "$presetName.json")
            if (file.exists()) file.delete()
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun exportToUri(context: Context, uri: Uri, preset: DacPreset) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(gson.toJson(preset).toByteArray())
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun getFileNameFromUri(context: Context, uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        result = cursor.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (result != null && cut != -1) {
                result = result.substring(cut + 1)
            }
        }
        return result?.substringBeforeLast(".") ?: "Imported_Preset"
    }

    fun importAndSaveFromUri(context: Context, uri: Uri): DacPreset? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val jsonString = inputStream.bufferedReader().use { it.readText() }
                var importedPreset = gson.fromJson(jsonString, DacPreset::class.java)

                val actualFileName = getFileNameFromUri(context, uri)

                importedPreset = importedPreset.copy(name = actualFileName)

                savePreset(importedPreset)
                importedPreset
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}