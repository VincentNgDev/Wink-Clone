# Lesson: How MVVM was implemented for the landing screen

MVVM = **Model — View — ViewModel**. The rule this whole project follows (see
`.claude/rules/folder-structure.md`): data flows one way,
**View → ViewModel → Model**, and state flows back **Model → ViewModel → View**. The
View never talks to the Model directly.

## Update (2026-09-13): Compose View + Hilt-wired ViewModel

This lesson was first written when the View layer was `LandingActivity` + XML +
`CarouselAdapter` (RecyclerView/ViewPager2) and the ViewModel was hand-constructed via a
`viewModelFactory { }` block. Both of those are gone now:

- The **View** is `ui/landing/LandingScreen.kt` — a `@Composable` function, not an
  Activity — since [[08-2026-09-09-jetpack-compose-and-lifecycle]]'s rebuild.
- The **ViewModel** is *still* `LandingViewModel`, same responsibilities, but it's now
  constructed by **Hilt** (`@HiltViewModel` + `@Inject constructor`) instead of a
  hand-written factory — see [[12-2026-09-12-hilt-dependency-injection]].
- The **wiring between them** — who actually calls `LandingScreen(uiState = ..., ...)`
  and who actually asks for a `LandingViewModel` — used to be `LandingActivity.onCreate()`.
  Since [[13-2026-09-13-navigation-compose-vs-multi-activity]]'s migration, it's a
  `composable(WinkDestination.Landing.route) { }` block inside
  `ui/navigation/WinkNavHost.kt`.

The rest of this lesson is rewritten below to describe that current wiring. The three
*layers themselves* — the actual MVVM split — haven't changed conceptually at all; only
*which framework glues them together* has, twice now (View system → Compose, then
manual factory → Hilt). That stability is worth noticing: MVVM's View/ViewModel/Model
boundary didn't have to change either time, because the ViewModel never depended on
*how* the View rendered or *how* it was constructed in the first place.

## The three layers, with this feature's actual files

```
View                        ViewModel                   Model
────                        ─────────                   ─────
ui/landing/                 viewmodel/landing/           data/model/
  LandingScreen.kt             LandingViewModel.kt         CarouselSlide.kt
                                LandingUiState.kt         data/repository/
ui/navigation/                                              CarouselRepository.kt (interface)
  WinkNavHost.kt (wiring)                                   DefaultCarouselRepository.kt (impl)
                                                           di/
                                                             RepositoryModule.kt (Hilt bindings)
```

### Model — `data/model/` + `data/repository/`

- **`CarouselSlide`** (`data/model/CarouselSlide.kt`) — a plain, immutable `data class`
  describing one slide (`id`, `title`, `subtitle`, `imageContentDescription`, and either
  `imageRes` or `imageUrl`). No Android UI code, no logic — just data.
- **`CarouselRepository`** (`data/repository/CarouselRepository.kt`) — an *interface*:
  `suspend fun getSlides(): List<CarouselSlide>`. This is the seam between "how slides
  are fetched" and "how they're used" — the ViewModel only knows about this interface.
- **`DefaultCarouselRepository`** (`data/repository/DefaultCarouselRepository.kt`) — the
  *current* implementation: returns 3 hardcoded slides built from `strings.xml` +
  drawable resources. A future networked implementation (e.g. Retrofit-backed) could
  implement the same interface and be swapped in by changing one line in
  `di/RepositoryModule.kt` — nothing in the ViewModel or View would change.

### ViewModel — `viewmodel/landing/`

- **`LandingUiState`** (`viewmodel/landing/LandingUiState.kt`) — a single immutable
  `data class` snapshot of everything the screen needs to render:
  `isLoading`, `slides`, `hasError`. Instead of three separate mutable fields the View
  could read at inconsistent times, there's always exactly one current state object.
- **`LandingViewModel`** (`viewmodel/landing/LandingViewModel.kt`) — holds a
  `MutableStateFlow<LandingUiState>` privately (`_uiState`) and exposes a read-only
  `StateFlow<LandingUiState>` (`uiState`) publicly. On `init`, it calls `loadSlides()`,
  which calls `carouselRepository.getSlides()` inside a `viewModelScope.launch`, and
  updates `_uiState` with the result (or `hasError = true` on failure).
- Notice: **no `android.view.*`, no `Context`, no `Activity` reference anywhere in this
  package.** That's a hard MVVM rule here (`.claude/rules/folder-structure.md`) — a
  ViewModel that imported `android.widget.TextView` would be impossible to unit-test on
  the JVM and would risk leaking a destroyed Activity (see
  [[03-2026-09-08-android-lifecycle]]).

### View — `ui/landing/LandingScreen.kt`

