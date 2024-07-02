package br.com.terras.app.coins.list.domain.model

import br.com.terras.app.dsm.ui.component.TrendColor

data class CoinVO(
    val id: String,
    val symbol: String,
    val name: String,
    val image: String,
    val marketCapRank: String,
    val price: String,
    val priceChangePercentage: String,
    val trendColor: TrendColor
)