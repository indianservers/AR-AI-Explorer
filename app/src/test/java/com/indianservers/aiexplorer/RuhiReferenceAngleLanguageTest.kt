package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.workspace.*
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import org.junit.Assert.*
import org.junit.Test

class RuhiReferenceAngleLanguageTest {
    private fun scene()=WorkspaceState(module=MathModule.Geometry2D,points=listOf(Vec2(-4.0,0.0),Vec2(4.0,0.0),Vec2(0.0,-4.0),Vec2(0.0,4.0)),shapes=listOf(Shape2D("one",Shape2DType.Line,listOf(0,1),"Track_907"),Shape2D("two",Shape2DType.Line,listOf(2,3),"Track_811")),labSessionValues=mapOf("ruhi.studio.point.Start_907" to "0","ruhi.studio.point.End_811" to "1","ruhi.studio.point.Low_662" to "2","ruhi.studio.point.Top_553" to "3"))
    @Test fun everyNewSourceRowMatchesItsOriginalSlotsAndIndependentGeometry() {
        val root=requireNotNull(generateSequence(java.io.File(requireNotNull(System.getProperty("user.dir")))){it.parentFile}.firstOrNull{java.io.File(it,"docs/qa/ruhi-500/replay.tsv").exists()})
        val ids=setOf(205,211,213,219,225);val rows=java.io.File(root,"docs/qa/ruhi-500/replay.tsv").readLines().map{it.split('\t')}.filter{it[0].toInt() in ids};assertEquals(5,rows.size)
        fun decode(v:String)=String(java.util.Base64.getDecoder().decode(v),Charsets.UTF_8)
        rows.forEach{row->val s=RuhiSeed500Fixture.state(row[3],row[0].toInt());val c=RuhiSeed500Fixture.context(row[0].toInt());val h=WorkspaceHistoryByModule();val r=RuhiDialogueManager().execute(s,decode(row[1]),h,c);val check=RuhiSeedBehaviorOracle.check(row[2],RuhiSeedJson.objectValue(decode(row[6])),s,r,c);assertTrue("${row[0]} ${check.diagnostics}",check.verified);assertEquals(1,h.forModule(s.module).protocol.size)}
    }
    @Test fun intersectionMarksUseUnseenLineNamesAndRecomputeAnchorAfterAnEndpointMoves() {
        val s=scene();val h=WorkspaceHistoryByModule();val m=RuhiDialogueManager();val r=m.execute(s,"Show the angle between intersecting lines Track_907 and Track_811",h)
        assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertTrue(r.message.contains("90.0 degrees"));assertEquals(s.points,r.state.points)
        val mark=RuhiReferenceAngleLanguage.marks(r.state).single();assertEquals(Vec2(0.0,0.0),RuhiReferenceAngleLanguage.measure(r.state,mark).arms[1])
        val moved=r.state.copy(points=r.state.points.mapIndexed{i,p->if(i==3)Vec2(4.0,4.0)else p});val current=RuhiReferenceAngleLanguage.measure(moved,mark)
        assertEquals(2.0,current.arms[1].x,1e-10);assertEquals(0.0,current.arms[1].y,1e-10);assertEquals(63.43494882292201,current.degrees,1e-9)
        val restored=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(moved)).state!!;assertEquals(mark,RuhiReferenceAngleLanguage.marks(restored).single());assertEquals(current,RuhiReferenceAngleLanguage.measure(restored,mark))
        val undo=m.execute(r.state,"Undo.",h);assertTrue(RuhiReferenceAngleLanguage.marks(undo.state).isEmpty());val redo=m.execute(undo.state,"Redo.",h);assertEquals(mark,RuhiReferenceAngleLanguage.marks(redo.state).single())
        val remapped=StudioReferenceMetadata.remapPoints(r.state.labSessionValues,mapOf(0 to 3,1 to 2,2 to 1,3 to 0));assertEquals(listOf(3,2,1,0),RuhiReferenceAngleLanguage.marks(s.copy(labSessionValues=remapped)).single().indices)
        assertTrue(StudioReferenceMetadata.remapPoints(r.state.labSessionValues,mapOf(0 to 0,1 to 1,2 to 2)).keys.none{it.startsWith(RuhiReferenceAngleLanguage.PREFIX)})
    }
    @Test fun originAndAxesUseVirtualReferencesWithoutCreatingOrOverwritingPoints() {
        val s=WorkspaceState(module=MathModule.Geometry2D,points=listOf(Vec2(2.0,0.0),Vec2(0.0,3.0),Vec2(99.0,99.0)),labSessionValues=mapOf("ruhi.studio.point.Arm_907" to "0","ruhi.studio.point.Arm_811" to "1","ruhi.studio.point.O" to "2"))
        val m=RuhiDialogueManager();val origin=m.execute(s,"Show the angle at origin between Arm_907 and Arm_811",WorkspaceHistoryByModule());assertEquals(origin.message,GraphCommandStatus.APPLIED,origin.status);assertTrue(origin.message.contains("90.0 degrees"));assertEquals(s.points,origin.state.points);assertEquals(Vec2(0.0,0.0),RuhiReferenceAngleLanguage.measure(origin.state,RuhiReferenceAngleLanguage.marks(origin.state).single()).arms[1])
        val axis=m.execute(s,"Find angle between the x-axis and Arm_907Arm_811",WorkspaceHistoryByModule());assertEquals(axis.message,GraphCommandStatus.APPLIED,axis.status);assertEquals(123.69006752597979,RuhiReferenceAngleLanguage.measure(axis.state,RuhiReferenceAngleLanguage.marks(axis.state).single()).degrees,1e-9)
        val vectors=s.copy(shapes=listOf(Shape2D("u",Shape2DType.Vector,listOf(0,1),"Arrow_907"),Shape2D("v",Shape2DType.Vector,listOf(1,2),"Arrow_811")))
        val r=m.execute(vectors,"Calculate the angle formed by vectors Arrow_907 and Arrow_811",WorkspaceHistoryByModule());assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertEquals(vectors.points,r.state.points);assertEquals(vectors.shapes,r.state.shapes)
    }
    @Test fun invalidOrMissingReferencesLeaveAllGeometryAndAnnotationsAlone() {
        val m=RuhiDialogueManager();val s=scene();val parallel=s.copy(points=listOf(Vec2(0.0,0.0),Vec2(4.0,0.0),Vec2(0.0,2.0),Vec2(4.0,2.0)))
        val invalid=m.execute(parallel,"Show the angle between intersecting lines Track_907 and Track_811",WorkspaceHistoryByModule());assertEquals(GraphCommandStatus.INVALID,invalid.status);assertEquals(parallel.points,invalid.state.points);assertTrue(RuhiReferenceAngleLanguage.marks(invalid.state).isEmpty())
        val absent=m.execute(s,"Show the angle between intersecting lines Unknown_111 and Track_811",WorkspaceHistoryByModule());assertEquals(GraphCommandStatus.CLARIFICATION,absent.status);assertEquals(s.shapes,absent.state.shapes)
        val zero=s.copy(points=s.points.mapIndexed{i,p->if(i==1)s.points[0]else p});val q=m.execute(zero,"Find angle between the x-axis and Start_907End_811",WorkspaceHistoryByModule());assertEquals(GraphCommandStatus.INVALID,q.status);assertTrue(RuhiReferenceAngleLanguage.marks(q.state).isEmpty())
        val negative=m.execute(s,"Do not show the angle between intersecting lines Track_907 and Track_811",WorkspaceHistoryByModule());assertTrue(RuhiReferenceAngleLanguage.marks(negative.state).isEmpty())
    }
}
