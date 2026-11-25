package br.com.terras.app.coins.detail.domain

import br.com.terras.app.coins.detail.data.model.CoinDate
import br.com.terras.app.coins.detail.data.model.CoinDetailLinksResponse
import br.com.terras.app.coins.detail.data.model.CoinDetailMarketChartResponse
import br.com.terras.app.coins.detail.data.model.CoinDetailResponse
import br.com.terras.app.coins.detail.data.model.CoinDetailTickerResponse
import br.com.terras.app.coins.detail.data.model.CoinInsightResponse
import br.com.terras.app.coins.detail.data.model.CoinValue
import br.com.terras.app.coins.detail.domain.model.CoinChartDetailVO
import br.com.terras.app.coins.detail.domain.model.CoinDetailLinkType
import br.com.terras.app.coins.detail.domain.model.CoinDetailLinkVO
import br.com.terras.app.coins.detail.domain.model.CoinDetailPlatformVO
import br.com.terras.app.coins.detail.domain.model.CoinDetailPricePercentageVO
import br.com.terras.app.coins.detail.domain.model.CoinDetailSentimentVO
import br.com.terras.app.coins.detail.domain.model.CoinDetailTickerVO
import br.com.terras.app.coins.detail.domain.model.CoinDetailVO
import br.com.terras.app.coins.detail.domain.model.CoinInsightVO
import br.com.terras.app.coins.detail.domain.model.DataPoint
import br.com.terras.app.coins.detail.domain.model.MarketChartDataPoint
import br.com.terras.app.coins.detail.domain.model.TickerTrustLevel
import br.com.terras.app.common.formatAxisMoney
import br.com.terras.app.common.formatCompactCount
import br.com.terras.app.common.formatCompactMoney
import br.com.terras.app.common.formatDominancePercentage
import br.com.terras.app.common.formatMoney
import br.com.terras.app.common.formatPercentage
import br.com.terras.app.common.toChartAxisTime
import br.com.terras.app.common.toChartDay
import br.com.terras.app.common.toChartHour
import br.com.terras.app.common.toFormatDate
import br.com.terras.app.dsm.ui.component.TrendColor
import java.net.URI
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt

interface DetailMapper {
    fun toCoin(response: CoinDetailResponse): CoinDetailVO
    fun toChart(response: CoinDetailMarketChartResponse): CoinChartDetailVO
    fun toInsights(response: List<CoinInsightResponse>): List<CoinInsightVO>
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
        val marketData = response.marketData
        val currentLocale = Locale.getDefault()
        val fdvFormatted = marketData.fullyDilutedValuation.formatCurrencyAmount(currentLocale)
        val tvlFormatted = marketData.totalValueLocked.formatCurrencyAmount(currentLocale)
        val athDistanceRaw = marketData.athChangePercentage.resolveValueByLocale(currentLocale)
        val atlDistanceRaw = marketData.atlChangePercentage.resolveValueByLocale(currentLocale)

        val marketDataTrends = buildMap {
            athDistanceRaw?.let { put("Distance from ATH", getTrendColor(it)) }
            atlDistanceRaw?.let { put("Distance from ATL", getTrendColor(it)) }
        }

