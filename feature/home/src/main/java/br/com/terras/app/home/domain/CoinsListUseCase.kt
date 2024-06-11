package br.com.terras.app.home.domain

import br.com.terras.app.home.data.CoinsListRepository
import br.com.terras.app.home.domain.model.CoinVO
import javax.inject.Inject

interface CoinsListUseCase {
    suspend fun getCoinsList(): Result<List<CoinVO>>
}

class CoinsListUseCaseImpl @Inject constructor(
    private val repository: CoinsListRepository,
    private val mapper: CoinsMapper
) : CoinsListUseCase {

    override suspend fun getCoinsList(
    ): Result<List<CoinVO>> {
        return repository.getCoinsList().map { result ->
            mapper.toCoinsList(result)
        }
    }
}