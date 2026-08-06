# Solver Final Known Limitations

## Mathematical Scope

- The engine is a validated offline subset, not a complete general-purpose CAS.
- Partial-fraction decomposition is deferred.
- General inverse-function, intercept, triangle, trig-identity, and trig-equation
  calculators are deferred.
- General tangent/normal, differential-equation, eigenvalue, and sigma workflows
  are deferred.
- Tutor equivalence is strongest for supported symbolic expressions and linear
  equations; unfamiliar transformations fail closed.
- Practice templates cover supported skills and intentionally fall back to simpler
  validated forms for unsupported source classifications.

## Learning Model

- Mastery is a local heuristic estimate, not a psychometrically calibrated score.
- Analytics are aggregate; session replay and raw learner-line history are absent.
- Difficulty is deterministic and structural but cannot capture every classroom
  or learner-specific source of difficulty.

## Platform Validation

- JVM tests and Android test compilation pass.
- Physical-device TalkBack, maximum font scale, frame timing, low-memory behavior,
  process death, and a broad Android-version matrix remain release-QA activities.

## Integration

External recognized text has an adapter boundary only. No image, recognition, or
Math Camera workflow is connected to Solver.

