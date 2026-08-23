# AR 3D Graph Phase 3C — Emulator Report

Date: 2026-08-21

## Available target

| Field | Value |
|---|---|
| ADB serial | `emulator-5554` |
| AVD/profile | `Medium_Phone_API_35(AVD)` / `sdk_gphone64_x86_64` |
| Manufacturer | Google |
| Android | 15 / API 35 |
| Resolution / density | 1080 × 2400 / 420 dpi |
| Orientation | Portrait device state; portrait/landscape layouts exercised synthetically |
| Image | x86_64 emulator image |
| Google Play Services for AR | `com.google.ar.core` absent |
| Virtual AR camera | Not usable without ARCore package/image |

The final APK installed successfully. Cold launch through the exported launcher (`SplashActivity`) completed in 3,557 ms and reached `MainActivity`. Home memory after stabilization was 138,233 KB PSS and 247,320 KB RSS. Evidence screenshot: [API 35 Home](phase3c_evidence/api35-home.png).

Unsupported-device tests passed: normal install/start, no startup camera permission, safe AR screen composition, Plot/Clear/Back availability, 25 screen open/close cycles, responsive profiles, Help, invalid-input correction, and no crash/session/camera ownership on the non-AR target.

Unavailable configurations: ARCore-compatible AVD, lowest supported API AVD, independent small phone, tablet, Google Play AR image, virtual AR camera, real gesture/three-button system-navigation comparison, and live large-font/TalkBack matrix. Five phone/tablet/portrait/landscape/font-scale configurations were exercised through controlled Compose configuration changes, but are not claimed as separate emulator hardware.

Logs contained no test failure, crash or skip. Screenshots of live AR could not be captured because ARCore is absent. No capability was faked.
