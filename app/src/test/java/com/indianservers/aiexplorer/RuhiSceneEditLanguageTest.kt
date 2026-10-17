package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.workspace.*
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class RuhiSceneEditLanguageTest {
    private fun query(s:WorkspaceState,t:String,h:WorkspaceHistoryByModule=WorkspaceHistoryByModule(),c:RuhiSceneContext=RuhiSceneContext())=RuhiDialogueManager().execute(s,t,h,c)
    @Test fun original25RowsPreserveSlotsAndHaveIndependentNativeSceneChecks() {
        val root=requireNotNull(generateSequence(java.io.File(requireNotNull(System.getProperty("user.dir")))){it.parentFile}.firstOrNull{java.io.File(it,"docs/qa/ruhi-500/replay.tsv").exists()})
        fun decode(v:String)=String(java.util.Base64.getDecoder().decode(v),Charsets.UTF_8)
        val rows=java.io.File(root,"docs/qa/ruhi-500/replay.tsv").readLines().map{it.split('\t')}.filter{it[2]=="EDIT_SCENE"};assertEquals(25,rows.size);val errors=mutableListOf<String>()
        rows.forEach{row->val id=row[0].toInt();val s=RuhiSeed500Fixture.state(row[3],id);val c=RuhiSeed500Fixture.context(id);val h=RuhiSeed500Fixture.history(id,s);val r=query(s,decode(row[1]),h,c);val check=RuhiSceneEditOracle.check(RuhiSeedJson.objectValue(decode(row[6])),s,r,c);if(!check.verified)errors+="$id ${check.diagnostics}"};assertTrue(errors.joinToString("\n"),errors.isEmpty())
    }
    @Test fun duplicateCirclesHaveIndependentControlsAndLabelsUseActualOpaqueNames() {
        val s=WorkspaceState(module=MathModule.Geometry2D,points=listOf(Vec2(2.0,3.0),Vec2(5.0,7.0)),shapes=listOf(Shape2D("circle,/907",Shape2DType.Circle,listOf(0,1),"Orbit_907")),labSessionValues=mapOf("ruhi.studio.point.Anchor_907" to "0"))
        val r=query(s,"Duplicate the selected circle",c=RuhiSceneContext(selectedShapeId="circle,/907"));assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);val clone=r.state.shapes.last();assertEquals(listOf(2,3),clone.pointIndices);val a=r.state.points[2];val b=r.state.points[3];assertEquals(25.0,(b.x-a.x).pow(2)+(b.y-a.y).pow(2),1e-9)
        val moved=r.state.copy(points=r.state.points.mapIndexed{i,p->if(i==0)p+Vec2(7.0,8.0)else p});assertEquals(a,moved.points[clone.pointIndices[0]])
        val label=query(s,"Ruhi, please put a label next to point Anchor_907");assertEquals(GraphCommandStatus.APPLIED,label.status);assertEquals("Anchor_907",label.semanticFrame!!.parameters["target"]);assertEquals(s.points,label.state.points)
        val edge=s.copy(shapes=listOf(Shape2D("line,/811",Shape2DType.Segment,listOf(0,1),"Edge_811")));val marked=query(edge,"Highlight the endpoints of Edge_811");assertEquals(marked.message,GraphCommandStatus.APPLIED,marked.status);assertEquals("true",marked.state.labSessionValues[RuhiSceneEditLanguage.PREFIX+"endpoints.line,/811"])
        val infinite=edge.copy(shapes=listOf(edge.shapes.single().copy(type=Shape2DType.Line)));assertEquals(GraphCommandStatus.CLARIFICATION,query(infinite,"Highlight the endpoints of Edge_811").status)
    }
    @Test fun ambiguityUsesCurrentObjectChipsAndStaleAnswersNeverEditAnotherObject() {
        val s=WorkspaceState(module=MathModule.Geometry2D,points=listOf(Vec2(0.0,0.0),Vec2(2.0,0.0),Vec2(0.0,3.0)),shapes=listOf(Shape2D("first,/907",Shape2DType.Triangle,listOf(0,1,2),"Hill_907"),Shape2D("next,/811",Shape2DType.Triangle,listOf(0,1,2),"Roof_811")))
        val r=query(s,"Color the selected triangle red");assertEquals(GraphCommandStatus.CLARIFICATION,r.status);assertEquals(listOf("Hill_907","Roof_811"),r.dialogue!!.suggestions.map{it.label})
        val loaded=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(r.state),recover=false).state!!;assertEquals(r.dialogue,RuhiDialogueManager().restoredView(loaded));val chosen=query(loaded,r.dialogue!!.suggestions.last().answer);assertEquals(chosen.message,GraphCommandStatus.APPLIED,chosen.status);assertEquals("#FF0000",chosen.state.shapes[1].styleKey);assertEquals(s.shapes[0],chosen.state.shapes[0])
        val stale=query(r.state.copy(shapes=s.shapes.drop(1)),r.dialogue!!.suggestions.first().answer);assertEquals(GraphCommandStatus.CLARIFICATION,stale.status);assertNull(RuhiDialogueMemory.read(stale.state).pending);assertNull(RuhiDialogueMemory.read(query(r.state,"Never mind").state).pending)
    }
    @Test fun renameSaveAndClearPersistAndUndoRestoresTheWholeConstruction() {
        val s=RuhiSeed500Fixture.state("GEOMETRY_2D");val h=WorkspaceHistoryByModule();val renamed=query(s,"Rename the current scene to Trial 907",h);assertEquals("Trial 907",renamed.state.name);val undone=query(renamed.state,"Undo the last change",h);assertEquals(s.name,undone.state.name)
        val saved=query(renamed.state,"Save the current construction");val loaded=WorkspaceProjectCodec.decode(saved.state.labSessionValues.getValue(RuhiSceneEditLanguage.SAVED),recover=false).state!!;assertEquals(renamed.state.name,loaded.name);assertEquals(s.points,loaded.points);assertEquals(s.shapes,loaded.shapes)
        val cleared=query(s,"Clear the canvas",h);assertTrue(cleared.state.points.isEmpty());assertTrue(cleared.state.shapes.isEmpty());assertEquals(s.solids,cleared.state.solids);val restore=query(cleared.state,"Undo the last change",h);assertEquals(s.points,restore.state.points);assertEquals(s.shapes,restore.state.shapes)
    }
    @Test fun layoutOnlyPreparationChangesNeverHideRealSceneOrSelectionMutations() {
        val s=WorkspaceState(module=MathModule.Geometry2D,points=listOf(Vec2(2.0,3.0)))
        assertTrue(RuhiPreparedSceneGuard.onlyViewportMetricsChanged(s,s.copy(labSessionValues=mapOf(RuhiSceneEditLanguage.ASPECT to "1.7"),modifiedAt=s.modifiedAt+1)))
        assertFalse(RuhiPreparedSceneGuard.onlyViewportMetricsChanged(s,s.copy(points=listOf(Vec2(4.0,5.0)))))
        assertFalse(RuhiPreparedSceneGuard.onlyViewportMetricsChanged(s,s.copy(graph2DView=s.graph2DView.copy(zoom=2f))))
        assertFalse(RuhiPreparedSceneGuard.onlyViewportMetricsChanged(s,s.copy(name="Different scene")))
        assertFalse(RuhiPreparedSceneGuard.onlyViewportMetricsChanged(s,s.copy(labSessionValues=mapOf("ruhi.geometry3d.selection" to "1,2"))))
    }
    @Test fun transparencyPercentMeansTheComplementOfOpacityForNewNumericValues() {
        val s=WorkspaceState(module=MathModule.Graph3D,surfaceLayers=listOf(SpatialSurfaceLayer("mesh,/907","x+y")))
        for(percent in listOf(12.5,30.0,73.25)){val r=query(s,"Make the mesh $percent percent see-through");assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertEquals(1-percent/100,r.state.surfaceLayers.single().opacity,1e-9);assertEquals(s.surfaceLayers.single().expression,r.state.surfaceLayers.single().expression)}
        for(percent in listOf(101,999)){val r=query(s,"Make the mesh $percent percent see-through");assertEquals(GraphCommandStatus.INVALID,r.status);assertEquals(s.surfaceLayers,r.state.surfaceLayers)}
    }
    @Test fun redLineSelectionRejectsCurvedGraphsAndFindsImplicitVerticalLines() {
        val curve=WorkspaceState(module=MathModule.Graph2D,functions=listOf(FunctionDefinition("parabola","P","y=x^2","#FF0000")))
        val rejected=query(curve,"Thicken the red line");assertEquals(GraphCommandStatus.CLARIFICATION,rejected.status);assertEquals(curve.functions,rejected.state.functions)
        val vertical=curve.copy(functions=curve.functions+FunctionDefinition("vertical","Wall","x=3","#FF0000"));val selected=query(vertical,"Thicken the red line");assertEquals(selected.message,GraphCommandStatus.APPLIED,selected.status);assertEquals(curve.functions.single(),selected.state.functions[0]);assertEquals(6.3f,selected.state.functions[1].appearance.width,.0001f)
    }
    @Test fun nativeGridAndLabelsCanBeRestoredAndFitUsesCurrentCircleExtentAndAspect() {
        for(module in listOf(MathModule.Geometry2D,MathModule.Graph2D,MathModule.Graph3D,MathModule.Geometry3D)){val s=WorkspaceState(module=module);val hidden=query(s,"Hide the coordinate grid");assertEquals(hidden.message,GraphCommandStatus.APPLIED,hidden.status);assertFalse(RuhiSceneEditLanguage.grid(hidden.state));assertTrue(RuhiSceneEditLanguage.grid(query(hidden.state,"Show the coordinate grid").state))}
        val s=WorkspaceState(module=MathModule.Geometry2D,points=listOf(Vec2(3.0,-4.0),Vec2(13.0,-4.0)),shapes=listOf(Shape2D("orb",Shape2DType.Circle,listOf(0,1))),labSessionValues=mapOf(RuhiSceneEditLanguage.ASPECT to ".7","ruhi.studio.point.Origin_907" to "0"));val fit=query(s,"Reset zoom to fit all objects");assertEquals(fit.message,GraphCommandStatus.APPLIED,fit.status);val v=fit.state.graph2DView;assertEquals(3.0,v.centerX,1e-9);assertEquals(-4.0,v.centerY,1e-9);assertTrue(10<7/v.zoom*.7)
        val hidden=query(s,"Remove labels from all points");assertFalse(RuhiSceneEditLanguage.pointLabelVisible(hidden.state,0));val shown=query(hidden.state,"Put a label next to point Origin_907");assertTrue(RuhiSceneEditLanguage.pointLabelVisible(shown.state,0));assertFalse(RuhiSceneEditLanguage.pointLabelVisible(shown.state,1))
    }
}
