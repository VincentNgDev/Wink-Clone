plugins {
    alias(libs.plugins.android.library)
}

// Android: placeholder for this app's future persistence layer (Room database, DAOs, entities)
// — see folder-structure.md's `core/database` entry. Nothing persists locally yet, so this
// module is intentionally empty until a lesson adds Room, rather than pulling in a dependency
// nothing uses yet.
android {
    namespace = "com.example.clonedwink.core.database"
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
