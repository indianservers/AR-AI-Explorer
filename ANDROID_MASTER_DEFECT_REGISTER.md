# Android Master Defect Register

Date: 2026-08-20

| ID / severity | Case | Root cause / evidence | Fix | Status |
|---|---|---|---|---|
| AND-NAV-P1-007 / P1 | INT-P1-NAV-003 | Visible launcher entries fell through to a placeholder. Baseline: `quality/android-final-audit/baseline/main-menu.xml`. | Map 2D/3D Geometry and Graph entries to real modules; permanent UI regression added. | PASS |
| AND-ISO-P1-008 / P1 | INT-P1-ISO-001 | `open(Geometry3D)` explicitly cleared solids/vectors when arriving from another module. | Removed destructive re-entry branch; unit and device direct-switch evidence pass. | PASS |
| AND-ISO-P1-009 / P1 | INT-P1-ISO-006 | One global `CommandHistory` served all non-Solver modules. | Added `WorkspaceHistoryByModule`; replaced old cross-module expectation. | PASS |
| AND-A11Y-P2-010 / P2 | INT-P1-NAV-003 | Compact Back/Menu buttons had no accessible names. Device test could not find Menu. | Added `Back to previous screen` and `Open Maths menu` descriptions. | PASS |
| AND-ACT-P1-011 / P1 | INT-P1-ACT-004..008 | Solver header exposes Undo/Redo/Clear All but no shared Save, Import, Export, Share, Help or Settings workflow. | Major persistence/action capability intentionally not added during integration-only audit. | FAIL |
| AND-GATE-P1-012 / P1 | Final gate | 3D audit has four open P1 construction/schema defects and status FAIL. | See `quality/android-audit/3d-geometry-upgrade/3D_GEOMETRY_DEFECTS.md`. | FAIL |
| AND-GATE-P1-013 / P1 | Final gate | Solver/CAS file format is absent and Solver individual readiness is FAIL. | See `SOLVER_CAS_FILE_FORMAT.md` and `SOLVER_CAS_RELEASE_READINESS.md`. | FAIL |
| AND-REL-T2-014 / P2 test | Baseline unit suite | Three tests assume removed seeded demo objects; one assumes solid JSON fields in an empty fixture. | Test fixtures/contracts require reconciliation; assertions were not weakened. | FAIL |
| AND-CONTENT-P2-015 / P2 | Baseline unit suite | Physics/Chemistry catalog counts and three universal-model expectations fail. | Outside the five-workspace integration fix; remains a release-suite blocker. | FAIL |

Open product defects: three P1 rows. Open release/test blockers: two P2 rows. No P0 was observed.
