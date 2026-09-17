package net.peaksoftstudios.fiveg.networkmode.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** Walks the Context wrapper chain to find the hosting Activity, instead of assuming a direct cast. */
private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

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

    val view = LocalView.current
    if (!view.isInEditMode) {
        // Edge-to-edge (enableEdgeToEdge() in MainActivity) keeps the system bars
        // transparent so app content draws behind them — we intentionally do NOT paint
        // an opaque window.statusBarColor here. Doing so used to defeat edge-to-edge on
        // API < 35 (and is a no-op on API 35+, where the platform ignores it), which is
        // what caused edge-to-edge to not display for all users. We only adjust icon
        // contrast, which stays compatible with fully transparent bars.
        val useDarkStatusBarIcons = colorScheme.primary.luminance() > 0.5f
        SideEffect {
            val activity = view.context.findActivity()
            if (activity != null) {
                val insetsController = WindowCompat.getInsetsController(activity.window, view)
                insetsController.isAppearanceLightStatusBars = useDarkStatusBarIcons
                // The bottom NavigationBar uses colorScheme.surface, so system nav icons
                // must flip with it for contrast.
                insetsController.isAppearanceLightNavigationBars =
                    colorScheme.surface.luminance() > 0.5f
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}