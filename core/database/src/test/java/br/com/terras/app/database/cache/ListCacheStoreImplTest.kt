package br.com.terras.app.database.cache

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

private class FakeListCacheDao : ListCacheDao {
    val entries = linkedMapOf<String, ListCacheEntity>()
    var deleteOlderThanCutoff: Long? = null

    override suspend fun getByKey(cacheKey: String): ListCacheEntity? = entries[cacheKey]

    override suspend fun upsert(entry: ListCacheEntity) {
        entries[entry.cacheKey] = entry
    }

    override suspend fun deleteByKeyPrefix(keyPrefix: String) {
        entries.keys.filter { it.startsWith(keyPrefix) }.forEach { entries.remove(it) }
    }

    override suspend fun deleteOtherEntriesForCategoryAndCurrency(
        category: String,
        currency: String,
        keepKey: String
    ) {
        entries.entries.removeAll { (_, value) ->
            value.category == category && value.currency == currency && value.cacheKey != keepKey
        }
    }

    override suspend fun deleteOlderThan(cutoffMillis: Long) {
        deleteOlderThanCutoff = cutoffMillis
        entries.entries.removeAll { (_, value) -> value.fetchedAtMillis < cutoffMillis }
    }
}

class ListCacheStoreImplTest {

    @Test
    fun `putSnapshot getSnapshot and updatePricesOnly preserve fetchedAtMillis`() = runTest {
        val dao = FakeListCacheDao()
        val store = ListCacheStoreImpl(dao)

        store.putSnapshot(
            cacheKey = "coins_market:page_1:brl",
            category = "coins_market",
            currency = "brl",
            payloadJson = """[{"id":"bitcoin"}]""",
            fetchedAtMillis = 1_000_000L,
            priceUpdatedAtMillis = 1_000_000L
        )

        store.updatePricesOnly(
            cacheKey = "coins_market:page_1:brl",
            updatedPayloadJson = """[{"id":"bitcoin","current_price":500000.0}]""",
            priceUpdatedAtMillis = 1_090_000L
        )

        val snapshot = store.getSnapshot("coins_market:page_1:brl")
        Assert.assertNotNull(snapshot)
        Assert.assertEquals(1_000_000L, snapshot!!.fetchedAtMillis)
        Assert.assertEquals(1_090_000L, snapshot.priceUpdatedAtMillis)
        Assert.assertTrue(snapshot.payloadJson.contains("500000.0"))
    }

    @Test
    fun `invalidateOtherPages clears page 2 while preserving page 1`() = runTest {
        val dao = FakeListCacheDao()
        val store = ListCacheStoreImpl(dao)

        store.putSnapshot(
            cacheKey = "coins_market:page_1:brl",
            category = "coins_market",
            currency = "brl",
            payloadJson = "[]"
        )
        store.putSnapshot(
            cacheKey = "coins_market:page_2:brl",
            category = "coins_market",
            currency = "brl",
            payloadJson = "[]"
        )
        store.putSnapshot(
            cacheKey = "favorites:bitcoin:brl",
            category = "favorites",
            currency = "brl",
            payloadJson = "[]"
        )

        store.invalidateOtherPages(
            category = "coins_market",
            currency = "brl",
            keepKey = "coins_market:page_1:brl"
        )

        Assert.assertNotNull(dao.entries["coins_market:page_1:brl"])
        Assert.assertNull(dao.entries["coins_market:page_2:brl"])
        Assert.assertNotNull(dao.entries["favorites:bitcoin:brl"])
    }

    @Test
    fun `singleFlight coalesces concurrent invocations for same key`() = runTest {
        val dao = FakeListCacheDao()
        val store = ListCacheStoreImpl(dao)
        val executions = AtomicInteger(0)
        val started = CompletableDeferred<Unit>()
        val gate = CompletableDeferred<Unit>()

        val job1 = async {
            store.singleFlight("shared-key") {
                executions.incrementAndGet()
                started.complete(Unit)
                gate.await()
                "result-value"
            }
        }
        started.await()
        val job2 = async(start = CoroutineStart.UNDISPATCHED) {
            store.singleFlight("shared-key") {
                executions.incrementAndGet()
                "should-not-run"
            }
        }

        gate.complete(Unit)
        Assert.assertEquals("result-value", job1.await())
        Assert.assertEquals("result-value", job2.await())
        Assert.assertEquals(1, executions.get())
    }
}
