package br.com.terras.app.cryptotrend.debug

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import br.com.terras.app.dsm.ui.theme.DSMTheme

class DsmCatalogActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val systemDark = isSystemInDarkTheme()
            var isDarkTheme by rememberSaveable { mutableStateOf(systemDark) }
            val useLightSystemBars = !isDarkTheme

            val applySystemBars = remember(useLightSystemBars) {
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

            DisposableEffect(applySystemBars) {
                applySystemBars()
                onDispose {}
            }

            DSMTheme(useDarkTheme = isDarkTheme) {
                DsmCatalogScreen(
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = { isDarkTheme = !isDarkTheme },
                    onBack = { finish() }
                )
            }
        }
    }
}
