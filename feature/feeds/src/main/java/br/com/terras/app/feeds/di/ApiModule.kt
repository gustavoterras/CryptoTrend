package br.com.terras.app.feeds.di

import br.com.terras.app.cryptotrend.feature.feeds.BuildConfig
import br.com.terras.app.feeds.network.ApiServicesFeeds
import br.com.terras.app.network.client.HttpClientFactory
import br.com.terras.app.network.client.HttpClientSpec
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
private annotation class NewsApiClient

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {

    @Provides
    @Singleton
    @NewsApiClient
    fun provideNewsApiHttpClient(factory: HttpClientFactory): HttpClient {
        val trimmedToken = BuildConfig.API_TOKEN_NEWS.trim()
        val headers = if (trimmedToken.isNotEmpty()) {
            mapOf("x-api-key" to trimmedToken)
        } else {
            emptyMap()
        }
        return factory.createHttpClient(
            HttpClientSpec(
                defaultHeaders = headers,
                logTag = "NewsApiHttp"
            )
        )
    }

    @Provides
    @Singleton
    fun provideApiService(
        @NewsApiClient client: HttpClient
    ): ApiServicesFeeds = ApiServicesFeeds(BuildConfig.BASE_API_URL_NEWS, client)
}