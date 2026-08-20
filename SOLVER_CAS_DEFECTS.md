# Solver/CAS Defects

Date: 2026-08-20

| ID / severity | Test and reproduction | Expected / actual | Root cause | Fix | Verification |
|---|---|---|---|---|---|
| SCAS-P0-001 / P0 | Legacy package; prose containing `mode of`, tangent questions, `integral domain`, or unsupported contour language | Route correctly or fail closed / Phase 3 could fabricate an antiderivative | Broad substring routing | School solver first; anchored advanced prefixes; intent guard | CLOSED / PASS: legacy tests and static fail-closed corpus |
| SCAS-P1-002 / P1 | Live input semantics | Editable field / field was read-only | Semantics were on the wrapper | Make actual field editable | CLOSED / PASS: 60-case device batch |
| SCAS-P1-003 / P1 | Session controls | Undo/Redo/Clear All / absent | Session actions not implemented | 100-state history, redo, confirmation, restoration | CLOSED / PASS: device session tests |
| SCAS-GOLD-T001 / test defect | First 1,200-case execution | Operators decoded / Unicode escapes passed literally | Flat reader omitted Unicode decoding; 100 false failures | Decode JSON Unicode escapes | CLOSED / PASS: 1,200/1,200 twice |
| SCAS-GOLD-T002 / oracle defect | First execution | Genuine invalid cases / valid irrational roots labeled unsupported | Draft corpus understated production support; 25 false failures | Use out-of-range complex-root orders | CLOSED / PASS: 1,200/1,200 twice |
| SCAS-GOLD-T003 / oracle defect | Second execution | Exact limit with route-appropriate tolerance / exact-string mismatch | Numerical route differed by at most about `2e-9`; 29 false failures | Record exact and approximate oracle with `1e-8` tolerance | CLOSED / PASS: 1,200/1,200 twice |
| SCAS-P2-004 / P2 functional | Prior combined Phase 2-4 device suite | Advanced details reachable / assertions cannot reach/find several controls | Previous run passed 2/11 | Not changed by this task | OPEN / FAIL |
| SCAS-T2-005 / test defect | Legacy profile/step UI assertions | Current contract / stale profile and `Hide steps` expectations | Device tests are stale | Not changed by this task | OPEN / FAIL |

Golden-corpus unresolved failures: **0**. New production wrong-answer defects exposed by the final 1,200 cases: **0**.

`HARNESS-001`: the root filtered Gradle task selected `:arengine`, which had no matching Solver tests. The app-scoped task remains correct.
