package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.workspace.*
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.Base64
import kotlin.math.*

class RuhiPointLanguageTest {
    private val root:File=requireNotNull(generateSequence(File(requireNotNull(System.getProperty("user.dir")))){it.parentFile}.firstOrNull{File(it,"docs/qa/ruhi-500/replay.tsv").exists()})
    @Test fun everyDistanceTemplateUsesUnseenLabelsAndChangedCoordinatesInBothStudios() {
        var checked=0
        for(row in File(root,"docs/qa/ruhi-500/replay.tsv").readLines().take(150)) {
            val p=row.split('\t');val source=String(Base64.getDecoder().decode(p[1]),Charsets.UTF_8)
            val first="Vertex_907";val second="Omega_811"
            val renamed=source.replace(Regex("\\b${Regex.escape(p[4])}\\b"),first).replace(Regex("\\b${Regex.escape(p[5])}\\b"),second)
            assertEquals(RuhiSeedNlpInput.normalize(source),RuhiSeedNlpInput.normalize(renamed))
            for(module in listOf(MathModule.Geometry2D,MathModule.Geometry3D))for(offset in listOf(0.0,17.0)) {
                val a=Vec3(-3.0+offset,2.0,1.0);val b=Vec3(9.0,7.0+offset,-4.0)
                val s=WorkspaceState(module=module,points=listOf(Vec2(a.x,a.y),Vec2(b.x,b.y)),points3D=listOf(Point3D("p",first,a),Point3D("q",second,b)),labSessionValues=mapOf("ruhi.studio.point.$first" to "0","ruhi.studio.point.$second" to "1"))
                val h=WorkspaceHistoryByModule();val r=RuhiDialogueManager().execute(s,renamed,h)
                assertEquals("${p[0]}: ${r.message}",GraphCommandStatus.APPLIED,r.status)
                assertEquals(first,r.semanticFrame!!.parameters["point1"]);assertEquals(second,r.semanticFrame!!.parameters["point2"])
                val expected=if(module==MathModule.Geometry2D)hypot(a.x-b.x,a.y-b.y) else (a-b).magnitude()
                val actual=Regex("= ([0-9.eE+-]+) units").find(r.message)!!.groupValues[1].toDouble()
                assertEquals(expected,actual,1e-10);assertEquals(s.points,r.state.points);assertEquals(s.points3D,r.state.points3D);assertFalse(h.forModule(module).canUndo)
                assertFalse("Negative differences must be parenthesized before squaring",Regex("(?<!\\()-\\d+(?:\\.\\d+)?²").containsMatchIn(r.message))
                checked++
            }
        }
        assertEquals(600,checked)
    }
    @Test fun missingAndCaseAmbiguousPointsAskWithoutInventingEntities() {
        val m=RuhiDialogueManager();val h=WorkspaceHistoryByModule()
        val s=WorkspaceState(module=MathModule.Geometry3D,points3D=listOf(Point3D("a","Alpha7",Vec3(0.0,0.0,0.0))))
        val missing=m.execute(s,"distance from Alpha7 to Beta9",h)
        assertEquals(GraphCommandStatus.CLARIFICATION,missing.status);assertEquals(s.points3D,missing.state.points3D);assertTrue(missing.message.contains("Beta9"));assertFalse(h.forModule(s.module).canUndo)
        val both=s.copy(points3D=s.points3D+Point3D("b","alpha7",Vec3(1.0,2.0,3.0))+Point3D("c","Beta9",Vec3(2.0,0.0,0.0)))
        assertEquals(GraphCommandStatus.CLARIFICATION,m.execute(both,"distance from Alpha7 to Beta9",h).status)
    }
    @Test fun slotFeatureInputsNeverContainThePointNames() {
        val a=RuhiPointSlotInput.tokens("How far is Vertex_907 from Omega_811?")
        val b=RuhiPointSlotInput.tokens("How far is NewPoint5 from OtherPoint6?")
        assertEquals(a.map{it.features},b.map{it.features});assertTrue(a.none{it.features.contains("Vertex")||it.features.contains("Omega")})
        assertEquals(RuhiPointSlotInput.tokens("distance from A to B").map{it.features},RuhiPointSlotInput.tokens("distance from Alpha7 to Beta9").map{it.features})
        assertEquals(RuhiPointSlotInput.tokens("distance from a to b").map{it.features},RuhiPointSlotInput.tokens("distance from Alpha7 to Beta9").map{it.features})
    }
    @Test fun missingPointFollowupCreatesTheRequestedNamedPointThenResumesTheDistance() {
        for(module in listOf(MathModule.Geometry2D,MathModule.Geometry3D)) {
            val m=RuhiDialogueManager();val h=WorkspaceHistoryByModule()
            val initial=WorkspaceState(module=module,points=listOf(Vec2(0.0,0.0)),points3D=listOf(Point3D("a","Alpha7",Vec3(0.0,0.0,0.0))),labSessionValues=mapOf("ruhi.studio.point.Alpha7" to "0"))
            val q=m.execute(initial,"How far is Alpha7 from Beta9?",h);assertEquals(GraphCommandStatus.CLARIFICATION,q.status);assertTrue(q.dialogue!!.pending)
            val choice=m.execute(q.state,q.dialogue!!.suggestions.single().answer,h);assertEquals(GraphCommandStatus.CLARIFICATION,choice.status);assertTrue(choice.message.contains("Where should point Beta9 go"))
            val bad=m.execute(choice.state,if(module==MathModule.Geometry2D)"(abc,4)" else "(abc,4,0)",h);assertEquals(GraphCommandStatus.INVALID,bad.status);assertTrue(bad.dialogue!!.pending)
            val good=m.execute(bad.state,if(module==MathModule.Geometry2D)"(3,4)" else "(3,4,0)",h);assertEquals(good.message,GraphCommandStatus.APPLIED,good.status);assertTrue(good.message.contains("Distance Alpha7 to Beta9 = 5.0 units"));assertFalse(good.dialogue!!.pending)
            if(module==MathModule.Geometry2D){assertEquals(2,good.state.points.size);assertEquals("1",good.state.labSessionValues["ruhi.studio.point.Beta9"])}else{assertEquals(2,good.state.points3D.size);assertEquals("Beta9",good.state.points3D.last().name)}
            val undo=m.execute(good.state,"Undo.",h);assertEquals(GraphCommandStatus.APPLIED,undo.status);if(module==MathModule.Geometry2D)assertEquals(1,undo.state.points.size)else assertEquals(1,undo.state.points3D.size)
        }
    }
    @Test fun standaloneNamedPointRequestWaitsForCoordinatesAndCanBeCancelled() {
        val m=RuhiDialogueManager();val h=WorkspaceHistoryByModule();val s=WorkspaceState(module=MathModule.Geometry3D)
        val q=m.execute(s,"Create point Vertex_907",h);assertEquals(GraphCommandStatus.CLARIFICATION,q.status);assertTrue(q.dialogue!!.pending)
        val answer=m.execute(q.state,"(2,3,4)",h);assertEquals(GraphCommandStatus.APPLIED,answer.status);assertEquals("Vertex_907",answer.state.points3D.single().name);assertEquals(Vec3(2.0,3.0,4.0),answer.state.points3D.single().position)
        val cancel=m.execute(q.state,"Cancel that",h);assertEquals(GraphCommandStatus.APPLIED,cancel.status);assertNull(RuhiDialogueMemory.read(cancel.state).pending);assertTrue(cancel.state.points3D.isEmpty())
    }
}
