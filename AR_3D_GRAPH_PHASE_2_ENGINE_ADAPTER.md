# AR 3D Graph — Phase 2 Engine Adapter

## Integration points

`Existing3DGraphEngineBridge` calls the exact public paths used by the normal 3D Graph workspace:

- Explicit: `SurfaceInputInterpreter.interpret` and `Graph3D.mesh`.
- Implicit: `SurfaceInputInterpreter.interpret` and `TypedSurfaceMesher.mesh` with `SurfaceDefinition3D.Implicit`.
- Parametric surface: the same interpreter and typed mesher with `SurfaceDefinition3D.Parametric`.
- Styling: `SpatialSurfaceLayer.workspaceAppearance()` and `WorkspaceVisualStyles.Spectral.axes`.

The bridge validates only adapter-level request boundaries (ordered enabled equations, increasing domain, and the existing 8–56 density range). Parsing, mathematical evaluation, sampling, tolerances, discontinuity behavior, and native typed topology remain in the existing engines.

## Contract

Input is `GraphGenerationRequest`: an ordered immutable equation list plus domain minimum/maximum and density. Each equation retains ID, expression, enabled state, color index, and opacity.

Success returns ordered `EngineMeshSnapshot` values containing canonical equation, exact double vertices, rows/columns, native indices where supplied, palette, line colour, and opacity. Failures are separated into `ValidationError` and `GenerationError`; causes never escape into the UI/render loop.

## Representation-only conversion

`GraphEngineAdapter`:

1. copies engine vectors to AR vector records without numerical modification;
2. preserves native typed-mesh triangle indices and order exactly;
3. derives the same two-triangle regular-grid topology only when `Graph3D` exposes rows/columns but no index array;
4. derives vertex normals from that final topology because the normal renderer also derives normals at render preparation time;
5. samples the engine-owned palette across the unchanged z range and applies existing opacity;
6. derives per-mesh and aggregate bounds;
7. creates renderer-only axis/grid line records using the existing spectral axis colours.

No equation is reparsed or reevaluated in `ar3dgraph`. No engine-owned list is mutated.

## Parity evidence

`AR3DGraphEngineParityTest` compares the bridge and adapter with direct existing-engine calls for all 14 required explicit equations. Vertices, counts, rows, columns, derived topology, domain, density, and bounds are checked exactly. It also compares native vertices and triangle indices exactly for an implicit sphere and parametric torus, and verifies ordered multiple equations, styles, transparency, invalid input, empty input, domain errors, and extreme/non-finite cases.

Parametric curves are not currently an output type of the normal surface workspace, so Phase 2 does not invent them.

