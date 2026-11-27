package br.com.terras.app.database.portfolio

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class PortfolioTransactionType {
    BUY,
    SELL;

    companion object {
        fun fromStorage(raw: String): PortfolioTransactionType =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: BUY
    }
}

data class PortfolioTransactionRecord(
    val id: Long = 0L,
    val coinId: String,
    val coinSymbol: String,
    val coinName: String,
    val coinImageUrl: String,
    val type: PortfolioTransactionType,
    val quantity: Double,
    val unitPrice: Double,
    val timestamp: Long,
    val note: String = ""
)

interface PortfolioLocalRepository {
    fun observeTransactionsChronological(): Flow<List<PortfolioTransactionRecord>>
    suspend fun getTransactionById(id: Long): PortfolioTransactionRecord?
    suspend fun insertTransaction(record: PortfolioTransactionRecord): Long
    suspend fun updateTransaction(record: PortfolioTransactionRecord)
    suspend fun deleteTransaction(id: Long)
}

@Singleton
class PortfolioLocalRepositoryImpl @Inject constructor(
    private val dao: PortfolioTransactionDao
) : PortfolioLocalRepository {

    override fun observeTransactionsChronological(): Flow<List<PortfolioTransactionRecord>> =
        dao.observeAllChronological().map { entities ->
            entities.map { it.toRecord() }
        }

    override suspend fun getTransactionById(id: Long): PortfolioTransactionRecord? =
        dao.getById(id)?.toRecord()

    override suspend fun insertTransaction(record: PortfolioTransactionRecord): Long {
        return dao.insert(record.toEntity(idOverride = 0L))
    }

    override suspend fun updateTransaction(record: PortfolioTransactionRecord) {
        if (record.id <= 0L) return
        dao.update(record.toEntity(idOverride = record.id))
    }

    override suspend fun deleteTransaction(id: Long) {
        if (id > 0L) {
            dao.deleteById(id)
        }
    }

    private fun PortfolioTransactionEntity.toRecord(): PortfolioTransactionRecord =
        PortfolioTransactionRecord(
            id = id,
            coinId = coinId,
            coinSymbol = coinSymbol,
            coinName = coinName,
            coinImageUrl = coinImageUrl,
            type = PortfolioTransactionType.fromStorage(type),
            quantity = quantity,
            unitPrice = unitPrice,
            timestamp = timestamp,
            note = note
        )

    private fun PortfolioTransactionRecord.toEntity(idOverride: Long): PortfolioTransactionEntity =
        PortfolioTransactionEntity(
            id = idOverride,
            coinId = coinId.trim().lowercase(),
            coinSymbol = coinSymbol.trim().uppercase(),
            coinName = coinName.trim(),
            coinImageUrl = coinImageUrl.trim(),
            type = type.name,
            quantity = quantity.coerceAtLeast(0.0),
            unitPrice = unitPrice.coerceAtLeast(0.0),
            timestamp = timestamp,
            note = note.trim()
        )
}
