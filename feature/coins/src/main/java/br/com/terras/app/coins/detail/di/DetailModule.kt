package br.com.terras.app.coins.detail.di

import br.com.terras.app.coins.detail.data.DetailRepository
import br.com.terras.app.coins.detail.data.DetailRepositoryImpl
import br.com.terras.app.coins.detail.domain.DetailMapper
import br.com.terras.app.coins.detail.domain.DetailMapperImpl
import br.com.terras.app.coins.detail.domain.DetailUseCase
import br.com.terras.app.coins.detail.domain.DetailUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@InstallIn(ViewModelComponent::class)
@Module
abstract class DetailModule {

    @Binds
    abstract fun detailRepository(repository: DetailRepositoryImpl): DetailRepository

    @Binds
    abstract fun detailUseCase(useCase: DetailUseCaseImpl): DetailUseCase

    @Binds
    abstract fun detailMapper(mapper: DetailMapperImpl): DetailMapper
}