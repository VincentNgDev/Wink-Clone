package com.example.clonedwink.feature.landing.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import coil3.compose.AsyncImage
import com.example.clonedwink.core.model.CarouselSlide
import com.example.clonedwink.core.ui.components.CarouselAutoScroll
import com.example.clonedwink.core.ui.components.CarouselDotIndicator
import com.example.clonedwink.core.ui.theme.ClonedWinkTheme
// Kotlin: this module has its own `R` (generated from feature/landing/src/main/res) for
// landing_*/carousel_slide_*/carousel_*-dimen resources, but the brand colors and the one
// dot-spacing dimen this screen also needs live in `core:ui`'s resources instead — AGP gives
// each module a *non-transitive* R class (it only contains resources that module itself
// declares), so a file that needs both has to import both R classes and can't let them collide
// under the same name. `import ... as CoreUiR` renames the second import so `R.string.foo`
// (this module's own) and `CoreUiR.color.bar` (core:ui's) are unambiguous side by side.
import com.example.clonedwink.core.ui.R as CoreUiR
import com.example.clonedwink.feature.landing.R
import com.example.clonedwink.feature.landing.viewmodel.LandingUiState
import kotlin.math.absoluteValue

// Android/Kotlin: this whole file is the "View" half of the landing screen's MVVM split, but
// as @Composable *functions* instead of an Activity + XML layout. `app`'s WinkNavHost owns the
// LandingViewModel (obtained via `hiltViewModel()`, scoped to this route's back-stack entry)
// and is the only thing that touches it directly; everything below only ever receives a plain
// LandingUiState snapshot and event lambdas, exactly like folder-structure.md's "a ui/ file
// should never import from data/ directly — it should only talk to its ViewModel" rule, just
// expressed with functions/parameters instead of a class hierarchy.

private const val AUTO_SCROLL_INTERVAL_MS = 4000L
private const val MIN_PEEK_SCALE = 0.85f
private const val MIN_PEEK_ALPHA = 0.6f

/**
 * The full landing screen: a vibrant brand-gradient background with soft blurred "blobs" for
 * depth, a frosted-glass onboarding carousel, a matching dot indicator, and a pill CTA button —
 * see .claude/references/wink-*.png for the visual style this is matching (bold pink brand
 * color, rounded type, glass/card surfaces floating over color).
 *
 * @param onGetStartedClick fired when the CTA button is tapped; WinkNavHost's Landing
 *   destination decides what that means (navigating to Home, popping Landing off the back
 *   stack), keeping that decision in the View layer rather than baking navigation into this
 *   stateless composable.
 * @param onErrorShown fired once, the first time [uiState] reports [LandingUiState.hasError] —
 *   WinkNavHost's Landing destination uses it to show a Toast.
 */
