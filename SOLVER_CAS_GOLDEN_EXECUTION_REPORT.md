# Solver/CAS Golden Dataset Execution Report

Date: 2026-08-20
Production entry point: `Phase3SolverEngine.solve`
Dataset SHA-256: `714FA55A4F58833CBF97C119B48A9C2EF466E1526D21F30245A2F97740D519E9`

## Final result

**PASS — 1,200/1,200 static cases passed, with zero wrong answers, zero unexpected exceptions, and zero unclosed golden-corpus failures.**

| Category | Executed | Passed | Failed |
|---|---:|---:|---:|
| Arithmetic | 200 | 200 | 0 |
| Algebra / symbolic | 300 | 300 | 0 |
| Equations / inequalities | 200 | 200 | 0 |
| Calculus | 200 | 200 | 0 |
| Matrices | 100 | 100 | 0 |
| Vectors / complex | 100 | 100 | 0 |
| Invalid / undefined / ambiguous / unsupported | 100 | 100 | 0 |
| **Total** | **1,200** | **1,200** | **0** |

There are 1,080 supported-result contracts and 120 safe-rejection contracts. All supported cases returned a presentable matching answer, `Verified` production verification, and at least one traceable explained step. All rejection contracts returned no final answer and were not presentable as correct.

## Runs

| Run | Scope | Result | Evidence |
|---|---|---|---|
| Development 1 | Static runner | 1,075 pass / 125 fail | Exposed 100 JSON Unicode-decoding harness failures and 25 incorrect unsupported-root expectations; no production wrong answer. |
| Development 2 | Static runner after oracle corrections | 1,171 pass / 29 fail | Exposed exact-string comparison being applied to verified floating limit output. |
| Final cycle 1 | Static runner | **1,200 pass / 0 fail** | Gradle `BUILD SUCCESSFUL`; complete CSV emitted. |
| Final cycle 2 | Complete Solver package | **1,200 golden pass / 0 fail; 118 legacy pass / 0 fail** | 119 JUnit tests, zero failures/skips; golden 1.195 s; Solver package 6.667 s. |

## Closed findings

- `SCAS-GOLD-T001`: JSON `\u003c` and `\u003e` were passed literally. Closed by decoding JSON Unicode escapes.
- `SCAS-GOLD-T002`: the draft oracle mislabeled irrational numeric roots unsupported; production correctly returned verified approximations. Closed by replacing the family with genuine out-of-range complex-root orders.
- `SCAS-GOLD-T003`: removable limits used exact-string comparison although production intentionally returns verified floating approximations. Closed by retaining the exact oracle and applying the recorded `1e-8` tolerance.

No new production Solver defect was exposed by the final corpus. Existing non-golden UI/release blockers remain tracked separately.

## Artifacts

- Static corpus: `SOLVER_CAS_GOLDEN_DATASET.jsonl`
- Methodology: `SOLVER_CAS_GOLDEN_DATASET_README.md`
- Runner: `app/src/test/java/com/indianservers/aiexplorer/solver/SolverCasStaticGoldenDatasetTest.kt`
- Machine result: `app/build/reports/solver-cas-golden/SOLVER_CAS_GOLDEN_RESULTS.csv`
- Maintenance generator: `quality/solver-cas/generate-static-golden-dataset.ps1`
