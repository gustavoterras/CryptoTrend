package br.com.terras.app.coins.list.domain

import br.com.terras.app.coins.list.data.model.CoinsResponse
import br.com.terras.app.coins.list.data.model.GlobalMarketResponse
import br.com.terras.app.coins.list.data.model.TrendingCoinsResponse
import br.com.terras.app.coins.list.domain.model.CoinVO
import br.com.terras.app.coins.list.domain.model.FearAndGreedSentiment
import br.com.terras.app.coins.list.domain.model.GlobalMarketVO
import br.com.terras.app.common.downsampleSparkline
import br.com.terras.app.common.formatCompactCount
import br.com.terras.app.common.formatCompactMoney
import br.com.terras.app.common.formatDominancePercentage
import br.com.terras.app.common.formatMoney
import br.com.terras.app.common.formatPercentage
import br.com.terras.app.common.formatShortMoney
import br.com.terras.app.dsm.ui.component.TrendColor
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt

interface CoinsMapper {
    fun toCoins(coinsResponse: List<CoinsResponse>): List<CoinVO>
    fun toTrendingCoins(
        trendingResponse: TrendingCoinsResponse,
        marketCoins: List<CoinsResponse> = emptyList()
    ): List<CoinVO>
    fun toGlobalMarket(response: GlobalMarketResponse): GlobalMarketVO
}

class CoinsMapperImpl @Inject constructor() : CoinsMapper {
    override fun toCoins(coinsResponse: List<CoinsResponse>): List<CoinVO> {
        return coinsResponse.take(20).map { response ->
            mapMarketCoin(response)
        }
    }

    override fun toTrendingCoins(
        trendingResponse: TrendingCoinsResponse,
        marketCoins: List<CoinsResponse>
    ): List<CoinVO> {
        val marketById = marketCoins.associateBy { it.id }
        val currencyKey = if (Locale.getDefault().language == "pt") "brl" else "usd"

        return trendingResponse.coins
            .asSequence()
            .map { it.item }
            .filter { it.id.isNotBlank() }
            .take(20)
            .map { item ->
                val enriched = marketById[item.id]
                if (enriched != null) {
                    mapMarketCoin(enriched)
                } else {
                    val rawPrice = item.data?.price ?: 0.0
                    val rawVolume = parseVolumeString(item.data?.totalVolume)
                    val rawChange = item.data?.priceChangePercentage24h?.get(currencyKey)
                        ?: item.data?.priceChangePercentage24h?.get("usd")
                        ?: 0.0
                    val hasPrice = item.data?.price != null
                    val hasChange = item.data?.priceChangePercentage24h?.isNotEmpty() == true
                    val formattedVolume = when {
                        rawVolume > 0.0 -> rawVolume.formatShortMoney(Locale.US)
                        !item.data?.totalVolume.isNullOrBlank() -> item.data?.totalVolume.orEmpty()
                        else -> ""
                    }

                    CoinVO(
                        id = item.id,
                        symbol = item.symbol.uppercase(),
                        name = item.name,
                        image = item.small.ifBlank { item.large.ifBlank { item.thumb } },
                        marketCapRank = item.marketCapRank?.toString().orEmpty(),
                        price = if (hasPrice) rawPrice.formatMoney(Locale.US) else "",
                        priceChangePercentage = if (hasChange) rawChange.formatPercentage() else "",
                        trendColor = if (hasChange) getTrendColor(rawChange) else TrendColor.NONE,
                        volume = formattedVolume,
                        rawVolume = rawVolume,
                        rawPrice = rawPrice,
                        rawPriceChangePercentage = rawChange,
                        sparklinePrices = emptyList()
                    )
                }
            }
            .toList()
    }

    override fun toGlobalMarket(response: GlobalMarketResponse): GlobalMarketVO {
        val data = response.data
        val currentLocale = Locale.getDefault()
        val isEnglish = currentLocale.language == "en"
        val preferredCurrency = if (isEnglish) "usd" else "brl"
        val brLocale = Locale("pt", "BR")

        val marketCapEntry = resolveCurrencyAmount(
            values = data.totalMarketCap,
            preferredCurrency = preferredCurrency,
            preferredLocale = if (isEnglish) Locale.US else brLocale
        )
        val volumeEntry = resolveCurrencyAmount(
            values = data.totalVolume,
            preferredCurrency = preferredCurrency,
            preferredLocale = if (isEnglish) Locale.US else brLocale
        )

        val btcDominanceValue = (data.marketCapPercentage["btc"] ?: 0.0).coerceIn(0.0, 100.0)
        val hasBtcDom = btcDominanceValue > 0.0
        val btcDomScore = if (hasBtcDom) btcDominanceValue.roundToInt().coerceIn(0, 100) else 0
        val altDomScore = if (hasBtcDom) (100 - btcDomScore).coerceIn(0, 100) else 0
        val ethDominanceValue = data.marketCapPercentage["eth"] ?: 0.0
        val changePct = data.marketCapChangePercentage24hUsd
        val rawFngValue = data.fearAndGreedIndex?.value?.coerceIn(0, 100)
        val sentiment = resolveSentiment(
            rawSentiment = data.fearAndGreedIndex?.sentiment,
            value = rawFngValue ?: 50
        )

        return GlobalMarketVO(
            fearAndGreedValue = rawFngValue ?: 0,
            fearAndGreedSentiment = sentiment,
            fearAndGreedLabel = localizedSentimentLabel(sentiment, isEnglish),
            hasFearAndGreed = rawFngValue != null,
            totalMarketCap = if (marketCapEntry.first > 0.0) {
                marketCapEntry.first.formatCompactMoney(marketCapEntry.second)
            } else {
                "--"
            },
            marketCapChangePercentage = changePct.formatPercentage(currentLocale),
            trendColor = getTrendColor(changePct),
            totalVolume24h = if (volumeEntry.first > 0.0) {
                volumeEntry.first.formatCompactMoney(volumeEntry.second)
            } else {
                "--"
            },
            btcDominance = if (hasBtcDom) {
                btcDominanceValue.formatDominancePercentage(currentLocale)
            } else {
                "--"
            },
            ethDominance = if (ethDominanceValue > 0.0) {
                ethDominanceValue.formatDominancePercentage(currentLocale)
            } else {
                "--"
            },
            activeCryptocurrencies = if (data.activeCryptocurrencies > 0) {
                data.activeCryptocurrencies.formatCompactCount(currentLocale)
            } else {
                "--"
            },
            marketsCount = if (data.markets > 0) {
                data.markets.formatCompactCount(currentLocale)
            } else {
                "--"
            },
            btcDominancePercent = btcDominanceValue,
            btcDominanceScore = btcDomScore,
            altcoinsDominanceScore = altDomScore,
            hasBtcDominance = hasBtcDom
        )
    }

