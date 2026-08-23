# AR 3D Graph — Phase 2 Regression Report

## Baseline and preservation

The pre-implementation `assembleDebug test --continue` baseline passed. Existing Git changes and the user-requested Markdown removals were preserved. The Phase 1 shell launched on the same API 35 emulator before implementation.

The complete app instrumented suite contains 81 tests with substantial pre-existing failures unrelated to AR 3D Graph. The observed failure families include stale product-title assertions, old curriculum labels, an invalid regex in an existing test, integrated-concept expectations, older interactive-reference and learning-workspace expectations, and Phase 3 teacher-classroom expectations. No Phase 2 AR test belongs to these families.

## Existing-module checks

Manual navigation used one continuing emulator/app session and covered launch/home, the normal 3D Graph workspace, menu/workspace navigation, AR entry, rotation, background/resume, Clear, and Back. The original normal 3D Graph screen and its protected equation/properties UI blocks are byte-identical to backup. No normal workspace starts an ARCore session or requests camera permission.

The broad JVM suite exercises 2D Geometry, 3D Geometry, 2D Graph, normal 3D Graph, Solver/CAS, navigation, state, file codecs, and shared math behavior. The complete instrumented suite provides the broader emulator regression signal; known baseline failures are not counted as Phase 2 regressions.

## Final cycles

Cycle 1 used the exact final source: 33/33 AR JVM tests, 1,023/1,023 app JVM tests, APK assembly, Android-test compilation, both lint tasks, and 1/1 focused AR connected test passed. The final APK manual workflow also passed on the non-AR emulator.

Cycle 2 reran the focused AR connected workflow from a clean stopped test process: 1/1 passed. The same final APK then passed unsupported state, Plot, graph-ready state, rotation, background/resume, Clear, Back, and Logcat smoke checks.

The broad app connected run completed all 1,200 Solver golden cases before ADB/UTP teardown failed: 1,190 passed and 10 existing matrix step-agreement cases failed. Its XML contains 61 finalized app tests with 34 existing failures; the retained Phase 1 baseline was 81 total, 16 passed, and 65 failed. No AR test failed in either result set. Because the available app-wide suite is already red and ADB prevented a finalized complete report, two literal all-app green cycles cannot honestly be claimed. The two consecutive Phase 2-focused cycles are green with no new failure.

## Protection hashes

All 13 protected engine/parser/renderer files matched the pre-Phase-2 backup by SHA-256. Normal 3D Graph function blocks also matched: `Graph3DScreen` `F024AEDA…`, `Graph3DEquationPanel` `A0FC8A39…`, and `Graph3DPropertiesPanel` `CD9C82F6…`.
