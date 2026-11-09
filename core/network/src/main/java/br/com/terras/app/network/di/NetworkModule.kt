package br.com.terras.app.network.di

import android.util.Log
import br.com.terras.app.cryptotrend.core.network.BuildConfig.API_TOKEN
import br.com.terras.app.cryptotrend.core.network.BuildConfig.API_TOKEN_NEWS
import br.com.terras.app.cryptotrend.core.network.BuildConfig.BASE_API_URL
import br.com.terras.app.cryptotrend.core.network.BuildConfig.BASE_API_URL_NEWS
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.android.Android
import io.ktor.client.engine.android.AndroidEngineConfig
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.observer.ResponseObserver
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val NETWORK_TIME_OUT = 6_000L
    private const val AUTH_HEADER_COINGECKO = "x_cg_api_key"
    private const val AUTH_HEADER_NEWS = "x-api-key"

    @Provides
    @Singleton
    @CoinGeckoBaseUrl
    fun provideCoinGeckoBaseUrl(): String = BASE_API_URL

    @Provides
    @Singleton
    @NewsApiBaseUrl
    fun provideNewsApiBaseUrl(): String = BASE_API_URL_NEWS

    private fun HttpClientConfig<AndroidEngineConfig>.applyCommonConfiguration() {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                useAlternativeNames = true
                ignoreUnknownKeys = true
                encodeDefaults = false
            })
        }

        install(HttpTimeout) {
            requestTimeoutMillis = NETWORK_TIME_OUT
            connectTimeoutMillis = NETWORK_TIME_OUT
            socketTimeoutMillis = NETWORK_TIME_OUT
        }

        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    Log.v("Logger Ktor =>", message)
                }
            }
            level = LogLevel.ALL
        }

        install(ResponseObserver) {
            onResponse { response ->
                Log.d("HTTP status:", "${response.status.value}")
            }
        }

        defaultRequest {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
        }
    }

    @Provides
    @Singleton
    @CoinGeckoClient
    fun provideCoinGeckoHttpClient(): HttpClient {
        return HttpClient(Android) {
            applyCommonConfiguration()
            install(DefaultRequest) {
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                header(AUTH_HEADER_COINGECKO, API_TOKEN)
            }
        }
    }

    @Provides
    @Singleton
    @NewsApiClient
    fun provideNewsApiHttpClient(): HttpClient {
        return HttpClient(Android) {
            applyCommonConfiguration()
            install(DefaultRequest) {
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                header(AUTH_HEADER_NEWS, API_TOKEN_NEWS)
            }
        }
    }
}