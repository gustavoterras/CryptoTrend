package br.com.terras.app.coins.detail.presentation

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.terras.app.coins.detail.domain.model.CoinDetailLinkType
import br.com.terras.app.coins.detail.domain.model.CoinDetailLinkVO
import br.com.terras.app.coins.detail.domain.model.CoinDetailPlatformVO
import br.com.terras.app.coins.detail.domain.model.CoinDetailPricePercentageVO
import br.com.terras.app.coins.detail.domain.model.CoinDetailSentimentVO
import br.com.terras.app.coins.detail.domain.model.CoinDetailTickerVO
import br.com.terras.app.coins.detail.domain.model.TickerTrustLevel
import br.com.terras.app.cryptotrend.core.dsm.R
import br.com.terras.app.dsm.ui.component.TrendColor

@Composable
internal fun PriceChangePeriodStrip(
    primaryPeriods: List<CoinDetailPricePercentageVO>,
    extraPeriods: List<CoinDetailPricePercentageVO>,
    modifier: Modifier = Modifier
) {
    val allPeriods = remember(primaryPeriods, extraPeriods) {
        primaryPeriods + extraPeriods
    }
    if (allPeriods.isEmpty()) return

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .testTag("price_change_period_strip")
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .widthIn(min = maxWidth)
                .padding(horizontal = 18.dp)
        ) {
            allPeriods.forEach { item ->
                PriceChangeStatColumn(item = item)
            }
        }
    }
}

