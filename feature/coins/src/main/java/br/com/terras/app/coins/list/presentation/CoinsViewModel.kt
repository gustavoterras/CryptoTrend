package br.com.terras.app.coins.list.presentation

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.terras.app.coins.list.domain.CoinsUseCase
import br.com.terras.app.coins.list.domain.model.CoinVO
import br.com.terras.app.dsm.ui.component.TrendColor
import br.com.terras.app.feeds.domain.FeedsUseCase
import br.com.terras.app.network.favorites.FavoriteCoinsLocalRepository
import br.com.terras.app.network.refresh.DataRefreshCoordinator
import br.com.terras.app.search.domain.SearchUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CoinsViewModel @Inject constructor(
    private val useCase: CoinsUseCase,
    private val searchUseCase: SearchUseCase,
    private val feedsUseCase: FeedsUseCase,
    private val refreshCoordinator: DataRefreshCoordinator,
    favoriteLocalRepository: FavoriteCoinsLocalRepository
) : ViewModel() {

    sealed interface CoinsIntent {
        data object LoadCoins : CoinsIntent
        data object LoadMoreCoins : CoinsIntent
        data object Refresh : CoinsIntent
        data class SearchQueryChanged(val query: String) : CoinsIntent
        data class ExecuteSearch(val query: String) : CoinsIntent
        data object ClearSearch : CoinsIntent
        data object LoadTrendingCoins : CoinsIntent
        data object LoadMoreTrendingCoins : CoinsIntent
        data object RefreshTrending : CoinsIntent
        data class TrendingSearchQueryChanged(val query: String) : CoinsIntent
        data class ExecuteTrendingSearch(val query: String) : CoinsIntent
        data object ClearTrendingSearch : CoinsIntent
    }

    sealed interface CoinsState {
        data object Loading : CoinsState
        data class Success(
            val coinsList: List<CoinVO>,
            val isFromSearchApi: Boolean = false,
            val isLoadingMore: Boolean = false,
            val hasMorePages: Boolean = false
        ) : CoinsState
        data object EmptySearch : CoinsState
        data object Error : CoinsState
    }

    private val _coins: MutableStateFlow<CoinsState> = MutableStateFlow(CoinsState.Loading)
    val coins: StateFlow<CoinsState> = _coins.asStateFlow()

    private val _searchQuery: MutableStateFlow<String> = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isRefreshing: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _trendingCoins: MutableStateFlow<CoinsState> = MutableStateFlow(CoinsState.Loading)
    val trendingCoins: StateFlow<CoinsState> = _trendingCoins.asStateFlow()

    private val _trendingSearchQuery: MutableStateFlow<String> = MutableStateFlow("")
    val trendingSearchQuery: StateFlow<String> = _trendingSearchQuery.asStateFlow()

    private val _isTrendingRefreshing: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isTrendingRefreshing: StateFlow<Boolean> = _isTrendingRefreshing.asStateFlow()

    val favoriteIds: StateFlow<Set<String>> = favoriteLocalRepository.observeFavoriteIds()
        .map { it.toSet() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

    private var allCoins: List<CoinVO> = emptyList()
    private var allTrendingCoins: List<CoinVO> = emptyList()
    private var currentCoinsPage: Int = 1
    private var currentTrendingPage: Int = 1
    private var hasMoreCoinsPages: Boolean = false
    private var hasMoreTrendingPages: Boolean = false
    private var isLoadingMoreCoins: Boolean = false
    private var isLoadingMoreTrending: Boolean = false
    private var searchJob: Job? = null
    private var trendingSearchJob: Job? = null
    private var loadMoreCoinsJob: Job? = null
    private var loadMoreTrendingJob: Job? = null

    init {
        handleIntent(CoinsIntent.LoadCoins)
    }

    fun handleIntent(intent: CoinsIntent) {
        when (intent) {
            is CoinsIntent.LoadCoins -> fetchCoins()
            is CoinsIntent.LoadMoreCoins -> loadMoreCoins()
            is CoinsIntent.Refresh -> refreshCoinsAndFeeds()
            is CoinsIntent.SearchQueryChanged -> onSearchQueryChanged(intent.query)
            is CoinsIntent.ExecuteSearch -> onExecuteSearch(intent.query)
            is CoinsIntent.ClearSearch -> clearSearch()
            is CoinsIntent.LoadTrendingCoins -> loadTrendingIfNeeded()
            is CoinsIntent.LoadMoreTrendingCoins -> loadMoreTrendingCoins()
            is CoinsIntent.RefreshTrending -> refreshTrendingAndFeeds()
            is CoinsIntent.TrendingSearchQueryChanged -> onTrendingSearchQueryChanged(intent.query)
            is CoinsIntent.ExecuteTrendingSearch -> onExecuteTrendingSearch(intent.query)
            is CoinsIntent.ClearTrendingSearch -> clearTrendingSearch()
        }
    }

    @VisibleForTesting
    fun getCoins() = fetchCoins()

    @VisibleForTesting
    fun getTrendingCoins() = fetchTrendingCoins()

    private fun loadTrendingIfNeeded() {
        if (allTrendingCoins.isNotEmpty() && _trendingCoins.value !is CoinsState.Error) return
        fetchTrendingCoins()
    }

    private fun fetchCoins() {
        loadMoreCoinsJob?.cancel()
        isLoadingMoreCoins = false
        _coins.update { CoinsState.Loading }
        viewModelScope.launch {
            useCase.getCoins(page = 1)
                .onFailure {
                    _coins.update {
                        CoinsState.Error
                    }
                }
                .onSuccess { result ->
                    currentCoinsPage = 1
                    hasMoreCoinsPages = result.size >= CoinsUseCase.CRYPTO_PAGE_SIZE
                    allCoins = result.distinctBy { it.id }
                    val currentQuery = _searchQuery.value.trim()
                    if (currentQuery.isEmpty()) {
                        _coins.update {
                            CoinsState.Success(
                                coinsList = allCoins,
                                isLoadingMore = false,
                                hasMorePages = hasMoreCoinsPages
                            )
                        }
                    } else {
                        searchLocalOrApi(currentQuery, debounceApi = false)
                    }
                }
        }
    }

    private fun loadMoreCoins() {
        if (_searchQuery.value.isNotBlank() ||
            _isRefreshing.value ||
            isLoadingMoreCoins ||
            !hasMoreCoinsPages ||
            _coins.value !is CoinsState.Success
        ) {
            return
        }

        isLoadingMoreCoins = true
        _coins.update {
            CoinsState.Success(
                coinsList = allCoins,
                isLoadingMore = true,
                hasMorePages = hasMoreCoinsPages
            )
        }

        loadMoreCoinsJob = viewModelScope.launch {
            val nextPage = currentCoinsPage + 1
            useCase.getCoins(page = nextPage)
                .onFailure {
                    isLoadingMoreCoins = false
                    if (_searchQuery.value.isBlank()) {
                        _coins.update {
                            CoinsState.Success(
                                coinsList = allCoins,
                                isLoadingMore = false,
                                hasMorePages = hasMoreCoinsPages
                            )
                        }
                    }
                }
                .onSuccess { nextPageItems ->
                    val existingIds = allCoins.asSequence().map { it.id }.toHashSet()
                    val uniqueNewItems = nextPageItems.filterNot { it.id in existingIds }
                    if (uniqueNewItems.isNotEmpty()) {
                        allCoins = allCoins + uniqueNewItems
                        currentCoinsPage = nextPage
                    }
                    hasMoreCoinsPages = uniqueNewItems.isNotEmpty() &&
                        nextPageItems.size >= CoinsUseCase.CRYPTO_PAGE_SIZE
                    isLoadingMoreCoins = false

                    val currentQuery = _searchQuery.value.trim()
                    if (currentQuery.isEmpty()) {
                        _coins.update {
                            CoinsState.Success(
                                coinsList = allCoins,
                                isLoadingMore = false,
                                hasMorePages = hasMoreCoinsPages
                            )
                        }
                    } else {
                        searchLocalOrApi(currentQuery, debounceApi = false, showLoading = false)
                    }
                }
        }
    }

    private fun fetchTrendingCoins() {
        loadMoreTrendingJob?.cancel()
        isLoadingMoreTrending = false
        _trendingCoins.update { CoinsState.Loading }
        viewModelScope.launch {
            useCase.getTrendingCoins(page = 1)
                .onFailure {
                    _trendingCoins.update {
                        CoinsState.Error
                    }
                }
                .onSuccess { result ->
                    currentTrendingPage = 1
                    hasMoreTrendingPages = result.size >= CoinsUseCase.TRENDING_PAGE_SIZE
                    allTrendingCoins = result.distinctBy { it.id }
                    val currentQuery = _trendingSearchQuery.value.trim()
                    if (currentQuery.isEmpty()) {
                        _trendingCoins.update {
                            CoinsState.Success(
                                coinsList = allTrendingCoins,
                                isLoadingMore = false,
                                hasMorePages = hasMoreTrendingPages
                            )
                        }
                    } else {
                        searchTrendingLocalOrApi(currentQuery, debounceApi = false)
                    }
                }
        }
    }

    private fun loadMoreTrendingCoins() {
        if (_trendingSearchQuery.value.isNotBlank() ||
            _isTrendingRefreshing.value ||
            isLoadingMoreTrending ||
            !hasMoreTrendingPages ||
            _trendingCoins.value !is CoinsState.Success
        ) {
            return
        }

        isLoadingMoreTrending = true
        _trendingCoins.update {
            CoinsState.Success(
                coinsList = allTrendingCoins,
                isLoadingMore = true,
                hasMorePages = hasMoreTrendingPages
            )
        }

        loadMoreTrendingJob = viewModelScope.launch {
            val nextPage = currentTrendingPage + 1
            useCase.getTrendingCoins(page = nextPage)
                .onFailure {
                    isLoadingMoreTrending = false
                    if (_trendingSearchQuery.value.isBlank()) {
                        _trendingCoins.update {
                            CoinsState.Success(
                                coinsList = allTrendingCoins,
                                isLoadingMore = false,
                                hasMorePages = hasMoreTrendingPages
                            )
                        }
                    }
                }
                .onSuccess { nextPageItems ->
                    val existingIds = allTrendingCoins.asSequence().map { it.id }.toHashSet()
                    val uniqueNewItems = nextPageItems.filterNot { it.id in existingIds }
                    if (uniqueNewItems.isNotEmpty()) {
                        allTrendingCoins = allTrendingCoins + uniqueNewItems
                        currentTrendingPage = nextPage
                    }
                    hasMoreTrendingPages = uniqueNewItems.isNotEmpty() &&
                        nextPageItems.size >= CoinsUseCase.TRENDING_PAGE_SIZE
                    isLoadingMoreTrending = false

                    val currentQuery = _trendingSearchQuery.value.trim()
                    if (currentQuery.isEmpty()) {
                        _trendingCoins.update {
                            CoinsState.Success(
                                coinsList = allTrendingCoins,
                                isLoadingMore = false,
                                hasMorePages = hasMoreTrendingPages
                            )
                        }
                    } else {
                        searchTrendingLocalOrApi(currentQuery, debounceApi = false, showLoading = false)
                    }
                }
        }
    }

    private fun refreshCoinsAndFeeds() {
        if (_isRefreshing.value) return
        loadMoreCoinsJob?.cancel()
        isLoadingMoreCoins = false
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                refreshCoordinator.triggerRefresh()
                coroutineScope {
                    val coinsDeferred = async { useCase.getCoins(page = 1) }
                    val feedsDeferred = async { feedsUseCase.getFeeds("bitcoin") }
                    val coinsResult = coinsDeferred.await()
                    feedsDeferred.await()

                    coinsResult
                        .onFailure {
                            if (allCoins.isEmpty()) {
                                _coins.update { CoinsState.Error }
                            }
                        }
                        .onSuccess { result ->
                            currentCoinsPage = 1
                            hasMoreCoinsPages = result.size >= CoinsUseCase.CRYPTO_PAGE_SIZE
                            allCoins = result.distinctBy { it.id }
                            val currentQuery = _searchQuery.value.trim()
                            if (currentQuery.isEmpty()) {
                                _coins.update {
                                    CoinsState.Success(
                                        coinsList = allCoins,
                                        isLoadingMore = false,
                                        hasMorePages = hasMoreCoinsPages
                                    )
                                }
                            } else {
                                searchLocalOrApi(
                                    trimmedQuery = currentQuery,
                                    debounceApi = false,
                                    showLoading = false
                                )
                            }
                        }
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun refreshTrendingAndFeeds() {
        if (_isTrendingRefreshing.value) return
        loadMoreTrendingJob?.cancel()
        isLoadingMoreTrending = false
        viewModelScope.launch {
            _isTrendingRefreshing.value = true
            try {
                refreshCoordinator.triggerRefresh()
                coroutineScope {
                    val trendingDeferred = async { useCase.getTrendingCoins(page = 1) }
                    val feedsDeferred = async { feedsUseCase.getFeeds("bitcoin") }
                    val trendingResult = trendingDeferred.await()
                    feedsDeferred.await()

                    trendingResult
                        .onFailure {
                            if (allTrendingCoins.isEmpty()) {
                                _trendingCoins.update { CoinsState.Error }
                            }
                        }
                        .onSuccess { result ->
                            currentTrendingPage = 1
                            hasMoreTrendingPages = result.size >= CoinsUseCase.TRENDING_PAGE_SIZE
                            allTrendingCoins = result.distinctBy { it.id }
                            val currentQuery = _trendingSearchQuery.value.trim()
                            if (currentQuery.isEmpty()) {
                                _trendingCoins.update {
                                    CoinsState.Success(
                                        coinsList = allTrendingCoins,
                                        isLoadingMore = false,
                                        hasMorePages = hasMoreTrendingPages
                                    )
                                }
                            } else {
                                searchTrendingLocalOrApi(
                                    trimmedQuery = currentQuery,
                                    debounceApi = false,
                                    showLoading = false
                                )
                            }
                        }
                }
            } finally {
                _isTrendingRefreshing.value = false
            }
        }
    }

    private fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        searchLocalOrApi(query.trim(), debounceApi = true)
    }

    private fun onExecuteSearch(query: String) {
        _searchQuery.value = query
        searchLocalOrApi(query.trim(), debounceApi = false)
    }

    private fun clearSearch() {
        searchJob?.cancel()
        _searchQuery.value = ""
        if (allCoins.isNotEmpty()) {
            _coins.update {
                CoinsState.Success(
                    coinsList = allCoins,
                    isLoadingMore = isLoadingMoreCoins,
                    hasMorePages = hasMoreCoinsPages
                )
            }
        } else {
            fetchCoins()
        }
    }

    private fun onTrendingSearchQueryChanged(query: String) {
        _trendingSearchQuery.value = query
        searchTrendingLocalOrApi(query.trim(), debounceApi = true)
    }

    private fun onExecuteTrendingSearch(query: String) {
        _trendingSearchQuery.value = query
        searchTrendingLocalOrApi(query.trim(), debounceApi = false)
    }

    private fun clearTrendingSearch() {
        trendingSearchJob?.cancel()
        _trendingSearchQuery.value = ""
        if (allTrendingCoins.isNotEmpty()) {
            _trendingCoins.update {
                CoinsState.Success(
                    coinsList = allTrendingCoins,
                    isLoadingMore = isLoadingMoreTrending,
                    hasMorePages = hasMoreTrendingPages
                )
            }
        } else {
            fetchTrendingCoins()
        }
    }

    private fun searchLocalOrApi(
        trimmedQuery: String,
        debounceApi: Boolean,
        showLoading: Boolean = true
    ) {
        searchJob?.cancel()
        if (trimmedQuery.isEmpty()) {
            if (allCoins.isNotEmpty()) {
                _coins.update {
                    CoinsState.Success(
                        coinsList = allCoins,
                        isLoadingMore = isLoadingMoreCoins,
                        hasMorePages = hasMoreCoinsPages
                    )
                }
            } else {
                fetchCoins()
            }
            return
        }

        val localMatches = allCoins.filter { coin ->
            coin.name.contains(trimmedQuery, ignoreCase = true) ||
                coin.symbol.contains(trimmedQuery, ignoreCase = true) ||
                coin.id.contains(trimmedQuery, ignoreCase = true)
        }

        if (localMatches.isNotEmpty()) {
            _coins.update { CoinsState.Success(localMatches, isFromSearchApi = false) }
        } else {
            if (showLoading) {
                _coins.update { CoinsState.Loading }
            }
            searchJob = viewModelScope.launch {
                if (debounceApi) {
                    delay(500L)
                }
                searchUseCase.searchCoins(trimmedQuery)
                    .onFailure {
                        _coins.update { CoinsState.Error }
                    }
                    .onSuccess { apiResults ->
                        if (apiResults.isNotEmpty()) {
                            val mappedCoins = mapSearchApiResults(apiResults)
                            _coins.update {
                                CoinsState.Success(mappedCoins, isFromSearchApi = true)
                            }
                        } else {
                            _coins.update { CoinsState.EmptySearch }
                        }
                    }
            }
        }
    }

    private fun searchTrendingLocalOrApi(
        trimmedQuery: String,
        debounceApi: Boolean,
        showLoading: Boolean = true
    ) {
        trendingSearchJob?.cancel()
        if (trimmedQuery.isEmpty()) {
            if (allTrendingCoins.isNotEmpty()) {
                _trendingCoins.update {
                    CoinsState.Success(
                        coinsList = allTrendingCoins,
                        isLoadingMore = isLoadingMoreTrending,
                        hasMorePages = hasMoreTrendingPages
                    )
                }
            } else {
                fetchTrendingCoins()
            }
            return
        }

        val localMatches = allTrendingCoins.filter { coin ->
            coin.name.contains(trimmedQuery, ignoreCase = true) ||
                coin.symbol.contains(trimmedQuery, ignoreCase = true) ||
                coin.id.contains(trimmedQuery, ignoreCase = true)
        }

        if (localMatches.isNotEmpty()) {
            _trendingCoins.update { CoinsState.Success(localMatches, isFromSearchApi = false) }
        } else {
            if (showLoading) {
                _trendingCoins.update { CoinsState.Loading }
            }
            trendingSearchJob = viewModelScope.launch {
                if (debounceApi) {
                    delay(500L)
                }
                searchUseCase.searchCoins(trimmedQuery)
                    .onFailure {
                        _trendingCoins.update { CoinsState.Error }
                    }
                    .onSuccess { apiResults ->
                        if (apiResults.isNotEmpty()) {
                            val mappedCoins = mapSearchApiResults(apiResults)
                            _trendingCoins.update {
                                CoinsState.Success(mappedCoins, isFromSearchApi = true)
                            }
                        } else {
                            _trendingCoins.update { CoinsState.EmptySearch }
                        }
                    }
            }
        }
    }

    private fun mapSearchApiResults(apiResults: List<br.com.terras.app.search.domain.model.CoinVO>): List<CoinVO> {
        return apiResults.take(20).map { searchCoin ->
            CoinVO(
                id = searchCoin.id,
                symbol = searchCoin.symbol,
                name = searchCoin.name,
                image = searchCoin.thumb,
                marketCapRank = "",
                price = "",
                priceChangePercentage = "",
                trendColor = TrendColor.NONE
            )
        }
    }
}