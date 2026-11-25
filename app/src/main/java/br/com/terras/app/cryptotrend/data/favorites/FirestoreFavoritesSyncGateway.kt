package br.com.terras.app.cryptotrend.data.favorites

import android.content.Context
import br.com.terras.app.cryptotrend.R
import br.com.terras.app.cryptotrend.core.dsm.R as DsmR
import br.com.terras.app.network.favorites.AuthUserState
import br.com.terras.app.network.favorites.FavoritesIdsState
import br.com.terras.app.network.favorites.FavoritesSyncGateway
import br.com.terras.app.network.favorites.UnauthenticatedFavoriteException
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreFavoritesSyncGateway @Inject constructor(
    @ApplicationContext context: Context
) : FavoritesSyncGateway {

    private val appContext = context.applicationContext
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(
        appContext.getString(R.string.firestore_database_id)
    )
    private val auth: FirebaseAuth = Firebase.auth
    private val gatewayScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _authState = MutableStateFlow(mapAuthUser(auth.currentUser))
    override val authState: StateFlow<AuthUserState> = _authState.asStateFlow()

    private val _favoriteIdsState = MutableStateFlow<FavoritesIdsState>(
        if (auth.currentUser?.uid.isNullOrBlank()) {
            FavoritesIdsState.Unauthenticated
        } else {
            FavoritesIdsState.Loading
        }
    )
    override val favoriteIdsState: StateFlow<FavoritesIdsState> = _favoriteIdsState.asStateFlow()

    private var listenerJob: Job? = null
    private var observedUid: String? = null

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val mapped = mapAuthUser(firebaseAuth.currentUser)
        _authState.value = mapped
        syncListenerForUser(mapped)
    }

    init {
        auth.addAuthStateListener(authStateListener)
        syncListenerForUser(_authState.value)
    }

    private fun mapAuthUser(user: FirebaseUser?): AuthUserState {
        val uid = user?.uid
        return if (uid.isNullOrBlank()) {
            AuthUserState.Unauthenticated
        } else {
            AuthUserState.Authenticated(
                uid = uid,
                displayName = user.displayName,
                email = user.email,
                photoUrl = user.photoUrl?.toString()
            )
        }
    }

    @Synchronized
    private fun syncListenerForUser(userState: AuthUserState) {
        when (userState) {
            is AuthUserState.Unauthenticated -> {
                observedUid = null
                listenerJob?.cancel()
                listenerJob = null
                _favoriteIdsState.value = FavoritesIdsState.Unauthenticated
            }
            is AuthUserState.Authenticated -> {
                val uid = userState.uid
                if (uid.isBlank()) {
                    observedUid = null
                    listenerJob?.cancel()
                    listenerJob = null
                    _favoriteIdsState.value = FavoritesIdsState.Unauthenticated
                    return
                }
                if (observedUid == uid && listenerJob?.isActive == true) {
                    return
                }
                observedUid = uid
                listenerJob?.cancel()
                _favoriteIdsState.value = FavoritesIdsState.Loading
                val path = "users/$uid/favorites"
                listenerJob = gatewayScope.launch {
                    db.collection("users")
                        .document(uid)
                        .collection("favorites")
                        .snapshots()
                        .map { snapshot ->
                            snapshot.toObjects(
                                FavoriteCoinDocument::class.java,
                                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                            )
                                .sortedWith(
                                    compareByDescending<FavoriteCoinDocument> {
                                        it.createdAt?.seconds ?: Long.MAX_VALUE
                                    }.thenBy { it.coinId.ifBlank { it.id } }
                                )
                                .mapNotNull { doc ->
                                    val resolvedId = doc.coinId.ifBlank { doc.id }.trim()
                                    resolvedId.takeIf { it.isNotEmpty() }
                                }
                                .distinct()
                        }
                        .catch { error ->
                            val firestoreError = generateSequence(error) { it.cause }
                                .filterIsInstance<FirebaseFirestoreException>()
                                .firstOrNull()
                            if (firestoreError == null && error is CancellationException) {
                                throw error
                            }
                            val exceptionToLog = firestoreError ?: (error as? Exception)
                            if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                                handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                            }
                            val rootError = firestoreError ?: error
                            _favoriteIdsState.value = FavoritesIdsState.Error(
                                rootError.localizedMessage
                                    ?: appContext.getString(DsmR.string.favorites_sync_load_error)
                            )
                        }
                        .collect { coinIds ->
                            if (auth.currentUser?.uid == uid) {
                                _favoriteIdsState.value = FavoritesIdsState.Ready(coinIds)
                            }
                        }
                }
            }
        }
    }

    suspend fun detachListenersBeforeSignOut() {
        withContext(NonCancellable) {
            observedUid = null
            listenerJob?.cancel()
            listenerJob = null
            _favoriteIdsState.value = FavoritesIdsState.Unauthenticated
        }
    }

    private fun requireValidUserId(): String {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            throw UnauthenticatedFavoriteException()
        }
        return uid
    }

    override suspend fun toggleFavorite(
        coinId: String,
        symbol: String,
        name: String
    ): Result<Boolean> {
        val currentUid = auth.currentUser?.uid
        val normalizedCoinId = coinId.trim()
        var docPath = "users/${currentUid ?: "unauthenticated"}/favorites/$normalizedCoinId"
        var operationType = OperationType.WRITE

        return try {
            val uid = requireValidUserId()
            require(
                normalizedCoinId.length in 1..128 && VALID_ID_REGEX.matches(normalizedCoinId)
            ) {
                appContext.getString(DsmR.string.favorites_invalid_id)
            }
            val cleanSymbol = symbol.trim().uppercase().take(32)
            val cleanName = name.trim().take(128)

            val docRef = db.collection("users")
                .document(uid)
                .collection("favorites")
                .document(normalizedCoinId)
            docPath = docRef.path

            val currentlyFavoriteInState =
                (_favoriteIdsState.value as? FavoritesIdsState.Ready)
                    ?.coinIds
                    ?.contains(normalizedCoinId) == true

            val existingDoc = runCatching {
                operationType = OperationType.GET
                docRef.get().await()
            }.getOrElse { fetchError ->
                if (fetchError is CancellationException) throw fetchError
                handleFirestoreError(fetchError, OperationType.GET, docPath)
                return Result.failure(fetchError)
            }

            if (existingDoc.exists() || currentlyFavoriteInState && existingDoc.exists()) {
                operationType = OperationType.DELETE
                docRef.delete().await()
                Result.success(false)
            } else {
                operationType = OperationType.CREATE
                val payload = mapOf(
                    "userId" to uid,
                    "coinId" to normalizedCoinId,
                    "symbol" to cleanSymbol,
                    "name" to cleanName,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                Result.success(true)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (e !is UnauthenticatedFavoriteException) {
                handleFirestoreError(e, operationType, docPath)
            }
            Result.failure(e)
        }
    }

    override suspend fun removeFavorite(coinId: String): Result<Unit> {
        val currentUid = auth.currentUser?.uid
        val normalizedCoinId = coinId.trim()
        var docPath = "users/${currentUid ?: "unauthenticated"}/favorites/$normalizedCoinId"

        return try {
            val uid = requireValidUserId()
            require(
                normalizedCoinId.length in 1..128 && VALID_ID_REGEX.matches(normalizedCoinId)
            ) {
                appContext.getString(DsmR.string.favorites_invalid_remove_id)
            }
            val docRef = db.collection("users")
                .document(uid)
                .collection("favorites")
                .document(normalizedCoinId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (e !is UnauthenticatedFavoriteException) {
                handleFirestoreError(e, OperationType.DELETE, docPath)
            }
            Result.failure(e)
        }
    }

    companion object {
        private val VALID_ID_REGEX = Regex("^[a-zA-Z0-9_.\\-]+$")
    }
}
