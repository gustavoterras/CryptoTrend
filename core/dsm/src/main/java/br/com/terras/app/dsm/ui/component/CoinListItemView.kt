package br.com.terras.app.dsm.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
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
import br.com.terras.app.dsm.ui.theme.MoneyFontFamily
import br.com.terras.app.dsm.ui.theme.asMoneyStyle
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import kotlinx.coroutines.delay

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
    rawPrice: Double? = null,
    rawPriceChangePercentage: Double? = null,
    animatePriceFlash: Boolean = false,
    animatePriceOdometer: Boolean = false,
    onItemClick: (String) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val upFlashColor = MaterialTheme.colorScheme.scrim
    val downFlashColor = if (isDark) Color(0xFFFF6B63) else Color(0xFFE04838)
    val sparklineColor = when (trendColor) {
        TrendColor.DOWN -> downFlashColor
        TrendColor.UP -> upFlashColor
        TrendColor.FLAT, TrendColor.NONE -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val titleColor = MaterialTheme.colorScheme.surface
    val subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant

    var priceFlashColor by remember(coinId) { mutableStateOf<Color?>(null) }
    var lastObservedPrice by remember(coinId) { mutableStateOf<Double?>(null) }

    LaunchedEffect(rawPrice, animatePriceFlash) {
        val current = rawPrice
        val previous = lastObservedPrice
        if (animatePriceFlash && current != null && current.isFinite()) {
            if (previous != null && current != previous) {
                priceFlashColor = if (current > previous) upFlashColor else downFlashColor
                lastObservedPrice = current
                delay(750L)
                priceFlashColor = null
            } else {
                lastObservedPrice = current
            }
        } else {
            lastObservedPrice = current
            priceFlashColor = null
        }
    }

    val animatedPriceColor by animateColorAsState(
        targetValue = priceFlashColor ?: titleColor,
        animationSpec = tween(durationMillis = if (priceFlashColor != null) 140 else 550),
        label = "coinRowPriceFlashColor"
    )

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
                        if (hasVolume) {
                            Text(
                                text = buildAnnotatedString {
                                    append("Vol ")
                                    withStyle(
                                        SpanStyle(
                                            fontFamily = MoneyFontFamily,
                                            fontFeatureSettings = "tnum"
                                        )
                                    ) {
                                        append(coinVolume)
                                    }
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Normal,
                                color = subtitleColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else if (coinName.isNotBlank()) {
                            Text(
                                text = coinName,
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
                OdometerText(
                    text = coinPrice,
                    rawValue = rawPrice,
                    animateChanges = animatePriceOdometer,
                    style = MaterialTheme.typography.bodyLarge.asMoneyStyle(),
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = animatedPriceColor,
                    textAlign = TextAlign.End
                )
            }

            if (!coinPriceChancePercentage.isNullOrEmpty() || trendColor != TrendColor.NONE) {
                TrendChangeBadge(
                    coinId = coinId,
                    percentageText = coinPriceChancePercentage.orEmpty(),
                    rawPercentage = rawPriceChangePercentage,
                    animateOdometer = animatePriceOdometer,
                    trendColor = trendColor
                )
            }
        }
    }
}

@Composable
private fun TrendChangeBadge(
    coinId: String,
    percentageText: String,
    rawPercentage: Double?,
    animateOdometer: Boolean,
    trendColor: TrendColor
) {
    val hasBadgeBackground = trendColor != TrendColor.NONE
    val contentColor = if (hasBadgeBackground) trendColor.onColor else trendColor.textColor
    val badgeShape = RoundedCornerShape(6.dp)

    Surface(
        color = trendColor.color,
        shape = badgeShape,
        modifier = Modifier.testTag("coin_trend_badge_$coinId")
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (hasBadgeBackground) 6.dp else 0.dp,
                vertical = if (hasBadgeBackground) 2.5.dp else 0.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (trendColor != TrendColor.NONE) {
                TrendDirectionIcon(
                    trendColor = trendColor,
                    color = contentColor,
                    modifier = Modifier.size(8.dp)
                )
            }
            if (percentageText.isNotEmpty()) {
                OdometerText(
                    text = percentageText,
                    rawValue = rawPercentage,
                    animateChanges = animateOdometer,
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
private fun TrendDirectionIcon(
    trendColor: TrendColor,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        when (trendColor) {
            TrendColor.UP -> {
                val path = Path().apply {
                    moveTo(w * 0.5f, h * 0.14f)
                    lineTo(w * 0.92f, h * 0.84f)
                    lineTo(w * 0.08f, h * 0.84f)
                    close()
                }
                drawPath(path = path, color = color)
            }
            TrendColor.DOWN -> {
                val path = Path().apply {
                    moveTo(w * 0.08f, h * 0.16f)
                    lineTo(w * 0.92f, h * 0.16f)
                    lineTo(w * 0.5f, h * 0.86f)
                    close()
                }
                drawPath(path = path, color = color)
            }
            TrendColor.FLAT -> {
                drawLine(
                    color = color,
                    start = Offset(w * 0.12f, h * 0.5f),
                    end = Offset(w * 0.88f, h * 0.5f),
                    strokeWidth = 1.6.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            TrendColor.NONE -> Unit
        }
    }
}

@ThemePreviews
@Composable
private fun CoinListItemViewPreview() {
    DSMTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column {
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
                CoinListItemView(
                    coinId = "ethereum",
                    coinLogo = "",
                    coinName = "Ethereum",
                    coinSymbol = "ETH",
                    coinPosition = "2",
                    coinPrice = "$1,265.40",
                    coinPriceChancePercentage = "-1.84%",
                    trendColor = TrendColor.DOWN,
                    coinVolume = "$6,410,000,000",
                    sparklinePrices = listOf(1310.0, 1298.0, 1302.0, 1280.0, 1274.0, 1265.4),
                    quoteCurrency = "USDT"
                ) {}
            }
        }
    }
}