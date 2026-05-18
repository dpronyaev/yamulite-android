package dev.pdv.yamulite.ui.theme

import android.app.Activity
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import dev.pdv.yamulite.data.settings.ThemePreference

private val LightScheme = lightColorScheme(
    primary = Color(0xFFFFCC00),
    onPrimary = Color.Black,
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFFFCC00),
    onPrimary = Color.Black,
)

// Роскошная бархатная — глубокий пурпур с золотисто-розовыми акцентами
private val VelvetScheme = darkColorScheme(
    primary = Color(0xFFE8A0B4),           // розовое золото
    onPrimary = Color(0xFF3D0020),
    primaryContainer = Color(0xFF5C1035),
    onPrimaryContainer = Color(0xFFFFD9E3),
    secondary = Color(0xFFCFB8F0),         // мягкая лаванда
    onSecondary = Color(0xFF2C1060),
    secondaryContainer = Color(0xFF432878),
    onSecondaryContainer = Color(0xFFECDCFF),
    tertiary = Color(0xFFFFD700),          // золото
    onTertiary = Color(0xFF2A1800),
    tertiaryContainer = Color(0xFF3D2400),
    onTertiaryContainer = Color(0xFFFFE08A),
    background = Color(0xFF0D0520),        // бархатный тёмно-фиолетовый
    onBackground = Color(0xFFF0E8FF),
    surface = Color(0xFF160B30),
    onSurface = Color(0xFFF0E8FF),
    surfaceVariant = Color(0xFF2A1550),
    onSurfaceVariant = Color(0xFFCFC0EA),
    outline = Color(0xFF7C6A9A),
    outlineVariant = Color(0xFF3D2A60),
    error = Color(0xFFCF6679),
    onError = Color(0xFF680028),
    errorContainer = Color(0xFF93003F),
    onErrorContainer = Color(0xFFFFD9E3),
    inverseSurface = Color(0xFFF0E8FF),
    inverseOnSurface = Color(0xFF0D0520),
    inversePrimary = Color(0xFF8B1A4A),
)

@Composable
fun YaMuLiteTheme(
    theme: ThemePreference = ThemePreference.System,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val ctx = LocalContext.current

    val colors = when (theme) {
        ThemePreference.System -> when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                if (systemDark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
            systemDark -> DarkScheme
            else -> LightScheme
        }
        ThemePreference.Light -> LightScheme
        ThemePreference.Dark -> DarkScheme
        ThemePreference.Velvet -> VelvetScheme
    }

    val darkBars = theme == ThemePreference.Dark || theme == ThemePreference.Velvet ||
        (theme == ThemePreference.System && systemDark)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colors.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkBars
        }
    }

    MaterialTheme(colorScheme = colors, content = content)
}
