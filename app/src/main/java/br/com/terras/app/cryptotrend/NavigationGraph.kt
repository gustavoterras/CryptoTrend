package br.com.terras.app.cryptotrend

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import br.com.terras.app.coins.detail.presentation.DetailActivityCompose
import br.com.terras.app.dsm.ui.component.WebViewScreen
import br.com.terras.app.feeds.presentation.FeedsActivityCompose
import br.com.terras.app.favorites.presentation.FavoriteActivityCompose
import br.com.terras.app.coins.list.presentation.CoinsActivityCompose
import br.com.terras.app.search.presentation.SearchActivityCompose
import br.com.terras.app.navigation.ScreenRouter.COIN_LIST
import br.com.terras.app.navigation.ScreenRouter.FAVORITE
import br.com.terras.app.navigation.ScreenRouter.FEEDS
import br.com.terras.app.navigation.ScreenRouter.WEB_VIEW
import br.com.terras.app.navigation.ScreenRouter.COIN_DETAIL
import br.com.terras.app.navigation.ScreenRouter.SEARCH

@Composable
fun NavigationGraph(navController: NavHostController) {
    NavHost(navController, startDestination = FAVORITE.value) {
        composable(
            route = WEB_VIEW.value,
            deepLinks = listOf(navDeepLink { uriPattern = "${WEB_VIEW.value}/{url}" })
        ) {
            val url = it.arguments?.getString("url").orEmpty()
            WebViewScreen(url)
        }
        composable(FAVORITE.value) {
            FavoriteActivityCompose {
                navController.navigate(COIN_DETAIL.value.replace("{id}", it))
            }
        }
        composable(COIN_LIST.value) {
            CoinsActivityCompose {
                navController.navigate(COIN_DETAIL.value.replace("{id}", it))
            }
        }
        composable(
            route = COIN_DETAIL.value,
            deepLinks = listOf(navDeepLink {
                uriPattern = "${COIN_DETAIL.value}/{id}"
            })
        ) {
            val id = it.arguments?.getString("id").orEmpty()

            DetailActivityCompose(id, navController)
        }
        composable(SEARCH.value) {
            SearchActivityCompose {
                navController.navigate(COIN_DETAIL.value.replace("{id}", it))
            }
        }
        composable(FEEDS.value) {
            FeedsActivityCompose {
                navController.navigate(WEB_VIEW.value.replace("{url}", it))
            }
        }
    }
}