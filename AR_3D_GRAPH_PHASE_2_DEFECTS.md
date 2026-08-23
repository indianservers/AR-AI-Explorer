# AR 3D Graph — Phase 2 Defects

## Fixed in Phase 2

| ID | Defect | Root cause | Resolution | Verification |
|---|---|---|---|---|
| AR2-001 | Focused AR screen test and manual entry crashed while creating the ViewModel. | Injectable default parameters on the primary constructor were not compatible with the Android saved-state ViewModel factory. | Kept a `SavedStateHandle`-only public primary constructor and moved injection to an internal secondary constructor. | Focused instrumented AR UI test passed; rebuilt app opened safely. |
| AR2-002 | Focused UI assertions raced off-main graph generation. | The test expected immediate state after Plot. | Added deterministic test execution and state-aware selectors without making production generation synchronous. | Focused UI test passed. |
| AR2-003 | Camera-view creation could restart the lifecycle effect and dispose the AR session. | `cameraView` was a `DisposableEffect` key. | Removed it from the key set and used `rememberUpdatedState` for lifecycle callbacks/disposal. | Final compile/unit/focused instrumentation rerun recorded in the test report. |
| AR2-004 | First final focused rerun found no Compose hierarchy. | The prior 49-minute app run left stale instrumentation processes after an ADB/UTP crash. | Force-stopped the stale app/test runners without clearing app data and reran. | Two consecutive focused runs passed 1/1. |

## Pre-existing failures observed

The complete app instrumented suite reports failures in existing title, curriculum, regex, integrated-concept, interactive-reference, learning-workspace, teacher-classroom, and Solver matrix-step assertions. It also suffered an ADB reset/UTP `NoClassDefFoundError` during result teardown. These conditions are outside the Phase 2 implementation; protected modules were not changed to hide them. See the regression report for counts.

## Open Phase 2 defects

No known code defect remains in the Phase 2 adapter, state, ray, or anchor abstractions. Real AR rendering and anchor tracking cannot be certified on the available non-AR emulator. Strict phase closure is also blocked by the already-red app-wide connected baseline and the ADB/UTP teardown failure; neither is represented as a pass.
