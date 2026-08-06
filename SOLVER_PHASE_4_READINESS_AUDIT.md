# Solver Phase 4 Readiness Audit

## Baseline

Phases 1 through 3 provide a keyboard-only offline Solver with typed expressions,
deterministic classification, structured steps, rule citations, multiple
methods, four explanation profiles, restrictions, verification, local history,
declarative visualisations, supported calculus and complex-number paths.

The focused Solver suite passes. The broad repository baseline retains nine
failures outside the Solver package; these must remain clearly separated from
Phase 4 regressions.

## Critical correctness work before tutoring

1. Learner steps cannot be compared as raw strings. Equivalent formatting,
   expansion and alternative routes require parsed or independently evaluated
   equivalence.
2. Division by a variable must flag the zero branch.
3. Squaring both sides must record possible extraneous roots.
4. Logarithm, radical, denominator and matrix-domain violations must fail
   closed.
5. Recognised but incomplete tutor input must never fall through to an
   unrelated algebra interpretation.
6. Practice candidates must be solved and verified before display.

Phase 4 addresses these items in Solver-owned domain services.

## Incomplete Phase 1-3 features

- No progressive hint mode.
- No learner-entered next-step workflow.
- No validated practice generator.
- No deterministic difficulty estimate.
- No private Solver mastery history.
- No shared-engine calculator catalogue in the Solver UI.
- No future external-input adapter contract.
- Visual playback state survives ordinary configuration through the ViewModel,
  but full process-death restoration of an open solution is limited.

## Explanation weaknesses

- Phase 1-2 explanations are strongest for registered algebraic rules.
- Some advanced core-kernel steps use a precise kernel explanation as fallback
  because they do not yet have four separately authored rule profiles.
- Visual formula explanations cover major recognised formulas, not every
  formula in the application.

## Unsupported or partial problem classes

- General theorem proving, olympiad proof search and unrestricted symbolic CAS.
- Broad piecewise limits and continuity.
- General optimisation and extrema classification.
- General differential equations.
- General symbolic complex functions and branch-cut analysis.
- Eigenvalues are not advertised by the calculator catalogue because the
  verified shared engine does not currently provide a complete path.

## Verification gaps

- Numerical sampling is evidence, not proof, for unrestricted symbolic
  identities.
- Some advanced core-kernel results provide reverse-operation or finite
  difference verification rather than a second symbolic derivation.
- Visual verification supplements symbolic verification and must never replace
  it.

## Visualisation gaps

- Solver visuals are bounded 2D explanations and intentionally avoid loading
  the 3D engine.
- Some declared visual types have formula-catalogue use but no general-purpose
  generator for every problem family.
- Physical-device screenshot goldens and frame timing are not configured.

## Performance risks

- Re-solving on every explanation-profile or method change is deterministic but
  can repeat work.
- A 48-entry visual-spec cache limits repeated sampling.
- Practice generation must cap attempts and template sizes.
- Tutor equivalence sampling must use a small deterministic safe-value set.

## Accessibility gaps

- Existing visuals include text alternatives and reduced-motion behavior.
- Tutor feedback and mastery controls need explicit semantics.
- Canvas labels remain primarily in adjacent text rather than painted into the
  graphic.

## Architectural duplication

- The app already has broader calculator and learner-analysis components.
  Phase 4 must reuse public deterministic kernels but keep Solver learning data
  isolated.
- Practice calculators must be catalogue presets over one central Solver
  engine, not separate calculators.

## Possible regressions

- Adding state to `SolverViewModel` can affect rotation and history behavior.
- Tutor keyboard fields must reuse, not alter, the existing keyboard.
- Analytics persistence must use a separate Solver-owned preference file.
- The final UI must use progressive disclosure to avoid excessive vertical
  expansion.

## Release priorities

1. Mathematical step evaluation and fail-closed domain checks.
2. Validated hint and practice generation.
3. Private local analytics with transparent clear control.
4. Compact progressive-disclosure UI.
5. Golden/property corpus and offline boundary tests.

