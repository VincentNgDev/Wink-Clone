package com.example.clonedwink.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.clonedwink.R
import com.example.clonedwink.data.model.home.HomeContent
import com.example.clonedwink.data.model.home.PartnerItem
import com.example.clonedwink.data.model.home.PlaceCard
import com.example.clonedwink.data.model.home.QuickLinkItem
import com.example.clonedwink.ui.theme.ClonedWinkTheme
import com.example.clonedwink.viewmodel.home.HomeUiState

// Android/Kotlin: this is the "View" half of the home screen's MVVM split, exactly the same
// stateless-composable pattern LandingScreen.kt uses — see that file's opening comment for the
// full explanation. `ui/navigation/WinkNavHost.kt` owns the HomeViewModel (via `hiltViewModel()`)
// and is the only thing that touches it directly; everything here only ever receives a
// HomeUiState snapshot plus event lambdas.

/**
 * The full home screen: a pink gradient header with a floating "My WINK+ Points" card, four
 * quick-link shortcuts, the "Friends of WINK+" partner row, a promo banner carousel, a station
 * selector, two rows of place cards, two rows of bigger feature cards, and two rows of media
 * cards — see .claude/references/home/wink-home-0{1,2,3,4}.jpeg for the layout this matches.
 */
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onScanQrClick: () -> Unit,
    onRewardsClick: () -> Unit,
    onQuickLinkClick: (QuickLinkItem) -> Unit,
    onStationInfoClick: () -> Unit,
    onErrorShown: () -> Unit,
) {
    // Kotlin: same `LaunchedEffect(uiState.hasError)` "fire once per new error" pattern as
    // LandingScreen — see that composable's comment for why keying off `hasError` (not running
    // this on every recomposition) matters.
    LaunchedEffect(uiState.hasError) {
        if (uiState.hasError) onErrorShown()
    }

    // Kotlin: `by remember { mutableStateOf(false) }` — Compose's own local UI state, scoped to
    // this composition (not the ViewModel). "Is the More bottom sheet open" and "which bottom
    // nav tab looks selected" are purely visual/ephemeral — they don't need to survive process
    // death or be shared with any other screen, so they don't belong in HomeUiState.
    var isMoreSheetVisible by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(HomeBottomNavTab.HOME) }

    val content = uiState.content

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.background)),
    ) {
        // Android: a full-bleed pink->purple gradient behind everything, fixed height — drawn
        // *before* the Scaffold below so it sits behind the status bar too (MainActivity calls
        // enableEdgeToEdge() once for the whole app), matching the reference screenshot's
        // immersive colored header instead of a plain white status bar strip.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(R.dimen.home_header_gradient_height))
                .background(
                    Brush.linearGradient(
                        colors = listOf(colorResource(R.color.brand_pink), colorResource(R.color.brand_purple)),
                    ),
                ),
        )

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            bottomBar = {
                HomeBottomNavBar(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
            },
        ) { scaffoldPadding ->
            if (uiState.isLoading && content.quickLinks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(scaffoldPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = colorResource(R.color.brand_pink))
                }
                return@Scaffold
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                contentPadding = PaddingValues(bottom = scaffoldPadding.calculateBottomPadding() + 16.dp),
            ) {
                item {
                    LoyaltyCard(
                        points = content.loyaltyPoints,
                        onScanQrClick = onScanQrClick,
                        onRewardsClick = onRewardsClick,
                        modifier = Modifier
                            .padding(horizontal = dimensionResource(R.dimen.home_content_padding))
                            .padding(top = dimensionResource(R.dimen.loyalty_card_overlap))
                            .homeCardShadow(),
                    )
                }

                item {
                    QuickLinksRow(
                        quickLinks = content.quickLinks,
                        onQuickLinkClick = { item ->
                            if (item.id == "more") isMoreSheetVisible = true else onQuickLinkClick(item)
                        },
                        modifier = Modifier.padding(
                            top = 24.dp,
                            start = dimensionResource(R.dimen.home_content_padding),
                            end = dimensionResource(R.dimen.home_content_padding),
                        ),
                    )
                }

                item {
                    PartnerRow(
                        title = stringResource(R.string.home_section_friends_of_wink),
                        partners = content.partners,
                        modifier = Modifier.padding(top = dimensionResource(R.dimen.home_section_spacing)),
                    )
                }

                item {
                    PromoBannerCarousel(
                        banners = content.promoBanners,
                        modifier = Modifier.padding(top = dimensionResource(R.dimen.home_section_spacing)),
                    )
                }

                item {
                    StationSelectorBar(
                        stationName = content.currentStationName,
                        onStationInfoClick = onStationInfoClick,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }

                item {
                    PlaceCardRow(
                        title = stringResource(R.string.home_section_dining_deals),
                        places = content.diningDeals,
                        modifier = Modifier.padding(top = dimensionResource(R.dimen.home_section_spacing)),
                    )
                }

                item {
                    PlaceCardRow(
                        title = stringResource(R.string.home_section_dinner_nearby),
                        places = content.dinnerNearby,
                        modifier = Modifier.padding(top = dimensionResource(R.dimen.home_section_spacing)),
                    )
                }

                item {
                    FeatureCardRow(
                        title = stringResource(R.string.home_section_exciting_events),
                        features = content.excitingEvents,
                        modifier = Modifier.padding(top = dimensionResource(R.dimen.home_section_spacing)),
                    )
                }

                item {
                    FeatureCardRow(
                        title = stringResource(R.string.home_section_hot_deals),
                        features = content.hotDeals,
                        modifier = Modifier.padding(top = dimensionResource(R.dimen.home_section_spacing)),
                    )
                }

                item {
                    MediaCardRow(
                        title = stringResource(R.string.home_section_news),
                        mediaCards = content.newsHighlights,
                        cardWidth = dimensionResource(R.dimen.media_card_landscape_width),
                        cardHeight = dimensionResource(R.dimen.media_card_landscape_height),
                        modifier = Modifier.padding(top = dimensionResource(R.dimen.home_section_spacing)),
                    )
                }

                item {
                    MediaCardRow(
                        title = stringResource(R.string.home_section_movies),
                        mediaCards = content.movies,
                        cardWidth = dimensionResource(R.dimen.media_card_portrait_width),
                        cardHeight = dimensionResource(R.dimen.media_card_portrait_height),
                        modifier = Modifier.padding(top = dimensionResource(R.dimen.home_section_spacing)),
                    )
                }
            }
        }
    }

    if (isMoreSheetVisible) {
        MoreQuickLinksSheet(
            items = content.moreQuickLinks,
            onItemClick = onQuickLinkClick,
            onDismiss = { isMoreSheetVisible = false },
        )
    }
}

