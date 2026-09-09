# Add the home screen (`HomeActivity`) with all four reference sections

## What

- **Data layer (`data/model/home/*.kt`, `data/repository/HomeRepository.kt`,
  `data/repository/DefaultHomeRepository.kt`)**: new plain data classes —
  `QuickLinkItem`, `PartnerItem`, `PromoBanner`, `PlaceCard`, `FeatureCard`, `MediaCard`,
  and the aggregate `HomeContent` — plus a `HomeRepository` interface and
  `DefaultHomeRepository` hardcoded implementation, mirroring `CarouselRepository`/
  `DefaultCarouselRepository`'s existing shape. Fixed navigational labels (quick-link
  tile text, section headers, bottom-nav labels) go through `strings.xml`; the mock
  "listing" content itself (partner names, promo copy, place/feature/media card text)
  is plain Kotlin literals in `DefaultHomeRepository`, since it stands in for what a
  future backend response would already return formatted — see that file's comment.
  No real photo assets exist yet, so every card's image area falls back to a
  brand-color gradient tile (`placeholderBrandGradient`, cycled by list position),
  the same "no asset yet" pattern `GlassSlideCard`'s `brandGradientForSlide` already
  used on the landing screen.
- **ViewModel (`viewmodel/home/HomeUiState.kt`, `viewmodel/home/HomeViewModel.kt`)**:
  same `StateFlow<UiState>` + `init { load...() }` + `runCatching { }` shape as
  `LandingViewModel`. Unit tested in `HomeViewModelTest` with a `FakeHomeRepository`,
  mirroring `LandingViewModelTest`.
- **UI (`ui/home/HomeScreen.kt`, `HomeSections.kt`, `HomeComponents.kt`,
  `HomeActivity.kt`)**: a `Scaffold` with a Home/Search/Profile bottom bar (only Home
  is wired up) over a `LazyColumn` containing, top to bottom:
  - a full-bleed pink→purple gradient header with a floating white "My WINK+ Points"
    card (Scan QR / Rewards row underneath) — `wink-home-01.jpeg`;
  - the four Bus/Train/MRT Map/More quick-link tiles, where "More" opens a
    `ModalBottomSheet` with extra shortcuts (`wink-home-01.jpeg`);
  - the "Friends of WINK+" horizontally scrollable partner row — square logo tile
    (brand-gradient + initial) + name + description (`wink-home-01.jpeg`);
  - an auto-scrolling promo banner `HorizontalPager` with a dot indicator, reusing
    `LandingScreen`'s lifecycle-aware pause-while-backgrounded pattern
    (`wink-home-01.jpeg`);
  - a station selector bar (`wink-home-01.jpeg`);
  - two `PlaceCardRow`s — MRTreats Dining Deals (discount badge + strike-through
    price) and Dinner Nearby (star rating + review count + open/hours) — sharing one
    `PlaceCard` shape and `PlaceCardView` composable (`wink-home-02.jpeg`);
  - two `FeatureCardRow`s — Exciting Events Around the Island and Hot Deals and
    Promotions — bigger square cards than the partner logos, with an optional
    overlay badge like "FREE" (`wink-home-03.jpeg`);
  - two `MediaCardRow`s — News and Highlights (landscape cards) and Movies (portrait
    cards) — the same `MediaCardView` composable at two different fixed dimensions
    (`wink-home-04.jpeg`).
  Clean-card/glassmorphism styling matches the existing app: rounded corners, a real
  `Modifier.shadow` on the loyalty card, translucent pink pill for the selected
  bottom-nav tab, and the existing brand purple/pink/teal palette reused everywhere
  instead of introducing a new one.
- **Resources**: ~30 new `strings.xml` entries (home chrome text only, see above),
  ~35 new `dimens.xml` tokens (one block per section, all under a `home_`/section-name
  prefix), and three new `colors.xml` semantic tokens (`rating_star`, `status_open`,
  `chip_background`) that the existing brand palette had no equivalent for.
- **Navigation**: `LandingActivity`'s `onGetStartedClick` (previously a no-op) now
  starts `HomeActivity` via `Intent` and calls `finish()`, so Back from the home
  screen exits the app instead of returning to onboarding. Registered
  `.ui.home.HomeActivity` in `AndroidManifest.xml` (`exported="false"` — it's only
  ever launched from within this app).
- Verified with `gradlew.bat testDebugUnitTest` (all pass, including the two new
  `HomeViewModelTest` cases) and `gradlew.bat lint assembleDebug` (clean build; the
  only lint findings are pre-existing Gradle/SDK-version advisories unrelated to this
  change).

## Why

Requested: build the home page shown across `.claude/references/home/wink-home-0{1..4}.jpeg`,
using a modern glassmorphism/clean-card style consistent with the landing screen's
existing redesign.

## Follow-up / known limitations

- Every card image is a brand-gradient placeholder — no real photo assets or network
  image loading are wired up yet. `PlaceCard`/`FeatureCard`/`MediaCard` already carry
  a nullable-`imageUrl`-shaped seam (`imageContentDescription` today; adding
  `imageUrl` later and rendering it via Coil's `AsyncImage`, as `GlassSlideCard`
  already does) would need no other changes.
- Scan QR, Rewards, the individual quick-link tiles (Bus/Train/MRT Map + the "More"
  sheet's extras), Station Info, and the Search/Profile bottom-nav tabs are all
  no-ops per the task brief ("Noting for now") — none of those destination screens
  exist yet.
- "See more" only renders (as a no-op tap target) on the two `PlaceCardRow`s (dining
  deals, dinner nearby) for now; the feature/media rows don't show a "See more" link
  at all, matching which sections the reference screenshots actually show one on.
- The "Are You Looking For..." card grid visible at the bottom of `wink-home-04.jpeg`
  was intentionally left out — the task's four numbered points don't mention it.
