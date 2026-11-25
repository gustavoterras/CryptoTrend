package br.com.terras.app.coins

import br.com.terras.app.coins.detail.data.model.CoinDate
import br.com.terras.app.coins.detail.data.model.CoinDetailImage
import br.com.terras.app.coins.detail.data.model.CoinDetailMarketChartResponse
import br.com.terras.app.coins.detail.data.model.CoinDetailMarketData
import br.com.terras.app.coins.detail.data.model.CoinDetailResponse
import br.com.terras.app.coins.detail.data.model.CoinInsightResponse
import br.com.terras.app.coins.detail.data.model.CoinValue
import br.com.terras.app.coins.detail.domain.model.CoinChartDetailVO
import br.com.terras.app.coins.detail.domain.model.CoinDetailVO
import br.com.terras.app.coins.detail.domain.model.CoinInsightVO
import br.com.terras.app.coins.list.data.model.CoinsResponse
import br.com.terras.app.coins.list.data.model.FearAndGreedIndexResponse
import br.com.terras.app.coins.list.data.model.GlobalMarketDataResponse
import br.com.terras.app.coins.list.data.model.GlobalMarketResponse
import br.com.terras.app.coins.list.data.model.TrendingCoinData
import br.com.terras.app.coins.list.data.model.TrendingCoinItem
import br.com.terras.app.coins.list.data.model.TrendingCoinWrapper
import br.com.terras.app.coins.list.data.model.TrendingCoinsResponse
import br.com.terras.app.coins.list.domain.model.CoinVO
import br.com.terras.app.coins.list.domain.model.FearAndGreedSentiment
import br.com.terras.app.coins.list.domain.model.GlobalMarketVO
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

val mockTrendingCoinDataStub = TrendingCoinData(
    price = 100.0,
    priceChangePercentage24h = mapOf("usd" to 1.0, "brl" to 1.0),
    totalVolume = "$1,250,000",
    sparkline = "https://www.coingecko.com/coins/1/sparkline.svg"
)

val mockTrendingCoinItemStub = TrendingCoinItem(
    id = "id",
    symbol = "symbol",
    name = "name",
    marketCapRank = 1,
    thumb = "thumb",
    small = "image",
    large = "large",
    score = 0,
    data = mockTrendingCoinDataStub
)

val mockTrendingCoinsResponseStub = TrendingCoinsResponse(
    coins = listOf(TrendingCoinWrapper(item = mockTrendingCoinItemStub))
)

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

val mockCoinInsightResponseStub = CoinInsightResponse(
    title = "Institutional demand rises",
    description = "ETF net inflows accelerated over the past 24 hours.",
    relatedCoinIds = listOf("bitcoin"),
    postedAt = "2026-06-15T03:56:19.514666Z"
)

val mockCoinInsightVOStub = CoinInsightVO(
    title = "Institutional demand rises",
    description = "ETF net inflows accelerated over the past 24 hours.",
    relatedCoinIds = listOf("bitcoin"),
    postedAt = "Jun 15, 2026"
)

val mockGlobalMarketResponseStub = GlobalMarketResponse(
    data = GlobalMarketDataResponse(
        activeCryptocurrencies = 15420,
        markets = 1120,
        totalMarketCap = mapOf("brl" to 16_420_000_000_000.0, "usd" to 3_210_000_000_000.0),
        totalVolume = mapOf("brl" to 612_800_000_000.0, "usd" to 120_000_000_000.0),
        marketCapPercentage = mapOf("btc" to 54.2, "eth" to 16.5),
        marketCapChangePercentage24hUsd = 2.4,
        updatedAt = 1_780_000_000L,
        fearAndGreedIndex = FearAndGreedIndexResponse(value = 44, sentiment = "fear")
    )
)

val mockGlobalMarketVOStub = GlobalMarketVO(
    fearAndGreedValue = 44,
    fearAndGreedSentiment = FearAndGreedSentiment.FEAR,
    fearAndGreedLabel = "Medo",
    hasFearAndGreed = true,
    totalMarketCap = "R$ 16,42 tri",
    marketCapChangePercentage = "+2,4%",
    trendColor = TrendColor.UP,
    totalVolume24h = "R$ 612,80 bi",
    btcDominance = "54,2%",
    ethDominance = "16,5%",
    activeCryptocurrencies = "15.420",
    marketsCount = "1.120",
    btcDominancePercent = 54.2,
    btcDominanceScore = 54,
    altcoinsDominanceScore = 46,
    hasBtcDominance = true
)