package br.com.terras.app.feeds.presentation

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.terras.app.feeds.domain.FeedsUseCase
import br.com.terras.app.feeds.domain.model.ArticleVO
import br.com.terras.app.network.refresh.DataRefreshCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedsViewModel @Inject constructor(
    private val useCase: FeedsUseCase,
    private val refreshCoordinator: DataRefreshCoordinator
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

    private val _isRefreshing: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var currentTopic: String = "bitcoin"

    init {
        handleIntent(FeedsIntent.LoadFeeds())
        observeMarketRefresh()
    }

    private fun observeMarketRefresh() {
        viewModelScope.launch {
            refreshCoordinator.refreshEvents.collect {
                refreshFeeds(currentTopic)
            }
        }
    }

    fun handleIntent(intent: FeedsIntent) {
        when (intent) {
            is FeedsIntent.LoadFeeds -> {
                currentTopic = intent.topic
                getFeeds(intent.topic)
            }
            is FeedsIntent.Refresh -> refreshFeeds(currentTopic)
        }
    }

    @VisibleForTesting
    fun getFeeds(topic: String = "bitcoin") {
        currentTopic = topic
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

    private fun refreshFeeds(topic: String = currentTopic) {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                useCase.getFeeds(topic)
                    .onFailure {
                        if (_feeds.value !is FeedsState.Success) {
                            _feeds.update { FeedsState.Error }
                        }
                    }
                    .onSuccess { result ->
                        _feeds.update { FeedsState.Success(result) }
                    }
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}