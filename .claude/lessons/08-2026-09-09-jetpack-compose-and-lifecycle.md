# Lesson: Jetpack Compose — what it is, how it's set up, and its lifecycle

This lesson is ordered by **conceptual dependency**, i.e. what you need to understand
before the next section makes sense — which also happens to track roughly how important
each piece is day-to-day:

1. What Compose *is* and why it replaced the old View system here (the "why bother").
2. How it's installed (you can't use what isn't wired into the build).
3. `@Composable` functions + state/recomposition (the one idea everything else is built
   on — get this wrong and nothing downstream makes sense).
4. The **Composition lifecycle** (a Compose-specific concept, easy to confuse with #5).
5. How the Composition lifecycle relates to the **Android Activity lifecycle** you
   already know from [[03-2026-09-08-android-lifecycle]] — the question you actually
   asked about, but it only makes sense once #3 and #4 are in place.
6. A full walkthrough of this project's real landing screen tying all of it together.

This project's landing screen was originally built with XML layouts + `findViewById`
(see [[04-2026-09-08-drawing-the-ui]]) and was later **rebuilt entirely in Compose**
(`.claude/changes/2026-09-08-rebuild-landing-in-compose-glassmorphism.md`). Every code
example below is real code from that rebuild, not a toy snippet.

## 1. What is Jetpack Compose?

Jetpack Compose is Android's modern **declarative** UI toolkit, and is now Google's
recommended default for new UI (the older "View system" from
[[04-2026-09-08-drawing-the-ui]] is still fully supported, but Compose is where new
investment goes). The difference is a fundamental shift in *how you describe a screen*:

| | Old View system | Jetpack Compose |
|---|---|---|
| You describe UI as | XML files (`activity_landing.xml`) | Kotlin functions (`LandingScreen.kt`) |
| Putting it on screen | `setContentView(R.layout...)` inflates a View tree once | `setContent { }` hands Compose a `@Composable` function |
| Updating the screen when data changes | You imperatively mutate views: `carouselAdapter.submitList(...)`, `textView.text = ...` | You just re-run the function with new data; Compose figures out what changed |
| Looking up a view | `findViewById(R.id.carouselViewPager)` | There's no view to look up — the function's parameters *are* the data |
| Mental model | "Build the tree once, then poke at it" | "Describe the current UI as a pure function of current state; redraw whenever state changes" |

The old model's core weakness, once an app has real state, is that *you* are
responsible for keeping every mutated View in sync with the data by hand
(`observeUiState()` in the old `LandingActivity` manually pushed every field:
`carouselAdapter.submitList(...)`, then `carouselIndicator.visibility = ...`, then
`Toast.makeText(...)` — three separate imperative updates, one per piece of state). Miss
one and the screen shows stale data. Compose's promise: **you never mutate a view
directly — you just describe what the UI should look like for the current state, and
Compose (via *recomposition*, section 3) figures out the minimal update itself.**

## 2. Installing Compose (how this project did it, and the generic checklist)

Compose needs three things wired into the Gradle build. This project's actual setup:

**a) The Compose Compiler Gradle plugin**, applied at the root (`build.gradle.kts:1-4`)
and to the app module (`app/build.gradle.kts:1-4`):

```kotlin
// build.gradle.kts (root) — declared but not applied here; each module opts in itself
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
}
```

```kotlin
// app/build.gradle.kts — actually applied to this module
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}
```

The Compose compiler is a **Kotlin compiler plugin** — it rewrites `@Composable`
functions at compile time to support recomposition (section 3). Because this plugin
hooks into `kotlinc` directly, it still needs a Kotlin Gradle Plugin version even though
this project never applies the separate `org.jetbrains.kotlin.android` plugin (AGP 9's
built-in Kotlin support supplies that). See the comment above `kotlin = "2.2.10"` in
`gradle/libs.versions.toml` for why that version is pinned to match.

**b) `buildFeatures.compose = true`** (`app/build.gradle.kts:37-39`) — this is the
switch that actually makes `androidx.compose.*` dependencies usable and lets
`setContent { }` accept `@Composable` lambdas.

**c) The dependencies themselves** (`gradle/libs.versions.toml` + `app/build.gradle.kts`,
per `.claude/rules/coding-style.md`'s "declare in the version catalog, not inline"
rule):

