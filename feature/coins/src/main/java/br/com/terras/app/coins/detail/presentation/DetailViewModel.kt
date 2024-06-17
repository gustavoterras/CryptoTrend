package br.com.terras.app.coins.detail.presentation

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.terras.app.coins.detail.presentation.DetailViewModel.DetailState.Loading
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Objects
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    //private val useCase: CoinsUseCase
) : ViewModel() {

    private val _coins: MutableStateFlow<DetailState> = MutableStateFlow(Loading)
    val coins: StateFlow<DetailState> = _coins.asStateFlow()

    init {
        getCoins()
    }

    @VisibleForTesting
    fun getCoins() {
        viewModelScope.launch {
//            useCase.getCoins()
//                .onFailure {
//                    _coins.update {
//                        Error
//                    }
//                }
//                .onSuccess { result ->
//                    _coins.update {
//                        Success(result)
//                    }
//                }
        }
    }

    interface DetailState {
        data object Loading : DetailState
        data class Success(val coinsList: List<Objects>) : DetailState
        data object Error : DetailState
    }
}