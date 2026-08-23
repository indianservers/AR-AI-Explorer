# AR 3D Graph — Phase 2 Architecture

## Scope and boundary

Phase 2 adds engine reuse, immutable render-data conversion, AR camera rendering, and direct world-pose placement. It does not add Phase 3 gestures, export/share, final polish, performance tuning, accessibility completion, physical-device coverage, or release validation.

The dependency direction remains acyclic:

```text
app (owns existing 3D engines)
  ├─ Existing3DGraphEngineBridge
  └─ depends on ar3dgraph
         ├─ GraphEngineContract
         ├─ GraphEngineAdapter
         ├─ ARCore lifecycle and placement
         └─ OpenGL ES AR renderer
```

`ar3dgraph` cannot depend on `app`, so the app-side bridge implements the AR module's contract. This is a read-only boundary: engine-owned collections are copied into immutable transfer models, and the AR module holds no Activity in the adapter and owns no mathematical parser or sampler.

## Existing data flow traced

The normal explicit-surface route is `SurfaceInputInterpreter` → `Graph3D.mesh(expression, min, max, density)` → `SurfaceMesh`. Implicit and parametric surfaces use `SurfaceInputInterpreter` → `TypedSurfaceMesher.mesh(definition, density).geometry`. The normal workspace derives regular-grid topology, normals, and palette samples for rendering; axes and grid are renderer-level constructs.

Inputs are equation text, surface kind, square domain, density, enabled state, palette index, and opacity. Outputs are double-precision vertices, row/column dimensions or native triangle indices, canonical equation identity, palette/material appearance, and bounds derivable without changing vertices. Explicit surfaces use the engine's regular `(x,y,z)` grid; typed geometry supplies native vertices and triangle order. Mathematical z remains upright after a graph-root-only `-90°` X rotation maps the normal graph plane to AR world space.

## Runtime ownership

- `AR3DGraphViewModel` owns UI state and one cancellable generation future.
- `Existing3DGraphEngineBridge` invokes the existing public engines and returns snapshots.
- `GraphEngineAdapter` performs representation-only topology, normal, colour-buffer, bounds, axes, and grid preparation.
- `ARGraphCameraView` owns the `GLSurfaceView` and forwards taps.
- `ARGraphRenderer` owns camera/graph GL resources and frame rendering.
- `ARCoreSessionManager` owns one optional ARCore `Session`.
- `AnchorPlacementController` owns at most one active placement anchor.

Geometry is generated and converted off the main thread, uploaded only when its revision changes, and never recalculated per AR frame. Frame work is limited to camera update, matrices, and drawing.

## Protection proof

No existing engine was copied, moved, renamed, or edited. A SHA-256 comparison against `C:\Indian Servers\AIExplorer-backup-20260821-104943.zip` found all 13 protected files identical. The protected `Graph3DScreen`, `Graph3DEquationPanel`, and `Graph3DPropertiesPanel` blocks in `MainActivity.kt` are also identical to that backup. The only Phase 2 app connection is an additive bridge and the existing AR destination's injected bridge instance.

