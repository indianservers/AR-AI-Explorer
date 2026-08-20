# Android Cross-Workspace Test Report

Date: 2026-08-20

| Phase | Planned | Executed | PASS | FAIL | BLOCKED | NOT RUN | Gate |
|---|---:|---:|---:|---:|---:|---:|---|
| Phase 1 | 94 | 28 | 23 | 5 | 0 | 66 | FAIL |
| Phase 2 | 73 | 0 | 0 | 0 | 73 | 0 | BLOCKED |
| Phase 3 | 149 | 0 | 0 | 0 | 149 | 0 | BLOCKED |
| Phase 4 | 70 | 0 | 0 | 0 | 70 | 0 | BLOCKED |
| Phase 5 | 51 | 0 | 0 | 0 | 51 | 0 | BLOCKED |

## Executed evidence

- Baseline automated suite: 997 tests, 989 PASS and 8 FAIL.
- Debug app and instrumentation APKs: PASS; installation on `emulator-5554`: PASS.
- Twenty complete five-workspace navigation cycles: PASS before and after shared-header repair; latest run 59.017 s.
- Shared launcher `2D Geometry` → `3D Geometry` route: PASS after repair, 15.075 s.
- 3D content switch preservation and per-module Undo/Redo regressions: PASS.
- Device log window after testing: no app `FATAL EXCEPTION` or app ANR found.

## Phase 1 failures

Five Solver shared-action cases fail: Save, Import, Export, Share, and Help/Settings parity are absent from the Solver workspace surface. Previous individual evidence also leaves 2D Geometry, 3D Geometry, and Solver/CAS gates failed. Phase 1 therefore cannot complete two clean runs.