@Composable
fun LandingScreen(
    uiState: LandingUiState,
    onGetStartedClick: () -> Unit,
    onErrorShown: () -> Unit,
) {
    // Kotlin: `LaunchedEffect(uiState.hasError)` re-runs its block only when `hasError`
    // *changes* value (Compose compares against the previous composition's key) — so this
    // fires once per loading failure, not on every recomposition, mirroring how the old
    // Activity's `if (state.hasError)` inside a StateFlow collector only reacted to new values.
    LaunchedEffect(uiState.hasError) {
        if (uiState.hasError) onErrorShown()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Android: a 135°-diagonal two-stop gradient — the same brand_purple -> brand_pink
            // pairing bg_carousel_slide_1.xml already used, just drawn as a Compose Brush
            // instead of an XML <shape> so it can fill the whole screen behind the glass UI.
            .background(
                Brush.linearGradient(
                    colors = listOf(colorResource(CoreUiR.color.brand_purple), colorResource(CoreUiR.color.brand_pink)),
                ),
            ),
    ) {
        BackgroundBlobs()

        Column(
            modifier = Modifier
                // Android: `fillMaxHeight()` here (not `fillMaxSize()`) deliberately leaves
                // width alone — `fillMaxSize` would pin both min *and* max width to the full
                // available width, and a `widthIn(max = ...)` applied afterwards can't shrink
                // below an already-pinned min, so the cap below would silently do nothing.
                .fillMaxHeight()
                // Android: `WindowInsets.safeDrawing` covers the status bar, nav bar, and
                // display cutouts. MainActivity calls `enableEdgeToEdge()` once for the whole
                // app, which lets our gradient draw *behind* the system bars for an immersive
                // edge-to-edge look; this padding then keeps the actual title/CTA content clear
                // of them.
                .windowInsetsPadding(WindowInsets.safeDrawing)
                // Android: this screen is designed phone-first (a single narrow column of
                // cards). Without a cap, the same layout on a tablet/foldable stretches the
                // carousel card into an oddly wide, short bar — `widthIn(max = ...)` caps how
                // wide this column is allowed to grow, and the `fillMaxWidth()` right after
                // grows it to exactly that (now-capped) width instead of Column's normal
                // wrap-content default, so the standard "cap then fill" pair only takes effect
                // once the available width is genuinely wider than a phone.
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = dimensionResource(R.dimen.landing_content_padding)),
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.landing_title),
                color = colorResource(CoreUiR.color.white),
                style = MaterialTheme.typography.headlineLarge,
            )
            Text(
                text = stringResource(R.string.landing_tagline),
                color = colorResource(CoreUiR.color.white).copy(alpha = 0.85f),
                fontSize = MaterialTheme.typography.titleMedium.fontSize,
                modifier = Modifier.padding(top = 4.dp),
            )

            Spacer(modifier = Modifier.height(28.dp))

            if (uiState.isLoading && uiState.slides.isEmpty()) {
                // Android: only the very first load (before DefaultCarouselRepository has
                // returned anything) hits this branch — see LandingUiState's isLoading=true
                // default. A centered spinner over the gradient stands in for the carousel
                // until the first uiState with slides arrives.
                //
                // Kotlin: `Modifier.weight(1f)` here (a `ColumnScope` member, usable because
                // this whole branch sits directly inside the outer `Column { ... }` lambda
                // above) claims all the vertical space this Column isn't using for the title,
                // spacers, and the button below — same space the loaded SlideCarousel branch
                // claims below, so the spinner and the real carousel occupy the same region
                // instead of the spinner being a small fixed box that leaves a gap.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = colorResource(CoreUiR.color.white))
                }
            } else if (uiState.slides.isNotEmpty()) {
                SlideCarousel(slides = uiState.slides, modifier = Modifier.weight(1f))
            }

            // Android: a fixed gap (not the old `Modifier.weight(1f)` spacer) so the button
            // sits the *same* distance below the page indicator as the indicator sits below the
            // carousel itself — see SlideCarousel below, which uses this same dimen for that gap.
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.carousel_indicator_spacing)))

            GetStartedButton(onClick = onGetStartedClick)
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.landing_content_padding)))
        }
    }
}

// Kotlin: `@Composable private fun` — same rules as any other composable, just not exported
// outside this file. Splitting the screen into small private composables (this one, the
// carousel, the card, the button, the indicator) keeps each recomposition scope small: Compose
// can skip re-running a function whose inputs didn't change, so smaller functions mean cheaper
// updates when e.g. only the pager's current page changes.
@Composable
private fun BackgroundBlobs() {
    // Android: `Modifier.blur(...)` needs a real API 31+ RenderEffect to actually soften edges;
    // on older devices (this app's minSdk is 24) it silently draws the shape unblurred instead
    // of crashing, so these still read fine as flat translucent color washes there. It has to
    // come *last* in the chain, after `clip`/`background` — blur softens whatever was already
    // drawn, so blurring first and clipping after would crop away the soft falloff at a hard
    // circular edge instead of letting it fade out into a glow.
    Box(
        modifier = Modifier
            .offset(x = (-60).dp, y = (-40).dp)
            .size(dimensionResource(R.dimen.blob_size_large))
            .clip(CircleShape)
            .background(colorResource(CoreUiR.color.blob_teal))
            .blur(80.dp),
    )
    Box(
        modifier = Modifier
            .offset(x = 220.dp, y = 520.dp)
            .size(dimensionResource(R.dimen.blob_size_small))
            .clip(CircleShape)
            .background(colorResource(CoreUiR.color.blob_purple))
            .blur(60.dp),
    )
}

