# Add navigation-compose vs. multi-Activity lesson; fix broken WinkNavHost.kt

## What

- New `.claude/lessons/13-2026-09-13-navigation-compose-vs-multi-activity.md`: dedicated
  lesson answering whether `androidx.navigation:navigation-compose` is a better navigation
  model than the old multiple-Activities-in-the-manifest approach. Covers a cost comparison
  table, installation (version catalog + module dependency, including the
  `hilt-lifecycle-viewmodel-compose` vs. deprecated `hilt-navigation-compose` gotcha),
  implementation walkthrough of this repo's real `WinkDestination.kt`/`WinkNavHost.kt`/
  `MainActivity.kt`, caveats/limitations (string route type safety, argument passing,
  deep links, testing shape, process death/saved state), and when a dedicated Activity is
  still the right call (widgets, share targets, `launchMode`/process isolation,
  Activity-expecting third-party SDKs).
- Fixed `app/src/main/java/com/example/clonedwink/ui/navigation/WinkNavHost.kt`: restored
  the `WinkNavHost` function's opening `{` after its parameter list, which had been
  accidentally deleted (found as an already-uncommitted working-tree change) — the file
  had no way to compile without it (a block function body can't exist without braces).

## Why

Requested: a lesson on whether `navigation-compose` is the better solution vs. multiple
manifest Activities, covering installation, implementation, and caveats/limitations. The
`WinkNavHost.kt` brace was fixed in the same turn since it was a build-breaking syntax
error sitting in the working tree, discovered while reading the file this lesson
references.

## Follow-up / known limitations

- Documentation-only for the lesson; the `WinkNavHost.kt` fix is the only functional
  change, restoring it to match the already-committed version at `47ad598`.
- Verified with `gradlew.bat compileDebugKotlin` — compiles clean.
