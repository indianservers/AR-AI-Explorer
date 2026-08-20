# Solver/CAS Master Test Plan

Status vocabulary: `PASS`, `FAIL`, `BLOCKED`, `NOT RUN` only.

## Environment and gate policy

- Application: `com.indianservers.aiexplorer`, Android minSdk 31, targetSdk 36.
- Normal entry: Home > Menu > Solver. A test that bypasses Home is supplementary only.
- A phase passes only after its numbered live-UI cases pass and two consecutive regression runs pass.
- Unit/property tests supplement, but never replace, live UI evidence.
- Independent UI scenarios use Clear All. The app remains open within each operation run.
- A wrong or unverified answer is an automatic functional failure.

## Phase 1 numbered cases (110 minimum live operations)

| IDs | Count | Scenario and oracle |
|---|---:|---|
| P1-AR-001..030 | 30 | Arithmetic: precedence, signs, decimals, powers, roots, percent, factorial, constants, implicit multiplication; compare exact values. |
| P1-FR-001..015 | 15 | Fraction arithmetic/reduction and mixed inputs; compare reduced rational and approximation. |
| P1-LIN-001..015 | 15 | Linear equations including fractions, both sides, no/infinite solution; substitute candidates. |
| P1-QUAD-001..015 | 15 | Quadratics including repeated/complex roots and three methods; substitute roots. |
| P1-POLY-001..005 | 5 | Cubic/basic polynomial roots; expand factors and substitute. |
| P1-SYS-001..010 | 10 | 2x2/3x3 systems, singular/inconsistent; substitute every tuple. |
| P1-INEQ-001..010 | 10 | Linear/compound/quadratic inequalities and interval notation; boundary/sample checks. |
| P1-UX-001..010 | 10 | Invalid syntax/error span, copy/paste, edit prior result, exact/decimal, angle mode, orientation, background/resume. |
| P1-UNDO-001 | 25 ops | Twenty-five consecutive Undo actions restore exact session snapshots. |
| P1-REDO-001 | 25 ops | Twenty-five consecutive Redo actions restore exact session snapshots. |
| P1-CLEAR-001 | 3 ops | Cancel preserves; confirm clears; Undo restores whole session. |

Required boundaries are included in P1-AR/FR/LIN/QUAD/UX: division by zero, 0^0, even negative root, complex roots, undefined/invalid log, denominator variable, extreme magnitude, deep/long input, and ambiguous multiplication.

## Phase 2 numbered cases (140 minimum live operations)

| IDs | Count | Scenario and oracle |
|---|---:|---|
| P2-SIM-001..020 | 20 | Simplify, with restrictions preserved and sampled equivalence. |
| P2-EXP-001..020 | 20 | Expand products/powers/nested/binomial; factor back. |
| P2-FAC-001..020 | 20 | Factor over selected number system; expand back. |
| P2-RAT-001..015 | 15 | Rational expressions/common denominators/division; excluded values visible. |
| P2-PFR-001..010 | 10 | Partial fractions; recombine and compare coefficients. |
| P2-SUB-001..015 | 15 | Numeric/symbolic/simultaneous substitution and composition. |
| P2-PAR-001..010 | 10 | Parameterised equations, including parameter-driven solution counts. |
| P2-DOM-001..010 | 10 | Real/complex/integer/sign/nonzero/interval assumptions. |
| P2-EQV-001..010 | 10 | Equivalent pairs with domain checks. |
| P2-NEQ-001..010 | 10 | Non-equivalent pairs with a counterexample where practical. |

Each P2 case includes result check, reverse transform/safe samples, restrictions, Undo/Redo, original edit, and dependent-output refresh.

## Phase 3 numbered cases (180 minimum live operations)

| IDs | Count | Scenario and oracle |
|---|---:|---|
| P3-LIM-001..020 | 20 | One/two-sided, infinity, indeterminate, piecewise and divergent limits. |
| P3-DER-001..030 | 30 | Ordinary/higher/partial/implicit/parametric derivatives and analysis; finite differences. |
| P3-INT-001..030 | 30 | Indefinite/definite/improper/numerical/multiple/geometric integrals; differentiate or numerically compare. |
| P3-SER-001..015 | 15 | Sequences, sums, Taylor/Maclaurin and convergence/error. |
| P3-ODE-001..015 | 15 | Supported ODE/IVP/BVP and honest missing-condition fallback; substitute. |
| P3-MAT-001..030 | 30 | Matrix arithmetic, inverse/rank/RREF/system/eigen workflows; matrix identities. |
| P3-VEC-001..020 | 20 | Vector arithmetic, dot/cross/angle/projection including zero vector. |
| P3-CPLX-001..010 | 10 | Cartesian/polar/modulus/argument/powers/roots and branch conventions. |
| P3-STAT-001..010 | 10 | Descriptive statistics, combinatorics, distributions and regression. |

## Phase 4 numbered cases (110 minimum live operations)

| IDs | Count | Scenario and oracle |
|---|---:|---|
| P4-STEP-001..030 | 30 | Genuine single transformations, justification, profiles, hint/full solution, copy/bookmark/report. |
| P4-VER-001..015 | 15 | Equation, antiderivative, inverse, eigenpair and equivalence verification including prominent failure. |
| P4-XWS-001..015 | 15 | 2D/3D/Geometry handoffs and return with variables/domain/angle state intact. |
| P4-NBK-001..020 | 20 | Cards, edit/pin/rename/duplicate/reorder/search/group/notes/tags/favourites. |
| P4-FILE-001..015 | 15 | Autosave, Save As, reopen, process death, migration, transactional failure. |
| P4-SHR-001..015 | 15 | Editable import/export, text/image/PDF/share, corrupt/future/interrupted inputs. |

## Phase 5 numbered cases

| IDs | Count | Scenario and oracle |
|---|---:|---|
| P5-STU-001..020 | 20 | Complete discoverable school-student workflows listed in the specification. |
| P5-GOLD-AR-001..200 | 200 | Arithmetic golden records. |
| P5-GOLD-ALG-001..300 | 300 | Algebra golden records. |
| P5-GOLD-EQN-001..200 | 200 | Equation/inequality golden records. |
| P5-GOLD-CAL-001..200 | 200 | Calculus golden records. |
| P5-GOLD-MAT-001..100 | 100 | Matrix golden records. |
| P5-GOLD-VCX-001..100 | 100 | Vector/complex golden records. |
| P5-GOLD-INV-001..100 | 100 | Invalid/undefined fail-closed records. |
| P5-PROP-001..010 | 10 families | Required metamorphic identities with retained seeds. |
| P5-ADV-001..020 | 20 | Nested/long/large/pathological/rapid/cancel/lifecycle/low-memory cases. |
| P5-DEV-001..009 | 9 profiles | Small/standard/large/low-end/mid-range/tablet/landscape/foldable/all supported Android. |
| P5-A11Y-001..011 | 11 | TalkBack, spoken math, focus, font/scale/touch/keyboard/errors/steps/plain text/colour. |

## Initial status

All cases are `NOT RUN` until recorded in `SOLVER_CAS_TEST_REPORT.md`. Connected-emulator discovery is `PASS`; physical-device coverage is `NOT RUN`.
