package br.com.terras.app.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable
    data object Favorites : Screen

    @Serializable
    data object CoinsList : Screen

    @Serializable
    data class CoinDetail(val id: String) : Screen

    @Serializable
    data object Search : Screen

    @Serializable
    data object Feeds : Screen

    @Serializable
    data class WebView(val url: String) : Screen
}

@Deprecated("Migrated to Screen Type-Safe routes", ReplaceWith("Screen"))
enum class ScreenRouter(val value: String) {
    FAVORITE("favorite_screen"),
    COIN_LIST("market_screen"),
    COIN_DETAIL("detail_screen?id={id}"),
    SEARCH("search_screen"),
    FEEDS("feeds_screen"),
    WEB_VIEW("web_view_screen?url={url}")
}