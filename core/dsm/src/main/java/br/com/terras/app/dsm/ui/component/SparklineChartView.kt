package br.com.terras.app.dsm.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import br.com.terras.app.dsm.ui.annotation.ThemePreviews
import br.com.terras.app.dsm.ui.theme.DSMTheme

@Composable
fun SparklineChartView(
    prices: List<Double>,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    if (prices.size < 2) return

    val minVal = remember(prices) { prices.min() }
    val maxVal = remember(prices) { prices.max() }
    val range = remember(minVal, maxVal) { (maxVal - minVal).takeIf { it > 0.0 } ?: 1.0 }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val padY = 2.dp.toPx()
        val usableHeight = (height - padY * 2f).coerceAtLeast(1f)

        fun pointX(index: Int): Float =
            (index.toFloat() / prices.lastIndex) * width

        fun pointY(price: Double): Float {
            val ratio = if (maxVal == minVal) 0.5f else ((price - minVal) / range).toFloat()
            return padY + usableHeight * (1f - ratio)
        }

        val strokePath = Path().apply {
            moveTo(pointX(0), pointY(prices.first()))
            for (i in 0 until prices.lastIndex) {
                val x1 = pointX(i)
                val y1 = pointY(prices[i])
                val x2 = pointX(i + 1)
                val y2 = pointY(prices[i + 1])
                val midX = (x1 + x2) / 2f
                cubicTo(midX, y1, midX, y2, x2, y2)
            }
        }

        val fillPath = android.graphics.Path(strokePath.asAndroidPath())
            .asComposePath()
            .apply {
                lineTo(width, height)
                lineTo(0f, height)
                close()
            }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    lineColor.copy(alpha = 0.28f),
                    Color.Transparent
                ),
                startY = 0f,
                endY = height
            )
        )

        drawPath(
            path = strokePath,
            color = lineColor,
            style = Stroke(
                width = 1.6.dp.toPx(),
                cap = StrokeCap.Round
            )
        )
    }
}

@ThemePreviews
@Composable
private fun SparklineChartViewPreview() {
    DSMTheme {
        Surface(color = MaterialTheme.colorScheme.primary) {
            SparklineChartView(
                prices = listOf(10.0, 12.0, 11.2, 13.5, 12.8, 14.2, 13.9, 15.1),
                lineColor = Color(0xFF12A86E),
                modifier = Modifier
                    .width(74.dp)
                    .height(30.dp)
            )
        }
    }
}
