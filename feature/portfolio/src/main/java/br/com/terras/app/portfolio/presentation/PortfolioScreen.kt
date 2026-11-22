package br.com.terras.app.portfolio.presentation

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import br.com.terras.app.dsm.ui.component.SegmentedControl
import br.com.terras.app.dsm.ui.component.ShimmerEffect
import br.com.terras.app.dsm.ui.component.StickyHeaderView
import br.com.terras.app.dsm.ui.component.TrendColor
import br.com.terras.app.dsm.ui.component.portfolioChartPalette
import br.com.terras.app.dsm.ui.theme.DSMTheme
import br.com.terras.app.network.portfolio.PortfolioTransactionType
import br.com.terras.app.portfolio.domain.model.PortfolioHoldingVO
import br.com.terras.app.portfolio.domain.model.PortfolioSnapshot
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
            title = { Text("Excluir operação") },
            text = {
                val typeLabel = if (tx.type == PortfolioTransactionType.BUY) "compra" else "venda"
                Text(
                    "Deseja excluir esta $typeLabel de ${tx.quantityFormatted} ${tx.coinSymbol} (${tx.totalValueFormatted})?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.handleIntent(PortfolioIntent.ConfirmDeleteTransaction) },
                    modifier = Modifier.testTag("portfolio_confirm_delete_btn")
                ) {
                    Text(
                        text = "Excluir",
                        color = TrendColor.DOWN.textColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.handleIntent(PortfolioIntent.DismissDeleteDialog) }) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            title = { Text("Operação protegida") },
            text = { Text(msg) },
            confirmButton = {
                RippleButton(
                    text = "Entendi",
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
private fun PortfolioLoadingView(topPadding: androidx.compose.ui.unit.Dp) {
    val shimmerBrush = ShimmerEffect()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = topPadding + 12.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(shimmerBrush)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(shimmerBrush)
        )
        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(78.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(shimmerBrush)
            )
        }
    }
}

@Composable
private fun PortfolioEmptyView(
    topPadding: androidx.compose.ui.unit.Dp,
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
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
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
                    text = "Seu Portfólio Cripto",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.surface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Registre suas compras e vendas para acompanhar saldo consolidado, preço médio, lucro/prejuízo e alocação direto no dispositivo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

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
                        text = "Registrar primeira operação",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun PortfolioSuccessContent(
    state: PortfolioUiState.Content,
    topPadding: androidx.compose.ui.unit.Dp,
    onIntent: (PortfolioIntent) -> Unit,
    onCoinClick: (String) -> Unit
) {
    val snapshot = state.snapshot
    val palette = portfolioChartPalette()
    val allocationSlices = remember(snapshot.holdings, palette) {
        buildAllocationSlices(snapshot.holdings, palette)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("portfolio_content_list"),
        contentPadding = PaddingValues(
            top = topPadding + 8.dp,
            bottom = 116.dp,
            start = 16.dp,
            end = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (state.isOfflineFallback) {
            item(key = "offline_banner") {
                OfflineQuotesBanner(
                    onRetry = { onIntent(PortfolioIntent.Refresh) }
                )
            }
        }

        item(key = "summary_card") {
            PortfolioSummaryCard(summary = snapshot.summary)
        }

        if (allocationSlices.isNotEmpty()) {
            item(key = "allocation_card") {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Distribuição da carteira",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.surface
                        )
                        AllocationDonutChart(
                            slices = allocationSlices,
                            totalValueText = snapshot.summary.totalValueFormatted,
                            assetCountLabel = if (snapshot.summary.assetCount == 1) {
                                "1 ativo"
                            } else {
                                "${snapshot.summary.assetCount} ativos"
                            },
                            selectedSliceId = state.selectedSliceId,
                            onSelectSlice = { onIntent(PortfolioIntent.SelectAllocationSlice(it)) }
                        )
                    }
                }
            }
        }

        item(key = "subtab_toggle") {
            SegmentedControl(
                items = PortfolioSubTab.entries.map { it.label },
                defaultSelectedItemIndex = PortfolioSubTab.entries.indexOf(state.activeTab),
                equalWidth = true,
                itemHeight = 40.dp,
                cornerRadius = 12.dp,
                color = MaterialTheme.colorScheme.scrim,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.tertiary,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .padding(4.dp)
                    .testTag("portfolio_subtab_control"),
                onItemSelection = { idx ->
                    val chosen = PortfolioSubTab.entries.getOrElse(idx) { PortfolioSubTab.ASSETS }
                    onIntent(PortfolioIntent.SelectSubTab(chosen))
                }
            )
        }

        if (state.activeTab == PortfolioSubTab.ASSETS) {
            if (snapshot.holdings.isEmpty()) {
                item(key = "no_active_positions") {
                    NoActivePositionsCard(
                        onAddOperation = { onIntent(PortfolioIntent.OpenNewTransaction()) }
                    )
                }
            } else {
                items(snapshot.holdings, key = { "holding_${it.coinId}" }) { holding ->
                    HoldingCard(
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
                }
            }
        } else {
            items(snapshot.transactions, key = { "tx_${it.id}" }) { tx ->
                TransactionHistoryCard(
                    transaction = tx,
                    onEdit = { onIntent(PortfolioIntent.OpenEditTransaction(tx)) },
                    onDelete = { onIntent(PortfolioIntent.RequestDeleteTransaction(tx)) }
                )
            }
        }
    }
}

