package br.com.terras.app.coins.detail.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CoinDetailResponse(
    @SerialName("id") val id: String,
    @SerialName("symbol") val symbol: String,
    @SerialName("name") val name: String,
    @SerialName("image") val image: CoinDetailImage,
    @SerialName("market_cap_rank") val marketCapRank: Int?,
    @SerialName("market_data") val marketData: CoinDetailMarketData
)

@Serializable
data class CoinDetailImage(
    @SerialName("thumb") val thumb: String,
    @SerialName("small") val small: String,
    @SerialName("large") val large: String
)

@Serializable
data class CoinDetailMarketData(
    @SerialName("current_price") val currentPrice: CoinValue,
    @SerialName("price_change_percentage_24h") val priceChangePercentage24h: Double,
    @SerialName("price_change_percentage_7d") val priceChangePercentage7d: Double,
    @SerialName("price_change_percentage_30d") val priceChangePercentage30d: Double,
    @SerialName("price_change_percentage_60d") val priceChangePercentage60d: Double,
    @SerialName("price_change_percentage_1y") val priceChangePercentage1y: Double,
    @SerialName("market_cap") val marketCap: CoinValue?,
    @SerialName("total_volume") val totalVolume: CoinValue?,
    @SerialName("low_24h") val low24h: CoinValue?,
    @SerialName("high_24h") val high24h: CoinValue?,
    @SerialName("atl_date") val atlDate: CoinDate?,
    @SerialName("atl") val atlValue: CoinValue?,
    @SerialName("ath_date") val athDate: CoinDate?,
    @SerialName("ath") val athValue: CoinValue?,
    @SerialName("max_supply") val maxSupply: Double?,
    @SerialName("total_supply") val totalSupply: Double?,
    @SerialName("circulating_supply") val circulatingSupply: Double?,
    @SerialName("last_updated") val lastUpdated: String?,
)

@Serializable
data class CoinValue(
    @SerialName("brl") val brl: Double,
    @SerialName("usd") val usd: Double,
)

@Serializable
data class CoinDate(
    @SerialName("brl") val brl: String,
    @SerialName("usd") val usd: String,
)