package br.com.terras.app.favorites.presentation

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.terras.app.favorites.domain.FavoritesUseCase
import br.com.terras.app.favorites.domain.model.CoinVO
import br.com.terras.app.favorites.presentation.FavoritesViewModel.FavoritesState.Error
import br.com.terras.app.favorites.presentation.FavoritesViewModel.FavoritesState.Success
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

    private val _coins: MutableStateFlow<FavoritesState> = MutableStateFlow(FavoritesState.Loading)
    val coins: StateFlow<FavoritesState> = _coins.asStateFlow()

    init {
        getFavorites()
    }

    @VisibleForTesting
    fun getFavorites() {
        viewModelScope.launch {
            useCase.getCoinsByIds(listOf("bitcoin", "ethereum", "cardano"))
                .onFailure {
                    _coins.update {
                        Error
                    }
                }
                .onSuccess { result ->
                    _coins.update {
                        Success(result)
                    }
                }
        }
    }

    interface FavoritesState {
        data object Loading : FavoritesState
        data class Success(val coinsList: List<CoinVO>) : FavoritesState
        data object Error : FavoritesState
    }
}