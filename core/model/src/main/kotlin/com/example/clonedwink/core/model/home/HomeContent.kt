package com.example.clonedwink.core.model.home

// Android/MVVM: the entire renderable content of the home screen, bundled into one snapshot —
// same "one immutable object instead of a pile of separate fields" idea LandingUiState uses for
// the landing screen (see that file's comment). Every property has a default so `HomeContent()`
// with no arguments is a valid "nothing loaded yet" value, which is what HomeUiState's default
// constructor relies on.
data class HomeContent(
    val loyaltyPoints: Int = 0,
    val currentStationName: String = "",
    val quickLinks: List<QuickLinkItem> = emptyList(),
    val moreQuickLinks: List<QuickLinkItem> = emptyList(),
    val partners: List<PartnerItem> = emptyList(),
    val promoBanners: List<PromoBanner> = emptyList(),
    val diningDeals: List<PlaceCard> = emptyList(),
    val dinnerNearby: List<PlaceCard> = emptyList(),
    val excitingEvents: List<FeatureCard> = emptyList(),
    val hotDeals: List<FeatureCard> = emptyList(),
    val newsHighlights: List<MediaCard> = emptyList(),
    val movies: List<MediaCard> = emptyList(),
)
