package com.rapli.magivant.ui.theme

import android.app.Activity
import android.content.Context
import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.core.view.WindowInsetsControllerCompat
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme
import com.rapli.magivant.ui.util.getHeaderImage
import kotlin.math.max
import kotlin.math.min

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

    val colorScheme = rememberDynamicContrastScheme(
        scheme = baseScheme,
        isDark = darkTheme,
        isAmoled = amoledMode
    )

    LaunchedEffect(darkTheme) {
        val window = (context as? Activity)?.window ?: return@LaunchedEffect
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.decorView.isForceDarkAllowed = false
            window.isNavigationBarContrastEnforced = false
        }
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
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

// Enforces WCAG contrast ratios dynamically across generated MaterialKolor tokens.
@Composable
private fun rememberDynamicContrastScheme(
    scheme: ColorScheme,
    isDark: Boolean,
    isAmoled: Boolean
): ColorScheme {
    val bg = when {
        isAmoled -> scheme.surfaceContainerLowest
        !isDark -> scheme.surfaceContainerLowest
        else -> scheme.background
    }
    val surface = when {
        isAmoled -> scheme.surfaceContainerLowest
        !isDark -> scheme.surfaceContainerLow
        else -> scheme.surface
    }
    val anchorInk = if (isDark) scheme.inverseOnSurface else scheme.inverseSurface

    return scheme.copy(
        background = bg,
        surface = surface,
        surfaceTint = scheme.primary,
        onBackground = ensureDynamicContrast(bg, scheme.onBackground, anchorInk, minRatio = 7.0f),
        onSurface = ensureDynamicContrast(surface, scheme.onSurface, anchorInk, minRatio = 7.0f),
        onSurfaceVariant = ensureDynamicContrast(
            scheme.surfaceContainer,
            scheme.onSurfaceVariant,
            anchorInk,
            minRatio = 4.5f
        ),
        onSecondaryContainer = ensureDynamicContrast(
            scheme.secondaryContainer,
            scheme.onSecondaryContainer,
            anchorInk,
            minRatio = 4.5f
        ),
        onPrimaryContainer = ensureDynamicContrast(
            scheme.primaryContainer,
            scheme.onPrimaryContainer,
            anchorInk,
            minRatio = 4.5f
        )
    )
}

private fun contrastRatio(a: Color, b: Color): Float {
    val l1 = a.luminance()
    val l2 = b.luminance()
    val hi = max(l1, l2)
    val lo = min(l1, l2)
    return (hi + 0.05f) / (lo + 0.05f)
}

private fun ensureDynamicContrast(
    background: Color,
    foreground: Color,
    anchor: Color,
    minRatio: Float
): Color {
    if (contrastRatio(background, foreground) >= minRatio) return foreground
    var lo = 0f
    var hi = 1f
    var best = foreground
    repeat(10) {
        val mid = (lo + hi) * 0.5f
        val candidate = lerp(foreground, anchor, mid)
        if (contrastRatio(background, candidate) >= minRatio) {
            best = candidate
            hi = mid
        } else {
            lo = mid
        }
    }
    return best
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
