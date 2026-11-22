package br.com.terras.app.coins.list.domain

import br.com.terras.app.coins.list.data.CoinsRepository
import br.com.terras.app.coins.list.data.model.TrendingCoinWrapper
import br.com.terras.app.coins.list.data.model.TrendingCoinsResponse
import br.com.terras.app.coins.list.domain.model.CoinVO
import javax.inject.Inject

interface CoinsUseCase {
    suspend fun getCoins(page: Int = 1): Result<List<CoinVO>>
    suspend fun getTrendingCoins(page: Int = 1): Result<List<CoinVO>>

    companion object {
        const val CRYPTO_PAGE_SIZE = 20
        const val TRENDING_PAGE_SIZE = 7
    }
}

class CoinsUseCaseImpl @Inject constructor(
    private val repository: CoinsRepository,
    private val mapper: CoinsMapper
) : CoinsUseCase {

    private var cachedTrendingItems: List<TrendingCoinWrapper> = emptyList()

    override suspend fun getCoins(page: Int): Result<List<CoinVO>> {
        return repository.getCoins(page.coerceAtLeast(1)).map { result ->
            mapper.toCoins(result)
        }
    }

    override suspend fun getTrendingCoins(page: Int): Result<List<CoinVO>> {
        val safePage = page.coerceAtLeast(1)
        val sourceItems = if (safePage == 1 || cachedTrendingItems.isEmpty()) {
            val trendingResult = repository.getTrendingCoins()
            val trendingResponse = trendingResult.getOrElse { error ->
                return Result.failure(error)
            }
            trendingResponse.coins
                .filter { it.item.id.isNotBlank() }
                .distinctBy { it.item.id }
                .also { cachedTrendingItems = it }
        } else {
            cachedTrendingItems
        }

        val fromIndex = (safePage - 1) * CoinsUseCase.TRENDING_PAGE_SIZE
        if (fromIndex >= sourceItems.size) {
            return Result.success(emptyList())
        }
        val toIndex = minOf(fromIndex + CoinsUseCase.TRENDING_PAGE_SIZE, sourceItems.size)
        val pagedWrappers = sourceItems.subList(fromIndex, toIndex)

        val ids = pagedWrappers.map { it.item.id }
        val enrichedMarkets = if (ids.isNotEmpty()) {
            repository.getCoinsByIds(ids).getOrDefault(emptyList())
        } else {
            emptyList()
        }

        return Result.success(
            mapper.toTrendingCoins(
                trendingResponse = TrendingCoinsResponse(coins = pagedWrappers),
                marketCoins = enrichedMarkets
            )
        )
    }
}