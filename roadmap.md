# AI Explorer — Six-Phase Roadmap Beyond GeoGebra

Last updated: 16 July 2026  
Planning horizon: six gated product phases; dates are set only after team capacity and device targets are agreed.  
Primary platform: native Android (Kotlin + Jetpack Compose).  
Future spatial platform: ARCore.

## 1. Product goal

Build AI Explorer into a touch-first, game-like mathematics laboratory where users manipulate mathematics directly on the canvas: tap an object to select it, drag it with one finger, use two fingers to pan/zoom/rotate, and receive immediate visual, audio, and haptic feedback. Controls remain available for precision, but they must not be the main way to explore.

“Better than GeoGebra” means:

1. Reach GeoGebra-class mathematical breadth and dependable dynamic construction.
2. Surpass it on mobile direct manipulation, 3D interaction, guided discovery, feedback, and accessibility.
3. Use one renderer-independent math scene in 2D, 3D, lessons, multiplayer, and future ARCore views.
4. Never trade correctness, privacy, offline use, or classroom reliability for visual effects.

## 2. Current AI Explorer baseline

The repository currently contains a promising prototype, not yet a GeoGebra replacement.

Implemented today:

- Native Compose shell with 2D, 3D, graph, 3D graph, and trigonometry workspaces.
- Canvas tap construction for a small set of 2D shapes and direct dragging of stored 2D points.
- Basic expression parsing, sampled 2D functions, quadratic/linear insights, trace, derivative/integral overlays, and intersections.
- Projected Compose 3D solids and vectors with tap selection and screen-plane translation.
- 3D surface sampling with drag-to-orbit outside Trace mode.
- Basic solid measurements, undo/redo commands, in-memory saved-workspace snapshots, and JSON export.
- A small guided-activity shell and basic accessibility descriptions/tests.

Important interaction limitations in the current code:

- No unified gesture state machine: each canvas interprets touch differently.
- 2D has point dragging but no object/body dragging, canvas pan, pinch zoom, rotation, long-press menu, marquee/lasso, or snapping feedback.
- Graph dragging always changes the trace x-position; users cannot naturally pan or pinch-zoom the graph.
- The solid pane can translate a picked object in a fixed projected X/Z plane, but cannot orbit the camera by dragging empty space, pan, pinch-zoom, rotate/scale with handles, or drag along a selected axis/plane.
- The surface pane supports one-finger orbit, but no multi-touch pan/zoom, inertia, focus, or unified camera behavior.
- Drag updates create many history commands rather than one coalesced undoable gesture.
- Picking is based on approximate projected centers/endpoints rather than mesh, face, edge, curve, or label hit testing.
- Current 3D is a 2D Canvas projection, with no depth buffer, materials, lighting, occlusion, collision system, or AR-ready scene graph.

## 3. GeoGebra comparison and complete gap register

This gap register compares the inspected repository with GeoGebra’s documented Calculator Suite/Classic capabilities as of July 2026. “Partial” means there is a demonstrator but not a general, production-ready capability.

