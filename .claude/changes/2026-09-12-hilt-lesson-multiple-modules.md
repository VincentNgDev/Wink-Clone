# Expand the Hilt lesson: multiple modules and @Provides vs @Binds

## What

- `.claude/lessons/12-2026-09-12-hilt-dependency-injection.md`: new "`RepositoryModule`
  isn't 'the container' — it's one contributor to it" section. Clarifies that a `@Module` is
  closer to one `services.AddXxx(...)` registration than to the whole DI container/`Program.cs`;
  that Hilt merges every `@Module` targeting the same component into one graph; and
  introduces `@Provides` (via a hypothetical future `di/NetworkModule.kt` with a
  Retrofit-building example) alongside `@Binds`, explaining when each applies. Also added a
  matching row to the existing C#-mapping table for `@Provides` and for "multiple modules,
  one container."

## Why

Requested: clarify whether `RepositoryModule` acts like a DI container/`Program.cs`, and
confirm/explain that additional `@Module`s with their own `@InstallIn` are supported and how
that composes.

## Follow-up / known limitations

- Documentation-only change. The `NetworkModule`/`Retrofit` example is illustrative only —
  no such module exists in the codebase yet; if a real network layer is added later, prefer
  updating this lesson to cite the real file over leaving the hypothetical example stale.
