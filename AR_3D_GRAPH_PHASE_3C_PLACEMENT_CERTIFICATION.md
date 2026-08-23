# AR 3D Graph Phase 3C — Placement Certification

Date: 2026-08-21

Deterministic result: **PASS**. Live AR result: **BLOCKED**.

The certification exercises 900 combinations: nine canonical screen positions × four phone/tablet portrait/landscape viewports × five camera translations × five invertible projection scale pairs. Additional tests cover 100 distributed taps, translated camera/view coordinates, viewport insets, distinct corners/edges, singular matrices, invalid viewport coordinates and varied pitched rays.

For every successful case the asserted formula is:

`position = camera + normalize(ray) × 1.5 metres`

Measured formula-coordinate deviation was exactly 0 in the representation used by the test. Camera-to-placement distance stayed within 0.00001 m of 1.5 m; directions were finite, normalized and in front of the camera. Insets were subtracted before ray construction. Invalid viewports and matrices failed in a controlled result.

Code audit confirms the only route is screen coordinate → inverse view/projection camera ray → fixed 1.5 m world pose → direct anchor. Search found no `hitTest`, `PlaneRenderer`, `DepthPoint`, `InstantPlacementPoint` or `Trackable` use. `PlaneFindingMode`, `InstantPlacementMode` and `DepthMode` are explicitly disabled.

The available pure tests include horizontal and vertical ray headings plus camera translations and projection changes; a real device is still required to certify five physical camera yaw positions, five physical camera pitch positions, sensor roll, keyboard/overlay coordinates, changing camera viewport state and measured real-world distance. Initial camera-facing quaternion behavior is unit-tested and roll is excluded, but live non-mirrored labelled-axis orientation is blocked.
