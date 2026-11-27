package br.com.terras.app.database

import androidx.room.Database
import androidx.room.RoomDatabase
import br.com.terras.app.database.cache.ListCacheDao
import br.com.terras.app.database.cache.ListCacheEntity
import br.com.terras.app.database.favorites.FavoriteCoinDao
import br.com.terras.app.database.favorites.FavoriteCoinEntity
import br.com.terras.app.database.portfolio.PortfolioTransactionDao
import br.com.terras.app.database.portfolio.PortfolioTransactionEntity

@Database(
    entities = [FavoriteCoinEntity::class, PortfolioTransactionEntity::class, ListCacheEntity::class],
    version = 3,
    exportSchema = false
)
abstract class CryptoTrendDatabase : RoomDatabase() {
    abstract fun favoriteCoinDao(): FavoriteCoinDao
    abstract fun portfolioTransactionDao(): PortfolioTransactionDao
    abstract fun listCacheDao(): ListCacheDao
}
