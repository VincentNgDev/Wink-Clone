package com.example.clonedwink.di

import com.example.clonedwink.data.repository.CarouselRepository
import com.example.clonedwink.data.repository.DefaultCarouselRepository
import com.example.clonedwink.data.repository.DefaultHomeRepository
import com.example.clonedwink.data.repository.HomeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// Android: this is the piece that replaces the `viewModelFactory { initializer { ... } }` blocks
// that used to live in LandingActivity/HomeActivity — it's where "when something asks for a
// CarouselRepository, hand it a DefaultCarouselRepository" gets decided, but written *once* for
// the whole app instead of once per Activity.
//
// Kotlin: `@Module` marks this as a place Hilt should look for dependency-providing code.
// `@InstallIn(SingletonComponent::class)` says these bindings live in the app-wide container
// created by `WinkApplication`'s `@HiltAndroidApp` — so every binding here is effectively a
// singleton, shared across the whole app for as long as the process runs (as opposed to, say,
// `ActivityComponent::class`, which would recreate the binding per-Activity).
//
// Android: this is an `abstract class` with only `abstract` functions — nothing here is ever
// actually called at runtime. Hilt reads the *signatures* at compile time (via the KSP
// annotation processor configured in app/build.gradle.kts) to generate real code elsewhere that
// does the constructing. `@Binds` is the lightest way to tell it "the interface's implementation
// is this concrete class" — cheaper than writing a `@Provides fun bindX(): X { return Y() }`
// method for the same thing, since `DefaultCarouselRepository`/`DefaultHomeRepository` already
// declare an `@Inject constructor` themselves (see those files).
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    // Kotlin: an abstract function whose parameter is the concrete implementation and whose
    // return type is the interface — Hilt reads this as "build a DefaultCarouselRepository (it
    // knows how, from its @Inject constructor) whenever a CarouselRepository is requested."
    @Binds
    abstract fun bindCarouselRepository(impl: DefaultCarouselRepository): CarouselRepository

    @Binds
    abstract fun bindHomeRepository(impl: DefaultHomeRepository): HomeRepository
}
