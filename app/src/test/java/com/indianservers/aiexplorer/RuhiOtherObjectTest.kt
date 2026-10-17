package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.workspace.*
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import org.junit.Assert.*
import org.junit.Test

class RuhiOtherObjectTest {
    private class Session(var s:WorkspaceState,var c:RuhiSceneContext) {
        val manager=RuhiDialogueManager();val history=WorkspaceHistoryByModule()
        fun say(text:String)=manager.execute(s,text,history,c).also{s=it.state;c=it.context}
        fun restore(){s=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(s)).state!!}
    }
    @Test fun scaleUsesValidatedFactorOnOtherCurrentSolidAfterRestoreWithOneUndo() {
        val s=Session(WorkspaceState(module=MathModule.Geometry3D,solids=listOf(Solid(SolidType.Cube,2.0),Solid(SolidType.Sphere,6.0))),RuhiSceneContext(selectedSolidIndex=0))
        assertEquals(GraphCommandStatus.APPLIED,s.say("Scale the cube by 1.75").status);val first=s.s.solids[0]
        assertEquals("scale",RuhiActionContext.read(s.s)!!.kind);s.restore()
        val r=s.say("What about the other one")
        assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertEquals(first,s.s.solids[0]);assertEquals(10.5,s.s.solids[1].width,1e-9)
        assertEquals("What about the other one",RuhiDialogueMemory.read(s.s).turns.last().user)
        s.say("Undo");assertEquals(first,s.s.solids[0]);assertEquals(6.0,s.s.solids[1].width,0.0)
    }
    @Test fun multiAxisTranslationKeepsItsVectorRatherThanReusingDestination() {
        val initial=listOf(Solid(SolidType.Cube,2.0,position=Vec3(1.0,2.0,3.0)),Solid(SolidType.Cone,3.0,position=Vec3(-4.0,7.0,2.0)))
        val s=Session(WorkspaceState(module=MathModule.Geometry3D,solids=initial),RuhiSceneContext(selectedSolidIndex=0))
        val a=s.say("Translate the cube by vector (2, 3, 4)");assertEquals(a.message,GraphCommandStatus.APPLIED,a.status)
        val b=s.say("What about the other one");assertEquals(b.message,GraphCommandStatus.APPLIED,b.status)
        assertEquals(Vec3(3.0,5.0,7.0),s.s.solids[0].position);assertEquals(Vec3(-2.0,10.0,6.0),s.s.solids[1].position)
        assertEquals("translate",RuhiActionContext.read(s.s)!!.kind)
    }
    @Test fun solidMeasurementRecomputesOtherNativeSolidWithoutChangingGeometry() {
        val initial=listOf(Solid(SolidType.Cube,2.0),Solid(SolidType.Cuboid,3.0,4.0,5.0))
        val s=Session(WorkspaceState(module=MathModule.Geometry3D,solids=initial),RuhiSceneContext(selectedSolidIndex=0))
        val a=s.say("Find volume of the selected solid");assertEquals(a.message,GraphCommandStatus.APPLIED,a.status)
        assertEquals("o:0",RuhiActionContext.read(s.s)!!.target);s.restore()
        val b=s.say("What about the other one");assertEquals(b.message,GraphCommandStatus.APPLIED,b.status)
        assertTrue(b.message,b.message.contains("60"));assertEquals(initial,s.s.solids)
        val drawing=RuhiSolidMeasureLanguage.drawing(s.s)!!;assertEquals(60.0,drawing.entities.single().quantities.single().value,1e-9)
    }
    @Test fun severalOtherObjectsAskBoundChoicesAndSecondChoiceSurvivesRestore() {
        val solids=listOf(Solid(SolidType.Cube,2.0),Solid(SolidType.Cone,3.0),Solid(SolidType.Sphere,4.0))
        val s=Session(WorkspaceState(module=MathModule.Geometry3D,solids=solids),RuhiSceneContext(selectedSolidIndex=0))
        s.say("Rotate the cube 37 degrees around the X-axis");val before=s.s.solids
        val q=s.say("What about the other one");assertEquals(GraphCommandStatus.CLARIFICATION,q.status);assertEquals(2,q.dialogue!!.suggestions.size);assertEquals(before,s.s.solids)
        s.restore();val r=s.say("Choose the second suggested answer");assertEquals(r.message,GraphCommandStatus.APPLIED,r.status)
        assertEquals(before.take(2),s.s.solids.take(2));assertEquals(Vec3(37.0,0.0,0.0),s.s.solids[2].rotation);assertNull(RuhiDialogueMemory.read(s.s).pending)
    }
    @Test fun stableShapeIdsSurviveReorderingAndMeasurementsUseEditedControls() {
        val initial=WorkspaceState(module=MathModule.Geometry2D,points=listOf(Vec2(0.0,0.0),Vec2(2.0,0.0),Vec2(0.0,2.0),Vec2(5.0,0.0),Vec2(9.0,0.0),Vec2(5.0,3.0)),shapes=listOf(Shape2D("opaque,/907",Shape2DType.Triangle,listOf(0,1,2)),Shape2D("opaque,/811",Shape2DType.Triangle,listOf(3,4,5))))
        val s=Session(initial,RuhiSceneContext(selectedShapeId="opaque,/907"));s.say("calculate the area")
        s.s=s.s.copy(shapes=s.s.shapes.reversed(),points=s.s.points.toMutableList().apply{this[5]=Vec2(5.0,5.0)})
        s.restore();val r=s.say("What about the other one");assertEquals(r.message,GraphCommandStatus.APPLIED,r.status)
        assertTrue(r.message,r.message.contains("10.0"));assertEquals("s:opaque,/811",RuhiActionContext.read(s.s)!!.target)
    }
    @Test fun missingDeletedOrUnrecordedActionDoesNotReplayOldScale() {
        val s=Session(WorkspaceState(module=MathModule.Geometry3D,solids=listOf(Solid(SolidType.Cube,2.0),Solid(SolidType.Sphere,3.0))),RuhiSceneContext(selectedSolidIndex=0))
        assertEquals(GraphCommandStatus.CLARIFICATION,s.say("What about the other one").status)
        s.say("Scale the cube by 2");s.s=s.s.copy(solids=s.s.solids.reversed());val changed=s.s.solids
        assertEquals(GraphCommandStatus.CLARIFICATION,s.say("What about the other one").status);assertEquals(changed,s.s.solids)
        s.say("Create a cone");assertNull(RuhiActionContext.read(s.s));val created=s.s.solids
        assertEquals(GraphCommandStatus.CLARIFICATION,s.say("What about the other one").status);assertEquals(created,s.s.solids)
    }
    @Test fun genericMultiObjectScaleDoesNotBecomeSingleTargetActionMemory() {
        val s=Session(WorkspaceState(module=MathModule.Geometry3D,solids=listOf(Solid(SolidType.Cube,2.0),Solid(SolidType.Sphere,3.0))),RuhiSceneContext())
        val r=s.say("scale all of them by 1.5");assertEquals(r.message,GraphCommandStatus.APPLIED,r.status)
        assertEquals(3.0,s.s.solids[0].width,0.0);assertEquals(4.5,s.s.solids[1].width,0.0)
        assertNull(RuhiActionContext.read(s.s));val before=s.s.solids
        assertEquals(GraphCommandStatus.CLARIFICATION,s.say("What about the other one").status);assertEquals(before,s.s.solids)
    }
}
