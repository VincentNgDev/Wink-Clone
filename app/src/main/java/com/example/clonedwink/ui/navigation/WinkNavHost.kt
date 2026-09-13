package com.example.clonedwink.ui.navigation

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.clonedwink.R
import com.example.clonedwink.ui.home.HomeScreen
import com.example.clonedwink.ui.landing.LandingScreen
import com.example.clonedwink.viewmodel.home.HomeViewModel
import com.example.clonedwink.viewmodel.landing.LandingViewModel

// Android: this is the single-Activity-architecture piece. Previously LandingActivity and
// HomeActivity were two separate Android components, each started via an `Intent` (see
// .claude/lessons/07-2026-09-08-navigation.md) — now MainActivity hosts exactly one of these
// NavHosts, and every screen is a *destination* inside it. Switching screens becomes an
// in-process `navController.navigate(...)` call instead of starting a new Activity, which is
// why this function needs no Activity/Context reference of its own (it grabs one locally below,
// only for the Toast calls that used to rely on the old Activity's implicit `this`).
@Composable
fun WinkNavHost(
    // Kotlin: a default parameter — callers (just MainActivity, today) can omit this argument
    // entirely and get `rememberNavController()`'s result, but a test could pass in its own
    // NavHostController to inspect/drive navigation without needing a real Activity.
    navController: NavHostController = rememberNavController(),
) {
    // Android: `LocalContext.current` is Compose's way to read the current Android Context from
    // inside a @Composable when you're not in an Activity yourself — the old Activities used
    // their own `this` for the same Toast.makeText(...) calls.
    val context = LocalContext.current

    // Android/Kotlin: `NavHost` is the container that swaps its displayed content to match
    // whatever route is on top of `navController`'s back stack. `startDestination` is shown
    // first — the in-process equivalent of LandingActivity being the MAIN/LAUNCHER activity.
    NavHost(
        navController = navController,
        startDestination = WinkDestination.Landing.route,
    ) {
        // Kotlin: `composable(route) { ... }` registers one destination on the graph — the
        // trailing lambda is the @Composable content NavHost shows while this route is on top
        // of the back stack.
        composable(WinkDestination.Landing.route) {
            // Android/Kotlin: `hiltViewModel()` is navigation-compose's replacement for the old
            // `by viewModels()` delegate in LandingActivity — it asks Hilt for a
            // LandingViewModel scoped to *this* back-stack entry (created the first time this
            // route is navigated to, cleared when the entry is popped off the stack), rather
            // than scoped to a whole Activity that outlives every screen.
            val viewModel: LandingViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            LandingScreen(
                uiState = uiState,
                onGetStartedClick = {
                    navController.navigate(WinkDestination.Home.route) {
                        // Android: pops Landing off the back stack as part of this navigation —
                        // the NavHost equivalent of the old `startActivity(...); finish()`
                        // pair, so pressing Back from Home still exits the app instead of
                        // returning to onboarding.
                        popUpTo(WinkDestination.Landing.route) { inclusive = true }
                    }
                },
                onErrorShown = {
                    Toast.makeText(context, R.string.error_loading_slides, Toast.LENGTH_SHORT).show()
                },
            )
        }

        composable(WinkDestination.Home.route) {
            val viewModel: HomeViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            HomeScreen(
                uiState = uiState,
                onScanQrClick = {
                    // Android: no reward/QR-scan flow exists yet — intentionally a no-op, same
                    // as the old HomeActivity.
                },
                onRewardsClick = {
                    // Android: no rewards screen exists yet — intentionally a no-op.
                },
                onQuickLinkClick = {
                    // Android: Bus/Train/MRT Map and the "More" sheet's extra shortcuts don't
                    // route anywhere yet — intentionally a no-op.
                },
                onStationInfoClick = {
                    // Android: no station-info screen exists yet — intentionally a no-op.
                },
                onErrorShown = {
                    Toast.makeText(context, R.string.error_loading_home, Toast.LENGTH_SHORT).show()
                },
            )
        }
    }
}
