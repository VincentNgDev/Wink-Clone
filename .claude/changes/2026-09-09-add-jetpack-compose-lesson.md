# Add a Jetpack Compose + lifecycle lesson

## What

- New file: `.claude/lessons/08-2026-09-09-jetpack-compose-and-lifecycle.md`. Covers, in
  an order chosen by conceptual dependency (what you need before the next section makes
  sense): what Jetpack Compose is vs. the old View system; how it's installed in this
  project (Compose compiler plugin, `buildFeatures.compose`, the Compose BOM, per
  `build.gradle.kts`/`app/build.gradle.kts`/`gradle/libs.versions.toml`); `@Composable`
  functions, `remember`/`State`, and recomposition; the effect APIs (`LaunchedEffect`,
  `DisposableEffect`); the Composition lifecycle (enter/recompose/leave, plus the
  Composition→Layout→Drawing frame phases); how the Composition lifecycle relates to the
  Android Activity lifecycle (`LocalLifecycleOwner`, `repeatOnLifecycle` inside a
  `LaunchedEffect`, `collectAsStateWithLifecycle()`, what rotation does and doesn't
  change); and a full end-to-end walkthrough of the real landing screen, closing with an
  old-system/Compose/project cheat-sheet table.
- Every code example cites real project files/lines
  (`ui/landing/LandingActivity.kt`, `ui/landing/LandingScreen.kt`,
  `app/build.gradle.kts`, `gradle/libs.versions.toml`) rather than hypothetical
  snippets, and cross-links the existing numbered lessons
  ([[03-2026-09-08-android-lifecycle]], [[04-2026-09-08-drawing-the-ui]],
  [[05-2026-09-08-mvvm-architecture]], [[06-2026-09-08-carousel-slider-walkthrough]]).

## Why

Requested: a self-contained lesson explaining Jetpack Compose (what it is, how to
install/use it, its lifecycle, and how that lifecycle relates to the Android Activity
lifecycle), sequenced by importance for someone learning Android dev, added to
`.claude/lessons/`. This project's landing screen already migrated from the old
View/XML system to Compose
(`.claude/changes/2026-09-08-rebuild-landing-in-compose-glassmorphism.md`), so the
lesson is grounded entirely in that real migration rather than invented examples.

## Follow-up / known limitations

- No new code changed — documentation-only addition, so no build/test/lint run was
  needed.
- `DisposableEffect` is explained conceptually but has no real usage in this codebase
  yet to cite a line number for; worth updating with a real project reference if/when
  one is added (e.g. a broadcast receiver or listener registration in a future screen).
