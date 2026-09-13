plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.compiler)
}

// Android: the shared View-layer building blocks every feature module draws on — the app's
// Material theme (ClonedWinkTheme) plus composables genuinely reused across features
// (CarouselAutoScroll, CarouselDotIndicator, used by both the landing and home carousels). Also
// owns the brand color palette and dimens that are truly cross-feature design tokens, not any
// one screen's layout details — see the root CLAUDE.md change log entry for the full resource
// split rationale.
android {
    namespace = "com.example.clonedwink.core.ui"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    // Android: CarouselAutoScroll's repeatOnLifecycle/LocalLifecycleOwner call needs this —
    // see that file's comment for why the auto-scroll pauses while the app is backgrounded.
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
