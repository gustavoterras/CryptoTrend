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
    @SerialName("market_data") val marketData: CoinDetailMarketData,
    @SerialName("description") val description: Map<String, String?>? = null,
    @SerialName("categories") val categories: List<String?>? = null,
    @SerialName("genesis_date") val genesisDate: String? = null,
    @SerialName("hashing_algorithm") val hashingAlgorithm: String? = null,
    @SerialName("block_time_in_minutes") val blockTimeInMinutes: Double? = null,
    @SerialName("sentiment_votes_up_percentage") val sentimentVotesUpPercentage: Double? = null,
    @SerialName("sentiment_votes_down_percentage") val sentimentVotesDownPercentage: Double? = null,
    @SerialName("watchlist_portfolio_users") val watchlistPortfolioUsers: Int? = null,
    @SerialName("platforms") val platforms: Map<String, String?>? = null,
    @SerialName("links") val links: CoinDetailLinksResponse? = null,
    @SerialName("tickers") val tickers: List<CoinDetailTickerResponse>? = null
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
    @SerialName("price_change_percentage_24h") val priceChangePercentage24h: Double = 0.0,
    @SerialName("price_change_percentage_7d") val priceChangePercentage7d: Double = 0.0,
    @SerialName("price_change_percentage_14d") val priceChangePercentage14d: Double? = null,
    @SerialName("price_change_percentage_30d") val priceChangePercentage30d: Double = 0.0,
    @SerialName("price_change_percentage_60d") val priceChangePercentage60d: Double = 0.0,
    @SerialName("price_change_percentage_200d") val priceChangePercentage200d: Double? = null,
    @SerialName("price_change_percentage_1y") val priceChangePercentage1y: Double = 0.0,
    @SerialName("market_cap") val marketCap: CoinValue? = null,
    @SerialName("fully_diluted_valuation") val fullyDilutedValuation: Map<String, Double?>? = null,
    @SerialName("total_value_locked") val totalValueLocked: Map<String, Double?>? = null,
    @SerialName("total_volume") val totalVolume: CoinValue? = null,
    @SerialName("low_24h") val low24h: CoinValue? = null,
    @SerialName("high_24h") val high24h: CoinValue? = null,
    @SerialName("atl_date") val atlDate: CoinDate? = null,
    @SerialName("atl") val atlValue: CoinValue? = null,
    @SerialName("atl_change_percentage") val atlChangePercentage: Map<String, Double?>? = null,
    @SerialName("ath_date") val athDate: CoinDate? = null,
    @SerialName("ath") val athValue: CoinValue? = null,
    @SerialName("ath_change_percentage") val athChangePercentage: Map<String, Double?>? = null,
    @SerialName("max_supply") val maxSupply: Double? = null,
    @SerialName("total_supply") val totalSupply: Double? = null,
    @SerialName("circulating_supply") val circulatingSupply: Double? = null,
    @SerialName("last_updated") val lastUpdated: String? = null,
)

@Serializable
data class CoinValue(
    @SerialName("brl") val brl: Double = 0.0,
    @SerialName("usd") val usd: Double = 0.0,
)

@Serializable
data class CoinDate(
    @SerialName("brl") val brl: String = "",
    @SerialName("usd") val usd: String = "",
)

@Serializable
data class CoinDetailLinksResponse(
    @SerialName("homepage") val homepage: List<String?>? = null,
    @SerialName("whitepaper") val whitepaper: String? = null,
    @SerialName("blockchain_site") val blockchainSite: List<String?>? = null,
    @SerialName("official_forum_url") val officialForumUrl: List<String?>? = null,
    @SerialName("chat_url") val chatUrl: List<String?>? = null,
    @SerialName("announcement_url") val announcementUrl: List<String?>? = null,
    @SerialName("twitter_screen_name") val twitterScreenName: String? = null,
    @SerialName("subreddit_url") val subredditUrl: String? = null,
    @SerialName("repos_url") val reposUrl: CoinDetailReposUrlResponse? = null
)

@Serializable
data class CoinDetailReposUrlResponse(
    @SerialName("github") val github: List<String?>? = null,
    @SerialName("bitbucket") val bitbucket: List<String?>? = null
)

@Serializable
data class CoinDetailTickerResponse(
    @SerialName("base") val base: String? = null,
    @SerialName("target") val target: String? = null,
    @SerialName("market") val market: CoinDetailTickerMarketResponse? = null,
    @SerialName("last") val last: Double? = null,
    @SerialName("volume") val volume: Double? = null,
    @SerialName("converted_last") val convertedLast: Map<String, Double?>? = null,
    @SerialName("converted_volume") val convertedVolume: Map<String, Double?>? = null,
    @SerialName("trust_score") val trustScore: String? = null,
    @SerialName("bid_ask_spread_percentage") val bidAskSpreadPercentage: Double? = null,
    @SerialName("trade_url") val tradeUrl: String? = null,
    @SerialName("is_anomaly") val isAnomaly: Boolean? = false,
    @SerialName("is_stale") val isStale: Boolean? = false
)

@Serializable
data class CoinDetailTickerMarketResponse(
    @SerialName("name") val name: String? = null,
    @SerialName("identifier") val identifier: String? = null
)