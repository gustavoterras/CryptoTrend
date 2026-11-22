package br.com.terras.app.portfolio.domain

import br.com.terras.app.dsm.ui.component.TrendColor
import br.com.terras.app.network.portfolio.PortfolioTransactionRecord
import br.com.terras.app.network.portfolio.PortfolioTransactionType
import br.com.terras.app.portfolio.domain.model.PortfolioMarketQuote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class PortfolioCalculatorTest {

    private val calculator = PortfolioCalculator()
    private val ptBrLocale = Locale("pt", "BR")

    @Test
    fun `multiple buys compute weighted average cost and unrealized PnL accurately`() {
        val transactions = listOf(
            PortfolioTransactionRecord(
                id = 1L,
                coinId = "bitcoin",
                coinSymbol = "BTC",
                coinName = "Bitcoin",
                coinImageUrl = "https://img/btc.png",
                type = PortfolioTransactionType.BUY,
                quantity = 1.0,
                unitPrice = 100_000.0,
                timestamp = 1000L
            ),
            PortfolioTransactionRecord(
                id = 2L,
                coinId = "bitcoin",
                coinSymbol = "BTC",
                coinName = "Bitcoin",
                coinImageUrl = "https://img/btc.png",
                type = PortfolioTransactionType.BUY,
                quantity = 1.0,
                unitPrice = 200_000.0,
                timestamp = 2000L
            )
        )
        val quotes = mapOf(
            "bitcoin" to PortfolioMarketQuote(
                id = "bitcoin",
                symbol = "BTC",
                name = "Bitcoin",
                imageUrl = "https://img/btc.png",
                currentPrice = 180_000.0,
                priceChangePercentage24h = 5.0
            )
        )

        val snapshot = calculator.calculate(transactions, quotes, ptBrLocale)

        assertEquals(1, snapshot.holdings.size)
        val btc = snapshot.holdings.first()
        assertEquals(2.0, btc.quantity, 1e-9)
        assertEquals(150_000.0, btc.averageBuyPrice, 1e-6)
        assertEquals(300_000.0, btc.investedCost, 1e-6)
        assertEquals(360_000.0, btc.currentValue, 1e-6)
        assertEquals(60_000.0, btc.unrealizedPnLValue, 1e-6)
        assertEquals(20.0, btc.unrealizedPnLPercentage, 1e-6)
        assertEquals(100.0, btc.allocationPercentage, 1e-6)
        assertEquals(TrendColor.UP, snapshot.summary.totalPnLTrend)
        assertEquals(360_000.0, snapshot.summary.totalValue, 1e-6)
        assertEquals(60_000.0, snapshot.summary.totalPnLValue, 1e-6)
    }

    @Test
    fun `partial sell realizes profit while preserving remaining average cost basis`() {
        val transactions = listOf(
            PortfolioTransactionRecord(
                id = 1L,
                coinId = "ethereum",
                coinSymbol = "ETH",
                coinName = "Ethereum",
                coinImageUrl = "",
                type = PortfolioTransactionType.BUY,
                quantity = 4.0,
                unitPrice = 10_000.0,
                timestamp = 1000L
            ),
            PortfolioTransactionRecord(
                id = 2L,
                coinId = "ethereum",
                coinSymbol = "ETH",
                coinName = "Ethereum",
                coinImageUrl = "",
                type = PortfolioTransactionType.SELL,
                quantity = 1.0,
                unitPrice = 15_000.0,
                timestamp = 2000L
            )
        )
        val quotes = mapOf(
            "ethereum" to PortfolioMarketQuote(
                id = "ethereum",
                symbol = "ETH",
                name = "Ethereum",
                imageUrl = "",
                currentPrice = 12_000.0,
                priceChangePercentage24h = 0.0
            )
        )

        val snapshot = calculator.calculate(transactions, quotes, ptBrLocale)
        val eth = snapshot.holdings.first()

        assertEquals(3.0, eth.quantity, 1e-9)
        assertEquals(10_000.0, eth.averageBuyPrice, 1e-6)
        assertEquals(30_000.0, eth.investedCost, 1e-6)
        assertEquals(36_000.0, eth.currentValue, 1e-6)
        assertEquals(6_000.0, eth.unrealizedPnLValue, 1e-6)
        assertEquals(5_000.0, eth.realizedPnLValue, 1e-6)
        assertEquals(11_000.0, snapshot.summary.totalPnLValue, 1e-6)
        assertEquals(5_000.0, snapshot.summary.realizedPnLValue, 1e-6)
    }

    @Test
    fun `full exit removes active holding but retains realized PnL in summary and supports reentry`() {
        val transactions = listOf(
            PortfolioTransactionRecord(
                id = 1L,
                coinId = "solana",
                coinSymbol = "SOL",
                coinName = "Solana",
                coinImageUrl = "",
                type = PortfolioTransactionType.BUY,
                quantity = 10.0,
                unitPrice = 500.0,
                timestamp = 1000L
            ),
            PortfolioTransactionRecord(
                id = 2L,
                coinId = "solana",
                coinSymbol = "SOL",
                coinName = "Solana",
                coinImageUrl = "",
                type = PortfolioTransactionType.SELL,
                quantity = 10.0,
                unitPrice = 800.0,
                timestamp = 2000L
            )
        )

        val exitedSnapshot = calculator.calculate(transactions, emptyMap(), ptBrLocale)
        assertTrue(exitedSnapshot.holdings.isEmpty())
        assertTrue(exitedSnapshot.hasTransactions)
        assertEquals(3_000.0, exitedSnapshot.summary.realizedPnLValue, 1e-6)
        assertEquals(3_000.0, exitedSnapshot.summary.totalPnLValue, 1e-6)
        assertEquals(0.0, exitedSnapshot.summary.totalValue, 1e-6)

        val reentryTransactions = transactions + PortfolioTransactionRecord(
            id = 3L,
            coinId = "solana",
            coinSymbol = "SOL",
            coinName = "Solana",
            coinImageUrl = "",
            type = PortfolioTransactionType.BUY,
            quantity = 2.0,
            unitPrice = 900.0,
            timestamp = 3000L
        )
        val reentrySnapshot = calculator.calculate(reentryTransactions, emptyMap(), ptBrLocale)
        assertEquals(1, reentrySnapshot.holdings.size)
        val sol = reentrySnapshot.holdings.first()
        assertEquals(2.0, sol.quantity, 1e-9)
        assertEquals(900.0, sol.averageBuyPrice, 1e-6)
        assertFalse(sol.hasLiveQuote)
    }

    @Test
    fun `availableQuantityBeforeOrWithReplacement rejects overselling and protects dependent buys`() {
        val buy = PortfolioTransactionRecord(
            id = 1L,
            coinId = "bitcoin",
            coinSymbol = "BTC",
            coinName = "Bitcoin",
            coinImageUrl = "",
            type = PortfolioTransactionType.BUY,
            quantity = 1.0,
            unitPrice = 100_000.0,
            timestamp = 1000L
        )
        val sell = PortfolioTransactionRecord(
            id = 2L,
            coinId = "bitcoin",
            coinSymbol = "BTC",
            coinName = "Bitcoin",
            coinImageUrl = "",
            type = PortfolioTransactionType.SELL,
            quantity = 0.7,
            unitPrice = 120_000.0,
            timestamp = 2000L
        )

        val validSellMore = PortfolioTransactionRecord(
            id = 0L,
            coinId = "bitcoin",
            coinSymbol = "BTC",
            coinName = "Bitcoin",
            coinImageUrl = "",
            type = PortfolioTransactionType.SELL,
            quantity = 0.2,
            unitPrice = 120_000.0,
            timestamp = 3000L
        )
        assertTrue(
            calculator.availableQuantityBeforeOrWithReplacement(
                transactions = listOf(buy, sell),
                coinId = "bitcoin",
                candidateTransaction = validSellMore
            )
        )

        val oversell = validSellMore.copy(quantity = 0.5)
        assertFalse(
            calculator.availableQuantityBeforeOrWithReplacement(
                transactions = listOf(buy, sell),
                coinId = "bitcoin",
                candidateTransaction = oversell
            )
        )

        // Cannot delete initial buy because subsequent sell depends on it
        assertFalse(
            calculator.availableQuantityBeforeOrWithReplacement(
                transactions = listOf(buy, sell),
                coinId = "bitcoin",
                candidateTransaction = null,
                excludingTransactionId = buy.id
            )
        )
    }
}
