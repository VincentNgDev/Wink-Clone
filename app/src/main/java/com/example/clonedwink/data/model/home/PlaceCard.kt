package com.example.clonedwink.data.model.home

// Kotlin: one shape covers both the "MRTreats Dining Deals" cards (price fields set, rating
// fields null) and the "Dinner Nearby" cards (rating fields set, price fields null) from the
// reference screenshot, rather than two near-duplicate classes — ui/home/HomeComponents.kt's
// PlaceCardView renders whichever optional fields are non-null. All the price/rating/status
// text below is pre-formatted (e.g. "$15.00", "4.2", "Open now") rather than raw numbers,
// since this mock data layer stands in for what would otherwise be a server response already
// formatted for display — see DefaultHomeRepository.kt's file comment for why these particular
// strings live as Kotlin literals instead of in strings.xml.
/**
 * One card in a "place" row (dining deals or nearby restaurants): a photo, an optional discount
 * badge, a neighborhood tag, a title, an optional category chip, and either a price or a rating
 * line, plus a short description.
 */
data class PlaceCard(
    val id: String,
    val imageContentDescription: String,
    val locationTag: String,
    val title: String,
    val description: String,
    val discountBadge: String? = null,
    val categoryTag: String? = null,
    val priceOriginal: String? = null,
    val priceDiscounted: String? = null,
    val rating: String? = null,
    val reviewCountText: String? = null,
    val statusText: String? = null,
    val hoursText: String? = null,
)