| Capability | GeoGebra baseline | AI Explorer now | Gap to close |
|---|---|---|---|
| Dynamic object model | Linked algebraic and graphical representations | Flat lists of points/shapes/functions/solids | Typed object graph, stable IDs, dependencies, recomputation, undefined states |
| Direct 2D manipulation | Construct and dynamically drag objects | Point drag and multi-tap shape creation | Whole-object drag, handles, constraints, multi-select, snapping, pan/zoom |
| 2D construction breadth | Broad points, lines, conics, polygons, transforms and loci | 10 basic shape/tool types | Full construction catalog and robust degeneracy handling |
| Algebra view and command input | Linked algebra view plus extensive commands | Two editable function strings; no general command language | General object input, definitions, autocomplete, errors, object tree |
| Symbolic mathematics/CAS | Solve, factor, expand, substitute, exact derivative/integral | Numeric parser and a few numerical analyses | Symbolic engine, exact values, assumptions, steps, equation systems |
| Graphing breadth | 2D/3D functions, equations, curves, surfaces, sliders | Basic explicit 2D functions and one explicit surface | Implicit, parametric, polar, piecewise, inequalities, sequences, fields |
| Calculus and analysis | Roots, extrema, intersections, derivatives, integrals, regression | Narrow overlays and quadratic/linear special cases | General solvers, discontinuities, tangent/normal, limits, ODEs, regression |
| Tables and spreadsheet | Linked table/spreadsheet and data-analysis tools | None | Tables, cells, lists, matrices, CSV, statistics, linked plots |
| Statistics/probability | Distributions, statistical tests, probability calculator | None | Descriptive/inferential statistics, simulations, distributions, tests |
| 3D construction | Points, lines, planes, conics, solids, intersections, transforms | Seven solid families and vectors | Real 3D kernel, planes/curves, boolean/intersection tools, nets/extrusion |
| 3D rendering and interaction | Manipulable 3D graphics view | Canvas projection; limited move/orbit | GPU renderer, depth-correct picking, camera gestures, gizmos, clipping |
| Construction protocol | Step-by-step construction history | Internal undo stack only | Visible timeline, replay, scrub, branch, explain dependencies |
| Animation and scripting | Sliders, animation, GGBScript/JavaScript, events | Parameter sliders for fixed examples only | Timeline, keyframes, event/actions, safe scripting or visual rules |
| Custom tools | User-created reusable construction tools | None | Macro recording, parameterized tools, sharing and versioning |
| Styling and properties | Rich per-object properties, labels, layers | Mostly hard-coded colors and limited toggles | Inspector, themes, dynamic colors, layers, visibility conditions |
| Files/interoperability | Open/save/share and mature `.ggb` ecosystem | In-memory save and one-way JSON text export | Durable database, import, validation, migrations, `.ggb` bridge, media export |
| Resources and authoring | Activities, Books, embedded media, community sharing | Small hard-coded activity list | Lesson authoring, validation DSL, content library, packages, moderation |
| Classroom collaboration | Assignments, live progress, discussions, Google Classroom | None | Accounts/roles, classroom sessions, live state, teacher dashboard |
| Exam readiness | Dedicated restricted exam modes | None | Offline allowlists, Android lock-task/MDM path, audit screen, certification |
| Cross-platform reach | Web, Android, iOS, Windows, macOS | Android only | Web/desktop strategy or shared core APIs; responsive tablet/Chromebook UI |
| Accessibility/localization | Mature multi-platform UI and broad usage | Canvas descriptions and basic contrast | Semantic object navigation, keyboard/switch access, sonification, localization |
| Performance/reliability | Mature large-construction handling | Main-file prototype and synchronous sampling | Background computation, caching, incremental recompute, benchmarks, crash telemetry |
| AR/spatial learning | Not the central Calculator Suite experience | None | ARCore placement, scale, anchors, occlusion, spatial measurement and shared AR |

### Additional technical and product gaps

- Architecture: `MainActivity.kt` contains app state, UI, interaction, and renderers; it must be split into testable feature/domain/data/renderer layers.
- Geometry correctness: shapes reference free point indices but do not express construction constraints or dependencies.
- Expression safety: no typed errors, units, complex values, vectors/matrices, assumptions, locale-aware input, or resource limits.
- Persistence: saved workspaces and settings live only in ViewModel memory; DataStore is declared but not used as a complete settings system, and Room is absent.
- Undo: no gesture transaction/coalescing, named history timeline, autosave recovery, or multi-user history.
- Rendering: sampled meshes may omit invalid vertices without preserving topology; the current renderer cannot support reliable occlusion or selection.
- Testing: unit tests cover a few happy paths; gesture, property-based math, golden rendering, performance, migration, device, and AR tests are absent.
- Security/privacy: no account model, sync policy, consent/age handling, content moderation, encryption design, or analytics policy.
- Product operations: no content pipeline, telemetry taxonomy, feature flags, staged rollout, crash/ANR budget, or compatibility matrix.

## 4. Interaction contract for every phase

These rules are non-negotiable across 2D, graph, 3D, and AR:

| Gesture | Object target | Empty canvas target |
|---|---|---|
| Tap | Select; second tap exposes contextual handles | Place with active tool or clear selection |
| One-finger drag | Move selected free object/handle; constrained objects slide on constraint | Pan in 2D; orbit in 3D when no creation tool is active |
| Two-finger drag | Pan camera/view without changing the selected object | Pan camera/view |
| Pinch | Scale selected object only when Scale mode is explicit | Zoom camera around gesture centroid |
| Two-finger twist | Rotate selected object in explicit Rotate mode | Roll/orbit only where pedagogically useful |
| Long press | Context menu: properties, lock, duplicate, hide, delete, explain | Quick-create/radial tool menu |
| Double tap | Focus selected object | Fit construction/reset view |
| Stylus | Precise select/draw; hover preview where supported | Precise construction and handwriting input later |

