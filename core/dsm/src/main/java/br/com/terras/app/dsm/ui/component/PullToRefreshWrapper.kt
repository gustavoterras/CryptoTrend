package br.com.terras.app.dsm.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.com.terras.app.cryptotrend.core.dsm.R
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import kotlinx.coroutines.yield

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullToRefreshWrapper(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    indicatorTopPadding: Dp = 0.dp,
    indicatorTestTag: String = "pull_to_refresh_indicator",
    content: @Composable BoxScope.() -> Unit
) {
    val pullRefreshState = rememberPullToRefreshState()
    val currentIsRefreshing by rememberUpdatedState(isRefreshing)

    LaunchedEffect(pullRefreshState.isRefreshing) {
        if (pullRefreshState.isRefreshing) {
            onRefresh()
            yield()
            if (!currentIsRefreshing) {
                pullRefreshState.endRefresh()
            }
        }
    }

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            pullRefreshState.startRefresh()
        } else {
            pullRefreshState.endRefresh()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .nestedScroll(pullRefreshState.nestedScrollConnection)
    ) {
        content()

        val progress = if (pullRefreshState.isRefreshing) {
            1f
        } else {
            (pullRefreshState.progress * 1.5f).coerceIn(0f, 1f)
        }

        if (pullRefreshState.isRefreshing || pullRefreshState.progress > 0.01f) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = indicatorTopPadding + 8.dp)
                    .graphicsLayer {
                        alpha = progress.coerceIn(0f, 1f)
                        translationY = (pullRefreshState.progress * 48.dp.toPx()).coerceAtMost(56.dp.toPx())
                        scaleX = (0.75f + (progress * 0.25f)).coerceIn(0.75f, 1f)
                        scaleY = scaleX
                    }
            ) {
                val composition by rememberLottieComposition(
                    LottieCompositionSpec.RawRes(R.raw.crypto_refresh_animation)
                )
                val barColorArgb = MaterialTheme.colorScheme.scrim.toArgb()
                val dynamicProperties = rememberLottieDynamicProperties(
                    rememberLottieDynamicProperty(
                        property = LottieProperty.COLOR,
                        value = barColorArgb,
                        keyPath = arrayOf("**", "BarColor")
                    )
                )

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.background,
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag(indicatorTestTag)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        LottieAnimation(
                            composition = composition,
                            iterations = LottieConstants.IterateForever,
                            speed = if (pullRefreshState.isRefreshing) 1.25f else 0.8f,
                            dynamicProperties = dynamicProperties,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }
            }
        }
    }
}
