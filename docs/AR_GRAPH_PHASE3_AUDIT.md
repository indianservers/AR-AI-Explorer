# AR Space / Graph 3D — Phase 3 audit

Date: 2026-10-04

Status: implementation and emulator acceptance delivered; **physical AR acceptance remains pending**. Do not interpret emulator results as verification of anchoring, depth occlusion, real hands, thermal behavior, or walking around a construction.

## Implementation

The active Native Android AR Space screen extends the Phase 1/2 CAD, picking, transform, dependency, constraint, history and ARCore systems. Other AR tools remain available through the same studio selector. No replacement architecture or separate graph demo was introduced.

- **Slice planes:** use actual editable CAD planes and their transforms. Touch/hand/precision editing and snapping share the existing controls. Analysis supports an explicit selected plane, real mesh intersections, contour perimeter and in-plane dimensions. Playback scans along its normal, stops when the sheet closes or another edit takes ownership, and records one undo operation.
- **Intersections:** triangle/plane intersection operates on the same edited and transformed geometry used for picking. It handles sphere, cylinder, cone, cuboid, explicit, parametric and implicit meshes. Displayed section dimensions are numerical mesh measurements.
- **Conics:** circle/ellipse/parabola/hyperbola classification uses the quadratic form restricted to the actual world plane, including nonuniform object scaling. Apex intersections are identified as degenerate. Classification refers to the infinite cone extension; displayed contours are clipped by the actual finite solid and its cap.
- **Surface analysis:** existing equation-editor explicit layers and CAD function surfaces remain authoritative. Numerical partials, gradient, world normal, tangent plane equation, scalar value, local direction, directional derivative and angle update from the expression and transforms. Pick a point by tapping/pinching the analyzed surface or enter exact coordinates. Free mesh edits do not falsely claim an analytic differential. Corners/discontinuities report an unavailable derivative rather than inventing a tangent.
- **Vectors:** gradient, direction and unit normal have arrowheads. Gradient visibility, normalization and visual scale are separate controls. Direction can be entered as local XYZ or taken from a CAD vector.
- **Fields:** expression-based vector fields have bounds, density, normalization and arrow scale, with a bounded generator and adaptive displayed density. Scalar slices use evaluated F(x,y,z), a color range, slice position and interpolated level contours. No decorative particle flow or unsupported model-export button is exposed.
- **Implicit surfaces:** the existing background numerical mesher handles F=0 and equations with both sides. Sphere, torus and algebraic-surface tests check finite topology and equation residuals.
- **Quality:** AUTO is the default; LOW/MEDIUM/HIGH are available. Actual GL frame cadence, Android thermal status, power saver, scene size, distance and manipulation select displayed detail with hysteresis. Visual clustering leaves source geometry, picking, constraints and calculations intact; locked objects still receive visual quality reduction.
- **Depth:** the toggle checks ARCore hardware capabilities, rather than requiring an already-enabled depth frame. It drives actual runtime depth and the existing occlusion shader. Unsupported devices explain the fallback; thermal quality may temporarily suspend environmental depth.
- **History:** the timeline includes both applied and redo commands. Jumping uses the existing command operations; a new edit correctly clears the abandoned future.
- **Save/restore:** the existing durable project store and full project codec preserve equations, dependencies, meshes, constraints, object transforms, fields, measurements, analysis settings and world scale. Restore clears the session anchor ID and requests new placement.
- **Share:** full project archive, equation/object/measurement report and PNG are available through the Android share chooser. AR images use PixelCopy on the actual GL surface; mathematical fallback images capture the scene after dismissing the sheet and hiding editing UI. Opening a chooser does not send a file to another person.
- **Presentation:** immersive system bars, minimal reveal control, optional edit lock, optional object labels and a selected live measurement. Labels face the camera in screen space, scale within readable bounds and reject overlapping placements.
- **Activities:** XYZ points, vectors, planes, sphere, cone sections, gradient, tangent, vector field and implicit surface add real editable objects with a single undo command. They reuse the scene engine.
- **Measurements/snapping:** point-plane and point-line distance, line-plane/plane-plane/normal angles, signed vector projection, section spans, line-plane and plane-plane intersections, surface projection, and existing grid/point/vertex/edge/midpoint snaps. Existing analytic sphere tangency relationships and mesh constraint solving are retained.
- **Hands/accessibility:** exact entity hover stability, no manipulation through an open sheet or locked presentation, temporary POINT/PINCH/GRAB/SCALE/ROTATE feedback, readable controls, a persistent sheet Close button and 48 dp hand toggle. Every authored operation has touch/numeric alternatives.

## Thread and memory review

Native source meshing, CAD construction and analysis run on Dispatchers.Default with immutable captured workspace snapshots. The unused second legacy surface sample and unused main-thread GPU packing were removed. GL buffer operations remain on the GL thread. Transform-only updates retain the existing buffer reuse path.

Frame delivery to Compose is coalesced to prevent depth snapshots accumulating while the main thread is busy. Renderer release drops scene/plan/frame references. Mesh/topology/LOD caches and command history are bounded. MediaPipe ownership remains gated by live camera and the hands toggle, with pause/OFF closing the detector. PNG bitmaps are released in finally blocks; PixelCopy completion is awaited before recycling its bitmap, including cancellation.

## Verification

- 96 app JVM checks: 23 Phase 3, 39 Phase 2, 8 Phase 1, 4 AR Space, 3 graph appearance, 6 spatial authoring, 9 engine parity, 4 navigation.
- 170 arengine JVM checks; no failures/errors. Total: **266**.
- Debug app and instrumentation APKs build successfully.
- Emulator UI matrix: API 35 (emulator-5556) and API 37 (emulator-5554): **9 scenarios passed on each, 18 UI test executions total**. Final run logs, build log, JVM reports and screenshots are in `docs/qa/ar-phase3-evidence`. Final Logcat snapshots contain no FATAL EXCEPTION or ANR in entries. Presentation screenshots were visually inspected on both devices; Android fullscreen education is dismissed by the UI test. Image export hides editing controls without entering immersive mode.
- UI checks cover guided paraboloid analysis, numeric editing, real file exports/share choosers, presentation and return home; slice playback/undo; Phase 2 creation, rename, duplicate, visibility, connected mesh picking/editing and undo/redo; Phase 1 toolbar/mode switching; all seven AR studios; hand detector ownership/lifecycle.
- Visible incomplete-feature marker scan in the added CAD/analysis/field files finds no TODO, STUB, MOCK, PLACEHOLDER, COMING SOON or NotImplemented controls. Optional particle flow/model export are omitted.

## Acceptance limits

Only emulators are connected. API 35 reports an ARCore initialization failure and exercises the actual mathematical 3D fallback. The UI reports AR unavailability in ordinary language. No physical-device AR session, depth imagery, real hand manipulation, two-hand transitions, anchor drift, camera relocalization, sustained thermal/FPS or walk-around workflow has been accepted.

Explicit scalar surfaces have numerical differential analysis. Parametric/implicit surfaces support actual meshing and slicing; this delivery does not invent general analytic derivatives for arbitrary meshes. Section perimeter/span and implicit contours inherit finite mesh resolution. Source line/ray extents remain the Phase 2 finite display extents.

The final product workflow is covered mathematically and through touch/emulator UI where possible. Its placement/hand/depth/walk-around steps require the real AR-device acceptance pass before Phase 3 can be marked fully complete.
