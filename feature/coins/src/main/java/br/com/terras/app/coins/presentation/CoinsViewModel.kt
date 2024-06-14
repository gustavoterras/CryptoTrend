package br.com.terras.app.coins.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.terras.app.coins.domain.CoinsUseCaseImpl
import br.com.terras.app.coins.domain.model.CoinVO
import br.com.terras.app.coins.presentation.CoinsViewModel.CoinsListState.Error
import br.com.terras.app.coins.presentation.CoinsViewModel.CoinsListState.Loading
import br.com.terras.app.coins.presentation.CoinsViewModel.CoinsListState.Success
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CoinsViewModel @Inject constructor(
    private val useCase: CoinsUseCaseImpl
) : ViewModel() {

    private val _coinsList: MutableStateFlow<CoinsListState> = MutableStateFlow(Loading)
    val coinsList: StateFlow<CoinsListState> = _coinsList.asStateFlow()

    init {
        getUsers()
    }

    private fun getUsers() {
        viewModelScope.launch {
            useCase.getCoins()
                .onFailure {
                    _coinsList.update {
                        Error
                    }
                }
                .onSuccess { result ->
                    _coinsList.update {
                        Success(result)
                    }
                }
        }
    }

    interface CoinsListState {
        data object Loading : CoinsListState
        data class Success(val coinsList: List<CoinVO>) : CoinsListState
        data object Error : CoinsListState
    }
}