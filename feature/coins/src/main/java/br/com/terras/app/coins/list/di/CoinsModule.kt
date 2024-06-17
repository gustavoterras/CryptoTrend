package br.com.terras.app.coins.list.di

import br.com.terras.app.coins.list.data.CoinsRepository
import br.com.terras.app.coins.list.data.CoinsRepositoryImpl
import br.com.terras.app.coins.list.domain.CoinsUseCase
import br.com.terras.app.coins.list.domain.CoinsUseCaseImpl
import br.com.terras.app.coins.list.domain.CoinsMapper
import br.com.terras.app.coins.list.domain.CoinsMapperImpl
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