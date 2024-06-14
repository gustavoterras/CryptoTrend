package br.com.terras.app.feeds.presentation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.terras.app.dsm.ui.annotation.ThemePreviews
import br.com.terras.app.dsm.ui.component.ShimmerEffect
import br.com.terras.app.dsm.ui.component.StickyHeaderView
import br.com.terras.app.dsm.ui.theme.DSMTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FeedsItemShimmerCompose() {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(9.dp),
        modifier = Modifier
            .fillMaxSize()
    ) {
        stickyHeader {
            Surface(
                modifier = Modifier
                    .fillParentMaxWidth()
                    .height(100.dp),
                color = MaterialTheme.colorScheme.background
            ) {
                StickyHeaderView(
                    Modifier.padding(start = 18.dp, end = 18.dp)
                )
            }
        }
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