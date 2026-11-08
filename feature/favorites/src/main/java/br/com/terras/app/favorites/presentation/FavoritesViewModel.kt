package br.com.terras.app.favorites.presentation

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.terras.app.favorites.domain.FavoritesUseCase
import br.com.terras.app.favorites.domain.model.CoinVO
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val useCase: FavoritesUseCase
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

    init {
        handleIntent(FavoritesIntent.LoadFavorites)
    }

    fun handleIntent(intent: FavoritesIntent) {
        when (intent) {
            is FavoritesIntent.LoadFavorites -> getFavorites()
            is FavoritesIntent.Refresh -> getFavorites()
        }
    }

    @VisibleForTesting
    fun getFavorites() {
        _coins.update { FavoritesState.Loading }
        viewModelScope.launch {
            useCase.getCoinsByIds(listOf("bitcoin", "ethereum", "cardano"))
                .onFailure {
                    _coins.update {
                        FavoritesState.Error
                    }
                }
                .onSuccess { result ->
                    _coins.update {
                        FavoritesState.Success(result)
                    }
                }
        }
    }
}