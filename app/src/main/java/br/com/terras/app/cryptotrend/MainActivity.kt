package br.com.terras.app.cryptotrend

import android.animation.ObjectAnimator
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.PathInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import br.com.terras.app.cryptotrend.core.dsm.R
import br.com.terras.app.cryptotrend.splash.SplashScreen
import br.com.terras.app.dsm.ui.annotation.ThemePreviews
import br.com.terras.app.dsm.ui.component.BottomNavItem
import br.com.terras.app.dsm.ui.component.FloatBottomNavigation
import br.com.terras.app.dsm.ui.theme.DSMTheme
import br.com.terras.app.navigation.Screen
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.abs

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var isFirstFrameReady = false
    private var startSplashEntrance by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !isFirstFrameReady }
        splashScreen.setOnExitAnimationListener { splashProvider ->
            startSplashEntrance = true
            ObjectAnimator.ofFloat(splashProvider.view, View.ALPHA, 1f, 0f).apply {
                duration = 220L
                interpolator = PathInterpolator(0.22f, 1f, 0.36f, 1f)
                doOnEnd { splashProvider.remove() }
                start()
            }
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            DSMTheme {
                var showSplash by rememberSaveable { mutableStateOf(true) }
                val isDarkTheme = isSystemInDarkTheme()
                val useLightSystemBars = !showSplash && !isDarkTheme

                LaunchedEffect(Unit) {
                    withFrameNanos { }
                    isFirstFrameReady = true
                    kotlinx.coroutines.delay(350L)
                    if (!startSplashEntrance) {
                        startSplashEntrance = true
                    }
                }

                val applySystemBarsStyle = remember(useLightSystemBars) {
                    {
                        enableEdgeToEdge(
                            statusBarStyle = if (useLightSystemBars) {
                                SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                            } else {
                                SystemBarStyle.dark(Color.TRANSPARENT)
                            },
                            navigationBarStyle = if (useLightSystemBars) {
                                SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                            } else {
                                SystemBarStyle.dark(Color.TRANSPARENT)
                            }
                        )
                        val controller = WindowCompat.getInsetsController(window, window.decorView)
                        controller.isAppearanceLightStatusBars = useLightSystemBars
                        controller.isAppearanceLightNavigationBars = useLightSystemBars
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            window.isStatusBarContrastEnforced = false
                            window.isNavigationBarContrastEnforced = false
                        }
                    }
                }

                DisposableEffect(this@MainActivity, applySystemBarsStyle) {
                    applySystemBarsStyle()
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            applySystemBarsStyle()
                        }
                    }
                    lifecycle.addObserver(observer)
                    onDispose {
                        lifecycle.removeObserver(observer)
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    MainScreenView()

                    AnimatedVisibility(
                        visible = showSplash,
                        enter = EnterTransition.None,
                        exit = fadeOut(
                            animationSpec = tween(durationMillis = 440, easing = FastOutSlowInEasing)
                        ) + scaleOut(
                            targetScale = 1.04f,
                            animationSpec = tween(durationMillis = 440, easing = FastOutSlowInEasing)
                        )
                    ) {
                        SplashScreen(
                            startEntrance = startSplashEntrance,
                            onSplashFinished = { showSplash = false }
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun MainScreenView() {
        val navController = rememberNavController()

        val bottomNavItems = listOf(
            BottomNavItem(stringResource(R.string.nav_market), R.drawable.storefront_24dp, Screen.CoinsList),
            BottomNavItem(stringResource(R.string.nav_portfolio), R.drawable.ic_portfolio_24dp, Screen.Portfolio),
            BottomNavItem(stringResource(R.string.nav_news), R.drawable.newspaper_24dp, Screen.Feeds),
        )

        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        val isFullScreenDestination = currentDestination?.hierarchy?.any {
            it.hasRoute<Screen.CoinDetail>() || it.hasRoute<Screen.WebView>()
        } == true
        val showBottomBar = !isFullScreenDestination
        val currentShowBottomBar by rememberUpdatedState(showBottomBar)

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
                    if (source == NestedScrollSource.UserInput && currentShowBottomBar && !currentCollapsed) {
                        val deltaY = abs(consumed.y)
                        if (deltaY > 0.5f) {
                            accumulatedScrollPx += deltaY
                            if (accumulatedScrollPx >= collapseThresholdPx) {
                                isBottomBarCollapsed = true
                                accumulatedScrollPx = 0f
                            }
                        }
                    } else if (!currentShowBottomBar || currentCollapsed) {
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

            AnimatedVisibility(
                visible = showBottomBar,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                FloatBottomNavigation(
                    items = bottomNavItems,
                    navController = navController,
                    isCollapsed = isBottomBarCollapsed,
                    onExpandRequest = {
                        accumulatedScrollPx = 0f
                        isBottomBarCollapsed = false
                    }
                )
            }
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