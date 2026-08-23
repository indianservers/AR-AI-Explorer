# AR 3D Graph Phase 3A — Defects

Date: 2026-08-21

## Fixed

- P3A-001: every ACTION_UP previously requested placement, so drag and pinch could reposition the graph. Fixed with explicit arbitration.
- P3A-002: renderer had no user graph-root transform. Fixed with atomic immutable rotation/scale snapshots.
- P3A-003: no safe transform reset independent of anchor reset. Fixed with Reset View.
- P3A-004: clear/dispose could leave future gesture state conceptually live. Fixed with explicit controller reset/disposal.

## Open environment blocker

- P3A-ENV-001: the sole AVD has no `com.google.ar.core`. Severity: release-gate blocker for live AR gesture validation. Code and deterministic tests pass, but rotation direction, visual smoothness, occlusion and camera/tracking interruption cannot be certified on this device.

No known reproducible gesture-controller defect remains in executable coverage.