    private fun resolveSentiment(rawSentiment: String?, value: Int): FearAndGreedSentiment {
        return when (rawSentiment?.trim()?.lowercase()?.replace(" ", "_")) {
            "extreme_fear" -> FearAndGreedSentiment.EXTREME_FEAR
            "fear" -> FearAndGreedSentiment.FEAR
            "neutral" -> FearAndGreedSentiment.NEUTRAL
            "greed" -> FearAndGreedSentiment.GREED
            "extreme_greed" -> FearAndGreedSentiment.EXTREME_GREED
            else -> when {
                value <= 30 -> FearAndGreedSentiment.EXTREME_FEAR
                value <= 45 -> FearAndGreedSentiment.FEAR
                value <= 55 -> FearAndGreedSentiment.NEUTRAL
                value <= 70 -> FearAndGreedSentiment.GREED
                else -> FearAndGreedSentiment.EXTREME_GREED
            }
        }
    }

    private fun localizedSentimentLabel(
        sentiment: FearAndGreedSentiment,
        isEnglish: Boolean
    ): String = if (isEnglish) {
        when (sentiment) {
            FearAndGreedSentiment.EXTREME_FEAR -> "Extreme Fear"
            FearAndGreedSentiment.FEAR -> "Fear"
            FearAndGreedSentiment.NEUTRAL -> "Neutral"
            FearAndGreedSentiment.GREED -> "Greed"
            FearAndGreedSentiment.EXTREME_GREED -> "Extreme Greed"
        }
    } else {
        when (sentiment) {
            FearAndGreedSentiment.EXTREME_FEAR -> "Medo Extremo"
            FearAndGreedSentiment.FEAR -> "Medo"
            FearAndGreedSentiment.NEUTRAL -> "Neutro"
            FearAndGreedSentiment.GREED -> "Ganância"
            FearAndGreedSentiment.EXTREME_GREED -> "Ganância Extrema"
        }
    }

    private fun resolveCurrencyAmount(
        values: Map<String, Double>,
        preferredCurrency: String,
        preferredLocale: Locale
    ): Pair<Double, Locale> {
        val preferred = values[preferredCurrency]
        if (preferred != null && preferred > 0.0) {
            return preferred to preferredLocale
        }
        val usdFallback = values["usd"]
        if (usdFallback != null && usdFallback > 0.0) {
            return usdFallback to Locale.US
        }
        return 0.0 to preferredLocale
    }

    private fun mapMarketCoin(response: CoinsResponse): CoinVO {
        val rawVolume = response.totalVolume ?: 0.0
        val formattedVolume = rawVolume.takeIf { it > 0.0 }?.formatShortMoney().orEmpty()
        return CoinVO(
            id = response.id,
            symbol = response.symbol.uppercase(),
            name = response.name,
            image = response.image,
            marketCapRank = if (response.marketCapRank > 0) response.marketCapRank.toString() else "",
            price = response.price.formatMoney(),
            priceChangePercentage = response.priceChangePercentage.formatPercentage(),
            trendColor = getTrendColor(response.priceChangePercentage),
            volume = formattedVolume,
            rawVolume = rawVolume,
            rawPrice = response.price,
            rawPriceChangePercentage = response.priceChangePercentage,
            sparklinePrices = response.sparklineIn7d?.price.orEmpty().downsampleSparkline()
        )
    }

    private fun parseVolumeString(raw: String?): Double {
        if (raw.isNullOrBlank()) return 0.0
        val cleaned = raw.replace(Regex("[^0-9.]"), "")
        return cleaned.toDoubleOrNull() ?: 0.0
    }

    private fun getTrendColor(priceChangePercentage: Double): TrendColor {
        return when {
            priceChangePercentage > 0.0 -> TrendColor.UP
            priceChangePercentage < 0.0 -> TrendColor.DOWN
            else -> TrendColor.FLAT
        }
    }
}