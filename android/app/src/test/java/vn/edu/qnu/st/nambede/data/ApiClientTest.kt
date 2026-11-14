package vn.edu.qnu.st.nambede.data

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class ApiClientTest {

    private class FakeSession(override var token: String?) : Session {
        override fun clear() {
            token = null
        }
    }

    private val server = MockWebServer()
    private lateinit var session: FakeSession
    private lateinit var client: ApiClient

    @Before
    fun start() {
        server.start()
        session = FakeSession("jwt-token")
        client = ApiClient(server.url("/api/").toString(), session)
    }

    @After
    fun stop() {
        server.shutdown()
    }

    private suspend fun failure(block: suspend () -> Unit): ApiException {
        try {
            block()
        } catch (error: ApiException) {
            return error
        }
        fail("expected an ApiException")
        throw AssertionError()
    }

    @Test
    fun sendsTheTokenToTheGateway() = runTest {
        server.enqueue(MockResponse().setBody("""{"weekStart":"2026-09-28","weekEnd":"2026-10-04","entries":[],"me":null}"""))
        client.call { ranking(0) }
        val request = server.takeRequest()
        assertEquals("/api/ranking/weekly?limit=0", request.path)
        assertEquals("Bearer jwt-token", request.getHeader("Authorization"))
    }

    @Test
    fun errorsCarryTheStatusAndTheServersMessage() = runTest {
        server.enqueue(MockResponse().setResponseCode(409).setBody("""{"status":409,"error":"Conflict","message":"This exercise has already been submitted","path":"/submissions/6/submit"}"""))
        val error = failure { client.call { submit(6) } }
        assertEquals(409, error.status)
        assertEquals("This exercise has already been submitted", error.message)
        assertEquals("Bài đã nộp", errorText(error, mapOf(409 to "Bài đã nộp")))
    }

    @Test
    fun anExpiredTokenSignsTheUserOut() = runTest {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"status":401,"message":"Invalid or expired token"}"""))
        val error = failure { client.call { me() } }
        assertEquals(401, error.status)
        assertNull(session.token)
    }

    @Test
    fun submittingAnExerciseIsAPostToTheGateway() = runTest {
        server.enqueue(MockResponse().setBody("""{"id":6,"exerciseId":7,"exerciseTitle":"Bài 1","dueAt":"2026-10-08T08:08:25Z","status":"SUBMITTED","score":3.33,"correctCount":1,"questionCount":3,"questions":[]}"""))
        val result = client.call { submit(6) }
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/submissions/6/submit", request.path)
        assertEquals("SUBMITTED", result.status)
    }

    @Test
    fun joiningAClassSendsTheCode() = runTest {
        server.enqueue(MockResponse().setBody("""{"id":1,"name":"Tiếng Anh 10A1","code":"K7M2QX","teacherId":3,"studentCount":1}"""))
        assertEquals("K7M2QX", client.call { joinClass(JoinClassRequest("k7m2qx")) }.code)
        val request = server.takeRequest()
        assertEquals("/api/classes/join", request.path)
        assertEquals("""{"code":"k7m2qx"}""", request.body.readUtf8())
    }

    @Test
    fun aWrongCurrentPasswordKeepsTheSession() = runTest {
        server.enqueue(MockResponse().setResponseCode(403).setBody("""{"status":403,"message":"Current password is incorrect"}"""))
        val error = failure { client.call { changePassword(ChangePasswordRequest("sai", "Moi@2026")) } }
        assertEquals(403, error.status)
        assertEquals("jwt-token", session.token)
    }

    @Test
    fun tooManyWrongPasswordsAsksToWait() = runTest {
        server.enqueue(MockResponse().setResponseCode(429).setHeader("Retry-After", "240").setBody("""{"status":429,"message":"Too many failed sign-ins, try again in 240 seconds"}"""))
        val error = failure { client.call { login(LoginRequest("student1@nambede.local", "sai")) } }
        assertEquals(429, error.status)
        assertEquals("Bạn đã nhập sai quá nhiều lần. Vui lòng thử lại sau ít phút.", errorText(error))
    }

    @Test
    fun noConnectionBecomesStatusZero() = runTest {
        server.shutdown()
        val error = failure { client.call { me() } }
        assertEquals(0, error.status)
        assertEquals("Không kết nối được máy chủ. Kiểm tra mạng và địa chỉ gateway.", errorText(error))
    }
}
