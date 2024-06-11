package br.com.terras.app.people.di

import br.com.terras.app.network.di.NetworkModule.provideBaseUrl
import br.com.terras.app.network.di.NetworkModule.provideHttpClient
import br.com.terras.app.people.network.ApiService
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