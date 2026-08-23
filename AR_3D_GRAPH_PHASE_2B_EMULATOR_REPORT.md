# AR 3D Graph Phase 2B — Emulator Report

## Tested AVD

| Field | Value |
| --- | --- |
| AVD | Medium_Phone_API_35 |
| Android | 15 / API 35 |
| ABI | x86_64 |
| Resolution / density | 1080×2400 / 420 dpi |
| Orientations | Portrait and landscape |
| Image | Google Play-enabled image |
| Google Play Services | 26.29.32 |
| Google Play Services for AR | Package `com.google.ar.core` absent |
| Virtual camera/scene | Virtual camera advertised, but no usable ARCore session |

## Non-AR behavior

- APK installation and launcher start passed.
- Startup did not request camera permission.
- AR entry remained available and opened without a crash.
- The screen accurately reported `ARCore is not installed`.
- Retry remained controlled; it did not loop installation/session creation.
- Plot generated graph data while the unsupported banner remained stable.
- Reset Placement, Clear and Back worked.
- Portrait/landscape rotation and background/resume retained consistent UI state.
- Twenty connected open/close cycles passed.
- Camera permission remained `false`; `dumpsys media.camera` showed no app camera client.
- Logcat contained no app fatal exception, ANR, `CameraNotAvailableException`, or renderer failure.

## Persistent-session observations

The final workflow used one app process wherever practical. `GraphReadyForPlacement` survived rotation and HOME/resume. Background `COMPLETE` trim succeeded and the app resumed at Home. Existing workspaces remained navigable afterward. Evidence includes `home.png`, `ar-open.png`, `ar-plotted.png`, `landscape.png`, UI dumps and `logcat.txt` in `outputs/ar3dgraph_phase2b/cycle1/`.

## Limitations

No other AVD profiles were installed. Consequently, lowest-API, small-phone and tablet validation are blocked. Because ARCore is absent, camera preview, permission dialogs for a supported path, tracking, real anchors, GL AR rendering, live placement, real session loss and recovery cannot be honestly asserted. A physical ARCore-supported device remains required.
