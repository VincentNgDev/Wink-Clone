# Lesson: `navigation-compose` vs. multiple manifest Activities — is it actually better?

[[07-2026-09-08-navigation]] already tracked *that* this project migrated from two
Activities (`LandingActivity`/`HomeActivity`, each declared in `AndroidManifest.xml`) to
one Activity hosting `androidx.navigation:navigation-compose`. This lesson answers the
question directly — **is that the better approach, and why** — then walks installation,
implementation, the rest of the library's toolbox (nested graphs, passing results back,
bottom-nav-style tabs, deep links, dialogs, animations, testing), and the caveats/limits
worth knowing before reaching for this pattern on the next screen.

**Note on scope**: `WinkNavHost.kt` today only has two argument-free destinations
(Landing, Home), so most of the toolbox below isn't exercised by real code yet. Every
example is written against real, already-no-op hooks in this app — `HomeScreen`'s
`onScanQrClick`, `onRewardsClick`, `onQuickLinkClick`, and `onStationInfoClick`
(`WinkNavHost.kt`) — so they read as "here's what wiring this up for real would look
like," not made-up scenarios. Treat them as reference material for *when* a feature needs
one of these, not as a to-do list.

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

## The rest of the navigation-compose toolbox

`NavHost` + `composable` + `navController.navigate(...)` is the 20% of the API that covers
80% of screens (exactly what Landing → Home needed). The library has quite a bit more,
each piece solving one specific problem — here's what each does, with an example grounded
in a real hook this app already has.

### 1. Type-safe routes (`@Serializable`) instead of raw strings

**Problem it solves**: `WinkDestination.route` being a plain `String` means a typo in a
route pattern (`"stattion/{id}"`) or a mismatched argument name compiles fine and only
fails at runtime. Since Navigation Compose 2.8 (this project is on 2.9.7), a destination
can be a `@Serializable` class/object instead, and the compiler checks the route and its
arguments like any other function call.

**Setup**: add the Kotlin serialization plugin to the version catalog and apply it in
`app/build.gradle.kts` (`alias(libs.plugins.kotlin.serialization)`); no separate
`kotlinx-serialization-json` dependency is needed just for this — `navigation-compose`
already depends on `kotlinx-serialization-core` for it.

**Example** — wiring up `HomeScreen`'s `onStationInfoClick` (currently a no-op in
`WinkNavHost.kt`) to a real station detail screen, keyed by station ID with an optional
highlighted platform:

```kotlin
// WinkDestination.kt, rewritten as type-safe routes
@Serializable
object Landing

@Serializable
object Home

@Serializable
data class StationDetail(val stationId: String, val highlightPlatform: String? = null)
```

```kotlin
// WinkNavHost.kt
NavHost(navController = navController, startDestination = Landing) {
    composable<Landing> { /* ... */ }
    composable<Home> { entry ->
        val viewModel: HomeViewModel = hiltViewModel()
        HomeScreen(
            /* ... */
            onStationInfoClick = { stationId ->
                navController.navigate(StationDetail(stationId = stationId))
            },
        )
    }
    composable<StationDetail> { entry ->
        val args = entry.toRoute<StationDetail>()
        val viewModel: StationDetailViewModel = hiltViewModel()
        StationDetailScreen(stationId = args.stationId, highlightPlatform = args.highlightPlatform)
    }
}
```

`entry.toRoute<StationDetail>()` deserializes the back stack entry straight back into the
typed `StationDetail` instance — no manual `entry.arguments?.getString("stationId")`
bundle lookup, and `highlightPlatform` being nullable with a default is expressed the
normal Kotlin way instead of a separate `nullable = true` argument declaration.

**Still worth knowing**: this only makes the *route* type-safe. Anything encoded into it
is still serialized to a string under the hood, so it's still the wrong tool for passing a
large object (a full `Station` with a list of nested arrival times) — pass the ID, as
above, and let `StationDetailViewModel` fetch the rest. See the Caveats section for why
that also matters for MVVM layering.

### 2. Nested graphs — grouping a multi-step flow and sharing one ViewModel across it

