package com.jongwook.dshub.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary            = Primary,
    onPrimary          = OnPrimary,
    primaryContainer   = PrimaryContainer,
    onPrimaryContainer = Color(0xFF001D36),
    secondary          = Secondary,
    onSecondary        = OnSecondary,
    tertiary           = Tertiary,
    background         = Background,
    onBackground       = Color(0xFF101820),
    surface            = Surface,
    onSurface          = Color(0xFF101820),
    surfaceVariant     = SurfaceVariant,
    onSurfaceVariant   = Color(0xFF44546A),
    outline            = Outline,
    outlineVariant     = Color(0xFFC8D4E8)
)

private val DarkColorScheme = darkColorScheme(
    primary            = PrimaryDark,
    onPrimary          = Color(0xFF003258),
    primaryContainer   = PrimaryContainerDark,
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary          = SecondaryDark,
    onSecondary        = Color(0xFF263238),
    tertiary           = TertiaryDark,
    background         = BackgroundDark,
    onBackground       = Color(0xFFDCE6F8),
    surface            = SurfaceDark,
    onSurface          = Color(0xFFDCE6F8),
    surfaceVariant     = SurfaceVariantDark,
    onSurfaceVariant   = Color(0xFFAABDD4),
    outline            = Color(0xFF5A6D85),
    outlineVariant     = Color(0xFF354558)
)

@Composable
fun DSHubTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
