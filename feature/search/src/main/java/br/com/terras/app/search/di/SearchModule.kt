package br.com.terras.app.search.di

import br.com.terras.app.search.data.SearchRepository
import br.com.terras.app.search.data.SearchRepositoryImpl
import br.com.terras.app.search.domain.SearchUseCase
import br.com.terras.app.search.domain.SearchUseCaseImpl
import br.com.terras.app.search.domain.SearchMapper
import br.com.terras.app.search.domain.SearchMapperImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@InstallIn(ViewModelComponent::class)
@Module
abstract class SearchModule {

    @Binds
    abstract fun searchRepository(repository: SearchRepositoryImpl): SearchRepository

    @Binds
    abstract fun searchUseCase(useCase: SearchUseCaseImpl): SearchUseCase

    @Binds
    abstract fun searchMapper(mapper: SearchMapperImpl): SearchMapper
}