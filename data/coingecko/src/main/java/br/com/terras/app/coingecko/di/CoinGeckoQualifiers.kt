package br.com.terras.app.coingecko.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CoinGeckoClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CoinGeckoBaseUrl
