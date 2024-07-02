package br.com.terras.app.coins.detail.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.terras.app.coins.detail.domain.DetailUseCase
import br.com.terras.app.coins.detail.domain.model.CoinChartDetailVO
import br.com.terras.app.coins.detail.domain.model.CoinDetailVO
import br.com.terras.app.coins.detail.presentation.DetailViewModel.DetailState.Error
import br.com.terras.app.coins.detail.presentation.DetailViewModel.DetailState.Loading
import br.com.terras.app.coins.detail.presentation.DetailViewModel.DetailState.SuccessChart
import br.com.terras.app.coins.detail.presentation.DetailViewModel.DetailState.SuccessDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val useCase: DetailUseCase
) : ViewModel() {

    private val _detail: MutableStateFlow<DetailState> = MutableStateFlow(Loading)
    val detail: StateFlow<DetailState> = _detail.asStateFlow()

    private val _chart: MutableStateFlow<DetailState> = MutableStateFlow(Loading)
    val chart: StateFlow<DetailState> = _chart.asStateFlow()

    fun getDetailById(id: String) {
        viewModelScope.launch {
            useCase.getDetailById(id)
                .onSuccess { result ->
                    _detail.update {
                        SuccessDetail(result)
                    }
                }
                .onFailure {
                    _detail.update {
                        Error
                    }
                }
        }
    }

    fun getChartById(id: String, days: Int = 1) {
        viewModelScope.launch {
            useCase.getChartDetailById(id, days)
                .onSuccess { result ->
                    _chart.update {
                        SuccessChart(result)
                    }
                }
                .onFailure {
                    _chart.update {
                        Error
                    }
                }
        }
    }

    interface DetailState {
        data object Loading : DetailState
        data class SuccessDetail(val coin: CoinDetailVO) : DetailState
        data class SuccessChart(val chart: CoinChartDetailVO) : DetailState
        data object Error : DetailState
    }
}