Gesture arbitration must use touch slop, pointer count, active tool, hit target, selection state, and explicit mode. It must never move an object when the user intended to orbit the view. All gestures must have cancel/rollback behavior, one undo entry, accessible non-gesture equivalents, optional haptics, and a reduced-motion path.

## 5. Six-phase delivery plan

## Phase 1 — Touch-first interaction and scene foundation

**Implementation status (16 July 2026):** the core direct-manipulation slice is implemented: atomic point/shape/solid/vector gesture transactions, 2D and graph pan/zoom, graph trace arbitration, 3D orbit/pan/zoom, direct solid Move/Rotate/Scale modes, transform gizmos, double-tap camera reset, selection feedback, interaction hints, optional selection haptics, and workspace persistence for solid rotation. Long-press property menus, marquee/lasso selection, momentum, stylus hover, and a reusable renderer-level picking service remain follow-up Phase 1 hardening work.

### Objective

Make every canvas feel like an interactive game board and create the architecture on which all later geometry, 3D, and AR interaction depends.

### Product work

- Introduce a shared interaction state machine: Idle, Hover, Pressed, Selecting, Creating, DraggingObject, DraggingHandle, Panning, Orbiting, Pinching, ContextMenu, and Cancelled.
- Add direct object/body hit testing, selection halos, handle affordances, ghost previews, valid/invalid target highlighting, and drag trails.
- 2D: one-finger object/handle drag, two-finger pan, centroid pinch zoom, double-tap fit, long-press context menu, box selection, and optional lasso.
- Graph: drag a trace point only when it is hit or Trace mode is active; otherwise pan. Add pinch zoom, axis-locked zoom, fit function, and momentum with reduced-motion support.
- 3D solids: drag empty space to orbit, two-finger pan, pinch zoom, double-tap focus, and tap-select the visible object under the finger.
- Add translation arrows/planes, rotation rings, scale handles, local/world orientation, snap increments, and numeric override for selected 3D objects.
- Surface graphs: use the same camera controller as solids; trace selection competes correctly with orbit.
- Add haptic ticks for selection, snaps, intersections, special angles, valid placement, and errors; make haptics optional.
- Add a short interactive onboarding level teaching tap, drag, pan, pinch, orbit, focus, and undo.

### Engineering work

- Split canvas input from rendering using `InteractionController`, `Camera2D`, `Camera3D`, `SelectionModel`, `HitTestService`, and `GestureTransaction` interfaces.
- Define a renderer-neutral `SceneObject`/`SceneNode` identity and transform contract; keep math coordinates independent of pixels and future AR meters.
- Coalesce all pointer moves between down/up into one command; cancel restores the pre-gesture snapshot.
- Add spatial indexing for 2D targets and a ray/picking interface for 3D.
- Normalize mouse, touch, stylus, keyboard, and accessibility actions into semantic commands.
- Persist camera state per workspace and supply deterministic reset/fit behavior.
- Refactor the monolithic Compose file incrementally without changing mathematical results.

### Acceptance gate

- Users can tap and drag any movable 2D object or handle without opening a control panel.
- Users can pan and pinch-zoom every 2D/graph workspace at 60 fps on the agreed baseline device.
- Users can orbit, pan, zoom, select, translate, rotate, and scale a 3D solid directly on the pane.
- A 5-second drag produces exactly one undo step and redo restores the final transform.
- Gesture-conflict automated tests cover object drag vs pan, trace vs pan, and object transform vs camera orbit.
- No control-only operation remains for basic selection, movement, camera navigation, or focus.

### Better-than-GeoGebra outcome

A consistent mobile gesture grammar across all workspaces, with game-quality feedback and discoverable on-canvas handles.

## Phase 2 — Dynamic geometry kernel and complete 2D laboratory

**Implementation status (16 July 2026):** the dynamic-kernel slice is implemented with reusable point references, grid snapping, dependency recomputation, undefined-state detection, atomic undo/redo, construction protocol, and object properties. Available dynamic constructions include midpoint, line intersection, centroid, circumcenter, incenter, orthocenter, parallel/perpendicular lines, angle bisectors, vectors, three-point circles, ellipses, and regular polygons. Translate, rotate, reflect-X, and dilate create dependency-linked copies. Remaining catalog expansion includes point-on-path constraints, tangents, general conics, loci, additional centers, arbitrary-axis reflection, shear, macros, layers, multi-selection, and robust-predicate hardening.

### Objective

Replace flat demo objects with a general, dependable dynamic construction system and reach GeoGebra-class 2D geometry.

### Product work

