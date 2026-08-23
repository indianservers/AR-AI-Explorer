# AR 3D Graph Phase 2B — Test Matrix

Date: 2026-08-21  
Release-gate status: **BLOCKED**

## Environment matrix

| Configuration | API / display | ARCore | Coverage | Result |
| --- | --- | --- | --- | --- |
| Medium Phone API 35 AVD | Android 15/API 35, 1080×2400, 420 dpi, x86_64, portrait and landscape | `com.google.ar.core` absent; Play Services 26.29.32; virtual camera advertised | Install/start, unsupported flow, Plot/Clear/Reset/Back, rotation, background/resume, 20 reopen cycles, memory/logcat, existing-workspace navigation | Pass for non-AR coverage |
| ARCore-capable emulator | Not available | Not available | Camera preview, tracking, real anchors, frames and GL rendering | Blocked |
| Lowest supported API | Not installed | Unknown | Backward compatibility | Blocked |
| Small phone | Not installed | Unknown | Compact layout and coordinate mapping | Blocked |
| Tablet | Not installed | Unknown | Large viewport and coordinate mapping | Blocked |

ARCore support was not faked. Controlled JVM/test doubles cover states and math that cannot be driven on this AVD, but do not replace real tracking validation.

## Functional matrix

| Area | Test evidence | Result |
| --- | --- | --- |
| Build | `assembleDebug test --continue`; two forced JVM rebuild cycles | Pass |
| Capability states | Checking, supported/unsupported, missing/install/update/requested, retry/cancel/error modeled with fakes | Pass |
| Permission policy | Camera permission remained false at launch, in unsupported AR, and after exit; permission state machine tested | Pass; live grant/deny dialogs blocked |
| Unsupported device | Accurate “ARCore is not installed” state, controlled retry, Back, no crash | Pass |
| Equation parity | 16 required explicit surfaces plus implicit sphere and parametric torus | Pass |
| Validation/races | Empty, whitespace, syntax/variables/parentheses/functions, divide/non-finite/extremes, repeated Plot, Clear/exit while generating | Pass |
| Tap ray | Nine canonical positions, 100 seeded random positions, insets, invalid viewports/matrices | Pass |
| Fixed distance | Every accepted ray placement numerically 1.5 m | Pass |
| Orientation transform | Asymmetric equations, varied camera translation/yaw/pitch; upright camera-facing pose and handedness invariants | Pass mathematically; live visual check blocked |
| Plane/wall detection | Static search plus session configuration audit | Pass |
| Anchor lifecycle | 50 replacements, one active, reset/clear/close cleanup using doubles | Pass; real ARCore anchors blocked |
| Lifecycle/resources | pause/resume/close ordering, duplicate-loop prevention, cancellation, 20 reopen cycles, rotation/background/trim | Pass for available paths |
| AR render | Contract/buffer conversion and renderer resource tests | Pass in automation; camera/GL frame output blocked |
| Existing workspaces | JVM suite; persistent five-workspace navigation; representative existing connected tests | 2/5 connected pass, 3 pre-existing assertion failures |
| Protected sources | 13/13 protected engine/parser/renderer files and three normal-3D UI blocks | Pass |

## Blocked runtime scenarios

The unavailable ARCore-capable target blocks 22 live scenario groups: camera preview; permission grant, deny, deny twice, permanent deny and Settings recovery; installation/update UI; session creation; camera unavailable recovery; tracking pause/recovery; real anchor creation/replacement; first placement; real orientation/mirroring; axes/grid/labels visual inspection; depth/front/back faces; transparency; multi-graph rendering; portrait/landscape projection from ARCore; frame stability; CPU/GPU frame workload; and session-loss recreation.

Screenshots and UI dumps are under `outputs/ar3dgraph_phase2b/cycle1/`.
