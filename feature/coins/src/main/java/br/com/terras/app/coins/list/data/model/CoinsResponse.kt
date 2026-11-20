package br.com.terras.app.coins.list.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CoinsResponse(
    @SerialName("id") val id: String,
    @SerialName("symbol") val symbol: String,
    @SerialName("name") val name: String,
    @SerialName("image") val image: String,
    @SerialName("market_cap_rank") val marketCapRank: Int,
    @SerialName("current_price") val price: Double,
    @SerialName("price_change_percentage_24h") val priceChangePercentage: Double,
    @SerialName("total_volume") val totalVolume: Double? = null,
    @SerialName("sparkline_in_7d") val sparklineIn7d: SparklineResponse? = null
)

@Serializable
data class SparklineResponse(
    @SerialName("price") val price: List<Double> = emptyList()
)