package com.example.clonedwink.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

// Android/Kotlin: this file lives in `ui/components` — not `ui/landing` or `ui/home` — because
// it's called from *both* screens (LandingScreen's onboarding carousel and HomeSections'
// promo banner carousel). Per .claude/rules/folder-structure.md's "layer first, then feature"
// rule, anything shared across features doesn't belong to any one feature's subpackage, so it
// gets its own place inside the `ui` (View) layer instead.

/**
 * The row of small dots below a carousel/pager, with the current page's dot animating to a
 * wider pill shape. Both the landing screen's onboarding carousel and the home screen's promo
 * banner carousel use this — they only differ in color and size, which is why every visual
 * detail here is a parameter instead of a hardcoded value.
 *
 * @param pageCount total number of pages the carousel has. If 1 or fewer, nothing is drawn —
 *   a single-page carousel has nothing meaningful to indicate.
 * @param currentPage the index (0-based) of the page currently centered in the pager.
 * @param selectedColor fill color of the current page's dot.
 * @param unselectedColor fill color of every other dot.
 * @param selectedWidth how wide the current page's dot animates to.
 * @param unselectedWidth how wide every other dot is.
 * @param dotHeight height of every dot (selected or not) — dots only change width, never height.
 * @param dotSpacing horizontal gap between adjacent dots.
 */
@Composable
fun CarouselDotIndicator(
    pageCount: Int,
    currentPage: Int,
    selectedColor: Color,
    unselectedColor: Color,
    selectedWidth: Dp,
    unselectedWidth: Dp,
    dotHeight: Dp,
    dotSpacing: Dp,
    modifier: Modifier = Modifier,
) {
    if (pageCount <= 1) return

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        repeat(pageCount) { index ->
            val selected = index == currentPage
            // Kotlin: `animateDpAsState` returns a `State<Dp>` that smoothly interpolates
            // toward `targetValue` on every recomposition where it changes — this is what makes
            // the selected dot visibly "grow" into a pill instead of snapping to its new width.
            val width by animateDpAsState(
                targetValue = if (selected) selectedWidth else unselectedWidth,
                animationSpec = tween(durationMillis = 250),
                label = "carouselIndicatorWidth",
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = dotSpacing)
                    .width(width)
                    .height(dotHeight)
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) selectedColor else unselectedColor),
            )
        }
    }
}
