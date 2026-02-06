package com.example.cakecompiler.presentation.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CakeColorScheme = lightColorScheme(
    primary = Color(0xFF6D4C41),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFF8F0),
    onPrimaryContainer = Color(0xFF3E2723),
    secondary = Color(0xFFD4AF37),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFF8E1),
    onSecondaryContainer = Color(0xFF5D4037),
    tertiary = Color(0xFFAD1457),
    onTertiary = Color.White,
    surface = Color(0xFFFFFBF5),
    onSurface = Color(0xFF3E2723),
    surfaceVariant = Color(0xFFF5EDE4),
    onSurfaceVariant = Color(0xFF5D4037),
    background = Color(0xFFFFF8F0),
    onBackground = Color(0xFF3E2723),
    outline = Color(0xFFBCAAA4)
)

@Composable
fun CakeCompilerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CakeColorScheme,
        content = content
    )
}