- Create free and dependent points; point-on-object, intersections, midpoint, centers, division points, and projection.
- Complete lines, rays, segments, vectors, parallels, perpendiculars, bisectors, tangents, loci, and path tracing.
- Complete polygons, regular polygons, circles, arcs, sectors, ellipse, parabola, hyperbola, and conic-through-points.
- Add transforms: translate, rotate, reflect, dilate, shear, group, copy, and transformation composition.
- Add live measurements, exact/decimal display, units, labels, annotations, angles, areas, perimeters, equations, and coordinates.
- Add snapping to grid, points, curves, intersections, midpoints, tangencies, perpendicular/parallel directions, and special angles.
- Add object drawer and property inspector: rename, visibility, lock, auxiliary, style, layer, trace, duplicate, delete, and dependency explanation.
- Add algebra/object view dynamically linked to the canvas and a construction protocol that can scrub and replay steps.
- Turn a completed construction into a reusable custom macro/tool.

### Engineering work

- Introduce typed immutable math objects with stable UUIDs, free/dependent status, inputs, outputs, style, visibility, and validity state.
- Build a directed acyclic dependency graph with topological incremental recomputation, cycle prevention, and dirty-node propagation.
- Use robust geometric predicates and explicit tolerance/precision policy; never silently invent results for degenerate constructions.
- Model undefined/ambiguous results and provide visual recovery guidance.
- Implement constraint solving for dragging dependent objects along paths and for inverse manipulation where mathematically valid.
- Version workspace schema v2+, migrations, full import validation, and deterministic serialization.
- Add Room autosave/recovery and DataStore-backed preferences.

### Quality and tests

- Property-based tests for incidence, parallelism, perpendicularity, congruence, transformations, and invariant preservation.
- Dependency stress tests with at least 10,000 objects and bounded incremental recomputation.
- Golden tests for labels, selections, degeneracies, dark/high-contrast themes, and common tablet/phone sizes.
- Accessibility: semantic object list, next/previous object navigation, coordinate announcements, keyboard/switch manipulation.

### Acceptance gate

- A user can construct and continuously deform a triangle with circumcenter, incircle, perpendicular bisectors, measurements, and dependent labels.
- All dependent objects update during drag within the frame budget and undo returns the entire drag atomically.
- Construction replay and exported/imported reconstruction produce the same object graph.
- At least 90% of the agreed 2D tool catalog is production-ready and covered by mathematical invariant tests.

### Better-than-GeoGebra outcome

On-canvas dependency explanations show not only what an object is, but why it moves or becomes undefined, with guided recovery during manipulation.

## Phase 3 — Graphing, CAS, calculus, data, statistics, and probability

**Implementation status (16 July 2026):** the advanced graphing slice is implemented. Definitions can be edited, added, hidden, and deleted; the canvas renders explicit, conditional piecewise, polar, parametric, and implicit equations. Parameter `a` is detected and receives a live slider. General numerical roots, extrema, intersections, tangent/normal, derivative curves, Simpson integrals, and area-between-curves are available. Table, editable data-point, linear-regression, descriptive-statistics, normal-density, and binomial-probability modes are included. Remaining Phase 3 expansion includes a full symbolic CAS, exact assumptions/units, multiple named parameters with persisted ranges, inequalities, complex plots, sequences, matrices/spreadsheet cells, statistical tests/distributions, ODE fields, adaptive interval arithmetic, and a dedicated math keyboard.

### Objective

Become a complete mathematics calculator rather than only a geometry visualizer.

### Product work

- General algebra input with math keyboard, autocomplete, syntax preview, inline errors, command palette, history, and named objects.
- Plot explicit, implicit, parametric, polar, piecewise, inequality, complex, sequence, vector-field, slope-field, and data-defined objects.
- Add parameter discovery and on-canvas sliders with animation, play/pause, speed, loop/ping-pong, and timeline recording.
- Directly drag curves, intercepts, vertices, asymptotes, and parameters where inverse manipulation is well-defined.
- Add roots, extrema, intersections, asymptotes, limits, discontinuities, tangent/normal, derivatives, definite/indefinite integrals, area between curves, Taylor approximations, and numerical ODE exploration.
- Add exact symbolic results: simplify, expand, factor, substitute, solve/nsolve, systems, derivatives, integrals, assumptions, fractions, radicals, and complex numbers.
- Add tables, lists, matrices, spreadsheet cells, CSV import/export, one/two-variable analysis, regression families, residuals, confidence intervals, and statistical tests.
- Add probability distributions, cumulative/inverse probability, simulations, combinatorics, and hypothesis-test visualizations.
- Make graphs sonifiable and navigable by special points for blind/low-vision users.

