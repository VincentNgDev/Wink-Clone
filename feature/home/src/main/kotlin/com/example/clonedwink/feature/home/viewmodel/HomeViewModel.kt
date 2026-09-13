package com.example.clonedwink.feature.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clonedwink.feature.home.data.repository.HomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// Android/Kotlin: this is the same shape as LandingViewModel — see that file's comment for the
// full explanation of why extending ViewModel (surviving rotation), depending on the
// HomeRepository *interface* (not DefaultHomeRepository directly, so tests can substitute a
// fake), exposing `uiState` as a read-only StateFlow, and what `@HiltViewModel`/`@Inject`
// constructor do, all matter here too.
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeContent()
    }

    fun loadHomeContent() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, hasError = false) }

            runCatching { homeRepository.getHomeContent() }
                .onSuccess { content -> _uiState.update { it.copy(isLoading = false, content = content) } }
                .onFailure { _uiState.update { it.copy(isLoading = false, hasError = true) } }
        }
    }
}
