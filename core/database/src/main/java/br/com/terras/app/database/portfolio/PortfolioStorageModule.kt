package br.com.terras.app.database.portfolio

import br.com.terras.app.database.CryptoTrendDatabase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PortfolioStorageModule {

    @Binds
    @Singleton
    abstract fun bindPortfolioLocalRepository(
        impl: PortfolioLocalRepositoryImpl
    ): PortfolioLocalRepository

    companion object {
        @Provides
        fun providePortfolioTransactionDao(
            database: CryptoTrendDatabase
        ): PortfolioTransactionDao = database.portfolioTransactionDao()
    }
}
