# Add learning-oriented comments to landing/carousel code

## What

- Added extensive explanatory comments to every Kotlin file in the landing carousel
  feature: `LandingActivity`, `CarouselAdapter`, `LandingViewModel`, `LandingUiState`,
  `CarouselSlide`, `CarouselRepository`, `DefaultCarouselRepository`, and
  `LandingViewModelTest`. Comments cover Android lifecycle mechanics (Activity/ViewModel
  lifecycle, `onCreate`, `repeatOnLifecycle`, `viewModelScope`/`lifecycleScope`,
  `onCreateViewHolder`/`onBindViewHolder`) and Kotlin syntax (`data class`, delegated
  properties, scope/Elvis operators, `suspend`, coroutine builders, `StateFlow`,
  `runCatching`, companion objects, backtick-named test functions, etc.).
- Added a handful of `<!-- -->` comments to `activity_landing.xml` and
  `item_carousel_slide.xml` explaining ConstraintLayout constraints, ViewPager2's
  runtime-wired adapter, the TabLayout-as-dot-indicator setup, and FrameLayout stacking.
- Added a **Learning mode: comments** section to `CLAUDE.md` documenting that this
  project intentionally overrides the terse-comment guidance in
  `.claude/rules/coding-style.md` while the user is learning Android/Kotlin, so future
  work keeps commenting generously instead of reverting to the default terse style.

## Why

The user is learning Android development with Kotlin through this project and asked for
comments that explain the Android lifecycle and Kotlin syntax as they read the code, not
just what each line does.

## Follow-up / known limitations

- Only the landing/carousel feature (the only feature that exists so far) has been
  annotated this heavily; other files predate this preference and should be commented as
  thoroughly when they're next touched, per the new CLAUDE.md section.
- Re-ran `gradlew.bat testDebugUnitTest` after the comment pass — build succeeded, all
  tests still pass (comments are non-functional, but this was verified rather than
  assumed).
