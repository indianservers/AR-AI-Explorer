# AR 3D Graph Phase 3B — Defect Register

Date: 2026-08-21

| ID | Severity | Defect and root cause | Isolated fix | Retest |
|---|---:|---|---|---|
| P3B-001 | High | Activity/process recreation could retain `Placed` UI state although native anchor was gone | invalidate native placement; regenerate prior valid graph; restore serializable transform | pass |
| P3B-002 | High | Retry closed the existing session manager and attempted to reuse it | screen-scoped retry epoch creates a new manager and returns state to Idle | pass |
| P3B-003 | High | failed session configuration could leak the local candidate Session | close candidate transactionally on all failures | pass |
| P3B-004 | Medium | permission-revoked resume could call session resume using stale UI permission state | direct permission check gates resume | pass |
| P3B-005 | Medium | AndroidView removal had no explicit renderer release callback | idempotent `onRelease` disposal | pass |
| P3B-006 | Medium | rapid tracking alternation could flash status | 400 ms stable-state filter; first state immediate | pass |
| P3B-007 | Medium | fixed control stack could clip in constrained height/large text and lacked Help | bounded scrollable controls, 48 dp targets, accessible Help | pass |
| P3B-008 | Medium | failed/new buffer preparation could mix old/new renderer state; late calls after close were accepted | transactional preparation and closed guards | pass |
| P3B-009 | Medium | repeated camera/matrix/location allocation/lookups occurred in frame paths | reusable buffers/matrices and cached GL locations | pass |
| P3B-010 | Medium | very large adapter output had no AR allocation ceiling | pre-allocation AR-only safety validation, no resampling | pass |
| TEST-3B-001 | Test only | Activity orientation recreation removed bare test-only Compose content | deterministic responsive configuration profiles | failing test passed; both cycles restarted |

Open environment blocker: the sole AVD has no Google Play Services for AR. Live camera, tracking, placement, rotation, pinch, reposition, camera contention and native/GPU cleanup cannot be certified. No introduced Critical or High product defect remains open in executable coverage.
