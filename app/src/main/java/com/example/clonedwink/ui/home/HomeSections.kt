package com.example.clonedwink.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.clonedwink.R
import com.example.clonedwink.data.model.home.FeatureCard
import com.example.clonedwink.data.model.home.MediaCard
import com.example.clonedwink.data.model.home.PartnerItem
import com.example.clonedwink.data.model.home.PlaceCard
import com.example.clonedwink.data.model.home.PromoBanner
import com.example.clonedwink.ui.components.CarouselAutoScroll
import com.example.clonedwink.ui.components.CarouselDotIndicator
import com.example.clonedwink.ui.components.HorizontalCardSection

// Android/Kotlin: every horizontally scrollable row on the home screen (partners, the promo
// banner carousel, place cards, feature cards, media cards) plus the card composable each one
// lays out. All of them take plain data-layer lists in and fire plain lambdas out — see
// LandingScreen.kt's file comment for why that "only ever talk to the ViewModel via a snapshot
// + callbacks" split matters here too. HomeScreen.kt assembles these into the full page.
//
// PartnerRow, PlaceCardRow, FeatureCardRow, and MediaCardRow all delegate to
// `ui/components/HorizontalCardSection.kt` for their "title + LazyRow" shape — each one only
// describes its own per-item card. PromoBannerCarousel delegates its auto-scroll and dot
// indicator to `ui/components/CarouselAutoScroll.kt` and `CarouselDotIndicator.kt`, the same
// shared pieces LandingScreen.kt's onboarding carousel uses.

private const val PROMO_AUTO_SCROLL_INTERVAL_MS = 5000L

@Composable
fun PartnerRow(title: String, partners: List<PartnerItem>, modifier: Modifier = Modifier) {
    // Kotlin: `partners.withIndex()` pairs each item with its position up front (as
    // `IndexedValue<PartnerItem>`), so `PartnerCard` below can look up its placeholder gradient
    // color by position without re-scanning the list per item the way `list.indexOf(item)`
    // would.
    HorizontalCardSection(
        title = title,
        items = partners.withIndex().toList(),
        itemSpacing = dimensionResource(R.dimen.partner_card_spacing),
        key = { it.value.id },
        modifier = modifier,
    ) { (index, partner) ->
        PartnerCard(partner = partner, colorIndex = index)
    }
}

@Composable
private fun PartnerCard(partner: PartnerItem, colorIndex: Int) {
    Column(
        modifier = Modifier.width(dimensionResource(R.dimen.partner_card_width)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(dimensionResource(R.dimen.partner_logo_size))
                .clip(RoundedCornerShape(dimensionResource(R.dimen.partner_logo_corner_radius)))
                .background(Brush.linearGradient(placeholderBrandGradient(colorIndex))),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                // Kotlin: `.first().uppercase()` on the partner name stands in for a real logo
                // image — a bold initial on a brand-color tile, the same "no asset yet" fallback
                // approach LandingScreen's gradient cards use, just rendered as a letter instead
                // of an icon since a partner is better recognized by its initial than by a
                // generic Material icon.
                text = partner.name.first().uppercase(),
                color = colorResource(R.color.white),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = partner.name,
            color = colorResource(R.color.on_surface),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = partner.description,
            color = colorResource(R.color.on_surface_variant),
            fontSize = 11.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun PromoBannerCarousel(banners: List<PromoBanner>, modifier: Modifier = Modifier) {
    if (banners.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { banners.size })
    CarouselAutoScroll(pagerState = pagerState, itemCount = banners.size, intervalMs = PROMO_AUTO_SCROLL_INTERVAL_MS)

    Column(modifier = modifier) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(R.dimen.promo_banner_height)),
            contentPadding = PaddingValues(horizontal = dimensionResource(R.dimen.promo_banner_peek_padding)),
            pageSpacing = dimensionResource(R.dimen.promo_banner_page_margin),
        ) { page ->
            PromoBannerCard(banner = banners[page], colorIndex = page)
        }

        if (banners.size > 1) {
            Spacer(modifier = Modifier.height(10.dp))
            CarouselDotIndicator(
                pageCount = banners.size,
                currentPage = pagerState.currentPage,
                selectedColor = colorResource(R.color.brand_pink),
                unselectedColor = colorResource(R.color.chip_background),
                selectedWidth = dimensionResource(R.dimen.promo_indicator_selected_width),
                unselectedWidth = dimensionResource(R.dimen.promo_indicator_unselected_width),
                dotHeight = dimensionResource(R.dimen.promo_indicator_dot_height),
                dotSpacing = dimensionResource(R.dimen.carousel_dot_spacing),
            )
        }
    }
}