        val marketDataList = buildList {
            marketData.marketCap?.getCurrentPriceByLocale()?.let {
                add("Market Cap" to it)
            }
            fdvFormatted?.let {
                add("FDV" to it)
            }
            tvlFormatted?.let {
                add("TVL" to it)
            }
            marketData.totalVolume?.getCurrentPriceByLocale()?.let {
                add("Trading Volume 24h" to it)
            }
            marketData.high24h?.getCurrentPriceByLocale()?.let {
                add("Highest Price 24h" to it)
            }
            marketData.low24h?.getCurrentPriceByLocale()?.let {
                add("Lowest Price 24h" to it)
            }
            marketData.circulatingSupply?.takeIf { it > 0.0 }?.let {
                add("Available Supply" to it.formatMoney())
            }
            marketData.totalSupply?.takeIf { it > 0.0 }?.let {
                add("Total Supply" to it.formatMoney())
            }
            marketData.maxSupply?.takeIf { it > 0.0 }?.let {
                add("Max Supply" to it.formatMoney())
            }
            marketData.athValue?.getCurrentPriceByLocale()?.let {
                add("All-Time High Price" to it)
            }
            athDistanceRaw?.let {
                add("Distance from ATH" to it.formatPercentage(currentLocale))
            }
            marketData.athDate?.getCurrentDateByLocale()?.takeIf { it.isNotBlank() }?.let {
                add("All-Time High Date" to it)
            }
            marketData.atlValue?.getCurrentPriceByLocale()?.let {
                add("All-Time Low Price" to it)
            }
            atlDistanceRaw?.let {
                add("Distance from ATL" to it.formatPercentage(currentLocale))
            }
            marketData.atlDate?.getCurrentDateByLocale()?.takeIf { it.isNotBlank() }?.let {
                add("All-Time Low Date" to it)
            }
            response.genesisDate?.formatGenesisDate(currentLocale)?.let {
                add("Genesis Date" to it)
            }
            response.hashingAlgorithm?.trim()?.takeIf { it.isNotEmpty() }?.let {
                add("Hashing Algorithm" to it)
            }
            response.blockTimeInMinutes?.formatBlockTime(currentLocale)?.let {
                add("Block Time" to it)
            }
            response.watchlistPortfolioUsers?.takeIf { it > 0 }?.let {
                add("Watchlist Users" to it.formatCompactCount(currentLocale))
            }
            marketData.lastUpdated?.takeIf { it.isNotBlank() }?.let { rawDate ->
                runCatching { rawDate.toFormatDate() }.getOrNull()?.let {
                    add("Last updated" to it)
                }
            }
        }

