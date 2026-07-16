# AI Explorer

AI Explorer is a native Android mathematics exploration app built with Kotlin and Jetpack Compose.

The current implementation replaces the starter XML project with a dark-first Compose application using the package `com.indianservers.aiexplorer`. It includes functional 2D coordinate geometry, 2D graphing, projected 3D solid exploration, 3D surface graphing, command history, workspace JSON export, accessibility descriptions, and automated math tests.

## Subject hub and adaptive shell

- The first screen is a learning-laboratory hub. Maths is active; Physics, Chemistry, Biology, Astro Physics, and IQ Labs are visible as disabled future subjects.
- Selecting Maths opens the current Explorer without changing existing workspaces.
- The Mathematics Menu lists the active workspaces plus planned Formulas, MCQs, Visualize Formulas, Theorems, Visual Proofs, Maths Dictionary, and Probability & Statistics areas.
- Safe-area insets, adaptive card sizing, non-wrapping weighted navigation, and scrollable panels support portrait/landscape phones and tablets.
- Focusable controls, optional touchscreen/Leanback declarations, a TV launcher entry, and a TV banner support Smart TV/D-pad operation.
- The large action dock is closed by default and opens from `More`, preventing it from covering the graph on mobile screens.

## Direct interaction

- 2D geometry: tap and drag points or complete shapes; drag empty space to pan, pinch to zoom, and double-tap to fit.
- Graphing: drag the trace in Trace mode, drag the canvas in Plot mode, and use two fingers to pan/zoom in every mode.
- 3D solids: drag empty space to orbit, use two fingers to pan/zoom, and use the on-canvas Move/Rotate/Scale modes to transform selected solids. Switch among object, vertex, edge, and face picking, perspective/orthographic cameras, cross-sections, clipping, and wire/filled rendering.
- 3D surfaces: one-finger drag orbits by default; two fingers pan, pinch zooms, and two-finger twist rolls. Tap X/Y/Z or XY/XZ/YZ for canonical axis and plane views, double-tap to refocus, or choose Trace before dragging the trace point.
- Direct manipulation is previewed live and committed as one atomic undo/redo command when the gesture ends.

## Dynamic geometry

- Constructions reuse existing points when tapped and grid-snap newly placed points.
- Derived midpoint, intersection, centroid, circumcenter, incenter, and orthocenter points update live when their parent points move.
- Advanced objects include vectors, parallel/perpendicular lines, angle bisectors, three-point circles, ellipses, and regular polygons.
- Translated, rotated, reflected, and dilated copies remain dynamically linked to their source objects.
- The object inspector supports rename, show/hide, lock/unlock, style cycling, deletion, and undo/redo.
- The construction protocol displays the current undoable construction sequence.

## Advanced graphing and analysis

- Editable, addable, hideable, and deletable graph definitions.
- Explicit, piecewise, polar, parametric, and implicit plotting.
- Live parameter `a` sliders for parameterized expressions.
- Trace, tangent, normal, derivative, integral, area-between-curves, intersections, roots, and extrema analysis.
- Value tables, editable point datasets, linear regression, descriptive statistics, normal PDF, and binomial probability.
- Comparisons and lazy `if(condition, true, false)` expressions plus multi-argument `min`/`max`.

## Spatial mathematics

- Twelve solid families, including frustums, triangular prisms, Platonic solids, hemispheres, and torus.
- Renderer-neutral mesh topology shared by drawing, sub-object hit testing, highlighting, and section-plane intersection.
- Live volume, total surface area, face, edge, and vertex measurements.
- Direct object/vertex/edge/face selection plus local horizontal cross-sections and clipping.
- Perspective and orthographic projection modes; Canvas remains the fallback renderer boundary for a future GPU/ARCore view.

## Guided learning and classroom foundation

- Authored lesson definitions with allowed tools, accessibility labels, multiple checkpoints, proofs, progressive hints, and misconception feedback.
- Per-lesson progress tracks completed checkpoints, attempts, hint use, timestamps, and completion without reducing learning to a score.
- Learner and teacher views show assignment completion, support signals, and checkpoint-level evidence.
- Idempotent offline learning-operation queue provides a stable boundary for future authenticated synchronization.
- Versioned learning packages contain workspace, progress, and assignment metadata with JSON escaping, size limits, schema checks, and structural validation.

## ARCore spatial mode

- AR is optional: unsupported devices retain the complete app and an accessible seated spatial simulator.
- Runtime ARCore support/install/update checks and on-demand camera permission.
- Session configuration includes horizontal/vertical planes, environmental HDR, autofocus, and capability-gated Depth.
- Shared mathematical solids, measurements and transforms; explicit `1 unit` to meters/centimeters scale.
- Direct scene drag, pinch and twist gestures with atomic undo, local anchor persistence, placement reset and depth preference.
- Tracking/safety guidance and clear disclosure that spatial measurements are educational estimates.

## Build

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease
```

## APKs

- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release-ready unsigned APK: `app/build/outputs/apk/release/app-release-unsigned.apk`

## Architecture

- `core`: renderer-independent math, expression parsing, 2D geometry, graph analysis, 3D solid measurements/meshes/cross-sections, and 3D surface sampling.
- `learning`: authored lessons, checkpoint evaluation, progress, assignments, teacher summaries, offline operations, and validated learning packages.
- `spatial`: ARCore session/capability boundary, spatial placement, math-to-meter transforms, scale policy, tracking guidance, and simulator fallback.
- `workspace`: workspace model, command-based undo/redo, and versioned JSON export.
- `MainActivity.kt`: Compose app shell and renderers for the four primary workspaces.
- `test`: unit tests for the mathematical acceptance cases.
- `androidTest`: Compose launch and accessibility smoke tests.

## Notes

Studio 3D and the universal AR fallback use Compose Canvas projection. ARCore installation/session/Depth integration and renderer-neutral spatial state are present; a production GPU camera compositor, frame hit-testing and native anchor renderer remain the next physical-device integration step.
