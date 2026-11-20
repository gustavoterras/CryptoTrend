package br.com.terras.app.coins.list.domain

import br.com.terras.app.coins.list.data.CoinsRepository
import br.com.terras.app.coins.list.domain.model.CoinVO
import javax.inject.Inject

interface CoinsUseCase {
    suspend fun getCoins(): Result<List<CoinVO>>
    suspend fun getTrendingCoins(): Result<List<CoinVO>>
}

class CoinsUseCaseImpl @Inject constructor(
    private val repository: CoinsRepository,
    private val mapper: CoinsMapper
) : CoinsUseCase {

    override suspend fun getCoins(): Result<List<CoinVO>> {
        return repository.getCoins().map { result ->
            mapper.toCoins(result)
        }
    }

    override suspend fun getTrendingCoins(): Result<List<CoinVO>> {
        return repository.getTrendingCoins().map { trendingResponse ->
            val ids = trendingResponse.coins
                .map { it.item.id }
                .filter { it.isNotBlank() }
                .take(20)
            val enrichedMarkets = if (ids.isNotEmpty()) {
                repository.getCoinsByIds(ids).getOrDefault(emptyList())
            } else {
                emptyList()
            }
            mapper.toTrendingCoins(trendingResponse, enrichedMarkets)
        }
    }
}