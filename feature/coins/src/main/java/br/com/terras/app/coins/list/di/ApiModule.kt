package br.com.terras.app.coins.list.di

import br.com.terras.app.coins.list.network.ApiServiceCoins
import br.com.terras.app.coingecko.di.CoinGeckoBaseUrl
import br.com.terras.app.coingecko.di.CoinGeckoClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {

    @Provides
    @Singleton
    fun provideApiService(
        @CoinGeckoBaseUrl baseUrl: String,
        @CoinGeckoClient client: HttpClient
    ): ApiServiceCoins = ApiServiceCoins(baseUrl, client)
}