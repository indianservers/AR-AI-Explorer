# Solver Supported Problems - Phase 2

Phase 2 includes every supported Phase 1 problem plus the deterministic forms below.

## Algebra and equations

- Multi-step linear equations and two-variable linear systems inherited from Phase 1
- Quadratic equations recognized by the existing deterministic teaching kernel
- Factoring, completing-square, and quadratic-formula method choices when applicable
- Polynomial simplification, expansion, and rational factorisation supported by the exact CAS
- Partial fractions for rational functions whose denominator splits into supported distinct rational linear factors
- Compound linear inequalities supported by the exact CAS command path
- Domain analysis for rational, radical, logarithmic, and inverse-function expressions

## Functions

- Exact function evaluation, for example `f(x)=x^2+1; f(3)`
- Domain and branch analysis using `domain expression`
- Existing exact CAS substitution and simplification

General inverse/range proofs and arbitrary piecewise parsing remain bounded as documented in the limitations report.

## Trigonometry

- Exact `sin`, `cos`, and `tan` values at standard degree angles
- Degree/radian conversion
- Pythagorean, sine-rule, and cosine-rule knowledge entries with conditions and profile-specific explanations

Examples:

- `sin(30deg)`
- `cos(180deg)`
- `tan(45deg)`
- `180deg to rad`

## Sequences and series

- Arithmetic nth term and finite sum
- Geometric nth term and finite sum
- Infinite geometric sum after checking `|r| < 1`
- Existing deterministic recurrence and common series support through the local teaching/CAS kernels

Command forms:

- `arithmeticTerm(a,d,n)`
- `arithmeticSum(a,d,n)`
- `geometricTerm(a,r,n)`
- `geometricSum(a,r,n)`
- `infiniteGeometricSum(a,r)`

## Coordinate geometry

- Distance
- Midpoint
- Gradient, including explicit vertical-line handling

Command forms use four coordinates, such as `distance(0,0,3,4)`.

## Matrices

- Exact matrix addition and subtraction
- Exact matrix multiplication with dimension checking
- Determinant
- Inverse with singularity refusal
- RREF/row reduction
- Transpose
- Rank

Matrices use nested row notation such as `[[1,2],[3,4]]`.

## Number theory

- GCD/HCF and LCM
- Euclidean and prime-factor method choices for GCD
- Prime factorisation
- Canonical modular residue
- Base conversion for bases 2 through 36
- Existing deterministic number-theory support reachable through the local teaching kernel

Examples:

- `gcd(84,30)`
- `lcm(12,18)`
- `primeFactors(84)`
- `mod(-3,7)`
- `base(FF,16,10)`

## Explanation and result controls

Every supported result may carry:

- Recommended and alternative methods
- One of four explanation profiles
- Assumptions and excluded values
- Exact and optional approximate answers
- Common possible mistakes with counterexamples
- Rule citations attached to exact steps
- Structured verification strength
