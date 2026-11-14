package vn.edu.qnu.st.nambede.data

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** A failed call: HTTP status (0 when the gateway cannot be reached) and the server's message. */
class ApiException(val status: Int, message: String) : Exception(message)

/** Where the JWT lives; [clear] signs the user out. */
interface Session {
    val token: String?
    fun clear()
}

/** Retrofit client for the API Gateway: adds "Authorization: Bearer <JWT>" and turns failures into [ApiException]. */
class ApiClient(baseUrl: String, private val session: Session) {

    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val token = session.token
            val request = chain.request()
            chain.proceed(if (token == null) request else request.newBuilder().header("Authorization", "Bearer $token").build())
        }
        .build()

    val api: NamBeDeApi = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(http)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(NamBeDeApi::class.java)

    /** Runs one call. A 401 while signed in means the token expired: the session is cleared and the app shows the login screen. */
    suspend fun <T> call(block: suspend NamBeDeApi.() -> T): T {
        try {
            return api.block()
        } catch (error: HttpException) {
            val body = error.response()?.errorBody()?.string()
            val parsed = body?.let { runCatching { json.decodeFromString(ErrorBody.serializer(), it) }.getOrNull() }
            if (error.code() == 401 && session.token != null) session.clear()
            throw ApiException(error.code(), parsed?.message ?: parsed?.detail ?: error.message())
        } catch (error: IOException) {
            throw ApiException(0, "Network error")
        }
    }

    companion object {
        val json = Json {
            ignoreUnknownKeys = true
            // Leave out null fields when sending, e.g. topicId of a random practice round
            explicitNulls = false
        }
    }
}
