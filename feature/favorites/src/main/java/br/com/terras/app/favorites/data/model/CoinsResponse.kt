package br.com.terras.app.favorites.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CoinsResponse(
    @SerialName("id") val id: String = "",
    @SerialName("symbol") val symbol: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("image") val image: String = "",
    @SerialName("market_cap_rank") val marketCapRank: Int = 0,
    @SerialName("current_price") val price: Double = 0.0,
    @SerialName("price_change_percentage_24h") val priceChangePercentage: Double = 0.0,
    @SerialName("total_volume") val totalVolume: Double? = null,
    @SerialName("sparkline_in_7d") val sparklineIn7d: SparklineResponse? = null
)

@Serializable
data class SparklineResponse(
    @SerialName("price") val price: List<Double> = emptyList()
)