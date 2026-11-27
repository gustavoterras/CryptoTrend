package br.com.terras.app.portfolio.domain

import br.com.terras.app.common.formatAxisMoney
import br.com.terras.app.common.formatCryptoQuantity
import br.com.terras.app.common.formatMoney
import br.com.terras.app.common.formatPercentage
import br.com.terras.app.common.toFormatDate
import br.com.terras.app.dsm.ui.component.TrendColor
import br.com.terras.app.database.portfolio.PortfolioTransactionRecord
import br.com.terras.app.database.portfolio.PortfolioTransactionType
import br.com.terras.app.portfolio.domain.model.PortfolioHoldingVO
import br.com.terras.app.portfolio.domain.model.PortfolioMarketQuote
import br.com.terras.app.portfolio.domain.model.PortfolioSnapshot
import br.com.terras.app.portfolio.domain.model.PortfolioSummaryVO
import br.com.terras.app.portfolio.domain.model.PortfolioTransactionVO
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs

class PortfolioCalculator @Inject constructor() {

    private data class MutableAccumulator(
        val coinId: String,
        var symbol: String,
        var name: String,
        var imageUrl: String,
        var quantity: Double = 0.0,
        var costBasis: Double = 0.0,
        var realizedPnL: Double = 0.0
    )

    companion object {
        private const val QUANTITY_EPSILON = 1e-10
        private const val DISPLAY_VALUE_EPSILON = 0.005
    }

