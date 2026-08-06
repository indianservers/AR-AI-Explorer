# Solver Phase 3 Performance Report

## Architecture controls

- No 3D engine is loaded for Solver visuals.
- Curve sampling is bounded at 129 points.
- Integral area models use 24 midpoint rectangles.
- Complex root order is bounded by the existing kernel at 24.
- Visual specs use immutable data and primitive values.
- A synchronized 48-entry access-ordered cache avoids regenerating repeated
  specifications.
- Canvas rendering allocates no bitmap and owns no external rendering resource.
- Full-screen display reuses the same specification.
- Playback is limited to one transition per 1.2 seconds and stops at the end.

## Benchmark support

`SolverPerformanceProbe` records nanosecond samples and reports median, p95 and
maximum latency by operation. The Phase 3 test suite measures representative
solve-and-specify workloads across arithmetic, equations, quadratics,
derivatives, definite integrals, complex roots and matrices.

Test thresholds:

- median solve-and-specify below 500 ms;
- p95 below 2,000 ms on the local JVM test environment.

The focused 36-method Solver suite completed in approximately 1.1 seconds in
the recorded Gradle XML results. This is a development-machine figure, not an
Android device guarantee.

## Rendering and accessibility

- Initial render and transition behavior are covered by compiled Compose UI
  tests and bounded Canvas complexity.
- Android animator scale zero disables timed playback and changes Play into
  explicit next-state advancement.
- Text alternatives remain available when graphics are not rendered.
- Large-screen full-screen mode does not increase sample count.

## Remaining device measurements

Frame timing, jank, memory pressure and rotation costs require benchmark runs on
representative physical Android devices. Those results are not fabricated in
this report.

