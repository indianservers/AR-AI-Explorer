# AR 3D Graph Phase 3C — Performance Report

Date: 2026-08-21

| Metric | Phase 3C | Comparison |
|---|---:|---|
| Cold launcher to Splash | 3,557 ms | Phase 3B 3,156 ms; +401 ms, normal single-run variance |
| Stabilized Home PSS | 138,233 KB | Phase 3B 128,584 KB; +9,649 KB |
| Stabilized Home RSS | 247,320 KB | Phase 3B 232,956 KB; +14,364 KB |
| APK size | 112,293,975 bytes | Final debug artifact |
| Simple generation/conversion | 8.1605 ms | Phase 3B frozen implementation |
| Medium generation | 2.3038 ms | Phase 3B frozen implementation |
| Complex generation | 1.7060 ms | Phase 3B frozen implementation |
| Three meshes | 3.9910 ms | 2,187 vertices / 12,168 indices |
| Five-cycle host heap delta | 3,670,232 bytes | Phase 3B measurement |

The Phase 3C cold run used exported `SplashActivity`, reached `MainActivity`, and was measured by `am start -W`. Phase 3C did not change production code, mathematical density, vertices, topology or shaders. The PSS/RSS increase is a single-sample observation on a warmed API 35 emulator and is not interpreted as a regression without a controlled series.

ARCore session initialization, camera-preview readiness, GPU upload, placement/reposition latency, rotation/scale frames, native ARCore/GPU memory, idle AR use, thermal and battery figures are unavailable because ARCore and a physical device are absent. No values are invented; these are release blockers.
