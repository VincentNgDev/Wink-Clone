# Expand the navigation-compose lesson: the rest of the library's API surface

## What

- `.claude/lessons/13-2026-09-13-navigation-compose-vs-multi-activity.md`: added "The rest
  of the navigation-compose toolbox" section, with eight subsections each covering one
  navigation-compose feature beyond the basic `NavHost`/`composable`/`navigate` trio
  already documented — type-safe `@Serializable` routes (`composable<T>`/`toRoute<T>()`),
  nested graphs (`navigation(startDestination, route) { }`) with a ViewModel shared across
  a multi-step flow via `hiltViewModel(navController.getBackStackEntry(...))`, passing a
  result back up the stack via `SavedStateHandle`, bottom-nav-style tabs
  (`currentBackStackEntryAsState()`, `launchSingleTop`, `saveState`/`restoreState`), deep
  links (`navDeepLink`), dialog destinations (`dialog { }`), per-destination animations
  (`enterTransition`/`exitTransition`/`popEnterTransition`/`popExitTransition`), and testing
  navigation with `TestNavHostController`. Every example is written against a real, already
  no-op hook in this app (`HomeScreen`'s `onScanQrClick`/`onRewardsClick`/
  `onQuickLinkClick`/`onStationInfoClick`) rather than an unrelated hypothetical.
  - Also trimmed/updated the existing "Caveats and limitations" section so the string-route
    type-safety bullet points at the new toolbox item instead of duplicating it, and added a
    caveat about not over-reaching for nested-graph ViewModel sharing when entry-scoped
    `hiltViewModel()` is simpler.
  - Updated the lesson's intro and "Key takeaway" to reflect the expanded scope.

## Why

Requested: expand the navigation-compose lesson with the library's other navigation types
and functions beyond the basic setup, with as many concrete examples/use cases as
possible.

## Follow-up / known limitations

- Documentation-only change; none of the new examples (type-safe routes, nested graphs,
  deep links, dialogs, bottom nav, tests) are implemented in the actual app yet — they
  illustrate the pattern for when a real feature needs it (e.g. a Rewards/Profile bottom
  nav, a real Bus/Train/MRT Map flow, a station detail screen), not code that exists today.
