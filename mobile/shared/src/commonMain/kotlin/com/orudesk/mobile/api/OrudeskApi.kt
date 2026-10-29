package com.orudesk.mobile.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.io.IOException
import kotlinx.serialization.json.Json

/**
 * The Orudesk API, for the apps (Day 72). Only what the apps use so far: the two login steps, refresh, logout and
 * the profile. Answers are turned into types (LoginResult) here, once, so no screen ever looks at a status code.
 *
 * @param engine the HTTP engine: OkHttp on Android, URLSession on iOS, a mock in tests
 */
class OrudeskApi(baseUrl: String, engine: HttpClientEngine = platformEngine()) {

    private val json = Json { ignoreUnknownKeys = true }

    private val http = HttpClient(engine) {
        expectSuccess = false   // we read 401s and 429s ourselves
        install(ContentNegotiation) { json(json) }
        defaultRequest {
            url(baseUrl.trimEnd('/') + "/")
            contentType(ContentType.Application.Json)
        }
    }

    suspend fun login(email: String, password: String): LoginResult = attempt {
        val response = http.post("api/v1/auth/login") { setBody(LoginRequest(email, password)) }
        when {
            response.status.isSuccess() -> LoginResult.SignedIn(tokens(response))
            response.status.value == 401 -> {
                val problem = problem(response)
                if (problem.title == "Second factor required" && problem.loginToken != null) {
                    LoginResult.SecondFactorRequired(problem.loginToken)
                } else {
                    LoginResult.WrongCredentials
                }
            }
            else -> failure(response)
        }
    }

    suspend fun loginSecondFactor(loginToken: String, code: String): LoginResult = attempt {
        val response = http.post("api/v1/auth/login/two-factor") { setBody(SecondFactorRequest(loginToken, code.trim())) }
        when {
            response.status.isSuccess() -> LoginResult.SignedIn(tokens(response))
            response.status.value == 400 -> LoginResult.WrongCode
            response.status.value == 401 -> LoginResult.Expired
            else -> failure(response)
        }
    }

    /** New tokens for a refresh token, which stops working. Null when it was refused (the session has ended). */
    suspend fun refresh(refreshToken: String): Tokens? {
        val response = http.post("api/v1/auth/refresh") { setBody(RefreshRequest(refreshToken)) }
        return if (response.status.isSuccess()) tokens(response) else null
    }

    suspend fun logout(refreshToken: String) {
        http.post("api/v1/auth/logout") { setBody(RefreshRequest(refreshToken)) }
    }

    suspend fun me(accessToken: String): Profile? {
        val response = http.get("api/v1/users/me") { bearerAuth(accessToken) }
        return if (response.status.isSuccess()) response.body() else null
    }

    private suspend fun tokens(response: HttpResponse): Tokens {
        val body: TokenResponse = response.body()
        // A JSON client (not a browser) always gets the refresh token in the body
        val refreshToken = requireNotNull(body.refreshToken) { "the API sent no refresh token" }
        return Tokens(body.accessToken, body.expiresIn, refreshToken)
    }

    private suspend fun problem(response: HttpResponse): Problem =
        runCatching { json.decodeFromString<Problem>(response.body<String>()) }.getOrDefault(Problem())

    private suspend fun failure(response: HttpResponse): LoginResult = when (response.status.value) {
        429 -> LoginResult.Throttled(response.headers[HttpHeaders.RetryAfter]?.toLongOrNull() ?: 60)
        503 -> LoginResult.Unavailable
        else -> LoginResult.Failed(problem(response).detail)
    }

    private inline fun attempt(block: () -> LoginResult): LoginResult = try {
        block()
    } catch (e: IOException) {
        LoginResult.NetworkError
    }
}

/** The platform's HTTP engine: each platform's source set provides it. */
expect fun platformEngine(): HttpClientEngine
