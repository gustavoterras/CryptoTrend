package br.com.terras.app.feeds.di

import br.com.terras.app.feeds.network.ApiServicesFeeds
import br.com.terras.app.network.di.NewsApiBaseUrl
import br.com.terras.app.network.di.NewsApiClient
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
        @NewsApiBaseUrl baseUrl: String,
        @NewsApiClient client: HttpClient
    ): ApiServicesFeeds = ApiServicesFeeds(baseUrl, client)
}