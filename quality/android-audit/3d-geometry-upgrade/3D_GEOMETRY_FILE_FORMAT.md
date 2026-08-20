# 3D Geometry File Format

Status: FAIL for new construction-object schema.

Current state:

- Existing workspace state already persists `points3D` and `vectors3D`.
- Segment, Line, and Ray added in this pass are vector-backed templates named `segN`, `lineN`, and `rayN`.

Missing:

- Dedicated persisted types for 3D Segment, Line, Ray, Plane, Sphere construction objects, and Intersections.
- Versioned migration notes for the new 3D construction schema.
- Import/export contract for reusable 3D constructions.
