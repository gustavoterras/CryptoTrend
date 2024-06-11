package br.com.terras.app.people.domain

import br.com.terras.app.people.data.CoinsListRepository
import br.com.terras.app.people.domain.model.CoinVO
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