plugins {
    alias(libs.plugins.android.library)
}

// Android: placeholder for a future user-profile feature — no screen exists yet. Wired into
// settings.gradle.kts now so the module graph matches the target multi-module layout ahead of
// that lesson.
android {
    namespace = "com.example.clonedwink.feature.profile"
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
