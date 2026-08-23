# AR 3D Graph Phase 3B — Resource Cleanup

Date: 2026-08-21

Twenty unsupported-device AR open/close cycles passed in each connected run. Twenty session-coordinator create/pause/resume/close cycles passed. Clear and screen exit cancel generation and invalidate its generation ID; late callbacks cannot publish. `AndroidView.onRelease` disposes the camera view, gesture controller and renderer. Camera-view disposal, renderer close, session close and anchor reset are idempotent.

Renderer entry points reject graph, placement, reset and clear calls after close. Replacement mesh buffers are fully prepared before they replace the active graph. Clear resets anchor, graph data, transform and buffer ownership. Reset Placement detaches only the anchor. Reset View changes only user transform.

After cold final launch, Home used 128,584 KB PSS and 232,956 KB RSS. `dumpsys media.camera` showed no AI Explorer camera client. The AVD had no ARCore process/session, so real camera release, native anchor count, GPU buffer reclamation and supported-session leak stabilization are blocked for Phase 3C.
