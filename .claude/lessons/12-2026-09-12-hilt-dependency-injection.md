# Lesson: Dependency injection with Hilt

[[05-2026-09-08-mvvm-architecture]] explained *why* `LandingViewModel` depends on the
`CarouselRepository` **interface** instead of the concrete `DefaultCarouselRepository` —
dependency inversion, so tests can swap in a fake. This lesson covers the other half of
that story: *who actually constructs* the real `DefaultCarouselRepository` and hands it to
the ViewModel, which is what "dependency injection" (DI) means, and how Hilt now does that
job instead of hand-written code.

## What "dependency injection" actually means

A class being "handed" the things it depends on, rather than constructing them itself.
`LandingViewModel(private val carouselRepository: CarouselRepository)` never writes
`DefaultCarouselRepository(...)` anywhere inside its own body — it just declares "I need a
`CarouselRepository`" as a constructor parameter, and *something else* decides what concrete
object to pass in. That "something else" is the injector. Before this change, the injector
was a few lines of hand-written code; now it's Hilt.

## Before Hilt: manual constructor injection

This project already did dependency injection — just by hand, with no library. Each
Activity built its ViewModel's dependency graph itself:

```kotlin
// LandingActivity.kt, before this lesson's change
private val viewModel: LandingViewModel by viewModels {
    viewModelFactory {
        initializer { LandingViewModel(DefaultCarouselRepository(applicationContext)) }
    }
}
```

This worked fine for one repository and two screens, but doesn't scale:

- Every new screen needing a ViewModel repeats the same `viewModelFactory { initializer
  { ... } }` boilerplate.
- If a dependency needed to be a true singleton shared across the whole app (a database, an
  HTTP client), you'd have to construct it somewhere reachable by every factory and thread
  it through by hand.
- Nothing checks at compile time that every dependency is actually satisfiable — a typo or
  missing wire-up is a runtime crash, not a build error.

