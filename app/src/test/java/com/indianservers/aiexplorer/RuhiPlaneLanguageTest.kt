package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.workspace.*
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import org.junit.Assert.*
import org.junit.Test

class RuhiPlaneLanguageTest {
    @Test fun olderNativeWorkspaceSnapshotsLoadWithoutInventingPlanes() {
        val s=scene();val old=WorkspaceSnapshotCodec.encode(s).replaceFirst("AIEXPLORER_WORKSPACE|${WorkspaceSnapshotCodec.currentSchema}|","AIEXPLORER_WORKSPACE|11|")
        val loaded=WorkspaceSnapshotCodec.decode(old,recover=false);assertNotNull(loaded.diagnostics.toString(),loaded.state);assertEquals(s.points3D,loaded.state!!.points3D);assertTrue(loaded.state!!.planes3D.isEmpty());assertTrue(loaded.recovered)
    }
    private fun scene()=WorkspaceState(module=MathModule.Geometry3D,points3D=listOf(Point3D("corner|907","Anchor_907",Vec3(0.0,0.0,0.0)),Point3D("edge,811","East_811",Vec3(2.0,0.0,0.0)),Point3D("north-662","North_662",Vec3(0.0,2.0,0.0)),Point3D("up-553","Up_553",Vec3(0.0,0.0,2.0))))
    private fun planes():WorkspaceState {
        val m=RuhiDialogueManager();val h=WorkspaceHistoryByModule()
        val floor=m.execute(scene(),"Create plane Floor through Anchor_907, East_811 and North_662",h);assertEquals(floor.message,GraphCommandStatus.APPLIED,floor.status)
        val wall=m.execute(floor.state,"Create plane Wall through Anchor_907, East_811, and Up_553",h);assertEquals(wall.message,GraphCommandStatus.APPLIED,wall.status);return wall.state
    }
    @Test fun source221MatchesOriginalSlotsAndIndependentPlaneEquations() {
        val s=RuhiSeed500Fixture.state("GEOMETRY_2D_OR_3D",221);val c=RuhiSeed500Fixture.context(221);val r=RuhiDialogueManager().execute(s,"Measure the dihedral angle between two selected planes",WorkspaceHistoryByModule(),c)
        val check=RuhiSeedBehaviorOracle.check("MEASURE_ANGLE",mapOf("planes" to "selected_pair","kind" to "dihedral"),s,r,c);assertTrue(check.diagnostics.toString(),check.verified);assertEquals(45.0,RuhiSpatialPlaneLanguage.drawings(r.state).single().degrees,1e-9)
    }
    @Test fun nativeDefinitionsAndCanonicalDependenciesFollowPointEditsAcrossSaveLoad() {
        val s=planes();assertEquals(2,s.planes3D.size);assertEquals(scene().points3D,s.points3D)
        val document=UniversalWorkspaceBridge.fromWorkspace(s);val floor=s.planes3D.first();assertEquals(floor.pointIds.map{"point3d-$it"}.toSet(),document.objects.getValue("plane3d-${floor.id}").dependencies)
        val saved=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(s.copy(universalMathDocument=document)),recover=false);assertNotNull(saved.diagnostics.toString(),saved.state);assertEquals(s.planes3D,saved.state!!.planes3D)
        val point=saved.state!!.points3D[2];val moved=TransformPoint3DCommand(2,point,point.copy(position=Vec3(0.0,2.0,2.0))).apply(saved.state!!)
        assertEquals(45.0,NativePlaneGeometry.angle(moved,moved.planes3D.map{it.id}).degrees,1e-9)
        val canonicalPoint=moved.universalMathDocument!!.objects.getValue("point3d-${point.id}").payload as UniversalMathPayload.Coordinates;assertEquals(listOf(0.0,2.0,2.0),canonicalPoint.values)
        val projected=UniversalWorkspaceBridge.applyToWorkspace(moved.universalMathDocument!!,moved);assertEquals(moved.points3D,projected.points3D);assertEquals(moved.planes3D,projected.planes3D)
        val restored=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(moved),recover=false).state!!;assertEquals(moved.planes3D,restored.planes3D);assertEquals(45.0,NativePlaneGeometry.angle(restored,restored.planes3D.map{it.id}).degrees,1e-9)
    }
    @Test fun missingPairResumesAfterChipSaveLoadAndUsesOneHistoryEntry() {
        val s=planes();val m=RuhiDialogueManager();val h=WorkspaceHistoryByModule();val q=m.execute(s,"Measure the dihedral angle between two selected planes",h)
        assertEquals(GraphCommandStatus.CLARIFICATION,q.status);assertTrue(q.dialogue!!.pending);assertEquals(0,h.forModule(s.module).protocol.size)
        val restored=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(q.state)).state!!;assertEquals(q.dialogue!!.suggestions,m.restoredView(restored)!!.suggestions)
        val resume=m.execute(restored,"Resume",h);val r=m.execute(resume.state,"Use planes Floor and Wall",h)
        assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertTrue(r.message.contains("90.0 degrees"));assertEquals(1,h.forModule(s.module).protocol.size);assertNull(RuhiDialogueMemory.read(r.state).pending)
        val undo=m.execute(r.state,"Undo.",h);assertTrue(RuhiSpatialPlaneLanguage.marks(undo.state).isEmpty());val redo=m.execute(undo.state,"Redo.",h);assertEquals(RuhiSpatialPlaneLanguage.marks(r.state),RuhiSpatialPlaneLanguage.marks(redo.state))
    }
    @Test fun invalidPlanesAndNameCollisionsNeverChangeTheSceneWhileParallelPlanesMeasureZero() {
        val m=RuhiDialogueManager();val s=scene();val collinear=s.copy(points3D=s.points3D.mapIndexed{i,p->if(i==2)p.copy(position=Vec3(4.0,0.0,0.0))else p})
        val invalid=m.execute(collinear,"Create plane Test through Anchor_907, East_811 and North_662",WorkspaceHistoryByModule());assertEquals(GraphCommandStatus.INVALID,invalid.status);assertTrue(invalid.state.planes3D.isEmpty());assertEquals(collinear.points3D,invalid.state.points3D)
        val actual=planes();val collision=m.execute(actual,"Create plane Floor through Anchor_907, East_811 and Up_553",WorkspaceHistoryByModule());assertEquals(GraphCommandStatus.CLARIFICATION,collision.status);assertEquals(actual.planes3D,collision.state.planes3D)
        val shifted=actual.copy(points3D=actual.points3D+actual.points3D.take(3).map{it.copy(id=it.id+"-offset",position=it.position+Vec3(0.0,0.0,1.0))},planes3D=listOf(actual.planes3D[0],Plane3DObject("shifted","Shifted",actual.planes3D[0].pointIds.map{it+"-offset"})))
        val angle=NativePlaneGeometry.angle(shifted,shifted.planes3D.map{it.id});assertTrue(angle.parallel);assertEquals(0.0,angle.degrees,0.0);assertTrue(angle.arc.all{listOf(it.x,it.y,it.z).all(Double::isFinite)})
    }
    @Test fun deletingAParentCascadesPlanesAndUndoRestoresTheSameStableDefinitions() {
        val base=planes();val m=RuhiDialogueManager();val measured=m.execute(base,"Measure the dihedral angle between two selected planes",WorkspaceHistoryByModule(),RuhiSceneContext(selectedPlaneIds=base.planes3D.map{it.id})).state
        val s=measured.copy(universalMathDocument=UniversalWorkspaceBridge.fromWorkspace(measured));val command=DeletePoint3DCommand(3,s.points3D[3],s.planes3D);val deleted=command.apply(s)
        assertEquals(1,deleted.planes3D.size);assertTrue(RuhiSpatialPlaneLanguage.drawings(deleted).isEmpty());assertTrue(UniversalMathDocumentEngine().validate(deleted.universalMathDocument!!).valid)
        val restored=command.undo(deleted);assertEquals(s.planes3D,restored.planes3D);assertEquals(s.points3D,restored.points3D);assertEquals(90.0,RuhiSpatialPlaneLanguage.drawings(restored).single().degrees,1e-9)
        val reordered=restored.copy(points3D=restored.points3D.reversed(),planes3D=restored.planes3D.reversed());assertEquals(90.0,RuhiSpatialPlaneLanguage.drawings(reordered).single().degrees,1e-9)
    }
    @Test fun stalePairAnswersAndCancellationDoNotMeasureChangedPlanes() {
        val s=planes();val m=RuhiDialogueManager();val h=WorkspaceHistoryByModule();val q=m.execute(s,"Measure the dihedral angle between two selected planes",h)
        val changed=q.state.copy(points3D=q.state.points3D.mapIndexed{i,p->if(i==3)p.copy(position=Vec3(0.0,1.0,1.0))else p})
        val r=m.execute(changed,"Use planes Floor and Wall",h);assertEquals(GraphCommandStatus.CLARIFICATION,r.status);assertTrue(r.message.contains("scene changed"));assertNull(RuhiDialogueMemory.read(r.state).pending);assertTrue(RuhiSpatialPlaneLanguage.marks(r.state).isEmpty())
        val cancel=m.execute(q.state,"Never mind.",h);assertNull(RuhiDialogueMemory.read(cancel.state).pending);assertEquals(s.planes3D,cancel.state.planes3D)
    }
}
