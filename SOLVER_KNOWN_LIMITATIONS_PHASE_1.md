# Solver Known Limitations - Phase 1

## Deliberate Phase 1 boundaries

- Input is keyboard-only. Camera, gallery, OCR, handwriting, and image-to-math are intentionally disconnected.
- Solver has no internet, cloud CAS, remote inference, or runtime model download.
- Trigonometric, logarithmic, calculus, matrix, complex-number, statistics, probability, geometry, and advanced number-theory solving are outside Phase 1.
- Nonlinear equations, quadratic equations, polynomial root finding, and branch-producing equations such as `x^2 = 4` are rejected rather than partially solved.
- Systems are limited to exactly two linear equations in exactly two variables.
- Inequalities are limited to one linear variable; compound, rational, and nonlinear inequalities are not solved.

## Exact arithmetic boundaries

- Square roots are solved only when the result is an exact rational value. `sqrt(144)` is supported; `sqrt(2)` is reported as unsupported.
- General `root(expression, degree)` is represented by the AST, but exact solving is currently limited to degree two.
- Symbolic constants such as `pi` and `e` are preserved but are not converted to approximate numbers by the exact Phase 1 evaluator.
- Integer exponents are supported by the exact evaluator. Fractional and general symbolic exponents are not.
- Division by zero is rejected.

## Algebra boundaries

- Simplification, expansion, polynomial arithmetic, and simple factorisation are limited to forms safely supported by the bundled exact CAS.
- Phase 1 returns one deterministic method. Multiple solution methods and explanation-level controls belong to Phase 2.
- Colon proportions require a ratio on both sides of the equals sign and non-zero denominators.
- Variable denominators and domain exclusions are not solved in Phase 1.
- Identities and contradictions whose variable cancels return `All real values` or `No solution`; their verification status is not promoted beyond the available exact check.

## Verification boundaries

- Arithmetic and linear relations use exact checks.
- Symbolic transformations use several safe exact sample points when direct structural equivalence is unavailable.
- Sampling is a validation safeguard, not a formal proof. A result with no safe sample points is marked inconclusive.
- Phase 1 does not use probabilistic, AI-generated, or remote verification.

## UI and persistence

- History is local to Solver and capped at 100 records.
- History stores text metadata only and does not store images.
- The result display uses readable plain mathematical text; a dedicated typeset formula renderer is not introduced in Phase 1.
- Existing keyboard undo, redo, caret movement, selection, copy, cut, and paste are reused as implemented by the shared keyboard.

## Future work

These limitations should be reviewed before Phase 2. Phase 2 must extend this module rather than replacing the AST, parser contracts, step model, verification model, or history boundary.
