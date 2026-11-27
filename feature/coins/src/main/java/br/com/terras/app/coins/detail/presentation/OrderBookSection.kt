package br.com.terras.app.coins.detail.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.terras.app.coins.detail.domain.model.OrderBookEntryVO
import br.com.terras.app.coins.detail.domain.model.OrderBookMultiplier
import br.com.terras.app.coins.detail.domain.model.OrderBookMultiplierOptionVO
import br.com.terras.app.coins.detail.domain.model.OrderBookSnapshotVO
import br.com.terras.app.coins.detail.domain.model.OrderBookUiState
import br.com.terras.app.cryptotrend.core.dsm.R
import br.com.terras.app.dsm.ui.annotation.ThemePreviews
import br.com.terras.app.dsm.ui.component.ShimmerEffect
import br.com.terras.app.dsm.ui.component.TrendColor
import br.com.terras.app.dsm.ui.theme.DSMTheme
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun OrderBookSection(
    state: OrderBookUiState,
    onToggleExpand: (Boolean) -> Unit,
    onSelectMultiplier: (OrderBookMultiplier) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val pairSymbol = when (state) {
        is OrderBookUiState.Hidden -> return
        is OrderBookUiState.Collapsed -> state.pairSymbol
        is OrderBookUiState.Loading -> state.pairSymbol
        is OrderBookUiState.Visible -> state.snapshot.pairSymbol
    }
    val expanded = state !is OrderBookUiState.Collapsed
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "orderBookChevron"
    )
    val bidColor = TrendColor.UP.textColor
    val askColor = TrendColor.DOWN.textColor

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .testTag("order_book_section")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable(role = Role.Button) { onToggleExpand(!expanded) }
                .testTag("order_book_toggle_button"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_bar_chart_24dp),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.scrim,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.detail_order_book_title, pairSymbol),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.surface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = stringResource(
                    if (expanded) R.string.detail_order_book_collapse_cd
                    else R.string.detail_order_book_expand_cd
                ),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(24.dp)
                    .rotate(chevronRotation)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(animationSpec = tween(220)) + fadeIn(animationSpec = tween(180)),
            exit = shrinkVertically(animationSpec = tween(200)) + fadeOut(animationSpec = tween(150))
        ) {
            when (state) {
                is OrderBookUiState.Loading -> OrderBookSkeleton()
                is OrderBookUiState.Visible -> OrderBookContent(
                    snapshot = state.snapshot,
                    bidColor = bidColor,
                    askColor = askColor,
                    onSelectMultiplier = onSelectMultiplier
                )
                is OrderBookUiState.Collapsed,
                is OrderBookUiState.Hidden -> Unit
            }
        }
    }
}

