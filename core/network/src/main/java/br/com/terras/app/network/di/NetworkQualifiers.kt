package br.com.terras.app.network.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CoinGeckoClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class NewsApiClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CoinGeckoBaseUrl

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class NewsApiBaseUrl
