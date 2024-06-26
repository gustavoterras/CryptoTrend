package br.com.terras.app.navigation

enum class ScreenRouter(val value: String) {
    FAVORITE("favorite_screen"),
    COIN_LIST("market_screen"),
    SEARCH("search_screen"),
    FEEDS("feeds_screen"),
    WEB_VIEW("web_view_screen?url={url}")
}