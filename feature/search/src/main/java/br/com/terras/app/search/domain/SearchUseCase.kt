package br.com.terras.app.search.domain

import br.com.terras.app.search.data.SearchRepository
import br.com.terras.app.search.domain.model.CoinVO
import javax.inject.Inject

interface SearchUseCase {
    suspend fun searchCoins(query: String): Result<List<CoinVO>>
}

class SearchUseCaseImpl @Inject constructor(
    private val repository: SearchRepository,
    private val mapper: SearchMapper
) : SearchUseCase {

    override suspend fun searchCoins(query: String): Result<List<CoinVO>> {
        return repository.searchCoins(query).map { result ->
            mapper.toSearch(result)
        }
    }
}