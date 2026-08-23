# AR 3D Graph Phase 2B — Release Gate

Decision date: 2026-08-21  
Decision: **BLOCKED — NOT READY TO START PHASE 3**

## Gate summary

| Criterion | Result |
| --- | --- |
| Application builds | Pass |
| Final automated JVM tests | Pass: 1,067/1,067 per cycle |
| Focused AR connected tests | Pass: 2/2 per cycle |
| Unsupported non-AR flow | Pass |
| Mathematical parity | Pass: 16 explicit + implicit + parametric + multi/style/domain cases |
| Tap-anywhere/fixed 1.5 m math | Pass |
| No plane/wall detection | Pass |
| Anchor replacement/reset/clear | Pass with controlled doubles; live AR blocked |
| Lifecycle/job/resource cleanup | Pass for reachable/faked paths |
| Existing protected engines/UI | Pass, unchanged |
| Introduced Critical/High defects | Pass: P2B-002 fixed; no open introduced Critical/High defect |
| App-wide connected regression | Fail: pre-existing suite remains red |
| ARCore camera/tracking/render validation | Blocked: no capable target |
| Two consecutive complete regression cycles | Blocked; two focused cycles pass, but complete cycles cannot execute/pass |

## Totals

The distinct final-source automated execution contains **1,074 tests: 1,071 passed, 3 failed, 0 skipped**. The three failures are pre-existing connected UI assertions. In addition, **22 live AR scenario groups are blocked** by the emulator configuration. Cycle-by-cycle repeated counts and the retained broad-suite baseline are in the test report.

## Fixed defects

Two Phase-2-introduced defects were fixed: generation continuing after screen exit, and delayed renderer/anchor cleanup when disposal occurs after GL pause. Both passed specific, related-group, connected and repeated-cycle retests. No protected engine was edited.

## Required before unblocking

1. Run the complete AR matrix on an ARCore-compatible emulator and at least one supported physical device: camera permission paths, tracking, real anchors, placement/orientation, camera/GL rendering, session loss/recovery and performance.
2. Run lowest-supported API plus small-phone and tablet viewport coverage.
3. Establish or repair an authorized green baseline for the existing app-wide connected suite outside this AR-only stabilization scope.
4. Execute two consecutive complete regression cycles with no code changes and zero unexplained failures.

Until those conditions are met, Phase 2B must not be declared complete and Phase 3 should not begin.
