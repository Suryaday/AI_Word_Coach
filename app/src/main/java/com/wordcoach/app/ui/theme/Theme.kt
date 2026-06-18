package com.wordcoach.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Calm, high-contrast palette that is easy on older eyes.
private val Blue = Color(0xFF1565C0)
private val BlueDark = Color(0xFF0D47A1)
private val Amber = Color(0xFFFFB300)
private val Cream = Color(0xFFFFF8F2)
private val InkText = Color(0xFF1A1A1A)

private val LightColors = lightColorScheme(
    primary = Blue,
    onPrimary = Color.White,
    secondary = Amber,
    onSecondary = Color(0xFF3A2A00),
    background = Cream,
    onBackground = InkText,
    surface = Color.White,
    onSurface = InkText,
    surfaceVariant = Color(0xFFE9EEF6),
    onSurfaceVariant = Color(0xFF2B2B2B)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF90CAF9),
    onPrimary = Color(0xFF00315B),
    secondary = Amber,
    background = Color(0xFF101418),
    onBackground = Color(0xFFF2F2F2),
    surface = Color(0xFF1B2026),
    onSurface = Color(0xFFF2F2F2)
)

// Larger-than-default typography so text is comfortable to read.
private val LargeTypography = Typography(
    displayLarge = TextStyle(fontSize = 52.sp, fontWeight = FontWeight.Bold),
    headlineLarge = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 22.sp, lineHeight = 30.sp),
    bodyMedium = TextStyle(fontSize = 19.sp, lineHeight = 26.sp),
    labelLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
)

@Composable
fun WordCoachTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = LargeTypography,
        content = content
    )
}
