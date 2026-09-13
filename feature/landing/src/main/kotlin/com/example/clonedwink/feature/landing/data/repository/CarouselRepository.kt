package com.example.clonedwink.feature.landing.data.repository

import com.example.clonedwink.core.model.CarouselSlide

/**
 * Source of the landing carousel's slides. [DefaultCarouselRepository] serves a hardcoded
 * list today; a future remote implementation (e.g. backed by `core:network`) can implement this
 * same interface and be swapped in without touching the ViewModel or UI.
 */
// Kotlin: this is an `interface` with no implementation — just a contract. Any class that
// writes `: CarouselRepository` must provide a body for getSlides(). This is what lets
// LandingViewModel depend on "something that can fetch slides" without caring whether that
// something is hardcoded data (today) or a network call (later) — a classic MVVM seam.
interface CarouselRepository {
    // Kotlin: `suspend` marks a function that can be paused and resumed without blocking
    // the thread it's called from — the compiler rewrites it under the hood so it can
    // `await` slow work (like a network request) cooperatively. A suspend function can only
    // be called from another suspend function or from a coroutine (e.g. viewModelScope.launch
    // in LandingViewModel). This one doesn't actually do I/O yet (DefaultCarouselRepository
    // reads local strings/drawables synchronously), but marking it suspend now means a real
    // network-backed implementation can be dropped in later without changing this signature.
    suspend fun getSlides(): List<CarouselSlide>
}
