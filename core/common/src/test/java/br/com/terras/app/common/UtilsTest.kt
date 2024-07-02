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
}