package br.com.terras.app.dsm.ui.component

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
import br.com.terras.app.dsm.ui.theme.DSMTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CoinListItemShimmerView() {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(9.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 18.dp, end = 18.dp)
    ) {
        stickyHeader {
            Surface(
                modifier = Modifier
                    .fillParentMaxWidth()
                    .height(100.dp),
                color = MaterialTheme.colorScheme.background
            ) {
                StickyHeaderView()
            }
        }
        items(
            count = 10,
            itemContent = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(66.dp)
                        .background(brush = ShimmerEffect(), shape = RoundedCornerShape(18.dp))
                )
            }
        )
    }
}

@ThemePreviews
@Composable
private fun PeopleActivityPreview() {
    DSMTheme {
        Surface {
            CoinListItemShimmerView()
        }
    }
}