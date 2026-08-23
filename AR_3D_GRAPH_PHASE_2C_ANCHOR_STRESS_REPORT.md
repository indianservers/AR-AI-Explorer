# AR 3D Graph Phase 2C — Anchor Stress Report

Result: **PASS with controlled anchors; real ARCore anchors blocked**

Phase 2C hardened replacement into an attachment-aware transaction:

1. validate tracking and finite pose;
2. create the replacement anchor;
3. attach/transfer the graph root through the activation callback;
4. commit new active ownership;
5. detach the previous anchor.

If creation fails, the old anchor is retained. If graph attachment fails, the replacement is detached and the old anchor remains owned and attached. Paused tracking creates nothing.

Automated stress performed one initial placement plus **100 consecutive replacements**. At every stable point the controller reported one active anchor, all earlier anchors were detached, and the last remained active. Reset, Clear, Close and five owner reopen cycles left zero anchors. Failure injection covered creation failure and failure after creation/before attachment.

Clear/navigation/session concurrency is serialized through the controller’s synchronized operations. Renderer activation now occurs inside the transaction instead of after prior-anchor detachment.

Real ARCore anchor tracking, relocalization and detach behavior remain physical-device validation items.
