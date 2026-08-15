# Calculus and Advanced Mathematics — Four-Phase Plan and Phase 1 Report

## Audit summary

AI Explorer already provides symbolic higher derivatives, common elementary antiderivatives, verified definite numerical integration, partial fractions foundations, Taylor series, first-order ODE foundations, Laplace/Fourier/Z-transform foundations, gradients, directional derivatives, divergence, curl, tangent planes, and graph-linked calculus analysis.

The remaining competitive gaps are depth and evidence: broader limit methods, improper/multiple integration, wider ODE families, vector-calculus theorem workflows, and consistently verified method selection.

## Four phases

| Phase | Scope | Status |
|---|---|---|
| 1. Rigorous foundations | One-sided/two-sided limits, infinity/DNE, continuity and discontinuity classification, method evidence, derivative applications | Implemented |
| 2. Advanced integration | Improper integrals, convergence, parameterized integrals, double/triple integrals, coordinate changes and certified numerics | Pending |
| 3. Differential equations and series | Separable/linear/exact/Bernoulli/higher-order ODEs, systems, qualitative fields, series solutions, Laplace workflows | Pending |
| 4. Multivariable and vector calculus | Jacobians/Hessians, constrained optimization, line/surface integrals, Green/Gauss/Stokes workflows, visual proofs and release verification | Pending |

## Phase 1 implementation

`RigorousCalculusEngine` adds:

- Independent left- and right-hand sample evidence.
- Finite, positive-infinity, negative-infinity, DNE and unresolved classifications.
- Continuous, removable, jump, infinite and unresolved/oscillatory continuity classes.
- Direct-substitution, factor-cancellation, squeeze-pattern, L'Hopital-candidate and numerical-certification method labels.
- Stable-digit evidence and explicit refusal of unstable finite answers.
- Stationary-point, absolute-extrema, increasing and decreasing interval reports.
- Live routing from `AdvancedScientificCalculator` for `limit ... as x -> a` commands.

## Verification

Focused Phase 1 and multivariable regression tests pass with `BUILD SUCCESSFUL`. Cases include `(x²-4)/(x-2)`, `1/x`, `abs(x)/x`, `sin(x)/x`, and derivative applications for `x²` on a closed interval.
