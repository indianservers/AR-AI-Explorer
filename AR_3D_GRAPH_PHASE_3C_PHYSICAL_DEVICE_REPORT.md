# AR 3D Graph Phase 3C — Physical Device Report

Date: 2026-08-21  
Result: **BLOCKED — no physical ADB device was connected.**

`adb devices -l` exposed only `emulator-5554`. Therefore no manufacturer/model, Android version, ARCore support, Google Play Services for AR version, camera, thermal, battery or physical tracking result exists. No physical-device pass is claimed.

## Exact execution checklist for the next authorized ARCore device

Record: date/time, ADB authorization, manufacturer/model, Android/API, ARCore certification, Google Play Services for AR version, resolution/density, orientation, app variant, APK SHA-256, available storage, initial battery and thermal state. Do not erase user data or change unrelated settings.

1. Install only APK SHA-256 `38513BAF66F82AA31F2735396C2E15A86B204E4F21B9E1366E0BD165ADDF4B92`.
2. Verify cold startup and absence of camera permission outside AR.
3. Enter AR and exercise ARCore install/update handling.
4. Deny, grant and revoke camera permission; verify recovery.
5. Verify camera preview and tracking initialization.
6. Generate simple, asymmetric, complex, parametric and multiple graphs.
7. At nine viewport locations, tap anywhere without waiting for a plane.
8. Measure camera-to-anchor distance; accept 1.5 m within the documented device tolerance.
9. Verify camera-facing, upright, non-mirrored X/Y/Z orientation while yawing, pitching and walking around.
10. Perform 25 horizontal, 25 vertical and 25 diagonal drags; verify limits and no reposition.
11. Perform 25 pinch-in and 25 pinch-out gestures; verify 0.35–3.0 limits and no reposition.
12. Verify short tap repositions and old anchor detaches.
13. Reposition 100 times; confirm one active graph anchor.
14. Reset View 30 times and confirm identity transform only.
15. Reset Placement 25 times and re-place each time.
16. Clear 25 times; regenerate and re-place after every clear.
17. Exercise tracking loss/recovery, camera obstruction and session interruption.
18. Run background/resume, rotation, process recreation, exit/re-entry and another-module transitions.
19. Verify camera release after exit and no duplicate session/render loop.
20. Run TalkBack, large text/display scaling, Switch Access where available, and system Back/navigation variants.
21. Run at least 60 continuous minutes with the operation counts in the Phase 3C endurance report.
22. Sample Java/native/GPU memory, anchors, sessions, renderers and jobs throughout; verify stabilization.
23. Record startup, preview, generation, upload, placement, reposition and interaction latency.
24. Record frame stability, thermal status and battery delta without inventing unavailable metrics.
25. Re-run existing modules and 100 original 3D operations after AR endurance.
26. Capture screenshots/video and filtered crash/ANR/session logs.
27. Repeat the complete release cycle three times on an unchanged release-candidate build.
28. Obtain explicit human sign-off only after every mandatory gate passes.

Final status: **Emulator and automated validation complete; physical-device AR certification pending. Not yet approved for production release.**
