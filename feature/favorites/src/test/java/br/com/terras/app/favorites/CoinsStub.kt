package br.com.terras.app.favorites

import br.com.terras.app.dsm.ui.component.TrendColor
import br.com.terras.app.favorites.data.model.CoinsResponse
import br.com.terras.app.favorites.domain.model.CoinVO

val mockCoinVOStub = CoinVO(
    symbol = "SYMBOL",
    name = "name",
    image = "image",
    marketCapRank = "1",
    price = "R\$ 100,00",
    priceChangePercentage = "+1.00%",
    TrendColor.UP
)

val mockCoinsResponseStub = CoinsResponse(
    id = "id",
    symbol = "symbol",
    name = "name",
    image = "image",
    marketCapRank = 1,
    price = 100.0,
    priceChangePercentage = 1.0
)

val mockCoinsListResponseStub = listOf(mockCoinsResponseStub)

val mockCoinsVOListStub = listOf(mockCoinVOStub)