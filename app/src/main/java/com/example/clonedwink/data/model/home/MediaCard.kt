package com.example.clonedwink.data.model.home

/**
 * One card in a rectangular-photo-plus-caption row — used for both the landscape "News and
 * Highlights" row and the portrait "Movies" row in ui/home/HomeSections.kt; the two rows just
 * lay the same shape out at different card dimensions (see R.dimen.media_card_landscape_* /
 * R.dimen.media_card_portrait_*).
 */
data class MediaCard(
    val id: String,
    val imageContentDescription: String,
    val caption: String,
)
