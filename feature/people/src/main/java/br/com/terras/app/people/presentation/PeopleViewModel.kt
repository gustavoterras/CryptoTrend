package br.com.terras.app.people.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.terras.app.people.domain.CoinsListUseCaseImpl
import br.com.terras.app.people.domain.model.CoinVO
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class PeopleViewModel @Inject constructor(
    private val useCase: CoinsListUseCaseImpl
) : ViewModel() {

    private val _coinsList: MutableStateFlow<List<CoinVO>> = MutableStateFlow(emptyList())
    val coinsList: StateFlow<List<CoinVO>> = _coinsList

    init {
        getUsers()
    }

    private fun getUsers() {
        viewModelScope.launch {
            useCase.getCoinsList()
                .onFailure {
                    Log.e("TAG", "getCoins: ", it)
                }
                .onSuccess { result ->
                    _coinsList.update {
                        result
                    }
                }
        }
    }
}