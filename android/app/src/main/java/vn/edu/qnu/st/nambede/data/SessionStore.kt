package vn.edu.qnu.st.nambede.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The JWT and the signed-in user, kept in SharedPreferences so the app reopens signed in. */
class SessionStore(context: Context) : Session {

    private val prefs = context.getSharedPreferences("nambede.session", Context.MODE_PRIVATE)
    private val _user = MutableStateFlow(loadUser())

    /** null: signed out (the app shows the login screen). */
    val user: StateFlow<User?> = _user.asStateFlow()

    override val token: String?
        get() = prefs.getString(KEY_TOKEN, null)

    fun save(token: String, user: User) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_USER, ApiClient.json.encodeToString(User.serializer(), user))
            .apply()
        _user.value = user
    }

    fun update(user: User) {
        prefs.edit().putString(KEY_USER, ApiClient.json.encodeToString(User.serializer(), user)).apply()
        _user.value = user
    }

    override fun clear() {
        prefs.edit().clear().apply()
        _user.value = null
    }

    private fun loadUser(): User? {
        if (prefs.getString(KEY_TOKEN, null) == null) return null
        val saved = prefs.getString(KEY_USER, null) ?: return null
        return runCatching { ApiClient.json.decodeFromString(User.serializer(), saved) }.getOrNull()
    }

    private companion object {
        const val KEY_TOKEN = "token"
        const val KEY_USER = "user"
    }
}
