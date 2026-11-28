package br.com.terras.app.coins.detail.domain

import br.com.terras.app.common.formatAxisMoney
import br.com.terras.app.common.formatCryptoQuantity
import br.com.terras.app.common.formatMoney
import br.com.terras.app.common.formatPercentage
import br.com.terras.app.common.toFormatDate
import br.com.terras.app.dsm.ui.component.TrendColor
import br.com.terras.app.database.portfolio.PortfolioTransactionRecord
import br.com.terras.app.database.portfolio.PortfolioTransactionType
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs

data class ChartTradeMarkerVO(
    val transactionId: Long,
    val type: PortfolioTransactionType,
    val timestampMillis: Long,
    val unitPrice: Double,
    val quantityFormatted: String,
    val unitPriceFormatted: String,
    val dateFormatted: String
)

data class CoinPortfolioInsightVO(
    val coinId: String,
    val symbol: String,
    val hasOpenPosition: Boolean,
    val quantityFormatted: String,
    val averageBuyPriceFormatted: String,
    val currentValueFormatted: String,
    val investedCostFormatted: String,
    val totalPnLFormatted: String,
    val totalPnLPercentageFormatted: String,
    val totalPnLTrend: TrendColor,
    val unrealizedPnLFormatted: String,
    val realizedPnLFormatted: String,
    val hasRealizedPnL: Boolean,
    val tradeMarkers: List<ChartTradeMarkerVO>,
    val rawTotalPnL: Double = 0.0,
    val rawTotalPnLPercentage: Double = 0.0
)

class CoinDetailPortfolioCalculator @Inject constructor() {

    companion object {
        private const val QUANTITY_EPSILON = 1e-10
        private const val VALUE_EPSILON = 0.005
    }

    fun calculate(
        coinId: String,
        transactions: List<PortfolioTransactionRecord>,
        livePrice: Double?,
        locale: Locale = Locale.getDefault()
    ): CoinPortfolioInsightVO? {
        val normalizedId = coinId.trim().lowercase()
        if (normalizedId.isEmpty()) return null

        val coinTransactions = transactions
            .filter { it.coinId.trim().lowercase() == normalizedId }
            .sortedWith(compareBy<PortfolioTransactionRecord> { it.timestamp }.thenBy { it.id })

        if (coinTransactions.isEmpty()) return null

        var quantity = 0.0
        var costBasis = 0.0
        var realizedPnL = 0.0
        var totalBoughtCapital = 0.0
        var symbol = coinTransactions.last().coinSymbol.trim().uppercase()

        coinTransactions.forEach { tx ->
            if (tx.coinSymbol.isNotBlank()) {
                symbol = tx.coinSymbol.trim().uppercase()
            }
            when (tx.type) {
                PortfolioTransactionType.BUY -> {
                    val qty = tx.quantity.coerceAtLeast(0.0)
                    val unitPrice = tx.unitPrice.coerceAtLeast(0.0)
                    quantity += qty
                    costBasis += qty * unitPrice
                    totalBoughtCapital += qty * unitPrice
                }
                PortfolioTransactionType.SELL -> {
                    val sellQty = tx.quantity.coerceAtLeast(0.0).coerceAtMost(quantity)
                    if (sellQty > QUANTITY_EPSILON && quantity > QUANTITY_EPSILON) {
                        val avgCost = costBasis / quantity
                        val salePrice = tx.unitPrice.coerceAtLeast(0.0)
                        realizedPnL += (salePrice - avgCost) * sellQty
                        costBasis = (costBasis - avgCost * sellQty).coerceAtLeast(0.0)
                        quantity -= sellQty
                        if (quantity <= QUANTITY_EPSILON) {
                            quantity = 0.0
                            costBasis = 0.0
                        }
                    }
                }
            }
        }

        val hasOpenPosition = quantity > QUANTITY_EPSILON
        val avgBuyPrice = if (hasOpenPosition) costBasis / quantity else 0.0
        val effectiveLivePrice = livePrice?.takeIf { it > 0.0 }
            ?: if (hasOpenPosition) avgBuyPrice else coinTransactions.last().unitPrice
        val currentValue = if (hasOpenPosition) quantity * effectiveLivePrice else 0.0
        val unrealizedPnL = if (hasOpenPosition) currentValue - costBasis else 0.0
        val totalPnL = unrealizedPnL + realizedPnL

        val pnLPct = when {
            hasOpenPosition && costBasis > QUANTITY_EPSILON -> (unrealizedPnL / costBasis) * 100.0
            !hasOpenPosition && totalBoughtCapital > QUANTITY_EPSILON -> (realizedPnL / totalBoughtCapital) * 100.0
            else -> 0.0
        }

        val markers = coinTransactions.map { tx ->
            ChartTradeMarkerVO(
                transactionId = tx.id,
                type = tx.type,
                timestampMillis = tx.timestamp,
                unitPrice = tx.unitPrice,
                quantityFormatted = "${tx.quantity.formatCryptoQuantity(locale)} $symbol",
                unitPriceFormatted = tx.unitPrice.formatAxisMoney(locale),
                dateFormatted = tx.timestamp.toFormatDate()
            )
        }

        return CoinPortfolioInsightVO(
            coinId = normalizedId,
            symbol = symbol,
            hasOpenPosition = hasOpenPosition,
            quantityFormatted = "${quantity.formatCryptoQuantity(locale)} $symbol",
            averageBuyPriceFormatted = avgBuyPrice.formatAxisMoney(locale),
            currentValueFormatted = currentValue.formatMoney(locale),
            investedCostFormatted = costBasis.formatMoney(locale),
            totalPnLFormatted = totalPnL.formatSignedMoney(locale),
            totalPnLPercentageFormatted = pnLPct.formatPercentage(locale),
            totalPnLTrend = trendFor(totalPnL),
            unrealizedPnLFormatted = unrealizedPnL.formatSignedMoney(locale),
            realizedPnLFormatted = realizedPnL.formatSignedMoney(locale),
            hasRealizedPnL = abs(realizedPnL) > VALUE_EPSILON,
            tradeMarkers = markers,
            rawTotalPnL = totalPnL,
            rawTotalPnLPercentage = pnLPct
        )
    }

    private fun Double.formatSignedMoney(locale: Locale): String = when {
        this > VALUE_EPSILON -> "+${this.formatMoney(locale)}"
        this < -VALUE_EPSILON -> "-${abs(this).formatMoney(locale)}"
        else -> 0.0.formatMoney(locale)
    }

    private fun trendFor(value: Double): TrendColor = when {
        value > VALUE_EPSILON -> TrendColor.UP
        value < -VALUE_EPSILON -> TrendColor.DOWN
        else -> TrendColor.FLAT
    }
}
