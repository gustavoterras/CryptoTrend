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
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.cio.CIOEngineConfig
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val CONNECT_TIME_OUT = 8_000L
    private const val REQUEST_TIME_OUT = 15_000L
    private const val SOCKET_TIME_OUT = 15_000L
    private const val WEBSOCKET_PING_INTERVAL_MS = 10_000L
    private const val AUTH_HEADER_COINGECKO_DEMO = "x-cg-demo-api-key"
    private const val AUTH_HEADER_COINGECKO_PRO = "x-cg-pro-api-key"
    private const val AUTH_HEADER_NEWS = "x-api-key"

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        prettyPrint = true
        isLenient = true
        useAlternativeNames = true
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = false
    }

    @Provides
    @Singleton
    @CoinGeckoBaseUrl
    fun provideCoinGeckoBaseUrl(): String = BASE_API_URL

    @Provides
    @Singleton
    @NewsApiBaseUrl
    fun provideNewsApiBaseUrl(): String = BASE_API_URL_NEWS

    private fun HttpClientConfig<CIOEngineConfig>.applyCommonConfiguration(json: Json) {
        install(ContentEncoding) {
            gzip()
            deflate()
        }

        install(ContentNegotiation) {
            json(json)
        }

        install(HttpTimeout) {
            requestTimeoutMillis = REQUEST_TIME_OUT
            connectTimeoutMillis = CONNECT_TIME_OUT
            socketTimeoutMillis = SOCKET_TIME_OUT
        }

        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    Log.v("Logger Ktor =>", message)
                }
            }
            level = LogLevel.INFO
        }

        defaultRequest {
            accept(ContentType.Application.Json)
        }
    }

    @Provides
    @Singleton
    @CoinGeckoClient
    fun provideCoinGeckoHttpClient(json: Json): HttpClient {
        val trimmedToken = API_TOKEN.trim()
        val authHeader = if (trimmedToken.startsWith("CG-", ignoreCase = true)) {
            AUTH_HEADER_COINGECKO_DEMO
        } else {
            AUTH_HEADER_COINGECKO_PRO
        }
        return HttpClient(CIO) {
            applyCommonConfiguration(json)
            if (trimmedToken.isNotEmpty()) {
                install(DefaultRequest) {
                    header(authHeader, trimmedToken)
                }
            }
        }
    }

    @Provides
    @Singleton
    @CoinWebSocketClient
    fun provideWebSocketHttpClient(json: Json): HttpClient {
        return HttpClient(CIO) {
            install(HttpTimeout) {
                connectTimeoutMillis = CONNECT_TIME_OUT
            }
            install(WebSockets) {
                pingInterval = WEBSOCKET_PING_INTERVAL_MS
            }
            install(ContentNegotiation) {
                json(json)
            }
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        Log.v("Logger Ktor WS =>", message)
                    }
                }
                level = LogLevel.INFO
            }
        }
    }

    @Provides
    @Singleton
    @NewsApiClient
    fun provideNewsApiHttpClient(json: Json): HttpClient {
        return HttpClient(CIO) {
            applyCommonConfiguration(json)
            install(DefaultRequest) {
                header(AUTH_HEADER_NEWS, API_TOKEN_NEWS)
            }
        }
    }
}