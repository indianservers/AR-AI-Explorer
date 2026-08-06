# Solver Mathematical Accuracy Report

## Accuracy Policy

The Solver uses typed expressions and exact arithmetic where supported. It tracks
assumptions, domains, excluded values, solution branches, verification results,
and structured rules. A failed or unsupported verification prevents a confident
answer.

## Phase 4 Safeguards

- Hints are derived from verified solution steps, not free-form generation.
- Tutor input is parsed before evaluation.
- Equivalent expressions and equivalent linear equations are accepted.
- One-sided equation operations receive balance-specific feedback.
- Division by a variable is conditionally equivalent until its zero case is handled.
- Known invalid expansion and cancellation patterns receive rule-specific feedback.
- Ambiguous or unsupported transformations fail closed.
- Every displayed practice problem is solved and verified before presentation.
- Every active calculator preset passes through the central Solver engine.

## Test Evidence

The passing corpus includes arithmetic, fractions, equations, inequalities,
number theory, coordinate geometry, trigonometry, sequences, matrices, calculus,
complex numbers, tutor mutations, hints, practice generation, and invalid input.

The Solver does not claim general-purpose symbolic completeness. Unsupported
classes and deferred calculator presets are documented and hidden.

