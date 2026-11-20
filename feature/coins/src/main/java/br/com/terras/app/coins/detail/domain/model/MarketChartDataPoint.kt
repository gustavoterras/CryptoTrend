package br.com.terras.app.coins.detail.domain.model

data class MarketChartDataPoint(
    val date: String,
    val price: Double,
    val axisTime: String = "",
    val hourLabel: String = "",
    val dayLabel: String = ""
)