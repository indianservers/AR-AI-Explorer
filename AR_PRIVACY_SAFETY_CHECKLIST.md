# AR spatial maths privacy and safety checklist

Phase 7 treats every physical measurement as an educational estimate. This checklist is a release gate for live AR builds.

- [x] Camera permission is requested only when the learner enables live AR.
- [x] Camera frames remain in the local ARCore/OpenGL compositor and are not saved or uploaded.
- [x] Devices without ARCore, denied camera permission, and interrupted sessions retain the full spatial simulator.
- [x] Placement accepts only sufficiently confident plane/depth hits and clamps unsafe distance or height.
- [x] Measurements show propagated uncertainty and the words “educational estimate”.
- [x] Tracking loss preserves maths state and reports relocalization guidance.
- [x] Persistent data contains mathematical scene poses and anchor identifiers, never camera imagery.
- [x] Depth occlusion is capability-gated; unsupported devices retain outlined objects.
- [x] Environment lighting changes presentation only, never mathematical values.
- [x] Thermal policy progressively reduces mesh density, lighting, depth, and target frame rate.
- [x] Critical thermal state exposes a pause/safety quality policy.
- [x] Placement guidance reminds learners to remain aware of surroundings and avoid walking backward.
- [x] Future classroom sharing is opt-in and the network transport remains detached by default.

## Device verification before release

1. Test a current ARCore phone with depth, one without depth, and one unsupported device.
2. Test permission grant, denial, later grant, background/resume, rotation, and tracking loss.
3. Verify plane and depth hit placement, drag, pinch, twist, reset, undo, and relocalization.
4. Verify the six spatial lesson scenes at Balanced and Safety quality.
5. Confirm no camera frame or room image appears in app storage, logs, exports, or network traffic.
