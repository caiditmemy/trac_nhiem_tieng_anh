package vn.edu.qnu.st.nambede.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.edu.qnu.st.nambede.data.errorText

/** Data from the gateway: still loading, failed (message in Vietnamese), or there. Old data stays while reloading. */
data class Remote<T>(val data: T? = null, val loading: Boolean = true, val error: String? = null)

/** A screen that loads one thing and can reload it (pull to refresh, "Thử lại"). */
abstract class LoadingViewModel<T> : ViewModel() {
    private val _state = MutableStateFlow(Remote<T>())
    val state: StateFlow<Remote<T>> = _state.asStateFlow()

    protected abstract suspend fun fetch(): T

    /** Vietnamese wording for statuses that mean something special on this screen. */
    protected open val errorOverrides: Map<Int, String> = emptyMap()

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val data = fetch()
                _state.value = Remote(data = data, loading = false)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _state.update { it.copy(loading = false, error = errorText(error, errorOverrides)) }
            }
        }
    }
}
