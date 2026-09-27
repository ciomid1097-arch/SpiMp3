package com.spimp3.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Convenience accessors for the accent colors from any composable. */
@Composable
fun accentColor(): Color = LocalExtendedColors.current.accent

@Composable
fun onAccentColor(): Color = LocalExtendedColors.current.onAccent

@Composable
fun secondaryText(): Color = MaterialTheme.colorScheme.onSurfaceVariant
