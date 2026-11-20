package br.com.terras.app.coins.list.domain

import br.com.terras.app.coins.list.data.model.CoinsResponse
import br.com.terras.app.coins.list.data.model.TrendingCoinsResponse
import br.com.terras.app.coins.list.domain.model.CoinVO
import br.com.terras.app.common.downsampleSparkline
import br.com.terras.app.common.formatAxisMoney
import br.com.terras.app.common.formatMoney
import br.com.terras.app.common.formatPercentage
import br.com.terras.app.dsm.ui.component.TrendColor
import java.util.Locale
import javax.inject.Inject

interface CoinsMapper {
    fun toCoins(coinsResponse: List<CoinsResponse>): List<CoinVO>
    fun toTrendingCoins(
        trendingResponse: TrendingCoinsResponse,
        marketCoins: List<CoinsResponse> = emptyList()
    ): List<CoinVO>
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
                        rawVolume > 0.0 -> rawVolume.formatAxisMoney(Locale.US)
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

    private fun mapMarketCoin(response: CoinsResponse): CoinVO {
        val rawVolume = response.totalVolume ?: 0.0
        val formattedVolume = rawVolume.takeIf { it > 0.0 }?.formatAxisMoney().orEmpty()
        return CoinVO(
            id = response.id,
            symbol = response.symbol.uppercase(),
            name = response.name,
            image = response.image,
            marketCapRank = response.marketCapRank.toString(),
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