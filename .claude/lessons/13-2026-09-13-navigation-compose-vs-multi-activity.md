# Lesson: `navigation-compose` vs. multiple manifest Activities — is it actually better?

[[07-2026-09-08-navigation]] already tracked *that* this project migrated from two
Activities (`LandingActivity`/`HomeActivity`, each declared in `AndroidManifest.xml`) to
one Activity hosting `androidx.navigation:navigation-compose`. This lesson answers the
question directly — **is that the better approach, and why** — then walks installation,
implementation, and the caveats/limits worth knowing before reaching for this pattern on
the next screen.

## Short answer

**Yes, for this project's shape (a single, in-app, forward-flowing set of screens), and
yes generally for most apps today** — Google's own Android guidance has treated
single-Activity + a navigation graph as the default recommended architecture since 2018,
specifically *because* most "screens" in an app are really just different UI states of one
continuous experience, not independent entry points. But "better" isn't "strictly better
in every case" — see the **When a separate Activity is still the right call** section
below for the cases where the old manifest-multi-Activity model isn't actually obsolete.

## Why it's better here: what each model actually costs

| | Multiple Activities (`Intent` + manifest) | `navigation-compose` (this project, now) |
|---|---|---|
| Declaring a new screen | New `<activity>` entry in `AndroidManifest.xml` + new Activity class | New `object` on `WinkDestination` + one `composable(route) { }` block |
| Going to it | `startActivity(Intent(this, XActivity::class.java))` | `navController.navigate(WinkDestination.X.route)` |
| Back stack | Managed by the OS's Activity back stack; each hop re-runs the destination Activity's full lifecycle (`onCreate` → ... → `onResume`) | Managed by `NavHostController` *inside* one Activity; the Activity itself never restarts, only the composable content swaps |
| Passing data forward | `Intent.putExtra(...)` / `getStringExtra(...)`, stringly-typed bundle keys | Route arguments (a route pattern like `"station/{id}"`), or the newer type-safe `@Serializable` routes (see caveats) |
| ViewModel scope | One per Activity, via `by viewModels()` | One per `NavBackStackEntry` (or per nested graph), via `hiltViewModel()` — see [[12-2026-09-12-hilt-dependency-injection]] |
| Shared UI chrome (e.g. a bottom nav bar, an app-wide top bar) | Must be duplicated in every Activity's layout, or hand-rolled some other way | Lives once in `MainActivity`'s `setContent { }`, wrapping `WinkNavHost` — trivial to add without touching any screen |
| Transition animations | Default Activity transitions (or custom ones wired through `Intent`/theme overlays — clunkier) | Compose's own `enterTransition`/`exitTransition` per destination, or `AnimatedContent` |
| Startup cost per navigation | Real Activity creation is heavier — new `Context`, full lifecycle dispatch, window/surface churn | Just recomposition — no new `Context`/window, much cheaper |

The practical effect on this codebase: adding a third screen today means touching
`WinkDestination.kt` (one line) and `WinkNavHost.kt` (one `composable { }` block) — no
manifest edit, no new Activity class, no lifecycle boilerplate. That lower marginal cost
per screen is the concrete "better" here, not an abstract preference.

## Installation

Everything below is already wired into this repo (see
`.claude/changes/2026-09-13-migrate-to-navigation-compose.md`) — this section is the
"how would I add this to a project that doesn't have it yet" walkthrough, pointing at the
real files so it doubles as a reference.

**1. Version catalog** (`gradle/libs.versions.toml`) — declare the version and the library
coordinate, per this project's rule of never inlining dependency coordinates directly in
`build.gradle.kts`:

```toml
[versions]
navigationCompose = "2.9.7"

[libraries]
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
```

**2. Module dependency** (`app/build.gradle.kts`):

```kotlin
implementation(libs.androidx.navigation.compose)
```

That single artifact is enough for `NavHost`/`NavHostController`/`composable { }`. This
project needed one more, because it also uses Hilt-injected ViewModels per screen:

```toml
hiltLifecycleViewmodelCompose = "1.4.0"
androidx-hilt-lifecycle-viewmodel-compose = { group = "androidx.hilt", name = "hilt-lifecycle-viewmodel-compose", version.ref = "hiltLifecycleViewmodelCompose" }
```

```kotlin
implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
```

