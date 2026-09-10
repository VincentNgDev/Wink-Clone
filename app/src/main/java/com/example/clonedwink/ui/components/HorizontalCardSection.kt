package com.example.clonedwink.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import com.example.clonedwink.R

// Android/Kotlin: `ui/home/HomeSections.kt`'s PlaceCardRow, FeatureCardRow, and MediaCardRow —
// plus HomeScreen.kt's partner row, before this change — were four near-identical copies of
// "a SectionHeader, a fixed gap, then a LazyRow with the same content padding and spacedBy
// arrangement." This is that shape pulled out once; each of those four now just describes what
// its *own* card looks like and hands it to this function as the `item` lambda.

/**
 * A section title (via [SectionHeader]) followed by a horizontally scrollable row of cards —
 * the shape shared by every "Friends of WINK+ / Dining Deals / Exciting Events / ..." row on
 * the home screen. Only the per-item content differs between callers, so it's the one thing
 * they supply themselves via [item].
 *
 * @param T the type of each row entry — a plain data class like [com.example.clonedwink.data.model.home.PlaceCard],
 *   or an `IndexedValue<T>` (from `list.withIndex()`) when a caller's card needs its own
 *   position in the list, e.g. to pick a placeholder color.
 * @param key a stable, unique key per item — passed straight through to [LazyRow]'s `items`,
 *   which uses it to preserve scroll position/animations correctly if [items] is reordered.
 * @param onSeeMoreClick optional "See more" link next to the title; omitted (null) means no
 *   link is shown at all — see [SectionHeader].
 * @param item the composable each row entry renders as one scrollable card.
 */
@Composable
fun <T> HorizontalCardSection(
    title: String,
    items: List<T>,
    itemSpacing: Dp,
    key: (T) -> Any,
    modifier: Modifier = Modifier,
    onSeeMoreClick: (() -> Unit)? = null,
    item: @Composable (T) -> Unit,
) {
    Column(modifier = modifier) {
        SectionHeader(title = title, onSeeMoreClick = onSeeMoreClick)
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.home_section_header_spacing)))
        LazyRow(
            contentPadding = PaddingValues(horizontal = dimensionResource(R.dimen.home_content_padding)),
            horizontalArrangement = Arrangement.spacedBy(itemSpacing),
        ) {
            items(items, key = key) { entry -> item(entry) }
        }
    }
}
