# 3D Geometry Defects

| ID | Severity | Status | Summary | Evidence | Fixed in |
|---|---:|---|---|---|---|
| AND-3DG-001 | S2 | Fixed + verified | 3D Geometry module `Clear all` directly cleared workspace without shared confirmation. | Before fix: module button called `vm.clearCurrentWorkspace` directly. After fix: dialog `Clear 3D workspace?`; Cancel preserved Cube; Confirm cleared to `Scene 0`. | `MainActivity.kt` |
| AND-3DG-002 | S3 | Open | Clear All dialog says objects can be restored with Undo, but no Undo affordance was exposed after confirming clear in 3D Geometry. | Post-confirm UI tree contained `Scene 0`, empty canvas, no `Undo` text/content-desc. | N/A |

