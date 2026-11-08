package br.com.terras.app.feeds.presentation

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.terras.app.feeds.domain.FeedsUseCase
import br.com.terras.app.feeds.domain.model.ArticleVO
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

    sealed interface FeedsIntent {
        data class LoadFeeds(val topic: String = "bitcoin") : FeedsIntent
        data object Refresh : FeedsIntent
    }

    sealed interface FeedsState {
        data object Loading : FeedsState
        data class Success(val coinsList: List<ArticleVO>) : FeedsState
        data object Error : FeedsState
    }

    private val _feeds: MutableStateFlow<FeedsState> = MutableStateFlow(FeedsState.Loading)
    val feeds: StateFlow<FeedsState> = _feeds.asStateFlow()

    init {
        handleIntent(FeedsIntent.LoadFeeds())
    }

    fun handleIntent(intent: FeedsIntent) {
        when (intent) {
            is FeedsIntent.LoadFeeds -> getFeeds(intent.topic)
            is FeedsIntent.Refresh -> getFeeds()
        }
    }

    @VisibleForTesting
    fun getFeeds(topic: String = "bitcoin") {
        _feeds.update { FeedsState.Loading }
        viewModelScope.launch {
            useCase.getFeeds(topic)
                .onFailure {
                    _feeds.update {
                        FeedsState.Error
                    }
                }.onSuccess { result ->
                    _feeds.update {
                        FeedsState.Success(result)
                    }
                }
        }
    }
}