**Problem it solves**: a flow with several steps that all need the *same* in-progress
state (not just one leaf screen's own state). `navigation(startDestination, route) { }`
groups a set of destinations under one parent route, and `hiltViewModel()` can be scoped
to that parent instead of to a single leaf destination.

**Example** — `HomeScreen`'s `onQuickLinkClick` currently no-ops for the Bus/Train/MRT Map
shortcuts. A real map flow might be "pick a line, then pick a station on that line,
then view arrivals" — three screens that all need to remember which line was picked in
step 1:

```kotlin
NavHost(navController = navController, startDestination = WinkDestination.Home.route) {
    composable(WinkDestination.Home.route) { /* ... */ }

    navigation(startDestination = "transit/lines", route = "transit_map") {
        composable("transit/lines") { entry ->
            val parentEntry = remember(entry) { navController.getBackStackEntry("transit_map") }
            val viewModel: TransitMapViewModel = hiltViewModel(parentEntry)
            LineListScreen(onLineChosen = { line ->
                viewModel.selectLine(line)
                navController.navigate("transit/stations")
            })
        }
        composable("transit/stations") { entry ->
            val parentEntry = remember(entry) { navController.getBackStackEntry("transit_map") }
            val viewModel: TransitMapViewModel = hiltViewModel(parentEntry) // same instance as above
            StationListScreen(line = viewModel.uiState.value.selectedLine, /* ... */)
        }
    }
}
```

`navController.getBackStackEntry("transit_map")` looks up the *parent graph's* back stack
entry (created once, when any destination inside `transit_map` is first entered), and
passing that entry into `hiltViewModel(...)` — instead of letting it default to the
current destination's own entry — makes both `"transit/lines"` and `"transit/stations"`
resolve to the **exact same** `TransitMapViewModel` instance. The line picked in step 1
is just a property on that shared ViewModel, no navigation argument needed to carry it to
step 2 — and the instance is cleared automatically once the whole `transit_map` graph is
popped off the back stack (leaving the flow entirely clears its state, correctly).

### 3. Passing a result back up the stack — `SavedStateHandle`

**Problem it solves**: the reverse direction from a normal argument — a *later* screen
needs to hand something back to the screen that pushed it (e.g. "here's the station the
user picked"), without the two screens sharing a whole ViewModel via a nested graph.

**Example** — imagine a future `Search` screen whose "choose a station" affordance opens
a full `SelectStation` screen, then needs the choice back:

```kotlin
// Search destination — reads a result that may arrive later
composable(WinkDestination.Search.route) { entry ->
    val selectedStationId by entry.savedStateHandle
        .getStateFlow<String?>("selected_station_id", null)
        .collectAsStateWithLifecycle()
    SearchScreen(selectedStationId = selectedStationId, /* ... */)
}

// SelectStation destination — writes the result, then pops back to Search
composable(WinkDestination.SelectStation.route) {
    SelectStationScreen(
        onStationChosen = { stationId ->
            navController.previousBackStackEntry
                ?.savedStateHandle
                ?.set("selected_station_id", stationId)
            navController.popBackStack()
        },
    )
}
```

This is the officially recommended pattern for one-off "pick something and return it"
flows — no shared ViewModel, no event bus. `savedStateHandle` survives process death the
same way a ViewModel's `SavedStateHandle` does, so the result isn't lost if the OS kills
the app while `SelectStation` is on top.

### 4. Bottom-navigation-style tabs — `currentBackStackEntryAsState()`, `launchSingleTop`, `saveState`/`restoreState`

**Problem it solves**: a bottom nav bar (Home / Rewards / Profile, say — `HomeScreen`'s
`onRewardsClick` is currently a no-op that would plausibly become one of these tabs) needs
three things a plain `navigate(route)` call doesn't give you for free: knowing which tab is
currently selected (to highlight it), not stacking duplicate copies of a tab if tapped
repeatedly, and preserving *each tab's own* scroll position/back stack when switching away
and back.

```kotlin
@Composable
fun WinkBottomNavBar(navController: NavHostController) {
    // Kotlin/Compose: `currentBackStackEntryAsState()` turns the current back stack entry
    // into observable Compose State — this recomposes the nav bar every time the
    // destination changes, which is how `currentRoute == item.route` below stays correct.
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    NavigationBar {
        bottomNavItems.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        // Android: rewinds to the graph's start destination first, saving each
                        // popped tab's own state (scroll position, its own back stack) instead
                        // of discarding it — this is what makes switching tabs feel instant.
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        // Android: avoids pushing a second copy of the same tab's destination
                        // onto the back stack if it's already on top.
                        launchSingleTop = true
                        // Android: the counterpart to `saveState` above — restores that saved
                        // state instead of recreating the destination from scratch.
                        restoreState = true
                    }
                },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
            )
        }
    }
}
```

Without `saveState`/`restoreState`/`launchSingleTop`, switching tabs back and forth would
either pile up an ever-growing back stack (Back button would have to be pressed once per
tab switch to actually leave the app) or reset each tab's scroll position/state every time
it's revisited — both wrong for a bottom nav bar specifically. This trio only matters for
this "swap between sibling top-level destinations" pattern; the Landing → Home hop in this
project doesn't need any of it since it's a one-way, forward-only move.

### 5. Deep links — `navDeepLink`

**Problem it solves**: letting something *outside* the current screen — a push
notification, a widget tap, an external `wink://...` URI — open the app directly onto a
specific destination, not just the start destination.

**Example** — a "your bus is arriving" push notification that should open straight to a
station's detail screen:

```kotlin
composable(
    route = "station/{stationId}",
    arguments = listOf(navArgument("stationId") { type = NavType.StringType }),
    deepLinks = listOf(navDeepLink { uriPattern = "wink://station/{stationId}" }),
) { entry ->
    val stationId = entry.arguments?.getString("stationId")
    /* ... */
}
```

Two halves have to agree for an *external* deep link (one another app or the OS can
trigger) to work: this `navDeepLink` on the destination, **and** an `<intent-filter>` with
a matching `<data android:scheme="wink" />` on `MainActivity` in `AndroidManifest.xml` —
without the manifest half, the OS has no way to know this app should even be considered
for that URI. An *internal* deep link built via `NavDeepLinkBuilder`/a notification's
`PendingIntent` targeting this same route doesn't need the manifest `<data>` entry, since
it's launching `MainActivity` directly rather than asking the OS to resolve a URI.

### 6. Dialog destinations — `dialog { }`

**Problem it solves**: a confirmation dialog that should behave like a real destination —
dismissible with the system Back button, poppable with `popBackStack()`, reachable from a
deep link — rather than a local `var showDialog by remember { mutableStateOf(false) }`
flag that a screen has to manage by hand.

**Example** — a logout confirmation:

```kotlin
NavHost(navController = navController, startDestination = WinkDestination.Home.route) {
    composable(WinkDestination.Home.route) { /* ... */ }

    dialog(WinkDestination.ConfirmLogout.route) {
        AlertDialog(
            onDismissRequest = { navController.popBackStack() },
            title = { Text(stringResource(R.string.confirm_logout_title)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.logout()
                    navController.popBackStack()
                }) { Text(stringResource(R.string.confirm_logout_action)) }
            },
            dismissButton = {
                TextButton(onClick = { navController.popBackStack() }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}
```

`dialog { }` comes from the same `navigation-compose` artifact already in this project —
no extra dependency needed. Under the hood, `NavHost` renders it in an actual
`androidx.compose.ui.window.Dialog`, layered on top of whatever destination is beneath it
on the back stack, rather than replacing it the way `composable { }` does.

### 7. Per-destination animations — `enterTransition`/`exitTransition`/`popEnterTransition`/`popExitTransition`

**Problem it solves**: giving a specific destination its own transition instead of the
graph-wide default, using Compose's own animation APIs rather than legacy Activity window
transitions.

```kotlin
NavHost(
    navController = navController,
    startDestination = WinkDestination.Landing.route,
    // Android/Kotlin: applies to every destination that doesn't override it below — a
    // simple crossfade as the graph-wide default.
    enterTransition = { fadeIn(animationSpec = tween(300)) },
    exitTransition = { fadeOut(animationSpec = tween(300)) },
) {
    composable(WinkDestination.Landing.route) { /* ... */ }

    composable(
        route = WinkDestination.Home.route,
        // Android: overrides the graph default for just this destination — slides Home in
        // from the right when navigated to forward...
        enterTransition = { slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }) },
        // Android: ...and slides it back out to the right when popped (Back button), instead
        // of using the same fade as everything else.
        popExitTransition = { slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth }) },
    ) { /* ... */ }
}
```

Each lambda's receiver is `AnimatedContentTransitionScope<NavBackStackEntry>`, the same
type Compose's `AnimatedContent` uses elsewhere — so anything already known about
`slideInHorizontally`/`fadeIn`/etc. from general Compose animation work applies directly
here, no navigation-specific animation API to learn separately. This project currently
takes the library's own default transition (an instant switch, no animation specified
anywhere in `WinkNavHost.kt`) — these parameters are opt-in.

### 8. Testing navigation — `TestNavHostController`

**Problem it solves**: verifying a click actually navigates to the right destination,
without needing a real device/emulator's Activity lifecycle. This is exactly why
`WinkNavHost`'s `navController` parameter has a default (`rememberNavController()`) rather
than being hardcoded — a test can pass in its own controller instead.

```kotlin
@Test
fun getStartedClick_navigatesToHome() {
    lateinit var navController: TestNavHostController
    composeTestRule.setContent {
        navController = TestNavHostController(LocalContext.current).apply {
            navigatorProvider.addNavigator(ComposeNavigator())
        }
        WinkNavHost(navController = navController)
    }

    composeTestRule.onNodeWithText("Get started").performClick()

    assertEquals(WinkDestination.Home.route, navController.currentBackStackEntry?.destination?.route)
}
```

This belongs in `app/src/androidTest` (it needs real Compose UI to click through — see
[[10-2026-09-10-project-and-folder-structure]] for that test layout), not
`app/src/test`, since `composeTestRule` requires an actual Compose UI test environment.
Not written yet for this project, but this is the shape it would take.

## Caveats and limitations (read before adding the next screen)

- **String routes aren't type-safe by default.** `WinkDestination.route` is a plain
  `String` today; a typo in a route string (`"stattion/{id}"`) or a mismatched argument
  name compiles fine and fails at runtime. See toolbox item 1 above for the
  `@Serializable`-route alternative this project would migrate to once a destination needs
  arguments — not adopted yet because both current destinations (Landing, Home) are
  argument-free.
- **Even with type-safe routes, don't pass whole objects through navigation.** Anything
  encoded into the route is still serialized to a string under the hood; large or complex
  objects shouldn't travel as navigation arguments at all — pass an ID (as in the
  `StationDetail` example above) and re-fetch/look up the full object in the destination's
  own ViewModel instead. This is also just good MVVM: a ViewModel shouldn't receive
  Parcelable/Serializable UI objects across a navigation boundary.
- **Nested-graph ViewModel sharing (toolbox item 2) is easy to reach for when it isn't
  needed.** If two destinations don't actually need to share mutable in-progress state,
  giving them separate `hiltViewModel()` calls (the default, entry-scoped behavior) is
  simpler and keeps each screen's state independent — reach for
  `hiltViewModel(navController.getBackStackEntry(graphRoute))` only when there's a real
  multi-step flow with shared state to justify it.
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

Beyond the basic `NavHost`/`composable`/`navigate` trio this project uses today, the
library has a purpose-built tool for each of the harder navigation problems a real app
eventually hits: type-safe `@Serializable` routes for compile-checked arguments, nested
graphs for a multi-step flow with shared state, `SavedStateHandle` for handing a result
back up the stack, `saveState`/`restoreState`/`launchSingleTop` for bottom-nav-style tabs,
`navDeepLink` for external entry points, `dialog { }` for back-stack-aware confirmation
dialogs, per-destination `enterTransition`/`exitTransition` for custom animations, and
`TestNavHostController` for testing navigation without a device. None of these are needed
until the corresponding problem actually shows up — reach for each one only when its
specific "problem it solves" (see each toolbox item above) matches something this app
actually needs to do.
