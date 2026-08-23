# AR 3D Graph Phase 2C — Placement Accuracy

Result: **Numerical PASS; live ARCore placement BLOCKED**

## Accuracy matrix

The automated matrix covers centre, four corners, four edge centres, 100 deterministic random points, portrait, landscape, square and synthetic tablet dimensions, viewport-local inset coordinates, alternate projections, translated cameras, yaw/pitch variations and repeated poses.

For every accepted ray:

`position = cameraPosition + normalize(rayDirection) × 1.5 m`

The numerical tolerance is `1e-5 m`; direction normalization uses `1e-6`. All accepted poses were finite, in front of the camera and exactly 1.5 m within tolerance. Invalid dimensions, non-finite coordinates, singular matrices, invalid homogeneous values and out-of-viewport coordinates return controlled failures.

## Orientation

Asymmetric equations include `z=x+2y`, `z=2x-3y`, `z=x²+y`, `z=sin(x)+0.5y` and `z=exp(-x²)+y`. Placement yaw faces the camera while quaternion X and Z remain zero, preventing camera roll from tilting the graph. The renderer applies a root-only coordinate transform; engine vertices are never swapped or negated.

## Exclusions

Static audit found no `hitTest`, plane renderer, horizontal/vertical plane mode, `DepthPoint` or `InstantPlacementPoint`. Session configuration explicitly sets plane finding, instant placement and depth to `DISABLED`.

Live confirmation of visibility, mirroring, projected axes and real ARCore camera poses requires an ARCore-capable device.
