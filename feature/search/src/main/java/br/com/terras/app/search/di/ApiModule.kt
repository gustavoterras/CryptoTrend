package br.com.terras.app.search.di

import br.com.terras.app.search.network.ApiServiceSearch
import br.com.terras.app.network.di.NetworkModule.provideBaseUrl
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
    fun provideApiService(): ApiServiceSearch = ApiServiceSearch(provideBaseUrl(), provideHttpClient())
}