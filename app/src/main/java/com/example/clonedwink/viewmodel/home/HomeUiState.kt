package com.example.clonedwink.viewmodel.home

import com.example.clonedwink.data.model.home.HomeContent

// Android/MVVM: same "one immutable snapshot" shape as LandingUiState — see that file's
// comment. `content` defaults to an empty HomeContent() (see that class) so `HomeUiState()`
// with no arguments is a valid, ready-to-render "loading" state, exactly like `isLoading = true`
// on its own used to be.
data class HomeUiState(
    val isLoading: Boolean = true,
    val content: HomeContent = HomeContent(),
    val hasError: Boolean = false,
)
