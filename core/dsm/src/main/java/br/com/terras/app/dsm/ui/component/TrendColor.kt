package br.com.terras.app.dsm.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

enum class TrendColor {
    UP,
    DOWN,
    FLAT;

    val color: Color
        @Composable
        @ReadOnlyComposable
        get() {
            return when (this) {
                UP -> MaterialTheme.colorScheme.scrim
                DOWN -> MaterialTheme.colorScheme.error
                FLAT -> MaterialTheme.colorScheme.primary
            }
        }
}