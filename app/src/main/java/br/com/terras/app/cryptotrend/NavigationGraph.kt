package br.com.terras.app.cryptotrend

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import br.com.terras.app.dsm.ui.component.WebViewScreen
import br.com.terras.app.feeds.presentation.FeedsActivityView
import br.com.terras.app.home.presentation.HomeActivityView
import br.com.terras.app.navigation.ScreenRoute.COIN_LIST
import br.com.terras.app.navigation.ScreenRoute.FAVORITE
import br.com.terras.app.navigation.ScreenRoute.FEEDS
import br.com.terras.app.navigation.ScreenRoute.WEB_VIEW
import br.com.terras.app.people.presentation.PeopleActivityView

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
            PeopleActivityView()
        }
        composable(FEEDS.value) {
            FeedsActivityView {
                navController.navigate(WEB_VIEW.value.replace("{url}", it))
            }
        }
    }
}