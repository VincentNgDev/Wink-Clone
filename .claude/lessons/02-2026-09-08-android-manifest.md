# Lesson: What is `AndroidManifest.xml`?

`app/src/main/AndroidManifest.xml` is your app's **table of contents and permission
slip**, read by the Android OS *before* any of your Kotlin code runs. The OS doesn't
scan your compiled code to figure out what your app can do or which screens it has — it
reads this one XML file. If something isn't declared here, the OS behaves as if it
doesn't exist, even if the Kotlin class compiles fine (a classic beginner crash is
"Activity not found" from forgetting to add a new screen here).

This project's manifest, in full:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <application
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.Clonedwink">

        <activity
            android:name=".ui.landing.LandingActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

    </application>

</manifest>
```

## `<manifest>` — the root tag

Just a wrapper. It's implicitly scoped to this app's namespace/`applicationId`
(`com.example.clonedwink`, set once in `app/build.gradle.kts`), which is why every
component name inside can be written relative to it — `android:name=".ui.landing.LandingActivity"`
starts with a `.` instead of spelling out `com.example.clonedwink.ui.landing.LandingActivity`
in full.

`xmlns:android` and `xmlns:tools` are XML namespace declarations — they're what make
`android:...` and `tools:...` attribute prefixes resolvable throughout this file and
every layout XML in the project (see
[[04-2026-09-08-drawing-the-ui]]). `tools:` attributes (used elsewhere, e.g.
`tools:ignore`, `tools:text` in the layouts) are stripped out at build time — they only
affect the Android Studio preview/lint, never the shipped app.

## `<application>` — app-wide settings

Everything here applies regardless of which screen is currently open:

| Attribute | What it controls |
|---|---|
| `android:icon` / `android:roundIcon` | The launcher icon shown on the home screen/app drawer (`@mipmap/ic_launcher`, referencing `res/mipmap-*/` — Android picks the right resolution for the device automatically). |
| `android:label` | The app's display name, `@string/app_name` (from `res/values/strings.xml`) — shown under the icon and in the recent-apps switcher. |
| `android:theme` | The default visual theme (`@style/Theme.Clonedwink`, from `res/values/themes.xml`) every Activity inherits unless it overrides its own. See [[04-2026-09-08-drawing-the-ui]] for how themes/colors/dimens work. |
| `android:supportsRtl` | Opts into right-to-left layout mirroring for RTL languages (Arabic, Hebrew, etc.) — combined with using `start`/`end` instead of `left`/`right` in layout attributes (which this project's XML already does, e.g. `layout_marginStart`). |
| `android:allowBackup` | Whether Android's system backup can include this app's data at all. |
| `android:fullBackupContent` / `android:dataExtractionRules` | Point at `res/xml/backup_rules.xml` / `res/xml/data_extraction_rules.xml`, which give finer-grained control over *what* gets backed up/extracted (both currently near-empty scaffold defaults in this project). |

There is **no `android:name` attribute** on this `<application>` tag, which matters: see
"The `Application` class" below.

## `<activity>` — declaring a screen exists

```xml
<activity
    android:name=".ui.landing.LandingActivity"
    android:exported="true">
```

This is the **required registration** for every Activity in the app — see
[[01-2026-09-08-app-entry-point]] and [[03-2026-09-08-android-lifecycle]] for what an
Activity actually is and does. Two things this tag says:

- `android:name` — which Activity class this entry is for.
- `android:exported="true"` — whether components *outside this app* (other apps, or the
  OS launcher itself) are allowed to start it. Recent Android versions (targeting API 31+,
  which this project does via `targetSdk = 37`) actually **require** you to explicitly
  set `android:exported` on any activity with an `<intent-filter>` — leaving it out is a
  build-time manifest merger error, not just a bad default.

Since this project only has one `<activity>` entry, `LandingActivity` is the only screen
the OS is aware of. Adding a second screen later (see
[[07-2026-09-08-navigation]]) means adding a second `<activity>` entry here — skipping
that step is the single most common "why does starting my new screen crash" bug for
beginners.

### `<intent-filter>` — what makes it the launcher

```xml
<intent-filter>
    <action android:name="android.intent.action.MAIN" />
    <category android:name="android.intent.category.LAUNCHER" />
</intent-filter>
```

An `<intent-filter>` declares what *kinds of requests* (`Intent`s — see
[[07-2026-09-08-navigation]] for more on those) this component is willing to handle. The
specific pairing here — `action.MAIN` + `category.LAUNCHER` — is a fixed, recognized
combination meaning "this is a launchable app entry point": it's what puts the icon on
the home screen/app drawer, and what the OS starts when the user taps it. Only one
Activity in an app is normally marked this way; declaring it on more than one would
create multiple separate home-screen icons for the same app.

Intent filters aren't limited to launching — they're the same general mechanism apps use
to declare "I can handle opening a URL," "I can handle sharing an image," etc. This
project only uses the one for launching.

## The `Application` class — not used here, but worth knowing

You can optionally point `<application android:name="...">` at your own subclass of
`android.app.Application`. Its `onCreate()` runs once per process, *before* any Activity
— the common place for app-wide setup (dependency-injection container init, crash
reporting, logging setup). This project's `<application>` tag has no `android:name`, so
Android just uses the plain default `Application` class and does nothing extra before
`LandingActivity` starts. If this app later adds something that needs to run once at
process startup, that's the tag to add a `android:name=".SomeApplication"` to.

## Where things are declared *outside* the manifest (a common confusion)

The manifest declares *that a component exists and what it's allowed to do* — it does
**not** contain layouts, business logic, or most configuration values:

- **Application ID / namespace** (`com.example.clonedwink`) — set once in
  `app/build.gradle.kts`, not repeated in the manifest beyond the implicit scoping
  described above.
- **What a screen looks like** — in `res/layout/*.xml`, inflated by the Activity's own
  code (`setContentView(...)`), not referenced from the manifest at all beyond the class
  name existing.
- **Strings/colors/themes referenced by attributes like `android:label`/`android:theme`**
  — actually defined in `res/values/*.xml`; the manifest only points at them via
  `@string/...`/`@style/...`, per [[04-2026-09-08-drawing-the-ui]].
- **Permissions this app needs at runtime** (camera, location, internet, etc.) — would
  be declared here via `<uses-permission android:name="..." />` tags, but none exist yet
  in this project (no networking/camera/etc. features have been added).

## Other component types you'll eventually see declared here

This project currently only has `<activity>`, but the manifest is also where you'd
declare, as the app grows:

- **`<service>`** — a component that runs work in the background with no UI (e.g. music
  playback, sync).
- **`<receiver>`** — listens for system-wide broadcast events (e.g. "network became
  available," "boot completed").
- **`<provider>`** — exposes structured data from this app to other apps (Android's
  `ContentProvider` mechanism).
- **`<uses-permission>`** — declares a dangerous/sensitive capability the app needs
  (network access, camera, precise location...), often paired with a runtime permission
  prompt shown to the user.

None of these exist in this scaffold yet — they're mentioned here so the manifest's role
("this is the OS-facing registry of everything the app can do") makes sense as more
pieces get added.

## Key takeaway

Think of `AndroidManifest.xml` as answering, for the OS, three questions before your app
ever runs: **what components does this app have** (activities, services, ...), **which
one launches first**, and **what is it allowed to do** (exported components,
permissions). Everything else — what a screen looks like, what happens when a button is
tapped, what data is shown — lives in your Kotlin/XML resource files and is unrelated to
the manifest.
