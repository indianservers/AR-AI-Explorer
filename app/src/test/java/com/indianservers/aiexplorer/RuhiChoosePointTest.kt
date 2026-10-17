package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.workspace.*
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.Base64

class RuhiChoosePointTest {
    private class Session(var state:WorkspaceState) {
        val history=WorkspaceHistoryByModule()
        private val manager=RuhiDialogueManager()
        fun say(text:String)=manager.execute(state,text,history).also{state=it.state}
        fun restore(){state=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(state)).state!!}
    }
    private fun scene(module:MathModule,vararg names:String)=WorkspaceState(module=module,
        points=names.mapIndexed{i,_->Vec2(i*3.0,i*4.0)},
        points3D=names.mapIndexed{i,name->Point3D("native-$i",name,Vec3(i*3.0,i*4.0,0.0))},
        labSessionValues=names.mapIndexed{i,name->"ruhi.studio.point.$name" to "$i"}.toMap())
    @Test fun chooseExistingEndpointSurvivesRestoreAndDoesNotCreateRenameOrAddUndo() {
        for(module in listOf(MathModule.Geometry2D,MathModule.Geometry3D)) {
            val initial=scene(module,"Anchor907","Gamma811");val s=Session(initial)
            val q=s.say("Distance between Anchor907 and Missing53")
            val choice=q.dialogue!!.suggestions.singleOrNull{it.label=="Use Gamma811 for Missing53"}
            assertNotNull("Missing existing-point alternative: ${q.dialogue}",choice)
            s.restore();val out=s.say(choice!!.answer)
            assertEquals(out.message,GraphCommandStatus.APPLIED,out.status)
            assertTrue(out.message.contains("Distance Anchor907 to Gamma811 = 5.0 units"))
            assertEquals(initial.points,s.state.points);assertEquals(initial.points3D,s.state.points3D)
            assertTrue(s.history.forModule(module).protocol.isEmpty());assertNull(RuhiDialogueMemory.read(s.state).pending)
        }
    }
    @Test fun bothMissingRolesCanUseTwoExistingPointsAcrossSavedReplies() {
        for(module in listOf(MathModule.Geometry2D,MathModule.Geometry3D)) {
            val initial=scene(module,"Gamma811","Delta907");val s=Session(initial)
            s.say("How far is MissingA from MissingB?");s.restore()
            val a=s.say("Use Gamma811 for MissingA")
            assertEquals(a.message,GraphCommandStatus.CLARIFICATION,a.status)
            assertTrue(a.dialogue!!.suggestions.any{it.label=="Use Delta907 for MissingB"});s.restore()
            val b=s.say("Use Delta907 for MissingB")
            assertEquals(b.message,GraphCommandStatus.APPLIED,b.status);assertTrue(b.message.contains("Distance Gamma811 to Delta907 = 5.0 units"))
            assertEquals(initial.points,s.state.points);assertEquals(initial.points3D,s.state.points3D);assertTrue(s.history.forModule(module).protocol.isEmpty())
        }
    }
    @Test fun all150OriginalRowsSupportExistingAndMixedCreationChoicesInBothStudios() {
        val root=requireNotNull(generateSequence(File(requireNotNull(System.getProperty("user.dir")))){it.parentFile}.firstOrNull{File(it,"docs/qa/ruhi-500/replay.tsv").exists()})
        val rows=File(root,"docs/qa/ruhi-500/replay.tsv").readLines().take(150);assertEquals(150,rows.size)
        for(row in rows)for(module in listOf(MathModule.Geometry2D,MathModule.Geometry3D))for(mode in 0..2) {
            val p=row.split('\t');val source=String(Base64.getDecoder().decode(p[1]),Charsets.UTF_8);val a=p[4];val b=p[5]
            val initial=if(mode==0)scene(module,a,"Gamma811")else scene(module,"Gamma811","Delta907")
            val s=Session(initial);s.say(source);s.restore()
            if(mode==0) {
                val r=s.say("Use Gamma811 for $b");assertEquals("${p[0]} ${r.message}",GraphCommandStatus.APPLIED,r.status);assertTrue(r.message.contains("Distance $a to Gamma811 = 5.0 units"))
            } else if(mode==1) {
                s.say("Use Gamma811 for $a");s.restore()
                val r=s.say("Use Delta907 for $b");assertEquals("${p[0]} ${r.message}",GraphCommandStatus.APPLIED,r.status);assertTrue(r.message.contains("Distance Gamma811 to Delta907 = 5.0 units"))
            } else {
                s.say("Use Gamma811 for $a");s.restore();s.say("Create point $b at ");s.restore()
                val r=s.say(if(module==MathModule.Geometry2D)"(6,8)"else"(6,8,0)");assertEquals("${p[0]} ${r.message}",GraphCommandStatus.APPLIED,r.status);assertTrue(r.message.contains("Distance Gamma811 to $b = 10.0 units"))
                assertEquals(1,s.history.forModule(module).protocol.size);s.say("Undo")
            }
            assertEquals(initial.points,s.state.points);assertEquals(initial.points3D,s.state.points3D)
            assertTrue(s.history.forModule(module).protocol.isEmpty())
        }
    }
    @Test fun savedChoiceReadsCurrentCoordinatesAndRemembersActualPairForUnitReplies() {
        for(module in listOf(MathModule.Geometry2D,MathModule.Geometry3D)) {
            val s=Session(scene(module,"Anchor907","Gamma811"));s.say("Use meters for coordinate units")
            s.say("Distance between Anchor907 and Missing53");s.restore()
            s.state=s.state.copy(points=listOf(Vec2(0.0,0.0),Vec2(6.0,8.0)),points3D=s.state.points3D.mapIndexed{i,p->if(i==1)p.copy(position=Vec3(6.0,8.0,0.0))else p})
            val r=s.say("Use Gamma811 for Missing53");assertEquals(GraphCommandStatus.APPLIED,r.status);assertTrue(r.message.contains("= 10.0 m."));s.restore()
            assertTrue(s.say("Use centimeters instead").message.contains("Distance Anchor907 to Gamma811 = 1000.0 cm"))
        }
    }
    @Test fun unavailableOrAmbiguousChoicesDoNotRenameCreateOrMutateGeometry() {
        val initial=scene(MathModule.Geometry3D,"Anchor907","Gamma811")
        val s=Session(initial);val q=s.say("Distance between Anchor907 and Missing53")
        val stale=q.dialogue!!.suggestions.single{it.label=="Use Gamma811 for Missing53"}.answer
        s.state=s.state.copy(points3D=s.state.points3D+Point3D("duplicate","Gamma811",Vec3(9.0,9.0,9.0)))
        val before=s.state.points3D;val r=s.say(stale)
        assertEquals(GraphCommandStatus.CLARIFICATION,r.status);assertFalse(r.dialogue!!.suggestions.any{it.label=="Use Gamma811 for Missing53"});assertEquals(before,s.state.points3D);assertTrue(s.history.forModule(s.state.module).protocol.isEmpty())
        s.restore();s.say("Cancel that");assertNull(RuhiDialogueMemory.read(s.state).pending);assertEquals(before,s.state.points3D)
    }
    @Test fun unrelatedRoleDoesNotConsumeThePendingDistanceQuestion() {
        val s=Session(scene(MathModule.Geometry2D,"Anchor907","Gamma811"));s.say("Distance between Anchor907 and Missing53")
        val wrong=s.say("Use Gamma811 for OtherRole");assertEquals(GraphCommandStatus.CLARIFICATION,wrong.status)
        assertNotNull(RuhiDialogueMemory.read(s.state).pending);s.restore()
        assertTrue(s.say("Use Gamma811 for Missing53").message.contains("= 5.0 units"))
    }
}
