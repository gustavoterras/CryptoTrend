package br.com.terras.app.favorites.di

import br.com.terras.app.favorites.network.ApiServiceFavorites
import br.com.terras.app.network.di.CoinGeckoBaseUrl
import br.com.terras.app.network.di.CoinGeckoClient
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