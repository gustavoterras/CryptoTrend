package br.com.terras.app.coins.detail.domain

import br.com.terras.app.dsm.ui.component.TrendColor
import br.com.terras.app.database.portfolio.PortfolioTransactionRecord
import br.com.terras.app.database.portfolio.PortfolioTransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class CoinDetailPortfolioCalculatorTest {

    private val calculator = CoinDetailPortfolioCalculator()
    private val localeUs = Locale.US

    @Test
    fun `returns null when coin has no transactions in portfolio`() {
        val transactions = listOf(
            PortfolioTransactionRecord(
                id = 1L,
                coinId = "ethereum",
                coinSymbol = "ETH",
                coinName = "Ethereum",
                coinImageUrl = "",
                type = PortfolioTransactionType.BUY,
                quantity = 2.0,
                unitPrice = 3000.0,
                timestamp = 1_700_000_000_000L
            )
        )

        val insight = calculator.calculate("bitcoin", transactions, livePrice = 90000.0, locale = localeUs)
        assertNull(insight)
    }

    @Test
    fun `calculates weighted average price unrealized pnl and markers for open position`() {
        val transactions = listOf(
            PortfolioTransactionRecord(
                id = 1L,
                coinId = "Bitcoin",
                coinSymbol = "btc",
                coinName = "Bitcoin",
                coinImageUrl = "",
                type = PortfolioTransactionType.BUY,
                quantity = 1.0,
                unitPrice = 50000.0,
                timestamp = 1_700_000_000_000L
            ),
            PortfolioTransactionRecord(
                id = 2L,
                coinId = "bitcoin",
                coinSymbol = "BTC",
                coinName = "Bitcoin",
                coinImageUrl = "",
                type = PortfolioTransactionType.BUY,
                quantity = 1.0,
                unitPrice = 70000.0,
                timestamp = 1_700_100_000_000L
            )
        )

        val insight = calculator.calculate("bitcoin", transactions, livePrice = 80000.0, locale = localeUs)

        assertNotNull(insight)
        insight!!
        assertTrue(insight.hasOpenPosition)
        assertEquals("BTC", insight.symbol)
        assertEquals(TrendColor.UP, insight.totalPnLTrend)
        assertEquals(2, insight.tradeMarkers.size)
        assertEquals(PortfolioTransactionType.BUY, insight.tradeMarkers[0].type)
        assertTrue(insight.totalPnLFormatted.startsWith("+"))
        assertEquals("+33.3%", insight.totalPnLPercentageFormatted)
    }

    @Test
    fun `calculates realized and unrealized pnl across partial buy and sell`() {
        val transactions = listOf(
            PortfolioTransactionRecord(
                id = 1L,
                coinId = "solana",
                coinSymbol = "SOL",
                coinName = "Solana",
                coinImageUrl = "",
                type = PortfolioTransactionType.BUY,
                quantity = 10.0,
                unitPrice = 100.0,
                timestamp = 1_700_000_000_000L
            ),
            PortfolioTransactionRecord(
                id = 2L,
                coinId = "solana",
                coinSymbol = "SOL",
                coinName = "Solana",
                coinImageUrl = "",
                type = PortfolioTransactionType.SELL,
                quantity = 4.0,
                unitPrice = 150.0,
                timestamp = 1_700_200_000_000L
            )
        )

        val insight = calculator.calculate("solana", transactions, livePrice = 90.0, locale = localeUs)

        assertNotNull(insight)
        insight!!
        assertTrue(insight.hasOpenPosition)
        assertTrue(insight.hasRealizedPnL)
        // Realized: (150 - 100) * 4 = +200; Unrealized: (90 - 100) * 6 = -60; Total = +140
        assertEquals(TrendColor.UP, insight.totalPnLTrend)
        assertEquals(2, insight.tradeMarkers.size)
        assertEquals(PortfolioTransactionType.BUY, insight.tradeMarkers[0].type)
        assertEquals(PortfolioTransactionType.SELL, insight.tradeMarkers[1].type)
        assertTrue(insight.realizedPnLFormatted.startsWith("+"))
    }

    @Test
    fun `marks position closed when full balance sold`() {
        val transactions = listOf(
            PortfolioTransactionRecord(
                id = 1L,
                coinId = "cardano",
                coinSymbol = "ADA",
                coinName = "Cardano",
                coinImageUrl = "",
                type = PortfolioTransactionType.BUY,
                quantity = 100.0,
                unitPrice = 0.80,
                timestamp = 1_700_000_000_000L
            ),
            PortfolioTransactionRecord(
                id = 2L,
                coinId = "cardano",
                coinSymbol = "ADA",
                coinName = "Cardano",
                coinImageUrl = "",
                type = PortfolioTransactionType.SELL,
                quantity = 100.0,
                unitPrice = 0.60,
                timestamp = 1_700_200_000_000L
            )
        )

        val insight = calculator.calculate("cardano", transactions, livePrice = 0.70, locale = localeUs)

        assertNotNull(insight)
        insight!!
        assertFalse(insight.hasOpenPosition)
        assertTrue(insight.hasRealizedPnL)
        assertEquals(TrendColor.DOWN, insight.totalPnLTrend)
        assertTrue(insight.totalPnLFormatted.startsWith("-"))
        assertEquals(2, insight.tradeMarkers.size)
    }
}
