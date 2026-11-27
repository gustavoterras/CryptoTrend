package br.com.terras.app.feeds.data

import br.com.terras.app.feeds.MainDispatcherRule
import br.com.terras.app.feeds.getDateFrom
import br.com.terras.app.feeds.mockFeedsResponseStub
import br.com.terras.app.feeds.network.ApiServicesFeeds
import br.com.terras.app.database.cache.CachedListSnapshot
import br.com.terras.app.database.cache.ListCacheStore
import br.com.terras.app.database.cache.encodePayload
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.serialization.json.Json
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Locale

private class InMemoryFeedsCacheStore(
    var nowMillis: Long = 1_000_000L
) : ListCacheStore {
    override val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    val snapshots = linkedMapOf<String, CachedListSnapshot>()

    override fun currentTimeMillis(): Long = nowMillis

    override suspend fun getSnapshot(cacheKey: String): CachedListSnapshot? = snapshots[cacheKey]

    override suspend fun putSnapshot(
        cacheKey: String,
        category: String,
        currency: String,
        payloadJson: String,
        fetchedAtMillis: Long,
        priceUpdatedAtMillis: Long
    ) {
        snapshots[cacheKey] = CachedListSnapshot(
            cacheKey = cacheKey,
            category = category,
            currency = currency,
            payloadJson = payloadJson,
            fetchedAtMillis = fetchedAtMillis,
            priceUpdatedAtMillis = priceUpdatedAtMillis
        )
    }

    override suspend fun updatePricesOnly(
        cacheKey: String,
        updatedPayloadJson: String,
        priceUpdatedAtMillis: Long
    ) = Unit

    override suspend fun invalidateByPrefix(keyPrefix: String) = Unit
    override suspend fun invalidateOtherPages(category: String, currency: String, keepKey: String) = Unit
    override suspend fun <T> singleFlight(key: String, block: suspend () -> T): T = block()
}

@OptIn(ExperimentalCoroutinesApi::class)
class FeedsRepositoryImplTest {

    private val services: ApiServicesFeeds = mockk()
    private lateinit var cacheStore: InMemoryFeedsCacheStore
    private lateinit var repository: FeedsRepository

    @get:Rule
    val mainRule = MainDispatcherRule(UnconfinedTestDispatcher())

    @Before
    fun setup() {
        cacheStore = InMemoryFeedsCacheStore()
        repository = FeedsRepositoryImpl(services, cacheStore)
    }

    @Test
    fun `WHEN getFeeds THEN should return success response`() = runBlocking {
        val query = "bitcoin"
        val dateFrom = getDateFrom()
        val language = Locale.getDefault().language

        coEvery {
            services.getFeeds(query, dateFrom, language)
        } returns mockFeedsResponseStub

        val result = repository.getFeeds(query, dateFrom, language)

        Assert.assertEquals(Result.success(mockFeedsResponseStub), result)
    }

    @Test
    fun `WHEN feeds cache is within 15m TTL THEN returns cached feeds without calling API`() = runBlocking {
        val query = "bitcoin"
        val dateFrom = getDateFrom()
        val language = Locale.getDefault().language
        val key = "feeds:bitcoin:${language.lowercase()}:$dateFrom"

        cacheStore.putSnapshot(
            cacheKey = key,
            category = FeedsRepositoryImpl.CATEGORY_FEEDS,
            currency = language.lowercase(),
            payloadJson = cacheStore.encodePayload(mockFeedsResponseStub),
            fetchedAtMillis = cacheStore.nowMillis - 120_000L
        )

        val result = repository.getFeeds(query, dateFrom, language)

        Assert.assertEquals(Result.success(mockFeedsResponseStub), result)
        coVerify(exactly = 0) { services.getFeeds(any(), any(), any()) }
    }

    @Test
    fun `WHEN forceRefresh true THEN bypasses valid cache and hits API`() = runBlocking {
        val query = "bitcoin"
        val dateFrom = getDateFrom()
        val language = Locale.getDefault().language
        val key = "feeds:bitcoin:${language.lowercase()}:$dateFrom"

        cacheStore.putSnapshot(
            cacheKey = key,
            category = FeedsRepositoryImpl.CATEGORY_FEEDS,
            currency = language.lowercase(),
            payloadJson = cacheStore.encodePayload(mockFeedsResponseStub),
            fetchedAtMillis = cacheStore.nowMillis - 10_000L
        )
        coEvery {
            services.getFeeds(query, dateFrom, language)
        } returns mockFeedsResponseStub

        val result = repository.getFeeds(query, dateFrom, language, forceRefresh = true)

        Assert.assertEquals(Result.success(mockFeedsResponseStub), result)
        coVerify(exactly = 1) { services.getFeeds(query, dateFrom, language) }
    }
}