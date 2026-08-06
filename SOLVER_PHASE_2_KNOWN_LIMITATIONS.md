# Solver Phase 2 Known Limitations

## Deliberate boundaries

- Input remains keyboard-only. Camera, OCR, gallery, handwriting, and image-to-math are not connected.
- No remote CAS, AI, web service, or runtime model is used.
- Interactive visualisation, tutor dialogue, and practice generation are not Phase 2 features.

## Algebra and functions

- Literal-equation rearrangement is limited to forms recognized by the existing deterministic kernel.
- Polynomial long/synthetic division, factor/remainder theorem calculations, rational equations, radical equations, and logarithmic equations are accepted only when an existing exact kernel reports a verified supported form.
- Variable-denominator equations requiring branch splits are not generalized.
- Systems involving one linear and one quadratic equation depend on the existing nonlinear-system boundary.
- General function range, inverse proof, even/odd proof, arbitrary composition syntax, and arbitrary piecewise definitions are not complete.
- Complex roots may be retained by the existing quadratic kernel; unsupported real-only operations state the boundary.

## Trigonometry and geometry

- Exact trigonometric evaluation is limited to standard degree angles.
- General trigonometric equations and interval/general-solution notation are not complete.
- Triangle solving is represented in the rule knowledge base but not exposed as one universal free-form parser.
- Coordinate Phase 2 command support covers distance, midpoint, and gradient. General conic conversion and section/line command grammars remain limited.

## Matrices

- Matrix literals use nested square brackets.
- Exact addition, subtraction, multiplication, determinant, inverse, RREF, transpose, and rank are supported within the existing local CAS bounds.
- Large matrices remain bounded by interactive performance limits.
- General symbolic matrix entries are not accepted by Solver-owned matrix arithmetic.

## Number theory and sequences

- Base conversion supports bases 2 through 36.
- Prime factorisation is trial-division based and intended for interactive-sized integers.
- Extended Euclidean coefficients and general linear-congruence families depend on existing kernel support and do not yet have dedicated Solver command syntax.
- Rational approximation is not generalized.
- Infinite geometric sums are returned only for `|r| < 1`.
- General sigma and recurrence syntax depends on the existing deterministic CAS patterns.

## Explanations and verification

- Alternative methods are available only where a deterministic strategy declares applicability.
- Common-mistake cards are contextual warnings; they do not accuse the user unless future tutor-mode work explicitly supplies a user step.
- Symbolic transformations delegated to existing CAS retain that kernel's verified support boundary.
- Some advanced deterministic-kernel results expose one aggregate verification statement rather than a separate proof object for every sub-step.
