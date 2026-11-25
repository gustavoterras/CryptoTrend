package br.com.terras.app.dsm.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.terras.app.cryptotrend.core.dsm.R
import br.com.terras.app.dsm.ui.annotation.ThemePreviews
import br.com.terras.app.dsm.ui.theme.DSMTheme
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun CoinListItemView(
    coinId: String,
    coinLogo: String,
    coinName: String,
    coinSymbol: String,
    coinPosition: String? = null,
    coinPrice: String? = null,
    coinPriceChancePercentage: String? = null,
    trendColor: TrendColor = TrendColor.NONE,
    coinVolume: String? = null,
    sparklinePrices: List<Double> = emptyList(),
    quoteCurrency: String = stringResource(R.string.currency_quote_code),
    isFavorite: Boolean = false,
    onItemClick: (String) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val sparklineColor = when (trendColor) {
        TrendColor.DOWN -> if (isDark) Color(0xFFFF6B63) else Color(0xFFE04838)
        TrendColor.UP -> MaterialTheme.colorScheme.scrim
        TrendColor.FLAT, TrendColor.NONE -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val titleColor = MaterialTheme.colorScheme.surface
    val subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("coin_item_$coinId")
            .clickable { onItemClick(coinId) }
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left column: Icon + Symbol/Quote + Rank & Volume (or Coin Name fallback)
        Row(
            modifier = Modifier.weight(1.55f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlideImage(
                model = coinLogo,
                contentDescription = coinName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                val displaySymbol = coinSymbol.ifBlank { coinName }.uppercase()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                SpanStyle(
                                    color = titleColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            ) {
                                append(displaySymbol)
                            }
                            if (quoteCurrency.isNotBlank()) {
                                withStyle(
                                    SpanStyle(
                                        color = subtitleColor,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.5.sp
                                    )
                                ) {
                                    append(" / $quoteCurrency")
                                }
                            }
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    AnimatedVisibility(
                        visible = isFavorite,
                        enter = fadeIn() + scaleIn(
                            initialScale = 0.5f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        ),
                        exit = fadeOut() + scaleOut(targetScale = 0.5f)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_star_filled_24dp),
                            contentDescription = stringResource(R.string.coin_favorite_cd),
                            tint = MaterialTheme.colorScheme.inversePrimary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                val hasRank = !coinPosition.isNullOrBlank() && coinPosition != "null"
                val hasVolume = !coinVolume.isNullOrBlank()

                if (hasRank || hasVolume || coinName.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (hasRank) {
                            Text(
                                text = "#$coinPosition",
                                style = MaterialTheme.typography.labelMedium,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = titleColor.copy(alpha = 0.7f),
                                maxLines = 1,
                                modifier = Modifier.testTag("coin_rank_$coinId")
                            )
                        }
                        val volumeOrName = when {
                            hasVolume -> "Vol $coinVolume"
                            coinName.isNotBlank() -> coinName
                            else -> ""
                        }
                        if (volumeOrName.isNotEmpty()) {
                            Text(
                                text = volumeOrName,
                                style = MaterialTheme.typography.labelMedium,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Normal,
                                color = subtitleColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Middle column: 7d Sparkline Mini-Graph
        Box(
            modifier = Modifier
                .width(70.dp)
                .height(30.dp)
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (sparklinePrices.size >= 2) {
                SparklineChartView(
                    prices = sparklinePrices,
                    lineColor = sparklineColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                )
            }
        }

        // Right column: Price + 24h Percentage Change
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 6.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            if (!coinPrice.isNullOrEmpty()) {
                Text(
                    text = coinPrice,
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End
                )
            }

            if (!coinPriceChancePercentage.isNullOrEmpty()) {
                Text(
                    text = coinPriceChancePercentage,
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = trendColor.textColor,
                    maxLines = 1,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@ThemePreviews
@Composable
private fun CoinListItemViewPreview() {
    DSMTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            CoinListItemView(
                coinId = "bitcoin",
                coinLogo = "",
                coinName = "Bitcoin",
                coinSymbol = "BTC",
                coinPosition = "1",
                coinPrice = "$17,185.06",
                coinPriceChancePercentage = "+2.65%",
                trendColor = TrendColor.UP,
                coinVolume = "$17,204,940,467",
                sparklinePrices = listOf(16800.0, 16950.0, 16900.0, 17120.0, 17050.0, 17185.0),
                quoteCurrency = "USDT"
            ) {}
        }
    }
}