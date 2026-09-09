package com.example.clonedwink.ui.landing

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.clonedwink.R
import com.example.clonedwink.data.repository.DefaultCarouselRepository
import com.example.clonedwink.ui.home.HomeActivity
import com.example.clonedwink.ui.theme.ClonedWinkTheme
import com.example.clonedwink.viewmodel.landing.LandingViewModel

// Android: an Activity is one screen of the app and a core part of the Android component
// lifecycle. The system (not our code) decides when to create/start/resume/pause/stop/destroy
// one, e.g. on first launch, on rotation, when the user backgrounds the app, when memory is
// low, etc. We hook into that by overriding lifecycle methods like onCreate() below; we never
// call onCreate() ourselves. This is the launcher activity (see AndroidManifest.xml), so it's
// the first screen the user sees when the app opens.
//
// Kotlin/Android: this extends `ComponentActivity` (not `AppCompatActivity`) — the Compose
// entry point `setContent { }` only needs ComponentActivity, and this screen no longer inflates
// any XML layout or uses any View-system widget (MaterialButton, ViewPager2, etc.), so the
// extra AppCompat machinery (XML theme resolution, ActionBar delegate) isn't needed here.
class LandingActivity : ComponentActivity() {

    // Kotlin: `by viewModels { ... }` is a *delegated property* — instead of computing this
    // value directly, `viewModels()` returns a Lazy-like delegate object, and the `by` keyword
    // wires up `viewModel`'s getter to go through it. The net effect: the ViewModel is created
    // lazily (on first access) and, critically, the *same* instance is returned again after
    // rotation instead of a new one being constructed — see the comment on LandingViewModel's
    // class declaration for why that matters.
    //
    // Android: `viewModelFactory { initializer { ... } }` is how you construct a ViewModel
    // that needs constructor arguments (here, a DefaultCarouselRepository). Without a custom
    // factory, the `by viewModels()` delegate only knows how to call a no-arg constructor.
    private val viewModel: LandingViewModel by viewModels {
        viewModelFactory {
            initializer { LandingViewModel(DefaultCarouselRepository(applicationContext)) }
        }
    }

    // Android: onCreate() is the first lifecycle callback — called once when the Activity is
    // first constructed (and again, with a *new* Activity instance, after configuration
    // changes like rotation unless you've opted out of that). This is where one-time setup
    // happens; it is NOT re-called on every onStart/onResume.
    override fun onCreate(savedInstanceState: Bundle?) {
        // Android: always call the superclass implementation first — ComponentActivity does
        // its own required setup (restoring saved state, etc.) that our code depends on.
        super.onCreate(savedInstanceState)

        // Android: lets our Compose content draw behind the status bar and navigation bar for
        // an immersive, edge-to-edge look — LandingScreen's own WindowInsets.safeDrawing
        // padding (see LandingScreen.kt) then keeps the actual title/CTA content clear of them,
        // so only the gradient background bleeds all the way to the screen edges.
        enableEdgeToEdge()

        // Kotlin/Android: `setContent { }` is the Compose replacement for
        // `setContentView(R.layout...)` — instead of inflating an XML View tree, it hands
        // Compose a @Composable lambda to render as this Activity's entire UI, and Compose
        // takes over re-rendering it whenever the state it reads changes.
        setContent {
            // Kotlin: `by viewModel.uiState.collectAsStateWithLifecycle()` is the Compose
            // equivalent of the old `repeatOnLifecycle(STARTED) { viewModel.uiState.collect {
            // } }` pattern — it collects the StateFlow only while this Activity is at least
            // STARTED (pausing while backgrounded) and exposes the latest value as Compose
            // `State`, so `uiState` below always holds the current snapshot and LandingScreen
            // recomposes automatically whenever the ViewModel emits a new one.
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            ClonedWinkTheme {
                LandingScreen(
                    uiState = uiState,
                    onGetStartedClick = {
                        // Android: `Intent(context, TargetActivity::class.java)` is how you
                        // navigate between Activities — it describes "start this component",
                        // and `startActivity(...)` hands it to the system to actually launch.
                        // `finish()` right after removes LandingActivity from the back stack,
                        // so the system Back button from HomeActivity exits the app instead of
                        // returning to onboarding.
                        startActivity(Intent(this, HomeActivity::class.java))
                        finish()
                    },
                    onErrorShown = {
                        Toast.makeText(this, R.string.error_loading_slides, Toast.LENGTH_SHORT).show()
                    },
                )
            }
        }
    }
}
