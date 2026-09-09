# Add landing page with carousel slider

## What

- Added the first screen to the app: `LandingActivity` (`ui/landing/`), declared as the
  launcher activity in `AndroidManifest.xml`.
- Added a `ViewPager2` + `TabLayout` dot-indicator carousel (`activity_landing.xml`,
  `item_carousel_slide.xml`, `CarouselAdapter`) with a `Button` "Get started" CTA.
- Added the MVVM layers backing the carousel:
  - `data/model/CarouselSlide.kt` — plain data class; supports either a bundled
    `imageRes` or an `imageUrl` string so the same model works for hardcoded and
    API-sourced content.
  - `data/repository/CarouselRepository.kt` (interface) and
    `data/repository/DefaultCarouselRepository.kt` — a hardcoded implementation
    returning 3 slides built from `strings.xml` + placeholder drawables. A remote
    implementation (e.g. Retrofit-backed) can implement the same interface and be
    swapped in via the `LandingActivity` view-model factory without touching the
    ViewModel or UI.
  - `viewmodel/landing/LandingUiState.kt` and `LandingViewModel.kt` — exposes a single
    `StateFlow<LandingUiState>` (`isLoading`, `slides`, `hasError`); loads slides via
    `viewModelScope` on init.
- Added `LandingViewModelTest` (JVM unit test) covering the success and repository-error
  paths using `kotlinx-coroutines-test`.
- Added dependencies to `gradle/libs.versions.toml` /  `app/build.gradle.kts`:
  lifecycle-runtime-ktx, lifecycle-viewmodel-ktx, activity-ktx, viewpager2,
  recyclerview, constraintlayout, Coil 3 (`coil-android` + `coil-network-okhttp`) for
  image loading, and `kotlinx-coroutines-test`.
- Added carousel-related strings, colors, dimens, and drawables (placeholder slide
  backgrounds, bottom gradient scrim, dot indicator selector).

## Why

First screen for the app, per the request for a landing page whose carousel is bound to
a list that can be swapped between hardcoded content and a future API without changing
the View or ViewModel — the `CarouselRepository` interface is the seam for that swap.

## Follow-up / known limitations

- The 3 hardcoded slides use solid-color placeholder drawables, not real artwork/photos.
- No remote implementation of `CarouselRepository` exists yet (no networking library is
  wired up in this repo) — `imageUrl` support in `CarouselSlide`/`CarouselAdapter` is
  ready for it, but a Retrofit-based repository still needs to be added when there's a
  real API to call.
- The "Get started" button has no click handler yet — no destination screen exists.
- Not yet build-verified in this environment (no local Android SDK/Gradle run performed
  as part of this change); dependency versions were confirmed against Maven Central but
  a full `gradlew.bat build` should be run before relying on this.
