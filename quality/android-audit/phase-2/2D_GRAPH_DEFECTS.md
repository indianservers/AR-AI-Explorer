# 2D Graph Defects

| ID | Severity | Status | Summary | Evidence | Fixed in |
|---|---:|---|---|---|---|
| AND-2DGR-001 | S2 | Fixed + verified | Graph module `Clear all` directly cleared workspace without shared confirmation. | After fix: dialog `Clear Graph workspace?`; Cancel preserved `f(x)`; Confirm cleared to `Equations (0)`. | `MainActivity.kt`, `Graph2DWorkspaceUi.kt` |
| AND-2DGR-002 | S3 | Open | Clear All dialog says objects can be restored with Undo, but no Undo affordance was exposed after confirming clear in Graph. | Post-confirm UI tree contained `Equations (0)`, no `Undo` text/content-desc. | N/A |
| AND-2DGR-003 | S4 | Open | Graph editor can obscure workspace-level Clear All until Back dismisses the selected expression editor. | With `f(x)` editor open, bottom workspace toolbar was hidden; Back returned to canvas with Clear All reachable. | N/A |

