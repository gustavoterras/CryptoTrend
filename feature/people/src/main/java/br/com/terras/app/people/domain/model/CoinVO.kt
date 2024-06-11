package br.com.terras.app.people.domain.model

import br.com.terras.app.dsm.ui.component.TrendColor

data class CoinVO(
    val symbol: String,
    val name: String,
    val image: String,
    val marketCapRank: String,
    val price: String,
    val priceChangePercentage: String,
    val trendColor: TrendColor
)