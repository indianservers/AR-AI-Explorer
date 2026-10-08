package com.indianservers.aiexplorer

import androidx.lifecycle.SavedStateHandle
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.spatial.*
import com.indianservers.aiexplorer.workspace.*
import org.junit.Assert.*
import org.junit.Test

class ArCadPhaseThreeTest {
    private fun scene(vm:ExplorerViewModel)=ArCadSceneCompiler().build(SpatialRenderScene("test",emptyList()),vm.state).withArGraphObjects(vm.state,ArGraphGeometryCache())
    private fun create(vm:ExplorerViewModel,id:String,type:ArCadType)=vm.upsertArCadNode(ArCadNode(id,type,arCadDefaults(type)))
    @Test fun analyticDerivativesAndDirectionalDerivativeAreLive() {
        val vm=ExplorerViewModel(SavedStateHandle()); create(vm,"surface",ArCadType.FunctionSurface)
        vm.executeArCadValues("Analysis") { it+mapOf("arAnalysis.surface" to "surface","arAnalysis.x" to "1","arAnalysis.y" to "2","arAnalysis.dx" to "3","arAnalysis.dy" to "4","arAnalysis.gradient" to "true","arAnalysis.tangent" to "true") }
        val result=ArCadAnalysis.enrich(scene(vm),vm.state)
        assertEquals(5.0,result.values.getValue("f(x,y)").toDouble(),1e-8)
        assertEquals(4.4,result.values.getValue("Directional derivative").toDouble(),1e-7)
        assertTrue(result.scene.primitives.any { it.id=="analysis-tangent" }); assertTrue(result.scene.primitives.any { it.id=="analysis-gradient" })
        vm.previewArCadParameters("surface",vm.state.arCadNodes().getValue("surface").parameters+("parameter.a" to "2")); vm.endArGraphObjectGesture(false)
        assertEquals(10.0,ArCadAnalysis.enrich(scene(vm),vm.state).values.getValue("f(x,y)").toDouble(),1e-8)
    }
    @Test fun worldTangentPassesThroughTransformedPoint() {
        val vm=ExplorerViewModel(SavedStateHandle()); create(vm,"s",ArCadType.FunctionSurface)
        vm.updateArGraphObject("s","Pose") { it.copy(position=Vec3(2.0,3.0,4.0),rotation=Vec3(20.0,30.0,40.0),axisScale=Vec3(2.0,1.0,3.0)) }
        vm.executeArCadValues("Analysis") { it+mapOf("arAnalysis.surface" to "s","arAnalysis.tangent" to "true") }
        val result=ArCadAnalysis.enrich(scene(vm),vm.state)
        val point=result.scene.primitives.first { it.id=="analysis-P" }.geometry.vertices.single()
        val plane=result.scene.primitives.first { it.id=="analysis-tangent" }.geometry.vertices
        val n=AnalyticGeometry3D.cross(plane[1]-plane[0],plane[2]-plane[0]).normalized()
        assertEquals(0.0,(point-plane[0]).dot(n),1e-8)
    }
    @Test fun editedSurfaceDoesNotClaimAnalyticDerivative() {
        val vm=ExplorerViewModel(SavedStateHandle()); create(vm,"s",ArCadType.FunctionSurface)
        val original=scene(vm).primitives.single().geometry
        vm.commitArCadGeometry("s",original,"Edit mesh")
        vm.executeArCadValues("Analysis") { it+("arAnalysis.surface" to "s") }
        assertTrue(ArCadAnalysis.enrich(scene(vm),vm.state).values.getValue("Surface analysis").contains("Rebuild"))
    }
    @Test fun slicesRespectObjectTransforms() {
        val vm=ExplorerViewModel(SavedStateHandle()); create(vm,"sphere",ArCadType.Sphere); create(vm,"plane",ArCadType.Plane)
        vm.updateArGraphObject("sphere","Move sphere") { it.copy(position=Vec3(0.0,0.0,3.0)) }
        vm.executeArCadValues("Slice") { it+("arAnalysis.slice" to "plane") }
        assertFalse(ArCadAnalysis.enrich(scene(vm),vm.state).scene.primitives.any { it.id.startsWith("section-sphere") })
        vm.updateArGraphObject("plane","Move slice") { it.copy(position=Vec3(0.0,0.0,3.0)) }
        assertTrue(ArCadAnalysis.enrich(scene(vm),vm.state).scene.primitives.any { it.id.startsWith("section-sphere") })
    }
    @Test fun conicClassificationUsesQuadraticDiscriminant() {
        assertEquals("Circle",ArCadAnalysis.conic(Vec3(0.0,1.0,0.0),.5))
        assertEquals("Ellipse",ArCadAnalysis.conic(Vec3(.5,1.0,0.0),.5))
        assertEquals("Parabola",ArCadAnalysis.conic(Vec3(1.0,.5,0.0),.5))
        assertEquals("Hyperbola",ArCadAnalysis.conic(Vec3(1.0,0.0,0.0),.5))
    }
    @Test fun implicitSphereIsFiniteAndApproximatesEquation() {
        val vm=ExplorerViewModel(SavedStateHandle()); create(vm,"implicit",ArCadType.ImplicitSurface)
        val g=scene(vm).primitives.single().geometry
        assertTrue(g.triangles.isNotEmpty()); ArCadTopology.validate(g)
        assertTrue(g.vertices.all { kotlin.math.abs(it.dot(it)-1)<.06 })
    }
    @Test fun vectorFieldArrowsUseExpressionsAndNormalization() {
        val vm=ExplorerViewModel(SavedStateHandle()); create(vm,"field",ArCadType.VectorField)
        val g=scene(vm).primitives.single().geometry
        val p=g.vertices[0]; val arrow=g.vertices[1]-p
        assertEquals(.3,arrow.magnitude(),1e-8)
        assertEquals(0.0,arrow.dot(Vec3(p.x,p.y,0.0)),1e-8)
        assertTrue(g.vertices.size<=4000)
    }
    @Test fun lodOnlyReducesVisualCopy() {
        val vm=ExplorerViewModel(SavedStateHandle()); create(vm,"s",ArCadType.FunctionSurface)
        val source=scene(vm); val g=source.primitives.single().geometry
        val reduced=ArCadRenderLod().scene(source,0).primitives.single().geometry
        assertTrue(reduced.vertices.size<g.vertices.size); ArCadTopology.validate(reduced)
        assertSame(g,source.primitives.single().geometry)
    }
    @Test fun timelineJumpRetainsRedoAndDivergenceClearsFuture() {
        val vm=ExplorerViewModel(SavedStateHandle()); create(vm,"p",ArCadType.Point); create(vm,"sphere",ArCadType.Sphere)
        vm.jumpArHistory(0); assertTrue(vm.state.arCadNodes().isEmpty()); assertEquals(2,vm.arHistoryTimeline.size)
        vm.jumpArHistory(2); assertEquals(2,vm.state.arCadNodes().size)
        vm.jumpArHistory(1); create(vm,"plane",ArCadType.Plane); assertFalse(vm.canRedo); assertEquals(2,vm.arHistoryTimeline.size)
    }
    @Test fun advancedMeasurementsUseRealPlanesAndLines() {
        val vm=ExplorerViewModel(SavedStateHandle()); create(vm,"p",ArCadType.Point); create(vm,"plane",ArCadType.Plane); create(vm,"line",ArCadType.Line)
        vm.updateArGraphObject("p","Move") { it.copy(position=Vec3(0.0,0.0,3.0)) }
        val s=scene(vm)
        assertEquals(3.0,ArCadMeasurement(ArCadMeasureKind.PointPlaneDistance,listOf("p","plane")).value(s),1e-8)
        assertEquals(0.0,ArCadMeasurement(ArCadMeasureKind.LinePlaneAngle,listOf("line","plane")).value(s),1e-8)
    }
    @Test fun savedAnalysisAndNodeSettingsSurviveProjectCodec() {
        val vm=ExplorerViewModel(SavedStateHandle()); create(vm,"f",ArCadType.VectorField)
        vm.executeArCadValues("Settings") { it+mapOf("arAnalysis.x" to "pi/2","arAnalysis.quality" to "LOW") }
        val recovered=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(vm.state)).state!!
        assertEquals(vm.state.labSessionValues,recovered.labSessionValues)
    }
    @Test fun nonuniformConeUsesWorldPlaneMetric() {
        assertEquals("Ellipse",ArCadAnalysis.transformedConic(Vec3(0.0,1.0,0.0),com.indianservers.aiexplorer.arengine.contract.ArLocalTransform(axisScale=com.indianservers.aiexplorer.arengine.contract.ArVector3(2.0,1.0,1.0)),.5))
    }
    @Test fun scalarSliceComputesRangeAndActualContour() {
        val result=ArScalarSlice.build("x^2+y^2+z^2",0.0,2.0,contour=1.0)
        assertEquals(0.0,result.minimum,1e-10); assertEquals(8.0,result.maximum,1e-10)
        val contour=result.primitives.first { it.id=="scalar-contour" }
        assertTrue(contour.geometry.vertices.all { kotlin.math.abs(it.x*it.x+it.y*it.y-1)<.03 })
    }
    @Test fun labelsStayWithinScreenAndAvoidOverlap() {
        val boxes=arCadLabelLayout((0..20).map { ArCadLabel("Object $it",150f,200f) },300f,500f)
        assertTrue(boxes.isNotEmpty()); assertTrue(boxes.size<=5)
        boxes.forEach { assertTrue(it.left>=0 && it.right<=300 && it.top>=64 && it.bottom<=500) }
        boxes.forEachIndexed { i,a -> boxes.drop(i+1).forEach { b -> assertFalse(a.left<b.right && a.right>b.left && a.top<b.bottom && a.bottom>b.top) } }
    }
    @Test fun guidedConicsActivityAddsPlaneAndConeWithSingleUndo() {
        val vm=ExplorerViewModel(SavedStateHandle()); launchArCadActivity(vm,ArCadActivity.Conics)
        assertEquals(setOf(ArCadType.Plane,ArCadType.Cone),vm.state.arCadNodes().values.map { it.type }.toSet())
        assertTrue(ArCadAnalysis.enrich(scene(vm),vm.state).values.values.any { it=="Circle" })
        vm.undo(); assertTrue(vm.state.arCadNodes().isEmpty())
    }

    @Test fun planeIntersectionSnappingUsesFiniteVisiblePatches() {
        val vm=ExplorerViewModel(SavedStateHandle())
        create(vm,"a",ArCadType.Plane); create(vm,"b",ArCadType.Plane)
        vm.updateArGraphObject("b","Rotate plane") { it.copy(rotation=Vec3(90.0,0.0,0.0)) }
        val geometries=scene(vm).primitives.map { arCadWorldGeometry(it.geometry,vm.state.arGraphObject(it.id)) }
        val snap=ArCadSnapping.snap(Vec3(.37,.02,.02),geometries)
        assertEquals(ArCadSnapKind.Intersection,snap?.kind)
        assertEquals(0.0,snap!!.point.y,1e-8); assertEquals(0.0,snap.point.z,1e-8)
    }
    @Test fun restoreRequestsNewPlacementAndRetainsMathState() {
        val vm=ExplorerViewModel(SavedStateHandle()); create(vm,"s",ArCadType.Sphere)
        vm.transformSpatialPlacement("Anchor") { it.copy(anchorId="old-session-anchor",metersPerMathUnit=.25) }
        vm.saveWorkspace(); val saved=vm.savedWorkspaces.single(); vm.restoreArScene(saved)
        assertEquals("",vm.state.spatialPlacement.anchorId); assertEquals(.25,vm.state.spatialPlacement.metersPerMathUnit,0.0)
        assertEquals(saved.snapshot.labSessionValues,vm.state.labSessionValues)
    }

    @Test fun pickedWorldPointUpdatesLocalAnalysisCoordinates() {
        val vm=ExplorerViewModel(SavedStateHandle()); create(vm,"s",ArCadType.FunctionSurface)
        vm.updateArGraphObject("s","Move") { it.copy(position=Vec3(3.0,4.0,0.0),scale=2.0) }
        vm.executeArCadValues("Pick") { it+mapOf("arAnalysis.surface" to "s","arAnalysis.pickPoint" to "true") }
        assertTrue(vm.pickArSurfacePoint(com.indianservers.aiexplorer.arengine.interaction.ArPickHit("s",com.indianservers.aiexplorer.arengine.interaction.ArSubObjectKind.Whole,null,0.0,com.indianservers.aiexplorer.arengine.contract.ArVector3(5.0,8.0,10.0))))
        assertEquals(1.0,vm.state.labSessionValues.getValue("arAnalysis.x").toDouble(),1e-8)
        assertEquals(2.0,vm.state.labSessionValues.getValue("arAnalysis.y").toDouble(),1e-8)
    }

    @Test fun existingEquationEditorSurfaceCanBeAnalyzed() {
        val vm=ExplorerViewModel(SavedStateHandle()); vm.setSurfaceExpression("z=x^2+y^2")
        vm.executeArCadValues("Analyze native") { it+mapOf("arAnalysis.surface" to "surface-main","arAnalysis.x" to "1","arAnalysis.y" to "2") }
        val native=ArMathWorkspaceBridge.build(ArMathWorkspaceMode.Graph3D,vm.state).scene.withArGraphObjects(vm.state,ArGraphGeometryCache())
        assertEquals(5.0,ArCadAnalysis.enrich(native,vm.state).values.getValue("f(x,y)").toDouble(),1e-8)
    }

    @Test fun cornerDoesNotClaimAFalseTangentPlane() {
        val vm=ExplorerViewModel(SavedStateHandle()); vm.upsertArCadNode(ArCadNode("s",ArCadType.FunctionSurface,arCadDefaults(ArCadType.FunctionSurface)+("expressionZ" to "abs(x)+y^2")))
        vm.executeArCadValues("Analysis") { it+mapOf("arAnalysis.surface" to "s","arAnalysis.x" to "0","arAnalysis.y" to "0") }
        val result=ArCadAnalysis.enrich(scene(vm),vm.state)
        assertFalse(result.values.containsKey("Tangent plane")); assertTrue(result.values.getValue("Surface analysis").contains("derivative"))
    }

    @Test fun implicitEquationAcceptsBothSides() {
        val vm=ExplorerViewModel(SavedStateHandle()); vm.upsertArCadNode(ArCadNode("sphere",ArCadType.ImplicitSurface,arCadDefaults(ArCadType.ImplicitSurface)+("expressionF" to "x^2+y^2+z^2=1")))
        assertTrue(scene(vm).primitives.single().geometry.triangles.isNotEmpty())
    }

    @Test fun qualityReductionAlsoAppliesToLockedObjects() {
        val vm=ExplorerViewModel(SavedStateHandle()); create(vm,"s",ArCadType.FunctionSurface)
        vm.updateArGraphObject("s","Lock") { it.copy(locked=true) }
        val source=scene(vm); val reduced=ArCadRenderLod().scene(source,0)
        assertTrue(reduced.primitives.single().geometry.vertices.size<source.primitives.single().geometry.vertices.size)
        assertTrue(vm.state.arGraphObject("s").locked)
    }

    @Test fun torusAndAlgebraicImplicitMeshesSatisfyTheirEquationsNumerically() {
        val samples=listOf("(sqrt(x^2+z^2)-2)^2+y^2-.25" to .1,"x*y-z" to .04)
        samples.forEach { (expression,tolerance) ->
            val vm=ExplorerViewModel(SavedStateHandle())
            val parameters=arCadDefaults(ArCadType.ImplicitSurface)+mapOf("expressionF" to expression,"uMin" to "-3","uMax" to "3","vMin" to "-3","vMax" to "3","wMin" to "-3","wMax" to "3","samples" to "24")
            vm.upsertArCadNode(ArCadNode("surface",ArCadType.ImplicitSurface,parameters))
            val geometry=scene(vm).primitives.single().geometry; ArCadTopology.validate(geometry)
            val f=ExpressionEngine().compile(expression)
            assertTrue(geometry.vertices.all { kotlin.math.abs(f.eval(mapOf("x" to it.x,"y" to it.y,"z" to it.z)))<tolerance })
        }
    }

}
