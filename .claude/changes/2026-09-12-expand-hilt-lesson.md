# Expand the Hilt lesson: layman example, component scopes, C# mapping

## What

- `.claude/lessons/12-2026-09-12-hilt-dependency-injection.md`:
  - Corrected a slightly-too-strong claim in the `@Module`/`@Binds` section — `@InstallIn`
    alone controls binding *visibility*, not instance caching; that nuance is now explained
    properly in the new scopes section instead.
  - New "In layman's terms" section: the classic Dagger `CoffeeMaker`/`Heater`/`Pump`
    example, building up from "no DI" to "DI", then mapping it back onto this project's real
    `LandingViewModel`/`CarouselRepository` chain.
  - New "Component scopes" section: the full Hilt component hierarchy
    (`SingletonComponent` → `ActivityRetainedComponent`/`ServiceComponent` →
    `ViewModelComponent`/`ActivityComponent` → `FragmentComponent` → `ViewComponent`) and
    their matching scope annotations, plus a callout that this project's own
    `RepositoryModule` bindings are installed in `SingletonComponent` but aren't annotated
    `@Singleton`, so they're not actually guaranteed single-instance today.
  - New "Mapping this onto C# dependency injection" section: a table pairing Hilt's
    `@Inject`/`@Module`+`@Binds`/scope annotations/`@HiltViewModel`+`@AndroidEntryPoint`
    against `Microsoft.Extensions.DependencyInjection`'s constructor injection/
    `AddTransient`/`AddSingleton`/`AddScoped`/framework-activated controllers, plus the
    compile-time-vs-runtime distinction between the two.

## Why

Requested: a deeper explanation of Hilt, specifically a beginner-friendly analogy/example,
clarification of what `SingletonComponent` actually guarantees versus Hilt's other scopes
(prompted by a direct question about whether other component types exist), and a mapping to
C# DI for someone with that background.

## Follow-up / known limitations

- Documentation-only change — no code touched, so no build/test/lint run was needed.
- If a future feature actually needs `@Singleton`/`@ActivityRetainedScoped`/etc. in this
  project's own code (not just as a lesson example), update the lesson's "subtlety" callout
  to point at that real usage instead of only the hypothetical one.
