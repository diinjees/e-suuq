package com.example.ui.theme


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

import android.app.Activity

import androidx.core.view.WindowCompat

private val DarkColorScheme =
  darkColorScheme(
    primary = BrandGreenPrimary,
    secondary = BrandGoldSecondary,
    tertiary = BrandRedTertiary,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = DarkOnPrimary,
    onBackground = Color(0xFFE2E3DE),
    onSurface = Color(0xFFE2E3DE)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = BrandGreenPrimary,
    secondary = BrandGoldSecondary,
    tertiary = BrandRedTertiary,
    background = LightBackground,
    surface = LightSurface,
    onPrimary = LightOnPrimary,
    onBackground = Color(0xFF191C1A),
    onSurface = Color(0xFF191C1A)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Force E-Suuq Brand colors to prevent standard Android system overrides
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as? Activity)?.window
      if (window != null) {
        window.statusBarColor = colorScheme.background.toArgb()
        window.navigationBarColor = colorScheme.background.toArgb()
        val windowInsetsController = WindowCompat.getInsetsController(window, view)
        windowInsetsController.isAppearanceLightStatusBars = !darkTheme
        windowInsetsController.isAppearanceLightNavigationBars = !darkTheme
      }
    }
  }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
