package dev.polylex.sample

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.polylex.Polylex
import dev.polylex.PolylexException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SampleUiState {
    data object Loading : SampleUiState

    data class Active(
        val locale: String,
        val translationCount: Int,
    ) : SampleUiState

    data class Error(val message: String) : SampleUiState
}

class SampleViewModel : ViewModel() {

    private val _state = MutableStateFlow<SampleUiState>(SampleUiState.Loading)
    val state: StateFlow<SampleUiState> = _state.asStateFlow()

    fun refresh(force: Boolean = false) {
        viewModelScope.launch {
            try {
                if (force) Polylex.forceRefresh() else Polylex.refresh()
                _state.value = SampleUiState.Active(
                    locale = Polylex.activeLocale() ?: "?",
                    translationCount = Polylex.translationCount(),
                )
            } catch (e: PolylexException) {
                _state.value = SampleUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Pre-fetch the chosen locale; on success, invoke [onApplied] which the
     * activity uses to flip the AppCompat application locale and recreate
     * itself. Per the preflight pattern in the SDK README — we never commit a
     * locale switch the SDK can't actually serve.
     */
    fun switchToLocale(localeTag: String, onApplied: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val ok = Polylex.setActiveLocale(localeTag)
                if (ok) {
                    _state.value = SampleUiState.Active(
                        locale = Polylex.activeLocale() ?: localeTag,
                        translationCount = Polylex.translationCount(),
                    )
                }
                onApplied(ok)
            } catch (e: PolylexException) {
                _state.value = SampleUiState.Error(e.message ?: "Unknown error")
                onApplied(false)
            }
        }
    }
}
