package br.com.terras.app.coins.list.domain

import br.com.terras.app.coins.list.data.CoinsRepository
import br.com.terras.app.coins.list.data.model.TrendingCoinWrapper
import br.com.terras.app.coins.list.data.model.TrendingCoinsResponse
import br.com.terras.app.coins.list.domain.model.CoinVO
import br.com.terras.app.coins.list.domain.model.GlobalMarketVO
import javax.inject.Inject

interface CoinsUseCase {
    suspend fun getCoins(page: Int = 1): Result<List<CoinVO>>
    suspend fun getTrendingCoins(page: Int = 1): Result<List<CoinVO>>

    suspend fun getCachedCoins(page: Int = 1): List<CoinVO>? = null
    suspend fun getCoins(page: Int, forceRefresh: Boolean): Result<List<CoinVO>> = getCoins(page)
    suspend fun getCachedTrendingCoins(page: Int = 1): List<CoinVO>? = null
    suspend fun getTrendingCoins(page: Int, forceRefresh: Boolean): Result<List<CoinVO>> =
        getTrendingCoins(page)
    suspend fun getCachedGlobalMarketData(): GlobalMarketVO? = null
    suspend fun getGlobalMarketData(forceRefresh: Boolean = false): Result<GlobalMarketVO> =
        Result.failure(UnsupportedOperationException("Global market data not implemented"))

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

    override suspend fun getCoins(page: Int): Result<List<CoinVO>> =
        getCoins(page = page, forceRefresh = false)

    override suspend fun getCachedCoins(page: Int): List<CoinVO>? {
        val cached = repository.getCachedCoins(page.coerceAtLeast(1)) ?: return null
        return mapper.toCoins(cached)
    }

    override suspend fun getCoins(page: Int, forceRefresh: Boolean): Result<List<CoinVO>> {
        val safePage = page.coerceAtLeast(1)
        val result = if (forceRefresh) {
            repository.getCoins(page = safePage, forceRefresh = true)
        } else {
            repository.getCoins(safePage)
        }
        return result.map { items ->
            mapper.toCoins(items)
        }
    }

    override suspend fun getTrendingCoins(page: Int): Result<List<CoinVO>> =
        getTrendingCoins(page = page, forceRefresh = false)

    override suspend fun getCachedTrendingCoins(page: Int): List<CoinVO>? {
        val safePage = page.coerceAtLeast(1)
        val sourceItems = if (safePage == 1 || cachedTrendingItems.isEmpty()) {
            val cachedTrending = repository.getCachedTrendingCoins() ?: return null
            cachedTrending.coins
                .filter { it.item.id.isNotBlank() }
                .distinctBy { it.item.id }
                .also { cachedTrendingItems = it }
        } else {
            cachedTrendingItems
        }

        val fromIndex = (safePage - 1) * CoinsUseCase.TRENDING_PAGE_SIZE
        if (fromIndex >= sourceItems.size) {
            return emptyList()
        }
        val toIndex = minOf(fromIndex + CoinsUseCase.TRENDING_PAGE_SIZE, sourceItems.size)
        val pagedWrappers = sourceItems.subList(fromIndex, toIndex)
        val ids = pagedWrappers.map { it.item.id }
        val enrichedMarkets = if (ids.isNotEmpty()) {
            repository.getCachedCoinsByIds(ids).orEmpty()
        } else {
            emptyList()
        }

        return mapper.toTrendingCoins(
            trendingResponse = TrendingCoinsResponse(coins = pagedWrappers),
            marketCoins = enrichedMarkets
        )
    }

    override suspend fun getTrendingCoins(page: Int, forceRefresh: Boolean): Result<List<CoinVO>> {
        val safePage = page.coerceAtLeast(1)
        val sourceItems = if (forceRefresh || safePage == 1 || cachedTrendingItems.isEmpty()) {
            val trendingResult = if (forceRefresh) {
                repository.getTrendingCoins(forceRefresh = true)
            } else {
                repository.getTrendingCoins()
            }
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
            val enrichedResult = if (forceRefresh) {
                repository.getCoinsByIds(ids, forceRefresh = true)
            } else {
                repository.getCoinsByIds(ids)
            }
            enrichedResult.getOrDefault(emptyList())
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

    override suspend fun getCachedGlobalMarketData(): GlobalMarketVO? {
        val cached = repository.getCachedGlobalMarketData() ?: return null
        return mapper.toGlobalMarket(cached)
    }

    override suspend fun getGlobalMarketData(forceRefresh: Boolean): Result<GlobalMarketVO> {
        return repository.getGlobalMarketData(forceRefresh = forceRefresh).map { response ->
            mapper.toGlobalMarket(response)
        }
    }
}