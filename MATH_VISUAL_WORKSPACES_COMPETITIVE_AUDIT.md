# Visual Mathematics Workspaces Competitive Audit

Audit date: 2026-08-20
Scope: Android 2D Graph, 2D Geometry, 3D Graph, 3D Geometry, shared adaptive layout, and math keyboard
Comparators: current official Desmos and GeoGebra documentation

## Executive verdict

AI Explorer has a broad local-first mathematics engine and strong direct-manipulation foundations, but it is not yet at complete Desmos or GeoGebra parity. The strongest workspace is 2D Graph. The largest product gap is 3D Graph: the shipped UI accepts only explicit `z = f(x,y)` surfaces even though implicit and parametric surface engines exist elsewhere in the repository. The largest validation gap is Smart TV: television policies and launcher declarations exist and are unit-tested, but the four visual workspaces have not been exercised end-to-end on physical Android TV hardware.

Release posture for competitive-parity claims: **NOT READY**. The app may accurately claim broad offline graphing and geometry tools, but must not claim full Desmos/GeoGebra equivalence yet.

## Evidence basis

- Production UI and engine paths inspected in `Graph2DWorkspaceUi.kt`, `MainActivity.kt`, `SurfaceInputInterpreter.kt`, `GraphProduction.kt`, `Workspace.kt`, and the adaptive layout layer.
- Focused graph/geometry/spatial/adaptive unit audit: 189/189 tests passed with no skips. The initial run exposed two stale empty-canvas fixtures; the fixtures were made explicit and the clean full rerun passed.
- Real Compose navigation smoke: 2/2 tests passed in 135.71 seconds, including 20 complete cycles through 2D Geometry, 3D Geometry, 2D Graph, 3D Graph and Solver. A clean post-run logcat scan found no matching app/test fatal exception or app ANR.
- Android manifest declares optional touchscreen and Leanback support, a TV banner, and a Leanback launcher entry.
- Television profile resolves to directional input, 24 dp overscan, 56 dp minimum targets, navigation rail, remote page scrolling, and three panes.
- Competitor facts use official documentation only:
  - Desmos Graphing Calculator: https://help.desmos.com/hc/en-us/articles/4406040715149-Getting-Started-Desmos-Graphing-Calculator
  - Desmos regressions: https://help.desmos.com/hc/en-us/articles/4406972958733-Regressions
  - Desmos tables: https://help.desmos.com/hc/en-us/articles/4405489674381-Tables
  - Desmos sliders: https://help.desmos.com/hc/en-us/articles/202529069-Sliders-and-Movable-Points
  - Desmos 3D: https://help.desmos.com/hc/en-us/articles/19736835727885-Extending-from-2D-to-3D
  - GeoGebra suite overview: https://help.geogebra.org/hc/en-us/articles/10445800380957-GeoGebra-Tools-and-Features-An-Overview
  - GeoGebra 3D view: https://geogebra.github.io/docs/manual/en/3D_Graphics_View/
  - GeoGebra 3D tools: https://geogebra.github.io/docs/manual/en/tools/3D_Graphics_Tools/

## Workspace comparison

| Workspace | AI Explorer strengths verified in source | Where Desmos / GeoGebra remain better | Priority |
|---|---|---|---|
| 2D Graph | Explicit, implicit, polar, parametric, piecewise, inequalities, domain restrictions, sequences, recurrences, lists, regressions, sliders, tables, trace, POIs, tangent/normal, derivative, integral, area, CSV import, JSON/project sharing, image export | Desmos has more familiar expression syntax, table/spreadsheet paste behavior, regression `~` workflow with residual plots/statistics, mature audio trace, and highly polished labels/style controls. AI Explorer's accessibility action announces text; it is not equivalent to continuous graph sonification. | P1 |
| 2D Geometry | Dynamic points and dependencies, intersections, centers, line/ray/segment/vector, polygons, circles/arcs/ellipse, constraints, transformations, grouping, protocol playback, macros, traces, keyboard movement | GeoGebra has a deeper dependency-aware construction toolset: general conics, robust locus, configurable/custom tools, richer exact construction input, comprehensive transformations, and a mature construction protocol. AI Explorer presets such as rotate 30 degrees or reflect X are less general than object-driven constructions. | P1 |
| 3D Graph | Multiple explicit surface layers, surface/wireframe/contour/slice/gradient/bounds/trace modes, view presets, parameters, differential insight and GPU scene compilation | Production input is explicitly restricted to `z=f(x,y)`. Desmos supports points, equations in x/y/z, inequalities/solids, parametric curves and surfaces, sliders, restrictions, and surface intersections. GeoGebra adds planes, lines, circles/conics, solids, transformations, intersection tools and algebra-linked constructions. | P0 |
| 3D Geometry | 20 solid types, points/vectors, object/vertex/edge/face selection, transforms, snapping concepts, grouping/alignment, Boolean mesh operations, sections, measurements, nets/revolve, OBJ/STL and measurement export, camera presets | GeoGebra is a dynamic 3D construction system: lines, rays, planes, perpendicular/parallel constructions, conics, point-on-object dependencies, surface intersections, measurement constructions, transformations and exact command input. AI Explorer is currently closer to a solid modeller/measurement lab than a complete dynamic geometry system. | P0 |

