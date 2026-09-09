# Kotlin & Android Coding Style

This project follows the official [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
and [Android Kotlin style guide](https://developer.android.com/kotlin/style-guide). Below
is the subset that matters most day-to-day, written for someone new to both Android and
Kotlin.

## Naming

| What | Convention | Example |
|---|---|---|
| Class, interface, object | `UpperCamelCase` | `class LoginViewModel`, `interface UserRepository` |
| Function, property, variable | `lowerCamelCase` | `fun loadUser()`, `val userName` |
| Constant (`const val`, top-level or in a `companion object`) | `UPPER_SNAKE_CASE` | `const val MAX_RETRIES = 3` |
| Package | all lowercase, no underscores | `com.example.clonedwink.viewmodel.login` |
| Layout XML file | `snake_case`, prefixed by type | `activity_login.xml`, `fragment_profile.xml`, `item_user.xml` |
| XML view ID | `lowerCamelCase` | `android:id="@+id/loginButton"` |
| Boolean | reads as a question | `isLoading`, `hasError`, `canSubmit` |

Names should say what something *is* or *does* — prefer `userRepository` over `repo`,
`onLoginClicked()` over `onClick()`. Avoid single-letter names outside of tiny lambda
scopes (`it`, short `for` loop indices).

## Formatting

- 4-space indentation, no tabs.
- Max line length: 120 characters.
- One statement per line; no `;` to chain statements.
- Opening brace on the same line as the declaration:
  ```kotlin
  class LoginViewModel : ViewModel() {
      fun login() {
          // ...
      }
  }
  ```
- Order class members: properties, `init` blocks, then functions. Public API before
  private helpers.
- Let Android Studio's default Kotlin formatter (`Ctrl+Alt+L` / `Code > Reformat Code`)
  handle whitespace — don't hand-align columns or fight the formatter.

## Kotlin idioms to prefer

- **`val` over `var`.** Default to `val`; only use `var` when the value genuinely needs to
  change after creation. This is the single biggest habit to build coming from other
  languages.
- **Null safety — no `!!`.** Avoid the not-null assertion operator (`!!`); it crashes at
  runtime if you're wrong. Prefer:
  - `?.` (safe call): `user?.name`
  - `?:` (Elvis operator, default value): `val name = user?.name ?: "Unknown"`
  - `if (user != null) { ... }` with Kotlin's smart-casting when you need a whole block.
- **Data classes for plain data.** Any class that's just a bag of fields (API models,
  UI state, entities) should be a `data class`, not a regular `class`:
  ```kotlin
  data class User(val id: String, val name: String, val email: String)
  ```
- **Extension functions** for adding behavior to a type you don't own (e.g. a small helper
  on `String` or `View`), instead of a static "Utils" class.
- **Trailing lambdas** for the last function parameter:
  ```kotlin
  button.setOnClickListener { viewModel.onLoginClicked() }
  ```
- **String templates** over concatenation: `"Hello, $name"` not `"Hello, " + name`.
- **`when` over chained `if/else if`** once you have 3+ branches.
- Prefer expression bodies for simple one-line functions:
  ```kotlin
  fun isValidEmail(email: String) = email.contains("@")
  ```

## Coroutines (asynchronous work)

- Use Kotlin coroutines (`suspend fun`, `viewModelScope.launch { }`) for async work —
  network calls, database access — instead of callbacks or `AsyncTask`.
- Launch coroutines from a ViewModel using `viewModelScope`, never `GlobalScope`.
- Repository/data-layer functions that do I/O should be `suspend fun`s; let the ViewModel
  decide when to launch them.

## Android/MVVM-specific rules

- **ViewModels never reference Android UI classes.** No `Context`, `Activity`, `Fragment`,
  or `View` inside a `ViewModel`. If you need `Context` (e.g. for string resources), use
  `AndroidViewModel` sparingly, or better, pass the resolved value in from the View.
- **Expose state, not events, from ViewModels** where possible — a `StateFlow<UiState>` or
  `LiveData<UiState>` that the View observes and renders, rather than the ViewModel
  reaching into the View.
- **Views observe and render; they don't decide.** An Activity/Fragment's job is: forward
  user input to the ViewModel, and render whatever state the ViewModel emits. Business
  logic (validation, formatting decisions, what happens next) belongs in the ViewModel.
- **No hardcoded strings/colors/dimensions** in Kotlin or XML — put them in
  `res/values/strings.xml`, `colors.xml`, `dimens.xml` and reference them
  (`getString(R.string.login_error)`).
- **Immutable UI state.** Model screen state as a `data class` (e.g.
  `data class LoginUiState(val isLoading: Boolean = false, val error: String? = null)`)
  rather than a pile of separate mutable properties.

## Comments & documentation

- Prefer self-explanatory names over comments explaining *what* code does.
- Write a comment only when the *why* isn't obvious from the code itself (a workaround, a
  non-obvious constraint, a gotcha from the Android framework).
- Use `KDoc` (`/** ... */`) only on public APIs whose usage isn't obvious from the
  signature — not required on every function while learning.

## Before committing

- Run `gradlew.bat lint` and fix warnings before they pile up.
- Run `gradlew.bat testDebugUnitTest` for any ViewModel/Repository changes.
- Let Android Studio's formatter run on changed files (`Reformat Code`) before committing.
