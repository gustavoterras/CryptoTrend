package br.com.terras.app.search.presentation

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.terras.app.search.domain.SearchUseCase
import br.com.terras.app.search.domain.model.CoinVO
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

    sealed interface SearchIntent {
        data class QueryChanged(val query: String) : SearchIntent
        data class ExecuteSearch(val query: String) : SearchIntent
        data object ClearSearch : SearchIntent
    }

    sealed interface SearchState {
        data object Loading : SearchState
        data class Success(val coinsList: List<CoinVO>) : SearchState
        data object Error : SearchState
        data object Empty : SearchState
    }

    private val _coins: MutableStateFlow<SearchState> = MutableStateFlow(SearchState.Success(emptyList()))
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
                    _coins.update { SearchState.Loading }
                    getCoins(it)
                }
        }
    }

    fun handleIntent(intent: SearchIntent) {
        when (intent) {
            is SearchIntent.QueryChanged -> doSearch(intent.query)
            is SearchIntent.ExecuteSearch -> {
                _coins.update { SearchState.Loading }
                getCoins(intent.query)
            }
            is SearchIntent.ClearSearch -> {
                _searchQuery.update { "" }
                _coins.update { SearchState.Success(emptyList()) }
            }
        }
    }

    @VisibleForTesting
    fun getCoins(query: String) {
        viewModelScope.launch {
            useCase.searchCoins(query)
                .onFailure {
                    _coins.update {
                        SearchState.Error
                    }
                }
                .onSuccess { result ->
                    _coins.update {
                        if (result.isNotEmpty()) SearchState.Success(result) else SearchState.Empty
                    }
                }
        }
    }

    fun doSearch(query: String) {
        _searchQuery.update { query }
    }
}