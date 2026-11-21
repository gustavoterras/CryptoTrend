package br.com.terras.app.network.favorites

import kotlinx.coroutines.flow.StateFlow

sealed interface AuthUserState {
    data object Unauthenticated : AuthUserState
    data class Authenticated(
        val uid: String,
        val displayName: String? = null,
        val email: String? = null,
        val photoUrl: String? = null
    ) : AuthUserState
}

sealed interface FavoritesIdsState {
    data object Unauthenticated : FavoritesIdsState
    data object Loading : FavoritesIdsState
    data class Ready(val coinIds: List<String>) : FavoritesIdsState
    data class Error(val message: String) : FavoritesIdsState
}

class UnauthenticatedFavoriteException :
    IllegalStateException("É necessário entrar com uma conta Google para gerenciar favoritos.")

interface FavoritesSyncGateway {
    val authState: StateFlow<AuthUserState>
    val favoriteIdsState: StateFlow<FavoritesIdsState>

    suspend fun toggleFavorite(
        coinId: String,
        symbol: String,
        name: String
    ): Result<Boolean>

    suspend fun removeFavorite(coinId: String): Result<Unit>
}
