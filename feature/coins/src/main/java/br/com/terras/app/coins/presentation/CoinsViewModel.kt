package br.com.terras.app.coins.presentation

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.terras.app.coins.domain.CoinsUseCase
import br.com.terras.app.coins.domain.model.CoinVO
import br.com.terras.app.coins.presentation.CoinsViewModel.CoinsState.Error
import br.com.terras.app.coins.presentation.CoinsViewModel.CoinsState.Loading
import br.com.terras.app.coins.presentation.CoinsViewModel.CoinsState.Success
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

    private val _coins: MutableStateFlow<CoinsState> = MutableStateFlow(Loading)
    val coins: StateFlow<CoinsState> = _coins.asStateFlow()

    init {
        getCoins()
    }

    @VisibleForTesting
    fun getCoins() {
        viewModelScope.launch {
            useCase.getCoins()
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

    interface CoinsState {
        data object Loading : CoinsState
        data class Success(val coinsList: List<CoinVO>) : CoinsState
        data object Error : CoinsState
    }
}