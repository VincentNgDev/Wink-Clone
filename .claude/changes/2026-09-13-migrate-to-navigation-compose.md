# Migrate to a single Activity with Navigation Compose

## What

- `gradle/libs.versions.toml`: added `navigationCompose` (2.9.7) and
  `hiltLifecycleViewmodelCompose` (1.4.0) versions, plus the
  `androidx-navigation-compose`/`androidx-hilt-lifecycle-viewmodel-compose` libraries.
- `app/build.gradle.kts`: added `implementation(libs.androidx.navigation.compose)` and
  `implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)` (the latter is what makes
  the `hiltViewModel()` composable function available — see the Follow-up note on why this
  is a different artifact than `hilt-navigation-compose`).
- New `app/src/main/java/com/example/clonedwink/ui/navigation/WinkDestination.kt`: a sealed
  class of the app's routes (`Landing`, `Home`).
- New `app/src/main/java/com/example/clonedwink/ui/navigation/WinkNavHost.kt`: the `NavHost`
  that renders each destination, obtaining its `@HiltViewModel` via `hiltViewModel()` and
  wiring `LandingScreen`'s `onGetStartedClick` to `navController.navigate(Home) { popUpTo
  (Landing) { inclusive = true } }` — the in-process equivalent of the old
  `startActivity(...); finish()`.
- New `app/src/main/java/com/example/clonedwink/ui/MainActivity.kt`: the app's only Activity
  now — `@AndroidEntryPoint`, hosts `WinkNavHost()` inside `setContent { }`.
- Deleted `ui/landing/LandingActivity.kt` and `ui/home/HomeActivity.kt` — both screens are now
  composable destinations inside `MainActivity`/`WinkNavHost` instead of their own Activities.
  `LandingScreen.kt`/`HomeScreen.kt` (the composables) are unchanged.
- `AndroidManifest.xml`: one `<activity>` entry (`.ui.MainActivity`, MAIN/LAUNCHER) replacing
  the previous two.
- Updated `.claude/lessons/07-2026-09-08-navigation.md` with a new "Update (2026-09-13)"
  section describing this migration, since that lesson specifically predicted this move.

## Why

Requested: switch from the two-Activity, `Intent`-based navigation to
`androidx.navigation:navigation-compose`, and confirmed that this means collapsing to a
single-Activity architecture — each screen becomes a `NavHost` destination rather than its
own Activity, which is the standard pattern for this library.

## Follow-up / known limitations

- `hiltViewModel()` ended up importing from `androidx.hilt.lifecycle.viewmodel.compose`
  (artifact `hilt-lifecycle-viewmodel-compose`), not the more commonly-documented
  `androidx.hilt.navigation.compose` (artifact `hilt-navigation-compose`) — as of Hilt 1.4.0,
  the Compose `hiltViewModel()` API moved to this new artifact so it no longer pulls in a
  transitive dependency on `androidx.navigation` just to build a ViewModel. Using the old
  artifact still compiles but emits a deprecation warning pointing here.
- No back-stack state (e.g. scroll position) is being preserved across navigation beyond what
  `NavHost`/Compose already handle by default — not needed yet since there's no
  destination the user returns to.
- Verified with `gradlew.bat compileDebugKotlin` and `gradlew.bat build` (compile + lint +
  unit tests) — all pass. Not yet manually exercised on a device/emulator; the Landing →
  Home tap and Home's no-op click handlers behave identically to the pre-migration code,
  just routed through `NavHost` instead of an `Intent`.