```kotlin
// app/build.gradle.kts
implementation(platform(libs.androidx.compose.bom))   // pins every compose-* version below together
implementation(libs.androidx.compose.ui)
implementation(libs.androidx.compose.ui.graphics)
implementation(libs.androidx.compose.ui.tooling.preview)
implementation(libs.androidx.compose.material3)        // Material Design 3 components (Text, Icon, CircularProgressIndicator, ...)
implementation(libs.androidx.compose.material.icons.extended)
implementation(libs.androidx.activity.compose)          // gives ComponentActivity its setContent{} extension function
implementation(libs.androidx.lifecycle.runtime.compose) // gives collectAsStateWithLifecycle() (section 5)
debugImplementation(libs.androidx.compose.ui.tooling)   // only needed for @Preview rendering, stripped from release builds
```

The **BOM** (Bill of Materials) is worth calling out: `platform(libs.androidx.compose.bom)`
tells Gradle "read this artifact for version numbers only" — so none of the other
`compose-*` lines need their own `version.ref`, and bumping one `composeBom` entry in
`libs.versions.toml` upgrades the whole family together instead of you hand-matching
compatible versions of `ui`, `material3`, etc.

**Generic checklist**, if you were adding Compose to a brand-new module: apply the
Compose compiler plugin → set `buildFeatures.compose = true` → add the BOM +
`ui`/`ui-tooling-preview`/`material3`/`activity-compose` → make your Activity extend
`ComponentActivity` (or `AppCompatActivity`, which also supports it) → call
`setContent { }` instead of `setContentView(...)`.

## 3. `@Composable` functions, state, and recomposition — the one idea everything builds on

A `@Composable` function describes a piece of UI. `LandingScreen` (`ui/landing/LandingScreen.kt:100-105`):

```kotlin
@Composable
fun LandingScreen(
    uiState: LandingUiState,
    onGetStartedClick: () -> Unit,
    onErrorShown: () -> Unit,
) { /* ... */ }
```

Two things to notice immediately, both direct continuations of the MVVM rules from
[[05-2026-09-08-mvvm-architecture]]:

- **It's a plain function, not a class**, and it can only be called from inside another
  `@Composable` function (or `setContent { }`). Compose UI is built by composing many
  small functions calling each other — `LandingScreen` calls `SlideCarousel`, which
  calls `GlassSlideCard`, which calls `Icon`/`Text`/`Image` — instead of a class
  hierarchy of View objects.
- **It's *stateless***: `uiState` comes in as a parameter, `onGetStartedClick`/
  `onErrorShown` go out as lambdas. `LandingScreen` never reaches into `data/` or
  `LandingViewModel` directly — it's the exact same "View never talks to the Model,
  ViewModel decides, View only renders" rule as before, just expressed as function
  parameters instead of `findViewById` + a class implementing an interface.

### Recomposition: how the screen actually updates

**Recomposition** is Compose re-running a `@Composable` function to produce updated UI
after some piece of state it read has changed. This is the mechanism that replaces every
manual `view.text = "..."` from the old system. Critically, Compose is allowed to:

- **Skip** re-running a function whose inputs didn't change (this is *why* this project
  splits the screen into small private composables like `PageIndicator`,
  `GetStartedButton`, `BackgroundBlobs` instead of one giant function — smaller functions
  mean smaller, cheaper recomposition scopes when only one piece of state changes).
- Run functions **in any order**, and **run them multiple times**, or not at all.

This is exactly why a `@Composable` function must be **free of side effects** — it
cannot assume it runs exactly once, in order, top to bottom, the way a normal Kotlin
function does. (Section 4 covers the escape hatch for real side effects — network calls,
starting a coroutine loop — that must run in a controlled way despite this.)

### `remember` + `State<T>` — what makes a value "trigger" recomposition

Reading a `State<T>` value inside a composable's body **subscribes** that composable to
future changes of that value. `LandingActivity.kt:73`:

```kotlin
val uiState by viewModel.uiState.collectAsStateWithLifecycle()
```

