# 3D Geometry Defects

## Fixed in this pass

| ID | Severity | Defect | Status |
| --- | --- | --- | --- |
| 3D-DEF-001 | P1 | 3D Clear All disabled when only points existed. | PASS |
| 3D-DEF-002 | P1 | 3D Add sheet did not expose Segment, Line, Ray construction actions. | PASS |
| 3D-DEF-003 | P1 | New construction actions clipped on phone portrait when placed in a single row. | PASS |
| 3D-DEF-004 | P1 | Selected vector/point did not have bottom direct manipulation/delete controls. | PASS |
| 3D-DEF-005 | P2 | Touch-selectable 3D points could not be dragged. | PASS by code compile; emulator drag NOT RUN. |

## Still open / not fully implemented

| ID | Severity | Defect | Status |
| --- | --- | --- | --- |
| 3D-DEF-006 | P1 | Segment/Line/Ray are backed by vector templates, not separate first-class persisted construction types. | FAIL |
| 3D-DEF-007 | P1 | Plane, Sphere construction objects beyond existing solid sphere are not implemented as GeoGebra-style editable construction objects. | FAIL |
| 3D-DEF-008 | P1 | Algebra input for `P=(1,2,3)`, `line(A,B)`, `plane(A,B,C)` is not implemented in this pass. | FAIL |
| 3D-DEF-009 | P1 | Dependency graph/replay/history visualization is not implemented in this pass. | FAIL |
| 3D-DEF-010 | P1 | Save/share/export workflows comparable to GeoGebra materials are not verified in this pass. | NOT RUN |
