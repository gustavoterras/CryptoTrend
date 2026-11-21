package br.com.terras.app.cryptotrend

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import br.com.terras.app.cryptotrend.auth.AccountProfileDialog
import br.com.terras.app.cryptotrend.auth.GoogleSignInPromptDialog
import br.com.terras.app.cryptotrend.auth.attemptAutoSignIn
import br.com.terras.app.cryptotrend.auth.signOutWithCleanup
import br.com.terras.app.cryptotrend.core.dsm.R
import br.com.terras.app.cryptotrend.data.favorites.FirestoreFavoritesSyncGateway
import br.com.terras.app.cryptotrend.splash.SplashScreen
import br.com.terras.app.dsm.ui.annotation.ThemePreviews
import br.com.terras.app.dsm.ui.component.AuthUiController
import br.com.terras.app.dsm.ui.component.BottomNavItem
import br.com.terras.app.dsm.ui.component.FloatBottomNavigation
import br.com.terras.app.dsm.ui.component.LocalAuthUiController
import br.com.terras.app.dsm.ui.theme.DSMTheme
import br.com.terras.app.navigation.Screen
import br.com.terras.app.network.favorites.AuthUserState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var favoritesSyncGateway: FirestoreFavoritesSyncGateway

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
                var showSplash by rememberSaveable { mutableStateOf(true) }
                Box(modifier = Modifier.fillMaxSize()) {
                    MainScreenView()

                    AnimatedVisibility(
                        visible = showSplash,
                        enter = EnterTransition.None,
                        exit = fadeOut(animationSpec = tween(durationMillis = 420)) +
                            scaleOut(
                                targetScale = 1.05f,
                                animationSpec = tween(durationMillis = 420)
                            )
                    ) {
                        SplashScreen(
                            onSplashFinished = { showSplash = false }
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun MainScreenView() {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val credentialManager = remember(context) { CredentialManager.create(context) }
        val navController = rememberNavController()

        val authState by favoritesSyncGateway.authState.collectAsStateWithLifecycle()
        var showSignInDialog by remember { mutableStateOf(false) }
        var showProfileDialog by remember { mutableStateOf(false) }
        var pendingOnSignedIn by remember { mutableStateOf<(() -> Unit)?>(null) }

        LaunchedEffect(Unit) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {},
                onUnauthenticated = {},
                scope = scope
            )
        }

        val authUiController = remember(authState) {
            val signedIn = authState as? AuthUserState.Authenticated
            AuthUiController(
                isSignedIn = signedIn != null,
                displayName = signedIn?.displayName,
                email = signedIn?.email,
                photoUrl = signedIn?.photoUrl,
                requestSignIn = { onSignedIn ->
                    if (signedIn != null) {
                        onSignedIn()
                    } else {
                        pendingOnSignedIn = onSignedIn
                        showSignInDialog = true
                    }
                },
                openProfileMenu = {
                    if (signedIn != null) {
                        showProfileDialog = true
                    } else {
                        showSignInDialog = true
                    }
                }
            )
        }

        val bottomNavItems = listOf(
            BottomNavItem("Mercado", R.drawable.storefront_24dp, Screen.CoinsList),
            BottomNavItem("Notícias", R.drawable.newspaper_24dp, Screen.Feeds),
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

        CompositionLocalProvider(LocalAuthUiController provides authUiController) {
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

                if (showSignInDialog) {
                    GoogleSignInPromptDialog(
                        onDismiss = {
                            showSignInDialog = false
                            pendingOnSignedIn = null
                        },
                        onAuthSuccess = {
                            showSignInDialog = false
                            val callback = pendingOnSignedIn
                            pendingOnSignedIn = null
                            callback?.invoke()
                        }
                    )
                }

                val authenticatedUser = authState as? AuthUserState.Authenticated
                if (showProfileDialog && authenticatedUser != null) {
                    AccountProfileDialog(
                        displayName = authenticatedUser.displayName,
                        email = authenticatedUser.email,
                        onDismiss = { showProfileDialog = false },
                        onSignOutClick = {
                            showProfileDialog = false
                            signOutWithCleanup(
                                credentialManager = credentialManager,
                                scope = scope,
                                detachListeners = {
                                    favoritesSyncGateway.detachListenersBeforeSignOut()
                                }
                            )
                        }
                    )
                }
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