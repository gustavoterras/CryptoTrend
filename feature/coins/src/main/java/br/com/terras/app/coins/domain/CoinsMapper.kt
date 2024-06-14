package br.com.terras.app.coins.domain

import br.com.terras.app.common.formatMoney
import br.com.terras.app.common.formatPercentage
import br.com.terras.app.dsm.ui.component.TrendColor
import br.com.terras.app.coins.data.model.CoinsResponse
import br.com.terras.app.coins.domain.model.CoinVO
import javax.inject.Inject

interface CoinsMapper {
    fun toCoins(coinsResponse: List<CoinsResponse>): List<CoinVO>
}

class CoinsMapperImpl @Inject constructor() : CoinsMapper {
    override fun toCoins(coinsResponse: List<CoinsResponse>): List<CoinVO> {
        return coinsResponse.map { response ->
            CoinVO(
                symbol = response.symbol.uppercase(),
                name = response.name,
                image = response.image,
                marketCapRank = response.marketCapRank.toString(),
                price = response.price.formatMoney(),
                priceChangePercentage = (response.priceChangePercentage * 100).formatPercentage(),
                trendColor = getTrendColor(response.priceChangePercentage)
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