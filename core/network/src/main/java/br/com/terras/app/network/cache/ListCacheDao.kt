package br.com.terras.app.network.cache

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ListCacheDao {

    @Query("SELECT * FROM list_cache_entries WHERE cacheKey = :cacheKey LIMIT 1")
    suspend fun getByKey(cacheKey: String): ListCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: ListCacheEntity)

    @Query("DELETE FROM list_cache_entries WHERE cacheKey LIKE :keyPrefix || '%'")
    suspend fun deleteByKeyPrefix(keyPrefix: String)

    @Query(
        "DELETE FROM list_cache_entries " +
            "WHERE category = :category AND currency = :currency AND cacheKey != :keepKey"
    )
    suspend fun deleteOtherEntriesForCategoryAndCurrency(
        category: String,
        currency: String,
        keepKey: String
    )

    @Query("DELETE FROM list_cache_entries WHERE fetchedAtMillis < :cutoffMillis")
    suspend fun deleteOlderThan(cutoffMillis: Long)
}
