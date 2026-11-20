package com.rapli.magivant.ui

import android.content.Context
import android.net.Uri
import androidx.annotation.Keep
import com.rapli.magivant.usb.DacVolume
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

@Keep
data class DacPreset(
    val name: String,
    val volumeIndex: Int,
    val balanceBaseValue: Int,
    val isHighGain: Boolean,
    val digitalFilterPos: Int,
    val ledPos: Int,
    val rawVolumeReg: Int? = null,
    val schemaVersion: Int = 1
) {
    val volumeRegister: Int
        get() = rawVolumeReg ?: DacVolume.registerValues[volumeIndex]
}

class PresetManager internal constructor(private val presetFolder: File) {
    constructor(context: Context) : this(File(context.filesDir, "presets"))

    private fun presetFiles(): List<File> {
        if (!presetFolder.exists() && !presetFolder.mkdirs()) {
            throw IOException("Cannot create preset folder.")
        }
        return (presetFolder.listFiles() ?: throw IOException("Cannot read preset folder."))
            .filter { it.extension == "json" && Files.isRegularFile(it.toPath(), LinkOption.NOFOLLOW_LINKS) }
    }

    private fun readPreset(file: File): DacPreset? = try {
        file.inputStream().use { PresetCodec.read(it) }
    } catch (_: Exception) {
        null
    }

    @Synchronized
    fun savePreset(preset: DacPreset) {
        val json = PresetCodec.encode(preset)
        val previousFiles = presetFiles().filter { readPreset(it)?.name == preset.name }
        val fileName = MessageDigest.getInstance("SHA-256")
            .digest(preset.name.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        val target = File(presetFolder, "$fileName.json")
        require(target.canonicalFile.parentFile == presetFolder.canonicalFile) { "Invalid preset path." }
        val temporary = File.createTempFile("preset_", ".tmp", presetFolder)
        try {
            FileOutputStream(temporary).use { output ->
                output.write(json.toByteArray(Charsets.UTF_8))
                output.fd.sync()
            }
            Files.move(
                temporary.toPath(), target.toPath(),
                StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING
            )
            previousFiles.filter { it != target }.forEach { Files.delete(it.toPath()) }
        } finally {
            temporary.delete()
        }
    }

    @Synchronized
    fun getAllPresets(): List<DacPreset> =
        presetFiles().mapNotNull(::readPreset).distinctBy { it.name }.sortedBy { it.name }

    @Synchronized
    fun deletePreset(presetName: String) {
        presetFiles().filter { readPreset(it)?.name == presetName }
            .forEach { Files.delete(it.toPath()) }
    }

    fun exportToUri(context: Context, uri: Uri, preset: DacPreset) {
        val json = PresetCodec.encode(preset)
        val output = context.contentResolver.openOutputStream(uri, "wt")
            ?: throw IOException("Cannot open the destination file.")
        output.use { it.write(json.toByteArray(Charsets.UTF_8)) }
    }

    fun importAndSaveFromUri(context: Context, uri: Uri): DacPreset {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Cannot open the preset file.")
        val preset = input.use { PresetCodec.read(it) }
        savePreset(preset)
        return preset
    }
}
