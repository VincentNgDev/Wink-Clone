package com.example.clonedwink.core.model.home

/**
 * One card in a rectangular-photo-plus-caption row — used for both the landscape "News and
 * Highlights" row and the portrait "Movies" row in feature/home's HomeSections.kt; the two rows
 * just lay the same shape out at different card dimensions (see R.dimen.media_card_landscape_* /
 * R.dimen.media_card_portrait_*).
 */
data class MediaCard(
    val id: String,
    val imageContentDescription: String,
    val caption: String,
)
