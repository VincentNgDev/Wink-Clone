plugins {
    alias(libs.plugins.android.library)
}

// Android: placeholder for this app's future networking layer (Retrofit/OkHttp API service
// interfaces + DTOs) — see folder-structure.md's `core/network` entry. No repository needs a
// real network call yet (DefaultCarouselRepository/DefaultHomeRepository are both hardcoded
// data sources), so this module is intentionally empty until a lesson adds one, rather than
// pulling in Retrofit/OkHttp dependencies nothing uses yet.
android {
    namespace = "com.example.clonedwink.core.network"
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
