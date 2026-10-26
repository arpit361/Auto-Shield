package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val AutomotiveDarkColorScheme = darkColorScheme(
    primary = BraveOrange,
    onPrimary = Color.White,
    primaryContainer = BraveOrangeDark,
    onPrimaryContainer = Color.White,
    secondary = BraveAmber,
    onSecondary = Color.Black,
    secondaryContainer = CockpitSurfaceVariant,
    onSecondaryContainer = CockpitTextPrimary,
    tertiary = ShieldCyan,
    onTertiary = Color.Black,
    background = CockpitBackground,
    onBackground = CockpitTextPrimary,
    surface = CockpitSurface,
    onSurface = CockpitTextPrimary,
    surfaceVariant = CockpitSurfaceVariant,
    onSurfaceVariant = CockpitTextSecondary,
    outline = CockpitBorder
)

private val AutomotiveLightColorScheme = lightColorScheme(
    primary = BraveOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBCF),
    onPrimaryContainer = Color(0xFF3B0900),
    secondary = BraveAmber,
    onSecondary = Color.Black,
    background = Color(0xFFF7F8FA),
    onBackground = Color(0xFF13151A),
    surface = Color.White,
    onSurface = Color(0xFF13151A),
    surfaceVariant = Color(0xFFE5E7EB),
    onSurfaceVariant = Color(0xFF4B5563),
    outline = Color(0xFFCBD5E1)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to dark for automotive contrast
    dynamicColor: Boolean = false, // Keep consistent automotive Brave brand aesthetic
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) AutomotiveDarkColorScheme else AutomotiveLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
