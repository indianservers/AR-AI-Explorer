# AR Space: Graph 3D Phase 2 audit

Scope: the existing Native Android AR Space / Graph 3D screen. Phase 1 runtime, origin placement, collapsible toolbar, hand detector ownership, Touch/Gesture/Hybrid arbitration and object history are retained. No Phase 3 work was started.

## Implemented

- Indexed, welded topology shared by rendering, ray picking, selection, editing, inspector and snapping. Connected coplanar triangles form an actual face. Selection overlays contain only the selected face triangles, edge or vertex; hand hover gets a separate exact overlay.
- Object/face/edge/vertex contextual modes. Semantic selection resolves Point, Curve, Plane, Vector, ControlPoint and Intersection from the exact hit and primitive metadata. Touch and hand rays commit through the existing ArSelectionEngine.
- Connected move/rotate/resize edits; exact vertex coordinates and edge length; face offset/extrusion; edge midpoint/division. Mesh validation rejects non-finite coordinates and collapsed triangles.
- Compact precision bottom sheet using the existing safe expression engine (pi, Unicode pi, fractions and sqrt). Face area, unit normal and plane equation, edge length, local coordinates, analytic parameters and world measurements are exposed.
- Sub-object AR handles: face N/U/V, edge direction, vertex XYZ. Sphere radius handle works in AR and the mathematical fallback. Vector endpoint drag rebuilds the analytic vector and arrowhead. Plane faces provide normal/planar movement; object rotation edits its normal.
- Actual grid, point, vertex, midpoint, edge, axis, plane, intersection and triangle-surface snapping. Discrete construction targets take priority over generic projections. Snapping can be disabled.
- Persistent mesh constraints: coincident, parallel, perpendicular, equal/fixed length, fixed angle, point-on-line/plane/surface and locked vertices. Iterative projection checks residuals and rejects conflicts. Equal-radius and external sphere tangency are analytic dependencies, recomputed using world dimensions.
- Stable-ID DAG with missing-reference/cycle prevention. Point transforms and point mesh edits propagate to endpoint constructions, midpoints and centered primitives. Deleted ancestors hide downstream constructions.
- Point, line, segment, ray, vector, plane, triangle, polygon, 3D circle, sphere, cube, cuboid, cylinder, cone, triangular prism, pyramid, midpoint and line-plane intersection. Existing coordinate axes/grid remain available.
- Explicit and parametric surfaces: domains, resolution, opacity, wireframe and filled state. Parametric curves: xyz(t), range, samples, thickness, moving parameter point. Autodetected expression parameters have numeric fields and live sliders; a slider drag commits one history action.
- Valid sphere and plane equation displays follow object transforms. Equation coefficients can rebuild local analytic primitives. Reshaped spheres and non-planar plane meshes do not claim a unique analytic equation.
- Real vector components, magnitude/direction, normalization, addition, subtraction, dot/cross product, projection and angle. Persistent live distance and three-point angle measurements use transformed scene geometry.
- Scene explorer substructure, select/hide/show/lock/rename/delete and CAD duplication that preserves mesh edits. Groups support undoable move/rotation/scale/hide/lock/ungroup, including derived constructions. A transform group must include the dependencies of a grouped derived object.
- Existing history/snapshot persistence now includes CAD nodes, edits, constraints, groups and measurements. Gesture previews commit once.
- Background analytic meshing and group computation, per-node geometry caching, cached edge adjacency/face regions, transform-only GPU reuse, bounded sampling. Real GLES points are drawn. The fallback now displays and picks actual mathematical geometry rather than placeholder markers.

## Supported mathematical bounds

Mesh constraints reference vertices within an object. Cross-object constructions use DAG dependencies; analytic equal-radius/tangent relationships currently apply to spheres. Infinite lines/rays have a finite viewport representation and their inspector labels its length as displayed extent. Resolution is bounded (surface maximum 64, curve maximum 128); automatic device-dependent resolution tuning is not claimed. Arbitrary mesh equation inference is not provided. Analytic parameter rebuilding requires mesh constraints to be removed first so they are not silently invalidated. Face/edge topology-changing edits should be followed by selecting the intended entity again.

## Verification

Phase 1 emulator baseline: 3 instrumentation checks passed (toolbar/interactions and hand detector ownership/lifecycle).

Final verification:

- 69 app regression tests passed: 39 CAD, 8 Phase 1, 4 AR Space, 3 graph appearance, 6 production spatial authoring, 9 graph/engine parity.
- 170 AR engine tests remain passing (unchanged Phase 2 engine source; Gradle confirms their current results are up to date).
- Debug application and instrumentation APK builds passed. Application and tests were installed successfully on emulator-5556.
- Seven distinct instrumentation checks passed on the installed delivery build (91.678 seconds). They cover CAD creation, rename/duplication/visibility, expression-based vertex editing, real face/edge/vertex picking and connected dragging with undo/redo, retained toolbar modes, all seven studios, and detector ownership/lifecycle.
- git diff --check passed.

The last checks also fixed zero-motion taps creating undo entries, outward solid face winding and extrusion side winding, existing graph-layer equation edits being hidden by mesh overrides, and numeric edits bypassing object locks. Appearance changes preserve mesh edits; equation rebuilds and their undo are atomic.

Evidence logs: tmp/ar-phase2-delivery-build.log, tmp/ar-phase2-delivery-device.log and tmp/ar-phase2-full-regression.log. Screenshots: docs/qa/ar-phase2-evidence/.

Physical acceptance remains outstanding: only emulator-5554 and emulator-5556 are connected. ARCore on emulator-5556 reports an unrecoverable runtime error, so emulator checks exercise the real mathematical fallback and detector lifecycle rather than proving camera placement or hand-ray manipulation on a supported phone. No real-device AR/hand reliability or frame-rate claim is made.
