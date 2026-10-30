package com.rapli.magivant.ui.theme

import androidx.compose.ui.graphics.Color

val Red = Color(0xFFF44336)
val Pink = Color(0xFFE91E63)
val Purple = Color(0xFF9C27B0)
val DeepPurple = Color(0xFF673AB7)
val Indigo = Color(0xFF3F51B5)
val Blue = Color(0xFF2196F3)
val Cyan = Color(0xFF00BCD4)
val Teal = Color(0xFF009688)
val Green = Color(0xFF4CAF50)
val Yellow = Color(0xFFFFEB3B)
val Amber = Color(0xFFFFC107)
val Orange = Color(0xFFFF9800)
val Brown = Color(0xFF795548)
val BlueGrey = Color(0xFF607D8B)
val Sakura = Color(0xFFFF9CA8)

data class KeyColorOption(
    val name: String,
    val color: Color
)

val keyColorOptions = listOf(
    KeyColorOption("Red", Red),
    KeyColorOption("Pink", Pink),
    KeyColorOption("Sakura", Sakura),
    KeyColorOption("Purple", Purple),
    KeyColorOption("Deep Purple", DeepPurple),
    KeyColorOption("Indigo", Indigo),
    KeyColorOption("Blue", Blue),
    KeyColorOption("Cyan", Cyan),
    KeyColorOption("Teal", Teal),
    KeyColorOption("Green", Green),
    KeyColorOption("Yellow", Yellow),
    KeyColorOption("Amber", Amber),
    KeyColorOption("Orange", Orange),
    KeyColorOption("Brown", Brown),
    KeyColorOption("Blue Grey", BlueGrey)
)
