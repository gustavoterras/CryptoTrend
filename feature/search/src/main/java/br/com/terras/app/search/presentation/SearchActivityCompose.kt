package br.com.terras.app.search.presentation

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.terras.app.dsm.ui.annotation.ThemePreviews
import br.com.terras.app.dsm.ui.component.CoinListItemShimmerView
import br.com.terras.app.dsm.ui.component.CoinListItemView
import br.com.terras.app.dsm.ui.component.RippleButton
import br.com.terras.app.dsm.ui.component.StickyHeaderView
import br.com.terras.app.dsm.ui.theme.DSMTheme
import br.com.terras.app.search.domain.model.CoinVO
import br.com.terras.app.search.presentation.SearchViewModel.SearchState.Empty
import br.com.terras.app.search.presentation.SearchViewModel.SearchState.Error
import br.com.terras.app.search.presentation.SearchViewModel.SearchState.Loading
import br.com.terras.app.search.presentation.SearchViewModel.SearchState.Success

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun SearchActivityCompose(
    viewModel: SearchViewModel = hiltViewModel(),
    onItemClick: (String) -> Unit
) {

    var showDialog by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            StickyHeaderView(shouldShowSearchBar = true, onValueChangeListener = { query ->
                viewModel.doSearch(query)
            })
        }
    ) {
        val coinsListState by viewModel.coins.collectAsState()

        when (coinsListState) {
            is Loading -> CoinListItemShimmerView(shouldShowSearchBar = true)
            is Success -> OnSearchSuccess((coinsListState as Success).coinsList, onItemClick)
            is Empty -> OnSearchEmpty()
            is Error -> OnSearchError(showDialog) {
                showDialog = false
            }
        }
    }
}

@Composable
private fun OnSearchSuccess(coinsList: List<CoinVO>, onItemClick: (String) -> Unit) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(9.dp),
        modifier = Modifier.padding(top = 150.dp,start = 18.dp, end = 18.dp, bottom = 100.dp)
    ) {
        items(coinsList.size) {
            coinsList[it].run {
                CoinListItemView(
                    coinId = id,
                    coinLogo = thumb,
                    coinName = name,
                    coinSymbol = symbol,
                    onItemClick = onItemClick
                )
            }
        }
    }
}

@Composable
private fun OnSearchError(
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

@Composable
private fun OnSearchEmpty() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Nenhum resultado encontrado",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.surface,
            textAlign = TextAlign.Center
        )
    }
}


@ThemePreviews
@Composable
private fun SearchActivityComposePreview() {
    DSMTheme {
        Surface {
            SearchActivityCompose {}
        }
    }
}