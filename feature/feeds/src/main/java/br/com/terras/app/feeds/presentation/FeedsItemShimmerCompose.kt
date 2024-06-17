package br.com.terras.app.feeds.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(9.dp),
        modifier = Modifier.padding(top = 100.dp)
    ) {
        items(
            count = 3,
            itemContent = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                        .background(brush = ShimmerEffect(), shape = RoundedCornerShape(18.dp))
                )
            }
        )
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