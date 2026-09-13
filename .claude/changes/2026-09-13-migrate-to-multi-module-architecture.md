# Migrate to a multi-module Gradle structure

## What

Restructured the project from a single `:app` module into `core/*` + `feature/*` Gradle
modules:

- `core:model` — shared data classes (`CarouselSlide`, `HomeContent` and friends), moved from
  `app`'s `data.model` package.
- `core:ui` — the shared Compose theme (`ClonedWinkTheme`) and the two composables genuinely
  reused across features (`CarouselAutoScroll`, `CarouselDotIndicator`), plus the brand color
  palette (`colors.xml`) and the one dimension both carousels share (`carousel_dot_spacing`).
- `core:network`, `core:database` — empty scaffold modules, wired into `settings.gradle.kts`
  but with no source yet (nothing in this app does real networking or persistence today).
- `feature:landing` — the existing onboarding-carousel screen (`LandingScreen`,
  `LandingViewModel`, `CarouselRepository`/`DefaultCarouselRepository`), unchanged in behavior,
  moved out of `app` with its own Hilt module (`LandingModule`).
- `feature:home` — the existing home screen (`HomeScreen`, `HomeViewModel`,
  `HomeRepository`/`DefaultHomeRepository`), unchanged in behavior, moved out of `app` with its
  own Hilt module (`HomeModule`). `SectionHeader`/`HorizontalCardSection` moved here (not
  `core:ui`) since they're only ever used by Home.
- `feature:auth`, `feature:profile` — empty scaffold modules for future login/signup and
  profile screens; no screens exist for either yet.
- `app` now only holds `WinkApplication`, `MainActivity`, and `WinkNavHost`/`WinkDestination` —
  the pieces that wire the feature modules together — plus the Hilt+KSP aggregating root
  (`@HiltAndroidApp` requires the app module to run Hilt's aggregating annotation processing
  step over every module's `@Module` contributions).

Packages were renamed to mirror the new module paths (e.g. `data.model` → `core.model`,
`ui.home` → `feature.home.ui`). Resources were split by module: colors/theme (cross-feature) to
`core:ui`; per-feature strings/dimens to their owning feature module; `app_name` and the two
`error_loading_*` Toast strings (only ever read from `WinkNavHost`, which stays in `app`) to
`app`.

## Why

The user wanted to learn how a real production Android app's MVVM layers map onto Gradle
modules instead of just packages, and asked for the standard `core/*` + `feature/*` layout.

## Follow-up / known limitations

- `core:network` and `core:database` are empty on purpose — no repository does real I/O yet, so
  there was nothing to migrate into them. They exist so future lessons can add Retrofit/Room
  without another restructuring pass.
- `feature:auth` and `feature:profile` are empty scaffolds — the onboarding flow (an
  introduction carousel, not a login form) stayed as `feature:landing` rather than being
  shoehorned into `feature:auth`.
- Several files now import two `R` classes (their own feature module's, plus `core:ui`'s
  aliased as `CoreUiR`) because AGP's non-transitive R classes mean cross-module resource
  references can't share one `R` import. See `LandingScreen.kt`, `HomeScreen.kt`,
  `HomeComponents.kt`, `HomeSections.kt`, and `SectionHeader.kt`.
- Moving `CarouselSlide`/`PlaceCard`/`FeatureCard` into a separate module (`core:model`)
  surfaced a real Kotlin restriction: the compiler won't smart-cast a nullable property
  declared in a *different* module, so a few call sites (`LandingScreen.GlassSlideCard`,
  `HomeSections.PlaceCardView`/`FeatureCardView`) now copy the property into a local `val`
  before branching on it.
- `app`'s already-unused legacy View-system dependencies (`appcompat`, `material`, `viewpager2`,
  `recyclerview`, `constraintlayout` — leftover from before the Compose rewrite) were left as-is;
  removing them is unrelated cleanup outside this migration's scope.
