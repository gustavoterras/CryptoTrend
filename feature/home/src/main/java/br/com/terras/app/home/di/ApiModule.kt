package br.com.terras.app.home.di

import br.com.terras.app.home.network.ApiService
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
    fun provideApiService(): ApiService = ApiService(provideBaseUrl(), provideHttpClient())
}