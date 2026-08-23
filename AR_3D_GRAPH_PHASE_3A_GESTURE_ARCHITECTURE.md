# AR 3D Graph Phase 3A — Gesture Architecture

Date: 2026-08-21

## Ownership

`ARGraphCameraView` converts Android `MotionEvent` input into platform-independent gesture events. `ARGraphGestureController` is the sole arbitration state machine and owns the immutable user transform. `ARGraphRenderer` receives transform snapshots through an `AtomicReference` and applies them on the GL thread. The anchor remains owned by `AnchorPlacementController`.

The explicit states are Idle, PossibleTap, Rotating, Scaling and Cancelled. A tap is emitted only after an up event within touch slop and 500 ms. Movement beyond slop promotes a placed graph to rotation and suppresses placement. A second pointer promotes the interaction to scaling and suppresses placement. Pointer loss, a third pointer, leaving the viewport, tracking loss, reset, lifecycle disposal, or Android cancellation produces no placement.

## Isolation

Gesture code is confined to `ar3dgraph`. It does not depend on the normal 2D graph, normal 3D graph, geometry, smart-board or camera workflows. No shared renderer, parser or protected graph engine file was modified.

## Threading

Touch classification runs on the Android UI thread. It publishes immutable `ARGraphTransformState` objects. The GL thread consumes the newest snapshot once per frame; no mutable matrix is shared between threads and gesture motion never rebuilds graph geometry or creates an anchor.

## Accessibility and system interaction

Only events delivered inside the AR camera viewport are consumed. Compose controls, system Back and app navigation remain outside the GLSurfaceView event stream. The viewport exposes a descriptive content description and Reset View is a normal accessible button.
