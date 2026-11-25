package br.com.terras.app.coins.list.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GlobalMarketResponse(
    @SerialName("data") val data: GlobalMarketDataResponse = GlobalMarketDataResponse()
)

@Serializable
data class GlobalMarketDataResponse(
    @SerialName("active_cryptocurrencies") val activeCryptocurrencies: Int = 0,
    @SerialName("upcoming_icos") val upcomingIcos: Int = 0,
    @SerialName("ongoing_icos") val ongoingIcos: Int = 0,
    @SerialName("ended_icos") val endedIcos: Int = 0,
    @SerialName("markets") val markets: Int = 0,
    @SerialName("total_market_cap") val totalMarketCap: Map<String, Double> = emptyMap(),
    @SerialName("total_volume") val totalVolume: Map<String, Double> = emptyMap(),
    @SerialName("market_cap_percentage") val marketCapPercentage: Map<String, Double> = emptyMap(),
    @SerialName("market_cap_change_percentage_24h_usd") val marketCapChangePercentage24hUsd: Double = 0.0,
    @SerialName("updated_at") val updatedAt: Long = 0L,
    @SerialName("fear_and_greed_index") val fearAndGreedIndex: FearAndGreedIndexResponse? = null
)

@Serializable
data class FearAndGreedIndexResponse(
    @SerialName("value") val value: Int? = null,
    @SerialName("sentiment") val sentiment: String? = null
)

@Serializable
data class AlternativeFngResponse(
    @SerialName("data") val data: List<AlternativeFngEntry> = emptyList()
)

@Serializable
data class AlternativeFngEntry(
    @SerialName("value") val value: String = "",
    @SerialName("value_classification") val valueClassification: String = ""
)
