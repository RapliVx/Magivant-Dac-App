package com.rapli.magivant.ui.util

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import java.io.File
import java.io.FileOutputStream

private const val HEADER_IMAGE_KEY = "header_image_uri"
private const val PREFS_NAME = "settings"

fun Context.getHeaderImage(): String? {
    val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getString(HEADER_IMAGE_KEY, null)
}

fun Context.saveHeaderImage(uri: String) {
    val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit {
        putString(HEADER_IMAGE_KEY, uri)
    }
}

fun Context.clearCustomHeaderImage() {
    val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit {
        remove(HEADER_IMAGE_KEY)
    }
}

fun Context.copyUriToInternalStorage(uri: Uri): Uri? {
    return try {
        val inputStream = contentResolver.openInputStream(uri) ?: return null
        val file = File(filesDir, "custom_header_image_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(file)
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()
        Uri.fromFile(file)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