@Composable
private fun OfflineQuotesBanner(onRetry: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.tertiary,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("portfolio_offline_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Sem cotações ao vivo. Exibindo dados locais.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onRetry) {
                Text(
                    text = "Atualizar",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.scrim
                )
            }
        }
    }
}

@Composable
private fun PortfolioSummaryCard(summary: PortfolioSummaryVO) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("portfolio_summary_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Saldo total",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = summary.totalValueFormatted,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.surface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = summary.change24hTrend.color
                ) {
                    Text(
                        text = "24h ${summary.change24hPercentageFormatted}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = summary.change24hTrend.onColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryMetricTile(
                    label = "P&L total",
                    value = summary.totalPnLFormatted,
                    subValue = summary.unrealizedPnLPercentageFormatted,
                    valueColor = summary.totalPnLTrend.textColor,
                    modifier = Modifier.weight(1f)
                )
                SummaryMetricTile(
                    label = "Investido atual",
                    value = summary.totalInvestedFormatted,
                    subValue = "Realizado ${summary.realizedPnLFormatted}",
                    valueColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SummaryMetricTile(
    label: String,
    value: String,
    subValue: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.tertiary,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subValue,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HoldingCard(
    holding: PortfolioHoldingVO,
    isHighlighted: Boolean,
    onOpenDetail: () -> Unit,
    onQuickAdd: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted) {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.primary
            }
        ),
        border = BorderStroke(
            width = if (isHighlighted) 1.5.dp else 1.dp,
            color = if (isHighlighted) MaterialTheme.colorScheme.scrim else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(role = Role.Button, onClick = onOpenDetail)
            .testTag("portfolio_holding_card_${holding.coinId}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CoinAvatar(imageUrl = holding.imageUrl, symbol = holding.symbol, sizeDp = 40)
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = holding.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.surface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.tertiary
                            ) {
                                Text(
                                    text = holding.allocationPercentageFormatted,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "${holding.quantityFormatted} ${holding.symbol} • PM ${holding.averageBuyPriceFormatted}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onQuickAdd,
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .testTag("portfolio_holding_quick_add_${holding.coinId}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Nova operação de ${holding.name}",
                        tint = MaterialTheme.colorScheme.scrim
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (holding.hasLiveQuote) {
                            "Atual: ${holding.currentPriceFormatted}"
                        } else {
                            "Preço médio: ${holding.averageBuyPriceFormatted}"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = holding.currentValueFormatted,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.surface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = holding.unrealizedTrend.color
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = holding.unrealizedPnLFormatted,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = holding.unrealizedTrend.onColor
                        )
                        Text(
                            text = holding.unrealizedPnLPercentageFormatted,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = holding.unrealizedTrend.onColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NoActivePositionsCard(onAddOperation: () -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Nenhuma posição em aberto",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.surface
            )
            Text(
                text = "Todas as suas posições foram encerradas. Consulte o Histórico ou registre uma nova compra.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            TextButton(onClick = onAddOperation) {
                Text(
                    text = "Nova operação",
                    color = MaterialTheme.colorScheme.scrim,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun TransactionHistoryCard(
    transaction: PortfolioTransactionVO,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isBuy = transaction.type == PortfolioTransactionType.BUY
    val badgeTrend = if (isBuy) TrendColor.UP else TrendColor.DOWN
    val badgeLabel = if (isBuy) "COMPRA" else "VENDA"

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("portfolio_tx_card_${transaction.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CoinAvatar(
                        imageUrl = transaction.coinImageUrl,
                        symbol = transaction.coinSymbol,
                        sizeDp = 36
                    )
                    Column(modifier = Modifier.weight(1f)) {
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
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = badgeTrend.color
                            ) {
                                Text(
                                    text = badgeLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeTrend.onColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = transaction.dateFormatted,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                            contentDescription = "Editar operação",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(19.dp)
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
                            contentDescription = "Excluir operação",
                            tint = TrendColor.DOWN.textColor,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${transaction.quantityFormatted} ${transaction.coinSymbol} × ${transaction.unitPriceFormatted}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = transaction.totalValueFormatted,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.surface
                )
            }

            if (transaction.note.isNotBlank()) {
                Text(
                    text = transaction.note,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun buildAllocationSlices(
    holdings: List<PortfolioHoldingVO>,
    palette: List<Color>,
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
            symbol = "OUTROS",
            label = "Outros (${rest.size})",
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
