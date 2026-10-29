package com.orudesk.mobile.api

import kotlinx.serialization.Serializable

// The JSON the API sends and receives, as in its OpenAPI contract. Unknown fields are ignored (see OrudeskApi),
// so a newer API that adds fields doesn't break an older app.

@Serializable
internal data class LoginRequest(val email: String, val password: String)

@Serializable
internal data class SecondFactorRequest(val loginToken: String, val code: String)

@Serializable
internal data class RefreshRequest(val refreshToken: String)

@Serializable
internal data class TokenResponse(val accessToken: String, val expiresIn: Long, val refreshToken: String? = null)

/** An RFC 9457 problem: the API's way of saying what went wrong. */
@Serializable
internal data class Problem(val title: String? = null, val detail: String? = null, val loginToken: String? = null)

@Serializable
data class Profile(val id: String, val email: String, val roles: List<String> = emptyList())

/** What a successful login or refresh gives: an access token for requests, a refresh token to keep. */
data class Tokens(val accessToken: String, val expiresInSeconds: Long, val refreshToken: String)
