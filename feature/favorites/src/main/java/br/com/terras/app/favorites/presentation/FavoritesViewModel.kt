package br.com.terras.app.favorites.presentation

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.terras.app.favorites.domain.FavoritesUseCase
import br.com.terras.app.favorites.domain.model.CoinVO
import br.com.terras.app.network.favorites.FavoriteCoinsLocalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val useCase: FavoritesUseCase,
    private val favoriteLocalRepository: FavoriteCoinsLocalRepository
) : ViewModel() {

    sealed interface FavoritesIntent {
        data object LoadFavorites : FavoritesIntent
        data object Refresh : FavoritesIntent
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

    private var currentFavoriteIds: List<String> = emptyList()
    private var cachedCoinsById: Map<String, CoinVO> = emptyMap()

    init {
        observeLocalFavorites()
    }

    private fun observeLocalFavorites() {
        viewModelScope.launch {
            favoriteLocalRepository.observeFavoriteIds().collectLatest { ids ->
                currentFavoriteIds = ids
                fetchCoinsForIds(ids, isRefresh = false)
            }
        }
    }

    fun handleIntent(intent: FavoritesIntent) {
        when (intent) {
            is FavoritesIntent.LoadFavorites -> getFavorites()
            is FavoritesIntent.Refresh -> refreshFavorites()
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
            _coins.update { FavoritesState.Success(emptyList()) }
            return
        }

        val immediateCached = ids.mapNotNull { cachedCoinsById[it] }
        if (immediateCached.size == ids.size) {
            _coins.update { FavoritesState.Success(immediateCached) }
        } else if (!isRefresh && immediateCached.isEmpty()) {
            _coins.update { FavoritesState.Loading }
        }

        useCase.getCoinsByIds(ids)
            .onFailure {
                if (immediateCached.isNotEmpty()) {
                    _coins.update { FavoritesState.Success(immediateCached) }
                } else {
                    _coins.update { FavoritesState.Error }
                }
            }
            .onSuccess { result ->
                val resultById = result.associateBy { it.id }
                cachedCoinsById = cachedCoinsById + resultById
                val orderedCoins = ids.mapNotNull { resultById[it] }
                _coins.update { FavoritesState.Success(orderedCoins) }
            }
    }
}