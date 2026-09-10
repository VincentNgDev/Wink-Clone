# Lesson: The Home screen — a second full feature, real navigation, and lists in Compose

This lesson covers what shipped in `.claude/changes/2026-09-09-add-home-screen.md`:
`HomeActivity`, the screen the user actually lands on after tapping "Get started" on
`LandingScreen`. Most of the *concepts* here were already introduced by earlier lessons —
this one is about seeing the same patterns applied a second time (which is how you'll
know you actually understood them) plus a handful of genuinely new pieces: real
Activity-to-Activity navigation, Compose's **lazy list** composables, and a
`ModalBottomSheet`.

## 1. Navigation is no longer hypothetical

[[07-2026-09-08-navigation]] described the `Intent` + `startActivity` model as a
*hypothetical* — at the time, `LandingActivity` was the only screen, and its
"Get started" button did nothing. That's no longer true. `LandingActivity.kt:80-89`:

```kotlin
onGetStartedClick = {
    startActivity(Intent(this, HomeActivity::class.java))
    finish()
}
```

- `Intent(this, HomeActivity::class.java)` — "start this specific Activity class." `this`
  (a `Context`, since `LandingActivity` *is* one) is required so the system knows which
  app/process this request belongs to.
- `startActivity(...)` hands that `Intent` to the OS, which creates a new `HomeActivity`
  instance and runs its lifecycle (`onCreate()` → `onStart()` → `onResume()`, same
  callbacks as [[03-2026-09-08-android-lifecycle]]) — pushing it onto the back stack on
  top of `LandingActivity`.
- `finish()` immediately after **removes `LandingActivity` from that back stack**. Without
  it, pressing system Back from `HomeActivity` would return to the onboarding screen;
  with it, `LandingActivity` is gone, so Back from `HomeActivity` exits the app instead —
  the correct behavior for a one-time onboarding screen you never want to revisit.
