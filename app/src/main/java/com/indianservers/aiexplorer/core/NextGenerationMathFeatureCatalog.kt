package com.indianservers.aiexplorer.core

enum class NextMathDomain { Graph, Geometry2D, Spatial3D, Cas }
enum class NextMathInteraction { Drag, Handle, Probe, Paint, Timeline, Inspector, Generator, ImportExport }
data class NextMathFeature(val id:String,val title:String,val domain:NextMathDomain,val interactions:Set<NextMathInteraction>,val engine:String,val description:String)

/** Production registry used by the unified studio, accessibility tree and feature search. */
object NextGenerationMathFeatureCatalog {
    val features = listOf(
        f("curve-sculpt","Direct curve sculpting",NextMathDomain.Graph,setOf(NextMathInteraction.Drag),"CurveSculptEngine","Drag locally, preview inferred parameter changes, then commit."),
        f("drag-fit","Drag-to-fit modelling",NextMathDomain.Graph,setOf(NextMathInteraction.Drag,NextMathInteraction.Inspector),"DragToFitEngine","Move a model and recompute coefficients and residuals."),
        f("inequality-handles","Editable inequality boundaries",NextMathDomain.Graph,setOf(NextMathInteraction.Handle),"EditableInequality","Drag and toggle interval boundaries."),
        f("phase-portrait","Interactive ODE phase portraits",NextMathDomain.Graph,setOf(NextMathInteraction.Probe,NextMathInteraction.Drag),"OdePhasePortraitEngine","Seed and move verified RK4 solution trajectories."),
        f("vector-probe","Vector-field probes",NextMathDomain.Graph,setOf(NextMathInteraction.Probe),"VectorFieldProbeEngine","Inspect vector, magnitude, divergence, curl and flow."),
        f("complex-explorer","Complex-function explorer",NextMathDomain.Graph,setOf(NextMathInteraction.Probe),"ComplexFunctionExplorer","Domain colouring values, zeros, poles and conformal grids."),
        f("fourier","Linked Fourier workspace",NextMathDomain.Graph,setOf(NextMathInteraction.Handle),"LinkedFourierEngine","Edit signal samples and recompute spectrum and error."),
        f("graph-uncertainty","Uncertainty propagation",NextMathDomain.Graph,setOf(NextMathInteraction.Inspector),"GraphUncertaintyEngine","Propagate parameter uncertainty into confidence bands."),
        f("discontinuities","Discontinuity inspector",NextMathDomain.Graph,setOf(NextMathInteraction.Probe),"GraphDiscontinuityInspector","Inspect holes, jumps, asymptotes and branch boundaries."),
        f("large-data","Large-data LOD pipeline",NextMathDomain.Graph,setOf(NextMathInteraction.ImportExport),"LargeGraphDataPipeline","Stream min/max-preserving pixel buckets."),
        f("animation","Animation timeline",NextMathDomain.Graph,setOf(NextMathInteraction.Timeline),"GraphAnimationTimeline","Keyframes, easing, synchronized values and events."),
        f("sonification","Multigraph sonification",NextMathDomain.Graph,setOf(NextMathInteraction.Probe),"MultigraphSonificationEngine","Independent voices, stereo placement, rhythm and haptics."),
        f("construction-proof","Construction-to-proof",NextMathDomain.Geometry2D,setOf(NextMathInteraction.Inspector),"ConstructionProofEngine","Verify claims against construction dependencies."),
        f("degrees-freedom","Degrees of freedom",NextMathDomain.Geometry2D,setOf(NextMathInteraction.Inspector),"GeometryFreedomEngine","Show free dimensions, directions and restrictions."),
        f("intent-snap","Intent-aware snapping",NextMathDomain.Geometry2D,setOf(NextMathInteraction.Drag),"IntentAwareGeometrySnap","Rank midpoint, incidence and geometric intent previews."),
        f("exact-predicates","Exact predicates",NextMathDomain.Geometry2D,setOf(NextMathInteraction.Inspector),"ExactGeometryPredicates","High-precision orientation, incidence and tangency."),
        f("invariants","Invariant discovery",NextMathDomain.Geometry2D,setOf(NextMathInteraction.Inspector),"GeometryInvariantDiscovery","Detect stable measurements over dragged frames."),
        f("geometry-models","Non-Euclidean canvases",NextMathDomain.Geometry2D,setOf(NextMathInteraction.Drag),"NonEuclideanGeometryEngine","Euclidean, spherical, hyperbolic, projective and affine models."),
        f("matrix-link","Coordinate and matrix link",NextMathDomain.Geometry2D,setOf(NextMathInteraction.Handle),"CoordinateMatrixLinkEngine","Synchronize handles, coordinates and matrices."),
        f("transform-lab","Transformation composition",NextMathDomain.Geometry2D,setOf(NextMathInteraction.Timeline),"TransformationCompositionLab","Reorder transforms and expose non-commutativity."),
        f("construction-family","Parametric construction families",NextMathDomain.Geometry2D,setOf(NextMathInteraction.Handle),"ParametricConstructionFamilyEngine","Sweep constructions into envelopes and loci."),
        f("geometry-optimize","Geometric optimization",NextMathDomain.Geometry2D,setOf(NextMathInteraction.Drag),"GeometryOptimizationEngine","Optimize objectives over constrained constructions."),
        f("hybrid-solid","Hybrid exact solids",NextMathDomain.Spatial3D,setOf(NextMathInteraction.Generator),"HybridExactSolidEngine","Retain an exact CSG tree beside display meshes."),
        f("adaptive-surface","Adaptive implicit surfaces",NextMathDomain.Spatial3D,setOf(NextMathInteraction.Probe),"AdaptiveImplicitSurfaceEngine","Refine near crossings, curvature and focus."),
        f("surface-singularity","Surface singularities",NextMathDomain.Spatial3D,setOf(NextMathInteraction.Probe),"SurfaceSingularityAnalyzer","Identify undefined and gradient-degenerate points."),
        f("mechanisms","Mechanical constraints",NextMathDomain.Spatial3D,setOf(NextMathInteraction.Handle),"MechanicalConstraintEngine","Hinges, sliders, gears, rigid links and collisions."),
        f("clipping","Multi-plane clipping",NextMathDomain.Spatial3D,setOf(NextMathInteraction.Handle),"ClippingVolume","Editable boxes, wedges, slabs and plane sets."),
        f("spatial-integrals","Surface and volume integrals",NextMathDomain.Spatial3D,setOf(NextMathInteraction.Paint),"InteractiveSpatialIntegralEngine","Integrate over painted or bounded regions."),
        f("flux-flow","Flux and flow",NextMathDomain.Spatial3D,setOf(NextMathInteraction.Probe),"SpatialFluxFlowEngine","Trace streamlines and divergence samples."),
        f("mesh-topology","Topology inspector",NextMathDomain.Spatial3D,setOf(NextMathInteraction.Inspector),"MeshTopologyInspector","Components, boundaries, Euler characteristic, genus and manifold checks."),
        f("fabrication","Fabrication readiness",NextMathDomain.Spatial3D,setOf(NextMathInteraction.Inspector),"FabricationReadinessEngine","Walls, open shells, manifold faults and support regions."),
        f("ar-calibration","Calibrated AR",NextMathDomain.Spatial3D,setOf(NextMathInteraction.Handle),"CalibratedArWorkspace","Fit mathematical units to physical anchors with error."),
        f("equation-drag","Equation dragging",NextMathDomain.Cas,setOf(NextMathInteraction.Drag),"EquationDragEngine","Preview legal term, factor, power and side operations."),
        f("dimensions","Typed dimensions",NextMathDomain.Cas,setOf(NextMathInteraction.Inspector),"DimensionalCasEngine","Attach dimension vectors and reject invalid algebra."),
        f("contracts","Verified transformation contracts",NextMathDomain.Cas,setOf(NextMathInteraction.Inspector),"VerifiedTransformationContractEngine","Retain conditions and generate counterexamples."),
        f("cas-branches","Interactive branch analysis",NextMathDomain.Cas,setOf(NextMathInteraction.Probe),"InteractiveCasBranchAnalyzer","Real domains, complex cuts and principal values."),
        f("elimination","Polynomial elimination",NextMathDomain.Cas,setOf(NextMathInteraction.Timeline),"PolynomialEliminationExplorer","Expose Buchberger pairs, S-polynomials and remainders."),
        f("certified-numeric","Certified numeric fallback",NextMathDomain.Cas,setOf(NextMathInteraction.Inspector),"CertifiedSymbolicNumericEngine","Return intervals with explicit width and iterations."),
        f("assumption-graph","Assumption inference graph",NextMathDomain.Cas,setOf(NextMathInteraction.Inspector),"CasAssumptionInferenceEngine","Separate explicit, derived, temporary and contradictory facts."),
        f("differential-studio","Differential solution studio",NextMathDomain.Cas,setOf(NextMathInteraction.Handle),"DifferentialEquationSolutionStudio","Link initial-condition handles, trajectories and verification."),
        f("cas-construction","CAS-to-construction",NextMathDomain.Cas,setOf(NextMathInteraction.Generator),"CasToConstructionGenerator","Generate linked graph, geometry and vector commands."),
        f("expression-compare","Expression version comparison",NextMathDomain.Cas,setOf(NextMathInteraction.Inspector),"ExpressionVersionComparator","Structural diffs plus equivalence contracts."),
    )
    init { require(features.size==42&&features.map{it.id}.distinct().size==features.size) }
    fun domain(domain:NextMathDomain)=features.filter{it.domain==domain}
    fun search(query:String)=features.filter{it.title.contains(query,true)||it.description.contains(query,true)||it.engine.contains(query,true)}
    private fun f(id:String,title:String,domain:NextMathDomain,interactions:Set<NextMathInteraction>,engine:String,description:String)=NextMathFeature(id,title,domain,interactions,engine,description)
}