**Caveat worth flagging at install time**: most tutorials/Stack Overflow answers reach for
`androidx.hilt:hilt-navigation-compose` instead. That artifact defines the *same*
`hiltViewModel()` function name, but as of Hilt 1.4.0 it's deprecated in favor of
`hilt-lifecycle-viewmodel-compose` — the new artifact doesn't force a transitive dependency
on `androidx.navigation` just to obtain a Hilt ViewModel (useful if some screen wants
`hiltViewModel()` without ever pulling in navigation itself). Both compile; only one is
current. Double-check which one a guide is telling you to add.

**3. No new Gradle plugins, no manifest changes for the library itself** — unlike Hilt
(which needed the `ksp`/`hilt-android` plugins in `[plugins]`), `navigation-compose` is a
plain library dependency. The manifest *does* shrink, though: instead of one `<activity>`
per screen, you end up with exactly one (see `AndroidManifest.xml` in this repo — only
`.ui.MainActivity` is declared, holding the `MAIN`/`LAUNCHER` `<intent-filter>`).

## Implementation

Three pieces, all real files in this repo.

### 1. Routes — `WinkDestination.kt`

```kotlin
sealed class WinkDestination(val route: String) {
    object Landing : WinkDestination("landing")
    object Home : WinkDestination("home")
}
```

- Kotlin: `sealed class` means every subclass must live in this same file, so the compiler
  knows the *complete* set of possible destinations — a future `when (destination)` can be
  exhaustive with no `else` branch, and the compiler errors if a new destination is added
  without updating that `when`.
- Each destination is an `object` (singleton) because neither screen takes a runtime
  argument today. A screen that does (say, a station detail screen keyed by ID) would
  instead be a `data class WinkDestination.Station(val id: String) : WinkDestination("station/{id}")`
  — the `{id}` placeholder is a *path argument* NavHost parses out of the actual
  navigated route string (e.g. `"station/42"`).

### 2. The graph — `WinkNavHost.kt`

```kotlin
@Composable
fun WinkNavHost(
    navController: NavHostController = rememberNavController(),
) {
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = WinkDestination.Landing.route,
    ) {
        composable(WinkDestination.Landing.route) {
            val viewModel: LandingViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            LandingScreen(
                uiState = uiState,
                onGetStartedClick = {
                    navController.navigate(WinkDestination.Home.route) {
                        popUpTo(WinkDestination.Landing.route) { inclusive = true }
                    }
                },
                onErrorShown = { /* ... */ },
            )
        }

        composable(WinkDestination.Home.route) {
            val viewModel: HomeViewModel = hiltViewModel()
            /* ... */
        }
    }
}
```

- `NavHost(navController, startDestination) { ... }` is a `@Composable` container whose
  displayed content swaps to match whichever route is on top of `navController`'s back
  stack. `startDestination` is shown first — the direct equivalent of a
  `<intent-filter>`'s `MAIN`/`LAUNCHER` Activity, just expressed as a route string instead
  of a manifest entry.
