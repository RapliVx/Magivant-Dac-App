package com.rapli.magivant.ui.theme

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.rememberDynamicColorScheme
import com.rapli.magivant.ui.util.getHeaderImage

enum class ColorMode(val value: Int) {
    SYSTEM(3),
    LIGHT(1),
    DARK(2),
    DARKAMOLED(4);

    companion object {
        fun fromValue(value: Int) = entries.find { it.value == value } ?: SYSTEM
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
    val amoledMode = currentAppSettings.colorMode == ColorMode.DARKAMOLED
    val dynamicColor = currentAppSettings.keyColor == 0

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val baseScheme = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (amoledMode) applyAmoledOverrides(baseScheme) else baseScheme
        }
        !dynamicColor -> {
            rememberDynamicColorScheme(
                seedColor = Color(currentAppSettings.keyColor),
                isDark = darkTheme,
                isAmoled = amoledMode
            )
        }
        else -> {
            if (darkTheme) {
                val baseDark = darkColorScheme(
                    primary = PRIMARY,
                    secondary = PRIMARY_LIGHT,
                    tertiary = SECONDARY_LIGHT
                )
                if (amoledMode) applyAmoledOverrides(baseDark) else baseDark
            } else {
                expressiveLightColorScheme()
            }
        }
    }

    SystemBarStyle(
        darkMode = darkTheme
    )

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = Typography,
        motionScheme = MotionScheme.expressive(),
        content = content
    )
}

private fun applyAmoledOverrides(scheme: ColorScheme): ColorScheme = scheme.copy(
    background = AMOLED_BLACK,
    surface = AMOLED_BLACK,
    surfaceContainerLowest = AMOLED_BLACK,
    surfaceContainerLow = Color(0xFF0A0A0A),
    surfaceContainer = Color(0xFF111111),
    surfaceContainerHigh = Color(0xFF181818),
    surfaceContainerHighest = Color(0xFF202020)
)

@Composable
@ReadOnlyComposable
fun isInDarkTheme(themeMode: Int): Boolean {
    return when (themeMode) {
        1 -> false
        2, 4 -> true
        else -> isSystemInDarkTheme()
    }
}

@Composable
private fun SystemBarStyle(
    darkMode: Boolean,
    statusBarScrim: Color = Color.Transparent,
    navigationBarScrim: Color = Color.Transparent
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    LaunchedEffect(darkMode, statusBarScrim, navigationBarScrim) {
        activity?.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                statusBarScrim.toArgb(),
                statusBarScrim.toArgb()
            ) { darkMode },
            navigationBarStyle = when {
                darkMode -> SystemBarStyle.dark(navigationBarScrim.toArgb())
                else -> SystemBarStyle.light(
                    navigationBarScrim.toArgb(),
                    navigationBarScrim.toArgb()
                )
            }
        )
    }
}
