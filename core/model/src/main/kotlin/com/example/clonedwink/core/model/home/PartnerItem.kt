package com.example.clonedwink.core.model.home

/**
 * One card in the "Friends of WINK+" horizontally scrollable row — a partner brand's square
 * logo tile plus a short one-line description of the perk (e.g. "Unlock benefits").
 */
data class PartnerItem(
    val id: String,
    val name: String,
    val description: String,
)
