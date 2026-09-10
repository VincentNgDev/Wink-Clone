# Lesson: The project's folder structure — what every top-level file/folder is for

Earlier lessons explained specific files in depth as they came up:
[[01-2026-09-08-app-entry-point]] and [[02-2026-09-08-android-manifest]] cover
`AndroidManifest.xml`, and [[04-2026-09-08-drawing-the-ui]] covers `res/values/` and
`R`. This lesson is the missing wide-angle view — a tour of **every other** file and
folder at the root of the repo and under `app/`, answering "what is this, and why does it
exist" for each, so opening the project tree in Android Studio stops looking like a wall
of unfamiliar names.

## The big picture: two layers of "project"

An Android Studio project is a **Gradle project** (Gradle is the build tool — it compiles
Kotlin, packages resources, and produces the final `.apk`) that happens to contain
Android-specific pieces. Everything at the repo root is Gradle/tooling configuration;
everything under `app/src/` is the actual Android app. Keeping that split in mind makes
the root-level file list much less intimidating — most of it is about *building* the app,
not *being* the app.

```
wink-app-clone/                        ← the Gradle root project ("clonedwink")
├── settings.gradle.kts                ← declares which modules exist
├── build.gradle.kts                   ← root-level build config (shared across modules)
├── gradle.properties                  ← Gradle/JVM settings for this project
├── gradlew / gradlew.bat              ← the Gradle Wrapper scripts (Unix / Windows)
├── gradle/
│   ├── wrapper/                       ← exact Gradle version to download & use
│   ├── gradle-daemon-jvm.properties   ← exact JDK version the build itself runs on
│   └── libs.versions.toml             ← the version catalog (single source of dependency versions)
├── .idea/                             ← Android Studio's own project settings (not Gradle, not the app)
├── .gitignore                         ← top-level ignore rules
└── app/                               ← the one Gradle *module* this project has
    ├── build.gradle.kts               ← module-level build config (this app specifically)
    ├── .gitignore                     ← module-level ignore rules (build output, etc.)
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml    ← see [[02-2026-09-08-android-manifest]]
        │   ├── java/...                ← Kotlin source — see [[05-2026-09-08-mvvm-architecture]]
        │   ├── res/...                 ← resources — see [[04-2026-09-08-drawing-the-ui]] §7
        │   └── keepRules/rules.keep   ← R8/ProGuard keep rules
        ├── test/java/...              ← JVM unit tests (run on host, no device)
        └── androidTest/java/...       ← instrumented tests (run on a device/emulator)
```

## Gradle basics: modules, and why there's both a root and an `app/` build file

A Gradle project is made of one or more **modules** — independently buildable chunks of
code. This project has exactly one, `app` (an app could grow more later, e.g. a separate
`core` or `feature-login` module, each with its own `build.gradle.kts`). Two files
declare that structure:

- **`settings.gradle.kts`** — the very first file Gradle reads. `include(":app")`
  (`settings.gradle.kts:26`) is what tells Gradle "there is a module called `app`, look
  for it in the `app/` folder." It also configures where Gradle is allowed to download
  dependencies and plugins from (`google()`, `mavenCentral()` in the `repositories { }`
  blocks) and names the whole project (`rootProject.name = "clonedwink"`).
- **`build.gradle.kts` (root)** — applies to the project as a whole, not any one module.
  Here it only declares which Gradle **plugins** are available for modules to opt into
  (`alias(libs.plugins.android.application) apply false` — `apply false` means "make this
  plugin available, but don't turn it on at the root level, since the root itself isn't
  an Android module"). `app/build.gradle.kts` then actually applies (`alias(...)`, no
  `apply false`) the ones it needs.
- **`app/build.gradle.kts` (module-level)** — the file that actually configures *this
  app*: `namespace`/`applicationId` (see [[01-2026-09-08-app-entry-point]]), `compileSdk`/
  `minSdk`/`targetSdk`, `buildFeatures { compose = true }` (see
  [[08-2026-09-09-jetpack-compose-and-lifecycle]] §2), and the full `dependencies { }`
  block listing every library the app actually uses.

