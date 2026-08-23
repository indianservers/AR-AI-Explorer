# AR 3D Graph Phase 2C — Lifecycle and Recovery

Result: **PASS for available and injected paths; supported-session runtime blocked**

## Covered paths

- checking, supported, unsupported, missing, update and bounded retry capability states;
- permission required, granted, retryable denial and permanent denial classification;
- installation-request short circuit before session creation;
- create/resume/pause/close ordering and duplicate-resume prevention;
- camera unavailable, session error and retry messages through controlled transitions;
- tracking pause/stop and placement retry state;
- invalid matrices and pose failures;
- engine exception, empty mesh, invalid indices, non-finite vertices and missing palette;
- generation cancellation on Clear and screen disposal;
- immediate renderer CPU/anchor cleanup even after GL pause;
- 10 actual background/resume transitions, 10 rotations and 10 verified route re-entries.

Phase 2C fixed an engine-boundary exception path: unexpected contract exceptions are now converted into `GenerationError`, loading terminates, and a subsequent valid Plot recovers. Stale generations remain generation-ID guarded and cannot republish after Clear or exit.

No fatal exception, ANR, camera-unavailable exception or renderer error appeared in the persistent log inspection. CAMERA stayed ungranted and `dumpsys media.camera` showed no app client after exit.

Live permission revocation, supported session loss, camera contention and ARCore resume failure require a supported target.
