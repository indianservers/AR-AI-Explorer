# AR 3D Graph Phase 2C — UI Test Matrix

Date: 2026-08-21  
Status: **Executable coverage passed; real AR workflow blocked**

## Environment

| Target | Configuration | ARCore | Result |
| --- | --- | --- | --- |
| Medium_Phone_API_35 | Android 15/API 35, x86_64, 1080×2400, 420 dpi, Google Play image | `com.google.ar.core` absent; Play Services 26.29.32 | Non-AR and unsupported paths passed |
| ARCore-capable emulator | Not installed | Unavailable | Blocked |
| Physical AR device | Not connected | Unavailable | Blocked |

## UI operations

The connected AR screen test performed **201 meaningful visible-control operations** in one test session:

- 25 valid equation entries and Plot actions;
- 25 Reset Placement actions that retained graph data;
- 25 invalid equation entries and controlled rejected Plot actions;
- 25 Clear actions;
- 25 corrected equation entries and 25 successful retries;
- one unsupported camera-viewport tap proving it did not bypass the unavailable-session state.

The test clicks and edits Compose UI nodes; it does not call ViewModel actions as a substitute. The test engine is injected only because the AVD cannot run ARCore. Real viewport placement remains blocked.

## Persistent installed-app matrix

| Sequence | Count | Result |
| --- | ---: | --- |
| Launch without startup camera permission | 1 | Pass |
| Home → Visual Workspaces → AR via semantic nodes | 1 initial + 10 verified re-entries | Pass |
| Background/resume while verified on AR route | 10 | Pass |
| Portrait/landscape transitions | 10 | Pass |
| AR exit/re-entry using semantic route verification | 10 | Pass |
| Unsupported AR message and controlled navigation | Repeated | Pass |
| Actual camera placement/reposition/reset/place | 0 | Blocked by absent ARCore |

Evidence is under `outputs/ar3dgraph_phase2c/persistent/`, including `ar-open.png`, UI XML, route-verification dumps and screenshots. CAMERA remained ungranted.

## Tap routing

The camera view alone owns placement touch handling; equation fields, keyboard, buttons, app bar and system areas are outside that `AndroidView`. Local `MotionEvent.x/y` coordinates are passed to the renderer. Placement math now rejects coordinates outside `[0,width] × [0,height]`. Insets, toolbar and bottom controls therefore cannot become raw placement offsets. Phone portrait/landscape and synthetic tablet viewports pass numerically; a real tablet UI remains blocked.
