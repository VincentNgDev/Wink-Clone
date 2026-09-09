package com.example.clonedwink.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ContactSupport
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.clonedwink.R
import com.example.clonedwink.data.model.home.QuickLinkItem

// Android/Kotlin: small, reusable pieces shared across the home screen — a quick-link tile
// and its "More" bottom sheet, the loyalty points card, the station selector bar, the bottom
// nav bar, section headers, and the brand-gradient placeholder used by every card that has no
// real photo yet. ui/home/HomeSections.kt builds on top of these for each horizontally
// scrollable row; ui/home/HomeScreen.kt assembles the whole screen from both files.

/**
 * The fallback "photo" for any card with no real image — a two-stop brand gradient cycled by
 * [index] across purple/pink/teal, the same idea as LandingScreen's `brandGradientForSlide`
 * but keyed by list position instead of a fixed slide id, since home screen cards come from
 * lists of arbitrary length.
 */
@Composable
fun placeholderBrandGradient(index: Int): List<Color> {
    val palette = listOf(
        colorResource(R.color.brand_purple) to colorResource(R.color.brand_purple_dark),
        colorResource(R.color.brand_pink) to colorResource(R.color.brand_pink_dark),
        colorResource(R.color.brand_teal) to colorResource(R.color.brand_teal_dark),
    )
    // Kotlin: `Math.floorMod`, not plain `%` — some callers (e.g. HomeSections.kt's
    // PlaceCardView, keying off `place.id.hashCode()`) can pass a negative Int, and Kotlin's
    // `%` keeps the sign of its left operand (`-1 % 3 == -1`), which would index the palette
    // list out of bounds. `floorMod` always returns a non-negative result in `[0, palette.size)`.
    val paletteIndex = Math.floorMod(index, palette.size)
    val (start, end) = palette[paletteIndex]
    return listOf(start, end)
}

/** Maps a [QuickLinkItem.id] to the icon shown on its tile — see LandingScreen's `iconForSlide` for why this mapping lives in the View layer instead of the data model. */
fun quickLinkIconFor(id: String): ImageVector = when (id) {
    "bus" -> Icons.Filled.DirectionsBus
    "train" -> Icons.Filled.Train
    "mrt_map" -> Icons.Filled.Map
    "more" -> Icons.Filled.Apps
    "taxi" -> Icons.Filled.LocalTaxi
    "parking" -> Icons.Filled.LocalParking
    "vouchers" -> Icons.Filled.ConfirmationNumber
    "nearby_deals" -> Icons.Filled.LocalOffer
    "feedback" -> Icons.Filled.Feedback
    "contact_us" -> Icons.AutoMirrored.Filled.ContactSupport
    else -> Icons.Filled.Apps
}

/**
 * A section title with an optional trailing "See more" link, used above every horizontally
 * scrollable row on the home screen.
 */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, onSeeMoreClick: (() -> Unit)? = null) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(R.dimen.home_content_padding)),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = colorResource(R.color.on_surface),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        if (onSeeMoreClick != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = onSeeMoreClick),
            ) {
                Text(
                    text = stringResource(R.string.home_see_more),
                    color = colorResource(R.color.brand_pink),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = colorResource(R.color.brand_pink),
                    modifier = Modifier
                        .padding(start = 2.dp)
                        .size(16.dp),
                )
            }
        }
    }
}

