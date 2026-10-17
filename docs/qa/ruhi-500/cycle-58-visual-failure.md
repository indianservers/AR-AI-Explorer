# Cycle 58: numeric checks passed, visual verification failed

519 JVM tests and 36 emulator tests passed, but visual review contradicted the visible-point claim. `derivative-point-before.png` and `derivative-point-moved.png` still show D1 at (1,1), despite the current native drawing resolver returning (3,9). The off-curve question capture also retained the stale marker.

The Compose derivative drawing cache depended on functions, annotation metadata and view values, but omitted native points and point-reference bindings. The state-only assertions and a broad pixel-difference threshold did not prove correct rendered coordinates. This cycle is not sufficient evidence for the visible selected-point feature.

Cycle 59 adds those cache dependencies and exposes the exact derivative drawing payload in the existing canvas accessibility state. The native test checks the rendered payload at (3,9), then the unavailable diagnostic after an off-curve edit, before capturing. Numerical and source-preservation assertions remain. All source rows are retested before retraining and a fresh full emulator run.
