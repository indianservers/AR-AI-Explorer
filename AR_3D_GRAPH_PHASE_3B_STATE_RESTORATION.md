# AR 3D Graph Phase 3B — State Restoration

Date: 2026-08-21

`SavedStateHandle` now preserves equation text, domain, resolution, valid-graph existence, one-time guidance state, yaw, pitch and uniform scale. Restored numeric transforms are sanitized by the gesture controller. A valid prior graph is regenerated once through the existing engine after reconnection.

ARCore sessions, anchors, renderer objects, GPU buffers, textures, cameras and Activity references are never serialized. On screen/resource recreation, any prior anchor is considered invalid. Graph data and user transform remain, placement changes to `GraphReadyForPlacement`, and the user sees `Tap to place again`. The regenerated graph cannot appear at a stale pose.

Tests cover process-style reconstruction with a new ViewModel, graph regeneration, transform restoration, guidance non-repetition, fresh placement, screen exit, Reset View identity, Reset Placement preservation and Clear identity/data removal. All passed in both formal cycles.
