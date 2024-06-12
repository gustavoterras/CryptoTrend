package br.com.terras.app.people.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.terras.app.people.domain.CoinsListUseCaseImpl
import br.com.terras.app.people.domain.model.CoinVO
import br.com.terras.app.people.presentation.PeopleViewModel.CoinsListState.Error
import br.com.terras.app.people.presentation.PeopleViewModel.CoinsListState.Loading
import br.com.terras.app.people.presentation.PeopleViewModel.CoinsListState.Success
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class PeopleViewModel @Inject constructor(
    private val useCase: CoinsListUseCaseImpl
) : ViewModel() {

    private val _coinsList: MutableStateFlow<CoinsListState> = MutableStateFlow(Loading)
    val coinsList: StateFlow<CoinsListState> = _coinsList.asStateFlow()

    init {
        getUsers()
    }

    private fun getUsers() {
        viewModelScope.launch {
            useCase.getCoinsList()
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