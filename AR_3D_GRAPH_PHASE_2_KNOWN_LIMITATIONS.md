# AR 3D Graph — Phase 2 Known Limitations

## Environment limitations

The only available emulator is Android API 35 x86_64 with a virtual-scene camera but without Google Play Services for AR (`com.google.ar.core`). It validates installation, unsupported/install UI, equation generation, navigation, rotation, background/resume, and crash safety, but cannot create a real ARCore session or anchor. Ray, pose, replacement, cleanup, and repetition are validated with deterministic unit abstractions. Physical AR tracking is not claimed.

## Engine capability boundaries

- Explicit, implicit, and parametric surfaces are reused because the normal 3D Graph workspace supports them.
- Parametric curves are not added because the current surface engine does not expose them through this workspace flow.
- Labels are not rendered in AR Phase 2; axes and grid are converted to GL lines. Adding text labels is Phase 3 visual/accessibility work.
- The renderer preserves engine palette/transparency data but intentionally uses a simple unlit colour shader in Phase 2; final lighting/material polish is deferred.
- Fixed graph scale is `0.1 m` per math unit. User scaling is a Phase 3 gesture.

## Deferred exactly to Phase 3

- one-finger graph rotation;
- pinch-to-scale and two-finger rotation;
- gesture refinements;
- advanced performance and GPU-resource optimization;
- export and sharing;
- final visual polish and AR labels;
- accessibility completion;
- full physical-device/ARCore device matrix;
- real-world tracking, interruption, relocalization, and anchor stability certification;
- release-readiness validation.

No plane, wall, floor, table, hit-test, or depth-placement fallback will be introduced to address these limitations.

## Validation qualification

All isolated Phase 2 tests and two consecutive focused emulator cycles pass. The repository's full app connected suite is not green at baseline, and the latest broad run lost its final UTP report during an ADB reset after completing the 1,200-row Solver dataset. Consequently, the strict “two consecutive complete all-app regression cycles pass” condition is not claimed.
