package br.com.terras.app.people.di

import br.com.terras.app.people.data.CoinsListRepository
import br.com.terras.app.people.data.CoinsListRepositoryImpl
import br.com.terras.app.people.domain.CoinsListUseCase
import br.com.terras.app.people.domain.CoinsListUseCaseImpl
import br.com.terras.app.people.domain.CoinsMapper
import br.com.terras.app.people.domain.CoinsMapperImpl
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