package com.rapli.magivant.ui.theme

import androidx.compose.ui.graphics.Color

val Blue = Color(0xFF1A73E8)
val Red = Color(0xFFEA4335)
val Green = Color(0xFF34A853)
val Purple = Color(0xFF9333EA)
val Orange = Color(0xFFFB8C00)
val Teal = Color(0xFF009688)
val Pink = Color(0xFFE91E63)
val Sakura = Color(0xFFFF9CA8)
val Indigo = Color(0xFF3F51B5)
val Cyan = Color(0xFF00BCD4)
val Amber = Color(0xFFFFC107)
val DeepPurple = Color(0xFF673AB7)
val Brown = Color(0xFF795548)
val Yellow = Color(0xFFFFEB3B)
val BlueGrey = Color(0xFF607D8B)

data class KeyColorOption(
    val name: String,
    val color: Color
)

val keyColorOptions = listOf(
    KeyColorOption("Blue", Blue),
    KeyColorOption("Red", Red),
    KeyColorOption("Green", Green),
    KeyColorOption("Purple", Purple),
    KeyColorOption("Orange", Orange),
    KeyColorOption("Teal", Teal),
    KeyColorOption("Pink", Pink),
    KeyColorOption("Sakura", Sakura),
    KeyColorOption("Indigo", Indigo),
    KeyColorOption("Cyan", Cyan),
    KeyColorOption("Amber", Amber),
    KeyColorOption("Deep Purple", DeepPurple),
    KeyColorOption("Brown", Brown),
    KeyColorOption("Yellow", Yellow),
    KeyColorOption("Blue Grey", BlueGrey)
)
