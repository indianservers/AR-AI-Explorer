# Calculus and Advanced Mathematics Phase 4 Final Report

## Outcome

Phase 4 completes the four-phase roadmap with multivariable and vector-calculus computation that exposes orientation, assumptions and independent verification evidence.

## Implemented

| Capability | Verification |
|---|---|
| Jacobian | Scale-aware centred partial derivatives for every output/variable pair |
| Hessian | All second partials plus mixed-partial symmetry residual |
| Lagrange optimization | Multi-seed Newton solve, constraint residual and stationarity residual |
| Scalar line integral | Parametric arc-length factor and fine/coarse integration evidence |
| Vector work integral | Oriented `F(r(t)) dot r'(t)` integral; reverse orientation negates work |
| Parametric surface flux | `r_u cross r_v` orientation and vector-field substitution |
| Green certificate | Closed-boundary circulation versus double integral of planar curl |
| Gauss certificate | Six-face outward flux versus triple integral of divergence |
| Stokes certificate | Counter-clockwise circulation versus oriented surface curl integral |
| Calculator routing | Jacobian and Hessian commands with matrix and symmetry evidence |

## Example commands

```text
jacobian x^2+y;x+y^2 vars x,y at x=1,y=2
hessian x^2+3*x*y+2*y^2 vars x,y at x=1,y=2
```

## Verification

Focused Phase 4 tests cover polynomial Jacobians/Hessians, constrained extrema on the unit circle, unit-circle scalar/work integrals, planar flux, Green's theorem, the divergence theorem, Stokes' theorem and calculator routing. The complete focused Phase 1–4 regression set passes.

Result: **BUILD SUCCESSFUL**.

## Four-phase status

1. Rigorous limits and derivative applications — complete.
2. Advanced, improper, parameterized and multiple integration — complete.
3. ODEs, coupled systems, series and transforms — complete.
4. Multivariable/vector calculus and theorem verification — complete.
