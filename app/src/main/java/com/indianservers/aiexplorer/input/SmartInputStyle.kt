package com.indianservers.aiexplorer.input

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/** Presentation tokens shared by the intelligent editor and its keyboard. */
internal object SmartInputStyle {
    val Surface = Color(0xFF081222)
    val Inset = Color(0xFF040C19)
    val Blue = Color(0xFF729BFF)
    val Violet = Color(0xFFA77AFF)
    val Panel = Brush.linearGradient(listOf(Color(0xFF15213B), Surface, Color(0xFF060D1C)))
    val Border = Brush.linearGradient(listOf(Blue, Color(0xFF465D9D), Violet))

    fun key(accent: Color, selected: Boolean = false, highContrast: Boolean = false): Brush =
        Brush.verticalGradient(
            when {
                highContrast -> listOf(Color.Black, Color.Black)
                selected -> listOf(Color(0xFF703BDA), Color(0xFF3D207F))
                else -> listOf(lerp(Color(0xFF18243A), accent, .10f), Surface)
            },
        )
}
