package br.com.terras.app.coins.detail.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.terras.app.coins.detail.domain.CoinDetailPortfolioCalculator
import br.com.terras.app.coins.detail.domain.CoinPortfolioInsightVO
import br.com.terras.app.coins.detail.domain.DetailUseCase
import br.com.terras.app.coins.detail.domain.model.CoinChartDetailVO
import br.com.terras.app.coins.detail.domain.model.CoinDetailVO
import br.com.terras.app.coins.detail.domain.model.CoinInsightVO
import br.com.terras.app.coins.detail.domain.model.DataPoint
import br.com.terras.app.coins.detail.domain.model.OrderBookMultiplier
import br.com.terras.app.coins.detail.domain.model.OrderBookUiState
import br.com.terras.app.coins.detail.network.CoinWebSocketService
import br.com.terras.app.coins.detail.network.CoinbaseOrderBookService
import br.com.terras.app.coins.detail.network.LivePriceTick
import br.com.terras.app.coins.detail.network.OrderBookStreamEvent
import br.com.terras.app.common.ai.AiAnalysisUiState
import br.com.terras.app.common.ai.AiChartAnalysisReport
import br.com.terras.app.common.ai.AiChartAnalysisRequest
import br.com.terras.app.common.ai.CryptoAiAnalyzer
import br.com.terras.app.common.formatLivePrice
import br.com.terras.app.common.formatPercentage
import br.com.terras.app.common.toChartDay
import br.com.terras.app.common.toChartHour
import br.com.terras.app.common.toFormatDate
import br.com.terras.app.dsm.ui.component.TrendColor
import br.com.terras.app.feeds.domain.FeedsUseCase
import br.com.terras.app.feeds.domain.model.ArticleVO
import br.com.terras.app.database.favorites.FavoriteCoinsLocalRepository
import br.com.terras.app.database.portfolio.PortfolioLocalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DetailViewModel @Inject constructor(
    private val useCase: DetailUseCase,
    private val feedsUseCase: FeedsUseCase,
    private val favoriteLocalRepository: FavoriteCoinsLocalRepository,
    private val portfolioLocalRepository: PortfolioLocalRepository,
    private val webSocketService: CoinWebSocketService,
    private val orderBookService: CoinbaseOrderBookService,
    private val aiAnalyzer: CryptoAiAnalyzer,
    private val portfolioCalculator: CoinDetailPortfolioCalculator = CoinDetailPortfolioCalculator()
) : ViewModel() {

    sealed interface DetailIntent {
        data class LoadDetail(val id: String) : DetailIntent
        data class LoadChart(val id: String, val days: Int = 1) : DetailIntent
        data class LoadInsights(val coinId: String) : DetailIntent
        data class LoadNews(val query: String) : DetailIntent
        data class ToggleFavorite(val coinId: String) : DetailIntent
        data class Refresh(val id: String, val days: Int = DaysOfChart.DAY.value) : DetailIntent
        data class ToggleRealTime(
            val enabled: Boolean,
            val coinId: String,
            val coinSymbol: String = ""
        ) : DetailIntent
        data class ToggleOrderBookExpanded(val expanded: Boolean) : DetailIntent
        data class SelectOrderBookMultiplier(val multiplier: OrderBookMultiplier) : DetailIntent
        data class AnalyzeChartWithAi(val isBarChart: Boolean) : DetailIntent
        data object ClearChartAiAnalysis : DetailIntent
    }

    sealed interface DetailState {
        data object Loading : DetailState
        data class SuccessDetail(val coin: CoinDetailVO) : DetailState
        data class SuccessChart(val chart: CoinChartDetailVO, val days: Int = DaysOfChart.DAY.value) : DetailState
        data class SuccessInsights(val insights: List<CoinInsightVO>) : DetailState
        data class InsightsError(val message: String) : DetailState
        data class SuccessNews(val articles: List<ArticleVO>) : DetailState
        data object Error : DetailState
    }

    private val currentCoinId: MutableStateFlow<String> = MutableStateFlow("")
    private var currentCoinName: String = ""
    private var currentCoinSymbol: String = ""
    private val isPortuguese: Boolean
        get() = Locale.getDefault().language.equals("pt", ignoreCase = true)

    val isFavorite: StateFlow<Boolean> = currentCoinId
        .flatMapLatest { coinId ->
            if (coinId.isBlank()) {
                flowOf(false)
            } else {
                favoriteLocalRepository.observeIsFavorite(coinId)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    private val _detail: MutableStateFlow<DetailState> = MutableStateFlow(DetailState.Loading)
    val detail: StateFlow<DetailState> = _detail.asStateFlow()

    val portfolioInsight: StateFlow<CoinPortfolioInsightVO?> = combine(
        currentCoinId,
        _detail,
        portfolioLocalRepository.observeTransactionsChronological()
    ) { coinId, detailState, transactions ->
        if (coinId.isBlank()) {
            null
        } else {
            val livePrice = (detailState as? DetailState.SuccessDetail)?.coin?.rawPrice
            portfolioCalculator.calculate(
                coinId = coinId,
                transactions = transactions,
                livePrice = livePrice
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private data class OrderBookResolvedInput(
        val symbol: String,
        val isPairAvailable: Boolean,
        val isExpanded: Boolean
    )

    private val _orderBookPairAvailability: MutableStateFlow<Pair<String, Boolean>?> = MutableStateFlow(null)
    private val _isOrderBookExpanded: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val _orderBookMultiplier: MutableStateFlow<OrderBookMultiplier> = MutableStateFlow(OrderBookMultiplier.DEFAULT)

    val orderBookState: StateFlow<OrderBookUiState> = combine(
        currentCoinId,
        _detail,
        _orderBookPairAvailability,
        _isOrderBookExpanded
    ) { coinId, detailState, availability, isExpanded ->
        val cleanSymbol = if (coinId.isBlank() || detailState !is DetailState.SuccessDetail) {
            ""
        } else {
            detailState.coin.symbol.trim().uppercase(Locale.US)
        }
        val available = cleanSymbol.isNotEmpty() &&
            availability?.first == cleanSymbol &&
            availability.second
        OrderBookResolvedInput(
            symbol = cleanSymbol,
            isPairAvailable = available,
            isExpanded = isExpanded
        )
    }
        .distinctUntilChanged()
        .flatMapLatest { input ->
            if (!input.isPairAvailable || input.symbol.isEmpty()) {
                flowOf<OrderBookUiState>(OrderBookUiState.Hidden)
            } else {
                val pairSymbol = "${input.symbol}-USD"
                if (!input.isExpanded) {
                    flowOf<OrderBookUiState>(OrderBookUiState.Collapsed(pairSymbol))
                } else {
                    flow<OrderBookUiState> {
                        emit(OrderBookUiState.Loading(pairSymbol))
                        orderBookService.observeOrderBook(input.symbol, _orderBookMultiplier)
                            .map<OrderBookStreamEvent, OrderBookUiState> { event ->
                                when (event) {
                                    is OrderBookStreamEvent.Snapshot -> OrderBookUiState.Visible(event.data)
                                    is OrderBookStreamEvent.Unavailable -> OrderBookUiState.Hidden
                                }
                            }
                            .catch { emit(OrderBookUiState.Hidden) }
                            .collect { emit(it) }
                    }
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = OrderBookUiState.Hidden
        )

    private val _chart: MutableStateFlow<DetailState> = MutableStateFlow(DetailState.Loading)
    val chart: StateFlow<DetailState> = _chart.asStateFlow()

    private val _insights: MutableStateFlow<DetailState> = MutableStateFlow(DetailState.Loading)
    val insights: StateFlow<DetailState> = _insights.asStateFlow()

    private val _news: MutableStateFlow<DetailState> = MutableStateFlow(DetailState.Loading)
    val news: StateFlow<DetailState> = _news.asStateFlow()

    private val _isRefreshing: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _lastUpdatedAtMillis: MutableStateFlow<Long?> = MutableStateFlow(null)
    val lastUpdatedAtMillis: StateFlow<Long?> = _lastUpdatedAtMillis.asStateFlow()

    private val _refreshErrorMessage: MutableStateFlow<String?> = MutableStateFlow(null)
    val refreshErrorMessage: StateFlow<String?> = _refreshErrorMessage.asStateFlow()

    private val _isRealTimeActive: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isRealTimeActive: StateFlow<Boolean> = _isRealTimeActive.asStateFlow()

    private val _aiChartAnalysis: MutableStateFlow<AiAnalysisUiState<AiChartAnalysisReport>> =
        MutableStateFlow(AiAnalysisUiState.Idle)
    val aiChartAnalysis: StateFlow<AiAnalysisUiState<AiChartAnalysisReport>> = _aiChartAnalysis.asStateFlow()

    private var webSocketJob: Job? = null
    private var chartAiJob: Job? = null
    private var activeRealTimeCoinId: String? = null

    fun handleIntent(intent: DetailIntent) {
        when (intent) {
            is DetailIntent.LoadDetail -> getDetailById(intent.id)
            is DetailIntent.LoadChart -> {
                stopRealTime()
                getChartById(intent.id, intent.days)
            }
            is DetailIntent.LoadInsights -> getInsightsByCoinId(intent.coinId)
            is DetailIntent.LoadNews -> getNewsByQuery(intent.query)
            is DetailIntent.ToggleFavorite -> toggleFavorite(intent.coinId)
            is DetailIntent.Refresh -> refreshAll(intent.id, intent.days)
            is DetailIntent.ToggleRealTime -> toggleRealTime(intent.enabled, intent.coinId, intent.coinSymbol)
            is DetailIntent.ToggleOrderBookExpanded -> {
                _isOrderBookExpanded.value = intent.expanded
            }
            is DetailIntent.SelectOrderBookMultiplier -> {
                _orderBookMultiplier.value = intent.multiplier
            }
            is DetailIntent.AnalyzeChartWithAi -> analyzeChartWithAi(intent.isBarChart)
            is DetailIntent.ClearChartAiAnalysis -> {
                chartAiJob?.cancel()
                _aiChartAnalysis.value = AiAnalysisUiState.Idle
            }
        }
    }

    fun consumeRefreshErrorMessage() {
        _refreshErrorMessage.value = null
    }

    fun toggleFavorite(coinId: String) {
        if (coinId.isBlank()) return
        currentCoinId.value = coinId
        viewModelScope.launch {
            favoriteLocalRepository.toggleFavorite(coinId)
        }
    }

    fun getDetailById(id: String) {
        val isNewCoin = currentCoinId.value.isNotBlank() && currentCoinId.value != id
        if (isNewCoin) {
            stopRealTime()
            chartAiJob?.cancel()
            _aiChartAnalysis.value = AiAnalysisUiState.Idle
            _isOrderBookExpanded.value = false
            _orderBookMultiplier.value = OrderBookMultiplier.DEFAULT
            _orderBookPairAvailability.value = null
        }
        currentCoinId.value = id
        viewModelScope.launch {
            if (isNewCoin || _detail.value is DetailState.Error) {
                _detail.value = DetailState.Loading
            }
            useCase.getDetailById(id)
                .onSuccess { result ->
                    currentCoinName = result.name
                    currentCoinSymbol = result.symbol
                    val cleanSymbol = result.symbol.trim().uppercase(Locale.US)
                    val pairAvailable = resolveOrderBookPairAvailability(cleanSymbol)
                    _orderBookPairAvailability.value = cleanSymbol to pairAvailable
                    _lastUpdatedAtMillis.value = System.currentTimeMillis()
                    _detail.update {
                        DetailState.SuccessDetail(result)
                    }
                }
                .onFailure {
                    _detail.update {
                        DetailState.Error
                    }
                }
        }
    }

    private suspend fun resolveOrderBookPairAvailability(cleanSymbol: String): Boolean {
        if (cleanSymbol.isEmpty()) return false
        val cached = _orderBookPairAvailability.value
        if (cached != null && cached.first == cleanSymbol) {
            return cached.second
        }
        return try {
            orderBookService.isPairAvailable(cleanSymbol)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            false
        }
    }

    fun toggleRealTime(enabled: Boolean, coinId: String, coinSymbol: String = "") {
        val normalizedCoinId = coinId.trim()
        if (!enabled || normalizedCoinId.isEmpty()) {
            stopRealTime()
            return
        }

        if (_isRealTimeActive.value &&
            activeRealTimeCoinId.equals(normalizedCoinId, ignoreCase = true) &&
            webSocketJob?.isActive == true
        ) {
            return
        }

        webSocketJob?.cancel()
        activeRealTimeCoinId = normalizedCoinId
        _isRealTimeActive.value = true

        val locale = Locale.getDefault()
        val vsCurrency = if (locale.language.equals("pt", ignoreCase = true)) "brl" else "usd"
        val resolvedSymbol = coinSymbol.trim()
            .ifBlank { (_detail.value as? DetailState.SuccessDetail)?.coin?.symbol.orEmpty().trim() }
            .ifBlank { currentCoinSymbol.trim() }

        webSocketJob = viewModelScope.launch {
            webSocketService.observeLivePrice(normalizedCoinId, resolvedSymbol, vsCurrency).collect { tick ->
                val validated = validateAndFormatLiveTick(
                    tick = tick,
                    expectedCoinId = normalizedCoinId,
                    expectedVsCurrency = vsCurrency,
                    locale = locale
                ) ?: return@collect

                _lastUpdatedAtMillis.value = validated.timestampMillis

                _detail.update { state ->
                    val success = state as? DetailState.SuccessDetail ?: return@update state
                    val updatedTrend = validated.change24h?.let { change ->
                        when {
                            change > 0.0 -> TrendColor.UP
                            change < 0.0 -> TrendColor.DOWN
                            else -> TrendColor.FLAT
                        }
                    } ?: success.coin.trendColor

                    val updatedChanges = if (validated.formattedChange24h != null) {
                        success.coin.priceChangePercentageList.mapIndexed { index, item ->
                            if (index == 0 || item.priceChangeTime.equals("1D", ignoreCase = true)) {
                                item.copy(
                                    priceChangeValue = validated.formattedChange24h,
                                    trendColor = updatedTrend
                                )
                            } else {
                                item
                            }
                        }
                    } else {
                        success.coin.priceChangePercentageList
                    }

                    DetailState.SuccessDetail(
                        success.coin.copy(
                            price = validated.formattedPrice,
                            rawPrice = validated.price,
                            trendColor = updatedTrend,
                            priceChangePercentageList = updatedChanges
                        )
                    )
                }

                _chart.update { state ->
                    val successChart = state as? DetailState.SuccessChart ?: return@update state
                    val livePoint = DataPoint(
                        date = validated.timestampMillis.toFormatDate(),
                        y = validated.price,
                        xLabel = validated.timestampMillis.toChartHour(),
                        yLabel = validated.formattedPrice,
                        hourLabel = validated.timestampMillis.toChartHour(),
                        dayLabel = validated.timestampMillis.toChartDay(),
                        timestampMillis = validated.timestampMillis
                    )
                    val existing = successChart.chart.chartData
                    val updatedPoints = when {
                        existing.isEmpty() -> listOf(livePoint)
                        existing.last().timestampMillis >= validated.timestampMillis - LIVE_POINT_BUCKET_MS -> {
                            existing.dropLast(1) + livePoint
                        }
                        else -> (existing + livePoint).takeLast(MAX_REALTIME_CHART_POINTS)
                    }
                    successChart.copy(
                        chart = successChart.chart.copy(chartData = updatedPoints)
                    )
                }
            }
        }
    }

    private fun stopRealTime() {
        webSocketJob?.cancel()
        webSocketJob = null
        activeRealTimeCoinId = null
        _isRealTimeActive.value = false
    }

    override fun onCleared() {
        super.onCleared()
        webSocketJob?.cancel()
        chartAiJob?.cancel()
    }

    private fun analyzeChartWithAi(isBarChart: Boolean) {
        val detailSuccess = _detail.value as? DetailState.SuccessDetail
        val chartSuccess = _chart.value as? DetailState.SuccessChart
        val points = chartSuccess?.chart?.chartData.orEmpty()
        if (detailSuccess == null || points.isEmpty()) {
            _aiChartAnalysis.value = AiAnalysisUiState.Error(
                if (isPortuguese) {
                    "Aguarde o carregamento do gráfico antes de iniciar a análise."
                } else {
                    "Wait for the chart data to load before starting analysis."
                }
            )
            return
        }

        val coin = detailSuccess.coin
        val locale = Locale.getDefault()
        val timeframeLabel = if (_isRealTimeActive.value) {
            if (isPortuguese) "Tempo Real" else "Live"
        } else {
            DaysOfChart.entries.firstOrNull { it.value == chartSuccess?.days }?.text
                ?: "${chartSuccess?.days ?: 1}D"
        }
        val chartTypeLabel = if (isBarChart) {
            if (isPortuguese) "Candlestick" else "Candlestick"
        } else {
            if (isPortuguese) "Linha" else "Line"
        }

        val sampledPoints = sampleChartPoints(points, maxCount = 24)
        val sampledSummary = sampledPoints.joinToString(separator = "\n") { pt ->
            val label = pt.date.ifBlank { pt.hourLabel }.ifBlank { "-" }
            val priceText = pt.yLabel?.takeIf { it.isNotBlank() } ?: pt.y.formatLivePrice(locale)
            "$label: $priceText"
        }

        val startPrice = points.first().y
        val endPrice = points.last().y
        val minPrice = points.minOf { it.y }
        val maxPrice = points.maxOf { it.y }
        val netChangePercent = if (startPrice > 0.0) {
            ((endPrice - startPrice) / startPrice) * 100.0
        } else {
            0.0
        }

        val volume24h = coin.marketDataList.firstOrNull { it.first == "Trading Volume 24h" }?.second ?: "-"
        val high24h = coin.marketDataList.firstOrNull { it.first == "Highest Price 24h" }?.second ?: "-"
        val low24h = coin.marketDataList.firstOrNull { it.first == "Lowest Price 24h" }?.second ?: "-"
        val change24h = coin.priceChangePercentageList.firstOrNull()?.priceChangeValue ?: "-"
        val allPeriodChanges = (coin.priceChangePercentageList + coin.extraPriceChangePercentageList)
            .map { it.priceChangeTime.uppercase(Locale.US) to it.priceChangeValue }

        val orderBookSummary = (orderBookState.value as? OrderBookUiState.Visible)?.snapshot?.let { ob ->
            val bestBid = ob.bids.firstOrNull()?.priceFormatted ?: "-"
            val bestAsk = ob.asks.firstOrNull()?.priceFormatted ?: "-"
            "pair=${ob.pairSymbol}, spread=${ob.spreadAbsoluteFormatted} (${ob.spreadPercentageFormatted}), bestBid=$bestBid, bestAsk=$bestAsk"
        }

        val positionSummary = portfolioInsight.value?.let { p ->
            "holding=${p.quantityFormatted} ${p.symbol}, avgBuy=${p.averageBuyPriceFormatted}, currentValue=${p.currentValueFormatted}, totalPnL=${p.totalPnLFormatted} (${p.totalPnLPercentageFormatted})"
        }

        val request = AiChartAnalysisRequest(
            coinId = currentCoinId.value.ifBlank { coin.symbol.lowercase(Locale.US) },
            coinName = coin.name,
            coinSymbol = coin.symbol,
            currentPriceFormatted = coin.price,
            rawPrice = coin.rawPrice,
            timeframeLabel = timeframeLabel,
            chartTypeLabel = chartTypeLabel,
            change24hFormatted = change24h,
            high24hFormatted = high24h,
            low24hFormatted = low24h,
            volume24hFormatted = volume24h,
            periodChanges = allPeriodChanges,
            chartPointsCount = points.size,
            periodStartPriceFormatted = startPrice.formatLivePrice(locale),
            periodEndPriceFormatted = endPrice.formatLivePrice(locale),
            periodMinPriceFormatted = minPrice.formatLivePrice(locale),
            periodMaxPriceFormatted = maxPrice.formatLivePrice(locale),
            periodChangePercentFormatted = netChangePercent.formatPercentage(locale),
            sampledPointsSummary = sampledSummary,
            orderBookSummary = orderBookSummary,
            portfolioPositionSummary = positionSummary,
            languageTag = locale.language
        )

        chartAiJob?.cancel()
        chartAiJob = viewModelScope.launch {
            _aiChartAnalysis.value = AiAnalysisUiState.Loading
            try {
                val report = aiAnalyzer.analyzeChart(request)
                _aiChartAnalysis.value = AiAnalysisUiState.Success(report)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiChartAnalysis.value = AiAnalysisUiState.Error(
                    e.localizedMessage?.takeIf { it.isNotBlank() }
                        ?: if (isPortuguese) {
                            "Não foi possível gerar a análise do gráfico."
                        } else {
                            "Unable to generate chart analysis."
                        }
                )
            }
        }
    }

    private fun sampleChartPoints(points: List<DataPoint>, maxCount: Int): List<DataPoint> {
        if (points.size <= maxCount) return points
        val lastIdx = points.lastIndex
        return (0 until maxCount).map { idx ->
            val mapped = ((idx.toDouble() / (maxCount - 1)) * lastIdx).toInt().coerceIn(0, lastIdx)
            points[mapped]
        }.distinct()
    }

    internal data class ValidatedLiveTick(
        val price: Double,
        val formattedPrice: String,
        val change24h: Double?,
        val formattedChange24h: String?,
        val timestampMillis: Long
    )

    internal fun validateAndFormatLiveTick(
        tick: LivePriceTick,
        expectedCoinId: String,
        expectedVsCurrency: String,
        locale: Locale = Locale.getDefault()
    ): ValidatedLiveTick? {
        if (!tick.price.isFinite() || tick.price <= 0.0) return null
        if (tick.coinId.isNotBlank() && !tick.coinId.equals(expectedCoinId, ignoreCase = true)) {
            return null
        }
        if (tick.vsCurrency.isNotBlank() && !tick.vsCurrency.equals(expectedVsCurrency, ignoreCase = true)) {
            return null
        }
        val formattedPrice = tick.price.formatLivePrice(locale).takeIf { it.isNotBlank() } ?: return null
        val validChange = tick.change24h?.takeIf { it.isFinite() }
        val formattedChange = validChange?.formatPercentage(locale)
        val timestamp = tick.timestampMillis.takeIf { it > 0L } ?: System.currentTimeMillis()
        return ValidatedLiveTick(
            price = tick.price,
            formattedPrice = formattedPrice,
            change24h = validChange,
            formattedChange24h = formattedChange,
            timestampMillis = timestamp
        )
    }

    fun getChartById(id: String, days: Int = 1) {
        viewModelScope.launch {
            _chart.update { DetailState.Loading }
            useCase.getChartDetailById(id, days)
                .onSuccess { result ->
                    _chart.update {
                        DetailState.SuccessChart(result, days)
                    }
                }
                .onFailure {
                    _chart.update {
                        DetailState.Error
                    }
                }
        }
    }

    fun getInsightsByCoinId(coinId: String) {
        viewModelScope.launch {
            _insights.update { DetailState.Loading }
            useCase.getInsightsByCoinId(coinId)
                .onSuccess { insightsList ->
                    _insights.update { DetailState.SuccessInsights(insightsList) }
                }
                .onFailure { throwable ->
                    _insights.update { DetailState.InsightsError(mapInsightsError(throwable)) }
                }
        }
    }

    fun getNewsByQuery(query: String) {
        viewModelScope.launch {
            feedsUseCase.getFeeds(query)
                .onSuccess { articles ->
                    _news.update {
                        DetailState.SuccessNews(articles)
                    }
                }
                .onFailure {
                    _news.update {
                        DetailState.SuccessNews(emptyList())
                    }
                }
        }
    }

    private fun refreshAll(id: String, days: Int) {
        if (id.isBlank() || _isRefreshing.value) return
        currentCoinId.value = id
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                coroutineScope {
                    val detailDeferred = async { useCase.getDetailById(id) }
                    val chartDeferred = async { useCase.getChartDetailById(id, days) }
                    val insightsDeferred = async { useCase.getInsightsByCoinId(id) }
                    val newsName = currentCoinName
                    val newsDeferred = if (newsName.isNotBlank()) {
                        async { feedsUseCase.getFeeds(newsName) }
                    } else {
                        null
                    }

                    val detailResult = detailDeferred.await()
                    val chartResult = chartDeferred.await()
                    val insightsResult = insightsDeferred.await()
                    val newsResult = newsDeferred?.await()

                    detailResult
                        .onSuccess { result ->
                            currentCoinName = result.name
                            currentCoinSymbol = result.symbol
                            val cleanSymbol = result.symbol.trim().uppercase(Locale.US)
                            val pairAvailable = resolveOrderBookPairAvailability(cleanSymbol)
                            _orderBookPairAvailability.value = cleanSymbol to pairAvailable
                            _lastUpdatedAtMillis.value = System.currentTimeMillis()
                            _detail.update { DetailState.SuccessDetail(result) }
                            if (newsName.isBlank() && result.name.isNotBlank()) {
                                getNewsByQuery(result.name)
                            }
                        }
                        .onFailure {
                            if (_detail.value !is DetailState.SuccessDetail) {
                                _detail.update { DetailState.Error }
                            } else {
                                _refreshErrorMessage.value = if (isPortuguese) {
                                    "Não foi possível atualizar os dados no momento."
                                } else {
                                    "Unable to refresh data right now."
                                }
                            }
                        }

                    chartResult
                        .onSuccess { result ->
                            _chart.update { DetailState.SuccessChart(result, days) }
                        }
                        .onFailure {
                            if (_chart.value !is DetailState.SuccessChart) {
                                _chart.update { DetailState.Error }
                            }
                        }

                    insightsResult
                        .onSuccess { insightsList ->
                            _insights.update { DetailState.SuccessInsights(insightsList) }
                        }
                        .onFailure { throwable ->
                            if (_insights.value !is DetailState.SuccessInsights) {
                                _insights.update {
                                    DetailState.InsightsError(mapInsightsError(throwable))
                                }
                            }
                        }

                    newsResult?.onSuccess { articles ->
                        _news.update { DetailState.SuccessNews(articles) }
                    }
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun mapInsightsError(throwable: Throwable): String {
        val pt = isPortuguese
        return when (throwable) {
            is ClientRequestException -> when (throwable.response.status) {
                HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden ->
                    if (pt) {
                        "Insights do CoinGecko requerem uma chave de API Pro (plano Analyst+)."
                    } else {
                        "CoinGecko Insights require a Pro API key (Analyst+ plan)."
                    }
                HttpStatusCode.TooManyRequests ->
                    if (pt) {
                        "Limite de requisições atingido. Tente novamente em instantes."
                    } else {
                        "Rate limit reached. Please try again shortly."
                    }
                else ->
                    if (pt) {
                        "Não foi possível carregar os insights (${throwable.response.status.value})."
                    } else {
                        "Unable to load insights (${throwable.response.status.value})."
                    }
            }
            else -> if (pt) {
                "Não foi possível carregar os insights de mercado no momento."
            } else {
                "Unable to load market insights right now."
            }
        }
    }

    companion object {
        private const val LIVE_POINT_BUCKET_MS = 5_000L
        private const val MAX_REALTIME_CHART_POINTS = 200
    }
}