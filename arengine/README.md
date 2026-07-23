# AI Explorer AR Engine

`arengine` is the Android library boundary for live ARCore and deterministic spatial simulation.

## Ownership

This module owns:

- AR availability, installation, permission, and lifecycle contracts;
- camera, tracking, plane, hit, anchor, light, and depth snapshots;
- world-metre, anchor, and scene-local coordinate transformations;
- renderer-neutral scene transfer contracts;
- deterministic fake/simulator runtime behavior;
- later ARCore runtime and compositor implementations.

This module does **not** evaluate mathematics. Graph, Geometry, 3D, CAS, Universal Algebra, dependency resolution, style, selection, undo, and persistence remain owned by the existing application engines.

App-side adapters will convert existing `SpatialRenderScene` and universal-document snapshots to `ArScene` without copying their algorithms.

## Coordinate convention

- AR world space is right-handed and measured in metres.
- Quaternions use `(x, y, z, w)` order.
- Matrices are column-major and OpenGL-compatible.
- Mathematical vertices remain in scene-local mathematical units.
- `metersPerMathUnit` performs the explicit unit conversion.
- The tracked anchor pose is separate from the learner-editable local transform.
- Euler angles are presentation/input helpers only; stored rotations use quaternions.

## Phase 1 status

- [x] Independent Android library module
- [x] ARCore dependency through the shared version catalog
- [x] Runtime and capability contracts
- [x] Pose, quaternion, matrix, hit, anchor, light, and depth contracts
- [x] Renderer-neutral scene contracts
- [x] Deterministic fake runtime
- [x] Contract, lifecycle, hit-ranking, anchor, coordinate, and mesh tests
- [x] Application dependency wiring

## Phase 2 status

- [x] Asynchronous and synchronous availability mapping
- [x] Optional install/update flow with one prompt per explicit attempt
- [x] Camera-permission state without simulator loss
- [x] Typed recoverable and fatal ARCore exception mapping
- [x] Session creation, feature configuration, resume, pause, and native cleanup
- [x] Horizontal and vertical plane discovery
- [x] Full translation and quaternion pose mapping
- [x] Camera view/projection matrices and tracking-failure guidance
- [x] Environmental HDR with ambient-light fallback
- [x] Optional Depth16 acquisition and safe image closure
- [x] Plane, depth, oriented-point, and instant-placement hits
- [x] Stable pending-hit tokens and explicit native-anchor ownership
- [x] Camera texture and display-geometry coordination
- [x] ARCore state-mapper regression tests

## Phase 3 status

- [x] Live spatial UI migrated from the legacy controller to `ArCoreRuntime`
- [x] Legacy app-side ARCore controller and direct app ARCore dependency removed
- [x] Actual Android display rotation propagated to ARCore
- [x] ARCore-transformed camera texture coordinates consumed by the compositor
- [x] Camera crop, rotation and aspect mapping updated when display geometry changes
- [x] Camera and spatial shader compilation/link diagnostics
- [x] Deterministic texture, buffer, shader, program and renderer cleanup
- [x] Native anchor quaternion composed into the model matrix
- [x] Environmental HDR main light, exposure and spherical-harmonic ambient input
- [x] Scene-level frustum culling
- [x] Incremental mesh uploads when the canonical scene instance changes
- [x] Frame-time and thermal-aware Ultra, High, Balanced, Low and Safety policies
- [x] Outlines and labels preserved in every quality tier
- [x] Renderer/session pause and resume ordering coordinated from the app lifecycle
- [x] Renderer policy, model-matrix, culling, lighting and app-adapter regression tests

The production runtime and compositor are the single foundation used by the Phase 4 interaction layer.

## Phase 4 status

- [x] Tracked-plane visualization and feature/depth/instant-placement reticle states
- [x] Confidence, surface type, uncertainty and mathematical scale in placement previews
- [x] Ghost scene preview before native anchor creation
- [x] Ranked plane, depth, oriented-point, instant-placement and simulator hit policy
- [x] Horizontal, vertical, arbitrary and estimated placement classification
- [x] Native anchor pose kept separate from editable local transforms
- [x] Whole-object, vertex, edge and face picking
- [x] Deterministic overlap cycling and occluded-object inspection
- [x] Translate arrows, rotation rings and scale handles
- [x] Exact position, rotation, scale and vector editing
- [x] Axis, plane, point, vertex, edge, face and grid snapping policies
- [x] Multi-selection, grouping, alignment, distribution and shared transformations
- [x] Delete, duplicate, system copy, paste, hide, isolate, lock and layer ordering
- [x] Continuous object and group gestures coalesced into one canonical undo command
- [x] Tracking-loss recovery that preserves scene, selection and mathematical state
- [x] Stylus-aware, pressure-independent precision and hover policies
- [x] Native anchors released on replacement, reset and final-object deletion

Phase 5 can now add depth occlusion, constrained surface-analysis handles and uncertainty-aware
environmental measurement without changing the direct-manipulation model.