- **`LandingScreen`** (`ui/landing/LandingScreen.kt`) — a stateless `@Composable`
  function: it takes a `LandingUiState` and a couple of event lambdas
  (`onGetStartedClick`, `onErrorShown`) as parameters and renders whatever the state
  says (the gradient carousel, loading spinner, or CTA button) — no `LandingViewModel`
  reference inside the file at all. It forwards the one real user action (tapping "Get
  started") back out through `onGetStartedClick`, per the rule: **user input goes to the
  ViewModel/caller; the View never decides business logic itself.**
- `LandingScreen` never imports anything from `data/` — it only ever receives a plain
  `LandingUiState` snapshot, same rule as before, just expressed as function parameters
  instead of an Activity reading a class field.

### The wiring — `ui/navigation/WinkNavHost.kt`

Neither the ViewModel nor the View knows about the other directly. Something has to sit
between them: ask Hilt for a `LandingViewModel`, collect its `uiState`, and call
`LandingScreen(...)` with the result. That's `WinkNavHost`'s Landing `composable { }`
block:

```kotlin
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
        onErrorShown = { Toast.makeText(context, R.string.error_loading_slides, Toast.LENGTH_SHORT).show() },
    )
}
```

- `hiltViewModel()` is where Hilt's dependency graph and Compose meet — see the next
  section for what it does under the hood.
- `collectAsStateWithLifecycle()` turns the ViewModel's `StateFlow` into Compose
  `State`, so `LandingScreen` recomposes whenever `uiState` changes — this line is the
  Compose replacement for the old `repeatOnLifecycle(STARTED) { viewModel.uiState.collect { ... } }`
  block ([[03-2026-09-08-android-lifecycle]]).
- The two event lambdas are where *this* wiring layer, not `LandingScreen` itself,
  decides what "Get started" and "an error happened" actually mean for the app as a
  whole (navigate; show a Toast) — keeping that decision out of the stateless composable.

See [[13-2026-09-13-navigation-compose-vs-multi-activity]] for the full navigation-model
explanation this file is part of.

## Why the interface (`CarouselRepository`) matters, and who wires it up now

This is the classic **dependency inversion** piece of MVVM: `LandingViewModel`'s
constructor takes a `CarouselRepository` (the interface), not a
`DefaultCarouselRepository` (the concrete class):

```kotlin
@HiltViewModel
class LandingViewModel @Inject constructor(
    private val carouselRepository: CarouselRepository,
) : ViewModel()
```

Two consumers construct it differently, and neither needs the ViewModel to change:

1. **Real app** (`WinkNavHost`'s `hiltViewModel()` call, above): Hilt is what actually
   decides "give me a `DefaultCarouselRepository` for that `CarouselRepository`
   parameter." It reads that mapping from `di/RepositoryModule.kt`:

   ```kotlin
   @Module
   @InstallIn(SingletonComponent::class)
   abstract class RepositoryModule {
       @Binds
       abstract fun bindCarouselRepository(impl: DefaultCarouselRepository): CarouselRepository
   }
   ```

   This is the direct replacement for the hand-written
   `viewModelFactory { initializer { LandingViewModel(DefaultCarouselRepository(applicationContext)) } }`
   block that used to live in `LandingActivity` — see
   [[12-2026-09-12-hilt-dependency-injection]] for the full mechanics of `@Module`,
   `@InstallIn`, and `@Binds`. Either way, the point stands: *something outside the
   ViewModel* decides which concrete `CarouselRepository` to construct and hand in —
   first that was hand-written code in an Activity, now it's Hilt reading annotations.
2. **Unit test** (`LandingViewModelTest`, `app/src/test/...`):
   ```kotlin
   val viewModel = LandingViewModel(FakeCarouselRepository(slides))
   ```
   `FakeCarouselRepository` is a tiny hand-written stand-in that implements the same
   interface without touching real `Context`/resources/network — which is exactly why
   this ViewModel can be tested as a fast JVM unit test (`app/src/test/`) instead of a
   slow on-device instrumented test (`app/src/androidTest/`). Hilt is never involved in
   this path at all — plain constructor-calling still works, since `@Inject constructor`
   is still just a regular Kotlin constructor underneath the annotation.

## The one-way data flow in this feature, end to end

```
User taps "Get started"
        │
        ▼
onGetStartedClick (WinkNavHost) → navController.navigate(...)
   (a navigation decision, not a ViewModel concern — nothing to compute here)

App launch → Landing route first entered
        ▼
hiltViewModel() constructs LandingViewModel (Model dependency resolved by Hilt)
        ▼
LandingViewModel.init → loadSlides()
        │
        ▼
carouselRepository.getSlides()  (Model)
        │
        ▼
_uiState.update { ... }   (ViewModel state changes)
        │
        ▼
WinkNavHost collects viewModel.uiState via collectAsStateWithLifecycle()
        │
        ▼
LandingScreen(uiState = ...) recomposes (View renders)
```

State always flows Model → ViewModel → View. `LandingScreen` never imports anything
from `data/` directly — it only ever receives a `LandingUiState` snapshot as a
parameter, and `LandingViewModel` never imports anything from `ui/` — the two meet only
inside `WinkNavHost`.

See [[06-2026-09-08-carousel-slider-walkthrough]] for the carousel-specific UI mechanics
(historical — pre-Compose) and [[08-2026-09-09-jetpack-compose-and-lifecycle]] for how
the View layer actually puts pixels on screen today.