### Engineering work

- Decide through a benchmark/RFC whether the symbolic kernel is embedded, service-backed, or hybrid; offline classroom basics are mandatory.
- Replace ad-hoc parsing with a typed AST, namespaces, units, assumptions, localization, structured errors, complexity limits, and canonical serialization.
- Add adaptive interval-aware sampling, discontinuity detection, stable solvers, precision controls, and background/cancellable computation.
- Unify expression objects with the Phase 2 dependency graph so algebra, graph, geometry, table, and slider always agree.
- Add a pluggable analysis engine with provenance: exact, numeric approximation, tolerance, domain, and warnings shown to the user.

### Acceptance gate

- Standard secondary-school and first-year calculus workflows work offline and produce validated results.
- A parameter can be created from an expression, dragged on canvas, animated, tabled, and reused in geometry/3D.
- Large or invalid inputs cannot freeze the UI; computation is cancellable and reports structured errors.
- Reference suites compare symbolic/numeric answers against trusted datasets across real, complex, and edge cases.

### Better-than-GeoGebra outcome

Every answer can switch among symbolic steps, numeric evidence, graph, table, and manipulable visual proof without rebuilding the problem.

## Phase 4 — Production 3D engine and game-like spatial mathematics

**Implementation status (16 July 2026):** the renderer-neutral Phase 4 foundation is implemented. The solid catalog now includes cube/cuboid, sphere/hemisphere, cylinder, cone/frustum, pyramid, triangular prism, tetrahedron, octahedron, and torus, with shared mesh generation and corrected measurements. The touch workspace supports object/vertex/edge/face selection, highlighted sub-objects, cross-section planes, clipping, wire/filled views, and switchable perspective/orthographic cameras while preserving direct orbit, pan, zoom, move, rotate, and scale. The 3D graph now defaults to one-finger orbit, supports two-finger pan, pinch zoom and twist roll, and exposes direct X/Y/Z and XY/XZ/YZ camera taps with visible coordinate planes and colored axes. Mesh topology and plane intersections are unit tested and the schema exports frustum top radius. The Compose Canvas path remains the graceful fallback; GPU depth buffering, depth-correct occluded-hit cycling, exact boundary representations, 3D construction primitives, booleans/nets, renderer-loss persistence, and performance/thermal gates remain before the full Phase 4 acceptance gate is met.

### Objective

Replace projected Canvas 3D with a real GPU scene and make 3D construction as direct and expressive as a polished educational game.

### Product work

- Construct and manipulate 3D points, lines, rays, vectors, planes, polygons, circles/conics, prisms, pyramids, Platonic solids, spheres, cylinders, cones, frustums, torus, extrusions, and solids of revolution.
- Select visible object, face, edge, or vertex; cycle occluded hits; isolate, hide, lock, group, duplicate, and inspect.
- Add snapping to axes, planes, surfaces, vertices, edges, centers, normals, and angle increments.
- Add intersections, cross-sections, clipping planes, nets/unfolding, exploded views, booleans where appropriate, volume fill, and measurement tools.
- Add multiple 3D functions/surfaces, contours, slices, tangent planes, normals, gradients, critical points, and gradient paths.
- Add game-like challenges: build under constraints, timed construction (optional), physics-informed demonstrations, collectibles/progress, and replay—without turning correctness into a score gimmick.
- Add camera presets, home/focus, orbit inertia, orthographic/perspective modes, section view, and controller/mouse support.

### Engineering work

- Select a maintained Android GPU renderer behind `Renderer3D`; run a proof-of-concept for lifecycle, Compose interop, picking, materials, text, performance, and ARCore reuse before committing.
- Introduce scene graph transforms, mesh cache, depth buffer, physically coherent lighting, transparency ordering, instancing, level of detail, and offscreen selection IDs/raycasting.
- Separate mathematical boundary representation from render meshes; generated meshes are disposable views of exact/parametric objects.
- Use double precision for the math world and well-defined conversion to renderer floats/local origins.
- Add device quality tiers, thermal monitoring, frame pacing, adaptive mesh density, and graceful fallback.
- Keep camera/object/selection APIs identical between normal 3D and the Phase 6 AR renderer.

### Acceptance gate

