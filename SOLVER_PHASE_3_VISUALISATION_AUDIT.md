# Solver Phase 3 Visualisation Audit

## Scope and boundary

This audit covers reusable rendering and mathematics infrastructure for the
keyboard-only, fully offline Solver. Phase 3 does not connect Math Camera, OCR,
gallery input, remote services, or runtime downloads.

The Solver must produce declarative mathematical data. It must not mutate graph,
2D, 3D, or lesson workspace state. The boundary is:

```text
SolverSolution
  -> SolverVisualisationGenerator
  -> VisualisationSpec
  -> SolverVisualisationRenderer
  -> Solver-owned Compose Canvas
```

## Existing graph engine

- `core/AdvancedGraphing.kt` provides deterministic expression classification,
  finite-domain sampling, segmented curves, sequences, vector fields and
  inequalities.
- `core/GraphProduction.kt` provides typed graph rows, deterministic points of
  interest, transformations, summaries and keyboard navigation.
- `core/WorldClassMathSuite.kt` provides numerical differential and integral
  insights.
- `GraphTrigRendering.kt` and `Graph2DWorkspaceUi.kt` are complete workspace UI
  implementations. They are not appropriate dependencies for an embedded Solver
  explanation because they own interaction and workspace state.

Phase 3 reuses the public `ExpressionEngine` evaluation contract. It does not
call or alter workspace composables.

## Coordinate and 2D rendering

- Coordinate transforms and graph sampling are available in the core package.
- `Geometry2DRendering.kt` contains workspace-owned `DrawScope` shape routines.
- Existing games and proof labs contain useful visual precedents for balance
  scales, tiles and number lines, but those routines are private to their
  features.

The Solver uses a small independent Canvas adapter driven by typed specs. It
does not duplicate the full geometry workspace.

## 3D renderer

- `Spatial3DRendering.kt` and the `arengine` module own spatial rendering and
  lifecycle.
- Loading the 3D engine for number lines, fractions, calculus curves or Argand
  diagrams would add unnecessary memory and lifecycle cost.

No Phase 3 Solver visual requires the 3D engine.

## Animation framework

The app uses Compose animation APIs and explicit interaction timelines. Solver
playback is bounded, user-controlled and step-based. It:

- advances once every 1.2 seconds only after Play is selected;
- stops at the last visual state;
- pauses on direct navigation or full-screen expansion;
- changes to one-step advancement when Android animator scale is zero.

There is no uncontrolled continuous animation.

## Charting libraries

No third-party charting library is configured. Compose Canvas and the existing
expression evaluator are sufficient. Phase 3 adds no dependency.

## Formula and LaTeX rendering

`FormulaLatexRenderer.kt` provides app-specific formula presentation. Solver
steps currently use the established text/monospace expression rendering. The
visual specification stores mathematical structure and text alternatives, not
screenshots or pre-rendered formula images.

## Reusable public APIs

- `ExpressionEngine.compile(...).eval(...)`
- `AdvancedScientificCalculator.evaluate(...)`
- `MathSolverTutor` and the existing deterministic Solver kernels
- `CasDomainBranchAnalyzer`
- Compose `Canvas` and accessibility semantics
- Existing app colors and `GlowButton`

## Components not modified

- Math Camera, OCR and image-to-math
- Graph workspace internals
- 2D and 3D workspace behavior
- Existing keyboard behavior
- Existing navigation routes
- Existing saved-data formats
- Existing theme selection

## Performance constraints

- Sample counts are bounded to 129 curve points and 24 integration rectangles.
- Non-finite graph values are omitted and recorded as discontinuities.
- Visual specs are cached in a synchronized 48-entry LRU cache.
- Canvas rendering uses primitive paths and shapes without bitmap allocation.
- Full-screen mode reuses the same specification.
- The renderer does not retain external rendering resources.

## Proposed and implemented adapter

`SolverVisualisationRenderer` accepts a `VisualisationSpec` and returns a
`SolverVisualisation`. The default implementation is deliberately thin because
the specification is already render-ready and independently testable.