## The version catalog: `gradle/libs.versions.toml`

Per `.claude/rules/coding-style.md`, every dependency version in this project is declared
**once**, here, instead of hardcoded inline in `app/build.gradle.kts`:

```toml
[versions]
coreKtx = "1.19.0"
composeBom = "2026.08.00"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
```

- `[versions]` — every version number, named, in one place. Bump `coreKtx` here once and
  every module using `libs.androidx.core.ktx` picks up the new version.
- `[libraries]` — each dependency's Maven coordinates (`group:name`), pointing at a
  version from `[versions]` via `version.ref`. `app/build.gradle.kts` then refers to
  these as `libs.androidx.core.ktx` (dots become the generated accessor name) instead of
  writing out `"androidx.core:core-ktx:1.19.0"` as a raw string.
- `[plugins]` — the same idea, for Gradle **plugins** (build-time behavior, like "this
  module is an Android app" or "run the Compose compiler") rather than runtime libraries.
- Why bother? A raw string like `"androidx.core:core-ktx:1.19.0"` gives no compile-time
  safety (a typo just silently fails to resolve) and, with many dependencies, makes it
  easy for two libraries that are supposed to share a version to drift apart. The catalog
  centralizes both problems into one typo-checked, IDE-autocompleted file.

## Gradle's own moving parts: the Wrapper and the daemon toolchain

These are about *what runs the build itself*, separate from what the build produces:

- **`gradlew` / `gradlew.bat`** — the **Gradle Wrapper** scripts (Unix/Bash vs Windows
  respectively — that's why every command in `CLAUDE.md`'s "Common commands" section
  uses `gradlew.bat`, since development here happens on Windows). Running `./gradlew ...`
  instead of a globally-installed `gradle` guarantees everyone building this project
  (you, a CI server, a teammate) uses the *exact* same Gradle version — no "works on my
  machine because I have a different Gradle installed" class of bug.
- **`gradle/wrapper/gradle-wrapper.properties`** — where the Wrapper scripts get their
  instructions: `distributionUrl` (`gradle-9.5.0-bin.zip`) says exactly which Gradle
  version to download (once, then cached) and use. `gradle-wrapper.jar` alongside it is
  the small bootstrap program the `gradlew`/`gradlew.bat` scripts actually invoke.
- **`gradle/gradle-daemon-jvm.properties`** — a separate, newer setting: which **JDK**
  (Java Development Kit) the Gradle daemon process itself runs on (`toolchainVersion=25`
  here — see `CLAUDE.md`'s "Gradle daemon toolchain: JDK 25"). This is distinct from the
  app's own `compileOptions { sourceCompatibility = JavaVersion.VERSION_11 }` in
  `app/build.gradle.kts` — that setting controls what Kotlin/Java language level *your
  app's code* targets; this file controls what JDK *Gradle itself* runs under while doing
  the building. Gradle can (and often does) build code targeting an older Java version
  while running on a newer JDK.
- **`gradle.properties`** — general Gradle settings for this project: JVM memory flags
  for the build process (`org.gradle.jvmargs`), and feature flags like
  `org.gradle.configuration-cache=true` (lets Gradle skip re-evaluating build scripts on
  unchanged runs, for faster incremental builds).

## `app/src/main/keepRules/rules.keep` — telling R8 what *not* to delete

R8 is the tool that shrinks, obfuscates, and optimizes the app's compiled code for
release builds — it aggressively deletes anything it thinks is unused. The problem: R8
can't always tell that something *is* used if it's only referenced reflectively (e.g. a
class name looked up as a string, common with some JSON/DI libraries). `rules.keep` is
where you'd tell R8 "don't touch this, even though you can't see a direct reference" —
right now it's just the scaffold's commented-out example (a WebView JavaScript-interface
rule) with nothing active, because nothing in this project needs it yet. Note in
`app/build.gradle.kts:22-27` that the `release` build type currently has R8 optimization
turned **off** (`optimization { enable = false }`) — a scaffold default; this file will
matter more once release-build shrinking is actually turned on.

## Two kinds of tests, two folders — why they're not together

- **`app/src/test/java/...`** — JVM unit tests. These run directly on your development
  machine's JVM, with no emulator/device and no real Android framework — fast, but only
  usable for code that doesn't touch real Android APIs. This is where
  [[05-2026-09-08-mvvm-architecture]]'s ViewModel/Repository tests live
  (`LandingViewModelTest`, `HomeViewModelTest`), since MVVM's whole point is keeping that
  logic independent of `Context`/`View`/`Activity`.
- **`app/src/androidTest/java/...`** — instrumented tests. These actually install and run
  on a real device or emulator, because they need the real Android framework (rendering a
  View, querying a real `Context`). Slower, but the only option for anything UI-level.
  `ExampleInstrumentedTest.kt` here is still the unmodified scaffold default — nothing
  UI-level has been tested yet.
- Both mirror `main`'s package structure (`viewmodel/home/HomeViewModelTest.kt` tests
  `viewmodel/home/HomeViewModel.kt`), per `.claude/rules/folder-structure.md`.

## `.idea/` — Android Studio's project settings, not Gradle's

Everything under `.idea/` (`codeStyles/`, `gradle.xml`, `runConfigurations.xml`, etc.) is
IntelliJ/Android Studio's **own** project metadata — code style rules the IDE enforces,
which run configurations show up in the toolbar, which Gradle JDK the IDE points at. None
of it affects a command-line `gradlew.bat build` — it's purely "how does *this IDE*
present and interact with the project." It's checked into git here so the whole team's
Android Studio opens the project with the same formatter/run-configuration setup, rather
than everyone hand-configuring their own.

## The two `.gitignore` files

- **Root `.gitignore`** — ignores things at the whole-repo level: `.gradle/` (Gradle's
  own cache/output, regenerated from the build files above — never worth committing),
  local `local.properties` (machine-specific SDK path), IDE noise.
- **`app/.gitignore`** — module-scoped; here it just ignores `app/build/`, the module's
  own compiled output directory. Compiled output is always regenerated from source by
  running a build, which is exactly why it's excluded from version control rather than
  committed alongside the source that produces it.

## `res/xml/` — two files you'll likely never touch, but should know exist

Two scaffold-default resources declared on the `<application>` tag in
`AndroidManifest.xml` (see [[02-2026-09-08-android-manifest]]), both about Android's
automatic backup system:

- **`backup_rules.xml`** — for the older (pre-Android 12) full-backup system: which
  files/shared-prefs to include or exclude when Android backs up app data.
- **`data_extraction_rules.xml`** — the modern (Android 12+) equivalent, split into rules
  for cloud backup vs. device-to-device transfer separately.

Both are currently scaffold defaults (backup everything, no exclusions) — worth revisiting
once the app actually stores anything sensitive (auth tokens, for instance) that
shouldn't survive a backup/restore onto a different device.

## Key takeaway

Nearly everything at the repo root exists to answer one of three questions Gradle needs
answered before it can build anything: **which modules exist** (`settings.gradle.kts`),
**which plugins/dependencies at which exact versions** (`build.gradle.kts` files +
`libs.versions.toml`), and **which Gradle/JDK version to build with**
(`gradle/wrapper/`, `gradle-daemon-jvm.properties`). Everything under `app/src/` is where
the actual app — and its tests — live, split by *what kind of code it is*
(`main`/`test`/`androidTest`) the same deliberate way `.claude/rules/folder-structure.md`
splits `main` itself by *architectural layer* (`ui`/`viewmodel`/`data`).
