// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
    // Android/Kotlin: declared here with `apply false` (root-project convention for a
    // multi-module build, even though this project only has one `:app` module) so the plugin's
    // version is resolved once and `app/build.gradle.kts` can apply it without repeating the
    // version number.
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ksp) apply false
}