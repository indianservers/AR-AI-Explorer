# 3D Geometry Test Report

Legend: Pass = verified in live UI or focused JVM regression; Partial = attempted but not fully UI-verified; Blocked = not exposed/not completed in current phone UI.

| ID | Case | Status | Evidence / issue |
|---|---|---|---|
| A3DG-001 | Open 3D Geometry through normal navigation | Pass | Home → 3D opened workspace. |
| A3DG-002 | Test Clear All | Pass | Fixed `AND-3DG-001`; Cancel and Confirm verified. Undo gap: `AND-3DG-002`. |
| A3DG-003 | Rotate the 3D scene | Partial | 3D orientation/trackball controls present; full gesture sweep not completed. `ENH-3DG-001`. |
| A3DG-004 | Pan the scene | Partial | Workspace supports 3D canvas; full pan gesture verification not completed. `ENH-3DG-001`. |
| A3DG-005 | Zoom the scene | Partial | Retested live on 1080x2400 emulator with Cube in `Scene 1`. Cube remained visible/selectable after zoom-like canvas gestures, but stock adb could not produce a true two-finger pinch, zoom indicator remained `1x`, and Fit/Reset/min/max zoom controls were not exposed through the tested View UI. Evidence: `evidence/3d-geometry/a3dg-005-before.xml`, `a3dg-005-after.xml`, `a3dg-005-after-clear.xml`. `ENH-3DG-001`. |
| A3DG-006 | Use standard camera views | Partial | Orientation cube labels present; all views not exhaustively verified. `ENH-3DG-001`. |
| A3DG-007 | Show and hide scene elements | Partial | Scene controls present; all layers not exhaustively verified. `ENH-3DG-001`. |
| A3DG-008 | Test portrait, landscape, and tablet layouts | Partial | Phone portrait tested only. `ENH-3DG-004`. |
| A3DG-009 | Create a free 3D point | Blocked | Not exposed in tested phone UI. `ENH-3DG-001`. |
| A3DG-010 | Create an exact-coordinate point | Blocked | Not exposed in tested phone UI. `ENH-3DG-001`. |
| A3DG-011 | Rename and style points | Blocked | Point workflow not exposed. `ENH-3DG-001`. |
| A3DG-012 | Select overlapping points | Blocked | Point workflow not exposed. `ENH-3DG-001`. |
| A3DG-013 | Create a 3D segment | Blocked | Not exposed in tested phone UI. `ENH-3DG-001`. |
| A3DG-014 | Create a 3D line | Blocked | Not exposed in tested phone UI. `ENH-3DG-001`. |
| A3DG-015 | Create a 3D ray | Blocked | Not exposed in tested phone UI. `ENH-3DG-001`. |
| A3DG-016 | Create a vector between points | Partial | Vector engines/source covered by focused regression; UI path not completed. `ENH-3DG-001`. |
| A3DG-017 | Create a vector from components | Partial | Vector engines/source covered by focused regression; UI path not completed. `ENH-3DG-001`. |
| A3DG-018 | Add and subtract vectors | Partial | Engine/source covered; UI path not completed. `ENH-3DG-001`. |
| A3DG-019 | Multiply a vector by a scalar | Partial | Engine/source covered; UI path not completed. `ENH-3DG-001`. |
| A3DG-020 | Calculate dot product and vector angle | Partial | Engine/source covered; UI path not completed. `ENH-3DG-002`. |
| A3DG-021 | Calculate cross product | Partial | Engine/source covered; UI path not completed. `ENH-3DG-002`. |
| A3DG-022 | Create parallel and perpendicular lines | Blocked | Not exposed in tested phone UI. `ENH-3DG-001`. |
| A3DG-023 | Create and identify skew lines | Blocked | Not exposed in tested phone UI. `ENH-3DG-001`. |
| A3DG-024 | Create a plane through three points | Blocked | Not exposed in tested phone UI. `ENH-3DG-001`. |
| A3DG-025 | Create a plane from an equation | Blocked | Not exposed in tested phone UI. `ENH-3DG-001`. |
| A3DG-026 | Create a plane using point and normal | Blocked | Not exposed in tested phone UI. `ENH-3DG-001`. |
| A3DG-027 | Create parallel planes | Blocked | Not exposed in tested phone UI. `ENH-3DG-001`. |
| A3DG-028 | Create perpendicular planes | Blocked | Not exposed in tested phone UI. `ENH-3DG-001`. |
| A3DG-029 | Intersect a line and plane | Partial | Engine/source covered; UI path not completed. `ENH-3DG-002`. |
| A3DG-030 | Intersect two planes | Partial | Engine/source covered; UI path not completed. `ENH-3DG-002`. |
| A3DG-031 | Intersect three planes | Partial | Engine/source covered; UI path not completed. `ENH-3DG-002`. |
| A3DG-032 | Measure point-to-plane distance | Partial | Engine/source covered; UI path not completed. `ENH-3DG-002`. |
| A3DG-033 | Measure point-to-line distance | Partial | Engine/source covered; UI path not completed. `ENH-3DG-002`. |
| A3DG-034 | Measure line-plane and plane-plane angles | Partial | Engine/source covered; UI path not completed. `ENH-3DG-002`. |
| A3DG-035 | Create a cube | Pass | Cube added through Add sheet; `Scene 1` verified. |
| A3DG-036 | Create a cuboid | Partial | Solid catalog/JVM covered; individual UI add not completed. `ENH-3DG-001`. |
| A3DG-037 | Create a triangular prism | Partial | Solid catalog/JVM covered; individual UI add not completed. `ENH-3DG-001`. |
| A3DG-038 | Create general prisms | Partial | Solid catalog/JVM covered; individual UI add not completed. `ENH-3DG-001`. |
| A3DG-039 | Create a square pyramid | Partial | Solid catalog/JVM covered; individual UI add not completed. `ENH-3DG-001`. |
| A3DG-040 | Create a tetrahedron | Partial | Solid catalog/JVM covered; individual UI add not completed. `ENH-3DG-001`. |
| A3DG-041 | Create a general pyramid | Partial | Solid catalog/JVM covered; individual UI add not completed. `ENH-3DG-001`. |
| A3DG-042 | Create a cylinder | Partial | Solid catalog/JVM covered; individual UI add not completed. `ENH-3DG-001`. |
| A3DG-043 | Create a cone | Partial | Solid catalog/JVM covered; individual UI add not completed. `ENH-3DG-001`. |
| A3DG-044 | Create a sphere | Partial | Solid catalog/JVM covered; individual UI add not completed. `ENH-3DG-001`. |
| A3DG-045 | Create a hemisphere | Partial | Solid catalog/JVM covered; individual UI add not completed. `ENH-3DG-001`. |
| A3DG-046 | Create a frustum | Partial | Solid catalog/JVM covered; individual UI add not completed. `ENH-3DG-001`. |
| A3DG-047 | Create a torus | Partial | Solid catalog/JVM covered; individual UI add not completed. `ENH-3DG-001`. |
| A3DG-048 | Create supported regular polyhedra | Partial | Solid catalog/JVM covered; individual UI add not completed. `ENH-3DG-001`. |
| A3DG-049 | Generate solid nets | Blocked | Not exposed/completed in tested phone UI. `ENH-3DG-004`. |
| A3DG-050 | Create cross-sections | Partial | Related source paths exist; UI path not completed. `ENH-3DG-002`. |
| A3DG-051 | Intersect a line with a solid | Partial | Related source paths exist; UI path not completed. `ENH-3DG-002`. |
| A3DG-052 | Intersect two solids | Partial | Related source paths exist; UI path not completed. `ENH-3DG-002`. |
| A3DG-053 | Translate 3D objects | Partial | Move tool present; exact transform verification not completed. `ENH-3DG-001`. |
| A3DG-054 | Rotate 3D objects | Partial | Rotate tool present; exact transform verification not completed. `ENH-3DG-001`. |
| A3DG-055 | Reflect 3D objects | Blocked | Not exposed in tested phone UI. `ENH-3DG-001`. |
| A3DG-056 | Dilate 3D objects | Partial | Resize tool present; exact dilation verification not completed. `ENH-3DG-001`. |
| A3DG-057 | Test transparency and hidden surfaces | Partial | Theme/display controls present; full hidden-surface test not completed. `ENH-3DG-001`. |
| A3DG-058 | Perform long Undo/Redo | Blocked | Undo not exposed after Clear All. `AND-3DG-002`. |
| A3DG-059 | Save, reopen, and export a complex scene | Blocked | Not completed in this pass. `ENH-3DG-004`. |
| A3DG-060 | Stress-test 3D Geometry | Partial | Focused JVM regression passed; full device stress not completed. `ENH-3DG-004`. |
