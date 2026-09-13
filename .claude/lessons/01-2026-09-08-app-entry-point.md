# Lesson: What is the app's "main entry point"?

## Update (2026-09-13): the launcher Activity is now `MainActivity`, and there's an `Application` class

Since `.claude/changes/2026-09-13-migrate-to-navigation-compose.md`, the launcher
`<activity>` below is `.ui.MainActivity`, not `.ui.landing.LandingActivity` —
`LandingActivity` was deleted along with the two-Activity setup (see
[[13-2026-09-13-navigation-compose-vs-multi-activity]]). `MainActivity.onCreate()` calls
`setContent { WinkNavHost() }`, and *that* NavHost — not the manifest — decides which
screen (Landing, then Home) shows first at runtime; the manifest only decides which
single Activity the OS launches.

The "Application class (not used here)" section below is also out of date: since
`.claude/changes/2026-09-12-add-hilt-dependency-injection.md`, this project *does*
declare one — `WinkApplication`, registered via `android:name=".WinkApplication"` —
so Hilt's dependency graph is set up before `MainActivity` (or anything else) runs. See
[[02-2026-09-08-android-manifest]] and [[12-2026-09-12-hilt-dependency-injection]].

The rest of this lesson's *mechanics* (why there's no `main()`, how the OS reads the
manifest, the lifecycle handoff) are all still accurate — only the specific class names
below (`LandingActivity`) are stale.

## The short answer

There is **no `main()` function** in an Android app the way there is in a JVM/CLI
program. The entry point is declared *declaratively*, in XML, in
`app/src/main/AndroidManifest.xml`:

```xml
<activity
    android:name=".ui.landing.LandingActivity"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

- `android:name=".ui.landing.LandingActivity"` — the class Android should instantiate.
  The leading `.` is shorthand for the app's `namespace` (`com.example.clonedwink`, set
  in `app/build.gradle.kts`), so this really means
  `com.example.clonedwink.ui.landing.LandingActivity`.
- The `<intent-filter>` with `action MAIN` + `category LAUNCHER` is what makes this
  specific Activity show up as the tappable icon on the home screen/app drawer, and what
  the OS launches first when the user taps it. An app can declare many `<activity>`
  entries, but only one is normally marked as the launcher this way.
- `android:exported="true"` means other apps' components are allowed to start this
  Activity (required for anything with a `LAUNCHER` intent filter, otherwise the OS
  itself couldn't launch it either).

There's currently only one `<activity>` in the manifest, so `LandingActivity` is both the
entry point and the only screen in the app. See
[[02-2026-09-08-android-manifest]] for a full breakdown of what this file is and
everything else it declares.

## What actually happens when the user taps the app icon

1. The Android OS reads the manifest (already parsed/indexed at install time) and sees
   `LandingActivity` is the `MAIN`/`LAUNCHER` activity.
2. If the app's process isn't already running, the OS starts a new process for it and
   runs Android framework startup code (this is roughly Android's "hidden `main()`" —
   it lives inside the framework, not in our code).
3. The framework constructs an instance of `LandingActivity` (a no-arg constructor —
   we never write one ourselves; Android calls the default one).
4. The framework calls lifecycle callbacks on that instance in order: `onCreate()` →
   `onStart()` → `onResume()`. See
   [[03-2026-09-08-android-lifecycle]] for what each of those does. `onCreate()` is where
   our code (`LandingActivity.onCreate` in `ui/landing/LandingActivity.kt`) sets the
   content view and wires everything up.
5. Once `onResume()` returns, the Activity is fully visible and interactive.

## The "Application" class (not used here, but worth knowing)

Android also supports a project-wide `Application` subclass (`android:name` on the
`<application>` tag in the manifest) whose `onCreate()` runs once per process, *before*
any Activity — used for app-wide setup like DI containers or crash reporting init. This
project doesn't declare one (the `<application>` tag has no `android:name`), so Android
just uses the default `android.app.Application` and does nothing extra before
`LandingActivity` starts.

## Where this project's app-wide config lives

- **Application ID / package namespace**: `com.example.clonedwink` — set once in
  `app/build.gradle.kts` (`namespace = "com.example.clonedwink"` and
  `applicationId = "com.example.clonedwink"`), not scattered through source files.
- **App icon/name/theme**: on the `<application>` tag in the manifest
  (`android:icon`, `android:label="@string/app_name"`, `android:theme="@style/Theme.Clonedwink"`)
  — see [[04-2026-09-08-drawing-the-ui]] for how themes/resources work.
- **Backup behavior**: `android:allowBackup`, `android:dataExtractionRules`,
  `android:fullBackupContent` — scaffold defaults, not touched yet.

## Key takeaway

"Where does the app start?" in Android is really two questions:
1. *Which screen appears first?* → answered by the manifest's `LAUNCHER` intent filter.
2. *What code runs first on that screen?* → `LandingActivity.onCreate()`.

There's no single line of code you can point to and say "this is where the whole app
begins" — it's a framework-driven handoff from manifest → Activity lifecycle callback.
