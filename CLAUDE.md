****# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

Android clone of the Wink+ app, written in Kotlin, targeting an MVVM architecture. The repository is currently a fresh Android Studio scaffold (single `app` module, default package, no `git` history yet) — no `MainActivity`, ViewModel, or data layer exists yet beyond the generated placeholder tests. When adding the first screens, set up the MVVM package structure (e.g. `ui`, `viewmodel`, `data`/`repository`) rather than dumping everything into the root package, and add the relevant AndroidX libraries (Lifecycle/ViewModel, Activity/Fragment-KTX, Coroutines) to `gradle/libs.versions.toml` as they're needed — none are declared yet.

- Application ID / namespace: `com.example.clonedwink`
- Single Gradle module: `app`

## Build system

Gradle Kotlin DSL with a version catalog (`gradle/libs.versions.toml`) — add new dependencies there, not as inline coordinates in `app/build.gradle.kts`.

- AGP: 9.3.2, compileSdk/targetSdk: 37, minSdk: 24
- Java/Kotlin source/target compatibility: 11
- Gradle daemon toolchain: JDK 25 (`gradle/gradle-daemon-jvm.properties`)

## Common commands

Run from the repo root. Use `gradlew.bat` on Windows PowerShell, `./gradlew` from Git Bash.

```
# Build debug APK
gradlew.bat assembleDebug

# Full build (compile, lint, unit tests)
gradlew.bat build

# Install debug build on a connected device/emulator
gradlew.bat installDebug

# Run JVM unit tests (app/src/test)
gradlew.bat testDebugUnitTest

# Run a single unit test class or method
gradlew.bat testDebugUnitTest --tests "com.example.clonedwink.ExampleUnitTest"
gradlew.bat testDebugUnitTest --tests "com.example.clonedwink.ExampleUnitTest.addition_isCorrect"

# Run instrumented tests (app/src/androidTest, requires a connected device/emulator)
gradlew.bat connectedDebugAndroidTest

# Lint
gradlew.bat lint

# Clean build outputs
gradlew.bat clean
```

## Source layout

- `app/src/main/java/com/example/clonedwink/` — app source (empty aside from the manifest-declared theme; add MVVM packages here)
- `app/src/main/res/` — resources (values, mipmaps, drawables, backup/data-extraction XML rules)
- `app/src/test/java/...` — JVM unit tests (JUnit4, run on host)
- `app/src/androidTest/java/...` — instrumented tests (AndroidJUnit4/Espresso, run on device)
- `app/src/main/keepRules/rules.keep` — R8/ProGuard keep rules

## Change log

For every change made to this repository, add a corresponding entry under
`.claude/changes/`. Create one Markdown file per change (e.g.
`.claude/changes/2026-09-07-add-login-viewmodel.md`), named
`YYYY-MM-DD-short-description.md`, containing:

- **What** changed (files/features touched)
- **Why** the change was made
- Any follow-up or known limitations

Do this in the same turn as the code change, not as a separate cleanup step.

## Learning mode: comments

The user is learning Android development with Kotlin through this project. This
**overrides** the terse-comment guidance in `.claude/rules/coding-style.md` ("prefer
self-explanatory names over comments explaining what code does") — for this repo, comment
generously rather than sparsely.

When writing or editing Kotlin/XML in this project:

- Comment Android lifecycle methods (`onCreate`, `onStart`, `onResume`, `onPause`,
  `onStop`, `onDestroy`, `ViewModel.init`, `onCreateViewHolder`/`onBindViewHolder`, etc.)
  with *when the framework calls this and why it matters here* — not just what the line
  does.
- Explain non-obvious Kotlin syntax inline the first time it's used in a file: scope
  functions (`let`, `apply`, `run`, `also`, `with`), delegated properties (`by viewModels`,
  `by lazy`), trailing lambdas, `data class`, `sealed class`, coroutine builders
  (`launch`, `viewModelScope`, `lifecycleScope`, `repeatOnLifecycle`), `StateFlow`/
  `collect`, extension functions, `?.`/`?:`/smart-casts, and named/default parameters.
  A short `// Kotlin: ...` or `// Android: ...` prefix is fine to make these skimmable.
  Extensive comments aren't only for the initial write — apply the same generosity when
  editing or reviewing existing files that predate this preference.
- Still favor clarity over noise: don't restate what a well-named variable already says;
  spend the comment budget on framework/language mechanics that aren't obvious to someone
  new to both.
- This applies to new code and is being back-filled into existing files as they're
  touched — it's fine if older files are less annotated until then.