/** One Bus/Train/MRT Map/More tile: a rounded square icon chip with its label underneath. */
@Composable
fun QuickLinkTile(item: QuickLinkItem, onClick: (QuickLinkItem) -> Unit, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clickable { onClick(item) },
    ) {
        Box(
            modifier = Modifier
                .size(dimensionResource(R.dimen.quick_link_tile_size))
                .clip(RoundedCornerShape(dimensionResource(R.dimen.quick_link_corner_radius)))
                .background(colorResource(R.color.chip_background)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = quickLinkIconFor(item.id),
                // Android: decorative only — the Text label right below already announces
                // this tile's purpose to TalkBack, so a real description here would just
                // repeat it (same reasoning as the icon badges in LandingScreen's carousel).
                contentDescription = null,
                tint = colorResource(R.color.brand_purple),
                modifier = Modifier.size(dimensionResource(R.dimen.quick_link_icon_size)),
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = item.label,
            color = colorResource(R.color.on_surface),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

// Kotlin: `@OptIn(ExperimentalMaterial3Api::class)` — ModalBottomSheet/rememberModalBottomSheetState
// are still marked experimental in this Material 3 release, same reasoning as the
// `@OptIn(ExperimentalCoroutinesApi::class)` seen on the ViewModel tests: without this
// annotation, the compiler refuses to compile a call site that uses an experimental API.
/**
 * The bottom sheet the "More" quick-link tile opens, showing the rest of the app's shortcuts
 * in a simple grid-like flow of tiles.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MoreQuickLinksSheet(items: List<QuickLinkItem>, onItemClick: (QuickLinkItem) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(
                text = stringResource(R.string.home_more_sheet_title),
                color = colorResource(R.color.on_surface),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            // Kotlin: `chunked(4)` splits the flat list into sub-lists of (up to) 4 items each
            // — a lightweight way to lay out a grid using plain Row/Column instead of pulling
            // in LazyVerticalGrid for what's a short, bounded list.
            items.chunked(4).forEach { rowItems ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    rowItems.forEach { item ->
                        QuickLinkTile(
                            item = item,
                            onClick = {
                                onItemClick(it)
                                onDismiss()
                            },
                        )
                    }
                }
            }
        }
    }
}

/**
 * The white "My WINK+ Points" card that overlaps the bottom edge of the pink gradient header —
 * a points readout on top, and a Scan QR / Rewards row underneath separated by a thin divider.
 */
@Composable
fun LoyaltyCard(
    points: Int,
    onScanQrClick: () -> Unit,
    onRewardsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimensionResource(R.dimen.loyalty_card_corner_radius)))
            .background(colorResource(R.color.surface))
            .padding(dimensionResource(R.dimen.loyalty_card_padding)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.home_loyalty_title),
                color = colorResource(R.color.on_surface),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            // Kotlin: `"%,d".format(points)` — the standard library's `String.format` applied
            // to a single Int, using the `,` grouping flag to add thousands separators
            // (2450 -> "2,450"), matching the reference screenshot's points readout style.
            Text(
                text = "%,d".format(points),
                color = colorResource(R.color.on_surface),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 14.dp),
            color = colorResource(R.color.on_surface_variant).copy(alpha = 0.15f),
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            LoyaltyActionLink(
                icon = Icons.Filled.QrCodeScanner,
                label = stringResource(R.string.home_scan_qr),
                onClick = onScanQrClick,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(dimensionResource(R.dimen.loyalty_divider_height))
                    .background(colorResource(R.color.on_surface_variant).copy(alpha = 0.15f)),
            )
            LoyaltyActionLink(
                icon = Icons.Filled.CardGiftcard,
                label = stringResource(R.string.home_rewards),
                onClick = onRewardsClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun LoyaltyActionLink(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colorResource(R.color.brand_pink),
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = colorResource(R.color.brand_pink),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** The "current station / Station Info" strip below the promo banner carousel. */
@Composable
fun StationSelectorBar(stationName: String, onStationInfoClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(R.dimen.home_content_padding))
            .clip(RoundedCornerShape(dimensionResource(R.dimen.station_bar_corner_radius)))
            .background(colorResource(R.color.chip_background))
            .padding(dimensionResource(R.dimen.station_bar_padding)),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.NearMe,
                contentDescription = null,
                tint = colorResource(R.color.brand_pink),
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stationName,
                color = colorResource(R.color.on_surface),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Icon(
                imageVector = Icons.Filled.ExpandMore,
                contentDescription = stringResource(R.string.home_change_station_content_description),
                tint = colorResource(R.color.on_surface_variant),
                modifier = Modifier
                    .padding(start = 2.dp)
                    .size(18.dp),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable(onClick = onStationInfoClick),
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = colorResource(R.color.brand_pink),
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.home_station_info),
                color = colorResource(R.color.brand_pink),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

// Kotlin: an `enum class` — a fixed, closed set of named constants. `HomeBottomNavBar` below
// uses this instead of a raw String/Int for `selectedTab` so an invalid tab value ("profil"
// typo'd, or 4 out of range) simply can't compile, unlike a stringly-typed alternative.
enum class HomeBottomNavTab { HOME, SEARCH, PROFILE }

/**
 * The Home / Search / Profile bottom bar. Only Home is wired to anything real in this app so
 * far — Search/Profile clicks are no-ops the caller can fill in once those screens exist,
 * mirroring how LandingActivity's `onGetStartedClick` used to be a no-op before HomeActivity
 * existed.
 */
@Composable
fun HomeBottomNavBar(selectedTab: HomeBottomNavTab, onTabSelected: (HomeBottomNavTab) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(dimensionResource(R.dimen.bottom_nav_height))
            .background(colorResource(R.color.surface))
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BottomNavItem(
            icon = Icons.Filled.Home,
            label = stringResource(R.string.home_bottom_nav_home),
            selected = selectedTab == HomeBottomNavTab.HOME,
            onClick = { onTabSelected(HomeBottomNavTab.HOME) },
        )
        BottomNavItem(
            icon = Icons.Filled.Search,
            label = stringResource(R.string.home_bottom_nav_search),
            selected = selectedTab == HomeBottomNavTab.SEARCH,
            onClick = { onTabSelected(HomeBottomNavTab.SEARCH) },
        )
        BottomNavItem(
            icon = Icons.Filled.Person,
            label = stringResource(R.string.home_bottom_nav_profile),
            selected = selectedTab == HomeBottomNavTab.PROFILE,
            onClick = { onTabSelected(HomeBottomNavTab.PROFILE) },
        )
    }
}

@Composable
private fun BottomNavItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(dimensionResource(R.dimen.bottom_nav_pill_corner_radius)))
            // Android: a translucent pink pill behind the icon+label is what makes the
            // selected tab (Home, by default) read as "active" — `.copy(alpha = ...)` on a
            // Compose Color tints it without needing a second dedicated color resource.
            .background(if (selected) colorResource(R.color.brand_pink).copy(alpha = 0.12f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) colorResource(R.color.brand_pink) else colorResource(R.color.on_surface_variant),
            modifier = Modifier.size(dimensionResource(R.dimen.bottom_nav_icon_size)),
        )
        if (selected) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = colorResource(R.color.brand_pink),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