`by` here is the same delegated-property syntax explained in
[[03-2026-09-08-android-lifecycle]] for `by viewModels { }`, applied to a different kind
of delegate: `collectAsStateWithLifecycle()` returns a `State<LandingUiState>`, and `by`
makes `uiState` read as a plain `LandingUiState` while secretly going through that
`State` object's getter every time. Every time `LandingViewModel`'s `StateFlow` emits a
new value, this `State` updates, and every composable that *read* `uiState` (directly or
via a parameter) is scheduled for recomposition. `PageIndicator`, which never reads
`uiState`, is *not* recomposed just because a new slide loaded — only the composables
that actually touched the changed value are.

`animateDpAsState` (`LandingScreen.kt:424-428`, the selected dot's pill animation) works
the same way: it returns a `State<Dp>` that smoothly ticks toward a new target on every
frame, and each tick is itself a state change that triggers recomposition — that's what
makes the animation visible instead of jumping straight to the final width.

## 4. Effects: the controlled way to run real side effects

Section 3 said `@Composable` functions must have no side effects. But *some* code
genuinely needs to run exactly once, or start a coroutine, or clean something up — a
network call, an auto-scroll timer loop, a `Toast`. Compose's **effect APIs** are the
sanctioned way to do this, and they're all scoped to the Composition lifecycle covered
next in section 5.

**`LaunchedEffect(keys...)`** — runs a suspend block, and (this is the important part)
**cancels and restarts it whenever any key changes**, or cancels it for good when the
calling composable leaves the Composition. Two real uses in this project:

```kotlin
// LandingScreen.kt:110-112 — a "run once per state change" effect
LaunchedEffect(uiState.hasError) {
    if (uiState.hasError) onErrorShown()
}
```

Keyed on `uiState.hasError`, so this block only re-runs when that specific boolean
*changes* value — not on every recomposition — mirroring how the pre-Compose
`StateFlow` collector only reacted to genuinely new emissions.

```kotlin
// LandingScreen.kt:237-246 — a long-running coroutine loop, gated by the Android lifecycle
LaunchedEffect(slides.size, lifecycleOwner) {
    lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        if (slides.size <= 1) return@repeatOnLifecycle
        while (true) {
            delay(AUTO_SCROLL_INTERVAL_MS)
            pagerState.animateScrollToPage((pagerState.currentPage + 1) % slides.size)
        }
    }
}
```

This is the auto-scroll loop for the carousel — section 5 covers the
`repeatOnLifecycle` part in detail, since that's exactly where Compose's lifecycle and
Android's lifecycle meet.

**`DisposableEffect(keys...)`** — not used in this file, but worth knowing: like
`LaunchedEffect`, but for non-suspending setup/teardown pairs (e.g. registering a
`BroadcastReceiver` or a listener). Its block must end with an `onDispose { }` block that
runs when the composable leaves the Composition — the Compose equivalent of pairing
`onStart`/`onStop` overrides in the old Activity system.

Both effects share one rule: **the "start" work and its cleanup are tied together by
Compose, not by you remembering to write both halves** — the classic "forgot to cancel
in `onStop`" leak from [[03-2026-09-08-android-lifecycle]] is structurally harder to
cause here, since `LaunchedEffect`'s cancellation is automatic.

## 5. The Composition lifecycle — a *different* lifecycle from the Activity's

This is the concept most likely to get confused with the Android lifecycle you already
know, so it's worth naming precisely. A **Composition** is the tree of UI that a
`setContent { }` call (or a parent composable) produces and keeps up to date. It has its
own three-stage lifecycle, independent of any single Activity callback:

```
setContent { ... } first runs
        │
        ▼
  Enter the Composition          (this composable's code runs for the first time —
        │                         "initial composition"; remember{} initializes,
        │                         LaunchedEffect/DisposableEffect blocks start)
        ▼
   Recompose (0+ times)          (re-runs when a State value it read changes —
        │  ◄───────────┐          section 3 — can happen any number of times, or never)
        │              │
        └──────────────┘
        ▼
   Leave the Composition          (this composable is removed from the UI tree —
                                   e.g. an `if` branch stops calling it, or the whole
                                   Activity is destroyed — LaunchedEffect coroutines are
                                   cancelled, DisposableEffect.onDispose{} fires)
```

Concretely in this project: `LandingScreen`'s loading branch
(`LandingScreen.kt:167-182`) only calls `CircularProgressIndicator` while
`uiState.isLoading && uiState.slides.isEmpty()`. The moment slides finish loading, that
composable **leaves the Composition** (the `if` no longer calls it) and `SlideCarousel`
**enters the Composition** for the first time — each has its own independent enter →
recompose → leave lifecycle, nested inside the outer `LandingScreen` composable's own
lifecycle.

Layered on top of *entering/recomposing/leaving*, every frame that does redraw work
passes through three phases, in this fixed order: **Composition** (run the `@Composable`
functions, decide *what* UI to emit) → **Layout** (measure and place everything) →
**Drawing** (actually paint pixels). This is *why* `GlassSlideCard`'s peeking-card
scale/fade effect reads `pagerState.currentPageOffsetFraction` inside the
`graphicsLayer { }` block (`LandingScreen.kt:268-274`) rather than as a plain `val` in
the composable body: `currentPageOffsetFraction` changes on every scroll frame, and
`graphicsLayer`'s lambda runs in the **Drawing** phase, not Composition — so scrolling
only re-triggers the cheap drawing phase instead of re-running the whole
`LandingScreen`/`SlideCarousel` composable tree on every frame. (This was a real
`FrequentlyChangingValue` lint finding caught while building this feature — see the
change log entry — not a hypothetical.)

## 6. How the Composition lifecycle relates to the Android Activity lifecycle

This is the actual question you asked, and it only makes sense with sections 3–5 in
place. The short version: **a Composition has no lifecycle of its own independent of
some Android host (an Activity or Fragment) — it's driven by that host's window being
created, attached, and destroyed**, but "driven by" isn't "identical to." Here's the
mapping, building directly on the Activity callback table from
[[03-2026-09-08-android-lifecycle]]:

| Activity callback | What happens to the Composition |
|---|---|
| `onCreate()` calls `setContent { }` | The Composition object is created, but nothing is drawn yet |
| *(first measure/layout pass, right after `onCreate`)* | **Initial composition** happens — every composable runs for the first time, `remember{}` state initializes, `LaunchedEffect`/`DisposableEffect` blocks start |
| `onStart()` / `onResume()` | Window is visible; **recomposition** happens live as `State` values change (user interaction, new ViewModel emissions, animations) |
| `onPause()` | Still recomposing normally — a `onPause()` (e.g. a dialog appearing) doesn't by itself stop anything, same as the pre-Compose system |
| `onStop()` | The window is detached from the `WindowManager` — Compose stops scheduling new frames (nothing is on screen to draw), but **the Composition and its `remember`ed state are *not* disposed** as long as the Activity instance is still alive (e.g. just covered by another app) |
| `onDestroy()` (finishing for good, or a config-change recreation without retained state) | The Composition is **disposed** — this is "leave the Composition" from section 5: every `LaunchedEffect` coroutine is cancelled, every `DisposableEffect.onDispose { }` fires |

Two bridging APIs make this connection explicit in code rather than implicit magic:

**`LocalLifecycleOwner.current`** (`LandingScreen.kt:230`) hands a composable the
hosting Activity's actual `Lifecycle` object — the same `Lifecycle` type from
[[03-2026-09-08-android-lifecycle]], just reached via a composable instead of `this` on
an Activity. That's what lets `SlideCarousel` write:

```kotlin
val lifecycleOwner = LocalLifecycleOwner.current
LaunchedEffect(slides.size, lifecycleOwner) {
    lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { ... }
}
```

— which is **exactly** the old `lifecycleScope.launch { repeatOnLifecycle(STARTED) { } }`
pattern from [[03-2026-09-08-android-lifecycle]]'s `LandingActivity`, just invoked from
inside a composable via `LocalLifecycleOwner` instead of from the Activity class body
directly. The effect: the auto-scroll loop restarts every time the Activity reaches
`STARTED` and cancels every time it drops below `STARTED` — so it correctly pauses while
the app is backgrounded, regardless of whether the Composition itself was ever disposed.

**`collectAsStateWithLifecycle()`** (`LandingActivity.kt:73`) does the equivalent thing
for turning a `StateFlow` into Compose `State`: it's sugar for "collect this flow, but
only while the lifecycle is at least `STARTED`, pausing collection when backgrounded" —
the same `repeatOnLifecycle(STARTED) { flow.collect { } }` idea, built in, so
`LandingActivity` doesn't have to hand-write it the way the pre-Compose version did.

### What Compose changes about rotation

Rotation still works exactly as [[03-2026-09-08-android-lifecycle]] describes: unless
opted out, the OS still destroys and recreates the Activity instance on a config change,
and `LandingViewModel` still survives that (via `by viewModels { }`'s retained store) for
the same reasons. **Compose doesn't change any of that.** What it *does* change is what
happens next: because `LandingScreen` is a pure function of `LandingUiState`, "redraw
correctly after rotation" needs zero extra code — the brand-new Activity instance runs
`setContent { }` again, `collectAsStateWithLifecycle()` immediately reads whatever
`LandingViewModel`'s `StateFlow` currently holds (already-loaded slides, no reload), and
the resulting fresh Composition renders that state on its very first composition pass.
Compare this to the old system, where `observeUiState()` had to manually re-push every
field into every already-inflated view by hand.

## 7. Putting it all together: this project's landing screen, end to end

```
OS creates LandingActivity → onCreate()
        │
        ├─ by viewModels { ... }  → LandingViewModel created (or reused after rotation)
        │
        ├─ enableEdgeToEdge()
        │
        └─ setContent { }
                │
                ├─ collectAsStateWithLifecycle()   ← StateFlow → Compose State, gated STARTED
                │
                └─ ClonedWinkTheme { LandingScreen(uiState, onGetStartedClick, onErrorShown) }
                        │
                        │  [Initial composition: every composable below runs once]
                        │
                        ├─ LaunchedEffect(uiState.hasError) { if (hasError) onErrorShown() }
                        │
                        ├─ if (isLoading && slides.isEmpty()) CircularProgressIndicator()
                        │  else if (slides.isNotEmpty()) SlideCarousel(slides)
                        │       │
                        │       ├─ LocalLifecycleOwner.current
                        │       ├─ LaunchedEffect(slides.size, lifecycleOwner) {
                        │       │      repeatOnLifecycle(STARTED) { auto-scroll loop }
                        │       │  }
                        │       └─ HorizontalPager { page -> GlassSlideCard(..., 
                        │              graphicsLayer { /* drawing-phase read of scroll offset */ })
                        │          }
                        │
                        └─ GetStartedButton(onClick = onGetStartedClick)

[LandingViewModel emits a new LandingUiState] → State updates → recomposition of
        exactly the composables that read the changed part of uiState → screen updates,
        with zero manual view mutation anywhere.
```

Cross-references: [[03-2026-09-08-android-lifecycle]] for the Activity/ViewModel
lifecycle vocabulary this lesson builds on, [[04-2026-09-08-drawing-the-ui]] for the
View-system approach Compose replaced here, [[05-2026-09-08-mvvm-architecture]] for why
`LandingScreen` is stateless and never imports `data/` directly, and
[[06-2026-09-08-carousel-slider-walkthrough]] for the carousel's pre-Compose
implementation history.

## Cheat sheet: old system ↔ Compose ↔ where it lives in this project

| Concept | Old View system | Compose | This project |
|---|---|---|---|
| Describe a screen | XML layout file | `@Composable` function | `LandingScreen.kt` |
| Put it on screen | `setContentView(R.layout...)` | `setContent { }` | `LandingActivity.kt:66` |
| Look up a widget | `findViewById(R.id...)` | *(no lookup — it's a function parameter)* | n/a |
| React to new data | Manually mutate each view | Recomposition | Automatic, driven by `State` reads |
| Run code once per screen visit | `onCreate()` | Initial composition + `LaunchedEffect` | `LandingScreen.kt:110` |
| Pause background work while backgrounded | `repeatOnLifecycle(STARTED)` in `lifecycleScope` | `repeatOnLifecycle(STARTED)` in a `LaunchedEffect`, via `LocalLifecycleOwner` | `LandingScreen.kt:237-246` |
| Collect a ViewModel's `StateFlow`, lifecycle-aware | `repeatOnLifecycle(STARTED) { flow.collect { } }` | `collectAsStateWithLifecycle()` | `LandingActivity.kt:73` |
| Cleanup when a screen/element goes away | `onDestroy()` / `onCleared()` | Leaving the Composition (`DisposableEffect.onDispose`, `LaunchedEffect` cancellation) | n/a in this file yet |
| Survive rotation | `ViewModel` (unchanged by Compose) | `ViewModel` (unchanged by Compose) | `by viewModels { }`, `LandingActivity.kt:41-45` |
