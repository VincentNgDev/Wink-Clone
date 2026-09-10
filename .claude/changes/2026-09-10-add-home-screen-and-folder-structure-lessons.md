# Add Home screen lesson, update the navigation lesson, and add a project/folder-structure lesson

## What

- New `.claude/lessons/09-2026-09-09-home-screen-feature-and-lists.md`: covers the
  `HomeActivity` feature that shipped in
  `.claude/changes/2026-09-09-add-home-screen.md` but had no dedicated lesson yet — real
  `Intent`-based navigation from `LandingActivity`, a second Repository/ViewModel pair
  proving the MVVM pattern generalizes, `LazyColumn`/`LazyRow` (Compose's lazy list
  composables, not covered by the existing Compose lesson), reusing the
  `HorizontalPager` auto-scroll pattern for the promo banner, `ModalBottomSheet` +
  `@OptIn(ExperimentalMaterial3Api::class)`, and the placeholder-gradient "no asset yet"
  pattern now reused across two features.
- Updated `.claude/lessons/07-2026-09-08-navigation.md`: it previously stated "there is
  no navigation yet" and treated `Intent`-based navigation as hypothetical — now
  outdated, since Landing → Home navigation is real code. Rewrote the "Current state"
  section with the actual `LandingActivity`/`HomeActivity` code and manifest entries,
  replaced the hypothetical `Intent` code sample with the real one, and updated the
  "What this means for adding the next screen" / "Key takeaway" sections to describe the
  current two-Activity state instead of a single-Activity future.
- New `.claude/lessons/10-2026-09-10-project-and-folder-structure.md`: a wide-angle tour
  of every root-level and `app/`-level file/folder not already covered by an existing
  lesson — `settings.gradle.kts` vs. root vs. module `build.gradle.kts`, the
  `gradle/libs.versions.toml` version catalog, the Gradle Wrapper
  (`gradlew`/`gradlew.bat`/`gradle/wrapper/`) vs. `gradle-daemon-jvm.properties` vs.
  `gradle.properties`, `app/src/main/keepRules/rules.keep` (R8), the `test/` vs.
  `androidTest/` split, `.idea/` (IDE-only, not Gradle), the two `.gitignore` files, and
  `res/xml/backup_rules.xml`/`data_extraction_rules.xml`. Cross-references rather than
  duplicates the manifest lesson ([[02]]) and the `res/values`/MVVM package-layout
  content already in [[04]]/[[05]].

## Why

Requested: bring the lessons up to date with the most recent work (the Home screen
build had no lesson of its own, and the navigation lesson had gone stale the moment real
navigation shipped), plus a new lesson specifically explaining the Android/Gradle project
file layout (`.gradle`-related files, the manifest, etc.) beyond what
`.claude/rules/folder-structure.md`'s MVVM package-layout rule already documents.

## Follow-up / known limitations

- Lessons are numbered 01–10 now; if more are added, keep following
  `.claude/changes/2026-09-08-number-lessons-and-add-manifest-lesson.md`'s two-digit
  prefix + `[[wiki-link]]` cross-reference convention.
- No lesson yet covers Coil/`AsyncImage` actually loading a real network image (still a
  placeholder-gradient seam on every card, per lesson 09's closing section) — worth its
  own lesson once real image assets/URLs are wired up.
