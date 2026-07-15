# AI Explorer

AI Explorer is a native Android mathematics exploration app built with Kotlin and Jetpack Compose.

The current implementation replaces the starter XML project with a dark-first Compose application using the package `com.indianservers.aiexplorer`. It includes functional 2D coordinate geometry, 2D graphing, projected 3D solid exploration, 3D surface graphing, command history, workspace JSON export, accessibility descriptions, and automated math tests.

## Direct interaction

- 2D geometry: tap and drag points or complete shapes; drag empty space to pan, pinch to zoom, and double-tap to fit.
- Graphing: drag the trace in Trace mode, drag the canvas in Plot mode, and use two fingers to pan/zoom in every mode.
- 3D solids: drag empty space to orbit, use two fingers to pan/zoom, and use the on-canvas Move/Rotate/Scale modes to transform selected solids. Switch among object, vertex, edge, and face picking, perspective/orthographic cameras, cross-sections, clipping, and wire/filled rendering.
- 3D surfaces: drag to orbit (or move the trace in Trace mode) and use two fingers to pan/zoom.
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

## Build

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease
```

## APKs

- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release-ready unsigned APK: `app/build/outputs/apk/release/app-release-unsigned.apk`

## Architecture

- `core`: renderer-independent math, expression parsing, 2D geometry, graph analysis, 3D solid measurements/meshes/cross-sections, and 3D surface sampling.
- `workspace`: workspace model, command-based undo/redo, and versioned JSON export.
- `MainActivity.kt`: Compose app shell and renderers for the four primary workspaces.
- `test`: unit tests for the mathematical acceptance cases.
- `androidTest`: Compose launch and accessibility smoke tests.

## Notes

This pass intentionally uses a Compose canvas projection for 3D instead of SceneView/Filament. The math engine and mesh generation are isolated from the renderer so a production 3D renderer can be integrated later without rewriting the domain layer.
