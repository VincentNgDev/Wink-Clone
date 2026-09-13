# Add Hilt for dependency injection

## What

- `gradle/libs.versions.toml`: added `hilt` (2.59.2) and `ksp` (2.2.10-2.0.2) versions, the
  `hilt-android`/`hilt-compiler` libraries, and the `hilt-android`/`ksp` plugins.
- `build.gradle.kts` (root): applied the Hilt and KSP plugins with `apply false`.
- `app/build.gradle.kts`: applied the Hilt and KSP plugins for real, and added
  `implementation(libs.hilt.android)` + `ksp(libs.hilt.compiler)`.
- New `app/src/main/java/com/example/clonedwink/WinkApplication.kt`: `@HiltAndroidApp`
  Application class, the root of Hilt's dependency graph.
- New `app/src/main/java/com/example/clonedwink/di/RepositoryModule.kt`: `@Module`/
  `@InstallIn(SingletonComponent::class)` with `@Binds` functions mapping
  `CarouselRepository` → `DefaultCarouselRepository` and `HomeRepository` →
  `DefaultHomeRepository`.
- `DefaultCarouselRepository`/`DefaultHomeRepository`: constructors now `@Inject
  constructor(@param:ApplicationContext private val context: Context)` instead of a plain
  constructor, so Hilt knows how to build them.
- `LandingViewModel`/`HomeViewModel`: annotated `@HiltViewModel` with `@Inject constructor`.
- `LandingActivity`/`HomeActivity`: annotated `@AndroidEntryPoint`; replaced the hand-written
  `by viewModels { viewModelFactory { initializer { ... } } }` blocks with plain
  `by viewModels()` now that Hilt generates the factory.
- `AndroidManifest.xml`: registered `android:name=".WinkApplication"` on `<application>`.
- `gradle.properties`: added `android.disallowKotlinSourceSets=false`. KSP (as of the
  version pinned here) still registers its generated-sources directory through the older
  `kotlin.sourceSets` API, which AGP 9.3.2's built-in Kotlin support rejects by default
  (`google/ksp#2729`) — this flag is the documented opt-back-in until KSP ships a fix using
  AGP 9's newer `variant.sources` API.
- New `.claude/lessons/12-2026-09-12-hilt-dependency-injection.md`: explains what DI meant
  in this project before Hilt (manual constructor injection via `viewModelFactory`), what
  each new Hilt annotation does and why, and the end-to-end object graph for both screens.

## Why

Requested: add Hilt for DI, replacing the manual `viewModelFactory` wiring in
`LandingActivity`/`HomeActivity`, and document it as a lesson (following this project's
established `.claude/lessons/NN-...` numbering/cross-linking convention).

## Follow-up / known limitations

- `android.disallowKotlinSourceSets=false` is a workaround for a KSP/AGP-9 compatibility gap
  that only surfaced because this project builds on a very recent AGP release; revisit
  removing it next time the `ksp` version in `gradle/libs.versions.toml` is bumped.
- No `@Module`/`@Binds` exists yet for anything beyond the two repositories — a future
  network client (Retrofit/OkHttp) or database (Room) would get its own `@Provides` function
  in a new module (e.g. `di/NetworkModule.kt`), per Hilt convention of one module per
  concern rather than one giant module.
- Verified with `gradlew.bat assembleDebug`, `gradlew.bat testDebugUnitTest`, and
  `gradlew.bat lint` — all pass; no instrumented-test coverage added since neither Activity's
  behavior changed, only how its ViewModel is constructed.
