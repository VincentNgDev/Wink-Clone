package com.example.clonedwink.core.model

// Kotlin: `data class` auto-generates equals()/hashCode()/toString()/copy() based on the
// constructor properties below. That's why LandingViewModel and the adapter's DiffUtil can
// compare two CarouselSlide instances with `==` and get a real field-by-field comparison
// instead of Java's default reference (identity) comparison.
/**
 * One slide in the landing carousel. Either [imageRes] (bundled hardcoded artwork) or
 * [imageUrl] (an API-supplied image) is expected to be set; the adapter loads whichever
 * is present.
 */
data class CarouselSlide(
    // Kotlin: constructor parameters prefixed with `val` become read-only properties on
    // the class automatically — no separate field + getter boilerplate like in Java.
    val id: String,
    val title: String,
    val subtitle: String,
    val imageContentDescription: String,
    // Kotlin: `Int?` is a *nullable* Int — the `?` means this property may hold `null`.
    // Plain `Int` can never be null; the type system enforces that at compile time.
    // `= null` gives it a default value, so callers can omit it (see DefaultCarouselRepository,
    // which only ever sets imageRes, leaving imageUrl null via this default).
    val imageRes: Int? = null,
    val imageUrl: String? = null,
)
