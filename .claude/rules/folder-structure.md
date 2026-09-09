# Folder Structure

This project follows MVVM (Model-View-ViewModel). MVVM splits code into three layers so
each piece has one job:

- **Model** — data classes and the logic to fetch/store them (network, database, repository).
- **View** — Activities/Fragments/XML layouts. Only responsible for displaying state and
  forwarding user actions. Should contain almost no logic.
- **ViewModel** — holds UI state and exposes it to the View (typically via `LiveData` or
  `StateFlow`). Survives configuration changes (e.g. screen rotation) and never references
  a View, Activity, or Context directly.

Data flows one way: **View → ViewModel → Model**, and state flows back
**Model → ViewModel → View**. The View never talks to the Model directly.

## Package layout

All source lives under `app/src/main/java/com/example/clonedwink/`. Organize by **layer
first, then by feature**, so the MVVM boundaries stay obvious as the app grows:

```
com.example.clonedwink/
├── ui/                     # View layer — one subpackage per screen/feature
│   ├── login/
│   │   ├── LoginActivity.kt
│   │   └── LoginFragment.kt
│   └── profile/
│       └── ProfileFragment.kt
├── viewmodel/              # ViewModel layer — one subpackage per screen/feature
│   ├── login/
│   │   └── LoginViewModel.kt
│   └── profile/
│       └── ProfileViewModel.kt
├── data/                   # Model layer
│   ├── repository/         # Repositories — the single source of truth each ViewModel talks to
│   │   └── UserRepository.kt
│   ├── remote/             # Network: API service interfaces, DTOs
│   │   └── UserApiService.kt
│   ├── local/              # Persistence: Room database, DAOs, entities
│   │   └── UserDao.kt
│   └── model/              # Plain data classes shared across layers
│       └── User.kt
├── di/                     # Dependency injection setup (Hilt modules, once added)
└── util/                   # Small stateless helpers (extension functions, formatters)
```

As a feature grows a matching subpackage in `ui/` and `viewmodel/` (e.g. `login/`,
`profile/`), it should use the same feature name in both places so the two are easy to
pair up.

## Resources (`app/src/main/res/`)

Standard Android resource layout — nothing MVVM-specific here:

- `layout/` — XML layouts, named after the screen/component they belong to:
  `activity_login.xml`, `fragment_profile.xml`, `item_user.xml`.
- `values/` — `strings.xml`, `colors.xml`, `themes.xml`, `dimens.xml`. Never hardcode
  strings, colors, or dimensions inline in a layout or in Kotlin code — put them here
  and reference them.
- `drawable/`, `mipmap-*/` — icons and images.
- `values-night/` — dark-theme overrides.

## Tests

- `app/src/test/java/...` — JVM unit tests (JUnit4). This is where **ViewModel tests**
  and **Repository tests** live, since neither layer touches the Android framework
  directly and both run fast on the host JVM.
- `app/src/androidTest/java/...` — instrumented tests (AndroidJUnit4/Espresso), for
  anything that needs a real device/emulator (Views, database, full-screen flows).
- Mirror the `main` package structure inside `test`/`androidTest` — e.g.
  `viewmodel/login/LoginViewModelTest.kt` tests `viewmodel/login/LoginViewModel.kt`.

## Rules of thumb

- If you're about to add a class, ask "which layer is this?" first — View, ViewModel, or
  Model — then put it in the matching top-level package.
- A `ui/` file should never import from `data/` directly; it should only talk to its
  ViewModel.
- A `viewmodel/` file should never import `android.view.*`, `android.widget.*`, or hold a
  reference to an `Activity`/`Fragment`/`View`/`Context` — that's what causes memory leaks
  and breaks on rotation.
- Keep each feature's files together (same subpackage name in `ui/` and `viewmodel/`)
  rather than grouping everything by file type across the whole app.
