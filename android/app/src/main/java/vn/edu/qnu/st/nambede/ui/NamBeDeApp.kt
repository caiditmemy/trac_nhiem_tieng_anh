package vn.edu.qnu.st.nambede.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlin.coroutines.cancellation.CancellationException
import vn.edu.qnu.st.nambede.AppContainer
import vn.edu.qnu.st.nambede.ui.login.LoginRoute

/** Signed out: the login screen. Signed in: the student's screens. */
@Composable
fun NamBeDeApp(container: AppContainer) {
    val user by container.session.user.collectAsState()

    LaunchedEffect(Unit) {
        // Refresh the saved user (name, FREE / VIP); an expired token answers 401 and signs the user out
        if (container.session.token != null) {
            try {
                container.session.update(container.client.call { me() })
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                // Offline: keep the saved user for now
            }
        }
    }

    val current = user
    if (current == null) LoginRoute(container) else MainScaffold(container, current)
}
