package com.example.clonedwink

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

// Android: every app has exactly one Application instance, created by the system before any
// Activity, and it lives for as long as the process does — it's the natural place to set up
// anything that should exist for the whole app's lifetime, like Hilt's dependency graph.
//
// Kotlin/Android: `@HiltAndroidApp` triggers Hilt's code generation for the *entire* app — it
// generates a base class this Application extends under the hood, which in turn creates the
// top-level "SingletonComponent" dependency container (see di/RepositoryModule.kt) that every
// other Hilt-annotated class (`@AndroidEntryPoint` Activities, `@HiltViewModel` ViewModels) pulls
// its dependencies from. Every Hilt project needs exactly one class annotated like this, and it
// must be registered as android:name on <application> in AndroidManifest.xml or none of the
// other Hilt annotations in the app will work.
@HiltAndroidApp
class WinkApplication : Application()
