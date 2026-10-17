package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.workspace.*
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import org.junit.Assert.*
import org.junit.Test

class RuhiAppliedTargetCorrectionTest {
    private class Session(var state:WorkspaceState) {
        val manager=RuhiDialogueManager();val history=WorkspaceHistoryByModule()
        fun say(text:String,c:RuhiSceneContext=RuhiSceneContext())=manager.execute(state,text,history,c).also{state=it.state}
        fun restore(){state=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(state)).state!!}
    }
    private fun scene(colors:List<String> = listOf("red","blue"))=WorkspaceState(module=MathModule.Geometry3D,
        solids=colors.mapIndexed{i,_->Solid(SolidType.Cube,2.0+i,position=Vec3(i*4.0,0.0,0.0))},
        labSessionValues=colors.mapIndexed{i,color->"ruhi.studio.appearance.$i" to color}.toMap())
    private fun applied(r:RuhiExecutionResult){assertEquals(r.message,GraphCommandStatus.APPLIED,r.status)}
    @Test fun completedPositionRestoresWrongTargetAndMovesBlueWithTwoSeparateUndoSteps() {
        val initial=scene();val s=Session(initial)
        assertEquals(GraphCommandStatus.CLARIFICATION,s.say("Move the cube to (3, 2, 1)").status)
        applied(s.say("Choose the first suggested answer"));val wrong=s.state.solids
        s.restore();applied(s.say("Actually I meant the blue one"))
        assertEquals(initial.solids[0],s.state.solids[0]);assertEquals(Vec3(3.0,2.0,1.0),s.state.solids[1].position)
        assertEquals(initial.solids[1].width,s.state.solids[1].width,0.0)
        assertEquals("Actually I meant the blue one",RuhiDialogueMemory.read(s.state).turns.last().user)
        assertNull(RuhiDialogueMemory.read(s.state).pending)
        applied(s.say("Undo"));assertEquals(wrong,s.state.solids)
        applied(s.say("Undo"));assertEquals(initial.solids,s.state.solids)
        applied(s.say("Redo"));assertEquals(wrong,s.state.solids)
        applied(s.say("Redo"));assertEquals(initial.solids[0],s.state.solids[0]);assertEquals(Vec3(3.0,2.0,1.0),s.state.solids[1].position)
    }
    @Test fun scaleUsesExactPreimageAndOriginalFactorOnDifferentBlueDimensions() {
        for(factor in listOf(1.25,1.7,2.3)) {
            val initial=scene();val s=Session(initial)
            applied(s.say("Scale the selected object by $factor",RuhiSceneContext(selectedSolidIndex=0)));s.restore()
            applied(s.say("Actually I meant the blue one"))
            assertEquals(initial.solids[0],s.state.solids[0]);assertEquals(initial.solids[1].width*factor,s.state.solids[1].width,1e-12)
            assertEquals(initial.solids[1].position,s.state.solids[1].position)
        }
    }
    @Test fun translationAndRotationReplayOnActualBlueTargetPreservingItsOriginalPose() {
        for(command in listOf("Translate the selected object by vector (2, 3, 4)","Rotate the selected object 35 degrees around the Y-axis")) {
            val initial=scene();val s=Session(initial)
            applied(s.say(command,RuhiSceneContext(selectedSolidIndex=0)));s.restore();applied(s.say("Actually I meant the blue one"))
            val expected=Session(initial);applied(expected.say(command,RuhiSceneContext(selectedSolidIndex=1)))
            assertEquals(initial.solids[0],s.state.solids[0]);assertEquals(expected.state.solids[1],s.state.solids[1])
        }
    }
    @Test fun restoredMultipleBlueChoicesAreBoundAndPreserveUnrelatedObjects() {
        val initial=scene(listOf("red","blue","#0000FF"));val s=Session(initial)
        applied(s.say("Move the selected object to (2, 3, 4)",RuhiSceneContext(selectedSolidIndex=0)))
        val wrong=s.state.solids;val q=s.say("Actually I meant the blue one")
        assertEquals(GraphCommandStatus.CLARIFICATION,q.status);assertEquals(2,q.dialogue!!.suggestions.size);assertEquals(wrong,s.state.solids)
        s.restore();applied(s.say("Choose the second suggested answer"))
        assertEquals(initial.solids[0],s.state.solids[0]);assertEquals(initial.solids[1],s.state.solids[1]);assertEquals(Vec3(2.0,3.0,4.0),s.state.solids[2].position)
    }
    @Test fun changingCoordinatesColorOrOrderRejectsStaleCorrectionWithoutUndoEntry() {
        for(change in 0..2) {
            val s=Session(scene());applied(s.say("Scale the selected object by 1.5",RuhiSceneContext(selectedSolidIndex=0)))
            s.state=when(change){0->s.state.copy(solids=s.state.solids.mapIndexed{i,o->if(i==1)o.copy(position=Vec3(9.0,0.0,0.0))else o});1->s.state.copy(labSessionValues=s.state.labSessionValues+("ruhi.studio.appearance.1" to "green"));else->s.state.copy(solids=s.state.solids.reversed())}
            val current=s.state;val r=s.say("Actually I meant the blue one")
            assertEquals(GraphCommandStatus.CLARIFICATION,r.status);assertTrue(r.message.contains("scene changed"));assertEquals(current.solids,s.state.solids)
        }
    }
    @Test fun unitDisplayBetweenEditAndCorrectionIsPreserved() {
        val s=Session(scene());applied(s.say("Use meters for coordinate units"))
        applied(s.say("Scale the selected object by 1.5",RuhiSceneContext(selectedSolidIndex=0)))
        applied(s.say("Use centimeters instead"));s.restore();applied(s.say("Actually I meant the blue one"))
        assertEquals(GeometryMeasurementUnits.Length.CENTIMETER,GeometryMeasurementUnits.read(s.state).display)
        assertEquals(2.0,s.state.solids[0].width,0.0);assertEquals(4.5,s.state.solids[1].width,0.0)
    }
    @Test fun missingSnapshotOrReadOnlyLatestTaskDoesNotInventRestoration() {
        val s=Session(scene());applied(s.say("Scale the selected object by 1.5",RuhiSceneContext(selectedSolidIndex=0)))
        applied(s.say("Find the volume of the selected solid",RuhiSceneContext(selectedSolidIndex=0)))
        val current=s.state.solids;val r=s.say("Actually I meant the blue one")
        assertEquals(GraphCommandStatus.CLARIFICATION,r.status);assertEquals(current,s.state.solids)
        s.state=s.state.copy(labSessionValues=s.state.labSessionValues+(RuhiAppliedTargetCorrection.KEY to "corrupt"))
        assertEquals(GraphCommandStatus.CLARIFICATION,s.say("Actually I meant the blue one").status)
    }
    @Test fun twoDimensionalScalingRestoresControlPointsAndUsesBlueStableId() {
        val initial=WorkspaceState(module=MathModule.Geometry2D,points=listOf(Vec2(0.0,0.0),Vec2(2.0,0.0),Vec2(6.0,0.0),Vec2(9.0,0.0)),
            shapes=listOf(Shape2D("red,/997",Shape2DType.Circle,listOf(0,1),styleKey="red"),Shape2D("blue,/441",Shape2DType.Circle,listOf(2,3),styleKey="blue")))
        val s=Session(initial);val command="Scale the selected object by 1.5"
        applied(s.say(command,RuhiSceneContext(selectedShapeId=initial.shapes[0].id)));s.restore();applied(s.say("Actually I meant the blue one"))
        val expected=Session(initial);applied(expected.say(command,RuhiSceneContext(selectedShapeId=initial.shapes[1].id)))
        assertEquals(expected.state.points,s.state.points);assertEquals(initial.shapes,s.state.shapes)
    }
    @Test fun failedBlueReplayDoesNotPartiallyRestoreOriginalEditOrAddHistory() {
        val initial=scene().let{it.copy(solids=listOf(it.solids[0],it.solids[1].copy(width=9000.0,height=9000.0,depth=9000.0)))}
        val s=Session(initial);applied(s.say("Scale the selected object by 2",RuhiSceneContext(selectedSolidIndex=0)))
        val wrong=s.state.solids;val r=s.say("Actually I meant the blue one")
        assertEquals(r.message,GraphCommandStatus.CLARIFICATION,r.status);assertEquals(wrong,s.state.solids)
        applied(s.say("Undo"));assertEquals(initial.solids,s.state.solids)
    }
    @Test fun noBlueDoesNotApplyOrCreateAnUndoEntry() {
        val initial=scene(listOf("red","green"));val s=Session(initial)
        applied(s.say("Move the selected object to (2, 3, 4)",RuhiSceneContext(selectedSolidIndex=0)))
        val wrong=s.state.solids;val r=s.say("Actually I meant the blue one")
        assertEquals(GraphCommandStatus.CLARIFICATION,r.status);assertTrue(r.message.contains("No blue"));assertEquals(wrong,s.state.solids)
        applied(s.say("Undo"));assertEquals(initial.solids,s.state.solids)
    }
}
