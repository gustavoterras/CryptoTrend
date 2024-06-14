package br.com.terras.app.coins.data.model

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
    @SerialName("price_change_percentage_24h") val priceChangePercentage: Double
)