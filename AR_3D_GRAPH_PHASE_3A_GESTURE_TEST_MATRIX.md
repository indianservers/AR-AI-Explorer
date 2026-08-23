# AR 3D Graph Phase 3A — Gesture Test Matrix

Date: 2026-08-21

| Scenario | Automated result | Live AR result |
|---|---:|---:|
| Short tap places/repositions | Pass | Blocked by missing ARCore |
| Jitter below touch slop | Pass | Blocked |
| Long press suppression | Pass | Blocked |
| Horizontal/vertical/diagonal rotation | Pass, bounds and deltas | Blocked |
| Slow/fast/repeated drags | Pass, 100 repeated rotations | Blocked |
| Pinch in/out and clamps | Pass | Blocked |
| Drag to pinch transition | Pass | Blocked |
| Pointer-up and third-pointer cancellation | Pass | Blocked |
| Tracking loss during gesture | Pass | Blocked |
| Viewport exit and Android cancel | Pass | Blocked |
| Reset View identity | Pass | Blocked |
| Clear/dispose late-event safety | Pass | Blocked |
| Quaternion normalization | Pass | Blocked |
| AR screen 100+ UI operations | Pass, 201 operations/cycle | Not camera-dependent |

The state-machine tests validate the exact production controller used by `ARGraphCameraView`; they do not substitute for a real ARCore camera/tracking run.
