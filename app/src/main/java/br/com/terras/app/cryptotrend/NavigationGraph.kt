package br.com.terras.app.cryptotrend

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import br.com.terras.app.dsm.ui.component.WebViewScreen
import br.com.terras.app.feeds.presentation.FeedsActivityView
import br.com.terras.app.home.presentation.HomeActivityView
import br.com.terras.app.navigation.ScreenRouter.COIN_LIST
import br.com.terras.app.navigation.ScreenRouter.FAVORITE
import br.com.terras.app.navigation.ScreenRouter.FEEDS
import br.com.terras.app.navigation.ScreenRouter.WEB_VIEW
import br.com.terras.app.coins.presentation.CoinsActivityView

@Composable
fun NavigationGraph(navController: NavHostController) {
    NavHost(navController, startDestination = FAVORITE.value) {
        composable(
            WEB_VIEW.value,
            deepLinks = listOf(navDeepLink { uriPattern = "${WEB_VIEW.value}/{url}" })
        ) {
            val url = it.arguments?.getString("url").orEmpty()
            WebViewScreen(url)
        }
        composable(FAVORITE.value) {
            HomeActivityView()
        }
        composable(COIN_LIST.value) {
            CoinsActivityView()
        }
        composable(FEEDS.value) {
            FeedsActivityView {
                navController.navigate(WEB_VIEW.value.replace("{url}", it))
            }
        }
    }
}