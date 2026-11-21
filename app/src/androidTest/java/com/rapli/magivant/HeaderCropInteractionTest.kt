package com.rapli.magivant

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBackUnconditionally
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.platform.app.InstrumentationRegistry
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCropActivity
import com.yalantis.ucrop.view.UCropView
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeaderCropInteractionTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val files = mutableListOf<File>()

    @After
    fun tearDown() {
        files.forEach { it.delete() }
    }

    @Test
    fun cropActivity_opensAndSavesAStaticHeaderWithoutMissingRuntimeClasses() {
        val source = createImage()
        val destination = File.createTempFile("header_crop_result_", ".jpg", context.cacheDir).also { files += it }
        val intent = UCrop.of(Uri.fromFile(source), Uri.fromFile(destination))
            .withAspectRatio(16f, 9f)
            .withMaxResultSize(1600, 900)
            .getIntent(context)

        ActivityScenario.launchActivityForResult<UCropActivity>(intent).use { scenario ->
            awaitBitmap(scenario)
            onView(withId(com.yalantis.ucrop.R.id.menu_crop)).perform(click())
            val result = scenario.result
            assertEquals(Activity.RESULT_OK, result.resultCode)
            assertEquals(Uri.fromFile(destination), UCrop.getOutput(result.resultData))
            assertTrue(destination.length() > 0)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(destination.absolutePath, bounds)
            assertEquals(16f / 9f, bounds.outWidth.toFloat() / bounds.outHeight, 0.02f)
        }
    }

    @Test
    fun cancellingCrop_returnsWithoutWritingTheDestination() {
        val source = createImage()
        val destination = File.createTempFile("header_crop_cancel_", ".jpg", context.cacheDir).also { files += it }
        val intent = UCrop.of(Uri.fromFile(source), Uri.fromFile(destination))
            .withAspectRatio(16f, 9f)
            .getIntent(context)

        ActivityScenario.launchActivityForResult<UCropActivity>(intent).use { scenario ->
            awaitBitmap(scenario)
            pressBackUnconditionally()
            assertEquals(Activity.RESULT_CANCELED, scenario.result.resultCode)
            assertEquals(0L, destination.length())
        }
    }

    private fun createImage(): File {
        val source = File.createTempFile("header_crop_source_", ".jpg", context.cacheDir).also { files += it }
        val bitmap = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888)
        try {
            bitmap.eraseColor(android.graphics.Color.BLUE)
            source.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)) }
        } finally {
            bitmap.recycle()
        }
        return source
    }

    private fun awaitBitmap(scenario: ActivityScenario<UCropActivity>) {
        val deadline = SystemClock.uptimeMillis() + 10_000
        var loaded = false
        while (!loaded && SystemClock.uptimeMillis() < deadline) {
            scenario.onActivity {
                loaded = it.findViewById<UCropView>(com.yalantis.ucrop.R.id.ucrop).cropImageView.viewBitmap != null
            }
            if (!loaded) SystemClock.sleep(50)
        }
        assertTrue("The selected header must load in the crop activity", loaded)
    }
}
