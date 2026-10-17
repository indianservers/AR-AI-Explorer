package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.workspace.*
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import org.junit.Assert.*
import org.junit.Test

class RuhiEndpointClarificationTest {
    private fun scene(names:List<String> = listOf("Sun907","Moon811","North553","South662"))=WorkspaceState(module=MathModule.Geometry2D,points=listOf(Vec2(0.0,0.0),Vec2(6.0,8.0),Vec2(1.0,1.0),Vec2(3.0,5.0)).take(names.size),labSessionValues=names.mapIndexed{i,name->"ruhi.studio.point.$name" to "$i"}.toMap())
    private class Session(var state:WorkspaceState) {
        val h=WorkspaceHistoryByModule();val m=RuhiDialogueManager()
        fun say(text:String)=m.execute(state,text,h).also{state=it.state}
        fun restore(){state=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(state)).state!!}
    }
    @Test fun originalMidpointQuestionResumesSavedEndpointNamesWithOneDependentUndoableConstruction() {
        val s=Session(scene());val q=s.say("Find the midpoint of AB");assertEquals(GraphCommandStatus.CLARIFICATION,q.status);assertEquals("endpoint_reference",RuhiDialogueMemory.read(s.state).pending!!.kind);assertTrue(q.dialogue!!.suggestions.isNotEmpty());assertEquals(4,s.state.points.size)
        s.restore();val r=s.say("Sun907 and Moon811");assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertEquals("FIND_MIDPOINT",r.semanticFrame!!.intent);assertEquals("Sun907",r.semanticFrame!!.parameters["a"]);assertEquals("Moon811",r.semanticFrame!!.parameters["b"]);assertEquals(Vec2(3.0,4.0),s.state.points.last());val dep=s.state.pointDependencies.single();assertEquals(listOf(0,1),dep.inputIndices);assertEquals(PointDependencyType.Midpoint,dep.type);assertEquals(1,s.h.forModule(s.state.module).protocol.size)
        s.say("Undo");assertEquals(scene().points,s.state.points);s.say("Redo");assertEquals(Vec2(3.0,4.0),s.state.points.last())
    }
    @Test fun originalSlopeReplyReadsCurrentCoordinatesAndLeavesSceneAndHistoryUnchanged() {
        val s=Session(scene());s.say("Find the slope of AB");s.restore();s.state=s.state.copy(points=s.state.points.mapIndexed{i,p->if(i==1)Vec2(12.0,5.0)else p});val before=s.state
        val r=s.say("Sun907 and Moon811");assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertEquals("CALCULATE_SLOPE",r.semanticFrame!!.intent);val actual=Regex("Slope = ([-+0-9.eE]+)").find(r.message)!!.groupValues[1].toDouble();assertEquals(5.0/12,actual,1e-10);assertEquals(before.points,s.state.points);assertEquals(before.shapes,s.state.shapes);assertTrue(s.h.forModule(s.state.module).protocol.isEmpty());assertNull(RuhiDialogueMemory.read(s.state).pending)
    }
    @Test fun twoUnresolvedComparedSegmentsRetainEachPairAcrossSavedQuestions() {
        val s=Session(scene());s.say("Compare slopes of AB and CD");s.restore();val next=s.say("Sun907 and Moon811");assertEquals(next.message,GraphCommandStatus.CLARIFICATION,next.status);assertTrue(next.message.contains("CD"));s.restore()
        val r=s.say("North553 and South662");assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertTrue(r.message.contains("First slope = 1.3333333333333333"));assertTrue(r.message.contains("second slope = 2.0"));assertEquals(scene().points,s.state.points);assertTrue(s.h.forModule(s.state.module).protocol.isEmpty())
    }
    @Test fun missingAmbiguousAndRepeatedNativePointRepliesDoNotCommitAndCanBeCorrected() {
        val s=Session(scene());s.say("Find the midpoint of AB")
        for(answer in listOf("Missing907 and Moon811","Sun907 and Sun907")) {val r=s.say(answer);assertEquals(r.message,GraphCommandStatus.CLARIFICATION,r.status);assertEquals(4,s.state.points.size);assertTrue(s.h.forModule(s.state.module).protocol.isEmpty());s.restore()}
        s.state=s.state.copy(labSessionValues=s.state.labSessionValues+("ruhi.studio.point.SUN907" to "2"));val ambiguous=s.say("Sun907 and Moon811");assertEquals(GraphCommandStatus.CLARIFICATION,ambiguous.status);assertFalse(ambiguous.dialogue!!.suggestions.any{it.answer.contains("Sun907",true)});s.restore()
        val out=s.say("Moon811 and South662");assertEquals(out.message,GraphCommandStatus.APPLIED,out.status);assertEquals(Vec2(4.5,6.5),s.state.points.last())
    }
    @Test fun currentContextualChipIsBoundedExecutableAndTracksLiveMidpointParents() {
        val s=Session(scene(listOf("Sun907","Moon811")));val q=s.say("Find the midpoint of AB");assertTrue(q.dialogue!!.suggestions.size in 1..5);s.restore();val chip=s.m.restoredView(s.state)!!.suggestions.first();val r=s.say(chip.answer);assertEquals(r.message,GraphCommandStatus.APPLIED,r.status)
        s.state=s.state.copy(points=s.state.points.mapIndexed{i,p->if(i==1)Vec2(10.0,12.0)else p}).recomputed();assertEquals(Vec2(5.0,6.0),s.state.points.last());assertEquals(3,s.state.points.size)
    }
    @Test fun customLabelAndSplitOperationSurviveEndpointClarification() {
        for(command in listOf("Label the middle of AB as Center907","Split AB into two equal segments")) {
            val s=Session(scene());s.say(command);s.restore();val r=s.say("Sun907 and Moon811");assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertEquals(Vec2(3.0,4.0),s.state.points.last());assertEquals(1,s.h.forModule(s.state.module).protocol.size)
            if(command.startsWith("Label"))assertEquals("4",s.state.labSessionValues["ruhi.studio.point.Center907"])else {assertEquals(2,s.state.shapes.size);assertTrue(r.semanticFrame!!.parameters["split"]==true)}
        }
    }
    @Test fun lexicalAndUnicodePointNamesDoNotChangeTheRetainedOperation() {
        for(name in listOf("slope","and","circle","triangle","Δelta42","点甲","Vertex907"))for(command in listOf("Find the midpoint of AB","Find the slope of AB")) {
            val s=Session(scene(listOf(name,"Moon811")));s.say(command);s.restore();val r=s.say("$name and Moon811");assertEquals("$name: ${r.message}",GraphCommandStatus.APPLIED,r.status);assertEquals(name,r.semanticFrame!!.parameters["a"]);assertEquals(if(command.contains("midpoint"))"FIND_MIDPOINT"else"CALCULATE_SLOPE",r.semanticFrame!!.intent)
        }
    }
    @Test fun cancellationStudioChangesAndNewOperationsDoNotReuseAbandonedPurpose() {
        val s=Session(scene());s.say("Find the midpoint of AB");s.restore();s.say("Cancel that");assertNull(RuhiDialogueMemory.read(s.state).pending);assertEquals(4,s.state.points.size)
        s.say("Find the midpoint of AB");s.say("Find the slope of AB");val r=s.say("Sun907 and Moon811");assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertEquals("CALCULATE_SLOPE",r.semanticFrame!!.intent);assertEquals(4,s.state.points.size)
        s.say("Find the midpoint of AB");s.state=s.state.copy(module=MathModule.Geometry3D);val changed=s.say("Sun907 and Moon811");assertEquals(GraphCommandStatus.CLARIFICATION,changed.status);assertNull(RuhiDialogueMemory.read(s.state).pending);assertEquals(4,s.state.points.size)
    }
}
