package com.rapli.magivant.ui.util

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class HeaderTest {
    private lateinit var context: Context

    @Before
    fun setUp() = runBlocking {
        context = RuntimeEnvironment.getApplication()
        context.clearCustomHeaderImage()
    }

    @Test
    fun savingAndReplacingHeader_copiesBytesAndRemovesThePreviousManagedFile() = runBlocking {
        val source = File(context.cacheDir, "header.gif").apply { writeBytes("GIF89a".toByteArray()) }
        val first = context.saveHeaderImage(Uri.fromFile(source).toString())
        val firstFile = File(requireNotNull(Uri.parse(first).path))
        assertArrayEquals(source.readBytes(), firstFile.readBytes())
        assertEquals(first, context.getHeaderImage())
        val second = context.saveHeaderImage(Uri.fromFile(source).toString())
        assertFalse(firstFile.exists())
        assertTrue(File(requireNotNull(Uri.parse(second).path)).exists())
        assertTrue(source.exists())
    }

    @Test
    fun clearingHeader_removesManagedImageAndPreservesUnrelatedFiles() = runBlocking {
        val unrelated = File(context.filesDir, "keep.txt").apply { writeText("keep") }
        val source = File(context.cacheDir, "header.gif").apply { writeBytes("GIF89a".toByteArray()) }
        val saved = context.saveHeaderImage(Uri.fromFile(source).toString())
        context.clearCustomHeaderImage()
        assertNull(context.getHeaderImage())
        assertFalse(File(requireNotNull(Uri.parse(saved).path)).exists())
        assertTrue(unrelated.exists())
    }

    @Test
    fun failedReplacement_preservesThePreviousHeader() = runBlocking {
        val source = File(context.cacheDir, "header.gif").apply { writeBytes("GIF89a".toByteArray()) }
        val saved = context.saveHeaderImage(Uri.fromFile(source).toString())
        val empty = File(context.cacheDir, "empty.gif").apply { writeBytes(byteArrayOf()) }
        val failure = runCatching { context.saveHeaderImage(Uri.fromFile(empty).toString()) }
        assertTrue(failure.isFailure)
        assertEquals(saved, context.getHeaderImage())
        assertTrue(File(requireNotNull(Uri.parse(saved).path)).exists())
    }
}
