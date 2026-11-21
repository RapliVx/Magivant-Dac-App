package com.rapli.magivant.ui

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

class PresetStore(private val presetFolder: File) {
    private fun presetFiles(): List<File> {
        if (!presetFolder.exists() && !presetFolder.mkdirs()) {
            throw IOException("Cannot create preset folder.")
        }
        if (!presetFolder.isDirectory) throw IOException("Preset storage is not a directory.")
        return (presetFolder.listFiles() ?: throw IOException("Cannot read preset folder."))
            .filter { it.extension == "json" && Files.isRegularFile(it.toPath(), LinkOption.NOFOLLOW_LINKS) }
    }

    private fun readPreset(file: File): DacPreset? = try {
        file.inputStream().use(PresetCodec::read)
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

    fun exportTo(output: OutputStream, preset: DacPreset) {
        output.use { it.write(PresetCodec.encode(preset).toByteArray(Charsets.UTF_8)) }
    }

    fun importAndSave(input: InputStream): DacPreset = input.use(PresetCodec::read).also(::savePreset)

    fun exportTo(file: File, preset: DacPreset) =
        FileOutputStream(file).use { exportTo(it, preset) }

    fun importAndSave(file: File): DacPreset = FileInputStream(file).use(::importAndSave)
}
