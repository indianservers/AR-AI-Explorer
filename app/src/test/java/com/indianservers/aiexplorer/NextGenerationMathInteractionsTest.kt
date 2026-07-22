package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class NextGenerationMathInteractionsTest {
    @Test fun catalogExposesEveryAttachedCapability() {
        assertEquals(42, NextGenerationMathFeatureCatalog.features.size)
        assertEquals(12, NextGenerationMathFeatureCatalog.domain(NextMathDomain.Graph).size)
        assertEquals(10, NextGenerationMathFeatureCatalog.domain(NextMathDomain.Geometry2D).size)
        assertEquals(10, NextGenerationMathFeatureCatalog.domain(NextMathDomain.Spatial3D).size)
        assertEquals(10, NextGenerationMathFeatureCatalog.domain(NextMathDomain.Cas).size)
        assertTrue(NextGenerationMathFeatureCatalog.features.all { it.engine.isNotBlank() && it.interactions.isNotEmpty() })
    }
    @Test fun graphInteractionsAreLiveAndNumericallyGrounded() {
        val sculpt = CurveSculptEngine().preview("a*x+b", mapOf("a" to 2.0, "b" to 1.0), 2.0, 7.0)
        assertNotNull(sculpt.preferred); assertEquals(7.0, sculpt.preferred!!.predictedY, 1e-6)
        val fit = DragToFitEngine().drag(listOf(Vec2(0.0,0.0),Vec2(1.0,1.0),Vec2(2.0,2.0),Vec2(3.0,3.0)), GraphRegressionKind.Linear, 2.0, 4.0)
        assertTrue(fit.coefficientChanges.isNotEmpty())
        val inequality=EditableInequality("x",0.0,2.0,true,false).dragUpper(3.0).toggleUpper();assertTrue(inequality.contains(3.0))
        val ode=OdePhasePortraitEngine.seed(Vec2(0.0,1.0),{_,y->y},-1.0..1.0);assertTrue(ode.points.size>20)
        val probe=VectorFieldProbeEngine.probe(Vec2(1.0,0.0),field={x,y->Vec2(-y,x)});assertEquals(0.0,probe.divergence,1e-6);assertEquals(2.0,probe.curl,1e-5)
        val complex=ComplexFunctionExplorer.probe(ComplexValue(1.0,1.0)){z->ComplexValue(z.re*z.re-z.im*z.im,2*z.re*z.im)};assertEquals(2.0,complex.output.im,1e-9)
        val samples=(0 until 64).map{sin(2*PI*it/64)};val fourier=LinkedFourierEngine.analyse(samples,4);assertTrue(fourier.rmse<1e-9)
        val band=GraphUncertaintyEngine.propagate("a*x",mapOf("a" to 2.0),mapOf("a" to .1),listOf(2.0)).single();assertTrue(band.upper>band.estimate)
        assertTrue(GraphDiscontinuityInspector.inspect({x->1/x},-1.0..1.0,400).isNotEmpty())
        assertTrue(LargeGraphDataPipeline.aggregate((0..10000).map{Vec2(it.toDouble(),sin(it.toDouble()))},100).size<=101)
        val timeline=GraphAnimationTimeline(listOf(GraphKeyframe(0.0,mapOf("a" to 0.0)),GraphKeyframe(1.0,mapOf("a" to 10.0),GraphEasing.EaseInOut)));assertEquals(5.0,timeline.frame(.5).values.getValue("a"),1e-9)
        assertEquals(2,MultigraphSonificationEngine.voices(mapOf("f" to 1.0,"g" to -1.0),0.0,-2.0..2.0).size)
    }

    @Test fun geometryInteractionsExposeProofFreedomModelsAndOptimization() {
        val proof=ConstructionProofEngine.build(listOf(ProtocolStep("A","Point A",emptyList(),"Given"),ProtocolStep("B","Point B",listOf("A"),"Depends on A")));assertTrue(proof.valid)
        val freedom=GeometryFreedomEngine.analyse("A",listOf(Vec2(1.0,0.0)),listOf(GeometryConstraint.FixedLength("c","A","B",2.0)));assertEquals(1,freedom.freeDimensions)
        val snaps=IntentAwareGeometrySnap.infer(Vec2(1.01,0.0),emptyMap(),mapOf("AB" to (Vec2(0.0,0.0) to Vec2(2.0,0.0))));assertTrue(snaps.any{it.intent==GeometryIntent.Midpoint})
        assertEquals(ExactOrientation.CounterClockwise,ExactGeometryPredicates.orientation(Vec2(0.0,0.0),Vec2(1.0,0.0),Vec2(0.0,1.0)))
        assertTrue(GeometryInvariantDiscovery.discover(listOf(mapOf("length" to 2.0),mapOf("length" to 2.0+1e-10))).single().stable)
        val quarter=NonEuclideanGeometryEngine.distance(GeometryCanvasModel.Spherical,ModelPoint(listOf(1.0,0.0,0.0)),ModelPoint(listOf(0.0,1.0,0.0)));assertEquals(PI/2,quarter,1e-9)
        assertEquals(Vec2(2.0,3.0),Matrix3.translation(2.0,3.0).apply(Vec2(0.0,0.0)))
        val composition=TransformationCompositionLab.compose(listOf(Matrix3.translation(1.0,0.0),Matrix3.rotation(PI/2)),listOf(Vec2(0.0,0.0)));assertFalse(composition.commutesWithReverse)
        val family=ParametricConstructionFamilyEngine.sweep(0.0..1.0,10){t->mapOf("P" to Vec2(t,t*t))};assertEquals(11,family.frames.size)
        val optimum=GeometryOptimizationEngine.optimize(0.0..4.0,400,construction={x->mapOf("P" to Vec2(x,4-x))},objective={p->p.getValue("P").x*p.getValue("P").y});assertEquals(2.0,optimum.parameter,.02)
    }

    @Test fun spatialInteractionsCoverExactAnalysisFabricationAndCalibration() {
        val cube=Solid(SolidType.Cube,width=2.0);val hybrid=HybridExactSolidEngine.primitive(cube);assertTrue(hybrid.closed)
        val combined=HybridExactSolidEngine.combine(hybrid,HybridExactSolidEngine.primitive(cube.copy(position=Vec3(1.0,0.0,0.0))),BooleanMeshOperation.Union);assertTrue(combined.displayMesh.faces.isNotEmpty())
        val adaptive=AdaptiveImplicitSurfaceEngine.sample({x,y,z->x*x+y*y+z*z-1},SpatialBounds(Vec3(-1.5,-1.5,-1.5),Vec3(1.5,1.5,1.5)),base=5,maxDepth=2,focus=Vec3(1.0,0.0,0.0));assertTrue(adaptive.vertices.isNotEmpty())
        assertEquals(SurfaceSingularityKind.DegenerateGradient,SurfaceSingularityAnalyzer.analyse({x,y,z->x*x+y*y+z*z},listOf(Vec3(0.0,0.0,0.0))).single().kind)
        val mechanism=MechanicalConstraintEngine.solve(listOf(MechanicalJoint("gear",MechanicalJointKind.Gear,0,1,ratio=2.0)),mapOf("gear" to 3.0));assertEquals(6.0,mechanism.values.getValue("gear"),0.0)
        assertTrue(ClippingVolumeFactory.box(Vec3(-1.0,-1.0,-1.0),Vec3(1.0,1.0,1.0)).contains(Vec3(0.0,0.0,0.0)))
        val integral=InteractiveSpatialIntegralEngine.volume(SpatialBounds(Vec3(-1.0,-1.0,-1.0),Vec3(1.0,1.0,1.0)),12,{true});assertEquals(8.0,integral.estimate,1e-8)
        assertTrue(SpatialFluxFlowEngine.trace(Vec3(1.0,0.0,0.0),{p->Vec3(-p.y,p.x,0.0)},count=30).points.size>20)
        val topology=MeshTopologyInspector.inspect(hybrid.displayMesh);assertEquals(2,topology.eulerCharacteristic);assertTrue(topology.manifold)
        assertTrue(FabricationReadinessEngine.inspect(hybrid.displayMesh).dimensions.x>0)
        val ar=CalibratedArWorkspace.calibrate(listOf(Vec3(0.0,0.0,0.0),Vec3(1.0,0.0,0.0)),listOf(Vec3(1.0,2.0,3.0),Vec3(1.5,2.0,3.0)));assertEquals(.5,ar.scaleMetresPerUnit,1e-9)
    }

    @Test fun casInteractionsRemainVerifiableAndConstructionLinked() {
        val drag=EquationDragEngine.preview("x+2=5","2",EquationDragAction.MoveAddendAcrossEquality);assertTrue(drag.legal);assertTrue(drag.after.contains("- (2)"))
        val length=TypedQuantity(2.0,DimensionVector.Length,"m");assertEquals(4.0,DimensionalCasEngine.add(length,length).value,0.0)
        assertFalse(VerifiedTransformationContractEngine.verify("x","x+1",setOf("x")).valid)
        assertTrue(InteractiveCasBranchAnalyzer.analyse("sqrt(x)+log(x)").complexCuts.isNotEmpty())
        val x=Monomial(listOf(1,0));val y=Monomial(listOf(0,1));val one=Monomial(listOf(0,0));val p=MultiPolynomial(mapOf(x to 1.0,y to -1.0),listOf("x","y"));val q=MultiPolynomial(mapOf(y to 1.0,one to -1.0),listOf("x","y"));assertTrue(PolynomialEliminationExplorer.groebner(listOf(p,q)).basis.size>=2)
        val interval=CertifiedSymbolicNumericEngine.root({v->v*v-2},1.0..2.0);assertTrue(interval.width<1e-8)
        assertFalse(CasAssumptionInferenceEngine.build(listOf("x > 0","x <= 0")).consistent)
        assertTrue(DifferentialEquationSolutionStudio.firstOrder("y'=y",{_,v->v},Vec2(0.0,1.0),0.0..1.0).trajectory!!.points.size>2)
        assertTrue(CasToConstructionGenerator.generate("A=(1,2)").commands.single().startsWith("point2d"))
        val comparison=ExpressionVersionComparator.compare("x+x","2*x",setOf("x"));assertTrue(comparison.equivalent);assertTrue(comparison.tokens.any{it.kind!=ExpressionDiffKind.Equal})
    }
}
