# 3D Geometry Master Test Plan

Date: 2026-08-20
Scope: Phase 1 smoke/regression for the Android 3D Geometry workspace after exposing point and basic construction controls.

## Phase 1 test cases

| ID | Case | Status | Notes |
| --- | --- | --- | --- |
| 3D-P1-001 | Build debug Kotlin after code changes | PASS | `./gradlew.bat :app:compileDebugKotlin` succeeded. |
| 3D-P1-002 | Install debug APK to emulator | PASS | `./gradlew.bat :app:installDebug` installed to `Medium_Phone_API_35`. |
| 3D-P1-003 | Launch app and navigate Home to 3D normally | PASS | Used one relaunch after reinstall; then normal Home → Open 3D navigation. |
| 3D-P1-004 | Open 3D Add sheet | PASS | Add sheet opened from bottom `+ Add`. |
| 3D-P1-005 | Point, Segment, Line, Ray, Vector actions visible on phone portrait | PASS | Row wraps; Ray and Vector are visible/reachable on second row. |
| 3D-P1-006 | Add Segment construction | PASS | `Vector seg1` created and selected. |
| 3D-P1-007 | Selected vector bottom controls visible | PASS | Arrow controls, Delete, Clear all exposed after selection. |
| 3D-P1-008 | Clear All enabled for point/vector-only scene | PASS | Clear All was enabled with persisted point and after vector add. |
| 3D-P1-009 | Clear All confirmation and empty-canvas return | PASS | Confirmation appeared; confirmed clear returned to empty 3D canvas. |
| 3D-P1-010 | Crash check after 3D add/clear flow | PASS | Recent logcat had no `FATAL EXCEPTION`/`AndroidRuntime` crash. |
| 3D-P1-011 | Direct touch drag point | NOT RUN | Code path added; not completed in emulator before report cutoff. |
| 3D-P1-012 | Direct touch drag vector | NOT RUN | Existing vector drag path remains; not completed in emulator before report cutoff. |

## Exit gate

Phase 1 is not fully closed because touch-drag point/vector manipulation and broad construction-object regression remain NOT RUN.
