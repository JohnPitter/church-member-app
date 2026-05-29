package com.churchmanagement.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Cores padrão (usadas até as cores da organização carregarem da coleção `settings`).
val DefaultPrimary = Color(0xFF3B82F6)
val DefaultSecondary = Color(0xFF8B5CF6)

@Composable
fun AppTheme(
    primary: Color = DefaultPrimary,
    secondary: Color = DefaultSecondary,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = primary,
            secondary = secondary,
            background = Color(0xFF0F172A),
            surface = Color(0xFF1E293B),
        )
    } else {
        lightColorScheme(
            primary = primary,
            secondary = secondary,
            background = Color(0xFFF8FAFC),
            surface = Color(0xFFFFFFFF),
        )
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
