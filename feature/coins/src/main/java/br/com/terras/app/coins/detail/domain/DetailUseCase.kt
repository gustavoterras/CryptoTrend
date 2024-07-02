package br.com.terras.app.coins.detail.domain

import br.com.terras.app.coins.detail.data.DetailRepository
import br.com.terras.app.coins.detail.domain.model.CoinChartDetailVO
import br.com.terras.app.coins.detail.domain.model.CoinDetailVO
import javax.inject.Inject

interface DetailUseCase {
    suspend fun getDetailById(id: String): Result<CoinDetailVO>
    suspend fun getChartDetailById(id: String, days: Int): Result<CoinChartDetailVO>
}

class DetailUseCaseImpl @Inject constructor(
    private val repository: DetailRepository,
    private val mapper: DetailMapper
) : DetailUseCase {

    override suspend fun getDetailById(id: String): Result<CoinDetailVO> {
        return repository.getDetailById(id).map { result ->
            mapper.toCoin(result)
        }
    }

    override suspend fun getChartDetailById(id: String, days: Int): Result<CoinChartDetailVO> {
        return repository.getChartDetailById(id, days).map { result ->
            mapper.toChart(result)
        }
    }
}