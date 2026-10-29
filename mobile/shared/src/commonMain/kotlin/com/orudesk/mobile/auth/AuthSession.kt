package com.orudesk.mobile.auth

import com.orudesk.mobile.api.LoginResult
import com.orudesk.mobile.api.OrudeskApi
import com.orudesk.mobile.api.Profile
import com.orudesk.mobile.api.Tokens
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Where the user is in signing in. The screens draw whatever this says. */
sealed interface AuthState {
    /** Asking the API whether the stored refresh token still works. */
    data object Restoring : AuthState

    /** @param message why the last attempt failed, for the login screen to show; null for none */
    data class SignedOut(val message: String? = null) : AuthState

    /** The password was right; waiting for the code from the authenticator app. */
    data class NeedsCode(val message: String? = null) : AuthState

    data class SignedIn(val profile: Profile) : AuthState
}

/**
 * The login, as a small state machine (Day 73), shared by the Android and iOS apps:
 *
 *     Restoring ──► SignedOut ──password──► NeedsCode ──code──► SignedIn
 *                       ▲                       │                  │
 *                       └──── expired ──────────┘◄──── logout ─────┘
 *
 * Plain Kotlin and a StateFlow, no Android: a ViewModel (Android) or an ObservableObject (iOS) wraps it. The
 * second step's token is kept in memory only, so closing the app means typing the password again.
 */
class AuthSession(private val api: OrudeskApi, private val store: TokenStore) {

    private val _state = MutableStateFlow<AuthState>(AuthState.Restoring)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private var loginToken: String? = null
    private var tokens: Tokens? = null

    /** At start-up: signed in without a password if the stored refresh token still works. */
    suspend fun restore() {
        val stored = store.load()
        val fresh = stored?.let { api.refresh(it) }
        if (fresh == null) {
            if (stored != null) store.clear()
            _state.value = AuthState.SignedOut()
            return
        }
        signIn(fresh)
    }

    suspend fun login(email: String, password: String) {
        handle(api.login(email.trim(), password))
    }

    suspend fun submitCode(code: String) {
        val token = loginToken ?: run {
            _state.value = AuthState.SignedOut(EXPIRED)
            return
        }
        handle(api.loginSecondFactor(token, code))
    }

    /** Back from the code step to the password. */
    fun startOver() {
        loginToken = null
        _state.value = AuthState.SignedOut()
    }

    suspend fun logout() {
        val refreshToken = tokens?.refreshToken ?: store.load()
        tokens = null
        store.clear()
        _state.value = AuthState.SignedOut()
        // After the state changed: the user is out at once, even if the API can't be reached
        if (refreshToken != null) runCatching { api.logout(refreshToken) }
    }

    /** The current access token, for other requests; null when signed out. */
    fun accessToken(): String? = tokens?.accessToken

    private suspend fun handle(result: LoginResult) {
        val codeStep = loginToken != null
        _state.value = when (result) {
            is LoginResult.SignedIn -> {
                signIn(result.tokens)
                return
            }
            is LoginResult.SecondFactorRequired -> {
                loginToken = result.loginToken
                AuthState.NeedsCode()
            }
            LoginResult.WrongCode -> AuthState.NeedsCode("That code isn't right, or was already used. Try the newest one.")
            LoginResult.Expired -> {
                loginToken = null
                AuthState.SignedOut(EXPIRED)
            }
            else -> message(result).let { if (codeStep) AuthState.NeedsCode(it) else AuthState.SignedOut(it) }
        }
    }

    private suspend fun signIn(fresh: Tokens) {
        val profile = api.me(fresh.accessToken)
        if (profile == null) {
            store.clear()
            _state.value = AuthState.SignedOut("Signing in failed. Please try again.")
            return
        }
        loginToken = null
        tokens = fresh
        store.save(fresh.refreshToken)
        _state.value = AuthState.SignedIn(profile)
    }

    private fun message(result: LoginResult): String = when (result) {
        LoginResult.WrongCredentials -> "Wrong email or password."
        is LoginResult.Throttled -> {
            val minutes = (result.retryAfterSeconds + 59) / 60
            "Too many failed attempts. Try again in $minutes ${if (minutes == 1L) "minute" else "minutes"}."
        }
        LoginResult.Unavailable -> "Login is unavailable right now. Please try again in a minute."
        LoginResult.NetworkError -> "Can't reach the server. Check your connection."
        is LoginResult.Failed -> result.message ?: "Login failed."
        else -> "Login failed."
    }

    private companion object {
        const val EXPIRED = "That took too long. Please enter your email and password again."
    }
}
