package br.com.terras.app.coins.di

import br.com.terras.app.coins.data.CoinsRepository
import br.com.terras.app.coins.data.CoinsRepositoryImpl
import br.com.terras.app.coins.domain.CoinsUseCase
import br.com.terras.app.coins.domain.CoinsUseCaseImpl
import br.com.terras.app.coins.domain.CoinsMapper
import br.com.terras.app.coins.domain.CoinsMapperImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@InstallIn(ViewModelComponent::class)
@Module
abstract class CoinsModule {

    @Binds
    abstract fun coinsRepository(repository: CoinsRepositoryImpl): CoinsRepository

    @Binds
    abstract fun coinsUseCase(useCase: CoinsUseCaseImpl): CoinsUseCase

    @Binds
    abstract fun coinsMapper(mapper: CoinsMapperImpl): CoinsMapper
}