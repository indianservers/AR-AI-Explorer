# Solver/CAS Mathematical Accuracy

Date: 2026-08-20

Policy: a wrong answer is more severe than no answer; unsupported symbolic work must fail closed.

| Corpus | Requested | Executed | Passed | Failed | Status |
|---|---:|---:|---:|---:|---|
| Arithmetic | 200 | 200 | 200 | 0 | PASS |
| Algebra / symbolic | 300 | 300 | 300 | 0 | PASS |
| Equations / inequalities | 200 | 200 | 200 | 0 | PASS |
| Calculus | 200 | 200 | 200 | 0 | PASS |
| Matrices | 100 | 100 | 100 | 0 | PASS |
| Vectors / complex | 100 | 100 | 100 | 0 | PASS |
| Invalid / undefined / ambiguous / unsupported | 100 | 100 | 100 | 0 | PASS |
| **Total** | **1,200** | **1,200** | **1,200** | **0** | **PASS** |

The corpus is static, contains all required metadata, and executes directly through `Phase3SolverEngine.solve`. Expected results are independently derived from rational arithmetic, algebraic laws, substitution/elimination, calculus rules, matrix definitions, complex arithmetic, and explicit fail-closed contracts.

Supported cases: 1,080/1,080 returned matching results with `Verified` production status, non-empty steps, rule IDs, and explanations. Fail-closed cases: 120/120 returned no final answer and were not presentable as correct. Two consecutive final executions passed.

The existing 118 Solver tests also remain green; the package reports 119/119 including the new corpus runner. Golden mathematical-accuracy gate: **PASS**.
