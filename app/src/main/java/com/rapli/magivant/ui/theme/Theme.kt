package com.rapli.magivant.ui.theme

import android.app.Activity
import android.content.Context
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.core.view.WindowInsetsControllerCompat
import com.materialkolor.PaletteStyle
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
            1, 4 -> LIGHT
            2, 5 -> DARK
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

    fun toNightMode(): Int = when (this) {
        SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
        DARK, DARKAMOLED -> AppCompatDelegate.MODE_NIGHT_YES
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
    val view = LocalView.current
    val currentAppSettings = appSettings ?: ThemeController.getAppSettings(context)
    val systemDarkTheme = isSystemInDarkTheme()
    val darkTheme = currentAppSettings.colorMode.getDarkThemeValue(systemDarkTheme)
    val amoledMode = currentAppSettings.colorMode == ColorMode.DARKAMOLED
    val dynamicColor = currentAppSettings.keyColor == 0

    val baseScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            rememberDynamicColorScheme(
                seedColor = colorResource(id = android.R.color.system_accent1_500),
                isDark = darkTheme,
                isAmoled = amoledMode,
                style = PaletteStyle.TonalSpot
            )
        }
        !dynamicColor -> {
            rememberDynamicColorScheme(
                seedColor = Color(currentAppSettings.keyColor),
                isDark = darkTheme,
                isAmoled = amoledMode,
                style = PaletteStyle.TonalSpot
            )
        }
        else -> {
            if (darkTheme) darkColorScheme() else expressiveLightColorScheme()
        }
    }

    val colorScheme = remember(baseScheme, darkTheme, amoledMode) {
        buildMaterial3Scheme(
            scheme = baseScheme,
            isDark = darkTheme,
            isAmoled = amoledMode
        )
    }

    if (!view.isInEditMode) {
        SideEffect {
            val targetNightMode = currentAppSettings.colorMode.toNightMode()
            if (AppCompatDelegate.getDefaultNightMode() != targetNightMode) {
                AppCompatDelegate.setDefaultNightMode(targetNightMode)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                disableForceDarkRecursively(view)
                disableForceDarkRecursively(view.rootView)
            }
            val window = (context as? Activity)?.window ?: return@SideEffect
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.attributes = window.attributes.apply {
                    layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                disableForceDarkRecursively(window.decorView)
                window.isNavigationBarContrastEnforced = false
            }
            WindowInsetsControllerCompat(window, window.decorView).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = Typography,
        motionScheme = MotionScheme.expressive()
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = colorScheme.background,
            contentColor = colorScheme.onBackground
        ) {
            CompositionLocalProvider(LocalContentColor provides colorScheme.onSurface) {
                content()
            }
        }
    }
}

private fun disableForceDarkRecursively(view: View?) {
    if (view == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
    view.isForceDarkAllowed = false
    if (view is ViewGroup) {
        for (i in 0 until view.childCount) {
            disableForceDarkRecursively(view.getChildAt(i))
        }
    }
}

private fun buildMaterial3Scheme(
    scheme: ColorScheme,
    isDark: Boolean,
    isAmoled: Boolean
): ColorScheme {
    return if (!isDark) {
        val blackText = Color(0xFF000000)
        val charcoalText = Color(0xFF1C1B1F)

        scheme.copy(
            background = scheme.surfaceContainerLowest,
            surface = scheme.surface,
            surfaceContainerLowest = scheme.surfaceContainerLowest,
            surfaceContainerLow = scheme.surfaceContainerLow,
            surfaceContainer = scheme.surfaceContainer,
            surfaceContainerHigh = scheme.surfaceContainerHigh,
            surfaceContainerHighest = scheme.surfaceContainerHighest,
            surfaceVariant = scheme.surfaceVariant,
            surfaceTint = scheme.primary,
            onBackground = blackText,
            onSurface = blackText,
            onSurfaceVariant = charcoalText,
            onPrimaryContainer = blackText,
            onSecondaryContainer = blackText,
            onTertiaryContainer = blackText,
            outline = scheme.outline,
            outlineVariant = scheme.outlineVariant
        )
    } else {
        val darkBg = if (isAmoled) scheme.surfaceContainerLowest else scheme.background
        val darkSurface = if (isAmoled) scheme.surfaceContainerLowest else scheme.surface
        val onPrimaryDark = if (scheme.primary.luminance() > 0.4f) Color(0xFF000000) else scheme.onPrimary
        val onSecondaryDark = if (scheme.secondary.luminance() > 0.4f) Color(0xFF000000) else scheme.onSecondary
        val onTertiaryDark = if (scheme.tertiary.luminance() > 0.4f) Color(0xFF000000) else scheme.onTertiary

        scheme.copy(
            background = darkBg,
            surface = darkSurface,
            surfaceTint = scheme.primary,
            onPrimary = onPrimaryDark,
            onSecondary = onSecondaryDark,
            onTertiary = onTertiaryDark
        )
    }
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
