package com.example.clonedwink.viewmodel.landing

import com.example.clonedwink.data.model.CarouselSlide
import com.example.clonedwink.data.repository.CarouselRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// Android/testing: this lives under app/src/test (JVM unit tests), not app/src/androidTest,
// because LandingViewModel never touches real Android framework classes (no Context/View) —
// see coding-style.md's ViewModel rules. That means it can run as a plain JVM test, no
// emulator/device required, which is what makes it fast enough to run on every change.

// Kotlin: `@OptIn(ExperimentalCoroutinesApi::class)` acknowledges we're using coroutines-test
// APIs (StandardTestDispatcher, setMain/resetMain) that are still marked experimental by the
// library authors — without this annotation the compiler would refuse to compile the file.
@OptIn(ExperimentalCoroutinesApi::class)
class LandingViewModelTest {

    // Android/testing: `viewModelScope` (used inside LandingViewModel) internally runs on
    // `Dispatchers.Main`, which normally requires a real Android main looper that doesn't
    // exist in a plain JVM test. `StandardTestDispatcher` is a fake dispatcher built for
    // tests: coroutines launched on it don't run immediately — they queue up until something
    // explicitly advances the test's virtual clock/queue (see `advanceUntilIdle()` below).
    private val dispatcher = StandardTestDispatcher()

    // Kotlin/JUnit: `@Before` marks a function JUnit runs before *every* @Test method in this
    // class — fresh setup per test, so tests can't leak state into each other.
    @Before
    fun setUp() {
        // Swaps out the real Dispatchers.Main for our fake one, for the duration of this test.
        Dispatchers.setMain(dispatcher)
    }

    // Kotlin/JUnit: `@After` runs after every @Test — used here for symmetric cleanup so the
    // Main dispatcher override from setUp() doesn't leak into other test classes.
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // Kotlin: a function name wrapped in backticks (`` `loads slides from repository on init` ``)
    // is a Kotlin-only feature allowing spaces/punctuation in identifiers — purely for
    // human-readable test names in JUnit's output; it has no special meaning to the compiler
    // beyond "here is a normal function whose name happens to be this sentence."
    //
    // Kotlin: `= runTest { ... }` — the whole test body is the trailing lambda passed to
    // runTest, which runs coroutines using a special TestScope where `delay()` calls are
    // skipped virtually instead of actually waiting, so tests run instantly even if the real
    // code has delays.
    @Test
    fun `loads slides from repository on init`() = runTest {
        val slides = listOf(
            CarouselSlide(id = "1", title = "Title", subtitle = "Subtitle", imageContentDescription = "desc"),
        )
        // Constructing LandingViewModel here triggers its `init { loadSlides() }` block (see
        // LandingViewModel.kt) immediately — but because Dispatchers.Main is our
        // StandardTestDispatcher, that launched coroutine doesn't actually run its body yet;
        // it's queued.
        val viewModel = LandingViewModel(FakeCarouselRepository(slides))

        // Runs every queued coroutine on `dispatcher` to completion before continuing — this
        // is what actually lets loadSlides()'s coroutine execute and update _uiState.
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        // JUnit: assertFalse/assertTrue/assertEquals compare an actual value against an
        // expected one and fail the test with a message if they don't match.
        assertFalse(state.isLoading)
        assertFalse(state.hasError)
        assertEquals(slides, state.slides)
    }

    @Test
    fun `surfaces an error flag when the repository throws`() = runTest {
        val viewModel = LandingViewModel(FakeCarouselRepository(slides = null))

        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.hasError)
        assertTrue(state.slides.isEmpty())
    }

    // Kotlin: a private class nested inside the test class — a hand-written "fake" that
    // implements the real CarouselRepository interface (see CarouselRepository.kt) but with
    // fully controllable behavior, so these tests never touch a real Context or network call.
    // `slides: List<CarouselSlide>?` being nullable is what lets one fake double as both the
    // "happy path" (pass a real list) and "failure path" (pass null) fixture used above.
    private class FakeCarouselRepository(private val slides: List<CarouselSlide>?) : CarouselRepository {
        override suspend fun getSlides(): List<CarouselSlide> =
            // Kotlin: `slides ?: throw IllegalStateException(...)` — the Elvis operator's
            // right-hand side doesn't have to be a plain value; `throw` is itself a Kotlin
            // expression (of type Nothing), so this reads as "return slides, or if it's null,
            // throw instead." This is what LandingViewModel's `runCatching { }.onFailure { }`
            // in loadSlides() is exercising in the second test above.
            slides ?: throw IllegalStateException("boom")
    }
}
