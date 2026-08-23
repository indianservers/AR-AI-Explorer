# AR 3D Graph — Phase 2 Test Report

## Environment

- Host: Windows, workspace `C:\Indian Servers\AIExplorer`.
- Emulator: `Medium_Phone_API_35`, Android 15/API 35, x86_64, Google Play system image, virtual-scene camera.
- Emulator limitation: `com.google.ar.core` is not installed, so the product correctly reports “ARCore is not installed.” No AR success was faked.

## Automated coverage

Passing Phase 2 suites cover:

- all 14 required explicit equations;
- exact bridge/direct-engine vertices and explicit regular-grid topology;
- exact native typed-mesh vertices/indices for implicit sphere and parametric torus;
- multiple equation order, colours, opacity, domains, density, invalid/empty/extreme cases;
- centre, corner, edge, portrait, landscape, alternate projection, and failure ray cases;
- 1.5 m pose, camera-facing yaw, finite values, and no camera roll;
- first placement, 20 replacements, failed replacement recovery, paused tracking, reset, clear, disposal, and five reopen cycles;
- ViewModel generation, cancellation/latest-result behavior, invalid-input preservation, placement states, tracking pause, and graph-input process recreation;
- focused Compose UI input, Plot, ready state, Clear, and Back.

Build/test commands completed successfully before the final lifecycle correction:

```text
gradlew assembleDebug test --continue
gradlew :ar3dgraph:testDebugUnitTest :app:testDebugUnitTest
gradlew :ar3dgraph:compileDebugAndroidTestKotlin
gradlew :ar3dgraph:connectedDebugAndroidTest
gradlew assembleDebug test lintDebug --continue
```

Final exact-source results:

- `assembleDebug`, app/module JVM tests, Android-test compilation, and app/module `lintDebug`: passed, 181 tasks in 17m02s.
- AR module JVM: 33 tests, 33 passed.
- App JVM: 1,023 tests, 1,023 passed.
- Lint: zero errors (2 AR-module warnings; the app report contains existing warnings).
- Focused AR connected cycle 1 after stale-runner cleanup: 1/1 passed.
- Focused AR connected cycle 2: 1/1 passed.
- Manual final-APK cycle: unsupported state, Plot, rotation, background/resume, Clear, and Back passed; zero matching fatal/ANR/camera/renderer Logcat lines.

The broad app connected run wrote results for 61 tests (27 passed, 34 existing failures) and completed all 1,200 Solver golden rows (1,190 passed, 10 existing matrix final-step mismatches). ADB reset during result teardown, causing UTP to fail before it could finalize the remaining suite report. This is recorded as infrastructure/pre-existing regression evidence, not as a Phase 2 pass.

## Manual evidence

Evidence is stored under `outputs/ar3dgraph_phase2/cycle1` and `cycle2`. Cycle 1 shows the original 3D Graph return path. Cycle 2 was captured from the final APK and shows safe unsupported startup, generated graph-ready state, rotation, background/resume, Clear, Back, and final Logcat.

Post-fix Logcat contained no fatal exception, ANR, camera failure, or renderer failure. Static checks found zero `hitTest` references, one explicit `PlaneFindingMode.DISABLED`, zero plane-enabled references, and no surface-search prompt text.
