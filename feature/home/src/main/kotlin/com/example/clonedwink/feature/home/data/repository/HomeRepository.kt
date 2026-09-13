package com.example.clonedwink.feature.home.data.repository

import com.example.clonedwink.core.model.home.HomeContent

/**
 * Source of everything the home screen renders. [DefaultHomeRepository] serves hardcoded mock
 * content today; a future remote implementation (`core:network`-backed) can implement this same
 * interface and be swapped in without touching HomeViewModel or any Compose UI — the same seam
 * [com.example.clonedwink.feature.landing.data.repository.CarouselRepository] gives the landing
 * screen.
 */
interface HomeRepository {
    // Kotlin: `suspend` again — see CarouselRepository.kt's comment for the full explanation.
    // Nothing here actually does I/O yet, but marking it suspend now means a real network call
    // can replace DefaultHomeRepository later without changing this signature or HomeViewModel.
    suspend fun getHomeContent(): HomeContent
}
