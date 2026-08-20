# Solver/CAS Regression Matrix

Date: 2026-08-20

| Area | Golden cycle 1 | Golden cycle 2 / Solver package | Prior live UI | Status |
|---|---|---|---|---|
| Parser/input metadata | PASS | PASS | PASS for prior 60 inputs | PASS for golden gate |
| Exact/numeric arithmetic | 200/200 | 200/200 | PASS for prior subset | PASS |
| Symbolic algebra | 300/300 | 300/300 | Not separately run | PASS |
| Equations/inequalities/systems | 200/200 | 200/200 | Linear subset passed; full live matrix not run | PASS for golden gate |
| Calculus | 200/200 | 200/200 | Advanced device reachability failures remain | PASS for golden gate; UI blocker remains |
| Matrices | 100/100 | 100/100 | Not separately run | PASS |
| Vectors/complex | 100/100 | 100/100 | Not separately run | PASS, including 20 vector fail-closed contracts |
| Invalid/undefined/unsupported | 100/100 | 100/100 | P0 routing regression passed | PASS |
| Steps and verification | 1,080/1,080 | 1,080/1,080 | Legacy UI assertions remain stale | PASS in engine; UI blocker remains |
| Legacy Solver package | N/A | 118/118 legacy plus runner | N/A | PASS |
| Session history | N/A | Legacy unit coverage PASS | Undo/Redo/Clear All PASS | PASS |
| Files/handoffs | N/A | Not run | Not run | FAIL / NOT RUN |
| Accessibility/device matrix | N/A | Not run | Partial only | FAIL / INCOMPLETE |

Two consecutive complete static golden executions: **PASS**. Complete release regression across device UI, files, accessibility, physical devices, and two full product cycles: **FAIL / incomplete**.
