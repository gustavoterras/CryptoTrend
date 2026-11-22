package br.com.terras.app.portfolio.di

import br.com.terras.app.portfolio.data.PortfolioMarketRepository
import br.com.terras.app.portfolio.data.PortfolioMarketRepositoryImpl
import br.com.terras.app.portfolio.domain.PortfolioUseCase
import br.com.terras.app.portfolio.domain.PortfolioUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
abstract class PortfolioModule {

    @Binds
    abstract fun bindPortfolioMarketRepository(
        impl: PortfolioMarketRepositoryImpl
    ): PortfolioMarketRepository

    @Binds
    abstract fun bindPortfolioUseCase(
        impl: PortfolioUseCaseImpl
    ): PortfolioUseCase
}
