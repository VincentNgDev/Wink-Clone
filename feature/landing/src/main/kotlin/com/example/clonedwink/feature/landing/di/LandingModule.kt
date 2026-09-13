package com.example.clonedwink.feature.landing.di

import com.example.clonedwink.feature.landing.data.repository.CarouselRepository
import com.example.clonedwink.feature.landing.data.repository.DefaultCarouselRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// Android: this is where "when something asks for a CarouselRepository, hand it a
// DefaultCarouselRepository" gets decided, written once for the whole app instead of once per
// screen. It lives in `feature:landing` (not `app`) now that the module split means only this
// module — and things depending on it — need to know CarouselRepository even exists; `app`
// never binds it directly, but Hilt still discovers this @Module because `app` depends on
// `feature:landing`, which is enough for Hilt's aggregating step (run in the `app` module, since
// that's where `@HiltAndroidApp` lives) to pick it up.
//
// Kotlin: `@Module` marks this as a place Hilt should look for dependency-providing code.
// `@InstallIn(SingletonComponent::class)` says these bindings live in the app-wide container
// created by `WinkApplication`'s `@HiltAndroidApp` — so every binding here is effectively a
// singleton, shared across the whole app for as long as the process runs (as opposed to, say,
// `ActivityComponent::class`, which would recreate the binding per-Activity).
//
// Android: this is an `abstract class` with only `abstract` functions — nothing here is ever
// actually called at runtime. Hilt reads the *signatures* at compile time (via the KSP
// annotation processor configured in this module's build.gradle.kts) to generate real code
// elsewhere that does the constructing. `@Binds` is the lightest way to tell it "the interface's
// implementation is this concrete class" — cheaper than writing a `@Provides fun bindX(): X {
// return Y() }` method for the same thing, since `DefaultCarouselRepository` already declares an
// `@Inject constructor` itself (see that file).
@Module
@InstallIn(SingletonComponent::class)
abstract class LandingModule {

    // Kotlin: an abstract function whose parameter is the concrete implementation and whose
    // return type is the interface — Hilt reads this as "build a DefaultCarouselRepository (it
    // knows how, from its @Inject constructor) whenever a CarouselRepository is requested."
    @Binds
    abstract fun bindCarouselRepository(impl: DefaultCarouselRepository): CarouselRepository
}
