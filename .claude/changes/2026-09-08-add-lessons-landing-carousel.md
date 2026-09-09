# Add teaching lessons for the landing/carousel feature

## What

Added `.claude/lessons/`, a new set of Markdown teaching docs (same
`YYYY-MM-DD-short-description.md` naming convention as `.claude/changes/`), explaining
the codebase module by module for the user's Android/Kotlin learning:

- `2026-09-08-app-entry-point.md` — what "main entry point" means in Android (manifest
  `LAUNCHER` intent-filter → `LandingActivity`), since there's no `main()` function.
- `2026-09-08-android-lifecycle.md` — the Activity lifecycle (`onCreate`→`onDestroy`),
  why rotation destroys/recreates the Activity, and how `ViewModel`/`viewModelScope`/
  `lifecycleScope`/`repeatOnLifecycle` relate to it.
- `2026-09-08-mvvm-architecture.md` — how View/ViewModel/Model map onto this feature's
  actual files and the one-way data flow between them.
- `2026-09-08-drawing-the-ui.md` — the "most important" one per the user's request:
  XML layouts, inflation, `R.*`, `findViewById`, ConstraintLayout/FrameLayout/LinearLayout,
  `dp`/`sp` units, `res/values/*` resource references, shape/gradient/selector
  drawables, and Coil image loading.
- `2026-09-08-carousel-slider-walkthrough.md` — the carousel specifically: ViewPager2 +
  `CarouselAdapter`/`ListAdapter`/`DiffUtil`, `TabLayoutMediator` dot syncing, and the
  coroutine-based auto-scroll loop.
- `2026-09-08-navigation.md` — current state (single Activity, no navigation wired up
  yet) plus an explanation of the two general Android navigation models
  (`Intent`/`startActivity` vs. the Jetpack Navigation Component) and which fits this
  project next.

Lessons cross-link each other with `[[wiki-link]]`-style references to related lessons.

## Why

The user asked for an explanation of the carousel implementation, the Android lifecycle,
the app's entry point, the MVVM implementation, navigation, and (most importantly, per
the user) how the UI is actually "drawn" — and asked for it saved as lesson files rather
than only conversational output, split module by module.

## Follow-up / known limitations

- Lessons describe the codebase as of this commit; if the landing/carousel code changes
  meaningfully later (e.g. a real navigation destination is added, or the placeholder
  drawables are replaced), the relevant lesson(s) should be updated or a new dated one
  added rather than treating these as permanently accurate.
- No lesson yet for build system/Gradle internals or testing patterns in depth (the
  latter is touched on briefly inside the MVVM lesson) — candidates for future lesson
  files if asked.
