package com.example.flashlearn.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private fun buildColorScheme(t: ColorTokens): ColorScheme = lightColorScheme(
    primary              = t.buttonBackground,
    onPrimary            = t.buttonText,
    primaryContainer     = t.navBarActiveTab,
    onPrimaryContainer   = t.navBarActiveTabText,

    secondary            = t.buttonSecondary,
    onSecondary          = t.buttonSecondaryText,
    secondaryContainer   = t.chipBackground,
    onSecondaryContainer = t.chipText, // chipText is light (Indigo90) in dark mode → readable on Indigo30

    tertiary             = t.cardCorrect,
    onTertiary           = t.cardCorrectText,
    tertiaryContainer    = t.cardCorrectContainer,
    onTertiaryContainer  = t.cardCorrectContainerText,

    error                = t.cardIncorrect,
    onError              = t.cardIncorrectText,
    errorContainer       = t.cardIncorrectContainer,
    onErrorContainer     = t.cardIncorrectContainerText,

    background           = t.screenBackground,
    onBackground         = t.textPrimary,

    surface              = t.screenBackground,
    onSurface            = t.textPrimary,
    surfaceVariant       = t.panelBackground,
    onSurfaceVariant     = t.panelText,

    outline              = t.textFieldBorder,
    outlineVariant       = t.navBarInactiveIcon,
)

@Composable
fun FlashLearnTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val tokens = if (darkTheme) DarkTokens else LightTokens
    val colorScheme = buildColorScheme(tokens)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
