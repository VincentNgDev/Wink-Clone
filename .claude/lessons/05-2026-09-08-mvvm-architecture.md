# Lesson: How MVVM was implemented for the landing screen

MVVM = **Model — View — ViewModel**. The rule this whole project follows (see
`.claude/rules/folder-structure.md`): data flows one way,
**View → ViewModel → Model**, and state flows back **Model → ViewModel → View**. The
View never talks to the Model directly.

## The three layers, with this feature's actual files

```
View                    ViewModel                  Model
────                    ─────────                  ─────
ui/landing/              viewmodel/landing/          data/model/
  LandingActivity.kt       LandingViewModel.kt          CarouselSlide.kt
  CarouselAdapter.kt       LandingUiState.kt          data/repository/
                                                          CarouselRepository.kt (interface)
                                                          DefaultCarouselRepository.kt (impl)
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
  `LandingActivity`'s ViewModel factory — nothing in the ViewModel or View would change.

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

### View — `ui/landing/`

- **`LandingActivity`** (`ui/landing/LandingActivity.kt`) — inflates the layout,
  finds views, and **observes** `viewModel.uiState`, rendering whatever it receives
  (`carouselAdapter.submitList(state.slides)`, toggling the dot indicator's visibility,
  showing an error Toast). It also forwards nothing back yet (no user input beyond
  swiping, which ViewPager2 handles itself) — but the general rule is: **user input goes
  to the ViewModel; the View never decides business logic itself.**
- **`CarouselAdapter`** (`ui/landing/CarouselAdapter.kt`) — purely a rendering helper
  for RecyclerView/ViewPager2; it turns a `CarouselSlide` into bound views
  (`ImageView`/`TextView` text and image). It holds no business logic either.

## Why the interface (`CarouselRepository`) matters

This is the classic **dependency inversion** piece of MVVM: `LandingViewModel`'s
constructor takes a `CarouselRepository` (the interface), not a
`DefaultCarouselRepository` (the concrete class):

```kotlin
class LandingViewModel(private val carouselRepository: CarouselRepository) : ViewModel()
```

Two consumers construct it differently, and neither needs the ViewModel to change:

1. **Real app** (`LandingActivity`):
   ```kotlin
   private val viewModel: LandingViewModel by viewModels {
       viewModelFactory {
           initializer { LandingViewModel(DefaultCarouselRepository(applicationContext)) }
       }
   }
   ```
2. **Unit test** (`LandingViewModelTest`, `app/src/test/...`):
   ```kotlin
   val viewModel = LandingViewModel(FakeCarouselRepository(slides))
   ```
   `FakeCarouselRepository` is a tiny hand-written stand-in that implements the same
   interface without touching real `Context`/resources/network — which is exactly why
   this ViewModel can be tested as a fast JVM unit test (`app/src/test/`) instead of a
   slow on-device instrumented test (`app/src/androidTest/`).

## The one-way data flow in this feature, end to end

```
User swipes ViewPager2
        │  (ViewPager2 handles this itself — no ViewModel involved,
        │   there's no "business decision" being made by swiping)
        ▼
   [View renders whatever the ViewModel currently has]

App launch
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
LandingActivity collects viewModel.uiState (View observes)
        │
        ▼
carouselAdapter.submitList(...) / toggle indicator / show Toast
```

State always flows Model → ViewModel → View. The View (`LandingActivity`,
`CarouselAdapter`) never imports anything from `data/` directly — it only ever talks to
`LandingViewModel`.

See [[06-2026-09-08-carousel-slider-walkthrough]] for the carousel-specific UI mechanics
built on top of this, and [[04-2026-09-08-drawing-the-ui]] for how the View layer actually
puts pixels on screen.
