package br.com.terras.app.coins.list.presentation

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.terras.app.coins.list.domain.CoinsUseCase
import br.com.terras.app.coins.list.domain.model.CoinVO
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CoinsViewModel @Inject constructor(
    private val useCase: CoinsUseCase
) : ViewModel() {

    sealed interface CoinsIntent {
        data object LoadCoins : CoinsIntent
        data object Refresh : CoinsIntent
    }

    sealed interface CoinsState {
        data object Loading : CoinsState
        data class Success(val coinsList: List<CoinVO>) : CoinsState
        data object Error : CoinsState
    }

    private val _coins: MutableStateFlow<CoinsState> = MutableStateFlow(CoinsState.Loading)
    val coins: StateFlow<CoinsState> = _coins.asStateFlow()

    init {
        handleIntent(CoinsIntent.LoadCoins)
    }

    fun handleIntent(intent: CoinsIntent) {
        when (intent) {
            is CoinsIntent.LoadCoins -> fetchCoins()
            is CoinsIntent.Refresh -> fetchCoins()
        }
    }

    @VisibleForTesting
    fun getCoins() = fetchCoins()

    private fun fetchCoins() {
        _coins.update { CoinsState.Loading }
        viewModelScope.launch {
            useCase.getCoins()
                .onFailure {
                    _coins.update {
                        CoinsState.Error
                    }
                }
                .onSuccess { result ->
                    _coins.update {
                        CoinsState.Success(result)
                    }
                }
        }
    }
}