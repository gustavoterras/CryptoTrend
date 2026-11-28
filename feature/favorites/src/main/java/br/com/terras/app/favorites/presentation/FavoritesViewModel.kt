package br.com.terras.app.favorites.presentation

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.terras.app.common.currentVsCurrency
import br.com.terras.app.common.formatLivePrice
import br.com.terras.app.common.formatPercentage
import br.com.terras.app.database.favorites.FavoriteCoinsLocalRepository
import br.com.terras.app.dsm.ui.component.TrendColor
import br.com.terras.app.favorites.domain.FavoritesUseCase
import br.com.terras.app.favorites.domain.model.CoinVO
import br.com.terras.app.favorites.network.FavoriteLiveTicker
import br.com.terras.app.favorites.network.FavoriteTickerBatchEvent
import br.com.terras.app.favorites.network.FavoriteTickerTarget
import br.com.terras.app.favorites.network.FavoritesTickerBatchService
import br.com.terras.app.favorites.network.NoOpFavoritesTickerBatchService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val useCase: FavoritesUseCase,
    private val favoriteLocalRepository: FavoriteCoinsLocalRepository,
    private val tickerBatchService: FavoritesTickerBatchService = NoOpFavoritesTickerBatchService
) : ViewModel() {

    sealed interface FavoritesIntent {
        data object LoadFavorites : FavoritesIntent
        data object Refresh : FavoritesIntent
        data class SetLiveStreamActive(val active: Boolean) : FavoritesIntent
    }

    sealed interface FavoritesState {
        data object Loading : FavoritesState
        data class Success(val coinsList: List<CoinVO>) : FavoritesState
        data object Error : FavoritesState
    }

    private val _coins: MutableStateFlow<FavoritesState> = MutableStateFlow(FavoritesState.Loading)
    val coins: StateFlow<FavoritesState> = _coins.asStateFlow()

    private val _isRefreshing: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isLiveTickerConnected: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isLiveTickerConnected: StateFlow<Boolean> = _isLiveTickerConnected.asStateFlow()

    private val isLiveStreamActive = MutableStateFlow(false)
    private val tickerTargetsFlow = MutableStateFlow<List<FavoriteTickerTarget>>(emptyList())

    private var currentFavoriteIds: List<String> = emptyList()
    private var cachedCoinsById: Map<String, CoinVO> = emptyMap()

    init {
        observeLocalFavorites()
        observeLiveTickerBatch()
    }

    private fun observeLocalFavorites() {
        viewModelScope.launch {
            favoriteLocalRepository.observeFavoriteIds().collectLatest { ids ->
                currentFavoriteIds = ids
                fetchCoinsForIds(ids, isRefresh = false)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeLiveTickerBatch() {
        viewModelScope.launch {
            combine(isLiveStreamActive, tickerTargetsFlow) { active, targets ->
                if (active && targets.isNotEmpty()) targets else emptyList()
            }
                .distinctUntilChanged()
                .flatMapLatest { targets ->
                    if (targets.isEmpty()) {
                        _isLiveTickerConnected.value = false
                        emptyFlow()
                    } else {
                        tickerBatchService.observeTickerBatch(
                            targets = targets,
                            vsCurrency = currentVsCurrency()
                        )
                    }
                }
                .collect { event ->
                    when (event) {
                        is FavoriteTickerBatchEvent.Connected -> {
                            _isLiveTickerConnected.value = event.activeProductCount > 0
                        }
                        is FavoriteTickerBatchEvent.Tick -> {
                            _isLiveTickerConnected.value = true
                            applyLiveTicker(event.ticker)
                        }
                        is FavoriteTickerBatchEvent.Disconnected -> {
                            _isLiveTickerConnected.value = false
                        }
                    }
                }
        }
    }

    fun handleIntent(intent: FavoritesIntent) {
        when (intent) {
            is FavoritesIntent.LoadFavorites -> getFavorites()
            is FavoritesIntent.Refresh -> refreshFavorites()
            is FavoritesIntent.SetLiveStreamActive -> {
                isLiveStreamActive.value = intent.active
            }
        }
    }

    @VisibleForTesting
    fun getFavorites() {
        viewModelScope.launch {
            fetchCoinsForIds(currentFavoriteIds, isRefresh = false)
        }
    }

    private fun refreshFavorites() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                fetchCoinsForIds(currentFavoriteIds, isRefresh = true)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private suspend fun fetchCoinsForIds(ids: List<String>, isRefresh: Boolean) {
        if (ids.isEmpty()) {
            cachedCoinsById = emptyMap()
            tickerTargetsFlow.value = emptyList()
            _coins.update { FavoritesState.Success(emptyList()) }
            return
        }

        val immediateMemoryCached = ids.mapNotNull { cachedCoinsById[it] }
        val effectiveCached = if (immediateMemoryCached.size == ids.size) {
            immediateMemoryCached
        } else {
            val diskCached = useCase.getCachedCoinsByIds(ids).orEmpty()
            if (diskCached.isNotEmpty()) {
                val diskById = diskCached.associateBy { it.id }
                cachedCoinsById = diskById + cachedCoinsById
                ids.mapNotNull { cachedCoinsById[it] }
            } else {
                immediateMemoryCached
            }
        }

        if (effectiveCached.size == ids.size || (!isRefresh && effectiveCached.isNotEmpty())) {
            tickerTargetsFlow.value = effectiveCached.map { FavoriteTickerTarget(it.id, it.symbol) }
            _coins.update { FavoritesState.Success(effectiveCached) }
        } else if (!isRefresh && effectiveCached.isEmpty()) {
            _coins.update { FavoritesState.Loading }
        }

        val result = if (isRefresh) {
            useCase.getCoinsByIds(ids, forceRefresh = true)
        } else {
            useCase.getCoinsByIds(ids)
        }

        result
            .onFailure {
                if (effectiveCached.isNotEmpty()) {
                    tickerTargetsFlow.value = effectiveCached.map { FavoriteTickerTarget(it.id, it.symbol) }
                    _coins.update { FavoritesState.Success(effectiveCached) }
                } else {
                    tickerTargetsFlow.value = emptyList()
                    _coins.update { FavoritesState.Error }
                }
            }
            .onSuccess { resultCoins ->
                val resultById = resultCoins.associateBy { it.id }
                cachedCoinsById = cachedCoinsById + resultById
                val orderedCoins = ids.mapNotNull { resultById[it] }
                tickerTargetsFlow.value = orderedCoins.map { FavoriteTickerTarget(it.id, it.symbol) }
                _coins.update { FavoritesState.Success(orderedCoins) }
            }
    }

    private fun applyLiveTicker(ticker: FavoriteLiveTicker) {
        val currentState = _coins.value as? FavoritesState.Success ?: return
        val targetCoin = cachedCoinsById[ticker.coinId]
            ?: currentState.coinsList.firstOrNull { it.id == ticker.coinId }
            ?: return

        val updatedPrice = ticker.price
        val updatedChange = ticker.change24h ?: targetCoin.rawPriceChangePercentage
        val updatedCoin = targetCoin.copy(
            rawPrice = updatedPrice,
            price = updatedPrice.formatLivePrice(),
            rawPriceChangePercentage = updatedChange,
            priceChangePercentage = updatedChange.formatPercentage(),
            trendColor = when {
                updatedChange > 0.0 -> TrendColor.UP
                updatedChange < 0.0 -> TrendColor.DOWN
                else -> TrendColor.FLAT
            }
        )

        cachedCoinsById = cachedCoinsById + (updatedCoin.id to updatedCoin)
        _coins.update { state ->
            if (state is FavoritesState.Success) {
                state.copy(
                    coinsList = state.coinsList.map { coin ->
                        if (coin.id == updatedCoin.id) updatedCoin else coin
                    }
                )
            } else {
                state
            }
        }
    }
}