package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.workspace.*
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import java.io.File
import java.util.Base64
import org.junit.Assert.*
import org.junit.Test

class RuhiMidpointLanguageTest {
    private val root:File=requireNotNull(generateSequence(File(requireNotNull(System.getProperty("user.dir")))){it.parentFile}.firstOrNull{File(it,"docs/qa/ruhi-500/replay.tsv").exists()})
    private fun decode(s:String)=String(Base64.getDecoder().decode(s),Charsets.UTF_8)
    @Test fun everyMidpointRowSatisfiesTheOriginalSlotsMarkerHistoryAndPersistence() {
        val rows=File(root,"docs/qa/ruhi-500/replay.tsv").readLines().map{it.split('\t')}.filter{it[2]=="FIND_MIDPOINT"};assertEquals(25,rows.size)
        rows.forEach { row->
            val s=RuhiSeed500Fixture.state(row[3],row[0].toInt());val c=RuhiSeed500Fixture.context();val h=WorkspaceHistoryByModule();val manager=RuhiDialogueManager()
            val r=manager.execute(s,decode(row[1]),h,c);val check=RuhiSeedBehaviorOracle.check(row[2],RuhiSeedJson.objectValue(decode(row[6])),s,r,c)
            assertTrue("${row[0]}: ${check.diagnostics.joinToString()}",check.verified);assertEquals(1,h.forModule(s.module).protocol.size)
            val restored=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(r.state)).state!!;assertEquals(r.state.points,restored.points);assertEquals(r.state.pointDependencies,restored.pointDependencies);assertEquals(r.state.labSessionValues,restored.labSessionValues)
            val undo=manager.execute(r.state,"Undo.",h,c);assertEquals(s.points,undo.state.points);assertEquals(s.shapes,undo.state.shapes)
            val redo=manager.execute(undo.state,"Redo.",h,c);assertEquals(r.state.points,redo.state.points);assertEquals(r.state.pointDependencies,redo.state.pointDependencies)
        }
    }
    @Test fun unseenEndpointNamesAndDraggingParentsUpdateTheSameMarker() {
        val s=WorkspaceState(module=MathModule.Geometry2D,points=listOf(Vec2(-4.0,2.0),Vec2(10.0,8.0)),labSessionValues=mapOf("ruhi.studio.point.Start_907" to "0","ruhi.studio.point.Finish_811" to "1"))
        val r=RuhiDialogueManager().execute(s,"Mark the midpoint between Start_907 and Finish_811.",WorkspaceHistoryByModule())
        assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertEquals(Vec2(3.0,5.0),r.state.points.last())
        val updated=r.state.copy(points=r.state.points.mapIndexed{i,p->if(i==0)Vec2(2.0,-2.0)else p}).recomputed();assertEquals(Vec2(6.0,3.0),updated.points.last());assertEquals(3,updated.points.size)
    }
    @Test fun missingEndpointsResumeAfterCreationAndUndoTheWholeLastAnswer() {
        val m=RuhiDialogueManager();val h=WorkspaceHistoryByModule();var s=WorkspaceState(module=MathModule.Geometry2D)
        val q=m.execute(s,"Mark the midpoint between Start_907 and Finish_811.",h);assertEquals(GraphCommandStatus.CLARIFICATION,q.status);assertTrue(q.dialogue!!.pending)
        val first=m.execute(q.state,"(0,0)",h);assertEquals(GraphCommandStatus.APPLIED,first.status);assertEquals(1,first.state.points.size);assertTrue(first.dialogue!!.pending)
        val second=m.execute(first.state,"(6,8)",h);assertEquals(second.message,GraphCommandStatus.APPLIED,second.status);assertEquals(3,second.state.points.size);assertEquals(Vec2(3.0,4.0),second.state.points.last());assertFalse(second.dialogue!!.pending)
        val undo=m.execute(second.state,"Undo.",h);assertEquals(1,undo.state.points.size);assertTrue(undo.state.pointDependencies.isEmpty())
    }
    @Test fun labelCollisionsAndAmbiguousCompoundSegmentsNeverOverwritePoints() {
        val s=RuhiSeed500Fixture.state("GEOMETRY_2D");val r=RuhiDialogueManager().execute(s,"Label the middle of AB as M.",WorkspaceHistoryByModule())
        assertEquals(GraphCommandStatus.CLARIFICATION,r.status);assertEquals(s.points,r.state.points)
        val ambiguous=s.copy(labSessionValues=s.labSessionValues+mapOf("ruhi.studio.point.AB" to "3","ruhi.studio.point.BC" to "4"))
        val a=RuhiDialogueManager().execute(ambiguous,"Find the midpoint of ABC.",WorkspaceHistoryByModule());assertEquals(GraphCommandStatus.CLARIFICATION,a.status);assertEquals(ambiguous.points,a.state.points)
    }
}
