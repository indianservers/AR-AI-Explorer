# AR Module Audit and Compatibility Matrix

## Architecture

- `arengine`: renderer-neutral ARCore runtime contracts, production `ArCoreRuntime`, fake runtime, anchors, hit ranking, scene contracts, interaction engines, analysis, rendering policies, units, measurement and fitting.
- `ar3dgraph`: dedicated AR 3D graph feature using SceneView/Filament, ARCore session checks, plane placement, graph mesh rendering and graph gestures.
- `app/.../spatial`: existing shared OpenGL ES 3 camera compositor and mathematical scene renderer used by the Spatial AR shapes/geometry workspace.
- `app/.../ARCoordinatePlaneScreen.kt` and `ARVectorLabScreen.kt`: dedicated AR Labs screens using `SharedARLabCamera` and the shared `ArLabSessionController`.
- ARCore dependency: `com.google.ar:core:1.54.0`.
- SceneView dependency: `io.github.sceneview:arsceneview:4.32.0` (AR 3D Graph only).
- Rendering: custom OpenGL ES 3 compositor for Spatial AR and AR Labs; SceneView backed by Filament for AR 3D Graph; deterministic simulator/CPU fallbacks remain available.

## Existing Runtime Support

| Capability | Existing status | Upgrade status |
| --- | --- | --- |
| Plane detection | Horizontal and vertical | Preserved |
| Hit testing | Plane, depth, oriented point, instant placement | Reused by measurement and fitting |
| Anchors | Registry with explicit detach/cleanup | Measurement and fit anchor ownership added |
| Point clouds | Not used as reconstruction input | Intentionally unchanged |
| Depth | Automatic Depth with CPU snapshots | Raw Depth capability exposed; fallback preserved |
| Instant Placement | Supported with configuration fallback | Preserved |
| Occlusion | Depth-aware compositor | Preserved |
| Lighting | Environmental HDR, ambient fallback | Preserved |
| Gestures | Placement, translate, rotate, scale, snapping | Preserved |
| Measurement | Exact scene analysis and environmental estimates | Shared distance, angle, polygon, circle and fitting math added |
| Labels/formulas | Scene annotations and formula panels | Fit formulas and uncertainty-safe formatting added |
| Procedural geometry | Graph meshes and solid scene compiler | Fitted solids feed existing compiler |
| Lifecycle | Permission, install, pause/resume, close | Shared AR Labs controller added |
| Unsupported devices | Spatial simulator and 3D Viewer | Preserved |

## Lab Matrix

`OPEN` means a routed implementation exists. `PLANNED` means the catalog entry exists but no dedicated route existed at audit time.

| Lab | Status | Current purpose | Measurement | Fitting | Depth | Upgrade |
| --- | --- | --- | --- | --- | --- | --- |
| AR 3D Graph | OPEN | Place graph surfaces | Useful | No | Useful | Existing anchored SceneView workflow retained |
| AR 3D Shapes | OPEN | Solids, vectors and sections | Yes | Yes | Yes | Guided cylinder/cone/sphere/cuboid fitting added |
| AR 2D Shapes | OPEN/shared | Constructions on surfaces | Yes | No | Useful | Existing Spatial AR tools retained |
| AR Coordinate Plane | OPEN | Points, lines, slope and distance | Yes | No | Useful | Anchored points, line/measure/polygon modes added |
| AR Vector Lab | OPEN | Vector operations | Yes | No | Useful | Shared live camera/session foundation added |
| AR Trigonometry Lab | PLANNED | Unit circle and trig values | Yes | No | Useful | Capability profile only |
| AR Calculus Lab | PLANNED | Tangents, sums and integration | Useful | No | Useful | Capability profile only |
| AR Geometry Construction | PLANNED | Compass/ruler constructions | Yes | No | Useful | Capability profile only |
| AR Solids Dissection | PLANNED | Faces, edges, vertices and nets | Useful | Yes | Useful | Capability profile only |
| AR Volume Explorer | PLANNED | Volume formula visualization | Yes | Yes | Useful | Capability profile only |
| AR Transformation Lab | PLANNED | Translation/rotation/reflection/scale | Useful | No | Useful | Capability profile only |
| AR Physics-Math Workspace | PLANNED | Motion, vectors and waves | Yes | No | Useful | Capability profile only |
| AR Statistics Workspace | PLANNED | Spatial charts | Limited | No | No | Capability profile only |
| AR Number Line | PLANNED | Floor number line | Useful | No | Optional | Capability profile only |
| AR Function Machine | PLANNED | Input/process/output | No | No | No | Capability profile only |
| AR Mathematical Art | PLANNED | Fractals and mathematical art | No | No | Useful | Capability profile only |
| AR Formula Universe | PLANNED | Visual formula explanations | Useful | No | Useful | Capability profile only |
| AR Math Museum | PLANNED | Mathematical exhibits | No | No | Useful | Capability profile only |

## Reliability Findings

- The original AR 3D Graph and Spatial AR features use separate session/rendering integrations. They cannot conflict at runtime because only one routed workspace is composed at a time, but they are not yet one implementation class.
- The previous Coordinate and Vector designs displayed an AR-style background without owning live hit-tested mathematical content. They now use the shared AR Labs camera/session controller; Coordinate points are anchored world measurements.
- Low light, excessive motion and insufficient features are mapped to student-facing guidance.
- Measurement output is explicitly approximate and rounded from uncertainty; it is not suitable for engineering use.
- Raw Depth support is detected, but Raw Depth reconstruction is intentionally not used as an object scanner.
- The catalog contains 18 workspaces, not 15 working implementations. Thirteen remain planned and were not represented as completed features.

## Physical Device Checklist

1. Launch each open AR route and verify only one camera session owns the camera.
2. Deny and grant camera permission; verify simulator fallback and recovery.
3. Verify ARCore install/update flow on a supported device without current services.
4. Scan horizontal and vertical textured surfaces in bright and dim conditions.
5. Place a construction, walk left/right/up/down/forward/back and rotate the phone around it.
6. Background and foreground the app; verify anchors and construction state recover.
7. Coordinate Plane: place A/B/C, verify anchored points, distance, angle, undo and reset.
8. Fit each supported primitive around a real object; verify points, formulas, editable solid and lock.
9. Toggle Depth occlusion on a Depth device and verify fallback on a non-Depth device.
10. Verify portrait and landscape layouts and inspect logcat for camera, GL or anchor errors.
