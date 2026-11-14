package vn.edu.qnu.st.nambede

import android.app.Application
import android.content.Context
import vn.edu.qnu.st.nambede.data.ApiClient
import vn.edu.qnu.st.nambede.data.SessionStore

/** One session and one API client for the whole app. */
class AppContainer(context: Context) {
    val session = SessionStore(context)
    val client = ApiClient(BuildConfig.API_BASE_URL, session)
}

/** Keeps the container alive across screen rotations, so every screen sees the same session. */
class NamBeDeApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
