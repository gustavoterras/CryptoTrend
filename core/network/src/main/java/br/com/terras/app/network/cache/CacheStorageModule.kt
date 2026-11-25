package br.com.terras.app.network.cache

import br.com.terras.app.network.favorites.CryptoTrendDatabase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CacheStorageModule {

    @Binds
    @Singleton
    abstract fun bindListCacheStore(
        impl: ListCacheStoreImpl
    ): ListCacheStore

    @Binds
    @Singleton
    abstract fun bindCoinGeckoSimplePriceService(
        impl: CoinGeckoSimplePriceServiceImpl
    ): CoinGeckoSimplePriceService

    companion object {
        @Provides
        fun provideListCacheDao(
            database: CryptoTrendDatabase
        ): ListCacheDao = database.listCacheDao()
    }
}
