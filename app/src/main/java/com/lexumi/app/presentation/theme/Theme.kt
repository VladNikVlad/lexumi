package com.lexumi.app.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LexumiLightColors = lightColorScheme(
    primary = LexumiIndigo,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    secondary = LexumiOutline,
    background = LexumiBgStart,
    surface = androidx.compose.ui.graphics.Color.White,
    onBackground = LexumiIndigo,
    onSurface = LexumiIndigo,
    error = LexumiError,
    outline = LexumiOutline,
)

private val LexumiDarkColors = darkColorScheme(
    primary = LexumiPurpleLight,
    onPrimary = LexumiIndigo,
    secondary = LexumiTealLight,
    background = LexumiIndigo,
    surface = androidx.compose.ui.graphics.Color(0xFF241C55),
    onBackground = androidx.compose.ui.graphics.Color.White,
    onSurface = androidx.compose.ui.graphics.Color.White,
    error = LexumiError,
    outline = LexumiOutline,
)

@Composable
fun LexumiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) LexumiDarkColors else LexumiLightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = LexumiTypography,
        shapes = LexumiShapes,
        content = content,
    )
}