- `composable(route) { ... }` registers one destination on the graph. The trailing lambda
  is only invoked (composed) while that route is on top of the back stack — this is why
  `hiltViewModel()` calls live *inside* each `composable { }` block, not at the top of
  `WinkNavHost`: each block gets its own `NavBackStackEntry`, and `hiltViewModel()` scopes
  the ViewModel to *that* entry (created the first time the route is navigated to, cleared
  when it's popped), not to the whole Activity.
- `navController.navigate(route) { popUpTo(otherRoute) { inclusive = true } }` is the
  in-process replacement for `startActivity(Intent(...)); finish()`. `popUpTo(...)`
  removes destinations from the back stack as part of the same navigation call —
  `inclusive = true` also removes the named route itself (Landing), not just everything
  above it, so pressing Back from Home exits the app instead of returning to onboarding.

### 3. The host — `MainActivity.kt`

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ClonedWinkTheme {
                WinkNavHost()
            }
        }
    }
}
```

The Activity itself now knows nothing about which screen is showing — it just hosts one
`WinkNavHost()` inside `setContent { }`. `@AndroidEntryPoint` is still required (Hilt needs
an injection site to reach from), but there's exactly one of these annotations in the whole
app now, versus one per Activity before.

## Caveats and limitations (read before adding the next screen)

- **String routes aren't type-safe.** `WinkDestination.route` is a plain `String`; a typo
  in a route string (`"stattion/{id}"`) or a mismatched argument name compiles fine and
  fails at runtime. Navigation Compose 2.8+ (this project is on 2.9.7, so it's available)
  supports **type-safe routes** via `@Serializable` classes/objects instead of raw strings
  — worth migrating `WinkDestination` to once a route needs multiple typed arguments,
  since the compiler then checks argument types and names for you. Not adopted yet here
  because both current destinations are argument-free.
- **Argument passing is still stringly-typed under the hood for path/query params.**
  Even with type-safe routes, anything encoded into the URL-like route path is
  fundamentally a string; large or complex objects shouldn't be passed as navigation
  arguments at all — pass an ID and re-fetch/look up the object in the destination's own
  ViewModel instead (this is also just good MVVM: a ViewModel shouldn't receive
  Parcelable UI objects across a navigation boundary).
- **One Activity means one `Context`/window/task for the whole app.** Anything that
  legitimately needs its own task, its own `launchMode`, its own process, or to be
  launchable independently by another app or the OS (see below) can't be modeled as a
  `composable` destination — it needs a real Activity.
- **Deep links need explicit wiring.** With multiple Activities, an `<intent-filter>` on
  each Activity is enough for the OS to route a deep link straight to it. With one
  Activity, every deep link lands on `MainActivity`, and `navigation-compose`'s own
  `navDeepLink { }` builder (attached per `composable { }`) has to dispatch it to the right
  destination internally — an extra layer this project hasn't needed yet since it has no
  deep links at all.
- **Testing shape changes.** Espresso/instrumented tests that used to launch
  `HomeActivity` directly via `ActivityScenario.launch(HomeActivity::class.java)` can't
  anymore — there's only `MainActivity` to launch, and the test then has to drive
  `NavHostController.navigate(...)` (or interact through the UI) to reach the destination
  under test. Not yet exercised in `app/src/androidTest` here (see
  [[10-2026-09-10-project-and-folder-structure]] for that test layout), but it's the shape
  to expect.
- **Process death / saved state is a NavHost concern now, not an Activity one.** Before,
  each Activity got its own `savedInstanceState: Bundle` from the OS. Now,
  `rememberNavController()`'s back stack survives configuration changes and (with
  `SavedStateHandle`, wired through automatically) process death — but it's a different
  mechanism than the old per-Activity `onSaveInstanceState`, worth knowing about before
  assuming old habits carry over verbatim.
- **Nothing here changed the MVVM boundary.** `.claude/rules/folder-structure.md`'s "View
  → ViewModel → Model" rule and the ban on a ViewModel referencing `Activity`/`Context`/
  `View` still apply exactly as before — `hiltViewModel()` just changed *how* a ViewModel
  instance is obtained and scoped, not what a ViewModel is allowed to know about.

## When a separate Activity is still the right call

`navigation-compose` replaces the *within-app, forward-and-back* navigation model. It does
**not** replace every reason a real Activity exists. Keep a dedicated Activity (declared in
the manifest, its own `<intent-filter>` if needed) for:

- **Genuinely independent entry points** — a home-screen widget's configuration Activity,
  a share-target Activity another app launches via `Intent`, anything meant to be startable
  *without* the rest of the app's UI/back stack in play.
- **A different `launchMode`/task affinity requirement** — e.g. something that must always
  open in its own task (`singleInstance`), which a `composable` destination inside one
  Activity's task can't express.
- **Process isolation** — an Activity declared with `android:process=":something"` runs in
  a separate process; a `composable` destination always shares `MainActivity`'s process.
- **Interop with libraries/SDKs that expect to launch their own Activity** — some
  third-party SDKs (payment flows, camera/scanning libraries, OAuth webviews) ship their
  own Activity and expect `startActivityForResult`/the Activity Result API, not a Compose
  destination.

None of these apply to this project today (no widgets, no external entry points, no such
SDKs yet) — which is exactly why collapsing to one Activity + `navigation-compose` was a
clear win here rather than a tradeoff.

## Key takeaway

For an app whose screens are all part of one continuous, in-app flow — this project's
Landing → Home hop, and any screen added the same way — `navigation-compose` is a
straightforward improvement over one-Activity-per-screen: less boilerplate per new screen,
cheaper transitions, and ViewModel scoping that matches the *screen*, not an Activity that
happens to wrap it. It's not a universal replacement for the Activity/manifest model,
though — genuinely independent entry points, special task/process requirements, and
Activity-expecting third-party SDKs still need a real `<activity>` declaration. The
practical rule: default to a `composable` destination inside the existing `WinkNavHost`;
reach for a new Activity only when one of the specific cases above actually applies.
