package br.com.terras.app.feeds.presentation

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.terras.app.cryptotrend.core.dsm.R
import br.com.terras.app.dsm.ui.annotation.ThemePreviews
import br.com.terras.app.dsm.ui.component.PullToRefreshWrapper
import br.com.terras.app.dsm.ui.component.RippleButton
import br.com.terras.app.dsm.ui.component.StickyHeaderView
import br.com.terras.app.dsm.ui.theme.DSMTheme
import br.com.terras.app.feeds.domain.model.ArticleVO
import br.com.terras.app.feeds.presentation.FeedsViewModel.FeedsIntent
import br.com.terras.app.feeds.presentation.FeedsViewModel.FeedsState.Error
import br.com.terras.app.feeds.presentation.FeedsViewModel.FeedsState.Loading
import br.com.terras.app.feeds.presentation.FeedsViewModel.FeedsState.Success

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun FeedsActivityCompose(
    viewModel: FeedsViewModel = hiltViewModel(),
    onItemClick: (String) -> Unit
) {

    var showDialog by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            StickyHeaderView()
        }
    ) { innerPadding ->

        val feedsState by viewModel.feeds.collectAsState()
        val isRefreshing by viewModel.isRefreshing.collectAsState()
        var refreshDialog by remember(feedsState) { mutableStateOf(feedsState is Error) }

        PullToRefreshWrapper(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.handleIntent(FeedsIntent.Refresh) },
            indicatorTopPadding = innerPadding.calculateTopPadding(),
            indicatorTestTag = "feeds_pull_to_refresh"
        ) {
            when (feedsState) {
                is Loading -> FeedsItemShimmerCompose()
                is Success -> OnFeedsSuccess(
                    feeds = (feedsState as Success).coinsList,
                    topPadding = innerPadding.calculateTopPadding(),
                    onItemClick = onItemClick
                )
                is Error -> OnCoinsListError(
                    refreshDialog
                ) { refreshDialog = false }
            }
        }
    }
}

@Composable
private fun OnFeedsSuccess(
    feeds: List<ArticleVO>,
    topPadding: androidx.compose.ui.unit.Dp,
    onItemClick: (String) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            top = topPadding + 4.dp,
            bottom = 112.dp
        )
    ) {
        items(feeds.size) {
            feeds[it].run {
                FeedsItemCompose(
                    title,
                    description,
                    urlToImage,
                    stringResource(R.string.action_see_more),
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
        containerColor = MaterialTheme.colorScheme.primary,
        titleContentColor = MaterialTheme.colorScheme.surface,
        textContentColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        title = { Text(text = stringResource(R.string.dialog_alert_title)) },
        text = { Text(text = stringResource(R.string.error_loading_data)) },
        confirmButton = {
            RippleButton(text = stringResource(R.string.dialog_alert_acknowledge), onClick = onDismissRequest)
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