- Sustained target frame rate on the baseline device with a representative classroom scene and no unbounded thermal degradation.
- Face/edge/vertex picking is depth-correct and reliable at agreed pixel/touch tolerances.
- A user can create a plane-solid cross-section and manipulate it entirely through direct touch plus optional numeric precision.
- Math object identities and undo history survive renderer loss, rotation, process recreation, save/load, and renderer changes.

### Better-than-GeoGebra outcome

Touch-native gizmos, semantic snapping, construction challenges, and rich visual explanations make 3D manipulation feel immediate rather than desktop tools compressed onto a phone.

## Phase 5 — Learning platform, collaboration, compatibility, and production readiness

**Implementation status (16 July 2026):** the offline learning-platform foundation is implemented. Lessons are renderer-neutral authored definitions with objectives, allowed tools, accessibility labels, multiple weighted checkpoints, progressive hints, explanations, and misconception-specific feedback. Learner progress records checkpoint completion, attempts, hint use, timestamps, and completion state; the in-app coach now provides learner and teacher views, assignment summaries, support flags, and an idempotent offline operation queue. Versioned learning packages include the workspace, progress, and assignments, escape user-authored JSON safely, enforce type/schema/size/structure validation, and preserve a stable boundary for future sync/import. Unit coverage includes multi-checkpoint evaluation, accumulated progress, teacher support flags, offline replay, and hostile/incomplete package rejection. Remaining Phase 5 work requires product infrastructure beyond the local app: durable encrypted storage and migrations, authenticated accounts/roles, a sync service with convergent collaboration, lesson authoring UI, real classroom rosters, `.ggb` translation, media export, localization/RTL QA, moderation, audited exam mode, and institutional privacy/security operations.

### Objective

Turn the math engine into a trustworthy product for individual learners, teachers, classrooms, creators, and institutions.

### Product work

- Lesson/activity authoring with objectives, initial scene, allowed tools, checkpoints, hints, branching feedback, rubrics, explanations, and accessibility metadata.
- Dynamic proof library for geometry, algebra, trigonometry, calculus, statistics, and solid geometry.
- Adaptive practice based on misconception—not just right/wrong—with private learner progress and teacher-configurable hints.
- Teacher dashboard, assignments, live session state, anonymous presentation, progress/checkpoints, feedback, and exportable reports.
- Real-time co-editing with cursors/selections, presenter/follow mode, roles, comments, version history, and conflict-safe operations.
- Offline-first sync and share links/QR codes; export PNG/SVG/PDF/video/CSV and validated workspace packages.
- Build a scoped `.ggb` import/export compatibility bridge; publish a support matrix and preserve unsupported source data when possible.
- Add searchable curated resources, creator profiles, remix attribution, licensing, reporting, and moderation.
- Add a secure exam-mode program using Android lock-task/MDM capabilities where permitted, with clear regional certification boundaries.
- Full localization, RTL, screen-reader flows, keyboard/switch access, reduced motion, high contrast, color-vision-safe palettes, captions, and sonification.

### Engineering and operations

- Modularize into domain, interaction, renderer, persistence, sync, authoring, classroom, and AR-ready boundaries.
- Define accounts/roles, tenancy, encryption, retention, deletion/export, child/student privacy, consent, audit, abuse controls, and regional hosting requirements before cloud launch.
- Use an operation-based collaboration model compatible with deterministic math recomputation; specify CRDT/OT choice through an RFC and adversarial tests.
- Add authenticated APIs, rate limits, offline queue, conflict resolution, observability, feature flags, backups, disaster recovery, and cost budgets.
- Establish CI gates: unit/property/instrumented/golden/accessibility/performance/security/migration tests and staged Play rollout.
- Track crash-free sessions, ANR, input latency, recompute latency, frame time, lesson completion, hint effectiveness, and accessibility success—never sensitive math content by default.

### Acceptance gate

- A teacher can author, assign, observe, and review a lesson; a learner can complete it offline and sync later.
- Two or more users can safely edit one construction and converge after disconnection/reconnection.
- No P0/P1 accessibility findings in core workflows; supported locales pass layout and input QA.
- Persistence, sync, schema migration, account deletion, backup recovery, and exam restrictions pass documented audits.
- A published interoperability matrix covers common GeoGebra files and every export format.

### Better-than-GeoGebra outcome

Built-in misconception-aware guidance, high-quality mobile accessibility, and multiplayer manipulation are core engine features, not separate content overlays.

## Phase 6 — ARCore spatial mathematics

