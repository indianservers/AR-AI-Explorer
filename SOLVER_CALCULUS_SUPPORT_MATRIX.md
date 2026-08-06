# Solver Phase 3 Calculus and Complex Support Matrix

## Calculus

| Capability | Status | Notes |
|---|---|---|
| Polynomial derivatives | Supported | Exact symbolic rules |
| Rational derivatives | Supported where parser accepts expression | Quotient/domain conditions retained |
| Exponential/log derivatives | Supported elementary forms | Real-domain restrictions apply |
| Trigonometric derivatives | Supported elementary forms | Radian convention |
| Inverse-trigonometric derivatives | Supported by existing symbolic kernel where matched | Principal real branches |
| Product rule | Supported | Rule and formula explanation |
| Quotient rule | Supported | Denominator must be non-zero |
| Chain rule | Supported elementary compositions | Rule trace retained |
| Higher derivatives | Supported with explicit order up to kernel limit | Repeated symbolic differentiation |
| Removable limits | Supported canonical factorable forms | Punctured-neighbourhood reasoning |
| Selected canonical limits | Supported by existing local kernel | Unsupported forms fail closed |
| Continuity checks | Partial | Explicit general continuity command is not yet broad |
| Tangent/normal applications | Partial | Tangent visual is available for derivative results |
| Increasing/decreasing intervals | Not generalized | No unsupported claim is shown |
| Critical points/extrema | Not generalized | Planned after robust equation-set analysis |
| Basic antiderivatives | Supported elementary forms | Includes arbitrary constant |
| Substitution | Supported canonical reverse-chain forms | Local symbolic kernel decides applicability |
| Integration by parts | Supported canonical forms | Method rule retained |
| Partial fractions integration | Partial | Existing supported rational decomposition only |
| Definite integration | Supported finite elementary intervals | Adaptive Simpson verification |
| Area visualisation | Supported for finite sampled intervals | 24 bounded midpoint rectangles |
| Area between curves | Not generalized in Solver | Existing graph capability is not duplicated |
| Separable ODE | Partial existing kernel coverage | Only verified canonical commands are accepted |
| Initial-value problems | Partial existing kernel coverage | Unsupported forms fail closed |

## Complex numbers

| Capability | Status | Notes |
|---|---|---|
| Rectangular form | Supported |
| Polar data | Supported through magnitude and principal argument |
| Modulus and argument | Supported |
| Multiplication | Supported |
| Conjugate | Reported for parsed values |
| Powers | Partial | Root/polar kernel is primary Phase 3 path |
| Roots | Supported for orders 1 through 24 | Every k branch retained |
| Euler interpretation | Represented through polar convention | No symbolic transcendental simplifier claim |
| Argand visual | Supported |
| Complex quadratic roots | Supported where existing quadratic kernel emits them |

## Conventions

- Real calculus uses each expression's valid real domain.
- Trigonometric calculus uses radians.
- Indefinite integrals include an arbitrary constant.
- Principal complex argument is in `(-pi, pi]`.
- nth roots retain branches `k=0,...,n-1`.
- Numerical evidence never replaces symbolic or reverse-operation verification.

