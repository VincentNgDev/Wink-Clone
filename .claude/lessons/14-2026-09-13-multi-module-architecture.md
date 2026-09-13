# Lesson: Splitting one Gradle module into many (`core:*` / `feature:*`)

Every earlier lesson described the app's *packages* — `ui/home`, `viewmodel/landing`,
`data/repository`, and so on — all living inside one Gradle module, `app`. This lesson covers
what changed in `.claude/changes/2026-09-13-migrate-to-multi-module-architecture.md`: those
packages became separate Gradle **modules**, each independently compiled, each producing its
own small library artifact that other modules depend on. See
[[10-2026-09-10-project-and-folder-structure]] for the single-module version of this same
territory — this lesson is what replaces its "one Gradle project, one module" picture.

## Why bother splitting at all?

With one module, every file can see every other file — there's nothing stopping a repository
class from importing a Compose screen, or the home screen from reaching into the landing
screen's internals, even though [[05-2026-09-08-mvvm-architecture]] and
`.claude/rules/folder-structure.md` say neither should happen. Package names are a *convention*;
nothing enforces them.

A Gradle module boundary **is** enforced. If `feature:landing` doesn't declare a dependency on
`feature:home`, its code physically cannot import anything from `feature:home` — the compiler
has no classpath entry for it. Splitting the app into modules turns "please don't do this" rules
into "this literally will not compile" guarantees, and as a side effect, changing code inside
one feature module only ever triggers a Gradle rebuild of that module (and anything depending on
it) instead of the whole app.

## The new module graph

```
app
 ├─> core:ui        (ClonedWinkTheme, CarouselAutoScroll, CarouselDotIndicator)
 ├─> core:model     (needed directly for HomeScreen's QuickLinkItem parameter — see below)
 ├─> feature:landing ──┐
 └─> feature:home    ──┤
                        ├─> core:model
                        └─> core:ui

core:network, core:database, feature:auth, feature:profile
  — empty scaffold modules, wired into settings.gradle.kts, no source yet.
```

Two rules this graph follows on purpose:

- **Features never depend on each other.** `feature:landing` and `feature:home` both depend on
  `core:model`/`core:ui`, but neither depends on the other. If Home ever needed something from
  Landing, that "something" would need to move down into a `core` module both can reach —
  exactly the same "shared code goes in the shared layer" idea
  `.claude/rules/folder-structure.md` already applied to packages, just enforced at the module
  level now.
