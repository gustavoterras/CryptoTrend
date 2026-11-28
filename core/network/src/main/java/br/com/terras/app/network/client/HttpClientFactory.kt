package br.com.terras.app.network.client

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
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
import javax.inject.Inject
import javax.inject.Singleton

data class HttpClientSpec(
    val connectTimeoutMillis: Long = DEFAULT_CONNECT_TIMEOUT_MS,
    val requestTimeoutMillis: Long = DEFAULT_REQUEST_TIMEOUT_MS,
    val socketTimeoutMillis: Long = DEFAULT_SOCKET_TIMEOUT_MS,
    val defaultHeaders: Map<String, String> = emptyMap(),
    val logTag: String = "KtorHttp"
) {
    companion object {
        const val DEFAULT_CONNECT_TIMEOUT_MS = 8_000L
        const val DEFAULT_REQUEST_TIMEOUT_MS = 15_000L
        const val DEFAULT_SOCKET_TIMEOUT_MS = 15_000L
    }
}

data class WebSocketClientSpec(
    val connectTimeoutMillis: Long = HttpClientSpec.DEFAULT_CONNECT_TIMEOUT_MS,
    val pingIntervalMillis: Long = DEFAULT_PING_INTERVAL_MS,
    val logTag: String = "KtorWebSocket"
) {
    companion object {
        const val DEFAULT_PING_INTERVAL_MS = 10_000L
    }
}

interface HttpClientFactory {
    fun createHttpClient(spec: HttpClientSpec = HttpClientSpec()): HttpClient
    fun createWebSocketClient(spec: WebSocketClientSpec = WebSocketClientSpec()): HttpClient
}

@Singleton
class KtorHttpClientFactory @Inject constructor(
    private val json: Json
) : HttpClientFactory {

    override fun createHttpClient(spec: HttpClientSpec): HttpClient {
        return HttpClient(CIO) {
            install(ContentEncoding) {
                gzip()
                deflate()
            }

            install(ContentNegotiation) {
                json(json)
            }

            install(HttpTimeout) {
                requestTimeoutMillis = spec.requestTimeoutMillis
                connectTimeoutMillis = spec.connectTimeoutMillis
                socketTimeoutMillis = spec.socketTimeoutMillis
            }

            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        Log.v(spec.logTag, message)
                    }
                }
                level = LogLevel.INFO
            }

            defaultRequest {
                accept(ContentType.Application.Json)
            }

            if (spec.defaultHeaders.isNotEmpty()) {
                install(DefaultRequest) {
                    spec.defaultHeaders.forEach { (key, value) ->
                        if (key.isNotBlank() && value.isNotBlank()) {
                            header(key, value)
                        }
                    }
                }
            }
        }
    }

    override fun createWebSocketClient(spec: WebSocketClientSpec): HttpClient {
        return HttpClient(CIO) {
            install(HttpTimeout) {
                connectTimeoutMillis = spec.connectTimeoutMillis
            }
            install(WebSockets) {
                pingIntervalMillis = spec.pingIntervalMillis
            }
            install(ContentNegotiation) {
                json(json)
            }
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        Log.v(spec.logTag, message)
                    }
                }
                level = LogLevel.INFO
            }
        }
    }
}
