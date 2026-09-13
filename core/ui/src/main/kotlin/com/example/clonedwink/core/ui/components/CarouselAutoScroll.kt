package com.example.clonedwink.core.ui.components

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay

/**
 * Advances [pagerState] to the next page every [intervalMs], looping back to page 0 after the
 * last one. Shared by the landing screen's onboarding carousel (`feature:landing`) and the home
 * screen's promo banner carousel (`feature:home`) — both wanted identical auto-scroll behavior,
 * just at different intervals, which is why this lives in `core:ui` instead of either feature.
 *
 * This composable renders no UI of its own; it only wires up a side effect. Call it once
 * anywhere inside the same composition as the [pagerState] it controls (see
 * `feature:landing`'s `LandingScreen.kt`'s `SlideCarousel` or `feature:home`'s
 * `HomeSections.kt`'s `PromoBannerCarousel` for example call sites).
 *
 * @param itemCount how many pages [pagerState] has — if 1 or fewer, auto-scroll is skipped
 *   entirely, since there's nowhere else to scroll to.
 */
@Composable
fun CarouselAutoScroll(pagerState: PagerState, itemCount: Int, intervalMs: Long) {
    val lifecycleOwner = LocalLifecycleOwner.current

    // Kotlin: `LaunchedEffect(itemCount, lifecycleOwner)` re-launches this coroutine only when
    // either key changes — not on every recomposition — so scrolling the pager by hand (which
    // recomposes this composable's caller) doesn't restart the auto-scroll timer.
    //
    // Android: `repeatOnLifecycle(STARTED)` is what makes this pause while the app is
    // backgrounded and resume when it's foregrounded again, instead of a bare `while(true)`
    // loop that would keep silently advancing pages off-screen (and wasting battery) the whole
    // time the user isn't even looking at the screen.
    LaunchedEffect(itemCount, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            if (itemCount <= 1) return@repeatOnLifecycle
            while (true) {
                delay(intervalMs)
                val nextPage = (pagerState.currentPage + 1) % itemCount
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }
}
