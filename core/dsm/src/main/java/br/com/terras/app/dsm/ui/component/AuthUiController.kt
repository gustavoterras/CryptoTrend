package br.com.terras.app.dsm.ui.component

import androidx.compose.runtime.staticCompositionLocalOf

data class AuthUiController(
    val isSignedIn: Boolean = false,
    val displayName: String? = null,
    val email: String? = null,
    val photoUrl: String? = null,
    val requestSignIn: ((onSignedIn: () -> Unit) -> Unit) = {},
    val openProfileMenu: () -> Unit = {}
)

val LocalAuthUiController = staticCompositionLocalOf<AuthUiController?> { null }
