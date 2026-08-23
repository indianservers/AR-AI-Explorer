# AR 3D Graph Phase 3C — Defect Register

Date: 2026-08-21

No new production defect was reproduced in Phase 3C. No production code, protected engine or UI behavior was changed. Phase 3C only expanded certification tests and generated evidence/reports.

| ID | Area | Severity | Origin | Environment | Expected | Actual | Disposition / retest |
|---|---|---:|---|---|---|---|---|
| P3C-LIM-01 | Physical AR | Release blocker | Physical-device limitation | No physical ADB target | Complete live AR certification | Cannot execute | Open; run exact physical checklist |
| P3C-LIM-02 | AR emulator | Release blocker | Emulator limitation | API 35 AVD without `com.google.ar.core` | Live camera/session/gesture tests | Unsupported path only | Open; provision ARCore-compatible AVD |
| P3C-LIM-03 | Endurance/performance | Release blocker | Physical-device limitation | No live ARCore | 60-minute camera/GPU/native/thermal run | Deterministic cumulative run only | Open; physical run required |
| P3C-LIM-04 | Full matrix/accessibility | Medium certification gap | Infrastructure limitation | One AVD | Lowest API, small/tablet, TalkBack, nav variants | Controlled configuration tests only | Open; expand device lab |
| P3C-LIM-05 | Repository state | Release-process blocker | Pre-existing/user workspace | Clean release candidate and attributable diff | Very dirty worktree; numerous user-requested Markdown deletions and unrelated edits | Preserved; do not reset; create clean candidate later |

The direct ADB launch of non-exported `MainActivity` was correctly rejected by Android security; launching the exported `SplashActivity` passed and is not a product defect.

All ten Phase 3B defects remain fixed according to three full related automated reruns. Critical/high defects found in Phase 3C: 0. Fixed in Phase 3C: 0. Failed automated tests: 0.
