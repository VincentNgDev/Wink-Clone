# Lesson: The Android lifecycle

"The Android lifecycle" usually means two related but distinct things in this codebase:
the **Activity lifecycle** (a screen being created/shown/hidden/destroyed by the OS) and
the **ViewModel lifecycle** (state that survives an Activity being recreated). This
project's `LandingActivity` + `LandingViewModel` pair is a good concrete example of both.

## Update (2026-09-13): single Activity now; ViewModel scoping moved to the nav back stack

Two things below have changed since this lesson was written, both from
`.claude/changes/2026-09-13-migrate-to-navigation-compose.md` — see
[[13-2026-09-13-navigation-compose-vs-multi-activity]] for the full picture:

- **There's only one Activity instance for the whole app now**, `MainActivity` — not one
  per screen. Everything in section 1 about `onCreate`/`onStart`/`onResume`/rotation
  still happens exactly as described, it just now only ever happens to `MainActivity`,
  never to a per-screen `LandingActivity`/`HomeActivity` (both deleted).
- **Section 1's manual `lifecycleScope.launch { repeatOnLifecycle(STARTED) { viewModel.uiState.collect { } } }`
  pattern is gone from this codebase.** `LandingScreen`/`HomeScreen` are Compose
  functions now (see [[08-2026-09-09-jetpack-compose-and-lifecycle]]), and
  `WinkNavHost.kt` uses `viewModel.uiState.collectAsStateWithLifecycle()` instead — a
  Compose-aware helper that does the same "only collect while at least `STARTED`,
  pause/resume automatically" job as `repeatOnLifecycle`, just as one function call
  instead of a manually-nested block. The *reason* it exists (don't do work, or hold a
  collector, while the screen isn't visible) hasn't changed — only the API shape has.
- **Section 2's ViewModel scoping — "survives what the Activity doesn't" via
  `by viewModels { }`'s retained store — is now scoped to the nav back stack instead of
  the Activity.** `LandingViewModel` is obtained via `hiltViewModel()` inside
  `WinkNavHost`'s Landing `composable { }` block: it's created the first time that route
  is navigated to, survives rotation exactly as before (same underlying `ViewModelStore`
  mechanism), but is cleared when that `NavBackStackEntry` is popped off the back
  stack — which, in this app, happens to `LandingViewModel` almost immediately (Landing
  pops itself off on `popUpTo(...) { inclusive = true }` when navigating to Home), rather
  than only "when the Activity finishes for good." The underlying idea — a ViewModel
  outlives Activity recreation but not the logical screen going away — is unchanged;
  only *what counts as "the logical screen going away"* moved from "Activity finishes"
  to "back-stack entry is popped."

The sections below describe the pre-migration, one-Activity-per-screen,
manual-`repeatOnLifecycle` code — still an accurate description of *how those APIs work*
in general (this is genuinely useful knowledge for any Android code still using Views or
holding LiveData/Flow with the manual pattern), just no longer what this project's own
code currently does.

## 1. The Activity lifecycle

An Activity represents one screen. The **OS**, not your code, decides when to
create/start/resume/pause/stop/destroy one — on first launch, on rotation, when the user
presses Home, when the system needs memory back, etc. You hook into this by *overriding*
callback methods; you never call them yourself.

```
onCreate() → onStart() → onResume() → [screen visible & interactive]
                                            ↓ (user navigates away / backgrounds app)
                                       onPause() → onStop()
                                            ↓ (user returns)                    ↓ (finished for good, or OS reclaims memory)
                                       onRestart() → onStart() → onResume()   onDestroy()
```

- **`onCreate(savedInstanceState: Bundle?)`** — called once when the Activity instance
  is first constructed (and again, as a *new instance*, after a config change like
  rotation, unless you've opted out of that). This is where you inflate the layout and do
  one-time setup. In this project: `LandingActivity.onCreate()`
  (`ui/landing/LandingActivity.kt`) calls `setContentView(R.layout.activity_landing)`,
  looks up views, wires the ViewPager2 adapter, and starts observing the ViewModel.
- **`onStart()`** — screen is about to become visible (but maybe not interactive yet,
  e.g. partially covered).
- **`onResume()`** — screen is fully visible and in the foreground; user can interact
  with it.
- **`onPause()`** — another Activity is taking focus (e.g. a dialog appears, or the user
  is switching apps) — the current screen is still partially visible. Should be quick;
  don't do heavy work here.
- **`onStop()`** — screen is no longer visible at all (user pressed Home, switched
  apps, etc.).
- **`onDestroy()`** — Activity instance is being thrown away for good (user pressed
  Back, or the system is reclaiming resources). Final cleanup point.

This project doesn't override `onStart`/`onResume`/`onPause`/`onStop`/`onDestroy`
explicitly — it relies on a more modern, lifecycle-aware pattern instead (see below)
rather than manually starting/stopping work in each callback.

### The important nuance: rotation destroys and recreates the Activity

When the device rotates, Android by default **destroys the current Activity instance and
creates a brand-new one** (`onDestroy()` → new `onCreate()`). Any plain property on
`LandingActivity` (like `carouselAdapter`) is lost and rebuilt from scratch. This is
exactly the problem `ViewModel` exists to solve — see below.

### `repeatOnLifecycle` — the modern way to react to lifecycle state

Rather than manually starting/stopping a background job in `onStart()`/`onStop()`,
`LandingActivity` uses:

```kotlin
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state -> /* update views */ }
    }
}
```

(`ui/landing/LandingActivity.kt`, `observeUiState()` and `autoScroll()`)

- `lifecycleScope` is a `CoroutineScope` tied to the Activity — automatically cancelled
  in `onDestroy()`, so nothing here can leak past the screen being destroyed.
- `repeatOnLifecycle(STARTED)` **restarts** the block every time the Activity reaches
  `STARTED` (`onStart()` ran) and **cancels** it every time it drops below `STARTED`
  (`onStop()` ran). So collecting `viewModel.uiState` — and the carousel's auto-scroll
  loop — automatically pause while the app is backgrounded and resume when the user
  comes back, without any manual `onStart`/`onStop` overrides.

This is the currently-recommended pattern precisely because manually matching start/stop
work to `onStart`/`onStop` overrides is easy to get wrong (forgetting to cancel a job in
`onStop` is a classic memory/battery leak).

## 2. The ViewModel lifecycle (survives what the Activity doesn't)

`androidx.lifecycle.ViewModel` (`LandingViewModel` extends it, in
`viewmodel/landing/LandingViewModel.kt`) has a *different* lifecycle, scoped to the
"logical screen" rather than to one Activity *instance*:

- The **first** time `LandingActivity` asks for its ViewModel (via
  `by viewModels { ... }`), Android's `ViewModelProvider` constructs one and stashes it
  in a retained store that survives configuration changes.
- On rotation, `LandingActivity` is destroyed and a **new** instance is created — but
  that new instance's `by viewModels { ... }` fetches the **same** `LandingViewModel`
  instance from the store instead of building a new one. Any state already loaded into
  `_uiState` (e.g. the carousel slides) survives rotation for free.
- `ViewModel.onCleared()` is only called when the ViewModel is *really* done — e.g. the
  Activity finishes permanently (user pressed Back) — not on every rotation. That's also
  when `viewModelScope` (used in `LandingViewModel.loadSlides()`) is cancelled, so an
  in-flight coroutine started there is safely stopped if the screen is gone for good.

This is *why* `.claude/rules/coding-style.md` bans holding a `Context`/`View`/`Activity`
reference inside a ViewModel: those objects get destroyed on rotation, but the ViewModel
doesn't, so holding one would leak a destroyed Activity — a classic Android memory leak.

### `init { }` and when loading actually happens

```kotlin
init {
    loadSlides()
}
```

`init` blocks run once, as part of constructing the object — i.e. exactly once per
ViewModel *instance*. Because that instance survives rotation, `loadSlides()` runs once
per real "visit" to the landing screen, not once per rotation.

## 3. Putting it together for this feature

1. App launches → OS creates `LandingActivity` → `onCreate()` runs.
2. `by viewModels { ... }` either creates a new `LandingViewModel` (first time) or
   reuses the existing one (after rotation).
3. If newly created, the ViewModel's `init` block calls `loadSlides()`, which flips
   `isLoading = true` then asynchronously fetches slides and updates `_uiState`.
4. `LandingActivity.observeUiState()` collects `viewModel.uiState` (gated by
   `repeatOnLifecycle(STARTED)`) and updates the carousel/dots/error toast every time a
   new state is emitted — including immediately with whatever the *current* state already
   is, so a rotated-back-in Activity sees already-loaded slides instantly with no reload.

See [[05-2026-09-08-mvvm-architecture]] for how this View/ViewModel split maps onto the
MVVM layers, and [[06-2026-09-08-carousel-slider-walkthrough]] for the carousel-specific
mechanics.
