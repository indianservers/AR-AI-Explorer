# AR 3D Graph Phase 3B — Regression Report

Date: 2026-08-21

Two formal consecutive cycles ran on unchanged final source:

`assembleDebug test :ar3dgraph:lintDebug :ar3dgraph:connectedDebugAndroidTest --continue`

Each cycle passed 1,128 JVM tests and 5 connected tests with zero failures, errors or skips. Across both cycles: 2,266 passes. AR lint completed with zero errors (four warnings and one hint: responsive Configuration access, DI-only custom View constructor, version-catalog preference and primitive-state preference).

Connected coverage included 201 logged graph-screen operations, 20 open/close cycles, 20 Help clicks, five responsive configuration changes and the basic plot/clear/back workflow—at least 270 meaningful UI/state operations per cycle. The required live categories (15 rotations, 15 pinches, 15 repositions and 10 enabled Reset View operations) remain blocked because the graph cannot be placed without ARCore; they are not counted as passes.

Original 3D graph and cross-workspace production sources were not modified in Phase 3B. Their 1,027+ app JVM regression remains green, and the prior Phase 2C original-3D connected deep check remains applicable to unchanged code. The normal app starts without requesting CAMERA and no camera client was present.

Phase 3B changes are confined to `ar3dgraph` source/tests and these nine reports. The 13 protected engine/parser/renderer baselines remain untouched by Phase 3B; known pre-existing dirty app files and user-requested Markdown deletions were preserved.

A repository-wide app lint attempt completed all functional gates but its unrelated app-analysis tail exceeded the practical window and was stopped; formal cycles use the scoped AR lint required for Phase 3B. No app lint pass is claimed.