    fun calculate(
        transactions: List<PortfolioTransactionRecord>,
        quotes: Map<String, PortfolioMarketQuote>,
        locale: Locale = Locale.getDefault()
    ): PortfolioSnapshot {
        val chronological = transactions.sortedWith(
            compareBy<PortfolioTransactionRecord> { it.timestamp }.thenBy { it.id }
        )

        val accumulators = linkedMapOf<String, MutableAccumulator>()

        chronological.forEach { tx ->
            val coinId = tx.coinId.trim().lowercase()
            if (coinId.isEmpty()) return@forEach

            val acc = accumulators.getOrPut(coinId) {
                MutableAccumulator(
                    coinId = coinId,
                    symbol = tx.coinSymbol.trim().uppercase(),
                    name = tx.coinName.trim(),
                    imageUrl = tx.coinImageUrl.trim()
                )
            }

            if (tx.coinSymbol.isNotBlank()) acc.symbol = tx.coinSymbol.trim().uppercase()
            if (tx.coinName.isNotBlank()) acc.name = tx.coinName.trim()
            if (tx.coinImageUrl.isNotBlank()) acc.imageUrl = tx.coinImageUrl.trim()

            when (tx.type) {
                PortfolioTransactionType.BUY -> {
                    val qty = tx.quantity.coerceAtLeast(0.0)
                    val unitPrice = tx.unitPrice.coerceAtLeast(0.0)
                    acc.quantity += qty
                    acc.costBasis += qty * unitPrice
                }
                PortfolioTransactionType.SELL -> {
                    val sellQty = tx.quantity.coerceAtLeast(0.0).coerceAtMost(acc.quantity)
                    if (sellQty > QUANTITY_EPSILON && acc.quantity > QUANTITY_EPSILON) {
                        val avgCost = acc.costBasis / acc.quantity
                        val salePrice = tx.unitPrice.coerceAtLeast(0.0)
                        acc.realizedPnL += (salePrice - avgCost) * sellQty
                        acc.costBasis = (acc.costBasis - avgCost * sellQty).coerceAtLeast(0.0)
                        acc.quantity -= sellQty
                        if (acc.quantity <= QUANTITY_EPSILON) {
                            acc.quantity = 0.0
                            acc.costBasis = 0.0
                        }
                    }
                }
            }
        }

        val availableQuantities = accumulators
            .filterValues { it.quantity > QUANTITY_EPSILON }
            .mapValues { it.value.quantity }

        val activeHoldingsDraft = accumulators.values
            .filter { it.quantity > QUANTITY_EPSILON }
            .map { acc ->
                val quote = quotes[acc.coinId]
                val avgBuyPrice = if (acc.quantity > QUANTITY_EPSILON) acc.costBasis / acc.quantity else 0.0
                val currentPrice = quote?.currentPrice ?: avgBuyPrice
                val hasLiveQuote = quote != null
                val currentValue = acc.quantity * currentPrice
                val unrealizedPnL = currentValue - acc.costBasis
                val unrealizedPct = if (acc.costBasis > QUANTITY_EPSILON) {
                    (unrealizedPnL / acc.costBasis) * 100.0
                } else {
                    0.0
                }
                val change24hPct = quote?.priceChangePercentage24h ?: 0.0

                DraftHolding(
                    coinId = acc.coinId,
                    symbol = quote?.symbol?.takeIf { it.isNotBlank() } ?: acc.symbol,
                    name = quote?.name?.takeIf { it.isNotBlank() } ?: acc.name,
                    imageUrl = quote?.imageUrl?.takeIf { it.isNotBlank() } ?: acc.imageUrl,
                    quantity = acc.quantity,
                    averageBuyPrice = avgBuyPrice,
                    currentPrice = currentPrice,
                    hasLiveQuote = hasLiveQuote,
                    investedCost = acc.costBasis,
                    currentValue = currentValue,
                    unrealizedPnLValue = unrealizedPnL,
                    unrealizedPnLPercentage = unrealizedPct,
                    realizedPnLValue = acc.realizedPnL,
                    change24hPercentage = change24hPct
                )
            }
            .sortedByDescending { it.currentValue }

        val totalValue = activeHoldingsDraft.sumOf { it.currentValue }
        val totalInvested = activeHoldingsDraft.sumOf { it.investedCost }
        val totalUnrealizedPnL = totalValue - totalInvested
        val totalUnrealizedPct = if (totalInvested > QUANTITY_EPSILON) {
            (totalUnrealizedPnL / totalInvested) * 100.0
        } else {
            0.0
        }
        val totalRealizedPnL = accumulators.values.sumOf { it.realizedPnL }
        val totalPnL = totalUnrealizedPnL + totalRealizedPnL

        var previous24hPortfolioValue = 0.0
        var portfolio24hChangeValue = 0.0
        activeHoldingsDraft.forEach { holding ->
            if (holding.hasLiveQuote && holding.change24hPercentage > -99.9) {
                val prevValue = holding.currentValue / (1.0 + holding.change24hPercentage / 100.0)
                previous24hPortfolioValue += prevValue
                portfolio24hChangeValue += (holding.currentValue - prevValue)
            } else {
                previous24hPortfolioValue += holding.currentValue
            }
        }
        val portfolio24hChangePct = if (previous24hPortfolioValue > QUANTITY_EPSILON) {
            (portfolio24hChangeValue / previous24hPortfolioValue) * 100.0
        } else {
            0.0
        }

        val holdings = activeHoldingsDraft.map { draft ->
            val allocationPct = if (totalValue > QUANTITY_EPSILON) {
                (draft.currentValue / totalValue) * 100.0
            } else {
                0.0
            }
            PortfolioHoldingVO(
                coinId = draft.coinId,
                symbol = draft.symbol,
                name = draft.name,
                imageUrl = draft.imageUrl,
                quantity = draft.quantity,
                averageBuyPrice = draft.averageBuyPrice,
                currentPrice = draft.currentPrice,
                hasLiveQuote = draft.hasLiveQuote,
                investedCost = draft.investedCost,
                currentValue = draft.currentValue,
                unrealizedPnLValue = draft.unrealizedPnLValue,
                unrealizedPnLPercentage = draft.unrealizedPnLPercentage,
                realizedPnLValue = draft.realizedPnLValue,
                change24hPercentage = draft.change24hPercentage,
                allocationPercentage = allocationPct,
                quantityFormatted = draft.quantity.formatCryptoQuantity(locale),
                averageBuyPriceFormatted = draft.averageBuyPrice.formatAxisMoney(locale),
                currentPriceFormatted = draft.currentPrice.formatAxisMoney(locale),
                currentValueFormatted = draft.currentValue.formatMoney(locale),
                unrealizedPnLFormatted = draft.unrealizedPnLValue.formatSignedMoney(locale),
                unrealizedPnLPercentageFormatted = draft.unrealizedPnLPercentage.formatPercentage(locale),
                change24hFormatted = draft.change24hPercentage.formatPercentage(locale),
                allocationPercentageFormatted = String.format(locale, "%.1f%%", allocationPct),
                unrealizedTrend = trendFor(draft.unrealizedPnLValue, DISPLAY_VALUE_EPSILON),
                change24hTrend = trendFor(draft.change24hPercentage, 0.05)
            )
        }

        val ledger = chronological.reversed().map { tx ->
            val totalTx = tx.quantity * tx.unitPrice
            PortfolioTransactionVO(
                id = tx.id,
                coinId = tx.coinId.trim().lowercase(),
                coinSymbol = tx.coinSymbol.trim().uppercase(),
                coinName = tx.coinName.trim(),
                coinImageUrl = tx.coinImageUrl.trim(),
                type = tx.type,
                quantity = tx.quantity,
                unitPrice = tx.unitPrice,
                totalValue = totalTx,
                timestamp = tx.timestamp,
                note = tx.note,
                dateFormatted = tx.timestamp.toFormatDate(),
                quantityFormatted = tx.quantity.formatCryptoQuantity(locale),
                unitPriceFormatted = tx.unitPrice.formatAxisMoney(locale),
                totalValueFormatted = totalTx.formatMoney(locale)
            )
        }

        val summary = PortfolioSummaryVO(
            totalValue = totalValue,
            totalInvested = totalInvested,
            unrealizedPnLValue = totalUnrealizedPnL,
            unrealizedPnLPercentage = totalUnrealizedPct,
            realizedPnLValue = totalRealizedPnL,
            totalPnLValue = totalPnL,
            change24hValue = portfolio24hChangeValue,
            change24hPercentage = portfolio24hChangePct,
            assetCount = holdings.size,
            totalValueFormatted = totalValue.formatMoney(locale),
            totalInvestedFormatted = totalInvested.formatMoney(locale),
            totalPnLFormatted = totalPnL.formatSignedMoney(locale),
            unrealizedPnLFormatted = totalUnrealizedPnL.formatSignedMoney(locale),
            unrealizedPnLPercentageFormatted = totalUnrealizedPct.formatPercentage(locale),
            realizedPnLFormatted = totalRealizedPnL.formatSignedMoney(locale),
            change24hValueFormatted = portfolio24hChangeValue.formatSignedMoney(locale),
            change24hPercentageFormatted = portfolio24hChangePct.formatPercentage(locale),
            totalPnLTrend = trendFor(totalPnL, DISPLAY_VALUE_EPSILON),
            unrealizedPnLTrend = trendFor(totalUnrealizedPnL, DISPLAY_VALUE_EPSILON),
            realizedPnLTrend = trendFor(totalRealizedPnL, DISPLAY_VALUE_EPSILON),
            change24hTrend = trendFor(portfolio24hChangePct, 0.05)
        )

        return PortfolioSnapshot(
            summary = summary,
            holdings = holdings,
            transactions = ledger,
            availableQuantitiesByCoin = availableQuantities,
            hasTransactions = transactions.isNotEmpty()
        )
    }

