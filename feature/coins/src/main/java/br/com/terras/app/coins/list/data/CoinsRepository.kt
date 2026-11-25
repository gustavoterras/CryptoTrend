package br.com.terras.app.coins.list.data

import br.com.terras.app.coins.list.data.model.CoinsResponse
import br.com.terras.app.coins.list.data.model.FearAndGreedIndexResponse
import br.com.terras.app.coins.list.data.model.GlobalMarketResponse
import br.com.terras.app.coins.list.data.model.TrendingCoinsResponse
import br.com.terras.app.coins.list.network.ApiServiceCoins
import br.com.terras.app.network.cache.CoinGeckoSimplePriceService
import br.com.terras.app.network.cache.ListCacheStore
import br.com.terras.app.network.cache.NoOpCoinGeckoSimplePriceService
import br.com.terras.app.network.cache.NoOpListCacheStore
import br.com.terras.app.network.cache.decodeOrNull
import br.com.terras.app.network.cache.encodePayload
import javax.inject.Inject

interface CoinsRepository {
    suspend fun getCoins(page: Int = 1): Result<List<CoinsResponse>>
    suspend fun getTrendingCoins(): Result<TrendingCoinsResponse>
    suspend fun getCoinsByIds(ids: List<String>): Result<List<CoinsResponse>>

    suspend fun getCachedCoins(page: Int = 1): List<CoinsResponse>? = null
    suspend fun getCoins(page: Int, forceRefresh: Boolean): Result<List<CoinsResponse>> = getCoins(page)
    suspend fun getCachedTrendingCoins(): TrendingCoinsResponse? = null
    suspend fun getTrendingCoins(forceRefresh: Boolean): Result<TrendingCoinsResponse> = getTrendingCoins()
    suspend fun getCachedCoinsByIds(ids: List<String>): List<CoinsResponse>? = null
    suspend fun getCoinsByIds(ids: List<String>, forceRefresh: Boolean): Result<List<CoinsResponse>> =
        getCoinsByIds(ids)
    suspend fun getCachedGlobalMarketData(): GlobalMarketResponse? = null
    suspend fun getGlobalMarketData(forceRefresh: Boolean = false): Result<GlobalMarketResponse> =
        Result.failure(UnsupportedOperationException("Global market data not implemented"))
}

