package br.com.terras.app.network.di

import br.com.terras.app.network.client.HttpClientFactory
import br.com.terras.app.network.client.KtorHttpClientFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

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
    fun provideHttpClientFactory(json: Json): HttpClientFactory =
        KtorHttpClientFactory(json = json)

    @Provides
    @Singleton
    @DefaultHttpClient
    fun provideDefaultHttpClient(factory: HttpClientFactory): HttpClient =
        factory.createHttpClient()

    @Provides
    @Singleton
    @WebSocketHttpClient
    fun provideWebSocketHttpClient(factory: HttpClientFactory): HttpClient =
        factory.createWebSocketClient()
}