    fun availableQuantityBeforeOrWithReplacement(
        transactions: List<PortfolioTransactionRecord>,
        coinId: String,
        candidateTransaction: PortfolioTransactionRecord?,
        excludingTransactionId: Long? = null
    ): Boolean {
        val normalizedCoin = coinId.trim().lowercase()
        val updatedList = buildList {
            transactions.forEach { existing ->
                if (excludingTransactionId == null || existing.id != excludingTransactionId) {
                    add(existing)
                }
            }
            if (candidateTransaction != null) {
                add(candidateTransaction)
            }
        }
            .filter { it.coinId.trim().lowercase() == normalizedCoin }
            .sortedWith(compareBy<PortfolioTransactionRecord> { it.timestamp }.thenBy { it.id })

        var held = 0.0
        updatedList.forEach { tx ->
            when (tx.type) {
                PortfolioTransactionType.BUY -> held += tx.quantity.coerceAtLeast(0.0)
                PortfolioTransactionType.SELL -> {
                    held -= tx.quantity.coerceAtLeast(0.0)
                    if (held < -QUANTITY_EPSILON) return false
                    if (held <= QUANTITY_EPSILON) held = 0.0
                }
            }
        }
        return true
    }

    private fun Double.formatSignedMoney(locale: Locale): String = when {
        this > DISPLAY_VALUE_EPSILON -> "+${this.formatMoney(locale)}"
        this < -DISPLAY_VALUE_EPSILON -> "-${abs(this).formatMoney(locale)}"
        else -> 0.0.formatMoney(locale)
    }

    private fun trendFor(value: Double, flatThreshold: Double): TrendColor = when {
        value > flatThreshold -> TrendColor.UP
        value < -flatThreshold -> TrendColor.DOWN
        else -> TrendColor.FLAT
    }

    private data class DraftHolding(
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
        val change24hPercentage: Double
    )
}
