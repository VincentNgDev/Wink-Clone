# Update lessons for Hilt + navigation-compose migration

## What

Brought `.claude/lessons/` back in sync with the codebase after the Hilt DI +
navigation-compose migration (`47ad598`), which several lessons had drifted out of date
against:

- **`05-2026-09-08-mvvm-architecture.md`** — rewritten. The View/ViewModel/Model split
  itself didn't change, but the concrete wiring had: View is now `LandingScreen.kt` (a
  Compose function, not `LandingActivity`), the ViewModel is Hilt-constructed
  (`@HiltViewModel`/`@Inject`), and the two are connected inside
  `ui/navigation/WinkNavHost.kt` via `hiltViewModel()`, not an Activity's `onCreate()`.
- **`01-app-entry-point.md`, `02-android-manifest.md`, `03-android-lifecycle.md`,
  `09-home-screen-feature-and-lists.md`, `08-jetpack-compose-and-lifecycle.md`,
  `12-hilt-dependency-injection.md`** — added "Update" sections (the pattern already
  established in `07-navigation.md`) pointing out what's stale and linking to the
  current lesson/code, while keeping the original walkthroughs intact for their
  still-accurate general Android/Kotlin knowledge (e.g. the View system, manual
  `repeatOnLifecycle`, Activity-scoped `by viewModels()`).
- **`04-drawing-the-ui.md`, `06-carousel-slider-walkthrough.md`** — added pointers to the
  Compose equivalents that superseded the XML/ViewPager2/RecyclerView content they
  describe.
- **`10-project-and-folder-structure.md`** — noted the two packages added since it was
  written (`di/`, `ui/navigation/`).
- Fixed stale in-code learning comments in `LandingScreen.kt` and `HomeScreen.kt` that
  still described `LandingActivity`/`HomeActivity` owning their ViewModel via
  `by viewModels {}` and calling `enableEdgeToEdge()` per-screen — both now happen once,
  in `MainActivity`/`WinkNavHost`.

## Why

The lessons are meant to teach from this project's *actual current code*
(`CLAUDE.md`'s "learning mode"). Two migrations landed back-to-back (Hilt DI, then
navigation-compose) and left several lessons — including the central MVVM one —
describing deleted classes (`LandingActivity`, `HomeActivity`) and outdated patterns
(`by viewModels()` + hand-written factories) as if they were still current, which would
actively mislead someone learning from them.

## Follow-up / limitations

- `11-reusable-components.md` and `13-navigation-compose-vs-multi-activity.md` were
  checked and are already accurate — no changes needed.
- Historical sections were kept (not deleted) and clearly marked, matching this
  project's existing convention of layering "Update" notes on top of a lesson rather
  than rewriting history away — except `05-mvvm-architecture.md`, which was fully
  rewritten since the wiring section is the lesson's whole point and a "keep the old
  content below" append would have left the more *important* half of the lesson stale.
