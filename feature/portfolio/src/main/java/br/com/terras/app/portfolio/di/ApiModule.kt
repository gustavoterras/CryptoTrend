package br.com.terras.app.portfolio.di

import br.com.terras.app.network.di.CoinGeckoBaseUrl
import br.com.terras.app.network.di.CoinGeckoClient
import br.com.terras.app.portfolio.network.ApiServicePortfolio
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
    fun provideApiServicePortfolio(
        @CoinGeckoBaseUrl baseUrl: String,
        @CoinGeckoClient client: HttpClient
    ): ApiServicePortfolio = ApiServicePortfolio(baseUrl, client)
}
