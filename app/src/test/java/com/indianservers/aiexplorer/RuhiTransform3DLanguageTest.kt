package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.workspace.*
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class RuhiTransform3DLanguageTest {
    private fun query(s:WorkspaceState,t:String,h:WorkspaceHistoryByModule=WorkspaceHistoryByModule(),c:RuhiSceneContext=RuhiSceneContext())=RuhiDialogueManager().execute(s,t,h,c)
    @Test fun naturalMoveToCoordinatesUsesTheSameValidatedNativePositionAction() {
        val s=WorkspaceState(module=MathModule.Geometry3D,solids=listOf(Solid(SolidType.Cube,2.0),Solid(SolidType.Sphere,3.0)))
        for(target in listOf(Vec3(3.0,2.0,1.0),Vec3(-2.5,8.25,0.0))) {
            val h=WorkspaceHistoryByModule();val r=query(s,"Move the cube to (${target.x}, ${target.y}, ${target.z})",h)
            assertEquals(r.message,GraphCommandStatus.APPLIED,r.status)
            assertEquals(target,r.state.solids[0].position);assertEquals(s.solids[1],r.state.solids[1])
            assertEquals(s.solids[0].width,r.state.solids[0].width,0.0)
            assertEquals(s.solids,query(r.state,"Undo",h).state.solids)
        }
        val duplicate=s.copy(solids=s.solids+Solid(SolidType.Cube,3.0))
        val question=query(duplicate,"Move the cube to (3,2,1)")
        assertEquals(GraphCommandStatus.CLARIFICATION,question.status)
        assertEquals(2,question.dialogue!!.suggestions.size);assertEquals(duplicate.solids,question.state.solids)
        for(text in listOf("Move the cube to (abc,2,3)","Move the cube to (10001,2,3)")) {
            val r=query(s,text);assertEquals(text,GraphCommandStatus.INVALID,r.status);assertEquals(s.solids,r.state.solids)
        }
    }
    @Test fun original25RowsRetainSourceSlotsAndIndependentlyCheckedMeshPoseAndView() {
        val root=requireNotNull(generateSequence(java.io.File(requireNotNull(System.getProperty("user.dir")))){it.parentFile}.firstOrNull{java.io.File(it,"docs/qa/ruhi-500/replay.tsv").exists()})
        fun decode(v:String)=String(java.util.Base64.getDecoder().decode(v),Charsets.UTF_8)
        val rows=java.io.File(root,"docs/qa/ruhi-500/replay.tsv").readLines().map{it.split('\t')}.filter{it[2]=="TRANSFORM_3D"};assertEquals(25,rows.size);val errors=mutableListOf<String>()
        rows.forEach{row->val id=row[0].toInt();val s=RuhiSeed500Fixture.state(row[3],id);val c=RuhiSeed500Fixture.context(id);val h=RuhiSeed500Fixture.history(id,s);val r=query(s,decode(row[1]),h,c);val check=RuhiTransform3DOracle.check(RuhiSeedJson.objectValue(decode(row[6])),s,r,c);if(!check.verified)errors+="$id ${check.diagnostics}"};assertTrue(errors.joinToString("\n"),errors.isEmpty())
    }
    @Test fun arbitraryAnglesAndScalesActOnCurrentDimensionsAndMirrorAllNativeMeshTypes() {
        for(type in listOf(SolidType.Cuboid,SolidType.Cone,SolidType.Wedge,SolidType.Tetrahedron)){
            val q=Solid(type,2.0,3.0,4.0,radius=1.0,position=Vec3(2.0,-3.0,4.0),rotation=Vec3(13.0,27.0,41.0));val s=WorkspaceState(module=MathModule.Geometry3D,solids=listOf(q));val r=query(s,"Mirror this object across the yz-plane",c=RuhiSceneContext(selectedSolidIndex=0));assertEquals(r.message,GraphCommandStatus.APPLIED,r.status)
            val b=r.state.solids.single();assertTrue(WorkspaceJson.export(r.state).contains("\"mirrorX\":true"));SolidMeshFactory.create(q).vertices.zip(SolidMeshFactory.create(b).vertices).forEach{(u,v)->val wanted=SolidPoseMath.world(q,u).let{it.copy(x=-it.x)};assertTrue((wanted-SolidPoseMath.world(b,v)).magnitude()<1e-8)}
            val twice=query(r.state,"Mirror this object across the yz-plane",c=RuhiSceneContext(selectedSolidIndex=0));assertEquals(q,twice.state.solids.single());assertEquals(b,WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(r.state),recover=false).state!!.solids.single())
        }
        for(f in listOf(.375,1.75,4.2)){val s=WorkspaceState(module=MathModule.Geometry3D,solids=listOf(Solid(SolidType.Cube,2.0,3.0,4.0)));val r=query(s,"Scale the cube by $f");assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertEquals(24*f.pow(3),RuhiSolidMeasureMath.quantity(r.state.solids.single(),"volume").value,1e-7)}
    }
    @Test fun groupMotionUsesSharedPivotAndAllThreeWorldAxesAndPreservesOtherMotion() {
        for(axis in listOf("x","y","z")){
            val solids=listOf(Solid(SolidType.Cube,2.0,position=Vec3(2.0,1.0,3.0),rotation=Vec3(17.0,23.0,31.0)),Solid(SolidType.Cone,2.0,3.0,position=Vec3(-2.0,3.0,1.0),rotation=Vec3(11.0,29.0,7.0)),Solid(SolidType.Sphere,2.0))
            val s=WorkspaceState(module=MathModule.Geometry3D,solids=solids,labSessionValues=mapOf("ruhi.geometry3d.selection" to "0,1",RuhiMotionEngine.PREFIX+2 to RuhiAngularMotion("y",20.0,100).encode()))
            val out=RuhiTransform3DLanguage.execute(s,"Rotate both solids together around $axis",WorkspaceHistoryByModule(),RuhiSceneContext(),RuhiInputModality.TEXT,null,100)!!.result;assertEquals(out.message,GraphCommandStatus.APPLIED,out.status)
            val poses=RuhiMotionEngine.solids(out.state,3100);val pivot=(solids[0].position+solids[1].position)*.5
            val turn=GraphTransform().rotated(axis,90.0,Vec3(0.0,0.0,0.0))
            for(i in 0..1){assertTrue((poses[i].position-(pivot+turn.direction(solids[i].position-pivot))).magnitude()<1e-8);SolidMeshFactory.create(solids[i]).vertices.forEach{v->assertTrue((SolidPoseMath.world(poses[i],v)-(pivot+turn.direction(SolidPoseMath.world(solids[i],v)-pivot))).magnitude()<1e-8)}}
            assertEquals(60.0,poses[2].rotation.y,1e-9);assertEquals(s.labSessionValues[RuhiMotionEngine.PREFIX+2],out.state.labSessionValues[RuhiMotionEngine.PREFIX+2])
            val stopped=RuhiTransform3DLanguage.execute(out.state,"Stop rotation",WorkspaceHistoryByModule(),RuhiSceneContext(),RuhiInputModality.TEXT,null,3100)!!.result
            assertEquals(poses,stopped.state.solids);assertEquals(poses,RuhiMotionEngine.solids(stopped.state,9999));assertFalse(stopped.state.labSessionValues.keys.any{it==RuhiTransform3DLanguage.GROUP||it.startsWith(RuhiMotionEngine.PREFIX)})
        }
    }
    @Test fun ambiguousObjectAndGroupChipsRestoreResumeCancelAndRejectStaleScenes() {
        val s=WorkspaceState(module=MathModule.Geometry3D,solids=listOf(Solid(SolidType.Cube,2.0),Solid(SolidType.Cube,3.0)));val r=query(s,"Scale the cube by 1.7");assertEquals(GraphCommandStatus.CLARIFICATION,r.status);assertEquals(2,r.dialogue!!.suggestions.size)
        val loaded=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(r.state),recover=false).state!!;assertEquals(r.dialogue,RuhiDialogueManager().restoredView(loaded));val done=query(loaded,r.dialogue!!.suggestions.last().answer);assertEquals(done.message,GraphCommandStatus.APPLIED,done.status);assertEquals(5.1,done.state.solids[1].width,1e-9);assertEquals(s.solids[0],done.state.solids[0])
        val pair=query(s,"Rotate both solids together around z");assertEquals(GraphCommandStatus.CLARIFICATION,pair.status);val grouped=query(pair.state,pair.dialogue!!.suggestions.single().answer);assertEquals(grouped.message,GraphCommandStatus.APPLIED,grouped.status);assertEquals(listOf(0,1),RuhiTransform3DLanguage.groupIds(grouped.state))
        assertNull(RuhiDialogueMemory.read(query(r.state,"Never mind").state).pending);val stale=query(r.state.copy(solids=s.solids.drop(1)),r.dialogue!!.suggestions.first().answer);assertEquals(GraphCommandStatus.CLARIFICATION,stale.status);assertNull(RuhiDialogueMemory.read(stale.state).pending)
    }
    @Test fun scopedRotationUndoPreservesLaterTranslationAndRedoAndFaceSeparationIsReversible() {
        val s=WorkspaceState(module=MathModule.Geometry3D,solids=listOf(Solid(SolidType.Cube,2.0)));val h=WorkspaceHistoryByModule();val turn=query(s,"Rotate the cube 37 degrees around x",h);val moved=query(turn.state,"Move the cube 3 units right",h);val undo=query(moved.state,"Undo that last 3D rotation",h);assertEquals(undo.message,GraphCommandStatus.APPLIED,undo.status);assertEquals(Vec3(0.0,0.0,0.0),undo.state.solids.single().rotation);assertEquals(Vec3(3.0,0.0,0.0),undo.state.solids.single().position)
        val redo=query(undo.state,"Redo",h);assertEquals(Vec3(37.0,0.0,0.0),redo.state.solids.single().rotation);assertEquals(Vec3(3.0,0.0,0.0),redo.state.solids.single().position)
        val exploded=query(s,"Explode the faces of the cube");assertEquals(GraphCommandStatus.APPLIED,exploded.status);val mesh=SolidMeshFactory.exploded(exploded.state.solids.single(),RuhiTransform3DLanguage.explosion(exploded.state,0));assertEquals(24,mesh.vertices.size);assertEquals(6,mesh.faces.size)
        val restored=query(exploded.state,"Put the exploded cube back together");assertEquals(s.solids,restored.state.solids);assertEquals(0.0,RuhiTransform3DLanguage.explosion(restored.state,0),0.0)
        for(t in listOf("Scale the cube by -1","Scale the cube by 1e99","Rotate the cube 1e99 degrees around x")){val bad=query(s,t);assertEquals(bad.message,GraphCommandStatus.INVALID,bad.status);assertEquals(s.solids,bad.state.solids)}
    }
}
