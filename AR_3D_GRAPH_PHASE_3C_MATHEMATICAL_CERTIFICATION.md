# AR 3D Graph Phase 3C — Mathematical Certification

Date: 2026-08-21  
Result: **PASS — 100/100 deterministic parity cases.**

The Phase 3C suite invokes `Existing3DGraphEngineBridge` and `GraphEngineAdapter` from the same request and compares the unchanged engine snapshot with the AR representation.

| Category | Cases |
|---|---:|
| Polynomial/algebraic | 20 |
| Trigonometric | 20 |
| Exponential/logarithmic | 10 |
| Rational/discontinuous | 10 |
| Invalid-domain/partial-region | 10 |
| Asymmetric orientation | 10 |
| Boundary/extreme-domain | 10 |
| Parametric/implicit/multiple-equation | 10 |
| Total | 100 |

Compared per case: validation success, request domain, density, equation identity and canonical equation, mesh count/order, vertex count and exact vertex values, index count and exact order, finite generated normals, colour count/finite alpha, opacity, and bounds. Vertex and index conversion is representation-preserving, so equality is exact. The placement suite uses a 0.00001 m floating-point tolerance; no mathematical parity tolerance was required.

All 100 cases passed in each of the three clean cycles as part of 1,133 JVM tests/cycle. No parser, engine, sampling, resolution, precision, mesh generator or mathematical default was modified. `git diff` reports no tracked change under the protected `core`/`spatial` engine paths used by the audit.

Live visual orientation of labelled axes remains a physical-device gate; this does not invalidate engine/adapter mathematical parity, but it prevents production certification.
