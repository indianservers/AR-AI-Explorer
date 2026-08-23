# AR 3D Graph Phase 3C — Full Regression Report

Date: 2026-08-21

Automated result: **PASS**. Complete manual device matrix: **INCOMPLETE/BLOCKED**.

Each final cycle ran the complete JVM test tasks for `app`, `ar3dgraph` and `arengine`: 1,028 + 79 + 26 = 1,133 tests, with zero failures/errors/skips. This covers home/navigation, geometry/graph engines, Solver/CAS, workspace/session/import-export contracts, themes/state, AR integration and resource/lifecycle components represented by the repository test suite.

The original non-AR 3D workspace instrumented test performed 120 real Compose operations per cycle (360 total): add equation, enter, Plot, Layers, Clear and close properties over valid basic, complex, implicit and parametric inputs. It passed 3/3. No camera permission was requested and no AR session/gesture controller participates in this test.

The 100-case parity suite confirms unchanged engine geometry. `git diff` shows no tracked changes under audited protected `core`, `spatial`, `Spatial3DRendering.kt` or `Graph3D*` paths. The AR module contains only contracts/adapters and does not copy the parser or mesh engine. No-plane search is clean except explicit disable configuration.

Limitations: a manual 25-operation pass in every named workspace was not independently executed during Phase 3C; coverage comes from the 1,028 app JVM tests, prior phase reports and the focused original 3D instrumentation. Screen rotation/background/manual import-export across every workspace and performance comparisons require a broader device lab. Repository-wide lint was not used as a release pass because known unrelated warnings belong to the dirty pre-existing app; AR lint passed at zero errors.

The worktree remains intentionally dirty. Existing user edits and Markdown deletions were preserved. The only Phase 3C source changes are test-only expansions in the parity, placement, gesture, anchor, AR screen and original 3D regression suites.
