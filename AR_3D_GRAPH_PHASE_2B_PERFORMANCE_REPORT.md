# AR 3D Graph Phase 2B — Performance and Resources

Result: **PASS for measured non-AR/CPU paths; live AR frame performance blocked**

## Deterministic engine/adapter measurements

| Case | Time | Geometry |
| --- | ---: | --- |
| Simple surface | 10.1809 ms | 729 vertices, 4,056 indices |
| Medium surface | 7.3800 ms | 729 vertices, 4,056 indices |
| Complex surface | 7.3172 ms | 729 vertices, 4,056 indices |
| Three surfaces | 21.3889 ms | 2,187 vertices, 12,168 indices |
| Five generate/convert cycles | 4,194,304-byte heap delta | No test failure or retained task |

These are emulator-host JVM observations, not production benchmarks. Geometry generation occurs on Plot, not per tracking/frame event; 100 frame/placement events invoked the engine once.

## Emulator process measurements

| Point | PSS | RSS |
| --- | ---: | ---: |
| Cold Home | 129,826 KB | 240,336 KB |
| AR unsupported screen | 140,847 KB | 258,168 KB |
| After Plot | 139,670 KB | Not separately material |
| After AR exit | 143,801 KB | 262,384 KB |
| After background COMPLETE trim | 136,590 KB | 255,204 KB |

The post-trim PSS was about 6.8 MB above cold Home. It is consistent with normal app/runtime caching and was not monotonic AR growth; no ARCore session was active. Twenty open/close cycles and 20 five-workspace cycles completed without a crash. CPU-side graph/anchor state tests show one active placement, immediate old-anchor detachment, and renderer CPU-resource release on dispose.

Launcher-to-idle Home was an upper-bound ~6.3 s with UIAutomator polling. Navigation/scrolling to AR ready took ~9.4 s with automation gestures. Plot-to-ready observed through UIAutomator took ~2.6 s, while isolated graph generation measured about 7–10 ms.

## Blocked metrics

AR session initialization, first real placement, reposition latency against ARCore, camera frame stability, steady-scene CPU, GPU workload and real buffer/anchor retention require an ARCore-capable target. No optimization was introduced because no unusable performance defect was confirmed.
