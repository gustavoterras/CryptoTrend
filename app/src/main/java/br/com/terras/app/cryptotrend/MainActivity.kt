package br.com.terras.app.cryptotrend

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import br.com.terras.app.cryptotrend.core.dsm.R
import br.com.terras.app.dsm.ui.annotation.ThemePreviews
import br.com.terras.app.dsm.ui.component.BottomNavItem
import br.com.terras.app.dsm.ui.component.FloatBottomNavigation
import br.com.terras.app.dsm.ui.theme.DSMTheme
import br.com.terras.app.navigation.Screen
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.abs

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        try {
            com.google.firebase.FirebaseApp.initializeApp(this)
            val firebaseAppCheck = com.google.firebase.appcheck.FirebaseAppCheck.getInstance()
            firebaseAppCheck.installAppCheckProviderFactory(
                com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory.getInstance()
            )
        } catch (e: Exception) {
            // Already initialized
        }
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
            BottomNavItem("Notícias", R.drawable.newspaper_24dp, Screen.Feeds),
        )

        var isBottomBarCollapsed by rememberSaveable { mutableStateOf(false) }
        val currentCollapsed by rememberUpdatedState(isBottomBarCollapsed)
        var accumulatedScrollPx by remember { mutableFloatStateOf(0f) }
        val collapseThresholdPx = with(LocalDensity.current) { 12.dp.toPx() }

        val nestedScrollConnection = remember(collapseThresholdPx) {
            object : NestedScrollConnection {
                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource
                ): Offset {
                    if (source == NestedScrollSource.UserInput && !currentCollapsed) {
                        val deltaY = abs(consumed.y)
                        if (deltaY > 0.5f) {
                            accumulatedScrollPx += deltaY
                            if (accumulatedScrollPx >= collapseThresholdPx) {
                                isBottomBarCollapsed = true
                                accumulatedScrollPx = 0f
                            }
                        }
                    } else if (currentCollapsed) {
                        accumulatedScrollPx = 0f
                    }
                    return Offset.Zero
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
        ) {
            NavigationGraph(navController = navController)

            FloatBottomNavigation(
                items = bottomNavItems,
                navController = navController,
                isCollapsed = isBottomBarCollapsed,
                onExpandRequest = {
                    accumulatedScrollPx = 0f
                    isBottomBarCollapsed = false
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
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