package com.sameerasw.medrop.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.abs

object ColorUtil {
    private val pastelColors =
        listOf(
            Color(0xFFF48FB1),
            Color(0xFFCE93D8),
            Color(0xFFB39DDB),
            Color(0xFF9FA8DA),
            Color(0xFF90CAF9),
            Color(0xFF81D4FA),
            Color(0xFF80DEEA),
            Color(0xFF80CBC4),
            Color(0xFFA5D6A7),
            Color(0xFFC5E1A5),
            Color(0xFFE6EE9C),
            Color(0xFFFFF59D),
            Color(0xFFFFE082),
            Color(0xFFFFCC80),
            Color(0xFFFFAB91),
            Color(0xFFBCAAA4),
            Color(0xFFB0BEC5),
        )

    fun getPastelColorFor(key: Any): Color {
        val hash = abs(key.hashCode())
        val index = hash % pastelColors.size
        return pastelColors[index]
    }

    fun toRichColor(color: Color): Color {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color.toArgb(), hsv)
        hsv[1] = (hsv[1] * 2.5f).coerceIn(0.6f, 1f)
        hsv[2] = (hsv[2] * 0.65f).coerceIn(0.2f, 0.75f)
        return Color(android.graphics.Color.HSVToColor(hsv))
    }

    fun getVibrantColorFor(key: Any): Color {
        val baseColor = getPastelColorFor(key)
        return toRichColor(baseColor)
    }
}
