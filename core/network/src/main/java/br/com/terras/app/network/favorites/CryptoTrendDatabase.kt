package br.com.terras.app.network.favorites

import androidx.room.Database
import androidx.room.RoomDatabase
import br.com.terras.app.network.portfolio.PortfolioTransactionDao
import br.com.terras.app.network.portfolio.PortfolioTransactionEntity

@Database(
    entities = [FavoriteCoinEntity::class, PortfolioTransactionEntity::class],
    version = 2,
    exportSchema = false
)
abstract class CryptoTrendDatabase : RoomDatabase() {
    abstract fun favoriteCoinDao(): FavoriteCoinDao
    abstract fun portfolioTransactionDao(): PortfolioTransactionDao
}
