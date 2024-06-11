package br.com.terras.app.people.presentation

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.terras.app.dsm.ui.annotation.ThemePreviews
import br.com.terras.app.dsm.ui.component.CoinListItemView
import br.com.terras.app.dsm.ui.component.StickyHeaderView
import br.com.terras.app.dsm.ui.theme.DSMTheme

@OptIn(ExperimentalFoundationApi::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun PeopleActivityView(
    viewModel: PeopleViewModel = hiltViewModel()
) {

    Scaffold {

        val coinsList by viewModel.coinsList.collectAsStateWithLifecycle(lifecycleOwner = LocalLifecycleOwner.current)

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(9.dp)
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

            items(coinsList.size) {
                coinsList[it].run {
                    CoinListItemView(
                        coinPosition = marketCapRank,
                        coinLogo = image,
                        coinName = name,
                        coinSymbol = symbol,
                        coinPrice = price,
                        coinPriceChancePercentage = priceChangePercentage,
                        trendColor = trendColor
                    )
                }
            }
        }
    }
}

@ThemePreviews
@Composable
private fun PeopleActivityPreview() {
    DSMTheme {
        Surface {
            PeopleActivityView()
        }
    }
}