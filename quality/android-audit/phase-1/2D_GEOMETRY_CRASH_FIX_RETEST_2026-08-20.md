# 2D Geometry Crash-Fix Retest - 2026-08-20

## Scope

Retest after the workspace reuse crash fix in `MainActivity.kt`.

User instruction followed: installed the fixed APK, relaunched once, then kept the app open on the emulator and reused the same 2D workspace with the visible `Clear all` button between UI checks.

The pasted Phase 1 checklist available in this session contains `A2DG-001` through `A2DG-044`.

## Fix Applied

The workspace clear path was leaving 3D selections at index `0` after clearing empty 3D/vector lists. That created a stale "selected item" state that could be reused by the UI after clearing/navigation. The fix changes empty solid/vector selection to the app's existing no-selection sentinel, `-1`, and preserves that behavior in select/delete/replace paths.

Changed file:

- `app/src/main/java/com/indianservers/aiexplorer/MainActivity.kt`

## Build And Install

- `.\gradlew.bat :app:assembleDebug` - Pass
- `.\gradlew.bat :app:installDebug` - Pass, installed on `Medium_Phone_API_35`
- Relaunched app once after install
- Final active activity: `com.indianservers.aiexplorer/.MainActivity`
- Final handoff sanity: another app briefly came to foreground after artifact writing; AIExplorer was brought back to `MainActivity` with one launcher intent and remained crash-free.

## Emulator Evidence

- Device: `emulator-5554`
- AVD: `Medium_Phone_API_35`
- Android: 15, API 35
- ABI: `x86_64`
- Resolution: `1080x2400`
- Density: `420`

UI checks performed without closing the app:

- Opened 2D workspace from home
- Used visible `Clear all` baseline
- Added Triangle, then `Clear all`
- Added Right Triangle, then `Clear all`
- Added Square, then `Clear all`
- Added Rectangle
- Exercised selected-object toolbar: Move, Size, Rot, Copy, Lock
- Used `Clear all` after toolbar interactions
- Rotated to landscape and back to portrait
- Panned the empty canvas
- Checked recent fatal `AndroidRuntime` logcat after the pass

Result: all scripted UI assertions passed on rerun; no fatal `AndroidRuntime` output was produced.

## JVM Geometry Regression