@Composable
private fun SlideCarousel(slides: List<CarouselSlide>, modifier: Modifier = Modifier) {
    // Kotlin: `pageCount = { slides.size }` is a *lambda*, not a plain Int — rememberPagerState
    // re-reads it on every layout pass so the pager stays correct if `slides` grows/shrinks
    // later (e.g. a real network-backed CarouselRepository refreshing), without needing to
    // manually recreate the PagerState.
    val pagerState = rememberPagerState(pageCount = { slides.size })

    // Android: the pause-while-backgrounded auto-advance behavior — shared with the home
    // screen's promo banner carousel, so it lives in `core:ui`'s CarouselAutoScroll.kt instead
    // of being written out here a second time.
    CarouselAutoScroll(pagerState = pagerState, itemCount = slides.size, intervalMs = AUTO_SCROLL_INTERVAL_MS)

    // Kotlin: wrapping the pager + indicator in their own `Column(modifier)` — rather than
    // emitting them directly into the caller's Column, like this function used to — is what
    // lets LandingScreen hand in `Modifier.weight(1f)` from *its* Column scope and have that
    // weight apply to this whole group as one flexible-height block.
    Column(modifier = modifier) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                // Android: `weight(1f)` instead of a fixed `carousel_height` dimen — the pager
                // now grows to fill whatever space this Column's `modifier` weight was given,
                // so the carousel's actual on-screen height follows the available room between
                // the tagline and the button instead of a number picked by hand.
                .weight(1f),
            contentPadding = PaddingValues(horizontal = dimensionResource(R.dimen.carousel_peek_padding)),
            pageSpacing = dimensionResource(R.dimen.carousel_page_margin),
        ) { page ->
            GlassSlideCard(
                slide = slides[page],
                modifier = Modifier
                    .fillMaxSize()
                    // Android/Kotlin: `pagerState.currentPageOffsetFraction` changes on every
                    // scroll frame, so it's read *inside* this graphicsLayer lambda rather than as
                    // a `val` in the composable body above — graphicsLayer's block runs during the
                    // draw phase, not recomposition, so scrolling only re-triggers drawing instead
                    // of re-running this whole function on every frame. The formula itself
                    // reproduces the old ViewPager2 PageTransformer's `position` value — 0f for the
                    // fully centered page, growing toward 1f for neighbors as the user swipes — for
                    // the same "shrink + fade the peeking neighbors" effect.
                    .graphicsLayer {
                        val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                            .absoluteValue
                            .coerceIn(0f, 1f)
                        scaleY = lerp(MIN_PEEK_SCALE, 1f, 1f - pageOffset)
                        alpha = lerp(MIN_PEEK_ALPHA, 1f, 1f - pageOffset)
                    },
            )
        }

        // Android: this uses the same `carousel_indicator_spacing` dimen as the gap
        // LandingScreen puts between this whole carousel block and the Get Started button below
        // it, so both gaps read as one consistent rhythm instead of two hand-picked values.
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.carousel_indicator_spacing)))
        CarouselDotIndicator(
            pageCount = slides.size,
            currentPage = pagerState.currentPage,
            selectedColor = colorResource(CoreUiR.color.white),
            unselectedColor = colorResource(CoreUiR.color.glass_indicator_track),
            selectedWidth = dimensionResource(R.dimen.carousel_dot_bound_width),
            unselectedWidth = dimensionResource(R.dimen.carousel_dot_unselected_width),
            dotHeight = dimensionResource(R.dimen.carousel_dot_height),
            dotSpacing = dimensionResource(CoreUiR.dimen.carousel_dot_spacing),
        )
    }
}

