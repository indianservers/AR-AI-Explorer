# AR 3D Graph Phase 2B — Test Report

Date: 2026-08-21  
Overall status: **STABILIZED FOR AVAILABLE COVERAGE; PHASE 2B COMPLETION BLOCKED**

## Outcome

Two Phase-2-introduced lifecycle/resource defects were reproduced, fixed only inside `ar3dgraph`, and retested. No mathematical mismatch, protected-engine modification, crash, ANR, camera retention, or plane-based placement was found.

The strict completion gate cannot pass because no ARCore-capable emulator or physical device is available and the pre-existing app-wide connected suite is red. Phase 3 must not start on the basis of this run.

## Automated results

| Run | Passed | Failed | Skipped | Result |
| --- | ---: | ---: | ---: | --- |
| Cycle 1 app JVM, forced rebuild | 1,025 | 0 | 0 | Pass |
| Cycle 1 isolated AR JVM | 42 | 0 | 0 | Pass |
| Cycle 1 focused AR connected | 2 | 0 | 0 | Pass |
| Cycle 2 app JVM, forced rebuild, no code changes | 1,025 | 0 | 0 | Pass |
| Cycle 2 isolated AR JVM | 42 | 0 | 0 | Pass |
| Cycle 2 focused AR connected | 2 | 0 | 0 | Pass |
| Existing cross-workspace connected subset | 2 | 3 | 0 | Fail, pre-existing assertions |

The distinct Phase 2B final-source automated set is 1,074 tests: **1,071 passed and 3 failed**. The three failures are existing connected UI assertions, not AR failures. Separately, the retained broad connected run finalized 61 tests with 34 existing failures, and its 1,200-row Solver dataset had 1,190 passes and 10 existing step-agreement failures before ADB/UTP teardown.

## Persistent emulator workflow

The final APK installed with `install -r`. One continuing app session covered Home → Visual Workspaces → AR 3D Graph → Plot → rotation → background/resume → Reset Placement → Clear → Back. Camera permission stayed ungranted. The unsupported state was stable and accurate. Twenty AR-screen open/close cycles passed in connected automation. A five-workspace navigation stress test completed 20 cycles without a crash.

The launcher path reached the home UI in approximately 6.3 seconds including UIAutomator polling. Direct `am start` was correctly rejected because `MainActivity` is non-exported; launcher startup was used instead.

## Available-cycle conclusion

Both consecutive final-source focused cycles passed without code changes between them. They are not called “complete regression cycles” because live ARCore rendering/tracking and a green app-wide connected suite are mandatory. Cycle 1 and Cycle 2 therefore remain **blocked**, not passed, at the strict full-cycle level.

## Evidence

- Screenshots/UI XML: `outputs/ar3dgraph_phase2b/cycle1/`
- Logcat: `outputs/ar3dgraph_phase2b/cycle1/logcat.txt`
- Gradle XML: `app/build/test-results/`, `ar3dgraph/build/test-results/`, and module connected-test result directories
