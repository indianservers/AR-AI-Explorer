# Solver Developer Guide

## Pipeline

```text
SolverInputSource
  -> normalization and parser
  -> typed AST and classifier
  -> Phase3SolverEngine strategies
  -> structured steps and explanations
  -> symbolic and visual verification
  -> Phase 4 hints, tutor, practice, and aggregates
```

## Extension Rules

- Add mathematical behavior to the shared engine, not to a UI calculator.
- Use typed AST structures; do not solve by raw string replacement.
- Register rules and explanations before exposing a new transformation.
- Provide assumptions, domains, branches, and verification.
- Add a calculator preset as active only after its starter expression passes the
  central-engine catalogue test.
- Validate every generated practice candidate before returning it.
- Keep tutor evaluation deterministic and fail closed on uncertainty.
- Store only aggregate learning metrics unless a future privacy design explicitly
  approves more detail.

## Main Phase 4 Packages

- `domain/tutor`
- `domain/practice`
- `domain/analytics`
- `domain/catalogue`
- `domain/input`
- `data/local`
- `presentation/components/SolverLearningPanels.kt`

## Testing

Run:

```text
gradlew :app:testDebugUnitTest --tests "com.indianservers.aiexplorer.solver.*"
gradlew :app:compileDebugAndroidTestKotlin
```

Keep the offline boundary, catalogue routing, golden corpus, and misconception
mutation tests mandatory for every Solver release.

