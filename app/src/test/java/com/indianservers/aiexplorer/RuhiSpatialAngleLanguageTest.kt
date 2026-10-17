package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.workspace.*
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class RuhiSpatialAngleLanguageTest {
    private fun scene()=WorkspaceState(module=MathModule.Geometry3D,vectors3D=listOf(Vector3D("opaque|907",Vec3(0.0,0.0,0.0),Vec3(3.0,0.0,0.0),"Sun_907"),Vector3D("opaque,811",Vec3(5.0,5.0,5.0),Vec3(5.0,8.0,5.0),"Moon_811")))
    @Test fun source220PreservesItsSelectedPairSlotsAndPassesIndependentThreeDimensionalOracle() {
        val s=RuhiSeed500Fixture.state("GEOMETRY_2D_OR_3D",220);val c=RuhiSeed500Fixture.context(220);val h=WorkspaceHistoryByModule()
        val r=RuhiDialogueManager().execute(s,"What is the angle between these two 3D vectors?",h,c)
        val check=RuhiSeedBehaviorOracle.check("MEASURE_ANGLE",mapOf("vectors" to "selected_3d_pair"),s,r,c);assertTrue(check.diagnostics.toString(),check.verified);assertEquals(1,h.forModule(s.module).protocol.size)
        assertEquals(Math.toDegrees(acos(12.0/25.0)),RuhiSpatialAngleLanguage.drawings(r.state).single().degrees,1e-9)
    }
    @Test fun unseenNamesStableIdsAndChangedCoordinatesProduceLiveSpatialArcs() {
        val s=scene();val m=RuhiDialogueManager();val selected=m.execute(s,"Select vectors Sun_907 and Moon_811",WorkspaceHistoryByModule());assertEquals(selected.message,GraphCommandStatus.APPLIED,selected.status)
        val r=m.execute(selected.state,"What is the angle between these two 3D vectors?",WorkspaceHistoryByModule());assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertTrue(r.message.contains("90.0 degrees"));assertEquals(s.vectors3D,r.state.vectors3D)
        val moved=r.state.copy(vectors3D=listOf(s.vectors3D[1].copy(name="Renamed_1",end=Vec3(8.0,8.0,5.0)),s.vectors3D[0].copy(name="Renamed_2")))
        assertEquals(45.0,RuhiSpatialAngleLanguage.drawings(moved).single().degrees,1e-9)
        val saved=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(moved));assertNotNull(saved.diagnostics.toString(),saved.state);val restored=saved.state!!;assertEquals(RuhiSpatialAngleLanguage.marks(moved),RuhiSpatialAngleLanguage.marks(restored));assertEquals(RuhiSpatialAngleLanguage.drawings(moved),RuhiSpatialAngleLanguage.drawings(restored))
        val removed=moved.copy(vectors3D=moved.vectors3D.take(1)+s.vectors3D[0].copy(id="replacement"));assertTrue(RuhiSpatialAngleLanguage.drawings(removed).isEmpty())
        val q=m.execute(removed,"What is the angle between these two 3D vectors?",WorkspaceHistoryByModule());assertEquals(GraphCommandStatus.CLARIFICATION,q.status);assertEquals(removed.vectors3D,q.state.vectors3D)
    }
    @Test fun missingPairQuestionSurvivesSaveLoadAndResumesItsOriginalActionWithoutExtraUndo() {
        val s=scene();val m=RuhiDialogueManager();val h=WorkspaceHistoryByModule();val q=m.execute(s,"What is the angle between these two 3D vectors?",h)
        assertEquals(GraphCommandStatus.CLARIFICATION,q.status);assertTrue(q.dialogue!!.pending);assertEquals(1,q.dialogue!!.suggestions.size);assertEquals(0,h.forModule(s.module).protocol.size)
        val saved=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(q.state));assertNotNull(saved.diagnostics.toString(),saved.state);val restored=saved.state!!;assertEquals(q.dialogue!!.suggestions,m.restoredView(restored)!!.suggestions)
        val resumed=m.execute(restored,"Resume",h);assertTrue(resumed.dialogue!!.pending)
        val r=m.execute(resumed.state,"Use vectors Sun_907 and Moon_811",h);assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertTrue(r.message.contains("90.0 degrees"));assertNull(RuhiDialogueMemory.read(r.state).pending);assertEquals(1,h.forModule(s.module).protocol.size)
        val undo=m.execute(r.state,"Undo.",h);assertTrue(RuhiSpatialAngleLanguage.marks(undo.state).isEmpty());val redo=m.execute(undo.state,"Redo.",h);assertEquals(RuhiSpatialAngleLanguage.marks(r.state),RuhiSpatialAngleLanguage.marks(redo.state))
    }
    @Test fun changedVectorSceneAndCancellationNeverExecuteAnOldPairAnswer() {
        val s=scene();val m=RuhiDialogueManager();val h=WorkspaceHistoryByModule();val q=m.execute(s,"What is the angle between these two 3D vectors?",h)
        val changed=q.state.copy(vectors3D=q.state.vectors3D.mapIndexed{i,v->if(i==1)v.copy(end=v.end+Vec3(1.0,0.0,0.0))else v})
        val r=m.execute(changed,"Use vectors Sun_907 and Moon_811",h);assertEquals(GraphCommandStatus.CLARIFICATION,r.status);assertTrue(r.message.contains("scene changed"));assertNull(RuhiDialogueMemory.read(r.state).pending);assertTrue(RuhiSpatialAngleLanguage.drawings(r.state).isEmpty())
        val cancel=m.execute(q.state,"Never mind.",h);assertNull(RuhiDialogueMemory.read(cancel.state).pending);assertTrue(RuhiSpatialAngleLanguage.marks(cancel.state).isEmpty())
    }
    @Test fun zeroParallelAntiparallelAndDuplicateNamesHaveDeterministicSafeOutcomes() {
        val m=RuhiDialogueManager();val s=scene()
        val zero=s.copy(vectors3D=s.vectors3D.mapIndexed{i,v->if(i==1)v.copy(end=v.start)else v});val invalid=m.execute(zero,"What is the angle between 3D vectors Sun_907 and Moon_811?",WorkspaceHistoryByModule());assertEquals(GraphCommandStatus.INVALID,invalid.status);assertEquals(zero.vectors3D,invalid.state.vectors3D)
        for(sign in listOf(1.0,-1.0)) {
            val input=s.copy(vectors3D=s.vectors3D.mapIndexed{i,v->if(i==1)v.copy(end=v.start+Vec3(sign*3,0.0,0.0))else v})
            val r=m.execute(input,"What is the angle between 3D vectors Sun_907 and Moon_811?",WorkspaceHistoryByModule());assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);val d=RuhiSpatialAngleLanguage.drawings(r.state).single();assertEquals(if(sign>0)0.0 else 180.0,d.degrees,1e-9);assertTrue(d.arc.all{listOf(it.x,it.y,it.z).all(Double::isFinite)})
        }
        val duplicate=s.copy(vectors3D=s.vectors3D+s.vectors3D[0].copy(id="another"));val q=m.execute(duplicate,"Select vectors Sun_907 and Moon_811",WorkspaceHistoryByModule());assertEquals(GraphCommandStatus.CLARIFICATION,q.status);assertTrue(RuhiSpatialAngleLanguage.selected(q.state).isEmpty())
        val negative=m.execute(s,"Do not select vectors Sun_907 and Moon_811",WorkspaceHistoryByModule());assertTrue(RuhiSpatialAngleLanguage.selected(negative.state).isEmpty())
    }
}
