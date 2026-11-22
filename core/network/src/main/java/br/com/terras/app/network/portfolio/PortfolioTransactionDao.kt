package br.com.terras.app.network.portfolio

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PortfolioTransactionDao {
    @Query("SELECT * FROM portfolio_transactions ORDER BY timestamp ASC, id ASC")
    fun observeAllChronological(): Flow<List<PortfolioTransactionEntity>>

    @Query("SELECT * FROM portfolio_transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): PortfolioTransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PortfolioTransactionEntity): Long

    @Update
    suspend fun update(entity: PortfolioTransactionEntity)

    @Query("DELETE FROM portfolio_transactions WHERE id = :id")
    suspend fun deleteById(id: Long)
}
