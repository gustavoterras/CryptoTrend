package br.com.terras.app.feeds.presentation

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
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
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun FeedsActivityCompose(
    viewModel: FeedsViewModel = hiltViewModel(),
    onItemClick: (String) -> Unit
) {
    Scaffold(
        topBar = {
            StickyHeaderView()
        }
    ) { innerPadding ->
        val feedsState by viewModel.feeds.collectAsState()
        val isRefreshing by viewModel.isRefreshing.collectAsState()

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
                is Error -> OnFeedsError(
                    topPadding = innerPadding.calculateTopPadding(),
                    onRetry = { viewModel.handleIntent(FeedsIntent.Refresh) }
                )
            }
        }
    }
}

@Composable
private fun OnFeedsSuccess(
    feeds: List<ArticleVO>,
    topPadding: Dp,
    onItemClick: (String) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        contentPadding = PaddingValues(
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
private fun OnFeedsError(
    topPadding: Dp,
    onRetry: () -> Unit
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.error_avatar_animation)
    )
    val errorTint = MaterialTheme.colorScheme.error
    val dynamicProperties = rememberLottieDynamicProperties(
        rememberLottieDynamicProperty(
            property = LottieProperty.COLOR,
            value = errorTint.toArgb(),
            keyPath = arrayOf("**", "ErrorAvatarFill")
        ),
        rememberLottieDynamicProperty(
            property = LottieProperty.COLOR,
            value = errorTint.toArgb(),
            keyPath = arrayOf("**", "ErrorSoftFill")
        ),
        rememberLottieDynamicProperty(
            property = LottieProperty.COLOR,
            value = errorTint.toArgb(),
            keyPath = arrayOf("**", "ErrorStroke")
        ),
        rememberLottieDynamicProperty(
            property = LottieProperty.STROKE_COLOR,
            value = errorTint.toArgb(),
            keyPath = arrayOf("**", "ErrorStroke")
        ),
        rememberLottieDynamicProperty(
            property = LottieProperty.COLOR,
            value = errorTint.toArgb(),
            keyPath = arrayOf("**", "AlertAccentFill")
        ),
        rememberLottieDynamicProperty(
            property = LottieProperty.COLOR,
            value = errorTint.toArgb(),
            keyPath = arrayOf("**", "AlertAccentStroke")
        ),
        rememberLottieDynamicProperty(
            property = LottieProperty.STROKE_COLOR,
            value = errorTint.toArgb(),
            keyPath = arrayOf("**", "AlertAccentStroke")
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = topPadding, bottom = 96.dp)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            LottieAnimation(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                speed = 0.85f,
                dynamicProperties = dynamicProperties,
                modifier = Modifier
                    .size(200.dp)
                    .alpha(0.85f)
                    .testTag("feeds_error_watermark_lottie")
            )
            Text(
                text = stringResource(R.string.error_loading_data),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Box(modifier = Modifier.testTag("feeds_retry_button")) {
                RippleButton(
                    text = stringResource(R.string.action_retry),
                    onClick = onRetry
                )
            }
        }
    }
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