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
}