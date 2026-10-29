package com.orudesk.mobile

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf

/** What the fake API answers: a JSON body, a status, and any extra headers. */
data class Answer(val body: String, val status: HttpStatusCode = HttpStatusCode.OK, val headers: Map<String, String> = emptyMap())

/** A fake API for tests: answers by "METHOD /path", and remembers what was sent. */
class FakeApi(private val handlers: Map<String, () -> Answer>) {

    val requests = mutableListOf<Pair<String, String>>()

    val engine = MockEngine { request ->
        val key = "${request.method.value} ${request.url.encodedPath}"
        requests += key to ((request.body as? OutgoingContent.ByteArrayContent)?.bytes()?.decodeToString() ?: "")
        val answer = handlers[key]?.invoke() ?: Answer("""{"title": "No fake for $key"}""", HttpStatusCode(599, "No fake"))
        val headers = mapOf(HttpHeaders.ContentType to "application/json") + answer.headers
        respond(answer.body, answer.status, headersOf(*headers.map { it.key to listOf(it.value) }.toTypedArray()))
    }

    fun calls(key: String) = requests.count { it.first == key }

    fun sent(key: String) = requests.last { it.first == key }.second
}

const val TOKENS = """{"accessToken": "access-1", "expiresIn": 900, "refreshToken": "refresh-1", "tokenType": "Bearer"}"""
const val ME = """{"id": "u1", "tenantId": "t1", "email": "asha@orudesk.org", "roles": ["USER"], "grants": [], "enabled": true}"""
