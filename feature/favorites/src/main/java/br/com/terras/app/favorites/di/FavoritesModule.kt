package br.com.terras.app.favorites.di

import br.com.terras.app.favorites.data.FavoritesRepository
import br.com.terras.app.favorites.data.FavoritesRepositoryImpl
import br.com.terras.app.favorites.domain.FavoritesMapper
import br.com.terras.app.favorites.domain.FavoritesMapperImpl
import br.com.terras.app.favorites.domain.FavoritesUseCase
import br.com.terras.app.favorites.domain.FavoritesUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@InstallIn(ViewModelComponent::class)
@Module
abstract class FavoritesModule {

    @Binds
    abstract fun favoritesRepository(repository: FavoritesRepositoryImpl): FavoritesRepository

    @Binds
    abstract fun favoritesUseCase(useCase: FavoritesUseCaseImpl): FavoritesUseCase

    @Binds
    abstract fun favoritesMapper(mapper: FavoritesMapperImpl): FavoritesMapper
}