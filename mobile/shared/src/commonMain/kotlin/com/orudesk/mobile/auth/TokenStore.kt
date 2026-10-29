package com.orudesk.mobile.auth

/**
 * Where the refresh token survives the app being closed (Day 73). Each platform keeps it in its own safe place:
 * Android encrypts it with a key in the Android Keystore, iOS would use the Keychain. The access token is never
 * stored: it lives in memory and is replaced from the refresh token.
 */
interface TokenStore {
    fun load(): String?

    fun save(refreshToken: String)

    fun clear()
}

/** For tests and previews: forgets everything when the process ends. */
class InMemoryTokenStore(private var token: String? = null) : TokenStore {
    override fun load(): String? = token

    override fun save(refreshToken: String) {
        token = refreshToken
    }

    override fun clear() {
        token = null
    }
}
