package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.workspace.*
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import org.junit.Assert.*
import org.junit.Test

class RuhiCircleLanguageTest {
    @Test fun everyCircleRowPreservesOriginalSlotsAndSurvivesUndoRedoAndSaveLoad() {
        val root=requireNotNull(generateSequence(java.io.File(requireNotNull(System.getProperty("user.dir")))){it.parentFile}.firstOrNull{java.io.File(it,"docs/qa/ruhi-500/replay.tsv").exists()})
        val rows=java.io.File(root,"docs/qa/ruhi-500/replay.tsv").readLines().map{it.split('\t')}.filter{it[2]=="CREATE_CIRCLE"};assertEquals(25,rows.size)
        fun decode(v:String)=String(java.util.Base64.getDecoder().decode(v),Charsets.UTF_8)
        rows.forEach{row->val s=RuhiSeed500Fixture.state(row[3],row[0].toInt());val c=RuhiSeed500Fixture.context(row[0].toInt());val h=WorkspaceHistoryByModule();val m=RuhiDialogueManager();val r=m.execute(s,decode(row[1]),h,c);val check=RuhiSeedBehaviorOracle.check(row[2],RuhiSeedJson.objectValue(decode(row[6])),s,r,c);assertTrue("${row[0]} ${check.diagnostics}",check.verified);assertEquals(1,h.forModule(s.module).protocol.size)
            val restored=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(r.state),recover=false);assertNotNull("${row[0]} ${restored.diagnostics}",restored.state);assertEquals(r.state.shapes,restored.state!!.shapes);assertEquals(r.state.points,restored.state!!.points);assertEquals(r.state.pointDependencies,restored.state!!.pointDependencies)
            val undo=m.execute(r.state,"Undo.",h,r.context);assertEquals("row ${row[0]}",s.shapes,undo.state.shapes);assertEquals(s.points,undo.state.points);assertEquals(s.pointDependencies,undo.state.pointDependencies)
            val redo=m.execute(undo.state,"Redo.",h,r.context);assertEquals(r.state.shapes,redo.state.shapes);assertEquals(r.state.points,redo.state.points)
        }
    }
    @Test fun unseenPointNamesIncludingNumberWordsAndOriginAreEntityReferences() {
        val s=WorkspaceState(module=MathModule.Geometry2D,points=listOf(Vec2(-2.0,1.0),Vec2(4.0,5.0),Vec2(9.0,8.0)),labSessionValues=mapOf("ruhi.studio.point.One" to "0","ruhi.studio.point.Tip_907" to "1","ruhi.studio.point.Origin" to "2"))
        val m=RuhiDialogueManager();val named=m.execute(s,"Circle centered at One passing through Tip_907",WorkspaceHistoryByModule());assertEquals(named.message,GraphCommandStatus.APPLIED,named.status);assertEquals(listOf(0,1),named.state.shapes.single().pointIndices)
        val moved=named.state.copy(points=named.state.points.mapIndexed{i,p->if(i==1)Vec2(1.0,5.0)else p}).recomputed();assertEquals(5.0,RuhiCircleLanguage.describe(moved,moved.shapes.single()).radius,1e-9)
        val explicit=m.execute(s,"Draw a circle centered on point Origin",WorkspaceHistoryByModule());assertEquals(Vec2(9.0,8.0),RuhiCircleLanguage.describe(explicit.state,explicit.state.shapes.single()).center)
        val virtual=m.execute(s,"Draw a radius 7 circle at the origin",WorkspaceHistoryByModule());assertEquals(Vec2(0.0,0.0),RuhiCircleLanguage.describe(virtual.state,virtual.state.shapes.single()).center)
        val diameter=m.execute(s,"Construct a circle with diameter OneTip_907",WorkspaceHistoryByModule());assertEquals(diameter.message,GraphCommandStatus.APPLIED,diameter.status);val changed=diameter.state.copy(points=diameter.state.points.mapIndexed{i,p->if(i==1)Vec2(2.0,1.0)else p}).recomputed();val d=RuhiCircleLanguage.describe(changed,changed.shapes.single());assertEquals(Vec2(0.0,1.0),d.center);assertEquals(2.0,d.radius,1e-9)
    }
    @Test fun triangleCirclesAndAxisTangencyFollowTheirActualParentCoordinates() {
        val s=WorkspaceState(module=MathModule.Geometry2D,points=listOf(Vec2(0.0,0.0),Vec2(4.0,0.0),Vec2(0.0,3.0)),shapes=listOf(Shape2D("tri",Shape2DType.Triangle,listOf(0,1,2),"Tri_907")),labSessionValues=mapOf("ruhi.studio.point.Start_907" to "0","ruhi.studio.point.Finish_811" to "1","ruhi.studio.point.Tip_662" to "2"))
        val m=RuhiDialogueManager();val red=m.execute(WorkspaceState(module=MathModule.Geometry2D),"Add a red circle centered at (0,0)",WorkspaceHistoryByModule());assertEquals("C1",red.state.shapes.single().name);val circum=m.execute(s,"Show the circumcircle of triangle Tri_907",WorkspaceHistoryByModule());assertEquals(circum.message,GraphCommandStatus.APPLIED,circum.status);var d=RuhiCircleLanguage.describe(circum.state,circum.state.shapes.last());assertEquals(Vec2(2.0,1.5),d.center);assertEquals(2.5,d.radius,1e-9)
        val inCircle=m.execute(s,"Construct the incircle of triangle Tri_907",WorkspaceHistoryByModule());assertEquals(inCircle.message,GraphCommandStatus.APPLIED,inCircle.status);d=RuhiCircleLanguage.describe(inCircle.state,inCircle.state.shapes.last());assertEquals(Vec2(1.0,1.0),d.center);assertEquals(1.0,d.radius,1e-9)
        val moved=inCircle.state.copy(points=inCircle.state.points.mapIndexed{i,p->if(i==2)Vec2(0.0,6.0)else p}).recomputed();val inc=RuhiCircleLanguage.describe(moved,moved.shapes.last());assertEquals(inc.center.x,inc.radius,1e-9);assertEquals(inc.center.y,inc.radius,1e-9)
        val tangent=m.execute(WorkspaceState(module=MathModule.Geometry2D),"Draw a circle tangent to the x-axis centered at (3,2)",WorkspaceHistoryByModule());assertEquals(tangent.message,GraphCommandStatus.APPLIED,tangent.status)
        val controls=tangent.state.shapes.single().pointIndices;val changed=tangent.state.copy(points=tangent.state.points.mapIndexed{i,p->if(i==controls[0])Vec2(3.0,4.0)else p}).recomputed();assertEquals(4.0,RuhiCircleLanguage.describe(changed,changed.shapes.single()).radius,1e-9)
    }
    @Test fun relatedCirclesKeepTheirRelationsAfterTheirParentRadiusChanges() {
        val m=RuhiDialogueManager();val s=WorkspaceState(module=MathModule.Geometry2D)
        for(text in listOf("Create two circles that intersect","Draw two externally tangent circles")) {
            val r=m.execute(s,text,WorkspaceHistoryByModule());assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);val controls=r.state.shapes.first().pointIndices
            val moved=r.state.copy(points=r.state.points.mapIndexed{i,p->if(i==controls[1])Vec2(3.0,0.0)else p}).recomputed();val a=RuhiCircleLanguage.describe(moved,moved.shapes[0]);val b=RuhiCircleLanguage.describe(moved,moved.shapes[1]);assertEquals(3.0,a.radius,1e-9);assertEquals(3.0,b.radius,1e-9)
            val distance=a.center.distanceTo(b.center);if(text.contains("tangent"))assertEquals(6.0,distance,1e-9)else assertTrue(distance>0&&distance<6)
        }
        val parent=m.execute(s,"Draw a circle",WorkspaceHistoryByModule());val copied=m.execute(parent.state,"Make a concentric circle with twice the radius",WorkspaceHistoryByModule(),parent.context);assertEquals(copied.message,GraphCommandStatus.APPLIED,copied.status);val controls=parent.state.shapes.single().pointIndices
        val moved=copied.state.copy(points=copied.state.points.mapIndexed{i,p->if(i==controls[1])Vec2(2.0,0.0)else p}).recomputed();assertEquals(4.0,RuhiCircleLanguage.describe(moved,moved.shapes.last()).radius,1e-9)
    }
    @Test fun missingNamedCenterResumesFromCoordinatesAndOneUndoRemovesTheWholeAnswer() {
        val m=RuhiDialogueManager();val h=WorkspaceHistoryByModule();val s=WorkspaceState(module=MathModule.Geometry2D)
        val q=m.execute(s,"Draw a circle centered on point Anchor_907",h);assertEquals(GraphCommandStatus.CLARIFICATION,q.status);assertTrue(q.dialogue!!.pending);assertEquals("Create point Anchor_907",q.dialogue!!.suggestions.single().label)
        val r=m.execute(q.state,"(2,3)",h);assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);val d=RuhiCircleLanguage.describe(r.state,r.state.shapes.single());assertEquals(Vec2(2.0,3.0),d.center);assertEquals(1.0,d.radius,1e-9);assertNull(RuhiDialogueMemory.read(r.state).pending);assertEquals(1,h.forModule(s.module).protocol.size)
        val undo=m.execute(r.state,"Undo.",h);assertTrue(undo.state.points.isEmpty());assertTrue(undo.state.shapes.isEmpty())
    }
    @Test fun invalidDimensionsConflictingConstraintsAndDegenerateTrianglesNeverMutateTheScene() {
        val m=RuhiDialogueManager();val s=RuhiSeed500Fixture.state("GEOMETRY_2D")
        for(text in listOf("Create a circle of radius -2","Create a circle of radius 0","Create a circle of radius infinity","Create a circle of radius 501","Place a circle at (abc,2) with radius 3","Create a circle with radius 2 and diameter 8","Add a unit circle with radius 2","Construct the circle 2x^2+y^2=16")) {
            val r=m.execute(s,text,WorkspaceHistoryByModule());assertEquals("$text: ${r.message}",GraphCommandStatus.INVALID,r.status);assertEquals(s.points,r.state.points);assertEquals(s.shapes,r.state.shapes);assertEquals(s.pointDependencies,r.state.pointDependencies)
        }
        val flat=s.copy(points=s.points.mapIndexed{i,p->if(i in 0..2)Vec2(i.toDouble(),0.0)else p});val r=m.execute(flat,"Create a circle passing through A, B, and C",WorkspaceHistoryByModule());assertEquals(GraphCommandStatus.INVALID,r.status);assertEquals(flat.shapes,r.state.shapes)
        val question=m.execute(s,"What is the radius of the circle?",WorkspaceHistoryByModule());assertEquals(s.shapes,question.state.shapes)
    }
}
