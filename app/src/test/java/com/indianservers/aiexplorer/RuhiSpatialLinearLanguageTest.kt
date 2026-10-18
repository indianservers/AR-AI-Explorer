package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.workspace.*
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import org.junit.Assert.*
import org.junit.Test

class RuhiSpatialLinearLanguageTest {
    private fun scene() = WorkspaceState(module=MathModule.Geometry3D,points3D=listOf(
        Point3D("a-907","Anchor_907",Vec3(-2.0,1.0,3.0)),
        Point3D("b-811","Beta_811",Vec3(4.0,5.0,-1.0)),
        Point3D("c-662","Gamma_662",Vec3(1.0,-2.0,2.0))))
    private fun run(s:WorkspaceState,text:String)=RuhiDialogueManager().execute(s,text,WorkspaceHistoryByModule())
    @Test fun lineThroughArbitraryNamedPointsIsApplied() {
        val r=run(scene(),"Draw a line from Anchor_907 to Beta_811.")
        assertEquals(r.message,GraphCommandStatus.APPLIED,r.status)
    }
    @Test fun segmentBetweenArbitraryNamedPointsIsApplied() {
        val r=run(scene(),"Join Beta_811 and Gamma_662 with a segment.")
        assertEquals(r.message,GraphCommandStatus.APPLIED,r.status)
    }
    @Test fun rayRetainsOrderedOriginAndThroughPoint() {
        val r=run(scene(),"Draw a ray starting at Anchor_907 and going through Gamma_662.")
        assertEquals(r.message,GraphCommandStatus.APPLIED,r.status)
    }
    @Test fun unknownOrAmbiguousParentsNeedClarificationWithoutSceneEdits() {
        val s=scene()
        for(t in listOf("Draw a line from Missing to Beta_811.","Join Missing and Gamma_662 with a segment.")) {
            val r=run(s,t);assertEquals(r.message,GraphCommandStatus.CLARIFICATION,r.status)
            assertEquals(s.points3D,r.state.points3D);assertEquals(s.vectors3D,r.state.vectors3D)
        }
    }
    @Test fun coincidentParentsCannotDefineUniqueLinearObjects() {
        val s=scene();val coincident=s.copy(points3D=s.points3D.mapIndexed{i,p->if(i==1)p.copy(position=s.points3D[0].position)else p})
        for(t in listOf("Draw a line from Anchor_907 to Beta_811.","Draw a ray starting at Anchor_907 and going through Beta_811.")) {
            val r=run(coincident,t);assertEquals(r.message,GraphCommandStatus.INVALID,r.status)
            assertEquals(coincident.points3D,r.state.points3D)
        }
    }
    private fun built(kind:Linear3DKind):WorkspaceState {
        val s=scene();val text=when(kind) {
            Linear3DKind.Line->"Draw a line from Anchor_907 to Beta_811."
            Linear3DKind.Segment->"Join Anchor_907 and Beta_811 with a segment."
            Linear3DKind.Ray->"Draw a ray starting at Anchor_907 and going through Beta_811."
        }
        return run(s,text).state.also{assertEquals(1,it.linears3D.size)}
    }
    @Test fun exactDomainsAndSlabClippingDistinguishLineRayAndSegment() {
        for(kind in Linear3DKind.entries) {
            val s=built(kind);val obj=s.linears3D.single();val line=NativeLinearGeometry.evaluate(s,obj)
            assertEquals(s.points3D[0].position,line.at(0.0));assertEquals(s.points3D[1].position,line.at(1.0))
            assertEquals(kind==Linear3DKind.Line,line.containsParameter(-1.0))
            assertEquals(kind!=Linear3DKind.Segment,line.containsParameter(2.0));assertFalse(line.containsParameter(Double.NaN))
            val clipped=NativeLinearGeometry.clip(line,8.0)!!
            assertEquals(kind==Linear3DKind.Line,clipped.arrowStart);assertEquals(kind!=Linear3DKind.Segment,clipped.arrowEnd)
            if(kind!=Linear3DKind.Line)assertEquals(line.origin,clipped.start)
            if(kind==Linear3DKind.Segment)assertEquals(s.points3D[1].position,clipped.end)
            for(p in listOf(clipped.start,clipped.end)) {
                assertTrue(listOf(p.x,p.y,p.z).all{it in -8.000000001..8.000000001})
                val t=(p.x+2)/6;assertEquals(1+4*t,p.y,1e-9);assertEquals(3-4*t,p.z,1e-9)
            }
            assertEquals(obj.kind,kind);assertEquals(listOf("a-907","b-811"),obj.pointIds)
        }
    }
    @Test fun linkedDefinitionsFollowCurrentParentsAndCanonicalSaveProjection() {
        val s=built(Linear3DKind.Ray);val document=UniversalWorkspaceBridge.fromWorkspace(s)
        val obj=document.objects.getValue("linear3d-${s.linears3D.single().id}")
        assertEquals(UniversalMathKind.Ray,obj.kind);assertEquals(setOf("point3d-a-907","point3d-b-811"),obj.dependencies)
        val first=s.points3D[0];val moved=TransformPoint3DCommand(0,first,first.copy(position=Vec3(1.0,2.0,3.0))).apply(s.copy(universalMathDocument=document))
        val line=NativeLinearGeometry.evaluate(moved,moved.linears3D.single())
        assertEquals(Vec3(1.0,2.0,3.0),line.origin);assertEquals(Vec3(3.0,3.0,-4.0),line.direction)
        val loaded=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(moved),recover=false)
        assertNotNull(loaded.diagnostics.toString(),loaded.state);assertEquals(moved.linears3D,loaded.state!!.linears3D)
        assertEquals(line,NativeLinearGeometry.evaluate(loaded.state!!,loaded.state!!.linears3D.single()))
        val projected=UniversalWorkspaceBridge.applyToWorkspace(moved.universalMathDocument!!,moved.copy(linears3D=emptyList()))
        assertEquals(moved.linears3D,projected.linears3D)
        assertEquals(line,NativeLinearGeometry.evaluate(projected,projected.linears3D.single()))
        assertEquals(line,NativeLinearGeometry.evaluate(projected.copy(points3D=projected.points3D.reversed()),projected.linears3D.single()))
    }
    @Test fun creationAndParentDeletionUndoRestoreStableIdsAndDoNotTouchOtherGeometry() {
        val s=scene();val h=WorkspaceHistoryByModule();val m=RuhiDialogueManager()
        val r=m.execute(s,"Join Beta_811 and Gamma_662 with a segment.",h)
        assertEquals(1,h.forModule(s.module).protocol.size);assertEquals(s.vectors3D,r.state.vectors3D);assertEquals(s.points3D,r.state.points3D)
        val undone=h.forModule(s.module).undo(r.state);assertTrue(undone.linears3D.isEmpty())
        val redone=h.forModule(s.module).redo(undone);assertEquals(r.state.linears3D,redone.linears3D)
        val doc=redone.copy(universalMathDocument=UniversalWorkspaceBridge.fromWorkspace(redone))
        val deletion=DeletePoint3DCommand(1,doc.points3D[1],doc.planes3D,doc.linears3D)
        val deleted=deletion.apply(doc);assertTrue(deleted.linears3D.isEmpty());assertEquals(2,deleted.points3D.size)
        assertTrue(UniversalMathDocumentEngine().validate(deleted.universalMathDocument!!).valid)
        val restored=deletion.undo(deleted);assertEquals(doc.linears3D,restored.linears3D);assertEquals(doc.points3D,restored.points3D)
        assertEquals(NativeLinearGeometry.evaluate(doc,doc.linears3D.single()),NativeLinearGeometry.evaluate(restored,restored.linears3D.single()))
    }
    @Test fun lexicalAndUnicodeNamesRemainEntitiesAndDuplicateNamesAreRejected() {
        val initial=scene();val s=initial.copy(points3D=initial.points3D.mapIndexed{i,p->p.copy(name=listOf("one","नदी7","two")[i])})
        val r=run(s,"Draw a ray starting at one and going through नदी7.")
        assertEquals(r.message,GraphCommandStatus.APPLIED,r.status)
        assertEquals(listOf("a-907","b-811"),r.state.linears3D.single().pointIds)
        assertEquals(s.points3D,r.state.points3D)
        val duplicate=s.copy(points3D=s.points3D+Point3D("other","ONE",Vec3(2.0,3.0,4.0)))
        val rejected=run(duplicate,"Draw a line from one to two.")
        assertEquals(GraphCommandStatus.CLARIFICATION,rejected.status);assertTrue(rejected.state.linears3D.isEmpty())
    }
    @Test fun clippingParallelAxesOutsideTheBoxDoesNotFabricateVisibleSegments() {
        val l=NativeLinearGeometry.Linear("a","L",Linear3DKind.Line,Vec3(12.0,0.0,0.0),Vec3(0.0,1.0,0.0))
        assertNull(NativeLinearGeometry.clip(l,8.0))
        val ray=l.copy(kind=Linear3DKind.Ray,origin=Vec3(0.0,12.0,0.0))
        assertNull(NativeLinearGeometry.clip(ray,8.0))
        val toward=ray.copy(direction=Vec3(0.0,-1.0,0.0));val visible=NativeLinearGeometry.clip(toward,8.0)!!
        assertEquals(Vec3(0.0,8.0,0.0),visible.start);assertEquals(Vec3(0.0,-8.0,0.0),visible.end)
    }
    @Test fun canonicalProjectionPreservesCreationOrderAfterSerializationWithUnsortedIds() {
        val initial=scene();val s=initial.copy(linears3D=listOf(
            Linear3DObject("z-last-sorting","First",Linear3DKind.Line,listOf("a-907","b-811")),
            Linear3DObject("a-first-sorting","Second",Linear3DKind.Ray,listOf("c-662","b-811"))))
        val encoded=UniversalMathDocumentCodec.encode(UniversalWorkspaceBridge.fromWorkspace(s))
        val decoded=UniversalMathDocumentCodec.decode(encoded,recover=false).document!!
        val projected=UniversalWorkspaceBridge.applyToWorkspace(decoded,s.copy(linears3D=emptyList()))
        assertEquals(s.linears3D,projected.linears3D)
    }
    @Test fun sceneClearAndSavedSceneRestoreDoNotLeaveDanglingLinearParents() {
        val m=RuhiDialogueManager();val h=WorkspaceHistoryByModule();val s=built(Linear3DKind.Segment)
        val saved=m.execute(s,"Save this 3D scene.",h);assertEquals(saved.message,GraphCommandStatus.APPLIED,saved.status)
        val changed=saved.state.copy(linears3D=emptyList())
        val restored=m.execute(changed,"Load the saved 3D scene.",h)
        assertEquals(restored.message,GraphCommandStatus.APPLIED,restored.status)
        assertEquals(s.linears3D,restored.state.linears3D);assertEquals(s.points3D,restored.state.points3D)
        val cleared=m.execute(restored.state,"Clear the workspace.",h)
        assertEquals(cleared.message,GraphCommandStatus.APPLIED,cleared.status)
        assertTrue(cleared.state.points3D.isEmpty());assertTrue(cleared.state.linears3D.isEmpty())
    }
}
