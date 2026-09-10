# Lesson: Reusable Compose components — what, when, and where

This lesson answers three questions asked while working on the landing screen's carousel:
does every UI piece have to live in the same `ui` folder as the screen that uses it, can
the carousel/horizontal-scrolling sections become reusable, and how do you decide what's
*actually* worth extracting versus over-engineering a one-off. [[06-2026-09-08-carousel-slider-walkthrough]]
covered how the carousel works; this lesson covers what changed when part of it became
shared, and [[09-2026-09-09-home-screen-feature-and-lists]] covered `LazyRow`/`items`,
which the new shared section component builds on.

## A composable is just a function — folders don't constrain who can call it

In an XML-View Android app, a layout file is tied to the screen that inflates it. In
Jetpack Compose (which this project uses — see
[[08-2026-09-09-jetpack-compose-and-lifecycle]]), a `@Composable` function is called the
same way any other Kotlin function is: by importing it and writing its name. **Nothing
about a composable's file location changes whether another file can call it** — a
`private fun` is scoped to its file, but a plain `fun` (no `private`) is visible to any
file that imports it, project-wide, same as any other public/internal function.

So "does it have to be in the same `ui` folder?" — no, not for the language to allow it.
The folder *does* still communicate something to a human reader, though: it's how
`.claude/rules/folder-structure.md`'s "layer first, then feature" rule stays legible as
the app grows. That rule is about **where you put things so they're easy to find**, not
about what Kotlin will let you call.

## Where a shared component goes: a new `ui/components/` package

Before this change, `ui/landing/` and `ui/home/` each had their own screen-specific
subpackage — see [[10-2026-09-10-project-and-folder-structure]]'s package tour. Neither
was the right home for something used by *both* screens. The fix: a third package,
`ui/components/`, sitting alongside `ui/landing/` and `ui/home/` — still inside the View
layer (`ui/`), just not owned by any one feature:

```
ui/
├── components/          # NEW — cross-feature View pieces
│   ├── CarouselDotIndicator.kt
│   ├── CarouselAutoScroll.kt
│   ├── SectionHeader.kt
│   └── HorizontalCardSection.kt
├── landing/
│   └── LandingScreen.kt   # calls into ui/components for its carousel's dots + auto-scroll
└── home/
    ├── HomeScreen.kt
    ├── HomeComponents.kt   # still home-only pieces: LoyaltyCard, bottom nav, etc.
    └── HomeSections.kt     # calls into ui/components for its rows + promo carousel
```

`SectionHeader` is the clearest example of "this was always reusable, it just hadn't been
asked to be yet": it only ever took a title and an optional click handler — nothing
home-specific about it — but it lived in `ui/home/HomeComponents.kt` because nothing
outside `ui/home` needed it. The moment `HorizontalCardSection` (a cross-feature
component) needed to call it, leaving it in `ui/home` would mean a shared component
reaching *back into* a specific screen's package to find a dependency — backwards from
how the layers are supposed to point. Moving it to `ui/components` fixed that direction.

## Recognizing what's actually reusable: duplication, not "might be needed later"

The temptation with "make it reusable" is to over-engineer a generic component before
anything actually needs it twice — but `CLAUDE.md`'s project-wide guidance (echoing
`.claude/rules/coding-style.md`) is explicit: don't build abstractions for hypothetical
future use, only for what the task in front of you actually requires. The signal to
extract something isn't "this *could* be used elsewhere" — it's **"this already *is* the
same code, written out twice."** Two real examples found in this codebase:

1. **The dot indicator + auto-scroll effect.** `LandingScreen.kt`'s `SlideCarousel` (an
   onboarding carousel) and `HomeSections.kt`'s `PromoBannerCarousel` (a promo banner
   carousel) each had their own `LaunchedEffect { repeatOnLifecycle(STARTED) { ... } }`
   block that auto-advanced a `HorizontalPager` — identical logic, just a different
   interval and item count — and each had its own `Row { repeat(pageCount) { ... } }` dot
   row — identical shape, different colors and sizes. That's not "might be reusable
   someday"; it's the *same* code sitting in two files, which is exactly what
   `CarouselAutoScroll` and `CarouselDotIndicator` now share.
2. **The "header + horizontal row of cards" shape.** `PlaceCardRow`, `FeatureCardRow`, and
   `MediaCardRow` in `HomeSections.kt` — plus a fourth copy inlined directly in
   `HomeScreen.kt` for the partner row — were each: a `SectionHeader`, a fixed-height
   `Spacer`, then a `LazyRow` with the same content padding and `Arrangement.spacedBy`.
   Four copies of one skeleton, differing only in what each per-item card looked like.
   `HorizontalCardSection` is that skeleton, written once.

