# Next-generation Math Interactions

All 42 capabilities in the attached Graphing, 2D Geometry, 3D Mathematics and CAS specification are implemented as deterministic interaction engines and registered in Unified Math Studio's **Advanced labs** view.

## Graphing (12)

`NextGenerationGraphInteractions.kt` provides curve-sculpt previews and commits, drag-to-fit regression, inequality handles, RK4 phase portraits, vector probes and streamlines, complex probes/conformal grids, DFT reconstruction, first-order uncertainty propagation, discontinuity detection, min/max-preserving LOD aggregation, keyframe animation and multigraph audio/haptic mappings.

The linked Graph pane consumes the curve-sculpt preview directly: dragging a selected parameterized curve draws an anchor-to-target preview, infers the lowest-error parameter change and commits one semantic history operation when the gesture ends.

## 2D Geometry (10)

`NextGenerationGeometry2D.kt` provides dependency-verified construction proofs, degrees-of-freedom analysis, intent previews, high-precision predicates, invariant discovery, five geometry canvas models, linked homogeneous matrices, transformation composition, parametric families and constrained optimization sweeps.

## 3D Mathematics (10)

`NextGenerationSpatialMathematics.kt` provides exact CSG trees with display meshes, adaptive implicit sampling, singularity probes, mechanical constraints, clipping volumes, surface/volume integration, flow tracing, mesh topology, fabrication diagnostics and calibrated AR coordinate fitting.

## CAS (10)

`NextGenerationCasInteractions.kt` provides equation-drag previews, typed dimension vectors, transformation contracts with counterexamples, complex branch reports, staged Gröbner elimination, certified intervals, assumption graphs, linked differential-equation handles, CAS-to-construction plans and structural expression comparison.

## Verification and discovery

- `NextGenerationMathFeatureCatalog` is the canonical 42-feature registry used by the Advanced labs pane.
- `NextGenerationMathInteractionsTest` exercises every engine group and verifies the 12/10/10/10 feature split.
- All engines return preview or report objects suitable for canvas overlays, inspectors, undoable commits and accessible descriptions; calculations do not replace direct manipulation with static answers.
