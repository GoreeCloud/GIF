package com.goreecloud.gifs.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Source: GoreeCloud/gc-ui src/tokens.css, alpha Iris family (verified 2026-11-19).
private val light = lightColorScheme(
    primary = Color(0xFF5146C8), onPrimary = Color.White,
    primaryContainer = Color(0xFFE8E5FF), onPrimaryContainer = Color(0xFF30266F),
    background = Color(0xFFF6F7FB), onBackground = Color(0xFF1B2030),
    surface = Color.White, onSurface = Color(0xFF1B2030),
    surfaceVariant = Color(0xFFEEF0F7), onSurfaceVariant = Color(0xFF505B70),
    outline = Color(0xFF68758C), error = Color(0xFFB3261E)
)
private val dark = darkColorScheme(
    primary = Color(0xFFBFB8FF), onPrimary = Color(0xFF272057),
    primaryContainer = Color(0xFF413974), onPrimaryContainer = Color(0xFFE8E5FF),
    background = Color(0xFF0D121D), onBackground = Color(0xFFF2F4FB),
    surface = Color(0xFF171D2A), onSurface = Color(0xFFF2F4FB),
    surfaceVariant = Color(0xFF252D3D), onSurfaceVariant = Color(0xFFCED5E5),
    outline = Color(0xFF9CA8BC), error = Color(0xFFFFB4AB)
)

@Composable
fun GoreeCloudTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) dark else light, content = content)
}
