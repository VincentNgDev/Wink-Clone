# Extract reusable carousel and horizontal-section components

## What changed
- New package `app/src/main/java/com/example/clonedwink/ui/components/`:
  - `CarouselDotIndicator.kt` — the animated dot-row indicator, extracted from
    `LandingScreen.kt`'s private `PageIndicator` and the inline dot `Row` inside
    `HomeSections.kt`'s `PromoBannerCarousel`. Colors/sizes are now parameters.
  - `CarouselAutoScroll.kt` — the lifecycle-aware "advance to next page every N ms" effect,
    extracted from the identical `LaunchedEffect` blocks in both of those same two carousels.
  - `SectionHeader.kt` — moved as-is from `ui/home/HomeComponents.kt` (it was already
    screen-agnostic; it just hadn't needed to live outside `ui/home` until now).
  - `HorizontalCardSection.kt` — new generic `<T>` "title + `LazyRow`" wrapper.
- `ui/landing/LandingScreen.kt`: `SlideCarousel` now calls `CarouselAutoScroll` and
  `CarouselDotIndicator` instead of its own copies; its private `PageIndicator` is deleted.
- `ui/home/HomeSections.kt`: `PromoBannerCarousel` calls the same two shared pieces.
  `PartnerRow`, `PlaceCardRow`, `FeatureCardRow`, and `MediaCardRow` are now thin wrappers
  around `HorizontalCardSection`, each only describing its own per-item card. `PartnerRow`
  gained a `title` parameter so it can own its own header like the other three rows.
- `ui/home/HomeScreen.kt`: the manual `Column { SectionHeader(...); Spacer(...); PartnerRow(...) }`
  block for the partner row is replaced with a single `PartnerRow(title = ..., partners = ...)`
  call, matching how the other section rows are already used.
- `ui/home/HomeComponents.kt`: `SectionHeader` removed (moved out, see above).
- `app/src/main/res/values/dimens.xml`: added `carousel_dot_unselected_width`,
  `promo_indicator_selected_width`, `promo_indicator_unselected_width`, and
  `promo_indicator_dot_height` — these were previously hardcoded `Dp` literals inline, which
  is no longer allowed once they became parameters passed into a shared component (see
  `.claude/rules/coding-style.md`'s "no hardcoded dimensions" rule).

## Why
The user asked whether the onboarding carousel and the home screen's horizontal card rows
could become reusable components instead of screen-specific code. `SlideCarousel`/
`PageIndicator` (landing) and `PromoBannerCarousel` (home) were structurally identical
auto-scrolling-pager-plus-dot-indicator implementations with only styling differences, and
`PlaceCardRow`/`FeatureCardRow`/`MediaCardRow` (plus an inlined fourth copy for partners in
`HomeScreen.kt`) were four copies of the same "header + `LazyRow`" shape. Both were extracted
into `ui/components` — a new top-level View-layer package for pieces used across more than one
feature, per `.claude/rules/folder-structure.md`'s "layer first, then feature" split.

## Follow-up / known limitations
- The `HorizontalPager` itself (contentPadding, page spacing, per-page content, and the
  landing-only peek/scale `graphicsLayer` transform) was deliberately *not* merged into one
  generic pager component — the landing pager uses a flexible height and a neighbor
  scale/fade effect the promo pager doesn't have, and forcing both into one function would
  need more parameters than the duplication it would remove for just 2 call sites.
- `PlaceCardRow` still passes `onSeeMoreClick = {}` (a no-op) while `FeatureCardRow`/
  `MediaCardRow` don't pass one at all (so no "See more" link shows) — this pre-existing
  inconsistency was preserved exactly, not "fixed," since it wasn't part of what was asked.
