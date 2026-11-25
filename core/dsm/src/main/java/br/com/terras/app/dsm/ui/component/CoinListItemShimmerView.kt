package br.com.terras.app.dsm.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import br.com.terras.app.dsm.ui.annotation.ThemePreviews
import br.com.terras.app.dsm.ui.theme.DSMTheme

@Composable
fun CoinListItemShimmerRow(
    modifier: Modifier = Modifier,
    variantIndex: Int = 0
) {
    val brush = ShimmerEffect()
    val variant = ((variantIndex % 3) + 3) % 3
    val symbolWidth = when (variant) {
        0 -> 88.dp
        1 -> 74.dp
        else -> 96.dp
    }
    val metaWidth = when (variant) {
        0 -> 108.dp
        1 -> 92.dp
        else -> 116.dp
    }
    val priceWidth = when (variant) {
        0 -> 72.dp
        1 -> 64.dp
        else -> 78.dp
    }
    val changeWidth = when (variant) {
        0 -> 46.dp
        1 -> 40.dp
        else -> 50.dp
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1.55f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(brush = brush, shape = CircleShape)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(symbolWidth)
                        .height(14.dp)
                        .background(brush = brush, shape = RoundedCornerShape(4.dp))
                )
                Box(
                    modifier = Modifier
                        .width(metaWidth)
                        .height(10.dp)
                        .background(brush = brush, shape = RoundedCornerShape(4.dp))
                )
            }
        }

        Box(
            modifier = Modifier
                .width(70.dp)
                .height(30.dp)
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp)
            ) {
                val h = size.height
                val w = size.width
                val path = Path().apply {
                    when (variant) {
                        0 -> {
                            moveTo(0f, h * 0.65f)
                            lineTo(w * 0.25f, h * 0.40f)
                            lineTo(w * 0.50f, h * 0.72f)
                            lineTo(w * 0.75f, h * 0.28f)
                            lineTo(w, h * 0.45f)
                        }
                        1 -> {
                            moveTo(0f, h * 0.35f)
                            lineTo(w * 0.25f, h * 0.60f)
                            lineTo(w * 0.55f, h * 0.32f)
                            lineTo(w * 0.80f, h * 0.70f)
                            lineTo(w, h * 0.52f)
                        }
                        else -> {
                            moveTo(0f, h * 0.55f)
                            lineTo(w * 0.28f, h * 0.30f)
                            lineTo(w * 0.52f, h * 0.52f)
                            lineTo(w * 0.76f, h * 0.25f)
                            lineTo(w, h * 0.62f)
                        }
                    }
                }
                drawPath(
                    path = path,
                    brush = brush,
                    style = Stroke(
                        width = 2.2.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 6.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(priceWidth)
                    .height(14.dp)
                    .background(brush = brush, shape = RoundedCornerShape(4.dp))
            )
            Box(
                modifier = Modifier
                    .width(changeWidth)
                    .height(10.dp)
                    .background(brush = brush, shape = RoundedCornerShape(4.dp))
            )
        }
    }
}

@ThemePreviews
@Composable
private fun CoinListItemShimmerPreview() {
    DSMTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column {
                CoinListItemShimmerRow(variantIndex = 0)
                CoinListItemShimmerRow(variantIndex = 1)
                CoinListItemShimmerRow(variantIndex = 2)
            }
        }
    }
}