package br.com.terras.app.network.cache

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

data class CachedListSnapshot(
    val cacheKey: String,
    val category: String,
    val currency: String,
    val payloadJson: String,
    val fetchedAtMillis: Long,
    val priceUpdatedAtMillis: Long
)

interface ListCacheStore {
    val json: Json

    fun currentTimeMillis(): Long

    fun currentVsCurrency(locale: Locale = Locale.getDefault()): String =
        when (locale.language) {
            "en" -> "usd"
            else -> "brl"
        }

    suspend fun getSnapshot(cacheKey: String): CachedListSnapshot?

    suspend fun putSnapshot(
        cacheKey: String,
        category: String,
        currency: String,
        payloadJson: String,
        fetchedAtMillis: Long = currentTimeMillis(),
        priceUpdatedAtMillis: Long = fetchedAtMillis
    )

    suspend fun updatePricesOnly(
        cacheKey: String,
        updatedPayloadJson: String,
        priceUpdatedAtMillis: Long = currentTimeMillis()
    )

    suspend fun invalidateByPrefix(keyPrefix: String)

    suspend fun invalidateOtherPages(
        category: String,
        currency: String,
        keepKey: String
    )

    suspend fun <T> singleFlight(key: String, block: suspend () -> T): T

    companion object {
        const val CATEGORY_COINS_MARKET = "coins_market"
        const val CATEGORY_TRENDING = "coins_trending"
        const val CATEGORY_FAVORITES = "favorites"
        const val CATEGORY_FEEDS = "feeds"
        const val CATEGORY_GLOBAL_MARKET = "global_market"

        const val PRICE_TTL_MILLIS = 60_000L
        const val COINS_LIST_TTL_MILLIS = 10 * 60_000L
        const val TRENDING_TTL_MILLIS = 10 * 60_000L
        const val FEEDS_TTL_MILLIS = 15 * 60_000L
        const val GLOBAL_MARKET_TTL_MILLIS = 5 * 60_000L
        const val MAX_RETENTION_MILLIS = 24 * 60 * 60_000L
    }
}

inline fun <reified T> ListCacheStore.decodeOrNull(payloadJson: String): T? =
    runCatching { json.decodeFromString<T>(payloadJson) }.getOrNull()

inline fun <reified T> ListCacheStore.encodePayload(value: T): String =
    json.encodeToString(value)

object NoOpListCacheStore : ListCacheStore {
    override val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
    }

    override fun currentTimeMillis(): Long = System.currentTimeMillis()
    override suspend fun getSnapshot(cacheKey: String): CachedListSnapshot? = null
    override suspend fun putSnapshot(
        cacheKey: String,
        category: String,
        currency: String,
        payloadJson: String,
        fetchedAtMillis: Long,
        priceUpdatedAtMillis: Long
    ) = Unit

    override suspend fun updatePricesOnly(
        cacheKey: String,
        updatedPayloadJson: String,
        priceUpdatedAtMillis: Long
    ) = Unit

    override suspend fun invalidateByPrefix(keyPrefix: String) = Unit
    override suspend fun invalidateOtherPages(
        category: String,
        currency: String,
        keepKey: String
    ) = Unit

    override suspend fun <T> singleFlight(key: String, block: suspend () -> T): T = block()
}

@Singleton
class ListCacheStoreImpl @Inject constructor(
    private val dao: ListCacheDao
) : ListCacheStore {

    override val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
    }

    private val singleFlightMutex = Mutex()
    private val inFlight = ConcurrentHashMap<String, CompletableDeferred<Any?>>()

    @Volatile
    private var lastPruneAtMillis: Long = 0L

    override fun currentTimeMillis(): Long = System.currentTimeMillis()

    override suspend fun getSnapshot(cacheKey: String): CachedListSnapshot? {
        maybePruneExpiredEntries()
        return dao.getByKey(cacheKey)?.toSnapshot()
    }

    override suspend fun putSnapshot(
        cacheKey: String,
        category: String,
        currency: String,
        payloadJson: String,
        fetchedAtMillis: Long,
        priceUpdatedAtMillis: Long
    ) {
        maybePruneExpiredEntries()
        dao.upsert(
            ListCacheEntity(
                cacheKey = cacheKey,
                category = category,
                currency = currency,
                payloadJson = payloadJson,
                fetchedAtMillis = fetchedAtMillis,
                priceUpdatedAtMillis = priceUpdatedAtMillis
            )
        )
    }

    override suspend fun updatePricesOnly(
        cacheKey: String,
        updatedPayloadJson: String,
        priceUpdatedAtMillis: Long
    ) {
        val current = dao.getByKey(cacheKey) ?: return
        dao.upsert(
            current.copy(
                payloadJson = updatedPayloadJson,
                priceUpdatedAtMillis = priceUpdatedAtMillis
            )
        )
    }

    override suspend fun invalidateByPrefix(keyPrefix: String) {
        dao.deleteByKeyPrefix(keyPrefix)
    }

    override suspend fun invalidateOtherPages(
        category: String,
        currency: String,
        keepKey: String
    ) {
        dao.deleteOtherEntriesForCategoryAndCurrency(
            category = category,
            currency = currency,
            keepKey = keepKey
        )
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T> singleFlight(key: String, block: suspend () -> T): T {
        var existing: CompletableDeferred<Any?>? = null
        var created: CompletableDeferred<Any?>? = null

        singleFlightMutex.withLock {
            val active = inFlight[key]
            if (active != null && !active.isCompleted) {
                existing = active
            } else {
                val deferred = CompletableDeferred<Any?>()
                inFlight[key] = deferred
                created = deferred
            }
        }

        existing?.let { return it.await() as T }

        val owned = created!!
        return try {
            val result = block()
            owned.complete(result)
            result
        } catch (t: Throwable) {
            owned.completeExceptionally(t)
            throw t
        } finally {
            singleFlightMutex.withLock {
                if (inFlight[key] === owned) {
                    inFlight.remove(key)
                }
            }
        }
    }

    private suspend fun maybePruneExpiredEntries() {
        val now = currentTimeMillis()
        if (now - lastPruneAtMillis < PRUNE_INTERVAL_MILLIS) return
        lastPruneAtMillis = now
        dao.deleteOlderThan(now - ListCacheStore.MAX_RETENTION_MILLIS)
    }

    private fun ListCacheEntity.toSnapshot(): CachedListSnapshot = CachedListSnapshot(
        cacheKey = cacheKey,
        category = category,
        currency = currency,
        payloadJson = payloadJson,
        fetchedAtMillis = fetchedAtMillis,
        priceUpdatedAtMillis = priceUpdatedAtMillis
    )

    private companion object {
        private const val PRUNE_INTERVAL_MILLIS = 30 * 60_000L
    }
}
