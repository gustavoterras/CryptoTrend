package br.com.terras.app.cryptotrend.network

import br.com.terras.app.network.client.HttpClientSpec
import br.com.terras.app.network.client.KtorHttpClientFactory
import br.com.terras.app.network.client.WebSocketClientSpec
import kotlinx.serialization.json.Json
import org.junit.Assert.assertNotNull
import org.junit.Test

class KtorHttpClientFactoryTest {

    @Test
    fun createHttpClient_installsAllPluginsWithoutError() {
        val factory = KtorHttpClientFactory(Json { ignoreUnknownKeys = true })
        val client = factory.createHttpClient(
            HttpClientSpec(defaultHeaders = mapOf("x-test" to "1"))
        )
        assertNotNull(client)
        client.close()
    }

    @Test
    fun createWebSocketClient_installsAllPluginsWithoutError() {
        val factory = KtorHttpClientFactory(Json { ignoreUnknownKeys = true })
        val client = factory.createWebSocketClient(WebSocketClientSpec())
        assertNotNull(client)
        client.close()
    }
}
