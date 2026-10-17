package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.workspace.*
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.Base64

class RuhiDistancePolicyMatrixTest {
    private data class Row(val id:Int,val text:String,val first:String,val second:String)
    private fun rows():List<Row> {
        val root=requireNotNull(generateSequence(File(requireNotNull(System.getProperty("user.dir")))){it.parentFile}.firstOrNull{File(it,"docs/qa/ruhi-500/replay.tsv").exists()})
        return File(root,"docs/qa/ruhi-500/replay.tsv").readLines().take(150).map{val p=it.split('\t');Row(p[0].toInt(),String(Base64.getDecoder().decode(p[1]),Charsets.UTF_8),p[4],p[5])}.also{assertEquals(150,it.size)}
    }
    private class Session(var state:WorkspaceState) {
        val h=WorkspaceHistoryByModule();val manager=RuhiDialogueManager()
        fun say(text:String)=manager.execute(state,text,h).also{state=it.state}
        fun restore(){state=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(state)).state!!}
    }
    private fun state(module:MathModule,first:String)=WorkspaceState(module=module,points=listOf(Vec2(0.0,0.0)),points3D=listOf(Point3D("opaque,/907",first,Vec3(0.0,0.0,0.0))),labSessionValues=mapOf("ruhi.studio.point.$first" to "0"))
    private fun count(s:WorkspaceState)=if(s.module==MathModule.Geometry2D)s.points.size else s.points3D.size
    private fun coordinates(module:MathModule)=if(module==MathModule.Geometry2D)"(3,4)"else"(3,4,0)"
    @Test fun everyOriginalRowAsksOnlyForTheMissingEndpointAndResumesAfterRestore() {
        for(row in rows())for(module in listOf(MathModule.Geometry2D,MathModule.Geometry3D)) {
            val initial=state(module,row.first);val s=Session(initial);val q=s.say(row.text)
            assertEquals("${row.id} ${q.message}",GraphCommandStatus.CLARIFICATION,q.status);assertEquals(initial.points,s.state.points);assertEquals(initial.points3D,s.state.points3D);assertTrue(s.h.forModule(module).protocol.isEmpty())
            assertEquals(listOf("Create point ${row.second}"),q.dialogue!!.suggestions.map{it.label});assertTrue(q.dialogue!!.pending)
            s.restore();val ask=s.say(q.dialogue!!.suggestions.single().answer);assertEquals(GraphCommandStatus.CLARIFICATION,ask.status);assertEquals(1,count(s.state));s.restore()
            val r=s.say(coordinates(module));assertEquals("${row.id} ${r.message}",GraphCommandStatus.APPLIED,r.status);assertTrue(r.message.contains("= 5.0 units"));assertEquals(2,count(s.state));assertNull(RuhiDialogueMemory.read(s.state).pending);assertEquals(1,s.h.forModule(module).protocol.size)
            s.say("Undo");assertEquals(initial.points,s.state.points);assertEquals(initial.points3D,s.state.points3D)
        }
    }
    @Test fun bothMissingEndpointsAreCreatedOnlyAfterExplicitCoordinateAnswers() {
        for(row in rows())for(module in listOf(MathModule.Geometry2D,MathModule.Geometry3D)) {
            val s=Session(WorkspaceState(module=module));val q=s.say(row.text)
            assertEquals(GraphCommandStatus.CLARIFICATION,q.status);assertEquals(setOf(row.first,row.second),q.dialogue!!.suggestions.map{it.label.removePrefix("Create point ")}.toSet());assertEquals(0,count(s.state))
            s.restore();s.say(q.dialogue!!.suggestions.first().answer);s.say(if(module==MathModule.Geometry2D)"(0,0)"else"(0,0,0)")
            assertEquals(1,count(s.state));assertNotNull(RuhiDialogueMemory.read(s.state).pending);s.restore()
            val second=RuhiDialogueMemory.read(s.state).lastChoices.single();assertEquals("Create point ${row.second}",second.label)
            s.say(second.answer);val out=s.say(coordinates(module));assertEquals(out.message,GraphCommandStatus.APPLIED,out.status);assertTrue(out.message.contains("= 5.0 units"));assertEquals(2,count(s.state));assertNull(RuhiDialogueMemory.read(s.state).pending);assertEquals(2,s.h.forModule(module).protocol.size)
        }
    }
    @Test fun allOriginalRequestsRejectInvalidCoordinatesAndCancelWithoutCreatingPoints() {
        for(row in rows())for(module in listOf(MathModule.Geometry2D,MathModule.Geometry3D)) {
            val initial=state(module,row.first);val s=Session(initial);val q=s.say(row.text);s.say(q.dialogue!!.suggestions.single().answer)
            val bad=s.say(if(module==MathModule.Geometry2D)"(abc,4)"else"(abc,4,0)")
            assertEquals(GraphCommandStatus.INVALID,bad.status);assertTrue(bad.dialogue!!.pending);assertEquals(initial.points,s.state.points);assertEquals(initial.points3D,s.state.points3D);assertTrue(s.h.forModule(module).protocol.isEmpty());s.restore()
            s.say("Cancel that");assertNull(RuhiDialogueMemory.read(s.state).pending);assertEquals(initial.points,s.state.points);assertEquals(initial.points3D,s.state.points3D)
        }
    }
    @Test fun renamedEndpointFollowUpsDoNotRequireTrainingPointNames() {
        for(row in rows())for(module in listOf(MathModule.Geometry2D,MathModule.Geometry3D)) {
            val a="Vertex_907";val b="Omega_811";val source=row.text.replace(Regex("\\b${Regex.escape(row.first)}\\b"),a).replace(Regex("\\b${Regex.escape(row.second)}\\b"),b)
            val s=Session(state(module,a));val q=s.say(source);assertEquals(GraphCommandStatus.CLARIFICATION,q.status);assertEquals("Create point $b",q.dialogue!!.suggestions.single().label)
            s.restore();s.say(q.dialogue!!.suggestions.single().answer);val out=s.say(coordinates(module));assertEquals(out.message,GraphCommandStatus.APPLIED,out.status);assertTrue(out.message.contains("Distance $a to $b = 5.0 units"));assertEquals(2,count(s.state))
        }
    }
}
