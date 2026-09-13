package com.example.clonedwink.data.repository

import android.content.Context
import com.example.clonedwink.R
import com.example.clonedwink.data.model.CarouselSlide
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

// Kotlin: `class DefaultCarouselRepository(...) : CarouselRepository` — the primary
// constructor parameter and the supertype are declared right on the class header. This
// class "implements" the CarouselRepository interface (Kotlin uses `:` for both extending a
// class and implementing an interface — there's no separate `implements` keyword like Java).
//
// Android: it takes a `Context` because loading localized strings (context.getString) and
// resolving drawable resource IDs both go through the app's Context. This is a hardcoded
// data source today; a real app would instead take a Retrofit service or a database DAO here.
//
// Kotlin/Android: `@Inject constructor(...)` is what makes this class *self-sufficient* for
// Hilt — it never needs a matching `@Provides` function anywhere, because this annotation tells
// Hilt "here's how to build one: call this constructor." `@ApplicationContext` is a Hilt
// *qualifier* on the `Context` parameter — plain `Context` is ambiguous (an Activity context and
// the single app-wide context are both a `Context`), so this tells Hilt specifically to supply
// the long-lived application Context, which is safe to hold in a singleton-scoped repository
// (see di/RepositoryModule.kt) without risking an Activity-context leak.
class DefaultCarouselRepository @Inject constructor(
    // Kotlin: `@param:ApplicationContext` (rather than bare `@ApplicationContext`) pins this
    // annotation to the constructor *parameter* specifically — Dagger/Hilt need to see it there
    // at compile time to match this dependency to the `@ApplicationContext Context` binding.
    // Kotlin's default target for a constructor-property annotation like this is changing in a
    // future release (see the compiler warning this silences), so being explicit keeps today's
    // behavior either way.
    @param:ApplicationContext private val context: Context,
) : CarouselRepository {

    // Kotlin: `override` is required whenever a function fulfills an interface/superclass
    // member — the compiler rejects it if the signature doesn't actually match one.
    // `= listOf(...)` is an *expression body* (no `{ return ... }` needed) since the whole
    // function is just "build and return this list".
    //
    // Android: none of these slides set `imageRes`/`imageUrl` — this hardcoded data source has
    // no real artwork yet, so ui/landing/LandingScreen.kt's GlassSlideCard falls back to a
    // brand-color gradient keyed by `id` instead. A real photo-backed slide (imageUrl from a
    // network CarouselRepository, or a bundled vector/bitmap via imageRes) would render through
    // that same composable unchanged — only a `<shape>` gradient drawable specifically doesn't
    // work there, since Compose's `painterResource()` only understands VectorDrawables and
    // rasterized formats (PNG/JPG/WEBP), not arbitrary View-system drawables.
    override suspend fun getSlides(): List<CarouselSlide> = listOf(
        // Kotlin: named arguments (`id = "meet"`, `title = ...`) — you don't have to pass
        // constructor arguments positionally. This is why the calls below are readable even
        // though CarouselSlide has several parameters: each one says what it's for at the call
        // site.
        CarouselSlide(
            id = "meet",
            // Android: `context.getString(R.id)` resolves a resource ID from strings.xml into
            // the actual localized text at runtime — this is why the coding-style rules ban
            // hardcoded strings in Kotlin: R.string.* is the single source of truth, and
            // Android automatically picks the right translation based on device locale.
            title = context.getString(R.string.carousel_slide_1_title),
            subtitle = context.getString(R.string.carousel_slide_1_subtitle),
            imageContentDescription = context.getString(R.string.carousel_slide_1_content_description),
        ),
        CarouselSlide(
            id = "chat",
            title = context.getString(R.string.carousel_slide_2_title),
            subtitle = context.getString(R.string.carousel_slide_2_subtitle),
            imageContentDescription = context.getString(R.string.carousel_slide_2_content_description),
        ),
        CarouselSlide(
            id = "date",
            title = context.getString(R.string.carousel_slide_3_title),
            subtitle = context.getString(R.string.carousel_slide_3_subtitle),
            imageContentDescription = context.getString(R.string.carousel_slide_3_content_description),
        ),
        // Kotlin: trailing commas after the last element (here, and after each property
        // above) are allowed and encouraged — it keeps future diffs to one line when a new
        // entry is added, instead of also touching the previous line to add a comma.
    )
}