@Composable
private fun GlassSlideCard(slide: CarouselSlide, modifier: Modifier = Modifier) {
    val cardShape = RoundedCornerShape(dimensionResource(R.dimen.carousel_card_corner_radius))
    Box(
        modifier = modifier
            // Android: a real shadow (not just a translucent fill) is what makes this card
            // read as "lifted" off the gradient background — the same purpose
            // app:cardElevation served on the old MaterialCardView, just via Modifier.shadow.
            .shadow(elevation = dimensionResource(R.dimen.carousel_card_elevation), shape = cardShape)
            .clip(cardShape),
    ) {
        // Kotlin: copying the two nullable properties into local vals first, then branching on
        // those, isn't just style — Kotlin only smart-casts a nullable *property* to its
        // non-null type when the compiler can see its whole class in this same module; since
        // CarouselSlide now lives in `core:model`, a different module, `slide.imageRes != null`
        // followed by `slide.imageRes` (expecting a non-null Int) fails to compile here. A
        // local `val` copy has no such restriction, because the compiler tracks *that specific
        // variable's* nullability within this function regardless of which module declared the
        // original property.
        //
        // Kotlin: a `when` used as an *expression* — each branch's Unit-returning Composable
        // call is the branch's "value", and since every branch is covered (imageUrl, imageRes,
        // and the `else` fallback), no `else -> {}` is needed beyond the actual fallback case.
        // DefaultCarouselRepository's hardcoded slides set neither field today, so they always
        // hit the gradient fallback; a real network-backed slide with a photo URL, or a bundled
        // vector/bitmap asset, renders through the other two branches unchanged.
        val imageUrl = slide.imageUrl
        val imageRes = slide.imageRes
        when {
            imageUrl != null -> AsyncImage(
                model = imageUrl,
                contentDescription = slide.imageContentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            imageRes != null -> Image(
                // Android: `painterResource` only understands VectorDrawables and rasterized
                // formats (PNG/JPG/WEBP) — an XML `<shape>` gradient drawable throws at
                // runtime, which is exactly why this app no longer routes its placeholder
                // gradient art through a drawable resource at all (see the brandGradient()
                // fallback below instead).
                painter = painterResource(id = imageRes),
                contentDescription = slide.imageContentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            else -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(brandGradientForSlide(slide.id)))
                    .semantics { contentDescription = slide.imageContentDescription },
            )
        }

        // Android: a bottom-to-top black scrim so white title/subtitle text stays readable
        // against whatever's behind it, same purpose as the old scrim_bottom_gradient.xml
        // drawable — just expressed as a Brush instead of a separate drawable resource.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, colorResource(CoreUiR.color.carousel_scrim_opaque)),
                    ),
                ),
        )

        // Android: the frosted-glass icon badge — translucent white fill + a slightly-more-
        // opaque white border is the standard glassmorphism "pane of glass" recipe (see the
        // glass_fill/glass_border tokens in core:ui's colors.xml), floated over the vivid
        // gradient art.
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(dimensionResource(R.dimen.carousel_text_padding))
                .size(dimensionResource(R.dimen.glass_badge_size))
                .clip(CircleShape)
                .background(colorResource(CoreUiR.color.glass_fill))
                .border(
                    width = dimensionResource(R.dimen.glass_border_width),
                    color = colorResource(CoreUiR.color.glass_border),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = iconForSlide(slide.id),
                // Android: this icon is purely decorative reinforcement of the slide's theme —
                // slide.imageContentDescription (set on the Image above) already describes the
                // slide for accessibility, so a null description here avoids TalkBack
                // announcing the same thing twice.
                contentDescription = null,
                tint = colorResource(CoreUiR.color.white),
                modifier = Modifier.size(dimensionResource(R.dimen.glass_badge_icon_size)),
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(dimensionResource(R.dimen.carousel_text_padding)),
        ) {
            Text(
                text = slide.title,
                color = colorResource(CoreUiR.color.white),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = slide.subtitle,
                color = colorResource(CoreUiR.color.white).copy(alpha = 0.9f),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun iconForSlide(id: String): ImageVector = when (id) {
    "meet" -> Icons.Filled.Favorite
    "chat" -> Icons.AutoMirrored.Filled.Chat
    "date" -> Icons.Filled.CalendarMonth
    else -> Icons.Filled.Favorite
}

// Android: the fallback background for a slide with no real artwork — a diagonal two-stop
// brand gradient, one color pair per slide id, matching the palette the old bg_carousel_slide_
// *.xml drawables used (brand_purple for "meet", brand_pink for "chat", brand_teal for "date")
// before those were removed for being an XML drawable type Compose's painterResource can't load
// (see GlassSlideCard above).
@Composable
private fun brandGradientForSlide(id: String): List<Color> = when (id) {
    "meet" -> listOf(colorResource(CoreUiR.color.brand_purple), colorResource(CoreUiR.color.brand_purple_dark))
    "chat" -> listOf(colorResource(CoreUiR.color.brand_pink), colorResource(CoreUiR.color.brand_pink_dark))
    "date" -> listOf(colorResource(CoreUiR.color.brand_teal), colorResource(CoreUiR.color.brand_teal_dark))
    else -> listOf(colorResource(CoreUiR.color.brand_purple), colorResource(CoreUiR.color.brand_purple_dark))
}

@Composable
private fun GetStartedButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(R.dimen.cta_height))
            .clip(RoundedCornerShape(dimensionResource(R.dimen.cta_corner_radius)))
            // Android: a solid white pill reads as the "glass surface" call-to-action against
            // the saturated gradient background — the highest-contrast, most tappable element
            // on the screen, matching how Wink+'s own screenshots keep primary actions on a
            // plain white surface rather than another translucent layer.
            .background(colorResource(CoreUiR.color.white))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.landing_cta_get_started),
                color = colorResource(CoreUiR.color.brand_purple),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = colorResource(CoreUiR.color.brand_purple),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

// Android: `@Preview` renders this composable straight inside Android Studio's design pane
// without running the app on a device/emulator — handy for iterating on the glass/gradient
// visuals quickly. It needs its own fixed sample LandingUiState since there's no real
// LandingViewModel available at preview time.
@Preview(showBackground = true)
@Composable
private fun LandingScreenPreview() {
    ClonedWinkTheme {
        LandingScreen(
            uiState = LandingUiState(
                isLoading = false,
                slides = listOf(
                    CarouselSlide(
                        id = "meet",
                        title = "Meet new people nearby",
                        subtitle = "Discover matches based on shared interests",
                        imageContentDescription = "Two people chatting over coffee",
                    ),
                    CarouselSlide(
                        id = "chat",
                        title = "Chat without the awkwardness",
                        subtitle = "Icebreakers that actually start conversations",
                        imageContentDescription = "Chat bubbles between two phones",
                    ),
                ),
            ),
            onGetStartedClick = {},
            onErrorShown = {},
        )
    }
}
