plugins {
    alias(libs.plugins.android.library)
}

// Android: the Model layer's shared data classes (CarouselSlide, HomeContent, ...) — no
// dependencies of its own, since a plain `data class` needs nothing beyond the Kotlin standard
// library. Every other module that needs these types depends on this one.
android {
    namespace = "com.example.clonedwink.core.model"
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
