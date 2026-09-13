plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    // Android: applies Hilt's Gradle plugin — it's what rewrites @HiltAndroidApp/@AndroidEntryPoint
    // classes at compile time to wire in the generated dependency graph. Must be paired with the
    // KSP plugin below, which is what actually runs Hilt's annotation processor.
    alias(libs.plugins.hilt.android)
    // Kotlin: KSP runs Hilt's code generator against this module's @Inject/@Module/@Binds
    // annotations during the build — see gradle/libs.versions.toml's `ksp` version comment.
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.clonedwink"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.clonedwink"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // Android: turns on the Jetpack Compose toolchain for this module (the Compose compiler
    // plugin applied above hooks into kotlinc; this flag is what actually makes the `androidx.
    // compose.*` dependencies below usable and lets `setContent { }` accept @Composable code).
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.coil.android)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.hilt.android)
    // Kotlin/Android: `ksp(...)` (not `implementation(...)`) — this artifact is Hilt's
    // *annotation processor*, code that runs at compile time to generate the DI wiring classes.
    // It's never packaged into the APK itself, so it belongs in the `ksp` configuration, which
    // the KSP plugin applied above only feeds to the code generator.
    ksp(libs.hilt.compiler)
    // Android: NavHost/NavController/composable(...) for in-process, single-Activity navigation
    // between screens — see ui/navigation/WinkNavHost.kt.
    implementation(libs.androidx.navigation.compose)
    // Android: lets WinkNavHost's destinations call `hiltViewModel()` to get a Hilt-provided
    // ViewModel scoped to that destination, instead of each screen needing its own Activity.
    // Kotlin/Android: this is the `hilt-lifecycle-viewmodel-compose` artifact, not
    // `hilt-navigation-compose` — as of Hilt 1.4.0 the `hiltViewModel()` composable moved here so
    // it no longer drags in a transitive dependency on androidx.navigation just to build a
    // ViewModel; we already depend on navigation-compose above for NavHost itself, but keeping
    // this split means a screen could call `hiltViewModel()` even without Navigation Compose.
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    // Kotlin/Android: a Gradle BOM ("Bill of Materials") pins every androidx.compose.* library
    // below to mutually-compatible versions — `platform(...)` tells Gradle "read this artifact
    // for version numbers only," so none of the compose-* lines beneath need their own
    // version.ref, and bumping composeBom in libs.versions.toml upgrades all of them together.
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    // Android: ui-tooling powers the Android Studio @Preview renderer — it's only needed at
    // development time, so `debugImplementation` keeps it (and its extra APK weight) out of
    // release builds entirely instead of just being unused code.
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}