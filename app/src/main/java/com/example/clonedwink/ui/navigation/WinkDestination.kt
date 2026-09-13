package com.example.clonedwink.ui.navigation

// Kotlin: a `sealed class` restricts every subclass to the ones declared in this same file —
// the compiler then knows the complete set of possible destinations, so a future `when` over a
// WinkDestination can be exhaustive (every branch covered) with no `else` needed. Each
// destination below is declared as an `object` (a singleton — there's only ever one `Landing`)
// rather than a regular class, since neither screen takes a runtime argument yet. If one did
// (e.g. a station ID), it would become a `data class WinkDestination.Station(val id: String)`
// instead, and its `route` would need a placeholder like "station/{id}" for NavHost to parse.
sealed class WinkDestination(val route: String) {
    // Android: the app's first screen (onboarding). This is the `startDestination` NavHost is
    // given in WinkNavHost.kt — the in-process equivalent of being the <intent-filter>
    // MAIN/LAUNCHER activity in the old two-Activity setup.
    object Landing : WinkDestination("landing")

    // Android: the main screen reached after "Get started" — see WinkNavHost.kt's
    // `popUpTo(...) { inclusive = true }` for how it removes Landing from the back stack on the
    // way in, matching the old `startActivity(...); finish()` behavior.
    object Home : WinkDestination("home")
}
