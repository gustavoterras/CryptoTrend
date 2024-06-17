package br.com.terras.app.favorites.domain

import br.com.terras.app.common.formatMoney
import br.com.terras.app.common.formatPercentage
import br.com.terras.app.dsm.ui.component.TrendColor
import br.com.terras.app.favorites.data.model.CoinsResponse
import br.com.terras.app.favorites.domain.model.CoinVO
import javax.inject.Inject

interface FavoritesMapper {
    fun toCoins(response: List<CoinsResponse>): List<CoinVO>
}

class FavoritesMapperImpl @Inject constructor() : FavoritesMapper {
    override fun toCoins(response: List<CoinsResponse>): List<CoinVO> {
        return response.map {
            CoinVO(
                symbol = it.symbol.uppercase(),
                name = it.name,
                image = it.image,
                marketCapRank = it.marketCapRank.toString(),
                price = it.price.formatMoney(),
                priceChangePercentage = it.priceChangePercentage.formatPercentage(),
                trendColor = getTrendColor(it.priceChangePercentage)
            )
        }
    }

    private fun getTrendColor(priceChangePercentage: Double): TrendColor {
        return when {
            priceChangePercentage > 0.0 -> TrendColor.UP
            priceChangePercentage < 0.0 -> TrendColor.DOWN
            else -> TrendColor.FLAT
        }
    }
}