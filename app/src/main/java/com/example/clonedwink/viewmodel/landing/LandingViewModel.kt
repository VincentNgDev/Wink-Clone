package com.example.clonedwink.viewmodel.landing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clonedwink.data.repository.CarouselRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// Android: extending androidx.lifecycle.ViewModel gives this class a lifecycle that
// *outlives* any single Activity/Fragment instance. When the device rotates, LandingActivity
// is destroyed and recreated, but the same LandingViewModel instance survives (the framework
// hands the new Activity back the existing ViewModel instead of constructing a fresh one) —
// so in-flight state (loaded slides, isLoading, etc.) isn't lost on rotation. It's only
// destroyed for real (onCleared() called, viewModelScope cancelled) when the Activity finishes
// permanently. This is also why coding-style.md bans Context/View/Activity references inside
// a ViewModel — none of that survives rotation the same way, so holding one here would leak it.
//
// Kotlin: the constructor takes a `CarouselRepository` *interface*, not the concrete
// DefaultCarouselRepository — dependency inversion. Hilt decides which concrete implementation
// to hand in (see di/RepositoryModule.kt's `@Binds` and the `@HiltViewModel`/`@Inject` below);
// this class only needs to know "I can call getSlides() on whatever I was given," which is also
// what makes LandingViewModelTest able to pass in a FakeCarouselRepository directly (constructed
// with plain `LandingViewModel(fake)` — no Hilt involved at all in a JVM unit test).
//
// Android/Kotlin: `@HiltViewModel` marks this ViewModel as one Hilt knows how to build, and
// `@Inject constructor(...)` says how — "give me a CarouselRepository from the dependency graph
// and call this constructor." Together, this is what lets LandingActivity just write
// `by viewModels()` with no factory lambda at all (see that file): Hilt generates the factory
// that used to be hand-written there.
@HiltViewModel
class LandingViewModel @Inject constructor(
    private val carouselRepository: CarouselRepository,
) : ViewModel() {

    // Kotlin/Android: `MutableStateFlow` is a hot, observable holder of a single current
    // value (like LiveData, but a native Kotlin coroutines type). It always has a value
    // (seeded here with `LandingUiState()`, i.e. isLoading=true/no slides/no error) and every
    // collector immediately gets the latest one plus every future update.
    //
    // Kotlin: the leading underscore (`_uiState`) is a common convention for "the private,
    // mutable backing property" that a public, read-only property is derived from below.
    private val _uiState = MutableStateFlow(LandingUiState())

    // Kotlin: `asStateFlow()` exposes the same underlying flow but typed as the read-only
    // `StateFlow` interface (no `.value = ` setter, no `.update { }`). This is the
    // encapsulation pattern behind "expose state, not events" from coding-style.md: the View
    // (LandingActivity) can only *read* uiState; only this ViewModel can mutate it via
    // `_uiState`.
    val uiState: StateFlow<LandingUiState> = _uiState.asStateFlow()

    // Android/Kotlin: `init { }` is an initializer block — it runs once, as part of
    // constructing the object, right after the primary constructor's parameters are set.
    // Because ViewModel construction happens the first time the View asks for it (see
    // LandingActivity's `by viewModels { ... }`) and is *not* repeated across rotations, this
    // kicks off loading exactly once per ViewModel lifetime, not once per Activity recreation.
    init {
        loadSlides()
    }

    fun loadSlides() {
        // Android: `viewModelScope` is a CoroutineScope tied to this ViewModel's lifecycle —
        // it's automatically cancelled in ViewModel.onCleared(), so if the screen is torn down
        // for good mid-load, this coroutine (and any network call inside it) stops instead of
        // leaking. `.launch { }` starts a new coroutine running the block below.
        viewModelScope.launch {
            // Kotlin: `_uiState.update { it.copy(...) }` is the safe way to change a
            // MutableStateFlow's value from (possibly) multiple coroutines: `update` takes the
            // current value as `it` and atomically replaces it with whatever the lambda
            // returns. `it.copy(isLoading = true, hasError = false)` — `copy()` comes free
            // with `data class` (see LandingUiState) and returns a new instance with only the
            // named fields changed, leaving `slides` as whatever it already was.
            _uiState.update { it.copy(isLoading = true, hasError = false) }

            // Kotlin: `runCatching { ... }` runs the block and wraps the outcome in a Result —
            // success or the thrown exception — instead of using try/catch. `.onSuccess { }`
            // and `.onFailure { }` are trailing-lambda callbacks chained onto that Result; only
            // one of the two ever runs, functioning like a try/catch/else in one expression.
            runCatching { carouselRepository.getSlides() }
                .onSuccess { slides -> _uiState.update { it.copy(isLoading = false, slides = slides) } }
                .onFailure { _uiState.update { it.copy(isLoading = false, hasError = true) } }
        }
    }
}
