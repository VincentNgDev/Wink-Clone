# Number the lesson files and add a standalone AndroidManifest lesson

## What

- Renamed all `.claude/lessons/*.md` files to add a two-digit study-order prefix (e.g.
  `2026-09-08-app-entry-point.md` → `01-2026-09-08-app-entry-point.md`), so they sort
  into a suggested reading sequence.
- Inserted a new lesson, `02-2026-09-08-android-manifest.md`, dedicated to
  `AndroidManifest.xml` — the `<manifest>`/`<application>`/`<activity>`/`<intent-filter>`
  structure, what each attribute in this project's manifest controls, the (unused)
  `Application` class, and other component types (`<service>`, `<receiver>`,
  `<provider>`, `<uses-permission>`) not yet present but worth knowing about.
- Shifted the remaining lessons up one slot to make room (lifecycle → 03, drawing-the-ui
  → 04, MVVM → 05, carousel walkthrough → 06, navigation → 07) and updated every
  `[[wiki-link]]` cross-reference across all seven files to match the new filenames.

Final sequence:
```
01-2026-09-08-app-entry-point.md
02-2026-09-08-android-manifest.md
03-2026-09-08-android-lifecycle.md
04-2026-09-08-drawing-the-ui.md
05-2026-09-08-mvvm-architecture.md
06-2026-09-08-carousel-slider-walkthrough.md
07-2026-09-08-navigation.md
```

## Why

The user asked to number the lesson files for sequential study, and separately asked
for a standalone lesson explaining `AndroidManifest.xml` (previously only touched on
briefly inside the entry-point lesson) — placed right after the entry-point lesson since
the two are closely related.

## Follow-up / known limitations

- If more lessons are added later, keep the two-digit prefix scheme consistent and
  renumber/re-link as needed, the same way this change did.
