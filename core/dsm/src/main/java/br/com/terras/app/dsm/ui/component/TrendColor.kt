package br.com.terras.app.dsm.ui.component

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

enum class TrendColor {
    NONE,
    UP,
    DOWN,
    FLAT;

    val color: Color
        @Composable
        @ReadOnlyComposable
        get() {
            val isDark = isSystemInDarkTheme()
            return when (this) {
                UP -> if (isDark) Color(0xFF123829) else Color(0xFFD6F3E6)
                DOWN -> if (isDark) Color(0xFF3B1818) else Color(0xFFFCE8E6)
                FLAT -> MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.14f else 0.08f)
                NONE -> Color.Transparent
            }
        }

    val onColor: Color
        @Composable
        @ReadOnlyComposable
        get() {
            val isDark = isSystemInDarkTheme()
            return when (this) {
                UP -> if (isDark) Color(0xFF34E89E) else Color(0xFF076E44)
                DOWN -> if (isDark) Color(0xFFFF7A6E) else Color(0xFFB92B1E)
                FLAT, NONE -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        }

    val textColor: Color
        @Composable
        @ReadOnlyComposable
        get() {
            val isDark = isSystemInDarkTheme()
            return when (this) {
                UP -> if (isDark) Color(0xFF34E89E) else Color(0xFF0D8756)
                DOWN -> if (isDark) Color(0xFFFF7A6E) else Color(0xFFC93527)
                FLAT -> MaterialTheme.colorScheme.onSurfaceVariant
                NONE -> MaterialTheme.colorScheme.surface
            }
        }
}