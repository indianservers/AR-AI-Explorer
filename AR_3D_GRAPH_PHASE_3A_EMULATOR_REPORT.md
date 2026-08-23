# AR 3D Graph Phase 3A — Emulator Report

Date: 2026-08-21

Device: `Medium_Phone_API_35`, Android API 35, x86_64, 1080 × 2400, density 420.

The AR module connected suite passed 3/3 tests with zero failures in both unchanged-source release cycles. Its 201-operation screen test exercised equation entry, valid and invalid plotting, retries, Reset Placement, Clear and contained unsupported-viewport taps. Twenty repeated AR destination open/close cycles also passed.

Google Play Services for AR (`com.google.ar.core`) is not installed on the only available AVD. Consequently the AVD cannot create an ARCore session, supply camera tracking, place an anchor, or visually validate live rotation/pinch of a rendered graph. No fake camera, fake tracking state or false visual pass was introduced.
