package br.com.terras.app.common

import org.junit.Assert
import org.junit.Test
import java.util.Locale


class UtilsTest {

    @Test
    fun `WHEN called formatPercentage THEN returns a string formatted`() {
        val percentage = 100.0.formatPercentage(Locale.US)
        Assert.assertNotNull("+100.0%", percentage)
    }

    @Test
    fun `WHEN called formatMoney THEN returns a string formatted`() {
        val money = 10.0.formatMoney(Locale.US)
        Assert.assertEquals("$10.00", money)
    }

    @Test
    fun `WHEN called toFormatDate THEN returns a string formatted`() {
        val date = 1719677287443.toFormatDate()
        Assert.assertNotNull(date)
    }

    @Test
    fun `WHEN called formatLivePrice THEN adapts decimal scale and guards non-finite input`() {
        Assert.assertEquals("$95,432.10", 95432.1.formatLivePrice(Locale.US))
        Assert.assertEquals("$0.1425", 0.1425.formatLivePrice(Locale.US))
        Assert.assertEquals("$0.00001425", 0.00001425.formatLivePrice(Locale.US))
        Assert.assertEquals("$0.00", Double.NaN.formatLivePrice(Locale.US))
        Assert.assertEquals("$0.00", (-5.0).formatLivePrice(Locale.US))
    }

    @Test
    fun `WHEN called currentVsCurrency THEN maps English to usd and Portuguese to brl`() {
        Assert.assertEquals("usd", currentVsCurrency(Locale.US))
        Assert.assertEquals("brl", currentVsCurrency(Locale("pt", "BR")))
    }

    @Test
    fun `WHEN called formatShortMoney THEN formats millions billions and trillions with M B T`() {
        val ptBr = Locale("pt", "BR")
        Assert.assertEquals("$850,000", 850_000.0.formatShortMoney(Locale.US))
        Assert.assertEquals("$1.25M", 1_250_000.0.formatShortMoney(Locale.US))
        Assert.assertEquals("$35.42B", 35_420_000_000.0.formatShortMoney(Locale.US))
        Assert.assertEquals("$1.85T", 1_850_000_000_000.0.formatShortMoney(Locale.US))

        Assert.assertEquals("R$ 1,25M", 1_250_000.0.formatShortMoney(ptBr))
        Assert.assertEquals("R$ 35,42B", 35_420_000_000.0.formatShortMoney(ptBr))
        Assert.assertEquals("R$ 1,85T", 1_850_000_000_000.0.formatShortMoney(ptBr))
    }

    @Test
    fun `WHEN called formatShortNumber THEN formats large counts with M B T`() {
        val ptBr = Locale("pt", "BR")
        Assert.assertEquals("15,420", 15_420.0.formatShortNumber(Locale.US))
        Assert.assertEquals("19.8M", 19_800_000.0.formatShortNumber(Locale.US))
        Assert.assertEquals("2.5B", 2_500_000_000.0.formatShortNumber(Locale.US))
        Assert.assertEquals("589.5T", 589_500_000_000_000.0.formatShortNumber(Locale.US))

        Assert.assertEquals("19,8M", 19_800_000.0.formatShortNumber(ptBr))
        Assert.assertEquals("589,5T", 589_500_000_000_000.0.formatShortNumber(ptBr))
    }
}