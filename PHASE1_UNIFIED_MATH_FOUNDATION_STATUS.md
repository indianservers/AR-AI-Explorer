# Phase 1 — Unified Math Foundation Status

Date: 2026-08-20

## Phase objective

Make every mathematical object durable, addressable, undoable, and reusable across graph, geometry, algebra/CAS, table, and spatial views. Phase 1 is a foundation phase; feature breadth and visual polish belong to later phases.

## Completed slice: persistent multi-surface 3D graphs

- `WorkspaceState.surfaceLayers` is now the authoritative production state for all 3D graph surfaces.
- Add, edit, delete, render-mode, palette, material, domain, quality, opacity, texture, glow, visibility, and clear operations flow through `UnifiedSpatialMathController` and semantic workspace history.
- Every surface becomes a stable `UniversalMathObject`; `spatial-scene` depends on every surface rather than only the primary expression.
- Additional surfaces survive module switches, undo/redo, project save/load, and JSON export.
- Workspace snapshot schema is now version 7. Schema 6 projects migrate their single legacy expression to `surface-main`.
- Removing every surface removes the corresponding canonical objects and stale spatial-scene dependencies.

## Verification completed

- Production and unit-test Kotlin compilation: pass.
- Debug APK assembly and Android instrumentation-test compilation: pass.
- Focused controller, persistence, cross-workspace, shared-object-graph, and ViewModel history tests: pass.
- Cross-workspace device tests on the API 35 phone emulator: 2/2 pass; post-run AndroidRuntime error log is empty.
- Full unit suite executed: 1,005 tests; 1,000 pass and 5 fail.
- None of the five full-suite failures exercise the new surface-layer path:
  - Three old tests assume non-empty default points/shapes/solids, while the current workspace defaults are empty.
  - Two physics/chemistry catalogue tests expect 25/26 categories but the current catalogues contain 14 each.

The full suite is therefore not yet a release-green gate; those five baseline discrepancies remain open and must be resolved deliberately rather than weakening assertions.

## Remaining Phase 1 migrations

1. Make Graph 3D camera, projection, view preset, axes, scene appearance, analysis overlays, and selections durable workspace/view-document state.
2. Make Spatial AR render all canonical surface layers; it currently consumes only `surfaceExpression` (the primary-layer compatibility mirror).
3. Route remaining direct 2D/3D construction mutations through the unified controllers. Gesture previews may stay transient, but their committed result must always create one semantic history command and update the canonical document.
4. Persist Graph 2D viewport, table/analysis presentation, folders/notes, and selection as linked view state without duplicating mathematical definitions.
5. Add device UI coverage for creating two surfaces, switching workspaces, undo/redo, process recreation, and TV focus navigation.
6. Reconcile the five baseline unit-test discrepancies, then require a clean full unit gate before Phase 1 is declared complete.

## Phase 1 exit criteria

- No production math object exists only in a composable-local collection.
- Saved/reopened projects reproduce definitions, dependencies, style, visibility, and view state.
- Undo/redo remains isolated per workspace and restores canonical and rendered projections atomically.
- Graph, geometry, CAS/algebra, table, and spatial views resolve the same object IDs.
- Phone, tablet, desktop-sized Android, and TV navigation tests pass without crashes or focus traps.
- The complete automated unit and device gate is green.