What *wasn't* merged, deliberately: the `HorizontalPager` inside each carousel. The
landing one uses a flexible height (`Modifier.weight(1f)`, so it grows to fill the space
down to the Get Started button — see the previous change) and a `graphicsLayer` scale/fade
transform on the peeking neighbor cards; the promo one uses a fixed height and no
transform. Forcing both into one generic pager component, for only 2 call sites, would
need *more* parameters than the duplication it removes — that's the over-engineering line
this project's coding style explicitly warns against. When in doubt: extract the part
that's identical, leave the part that differs where it is.

## The mechanics: how a generic Compose component takes "any item, any card"

`HorizontalCardSection` needs to work for `PlaceCard`, `FeatureCard`, `MediaCard`, and even
`IndexedValue<PartnerItem>` (see below) — four unrelated types. Kotlin generics plus a
composable *slot* parameter make that possible without any shared base class:

```kotlin
@Composable
fun <T> HorizontalCardSection(
    title: String,
    items: List<T>,
    itemSpacing: Dp,
    key: (T) -> Any,
    modifier: Modifier = Modifier,
    onSeeMoreClick: (() -> Unit)? = null,
    item: @Composable (T) -> Unit,   // <- the "slot": caller decides what one card looks like
) {
    Column(modifier = modifier) {
        SectionHeader(title = title, onSeeMoreClick = onSeeMoreClick)
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.home_section_header_spacing)))
        LazyRow(/* shared padding/arrangement */) {
            items(items, key = key) { entry -> item(entry) }
        }
    }
}
```

- **`fun <T>`** — a *generic* function. `T` is a placeholder type the caller fills in at
  the call site; the function body never needs to know what `T` actually is, only that it
  gets a `List<T>` and a way to turn one `T` into a key.
- **`item: @Composable (T) -> Unit`** — a *trailing lambda parameter* whose type is itself
  a composable function. This is the "slot" — `HorizontalCardSection` owns the
  header/spacing/row *shape*, but has no opinion on what a card looks like. Each caller
  passes its own rendering as this lambda, exactly the same trailing-lambda syntax
  `LazyRow`'s own `items(...) { }` already uses.

A caller like `PlaceCardRow` becomes a one-liner around this:

```kotlin
@Composable
fun PlaceCardRow(title: String, places: List<PlaceCard>, modifier: Modifier = Modifier) {
    HorizontalCardSection(
        title = title,
        items = places,
        itemSpacing = dimensionResource(R.dimen.place_card_spacing),
        key = { it.id },
        modifier = modifier,
        onSeeMoreClick = {},
    ) { place -> PlaceCardView(place = place) }   // the slot, filled in
}
```

`PartnerRow` needed one extra trick: `PartnerCard` picks its placeholder gradient color by
the item's *position* in the list, but `HorizontalCardSection`'s `item` slot only receives
the item itself, not its index. Rather than adding an `index` parameter that only one
caller needs, `PartnerRow` pairs each item with its index *before* calling
`HorizontalCardSection`, using the standard library's `List<T>.withIndex()`:

```kotlin
HorizontalCardSection(
    items = partners.withIndex().toList(),   // List<IndexedValue<PartnerItem>>
    key = { it.value.id },                    // IndexedValue.value / .index destructure
) { (index, partner) -> PartnerCard(partner = partner, colorIndex = index) }
```

`withIndex()` turns `List<PartnerItem>` into `List<IndexedValue<PartnerItem>>`, so `T` is
just `IndexedValue<PartnerItem>` at this call site — `HorizontalCardSection` never needed
to change to support this, because generics mean it never had an opinion on what `T` was
in the first place. `FeatureCardRow` and `MediaCardRow` do the same thing, which also
quietly fixed a small pre-existing inefficiency: they used to look up each item's position
with `features.indexOf(feature)` — a full re-scan of the list for every single item
rendered — where `withIndex()` gets the position for free while building the list once.

## Key takeaway

A composable's folder is a hint for humans, not a boundary Kotlin enforces — so "is this
reusable" is never blocked by where a screen's code happens to live. The real question is
narrower: is this *already* duplicated code (extract it), or does it just *seem* like it
might be useful elsewhere (leave it, per this project's "don't build for hypothetical
future requirements" rule)? When something does qualify, Kotlin generics (`fun <T>`) plus
a composable slot parameter (`@Composable (T) -> Unit`) are usually enough to share the
*shape* of a UI piece while leaving each caller free to decide what its own content looks
like — no shared base class or inheritance required.
