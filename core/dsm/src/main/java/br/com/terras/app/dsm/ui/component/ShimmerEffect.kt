package br.com.terras.app.dsm.ui.component

import android.annotation.SuppressLint
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@SuppressLint("ComposableNaming")
@Composable
fun ShimmerEffect(showShimmer: Boolean = true, targetValue: Float = 1100f): Brush {
    return if (showShimmer) {
        val baseColor = MaterialTheme.colorScheme.surface
        val shimmerColors = listOf(
            baseColor.copy(alpha = 0.08f),
            baseColor.copy(alpha = 0.19f),
            baseColor.copy(alpha = 0.08f),
        )

        val transition = rememberInfiniteTransition(label = "dsmShimmerTransition")
        val translateAnimation = transition.animateFloat(
            initialValue = 0f,
            targetValue = targetValue,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "dsmShimmerTranslate"
        )
        Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(x = translateAnimation.value - 360f, y = 0f),
            end = Offset(x = translateAnimation.value, y = 180f)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color.Transparent, Color.Transparent),
            start = Offset.Zero,
            end = Offset.Zero
        )
    }
}