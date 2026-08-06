# Solver Final Performance Report

## Design Controls

- Hint generation reads an existing structured solution and performs no new solve.
- Tutor checks are deterministic and local.
- Practice generation validates one bounded candidate through the shared engine.
- Difficulty estimation is linear over the expression text and structured result.
- Learning analytics store compact aggregates, not raw sessions.
- Calculator presets reuse the shared engine and do not instantiate separate CASes.
- Learning panels are composed only when expanded.

## Automated Gates

The existing performance regression suite passes with these JVM limits:

- median solve-and-visual-spec generation below 500 ms;
- p95 below 2,000 ms;
- visual-spec caching behavior covered.

## Device Caveat

JVM timing is a regression signal, not an Android frame benchmark. Final release
testing should collect startup, frame, memory, and process-restoration data on a
low-memory device and a mid-range production target.

