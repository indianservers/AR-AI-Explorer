package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.workspace.*
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import org.junit.Assert.*
import org.junit.Test

class RuhiAngleLanguageTest {
    @Test fun selectedVertexQuestionResumesFromAChipAfterSaveLoadAndHasOneUndoEntry() {
        val s=scene();val m=RuhiDialogueManager();val h=WorkspaceHistoryByModule();val c=RuhiSceneContext(selectedShapeId="tri")
        val q=m.execute(s,"Is this angle acute or obtuse?",h,c)
        assertEquals(q.message,GraphCommandStatus.CLARIFICATION,q.status);assertTrue(q.dialogue!!.pending);assertEquals(3,q.dialogue!!.suggestions.size);assertEquals(0,h.forModule(s.module).protocol.size)
        val restored=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(q.state)).state!!
        assertEquals(q.dialogue!!.suggestions,m.restoredView(restored)!!.suggestions)
        val resume=m.execute(restored,"Resume",h);assertTrue(resume.dialogue!!.pending)
        val r=m.execute(resume.state,"Use vertex B",h);assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertTrue(r.message.contains("90.0 degrees (right)"));assertNull(RuhiDialogueMemory.read(r.state).pending);assertEquals(1,h.forModule(s.module).protocol.size)
        val undo=m.execute(r.state,"Undo.",h);assertTrue(RuhiAngleLanguage.marks(undo.state).isEmpty());assertEquals(s.points,undo.state.points)
    }
    @Test fun staleVertexAnswersAndCancellationNeverApplyAnnotations() {
        val s=scene();val m=RuhiDialogueManager();val h=WorkspaceHistoryByModule();val c=RuhiSceneContext(selectedShapeId="tri")
        val q=m.execute(s,"Measure the selected corner of the polygon",h,c)
        val changed=q.state.copy(points=q.state.points.mapIndexed{i,p->if(i==0)Vec2(5.0,0.0)else p})
        val r=m.execute(changed,"B",h);assertEquals(GraphCommandStatus.CLARIFICATION,r.status);assertTrue(r.message.contains("scene changed"));assertNull(RuhiDialogueMemory.read(r.state).pending);assertTrue(RuhiAngleLanguage.marks(r.state).isEmpty())
        val cancel=m.execute(q.state,"Never mind.",h);assertNull(RuhiDialogueMemory.read(cancel.state).pending);assertTrue(RuhiAngleLanguage.marks(cancel.state).isEmpty())
    }
    @Test fun concavePolygonMarksUseTheInteriorAndRemainLiveWhenConcavityChanges() {
        val p=listOf(Vec2(0.0,0.0),Vec2(4.0,0.0),Vec2(1.0,1.0),Vec2(0.0,4.0))
        val s=WorkspaceState(module=MathModule.Geometry2D,points=p,shapes=listOf(Shape2D("quad",Shape2DType.Polygon,listOf(0,1,2,3))))
        val m=RuhiDialogueManager();val r=m.execute(s,"Measure the selected corner of the polygon",WorkspaceHistoryByModule(),RuhiSceneContext(selectedShapeId="quad",selectedPointIndex=2))
        assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);val mark=RuhiAngleLanguage.marks(r.state).single();assertTrue(RuhiAngleLanguage.markDegrees(r.state,mark)>180)
        assertEquals(360.0,RuhiAngleLanguage.polygonAngles(r.state,listOf(0,1,2,3)).sum(),1e-8)
        val moved=r.state.copy(points=listOf(p[0],p[1],Vec2(4.0,4.0),p[3]));assertEquals(90.0,RuhiAngleLanguage.markDegrees(moved,mark),1e-8)
        val compacted=StudioReferenceMetadata.remapPoints(r.state.labSessionValues,mapOf(0 to 3,1 to 0,2 to 1,3 to 2));assertTrue(compacted.keys.any{it.startsWith(RuhiAngleLanguage.PREFIX)})
        val removed=StudioReferenceMetadata.remapPoints(r.state.labSessionValues,mapOf(0 to 0,1 to 1,2 to 2));assertTrue(removed.keys.none{it.startsWith(RuhiAngleLanguage.PREFIX)})
        val crossed=s.copy(points=listOf(Vec2(0.0,0.0),Vec2(4.0,4.0),Vec2(0.0,4.0),Vec2(4.0,0.0)))
        val invalid=m.execute(crossed,"Label the smallest corner of the quadrilateral",WorkspaceHistoryByModule(),RuhiSceneContext(selectedShapeId="quad"));assertEquals(GraphCommandStatus.INVALID,invalid.status);assertTrue(RuhiAngleLanguage.marks(invalid.state).isEmpty())
    }
    @Test fun nativeUnlabelledPointsCanBeReferencedByTheirDisplayedPointNames() {
        val s=scene().copy(labSessionValues=emptyMap());val r=RuhiDialogueManager().execute(s,"Measure angle P1, P2, P3",WorkspaceHistoryByModule())
        assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertTrue(r.message.contains("90.0 degrees"))
        val collision=s.copy(labSessionValues=mapOf("ruhi.studio.point.P1" to "1"));val q=RuhiDialogueManager().execute(collision,"Measure angle P1, P2, P3",WorkspaceHistoryByModule());assertEquals(GraphCommandStatus.CLARIFICATION,q.status);assertTrue(RuhiAngleLanguage.marks(q.state).isEmpty())
    }
    @Test fun namedWorkbookAnglesSatisfyOriginalSlotsAndIndependentOracle() {
        val root=requireNotNull(generateSequence(java.io.File(requireNotNull(System.getProperty("user.dir")))){it.parentFile}.firstOrNull{java.io.File(it,"docs/qa/ruhi-500/replay.tsv").exists()})
        val ids=setOf(201,202,203,204,206,207,208,209,210,212,214,215,216,217,218,222,223,224)
        val rows=java.io.File(root,"docs/qa/ruhi-500/replay.tsv").readLines().map{it.split('\t')}.filter{it[0].toInt() in ids};assertEquals(18,rows.size)
        fun decode(v:String)=String(java.util.Base64.getDecoder().decode(v),Charsets.UTF_8)
        rows.forEach{row->val s=RuhiSeed500Fixture.state(row[3],row[0].toInt());val c=RuhiSeed500Fixture.context(row[0].toInt());val r=RuhiDialogueManager().execute(s,decode(row[1]),WorkspaceHistoryByModule(),c);val check=RuhiSeedBehaviorOracle.check(row[2],RuhiSeedJson.objectValue(decode(row[6])),s,r,c);assertTrue("${row[0]} ${check.diagnostics}",check.verified)}
    }
    @Test fun arbitraryRayNamesAndCounterclockwiseOrientationUseGeometryNotPointLetters() {
        val s=scene().copy(labSessionValues=mapOf("ruhi.studio.point.End_907" to "0","ruhi.studio.point.Joint_811" to "1","ruhi.studio.point.Tip_662" to "2"))
        val manager=RuhiDialogueManager()
        val r=manager.execute(s,"Angle from Joint_811Tip_662 to Joint_811End_907 counterclockwise",WorkspaceHistoryByModule())
        assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertTrue(r.message,r.message.contains("270.0 degrees"));assertTrue(RuhiAngleLanguage.marks(r.state).single().oriented)
        val reversed=s.copy(points=listOf(Vec2(-4.0,0.0),Vec2(0.0,0.0),Vec2(0.0,3.0)))
        val q=manager.execute(reversed,"Angle from Joint_811Tip_662 to Joint_811End_907 counterclockwise",WorkspaceHistoryByModule())
        assertTrue(q.message,q.message.contains("90.0 degrees"))
        val bad=manager.execute(s,"Find the angle between Joint_811End_907 and End_907Tip_662",WorkspaceHistoryByModule())
        assertEquals(GraphCommandStatus.CLARIFICATION,bad.status);assertTrue(RuhiAngleLanguage.marks(bad.state).isEmpty())
    }
    @Test fun bisectorUsesLiveParentReferencesAndHasOneUndoableConstruction() {
        val s=scene();val manager=RuhiDialogueManager();val h=WorkspaceHistoryByModule()
        val r=manager.execute(s,"Show the angle bisector of ∠ABC",h)
        assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);val shape=r.state.shapes.last();assertEquals(Shape2DType.AngleBisector,shape.type);assertEquals(listOf(0,1,2),shape.pointIndices);assertEquals(s.points,r.state.points)
        val restored=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(r.state)).state!!;assertEquals(r.state.shapes,restored.shapes)
        val undo=manager.execute(r.state,"Undo.",h);assertEquals(s.shapes,undo.state.shapes);assertTrue(RuhiAngleLanguage.marks(undo.state).isEmpty())
        val redo=manager.execute(undo.state,"Redo.",h);assertEquals(r.state.shapes,redo.state.shapes)
        val straight=s.copy(points=listOf(Vec2(-1.0,0.0),Vec2(0.0,0.0),Vec2(1.0,0.0)))
        val invalid=manager.execute(straight,"Show the angle bisector of ∠ABC",WorkspaceHistoryByModule());assertEquals(GraphCommandStatus.INVALID,invalid.status);assertEquals(straight.shapes,invalid.state.shapes)
    }
    @Test fun vertexInferenceAsksInsteadOfGuessingWhenSeveralPolygonsContainTheVertex() {
        val s=scene().copy(shapes=scene().shapes+Shape2D("other",Shape2DType.Triangle,listOf(0,1,2),"Other"));val m=RuhiDialogueManager()
        val r=m.execute(s,"How many degrees is the angle at C?",WorkspaceHistoryByModule());assertEquals(GraphCommandStatus.CLARIFICATION,r.status)
        val selected=m.execute(s,"How many degrees is the angle at C?",WorkspaceHistoryByModule(),RuhiSceneContext(selectedShapeId="tri"));assertEquals(selected.message,GraphCommandStatus.APPLIED,selected.status)
    }
    private fun scene()=WorkspaceState(module=MathModule.Geometry2D,points=listOf(Vec2(4.0,0.0),Vec2(0.0,0.0),Vec2(0.0,3.0)),shapes=listOf(Shape2D("tri",Shape2DType.Triangle,listOf(0,1,2),"ABC")),labSessionValues=mapOf("ruhi.studio.point.A" to "0","ruhi.studio.point.B" to "1","ruhi.studio.point.C" to "2"))
    @Test fun arbitraryNamesUseCurrentCoordinatesAndAnnotationsSurviveHistoryAndPersistence() {
        val s=scene().copy(labSessionValues=mapOf("ruhi.studio.point.Start_907" to "0","ruhi.studio.point.Corner_811" to "1","ruhi.studio.point.Finish_662" to "2"));val h=WorkspaceHistoryByModule();val manager=RuhiDialogueManager()
        val r=manager.execute(s,"Measure angle Start_907, Corner_811, Finish_662",h)
        assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertTrue(r.message.contains("90.0 degrees"));assertEquals(s.points,r.state.points);assertEquals(s.shapes,r.state.shapes)
        assertEquals(listOf(0,1,2),RuhiAngleLanguage.marks(r.state).single().indices)
        val restored=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(r.state)).state!!;assertEquals(RuhiAngleLanguage.marks(r.state),RuhiAngleLanguage.marks(restored))
        val moved=r.state.copy(points=listOf(Vec2(3.0,3.0),Vec2(0.0,0.0),Vec2(0.0,3.0)))
        assertEquals(45.0,RuhiAngleLanguage.degrees(moved.points[0],moved.points[1],moved.points[2]),1e-10)
        val undo=manager.execute(r.state,"Undo.",h);assertTrue(RuhiAngleLanguage.marks(undo.state).isEmpty())
        val redo=manager.execute(undo.state,"Redo.",h);assertEquals(RuhiAngleLanguage.marks(r.state),RuhiAngleLanguage.marks(redo.state))
    }
    @Test fun notationVertexAndExtremaSelectTheActualCorner() {
        val s=scene();val m=RuhiDialogueManager();val context=RuhiSceneContext(selectedShapeId="tri")
        for(text in listOf("Measure angle ABC","Show ∠ABC in degrees","What is the angle at B in triangle ABC?","Label the largest angle in triangle ABC")) {
            val r=m.execute(s,text,WorkspaceHistoryByModule(),context);assertEquals(text+": "+r.message,GraphCommandStatus.APPLIED,r.status);assertTrue(r.message.contains("90.0 degrees"));assertEquals(1,RuhiAngleLanguage.marks(r.state).single().indices[1])
        }
        val r=m.execute(s,"Display all angles inside triangle ABC",WorkspaceHistoryByModule(),context)
        assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertEquals(3,RuhiAngleLanguage.marks(r.state).size)
        val values=Regex("= ([0-9.eE+-]+) degrees").findAll(r.message).map{it.groupValues[1].toDouble()}.toList();assertEquals(180.0,values.sum(),1e-10)
        val exterior=m.execute(s,"Calculate the exterior angle at vertex A",WorkspaceHistoryByModule(),context);assertTrue(exterior.message,RuhiAngleLanguage.marks(exterior.state).single().exterior)
    }
    @Test fun missingAmbiguousAndDegenerateReferencesDoNotChangeGeometry() {
        val m=RuhiDialogueManager();val s=scene()
        for(text in listOf("Measure angle ABD","Measure angle ABCD","Measure the selected corner of the polygon")) {
            val r=m.execute(s,text,WorkspaceHistoryByModule());assertEquals(text,GraphCommandStatus.CLARIFICATION,r.status);assertEquals(s.points,r.state.points);assertTrue(RuhiAngleLanguage.marks(r.state).isEmpty())
        }
        val coincident=s.copy(points=listOf(Vec2(0.0,0.0),Vec2(0.0,0.0),Vec2(1.0,0.0)))
        val r=m.execute(coincident,"Measure angle ABC",WorkspaceHistoryByModule());assertEquals(GraphCommandStatus.INVALID,r.status);assertTrue(RuhiAngleLanguage.marks(r.state).isEmpty())
        val no=m.execute(s,"Do not measure angle ABC",WorkspaceHistoryByModule());assertTrue(RuhiAngleLanguage.marks(no.state).isEmpty())
    }
    @Test fun deletingParentsRemapsOrRemovesAngleMarks() {
        val s=scene();val r=RuhiDialogueManager().execute(s,"Measure angle ABC",WorkspaceHistoryByModule())
        val remapped=StudioReferenceMetadata.remapPoints(r.state.labSessionValues,mapOf(0 to 2,1 to 0,2 to 1))
        assertEquals(listOf(2,0,1),RuhiAngleLanguage.marks(s.copy(labSessionValues=remapped)).single().indices)
        assertTrue(StudioReferenceMetadata.remapPoints(r.state.labSessionValues,mapOf(0 to 0,2 to 1)).keys.none{it.startsWith(RuhiAngleLanguage.PREFIX)})
    }
}
