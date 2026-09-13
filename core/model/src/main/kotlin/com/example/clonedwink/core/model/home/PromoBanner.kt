package com.example.clonedwink.core.model.home

/**
 * One full-width slide in the home screen's promo carousel (the "Redeem your F1 Exhibition
 * tickets" banner in the reference screenshot). [imageUrl] is nullable the same way
 * [com.example.clonedwink.core.model.CarouselSlide.imageUrl] is — a real backend can supply a
 * photo later; until then feature/home's HomeSections.kt falls back to a brand-gradient card
 * carrying just the title/subtitle text.
 */
data class PromoBanner(
    val id: String,
    val title: String,
    val subtitle: String,
    val imageContentDescription: String,
    val imageUrl: String? = null,
)
