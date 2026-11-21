package br.com.terras.app.network.favorites

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteCoinDao {
    @Query("SELECT id FROM favorite_coins ORDER BY addedAt DESC")
    fun observeFavoriteIds(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_coins WHERE id = :coinId)")
    fun observeIsFavorite(coinId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_coins WHERE id = :coinId)")
    suspend fun isFavorite(coinId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(coin: FavoriteCoinEntity)

    @Query("DELETE FROM favorite_coins WHERE id = :coinId")
    suspend fun deleteFavoriteById(coinId: String)
}
