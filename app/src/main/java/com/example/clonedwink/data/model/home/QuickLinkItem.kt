package com.example.clonedwink.data.model.home

// Kotlin: `data class` again (see CarouselSlide.kt for the original explanation of what that
// generates for free). Deliberately holding only an `id` + `label` here, not an ImageVector —
// keeping Compose UI types out of the Model layer is what lets ui/home/HomeComponents.kt's
// `quickLinkIconFor(id)` decide the icon, the same "id picks the icon in the View layer" split
// LandingScreen's `iconForSlide(id)` already established for carousel slides.
/**
 * One tappable quick-link tile — either one of the four always-visible shortcuts (Bus, Train,
 * MRT Map, More) or one of the extra shortcuts revealed inside the "More" bottom sheet.
 */
data class QuickLinkItem(
    val id: String,
    val label: String,
)
