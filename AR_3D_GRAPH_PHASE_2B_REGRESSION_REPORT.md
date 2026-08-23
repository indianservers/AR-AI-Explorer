# AR 3D Graph Phase 2B — Regression Report

## Existing-module result

The final app JVM suite passed 1,025/1,025 twice and exercises 2D/3D geometry, 2D/3D graph engines, Solver/CAS, state, navigation and file codecs. In the persistent emulator session, Home, Visual Workspaces, normal navigation and AR entry/exit remained functional. The five-workspace stress case completed 20 full navigation cycles.

The focused existing connected subset passed `sharedLauncherGeometryEntriesNavigateToRealWorkspaces` and `twentyCompleteFiveWorkspaceNavigationCyclesRemainStable`. Three tests failed:

- 2D Graph expected exactly one `Equations (5)` node but found none.
- Existing 3D Graph implicit/parametric test expected one `Implicit surface` node but persisted layers produced two.
- Existing 3D Graph explicit test expected one `Explicit surface` node but persisted layers produced two.

These are pre-existing/stale UI-test assumptions in a user-modified test file. They did not crash the app and were not changed during Phase 2B. The retained broader baseline was already red before Phase 2B (61 finalized tests, 34 existing failures; a prior retained run was 81 total, 65 failures). Therefore a green app-wide connected baseline cannot be claimed.

## Normal 3D Graph protection

The normal workspace does not request camera permission, start ARCore, use AR transforms or change its graph output. Thirteen protected engine/parser/renderer files remain byte-identical to the pre-Phase-2 backup. Protected `Graph3DScreen`, `Graph3DEquationPanel`, and `Graph3DPropertiesPanel` blocks retain hashes `F024AEDA…`, `A0FC8A39…`, and `CD9C82F6…`.

Static inspection found no AR placement use of `hitTest`, plane renderers, horizontal/vertical plane finding, `DepthPoint` or `InstantPlacementPoint`. Configuration remains `PlaneFindingMode.DISABLED`, `InstantPlacementMode.DISABLED`, and `DepthMode.DISABLED`.

## Phase 2B source changes

Production edits are limited to:

- `AR3DGraphViewModel.kt`: cancel generation and invalidate stale results when the screen exits.
- `AR3DGraphScreen.kt`: invoke the exit cleanup from composition disposal.
- `ARGraphCameraView.kt`: immediately release CPU/anchor references when disposed while retaining queued GL deletion.

Test additions expand placement rays, 50 anchor replacements, lifecycle/capability states, parity, cancellation, 20 reopen cycles and performance observations. No protected source, public API, default domain/resolution, parser, evaluator, mesh generator, shader or existing renderer was modified.

## Consecutive cycles

Two no-code-change final focused cycles passed all 1,069 JVM tests and both AR connected tests each. At the complete-regression level, both cycles are **blocked** by unavailable ARCore runtime coverage and pre-existing broad connected failures. This report does not relabel partial coverage as a complete pass.
