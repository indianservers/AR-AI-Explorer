# 2D Geometry Upgrade — Phase 1 Test Matrix

Date: 2026-08-20  
Scope: 2D Geometry workspace and directly shared Clear All/selection infrastructure.

## Matrix

| ID | Area | Live UI expectation | Status |
|---|---|---|---|
| G2D-P1-001 | Normal navigation | Open Home → 2D Geometry without direct activity launch | Pass |
| G2D-P1-002 | Empty state | Empty workspace must have no stale selected point/object | Pass |
| G2D-P1-003 | Clear All safety | Clear All opens confirmation, Cancel preserves objects | Pass |
| G2D-P1-004 | Clear All restore | Confirm Clear All empties workspace and Undo restores it | Pass |
| G2D-P1-005 | Add point | Add point through live UI/canvas | Not run in this loop |
| G2D-P1-006 | Add shape | Add a basic shape through `+ Add` | Pass |
| G2D-P1-007 | Selection | Select created object/point without stale state | Partial: Square created/visible, not reselected after restore |
| G2D-P1-008 | View controls | Fit/Undo view controls remain usable | Partial: compact Undo found through expanded header; Fit not retested |
| G2D-P1-009 | Add Shapes search | Add box exposes search and filters shape/tool entries | Pass: searched `line`, list filtered to matching line/ray/vector/advanced entries |
| G2D-P1-010 | Add Shapes grouping | Shape/tool entries are grouped into collapsible panes | Pass: verified `Basics`, `Lines & Rays`; implementation includes Triangles, Quadrilaterals, Curves, Polygons, Advanced constructions |
| G2D-P1-011 | Selected object manipulation | After selecting an object, Move/Resize/Rotate buttons with icons/arrows are visible and usable | Pass: Line selected; HUD showed Move/Resize/Rotate, arrows, size, rotate buttons; no crash after taps |
| G2D-P1-012 | Missing 2D construction shapes | Point and missing direct construction shapes are available from Add box | Pass: implementation adds Point, Line, Segment, Ray, Vector, Parallel, Perpendicular, Angle Bisector, Circle Through 3, Arc, Regular Polygon |
| G2D-P1-013 | Selected construction Clear All | Reuse app, tap visible HUD Clear all on selected construction | Pass after fix: HUD `Clear all` opened `Clear 2D workspace?`; confirm cleared to `No selection`; tracked as `AND-2DG-P1-005` |
| G2D-P1-014 | No canvas-blocking selected popup | Adding/selecting a shape must not show a large popup over the canvas | Pass: floating popup disabled; controls appear bottom-only; tracked as `AND-2DG-P1-006` |

Full 75-operation Phase 1 execution is not yet complete. This matrix records the implementation loops, build/install, and live emulator verification for the defects fixed here.
