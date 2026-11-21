package br.com.terras.app.coins.detail.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.terras.app.coins.detail.domain.DetailUseCase
import br.com.terras.app.coins.detail.domain.model.CoinChartDetailVO
import br.com.terras.app.coins.detail.domain.model.CoinDetailVO
import br.com.terras.app.coins.detail.domain.model.CoinInsightVO
import br.com.terras.app.feeds.domain.FeedsUseCase
import br.com.terras.app.feeds.domain.model.ArticleVO
import br.com.terras.app.network.favorites.FavoriteCoinsLocalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DetailViewModel @Inject constructor(
    private val useCase: DetailUseCase,
    private val feedsUseCase: FeedsUseCase,
    private val favoriteLocalRepository: FavoriteCoinsLocalRepository
) : ViewModel() {

    sealed interface DetailIntent {
        data class LoadDetail(val id: String) : DetailIntent
        data class LoadChart(val id: String, val days: Int = 1) : DetailIntent
        data class LoadInsights(val coinId: String) : DetailIntent
        data class LoadNews(val query: String) : DetailIntent
        data class ToggleFavorite(val coinId: String) : DetailIntent
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

    private val _chart: MutableStateFlow<DetailState> = MutableStateFlow(DetailState.Loading)
    val chart: StateFlow<DetailState> = _chart.asStateFlow()

    private val _insights: MutableStateFlow<DetailState> = MutableStateFlow(DetailState.Loading)
    val insights: StateFlow<DetailState> = _insights.asStateFlow()

    private val _news: MutableStateFlow<DetailState> = MutableStateFlow(DetailState.Loading)
    val news: StateFlow<DetailState> = _news.asStateFlow()

    fun handleIntent(intent: DetailIntent) {
        when (intent) {
            is DetailIntent.LoadDetail -> getDetailById(intent.id)
            is DetailIntent.LoadChart -> getChartById(intent.id, intent.days)
            is DetailIntent.LoadInsights -> getInsightsByCoinId(intent.coinId)
            is DetailIntent.LoadNews -> getNewsByQuery(intent.query)
            is DetailIntent.ToggleFavorite -> toggleFavorite(intent.coinId)
        }
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
                    val message = when (throwable) {
                        is ClientRequestException -> when (throwable.response.status) {
                            HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden ->
                                "Insights do CoinGecko requerem uma chave de API Pro (plano Analyst+)."
                            HttpStatusCode.TooManyRequests ->
                                "Limite de requisições atingido. Tente novamente em instantes."
                            else ->
                                "Não foi possível carregar os insights (${throwable.response.status.value})."
                        }
                        else -> "Não foi possível carregar os insights de mercado no momento."
                    }
                    _insights.update { DetailState.InsightsError(message) }
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
}