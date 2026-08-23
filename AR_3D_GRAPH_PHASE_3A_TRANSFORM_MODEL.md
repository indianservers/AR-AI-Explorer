# AR 3D Graph Phase 3A — Transform Model

Date: 2026-08-21

The model matrix is composed as:

`world = anchorPlacementFacing × userRotation × userUniformScale × graphNormalization`

`graphNormalization` preserves the existing coordinate conversion: centre the graph bounds, convert math units to 0.1 metres, then rotate the graph's mathematical Z-up frame into the renderer frame. The user transform is applied above the single graph root, so surfaces, axes and grid move together.

Rotation is represented by an immutable normalized quaternion derived from bounded yaw and pitch. Yaw wraps to [-180°, 180°]; pitch clamps to [-80°, 80°]. Scale is uniform and clamps to [0.35, 3.0]. These bounds prevent inversion, disappearance and unstable extreme values.

Repositioning replaces only the anchor and preserves the user transform. Reset Placement removes only the anchor. Reset View restores identity rotation and scale without changing the anchor. Regenerating graph geometry preserves both anchor and user transform. Clear and lifecycle disposal restore identity and release graph/anchor ownership.
