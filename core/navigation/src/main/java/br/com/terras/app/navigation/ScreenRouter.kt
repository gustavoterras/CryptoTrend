package br.com.terras.app.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable
    data object CoinsList : Screen

    @Serializable
    data class CoinDetail(val id: String) : Screen

    @Serializable
    data object Portfolio : Screen

    @Serializable
    data object Feeds : Screen

    @Serializable
    data class WebView(val url: String) : Screen
}