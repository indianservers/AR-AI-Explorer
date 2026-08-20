# Geometry gap comparison — Desmos / GeoGebra

Date: 2026-08-20

## Added in this pass

- 2D Geometry now exposes a first-class `Point` add action in the 2D add surface.
- 3D Geometry now has first-class `Point3D` objects with:
  - id and editable name
  - exact `x, y, z` coordinates
  - visibility
  - locked/editable state
  - style key
  - computed distance from origin
  - persistence/export snapshot support
  - canvas rendering, canvas hit selection, layer count/listing, and context/property inspector support

## Emulator verification

- `:app:assembleDebug`: passed.
- `:app:installDebug`: passed on `emulator-5554`.
- 2D Geometry: created/selectable `Point P3`; canvas accessibility reported `Point P3. Parents none. Dependents none`.
- 3D Geometry: add sheet showed `+ Add 3D Point`; creating it produced `Scene 1`; canvas accessibility reported `Point P1 at (0, 0, 0)`.
- Logcat after test: no app `FATAL EXCEPTION`.

## Still missed vs Desmos Geometry

Based on Desmos’ public Geometry pages, Desmos emphasizes constructions, transformations, measurements, algebra/geometry links, image backgrounds, and built-in graphing-calculator connections.

- Rich expression-list/token model for construction history and editable algebraic definitions.
- Stronger live algebra ↔ geometry bridge: expressions from geometry objects usable directly in calculations and transformations.
- Background image workflows for construction over an image.
- More mature transformation workflows for points, lines, figures, and compound transformations.
- More complete measurement workflows: lengths, angles, areas, perimeters as reusable dynamic values.
- Share/gallery workflows and polished construction examples.

## Still missed vs GeoGebra Geometry / 3D

GeoGebra’s official Geometry/3D surfaces include broad 2D construction tools, drag-test dynamic geometry, 3D functions/surfaces, solids, planes, intersections, cross-sections, and save/share workflows.

- Full 2D construction toolbox parity:
  - compass/straightedge-style workflows
  - parallel/perpendicular/tangent constructions
  - conics and locus lines
  - robust polygon/angle/area/length tools
- Dependency-safe dragging for every construction object, with automatic recomputation and “drag test” stability.
- Algebra/CAS-style object definitions and command input.
- Intersections as first-class generated objects across more combinations.
- 3D planes, lines, spheres, surfaces, and intersections/cross-sections as editable construction objects.
- 3D function and parametric surface construction inside the same geometry workflow.
- Save/share/export workflows comparable to GeoGebra materials.
- AR placement/walk-around parity for 3D math objects.
- Custom tool/macro authoring.

## Recommended next backlog

1. Add first-class 3D Line, Segment, Plane, Sphere, and Intersection objects.
2. Convert 2D/3D measurements into reusable dynamic objects, not only inspector text.
3. Add dependency graph visualization and construction replay/history.
4. Add algebra input for geometry objects: `A=(1,2)`, `P=(1,2,3)`, `line(A,B)`, `plane(A,B,C)`.
5. Add GeoGebra-style constraints: parallel, perpendicular, tangent, midpoint, intersection, locus.
6. Add Desmos-style expression list / token navigator for geometry objects.
7. Add import/export/share for reusable constructions.
