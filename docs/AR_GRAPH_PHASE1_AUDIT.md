# AR 3D Graph Phase 1 audit and verification

## Active implementation

AR 3D Graph is the Graph3D studio in MainActivity.kt / SpatialARScreen, reached through AR Space. The older ar3dgraph SceneView screen is not the active AR Space route. This upgrade extends the existing GLES3 compositor, ARCore runtime, shared mathematical surface mesher, WorkspaceState, WorkspaceSnapshotCodec and CommandHistory.

The bundled MediaPipe Tasks HandLandmarker model and tasks-vision dependency already existed. Detection uses a bounded worker and the ARCore CPU image stream; no second camera session is introduced. The compositor admits at most one vision image every 100 ms. Current Android Tasks documentation supports this VIDEO-mode API: https://developers.google.com/edge/mediapipe/solutions/vision/hand_landmarker/android . VIDEO inference blocks its worker, not the UI thread. Handedness scores are classification confidence; the detector does not return a per-landmark confidence score, and the app does not invent one.

## Findings addressed

- The old toolbar mixed several studios and advanced phases in a tall panel. Graph3D now has Expanded, Compact and Immersive states, a small drawn hand icon, native size animation, upward/downward swipe transitions and temporary tool dialogs.
- Existing hand mode consumed every touch. Graph3D now offers Touch, Gesture and default Hybrid.
- Surface gestures previously fell through to whole-origin manipulation. Stable surface IDs now have separate transforms, visibility, lock, delete and name metadata. These are stored in the existing workspace session map and snapshot codec. Object and vertex gesture previews commit a single reusable workspace command.
- Surface sampling remains cached while object transforms change. Per-object model matrices align rendering and picking. The compositor updates model transforms without re-uploading unchanged mesh buffers.
- Landmark semantics, confidence gating, stable hover and palm orientation live in engine classes. Pinch hysteresis, capture baselines, stale-frame cancellation and filtered movement extend the existing gesture controller.
- Detector ownership now includes camera-active/live-AR state. Pause, permission loss and OFF stop image admission and close the detector. Resume rechecks permission. Camera denial has retry/settings and the existing 3D fallback.
- Native ARCore plane hit creation remains the placement path. Graph3D retains prior session anchors for placement undo; offsets and units stay separate from object transforms. Anchors are released with the runtime.
- Point/vertex selection and drag are a foundation only. Full face/edge constraints and later-phase analysis are not enabled by this work.

## Verification status

Final debug application and instrumentation APK builds passed. 170 arengine unit tests and 25 selected app regression tests passed (195 total). Four emulator instrumentation tests passed: graph toolbar/mode/explorer interactions, one-hand model inference, two-hand model inference, and all seven AR Space studio navigation. Model tests also checked confidence, world landmarks, palm-normal extraction, distinct hand IDs and frame rejection after detector close.

Screenshots and exact build/device logs are in docs/qa/ar-phase1-evidence/. Expanded, compact, immersive and bottom-sheet layouts were inspected. The emulator returned an unrecoverable ARCore runtime error and displayed the explicit simulator/fallback path. These UI tests do not establish successful ARCore placement or live camera gesture behavior. No supported physical phone was connected for final acceptance.

Physical acceptance is pending. At the time of the build, adb lists emulator-5554 and emulator-5556 only. Test-image inference is not evidence of live rear-camera pointing, pinch manipulation or walk-around anchor stability. Phase 1 must not be labelled physically verified until those checks are performed on a supported physical device.

Remaining physical checks: horizontal-plane placement; move around the anchor; rear-camera one/two-hand recognition; pointing/hover; pinch/select/drag; two-hand scale/rotation; OFF followed by touch; background/resume and permission revocation; frame pacing under sustained camera and hand load.
