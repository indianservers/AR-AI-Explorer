package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.workspace.*
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import org.junit.Assert.*
import org.junit.Test

class RuhiSegmentSelectionFollowUpTest {
    private fun scene(name:String="AB", id:String="line,/907", type:Shape2DType=Shape2DType.Segment)=WorkspaceState(module=MathModule.Geometry2D,
        points=listOf(Vec2(0.0,0.0),Vec2(6.0,8.0),Vec2(-2.0,2.0)),
        shapes=listOf(Shape2D(id,type,listOf(0,1),name),Shape2D("triangle-811",Shape2DType.Triangle,listOf(0,1,2),"Roof")),
        labSessionValues=mapOf("ruhi.studio.point.Sun907" to "0","ruhi.studio.point.Moon811" to "1","ruhi.studio.point.M" to "2"))
    private class Session(var state:WorkspaceState) {
        val h=WorkspaceHistoryByModule();val m=RuhiDialogueManager()
        fun say(text:String,c:RuhiSceneContext=RuhiSceneContext())=m.execute(state,text,h,c).also{state=it.state}
        fun restore(){state=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(state)).state!!}
    }
    @Test fun originalSelectedSegmentMidpointResumesPlainNameAfterReloadWithCurrentParentsAndOneUndo() {
        val s=Session(scene());val q=s.say("Mark the midpoint of the selected segment")
        assertEquals(GraphCommandStatus.CLARIFICATION,q.status);assertEquals("endpoint_shape",RuhiDialogueMemory.read(s.state).pending!!.kind)
        assertTrue(q.dialogue!!.suggestions.any{it.label=="AB"});assertFalse(q.dialogue!!.suggestions.any{it.label=="Roof"});s.restore()
        s.state=s.state.copy(points=s.state.points.mapIndexed{i,p->if(i==1)Vec2(12.0,5.0)else p})
        val r=s.say("AB");assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertEquals("FIND_MIDPOINT",r.semanticFrame!!.intent);assertEquals("selected_segment",r.semanticFrame!!.parameters["target"])
        assertEquals("line,/907",r.context.selectedShapeId);assertEquals(Vec2(6.0,2.5),s.state.points.last());assertEquals(listOf(0,1),s.state.pointDependencies.single().inputIndices);assertEquals(1,s.h.forModule(s.state.module).protocol.size)
        s.say("Undo");assertEquals(3,s.state.points.size);assertEquals(Vec2(12.0,5.0),s.state.points[1]);s.say("Redo");assertEquals(Vec2(6.0,2.5),s.state.points.last())
        s.state=s.state.copy(points=s.state.points.mapIndexed{i,p->if(i==1)Vec2(2.0,4.0)else p}).recomputed();s.restore();assertEquals(Vec2(1.0,2.0),s.state.points.last())
    }
    @Test fun originalSelectedLineSlopeChipUsesStableIdAfterRenameAndReorderWithoutHistory() {
        val s=Session(scene());val q=s.say("Measure the slope of the selected line");val chip=q.dialogue!!.suggestions.single{it.label=="AB"};s.restore()
        s.state=s.state.copy(shapes=s.state.shapes.reversed().map{if(it.id=="line,/907")it.copy(name="Renamed_811")else it},points=s.state.points.mapIndexed{i,p->if(i==1)Vec2(12.0,5.0)else p})
        val before=s.state;val r=s.say(chip.answer);assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertEquals("selected_line",r.semanticFrame!!.parameters["target"]);assertTrue(r.message.contains("Slope = 0.4166666666666667"));assertEquals(before.points,s.state.points);assertEquals(before.shapes,s.state.shapes);assertTrue(s.h.forModule(s.state.module).protocol.isEmpty())
    }
    @Test fun invalidStaleAmbiguousAndIncompatibleRepliesDoNotSubstitutePointPairsOrCreateAnything() {
        val s=Session(scene());val q=s.say("Mark the midpoint of the selected segment");val stale=q.dialogue!!.suggestions.first().answer
        for(reply in listOf("Missing","Roof","Sun907 and Moon811","Use segment ***")) {
            val before=s.state;val r=s.say(reply);assertEquals(reply+": "+r.message,GraphCommandStatus.CLARIFICATION,r.status);assertEquals(before.points,s.state.points);assertEquals(before.shapes,s.state.shapes);assertTrue(s.h.forModule(s.state.module).protocol.isEmpty());s.restore()
        }
        s.state=s.state.copy(shapes=s.state.shapes+Shape2D("duplicate-name",Shape2DType.Line,listOf(1,2),"AB"));assertEquals(GraphCommandStatus.CLARIFICATION,s.say("AB").status)
        s.state=s.state.copy(shapes=s.state.shapes.filter{it.id!="line,/907"});assertEquals(GraphCommandStatus.CLARIFICATION,s.say(stale).status)
        val r=s.say("AB");assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertEquals(Vec2(2.0,5.0),s.state.points.last());assertEquals(listOf(1,2),s.state.pointDependencies.single().inputIndices)
    }
    @Test fun selectedObjectIdSurvivesLabelQuestionAndIgnoresUnrelatedNewSelection() {
        val s=Session(scene());s.say("Label the midpoint of the selected segment as M");s.restore();val q=s.say("AB");assertEquals(GraphCommandStatus.CLARIFICATION,q.status);assertEquals("midpoint_label",RuhiDialogueMemory.read(s.state).pending!!.kind);s.restore()
        s.state=s.state.copy(shapes=s.state.shapes+Shape2D("other",Shape2DType.Line,listOf(1,2),"Other"),points=s.state.points.mapIndexed{i,p->if(i==1)Vec2(4.0,6.0)else p})
        val r=s.say("Center907",RuhiSceneContext(selectedShapeId="other"));assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertEquals("line,/907",r.context.selectedShapeId);assertEquals(Vec2(2.0,3.0),s.state.points.last());assertEquals(Vec2(-2.0,2.0),s.state.points[2]);assertEquals(listOf(0,1),s.state.pointDependencies.single().inputIndices);assertEquals(1,s.h.forModule(s.state.module).protocol.size)
    }
    @Test fun removedOrChangedSelectedObjectReasksButPreservesChosenReplacementLabel() {
        val s=Session(scene());s.say("Label the midpoint of the selected segment as M");s.say("AB");s.restore()
        s.state=s.state.copy(shapes=s.state.shapes.map{if(it.id=="line,/907")it.copy(type=Shape2DType.Triangle,pointIndices=listOf(0,1,2))else it}+Shape2D("replacement",Shape2DType.Ray,listOf(1,2),"Ray907"))
        val q=s.say("Center907");assertEquals(GraphCommandStatus.CLARIFICATION,q.status);assertEquals(3,s.state.points.size);assertEquals("endpoint_shape",RuhiDialogueMemory.read(s.state).pending!!.kind);s.restore()
        val r=s.say("Ray907");assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertEquals(Vec2(2.0,5.0),s.state.points.last());assertEquals("3",s.state.labSessionValues["ruhi.studio.point.Center907"]);assertEquals("replacement",r.context.selectedShapeId)
    }
    @Test fun lexicalUnicodeAndSpacedObjectNamesStaySelectionEntities() {
        for(name in listOf("slope","circle","triangle","Δelta42","点甲","Long segment 907"))for(type in listOf(Shape2DType.Line,Shape2DType.Segment,Shape2DType.Ray)) {
            val s=Session(scene(name=name,type=type));s.say("Mark the midpoint of the selected segment");s.restore();val r=s.say(name);assertEquals("$name $type: ${r.message}",GraphCommandStatus.APPLIED,r.status);assertEquals("FIND_MIDPOINT",r.semanticFrame!!.intent);assertEquals(Vec2(3.0,4.0),s.state.points.last())
        }
    }
    @Test fun coloredSelectionKeepsColorConstraintAndRejectsWrongNativeObject() {
        val initial=scene().let{s->s.copy(shapes=s.shapes.map{it.copy(styleKey="red")}+Shape2D("blue-a",Shape2DType.Line,listOf(0,1),"BlueFirst",styleKey="blue")+Shape2D("blue-b",Shape2DType.Segment,listOf(1,2),"BlueSecond",styleKey="#0000FF"))}
        val s=Session(initial);val q=s.say("Find the slope of the blue line");assertEquals(GraphCommandStatus.CLARIFICATION,q.status);assertEquals(setOf("BlueFirst","BlueSecond"),q.dialogue!!.suggestions.map{it.label}.toSet());assertEquals(GraphCommandStatus.CLARIFICATION,s.say("AB").status)
        val r=s.say("BlueSecond");assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertEquals("blue_segment",r.semanticFrame!!.parameters["target"]);assertTrue(r.message.contains("Slope = 0.75"));assertTrue(s.h.forModule(s.state.module).protocol.isEmpty())
    }
    @Test fun cancellationModuleChangeAndNewPurposeDoNotConsumeAbandonedSegmentQuestion() {
        val s=Session(scene());s.say("Mark the midpoint of the selected segment");s.say("Cancel that");assertNull(RuhiDialogueMemory.read(s.state).pending);assertEquals(3,s.state.points.size)
        s.say("Mark the midpoint of the selected segment");s.say("Measure the slope of the selected line");val r=s.say("AB");assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertEquals("CALCULATE_SLOPE",r.semanticFrame!!.intent);assertEquals(3,s.state.points.size)
        s.say("Mark the midpoint of the selected segment");s.state=s.state.copy(module=MathModule.Geometry3D);val changed=s.say("AB");assertEquals(GraphCommandStatus.CLARIFICATION,changed.status);assertNull(RuhiDialogueMemory.read(s.state).pending)
    }
}
