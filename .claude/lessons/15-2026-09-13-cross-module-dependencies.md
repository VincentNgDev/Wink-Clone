# Lesson: How modules reference each other — and what to do when two of them seem to need each other

[[14-2026-09-13-multi-module-architecture]] introduced the module graph (`app` → `feature:*` →
`core:*`) and stated two rules: features never depend on each other, and `app` is thin. This
lesson is about the mechanics behind those rules — exactly how one module reaches into another's
code, why Gradle can't have a dependency cycle even if you wanted one, and what to actually do
when two features seem to need each other (which happens the moment two screens need to navigate
between one another, like this app's Landing → Home flow).

## Declaring "I use this other module": `project(":...")`

Every module's `dependencies { }` block can reference another module in the same build with the
`project(":path")` dependency type, instead of a Maven coordinate like `libs.foo`:

```kotlin
// feature/home/build.gradle.kts
dependencies {
    implementation(project(":core:model"))   // HomeContent, QuickLinkItem, ...
    implementation(project(":core:ui"))      // ClonedWinkTheme, CarouselAutoScroll, ...
}
```

The string is the same colon-separated path used in `settings.gradle.kts`'s `include(...)` calls
— `:core:model` maps to the `core/model/` folder. Once declared, every public (non-`private`)
class, function, and top-level declaration in `core:model` becomes importable from
`feature:home`'s Kotlin source, exactly like importing a class from `androidx.core:core-ktx`.
What's *not* automatic: `core:model`'s resources — see [[14-2026-09-13-multi-module-architecture]]'s
section on non-transitive `R` classes for why a resource still needs its own import.

## `implementation` vs `api`: does the dependency leak to *your* consumers?

Every module dependency line picks one of two configurations, and the choice controls whether a
module three levels away can see it:

- **`implementation`** — "I use this, but it's my own private implementation detail." Modules that
  depend on *you* do not get it on their classpath automatically.
- **`api`** — "I use this, and I'm re-exposing it as part of my own public surface." Modules that
  depend on you get this dependency transitively too, as if they'd declared it themselves.

This project already has a real example of what happens when a module's public API leaks a type
from an `implementation`-only dependency. `feature:home`'s `HomeScreen` composable takes an
`onQuickLinkClick: (QuickLinkItem) -> Unit` parameter — `QuickLinkItem` is a `core:model` class —
but `feature:home/build.gradle.kts` declares `core:model` as `implementation`, not `api`. `app`
calls `HomeScreen` from `WinkNavHost`, so the compiler needs `QuickLinkItem` visible *in `app`*
too — and because `implementation` doesn't leak, `app/build.gradle.kts` needed its own explicit
`implementation(project(":core:model"))` line (see the comment there) even though `app` never
constructs a `QuickLinkItem` itself. Had `feature:home` instead declared
`api(project(":core:model"))`, that type would have ridden along automatically and `app`'s extra
line wouldn't have been necessary.

The reason this project deliberately uses `implementation` everywhere anyway (accepting the extra
line in `app`) rather than switching to `api`: `api` dependencies re-compile every downstream
module whenever the exposed module changes, and they make it easy to lose track of which module
*actually* uses what. `implementation` is the conservative default recommended for exactly this
size of app — reach for `api` only when a dependency's types genuinely need to appear in your own
public function signatures across many call sites, not as a shortcut to avoid one extra line.

## Gradle project dependencies form a DAG — cycles are not just discouraged, they don't build

"Features never depend on each other" isn't only a style rule — it's load-bearing, because Gradle
literally cannot build a module graph with a cycle in it. To see exactly what happens, this
lesson's write-up included actually adding a cycle to this project temporarily:

```kotlin
// feature/home/build.gradle.kts — temporary, for this experiment only
implementation(project(":feature:landing"))

// feature/landing/build.gradle.kts — temporary, for this experiment only
implementation(project(":feature:home"))
```

Resolving dependency *metadata* alone (`gradlew.bat :feature:home:dependencies`) actually
succeeds — Gradle can still figure out which artifacts exist. The failure shows up the moment you
ask Gradle to *build* something, because compiling a module is itself a chain of tasks
(`compileDebugKotlin`, `kspDebugKotlin`, the AAR-bundling tasks, ...), and those per-module task
chains now depend on each other in both directions:

```
$ gradlew.bat :feature:home:assembleDebug
Circular dependency between the following tasks:
:feature:home:bundleLibCompileToJarDebug
\--- :feature:home:transformDebugClassesWithAsm
     +--- :feature:home:compileDebugJavaWithJavac
     |    +--- :feature:home:compileDebugKotlin
     |    |    +--- :feature:home:kspDebugKotlin
     |    |    |    \--- :feature:landing:bundleLibCompileToJarDebug
     |    |    |         \--- :feature:landing:transformDebugClassesWithAsm
     |    |    |              +--- :feature:home:bundleLibCompileToJarDebug (*)
     |    |    |              ...
BUILD FAILED
```

Gradle needs a finished, ordered list of tasks to run before it executes any of them (its **task
execution graph**), and a graph with a cycle has no valid ordering — there's no way to answer
"which one compiles first?" when each needs the other's output. This is a general property of
Gradle, not something specific to Android modules: a **DAG** (Directed Acyclic Graph — a graph
where you can always follow arrows forward and never loop back to where you started) is the only
shape Gradle's task scheduler can execute. Two modules pointing `implementation(project(...))` at
each other simply isn't a legal module graph, full stop — this is Gradle physically refusing to
build, not a lint warning.

## So what do you do when two features genuinely need to talk to each other?

This is the real question behind "how if some modules refer to each other" — and this app already
answers it in practice. `feature:landing`'s `LandingScreen` has a "Get started" button that needs
to take the user to `feature:home`'s screen. If `feature:landing` imported `HomeScreen` directly,
that would be exactly the illegal cycle above waiting to happen the day `feature:home` needed
anything back from `feature:landing`. Instead, neither feature module knows the other exists:

```kotlin
// feature/landing's LandingScreen.kt — no import of anything from feature:home
@Composable
fun LandingScreen(
    uiState: LandingUiState,
    onGetStartedClick: () -> Unit,   // just a callback — Landing doesn't know what this does
    onErrorShown: () -> Unit,
)
```

```kotlin
// app's WinkNavHost.kt — depends on BOTH features, so it's the only place allowed to
// know that "Get started" in Landing should mean "go to Home"
composable(WinkDestination.Landing.route) {
    LandingScreen(
        uiState = uiState,
        onGetStartedClick = {
            navController.navigate(WinkDestination.Home.route) {
                popUpTo(WinkDestination.Landing.route) { inclusive = true }
            }
        },
        onErrorShown = { /* ... */ },
    )
}
```

`app` is the one module that's allowed to depend on every feature (see
[[14-2026-09-13-multi-module-architecture]]'s "`app` is thin" rule), so it's the only place in the
whole project with enough visibility to wire two features together — LandingScreen exposes a
generic `() -> Unit` lambda instead of a hard "navigate to Home" call, and `WinkNavHost` decides
what that lambda actually does. This is the **inversion of control** pattern applied at the module
level: instead of the lower-level piece (a feature) reaching *up* or *sideways* to call something
it doesn't own, it exposes a hook, and the piece that already depends on both sides (`app`) fills
that hook in. The general recipe when two modules seem to need each other:

1. **Ask whether the shared thing is actually data or UI, not behavior.** If both features need
   the same *type* or *composable* (not a call into each other's logic), that's a sign it belongs
   one layer down, in a `core:*` module both can depend on — exactly how `CarouselSlide` ended up
   in `core:model` and `CarouselAutoScroll` in `core:ui` rather than duplicated, or one feature
   depending on the other, per [[14-2026-09-13-multi-module-architecture]].
2. **If it's genuinely "screen A needs to trigger screen B,"** expose a callback/lambda parameter
   (or, for a route by name rather than a function reference, a plain `String`/sealed-class value)
   from the module that needs to trigger the transition, and let a module that already depends on
   both sides — here, always `app`, since it owns `WinkNavHost` — supply what that callback does.
   Neither feature ever imports the other.
3. **Never add a project dependency just to "borrow one function."** If reaching for
   `implementation(project(":feature:other"))` feels like the only option, that function almost
   always belongs in a `core:*` module instead, or the two features shouldn't be calling each
   other directly at all.

## Key takeaway

A `project(":...")` dependency line is really a promise that the module graph stays a DAG —
Gradle enforces that promise at build time by refusing to schedule a task graph with a cycle in
it, the same way the Kotlin compiler enforces "no unresolved import." `implementation` vs `api`
controls how far a dependency's visibility travels once that promise holds. And "two features need
each other" is never actually true in a well-layered app — it's either "they need the same shared
thing" (push it down into `core:*`) or "one needs to trigger the other" (push the decision up into
`app`, the one module allowed to see both), which is exactly what `WinkNavHost` already does for
Landing → Home.
