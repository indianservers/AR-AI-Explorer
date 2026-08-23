# AR 3D Graph Phase 2C — Defect Register

| ID | Class | Severity | Reproduction | Cause | Fix | Retest |
| --- | --- | ---: | --- | --- | --- | --- |
| P2C-001 | Phase 2 anchor integration | High | Inject failure after new-anchor creation but before graph-root attachment | Ownership committed/detached the prior anchor without an explicit attachment transaction | Added attachment callback before ownership commit; failed attachment detaches replacement and preserves prior anchor | 101-placement stress, creation/attachment failure, reset/clear/close, both cycles pass |
| P2C-002 | Phase 2 adapter/recovery | Medium | Contract throws unexpectedly during Plot | Exception escaped before adapter result classification, allowing loading to remain unresolved | Convert contract exceptions to structured `GenerationError`; retry remains available | Specific failure/retry test, full 45 AR JVM tests, UI cycles pass |
| P2C-003 | Phase 2 tap hardening | Low | Call ray conversion with finite coordinates outside the viewport | Math accepted extrapolated NDC coordinates | Reject taps outside local camera viewport bounds | Boundary/failure matrix and full placement suite pass |
| PRE-001 | Pre-existing app connected suite | High release blocker | Run broad existing connected tests | Stale semantics/title/curriculum and Solver expectations; prior ADB/UTP teardown instability | Not modified under AR-only scope | Retained baseline: 34/61 failures; focused stale subset 3/5 failures in Phase 2B |
| ENV-001 | Emulator limitation | Critical coverage gap | Query/start ARCore on available AVD | `com.google.ar.core` absent | No fake support applied | Unsupported path passes; real AR workflow blocked |

The early test-only `assertExists` compile incompatibility and an unreliable coordinate-only navigation attempt were corrected in the harness and are not product defects. The coordinate attempt was not counted; semantic verification replaced it.

No open Critical or High defect introduced by the AR module remains in executable coverage.