@Composable
private fun OrderBookContent(
    snapshot: OrderBookSnapshotVO,
    bidColor: Color,
    askColor: Color,
    onSelectMultiplier: (OrderBookMultiplier) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("order_book_table")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(
                    R.string.detail_order_book_spread,
                    snapshot.spreadAbsoluteFormatted,
                    snapshot.spreadPercentageFormatted
                ),
                style = MaterialTheme.typography.labelMedium.merge(
                    TextStyle(fontFeatureSettings = "tnum")
                ),
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .testTag("order_book_spread_badge")
            )

            if (snapshot.availableMultipliers.isNotEmpty()) {
                Box {
                    val selectedLabel = if (snapshot.selectedStepFormatted.isNotBlank()) {
                        "${snapshot.selectedMultiplier.label} (${snapshot.selectedStepFormatted})"
                    } else {
                        snapshot.selectedMultiplier.label
                    }
                    Row(
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(
                                role = Role.DropdownList,
                                onClickLabel = stringResource(R.string.detail_order_book_multiplier_cd)
                            ) { menuExpanded = true }
                            .padding(horizontal = 6.dp)
                            .testTag("order_book_multiplier_button"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = selectedLabel,
                            style = MaterialTheme.typography.labelMedium.merge(
                                TextStyle(fontFeatureSettings = "tnum")
                            ),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.scrim,
                            maxLines = 1
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.detail_order_book_multiplier_cd),
                            tint = MaterialTheme.colorScheme.scrim,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.background)
                            .testTag("order_book_multiplier_menu")
                    ) {
                        snapshot.availableMultipliers.forEach { option ->
                            val isSelected = option.multiplier == snapshot.selectedMultiplier
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "${option.multiplier.label} · ${option.stepFormatted}",
                                        style = MaterialTheme.typography.labelLarge.merge(
                                            TextStyle(fontFeatureSettings = "tnum")
                                        ),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.scrim
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        }
                                    )
                                },
                                trailingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.scrim,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else {
                                    null
                                },
                                onClick = {
                                    menuExpanded = false
                                    onSelectMultiplier(option.multiplier)
                                },
                                modifier = Modifier.testTag("order_book_multiplier_option_${option.multiplier.factor}")
                            )
                        }
                    }
                }
            }
        }

        val totalBidDepth = snapshot.bids.lastOrNull()?.cumulativeTotal ?: 0.0
        val totalAskDepth = snapshot.asks.lastOrNull()?.cumulativeTotal ?: 0.0
        val totalDepth = totalBidDepth + totalAskDepth
        val targetBidRatio = if (totalDepth > 0.0) {
            (totalBidDepth / totalDepth).toFloat().coerceIn(0.06f, 0.94f)
        } else {
            0.5f
        }
        val animatedBidRatio by animateFloatAsState(
            targetValue = targetBidRatio,
            animationSpec = tween(durationMillis = 250),
            label = "orderBookBidPressureRatio"
        )
        val bidPct = if (totalDepth > 0.0) {
            ((totalBidDepth / totalDepth) * 100.0).roundToInt().coerceIn(1, 99)
        } else {
            50
        }
        val askPct = 100 - bidPct

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            OrderBookPressureHeader(
                bidRatio = animatedBidRatio,
                bidPct = bidPct,
                askPct = askPct,
                bidColor = bidColor,
                askColor = askColor
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.detail_order_book_amount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.detail_order_book_price),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.detail_order_book_price),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.detail_order_book_amount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            val rowCount = max(snapshot.bids.size, snapshot.asks.size)
            for (index in 0 until rowCount) {
                val bid = snapshot.bids.getOrNull(index)
                val ask = snapshot.asks.getOrNull(index)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (bid != null) {
                            OrderBookBidRow(
                                entry = bid,
                                priceColor = bidColor
                            )
                        }
                    }
                    VerticalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxHeight()
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        if (ask != null) {
                            OrderBookAskRow(
                                entry = ask,
                                priceColor = askColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderBookPressureHeader(
    bidRatio: Float,
    bidPct: Int,
    askPct: Int,
    bidColor: Color,
    askColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
            .testTag("order_book_pressure_bar")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                TradeDirectionArrowIcon(
                    pointsUp = true,
                    color = bidColor,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = stringResource(R.string.detail_order_book_bids),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.surface
                )
                Text(
                    text = "$bidPct%",
                    style = MaterialTheme.typography.labelSmall.merge(
                        TextStyle(fontFeatureSettings = "tnum")
                    ),
                    fontWeight = FontWeight.Bold,
                    color = bidColor
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = "$askPct%",
                    style = MaterialTheme.typography.labelSmall.merge(
                        TextStyle(fontFeatureSettings = "tnum")
                    ),
                    fontWeight = FontWeight.Bold,
                    color = askColor
                )
                Text(
                    text = stringResource(R.string.detail_order_book_asks),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.surface
                )
                TradeDirectionArrowIcon(
                    pointsUp = false,
                    color = askColor,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(bidRatio.coerceIn(0.05f, 0.95f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 999.dp, bottomStart = 999.dp, topEnd = 2.dp, bottomEnd = 2.dp))
                    .background(bidColor)
            )
            Box(
                modifier = Modifier
                    .weight((1f - bidRatio).coerceIn(0.05f, 0.95f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 2.dp, bottomStart = 2.dp, topEnd = 999.dp, bottomEnd = 999.dp))
                    .background(askColor)
            )
        }
    }
}

