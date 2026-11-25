package br.com.terras.app.favorites.data

import br.com.terras.app.favorites.MainDispatcherRule
import br.com.terras.app.favorites.mockCoinsListResponseStub
import br.com.terras.app.favorites.mockCoinsResponseStub
import br.com.terras.app.favorites.network.ApiServiceFavorites
import br.com.terras.app.network.cache.CachedListSnapshot
import br.com.terras.app.network.cache.CoinGeckoSimplePriceService
import br.com.terras.app.network.cache.ListCacheStore
import br.com.terras.app.network.cache.SimpleCoinPrice
import br.com.terras.app.network.cache.encodePayload
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

private class InMemoryFavoritesCacheStore(
    var nowMillis: Long = 1_000_000L
) : ListCacheStore {
    override val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    val snapshots = linkedMapOf<String, CachedListSnapshot>()

    override fun currentTimeMillis(): Long = nowMillis

    override fun currentVsCurrency(locale: Locale): String = "brl"

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

    override suspend fun invalidateByPrefix(keyPrefix: String) = Unit
    override suspend fun invalidateOtherPages(category: String, currency: String, keepKey: String) = Unit
    override suspend fun <T> singleFlight(key: String, block: suspend () -> T): T = block()
}

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesRepositoryImplTest {
    private val services: ApiServiceFavorites = mockk(relaxed = true)
    private val simplePriceService: CoinGeckoSimplePriceService = mockk(relaxed = true)
    private lateinit var cacheStore: InMemoryFavoritesCacheStore
    private lateinit var repository: FavoritesRepository

    @get:Rule
    val mainRule = MainDispatcherRule(UnconfinedTestDispatcher())

    @Before
    fun setup() {
        cacheStore = InMemoryFavoritesCacheStore()
        repository = FavoritesRepositoryImpl(services, simplePriceService, cacheStore)
    }

    @Test
    fun `WHEN getCoins THEN should return success response`() = runBlocking {
        val ids = listOf("bitcoin", "ethereum", "cardano")

        coEvery {
            services.getCoinsByIds(ids.joinToString(","))
        } returns mockCoinsListResponseStub

        val result = repository.getCoinsByIds(ids)

        Assert.assertEquals(Result.success(mockCoinsListResponseStub), result)
    }

    @Test
    fun `WHEN favorites cache price is over 60s THEN updates prices via simple price`() = runBlocking {
        val ids = listOf("ethereum", "bitcoin")
        val cachedBitcoin = mockCoinsResponseStub.copy(id = "bitcoin", price = 400_000.0, priceChangePercentage = 1.0)
        val cachedEthereum = mockCoinsResponseStub.copy(id = "ethereum", price = 15_000.0, priceChangePercentage = -0.5)
        cacheStore.putSnapshot(
            cacheKey = "favorites:bitcoin,ethereum:brl",
            category = ListCacheStore.CATEGORY_FAVORITES,
            currency = "brl",
            payloadJson = cacheStore.encodePayload(listOf(cachedBitcoin, cachedEthereum)),
            fetchedAtMillis = cacheStore.nowMillis - 90_000L,
            priceUpdatedAtMillis = cacheStore.nowMillis - 90_000L
        )
        coEvery {
            simplePriceService.getSimplePrices(listOf("bitcoin", "ethereum"), "brl")
        } returns mapOf(
            "bitcoin" to SimpleCoinPrice(420_000.0, 4.2, 1_780_000L)
        )

        val result = repository.getCoinsByIds(ids)

        val expected = listOf(
            cachedBitcoin.copy(price = 420_000.0, priceChangePercentage = 4.2),
            cachedEthereum
        )
        Assert.assertEquals(Result.success(expected), result)
        coVerify(exactly = 0) { services.getCoinsByIds(any()) }
    }
}