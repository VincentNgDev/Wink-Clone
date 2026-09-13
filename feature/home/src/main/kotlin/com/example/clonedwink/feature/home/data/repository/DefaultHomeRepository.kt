package com.example.clonedwink.feature.home.data.repository

import android.content.Context
import com.example.clonedwink.core.model.home.FeatureCard
import com.example.clonedwink.core.model.home.HomeContent
import com.example.clonedwink.core.model.home.MediaCard
import com.example.clonedwink.core.model.home.PartnerItem
import com.example.clonedwink.core.model.home.PlaceCard
import com.example.clonedwink.core.model.home.PromoBanner
import com.example.clonedwink.core.model.home.QuickLinkItem
import com.example.clonedwink.feature.home.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

// Android: takes a Context for the same reason DefaultCarouselRepository does — resolving the
// fixed navigational label strings (Bus, Train, MRT Map, ...) through context.getString(). The
// rest of this file's text (partner names, promo copy, place/feature/media card titles and
// descriptions) is deliberately plain Kotlin string literals instead of strings.xml entries:
// this hardcoded repository is standing in for a future network response, and a real API
// response is never round-tripped through Android string resources either — only text this app
// itself authors (buttons, section headers, nav labels) goes through strings.xml, per
// coding-style.md's "no hardcoded strings" rule. See HomeRepository.kt for the swap-in seam a
// real `core:network`-backed implementation would use later.
//
// Android: none of the cards below set a real image URL — like DefaultCarouselRepository's
// slides, this app has no bundled photo assets or network access wired up yet, so
// feature:home's HomeComponents.kt renders every card's photo area as a brand-color gradient
// tile (cycled by list position) instead. A future version of this repository handing back real
// `imageUrl`s would render unchanged, the same way GlassSlideCard already supports both cases.
// Kotlin/Android: `@Inject constructor` + `@ApplicationContext` — same reasoning as
// DefaultCarouselRepository.kt's matching constructor; see that file's comment.
class DefaultHomeRepository @Inject constructor(
    // Kotlin: `@param:ApplicationContext` — see DefaultCarouselRepository.kt's matching
    // constructor comment for why the explicit `param:` target matters here.
    @param:ApplicationContext private val context: Context,
) : HomeRepository {

    override suspend fun getHomeContent(): HomeContent = HomeContent(
        loyaltyPoints = 2450,
        currentStationName = "Eunos",
        quickLinks = listOf(
            QuickLinkItem(id = "bus", label = context.getString(R.string.quick_link_bus)),
            QuickLinkItem(id = "train", label = context.getString(R.string.quick_link_train)),
            QuickLinkItem(id = "mrt_map", label = context.getString(R.string.quick_link_mrt_map)),
            QuickLinkItem(id = "more", label = context.getString(R.string.quick_link_more)),
        ),
        // Android: these only ever surface inside the "More" bottom sheet (see
        // ui/home/HomeComponents.kt's MoreQuickLinksSheet) — the reference screenshot doesn't
        // show what's behind that sheet, so this list is this clone's own invented content,
        // reusing the same QuickLinkItem shape as the four always-visible tiles above.
        moreQuickLinks = listOf(
            QuickLinkItem(id = "taxi", label = context.getString(R.string.quick_link_taxi)),
            QuickLinkItem(id = "parking", label = context.getString(R.string.quick_link_parking)),
            QuickLinkItem(id = "vouchers", label = context.getString(R.string.quick_link_vouchers)),
            QuickLinkItem(id = "nearby_deals", label = context.getString(R.string.quick_link_nearby_deals)),
            QuickLinkItem(id = "feedback", label = context.getString(R.string.quick_link_feedback)),
            QuickLinkItem(id = "contact_us", label = context.getString(R.string.quick_link_contact_us)),
        ),
        partners = listOf(
            PartnerItem(id = "grab", name = "Grab App", description = "Book a ride"),
            PartnerItem(id = "uob_tmrw", name = "UOB TMRW", description = "More deals here"),
            PartnerItem(id = "citi", name = "Citi", description = "Unlock benefits"),
            PartnerItem(id = "premier_limo", name = "Premier Limo", description = "Book a ride today"),
            PartnerItem(id = "the_anchorage", name = "The Anchorage", description = "Dine and unwind"),
        ),
        promoBanners = listOf(
            PromoBanner(
                id = "exhibition_pole_position",
                title = "Redeem your Exhibition Pole Position tickets now with WINK Points!",
                subtitle = "9 Sep - 18 Oct",
                imageContentDescription = "A neon-lit race car ticket promo banner",
            ),
            PromoBanner(
                id = "shop_spend_save",
                title = "Take your savings places",
                subtitle = "5% savings when you shop, spend and ride",
                imageContentDescription = "A commuter walking through a station concourse",
            ),
        ),
        diningDeals = listOf(
            PlaceCard(
                id = "yum_yum_thai",
                imageContentDescription = "A spread of Thai curry, noodles, and a fried egg",
                discountBadge = "-25%",
                locationTag = "Paya Lebar",
                title = "Yum Yum Thai",
                categoryTag = "Thai",
                priceOriginal = "$20.00",
                priceDiscounted = "$15.00",
                description = "Tom Yum Noodles, Pad Thai, Green Curry",
            ),
            PlaceCard(
                id = "coexist_coffee",
                imageContentDescription = "An espresso shot pouring from a coffee machine",
                discountBadge = "-50%",
                locationTag = "Paya Lebar",
                title = "Coexist Coffee Co.",
                categoryTag = "Café",
                priceOriginal = "$12.00",
                priceDiscounted = "$6.00",
                description = "House Blend, Cold Brew, Pastries",
            ),
        ),
        dinnerNearby = listOf(
            PlaceCard(
                id = "seng_kee_herbal_soup",
                imageContentDescription = "A plate of black chicken herbal soup with dried chili",
                locationTag = "Kembangan",
                title = "Seng Kee Black Chicken Herbal Soup",
                categoryTag = "Zi Char",
                statusText = "Open now",
                hoursText = "11:30 am - 1:30 am",
                rating = "4.2",
                reviewCountText = "3,709 reviews",
                description = "Mee Sua Soup With Pork, Herbal Soup",
            ),
            PlaceCard(
                id = "kuan_zhai_wan_xiang",
                imageContentDescription = "A restaurant storefront decorated with flowers",
                locationTag = "Paya Lebar",
                title = "Kuan Zhai Wan Xiang",
                categoryTag = "Mala",
                rating = "4.1",
                reviewCountText = "114 reviews",
                description = "Unlimited Rice, Pork Belly",
            ),
        ),
        excitingEvents = listOf(
            FeatureCard(
                id = "table_tennis",
                imageContentDescription = "A table tennis paddle and ball",
                caption = "Book a table!",
                badgeText = "FREE",
            ),
            FeatureCard(
                id = "alive_artisans_market",
                imageContentDescription = "A colorful poster for the Alive Artisans Market",
                caption = "ALIVE Artisans Market",
                badgeText = "FREE",
            ),
            FeatureCard(
                id = "singa_river_festival",
                imageContentDescription = "A riverside festival lit up at night",
                caption = "Singapore River Festival",
                badgeText = "FREE",
            ),
        ),
        hotDeals = listOf(
            FeatureCard(
                id = "shiok_burger",
                imageContentDescription = "A burger and fries combo promotion",
                caption = "Shiok Burger: Enjoy 1-FOR-1 deal every day!",
            ),
            FeatureCard(
                id = "citi_smrt_card",
                imageContentDescription = "A commuter tapping a card at a fare gate",
                caption = "Citi SMRT Card: Apply & get cash back",
            ),
            FeatureCard(
                id = "morganfields_lunch",
                imageContentDescription = "A plate of chicken chop with rice",
                caption = "Morganfield's Chicken Lunch Deal",
            ),
        ),
        newsHighlights = listOf(
            MediaCard(
                id = "sia_investment",
                imageContentDescription = "A Singapore Airlines plane taxiing on the runway",
                caption = "Air India investment does not impede SIA's operational capabilities",
            ),
            MediaCard(
                id = "road_fire",
                imageContentDescription = "Firefighters attending to a roadside incident",
                caption = "About 50 evacuated as crews tackle blaze along the road",
            ),
        ),
        movies = listOf(
            MediaCard(
                id = "spider_man",
                imageContentDescription = "A superhero movie poster",
                caption = "Spider-Man: Brand New Day",
            ),
            MediaCard(
                id = "the_odyssey",
                imageContentDescription = "An epic adventure movie poster",
                caption = "The Odyssey",
            ),
            MediaCard(
                id = "kung_fu_society",
                imageContentDescription = "A martial arts comedy movie poster",
                caption = "Kung Fu Society",
            ),
        ),
    )
}
