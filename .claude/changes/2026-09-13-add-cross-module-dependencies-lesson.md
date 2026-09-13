# Add cross-module dependencies lesson

## What

Added `.claude/lessons/15-2026-09-13-cross-module-dependencies.md`, covering the mechanics of how
Gradle modules reference each other: `project(":...")` dependency declarations, `implementation`
vs `api` (using the real `feature:home` → `core:model` → `app` case from the multi-module
migration as the worked example), why Gradle's task graph can't contain a cycle, and the pattern
this app already uses to let two features cooperate without depending on each other (`WinkNavHost`
supplying `LandingScreen`'s `onGetStartedClick` callback rather than `feature:landing` importing
`feature:home`).

To confirm the circular-dependency failure mode was described accurately, temporarily added
`implementation(project(":feature:landing"))` to `feature:home/build.gradle.kts` and the reverse
to `feature:landing/build.gradle.kts`, ran `gradlew.bat :feature:home:assembleDebug` to capture
Gradle's actual "Circular dependency between the following tasks" error, then reverted both files
back to their original content (verified via `grep` afterward — both files are untracked/new from
the multi-module migration, so `git checkout` wasn't an option).

Added a one-line cross-reference from `.claude/lessons/14-2026-09-13-multi-module-architecture.md`'s
"Key takeaway" pointing at the new lesson.

## Why

The user asked for a lesson specifically about what happens when modules reference each other,
and lesson 14 only touched on this briefly (the "features never depend on each other" rule and a
short `project(":...")` example) rather than explaining the mechanism and the cycle-detection
failure mode in depth.

## Follow-up / known limitations

None — this is a documentation-only change; no source or build files differ from before this
change (the temporary circular-dependency edit was reverted).
