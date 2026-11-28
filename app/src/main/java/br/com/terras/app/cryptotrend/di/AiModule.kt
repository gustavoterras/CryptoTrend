package br.com.terras.app.cryptotrend.di

import br.com.terras.app.common.ai.CryptoAiAnalyzer
import br.com.terras.app.cryptotrend.ai.FirebaseCryptoAiAnalyzer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {

    @Binds
    @Singleton
    abstract fun bindCryptoAiAnalyzer(
        impl: FirebaseCryptoAiAnalyzer
    ): CryptoAiAnalyzer
}