**Implementation status (16 July 2026):** the ARCore-ready spatial foundation is implemented as an AR Optional Android feature. The app declares camera permission without excluding non-AR devices, uses ARCore SDK 1.54.0, checks support/install/update state at runtime, requests camera access only when live AR is chosen, configures horizontal/vertical plane finding, environmental HDR lighting, autofocus, and capability-gated automatic Depth, and explicitly releases native session resources. The same workspace solids open in a new AR spatial lab with an accessible non-camera simulator, direct one/two-finger scene move/scale/twist, explicit 1:1 or fit-to-space scale, visible math-unit-to-meter conversion, local anchor metadata, depth-occlusion preference, safety/tracking guidance, estimated-measurement disclosure, JSON persistence, and atomic undo. Coordinate conversion, scale bounds, degraded-tracking safety, persistence, and undo are unit tested. The full Phase 6 acceptance gate still requires a GPU camera compositor and ARCore frame loop, real plane/depth hit tests and native anchors, tracking-driven relocalization, renderer depth occlusion, Cloud Anchors/shared sessions, optional Geospatial placement, device/thermal/drift testing, and privacy/safety review on physical supported devices.

### Objective

Place the same trusted 3D math scene into the learner’s environment and make physical space an input to construction, measurement, proof, and collaboration.

### Entry criteria

Do not begin full AR production until Phases 1 and 4 have stable renderer-neutral scene objects, transforms, selection, gestures, persistence, performance budgets, and real 3D picking. AR must be another view of the same workspace, not a forked math engine.

### Product increments

1. **AR viewer:** check device support; guide plane scanning; tap to place; move/rotate/scale anchored constructions; reset/relocalize; return instantly to studio 3D.
2. **Spatial lab:** physical-scale ruler/grid, unit calibration, surface placement, vertical/horizontal snapping, shadows, light estimation, depth occlusion, and safe boundary guidance.
3. **AR construction:** create points on detected planes/depth hits, draw vectors between anchors, attach coordinate frames, intersect virtual planes/solids, and manipulate handles with ray-based picking.
4. **Guided AR lessons:** walk around conic sections and surfaces, inspect cross-sections, unfold nets, explore vectors/planes, compare predicted and observed measurements, and capture annotated evidence.
5. **Persistent/shared AR:** host/resolve Cloud Anchors for same-room lessons, presence and roles, anchor-quality indicator, relocalization recovery, and privacy-safe room codes.
6. **Optional geospatial lessons:** use Geospatial Anchors only for content that genuinely benefits from WGS84/Terrain/Rooftop placement; retain a non-geospatial alternative.

### ARCore engineering work

- Add a capability matrix and graceful non-AR fallback for unsupported devices, unavailable camera permission, poor tracking, missing Depth support, heat, low light, or no network.
- Implement an `ARSessionController` for lifecycle, install/update flow, permissions, tracking state, plane/depth hit tests, anchors, relocalization, and interruptions.
- Define transforms among math space, renderer world space, AR session space in meters, anchor-local space, and optional geospatial coordinates.
- Make scale explicit: mathematical units-to-meters policy, 1:1 mode, fit-to-space mode, visible scale badge, and locked scale for measurement activities.
- Reuse Phase 4 render meshes/materials while adding camera background, depth occlusion, environmental lighting, anchors, and performance tiers.
- Store semantic objects and anchor metadata, never make an AR pose the only source of mathematical truth.
- Cloud Anchors require consent, network state, expiry/retention UX, secure room lookup, abuse limits, and a local-only mode.
- Geospatial mode requires VPS availability/accuracy UI, quota/cost controls, location consent, and strict location-data minimization.

### AR safety and UX requirements

- Always show tracking quality and whether a placement/measurement is estimated.
- Pause or warn when walking speed, low light, lost tracking, or unsafe attention is detected; never require backward walking.
- Use high-contrast outlines and depth cues; avoid content that appears to block real hazards.
- Provide seated, one-handed, non-AR, reduced-motion, hearing-independent, and color-safe alternatives.
- Separate educational estimates from certified physical measurement and state accuracy limitations.

### Acceptance gate

- The same saved solid/surface opens in studio 3D and AR with identical mathematical definitions, labels, transforms, and measurements.
- Place, select, move, rotate, scale, undo, save, close, relocalize, and reopen work reliably on the supported-device matrix.
- Occlusion and anchor drift meet defined test thresholds; degraded tracking never silently corrupts math state.
- Two devices resolve a shared classroom anchor and converge on the same semantic construction.
- Battery, thermal, privacy, permission, safety, offline, and accessibility test plans pass before public release.

### Better-than-GeoGebra outcome

