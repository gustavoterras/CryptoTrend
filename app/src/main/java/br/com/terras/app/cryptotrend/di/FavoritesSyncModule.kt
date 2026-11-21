package br.com.terras.app.cryptotrend.di

import br.com.terras.app.cryptotrend.data.favorites.FirestoreFavoritesSyncGateway
import br.com.terras.app.network.favorites.FavoritesSyncGateway
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FavoritesSyncModule {

    @Binds
    @Singleton
    abstract fun bindFavoritesSyncGateway(
        impl: FirestoreFavoritesSyncGateway
    ): FavoritesSyncGateway
}
