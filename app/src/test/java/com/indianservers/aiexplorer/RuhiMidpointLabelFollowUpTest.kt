package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.workspace.*
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import org.junit.Assert.*
import org.junit.Test

class RuhiMidpointLabelFollowUpTest {
    private fun scene(a:String="A",b:String="B")=WorkspaceState(module=MathModule.Geometry2D,points=listOf(Vec2(0.0,0.0),Vec2(6.0,8.0),Vec2(9.0,9.0)),labSessionValues=mapOf("ruhi.studio.point.$a" to "0","ruhi.studio.point.$b" to "1","ruhi.studio.point.M" to "2"))
    private class Session(var state:WorkspaceState) {
        val history=WorkspaceHistoryByModule();val manager=RuhiDialogueManager()
        fun say(source:String)=manager.execute(state,source,history).also{state=it.state}
        fun restore(){state=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(state)).state!!}
    }
    @Test fun originalLabelCollisionOffersUnusedNamesAndResumesSavedConstructionAtomically() {
        val s=Session(scene());val q=s.say("Label the middle of AB as M");assertEquals(GraphCommandStatus.CLARIFICATION,q.status);assertEquals("midpoint_label",RuhiDialogueMemory.read(s.state).pending!!.kind);assertTrue(q.dialogue!!.suggestions.size in 3..5);assertFalse(q.dialogue!!.suggestions.any{it.label=="M"});assertEquals(3,s.state.points.size);s.restore()
        val r=s.say("Center907");assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertEquals("FIND_MIDPOINT",r.semanticFrame!!.intent);assertEquals("Center907",r.semanticFrame!!.parameters["label"]);assertEquals(Vec2(3.0,4.0),s.state.points.last());assertEquals("2",s.state.labSessionValues["ruhi.studio.point.M"]);assertEquals(Vec2(9.0,9.0),s.state.points[2]);assertEquals(listOf(0,1),s.state.pointDependencies.single().inputIndices);assertEquals(1,s.history.forModule(s.state.module).protocol.size)
        s.say("Undo");assertEquals(scene().points,s.state.points);s.say("Redo");assertEquals("3",s.state.labSessionValues["ruhi.studio.point.Center907"])
    }
    @Test fun endpointThenLabelQuestionRetainsBindingsAndReadsCurrentParentsAfterRestore() {
        val s=Session(scene("Sun907","Moon811"));s.say("Label the middle of AB as M");s.restore();val q=s.say("Sun907 and Moon811");assertEquals(q.message,GraphCommandStatus.CLARIFICATION,q.status);assertEquals("midpoint_label",RuhiDialogueMemory.read(s.state).pending!!.kind);assertTrue(q.dialogue!!.suggestions.none{it.answer.startsWith("Use endpoints")});s.restore()
        s.state=s.state.copy(points=s.state.points.mapIndexed{i,p->if(i==1)Vec2(8.0,10.0)else p});val r=s.say("Center907");assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertEquals(Vec2(4.0,5.0),s.state.points.last());assertEquals("Sun907",r.semanticFrame!!.parameters["a"]);assertEquals("Moon811",r.semanticFrame!!.parameters["b"]);assertEquals("Center907",r.semanticFrame!!.parameters["label"]);assertEquals(1,s.history.forModule(s.state.module).protocol.size)
    }
    @Test fun savedSuggestedLabelThatBecameOccupiedIsReaskedWithoutAnEdit() {
        val s=Session(scene());val q=s.say("Label the middle of AB as M");val stale=q.dialogue!!.suggestions.first();s.restore();s.state=s.state.copy(points=s.state.points+Vec2(7.0,7.0),labSessionValues=s.state.labSessionValues+("ruhi.studio.point.${stale.label}" to "3"));val before=s.state.points
        val retry=s.say(stale.answer);assertEquals(retry.message,GraphCommandStatus.CLARIFICATION,retry.status);assertEquals(before,s.state.points);assertTrue(s.history.forModule(s.state.module).protocol.isEmpty());assertFalse(retry.dialogue!!.suggestions.any{it.label==stale.label});s.restore();val done=s.say(s.manager.restoredView(s.state)!!.suggestions.first().answer);assertEquals(done.message,GraphCommandStatus.APPLIED,done.status);assertEquals(before,s.state.points.take(before.size))
    }
    @Test fun lexicalUnicodeAndNumberWordLabelsAreValuesRatherThanNewCommands() {
        for(label in listOf("slope","circle","and","one","Δelta42","点甲","Center907")) {
            val s=Session(scene());s.say("Label the middle of AB as M");s.restore();val r=s.say(label);assertEquals("$label: ${r.message}",GraphCommandStatus.APPLIED,r.status);assertEquals("FIND_MIDPOINT",r.semanticFrame!!.intent);assertEquals(label,r.semanticFrame!!.parameters["label"]);assertEquals("3",s.state.labSessionValues["ruhi.studio.point.$label"]);assertEquals(Vec2(3.0,4.0),s.state.points.last())
        }
    }
    @Test fun invalidOrUsedReplacementLabelsPreserveQuestionWithoutPointOrHistoryChanges() {
        val s=Session(scene());s.say("Label the middle of AB as M")
        for(answer in listOf("123","two words","Use midpoint label 123","Use midpoint label two words","Use midpoint label "+"Q".repeat(33),"M","A")) {
            val r=s.say(answer);assertEquals(r.message,GraphCommandStatus.CLARIFICATION,r.status);assertEquals("midpoint_label",RuhiDialogueMemory.read(s.state).pending!!.kind);assertEquals(scene().points,s.state.points);assertTrue(s.history.forModule(s.state.module).protocol.isEmpty());s.restore()
        }
        assertEquals(GraphCommandStatus.APPLIED,s.say("Center907").status)
    }
    @Test fun cancellationNewOperationAndStudioChangeDoNotCreateAnAbandonedMidpoint() {
        val s=Session(scene());s.say("Label the middle of AB as M");s.say("Cancel that");assertNull(RuhiDialogueMemory.read(s.state).pending);assertEquals(3,s.state.points.size)
        s.say("Label the middle of AB as M");val slope=s.say("Find the slope of AB");assertEquals(slope.message,GraphCommandStatus.APPLIED,slope.status);assertEquals("CALCULATE_SLOPE",slope.semanticFrame!!.intent);assertEquals(3,s.state.points.size);assertNull(RuhiDialogueMemory.read(s.state).pending);assertTrue(s.history.forModule(s.state.module).protocol.isEmpty())
        s.say("Label the middle of AB as M");s.state=s.state.copy(module=MathModule.Geometry3D);val changed=s.say("Center907");assertEquals(GraphCommandStatus.CLARIFICATION,changed.status);assertNull(RuhiDialogueMemory.read(s.state).pending);assertEquals(3,s.state.points.size)
    }
    @Test fun unlabelledNativeFallbackNamesAreAlsoReservedAndOriginalFreeLabelStillExecutes() {
        val base=scene().let{it.copy(labSessionValues=it.labSessionValues-"ruhi.studio.point.M")};val s=Session(base);val q=s.say("Label the middle of AB as P3");assertEquals(q.message,GraphCommandStatus.CLARIFICATION,q.status);assertEquals("midpoint_label",RuhiDialogueMemory.read(s.state).pending!!.kind);assertFalse(q.dialogue!!.suggestions.any{it.label=="P3"});assertEquals(GraphCommandStatus.APPLIED,s.say("Center907").status);assertEquals(Vec2(9.0,9.0),s.state.points[2])
        val direct=Session(base);val r=direct.say("Label the middle of AB as M");assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertEquals("3",direct.state.labSessionValues["ruhi.studio.point.M"])
    }
    @Test fun chosenReplacementLabelSurvivesAnotherEndpointQuestionAfterParentNamesChange() {
        val s=Session(scene());s.say("Label the middle of AB as M");s.restore();s.state=s.state.copy(labSessionValues=(s.state.labSessionValues-"ruhi.studio.point.A"-"ruhi.studio.point.B")+mapOf("ruhi.studio.point.Sun907" to "0","ruhi.studio.point.Moon811" to "1"))
        val q=s.say("Center907");assertEquals(q.message,GraphCommandStatus.CLARIFICATION,q.status);assertEquals("endpoint_reference",RuhiDialogueMemory.read(s.state).pending!!.kind);s.restore();val done=s.say("Sun907 and Moon811");assertEquals(done.message,GraphCommandStatus.APPLIED,done.status);assertEquals("Center907",done.semanticFrame!!.parameters["label"]);assertEquals("3",s.state.labSessionValues["ruhi.studio.point.Center907"]);assertEquals(Vec2(3.0,4.0),s.state.points.last());assertEquals("2",s.state.labSessionValues["ruhi.studio.point.M"])
    }
}
