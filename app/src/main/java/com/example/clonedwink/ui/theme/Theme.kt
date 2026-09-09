package com.example.clonedwink.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.clonedwink.R

// Kotlin: `@Composable` marks a function as part of the Compose UI tree — it can only be
// called from inside another @Composable, and Compose's compiler plugin (applied in
// app/build.gradle.kts) rewrites it so it can be re-invoked ("recomposed") automatically
// whenever the state it reads changes, instead of us manually pushing updates onto Views the
// way LandingActivity's old observeUiState() had to.
//
// Android: this is the Compose analogue of themes.xml/Theme.Clonedwink — one place that maps
// the app's semantic brand colors to Material 3's ColorScheme slots (primary, secondary, etc.)
// so every screen built with Compose picks them up via `MaterialTheme.colorScheme.*` instead of
// hardcoding hex values inline.
@Composable
fun ClonedWinkTheme(content: @Composable () -> Unit) {
    // Kotlin: `colorResource(id)` is itself a @Composable function — it resolves an
    // `@color/...` entry from colors.xml (or values-night/colors.xml, whichever the current
    // system configuration matches) into a Compose `Color`. Calling it here means our Compose
    // color scheme automatically tracks the *same* light/dark resource overrides the rest of
    // the app already relies on, instead of duplicating brand hex values a second time in code.
    val colorScheme = if (isSystemInDarkTheme()) {
        darkColorScheme(
            primary = colorResource(R.color.brand_purple),
            onPrimary = colorResource(R.color.white),
            secondary = colorResource(R.color.brand_pink),
            onSecondary = colorResource(R.color.white),
            tertiary = colorResource(R.color.brand_teal),
            onTertiary = colorResource(R.color.white),
            background = colorResource(R.color.background),
            onBackground = colorResource(R.color.on_surface),
            surface = colorResource(R.color.surface),
            onSurface = colorResource(R.color.on_surface),
            onSurfaceVariant = colorResource(R.color.on_surface_variant),
        )
    } else {
        lightColorScheme(
            primary = colorResource(R.color.brand_purple),
            onPrimary = colorResource(R.color.white),
            secondary = colorResource(R.color.brand_pink),
            onSecondary = colorResource(R.color.white),
            tertiary = colorResource(R.color.brand_teal),
            onTertiary = colorResource(R.color.white),
            background = colorResource(R.color.background),
            onBackground = colorResource(R.color.on_surface),
            surface = colorResource(R.color.surface),
            onSurface = colorResource(R.color.on_surface),
            onSurfaceVariant = colorResource(R.color.on_surface_variant),
        )
    }

    // Kotlin: `Typography(...)` with only a couple of named parameters set (defaults fill in
    // the rest) — mirrors the "bold, rounded" display type Wink+'s own screens use for
    // headlines, while leaving body/label styles at Material 3's defaults.
    val typography = Typography(
        headlineLarge = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp),
        titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content,
    )
}