- `HomeActivity` is declared in `AndroidManifest.xml` with `android:exported="false"`
  (unlike `LandingActivity`'s `exported="true"`) — see [[02-2026-09-08-android-manifest]].
  It has no `<intent-filter>`, so it's only reachable by an explicit `Intent` naming its
  class from inside this app, never launched directly by the OS or another app.

Everything else [[07-2026-09-08-navigation]] said still holds: this is still the
*manual*, one-Activity-per-screen model, not the Jetpack Navigation Component. With only
two screens and a single, one-directional hop between them (no back-and-forth flow, no
shared back stack of many screens), there's still no real need for that heavier tool yet.

## 2. A second repository/ViewModel pair — same shape, proving the pattern generalizes

[[05-2026-09-08-mvvm-architecture]] introduced the Repository → ViewModel → View chain
through `CarouselRepository`/`LandingViewModel`/`LandingScreen`. The home screen repeats
that *exact* shape under a different feature name, which is the real test of whether a
pattern is actually a pattern:

| Landing (existing) | Home (new) |
|---|---|
| `data/repository/CarouselRepository.kt` (interface) | `data/repository/HomeRepository.kt` |
| `data/repository/DefaultCarouselRepository.kt` (hardcoded impl) | `data/repository/DefaultHomeRepository.kt` |
| `viewmodel/landing/LandingUiState.kt` + `LandingViewModel.kt` | `viewmodel/home/HomeUiState.kt` + `HomeViewModel.kt` |
| `ui/landing/LandingScreen.kt` | `ui/home/HomeScreen.kt` (+ `HomeSections.kt`, `HomeComponents.kt` — split into three files because this screen has far more sections) |

`HomeViewModel` is constructed exactly like `LandingViewModel` — a `StateFlow<HomeUiState>`,
an `init { loadHome() }` block, and a `runCatching { }` around the repository call so a
failure becomes `uiState.hasError = true` instead of crashing. If you can read
`HomeViewModel.kt` and `LandingViewModel.kt` side by side and predict what each line does
before checking, [[05-2026-09-08-mvvm-architecture]] has actually sunk in.

One real difference worth noting: `HomeActivity`'s `viewModelFactory` passes
`DefaultHomeRepository(applicationContext)` (`HomeActivity.kt:29-33`) — same
constructor-injection-via-factory pattern as `LandingActivity`, because `HomeViewModel`,
like `LandingViewModel`, needs a repository instance and has no no-arg constructor for
the default `by viewModels()` delegate to fall back on.

## 3. `LazyColumn` — Compose's version of a scrolling list

Every earlier Compose lesson ([[08-2026-09-09-jetpack-compose-and-lifecycle]]) used plain
`Column` — which lays out **all** its children immediately, whether they're on screen or
not. That's fine for a handful of fixed elements (a title, a carousel, a button), but the
home screen has ~10 stacked sections, several containing their own scrollable rows of
cards. `HomeScreen.kt:118-122`:

```kotlin
LazyColumn(
    modifier = Modifier.fillMaxSize().statusBarsPadding(),
    contentPadding = PaddingValues(bottom = scaffoldPadding.calculateBottomPadding() + 16.dp),
) {
    item { LoyaltyCard(...) }
    item { QuickLinksRow(...) }
    item { Column { SectionHeader(...); PartnerRow(...) } }
    // ...more item { } blocks for every remaining section
}
```

- **`Lazy`** means Compose only composes/measures/draws the children that are actually
  (or about to be) visible on screen, and discards/recreates them as you scroll — the
  direct Compose descendant of the old View system's `RecyclerView`
  ([[04-2026-09-08-drawing-the-ui]], step 10), minus the manual `Adapter`/`ViewHolder`
  boilerplate that pattern required.
- Its trailing lambda isn't normal composable code — it's a special **`LazyListScope`**
  builder. Inside it you don't call composables directly; you describe *what content
  goes where* using builder functions: `item { }` for one fixed piece of content (used
  here — every section of this screen is a single distinct item, not a repeated row), or
  `items(list) { element -> ... }` for one entry per element of a list (used inside
  `PartnerRow`, see below, for the actual repeating cards).
- `contentPadding` (vs. a `Modifier.padding`) adds space *inside* the scrollable area at
  the top/bottom, so the last item still gets breathing room above the bottom nav bar
  without that padding being scrolled away with the content.

## 4. `LazyRow` — the same idea, horizontal, for a repeating list of cards

Inside several of those `item { }` blocks sits a horizontally-scrolling row of cards
built from a real `List<T>` — this is where `items(...)` (plural, list-driven) actually
gets used. `HomeSections.kt:63-76`:

```kotlin
@Composable
fun PartnerRow(partners: List<PartnerItem>, modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = dimensionResource(R.dimen.home_content_padding)),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.partner_card_spacing)),
    ) {
        items(partners.withIndex().toList(), key = { it.value.id }) { (index, partner) ->
            PartnerCard(partner = partner, colorIndex = index)
        }
    }
}
```

- **`key = { it.value.id }`** tells Compose how to tell entries apart across
  recompositions — if `partners` changes (an item added/removed/reordered), Compose uses
  the key to figure out which composables can be reused as-is versus which are genuinely
  new, instead of naively assuming position `0` is always "the same" item. This matters
  for both correctness (e.g. `remember`ed per-item state staying attached to the right
  item) and performance.
- `partners.withIndex().toList()` pairs each partner with its list position up front, so
  `PartnerCard` can pick a placeholder gradient color by index (`colorIndex`) without
  each card having to search the list for its own position.
- The same `LazyRow` shape repeats three more times in `HomeSections.kt` — for the place
  cards, feature cards, and media cards — always constrained to a fixed card size via
  `Modifier.width(...)`/`.size(...)` on the child (a `LazyRow`/`LazyColumn` needs its
  children to have *some* bounded size along the scroll axis; unlike a plain `Row`, it
  can't just ask an unbounded child "how big do you want to be").

## 5. The promo banner: reusing the `HorizontalPager` auto-scroll pattern verbatim

The "Friends of WINK+" partner list scrolls freely (`LazyRow`, above), but the promo
banner beneath it snaps to one full-width slide at a time and auto-advances — exactly
`LandingScreen`'s carousel from [[06-2026-09-08-carousel-slider-walkthrough]] and
[[08-2026-09-09-jetpack-compose-and-lifecycle]] section 4. `HomeSections.kt:127-134`
reuses that pattern almost line-for-line:

```kotlin
val pagerState = rememberPagerState(pageCount = { banners.size })
val lifecycleOwner = LocalLifecycleOwner.current

LaunchedEffect(banners.size, lifecycleOwner) {
    lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        // ...delay + pagerState.animateScrollToPage(...) loop, same as SlideCarousel
    }
}
```

This is worth noticing precisely *because* it's boring: once you have a working
lifecycle-aware auto-scroll pattern, you copy its shape rather than reinventing it. The
"why" (pause while backgrounded, restart cleanly if `STARTED` is re-entered) doesn't
change just because the content is promo banners instead of onboarding slides.

## 6. `ModalBottomSheet` — a new UI surface

The "More" quick-link tile opens a sheet that slides up from the bottom over the current
screen, rather than navigating to a whole new Activity — appropriate here since it's a
handful of extra shortcuts, not a distinct screen. `HomeComponents.kt:185-188`:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreQuickLinksSheet(items: List<QuickLinkItem>, onItemClick: (QuickLinkItem) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        // ... its content
    }
}
```

- **`@OptIn(ExperimentalMaterial3Api::class)`** — Kotlin/Android: some Material 3 APIs
  (this one included) are still marked experimental by the library authors — meaning
  their signature could still change in a future release. The compiler refuses to compile
  a call site that uses an experimental API unless you explicitly acknowledge that with
  `@OptIn(...)`, naming the specific `@RequiresOptIn`-annotated marker (here,
  `ExperimentalMaterial3Api`) that guards it. You saw the same mechanism already on
  `HomeViewModelTest`'s `@OptIn(ExperimentalCoroutinesApi::class)` — it's a general Kotlin
  feature, not something specific to bottom sheets.
- `sheetState = rememberModalBottomSheetState()` — `remember` here works exactly as
  explained in [[08-2026-09-09-jetpack-compose-and-lifecycle]] section 3: it creates the
  sheet's open/closed/dragging state once and survives it across recompositions, instead
  of resetting to a fresh state every time `MoreQuickLinksSheet` recomposes.
- `HomeScreen.kt` shows/hides it with a plain `var isMoreSheetVisible by remember { mutableStateOf(false) }`
  toggled from the "More" tile's `onClick` and the sheet's own `onDismiss` — no different
  in kind from any other piece of local UI state, just controlling whether
  `MoreQuickLinksSheet` is called at all (i.e. whether it's currently in the Composition
  — see the Composition lifecycle in [[08-2026-09-09-jetpack-compose-and-lifecycle]]
  section 5).

## 7. A pattern that now clearly generalizes: placeholder gradients for missing images

No real photo/logo assets exist for any card on this screen yet. Rather than leaving
blank space or a broken image icon, every card falls back to a brand-color gradient tile
(`placeholderBrandGradient`, cycled by list position) — the *exact same* "no asset yet"
fallback `GlassSlideCard` used for the landing carousel's slide images. Seeing it reused
here (partner logos, place/feature/media cards) rather than reinvented per-screen is the
payoff of [[04-2026-09-08-drawing-the-ui]] step 9's note about Coil: the seam for a real
image later is a single `imageUrl: String?` field away, in one place, for every card type
at once — not a rewrite.

## Key takeaway

Nothing on this screen introduced a new *architectural* idea — Repository/ViewModel/View
still split the same way, `Intent` navigation works exactly as
[[07-2026-09-08-navigation]] described it would, and the auto-scroll carousel logic was
copied, not reinvented. The new vocabulary is specifically about **displaying more
content than fits on screen**: `LazyColumn`/`LazyRow` (compose many items cheaply,
key them for stable identity) and `ModalBottomSheet` (a transient overlay surface that
doesn't need a whole new Activity). That's a reasonable general rule of thumb: reach for
a new *pattern* only when the content genuinely calls for it (a long or unbounded list),
and otherwise reuse what already works.
