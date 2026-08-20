# Solver/CAS Static Golden Dataset

Date: 2026-08-20

`SOLVER_CAS_GOLDEN_DATASET.jsonl` is the authoritative, committed Solver/CAS golden corpus. It contains exactly 1,200 individually numbered, human-readable JSON objects, one per line. Tests read this file directly; they do not generate cases at runtime.

## Category contract

| Category | Cases | Subcategory distribution |
|---|---:|---|
| Arithmetic | 200 | precedence 80; rational addition 40; percentage 30; perfect-square root 25; absolute value 25 |
| Algebra / symbolic | 300 | combine like terms 200; distributive expansion 100 |
| Equations / inequalities | 200 | linear equations 100; positive-coefficient inequalities 25; negative-coefficient inequalities 25; two-variable systems 50 |
| Calculus | 200 | polynomial derivatives 100; polynomial antiderivatives 50; removable limits 50 |
| Matrices | 100 | addition 40; subtraction 20; multiplication 20; transpose 10; determinant 10 |
| Vectors / complex | 100 | rectangular complex values 60; complex multiplication 20; unsupported vector-dot requests 20 |
| Invalid / undefined / ambiguous / unsupported | 100 | division by zero 25; invalid complex-root order 25; malformed syntax 25; unsupported contour prose 25 |
| **Total** | **1,200** | **1,080 supported-result contracts; 120 fail-closed contracts** |

## Record schema

Every record contains these non-empty fields:

- Identity and grouping: `case_id`, `category`, `subcategory`, `difficulty`, `regression_id`, `tags`.
- Input contract: `input`, `normalized_input`, `units`, `angle_mode`.
- Answer oracle: `expected_exact_result`, `expected_approximate_result`, `tolerance`, `expected_solution_set`, `comparison`.
- Mathematical context: `assumptions`, `domain_constraints`, `independent_derivation`.
- Behaviour contract: `expected_supported`, `expected_warnings`, `expected_error_classification`, `expected_step_summary`, `expected_verification`.

`NO_RESULT` means production must fail closed: the solution cannot be presentable as correct and must not contain a final answer.

## Independent expectation policy

Expected results were derived without copying production output. The derivations use rational arithmetic, algebraic laws, substitution/elimination, the calculus power rule, factor cancellation on a punctured domain, matrix definitions, and rectangular complex arithmetic. Invalid cases use domain, grammar, and supported-command bounds.

The runner uses four declared comparison contracts: 700 compact exact comparisons, 400 numerical symbolic-equivalence checks at seven independent sample points, 50 reverse-power antiderivative checks, and 50 scalar tolerance checks (`1e-8`) for the numerical limit route. Production verification must additionally report `Verified` for every supported case.

## Execution

Run only this corpus:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.indianservers.aiexplorer.solver.SolverCasStaticGoldenDatasetTest"
```

Run it with the complete Solver package:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.indianservers.aiexplorer.solver.*"
```

The runner validates schema, uniqueness, category totals, support/fail-closed behaviour, final answers, verification, and traceable steps. Per-case machine evidence is written to `app/build/reports/solver-cas-golden/SOLVER_CAS_GOLDEN_RESULTS.csv`.

`quality/solver-cas/generate-static-golden-dataset.ps1` is a maintenance utility that deterministically rebuilds the committed file. It is not called by the test. Regeneration requires review, a new hash, and two passing executions.

Current SHA-256: `714FA55A4F58833CBF97C119B48A9C2EF466E1526D21F30245A2F97740D519E9`.
