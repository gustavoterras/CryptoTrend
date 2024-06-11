package br.com.terras.app.feeds.di

import br.com.terras.app.feeds.data.FeedsRepository
import br.com.terras.app.feeds.data.FeedsRepositoryImpl
import br.com.terras.app.feeds.domain.FeedsMapper
import br.com.terras.app.feeds.domain.FeedsMapperImpl
import br.com.terras.app.feeds.domain.FeedsUseCase
import br.com.terras.app.feeds.domain.FeedsUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@InstallIn(ViewModelComponent::class)
@Module
abstract class FeedsModule {

    @Binds
    abstract fun feedsRepository(repository: FeedsRepositoryImpl): FeedsRepository

    @Binds
    abstract fun feedsUseCase(useCase: FeedsUseCaseImpl): FeedsUseCase

    @Binds
    abstract fun feedsMapper(mapper: FeedsMapperImpl): FeedsMapper
}