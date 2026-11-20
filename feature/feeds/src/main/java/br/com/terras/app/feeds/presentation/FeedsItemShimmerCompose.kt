package br.com.terras.app.feeds.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.terras.app.dsm.ui.annotation.ThemePreviews
import br.com.terras.app.dsm.ui.component.ShimmerEffect
import br.com.terras.app.dsm.ui.theme.DSMTheme

@Composable
fun FeedsItemShimmerCompose() {
    val brush = ShimmerEffect()
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(top = 100.dp, start = 18.dp, end = 18.dp, bottom = 100.dp)
    ) {
        items(count = 3) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .background(brush = brush, shape = RoundedCornerShape(14.dp))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(18.dp)
                        .background(brush = brush, shape = RoundedCornerShape(4.dp))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.65f)
                        .height(14.dp)
                        .background(brush = brush, shape = RoundedCornerShape(4.dp))
                )
            }
        }
    }
}

@ThemePreviews
@Composable
private fun FeedsItemShimmerComposePreview() {
    DSMTheme {
        Surface {
            FeedsItemShimmerCompose()
        }
    }
}