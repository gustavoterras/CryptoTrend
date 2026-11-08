package br.com.terras.app.cryptotrend

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import br.com.terras.app.cryptotrend.core.dsm.R
import br.com.terras.app.dsm.ui.annotation.ThemePreviews
import br.com.terras.app.dsm.ui.component.BottomNavItem
import br.com.terras.app.dsm.ui.component.FloatBottomNavigation
import br.com.terras.app.dsm.ui.theme.DSMTheme
import br.com.terras.app.navigation.Screen
import dagger.hilt.android.AndroidEntryPoint

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DSMTheme {
                MainScreenView()
            }
        }
    }

    @Composable
    fun MainScreenView() {
        val navController = rememberNavController()

        val bottomNavItems = listOf(
            BottomNavItem("Mercado", R.drawable.storefront_24dp, Screen.CoinsList),
            BottomNavItem("Favoritos", R.drawable.star_24dp, Screen.Favorites),
            BottomNavItem("Pesquisar", R.drawable.search_24dp, Screen.Search),
            BottomNavItem("Notícias", R.drawable.newspaper_24dp, Screen.Feeds),
        )

        Scaffold(
            bottomBar = {
                FloatBottomNavigation(
                    items = bottomNavItems,
                    navController = navController
                )
            }
        ) {
            NavigationGraph(navController = navController)
        }
    }

    @ThemePreviews
    @Composable
    fun MainActivityPreview() {
        DSMTheme {
            MainScreenView()
        }
    }
}