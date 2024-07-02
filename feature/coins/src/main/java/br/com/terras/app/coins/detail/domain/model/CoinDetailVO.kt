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
    val trendColor: TrendColor
)

data class CoinDetailPricePercentageVO(
    val priceChangeTime: String,
    val priceChangeValue: String,
    val trendColor: TrendColor
)