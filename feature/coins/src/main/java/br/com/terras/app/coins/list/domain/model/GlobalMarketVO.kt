package br.com.terras.app.coins.list.domain.model

import br.com.terras.app.dsm.ui.component.TrendColor

enum class FearAndGreedSentiment {
    EXTREME_FEAR,
    FEAR,
    NEUTRAL,
    GREED,
    EXTREME_GREED
}

data class GlobalMarketVO(
    val fearAndGreedValue: Int,
    val fearAndGreedSentiment: FearAndGreedSentiment,
    val fearAndGreedLabel: String,
    val hasFearAndGreed: Boolean,
    val totalMarketCap: String,
    val marketCapChangePercentage: String,
    val trendColor: TrendColor,
    val totalVolume24h: String,
    val btcDominance: String,
    val ethDominance: String,
    val activeCryptocurrencies: String,
    val marketsCount: String,
    val btcDominancePercent: Double = 0.0,
    val btcDominanceScore: Int = 0,
    val altcoinsDominanceScore: Int = 0,
    val hasBtcDominance: Boolean = false
)
