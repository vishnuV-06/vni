package com.vni.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VniDarkColorScheme = darkColorScheme(
    primary           = PurplePrimary,
    onPrimary         = TextWhite,
    primaryContainer  = PurpleFaint,
    secondary         = PurpleLight,
    onSecondary       = TextWhite,
    background        = DeepNavy,
    onBackground      = TextWhite,
    surface           = SurfaceNavy,
    onSurface         = TextWhite,
    surfaceVariant    = CardNavy,
    outline           = PurpleBorder,
    error             = DangerRed,
)

@Composable
fun VniTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VniDarkColorScheme,
        typography  = VniTypography,
        content     = content
    )
}
