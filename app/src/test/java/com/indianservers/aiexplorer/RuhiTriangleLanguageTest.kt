package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.workspace.*
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class RuhiTriangleLanguageTest {
    private val m get()=RuhiDialogueManager()
    @Test fun allOriginalTriangleRowsMeetTheirUnchangedConstraintsAndPersistWithUndoRedo() {
        val root=requireNotNull(generateSequence(java.io.File(requireNotNull(System.getProperty("user.dir")))){it.parentFile}.firstOrNull{java.io.File(it,"docs/qa/ruhi-500/replay.tsv").exists()})
        val rows=java.io.File(root,"docs/qa/ruhi-500/replay.tsv").readLines().map{it.split('\t')}.filter{it[2]=="CREATE_TRIANGLE"};assertEquals(25,rows.size)
        fun decode(v:String)=String(java.util.Base64.getDecoder().decode(v),Charsets.UTF_8)
        rows.forEach{row->val s=RuhiSeed500Fixture.state(row[3],row[0].toInt());val c=RuhiSeed500Fixture.context(row[0].toInt());val h=WorkspaceHistoryByModule();val manager=m;val r=manager.execute(s,decode(row[1]),h,c);val check=RuhiSeedBehaviorOracle.check(row[2],RuhiSeedJson.objectValue(decode(row[6])),s,r,c);assertTrue("${row[0]} ${check.diagnostics}",check.verified);assertEquals(1,h.forModule(s.module).protocol.size)
            val saved=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(r.state),recover=false);assertNotNull(saved.state);assertEquals(r.state.points,saved.state!!.points);assertEquals(r.state.pointDependencies,saved.state!!.pointDependencies);assertEquals(r.state.shapes,saved.state!!.shapes)
            val undo=manager.execute(r.state,"Undo.",h,r.context);assertEquals(s.points,undo.state.points);assertEquals(s.shapes,undo.state.shapes);val redo=manager.execute(undo.state,"Redo.",h,undo.context);assertEquals(r.state.shapes,redo.state.shapes);assertEquals(r.state.points,redo.state.points)
        }
    }
    @Test fun arbitraryLabelsUseExistingIndicesAndUpdatedCoordinates() {
        val s=WorkspaceState(module=MathModule.Geometry2D,points=listOf(Vec2(-2.0,1.0),Vec2(4.0,1.0),Vec2(1.0,5.0)),labSessionValues=mapOf("ruhi.studio.point.One" to "0","ruhi.studio.point.Edge_811" to "1","ruhi.studio.point.Tip_662" to "2"))
        val r=m.execute(s,"Create the triangle defined by points One, Edge_811, and Tip_662",WorkspaceHistoryByModule());assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertEquals(listOf(0,1,2),r.state.shapes.single().pointIndices);assertEquals(s.points,r.state.points)
        val changed=r.state.copy(points=r.state.points.mapIndexed{i,p->if(i==2)Vec2(1.0,9.0)else p}).recomputed();assertEquals(Vec2(1.0,9.0),changed.points[changed.shapes.single().pointIndices[2]])
        val angle=m.execute(s,"Make a triangle with a 42-degree angle at One",WorkspaceHistoryByModule());assertEquals(angle.message,GraphCommandStatus.APPLIED,angle.status);val shape=angle.state.shapes.single();assertEquals(0,shape.pointIndices.first());val edited=angle.state.copy(points=angle.state.points.mapIndexed{i,p->if(i==0)Vec2(-3.0,2.0)else p}).recomputed();assertEquals(42.0,RuhiTriangleLanguage.measures(shape.pointIndices.map{edited.points[it]}).second[0],1e-8)
    }
    @Test fun selectedEquilateralAndSimilarCopiesFollowTheirParentsAndKeepUnrelatedObjects() {
        val s=RuhiSeed500Fixture.state("GEOMETRY_2D");val c=RuhiSceneContext(selectedShapeId="triangle-abc")
        val r=m.execute(s,"Make the current triangle equilateral",WorkspaceHistoryByModule(),c);assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertEquals(s.points,r.state.points.take(s.points.size));assertEquals(s.shapes.first(),r.state.shapes.first());val shape=r.state.shapes.last();assertEquals("triangle-abc",shape.id)
        val changed=r.state.copy(points=r.state.points.mapIndexed{i,p->if(i==1)Vec2(3.0,2.0)else p}).recomputed();val lengths=RuhiTriangleLanguage.measures(shape.pointIndices.map{changed.points[it]}).first;assertTrue(lengths.all{abs(it-lengths[0])<1e-8})
        for(command in listOf("Construct a second triangle congruent to the first","Draw a triangle similar to ABC at scale 2")) {val copy=m.execute(s,command,WorkspaceHistoryByModule());assertEquals(copy.message,GraphCommandStatus.APPLIED,copy.status);val moved=copy.state.copy(points=copy.state.points.mapIndexed{i,p->if(i==2)Vec2(5.0,6.0)else p}).recomputed();val parent=moved.shapes[1].pointIndices.map{moved.points[it]};val child=moved.shapes.last().pointIndices.map{moved.points[it]};val factor=if(command.contains("similar"))2.0 else 1.0;assertTrue(parent.indices.all{abs(parent[it].distanceTo(parent[(it+1)%3])*factor-child[it].distanceTo(child[(it+1)%3]))<1e-8})}
    }
    @Test fun missingVertexAsksOneEssentialQuestionAndResumesAtomically() {
        val manager=m;val h=WorkspaceHistoryByModule();val s=WorkspaceState(module=MathModule.Geometry2D,points=listOf(Vec2(0.0,0.0),Vec2(4.0,0.0)),labSessionValues=mapOf("ruhi.studio.point.Start_907" to "0","ruhi.studio.point.Finish_811" to "1"))
        val q=manager.execute(s,"Create the triangle defined by points Start_907, Finish_811, and Tip_662",h);assertEquals(GraphCommandStatus.CLARIFICATION,q.status);assertEquals("Create point Tip_662",q.dialogue!!.suggestions.single().label)
        val r=manager.execute(q.state,"(2,3)",h);assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertEquals(listOf(0,1,2),r.state.shapes.single().pointIndices);assertNull(RuhiDialogueMemory.read(r.state).pending);assertEquals(1,h.forModule(s.module).protocol.size);val undo=manager.execute(r.state,"Undo.",h);assertEquals(s.points,undo.state.points);assertEquals(s.shapes,undo.state.shapes)
    }
    @Test fun invalidTrianglesDoNotMutateAnySceneOrHistory() {
        val s=RuhiSeed500Fixture.state("GEOMETRY_2D")
        for(text in listOf("Make a triangle with sides 1, 2, and 3","Make a triangle with sides 3, 4, and 12","Make a right triangle with hypotenuse 4 and one leg 5","Make an equilateral triangle of side -5","Show a triangle with base 8 and height 0","Make a triangle with interior angles 30, 60 and 100 degrees","Construct a triangle using A=(0,0), B=(1,1), C=(2,2)")) {val h=WorkspaceHistoryByModule();val r=m.execute(s,text,h);assertEquals("$text ${r.message}",GraphCommandStatus.INVALID,r.status);assertEquals(s.points,r.state.points);assertEquals(s.shapes,r.state.shapes);assertTrue(h.forModule(s.module).protocol.isEmpty())}
        val q=m.execute(s,"Make the current triangle equilateral",WorkspaceHistoryByModule());assertEquals(GraphCommandStatus.CLARIFICATION,q.status);assertEquals(s.shapes,q.state.shapes)
    }
    @Test fun explicitCoordinatesRespectLabelsAndOrdinaryQuestionsDoNotConstructTriangles() {
        val s=WorkspaceState(module=MathModule.Geometry2D);val r=m.execute(s,"Create triangle MoonSunStar with Moon(1,1), Sun(7,1), Star(4,6)",WorkspaceHistoryByModule());assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertEquals(listOf(Vec2(1.0,1.0),Vec2(7.0,1.0),Vec2(4.0,6.0)),r.state.points);assertEquals("0",r.state.labSessionValues["ruhi.studio.point.Moon"])
        val follow=m.execute(r.state,"Show the angles of the triangle",WorkspaceHistoryByModule(),r.context);assertEquals(r.state.shapes,follow.state.shapes)
        val q=m.execute(r.state,"What are the angles of this triangle?",WorkspaceHistoryByModule(),r.context);assertEquals(r.state.shapes,q.state.shapes)
    }
}