Mathematical constructions become persistent, shareable spatial objects with guided physical exploration, while remaining exactly the same editable construction in the standard 3D workspace.

## 6. Cross-phase release scorecard

Every phase ships only when its gate is met. Track at least:

- Correctness: invariant/reference suite pass rate; undefined/ambiguous cases handled explicitly.
- Directness: percentage of common tasks completed on canvas without opening a panel.
- Input: median/p95 touch-to-visual latency; gesture cancellation and conflict error rate.
- Performance: p50/p95 frame time, recomputation time, memory, battery, thermal behavior, ANR/crash-free sessions.
- Learnability: first-attempt success for tap/drag/pan/pinch/orbit and time to first successful construction.
- Accessibility: TalkBack/keyboard/switch task completion, contrast, target size, reduced-motion and sonification coverage.
- Reliability: save/load/migration fidelity, offline recovery, sync convergence, renderer/context loss recovery.
- Trust: privacy/security audit status, data deletion SLA, content reporting, exam-mode limitations disclosed.
- Compatibility: GeoGebra import/export coverage by object/command type with round-trip tests.

## 7. Recommended sequencing and dependencies

```text
Phase 1 interaction/scene contracts
  ├── Phase 2 dynamic 2D kernel
  │     └── Phase 3 algebra/CAS/data
  └── Phase 4 production 3D renderer
          └── Phase 6 ARCore

Phases 2–4 feed Phase 5 lessons, collaboration, interoperability, and production hardening.
Phase 5 privacy, identity, and collaboration foundations are prerequisites for shared AR.
```

Avoid implementing AR as an isolated prototype inside the current Canvas renderer. A small ARCore feasibility spike may run during Phase 4 to validate renderer/session integration, but user-facing AR belongs in Phase 6.

## 8. First implementation backlog (start of Phase 1)

1. Write the interaction/camera/scene RFC and gesture conflict table.
2. Add gesture transaction coalescing so drag is one undo command.
3. Implement `Camera2D` pan/zoom/fit and retrofit Coordinate + Graph canvases.
4. Add semantic hit targets and body dragging for 2D shapes.
5. Implement `Camera3D` orbit/pan/zoom/focus and retrofit both 3D canvases.
6. Add 3D transform gizmo prototype with axis/plane constraints and snap angles.
7. Add automated multi-pointer Compose tests and a repeatable input-latency benchmark.
8. Split the first canvas/controller out of `MainActivity.kt`; use it as the pattern for the remaining modules.
9. Validate interaction on phone, tablet, Chromebook/mouse, stylus, TalkBack, and reduced-motion modes.
10. Run a Phase 1 usability test with learners before expanding the tool catalog.

## 9. Reference baseline

GeoGebra official references used for the comparison:

- [Calculator Suite overview](https://help.geogebra.org/hc/en-us/articles/8379325433629-Calculator-Suite)
- [GeoGebra tools and features overview](https://help.geogebra.org/hc/en-us/articles/10445800380957-GeoGebra-Tools-and-Features-An-Overview)
- [GeoGebra views](https://geogebra.github.io/docs/manual/en/Views/)
- [3D Graphics tools](https://geogebra.github.io/docs/manual/en/tools/3D_Graphics_Tools/)
- [Spreadsheet tools](https://geogebra.github.io/docs/manual/en/tools/Spreadsheet_Tools/)
- [Custom tools](https://geogebra.github.io/docs/manual/en/tools/Custom_Tools/)
- [GeoGebra scripting](https://geogebra.github.io/docs/manual/en/Scripting/)
- [GeoGebra Resources and Books](https://help.geogebra.org/hc/en-us/articles/10449584308125-Get-started-with-GeoGebra-Resources)
- [GeoGebra Lessons/classroom assignment](https://help.geogebra.org/hc/en-us/articles/8828154551965-Assign-GeoGebra-Resources)
- [GeoGebra Exam Mode](https://help.geogebra.org/hc/en-us/articles/10469645779229-Exam-Mode)

ARCore official references used for Phase 6:

- [ARCore Android quickstart](https://developers.google.com/ar/develop/java/quickstart)
- [ARCore Depth on Android](https://developers.google.com/ar/develop/java/depth/developer-guide)
- [Cloud Anchors](https://developers.google.com/ar/develop/cloud-anchors)
- [Geospatial API](https://developers.google.com/ar/develop/geospatial)
- [Geospatial Anchors on Android](https://developers.google.com/ar/develop/java/geospatial/anchors)
