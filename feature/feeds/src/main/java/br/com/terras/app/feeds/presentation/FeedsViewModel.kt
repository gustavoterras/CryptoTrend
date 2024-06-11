package br.com.terras.app.feeds.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.terras.app.feeds.domain.FeedsUseCaseImpl
import br.com.terras.app.feeds.domain.model.ArticleVO
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedsViewModel @Inject constructor(
    private val useCase: FeedsUseCaseImpl
) : ViewModel() {

    private val _feeds: MutableStateFlow<List<ArticleVO>> = MutableStateFlow(emptyList())
    val feeds: StateFlow<List<ArticleVO>> = _feeds

    init {
        getFeeds()
    }

    private fun getFeeds() {
        viewModelScope.launch {
            useCase.getFeeds("bitcoin").onFailure {
                Log.e("TAG", "getFeeds: ", it)
            }.onSuccess { result ->
                _feeds.update {
                    result
                }
            }
        }
    }
}