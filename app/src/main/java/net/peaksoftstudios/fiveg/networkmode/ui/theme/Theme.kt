package net.peaksoftstudios.fiveg.networkmode.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    secondary = AccentSignal,
    tertiary = WarningSignal,
    background = Color(0xFF0A0A0A),
    surface = Color(0xFF121212),
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFF001B1B),
    onBackground = Color(0xFFE8EAF6),
    onSurface = Color(0xFFD6D9E0)
)

// --- Light Mode (optional, subtle dark feel) ---
private val LightColorScheme = lightColorScheme(
    primary = PrimaryDark,
    secondary = AccentSignal,
    tertiary = WarningSignal,
    background = Color(0xFFF4F7FA),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFF003333),
    onBackground = Color(0xFF0A0A0A),
    onSurface = Color(0xFF1A1A1A)
)
@Composable
fun _4GNetworkModeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}