## Smart TV assessment

Already present:

- Leanback launcher, TV banner, optional touchscreen declaration.
- Directional-input profile and focus groups.
- 24 dp overscan protection, 56 dp minimum target policy, navigation rail and remote scrolling.
- TV-specific workspace offsets in Solver, 2D Graph, 2D Geometry, 3D Geometry and 3D Graph.
- Math keyboard Backspace and Enter now use the adaptive minimum target: 48 dp on touch profiles and 56 dp on TV.

Still required before saying “matches on Smart TVs”:

1. Run all four workspaces on at least 1080p and 4K Android TV hardware, not only a phone emulator or profile resolver test.
2. Prove every control is D-pad reachable, has visible focus, activates with Enter/centre, and returns focus after drawers/dialogs.
3. Verify no canvas gesture is touch-only. Provide remote alternatives for orbit, pan, zoom, point movement, box/lasso selection, slider adjustment and section-plane editing.
4. Verify keyboard popup bounds and focus order at TV distance; ensure the expression editor never requires a software IME.
5. Capture screenshots with overscan enabled and at large font/display scaling.
6. Performance-test 3D meshes on representative low-end TV GPUs; current renderer policy does not identify Television as a spatial device class.

## Prioritized fixes

### P0 — parity and correctness blockers

1. Wire `SurfaceDefinition3D.Implicit` and `SurfaceDefinition3D.Parametric` into the real 3D Graph add/edit flow. Add 3D points, parametric curves, planes and inequality solids as first-class layer types.
2. Add 3D intersections: plane/plane, plane/surface, surface/surface, line/plane and solid sections, with visible derived objects and diagnostics.
3. Unify 3D Graph and 3D Geometry on the universal document. At present advanced surface types exist in shared/catalog engines while the production screen constructs only an explicit surface.
4. Add dynamic 3D construction dependencies (point on object, line through points, plane through points, perpendicular/parallel, midpoint, attach/detach) rather than only independent points/vectors/solids.
5. Complete physical Android TV D-pad, focus, overscan and GPU validation.

### P1 — high-value competitive gaps

1. Add Desmos-compatible regression entry (`~`), residual plots, coefficient uncertainty/statistics, model templates and direct table-to-regression flow.
2. Add spreadsheet-style table paste/import with multiple columns, draggable points and linked computed columns.
3. Implement actual multigraph audio trace/sonification. The engine catalog contains sonification models, but the visible “Read description” action only sends an accessibility announcement.
4. Extend 2D Geometry with parabola, hyperbola, general conic, robust locus and exact object-driven transformations.
5. Persist/reuse custom geometry macros and make construction protocol edits branch-safe.
6. Standardize import/export across every visual workspace, including corrupted/newer schema diagnostics and round-trip UI tests.

### P2 — polish and learning advantage

1. Replace internal/competitive copy such as “Desmos-style rows” with task-focused language.
2. Add discoverable syntax help, inline autocomplete and error spans for every supported graph form.
3. Improve object style parity: label placement, line/point variants, opacity, fill, domain endpoints, folders and batch styling.
4. Add cross-workspace “send to” actions with previews and undo: Graph ↔ Geometry ↔ Solver/CAS ↔ 3D.
5. Build guided investigations around the app's differentiators: verified steps, uncertainty, construction provenance, AR and offline learning.

## Crash/closure finding from Solver validation

The observed app closures during the Solver golden run were mostly deliberate test batch restarts/force-stops. Logcat did record one instrumentation ANR and one fatal exception in `AndroidJUnitRunner` while disconnecting `UiAutomation` after interruption. No ordinary production-thread fatal exception was found in the inspected log. The runner ANR remains a test-infrastructure defect and should not be described as a production crash without a non-instrumented reproduction.

## Exit criteria for a parity-ready release

- Every P0 item has UI-level automated coverage, not engine-only coverage.
- Two clean end-to-end cycles for the visual-workspace corpus and no instrumentation ANR.
- Phone, tablet/foldable and physical 1080p/4K Android TV matrices pass.
- Import/export/share round trips pass for all four workspaces.
- Accessibility includes D-pad focus, TalkBack semantics, large text, contrast, motion and real graph audio testing.
- Performance budgets are measured on low/mid/high Android devices and representative TV GPUs.