- **`app` is thin.** It only holds `WinkApplication` (Hilt's entry point),
  `MainActivity`/`WinkNavHost` (the NavHost that assembles both features into one app), and the
  two error strings that Toast calls need. Every screen, ViewModel, and repository lives in a
  feature module instead.

## `settings.gradle.kts`: declaring modules that don't match a single folder name

Single-module Kotlin projects like this one used to only have `include(":app")`. Now:

```kotlin
include(":app")
include(":core:model")
include(":core:network")
include(":core:database")
include(":core:ui")
include(":feature:landing")
include(":feature:home")
include(":feature:auth")
include(":feature:profile")
```

Each colon-separated path (`:core:model`) maps to a physical folder of the same shape
(`core/model/`) automatically — Gradle doesn't need to be told that separately. Each of those
folders gets its own `build.gradle.kts`, exactly like `app/build.gradle.kts` always has.

## `com.android.library` vs `com.android.application`

`app/build.gradle.kts` applies `com.android.application` — it's the one module that produces an
installable `.apk`, so it alone has `applicationId`, `versionCode`, `versionName`. Every other
module applies `com.android.library` instead:

```kotlin
// core/model/build.gradle.kts
plugins {
    alias(libs.plugins.android.library)
}
android {
    namespace = "com.example.clonedwink.core.model"
    // no applicationId, versionCode, or versionName — a library isn't installable on its own
}
```

A library module compiles to an **AAR** (Android's library archive format — think "a JAR, plus
resources"), meant to be consumed by another module's `dependencies { }` block, not run
directly. `gradle/libs.versions.toml`'s `[plugins]` table gained an `android-library` entry
alongside the existing `android-application` one so every module can `alias(...)` whichever it
needs.

## `implementation(project(":..."))`: depending on your own modules

Inside a module's `dependencies { }` block, a **project dependency** points at another module in
the same build, instead of a published library coordinate:

```kotlin
// app/build.gradle.kts
dependencies {
    implementation(project(":core:ui"))
    implementation(project(":feature:landing"))
    implementation(project(":feature:home"))
}
```

`implementation` (as opposed to `api`) means: "I use this dependency myself, but I'm not
promising it to *my* consumers." Concretely, `feature:home` depends on `core:model` as
`implementation` — which is why `app`, even though it depends on `feature:home`, still needed
its **own** `implementation(project(":core:model"))` line. `WinkNavHost` (in `app`) writes
`onQuickLinkClick = { ... }`, and that lambda's parameter type — `QuickLinkItem`, from
`core:model` — has to be visible to the compiler right there in `app`, regardless of what
`feature:home` privately depends on. `implementation` deliberately doesn't leak a module's own
dependencies to whatever depends on *it*; if it did (that's what the `api` configuration is for,
unused in this project so far), every module three layers deep would end up on every consumer's
classpath, defeating the point of splitting things up.

## Every module gets its own `R` class — and that's not automatic to work around

Each module compiles its own resources (`res/values/strings.xml`, etc.) into its own generated
`R` class, scoped to that module's `namespace`. `core:ui`'s `R` only contains what `core:ui`
itself declares (`R.color.brand_purple`, `R.dimen.carousel_dot_spacing`); `feature:landing`'s
`R` only contains what *it* declares (`R.string.landing_title`). This is AGP's **non-transitive R
class** behavior — a module doesn't inherit resource IDs from its dependencies' `R` classes the
way it inherits their public Kotlin classes.

That's why a file whose resources are split across modules needs two `R` imports side by side.
`feature:landing`'s `LandingScreen.kt` needs its own strings/dimens *and* `core:ui`'s brand
colors, so both get imported — the second one aliased, since two classes can't both be called
`R` in the same file:

```kotlin
import com.example.clonedwink.feature.landing.R
import com.example.clonedwink.core.ui.R as CoreUiR

// ...
Text(
    text = stringResource(R.string.landing_title),           // this module's own R
    color = colorResource(CoreUiR.color.brand_purple),        // core:ui's R, aliased
)
```

The same pattern shows up in `HomeScreen.kt`, `HomeComponents.kt`, `HomeSections.kt`, and
`SectionHeader.kt`. Note this only applies to *code* (`R.foo.bar` in Kotlin) — an XML resource
reference like `@color/brand_purple` inside another module's XML file (see `app`'s
`themes.xml`, which points at colors declared in `core:ui`) resolves fine without any import,
because XML resources are looked up by name against the final merged resource table at build
time, not through a per-module generated class.

## Where a resource lives isn't always "wherever it's used"

Splitting `res/` across modules forced a real decision for every color/dimen/string: does it
belong to one feature, or is it a cross-feature design token? The rule this project landed on:

- **`core:ui`** got `colors.xml`/`themes.xml`-adjacent tokens and the *one* dimension both
  carousels share (`carousel_dot_spacing`) — anything that's part of the app's overall design
  system, not any one screen's layout.
- **Each feature module** got everything else — its own strings, its own layout dimensions —
  even resources that *look* generic. `SectionHeader.kt` and `HorizontalCardSection.kt` read
  like reusable components (a title + optional link, a title + scrollable row), but grepping the
  old single-module codebase showed only Home ever called them. They moved into
  `feature:home:ui:components`, not `core:ui` — putting them in the shared module would have
  meant `core:ui` reaching for `R.string.home_see_more`, leaking one feature's resource IDs into
  a module every feature depends on. "Looks generic" isn't the test; "is it actually called from
  more than one feature" is — `CarouselAutoScroll`/`CarouselDotIndicator` passed that test (both
  `feature:landing` and `feature:home` call them), `SectionHeader` didn't.

