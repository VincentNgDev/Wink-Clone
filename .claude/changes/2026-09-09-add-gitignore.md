# Add .gitignore

## What
- Added a standard Android Studio/Gradle `.gitignore` at the repo root, ignoring
  `.gradle/`, `/build`, `/local.properties`, `.idea` caches/workspace files, `.kotlin/`,
  native build output (`.cxx`, `.externalNativeBuild`), logs/profiler files, and keystores.
- Ran `git rm -r --cached .gradle build .idea local.properties` to unstage the build/IDE
  artifacts that had already been added to the index before the `.gitignore` existed.
  This only removes them from git tracking — the files themselves are untouched on disk.

## Why
The project had no `.gitignore`, so `git add` had staged ~130 generated files (Gradle
caches, build outputs, `local.properties` with machine-specific SDK paths, IDE workspace
state). None of these belong in version control — they're regenerated per machine/build
and would cause noisy diffs and merge conflicts.

## Follow-up / known limitations
- `.idea/` still has its own nested `.gitignore` (Android Studio-generated) which allows a
  handful of shared config files (e.g. `.idea/.gitignore` itself) — those may still show as
  untracked and can be added deliberately if the team wants to share IDE run configs.
- No files were deleted from disk; only unstaged. The user should verify `git status`
  looks right before committing.