@Composable
private fun QuickLinksRow(quickLinks: List<QuickLinkItem>, onQuickLinkClick: (QuickLinkItem) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        quickLinks.forEach { item ->
            QuickLinkTile(item = item, onClick = onQuickLinkClick)
        }
    }
}

// Kotlin: a tiny `@Composable` extension function on Modifier — same idea as
// HomeComponents.kt's `placeholderBrandGradient`, just for a Modifier instead of a Color list.
// It has to be marked `@Composable` (unlike a plain extension function) because
// `dimensionResource(...)` is itself `@Composable` and can only be called from inside one. This
// adds a real drop shadow so the loyalty card reads as "lifted" off the gradient, the same
// purpose Modifier.shadow serves on LandingScreen's GlassSlideCard.
@Composable
private fun Modifier.homeCardShadow(): Modifier = this.shadow(
    elevation = dimensionResource(R.dimen.loyalty_card_elevation),
    shape = RoundedCornerShape(dimensionResource(R.dimen.loyalty_card_corner_radius)),
)

// Android: `@Preview` renders straight in Android Studio's design pane without a
// device/emulator — see LandingScreen's equivalent preview for why. This needs its own fixed
// sample HomeUiState since there's no real HomeViewModel available at preview time.
@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun HomeScreenPreview() {
    ClonedWinkTheme {
        HomeScreen(
            uiState = HomeUiState(
                isLoading = false,
                content = HomeContent(
                    loyaltyPoints = 2450,
                    currentStationName = "Eunos",
                    quickLinks = listOf(
                        QuickLinkItem(id = "bus", label = "Bus"),
                        QuickLinkItem(id = "train", label = "Train"),
                        QuickLinkItem(id = "mrt_map", label = "MRT Map"),
                        QuickLinkItem(id = "more", label = "More"),
                    ),
                    partners = listOf(
                        PartnerItem(id = "grab", name = "Grab App", description = "Book a ride"),
                        PartnerItem(id = "citi", name = "Citi", description = "Unlock benefits"),
                    ),
                    diningDeals = listOf(
                        PlaceCard(
                            id = "1",
                            imageContentDescription = "Food",
                            locationTag = "Paya Lebar",
                            title = "Yum Yum Thai",
                            categoryTag = "Thai",
                            discountBadge = "-25%",
                            priceOriginal = "$20.00",
                            priceDiscounted = "$15.00",
                            description = "Tom Yum Noodles, Pad Thai",
                        ),
                    ),
                ),
            ),
            onScanQrClick = {},
            onRewardsClick = {},
            onQuickLinkClick = {},
            onStationInfoClick = {},
            onErrorShown = {},
        )
    }
}
