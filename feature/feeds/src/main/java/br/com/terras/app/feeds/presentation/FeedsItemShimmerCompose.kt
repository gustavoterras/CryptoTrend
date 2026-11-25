package br.com.terras.app.feeds.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import br.com.terras.app.dsm.ui.annotation.ThemePreviews
import br.com.terras.app.dsm.ui.component.ShimmerEffect
import br.com.terras.app.dsm.ui.theme.DSMTheme

@Composable
fun FeedsItemShimmerCompose() {
    val brush = ShimmerEffect()
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(top = 96.dp, bottom = 100.dp),
        modifier = Modifier.testTag("feeds_list_skeleton")
    ) {
        items(count = 3) { index ->
            Column(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .background(brush = brush, shape = RoundedCornerShape(14.dp))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(if (index % 2 == 0) 0.90f else 0.82f)
                            .height(16.dp)
                            .background(brush = brush, shape = RoundedCornerShape(4.dp))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.68f)
                            .height(12.dp)
                            .background(brush = brush, shape = RoundedCornerShape(4.dp))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.52f)
                            .height(12.dp)
                            .background(brush = brush, shape = RoundedCornerShape(4.dp))
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.End)
                            .width(60.dp)
                            .height(12.dp)
                            .background(brush = brush, shape = RoundedCornerShape(4.dp))
                    )
                }
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 18.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
    }
}

@ThemePreviews
@Composable
private fun FeedsItemShimmerComposePreview() {
    DSMTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            FeedsItemShimmerCompose()
        }
    }
}