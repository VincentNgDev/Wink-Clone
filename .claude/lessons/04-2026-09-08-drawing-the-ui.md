# Lesson: How the UI actually gets "drawn" (the part with no direct analog outside Android)

This is the one that trips up people coming from web/other UI frameworks the most,
because Android splits "describe what the screen looks like" (XML) from "put it on
screen and keep it updated" (Kotlin) more strictly than most frameworks. Walking through
it top-to-bottom using this project's actual landing screen.

## Update: superseded by Jetpack Compose

The XML-layout-and-`findViewById` system this lesson walks through was replaced for the
landing screen in `.claude/changes/2026-09-08-rebuild-landing-in-compose-glassmorphism.md`,
and every screen since (Home, and any new one) has been built in Compose from the start —
see [[08-2026-09-09-jetpack-compose-and-lifecycle]], which opens with a direct
old-vs-new comparison table. `activity_landing.xml`/`item_carousel_slide.xml` and the
`LandingActivity`/`CarouselAdapter` classes this lesson references no longer exist in
the codebase.

This lesson is still worth reading, for two reasons: the **View system is still fully
supported Android**, so you'll meet it in other codebases/tutorials, and several ideas
here carry over unchanged into Compose — `R`/resource IDs (§3), `res/values/` and why
hardcoded strings/colors are banned, and `themes.xml`/`Theme.Clonedwink` (the
`ClonedWinkTheme` Compose wrapper in [[08-2026-09-09-jetpack-compose-and-lifecycle]]
reads from the exact same theme resource). Read this first if XML/`R`/resources are
totally unfamiliar, then read [[08-2026-09-09-jetpack-compose-and-lifecycle]] for how
this project actually draws its UI today.

## Step 1: Layouts are XML files, not Kotlin code

