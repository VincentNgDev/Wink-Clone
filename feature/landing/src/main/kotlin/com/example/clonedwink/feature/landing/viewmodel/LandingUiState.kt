package com.example.clonedwink.feature.landing.viewmodel

import com.example.clonedwink.core.model.CarouselSlide

// Android/MVVM: this is the *entire* renderable state of the landing screen, bundled into
// one immutable snapshot. Instead of the View asking "is it loading? did it error? what are
// the slides?" as three separate mutable fields it could read at inconsistent times, the
// ViewModel emits one LandingUiState at a time and the View just renders whatever it gets.
// See LandingViewModel's uiState StateFlow and WinkNavHost's LandingScreen call site.
//
// Kotlin: `data class` again (see core:model's CarouselSlide.kt) — and because every property
// has a default value, `LandingUiState()` with no arguments is a valid, ready-to-use "initial/
// loading" state. That's exactly how LandingViewModel seeds `_uiState` below.
data class LandingUiState(
    val isLoading: Boolean = true,
    val slides: List<CarouselSlide> = emptyList(),
    val hasError: Boolean = false,
)
