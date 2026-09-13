# Lesson: How the carousel slider was built, piece by piece

The carousel is five pieces working together: a **swipeable page container**
(ViewPager2), a **list of data** (`CarouselSlide`s from the ViewModel), an **adapter**
that turns data into row views (`CarouselAdapter`), a **dot indicator** synced to the
current page (`TabLayout` + `TabLayoutMediator`), and an **auto-scroll loop**. Read
[[04-2026-09-08-drawing-the-ui]] first if XML layouts/inflation/`R.*` aren't familiar yet —
this lesson assumes that foundation.

## Update: rebuilt in Compose — `ViewPager2`/`CarouselAdapter`/`TabLayout` are gone

`.claude/changes/2026-09-08-rebuild-landing-in-compose-glassmorphism.md` replaced every
piece below with a Compose equivalent, in `ui/landing/LandingScreen.kt`'s
`SlideCarousel`/`GlassSlideCard` and the shared `ui/components/CarouselAutoScroll.kt` /
`CarouselDotIndicator.kt` (extracted later — see
[[11-2026-09-10-reusable-components]]):

| This lesson (historical) | Current Compose equivalent |
|---|---|
| `ViewPager2` + XML | `HorizontalPager` (`androidx.compose.foundation.pager`) |
| `CarouselAdapter`/`ListAdapter`/`ViewHolder` recycling | `HorizontalPager`'s own lazy content lambda — no adapter/ViewHolder class needed; you just write `{ page -> GlassSlideCard(slides[page]) }` |
| `TabLayout` + `TabLayoutMediator` | `CarouselDotIndicator` — a small composable reading `pagerState.currentPage` directly |
| Manual `PageTransformer` for the peek/shrink effect | `Modifier.graphicsLayer { }` reading `pagerState.currentPageOffsetFraction`, applied per-page inside the `HorizontalPager` content lambda |
| Auto-scroll `Handler`/`Runnable` loop | `CarouselAutoScroll` — a composable wrapping `LaunchedEffect` + `repeatOnLifecycle(STARTED)` + `pagerState.animateScrollToPage(...)` |

The concepts this lesson teaches — why a swipeable list needs *some* recycling/lazy
strategy instead of inflating every page up front, what a page transformer/peek effect
is doing conceptually, why auto-scroll needs to pause while backgrounded — are all still
the right mental model; only the concrete APIs changed. See
[[08-2026-09-09-jetpack-compose-and-lifecycle]] for Compose fundamentals and
`ui/landing/LandingScreen.kt`'s `SlideCarousel` for the real, current code.

## Piece 1: `ViewPager2` — the swipeable container

Declared in `res/layout/activity_landing.xml`:

```xml
<androidx.viewpager2.widget.ViewPager2
    android:id="@+id/carouselViewPager"
    android:layout_width="match_parent"
    android:layout_height="@dimen/carousel_height"
    app:layout_constraintTop_toBottomOf="@id/landingTitle" />
```

Notice it has **no child views declared in XML** — that's because ViewPager2 doesn't
hold static content; it needs an *adapter* (Piece 3) at runtime to know what pages exist
and how to render them. This is a general Android pattern for "a container whose content
is a variable-length list of similar items": you don't write N copies of the child in
XML, you supply one adapter.

## Piece 2: `CarouselSlide` — one page's data

Already covered in [[05-2026-09-08-mvvm-architecture]] — a plain `data class` with a title,
subtitle, content description, and either an `imageRes` or `imageUrl`. The carousel
never hardcodes "3 slides" anywhere in the UI layer; it just renders whatever list
`LandingViewModel` currently has.

## Piece 3: `CarouselAdapter` — turning data into row views

`ViewPager2` (like `RecyclerView`, which it's built on) never keeps one View per data
item in memory. Instead it keeps a small pool of views — just enough to cover what's
currently visible plus a little buffer — and *recycles* them as the user swipes,
re-binding each recycled view to whatever item scrolled into range. This is the
`ListAdapter`/`RecyclerView.Adapter` contract `CarouselAdapter` implements
(`ui/landing/CarouselAdapter.kt`):

```kotlin
class CarouselAdapter : ListAdapter<CarouselSlide, CarouselAdapter.SlideViewHolder>(SlideDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlideViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_carousel_slide, parent, false)
        return SlideViewHolder(view)
    }

    override fun onBindViewHolder(holder: SlideViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    ...
}
```

- **`onCreateViewHolder`** — called only when a *new* row view is actually needed (not
  once per item — only enough times to fill the pool). Inflates
  `res/layout/item_carousel_slide.xml` (the single-page layout: background image +
  gradient scrim + title/subtitle, see [[04-2026-09-08-drawing-the-ui]] Step 7 for how that
  layout is structured) and wraps it in a `SlideViewHolder`.
- **`SlideViewHolder`** — caches the `findViewById` lookups for one row (image, title,
  subtitle) *once*, in its constructor, instead of repeating them every time the row is
  reused — that's the whole performance point of a ViewHolder.
- **`onBindViewHolder`** — called every time a (new or recycled) row needs to show a
  *different* item, e.g. as the user swipes. Calls `holder.bind(getItem(position))`.
- **`bind(slide)`** sets the actual view properties:
  ```kotlin
  image.load(slide.imageUrl ?: slide.imageRes)
  image.contentDescription = slide.imageContentDescription
  title.text = slide.title
  subtitle.text = slide.subtitle
  ```
  `image.load(...)` is Coil's extension function (see [[04-2026-09-08-drawing-the-ui]] Step
  9) — it decides at runtime whether it's loading a bundled drawable resource or fetching
  a URL.

