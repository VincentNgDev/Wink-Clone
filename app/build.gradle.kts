plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
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