package br.com.terras.app.favorites.di

import br.com.terras.app.coingecko.di.CoinGeckoBaseUrl
import br.com.terras.app.coingecko.di.CoinGeckoClient
import br.com.terras.app.coingecko.price.CoinGeckoSimplePriceService
import br.com.terras.app.favorites.network.ApiServiceFavorites
import br.com.terras.app.favorites.network.FavoritesTickerBatchService
import br.com.terras.app.favorites.network.FavoritesTickerBatchServiceImpl
import br.com.terras.app.network.di.WebSocketHttpClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
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

    @Provides
    @Singleton
    fun provideFavoritesTickerBatchService(
        @WebSocketHttpClient wsClient: HttpClient,
        simplePriceService: CoinGeckoSimplePriceService,
        json: Json
    ): FavoritesTickerBatchService = FavoritesTickerBatchServiceImpl(
        wsClient = wsClient,
        simplePriceService = simplePriceService,
        json = json
    )
}