One XML-only exception: `themes.xml` (the legacy `Theme.Clonedwink` style `AndroidManifest.xml`
points at) had to move to `app`, not `core:ui`, even though it references `core:ui`'s
colors. Verifying a library module's resources in isolation requires resolving every attribute
a style references (`colorPrimary`, `colorSecondary`, ...), and those attrs come from the
`material`/`appcompat` libraries — which `core:ui` (a Compose-only module) doesn't depend on, but
`app` already does. A style that needs the legacy Material/AppCompat attribute set belongs
wherever those libraries are actually on the classpath.

## A Kotlin gotcha this migration surfaced: cross-module smart casts

Moving `CarouselSlide`, `PlaceCard`, and `FeatureCard` into `core:model` broke code that used to
compile fine:

```kotlin
// This no longer compiles once CarouselSlide lives in a different module:
if (slide.imageRes != null) {
    Image(painter = painterResource(id = slide.imageRes), ...)  // error!
}
```

Kotlin's smart-cast (narrowing a nullable type to non-null after a null check) only works on a
`val` **property** when the compiler can see the entire declaring class in the *same*
compilation — it can't guarantee some future recompilation of `core:model` alone won't turn
`imageRes` into a computed property with a custom getter that could return a different value the
second time it's read. A **local variable**, though, has no such caveat — copy the property into
one first, and the smart-cast works exactly as before:

```kotlin
val imageRes = slide.imageRes
if (imageRes != null) {
    Image(painter = painterResource(id = imageRes), ...)  // fine — a local val, not a property
}
```

`LandingScreen.kt`'s `GlassSlideCard`, and `HomeSections.kt`'s `PlaceCardView`/`FeatureCardView`,
all needed this treatment after the split.

## Hilt still has exactly one aggregation point: `app`

Splitting `RepositoryModule` into `feature:landing`'s `LandingModule` and `feature:home`'s
`HomeModule` (see [[12-2026-09-12-hilt-dependency-injection]] for what `@Module`/`@Binds`/
`@InstallIn` do) didn't change where Hilt's code generation actually happens. `@HiltAndroidApp`
on `WinkApplication` still only exists in `app`, and Hilt's aggregating step — which scans every
`@Module`-annotated class reachable from the dependency graph and wires them all into one
`SingletonComponent` — still runs there, once, for the whole app. That's why `app` still applies
the Hilt + KSP plugins and depends on `hilt-android`/`hilt-compiler`, even though it no longer
declares a single `@Binds`/`@Provides` function of its own: Hilt finds `LandingModule` and
`HomeModule` because `app` transitively depends on both feature modules, which is enough for the
aggregating processor to see them, without `app` needing to reference either class directly.

## Key takeaway

A Gradle module boundary is a *compiler-enforced* version of the layer/feature boundaries
`.claude/rules/folder-structure.md` already asked for with packages — "features don't depend on
each other," "shared code lives in the shared layer" stop being conventions you can accidentally
violate and become things that simply won't build if violated. The cost is real, though: every
resource needs a home decided in advance (not "wherever it's used first"), cross-module
references need an aliased second `R` import, and a couple of Kotlin language guarantees (smart
casts) get stricter once a class's declaration and its use sites are compiled separately. None of
that is exotic to this project — it's the same set of trade-offs every multi-module Android app
takes on.

See [[15-2026-09-13-cross-module-dependencies]] for the mechanics behind the `project(":...")`
lines above — `implementation` vs `api`, why a dependency cycle simply won't build, and what to do
when two feature modules seem to need each other.
