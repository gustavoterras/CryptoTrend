package br.com.terras.app.favorites.data

import br.com.terras.app.coingecko.price.CoinGeckoSimplePriceService
import br.com.terras.app.coingecko.price.NoOpCoinGeckoSimplePriceService
import br.com.terras.app.common.currentVsCurrency
import br.com.terras.app.database.cache.ListCacheStore
import br.com.terras.app.database.cache.NoOpListCacheStore
import br.com.terras.app.database.cache.decodeOrNull
import br.com.terras.app.database.cache.encodePayload
import br.com.terras.app.favorites.data.model.CoinsResponse
import br.com.terras.app.favorites.network.ApiServiceFavorites
import javax.inject.Inject

interface FavoritesRepository {
    suspend fun getCoinsByIds(ids: List<String>): Result<List<CoinsResponse>>

    suspend fun getCachedCoinsByIds(ids: List<String>): List<CoinsResponse>? = null
    suspend fun getCoinsByIds(ids: List<String>, forceRefresh: Boolean): Result<List<CoinsResponse>> =
        getCoinsByIds(ids)
}

class FavoritesRepositoryImpl @Inject constructor(
    private val api: ApiServiceFavorites,
    private val simplePriceService: CoinGeckoSimplePriceService = NoOpCoinGeckoSimplePriceService,
    private val cacheStore: ListCacheStore = NoOpListCacheStore
) : FavoritesRepository {

    override suspend fun getCoinsByIds(ids: List<String>): Result<List<CoinsResponse>> =
        getCoinsByIds(ids = ids, forceRefresh = false)

    override suspend fun getCachedCoinsByIds(ids: List<String>): List<CoinsResponse>? {
        val normalized = normalizeIds(ids)
        if (normalized.isEmpty()) return emptyList()
        val currency = currentVsCurrency()
        val key = favoritesKey(normalized, currency)
        val snapshot = cacheStore.getSnapshot(key) ?: return null
        return cacheStore.decodeOrNull<List<CoinsResponse>>(snapshot.payloadJson)
    }

    override suspend fun getCoinsByIds(
        ids: List<String>,
        forceRefresh: Boolean
    ): Result<List<CoinsResponse>> {
        val normalized = normalizeIds(ids)
        if (normalized.isEmpty()) return Result.success(emptyList())

        val currency = currentVsCurrency()
        val key = favoritesKey(normalized, currency)

        if (forceRefresh) {
            return cacheStore.singleFlight("$key:force") {
                fetchAndCacheFavorites(ids, key, currency, fallbackSnapshot = null)
            }
        }

        val snapshot = cacheStore.getSnapshot(key)
        val cached = snapshot?.let { cacheStore.decodeOrNull<List<CoinsResponse>>(it.payloadJson) }
        if (snapshot != null && cached != null) {
            val now = cacheStore.currentTimeMillis()
            val structuralAge = now - snapshot.fetchedAtMillis
            val priceAge = now - snapshot.priceUpdatedAtMillis
            if (structuralAge <= FAVORITES_LIST_TTL_MILLIS) {
                if (priceAge <= PRICE_TTL_MILLIS) {
                    return Result.success(cached)
                }
                return overlaySimplePrices(key, currency, cached)
            }
            return cacheStore.singleFlight(key) {
                fetchAndCacheFavorites(ids, key, currency, fallbackSnapshot = cached)
            }
        }

        return cacheStore.singleFlight(key) {
            fetchAndCacheFavorites(ids, key, currency, fallbackSnapshot = null)
        }
    }

    private suspend fun fetchAndCacheFavorites(
        ids: List<String>,
        key: String,
        currency: String,
        fallbackSnapshot: List<CoinsResponse>?
    ): Result<List<CoinsResponse>> {
        return try {
            val response = api.getCoinsByIds(ids.joinToString(","))
            cacheStore.putSnapshot(
                cacheKey = key,
                category = CATEGORY_FAVORITES,
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

    private fun favoritesKey(sortedIds: List<String>, currency: String): String =
        "favorites:${sortedIds.joinToString(",")}:$currency"

    internal companion object {
        const val CATEGORY_FAVORITES = "favorites"
        const val FAVORITES_LIST_TTL_MILLIS = 10 * 60_000L
        const val PRICE_TTL_MILLIS = 60_000L
    }
}