package com.example.fltctl.ui.theme

import android.app.Activity
import android.os.Build
import androidx.annotation.FloatRange
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.core.view.ViewCompat
import com.example.fltctl.configs.SettingKeys
import com.example.fltctl.configs.SettingsCache

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

private val eInkColorScheme = lightColorScheme(
    background = Color.White,
    surface = Color.White,
    primary = Color.Black,
    secondary = Color.Black,
    tertiary = Color.Black,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    primaryContainer = Color.White,
    secondaryContainer = Color.White,
    tertiaryContainer = Color.White,
    onPrimaryContainer = Color.Black,
    onSecondaryContainer = Color.Black,
    onTertiaryContainer = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
    onSurfaceVariant = Color.Black,
    surfaceVariant = Color.White
)

val LocalEInkMode = compositionLocalOf { false }

val isInEInkMode: Boolean
    @Composable get() = LocalEInkMode.current

@Composable
fun FltCtlTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    eInkTheme: Boolean = SettingsCache[SettingKeys.UI_EINK_MODE] ?: false,
    @FloatRange(0.5, 1.3) densityScale: Float = 1f,
    @FloatRange(0.7, 1.5) fontScale: Float = 1f,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        eInkTheme -> eInkColorScheme.copy()
        !eInkTheme && dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme.copy()
        else -> LightColorScheme.copy()
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (view.context as Activity).window.statusBarColor = colorScheme.primary.toArgb()
            ViewCompat.getWindowInsetsController(view)?.isAppearanceLightStatusBars = darkTheme
        }
    }

    val density = LocalDensity.current
    val scaledDensity = if (densityScale != 1f || fontScale != 1f) Density(density.density * densityScale, density.fontScale * fontScale) else density


    CompositionLocalProvider(LocalDensity provides scaledDensity, LocalEInkMode provides eInkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
        ) {
            content.invoke()
        }
    }

}