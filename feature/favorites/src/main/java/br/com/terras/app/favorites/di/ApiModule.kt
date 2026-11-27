package br.com.terras.app.favorites.di

import br.com.terras.app.coingecko.di.CoinGeckoBaseUrl
import br.com.terras.app.coingecko.di.CoinGeckoClient
import br.com.terras.app.favorites.network.ApiServiceFavorites
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
    ): ApiServiceFavorites = ApiServiceFavorites(baseUrl, client)
}