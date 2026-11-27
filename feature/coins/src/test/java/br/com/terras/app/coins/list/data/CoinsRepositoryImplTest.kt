package br.com.terras.app.coins.list.data

import br.com.terras.app.coins.MainDispatcherRule
import br.com.terras.app.coins.list.data.model.AlternativeFngEntry
import br.com.terras.app.coins.list.data.model.AlternativeFngResponse
import br.com.terras.app.coins.list.data.model.FearAndGreedIndexResponse
import br.com.terras.app.coins.list.data.model.SparklineResponse
import br.com.terras.app.coins.list.network.ApiServiceCoins
import br.com.terras.app.coins.mockCoinsListResponseStub
import br.com.terras.app.coins.mockCoinsResponseStub
import br.com.terras.app.coins.mockGlobalMarketResponseStub
import br.com.terras.app.coins.mockTrendingCoinsResponseStub
import br.com.terras.app.database.cache.CachedListSnapshot
import br.com.terras.app.database.cache.ListCacheStore
import br.com.terras.app.database.cache.encodePayload
import br.com.terras.app.coingecko.price.CoinGeckoSimplePriceService
import br.com.terras.app.coingecko.price.SimpleCoinPrice
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

private class InMemoryCoinsListCacheStore(
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
    ) {
        val current = snapshots[cacheKey] ?: return
        snapshots[cacheKey] = current.copy(
            payloadJson = updatedPayloadJson,
            priceUpdatedAtMillis = priceUpdatedAtMillis
        )
    }

    override suspend fun invalidateByPrefix(keyPrefix: String) {
        snapshots.keys.filter { it.startsWith(keyPrefix) }.forEach { snapshots.remove(it) }
    }

    override suspend fun invalidateOtherPages(category: String, currency: String, keepKey: String) {
        snapshots.entries.removeAll { (_, v) ->
            v.category == category && v.currency == currency && v.cacheKey != keepKey
        }
    }

    override suspend fun <T> singleFlight(key: String, block: suspend () -> T): T = block()
}

@OptIn(ExperimentalCoroutinesApi::class)
class CoinsRepositoryImplTest {
    private val services: ApiServiceCoins = mockk(relaxed = true)
    private val simplePriceService: CoinGeckoSimplePriceService = mockk(relaxed = true)
    private lateinit var cacheStore: InMemoryCoinsListCacheStore
    private lateinit var repository: CoinsRepository

    private lateinit var previousLocale: java.util.Locale

    @get:Rule
    val mainRule = MainDispatcherRule(UnconfinedTestDispatcher())

    @Before
    fun setup() {
        previousLocale = java.util.Locale.getDefault()
        java.util.Locale.setDefault(java.util.Locale("pt", "BR"))
        cacheStore = InMemoryCoinsListCacheStore()
        repository = CoinsRepositoryImpl(services, simplePriceService, cacheStore)
    }

    @org.junit.After
    fun tearDown() {
        java.util.Locale.setDefault(previousLocale)
    }

    @Test
    fun `WHEN getCoins THEN should return success response`() = runBlocking {
        coEvery {
            services.getCoins()
        } returns mockCoinsListResponseStub

        val result = repository.getCoins()

        Assert.assertEquals(Result.success(mockCoinsListResponseStub), result)
    }

    @Test
    fun `WHEN getTrendingCoins THEN should return success response`() = runBlocking {
        coEvery {
            services.getTrendingCoins()
        } returns mockTrendingCoinsResponseStub

        val result = repository.getTrendingCoins()

        Assert.assertEquals(Result.success(mockTrendingCoinsResponseStub), result)
    }

    @Test
    fun `WHEN getCoins with page 2 THEN forwards page parameter to service`() = runBlocking {
        coEvery {
            services.getCoins(page = 2)
        } returns mockCoinsListResponseStub

        val result = repository.getCoins(page = 2)

        Assert.assertEquals(Result.success(mockCoinsListResponseStub), result)
    }

