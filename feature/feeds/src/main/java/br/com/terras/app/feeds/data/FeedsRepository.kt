package br.com.terras.app.feeds.data

import br.com.terras.app.feeds.data.model.FeedsResponse
import br.com.terras.app.feeds.network.ApiServicesFeeds
import br.com.terras.app.network.cache.ListCacheStore
import br.com.terras.app.network.cache.NoOpListCacheStore
import br.com.terras.app.network.cache.decodeOrNull
import br.com.terras.app.network.cache.encodePayload
import javax.inject.Inject

interface FeedsRepository {
    suspend fun getFeeds(
        query: String,
        fromDate: String,
        language: String
    ): Result<FeedsResponse>

    suspend fun getCachedFeeds(
        query: String,
        fromDate: String,
        language: String
    ): FeedsResponse? = null

    suspend fun getFeeds(
        query: String,
        fromDate: String,
        language: String,
        forceRefresh: Boolean
    ): Result<FeedsResponse> = getFeeds(query, fromDate, language)
}

class FeedsRepositoryImpl @Inject constructor(
    private val api: ApiServicesFeeds,
    private val cacheStore: ListCacheStore = NoOpListCacheStore
) : FeedsRepository {

    override suspend fun getFeeds(
        query: String,
        fromDate: String,
        language: String
    ): Result<FeedsResponse> =
        getFeeds(query = query, fromDate = fromDate, language = language, forceRefresh = false)

    override suspend fun getCachedFeeds(
        query: String,
        fromDate: String,
        language: String
    ): FeedsResponse? {
        val key = feedsKey(query, fromDate, language)
        val snapshot = cacheStore.getSnapshot(key) ?: return null
        return cacheStore.decodeOrNull<FeedsResponse>(snapshot.payloadJson)
    }

    override suspend fun getFeeds(
        query: String,
        fromDate: String,
        language: String,
        forceRefresh: Boolean
    ): Result<FeedsResponse> {
        val key = feedsKey(query, fromDate, language)
        if (forceRefresh) {
            return cacheStore.singleFlight("$key:force") {
                fetchAndCacheFeeds(query, fromDate, language, key, fallbackSnapshot = null)
            }
        }

        val snapshot = cacheStore.getSnapshot(key)
        val cached = snapshot?.let { cacheStore.decodeOrNull<FeedsResponse>(it.payloadJson) }
        if (snapshot != null && cached != null) {
            val age = cacheStore.currentTimeMillis() - snapshot.fetchedAtMillis
            if (age <= ListCacheStore.FEEDS_TTL_MILLIS) {
                return Result.success(cached)
            }
            return cacheStore.singleFlight(key) {
                fetchAndCacheFeeds(query, fromDate, language, key, fallbackSnapshot = cached)
            }
        }

        return cacheStore.singleFlight(key) {
            fetchAndCacheFeeds(query, fromDate, language, key, fallbackSnapshot = null)
        }
    }

    private suspend fun fetchAndCacheFeeds(
        query: String,
        fromDate: String,
        language: String,
        key: String,
        fallbackSnapshot: FeedsResponse?
    ): Result<FeedsResponse> {
        return try {
            val response = api.getFeeds(query, fromDate, language)
            cacheStore.putSnapshot(
                cacheKey = key,
                category = ListCacheStore.CATEGORY_FEEDS,
                currency = language.lowercase(),
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

    private fun feedsKey(query: String, fromDate: String, language: String): String =
        "feeds:${query.trim().lowercase()}:${language.trim().lowercase()}:$fromDate"
}