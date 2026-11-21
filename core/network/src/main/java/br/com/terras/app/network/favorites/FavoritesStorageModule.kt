package br.com.terras.app.network.favorites

import android.content.Context
import androidx.room.Room
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

        @Provides
        @Singleton
        fun provideCryptoTrendDatabase(
            @ApplicationContext context: Context
        ): CryptoTrendDatabase {
            return Room.databaseBuilder(
                context,
                CryptoTrendDatabase::class.java,
                DATABASE_NAME
            ).build()
        }

        @Provides
        fun provideFavoriteCoinDao(
            database: CryptoTrendDatabase
        ): FavoriteCoinDao {
            return database.favoriteCoinDao()
        }
    }
}