### `submitList` and `DiffUtil` — how it updates without manual diffing

`LandingActivity.observeUiState()` calls `carouselAdapter.submitList(state.slides)` every
time the ViewModel emits a new state. `ListAdapter` (the base class) diffs the new list
against whatever it showed before using `SlideDiffCallback`:

```kotlin
private object SlideDiffCallback : DiffUtil.ItemCallback<CarouselSlide>() {
    override fun areItemsTheSame(oldItem: CarouselSlide, newItem: CarouselSlide) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: CarouselSlide, newItem: CarouselSlide) = oldItem == newItem
}
```

`areItemsTheSame` answers "is this logically the same slide?" (compares `id`);
`areContentsTheSame` answers "did its visible content change?" (full field equality,
free from `data class`). `DiffUtil` uses both, on a background thread, to compute the
minimal insert/remove/change operations and animate only what actually changed — you
never write manual "loop and update views" code for the list.

## Piece 4: `TabLayout` as a dot indicator, synced via `TabLayoutMediator`

The row of dots below the carousel is a `TabLayout` — a widget normally used for tab
bars — repurposed purely for its dots:

```xml
<com.google.android.material.tabs.TabLayout
    android:id="@+id/carouselIndicator"
    app:tabBackground="@drawable/carousel_indicator_selector"
    app:tabIndicatorHeight="0dp"
    app:tabMaxWidth="@dimen/carousel_dot_size"
    app:tabMinWidth="@dimen/carousel_dot_size" />
```

`tabIndicatorHeight="0dp"` hides the usual sliding underline; `tabBackground` swaps in
`carousel_indicator_selector.xml` (the selected/unselected dot drawables from
[[04-2026-09-08-drawing-the-ui]] Step 8) instead of text/icons. On its own, a `TabLayout`
has no idea a `ViewPager2` exists — the link is made in `LandingActivity.onCreate()`:

```kotlin
TabLayoutMediator(carouselIndicator, carouselViewPager) { _, _ -> }.attach()
```

`TabLayoutMediator` is a small helper class whose whole job is: watch which page the
`ViewPager2` is on, and mark the matching tab "selected" (which is what makes the
selector drawable swap to `dot_selected.xml`). The trailing lambda `{ _, _ -> }` is where
you'd normally set `tab.text = ...` per position; it's a no-op here since the dots carry
no text. **`.attach()` is required** — without it, nothing actually syncs.

## Piece 5: Auto-scroll — a coroutine loop, not a `Timer`

```kotlin
private fun autoScroll(carouselViewPager: ViewPager2) {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                delay(AUTO_SCROLL_INTERVAL_MS)
                val itemCount = carouselAdapter.itemCount
                if (itemCount == 0) continue
                carouselViewPager.setCurrentItem((carouselViewPager.currentItem + 1) % itemCount, true)
            }
        }
    }
}
```

- `while (true) { delay(4000L); ... }` is a coroutine-native "repeat forever with a
  pause" loop — `delay` suspends without blocking the underlying thread, unlike
  `Thread.sleep`.
- `(currentItem + 1) % itemCount` wraps back to page 0 after the last page (e.g. 3
  items: page 2 → `(2+1) % 3 = 0`).
- The `true` argument to `setCurrentItem` asks for an animated (smooth swipe)
  transition rather than an instant jump.
- Wrapped in `repeatOnLifecycle(STARTED)` (see [[03-2026-09-08-android-lifecycle]]) so the
  loop is automatically cancelled while the app is backgrounded and restarted when it's
  visible again — otherwise it would keep silently advancing pages (and running a
  `while (true)` loop) even with the screen off.

## How a swipe actually flows through this

1. User drags a finger across the screen.
2. `ViewPager2` (a framework class — no code of ours involved) detects the gesture and
   animates to the next/previous page itself.
3. As the current page changes, `TabLayoutMediator` (also framework/library code)
   notices and marks the corresponding `TabLayout` tab selected → the dot's background
   drawable swaps via the state-list selector.
4. If the page change means a *new* row needs to exist (scrolled far enough that a
   previously-off-screen page comes into view), `CarouselAdapter.onBindViewHolder` is
   called to bind that `CarouselSlide`'s data into a (possibly recycled) row view.

None of this touches `LandingViewModel` — swiping is pure View-layer interaction with no
business decision behind it, which is consistent with MVVM's "the ViewModel holds state,
the View renders/forwards input" split (see [[05-2026-09-08-mvvm-architecture]]). The
ViewModel *is* involved once, earlier: when it first emits the slide list that
`carouselAdapter.submitList(...)` renders.

## Known limitations (carried over from the original implementation)

- The 3 slides use flat-color placeholder drawables (`bg_carousel_slide_1.xml`, etc.),
  not real photography.
- No networked `CarouselRepository` implementation exists yet — `imageUrl` support is
  wired end-to-end (model → adapter → Coil) but nothing populates it yet.
- The "Get started" button has no click handler — see
  [[07-2026-09-08-navigation]] for why, and what adding one would look like.
