package com.example.clonedwink.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.clonedwink.core.ui.theme.ClonedWinkTheme
import com.example.clonedwink.ui.navigation.WinkNavHost
import dagger.hilt.android.AndroidEntryPoint

// Android: the app's one and only Activity (single-Activity architecture) — every screen
// (onboarding, home, and any future screen) is now a composable *destination* inside the
// WinkNavHost below, instead of each screen being its own Activity (compare the old
// LandingActivity/HomeActivity pair). This is the app's LAUNCHER activity (see
// AndroidManifest.xml), so onCreate() below is called once by the system when the app's
// process is first started, and again — with a new instance — after configuration changes
// like rotation, unless that's opted out of.
//
// Android: `@AndroidEntryPoint` makes this Activity a Hilt injection site. It's required here
// because WinkNavHost's destinations call `hiltViewModel()`, which needs to reach the
// dependency graph `WinkApplication`'s `@HiltAndroidApp` set up — without this annotation, Hilt
// has nowhere to plug in and the app crashes at runtime the first time a screen is shown.
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Android: onCreate() is the first lifecycle callback — called once when this Activity
    // instance is constructed. This is where one-time setup happens; it is NOT re-called on
    // every onStart()/onResume().
    override fun onCreate(savedInstanceState: Bundle?) {
        // Android: always call the superclass implementation first — ComponentActivity does
        // its own required setup (restoring saved state, etc.) that our code depends on.
        super.onCreate(savedInstanceState)

        // Android: lets Compose content draw behind the status bar and navigation bar for an
        // immersive, edge-to-edge look. Each screen keeps its own real content clear of those
        // system bars via its own WindowInsets padding (see LandingScreen/HomeScreen).
        enableEdgeToEdge()

        // Kotlin/Android: `setContent { }` is the Compose replacement for
        // `setContentView(R.layout...)` — it hands Compose a @Composable lambda to render as
        // this Activity's entire UI. Because WinkNavHost is the only thing here, this Activity
        // itself never needs to know which screen is currently showing.
        setContent {
            ClonedWinkTheme {
                WinkNavHost()
            }
        }
    }
}
