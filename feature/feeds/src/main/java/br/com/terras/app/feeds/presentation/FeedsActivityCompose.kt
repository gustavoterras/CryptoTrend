package br.com.terras.app.feeds.presentation

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.terras.app.dsm.ui.annotation.ThemePreviews
import br.com.terras.app.dsm.ui.component.RippleButton
import br.com.terras.app.dsm.ui.component.StickyHeaderView
import br.com.terras.app.dsm.ui.theme.DSMTheme
import br.com.terras.app.feeds.domain.model.ArticleVO
import br.com.terras.app.feeds.presentation.FeedsViewModel.FeedsState.Loading
import br.com.terras.app.feeds.presentation.FeedsViewModel.FeedsState.Success
import br.com.terras.app.feeds.presentation.FeedsViewModel.FeedsState.Error

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun FeedsActivityCompose(
    viewModel: FeedsViewModel = hiltViewModel(),
    onItemClick: (String) -> Unit
) {

    var showDialog by remember { mutableStateOf(true) }

    Scaffold {

        val feedsState by viewModel.feeds.collectAsState()

        when (feedsState) {
            is Loading -> FeedsItemShimmerCompose()
            is Success -> OnFeedsSuccess((feedsState as Success).coinsList, onItemClick)
            is Error -> OnCoinsListError(
                showDialog
            ) { showDialog = false }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OnFeedsSuccess(feeds: List<ArticleVO>, onItemClick: (String) -> Unit) {
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
                StickyHeaderView(modifier = Modifier.padding(start = 18.dp, end = 18.dp))
            }
        }

        items(feeds.size) {
            feeds[it].run {
                FeedsItemCompose(
                    title,
                    description,
                    urlToImage,
                    "Veja mais",
                ) {
                    onItemClick(url)
                }
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
private fun FeedsActivityComposePreview() {
    DSMTheme {
        Surface {
            FeedsActivityCompose {}
        }
    }
}