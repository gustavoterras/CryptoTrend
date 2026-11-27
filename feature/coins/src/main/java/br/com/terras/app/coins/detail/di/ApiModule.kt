package br.com.terras.app.coins.detail.di

import br.com.terras.app.coins.detail.network.ApiServiceDetail
import br.com.terras.app.coins.detail.network.CoinWebSocketService
import br.com.terras.app.coins.detail.network.CoinWebSocketServiceImpl
import br.com.terras.app.coins.detail.network.CoinbaseOrderBookService
import br.com.terras.app.coins.detail.network.CoinbaseOrderBookServiceImpl
import br.com.terras.app.coingecko.price.CoinGeckoSimplePriceService
import br.com.terras.app.coingecko.di.CoinGeckoBaseUrl
import br.com.terras.app.coingecko.di.CoinGeckoClient
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
    ): ApiServiceDetail = ApiServiceDetail(baseUrl, client)

    @Provides
    @Singleton
    fun provideCoinWebSocketService(
        @WebSocketHttpClient wsClient: HttpClient,
        simplePriceService: CoinGeckoSimplePriceService,
        json: Json
    ): CoinWebSocketService = CoinWebSocketServiceImpl(
        wsClient = wsClient,
        simplePriceService = simplePriceService,
        json = json
    )

    @Provides
    @Singleton
    fun provideCoinbaseOrderBookService(
        @WebSocketHttpClient wsClient: HttpClient,
        json: Json
    ): CoinbaseOrderBookService = CoinbaseOrderBookServiceImpl(
        wsClient = wsClient,
        json = json
    )
}