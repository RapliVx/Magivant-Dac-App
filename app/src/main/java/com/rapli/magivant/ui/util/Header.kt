package com.rapli.magivant.ui.util

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import kotlin.coroutines.coroutineContext

private const val HEADER_IMAGE_KEY = "header_image_uri"
private const val PREFS_NAME = "settings"
private const val MAX_IMAGE_BYTES = 20 * 1024 * 1024
private val headerMutex = Mutex()

fun Context.getHeaderImage(): String? =
    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(HEADER_IMAGE_KEY, null)

private fun Context.deleteOldHeaderImages(keep: File? = null) {
    filesDir.listFiles()?.filter {
        it != keep && (it.name.startsWith("custom_header_image_") || it.name.startsWith("cropped_header_")) &&
            it.canonicalFile.parentFile == filesDir.canonicalFile
    }?.forEach { it.delete() }
}

suspend fun Context.saveHeaderImage(uri: String): String = withContext(Dispatchers.IO) {
    headerMutex.withLock {
        val savedUri = copyUriToInternalStorage(Uri.parse(uri))
        val file = File(requireNotNull(savedUri.path))
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.edit().putString(HEADER_IMAGE_KEY, savedUri.toString()).commit()) {
            file.delete()
            throw IOException("Cannot save the header image setting.")
        }
        deleteOldHeaderImages(keep = file)
        savedUri.toString()
    }
}

suspend fun Context.clearCustomHeaderImage() = withContext(Dispatchers.IO) {
    headerMutex.withLock {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.edit().remove(HEADER_IMAGE_KEY).commit()) throw IOException("Cannot clear the header image setting.")
        deleteOldHeaderImages()
    }
}

suspend fun Context.copyUriToInternalStorage(uri: Uri): Uri = withContext(Dispatchers.IO) {
    val file = File.createTempFile("custom_header_image_", ".img", filesDir)
    try {
        val input = contentResolver.openInputStream(uri) ?: throw IOException("Cannot open the selected image.")
        input.use { source ->
            file.outputStream().use { destination ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var total = 0
                while (true) {
                    coroutineContext.ensureActive()
                    val count = source.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > MAX_IMAGE_BYTES) throw IOException("Header image must be no larger than 20 MB.")
                    destination.write(buffer, 0, count)
                }
                if (total == 0) throw IOException("The selected image is empty.")
            }
        }
        Uri.fromFile(file)
    } catch (e: Exception) {
        file.delete()
        throw e
    }
}
