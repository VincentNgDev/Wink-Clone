package com.example.clonedwink.feature.home.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Kotlin: this module's own `R` (home_content_padding/home_see_more) plus core:ui's `R`
// (aliased `CoreUiR`) for the brand colors defined there — see feature:landing's
// LandingScreen.kt for the fuller explanation of why a file split across modules needs both.
import com.example.clonedwink.core.ui.R as CoreUiR
import com.example.clonedwink.feature.home.R

// Android/Kotlin: this file lives in `feature:home`'s own `ui/components` package, not
// `core:ui` — even though it looks generic (a title + optional link), it's only ever used by
// the home screen's rows (via HorizontalCardSection.kt below), and it reaches for
// home-specific resources (R.dimen.home_content_padding, R.string.home_see_more). Putting it in
// `core:ui` would leak home's resource IDs into a module every feature depends on; see
// folder-structure.md's "only truly shared code belongs in the shared module" spirit, applied
// across Gradle modules instead of just packages.

/**
 * A section title with an optional trailing "See more" link — used above every horizontally
 * scrollable row on the home screen, via [HorizontalCardSection].
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
            color = colorResource(CoreUiR.color.on_surface),
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
                    color = colorResource(CoreUiR.color.brand_pink),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = colorResource(CoreUiR.color.brand_pink),
                    modifier = Modifier
                        .padding(start = 2.dp)
                        .size(16.dp),
                )
            }
        }
    }
}
