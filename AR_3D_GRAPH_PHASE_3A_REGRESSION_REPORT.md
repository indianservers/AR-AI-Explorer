# AR 3D Graph Phase 3A — Regression Report

Date: 2026-08-21

Two consecutive unchanged-source cycles ran `assembleDebug`, all JVM tests, and `:ar3dgraph:connectedDebugAndroidTest`. Both completed successfully.

Per cycle: 1,117 JVM tests passed with zero failures/skips; 3 AR connected tests passed with zero failures/skips; the AR screen performed 201 meaningful UI operations; 20 open/close cycles passed. The full debug APK and all debug library artifacts assembled.

The 13 protected normal graph/geometry engine files matched their pre-Phase-3A baseline hashes. The normal 2D/3D graph, geometry and smart-board paths were not edited by Phase 3A. The AR camera permission remains destination-scoped and startup does not request it.

Known broader repository dirtiness and user-requested Markdown deletions predate Phase 3A and were preserved. Phase 3A created only its required eight reports at repository root.
