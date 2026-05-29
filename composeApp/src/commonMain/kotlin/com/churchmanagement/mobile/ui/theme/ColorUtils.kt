package com.churchmanagement.mobile.ui.theme

import androidx.compose.ui.graphics.Color

/** Converte uma cor hex ("#RRGGBB" ou "#AARRGGBB") em [Color]; usa [fallback] se inválida. */
fun parseHexColor(hex: String?, fallback: Color): Color {
    val clean = hex?.trim()?.removePrefix("#") ?: return fallback
    return try {
        when (clean.length) {
            6 -> Color(0xFF000000 or clean.toLong(16))
            8 -> Color(clean.toLong(16))
            else -> fallback
        }
    } catch (e: NumberFormatException) {
        fallback
    }
}
