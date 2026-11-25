package br.com.terras.app.coins.detail.domain.model

import br.com.terras.app.dsm.ui.component.TrendColor

data class CoinDetailVO(
    val symbol: String,
    val name: String,
    val image: String,
    val marketCapRank: String,
    val price: String,
    val priceChangePercentageList: List<CoinDetailPricePercentageVO>,
    val marketDataList: List<Pair<String, String>>,
    val trendColor: TrendColor,
    val rawPrice: Double = 0.0,
    val extraPriceChangePercentageList: List<CoinDetailPricePercentageVO> = emptyList(),
    val marketDataTrends: Map<String, TrendColor> = emptyMap(),
    val sentiment: CoinDetailSentimentVO? = null,
    val description: String = "",
    val categories: List<String> = emptyList(),
    val platforms: List<CoinDetailPlatformVO> = emptyList(),
    val links: List<CoinDetailLinkVO> = emptyList(),
    val tickers: List<CoinDetailTickerVO> = emptyList()
)

data class CoinDetailPricePercentageVO(
    val priceChangeTime: String,
    val priceChangeValue: String,
    val trendColor: TrendColor
)

data class CoinDetailSentimentVO(
    val upPercentage: Double,
    val downPercentage: Double,
    val upLabel: String,
    val downLabel: String,
    val watchlistUsersLabel: String? = null
)

data class CoinDetailPlatformVO(
    val network: String,
    val contractAddress: String,
    val shortAddress: String
)

enum class CoinDetailLinkType {
    WEBSITE,
    WHITEPAPER,
    EXPLORER,
    GITHUB,
    BITBUCKET,
    TWITTER,
    REDDIT,
    FORUM,
    CHAT,
    ANNOUNCEMENTS
}

data class CoinDetailLinkVO(
    val type: CoinDetailLinkType,
    val url: String,
    val hostLabel: String? = null
)

enum class TickerTrustLevel {
    HIGH,
    MEDIUM,
    LOW,
    UNKNOWN
}

data class CoinDetailTickerVO(
    val marketName: String,
    val pairLabel: String,
    val lastPrice: String,
    val volume24h: String,
    val spreadLabel: String? = null,
    val trustLevel: TickerTrustLevel = TickerTrustLevel.UNKNOWN,
    val tradeUrl: String = ""
)