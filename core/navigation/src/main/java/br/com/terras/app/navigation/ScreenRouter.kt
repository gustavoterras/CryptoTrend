package br.com.terras.app.navigation

enum class ScreenRouter(val value: String) {
    FAVORITE("favorite_screen"),
    COIN_LIST("market_screen"),
    COIN_DETAIL("detail_screen?id={id}"),
    SEARCH("search_screen"),
    FEEDS("feeds_screen"),
    WEB_VIEW("web_view_screen?url={url}")
}