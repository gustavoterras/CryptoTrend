package br.com.terras.app.cryptotrend

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import br.com.terras.app.coins.detail.presentation.DetailActivityCompose
import br.com.terras.app.coins.list.presentation.CoinsActivityCompose
import br.com.terras.app.dsm.ui.component.WebViewScreen
import br.com.terras.app.favorites.presentation.FavoriteActivityCompose
import br.com.terras.app.feeds.presentation.FeedsActivityCompose
import br.com.terras.app.navigation.Screen

@Composable
fun NavigationGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.CoinsList) {
        composable<Screen.Favorites> {
            FavoriteActivityCompose { coinId ->
                navController.navigate(Screen.CoinDetail(id = coinId))
            }
        }
        composable<Screen.CoinsList> {
            CoinsActivityCompose(
                favoritesContent = {
                    FavoriteActivityCompose(showTopBar = false) { coinId ->
                        navController.navigate(Screen.CoinDetail(id = coinId))
                    }
                },
                onItemClick = { coinId ->
                    navController.navigate(Screen.CoinDetail(id = coinId))
                }
            )
        }
        composable<Screen.CoinDetail> { backStackEntry ->
            val route = backStackEntry.toRoute<Screen.CoinDetail>()
            DetailActivityCompose(id = route.id, navHostController = navController)
        }
        composable<Screen.Feeds> {
            FeedsActivityCompose { url ->
                navController.navigate(Screen.WebView(url = url))
            }
        }
        composable<Screen.WebView> { backStackEntry ->
            val route = backStackEntry.toRoute<Screen.WebView>()
            WebViewScreen(url = route.url)
        }
    }
}