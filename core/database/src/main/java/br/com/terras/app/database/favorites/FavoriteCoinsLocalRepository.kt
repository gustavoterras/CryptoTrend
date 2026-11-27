package br.com.terras.app.database.favorites

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface FavoriteCoinsLocalRepository {
    fun observeFavoriteIds(): Flow<List<String>>
    fun observeIsFavorite(coinId: String): Flow<Boolean>
    suspend fun toggleFavorite(coinId: String): Boolean
    suspend fun addFavorite(coinId: String)
    suspend fun removeFavorite(coinId: String)
}

@Singleton
class FavoriteCoinsLocalRepositoryImpl @Inject constructor(
    private val dao: FavoriteCoinDao
) : FavoriteCoinsLocalRepository {

    override fun observeFavoriteIds(): Flow<List<String>> = dao.observeFavoriteIds()

    override fun observeIsFavorite(coinId: String): Flow<Boolean> = dao.observeIsFavorite(coinId)

    override suspend fun toggleFavorite(coinId: String): Boolean {
        val normalizedId = coinId.trim()
        if (normalizedId.isEmpty()) return false
        return if (dao.isFavorite(normalizedId)) {
            dao.deleteFavoriteById(normalizedId)
            false
        } else {
            dao.insertFavorite(FavoriteCoinEntity(id = normalizedId))
            true
        }
    }

    override suspend fun addFavorite(coinId: String) {
        val normalizedId = coinId.trim()
        if (normalizedId.isNotEmpty()) {
            dao.insertFavorite(FavoriteCoinEntity(id = normalizedId))
        }
    }

    override suspend fun removeFavorite(coinId: String) {
        val normalizedId = coinId.trim()
        if (normalizedId.isNotEmpty()) {
            dao.deleteFavoriteById(normalizedId)
        }
    }
}
