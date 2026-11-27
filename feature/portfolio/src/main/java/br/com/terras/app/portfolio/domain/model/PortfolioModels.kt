package br.com.terras.app.portfolio.domain.model

import br.com.terras.app.dsm.ui.component.TrendColor
import br.com.terras.app.database.portfolio.PortfolioTransactionType

data class PortfolioMarketQuote(
    val id: String,
    val symbol: String,
    val name: String,
    val imageUrl: String,
    val currentPrice: Double,
    val priceChangePercentage24h: Double
)

data class SelectableCoinVO(
    val id: String,
    val symbol: String,
    val name: String,
    val imageUrl: String,
    val currentPrice: Double? = null
)

data class PortfolioSummaryVO(
    val totalValue: Double,
    val totalInvested: Double,
    val unrealizedPnLValue: Double,
    val unrealizedPnLPercentage: Double,
    val realizedPnLValue: Double,
    val totalPnLValue: Double,
    val change24hValue: Double,
    val change24hPercentage: Double,
    val assetCount: Int,
    val totalValueFormatted: String,
    val totalInvestedFormatted: String,
    val totalPnLFormatted: String,
    val unrealizedPnLFormatted: String,
    val unrealizedPnLPercentageFormatted: String,
    val realizedPnLFormatted: String,
    val change24hValueFormatted: String,
    val change24hPercentageFormatted: String,
    val totalPnLTrend: TrendColor,
    val unrealizedPnLTrend: TrendColor,
    val realizedPnLTrend: TrendColor,
    val change24hTrend: TrendColor
)

data class PortfolioHoldingVO(
    val coinId: String,
    val symbol: String,
    val name: String,
    val imageUrl: String,
    val quantity: Double,
    val averageBuyPrice: Double,
    val currentPrice: Double,
    val hasLiveQuote: Boolean,
    val investedCost: Double,
    val currentValue: Double,
    val unrealizedPnLValue: Double,
    val unrealizedPnLPercentage: Double,
    val realizedPnLValue: Double,
    val change24hPercentage: Double,
    val allocationPercentage: Double,
    val quantityFormatted: String,
    val averageBuyPriceFormatted: String,
    val currentPriceFormatted: String,
    val currentValueFormatted: String,
    val unrealizedPnLFormatted: String,
    val unrealizedPnLPercentageFormatted: String,
    val change24hFormatted: String,
    val allocationPercentageFormatted: String,
    val unrealizedTrend: TrendColor,
    val change24hTrend: TrendColor
)

data class PortfolioTransactionVO(
    val id: Long,
    val coinId: String,
    val coinSymbol: String,
    val coinName: String,
    val coinImageUrl: String,
    val type: PortfolioTransactionType,
    val quantity: Double,
    val unitPrice: Double,
    val totalValue: Double,
    val timestamp: Long,
    val note: String,
    val dateFormatted: String,
    val quantityFormatted: String,
    val unitPriceFormatted: String,
    val totalValueFormatted: String
)

data class PortfolioSnapshot(
    val summary: PortfolioSummaryVO,
    val holdings: List<PortfolioHoldingVO>,
    val transactions: List<PortfolioTransactionVO>,
    val availableQuantitiesByCoin: Map<String, Double>,
    val hasTransactions: Boolean
)
