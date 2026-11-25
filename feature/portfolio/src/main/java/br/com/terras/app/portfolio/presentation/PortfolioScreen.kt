package br.com.terras.app.portfolio.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.terras.app.common.formatMoney
import br.com.terras.app.cryptotrend.core.dsm.R
import br.com.terras.app.dsm.ui.annotation.ThemePreviews
import br.com.terras.app.dsm.ui.component.AllocationDonutChart
import br.com.terras.app.dsm.ui.component.AllocationSlice
import br.com.terras.app.dsm.ui.component.AnimatedAddFab
import br.com.terras.app.dsm.ui.component.PullToRefreshWrapper
import br.com.terras.app.dsm.ui.component.RippleButton
import br.com.terras.app.dsm.ui.component.ShimmerEffect
import br.com.terras.app.dsm.ui.component.StickyHeaderView
import br.com.terras.app.dsm.ui.component.TrendColor
import br.com.terras.app.dsm.ui.component.portfolioChartPalette
import br.com.terras.app.dsm.ui.theme.DSMTheme
import br.com.terras.app.network.portfolio.PortfolioTransactionType
import br.com.terras.app.portfolio.domain.model.PortfolioHoldingVO
import br.com.terras.app.portfolio.domain.model.PortfolioSummaryVO
import br.com.terras.app.portfolio.domain.model.PortfolioTransactionVO
import br.com.terras.app.portfolio.domain.model.SelectableCoinVO
import br.com.terras.app.portfolio.presentation.PortfolioViewModel.PortfolioIntent
import br.com.terras.app.portfolio.presentation.PortfolioViewModel.PortfolioSubTab
import br.com.terras.app.portfolio.presentation.PortfolioViewModel.PortfolioUiState
import java.util.Locale

