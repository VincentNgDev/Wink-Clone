# Landing carousel fills height down to the Get Started button

## What changed
- `app/src/main/res/values/dimens.xml`: removed the now-unused fixed `carousel_height` (240dp)
  dimen; added `carousel_indicator_spacing` (16dp), reused in two places.
- `app/src/main/java/com/example/clonedwink/ui/landing/LandingScreen.kt`:
  - `SlideCarousel` now takes a `modifier: Modifier = Modifier`, wraps its `HorizontalPager` +
    `PageIndicator` in a `Column(modifier)`, and gives the pager `Modifier.weight(1f)` instead
    of a fixed height.
  - `LandingScreen` passes `Modifier.weight(1f)` to `SlideCarousel` (and to the loading-state
    spinner `Box`) instead of using a `Spacer(Modifier.weight(1f))` between the carousel and the
    button.
  - The gap between the carousel and the page indicator, and the gap between the indicator and
    the Get Started button, both now use the same `carousel_indicator_spacing` dimen (16dp).

## Why
The carousel had a fixed height (240dp) and a flexible spacer below it, leaving a large blank
gap between the page indicator and the Get Started button. The user wanted the carousel itself
to grow and fill that space, with the indicator-to-button gap matching the carousel-to-indicator
gap, instead of one fixed carousel + one large leftover gap.

## Follow-up / known limitations
- No behavior change to auto-scroll, peek/scale transform, or the dot indicator's animation —
  only the vertical sizing/spacing changed.
- The user separately asked (as a question, not yet actioned) whether the carousel and the
  home screen's horizontal scrollable rows could be extracted into shared reusable components —
  `SlideCarousel`/`PageIndicator` in `LandingScreen.kt` and `PromoBannerCarousel` in
  `HomeSections.kt` are structurally near-duplicates (auto-scrolling `HorizontalPager` + dot
  indicator) that would be a good candidate for that extraction if the user wants it done.
