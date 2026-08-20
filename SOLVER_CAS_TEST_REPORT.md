# Solver/CAS Test Report

Date: 2026-08-20

| Run | Scope | Status | Evidence |
|---|---|---|---|
| ENV-001 | Android SDK and device discovery | PASS | `emulator-5554`; platform-tools invoked from the installed SDK. |
| BUILD-001 | Debug app and instrumentation APKs | PASS | `:app:assembleDebug :app:assembleDebugAndroidTest`, final run successful. |
| UNIT-001 | Complete app Solver unit package | PASS | `:app:testDebugUnitTest --tests "com.indianservers.aiexplorer.solver.*"`; 118 tests passed after fixes. |
| P1-UI-001 | 60 numbered arithmetic, fraction, and linear live-UI solves | PASS | Instrumentation `OK (1 test)`, 159.573 s; every answer asserted through accessibility semantics. |
| P1-SESSION-001 | 25 Undo plus 25 Redo operations | PASS | Dedicated test passed on `emulator-5554`. |
| P1-SESSION-002 | Clear All cancel, confirm, and whole-session Undo | PASS | Dedicated test passed on `emulator-5554`. |
| P1 remaining corpus | Required quadratic/system/inequality/unit/edge live counts | NOT RUN | The executed subset cannot close the phase gate. |
| P2-P4-UI-001 | Combined legacy Phase 2-4 device suite | FAIL | 11 tests: 2 passed, 9 failed; stale expectations and result/control reachability failures. |
| GOLDEN-001 | Existing generated Solver corpus | PASS | Included in UNIT-001 and generates over 2,000 checks; it is not the requested static 1,200-case artifact. |
| GOLDEN-002 | Static 1,200-case production-engine corpus | PASS | Exact totals; 1,200/1,200 twice; 1,080 supported/verified; 120 safely rejected; per-case CSV emitted. |
| UNIT-002 | Complete Solver package with static runner | PASS | 119/119 JUnit tests: all pre-existing 118 plus the new corpus runner. |
| P5 required matrix | Physical device, OS/screen matrix, TalkBack, process death, file/share, two cycles | NOT RUN | Required environments and workflows were not completed. |

| Phase | Status | Reason |
|---|---|---|
| Phase 1 | FAIL | Passing subset, but mandatory category counts were not all executed. |
| Phase 2 | FAIL | Device regressions remain. |
| Phase 3 | FAIL | Device regressions remain despite the unit routing fix. |
| Phase 4 | FAIL | Device regressions remain and file workflows are absent. |
| Phase 5 | FAIL | Required matrix and two full cycles were not run. |

Overall: **FAIL**. No release-ready claim is made.
