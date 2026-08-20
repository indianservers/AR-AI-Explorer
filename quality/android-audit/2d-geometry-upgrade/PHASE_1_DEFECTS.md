# 2D Geometry Upgrade — Phase 1 Defects

| ID | Test case | Severity | Status | Summary | Root cause | Files changed |
|---|---|---:|---|---|---|---|
| AND-2DG-P1-001 | G2D-P1-003 | P1 | Fixed + emulator verified | 2D Geometry `Clear all` bypassed confirmation and cleared immediately. | `Geometry2DScreen` wired bottom dock directly to `vm.clearCurrentWorkspace` instead of shared confirmation dialog. | `MainActivity.kt` |
| AND-2DG-P1-002 | G2D-P1-002 | P2 | Fixed + emulator verified | Empty/imported 2D workspaces could retain an invalid selected point index. | `selectedPoint` initialized to `1`; hydrate/import normalization coerced empty states to `0` instead of `-1`. | `MainActivity.kt` |
| AND-2DG-P1-003 | G2D-P1-009, G2D-P1-010 | P2 | Fixed + emulator verified | 2D Add Shapes box had no search, no collapsible grouping, and missed direct construction entries such as Point, Line, Segment, Ray, Vector, Parallel, Perpendicular, Angle Bisector, Circle-through-3, Arc, and Regular Polygon. | Add flow only rendered the preset `ShapeExplorer2DShapes` list as a flat button row. | `MainActivity.kt` |
| AND-2DG-P1-004 | G2D-P1-011 | P2 | Fixed + emulator verified | Selected 2D objects did not expose obvious Move, Resize, Rotate icon/arrows in the object overlay, making direct manipulation look missing. | Manipulation controls existed mostly as text dock buttons/drag handles, but the selected-object overlay did not include a discoverable quick HUD. | `MainActivity.kt` |
| AND-2DG-P1-005 | G2D-P1-003 | P2 | Fixed + emulator verified | During the 2026-08-20 13:02 IST retest, tapping the bottom-dock `Clear all` on a selected construction was not reliable through the tested coordinate path. | Bottom-dock Clear all is nested in an expandable header area and can be awkward to hit during selected-object workflows. Added a dedicated selected-object HUD `Clear all` wired to the same shared confirmation dialog. | `MainActivity.kt` |
| AND-2DG-P1-006 | G2D-P1-011 | P1 | Fixed + emulator verified | Selected-object popup blocked touch interaction with the canvas after adding/selecting a shape. | The selected-object quick HUD was rendered over the canvas. Disabled the floating popup and moved nudge/resize/rotate/clear controls into the bottom dock only. | `MainActivity.kt`, `Geometry2DControls.kt` |

## Emulator evidence

- `g2d-clear-dialog.xml`: shared confirmation dialog opened with `Clear 2D workspace?`.
- `g2d-after-cancel.xml`: Cancel dismissed the dialog and preserved the Square.
- `g2d-after-confirm.xml`: Confirm cleared to empty 2D canvas.
- `g2d-after-header-expand.xml`: compact top header exposed Undo after expansion.
- `g2d-after-undo-clear.png`: Undo restored the square visually.

## 2026-08-20 Add Shapes / manipulation evidence

- Live emulator reused the running 2D app session after installing the modified build.
- Add 2D Shape dialog showed `Search shapes and tools`, `Basics`, `Lines & Rays`, and construction entries including `Point`, `Line`, `Segment`, and `Ray`.
- Search query `line` filtered the Add list to matching line/ray/vector/advanced construction options.
- Adding `Line` from filtered results selected the new line and showed `Selected object tools`.
- HUD buttons/arrows were visible: `Move ↕↔`, `Resize ⤢`, `Rotate ⟳`, `←`, `↑`, `↓`, `→`, `Size −`, `Size +`, `Rot −15°`, `Rot +15°`.
- Logcat check after the Add/search/manipulation flow found no AIExplorer `FATAL EXCEPTION`.

## 2026-08-20 Clear All follow-up evidence

- Added selected-object HUD `Clear all`.
- Emulator path: Home → 2D → `+ Add` → search `line` → add `Line` → HUD `Clear all`.
- HUD `Clear all` opened shared dialog `Clear 2D workspace?`.
- Confirming dialog cleared the workspace to canvas state `No selection`.

## 2026-08-20 popup removal evidence

- Disabled the floating selected-object popup so it no longer covers the graph/canvas.
- Added bottom-dock selected-object controls: `Move`, `Size`, `Rot`, arrows `← ↑ ↓ →`, `- Size`, `+ Size`, `-15°`, `+15°`, `Free/Ratio`, and `Clear`.
- Emulator path: Home → 2D → `+ Add` → search `line` → add `Line`.
- Verified `Selected object tools` no longer appears in the UI hierarchy after selecting the Line.
- Verified bottom controls remain visible and tappable; no AIExplorer `FATAL EXCEPTION` in checked logcat window.
