# AR 3D Graph Phase 3B — Performance Report

Date: 2026-08-21

## Measured final build

| Metric | Result |
|---|---:|
| Cold launcher-to-Splash activity | 3,156 ms |
| Home PSS / RSS | 128,584 KB / 232,956 KB |
| Simple graph generation/conversion | 8.1605 ms, 729 vertices, 4,056 indices |
| Medium graph | 2.3038 ms |
| Complex graph | 1.7060 ms |
| Three graphs | 3.9910 ms, 2,187 vertices, 12,168 indices |
| Five-cycle host heap delta | 3,670,232 bytes |

Phase 3A/Phase 2B observations were approximately 7–10 ms per single surface, 21.39 ms for three surfaces, 6.3 s upper-bound startup, and 129,826 KB cold-Home PSS. The methods and runtime state are not identical, so final values indicate preservation—not a claimed benchmark improvement.

Safe renderer work removed recurring direct-buffer creation for camera quad coordinates, reuses frame matrices, caches graph centre and GL attribute/uniform locations, and preserves upload-by-revision behavior. Buffer replacement is transactional. Mathematical density, vertices, indices, shaders and 1.5 m placement are unchanged. Unsafe AR allocations above 1,000,000 vertices or 6,000,000 indices are rejected before topology/GPU allocation rather than silently resampled.

ARCore initialization, camera readiness, GPU upload time, live placement latency, real rotation/scale frame response, native ARCore memory and idle-frame behavior could not be measured because `com.google.ar.core` is absent. No figures were invented.