    @Test
    fun `WHEN getCoinsByIds THEN should return success response`() = runBlocking {
        val ids = listOf("id")
        coEvery {
            services.getCoinsByIds(ids)
        } returns mockCoinsListResponseStub

        val result = repository.getCoinsByIds(ids)

        Assert.assertEquals(Result.success(mockCoinsListResponseStub), result)
    }

    @Test
    fun `WHEN cache is fresh within 60s THEN returns cached coins without calling network`() = runBlocking {
        val cachedCoin = mockCoinsResponseStub.copy(
            id = "bitcoin",
            price = 500_000.0,
            sparklineIn7d = SparklineResponse(listOf(490_000.0, 500_000.0))
        )
        cacheStore.putSnapshot(
            cacheKey = "coins_market:page_1:brl",
            category = CoinsRepositoryImpl.CATEGORY_COINS_MARKET,
            currency = "brl",
            payloadJson = cacheStore.encodePayload(listOf(cachedCoin)),
            fetchedAtMillis = cacheStore.nowMillis - 30_000L,
            priceUpdatedAtMillis = cacheStore.nowMillis - 30_000L
        )

        val result = repository.getCoins(page = 1, forceRefresh = false)

        Assert.assertEquals(Result.success(listOf(cachedCoin)), result)
        coVerify(exactly = 0) { services.getCoins(any()) }
        coVerify(exactly = 0) { simplePriceService.getSimplePrices(any(), any()) }
    }

    @Test
    fun `WHEN price cache is over 60s and structural cache is valid THEN overlays via simple price preserving sparkline`() = runBlocking {
        val sparkline = SparklineResponse(listOf(490_000.0, 495_000.0, 500_000.0))
        val cachedCoin = mockCoinsResponseStub.copy(
            id = "bitcoin",
            price = 500_000.0,
            priceChangePercentage = 1.0,
            sparklineIn7d = sparkline
        )
        cacheStore.putSnapshot(
            cacheKey = "coins_market:page_1:brl",
            category = CoinsRepositoryImpl.CATEGORY_COINS_MARKET,
            currency = "brl",
            payloadJson = cacheStore.encodePayload(listOf(cachedCoin)),
            fetchedAtMillis = cacheStore.nowMillis - 120_000L,
            priceUpdatedAtMillis = cacheStore.nowMillis - 120_000L
        )
        coEvery {
            simplePriceService.getSimplePrices(listOf("bitcoin"), "brl")
        } returns mapOf(
            "bitcoin" to SimpleCoinPrice(
                price = 515_000.0,
                change24h = 3.5,
                lastUpdatedAtSeconds = 1_780_000L
            )
        )

        val result = repository.getCoins(page = 1, forceRefresh = false)

        val expected = listOf(
            cachedCoin.copy(
                price = 515_000.0,
                priceChangePercentage = 3.5,
                sparklineIn7d = sparkline
            )
        )
        Assert.assertEquals(Result.success(expected), result)
        coVerify(exactly = 0) { services.getCoins(any()) }
        coVerify(exactly = 1) { simplePriceService.getSimplePrices(listOf("bitcoin"), "brl") }
    }

    @Test
    fun `WHEN structural cache expired and API fails THEN falls back to cached list`() = runBlocking {
        val cachedCoin = mockCoinsResponseStub.copy(id = "bitcoin")
        cacheStore.putSnapshot(
            cacheKey = "coins_market:page_1:brl",
            category = CoinsRepositoryImpl.CATEGORY_COINS_MARKET,
            currency = "brl",
            payloadJson = cacheStore.encodePayload(listOf(cachedCoin)),
            fetchedAtMillis = cacheStore.nowMillis - (CoinsRepositoryImpl.COINS_LIST_TTL_MILLIS + 10_000L),
            priceUpdatedAtMillis = cacheStore.nowMillis - (CoinsRepositoryImpl.COINS_LIST_TTL_MILLIS + 10_000L)
        )
        coEvery { services.getCoins(1) } throws RuntimeException("429 Too Many Requests")

        val result = repository.getCoins(page = 1, forceRefresh = false)

        Assert.assertEquals(Result.success(listOf(cachedCoin)), result)
    }

