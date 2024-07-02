package br.com.terras.app.coins.detail.domain

import br.com.terras.app.coins.detail.data.model.CoinDate
import br.com.terras.app.coins.detail.data.model.CoinDetailMarketChartResponse
import br.com.terras.app.coins.detail.data.model.CoinDetailResponse
import br.com.terras.app.coins.detail.data.model.CoinValue
import br.com.terras.app.coins.detail.domain.model.CoinChartDetailVO
import br.com.terras.app.coins.detail.domain.model.CoinDetailPricePercentageVO
import br.com.terras.app.coins.detail.domain.model.CoinDetailVO
import br.com.terras.app.coins.detail.domain.model.DataPoint
import br.com.terras.app.coins.detail.domain.model.MarketChartDataPoint
import br.com.terras.app.common.formatMoney
import br.com.terras.app.common.formatPercentage
import br.com.terras.app.common.toFormatDate
import br.com.terras.app.dsm.ui.component.TrendColor
import java.util.Locale
import javax.inject.Inject

interface DetailMapper {
    fun toCoin(response: CoinDetailResponse): CoinDetailVO
    fun toChart(response: CoinDetailMarketChartResponse): CoinChartDetailVO
}

class DetailMapperImpl @Inject constructor() : DetailMapper {

    private fun getTrendColor(priceChangePercentage: Double): TrendColor {
        return when {
            priceChangePercentage > 0.0 -> TrendColor.UP
            priceChangePercentage < 0.0 -> TrendColor.DOWN
            else -> TrendColor.FLAT
        }
    }

    override fun toCoin(response: CoinDetailResponse): CoinDetailVO {
        return CoinDetailVO(
            symbol = response.symbol.uppercase(),
            name = response.name,
            image = response.image.small,
            marketCapRank = response.marketCapRank.toString(),
            price = response.marketData.currentPrice.getCurrentPriceByLocale(),
            priceChangePercentageList = arrayListOf(
                getPriceChangePercentage("24H", response.marketData.priceChangePercentage24h),
                getPriceChangePercentage("7D", response.marketData.priceChangePercentage7d),
                getPriceChangePercentage("30D", response.marketData.priceChangePercentage30d),
                getPriceChangePercentage("60D", response.marketData.priceChangePercentage60d),
                getPriceChangePercentage("1Y", response.marketData.priceChangePercentage1y),
            ),
            marketDataList = listOf(
                Pair("Market Cap", response.marketData.marketCap?.getCurrentPriceByLocale().orEmpty()),
                Pair("Trading Volume 24h", response.marketData.totalVolume?.getCurrentPriceByLocale().orEmpty()),
                Pair("Highest Price 24h", response.marketData.high24h?.getCurrentPriceByLocale().orEmpty()),
                Pair("Lowest Price 24h", response.marketData.low24h?.getCurrentPriceByLocale().orEmpty()),
                Pair("Available Supply", response.marketData.circulatingSupply?.formatMoney().orEmpty()),
                Pair("Total Supply", response.marketData.totalSupply?.formatMoney().orEmpty()),
                Pair("Max Supply", response.marketData.maxSupply?.formatMoney().orEmpty()),
                Pair("All-Time High Price", response.marketData.athValue?.getCurrentPriceByLocale().orEmpty()),
                Pair("All-Time Low Price", response.marketData.atlValue?.getCurrentPriceByLocale().orEmpty()),
                Pair("All-Time High Date", response.marketData.athDate?.getCurrentDateByLocale().orEmpty()),
                Pair("All-Time Low Date", response.marketData.atlDate?.getCurrentDateByLocale().orEmpty()),
                Pair("Last updated", response.marketData.lastUpdated?.toFormatDate().orEmpty())
            ),
            trendColor = getTrendColor(response.marketData.priceChangePercentage24h)
        )
    }

    override fun toChart(response: CoinDetailMarketChartResponse): CoinChartDetailVO {
        return CoinChartDetailVO(mapMarketChartDataPointList(response).mapIndexed { _, marketChartDataPoint ->
            DataPoint(
                date = marketChartDataPoint.date,
                y = marketChartDataPoint.price,
                xLabel = null,
                yLabel = marketChartDataPoint.price.formatMoney()
            )
        }.toList())
    }

    private fun mapMarketChartDataPointList(marketChartDto: CoinDetailMarketChartResponse): List<MarketChartDataPoint> {
        val divAmount = when (marketChartDto.prices.size) {
            in 0..200 -> 1
            in 200..400 -> 2
            in 400..800 -> 6
            in 800..1600 -> 10
            else -> 12
        }

        return marketChartDto.prices.filterIndexed { index, _ ->
            index % divAmount == 0
        }.map {
            mapMarketChartDataPoint(it)
        }
    }

    private fun mapMarketChartDataPoint(dataPointList: List<Double>): MarketChartDataPoint {
        val date = dataPointList.first().toLong().toFormatDate()
        val price = dataPointList[1]

        return MarketChartDataPoint(
            date = date,
            price = price
        )
    }

    private fun CoinValue.getCurrentPriceByLocale(): String {
        val value = if (Locale.getDefault().language == "pt") brl else usd
        return value.formatMoney()
    }

    private fun CoinDate.getCurrentDateByLocale(): String {
        val date = if (Locale.getDefault().language == "pt") brl else usd
        return date.toFormatDate()
    }

    private fun getPriceChangePercentage(
        name: String,
        percentage: Double
    ): CoinDetailPricePercentageVO {
        return CoinDetailPricePercentageVO(
            priceChangeTime = name,
            priceChangeValue = percentage.formatPercentage(),
            trendColor = getTrendColor(percentage)
        )
    }
}
