package com.example.clonedwink.data.model.home

/**
 * One card in a "big square photo + caption" row (Exciting Events Around the Island, Hot Deals
 * and Promotions) — larger than a [PartnerItem] logo tile, with an optional overlay badge like
 * "FREE".
 */
data class FeatureCard(
    val id: String,
    val imageContentDescription: String,
    val caption: String,
    val badgeText: String? = null,
)