`app/src/main/res/layout/activity_landing.xml` *describes* the landing screen as a tree
of "Views" (Android's term for any UI widget — a button, text, image, container, etc.):

```xml
<androidx.constraintlayout.widget.ConstraintLayout ...>
    <TextView android:id="@+id/landingTitle" ... />
    <androidx.viewpager2.widget.ViewPager2 android:id="@+id/carouselViewPager" ... />
    <com.google.android.material.tabs.TabLayout android:id="@+id/carouselIndicator" ... />
    <Button android:id="@+id/getStartedButton" ... />
</androidx.constraintlayout.widget.ConstraintLayout>
```

This file contains **zero logic** — no data, no "if user is logged in show X." It's a
static blueprint. Every screen in this project (and most Android apps) has one XML
layout file per screen, plus one per reusable row (here,
`item_carousel_slide.xml` — one carousel page).

## Step 2: "Inflating" — turning XML into real objects at runtime

XML on its own is inert. `LandingActivity.onCreate()` (`ui/landing/LandingActivity.kt`)
does:

```kotlin
setContentView(R.layout.activity_landing)
```

This tells Android: read `activity_landing.xml`, and for every tag in it, construct the
actual Kotlin/Java object it names (a `ConstraintLayout` object, a `TextView` object, a
`ViewPager2` object, a `Button` object...), wire them into a parent/child tree matching
the XML nesting, and make that tree this Activity's visible content. This process is
called **inflation**. It's roughly analogous to a browser parsing HTML into a DOM tree —
except in Android, you (mostly) don't touch that tree again with more XML; you touch it
with Kotlin.

## Step 3: `R` — how Kotlin code refers to XML resources

Every resource (a layout file, a string, a color, an image, an id...) gets a unique
integer constant auto-generated into a class called `R` at build time. `R.layout.activity_landing`
above is one example. You'll see this pattern everywhere:

| XML resource | Kotlin reference |
|---|---|
| `res/layout/activity_landing.xml` | `R.layout.activity_landing` |
| `res/values/strings.xml` → `<string name="landing_title">` | `R.string.landing_title` |
| `res/drawable/bg_carousel_slide_1.xml` | `R.drawable.bg_carousel_slide_1` |
| `android:id="@+id/carouselViewPager"` in XML | `R.id.carouselViewPager` |

This indirection is *why* `.claude/rules/coding-style.md` bans hardcoded strings/colors:
`R.string.*` is the one source of truth, and Android automatically swaps in the right
translation/theme variant behind that same constant.

## Step 4: `findViewById` — getting a handle on an inflated View

After `setContentView`, individual views can be looked up by the `android:id` given to
them in XML:

```kotlin
val carouselViewPager = findViewById<ViewPager2>(R.id.carouselViewPager)
```

This walks the inflated tree looking for the view with that id, and casts it to
`ViewPager2` (the `<ViewPager2>` type parameter tells Kotlin what to cast to, so no
separate `as ViewPager2` is needed). From here on, `carouselViewPager` is a normal
Kotlin object you can call methods on (`.adapter = ...`, `.setCurrentItem(...)`).

This is the fundamental Android UI loop: **XML says what exists and how it's laid out;
Kotlin looks it up and tells it what to display / reacts to it.**

## Step 5: Layout containers — how position/size is decided

A "ViewGroup" is a View that can hold other Views and arranges them. This project uses
two kinds:

- **`ConstraintLayout`** (`activity_landing.xml`, the screen root) — each child
  positions itself relative to constraints you declare, e.g.:
  ```xml
  app:layout_constraintTop_toBottomOf="@id/landingTitle"
  app:layout_constraintStart_toStartOf="parent"
  ```
  means "my top edge sits below `landingTitle`'s bottom edge; my start (left, in LTR)
  edge aligns with my parent's start edge." It's like a lightweight constraint-solver —
  you declare relationships, not fixed pixel coordinates, so the layout adapts to
  different screen sizes. It's the recommended root layout because it keeps the view
  tree flat (better performance) instead of nesting many layouts inside each other.
- **`FrameLayout`** (`item_carousel_slide.xml`, one carousel page) — the simple case:
  children are stacked directly on top of each other in declaration order (first child
  at the back, each later child drawn above it). That's used deliberately here: the
  background image is declared first, a gradient scrim second (drawn on top of the
  image), and the title/subtitle text last (drawn on top of both) — see
  `item_carousel_slide.xml`.
- **`LinearLayout`** (nested inside the `FrameLayout` above, holding title+subtitle) —
  arranges children in a single row or column, in this case `android:orientation="vertical"`
  stacking the title above the subtitle.

## Step 6: Sizing units — `dp` vs `sp` vs `px`

You'll see `220dp`, `20sp`, `16dp` throughout the XML (e.g. `values/dimens.xml`,
`activity_landing.xml`). Android devices have wildly different pixel densities, so raw
pixels (`px`) would make the same layout look tiny on a high-density screen and huge on
a low-density one. Instead:

- **`dp`** (density-independent pixels) — a unit that's scaled by the system based on
  screen density so a `48dp` button is roughly the same *physical* size on every device.
  Used for almost everything: widths, heights, margins, padding.
  `carousel_height` = `220dp` in `values/dimens.xml` is a good example.
  reused across `activity_landing.xml` (via `@dimen/carousel_height`).
- **`sp`** (scale-independent pixels) — like `dp`, but *also* scaled by the user's
  system font-size accessibility setting. Always used for text size
  (`android:textSize="20sp"` in `item_carousel_slide.xml`), never `dp`, so users who
  bump up their system font size get bigger carousel text too.

## Step 7: Reusable values live in `res/values/`, referenced with `@`

Rather than hardcoding `#FF6C5CE7` or `16dp` inline in every layout, this project
defines them once and references them everywhere:

- **`values/colors.xml`** — e.g. `carousel_slide_1_background`, `carousel_dot_selected`.
  Referenced in XML as `@color/carousel_dot_selected`, and (rarely needed) in Kotlin via
  `ContextCompat.getColor(...)`.
- **`values/dimens.xml`** — e.g. `carousel_dot_size = 8dp`. Referenced as
  `@dimen/carousel_dot_size`.
