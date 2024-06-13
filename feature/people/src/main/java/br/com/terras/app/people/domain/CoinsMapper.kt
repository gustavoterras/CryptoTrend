package br.com.terras.app.people.domain

import br.com.terras.app.common.formatMoney
import br.com.terras.app.common.formatPercentage
import br.com.terras.app.dsm.ui.component.TrendColor
import br.com.terras.app.people.data.model.CoinsListResponse
import br.com.terras.app.people.domain.model.CoinVO
import javax.inject.Inject

interface CoinsMapper {
    fun toCoinsList(coinsListResponse: List<CoinsListResponse>): List<CoinVO>
}

class CoinsMapperImpl @Inject constructor() : CoinsMapper {
    override fun toCoinsList(coinsListResponse: List<CoinsListResponse>): List<CoinVO> {
        return coinsListResponse.map { response ->
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