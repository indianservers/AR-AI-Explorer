# AR 3D Graph Phase 3B — Lifecycle Report

Date: 2026-08-21

| Resource | Owner | Creation | Pause/resume | Destruction |
|---|---|---|---|---|
| ARCore session/camera | `ARCoreSessionManager` | successful screen entry after support and permission | GLSurfaceView pauses before session; permission is rechecked before resume | idempotent manager close |
| Lifecycle observer | `AR3DGraphScreen` composition | screen composition | dispatches ordered pause/resume | removed by `DisposableEffect` |
| Camera view/renderer/gesture controller | `AndroidView` instance | running session | GLSurfaceView lifecycle | `onRelease` and screen disposal; idempotent view disposal |
| Anchor | `AnchorPlacementController` | successful tap transaction | retained while same session pauses | reset, clear, renderer close or view release |
| Graph root/CPU mesh buffers | `ARGraphRenderer` GL owner | accepted graph revision | retained during pause | clear/replacement/close drops ownership |
| GL programs/camera texture | `ARGraphRenderer` EGL owner | surface creation | context preserved where Android permits | queued EGL-thread deletion |
| Generation job | `AR3DGraphViewModel` | Plot/restore | independent executor | cancellation plus generation-ID invalidation |
| ViewModel/serializable UI state | lifecycle ViewModel | route entry | survives configuration | cancels work on clear/final destruction |

Phase 3B fixed candidate-session cleanup when configuration fails, true session-owner replacement on Retry, permission-revocation gating before resume, calls-after-close rejection, idempotent camera-view release and stale-anchor invalidation on resource recreation. Twenty coordinator cycles and twenty UI open/close cycles passed. Supported ARCore camera contention, screen-off camera recovery and native session recreation remain blocked by the AVD.

No AR resource is held by a shared mutable singleton. Activity ownership is limited to the screen-scoped session manager and capability checker; changing the Activity or retry epoch creates a new owner and disposes the old one.
