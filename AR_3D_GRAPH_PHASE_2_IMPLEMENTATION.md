# AR 3D Graph — Phase 2 Implementation

## Delivered

- App-side bridge to the existing explicit, implicit, and parametric surface engines.
- Immutable engine and AR render-data contracts.
- Ordered multiple-equation support with existing palette and opacity behavior.
- Compact equation, domain, and resolution controls with Plot/Update, Reset Placement, and Clear.
- Explicit graph/placement states: no graph, generating, ready, waiting for tracking, placing, placed, repositioning, failed, and cleared.
- Cancellable off-main generation; obsolete results are discarded by generation ID and the last valid graph survives invalid updates.
- GLES 3 camera background and graph renderer with depth testing, alpha blending, double-sided surfaces, axes, and grid.
- Camera-ray, 1.5 m, yaw-facing direct ARCore anchor placement with replacement.
- Tracking-aware placement and emulator-safe unsupported/install/permission states.
- EGL-context-owned deletion of camera textures and shader programs on renderer recreation/disposal.

## Files added in Phase 2

- `app/src/main/java/com/indianservers/aiexplorer/AR3DGraphEngineBridge.kt`
- `app/src/test/java/com/indianservers/aiexplorer/AR3DGraphEngineParityTest.kt`
- `ar3dgraph/.../integration/GraphEngineContract.kt`
- `ar3dgraph/.../integration/GraphEngineAdapter.kt`
- `ar3dgraph/.../ar/PlacementMath.kt`
- `ar3dgraph/.../ar/AnchorPlacementController.kt`
- `ar3dgraph/.../rendering/ARGraphCameraView.kt`
- focused adapter, parity, placement, anchor, ViewModel, renderer, and instrumented UI tests.

Phase 1 files were extended narrowly: `ARCoreSessionManager` exposes its current session; the scene/renderer shell became functional; ViewModel/UI state/screen gained Phase 2 controls and states; `ar3dgraph/build.gradle.kts` gained test dependencies.

## Existing files minimally modified

- `MainActivity.kt`: the already-added AR destination now injects `Existing3DGraphEngineBridge`; protected normal 3D Graph blocks are unchanged.
- `app/build.gradle.kts` and `settings.gradle.kts`: Phase 1 additive module dependency/inclusion only.

Other modified files shown by Git predated this phase and were preserved. Earlier user-requested Markdown deletions were not reverted. These eight Phase 2 reports are newly created because the Phase 2 request explicitly requires them.

## Phase 2 defects fixed

1. The first focused instrumented launch exposed a ViewModel factory incompatibility caused by a primary constructor with injectable defaults. The public primary constructor is now `SavedStateHandle`-only; tests use an internal secondary constructor.
2. Focused UI synchronization and semantics selectors were corrected after graph generation moved off-main.
3. Lifecycle disposal was incorrectly keyed to the newly created camera view, which could close the session during composition. `rememberUpdatedState` now supplies the latest view without restarting the lifecycle effect.
