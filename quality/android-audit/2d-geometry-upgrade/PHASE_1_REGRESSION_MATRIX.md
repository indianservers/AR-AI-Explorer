# 2D Geometry Upgrade - Phase 1 Regression Matrix

Date: 2026-08-20  
Scope: 2D Geometry workspace only.

| Regression ID | Linked defect | Area | Required retest | Latest result |
|---|---|---|---|---|
| G2D-REG-001 | AND-2DG-P1-001 | Clear All confirmation | Add a user shape, tap Clear All, verify confirmation appears and Cancel preserves the object | Pass in first loop |
| G2D-REG-002 | AND-2DG-P1-001 | Clear All restore | Confirm Clear All, verify empty canvas, then Undo restores the construction | Pass in first loop |
| G2D-REG-003 | AND-2DG-P1-002 | Empty/imported selection | Empty workspace should report no stale selected point/object | Pass in first loop |
| G2D-REG-004 | AND-2DG-P1-003 | Add shape library | Open `+ Add`, verify search field and grouped collapsible panes | Pass in second loop |
| G2D-REG-005 | AND-2DG-P1-003 | Search filtering | Search `line`, verify matching line tools remain and unrelated groups are filtered out | Pass in second loop |
| G2D-REG-006 | AND-2DG-P1-003 | Missing construction entries | Verify Point, Line, Segment, Ray, Vector, Parallel, Perpendicular, Angle Bisector, Circle Through 3, Arc, Regular Polygon are addable entries | Partial emulator visibility plus code verification in second loop |
| G2D-REG-007 | AND-2DG-P1-004 | Selected-object tools | Add/select a construction object and verify Move, Resize, Rotate, arrows, size, and rotate controls | Pass in second loop |
| G2D-REG-008 | AND-2DG-P1-004 | Manipulation smoke | Tap at least one nudge/rotate/size control and verify the app remains responsive | Pass in second loop |
| G2D-REG-009 | AND-2DG-P1-005 | Selected construction Clear All | On a selected construction object, tap visible HUD Clear all and verify shared dialog + clear behavior | Pass after HUD Clear all fix |
| G2D-REG-010 | AND-2DG-P1-006 | Popup-free selected manipulation | Add/select a shape and verify no floating selected-object popup covers the canvas; all buttons remain bottom-only | Pass after popup removal |

## Latest command evidence

- `.\gradlew.bat :app:assembleDebug` - pass.
- `.\gradlew.bat :app:installDebug` - pass on `Medium_Phone_API_35`.
- `.\gradlew.bat :app:testDebugUnitTest --tests "*Geometry2D*" --tests "*WorkspaceDefaultsTest" --tests "*ShapeCatalogDeletionTest"` - pass.
- Emulator logcat after Add/search/manipulation/Clear All smoke - no AIExplorer `FATAL EXCEPTION`.
