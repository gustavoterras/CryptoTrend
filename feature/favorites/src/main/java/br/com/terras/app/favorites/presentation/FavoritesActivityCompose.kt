package br.com.terras.app.favorites.presentation

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.terras.app.dsm.ui.annotation.ThemePreviews
import br.com.terras.app.dsm.ui.component.CoinListItemShimmerView
import br.com.terras.app.dsm.ui.component.CoinListItemView
import br.com.terras.app.dsm.ui.component.RippleButton
import br.com.terras.app.dsm.ui.component.StickyHeaderView
import br.com.terras.app.dsm.ui.theme.DSMTheme
import br.com.terras.app.favorites.domain.model.CoinVO
import br.com.terras.app.favorites.presentation.FavoritesViewModel.FavoritesState.Loading
import br.com.terras.app.favorites.presentation.FavoritesViewModel.FavoritesState.Success
import br.com.terras.app.favorites.presentation.FavoritesViewModel.FavoritesState.Error

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun FavoriteActivityCompose(
    viewModel: FavoritesViewModel = hiltViewModel(),
    onItemClick: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            StickyHeaderView()
        }
    ) {

        val coinsListState by viewModel.coins.collectAsState()

        when (coinsListState) {
            is Loading -> CoinListItemShimmerView()
            is Success -> OnCoinsListSuccess((coinsListState as Success).coinsList, onItemClick)
            is Error -> OnCoinsListError(
                showDialog
            ) { showDialog = false }
        }
    }
}

@Composable
private fun OnCoinsListSuccess(coinsList: List<CoinVO>, onItemClick: (String) -> Unit) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(9.dp),
        modifier = Modifier.padding(top = 100.dp, start = 18.dp, end = 18.dp, bottom = 100.dp)
    ) {
        items(coinsList.size) {
            coinsList[it].run {
                CoinListItemView(
                    coinId = id,
                    coinPosition = marketCapRank,
                    coinLogo = image,
                    coinName = name,
                    coinSymbol = symbol,
                    coinPrice = price,
                    coinPriceChancePercentage = priceChangePercentage,
                    trendColor = trendColor,
                    onItemClick = onItemClick
                )
            }
        }
    }
}

@Composable
private fun OnCoinsListError(
    showDialog: Boolean,
    onDismissRequest: () -> Unit
) {
    if (showDialog.not()) return

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = "Alerta") },
        text = { Text(text = "Erro ao carregar os dados, tente novamente mais tarde.") },
        confirmButton = {
            RippleButton(text = "Entendi", onClick = onDismissRequest)
        }
    )
}

@ThemePreviews
@Composable
private fun FavoriteActivityComposePreview() {
    DSMTheme {
        Surface {
            FavoriteActivityCompose {}
        }
    }
}