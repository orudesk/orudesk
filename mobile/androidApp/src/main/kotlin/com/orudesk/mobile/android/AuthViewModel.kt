package com.orudesk.mobile.android

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.orudesk.mobile.api.OrudeskApi
import com.orudesk.mobile.auth.AuthSession
import com.orudesk.mobile.auth.AuthState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Android's side of the shared AuthSession: it survives screen rotation (a ViewModel outlives its Activity) and
 * runs the session's suspend functions in viewModelScope, which is cancelled when the screen goes for good.
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val session = AuthSession(OrudeskApi(BuildConfig.API_BASE_URL), KeystoreTokenStore(application))

    val state: StateFlow<AuthState> = session.state

    private val _busy = MutableStateFlow(false)
    /** A request is on its way: the screen disables its button. */
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    init {
        viewModelScope.launch { session.restore() }
    }

    fun login(email: String, password: String) = run { session.login(email, password) }

    fun submitCode(code: String) = run { session.submitCode(code) }

    fun startOver() = session.startOver()

    fun logout() = run { session.logout() }

    private fun run(action: suspend () -> Unit) {
        if (_busy.value) return   // a double tap sends one request
        _busy.value = true
        viewModelScope.launch {
            try {
                action()
            } finally {
                _busy.value = false
            }
        }
    }
}
