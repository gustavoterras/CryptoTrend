package br.com.terras.app.network.portfolio

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PortfolioLocalRepositoryImplTest {

    private class FakePortfolioTransactionDao : PortfolioTransactionDao {
        private val items = MutableStateFlow<List<PortfolioTransactionEntity>>(emptyList())
        private var nextId = 1L

        override fun observeAllChronological(): Flow<List<PortfolioTransactionEntity>> =
            items.map { list ->
                list.sortedWith(compareBy<PortfolioTransactionEntity> { it.timestamp }.thenBy { it.id })
            }

        override suspend fun getById(id: Long): PortfolioTransactionEntity? =
            items.value.firstOrNull { it.id == id }

        override suspend fun insert(entity: PortfolioTransactionEntity): Long {
            val assignedId = if (entity.id > 0L) entity.id else nextId++
            val saved = entity.copy(id = assignedId)
            items.value = items.value.filterNot { it.id == assignedId } + saved
            return assignedId
        }

        override suspend fun update(entity: PortfolioTransactionEntity) {
            items.value = items.value.map { existing ->
                if (existing.id == entity.id) entity else existing
            }
        }

        override suspend fun deleteById(id: Long) {
            items.value = items.value.filterNot { it.id == id }
        }
    }

    @Test
    fun `insertTransaction normalizes fields and assigns id`() = runTest {
        val dao = FakePortfolioTransactionDao()
        val repository = PortfolioLocalRepositoryImpl(dao)

        val id = repository.insertTransaction(
            PortfolioTransactionRecord(
                id = 99L,
                coinId = "  Bitcoin ",
                coinSymbol = " btc ",
                coinName = " Bitcoin ",
                coinImageUrl = " https://img.png ",
                type = PortfolioTransactionType.BUY,
                quantity = 0.5,
                unitPrice = 300000.0,
                timestamp = 1_700_000_000_000L,
                note = " Compra inicial "
            )
        )

        assertEquals(1L, id)
        val saved = repository.getTransactionById(id)
        assertEquals("bitcoin", saved?.coinId)
        assertEquals("BTC", saved?.coinSymbol)
        assertEquals("Bitcoin", saved?.coinName)
        assertEquals("https://img.png", saved?.coinImageUrl)
        assertEquals(PortfolioTransactionType.BUY, saved?.type)
        assertEquals("Compra inicial", saved?.note)
    }

    @Test
    fun `observeTransactionsChronological emits sorted transactions and reflects updates and deletes`() = runTest {
        val dao = FakePortfolioTransactionDao()
        val repository = PortfolioLocalRepositoryImpl(dao)

        val secondId = repository.insertTransaction(
            PortfolioTransactionRecord(
                coinId = "ethereum",
                coinSymbol = "eth",
                coinName = "Ethereum",
                coinImageUrl = "",
                type = PortfolioTransactionType.BUY,
                quantity = 2.0,
                unitPrice = 15000.0,
                timestamp = 2000L
            )
        )
        val firstId = repository.insertTransaction(
            PortfolioTransactionRecord(
                coinId = "bitcoin",
                coinSymbol = "btc",
                coinName = "Bitcoin",
                coinImageUrl = "",
                type = PortfolioTransactionType.BUY,
                quantity = 0.25,
                unitPrice = 320000.0,
                timestamp = 1000L
            )
        )

        val ordered = repository.observeTransactionsChronological().first()
        assertEquals(listOf(firstId, secondId), ordered.map { it.id })

        repository.updateTransaction(ordered.first().copy(quantity = 0.4, note = "Editada"))
        val updated = repository.getTransactionById(firstId)
        assertEquals(0.4, updated?.quantity ?: 0.0, 1e-9)
        assertEquals("Editada", updated?.note)

        repository.deleteTransaction(secondId)
        assertNull(repository.getTransactionById(secondId))
        assertEquals(1, repository.observeTransactionsChronological().first().size)
    }
}
