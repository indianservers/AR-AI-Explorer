# AR 3D Graph Phase 2C — Regression Report

## Original 3D Graph

A dedicated connected test completed **60 visible operations** in the protected normal workspace: ten cycles of add equation, enter expression, Plot, open Layers, Clear all and close Properties. Coverage included basic, complex, trigonometric, exponential, asymmetric, rational, implicit and parametric surfaces. It passed in both release-gate cycles.

The normal workspace never requested CAMERA or initialized the Phase 2 AR session. Its source was not edited. Thirteen protected engine/parser/renderer files remain byte-identical to the pre-Phase-2 backup; the protected normal-3D screen blocks retain their Phase 2B hashes.

## Full application signal

- App JVM: 1,027/1,027 passed per cycle.
- Existing `arengine`: 26/26 passed per cycle.
- Twenty five-workspace navigation cycles test: 20 cycles × 2 runs passed, covering 2D Geometry, 3D Geometry, 2D Graph, normal 3D Graph and Solver.
- Persistent manual/semantic navigation visited Home, Visual Workspaces, AR and an existing data workspace without crash.

The broad historical app connected suite is not green: Phase 2B retained 61 finalized tests with 34 pre-existing failures and a focused five-test subset with three stale semantics failures. Those unrelated user-owned/product-baseline failures were not changed. Therefore a literal all-app regression pass cannot be claimed.

## Non-interference audit

ARCore/session/renderer classes are referenced only by the AR module/AR destination. The adapter copies immutable data and does not mutate shared mesh state. No shared singleton retains an Activity or AR session. Existing startup and navigation require no camera permission. Static search found no plane/depth placement fallback.
