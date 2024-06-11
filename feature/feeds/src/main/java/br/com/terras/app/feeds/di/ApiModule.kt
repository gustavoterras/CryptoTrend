package br.com.terras.app.feeds.di

import br.com.terras.app.feeds.network.ApiServicesFeeds
import br.com.terras.app.network.di.NetworkModule.provideBaseUrlNews
import br.com.terras.app.network.di.NetworkModule.provideHttpClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {

    @Provides
    @Singleton
    fun provideApiService(): ApiServicesFeeds = ApiServicesFeeds(provideBaseUrlNews(), provideHttpClient())
}