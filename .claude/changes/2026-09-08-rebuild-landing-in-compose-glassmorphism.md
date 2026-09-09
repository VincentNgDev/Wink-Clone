# Rebuild the landing screen in Jetpack Compose with a glassmorphism redesign

## What

- **Gradle/Compose setup (`gradle/libs.versions.toml`, `build.gradle.kts`,
  `app/build.gradle.kts`)**: added the Compose Compiler Gradle plugin
  (`org.jetbrains.kotlin.plugin.compose`, pinned to Kotlin `2.2.10` — the KGP version
  AGP 9.3.2's built-in Kotlin support already brings onto the classpath, since this
  project never applies the separate `org.jetbrains.kotlin.android` plugin), enabled
  `buildFeatures.compose`, and added the Compose BOM (`2026.08.00`) plus
  `ui`/`ui-graphics`/`ui-tooling(-preview)`/`material3`/`material-icons-extended`,
  `activity-compose`, `lifecycle-runtime-compose`, and `coil-compose` dependencies.
- **New Compose UI (`ui/theme/Theme.kt`, `ui/landing/LandingScreen.kt`)**: a
  `ClonedWinkTheme` that builds a Material 3 `ColorScheme` from the existing
  `colors.xml`/`values-night/colors.xml` tokens via `colorResource()` (so light/dark
  still resolve automatically, no logic duplicated). `LandingScreen` is a stateless
  composable (`uiState` in, `onGetStartedClick`/`onErrorShown` callbacks out) rendering:
  a full-bleed brand_purple→brand_pink gradient background with two soft blurred
  "blob" accents; a frosted-glass `HorizontalPager` carousel (translucent
  white-fill/white-border icon badge per slide, bottom scrim + title/subtitle, real
  drop shadow via `Modifier.shadow`, and the old ViewPager2 peeking-card scale/fade
  effect reproduced via `graphicsLayer` keyed off `pagerState.currentPageOffsetFraction`
  — read inside the draw-phase `graphicsLayer` block, not the composable body, per a
  real `FrequentlyChangingValue` lint finding caught during this change); an animated
  pill-shaped dot indicator; and a white pill "Get started" CTA. The auto-scroll loop
  is gated by `lifecycleOwner.lifecycle.repeatOnLifecycle(STARTED)`, mirroring the old
  `lifecycleScope`-based pause-while-backgrounded behavior. Content is capped to
  `widthIn(max = 480.dp)` + centered so the layout stays phone-proportioned on
  tablets/foldables instead of stretching into a wide, short bar.
- **`LandingActivity.kt`**: now a `ComponentActivity` (not `AppCompatActivity` — no XML
  layout or View-system widgets remain) calling `enableEdgeToEdge()` and
  `setContent { }`; still owns `LandingViewModel` via `by viewModels { viewModelFactory
  { initializer { ... } } }` unchanged, and collects `uiState` with
  `collectAsStateWithLifecycle()`.
- **Data layer (`DefaultCarouselRepository.kt`)**: stopped setting `imageRes` on the
  hardcoded slides — `painterResource()` only supports VectorDrawables and rasterized
  formats (PNG/JPG/WEBP), and the old `bg_carousel_slide_*.xml` were `<shape>` gradient
  drawables, which crashed the app on launch (`IllegalArgumentException: Only
  VectorDrawables and rasterized asset types are supported`) the first time this was
  tested on a real emulator. `GlassSlideCard` now falls back to a Compose-native
  `brandGradientForSlide(id)` Brush when neither `imageUrl` nor `imageRes` is set, and
  otherwise renders `imageUrl` via Coil's `AsyncImage` or `imageRes` via
  `painterResource` (for a real future vector/bitmap asset) — so a network-backed
  repository with real photos still works unchanged.
- Deleted the now-unused View-system files: `CarouselAdapter.kt`,
  `activity_landing.xml`, `item_carousel_slide.xml`, `carousel_indicator_selector.xml`,
  `dot_selected.xml`, `dot_unselected.xml`, `scrim_bottom_gradient.xml`, and
  `bg_carousel_slide_{1,2,3}.xml`; removed the color/dimen tokens that only they used
  (`carousel_scrim_transparent`, `carousel_dot_selected`, `carousel_dot_unselected`,
  `carousel_scrim_height`) and added new ones the glass redesign needs
  (`glass_fill`, `glass_border`, `glass_indicator_track`, `blob_teal`, `blob_purple`,
  `glass_badge_size`, `glass_badge_icon_size`, `glass_border_width`,
  `blob_size_large/small`) plus a `landing_tagline` string.
- `LandingViewModel`/`LandingUiState`/`CarouselRepository`/`CarouselSlide` and their
  tests are untouched — this was a View-layer-only migration.
- Verified with `gradlew.bat build` (compile + lint + `testDebugUnitTest`, all pass,
  zero lint findings in the new files) and by installing on a booted
  `Pixel_Tablet_API_35` emulator: fixed one real runtime crash (the `painterResource`
  issue above) and one real layout bug (`fillMaxSize().widthIn(max = ...)` doesn't
  actually cap width, because `fillMaxSize` pins min width to the full available width
  first — fixed by switching to `fillMaxHeight().widthIn(max = ...).fillMaxWidth()`)
  found only by looking at the rendered screen.

## Why

Requested: redesign the landing screen in Jetpack Compose with a modern glassmorphism
effect or clean card layouts, matching the visual style of the real Wink+ app
screenshots in `.claude/references/` (bold saturated pink/purple, rounded bold type,
white circular icon badges, floating rounded cards). Consulted the `ui-ux-pro-max`
skill for the glassmorphism recipe (translucent white fill 15–30% opacity, border ~20%
opacity, backdrop blur) and font-pairing/palette guidance; kept the project's existing
purple/pink/teal brand tokens rather than the skill's generic rose palette, since
`colors.xml` already documents them as this app's established identity.

## Follow-up / known limitations

- No custom rounded display font was bundled (e.g. Fredoka/Baloo 2, which the
  `ui-ux-pro-max` skill suggested) — Compose's downloadable-fonts API needs a Google
  Fonts provider/certificate setup this repo doesn't have yet, and fetching a static
  font file wasn't attempted this pass. Headline type currently leans on bold/extrabold
  system-font weight instead. Bundling a real font under `res/font/` would get closer
  to Wink+'s actual look.
- `Modifier.blur()` only produces a real soft blur on API 31+ (confirmed visually on
  the API 35 emulator); on this app's minSdk 24 floor it silently draws the blob shapes
  unblurred (still fine as flat translucent color washes, just not soft-edged).
- `onGetStartedClick` is a no-op — no next screen exists yet in this scaffold.
- The 480dp width cap was chosen for a comfortable phone-proportioned card on
  tablets/foldables; revisit if a genuine two-pane tablet layout is ever wanted instead
  of a centered single column.
