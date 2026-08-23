# AR 3D Graph Phase 3C — Anchor Stress Report

Date: 2026-08-21

Test-double result: **PASS**. Real ARCore result: **BLOCKED**.

The anchor controller passed first placement plus 100 replacements while retaining exactly one active anchor. All 100 old anchors detached and the final anchor remained active. A separate test completed 25 place/reset and 25 place/clear cycles; all 50 anchors detached and active count returned to zero.

Injected conditions covered tracking pause, anchor factory failure and graph-attachment failure. Replacement is transactional: the previous anchor is kept until the new anchor and graph attachment succeed; failed new anchors detach; failed replacements preserve the old anchor. Reset, Clear, close/dispose and five reopen cycles detach ownership.

Pose calculation failures are contained by placement math before controller invocation. Lifecycle/session interruption, renderer release and screen exit have related coordinator/renderer/view-model tests. Old-anchor `detach()` throwing is not reproducible with the current interface test double and must be observed against ARCore. Rotation/scale interleaving and session-interruption stress require live ARCore and are blocked.

No anchor leak was detected in deterministic tests. Production certification still requires the physical checklist’s 100 live replacements, 25 live resets, 25 live clears and memory/session observation.
