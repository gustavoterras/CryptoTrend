package br.com.terras.app.network.favorites

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FavoritesStorageModule {

    @Binds
    @Singleton
    abstract fun bindFavoriteCoinsLocalRepository(
        impl: FavoriteCoinsLocalRepositoryImpl
    ): FavoriteCoinsLocalRepository

    companion object {
        private const val DATABASE_NAME = "cryptotrend_local.db"

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `portfolio_transactions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `coinId` TEXT NOT NULL,
                        `coinSymbol` TEXT NOT NULL,
                        `coinName` TEXT NOT NULL,
                        `coinImageUrl` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `quantity` REAL NOT NULL,
                        `unitPrice` REAL NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `note` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_portfolio_transactions_coinId` ON `portfolio_transactions` (`coinId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_portfolio_transactions_timestamp` ON `portfolio_transactions` (`timestamp`)"
                )
            }
        }

        @Provides
        @Singleton
        fun provideCryptoTrendDatabase(
            @ApplicationContext context: Context
        ): CryptoTrendDatabase {
            return Room.databaseBuilder(
                context,
                CryptoTrendDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(MIGRATION_1_2)
                .build()
        }

        @Provides
        fun provideFavoriteCoinDao(
            database: CryptoTrendDatabase
        ): FavoriteCoinDao {
            return database.favoriteCoinDao()
        }
    }
}
