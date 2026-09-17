package net.peaksoftstudios.fiveg.networkmode.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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

private val LightColorScheme = lightColorScheme(
    primary = Navy,
    onPrimary = Color.White,
    primaryContainer = NavyLight,
    onPrimaryContainer = Color.White,
    secondary = MintDark,
    onSecondary = Color.White,
    secondaryContainer = MintSoft,
    onSecondaryContainer = MintDark,
    tertiary = Mint,
    onTertiary = Navy,
    tertiaryContainer = AmberSoft,
    onTertiaryContainer = AmberDeep,
    background = Canvas,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = SurfaceMuted,
    onSurfaceVariant = InkMuted,
    surfaceContainer = SurfaceMuted,
    surfaceContainerLow = Canvas,
    surfaceContainerHigh = SurfaceMuted,
    outline = Border,
    outlineVariant = Border,
    error = Danger,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = Mint,
    onPrimary = CanvasDark,
    primaryContainer = NavyLight,
    onPrimaryContainer = Color.White,
    secondary = Mint,
    onSecondary = CanvasDark,
    secondaryContainer = MintSoftDark,
    onSecondaryContainer = Mint,
    tertiary = Mint,
    onTertiary = CanvasDark,
    tertiaryContainer = Color(0xFF3D3316),
    onTertiaryContainer = Color(0xFFF5C542),
    background = CanvasDark,
    onBackground = InkDark,
    surface = SurfaceDark,
    onSurface = InkDark,
    surfaceVariant = SurfaceMutedDark,
    onSurfaceVariant = InkMutedDark,
    surfaceContainer = SurfaceMutedDark,
    surfaceContainerLow = CanvasDark,
    surfaceContainerHigh = SurfaceMutedDark,
    outline = BorderDark,
    outlineVariant = BorderDark,
    error = Danger,
    onError = Color.White
)

@Composable
fun _4GNetworkModeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Brand palette is fixed (navy + mint); dynamic colour is intentionally not used.
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        // Edge-to-edge (enableEdgeToEdge() in MainActivity) keeps the system bars
        // transparent so app content draws behind them. We only adjust icon contrast
        // so the bars stay compatible with fully transparent backgrounds.
        val lightBars = colorScheme.background.luminance() > 0.5f
        SideEffect {
            val activity = view.context.findActivity()
            if (activity != null) {
                val insetsController = WindowCompat.getInsetsController(activity.window, view)
                insetsController.isAppearanceLightStatusBars = lightBars
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