@Composable
fun PortfolioActivityCompose(
    viewModel: PortfolioViewModel = hiltViewModel(),
    onCoinClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val editorState by viewModel.editorState.collectAsStateWithLifecycle()
    val pickerState by viewModel.coinPickerState.collectAsStateWithLifecycle()
    val pendingDelete by viewModel.pendingDeleteTransaction.collectAsStateWithLifecycle()
    val deleteError by viewModel.deleteErrorMessage.collectAsStateWithLifecycle()

    PortfolioScreenContent(
        uiState = uiState,
        onIntent = viewModel::handleIntent,
        onCoinClick = onCoinClick
    )

    PortfolioTransactionSheet(
        editorState = editorState,
        pickerState = pickerState,
        onIntent = viewModel::handleIntent
    )

    pendingDelete?.let { tx ->
        AlertDialog(
            onDismissRequest = { viewModel.handleIntent(PortfolioIntent.DismissDeleteDialog) },
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.surface,
            textContentColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            title = { Text(stringResource(R.string.portfolio_delete_dialog_title)) },
            text = {
                val typeLabel = if (tx.type == PortfolioTransactionType.BUY) {
                    stringResource(R.string.portfolio_type_buy_lower)
                } else {
                    stringResource(R.string.portfolio_type_sell_lower)
                }
                Text(
                    stringResource(
                        R.string.portfolio_delete_dialog_message,
                        typeLabel,
                        tx.quantityFormatted,
                        tx.coinSymbol,
                        tx.totalValueFormatted
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.handleIntent(PortfolioIntent.ConfirmDeleteTransaction) },
                    modifier = Modifier.testTag("portfolio_confirm_delete_btn")
                ) {
                    Text(
                        text = stringResource(R.string.action_delete),
                        color = TrendColor.DOWN.textColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.handleIntent(PortfolioIntent.DismissDeleteDialog) }) {
                    Text(stringResource(R.string.action_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    deleteError?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.handleIntent(PortfolioIntent.ClearDeleteError) },
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.surface,
            textContentColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            title = { Text(stringResource(R.string.portfolio_protected_dialog_title)) },
            text = { Text(msg) },
            confirmButton = {
                RippleButton(
                    text = stringResource(R.string.dialog_alert_acknowledge),
                    onClick = { viewModel.handleIntent(PortfolioIntent.ClearDeleteError) }
                )
            }
        )
    }
}

@Composable
fun PortfolioScreenContent(
    uiState: PortfolioUiState,
    onIntent: (PortfolioIntent) -> Unit,
    onCoinClick: (String) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            StickyHeaderView()
        },
        floatingActionButton = {
            if (uiState is PortfolioUiState.Content) {
                AnimatedAddFab(
                    onClick = { onIntent(PortfolioIntent.OpenNewTransaction()) },
                    testTag = "portfolio_add_fab"
                )
            }
        }
    ) { innerPadding ->
        when (uiState) {
            is PortfolioUiState.Loading -> {
                PortfolioLoadingView(topPadding = innerPadding.calculateTopPadding())
            }
            is PortfolioUiState.Empty -> {
                PortfolioEmptyView(
                    topPadding = innerPadding.calculateTopPadding(),
                    onAddFirstOperation = { onIntent(PortfolioIntent.OpenNewTransaction()) }
                )
            }
            is PortfolioUiState.Content -> {
                PullToRefreshWrapper(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { onIntent(PortfolioIntent.Refresh) },
                    indicatorTopPadding = innerPadding.calculateTopPadding(),
                    indicatorTestTag = "portfolio_pull_to_refresh"
                ) {
                    PortfolioSuccessContent(
                        state = uiState,
                        topPadding = innerPadding.calculateTopPadding(),
                        onIntent = onIntent,
                        onCoinClick = onCoinClick
                    )
                }
            }
        }
    }
}

@Composable
private fun PortfolioLoadingView(topPadding: Dp) {
    val shimmerBrush = ShimmerEffect()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = topPadding + 14.dp, start = 18.dp, end = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(width = 88.dp, height = 14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
                )
                Box(
                    modifier = Modifier
                        .size(width = 160.dp, height = 30.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(shimmerBrush)
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(width = 110.dp, height = 12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerBrush)
                    )
                }
            }
        }

        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

        Box(
            modifier = Modifier
                .size(196.dp)
                .align(Alignment.CenterHorizontally)
                .clip(CircleShape)
                .background(shimmerBrush)
        )

        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

        repeat(3) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(shimmerBrush)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .size(width = 90.dp, height = 14.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(shimmerBrush)
                        )
                        Box(
                            modifier = Modifier
                                .size(width = 130.dp, height = 11.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(shimmerBrush)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(width = 72.dp, height = 16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
                )
            }
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun PortfolioEmptyView(
    topPadding: Dp,
    onAddFirstOperation: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = topPadding, bottom = 96.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_portfolio_24dp),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.scrim,
                    modifier = Modifier.size(30.dp)
                )
            }

            Text(
                text = stringResource(R.string.portfolio_empty_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.surface,
                textAlign = TextAlign.Center
            )

            Text(
                text = stringResource(R.string.portfolio_empty_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onAddFirstOperation,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.scrim,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("portfolio_empty_cta")
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = stringResource(R.string.portfolio_empty_cta),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PortfolioSuccessContent(
    state: PortfolioUiState.Content,
    topPadding: Dp,
    onIntent: (PortfolioIntent) -> Unit,
    onCoinClick: (String) -> Unit
) {
    val context = LocalContext.current
    val snapshot = state.snapshot
    val palette = portfolioChartPalette()
    val othersSymbol = stringResource(R.string.portfolio_others_symbol)
    val allocationSlices = remember(snapshot.holdings, palette, othersSymbol) {
        buildAllocationSlices(
            holdings = snapshot.holdings,
            palette = palette,
            othersSymbol = othersSymbol,
            othersLabelProvider = { count ->
                context.getString(R.string.portfolio_others_label, count)
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("portfolio_content_list"),
        contentPadding = PaddingValues(
            top = topPadding + 6.dp,
            bottom = 116.dp
        )
    ) {
        if (state.isOfflineFallback) {
            item(key = "offline_banner") {
                OfflineQuotesBanner(
                    onRetry = { onIntent(PortfolioIntent.Refresh) }
                )
            }
        }

        item(key = "summary_header") {
            PortfolioSummaryHeader(summary = snapshot.summary)
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 18.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }

        if (allocationSlices.isNotEmpty()) {
            item(key = "allocation_section") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.portfolio_allocation_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.surface
                    )
                    AllocationDonutChart(
                        slices = allocationSlices,
                        totalValueText = snapshot.summary.totalValueFormatted,
                        assetCountLabel = if (snapshot.summary.assetCount == 1) {
                            stringResource(R.string.portfolio_asset_count_singular)
                        } else {
                            stringResource(R.string.portfolio_asset_count_plural, snapshot.summary.assetCount)
                        },
                        selectedSliceId = state.selectedSliceId,
                        onSelectSlice = { onIntent(PortfolioIntent.SelectAllocationSlice(it)) }
                    )
                }
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 18.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }

        item(key = "subtab_bar") {
            val selectedIdx = PortfolioSubTab.entries.indexOf(state.activeTab).coerceAtLeast(0)
            TabRow(
                selectedTabIndex = selectedIdx,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.surface,
                indicator = { tabPositions ->
                    if (selectedIdx < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier
                                .tabIndicatorOffset(tabPositions[selectedIdx])
                                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)),
                            height = 3.dp,
                            color = MaterialTheme.colorScheme.scrim
                        )
                    }
                },
                divider = {
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("portfolio_subtab_control")
            ) {
                PortfolioSubTab.entries.forEachIndexed { index, tab ->
                    val isSelected = selectedIdx == index
                    val tabTag = if (tab == PortfolioSubTab.ASSETS) {
                        "portfolio_subtab_assets"
                    } else {
                        "portfolio_subtab_history"
                    }
                    Tab(
                        selected = isSelected,
                        onClick = { onIntent(PortfolioIntent.SelectSubTab(tab)) },
                        selectedContentColor = MaterialTheme.colorScheme.surface,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag(tabTag),
                        text = {
                            Text(
                                text = stringResource(tab.labelRes),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }
            }
        }

        if (state.activeTab == PortfolioSubTab.ASSETS) {
            if (snapshot.holdings.isEmpty()) {
                item(key = "no_active_positions") {
                    NoActivePositionsSection(
                        onAddOperation = { onIntent(PortfolioIntent.OpenNewTransaction()) }
                    )
                }
            } else {
                itemsIndexed(snapshot.holdings, key = { _, item -> "holding_${item.coinId}" }) { index, holding ->
                    HoldingRow(
                        holding = holding,
                        isHighlighted = state.selectedSliceId == holding.coinId,
                        onOpenDetail = { onCoinClick(holding.coinId) },
                        onQuickAdd = {
                            onIntent(
                                PortfolioIntent.OpenNewTransaction(
                                    prefilledCoin = SelectableCoinVO(
                                        id = holding.coinId,
                                        symbol = holding.symbol,
                                        name = holding.name,
                                        imageUrl = holding.imageUrl,
                                        currentPrice = if (holding.hasLiveQuote) holding.currentPrice else null
                                    ),
                                    lockCoin = true
                                )
                            )
                        }
                    )
                    if (index != snapshot.holdings.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 18.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        } else {
            itemsIndexed(snapshot.transactions, key = { _, tx -> "tx_${tx.id}" }) { index, tx ->
                TransactionHistoryRow(
                    transaction = tx,
                    onEdit = { onIntent(PortfolioIntent.OpenEditTransaction(tx)) },
                    onDelete = { onIntent(PortfolioIntent.RequestDeleteTransaction(tx)) }
                )
                if (index != snapshot.transactions.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 18.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun OfflineQuotesBanner(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("portfolio_offline_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.portfolio_offline_banner),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onRetry) {
                Text(
                    text = stringResource(R.string.action_refresh),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.scrim
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 18.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Composable
private fun PortfolioSummaryHeader(summary: PortfolioSummaryVO) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp)
            .testTag("portfolio_summary_card"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1.1f)
                .padding(end = 10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = stringResource(R.string.portfolio_summary_total_balance),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = summary.totalValueFormatted,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.surface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "24h ${summary.change24hValueFormatted}",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    text = "(${summary.change24hPercentageFormatted})",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = summary.change24hTrend.textColor,
                    maxLines = 1
                )
            }
        }

        Column(
            modifier = Modifier.weight(0.95f),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            PortfolioStatRow(
                label = stringResource(R.string.portfolio_summary_total_pnl),
                value = "${summary.totalPnLFormatted} (${summary.unrealizedPnLPercentageFormatted})",
                valueColor = summary.totalPnLTrend.textColor
            )
            PortfolioStatRow(
                label = stringResource(R.string.portfolio_summary_invested),
                value = summary.totalInvestedFormatted,
                valueColor = MaterialTheme.colorScheme.surface
            )
            PortfolioStatRow(
                label = stringResource(R.string.portfolio_summary_realized),
                value = summary.realizedPnLFormatted,
                valueColor = summary.realizedPnLTrend.textColor
            )
        }
    }
}

@Composable
private fun PortfolioStatRow(
    label: String,
    value: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
            maxLines = 1,
            textAlign = TextAlign.End,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f, fill = false)
                .padding(start = 6.dp)
        )
    }
}

@Composable
private fun HoldingRow(
    holding: PortfolioHoldingVO,
    isHighlighted: Boolean,
    onOpenDetail: () -> Unit,
    onQuickAdd: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isHighlighted) {
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.28f)
                } else {
                    Color.Transparent
                }
            )
            .clickable(role = Role.Button, onClick = onOpenDetail)
            .padding(start = 18.dp, end = 6.dp, top = 12.dp, bottom = 12.dp)
            .testTag("portfolio_holding_card_${holding.coinId}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1.35f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CoinAvatar(imageUrl = holding.imageUrl, symbol = holding.symbol, sizeDp = 38)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                color = MaterialTheme.colorScheme.surface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        ) {
                            append(holding.symbol)
                        }
                        withStyle(
                            SpanStyle(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        ) {
                            append(" • ${holding.allocationPercentageFormatted}")
                        }
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val avgPrefix = stringResource(R.string.portfolio_holding_avg_prefix)
                Text(
                    text = "${holding.quantityFormatted} ${holding.symbol} • $avgPrefix ${holding.averageBuyPriceFormatted}",
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = holding.currentValueFormatted,
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.surface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End
            )
            Text(
                text = "${holding.unrealizedPnLFormatted} (${holding.unrealizedPnLPercentageFormatted})",
                style = MaterialTheme.typography.labelMedium,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = holding.unrealizedTrend.textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End
            )
        }

        IconButton(
            onClick = onQuickAdd,
            modifier = Modifier
                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                .testTag("portfolio_holding_quick_add_${holding.coinId}")
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(R.string.portfolio_holding_quick_add_cd, holding.name),
                tint = MaterialTheme.colorScheme.scrim,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun NoActivePositionsSection(onAddOperation: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.portfolio_no_open_positions_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.surface
        )
        Text(
            text = stringResource(R.string.portfolio_no_open_positions_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        TextButton(onClick = onAddOperation) {
            Text(
                text = stringResource(R.string.portfolio_new_transaction),
                color = MaterialTheme.colorScheme.scrim,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun TransactionHistoryRow(
    transaction: PortfolioTransactionVO,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isBuy = transaction.type == PortfolioTransactionType.BUY
    val typeTrend = if (isBuy) TrendColor.UP else TrendColor.DOWN
    val typeLabel = if (isBuy) {
        stringResource(R.string.portfolio_tx_type_buy_badge)
    } else {
        stringResource(R.string.portfolio_tx_type_sell_badge)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 6.dp, top = 12.dp, bottom = 12.dp)
            .testTag("portfolio_tx_card_${transaction.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1.25f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CoinAvatar(
                imageUrl = transaction.coinImageUrl,
                symbol = transaction.coinSymbol,
                sizeDp = 36
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = transaction.coinSymbol,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.surface
                    )
                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = typeTrend.textColor
                    )
                }
                Text(
                    text = "${transaction.quantityFormatted} × ${transaction.unitPriceFormatted}",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val subtitle = if (transaction.note.isNotBlank()) {
                    "${transaction.dateFormatted} • ${transaction.note}"
                } else {
                    transaction.dateFormatted
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Column(
            modifier = Modifier.padding(start = 6.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = transaction.totalValueFormatted,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.surface,
                maxLines = 1
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onEdit,
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .testTag("portfolio_tx_edit_${transaction.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = stringResource(R.string.portfolio_tx_edit_cd),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .testTag("portfolio_tx_delete_${transaction.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.portfolio_tx_delete_cd),
                    tint = TrendColor.DOWN.textColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun buildAllocationSlices(
    holdings: List<PortfolioHoldingVO>,
    palette: List<Color>,
    othersSymbol: String = "OTHERS",
    othersLabelProvider: (Int) -> String = { count -> "Others ($count)" },
    locale: Locale = Locale.getDefault()
): List<AllocationSlice> {
    if (holdings.isEmpty() || palette.isEmpty()) return emptyList()
    val positiveHoldings = holdings.filter { it.allocationPercentage > 0.0 }
    if (positiveHoldings.isEmpty()) return emptyList()

    val maxDirectSlices = 5
    val slices = mutableListOf<AllocationSlice>()
    val top = positiveHoldings.take(maxDirectSlices)
    top.forEachIndexed { index, holding ->
        slices += AllocationSlice(
            id = holding.coinId,
            symbol = holding.symbol,
            label = holding.name,
            valueText = holding.currentValueFormatted,
            percentage = holding.allocationPercentage.toFloat(),
            percentageText = holding.allocationPercentageFormatted,
            color = palette[index % palette.size]
        )
    }
    if (positiveHoldings.size > maxDirectSlices) {
        val rest = positiveHoldings.drop(maxDirectSlices)
        val restPct = rest.sumOf { it.allocationPercentage }
        val restValue = rest.sumOf { it.currentValue }
        slices += AllocationSlice(
            id = "others",
            symbol = othersSymbol,
            label = othersLabelProvider(rest.size),
            valueText = restValue.formatMoney(locale),
            percentage = restPct.toFloat(),
            percentageText = String.format(locale, "%.1f%%", restPct),
            color = palette.last()
        )
    }
    return slices
}

@ThemePreviews
@Composable
private fun PortfolioEmptyPreview() {
    DSMTheme {
        PortfolioScreenContent(
            uiState = PortfolioUiState.Empty,
            onIntent = {},
            onCoinClick = {}
        )
    }
}
