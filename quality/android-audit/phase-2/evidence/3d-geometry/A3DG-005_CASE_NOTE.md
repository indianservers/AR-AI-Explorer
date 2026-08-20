# A3DG-005 — Zoom the scene

Date: 2026-08-20  
Device: `Medium_Phone_API_35`, 1080x2400  
Session: reused existing AIExplorer session; no relaunch.

## Steps attempted

1. Navigated Graph → Home → 3D through normal app UI.
2. Started from empty 3D workspace.
3. Added Cube through `+ Add`.
4. Verified scene accessibility description included Cube at origin with volume/area.
5. Expanded View control.
6. Attempted canvas zoom-like gestures around the cube.
7. Verified cube remained visible/selectable.
8. Cleared workspace through Clear All confirmation and verified `Scene 0`.

## Result

Partial.

The app stayed stable and the cube remained visible/selectable, but the test environment did not provide a reliable true two-finger pinch through stock `adb input`. The exposed View UI showed `1x` and did not expose Fit to Scene, Reset View, minimum zoom, or maximum zoom controls in the accessibility tree.

## Evidence files

- `a3dg-005-before.xml`
- `a3dg-005-after.xml`
- `a3dg-005-after-clear.xml`