@Composable
private fun OrderBookSkeleton(
    modifier: Modifier = Modifier
) {
    val brush = ShimmerEffect()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("order_book_skeleton")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(136.dp)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
            Box(
                modifier = Modifier
                    .width(72.dp)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .width(96.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
            Box(
                modifier = Modifier
                    .width(96.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .height(5.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(brush)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .width(30.dp)
                        .height(9.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )
                Box(
                    modifier = Modifier
                        .width(38.dp)
                        .height(9.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )
            }
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .width(38.dp)
                        .height(9.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )
                Box(
                    modifier = Modifier
                        .width(30.dp)
                        .height(9.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )
            }
        }

        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )

        repeat(8) { index ->
            val depthFraction = ((index + 1) * 0.11f).coerceIn(0.15f, 0.92f)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxWidth(depthFraction)
                            .fillMaxHeight()
                            .background(brush)
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .fillMaxWidth(depthFraction)
                            .fillMaxHeight()
                            .background(brush)
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderBookBidRow(
    entry: OrderBookEntryVO,
    priceColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .fillMaxWidth(entry.depthPercentage.coerceIn(0f, 1f))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            priceColor.copy(alpha = 0.06f),
                            priceColor.copy(alpha = 0.22f)
                        )
                    )
                )
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(start = 2.dp, end = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = entry.sizeFormatted,
                style = MaterialTheme.typography.bodySmall.merge(
                    TextStyle(fontFeatureSettings = "tnum")
                ),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = entry.priceFormatted,
                style = MaterialTheme.typography.bodySmall.merge(
                    TextStyle(fontFeatureSettings = "tnum")
                ),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = priceColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun OrderBookAskRow(
    entry: OrderBookEntryVO,
    priceColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .fillMaxWidth(entry.depthPercentage.coerceIn(0f, 1f))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            priceColor.copy(alpha = 0.22f),
                            priceColor.copy(alpha = 0.06f)
                        )
                    )
                )
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(start = 6.dp, end = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = entry.priceFormatted,
                style = MaterialTheme.typography.bodySmall.merge(
                    TextStyle(fontFeatureSettings = "tnum")
                ),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = priceColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = entry.sizeFormatted,
                style = MaterialTheme.typography.bodySmall.merge(
                    TextStyle(fontFeatureSettings = "tnum")
                ),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@ThemePreviews
@Composable
private fun OrderBookSectionPreview() {
    val sampleBids = listOf(
        OrderBookEntryVO(65420.10, "$65,420.10", 0.420, "0.4200", 0.420, 0.18f),
        OrderBookEntryVO(65418.50, "$65,418.50", 0.850, "0.8500", 1.270, 0.45f),
        OrderBookEntryVO(65415.00, "$65,415.00", 1.120, "1.120", 2.390, 0.78f),
        OrderBookEntryVO(65410.25, "$65,410.25", 0.610, "0.6100", 3.000, 1.00f)
    )
    val sampleAsks = listOf(
        OrderBookEntryVO(65421.30, "$65,421.30", 0.310, "0.3100", 0.310, 0.15f),
        OrderBookEntryVO(65424.00, "$65,424.00", 0.740, "0.7400", 1.050, 0.40f),
        OrderBookEntryVO(65428.80, "$65,428.80", 0.990, "0.9900", 2.040, 0.70f),
        OrderBookEntryVO(65432.00, "$65,432.00", 0.760, "0.7600", 2.800, 0.93f)
    )
    DSMTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            OrderBookSection(
                state = OrderBookUiState.Visible(
                    snapshot = OrderBookSnapshotVO(
                        pairSymbol = "BTC-USD",
                        bids = sampleBids,
                        asks = sampleAsks,
                        spreadAbsolute = 1.20,
                        spreadAbsoluteFormatted = "$1.20",
                        spreadPercentageFormatted = "0.01%",
                        isLive = false,
                        selectedMultiplier = OrderBookMultiplier.X10,
                        selectedStepFormatted = "$0.10",
                        availableMultipliers = listOf(
                            OrderBookMultiplierOptionVO(OrderBookMultiplier.X1, "$0.01"),
                            OrderBookMultiplierOptionVO(OrderBookMultiplier.X10, "$0.10"),
                            OrderBookMultiplierOptionVO(OrderBookMultiplier.X100, "$1"),
                            OrderBookMultiplierOptionVO(OrderBookMultiplier.X1000, "$10")
                        )
                    )
                ),
                onToggleExpand = {},
                onSelectMultiplier = {}
            )
        }
    }
}

@ThemePreviews
@Composable
private fun OrderBookSectionLoadingPreview() {
    DSMTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            OrderBookSection(
                state = OrderBookUiState.Loading(pairSymbol = "BTC-USD"),
                onToggleExpand = {}
            )
        }
    }
}
