package br.com.terras.app.network.favorites

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [FavoriteCoinEntity::class],
    version = 1,
    exportSchema = false
)
abstract class CryptoTrendDatabase : RoomDatabase() {
    abstract fun favoriteCoinDao(): FavoriteCoinDao
}
