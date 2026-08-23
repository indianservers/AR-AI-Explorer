# AR 3D Graph Phase 2C — Closure Report

Decision date: 2026-08-21  
Overall status: **HARDENED, BUT PHASE 2C COMPLETION BLOCKED**  
Readiness for Phase 3: **NO**

## Executive result

The complete application builds. Three isolated Phase 2 hardening defects were fixed without touching protected engines or existing UI. All executable Phase 2C tests passed in two consecutive clean, unchanged-source cycles. The strict completion gate remains blocked because the only AVD lacks ARCore and the pre-existing broad app connected suite is red.

## Counts

Per release-gate cycle:

| Layer | Passed | Failed | Skipped |
| --- | ---: | ---: | ---: |
| App JVM | 1,027 | 0 | 0 |
| Isolated AR JVM | 45 | 0 | 0 |
| Existing arengine JVM | 26 | 0 | 0 |
| AR connected UI | 3 | 0 | 0 |
| Original-3D + navigation connected UI | 2 | 0 | 0 |
| **Cycle total** | **1,103** | **0** | **0** |

Across two repeated cycles: **2,206 passed, 0 failed, 0 skipped** in the executable gate. Separately, **22 live AR scenario groups remain blocked**, and historical unrelated broad connected failures remain open.

## Required outcomes

| Outcome | Result |
| --- | --- |
| UI operations | Pass: 201 AR-screen, 60 original-3D, 200 cross-workspace navigation operations |
| Mathematical parity | Pass: 50/50 golden cases |
| Placement accuracy | Pass numerically at 1.5 m; live placement blocked |
| Orientation/axes | Pass mathematically; live visual certification blocked |
| Anchor ownership | Pass: atomic rollback and 101-placement controlled stress |
| Lifecycle | Pass on unsupported/injected paths and actual rotation/background/re-entry |
| Endurance | No available-path leak signal; real AR/GPU resource endurance blocked |
| Unsupported device | Pass; accurate state, no crash, no camera permission |
| Existing 3D Graph | Pass: 60 UI operations twice; protected code unchanged |
| Full application regression | JVM/navigation pass; broad connected baseline remains pre-existing red |
| Protected files | Pass: 13/13 SHA-256 matches |
| Plane/wall/floor/table detection | Absent; plane, instant placement and depth disabled |

## Defects fixed

1. Atomic anchor attachment rollback before prior-anchor detachment.
2. Structured recovery from unexpected engine-contract exceptions.
3. Rejection of out-of-viewport placement coordinates.

No introduced Critical/High defect remains open in executable coverage.

## Release-gate cycles

- **Cycle 1:** clean build and all executable tests passed. Full-cycle status **BLOCKED** by absent ARCore and pre-existing broad-suite failures.
- **Cycle 2:** repeated clean build on unchanged source; all executable tests passed again. Full-cycle status **BLOCKED** for the same external/baseline reasons.

They are not relabeled as two complete passes because the prompt requires real ARCore camera/tracking/rendering.

## Required before Phase 3

1. Run the full UI workflow on an ARCore-compatible emulator and supported physical device: permission, camera preview, placement, reposition, reset/place, clear/replot, orientation and session recovery.
2. Validate live rendering integrity, axes/mirroring, real anchor ownership, camera release, GPU/native memory and frame stability.
3. Test lowest-supported API, small phone and tablet profiles.
4. Establish an authorized green baseline for the existing broad connected suite.
5. Repeat two complete release-gate cycles after those blockers are removed.

Phase 3 must not begin from this closure state.
