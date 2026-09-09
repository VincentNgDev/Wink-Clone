# Set up the Wink+ brand theme and modernize the carousel

## What

- **Theme (`values/colors.xml`, `values-night/colors.xml`, `values/themes.xml`,
  `values-night/themes.xml`)**: replaced the default Android Studio scaffold palette
  (`purple_500`/`teal_200`/...) with a Wink+ brand palette (`brand_purple`, `brand_pink`,
  `brand_teal`, each with a darker variant) and semantic light/dark tokens (`surface`,
  `background`, `on_surface`, `on_surface_variant`, `carousel_dot_unselected`) that swap
  automatically between the `values` and `values-night` color files. Switched the theme
  parent from `Theme.MaterialComponents.DayNight.DarkActionBar` to
  `Theme.Material3.DayNight.NoActionBar` (Material 1.14.0 already on the classpath) and
  set Material3 color attributes (`colorPrimary`/`colorSecondary`/`colorTertiary` +
  `colorSurface`/`colorOnSurface`/`colorOnBackground`) from those semantic tokens, plus a
  light/dark-correct `windowLightStatusBar`.
- **Carousel (`activity_landing.xml`, `item_carousel_slide.xml`, `LandingActivity.kt`,
  dot drawables, `dimens.xml`)**: redesigned the carousel into a "peeking card" style —
  `MaterialCardView` slide items with rounded corners (`cardCornerRadius`) and elevation
  instead of a flat full-bleed `FrameLayout`; `ViewPager2` given horizontal padding +
  `clipToPadding`/`clipChildren="false"` so neighboring cards peek in from the screen
  edges; a new `setUpCarouselPageTransformer()` in `LandingActivity` combining a
  `MarginPageTransformer` (gap between cards) with a scale/fade transform on
  off-center pages. The dot indicator now expands into a pill shape for the selected
  page (`dot_selected.xml` is a rounded rect, not a same-size circle) instead of just
  recoloring a fixed-size dot. The "Get started" button is now a pill-shaped
  `MaterialButton` in the brand primary color. Slide background drawables
  (`bg_carousel_slide_*.xml`) use diagonal two-tone gradients instead of flat solid
  colors.
- Removed the now-unused default scaffold colors (`purple_200/500/700`, `teal_200/700`,
  `black`) and the old per-slide `carousel_slide_*_background` color aliases.
- Verified with `gradlew.bat assembleDebug`, `gradlew.bat lint`, and
  `gradlew.bat testDebugUnitTest` — all pass, no new lint warnings.

## Why

Requested: set up the app's visual theme (referencing the real Wink+ app's branding)
before modernizing the carousel, and then give the carousel a more modern look
(rounded "peeking card" carousels with scale/fade transitions and expanding dot
indicators are common in current dating-app UIs, e.g. Tinder/Bumble/Hinge).

## Follow-up / known limitations

- The brand colors are a best-effort approximation of Wink+'s look (this repo has no
  access to Wink+'s actual design assets/brand guide) — swap `brand_purple`/
  `brand_pink`/`brand_teal` in `values/colors.xml` for exact brand hex values if/when
  they're available.
- Carousel artwork is still solid-color/gradient placeholder drawables, not real
  photos — the peeking-card + scale/fade effect will look best once real slide images
  are dropped in via `CarouselSlide.imageRes`/`imageUrl`.
- Not tested on a real device/emulator in this environment (no connected device) —
  verified via `assembleDebug`/`lint`/`testDebugUnitTest` only; recommend running
  `installDebug` and eyeballing the carousel motion and light/dark theme before
  shipping.
