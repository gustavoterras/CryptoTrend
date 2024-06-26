package br.com.terras.app.feeds.presentation

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.terras.app.feeds.domain.FeedsUseCase
import br.com.terras.app.feeds.domain.model.ArticleVO
import br.com.terras.app.feeds.presentation.FeedsViewModel.FeedsState.Error
import br.com.terras.app.feeds.presentation.FeedsViewModel.FeedsState.Loading
import br.com.terras.app.feeds.presentation.FeedsViewModel.FeedsState.Success
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedsViewModel @Inject constructor(
    private val useCase: FeedsUseCase
) : ViewModel() {

    private val _feeds: MutableStateFlow<FeedsState> = MutableStateFlow(Loading)
    val feeds: StateFlow<FeedsState> = _feeds.asStateFlow()

    init {
        getFeeds()
    }

    @VisibleForTesting
    fun getFeeds() {
        viewModelScope.launch {
            useCase.getFeeds("bitcoin")
                .onFailure {
                    _feeds.update {
                        Error
                    }
                }.onSuccess { result ->
                    _feeds.update {
                        Success(result)
                    }
                }
        }
    }

    interface FeedsState {
        data object Loading : FeedsState
        data class Success(val coinsList: List<ArticleVO>) : FeedsState
        data object Error : FeedsState
    }
}