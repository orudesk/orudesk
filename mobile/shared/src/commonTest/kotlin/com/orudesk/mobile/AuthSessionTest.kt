package com.orudesk.mobile

import com.orudesk.mobile.api.OrudeskApi
import com.orudesk.mobile.auth.AuthSession
import com.orudesk.mobile.auth.AuthState
import com.orudesk.mobile.auth.InMemoryTokenStore
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthSessionTest {

    private val store = InMemoryTokenStore()

    private fun session(fake: FakeApi) = AuthSession(OrudeskApi("https://orudesk.test", fake.engine), store)

    @Test
    fun aPasswordLoginSignsInAndKeepsTheRefreshToken() = runTest {
        val fake = FakeApi(mapOf(
            "POST /api/v1/auth/login" to { Answer(TOKENS) },
            "GET /api/v1/users/me" to { Answer(ME) },
        ))
        val session = session(fake)

        session.login(" asha@orudesk.org ", "asha-password")

        assertEquals("asha@orudesk.org", assertIs<AuthState.SignedIn>(session.state.value).profile.email)
        assertEquals("refresh-1", store.load())
        assertEquals("""{"email":"asha@orudesk.org","password":"asha-password"}""", fake.sent("POST /api/v1/auth/login"))
    }

    @Test
    fun withTwoFactorOnItAsksForTheCodeThenSignsIn() = runTest {
        val fake = FakeApi(mapOf(
            "POST /api/v1/auth/login" to {
                Answer("""{"title": "Second factor required", "loginToken": "step-two"}""", HttpStatusCode.Unauthorized)
            },
            "POST /api/v1/auth/login/two-factor" to { Answer(TOKENS) },
            "GET /api/v1/users/me" to { Answer(ME) },
        ))
        val session = session(fake)

        session.login("asha@orudesk.org", "asha-password")
        assertIs<AuthState.NeedsCode>(session.state.value)
        assertNull(store.load())   // nothing kept before the second step

        session.submitCode(" 123456 ")

        assertIs<AuthState.SignedIn>(session.state.value)
        assertEquals("""{"loginToken":"step-two","code":"123456"}""", fake.sent("POST /api/v1/auth/login/two-factor"))
    }

    @Test
    fun aWrongCodeStaysOnTheCodeStepAndAnExpiredOneGoesBackToThePassword() = runTest {
        var secondStep = HttpStatusCode.BadRequest
        val fake = FakeApi(mapOf(
            "POST /api/v1/auth/login" to {
                Answer("""{"title": "Second factor required", "loginToken": "step-two"}""", HttpStatusCode.Unauthorized)
            },
            "POST /api/v1/auth/login/two-factor" to { Answer("""{"title": "x"}""", secondStep) },
        ))
        val session = session(fake)
        session.login("asha@orudesk.org", "asha-password")

        session.submitCode("000000")
        assertTrue(assertIs<AuthState.NeedsCode>(session.state.value).message!!.contains("isn't right"))

        secondStep = HttpStatusCode.Unauthorized
        session.submitCode("000000")
        assertTrue(assertIs<AuthState.SignedOut>(session.state.value).message!!.contains("took too long"))
    }

    @Test
    fun aWrongPasswordAndThrottlingSaySo() = runTest {
        var answer = HttpStatusCode.Unauthorized
        val fake = FakeApi(mapOf(
            "POST /api/v1/auth/login" to { Answer("""{"title": "Login failed"}""", answer, mapOf("Retry-After" to "754")) },
        ))
        val session = session(fake)

        session.login("asha@orudesk.org", "wrong")
        assertEquals("Wrong email or password.", assertIs<AuthState.SignedOut>(session.state.value).message)

        answer = HttpStatusCode.TooManyRequests
        session.login("asha@orudesk.org", "wrong")
        assertEquals("Too many failed attempts. Try again in 13 minutes.",
            assertIs<AuthState.SignedOut>(session.state.value).message)
    }

    @Test
    fun restoresWithAStoredRefreshTokenOrForgetsARefusedOne() = runTest {
        var refresh = HttpStatusCode.OK
        val fake = FakeApi(mapOf(
            "POST /api/v1/auth/refresh" to {
                if (refresh == HttpStatusCode.OK) Answer(TOKENS) else Answer("""{"title": "Invalid refresh token"}""", refresh)
            },
            "GET /api/v1/users/me" to { Answer(ME) },
        ))
        store.save("refresh-0")
        val session = session(fake)

        session.restore()
        assertIs<AuthState.SignedIn>(session.state.value)
        assertEquals("refresh-1", store.load())   // rotated

        refresh = HttpStatusCode.Unauthorized
        session.restore()
        assertIs<AuthState.SignedOut>(session.state.value)
        assertNull(store.load())
    }

    @Test
    fun logoutSignsOutAtOnceAndTellsTheApi() = runTest {
        val fake = FakeApi(mapOf(
            "POST /api/v1/auth/login" to { Answer(TOKENS) },
            "GET /api/v1/users/me" to { Answer(ME) },
            "POST /api/v1/auth/logout" to { Answer("", HttpStatusCode.NoContent) },
        ))
        val session = session(fake)
        session.login("asha@orudesk.org", "asha-password")

        session.logout()

        assertIs<AuthState.SignedOut>(session.state.value)
        assertNull(store.load())
        assertEquals(1, fake.calls("POST /api/v1/auth/logout"))
    }
}
