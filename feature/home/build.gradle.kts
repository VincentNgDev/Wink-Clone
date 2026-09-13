plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.compiler)
    // Android: this module owns HomeViewModel (@HiltViewModel) and HomeModule (its @Binds
    // home-repository wiring) — see di/HomeModule.kt — so it needs Hilt's annotation processor
    // the same way `app` used to before this split.
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

// Android: the home screen feature — HomeScreen and everything it composes, HomeViewModel, and
// the HomeRepository that backs it. Depends on `core:model` for the home data classes and
// `core:ui` for the shared theme/carousel components, but never on `feature:landing` or any
// other feature — features stay siblings, not dependents of each other.
android {
    namespace = "com.example.clonedwink.feature.home"
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