        return CoinDetailVO(
            symbol = response.symbol.uppercase(),
            name = response.name,
            image = response.image.small,
            marketCapRank = response.marketCapRank?.toString().orEmpty(),
            price = marketData.currentPrice.getCurrentPriceByLocale(),
            priceChangePercentageList = listOf(
                getPriceChangePercentage("1D", marketData.priceChangePercentage24h),
                getPriceChangePercentage("7D", marketData.priceChangePercentage7d),
                getPriceChangePercentage("14D", marketData.priceChangePercentage14d ?: 0.0),
                getPriceChangePercentage("30D", marketData.priceChangePercentage30d)
            ),
            extraPriceChangePercentageList = listOf(
                getPriceChangePercentage("60D", marketData.priceChangePercentage60d),
                getPriceChangePercentage("200D", marketData.priceChangePercentage200d ?: 0.0),
                getPriceChangePercentage("1Y", marketData.priceChangePercentage1y)
            ),
            marketDataList = marketDataList,
            marketDataTrends = marketDataTrends,
            trendColor = getTrendColor(marketData.priceChangePercentage24h),
            rawPrice = marketData.currentPrice.getRawPriceByLocale(),
            sentiment = mapSentiment(response, currentLocale),
            description = mapDescription(response.description, currentLocale),
            categories = response.categories
                ?.mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }
                ?.distinct()
                .orEmpty(),
            platforms = mapPlatforms(response.platforms),
            links = mapLinks(response.links),
            tickers = mapTickers(response.tickers, currentLocale)
        )
    }

    override fun toChart(response: CoinDetailMarketChartResponse): CoinChartDetailVO {
        return CoinChartDetailVO(mapMarketChartDataPointList(response).map { marketChartDataPoint ->
            DataPoint(
                date = marketChartDataPoint.date,
                y = marketChartDataPoint.price,
                xLabel = marketChartDataPoint.axisTime,
                yLabel = marketChartDataPoint.price.formatMoney(),
                hourLabel = marketChartDataPoint.hourLabel,
                dayLabel = marketChartDataPoint.dayLabel,
                timestampMillis = marketChartDataPoint.timestampMillis
            )
        })
    }

    override fun toInsights(response: List<CoinInsightResponse>): List<CoinInsightVO> {
        return response
            .filter { it.title.isNotBlank() || it.description.isNotBlank() }
            .map { insight ->
                val formattedDate = insight.postedAt
                    .takeIf { it.isNotBlank() }
                    ?.let { rawDate -> runCatching { rawDate.toFormatDate() }.getOrDefault("") }
                    .orEmpty()
                CoinInsightVO(
                    title = insight.title,
                    description = insight.description,
                    relatedCoinIds = insight.relatedCoinIds,
                    postedAt = formattedDate
                )
            }
    }

    private fun mapSentiment(
        response: CoinDetailResponse,
        locale: Locale
    ): CoinDetailSentimentVO? {
        val rawUp = response.sentimentVotesUpPercentage
        val rawDown = response.sentimentVotesDownPercentage
        if (rawUp == null && rawDown == null) return null

        val up = (rawUp ?: (100.0 - (rawDown ?: 50.0))).coerceIn(0.0, 100.0)
        val down = (rawDown ?: (100.0 - up)).coerceIn(0.0, 100.0)
        if (up <= 0.0 && down <= 0.0) return null

        return CoinDetailSentimentVO(
            upPercentage = up,
            downPercentage = down,
            upLabel = up.formatDominancePercentage(locale),
            downLabel = down.formatDominancePercentage(locale),
            watchlistUsersLabel = response.watchlistPortfolioUsers
                ?.takeIf { it > 0 }
                ?.formatCompactCount(locale)
        )
    }

    private fun mapDescription(
        descriptions: Map<String, String?>?,
        locale: Locale
    ): String {
        if (descriptions.isNullOrEmpty()) return ""
        val preferredLang = if (locale.language.equals("pt", ignoreCase = true)) "pt" else "en"
        val rawText = descriptions[preferredLang]?.takeIf { !it.isNullOrBlank() }
            ?: descriptions["en"]?.takeIf { !it.isNullOrBlank() }
            ?: descriptions.values.firstOrNull { !it.isNullOrBlank() }
            ?: return ""

        return sanitizeHtmlDescription(rawText)
    }

    private fun sanitizeHtmlDescription(raw: String): String {
        return raw
            .replace("\r\n", "\n")
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("</p>", RegexOption.IGNORE_CASE), "\n\n")
            .replace(Regex("<[^>]*>"), "")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }

    private fun mapPlatforms(platforms: Map<String, String?>?): List<CoinDetailPlatformVO> {
        if (platforms.isNullOrEmpty()) return emptyList()
        return platforms.entries.mapNotNull { (rawNetwork, rawAddress) ->
            val network = rawNetwork.trim()
            val address = rawAddress?.trim().orEmpty()
            if (network.isEmpty() || address.isEmpty()) return@mapNotNull null

            CoinDetailPlatformVO(
                network = formatNetworkName(network),
                contractAddress = address,
                shortAddress = shortenAddress(address)
            )
        }
    }

    private fun formatNetworkName(rawNetwork: String): String {
        return rawNetwork
            .split('-', '_')
            .filter { it.isNotBlank() }
            .joinToString(" ") { part ->
                part.replaceFirstChar { ch ->
                    if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
                }
            }
    }

    private fun shortenAddress(address: String): String {
        if (address.length <= 14) return address
        return "${address.take(6)}...${address.takeLast(6)}"
    }

    private fun mapLinks(links: CoinDetailLinksResponse?): List<CoinDetailLinkVO> {
        if (links == null) return emptyList()
        val seenUrls = mutableSetOf<String>()
        val result = mutableListOf<CoinDetailLinkVO>()

        fun addLink(type: CoinDetailLinkType, rawUrl: String?, includeHost: Boolean = false) {
            val sanitized = sanitizeHttpUrl(rawUrl) ?: return
            val dedupKey = sanitized.lowercase(Locale.US).trimEnd('/')
            if (!seenUrls.add(dedupKey)) return
            val host = if (includeHost) extractHostLabel(sanitized) else null
            result.add(CoinDetailLinkVO(type = type, url = sanitized, hostLabel = host))
        }

        links.homepage?.forEach { addLink(CoinDetailLinkType.WEBSITE, it) }
        addLink(CoinDetailLinkType.WHITEPAPER, links.whitepaper)
        links.blockchainSite?.take(3)?.forEach { addLink(CoinDetailLinkType.EXPLORER, it, includeHost = true) }
        links.reposUrl?.github?.take(2)?.forEach { addLink(CoinDetailLinkType.GITHUB, it) }
        links.reposUrl?.bitbucket?.take(1)?.forEach { addLink(CoinDetailLinkType.BITBUCKET, it) }
        links.twitterScreenName?.trim()?.trimStart('@')?.takeIf { it.isNotEmpty() }?.let { handle ->
            addLink(CoinDetailLinkType.TWITTER, "https://x.com/$handle")
        }
        addLink(CoinDetailLinkType.REDDIT, links.subredditUrl)
        links.officialForumUrl?.take(2)?.forEach { addLink(CoinDetailLinkType.FORUM, it, includeHost = true) }
        links.chatUrl?.take(2)?.forEach { addLink(CoinDetailLinkType.CHAT, it, includeHost = true) }
        links.announcementUrl?.take(2)?.forEach { addLink(CoinDetailLinkType.ANNOUNCEMENTS, it, includeHost = true) }

        return result
    }

    private fun sanitizeHttpUrl(rawUrl: String?): String? {
        val trimmed = rawUrl?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        return if (trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("http://", ignoreCase = true)
        ) {
            trimmed
        } else {
            null
        }
    }

    private fun extractHostLabel(url: String): String? {
        return runCatching {
            URI(url).host?.removePrefix("www.")?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    private fun mapTickers(
        tickers: List<CoinDetailTickerResponse>?,
        locale: Locale
    ): List<CoinDetailTickerVO> {
        if (tickers.isNullOrEmpty()) return emptyList()
        val isPortuguese = locale.language.equals("pt", ignoreCase = true)
        val preferredKey = if (isPortuguese) "brl" else "usd"

        return tickers
            .asSequence()
            .filter { it.isStale != true && it.isAnomaly != true }
            .filter { !it.market?.name.isNullOrBlank() && !it.base.isNullOrBlank() && !it.target.isNullOrBlank() }
            .sortedByDescending { it.convertedVolume?.get("usd") ?: it.volume ?: 0.0 }
            .take(20)
            .map { ticker ->
                val marketName = ticker.market?.name.orEmpty().trim()
                val pairLabel = "${ticker.base.orEmpty().uppercase()}/${ticker.target.orEmpty().uppercase()}"

                val (priceValue, priceLocale) = resolveConvertedAmount(
                    map = ticker.convertedLast,
                    preferredKey = preferredKey,
                    preferredLocale = locale,
                    fallbackAmount = ticker.last
                )
                val lastPriceFormatted = priceValue?.formatAxisMoney(priceLocale) ?: "-"

                val (volValue, volLocale) = resolveConvertedAmount(
                    map = ticker.convertedVolume,
                    preferredKey = preferredKey,
                    preferredLocale = locale,
                    fallbackAmount = ticker.volume
                )
                val volumeFormatted = volValue?.takeIf { it > 0.0 }?.formatCompactMoney(volLocale) ?: "-"

                val spreadLabel = ticker.bidAskSpreadPercentage
                    ?.takeIf { it >= 0.0 }
                    ?.let { spread ->
                        val symbols = DecimalFormatSymbols(locale)
                        "${DecimalFormat("#,##0.00", symbols).format(spread)}%"
                    }

                val trustLevel = when (ticker.trustScore?.trim()?.lowercase(Locale.US)) {
                    "green" -> TickerTrustLevel.HIGH
                    "yellow" -> TickerTrustLevel.MEDIUM
                    "red" -> TickerTrustLevel.LOW
                    else -> TickerTrustLevel.UNKNOWN
                }

                CoinDetailTickerVO(
                    marketName = marketName,
                    pairLabel = pairLabel,
                    lastPrice = lastPriceFormatted,
                    volume24h = volumeFormatted,
                    spreadLabel = spreadLabel,
                    trustLevel = trustLevel,
                    tradeUrl = sanitizeHttpUrl(ticker.tradeUrl).orEmpty()
                )
            }
            .toList()
    }

    private fun resolveConvertedAmount(
        map: Map<String, Double?>?,
        preferredKey: String,
        preferredLocale: Locale,
        fallbackAmount: Double?
    ): Pair<Double?, Locale> {
        val preferred = map?.get(preferredKey)?.takeIf { it > 0.0 }
        if (preferred != null) return preferred to preferredLocale

        val usd = map?.get("usd")?.takeIf { it > 0.0 }
        if (usd != null) return usd to Locale.US

        return fallbackAmount?.takeIf { it > 0.0 } to Locale.US
    }

    private fun mapMarketChartDataPointList(marketChartDto: CoinDetailMarketChartResponse): List<MarketChartDataPoint> {
        if (marketChartDto.prices.isEmpty()) return emptyList()

        val firstTimestamp = marketChartDto.prices.firstOrNull()?.firstOrNull()?.toLong() ?: 0L
        val lastTimestamp = marketChartDto.prices.lastOrNull()?.firstOrNull()?.toLong() ?: firstTimestamp
        val spanHours = ((lastTimestamp - firstTimestamp).coerceAtLeast(0L)) / 3_600_000L

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
            mapMarketChartDataPoint(it, spanHours)
        }
    }

    private fun mapMarketChartDataPoint(dataPointList: List<Double>, spanHours: Long): MarketChartDataPoint {
        val timestamp = dataPointList.first().toLong()
        val date = timestamp.toFormatDate()
        val axisTime = timestamp.toChartAxisTime(spanHours)
        val hourLabel = timestamp.toChartHour()
        val dayLabel = timestamp.toChartDay()
        val price = dataPointList[1]

        return MarketChartDataPoint(
            date = date,
            price = price,
            axisTime = axisTime,
            hourLabel = hourLabel,
            dayLabel = dayLabel,
            timestampMillis = timestamp
        )
    }

    private fun Map<String, Double?>?.resolveEntryByLocale(locale: Locale): Pair<Double, Locale>? {
        if (this.isNullOrEmpty()) return null
        val isPortuguese = locale.language.equals("pt", ignoreCase = true)
        if (isPortuguese) {
            val brl = this["brl"]
            if (brl != null && brl > 0.0) return brl to locale
        }
        val usd = this["usd"]
        if (usd != null && usd > 0.0) {
            val targetLocale = if (isPortuguese) Locale.US else locale
            return usd to targetLocale
        }
        return null
    }

    private fun Map<String, Double?>?.resolveValueByLocale(locale: Locale): Double? {
        if (this.isNullOrEmpty()) return null
        val preferredKey = if (locale.language.equals("pt", ignoreCase = true)) "brl" else "usd"
        return this[preferredKey] ?: this["usd"]
    }

    private fun Map<String, Double?>?.formatCurrencyAmount(locale: Locale): String? {
        val (amount, targetLocale) = resolveEntryByLocale(locale) ?: return null
        return amount.formatMoney(targetLocale)
    }

    private fun String.formatGenesisDate(locale: Locale): String? {
        val trimmed = trim()
        if (trimmed.isEmpty()) return null
        return runCatching {
            LocalDate.parse(trimmed).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
        }.getOrElse { trimmed }
    }

    private fun Double.formatBlockTime(locale: Locale): String? {
        if (this <= 0.0) return null
        return if (this < 1.0) {
            val seconds = (this * 60.0).roundToInt().coerceAtLeast(1)
            "$seconds s"
        } else {
            val rounded = roundToInt()
            if (kotlin.math.abs(this - rounded) < 0.05) {
                "$rounded min"
            } else {
                val symbols = DecimalFormatSymbols(locale)
                "${DecimalFormat("#,##0.#", symbols).format(this)} min"
            }
        }
    }

    private fun CoinValue.getRawPriceByLocale(): Double =
        if (Locale.getDefault().language == "pt") brl else usd

    private fun CoinValue.getCurrentPriceByLocale(): String {
        return getRawPriceByLocale().formatMoney()
    }

    private fun CoinDate.getCurrentDateByLocale(): String {
        val date = if (Locale.getDefault().language == "pt") brl else usd
        return if (date.isBlank()) "" else runCatching { date.toFormatDate() }.getOrDefault("")
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
