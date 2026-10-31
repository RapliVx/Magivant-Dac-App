package com.rapli.magivant.ui.theme

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.core.view.WindowInsetsControllerCompat
import com.materialkolor.rememberDynamicColorScheme
import com.rapli.magivant.ui.util.getHeaderImage

enum class ColorMode(val value: Int) {
    SYSTEM(3),
    LIGHT(4),
    DARK(5),
    DARKAMOLED(6);

    companion object {
        fun fromValue(value: Int) = when (value) {
            3 -> SYSTEM
            4 -> LIGHT
            5 -> DARK
            6 -> DARKAMOLED
            else -> SYSTEM
        }
    }

    fun getDarkThemeValue(systemDarkTheme: Boolean) = when (this) {
        SYSTEM -> systemDarkTheme
        LIGHT -> false
        DARK -> true
        DARKAMOLED -> true
    }
}

data class AppSettings(
    val colorMode: ColorMode = ColorMode.SYSTEM,
    val keyColor: Int = 0,
    val dpiScale: Float = 1.0f,
    val isFloatingNavbar: Boolean = true,
    val isFloatingNavbarGesture: Boolean = true,
    val isClassicBanner: Boolean = false,
    val customHeaderUri: String? = null
)

object ThemeController {
    const val PREFS_NAME = "settings"

    fun getAppSettings(context: Context): AppSettings {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val colorMode = ColorMode.fromValue(
            prefs.getInt("color_mode", ColorMode.SYSTEM.value)
        )
        val keyColor = prefs.getInt("key_color", 0)
        val dpiScale = prefs.getFloat("app_dpi", 1.0f)
        val isFloatingNavbar = prefs.getBoolean("floating_navbar", true)
        val isFloatingNavbarGesture = prefs.getBoolean("floating_navbar_gesture", true)
        val isClassicBanner = prefs.getBoolean("classic_banner", false)
        val customHeaderUri = context.getHeaderImage()
        return AppSettings(
            colorMode = colorMode,
            keyColor = keyColor,
            dpiScale = dpiScale,
            isFloatingNavbar = isFloatingNavbar,
            isFloatingNavbarGesture = isFloatingNavbarGesture,
            isClassicBanner = isClassicBanner,
            customHeaderUri = customHeaderUri
        )
    }
}

@ExperimentalMaterial3ExpressiveApi
@Composable
fun MAGIVANTDACTheme(
    appSettings: AppSettings? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val currentAppSettings = appSettings ?: ThemeController.getAppSettings(context)
    val systemDarkTheme = isSystemInDarkTheme()
    val darkTheme = currentAppSettings.colorMode.getDarkThemeValue(systemDarkTheme)
    val amoledMode = currentAppSettings.colorMode.value == ColorMode.DARKAMOLED.value
    val dynamicColor = currentAppSettings.keyColor == 0

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            rememberDynamicColorScheme(
                seedColor = colorResource(id = android.R.color.system_accent1_500),
                isDark = darkTheme,
                isAmoled = amoledMode
            )
        }
        !dynamicColor -> {
            rememberDynamicColorScheme(
                seedColor = Color(currentAppSettings.keyColor),
                isDark = darkTheme,
                isAmoled = amoledMode
            )
        }
        else -> {
            if (darkTheme) darkColorScheme() else expressiveLightColorScheme()
        }
    }

    LaunchedEffect(darkTheme) {
        val window = (context as? Activity)?.window ?: return@LaunchedEffect
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = Typography,
        motionScheme = MotionScheme.expressive(),
        content = content
    )
}

@Composable
@ReadOnlyComposable
fun isInDarkTheme(themeMode: Int): Boolean {
    return when (themeMode) {
        1, 4 -> false
        2, 5, 6 -> true
        else -> isSystemInDarkTheme()
    }
}