- **`values/strings.xml`** — all user-visible text, referenced as `@string/landing_title`
  or, in Kotlin, `context.getString(R.string.landing_title)` (see
  `DefaultCarouselRepository.kt`).
- **`values/themes.xml`** (`Theme.Clonedwink`) — app-wide defaults like
  `colorPrimary`, applied via `android:theme="@style/Theme.Clonedwink"` on the
  `<application>` tag in `AndroidManifest.xml`. Individual views can reference theme
  attributes with `?attr/...` (see `android:statusBarColor` in `themes.xml`).
- **`values-night/themes.xml`** — an *alternate* version of the same theme, automatically
  used instead of `values/themes.xml` when the device is in dark mode. Same resource
  name (`R.style.Theme_Clonedwink`), different definition picked at runtime — this
  qualifier-suffix pattern (`-night`, and similarly `-hdpi`, `-v26`, etc. seen in
  `res/mipmap-anydpi-v26/`) is how Android handles "different value depending on device
  config" without any `if` statements in your code.

## Step 8: Drawables — images and shapes, also declared in XML

Not all images are bitmap files. This project's carousel visuals are built from
*vector/shape drawables* — XML files that describe a shape to render:

- **`drawable/bg_carousel_slide_1.xml`** — a solid-color rectangle:
  ```xml
  <shape><solid android:color="@color/carousel_slide_1_background" /></shape>
  ```
  (a placeholder for real photography — see the carousel lesson's follow-up notes).
- **`drawable/scrim_bottom_gradient.xml`** — a gradient shape (transparent → black),
  layered over the slide image so white text stays readable regardless of the photo
  underneath:
  ```xml
  <shape><gradient android:angle="90" android:startColor="@color/carousel_scrim_transparent" android:endColor="@color/carousel_scrim_opaque" /></shape>
  ```
- **`drawable/carousel_indicator_selector.xml`** — a *state list* drawable: which
  drawable to use depends on the View's state (`dot_selected.xml` when
  `android:state_selected="true"`, `dot_unselected.xml` otherwise). This is what makes
  the currently-active carousel dot look different with zero Kotlin code — TabLayout
  marks the active tab as "selected" and the selector swaps drawables automatically.
- **`drawable/dot_selected.xml`** — an oval shape (`android:shape="oval"`) with a fixed
  size — this is literally how a "dot" is drawn: not an image asset, a tiny circle shape.

## Step 9: Actually loading a bitmap image (Coil)

For real photos (not shape drawables), this project uses the Coil image-loading
library:

```kotlin
image.load(slide.imageUrl ?: slide.imageRes)
```

(`ui/landing/CarouselAdapter.kt`) — `load(...)` is a Kotlin *extension function* Coil
adds to `ImageView`. It handles decoding, caching, and (for a URL) an async network
fetch, then sets the result as the ImageView's bitmap once ready — all off the main
thread, so it never freezes the UI while an image downloads.

## Step 10: Redrawing when data changes — the RecyclerView/ViewPager2 pattern

For a *list* of similar items (the carousel's pages), Android doesn't inflate a whole
new View per item indefinitely — see [[06-2026-09-08-carousel-slider-walkthrough]] for how
`CarouselAdapter` recycles a small number of views instead, which is the standard
Android answer to "how do I efficiently draw a scrolling list."

## The full mental model, summarized

```
XML (res/layout/*.xml)  →  inflate()/setContentView()  →  real View objects in memory
        ↑ referenced via R.*                                       │
        │                                                findViewById(R.id...)
   res/values/*.xml (strings/colors/dimens/themes)                 │
   res/drawable/*.xml (shapes/gradients/selectors)                 ▼
                                                    Kotlin reads/writes View properties
                                                    (view.text = ..., view.visibility = ...)
                                                    to reflect current app state
```

Nothing "redraws" automatically when your data changes — a View shows whatever its
properties were last set to. That's exactly the job `LandingActivity.observeUiState()`
does: every time `LandingViewModel` emits a new `LandingUiState`, it reaches into the
already-inflated views and updates them (`carouselAdapter.submitList(...)`,
`carouselIndicator.visibility = ...`).
