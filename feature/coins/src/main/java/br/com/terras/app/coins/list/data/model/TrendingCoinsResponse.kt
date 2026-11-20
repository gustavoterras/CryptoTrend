package br.com.terras.app.coins.list.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TrendingCoinsResponse(
    @SerialName("coins") val coins: List<TrendingCoinWrapper> = emptyList()
)

@Serializable
data class TrendingCoinWrapper(
    @SerialName("item") val item: TrendingCoinItem = TrendingCoinItem()
)

@Serializable
data class TrendingCoinItem(
    @SerialName("id") val id: String = "",
    @SerialName("symbol") val symbol: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("market_cap_rank") val marketCapRank: Int? = null,
    @SerialName("thumb") val thumb: String = "",
    @SerialName("small") val small: String = "",
    @SerialName("large") val large: String = "",
    @SerialName("score") val score: Int = 0,
    @SerialName("data") val data: TrendingCoinData? = null
)

@Serializable
data class TrendingCoinData(
    @SerialName("price") val price: Double? = null,
    @SerialName("price_change_percentage_24h") val priceChangePercentage24h: Map<String, Double> = emptyMap(),
    @SerialName("total_volume") val totalVolume: String? = null,
    @SerialName("sparkline") val sparkline: String? = null
)
