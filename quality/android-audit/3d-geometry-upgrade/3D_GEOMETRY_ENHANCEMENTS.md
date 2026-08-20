# 3D Geometry Enhancements

Implemented:

- Added 3D point drag lifecycle in the view model and renderer callback contract.
- Added Segment, Line, and Ray quick construction templates.
- Exposed Point, Segment, Line, Ray, and Vector in the 3D Add sheet.
- Wrapped construction buttons for phone portrait.
- Added bottom selected-object controls for 3D points/vectors: X-/X+/Y+/Z+ arrows and Delete.
- Made Delete target accept selected solids, vectors, and points.
- Made Clear All enable when the scene contains points only.

Recommended next:

- Promote Segment/Line/Ray from vector-named templates into separate persisted object types with object-specific rendering.
- Add first-class 3D planes and intersections.
- Add algebra input parser and expression list for 3D geometry.
- Add full dynamic measurement objects.
- Add construction replay/dependency graph UI.