    @Test
    fun `WHEN forceRefresh page 1 THEN invalidates page 2 and hits API`() = runBlocking {
        cacheStore.putSnapshot(
            cacheKey = "coins_market:page_2:brl",
            category = CoinsRepositoryImpl.CATEGORY_COINS_MARKET,
            currency = "brl",
            payloadJson = cacheStore.encodePayload(mockCoinsListResponseStub)
        )
        coEvery { services.getCoins(1) } returns mockCoinsListResponseStub

        val result = repository.getCoins(page = 1, forceRefresh = true)

        Assert.assertEquals(Result.success(mockCoinsListResponseStub), result)
        Assert.assertNull(cacheStore.getSnapshot("coins_market:page_2:brl"))
        Assert.assertNotNull(cacheStore.getSnapshot("coins_market:page_1:brl"))
    }

    @Test
    fun `WHEN getGlobalMarketData cache is fresh THEN returns cached snapshot without network call`() = runBlocking {
        cacheStore.putSnapshot(
            cacheKey = "global_market:global",
            category = CoinsRepositoryImpl.CATEGORY_GLOBAL_MARKET,
            currency = "global",
            payloadJson = cacheStore.encodePayload(mockGlobalMarketResponseStub),
            fetchedAtMillis = cacheStore.nowMillis - 60_000L
        )

        val result = repository.getGlobalMarketData(forceRefresh = false)

        Assert.assertEquals(Result.success(mockGlobalMarketResponseStub), result)
        coVerify(exactly = 0) { services.getGlobalMarketData() }
    }

    @Test
    fun `WHEN getGlobalMarketData cache expired and API fails THEN falls back to cached snapshot`() = runBlocking {
        cacheStore.putSnapshot(
            cacheKey = "global_market:global",
            category = CoinsRepositoryImpl.CATEGORY_GLOBAL_MARKET,
            currency = "global",
            payloadJson = cacheStore.encodePayload(mockGlobalMarketResponseStub),
            fetchedAtMillis = cacheStore.nowMillis - (CoinsRepositoryImpl.GLOBAL_MARKET_TTL_MILLIS + 10_000L)
        )
        coEvery { services.getGlobalMarketData() } throws RuntimeException("rate limit")

        val result = repository.getGlobalMarketData(forceRefresh = false)

        Assert.assertEquals(Result.success(mockGlobalMarketResponseStub), result)
    }

    @Test
    fun `WHEN getGlobalMarketData forceRefresh THEN calls API and updates cache`() = runBlocking {
        coEvery { services.getGlobalMarketData() } returns mockGlobalMarketResponseStub

        val result = repository.getGlobalMarketData(forceRefresh = true)

        Assert.assertEquals(Result.success(mockGlobalMarketResponseStub), result)
        coVerify(exactly = 1) { services.getGlobalMarketData() }
        Assert.assertNotNull(cacheStore.getSnapshot("global_market:global"))
    }

    @Test
    fun `WHEN CoinGecko global omits fear_and_greed_index THEN enriches from fallback endpoint`() = runBlocking {
        val withoutFng = mockGlobalMarketResponseStub.copy(
            data = mockGlobalMarketResponseStub.data.copy(fearAndGreedIndex = null)
        )
        coEvery { services.getGlobalMarketData() } returns withoutFng
        coEvery { services.getFallbackFearAndGreed() } returns AlternativeFngResponse(
            data = listOf(AlternativeFngEntry(value = "68", valueClassification = "Greed"))
        )

        val result = repository.getGlobalMarketData(forceRefresh = true)

        Assert.assertEquals(
            FearAndGreedIndexResponse(value = 68, sentiment = "Greed"),
            result.getOrNull()?.data?.fearAndGreedIndex
        )
    }
}