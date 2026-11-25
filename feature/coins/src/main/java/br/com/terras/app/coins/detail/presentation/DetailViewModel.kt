package br.com.terras.app.coins.detail.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.terras.app.coins.detail.domain.CoinDetailPortfolioCalculator
import br.com.terras.app.coins.detail.domain.CoinPortfolioInsightVO
import br.com.terras.app.coins.detail.domain.DetailUseCase
import br.com.terras.app.coins.detail.domain.model.CoinChartDetailVO
import br.com.terras.app.coins.detail.domain.model.CoinDetailVO
import br.com.terras.app.coins.detail.domain.model.CoinInsightVO
import br.com.terras.app.feeds.domain.FeedsUseCase
import br.com.terras.app.feeds.domain.model.ArticleVO
import br.com.terras.app.network.favorites.FavoriteCoinsLocalRepository
import br.com.terras.app.network.portfolio.PortfolioLocalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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
    private val portfolioCalculator: CoinDetailPortfolioCalculator = CoinDetailPortfolioCalculator()
) : ViewModel() {

    sealed interface DetailIntent {
        data class LoadDetail(val id: String) : DetailIntent
        data class LoadChart(val id: String, val days: Int = 1) : DetailIntent
        data class LoadInsights(val coinId: String) : DetailIntent
        data class LoadNews(val query: String) : DetailIntent
        data class ToggleFavorite(val coinId: String) : DetailIntent
        data class Refresh(val id: String, val days: Int = DaysOfChart.DAY.value) : DetailIntent
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

    fun handleIntent(intent: DetailIntent) {
        when (intent) {
            is DetailIntent.LoadDetail -> getDetailById(intent.id)
            is DetailIntent.LoadChart -> getChartById(intent.id, intent.days)
            is DetailIntent.LoadInsights -> getInsightsByCoinId(intent.coinId)
            is DetailIntent.LoadNews -> getNewsByQuery(intent.query)
            is DetailIntent.ToggleFavorite -> toggleFavorite(intent.coinId)
            is DetailIntent.Refresh -> refreshAll(intent.id, intent.days)
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
        currentCoinId.value = id
        viewModelScope.launch {
            useCase.getDetailById(id)
                .onSuccess { result ->
                    currentCoinName = result.name
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
}