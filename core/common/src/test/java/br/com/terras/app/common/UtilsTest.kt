package br.com.terras.app.common

import org.junit.Assert
import org.junit.Test


class UtilsTest {

    @Test
    fun `WHEN called formatPercentage THEN returns a string formatted`() {
        val percentage = 100.0.formatPercentage()
        Assert.assertNotNull(percentage)
    }

    @Test
    fun `WHEN called formatMoney THEN returns a string formatted`() {
        val money = 10.0.formatMoney()
        Assert.assertNotNull(money)
    }
}