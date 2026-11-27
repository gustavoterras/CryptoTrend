package br.com.terras.app.database.favorites

import br.com.terras.app.database.CryptoTrendDatabase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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
        @Provides
        fun provideFavoriteCoinDao(
            database: CryptoTrendDatabase
        ): FavoriteCoinDao = database.favoriteCoinDao()
    }
}
