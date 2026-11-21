package com.rapli.magivant.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.util.prefs.Preferences

private val lightScheme = lightColorScheme(
    primary = Color(0xFF4D6097),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE2FF),
    onPrimaryContainer = Color(0xFF071943),
    secondary = Color(0xFF595D72),
    secondaryContainer = Color(0xFFE0E1F9),
    surface = Color(0xFFFAF8FF),
    surfaceContainer = Color(0xFFF0EFF7)
)

private val darkScheme = darkColorScheme(
    primary = Color(0xFFB7C5FF),
    onPrimary = Color(0xFF1D2F60),
    primaryContainer = Color(0xFF354779),
    onPrimaryContainer = Color(0xFFDDE2FF),
    secondary = Color(0xFFC3C5DD),
    secondaryContainer = Color(0xFF42465A),
    surface = Color(0xFF111319),
    surfaceContainer = Color(0xFF1D2027)
)

fun main() = application {
    val controller = remember { DesktopDacController() }
    val settings = remember { Preferences.userRoot().node("com/rapli/magivant-dac") }
    var darkTheme by remember { mutableStateOf(settings.getBoolean("dark_theme", false)) }

    DisposableEffect(controller) {
        onDispose { controller.close() }
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Magivant DAC",
        state = rememberWindowState(width = 1440.dp, height = 920.dp)
    ) {
        MaterialTheme(colorScheme = if (darkTheme) darkScheme else lightScheme) {
            DesktopApp(
                controller = controller,
                modifier = Modifier.fillMaxSize(),
                isDarkTheme = darkTheme,
                onToggleTheme = {
                    darkTheme = !darkTheme
                    settings.putBoolean("dark_theme", darkTheme)
                }
            )
        }
    }
}
