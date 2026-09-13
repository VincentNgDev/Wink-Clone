plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.compiler)
    // Android: this module owns LandingViewModel (@HiltViewModel) and LandingModule (its
    // @Binds carousel-repository wiring) — see di/LandingModule.kt — so it needs Hilt's
    // annotation processor the same way `app` used to before this split.
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

// Android: the onboarding/carousel feature — LandingScreen, LandingViewModel, and the
// CarouselRepository that backs it. Depends on `core:model` for CarouselSlide and `core:ui` for
// the shared theme/carousel components, but never on `feature:home` or any other feature —
// features stay siblings, not dependents of each other.
android {
    namespace = "com.example.clonedwink.feature.landing"
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
    implementation(project(":core:model"))
    implementation(project(":core:ui"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.coil.android)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
