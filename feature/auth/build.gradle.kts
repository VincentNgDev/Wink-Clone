plugins {
    alias(libs.plugins.android.library)
}

// Android: placeholder for a future login/signup feature — no screen exists yet (this app's
// onboarding flow lives in `feature:landing`, not here). Wired into settings.gradle.kts now so
// the module graph matches the target multi-module layout ahead of that lesson.
android {
    namespace = "com.example.clonedwink.feature.auth"
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
}