class CoinsRepositoryImpl @Inject constructor(
    private val api: ApiServiceCoins,
    private val simplePriceService: CoinGeckoSimplePriceService = NoOpCoinGeckoSimplePriceService,
    private val cacheStore: ListCacheStore = NoOpListCacheStore
) : CoinsRepository {

    override suspend fun getCoins(page: Int): Result<List<CoinsResponse>> =
        getCoins(page = page, forceRefresh = false)

    override suspend fun getCachedCoins(page: Int): List<CoinsResponse>? {
        val safePage = page.coerceAtLeast(1)
        val currency = cacheStore.currentVsCurrency()
        val key = coinsMarketKey(safePage, currency)
        val snapshot = cacheStore.getSnapshot(key) ?: return null
        return cacheStore.decodeOrNull<List<CoinsResponse>>(snapshot.payloadJson)
    }

    override suspend fun getCoins(page: Int, forceRefresh: Boolean): Result<List<CoinsResponse>> {
        val safePage = page.coerceAtLeast(1)
        val currency = cacheStore.currentVsCurrency()
        val key = coinsMarketKey(safePage, currency)

        if (forceRefresh) {
            return cacheStore.singleFlight("$key:force") {
                if (safePage == 1) {
                    cacheStore.invalidateOtherPages(
                        category = ListCacheStore.CATEGORY_COINS_MARKET,
                        currency = currency,
                        keepKey = key
                    )
                }
                fetchAndCacheCoins(safePage, key, currency, fallbackSnapshot = null)
            }
        }

        val snapshot = cacheStore.getSnapshot(key)
        val cachedList = snapshot?.let { cacheStore.decodeOrNull<List<CoinsResponse>>(it.payloadJson) }
        if (snapshot != null && cachedList != null) {
            val now = cacheStore.currentTimeMillis()
            val structuralAge = now - snapshot.fetchedAtMillis
            val priceAge = now - snapshot.priceUpdatedAtMillis

            if (structuralAge <= ListCacheStore.COINS_LIST_TTL_MILLIS) {
                if (priceAge <= ListCacheStore.PRICE_TTL_MILLIS) {
                    return Result.success(cachedList)
                }
                return overlaySimplePrices(
                    key = key,
                    currency = currency,
                    cachedList = cachedList
                )
            }
            return cacheStore.singleFlight(key) {
                fetchAndCacheCoins(safePage, key, currency, fallbackSnapshot = cachedList)
            }
        }

        return cacheStore.singleFlight(key) {
            fetchAndCacheCoins(safePage, key, currency, fallbackSnapshot = null)
        }
    }

    override suspend fun getTrendingCoins(): Result<TrendingCoinsResponse> =
        getTrendingCoins(forceRefresh = false)

    override suspend fun getCachedTrendingCoins(): TrendingCoinsResponse? {
        val snapshot = cacheStore.getSnapshot(TRENDING_CACHE_KEY) ?: return null
        return cacheStore.decodeOrNull<TrendingCoinsResponse>(snapshot.payloadJson)
    }

    override suspend fun getTrendingCoins(forceRefresh: Boolean): Result<TrendingCoinsResponse> {
        if (forceRefresh) {
            return cacheStore.singleFlight("$TRENDING_CACHE_KEY:force") {
                fetchAndCacheTrending(fallbackSnapshot = null)
            }
        }

        val snapshot = cacheStore.getSnapshot(TRENDING_CACHE_KEY)
        val cached = snapshot?.let { cacheStore.decodeOrNull<TrendingCoinsResponse>(it.payloadJson) }
        if (snapshot != null && cached != null) {
            val age = cacheStore.currentTimeMillis() - snapshot.fetchedAtMillis
            if (age <= ListCacheStore.TRENDING_TTL_MILLIS) {
                return Result.success(cached)
            }
            return cacheStore.singleFlight(TRENDING_CACHE_KEY) {
                fetchAndCacheTrending(fallbackSnapshot = cached)
            }
        }

        return cacheStore.singleFlight(TRENDING_CACHE_KEY) {
            fetchAndCacheTrending(fallbackSnapshot = null)
        }
    }

    override suspend fun getCoinsByIds(ids: List<String>): Result<List<CoinsResponse>> =
        getCoinsByIds(ids = ids, forceRefresh = false)

    override suspend fun getCachedCoinsByIds(ids: List<String>): List<CoinsResponse>? {
        val normalized = normalizeIds(ids)
        if (normalized.isEmpty()) return emptyList()
        val currency = cacheStore.currentVsCurrency()
        val key = coinsByIdsKey(normalized, currency)
        val snapshot = cacheStore.getSnapshot(key) ?: return null
        return cacheStore.decodeOrNull<List<CoinsResponse>>(snapshot.payloadJson)
    }

    override suspend fun getCoinsByIds(
        ids: List<String>,
        forceRefresh: Boolean
    ): Result<List<CoinsResponse>> {
        val normalized = normalizeIds(ids)
        if (normalized.isEmpty()) return Result.success(emptyList())

        val currency = cacheStore.currentVsCurrency()
        val key = coinsByIdsKey(normalized, currency)

        if (forceRefresh) {
            return cacheStore.singleFlight("$key:force") {
                fetchAndCacheCoinsByIds(ids, key, currency, fallbackSnapshot = null)
            }
        }

        val snapshot = cacheStore.getSnapshot(key)
        val cached = snapshot?.let { cacheStore.decodeOrNull<List<CoinsResponse>>(it.payloadJson) }
        if (snapshot != null && cached != null) {
            val now = cacheStore.currentTimeMillis()
            val structuralAge = now - snapshot.fetchedAtMillis
            val priceAge = now - snapshot.priceUpdatedAtMillis
            if (structuralAge <= ListCacheStore.COINS_LIST_TTL_MILLIS) {
                if (priceAge <= ListCacheStore.PRICE_TTL_MILLIS) {
                    return Result.success(cached)
                }
                return overlaySimplePrices(
                    key = key,
                    currency = currency,
                    cachedList = cached
                )
            }
            return cacheStore.singleFlight(key) {
                fetchAndCacheCoinsByIds(ids, key, currency, fallbackSnapshot = cached)
            }
        }

        return cacheStore.singleFlight(key) {
            fetchAndCacheCoinsByIds(ids, key, currency, fallbackSnapshot = null)
        }
    }

    override suspend fun getCachedGlobalMarketData(): GlobalMarketResponse? {
        val snapshot = cacheStore.getSnapshot(GLOBAL_MARKET_CACHE_KEY) ?: return null
        return cacheStore.decodeOrNull<GlobalMarketResponse>(snapshot.payloadJson)
    }

    override suspend fun getGlobalMarketData(forceRefresh: Boolean): Result<GlobalMarketResponse> {
        if (forceRefresh) {
            return cacheStore.singleFlight("$GLOBAL_MARKET_CACHE_KEY:force") {
                fetchAndCacheGlobalMarketData(fallbackSnapshot = null)
            }
        }

        val snapshot = cacheStore.getSnapshot(GLOBAL_MARKET_CACHE_KEY)
        val cached = snapshot?.let { cacheStore.decodeOrNull<GlobalMarketResponse>(it.payloadJson) }
        if (snapshot != null && cached != null) {
            val age = cacheStore.currentTimeMillis() - snapshot.fetchedAtMillis
            if (age <= ListCacheStore.GLOBAL_MARKET_TTL_MILLIS) {
                return Result.success(cached)
            }
            return cacheStore.singleFlight(GLOBAL_MARKET_CACHE_KEY) {
                fetchAndCacheGlobalMarketData(fallbackSnapshot = cached)
            }
        }

        return cacheStore.singleFlight(GLOBAL_MARKET_CACHE_KEY) {
            fetchAndCacheGlobalMarketData(fallbackSnapshot = null)
        }
    }

    private suspend fun fetchAndCacheCoins(
        page: Int,
        key: String,
        currency: String,
        fallbackSnapshot: List<CoinsResponse>?
    ): Result<List<CoinsResponse>> {
        return try {
            val response = api.getCoins(page)
            cacheStore.putSnapshot(
                cacheKey = key,
                category = ListCacheStore.CATEGORY_COINS_MARKET,
                currency = currency,
                payloadJson = cacheStore.encodePayload(response)
            )
            Result.success(response)
        } catch (e: Exception) {
            if (fallbackSnapshot != null) {
                Result.success(fallbackSnapshot)
            } else {
                Result.failure(e)
            }
        }
    }

    private suspend fun fetchAndCacheTrending(
        fallbackSnapshot: TrendingCoinsResponse?
    ): Result<TrendingCoinsResponse> {
        return try {
            val response = api.getTrendingCoins()
            cacheStore.putSnapshot(
                cacheKey = TRENDING_CACHE_KEY,
                category = ListCacheStore.CATEGORY_TRENDING,
                currency = "global",
                payloadJson = cacheStore.encodePayload(response)
            )
            Result.success(response)
        } catch (e: Exception) {
            if (fallbackSnapshot != null) {
                Result.success(fallbackSnapshot)
            } else {
                Result.failure(e)
            }
        }
    }

    private suspend fun fetchAndCacheCoinsByIds(
        ids: List<String>,
        key: String,
        currency: String,
        fallbackSnapshot: List<CoinsResponse>?
    ): Result<List<CoinsResponse>> {
        return try {
            val response = api.getCoinsByIds(ids)
            cacheStore.putSnapshot(
                cacheKey = key,
                category = ListCacheStore.CATEGORY_TRENDING,
                currency = currency,
                payloadJson = cacheStore.encodePayload(response)
            )
            Result.success(response)
        } catch (e: Exception) {
            if (fallbackSnapshot != null) {
                Result.success(fallbackSnapshot)
            } else {
                Result.failure(e)
            }
        }
    }

    private suspend fun fetchAndCacheGlobalMarketData(
        fallbackSnapshot: GlobalMarketResponse?
    ): Result<GlobalMarketResponse> {
        return try {
            val rawResponse = api.getGlobalMarketData()
            val enrichedResponse = if (rawResponse.data.fearAndGreedIndex?.value != null) {
                rawResponse
            } else {
                val fngFallback = runCatching { fetchFallbackFearAndGreedIndex() }.getOrNull()
                if (fngFallback != null) {
                    rawResponse.copy(
                        data = rawResponse.data.copy(fearAndGreedIndex = fngFallback)
                    )
                } else {
                    rawResponse
                }
            }
            cacheStore.putSnapshot(
                cacheKey = GLOBAL_MARKET_CACHE_KEY,
                category = ListCacheStore.CATEGORY_GLOBAL_MARKET,
                currency = "global",
                payloadJson = cacheStore.encodePayload(enrichedResponse)
            )
            Result.success(enrichedResponse)
        } catch (e: Exception) {
            if (fallbackSnapshot != null) {
                Result.success(fallbackSnapshot)
            } else {
                val fngFallback = runCatching { fetchFallbackFearAndGreedIndex() }.getOrNull()
                if (fngFallback != null) {
                    Result.success(
                        GlobalMarketResponse(
                            data = br.com.terras.app.coins.list.data.model.GlobalMarketDataResponse(
                                fearAndGreedIndex = fngFallback
                            )
                        )
                    )
                } else {
                    Result.failure(e)
                }
            }
        }
    }

    private suspend fun fetchFallbackFearAndGreedIndex(): FearAndGreedIndexResponse? {
        val entry = api.getFallbackFearAndGreed().data.firstOrNull() ?: return null
        val score = entry.value.trim().toIntOrNull()?.coerceIn(0, 100) ?: return null
        return FearAndGreedIndexResponse(
            value = score,
            sentiment = entry.valueClassification
        )
    }

    private suspend fun overlaySimplePrices(
        key: String,
        currency: String,
        cachedList: List<CoinsResponse>
    ): Result<List<CoinsResponse>> {
        return cacheStore.singleFlight("$key:price_overlay") {
            val updatedList = runCatching {
                val ids = cachedList.map { it.id }
                val quotes = simplePriceService.getSimplePrices(ids, currency)
                if (quotes.isEmpty()) {
                    cachedList
                } else {
                    cachedList.map { coin ->
                        val fresh = quotes[coin.id] ?: return@map coin
                        coin.copy(
                            price = fresh.price,
                            priceChangePercentage = fresh.change24h ?: coin.priceChangePercentage
                        )
                    }.also { merged ->
                        cacheStore.updatePricesOnly(
                            cacheKey = key,
                            updatedPayloadJson = cacheStore.encodePayload(merged),
                            priceUpdatedAtMillis = cacheStore.currentTimeMillis()
                        )
                    }
                }
            }.getOrDefault(cachedList)
            Result.success(updatedList)
        }
    }

    private fun normalizeIds(ids: List<String>): List<String> =
        ids.asSequence()
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()
            .toList()

    private fun coinsMarketKey(page: Int, currency: String): String =
        "coins_market:page_${page}:$currency"

    private fun coinsByIdsKey(sortedIds: List<String>, currency: String): String =
        "coins_by_ids:${sortedIds.joinToString(",")}:$currency"

    private companion object {
        private const val TRENDING_CACHE_KEY = "coins_trending:global"
        private const val GLOBAL_MARKET_CACHE_KEY = "global_market:global"
    }
}