@Composable
private fun PromoBannerCard(banner: PromoBanner, colorIndex: Int) {
    val cardShape = RoundedCornerShape(dimensionResource(R.dimen.promo_banner_corner_radius))
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(cardShape)
            .background(Brush.linearGradient(placeholderBrandGradient(colorIndex))),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, colorResource(R.color.carousel_scrim_opaque)),
                    ),
                ),
        )
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Text(
                text = banner.title,
                color = colorResource(R.color.white),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = banner.subtitle,
                color = colorResource(R.color.white).copy(alpha = 0.9f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
fun PlaceCardRow(title: String, places: List<PlaceCard>, modifier: Modifier = Modifier) {
    HorizontalCardSection(
        title = title,
        items = places,
        itemSpacing = dimensionResource(R.dimen.place_card_spacing),
        key = { it.id },
        modifier = modifier,
        // Android: dining-deal/dinner-nearby rows show a "See more" link (even though it's a
        // no-op today — see HomeBottomNavTab's comment for the same "nothing built yet" reasoning);
        // FeatureCardRow/MediaCardRow below intentionally don't, matching the reference screenshots.
        onSeeMoreClick = {},
    ) { place -> PlaceCardView(place = place) }
}

@Composable
private fun PlaceCardView(place: PlaceCard) {
    Column(
        modifier = Modifier
            .width(dimensionResource(R.dimen.place_card_width))
            .clip(RoundedCornerShape(dimensionResource(R.dimen.place_card_corner_radius)))
            .background(colorResource(R.color.surface)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(R.dimen.place_card_image_height))
                .background(Brush.linearGradient(placeholderBrandGradient(place.id.hashCode()))),
        ) {
            if (place.discountBadge != null) {
                Text(
                    text = place.discountBadge,
                    color = colorResource(R.color.white),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(colorResource(R.color.brand_pink))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }

        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = place.locationTag,
                color = colorResource(R.color.on_surface_variant),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = place.title,
                color = colorResource(R.color.on_surface),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )

            if (place.categoryTag != null) {
                Text(
                    text = place.categoryTag,
                    color = colorResource(R.color.on_surface_variant),
                    fontSize = 11.sp,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(colorResource(R.color.chip_background))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }

            // Kotlin: `place.priceDiscounted != null` gates a price row, `place.rating != null`
            // gates a rating row — PlaceCard's comment explains why one shape covers both the
            // dining-deal and dinner-nearby card variants instead of two near-duplicate types.
            if (place.priceDiscounted != null) {
                Row(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = place.priceDiscounted,
                        color = colorResource(R.color.brand_pink),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    if (place.priceOriginal != null) {
                        Text(
                            text = place.priceOriginal,
                            color = colorResource(R.color.on_surface_variant),
                            fontSize = 13.sp,
                            textDecoration = TextDecoration.LineThrough,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
            } else if (place.rating != null) {
                Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = colorResource(R.color.rating_star),
                        modifier = Modifier.width(14.dp),
                    )
                    Text(
                        text = place.rating,
                        color = colorResource(R.color.on_surface),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                    if (place.reviewCountText != null) {
                        Text(
                            text = "| ${place.reviewCountText}",
                            color = colorResource(R.color.brand_pink),
                            fontSize = 12.sp,
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }
            }

            if (place.statusText != null) {
                Row(modifier = Modifier.padding(top = 4.dp)) {
                    Text(
                        text = place.statusText,
                        color = colorResource(R.color.status_open),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (place.hoursText != null) {
                        Text(
                            text = "  ${place.hoursText}",
                            color = colorResource(R.color.on_surface_variant),
                            fontSize = 12.sp,
                        )
                    }
                }
            }

            Text(
                text = place.description,
                color = colorResource(R.color.on_surface_variant),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
fun FeatureCardRow(title: String, features: List<FeatureCard>, modifier: Modifier = Modifier) {
    HorizontalCardSection(
        title = title,
        items = features.withIndex().toList(),
        itemSpacing = dimensionResource(R.dimen.feature_card_spacing),
        key = { it.value.id },
        modifier = modifier,
    ) { (index, feature) ->
        FeatureCardView(feature = feature, colorIndex = index)
    }
}

@Composable
private fun FeatureCardView(feature: FeatureCard, colorIndex: Int) {
    Column(modifier = Modifier.width(dimensionResource(R.dimen.feature_card_size))) {
        Box(
            modifier = Modifier
                .size(dimensionResource(R.dimen.feature_card_size))
                .clip(RoundedCornerShape(dimensionResource(R.dimen.feature_card_corner_radius)))
                .background(Brush.linearGradient(placeholderBrandGradient(colorIndex))),
        ) {
            if (feature.badgeText != null) {
                Text(
                    text = feature.badgeText,
                    color = colorResource(R.color.white),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(colorResource(R.color.on_surface).copy(alpha = 0.55f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
        Text(
            text = feature.caption,
            color = colorResource(R.color.on_surface),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
fun MediaCardRow(
    title: String,
    mediaCards: List<MediaCard>,
    cardWidth: Dp,
    cardHeight: Dp,
    modifier: Modifier = Modifier,
) {
    HorizontalCardSection(
        title = title,
        items = mediaCards.withIndex().toList(),
        itemSpacing = dimensionResource(R.dimen.media_card_spacing),
        key = { it.value.id },
        modifier = modifier,
    ) { (index, media) ->
        MediaCardView(media = media, colorIndex = index, cardWidth = cardWidth, cardHeight = cardHeight)
    }
}

@Composable
private fun MediaCardView(media: MediaCard, colorIndex: Int, cardWidth: Dp, cardHeight: Dp) {
    Column(modifier = Modifier.width(cardWidth)) {
        Box(
            modifier = Modifier
                .width(cardWidth)
                .height(cardHeight)
                .clip(RoundedCornerShape(dimensionResource(R.dimen.media_card_corner_radius)))
                .background(Brush.linearGradient(placeholderBrandGradient(colorIndex))),
        )
        Text(
            text = media.caption,
            color = colorResource(R.color.on_surface),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
