package br.com.terras.app.home.di

import br.com.terras.app.home.data.CoinsListRepository
import br.com.terras.app.home.data.CoinsListRepositoryImpl
import br.com.terras.app.home.domain.CoinsListUseCase
import br.com.terras.app.home.domain.CoinsListUseCaseImpl
import br.com.terras.app.home.domain.CoinsMapper
import br.com.terras.app.home.domain.CoinsMapperImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@InstallIn(ViewModelComponent::class)
@Module
abstract class UserListModule {

    @Binds
    abstract fun userListRepository(userListRepository: CoinsListRepositoryImpl): CoinsListRepository

    @Binds
    abstract fun userListUseCase(userListUseCase: CoinsListUseCaseImpl): CoinsListUseCase

    @Binds
    abstract fun userMapper(userMapper: CoinsMapperImpl): CoinsMapper
}