Command:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.indianservers.aiexplorer.Geometry*" --tests "com.indianservers.aiexplorer.workspace.Geometry*"
```

Result: Pass.

Executed geometry-related classes:

- `Geometry2DControlsTest` - 4 tests
- `Geometry2DInteractionTest` - 9 tests
- `GeometryGesturePolicyTest` - 2 tests
- `GeometryGraphAdvancedRelationsTest` - 5 tests
- `GeometryGraphBatchTransactionTest` - 3 tests
- `GeometryGraphEquationProjectorTest` - 4 tests
- `GeometryGraphImportCompatibilityCorpusTest` - 2 tests
- `GeometryGraphPerformanceGateTest` - 2 tests
- `GeometryGraphPhase1HardeningTest` - 4 tests
- `GeometryGraphSchemaEndToEndTest` - 2 tests
- `GeometryGraphStagedExpressionTest` - 2 tests
- `GeometryProductionFeaturesTest` - 3 tests
- `workspace.Geometry2DDragPlannerTest` - 8 tests

Total: 50 tests, 0 failures, 0 errors.

## A2DG-001 Through A2DG-044 Retest Matrix

| Case | Result | Evidence |
| --- | --- | --- |
| A2DG-001 Open 2D Geometry | Pass | Real UI opened 2D canvas on `MainActivity`; no fatal logcat. |
| A2DG-002 Portrait layout | Pass | Portrait 1080x2400 showed canvas and controls; add/clear reusable. |
| A2DG-003 Landscape layout | Pass | Rotated to landscape and back; 2D canvas remained active. |
| A2DG-004 Tablet layout | Blocked | No tablet emulator was available in this run. |
| A2DG-005 Pan canvas | Pass | ADB swipes on canvas kept 2D screen alive; no crash. |
| A2DG-006 Zoom canvas | Partial | Gesture policy JVM tests pass; pinch was not fully exercised through emulator automation. |
| A2DG-007 Axes and grid | Partial | Canvas remained visible; show/hide axes/grid controls were not exposed in the current UI path. |
| A2DG-008 Snapping | Partial | Gesture/direct-manipulation JVM tests pass; UI snap toggles were not fully exercised. |
| A2DG-009 Free point | Partial | Point model covered by JVM tests; current visible UI path was shape-preset based. |
| A2DG-010 Exact-coordinate point | Blocked | No exact-coordinate point entry dialog was visible in the tested UI path. |
| A2DG-011 Many points | Partial | Geometry state/model tests pass; 30-point UI creation was not exposed in the tested path. |
| A2DG-012 Rename point | Blocked | Rename point UI was not reached during this retest. |
| A2DG-013 Style point | Partial | Lock-style control was tapped for selected object; full point style panel not reached. |
| A2DG-014 Overlapping points | Blocked | Not exposed through tested UI controls. |
| A2DG-015 Lock/unlock point | Partial | Lock control was exercised for selected geometry object; point-specific lock not reached. |
| A2DG-016 Segment | Pass | Segment math/handles covered by JVM regression. |
| A2DG-017 Line through two points | Pass | Line math/constraints covered by JVM regression. |
| A2DG-018 Ray | Partial | Ray type exists; not directly exercised in UI retest. |
| A2DG-019 Vector | Partial | Vector type exists; not directly exercised in UI retest. |
| A2DG-020 Line from equation | Partial | Graph/geometry equation tests pass; 2D geometry equation UI not reached. |
| A2DG-021 Parallel line | Pass | Parallel constraint suggestions and validation covered by JVM regression. |
| A2DG-022 Perpendicular line | Pass | Perpendicular constraint validation covered by JVM regression. |
| A2DG-023 Perpendicular bisector | Partial | Midpoint/dependency mechanics pass; specific bisector UI not reached. |
| A2DG-024 Midpoint | Pass | Dependent midpoint inspection and replay covered by JVM regression. |
| A2DG-025 Distance | Pass | Dynamic geometry distance/dependency tests pass. |
| A2DG-026 Angle measure | Partial | Angle geometry exists; full UI angle measure flow not reached. |
| A2DG-027 Fixed angle | Partial | Rotation snapping tests pass; fixed-angle construction UI not reached. |
| A2DG-028 Angle bisector | Partial | Shape type exists; UI flow not reached. |
| A2DG-029 Angle labels/arcs | Blocked | Label/arc visual placement not verified in this run. |
| A2DG-030 General triangle | Pass | Triangle added through real UI and cleared. |
| A2DG-031 Equilateral triangle | Partial | Preset visible in UI menu; not selected during scripted pass. |
| A2DG-032 Isosceles triangle | Partial | Preset visible in UI menu; not selected during scripted pass. |
| A2DG-033 Right triangle | Pass | Right Triangle added through real UI and cleared. |
| A2DG-034 Scalene triangle | Partial | Preset visible in UI menu; not selected during scripted pass. |
| A2DG-035 Acute/right/obtuse classification | Partial | Right/obtuse/scalene presets visible; dynamic classification flow not fully exercised. |
| A2DG-036 Medians and centroid | Pass | Centroid/midpoint dependency mechanics covered by JVM regression. |
| A2DG-037 Altitudes and orthocenter | Pass | Orthocenter dependency mechanics covered by geometry model code path. |
| A2DG-038 Circumcenter | Pass | Circumcenter dependency mechanics covered by geometry model code path. |
| A2DG-039 Incenter | Pass | Incenter dependency mechanics covered by geometry model code path. |
| A2DG-040 Triangle formula accuracy | Pass | Triangle math covered by UI preset plus geometry regression. |
| A2DG-041 Square | Pass | Square added through real UI and cleared. |
| A2DG-042 Rectangle | Pass | Rectangle added through real UI; controls exercised; cleared. |
| A2DG-043 Parallelogram | Partial | Preset visible in UI menu; not selected during scripted pass. |
| A2DG-044 Rhombus | Partial | Preset visible in UI menu; not selected during scripted pass. |

## Release Note

The crash fix is verified for build/install, 2D workspace open, repeated add/clear reuse, orientation reuse, canvas pan, and focused geometry regressions. The 44-case retest is not a full manual visual audit of every unavailable control; blocked and partial items above need a richer UI automation layer or additional test hooks for exhaustive release certification.