@Composable
private fun PriceChangeStatColumn(
    item: CoinDetailPricePercentageVO
) {
    Column(
        modifier = Modifier
            .testTag("price_change_item_${item.priceChangeTime}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = item.priceChangeTime,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false
        )
        Text(
            text = item.priceChangeValue,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = item.trendColor.textColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
internal fun CommunitySentimentSection(
    sentiment: CoinDetailSentimentVO,
    modifier: Modifier = Modifier
) {
    val total = (sentiment.upPercentage + sentiment.downPercentage).coerceAtLeast(1.0)
    val upWeight = (sentiment.upPercentage / total).toFloat().coerceIn(0.05f, 0.95f)
    val downWeight = (1f - upWeight).coerceIn(0.05f, 0.95f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .testTag("coin_sentiment_section"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_sentiment_24dp),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.scrim,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.detail_section_sentiment),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.surface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            sentiment.watchlistUsersLabel?.let { count ->
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primary,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Text(
                        text = stringResource(R.string.detail_sentiment_watching, count),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(upWeight)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 999.dp, bottomStart = 999.dp, topEnd = 3.dp, bottomEnd = 3.dp))
                    .background(TrendColor.UP.textColor)
            )
            Box(
                modifier = Modifier
                    .weight(downWeight)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp, topEnd = 999.dp, bottomEnd = 999.dp))
                    .background(TrendColor.DOWN.textColor)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "▲ ${stringResource(R.string.detail_sentiment_bullish)} · ${sentiment.upLabel}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = TrendColor.UP.textColor
            )
            Text(
                text = "${sentiment.downLabel} · ${stringResource(R.string.detail_sentiment_bearish)} ▼",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = TrendColor.DOWN.textColor
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CoinAboutExtrasContent(
    categories: List<String>,
    description: String,
    platforms: List<CoinDetailPlatformVO>,
    onCopyContract: (CoinDetailPlatformVO) -> Unit
) {
    if (categories.isNotEmpty()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_category_24dp),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.scrim,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.detail_about_categories),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.surface
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                categories.forEach { category ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(
                            text = category,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }

    if (description.isNotBlank()) {
        var isExpanded by remember(description) { mutableStateOf(false) }
        var hasOverflow by remember(description) { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp,
                maxLines = if (isExpanded) Int.MAX_VALUE else 4,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { layoutResult ->
                    if (!isExpanded) {
                        hasOverflow = layoutResult.hasVisualOverflow
                    }
                }
            )
            if (hasOverflow || isExpanded) {
                Text(
                    text = stringResource(if (isExpanded) R.string.action_see_less else R.string.action_see_more),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.scrim,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { isExpanded = !isExpanded }
                        .padding(vertical = 4.dp, horizontal = 2.dp)
                        .testTag("about_description_toggle")
                )
            }
        }
    }

    if (platforms.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(R.string.detail_about_contracts),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                platforms.forEach { platform ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onCopyContract(platform) }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = platform.network,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = platform.shortAddress,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.surface
                            )
                            Icon(
                                painter = painterResource(id = R.drawable.ic_content_copy_24dp),
                                contentDescription = stringResource(R.string.detail_copy_contract_cd, platform.network),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun OfficialLinksSection(
    links: List<CoinDetailLinkVO>,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .testTag("coin_links_section"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_link_24dp),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.scrim,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = stringResource(R.string.detail_section_links),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.surface
            )
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            links.forEachIndexed { index, link ->
                val baseLabel = when (link.type) {
                    CoinDetailLinkType.WEBSITE -> stringResource(R.string.detail_link_website)
                    CoinDetailLinkType.WHITEPAPER -> stringResource(R.string.detail_link_whitepaper)
                    CoinDetailLinkType.EXPLORER -> stringResource(R.string.detail_link_explorer)
                    CoinDetailLinkType.GITHUB -> stringResource(R.string.detail_link_github)
                    CoinDetailLinkType.BITBUCKET -> stringResource(R.string.detail_link_bitbucket)
                    CoinDetailLinkType.TWITTER -> stringResource(R.string.detail_link_twitter)
                    CoinDetailLinkType.REDDIT -> stringResource(R.string.detail_link_reddit)
                    CoinDetailLinkType.FORUM -> stringResource(R.string.detail_link_forum)
                    CoinDetailLinkType.CHAT -> stringResource(R.string.detail_link_chat)
                    CoinDetailLinkType.ANNOUNCEMENTS -> stringResource(R.string.detail_link_announcements)
                }
                val chipLabel = if (!link.hostLabel.isNullOrBlank()) {
                    "$baseLabel · ${link.hostLabel}"
                } else {
                    baseLabel
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .heightIn(min = 36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onLinkClick(link.url) }
                        .testTag("official_link_chip_${link.type.name.lowercase()}_$index")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = chipLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.surface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            painter = painterResource(id = R.drawable.ic_open_in_new_24dp),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun WhereToTradeSection(
    tickers: List<CoinDetailTickerVO>,
    onTickerClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAll by remember(tickers) { mutableStateOf(false) }
    val visibleTickers = if (showAll || tickers.size <= 5) tickers else tickers.take(5)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .testTag("coin_tickers_section"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.storefront_24dp),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.scrim,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = stringResource(R.string.detail_section_tickers),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.surface
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            visibleTickers.forEachIndexed { index, ticker ->
                val trustDotColor = when (ticker.trustLevel) {
                    TickerTrustLevel.HIGH -> TrendColor.UP.textColor
                    TickerTrustLevel.MEDIUM -> Color(0xFFE0A100)
                    TickerTrustLevel.LOW -> TrendColor.DOWN.textColor
                    TickerTrustLevel.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                }
                val isClickable = ticker.tradeUrl.isNotBlank()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .then(
                            if (isClickable) {
                                Modifier.clickable { onTickerClick(ticker.tradeUrl) }
                            } else {
                                Modifier
                            }
                        )
                        .padding(vertical = 10.dp)
                        .testTag("ticker_item_$index"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(trustDotColor)
                            )
                            Text(
                                text = ticker.marketName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.surface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Text(
                                text = ticker.pairLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.detail_tickers_volume, ticker.volume24h),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            ticker.spreadLabel?.let { spread ->
                                Text(
                                    text = stringResource(R.string.detail_tickers_spread, spread),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = ticker.lastPrice,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.surface,
                            maxLines = 1
                        )
                        if (isClickable) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_open_in_new_24dp),
                                contentDescription = stringResource(
                                    R.string.detail_tickers_open_cd,
                                    ticker.pairLabel,
                                    ticker.marketName
                                ),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                if (index != visibleTickers.lastIndex) {
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }

        if (tickers.size > 5) {
            Text(
                text = stringResource(
                    if (showAll) R.string.detail_tickers_show_less else R.string.detail_tickers_show_all,
                    tickers.size
                ),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.scrim,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { showAll = !showAll }
                    .padding(vertical = 4.dp, horizontal = 2.dp)
                    .testTag("tickers_toggle_all")
            )
        }
    }
}
