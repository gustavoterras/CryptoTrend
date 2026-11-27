package br.com.terras.app.coingecko.di

import br.com.terras.app.cryptotrend.data.coingecko.BuildConfig
import br.com.terras.app.network.client.HttpClientFactory
import br.com.terras.app.network.client.HttpClientSpec
import br.com.terras.app.coingecko.price.CoinGeckoSimplePriceService
import br.com.terras.app.coingecko.price.CoinGeckoSimplePriceServiceImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CoinGeckoModule {

    @Binds
    @Singleton
    abstract fun bindCoinGeckoSimplePriceService(
        impl: CoinGeckoSimplePriceServiceImpl
    ): CoinGeckoSimplePriceService

    companion object {
        @Provides
        @Singleton
        @CoinGeckoBaseUrl
        fun provideCoinGeckoBaseUrl(): String = BuildConfig.BASE_API_URL

        @Provides
        @Singleton
        @CoinGeckoClient
        fun provideCoinGeckoHttpClient(
            factory: HttpClientFactory,
            @CoinGeckoBaseUrl baseUrl: String
        ): HttpClient {
            val trimmedToken = BuildConfig.API_TOKEN.trim()
            val headers = if (trimmedToken.isNotEmpty()) {
                val authHeader = if (baseUrl.contains("pro-api.coingecko.com", ignoreCase = true)) {
                    "x-cg-pro-api-key"
                } else {
                    "x-cg-demo-api-key"
                }
                mapOf(authHeader to trimmedToken)
            } else {
                emptyMap()
            }
            return factory.createHttpClient(
                HttpClientSpec(
                    defaultHeaders = headers,
                    logTag = "CoinGeckoHttp"
                )
            )
        }
    }
}
