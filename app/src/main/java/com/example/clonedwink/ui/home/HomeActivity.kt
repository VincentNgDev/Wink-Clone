package com.example.clonedwink.ui.home

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
import com.example.clonedwink.data.repository.DefaultHomeRepository
import com.example.clonedwink.ui.theme.ClonedWinkTheme
import com.example.clonedwink.viewmodel.home.HomeViewModel

// Android: the app's main screen after onboarding — see LandingActivity.kt's `onGetStartedClick`
// for where this gets launched from. Same `ComponentActivity` + Compose-only shape as
// LandingActivity (no XML layout, no AppCompat machinery); see that file's class-level comment
// for the full explanation of the Android Activity lifecycle and why this extends
// ComponentActivity rather than AppCompatActivity.
class HomeActivity : ComponentActivity() {

    // Kotlin/Android: `by viewModels { viewModelFactory { initializer { ... } } }` — identical
    // pattern to LandingActivity's `viewModel` delegate; see that file's comment for what each
    // piece does. HomeViewModel needs a HomeRepository, so DefaultHomeRepository(applicationContext)
    // is constructed here rather than relying on a no-arg constructor.
    private val viewModel: HomeViewModel by viewModels {
        viewModelFactory {
            initializer { HomeViewModel(DefaultHomeRepository(applicationContext)) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Android: lets the pink gradient header in HomeScreen draw behind the status bar for
        // an immersive look — HomeScreen's own `Modifier.statusBarsPadding()` on its scrollable
        // content then keeps the loyalty card/section text clear of it. See LandingActivity's
        // matching call for the same reasoning applied to that screen.
        enableEdgeToEdge()

        setContent {
            // Kotlin: same `collectAsStateWithLifecycle()` pattern as LandingActivity — see
            // that file's comment for why this pauses collection while backgrounded instead of
            // always running.
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            ClonedWinkTheme {
                HomeScreen(
                    uiState = uiState,
                    onScanQrClick = {
                        // Android: no reward/QR-scan flow exists yet in this scaffold — per the
                        // task brief this is intentionally a no-op for now, the same
                        // "Noting for now" placeholder LandingActivity's onGetStartedClick used
                        // to be before HomeActivity existed.
                    },
                    onRewardsClick = {
                        // Android: no rewards screen exists yet — intentionally a no-op for now.
                    },
                    onQuickLinkClick = {
                        // Android: Bus/Train/MRT Map and the "More" sheet's extra shortcuts
                        // don't route anywhere yet — intentionally a no-op for now.
                    },
                    onStationInfoClick = {
                        // Android: no station-info screen exists yet — intentionally a no-op.
                    },
                    onErrorShown = {
                        Toast.makeText(this, R.string.error_loading_home, Toast.LENGTH_SHORT).show()
                    },
                )
            }
        }
    }
}
