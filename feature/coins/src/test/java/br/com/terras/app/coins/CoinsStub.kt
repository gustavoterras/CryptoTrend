package br.com.terras.app.coins

import br.com.terras.app.coins.detail.data.model.CoinDate
import br.com.terras.app.coins.detail.data.model.CoinDetailImage
import br.com.terras.app.coins.detail.data.model.CoinDetailMarketChartResponse
import br.com.terras.app.coins.detail.data.model.CoinDetailMarketData
import br.com.terras.app.coins.detail.data.model.CoinDetailResponse
import br.com.terras.app.coins.detail.data.model.CoinValue
import br.com.terras.app.coins.detail.domain.model.CoinChartDetailVO
import br.com.terras.app.coins.detail.domain.model.CoinDetailVO
import br.com.terras.app.coins.list.data.model.CoinsResponse
import br.com.terras.app.coins.list.domain.model.CoinVO
import br.com.terras.app.dsm.ui.component.TrendColor

val mockCoinVOStub = CoinVO(
    id = "id",
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

val mockCoinDetailResponseStub = CoinDetailResponse(
    id = "id",
    symbol = "symbol",
    name = "name",
    image = CoinDetailImage("image", "image", "image"),
    marketCapRank = 1,
    marketData = CoinDetailMarketData(
        currentPrice = CoinValue(brl = 100.0, usd = 100.0),
        priceChangePercentage24h = 1.0,
        priceChangePercentage7d = 1.0,
        priceChangePercentage30d = 1.0,
        priceChangePercentage60d = 1.0,
        priceChangePercentage1y = 1.0,
        marketCap = CoinValue(brl = 100.0, usd = 100.0),
        totalVolume = CoinValue(brl = 100.0, usd = 100.0),
        low24h = CoinValue(brl = 100.0, usd = 100.0),
        high24h = CoinValue(brl = 100.0, usd = 100.0),
        atlDate = CoinDate(brl = "2024-03-14T07:10:36.635Z", usd = "2024-03-14T07:10:36.635Z"),
        atlValue = CoinValue(brl = 100.0, usd = 100.0),
        athDate = CoinDate(brl = "2024-03-14T07:10:36.635Z", usd = "2024-03-14T07:10:36.635Z"),
        athValue = CoinValue(brl = 100.0, usd = 100.0),
        maxSupply = 100.0,
        totalSupply = 100.0,
        circulatingSupply = 100.0,
        lastUpdated = "2024-03-14T07:10:36.635Z"
    )
)

val mockCoinDetailChartResponseStub = CoinDetailMarketChartResponse(
    prices = listOf(listOf(1.0, 1.0)),
    marketCaps = listOf(listOf(1.0, 1.0)),
    totalVolumes = listOf(listOf(1.0, 1.0))
)

val mockCoinDetailVOStub = CoinDetailVO(
    symbol = "SYMBOL",
    name = "name",
    image = "image",
    marketCapRank = "1",
    price = "R\$ 100,00",
    priceChangePercentageList = arrayListOf(),
    marketDataList = arrayListOf(),
    TrendColor.UP
)

val mockCoinChartDetailVOStub = CoinChartDetailVO(
    chartData = arrayListOf()
)