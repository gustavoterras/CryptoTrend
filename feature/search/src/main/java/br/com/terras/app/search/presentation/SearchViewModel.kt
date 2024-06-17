package br.com.terras.app.search.presentation

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.terras.app.search.domain.SearchUseCase
import br.com.terras.app.search.domain.model.CoinVO
import br.com.terras.app.search.presentation.SearchViewModel.SearchState.Empty
import br.com.terras.app.search.presentation.SearchViewModel.SearchState.Error
import br.com.terras.app.search.presentation.SearchViewModel.SearchState.Loading
import br.com.terras.app.search.presentation.SearchViewModel.SearchState.Success
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val useCase: SearchUseCase
) : ViewModel() {

    private val _coins: MutableStateFlow<SearchState> = MutableStateFlow(Success(emptyList()))
    val coins: StateFlow<SearchState> = _coins.asStateFlow()
    private val _searchQuery = MutableStateFlow("")

    init {
        initValues()
    }

    private fun initValues() {
        viewModelScope.launch {
            _searchQuery.debounce(1000)
                .distinctUntilChanged()
                .filter { it.isNotEmpty() }
                .collectLatest {
                    _coins.update { Loading }
                    getCoins(it)
                }
        }
    }

    @VisibleForTesting
    fun getCoins(query: String) {
        viewModelScope.launch {
            useCase.searchCoins(query)
                .onFailure {
                    _coins.update {
                        Error
                    }
                }
                .onSuccess { result ->
                    _coins.update {
                        if (result.isNotEmpty()) Success(result) else Empty
                    }
                }
        }
    }

    fun doSearch(query: String) {
        _searchQuery.update { query }
    }

    interface SearchState {
        data object Loading : SearchState
        data class Success(val coinsList: List<CoinVO>) : SearchState
        data object Error : SearchState
        data object Empty : SearchState
    }
}