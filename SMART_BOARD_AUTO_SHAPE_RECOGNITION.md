# Smart Board Auto-Shape Recognition

## Implemented behavior

The recognizer is deterministic, bounded and offline. It groups recent spatially related strokes after a configurable pause, simplifies geometry, ranks candidates and offers a preview. It never replaces ink until the user accepts.

Supported structured vocabulary includes:

- lines, segments, rays, arrows and vectors
- circles, ellipses, squares, rectangles and polygons
- triangles, right/equilateral triangles, pentagons, hexagons and stars
- arcs, curves and closed regions
- axes, number lines, angles, parallel/perpendicular marks and graph grids
- Physics-oriented force arrows, springs, resistors, wires and nodes
- text/flowchart/table/grid/lab-container primitives

The current geometric fitter has dedicated production fits for the common primitives and a structured vocabulary for later specialist detectors. Low-confidence and handwriting-like oscillatory paths are rejected.

## Recognition sequence

1. Preserve pressure-aware vector ink.
2. Select recent strokes by time and expanded spatial bounds.
3. Apply bounded point sampling and Douglas–Peucker simplification.
4. Detect closed/open structure and multi-stroke relationships.
5. Rank candidates with confidence and rationale.
6. Show alternatives on the canvas.
7. On acceptance, create `ShapeElement`, link source strokes and optionally hide—not delete—the ink.
8. Undo restores the original visibility and removes the clean shape.

## Interaction

- Automatic suggestions can be disabled.
- Pause delay is configurable from 300–3000 ms.
- Manual “Recognize shape” works on selected strokes.
- Selected shapes support rotate, lock/unlock and style cycling.
- Geometry shapes can open the 2D Geometry workspace.
- Axes, number lines and graph grids can open the Graph workspace.

## Safety and quality constraints

- Input size and point sampling are bounded.
- NaN/infinite geometry is rejected by model invariants.
- Recognition is local and network-free.
- Source ink is recoverable.
- Candidate confidence is visible.
- The system declines uncertain handwriting instead of forcing a shape.
