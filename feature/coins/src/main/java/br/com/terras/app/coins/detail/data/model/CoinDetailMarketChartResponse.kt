package br.com.terras.app.coins.detail.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CoinDetailMarketChartResponse(
    @SerialName("prices") val prices: List<List<Double>>,
    @SerialName("market_caps") val marketCaps: List<List<Double>>,
    @SerialName("total_volumes") val totalVolumes: List<List<Double>>
)