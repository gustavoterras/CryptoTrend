package br.com.terras.app.dsm.ui.component

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
import androidx.compose.ui.unit.dp
import br.com.terras.app.dsm.ui.annotation.ThemePreviews
import br.com.terras.app.dsm.ui.theme.DSMTheme

@Composable
fun CoinListItemShimmerRow(modifier: Modifier = Modifier) {
    val brush = ShimmerEffect()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1.45f),
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
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(86.dp)
                        .height(14.dp)
                        .background(brush = brush, shape = RoundedCornerShape(4.dp))
                )
                Box(
                    modifier = Modifier
                        .width(108.dp)
                        .height(10.dp)
                        .background(brush = brush, shape = RoundedCornerShape(4.dp))
                )
            }
        }

        Box(
            modifier = Modifier
                .width(74.dp)
                .height(24.dp)
                .padding(horizontal = 6.dp)
                .background(brush = brush, shape = RoundedCornerShape(6.dp))
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 6.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(68.dp)
                    .height(14.dp)
                    .background(brush = brush, shape = RoundedCornerShape(4.dp))
            )
            Box(
                modifier = Modifier
                    .width(44.dp)
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
            CoinListItemShimmerRow()
        }
    }
}