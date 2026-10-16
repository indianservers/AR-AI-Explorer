package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.workspace.*
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import org.junit.Assert.*
import org.junit.Test

class RuhiSharedArchitectureTest {
    @Test fun spokenMathGraphConversationUsesOneSceneAndComputesVertex() {
        val a=RuhiStudioAssistant();val h=WorkspaceHistoryByModule();var state=WorkspaceState(module=MathModule.Graph2D,functions=emptyList());var c=RuhiSceneContext()
        fun run(s:String):RuhiExecutionResult { val r=a.execute(state,s,h,c,RuhiInputModality.VOICE_TRANSCRIPT);assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);state=r.state;c=r.context;return r }
        val first=run("Ruhi, draw y equals x squared.")
        assertEquals(RuhiEngine.GRAPH_2D,first.command!!.engine);assertEquals(1,first.command!!.schemaVersion)
        val id=state.functions.single().id
        run("Make it red.");assertEquals("#FF0000",state.functions.single().colorKey)
        run("Shift it three units right.")
        assertEquals(id,state.functions.single().id)
        val f=ExpressionEngine().compile(stripEquation(state.functions.single().expression));assertEquals(0.0,f.eval(mapOf("x" to 3.0)),1e-8);assertEquals(4.0,f.eval(mapOf("x" to 5.0)),1e-8)
        val vertex=run("Show the vertex.");assertTrue(vertex.message.contains("3.0, 0.0"));assertTrue(state.labSessionValues["ruhi.graph2d.vertex.$id"]!!.endsWith("3.0,0.0"))
        run("Undo.");assertFalse("ruhi.graph2d.vertex.$id" in state.labSessionValues)
        run("Undo.");assertEquals(0.0,ExpressionEngine().compile(stripEquation(state.functions.single().expression)).eval(mapOf("x" to 0.0)),1e-8)
    }
    @Test fun nativeTwoDControlPointsDriveDependentConstructionAndMeasurements() {
        val a=RuhiStudioAssistant();val h=WorkspaceHistoryByModule();var state=WorkspaceState(module=MathModule.Geometry2D);var c=RuhiSceneContext()
        fun run(s:String):RuhiExecutionResult { val r=a.execute(state,s,h,c);assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);state=r.state;c=r.context;return r }
        run("Create point A at (2, 3).");run("Create point B at (6, 5).");run("Draw a line segment connecting A and B.")
        assertEquals(listOf(0,1),state.shapes.single().pointIndices)
        assertTrue(run("Calculate the distance between A and B.").message.contains(kotlin.math.sqrt(20.0).toString()))
        run("Find the midpoint of segment AB.");assertEquals(Vec2(4.0,4.0),state.points.last())
        state=state.copy(points=state.points.mapIndexed{i,p->if(i==0) Vec2(0.0,1.0) else p}).recomputed()
        assertEquals(Vec2(3.0,3.0),state.points.last())
        assertTrue(run("Calculate the distance between A and B.").message.contains(kotlin.math.sqrt(52.0).toString()))
    }
    @Test fun geometryShapesUseDeterministicAreaAndCenterPreservingScale() {
        val a=RuhiStudioAssistant();val h=WorkspaceHistoryByModule();var state=WorkspaceState(module=MathModule.Geometry2D);var c=RuhiSceneContext()
        fun run(s:String):RuhiExecutionResult {val r=a.execute(state,s,h,c);assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);state=r.state;c=r.context;return r}
        run("Draw a circle centered at the origin with radius five.")
        val center=state.points.first();assertTrue(run("Calculate the area.").message.contains((25*Math.PI).toString()))
        run("Make it twice as large without moving its center.");assertEquals(center,state.points.first());assertEquals(10.0,state.points[0].distanceTo(state.points[1]),1e-8)
        run("Make it blue.");assertEquals("#0000FF",state.shapes.single().styleKey)
    }
    @Test fun realThreeDMeshVolumeAndAnimationAreDeterministicFunctionsOfTime() {
        var now=1000L;val a=RuhiStudioAssistant{now};val h=WorkspaceHistoryByModule();var state=WorkspaceState(module=MathModule.Geometry3D);var c=RuhiSceneContext()
        fun run(s:String):RuhiExecutionResult {val r=a.execute(state,s,h,c);assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);state=r.state;c=r.context;return r}
        val create=run("Ruhi, create a rotating 3D cone.")
        assertEquals(RuhiEngine.GEOMETRY_3D,create.command!!.engine);assertTrue((create.command!!.operation as RuhiOperation.CreateSolid).animation!=null)
        assertEquals(2*Math.PI/3,Geometry3D.measure(state.solids.single()).volume,1e-8)
        assertTrue(SolidMeshFactory.create(state.solids.single()).vertices.isNotEmpty())
        assertEquals(0.0,RuhiMotionEngine.solids(state,1000).single().rotation.y,1e-8)
        assertEquals(45.0,RuhiMotionEngine.solids(state,2500).single().rotation.y,1e-8)
        val protocol=h.forModule(MathModule.Geometry3D).protocol;RuhiMotionEngine.solids(state,7000);assertEquals(protocol,h.forModule(MathModule.Geometry3D).protocol)
        now=2500;run("Stop the rotation.");assertEquals(45.0,state.solids.single().rotation.y,1e-8);assertFalse(state.labSessionValues.keys.any{it.startsWith(RuhiMotionEngine.PREFIX)})
        run("Undo.");assertTrue(state.labSessionValues.keys.any{it.startsWith(RuhiMotionEngine.PREFIX)})
        val recovered=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(state)).state!!;assertEquals(state.solids,recovered.solids);assertEquals(state.labSessionValues,recovered.labSessionValues)
    }
    @Test fun allFourEnginesShareScopedHistoryWithoutOverwritingOtherScenes() {
        val a=RuhiStudioAssistant();val h=WorkspaceHistoryByModule();var s=WorkspaceState(module=MathModule.Graph2D,functions=emptyList(),surfaceLayers=emptyList());var c=RuhiSceneContext()
        for(text in listOf("In 2D graph, plot y = x^2.","In 3D graph, plot z = x+y.","In 2D geometry, create a square with side length four.","In 3D geometry, create a sphere with radius three.")) {
            val r=a.execute(s,text,h,c);assertEquals(text+": "+r.message,GraphCommandStatus.APPLIED,r.status);s=r.state;c=r.context
        }
        assertEquals(1,s.functions.size);assertEquals(1,s.surfaceLayers.size);assertEquals(1,s.shapes.size);assertEquals(1,s.solids.size)
        val undone=a.execute(s,"Undo.",h,c);assertEquals(GraphCommandStatus.APPLIED,undone.status);assertTrue(undone.state.solids.isEmpty());assertEquals(s.functions,undone.state.functions);assertEquals(s.shapes,undone.state.shapes);assertEquals(s.surfaceLayers,undone.state.surfaceLayers)
        val g=a.execute(undone.state,"In 2D geometry, undo.",h,undone.context);assertEquals(GraphCommandStatus.APPLIED,g.status);assertTrue(g.state.shapes.isEmpty());assertEquals(s.functions,g.state.functions)
    }
    @Test fun ambiguousInvalidAndOutOfDomainInputNeverMutatesScene() {
        val a=RuhiStudioAssistant();val h=WorkspaceHistoryByModule();val start=WorkspaceState(module=MathModule.Geometry3D)
        for(text in listOf("Create a cylinder with radius -2 and height 4.","Create a cube with side length 0.","Move it 999999 units right.")) {
            val r=a.execute(start,text,h);assertEquals(text,GraphCommandStatus.INVALID,r.status);assertEquals(start,r.state);assertFalse(h.forModule(start.module).canUndo)
        }
        assertEquals(GraphCommandStatus.CLARIFICATION,a.execute(start,"Rotate the selected object.",h).status)
        assertFalse(RuhiMathLanguage.isStudioInstruction("Teach me quadratic equations"));assertFalse(RuhiMathLanguage.isStudioInstruction("Explain quadratic equations"));assertFalse(RuhiMathLanguage.isStudioInstruction("Explain this quadratic equation"))
        assertEquals(GraphCommandStatus.CLARIFICATION,a.execute(start,"Do not delete anything.",h).status)
        assertThrows(IllegalArgumentException::class.java){RuhiCommand(2,RuhiEngine.GRAPH_2D,RuhiOperation.ShowVertex)}
    }
    @Test fun namedTargetsNeverSilentlyEditAnUnrelatedSelectedSolid() {
        val a=RuhiStudioAssistant();val h=WorkspaceHistoryByModule();var state=WorkspaceState(module=MathModule.Geometry3D);var c=RuhiSceneContext()
        for(text in listOf("Create a cone with radius 2 and height 4.","Create a sphere with radius 1.")) { val r=a.execute(state,text,h,c);assertEquals(GraphCommandStatus.APPLIED,r.status);state=r.state;c=r.context }
        assertEquals(1,c.selectedSolidIndex)
        val r=a.execute(state,"Move the cone 3 units upward.",h,c)
        assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertEquals(0,r.context.selectedSolidIndex)
        assertEquals(3.0,r.state.solids[0].position.y,1e-8);assertEquals(state.solids[1],r.state.solids[1])
        val n=a.execute(r.state,"Scale it by 2.",h,r.context)
        assertEquals(4.0,n.state.solids[0].radius,1e-8);assertEquals(1.0,n.state.solids[1].radius,1e-8)
    }
    @Test fun nativePointCompactionKeepsNamesBoundToSurvivingObjects() {
        val values=mapOf("ruhi.studio.point.A" to "0","ruhi.studio.point.B" to "1","ruhi.studio.point.C" to "2","unrelated" to "keep")
        val after=StudioReferenceMetadata.remapPoints(values,mapOf(1 to 0,2 to 1))
        assertFalse("ruhi.studio.point.A" in after);assertEquals("0",after["ruhi.studio.point.B"]);assertEquals("1",after["ruhi.studio.point.C"]);assertEquals("keep",after["unrelated"])
    }
    @Test fun engineProposalMayRouteOnlyAnIncompatibleWellFormedRequestAtHighConfidence() {
        val previous=RuhiLanguageServices.engineClassifier
        try {
            val a=RuhiStudioAssistant();val state=WorkspaceState(module=MathModule.Geometry2D,points=listOf(Vec2(1.0,2.0)),surfaceLayers=emptyList())
            RuhiLanguageServices.engineClassifier=RuhiEngineClassifier{RuhiEnginePrediction(RuhiEngine.GRAPH_3D,.99f,.9f)}
            val r=a.execute(state,"Plot z = x+y.",WorkspaceHistoryByModule())
            assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertEquals(MathModule.Graph3D,r.state.module);assertEquals(state.points,r.state.points)
            RuhiLanguageServices.engineClassifier=RuhiEngineClassifier{RuhiEnginePrediction(RuhiEngine.GRAPH_3D,.7f,.2f)}
            val blocked=a.execute(state,"Plot z = x+y.",WorkspaceHistoryByModule());assertEquals(GraphCommandStatus.CLARIFICATION,blocked.status);assertEquals(state,blocked.state)
        } finally { RuhiLanguageServices.engineClassifier=previous }
    }
    @Test fun deletingAnIndexedSolidCannotRetargetAnExternalDependency() {
        val a=RuhiStudioAssistant();val h=WorkspaceHistoryByModule()
        val source=WorkspaceState(module=MathModule.Geometry3D,solids=listOf(Solid(SolidType.Cone,2.0),Solid(SolidType.Sphere,2.0)))
        val doc=UniversalWorkspaceBridge.fromWorkspace(source)
        val dependent=UniversalMathObject("external-reference",UniversalMathKind.Text,"Uses the cone",UniversalMathPayload.Properties(mapOf("text" to "linked")),dependencies=setOf("solid-0"))
        val state=source.copy(universalMathDocument=doc.copy(objects=doc.objects+(dependent.id to dependent)))
        val result=a.execute(state,"Delete the selected solid.",h,RuhiSceneContext(selectedSolidIndex=0))
        assertEquals(GraphCommandStatus.INVALID,result.status);assertEquals(state,result.state);assertFalse(h.forModule(state.module).canUndo)
    }
    @Test fun vertexQueryNeverClaimsAnUnrenderableCoordinateWasMarked() {
        val a=RuhiStudioAssistant();val h=WorkspaceHistoryByModule();val state=WorkspaceState(module=MathModule.Graph2D,functions=listOf(FunctionDefinition("f1","f","x^2+10000000000","cyan")))
        val result=a.execute(state,"Show the vertex.",h)
        assertEquals(GraphCommandStatus.INVALID,result.status);assertEquals(state,result.state);assertFalse(h.forModule(state.module).canUndo)
    }
}