Hilt (built on top of Google's Dagger) automates exactly this "who builds what and hands it
to whom" bookkeeping, generating the equivalent factory code at compile time instead of it
being written by hand.

## The four annotations this project now uses, and what each does

### 1. `@HiltAndroidApp` — one per app, on the `Application` class

```kotlin
// WinkApplication.kt
@HiltAndroidApp
class WinkApplication : Application()
```

This is the root of everything. It tells Hilt "generate the app-wide dependency container
here." Every other annotation below only works because this one exists and is wired into
`AndroidManifest.xml` as `android:name=".WinkApplication"` — without that manifest entry,
the system would construct a plain `Application` instead of Hilt's generated subclass, and
every `@AndroidEntryPoint`/`@HiltViewModel` in the app would fail at runtime.

### 2. `@Module` + `@InstallIn` + `@Binds` — where you teach Hilt "interface → implementation"

```kotlin
// di/RepositoryModule.kt
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindCarouselRepository(impl: DefaultCarouselRepository): CarouselRepository

    @Binds
    abstract fun bindHomeRepository(impl: DefaultHomeRepository): HomeRepository
}
```

This is the direct replacement for the one line that used to live inside each Activity's
`initializer { }` block (`DefaultCarouselRepository(applicationContext)`) — except it's
written once for the whole app, in a `di/` package (see
[[10-2026-09-10-project-and-folder-structure]] for where that sits in the project layout),
instead of once per screen. `@InstallIn(SingletonComponent::class)` makes this binding
*visible* app-wide — any `@AndroidEntryPoint`/`@HiltViewModel` class anywhere in the app can
now ask for a `CarouselRepository` and get a `DefaultCarouselRepository` back. (It does
*not*, on its own, guarantee only one instance ever gets built — see the "Component scopes"
section below for that nuance.)

`@Binds` only works because `DefaultCarouselRepository`/`DefaultHomeRepository` know how to
build *themselves* — see the next annotation.

### 3. `@Inject constructor` — how a concrete class tells Hilt how to build it

```kotlin
// DefaultCarouselRepository.kt
class DefaultCarouselRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : CarouselRepository { ... }
```

`@Inject` on a constructor is Hilt/Dagger's simplest building block: "to make one of these,
call this constructor, and here's what it needs." `@param:ApplicationContext` is a
*qualifier* — plain `Context` is ambiguous to Hilt (an Activity context and the one
app-wide context are both typed `Context`), so this tells it specifically to supply the
long-lived application-wide Context, which is safe for a singleton-scoped repository to
hold onto (an Activity context held past that Activity's lifetime is a memory leak; the
application Context lives as long as the process does, so it never has that problem). The
`@param:` prefix (rather than a bare `@ApplicationContext`) pins the annotation to the
constructor parameter specifically, which is what Dagger's code generator inspects — see the
inline comment on that constructor for why Kotlin needed that made explicit.

Because `DefaultCarouselRepository` already says how to build itself, `RepositoryModule`'s
`@Binds` function only needs to say *which* concrete class answers for the interface — it
never constructs anything by hand.

### 4. `@HiltViewModel` + `@AndroidEntryPoint` — wiring a ViewModel into an Activity

```kotlin
// LandingViewModel.kt
@HiltViewModel
class LandingViewModel @Inject constructor(
    private val carouselRepository: CarouselRepository,
) : ViewModel() { ... }
```

```kotlin
// LandingActivity.kt
@AndroidEntryPoint
class LandingActivity : ComponentActivity() {
    private val viewModel: LandingViewModel by viewModels()
    ...
}
```

`@HiltViewModel` + `@Inject constructor` together are what let `by viewModels()` work with
**no factory lambda at all** — Hilt generates the equivalent of the old `viewModelFactory {
initializer { LandingViewModel(DefaultCarouselRepository(applicationContext)) } }` block
automatically. `@AndroidEntryPoint` is the other half: it's what gives `LandingActivity` a
connection into the app-wide dependency container in the first place, so `by viewModels()`
has somewhere to ask for a `LandingViewModel` from. Every Activity/Fragment that wants a
`@HiltViewModel` needs this annotation, or Hilt has no way to reach it.

## In layman's terms: the classic coffee-maker example

Strip away the Android/Kotlin specifics and DI is a simple idea, best seen with the example
Dagger's own docs are famous for: a `CoffeeMaker` that needs a `Heater` and a `Pump`.

**Without DI**, a class builds its own tools:

```kotlin
class CoffeeMaker {
    private val heater = ElectricHeater()          // builds its own heater...
    private val pump = Thermosiphon(ElectricHeater())  // ...and its pump needs one too
    fun brew() { ... }
}
```

This has two problems that get worse as an app grows: `CoffeeMaker` is now permanently
glued to `ElectricHeater` specifically (swapping in a nicer `InductionHeater` later, or a
`FakeHeater` for a test, means editing `CoffeeMaker`'s own code), and two separate
`ElectricHeater` objects get built when really only one physical heater exists.

**With DI**, a class just states what it needs and lets someone else hand it over:

```kotlin
class CoffeeMaker @Inject constructor(
    private val heater: Heater,   // an interface — CoffeeMaker doesn't know or care which
    private val pump: Pump,       // concrete class it gets
) {
    fun brew() { ... }
}
```

Somewhere else (a `@Module`, exactly like `RepositoryModule`), you say once: "when
something needs a `Heater`, give it an `ElectricHeater`." Hilt reads every `@Inject`
constructor and every `@Module` binding in the whole app at build time, and generates code
equivalent to this — wiring the whole tree, sharing the one heater everywhere it's needed:

```kotlin
val heater = ElectricHeater()
val pump = Thermosiphon(heater)
val coffeeMaker = CoffeeMaker(heater, pump)
```

That's *exactly* what happens with `LandingViewModel`: it states "I need a
`CarouselRepository`"; Hilt looks up `RepositoryModule`'s `@Binds` and sees
`DefaultCarouselRepository` answers for it; Hilt sees `DefaultCarouselRepository` in turn
needs a `Context` and already knows how to supply the app-wide one; and it generates the
constructor-calling code for the whole chain, all before the app ever runs.

**One-sentence version**: dependency injection means a class says *what* it needs instead of
building it itself, and something else (here, Hilt) plugs in the real thing.

## The full object graph, end to end

```
WinkApplication (@HiltAndroidApp)
    │  creates the app-wide SingletonComponent container
    ▼
RepositoryModule (@Module, @InstallIn(SingletonComponent::class))
    │  "CarouselRepository → DefaultCarouselRepository" binding registered
    ▼
LandingActivity (@AndroidEntryPoint)
    │  by viewModels() asks the container for a LandingViewModel
    ▼
LandingViewModel (@HiltViewModel, @Inject constructor)
    │  Hilt sees it needs a CarouselRepository, looks up the binding above
    ▼
DefaultCarouselRepository (@Inject constructor)
    │  Hilt sees it needs an @ApplicationContext Context, which Hilt provides
    │  out of the box (no @Binds needed — Hilt ships this binding for free)
    ▼
LandingViewModel is constructed with a real DefaultCarouselRepository and handed to
LandingActivity's `viewModel` property
```

`HomeViewModel`/`HomeActivity`/`DefaultHomeRepository` follow the exact same shape, one
level down (`HomeRepository` → `DefaultHomeRepository`, in the same `RepositoryModule`).

## Component scopes: `SingletonComponent` is not the only one

`SingletonComponent` is the outermost, longest-lived container — tied to `WinkApplication`,
it lives exactly as long as the app process does. But that's one level of a whole hierarchy
Hilt predefines, each level mirroring a specific Android lifecycle and nested inside the
one above it:

```
SingletonComponent            → whole app process (WinkApplication)
 ├─ ActivityRetainedComponent → survives configuration changes (same instance across rotation)
 │   ├─ ViewModelComponent    → one per ViewModel instance
 │   └─ ActivityComponent     → one per Activity instance (a *new* one after rotation)
 │       └─ FragmentComponent → one per Fragment instance
 │           └─ ViewComponent → one per View instance
 └─ ServiceComponent          → one per Service instance
```

A binding installed with `@InstallIn(XComponent::class)` is only reachable from code running
inside that component or something nested under it — an `@InstallIn(ActivityComponent::class)`
binding could never be injected into `WinkApplication`, since Application sits above/outside
any single Activity's lifetime. This project only ever installs into `SingletonComponent`
because both repositories are simple, stateless, and reasonable to treat as app-wide; a
future per-screen dependency (say, something that should reset every time a screen is
reopened) would use one of the narrower components instead.

Each component has a matching **scope annotation** that, added to a binding, makes Hilt
*cache and reuse one instance* for that container's lifetime instead of building a fresh one
on every injection:

| Component | Scope annotation | Lifetime |
|---|---|---|
| `SingletonComponent` | `@Singleton` | whole app process |
| `ActivityRetainedComponent` | `@ActivityRetainedScoped` | survives rotation; dies when the Activity finishes for good |
| `ViewModelComponent` | `@ViewModelScoped` | one ViewModel instance |
| `ActivityComponent` | `@ActivityScoped` | one Activity instance |
| `FragmentComponent` | `@FragmentScoped` | one Fragment instance |
| `ServiceComponent` | `@ServiceScoped` | one Service instance |

**A subtlety in this project's own code worth internalizing**: `RepositoryModule`'s two
`@Binds` functions are installed in `SingletonComponent`, but *aren't* annotated
`@Singleton`. `@InstallIn` alone only controls *visibility* (who can ask for this binding);
it does not by itself guarantee only one instance ever gets built. Without `@Singleton`,
Hilt actually constructs a fresh `DefaultCarouselRepository` every time something injects a
`CarouselRepository` — harmless today, since only `LandingViewModel` ever does that once,
so it behaves identically either way. If `DefaultCarouselRepository` held real state that
needed sharing (an in-memory cache, an open connection), you'd add `@Singleton` to force
one shared instance:

```kotlin
@Binds
@Singleton
abstract fun bindCarouselRepository(impl: DefaultCarouselRepository): CarouselRepository
```

## `RepositoryModule` isn't "the container" — it's one contributor to it

It's tempting to read `RepositoryModule` as *the* Hilt container, the way you might think of
`Program.cs` as *the* place a C# app's DI is configured. It's closer to being **one
`services.AddXxx(...)` call**, not the whole `Program.cs`. The actual container (per
component — see above) is generated by Hilt from **every** `@Module` in the app that targets
it, merged together. You're meant to have more than one, split by concern, exactly the way
you'd split `Program.cs` registrations into `services.AddNetworkServices()`,
`services.AddDatabaseServices()`, etc. extension methods instead of one giant method.

A likely next module for this project, once a real network layer exists (see this lesson's
own change log for that exact follow-up item):

```kotlin
// di/NetworkModule.kt
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // Kotlin/Android: `@Provides` (not `@Binds`) is for when Hilt can't just call a
    // constructor — building a Retrofit instance means calling `Retrofit.Builder()...build()`,
    // not `Retrofit(...)`, and Hilt has no way to know that on its own. `@Binds` only works
    // for "this interface's implementation is this one class with an @Inject constructor";
    // `@Provides` is the escape hatch for anything built through a factory/builder, or for a
    // type you don't own (you can't add `@Inject` to Retrofit's own constructor).
    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit = Retrofit.Builder()
        .baseUrl("https://api.example.com/")
        .build()
}
```

Both `RepositoryModule` and `NetworkModule` install into the same `SingletonComponent` —
Hilt merges their bindings into one graph, so a `CarouselRepository` implementation could
now also have its own `@Inject constructor` parameter typed `Retrofit`, and Hilt would wire
it through from `NetworkModule` without either module needing to know the other exists.
Nothing stops a module from targeting a *different*, narrower component either (e.g. an
`@InstallIn(ActivityComponent::class)` module for something that should only exist while one
specific Activity is alive) — modules and components are independent axes: how many modules
you have is just organization; which component each one installs into is what actually
controls lifetime/visibility.

## Mapping this onto C# dependency injection

If you've used the built-in container in ASP.NET Core / the .NET generic host
(`Microsoft.Extensions.DependencyInjection` — `IServiceCollection`/`IServiceProvider`), the
concepts above translate almost one-to-one:

| Hilt (this project) | C# (`Microsoft.Extensions.DependencyInjection`) | Concept |
|---|---|---|
| `@Inject constructor(...)` on `DefaultCarouselRepository` | *(nothing — C# constructor injection needs no attribute; the container matches parameter types automatically)* | "here's what I need, hand it to me" |
| `@Module` + `@Binds` in `RepositoryModule` | `services.AddXxx<CarouselRepository, DefaultCarouselRepository>();` in `Program.cs` | register "interface → implementation" |
| `@Module` + `@Provides` (e.g. a hypothetical `NetworkModule`) | `services.AddXxx<Retrofit>(sp => Retrofit.Builder()....build());` | register something built via a factory/builder, not a bare constructor |
| Multiple `@Module` classes installed in the same component | Multiple `services.AddXxx(...)` calls (or grouped into extension methods) in the same `Program.cs` | registrations from different files/concerns, merged into one container |
| `@InstallIn(SingletonComponent::class)`, no `@Singleton` | `services.AddTransient<T, Impl>()` | a new instance every time it's resolved |
| `@Binds` + `@Singleton` | `services.AddSingleton<T, Impl>()` | one instance, whole app lifetime |
| `@ActivityRetainedScoped` / `@ViewModelScoped` | `services.AddScoped<T, Impl>()` | one instance per logical "unit of work" (an HTTP request in ASP.NET; an Activity-retained/ViewModel lifetime here) |
| `WinkApplication`'s Hilt-generated `SingletonComponent` | the root `IServiceProvider` built from `builder.Services` in `Program.cs` | the container everything else resolves from |
| `@HiltViewModel` + `@AndroidEntryPoint` | a `Controller`/minimal-API handler with constructor parameters — the framework activates it via DI, you never `new` it yourself | "framework-owned class, DI-supplied dependencies" |

**The one real conceptual difference**: .NET's container resolves everything **at runtime**
via reflection — forget to register a service, and you only find out when that code path
actually executes (a runtime exception the compiler can't catch). Hilt/Dagger is
**compile-time**: the KSP annotation processor (`app/build.gradle.kts`) reads every
`@Inject`/`@Binds`/`@Module` at build time and *generates real Kotlin/Java source code* that
does the wiring — so a missing binding (say, if `RepositoryModule` forgot the
`HomeRepository` mapping) is a **build failure**, not a crash that only shows up once
`HomeActivity` happens to launch. That compile-time graph validation is the main thing Hilt
buys over a plain runtime container like `Microsoft.Extensions.DependencyInjection`.

## What did *not* change

- `LandingViewModelTest`/any future `HomeViewModelTest` still construct their ViewModel
  directly — `LandingViewModel(FakeCarouselRepository(slides))` — with no Hilt involved.
  Hilt only matters for how the *real app* wires things together; a JVM unit test is free to
  bypass the whole dependency graph and pass in a hand-written fake, exactly as before.
- `CarouselRepository`/`HomeRepository` (the interfaces), and every `ui`/`viewmodel`
  package's *file location*, are unchanged — Hilt only replaced *how a concrete instance
  gets constructed and handed over*, not the MVVM layering itself. `.claude/rules/
  folder-structure.md`'s "View → ViewModel → Model" rule, and the ban on ViewModels
  importing `android.view.*`/holding an `Activity`, still hold exactly as before.

## A build quirk worth knowing about: KSP vs. AGP 9's "built-in Kotlin"

Hilt's code generation runs through KSP (Kotlin Symbol Processing) — see the `ksp` plugin
applied in `app/build.gradle.kts` and `ksp(libs.hilt.compiler)` in its dependencies. This
project's AGP version (9.3.2) ships a newer "built-in Kotlin" compilation mode (see
[[10-2026-09-10-project-and-folder-structure]] for the Gradle/Kotlin-plugin split this
relates to) that, as of the KSP version pinned in `gradle/libs.versions.toml`, rejects the
older API KSP still uses to register its generated-sources folder
(`google/ksp#2729` — a real, currently-open upstream bug, not a mistake in this project).
The fix applied here is a documented flag in `gradle.properties`:
`android.disallowKotlinSourceSets=false`, which opts back into the older behavior KSP needs.
It's a narrow, well-understood workaround (not a design choice) — worth removing the next
time the `ksp` version catalog entry is bumped and this stops being necessary.

## Key takeaway

Dependency injection isn't a Hilt-specific concept — this project was already doing it
manually (constructor injection + a hand-rolled factory). Hilt's four annotations
(`@HiltAndroidApp`, `@Module`/`@InstallIn`/`@Binds`, `@Inject constructor`, `@HiltViewModel`/
`@AndroidEntryPoint`) are a compile-time code generator for exactly that same pattern, so it
doesn't have to be repeated by hand for every new screen and every new dependency. Nothing
about *how* the ViewModel/Repository/Interface layers relate to each other changed — only
*who writes the code that connects them* did.
