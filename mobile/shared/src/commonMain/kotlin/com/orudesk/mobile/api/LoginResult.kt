package com.orudesk.mobile.api

/**
 * Every way a login step can end. A sealed interface, so a `when` over it must handle each case: the compiler
 * reminds every screen when a new one is added (as TypeScript's union type does for the web app).
 */
sealed interface LoginResult {
    data class SignedIn(val tokens: Tokens) : LoginResult

    /** The password was right; the account has two-factor sign-in on. Send a code with this token. */
    data class SecondFactorRequired(val loginToken: String) : LoginResult

    data object WrongCredentials : LoginResult

    data object WrongCode : LoginResult

    /** The second step took too long or had too many wrong codes: start again with the password. */
    data object Expired : LoginResult

    data class Throttled(val retryAfterSeconds: Long) : LoginResult

    data object Unavailable : LoginResult

    data object NetworkError : LoginResult

    data class Failed(val message: String?) : LoginResult
}
