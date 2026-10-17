package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.workspace.*
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import org.junit.Assert.*
import org.junit.Test

class RuhiPreserveCubeTest {
    private class Session(solids:List<Solid>) {
        var s=WorkspaceState(module=MathModule.Geometry3D,solids=solids,labSessionValues=mapOf("ruhi.studio.appearance.0" to "blue"));var c=RuhiSceneContext()
        val manager=RuhiDialogueManager();val history=WorkspaceHistoryByModule()
        fun say(text:String)=manager.execute(s,text,history,c).also{s=it.state;c=it.context}
        fun restore(){s=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(s)).state!!}
    }
    @Test fun originalConstraintCompletesOnlyRemainingTargetAndUsesOneUndo() {
        val solids=listOf(Solid(SolidType.Cube,2.0),Solid(SolidType.Sphere,4.0,position=Vec3(6.0,0.0,0.0)))
        val s=Session(solids);val q=s.say("Scale it by 1.5");assertEquals(q.message,GraphCommandStatus.CLARIFICATION,q.status)
        assertEquals("target",RuhiDialogueMemory.read(s.s).pending!!.kind);s.restore()
        val r=s.say("Do not change the existing cube");assertEquals(r.message,GraphCommandStatus.APPLIED,r.status)
        assertEquals(solids[0],s.s.solids[0]);assertEquals(6.0,s.s.solids[1].width,0.0);assertEquals("blue",s.s.labSessionValues["ruhi.studio.appearance.0"])
        assertEquals("preserve_cube",r.semanticFrame!!.parameters["constraint"]);assertEquals("Do not change the existing cube",RuhiDialogueMemory.read(s.s).turns.last().user)
        assertEquals("scale",RuhiActionContext.read(s.s)!!.kind)
        s.say("Undo");assertEquals(solids,s.s.solids)
    }
    @Test fun severalRemainingTargetsRestoreBoundChoicesAndRejectProtectedTarget() {
        val solids=listOf(Solid(SolidType.Cube,2.0),Solid(SolidType.Cone,3.0),Solid(SolidType.Sphere,4.0))
        val s=Session(solids);s.say("Scale the selected object by 2")
        val q=s.say("Do not change the existing cube");assertEquals(GraphCommandStatus.CLARIFICATION,q.status);assertEquals(2,q.dialogue!!.suggestions.size);assertEquals(solids,s.s.solids)
        s.restore();assertEquals(q.dialogue!!.suggestions,s.manager.restoredView(s.s)!!.suggestions)
        assertEquals(GraphCommandStatus.CLARIFICATION,s.say("Move the cube to (9, 8, 7)").status);assertEquals(solids,s.s.solids)
        val r=s.say("Choose the second suggested answer");assertEquals(r.message,GraphCommandStatus.APPLIED,r.status)
        assertEquals(solids.take(2),s.s.solids.take(2));assertEquals(8.0,s.s.solids[2].width,0.0)
    }
    @Test fun twoCubesAskWhichToProtectThenApplyOnlyToTheOtherCube() {
        val solids=listOf(Solid(SolidType.Cube,2.0),Solid(SolidType.Cube,3.0));val s=Session(solids)
        s.say("Move the cube to (3, 2, 1)")
        val q=s.say("Do not change the existing cube");assertTrue(q.message,q.message.contains("Which existing cube"));assertEquals(2,q.dialogue!!.suggestions.size)
        s.restore();val r=s.say(q.dialogue!!.suggestions[1].answer);assertEquals(r.message,GraphCommandStatus.APPLIED,r.status)
        assertEquals(solids[1],s.s.solids[1]);assertEquals(Vec3(3.0,2.0,1.0),s.s.solids[0].position)
        s.say("Undo");assertEquals(solids,s.s.solids)
    }
    @Test fun contradictoryCubeOnlyPendingEditDoesNotExecuteAndCanBeCancelled() {
        val solids=listOf(Solid(SolidType.Cube,2.0),Solid(SolidType.Sphere,3.0));val s=Session(solids)
        // A pending transform that genuinely names only the cube is persisted without executing it.
        val q=s.say("Move the selected object to (4, 5, 6)");assertEquals(GraphCommandStatus.CLARIFICATION,q.status)
        val memory=RuhiDialogueMemory.read(s.s);s.s=s.s.copy(labSessionValues=s.s.labSessionValues+(RuhiDialogueMemory.KEY to memory.copy(pending=memory.pending!!.copy(targets=listOf("0"))).encode()))
        val r=s.say("Do not change the existing cube");assertTrue(r.message,r.message.contains("only the protected cube"));assertEquals(solids,s.s.solids)
        s.restore();assertEquals(GraphCommandStatus.CLARIFICATION,s.say("Move the cube to (9, 8, 7)").status);assertEquals(solids,s.s.solids)
        s.say("Cancel");assertNull(RuhiDialogueMemory.read(s.s).pending);assertEquals(solids,s.s.solids)
    }
    @Test fun staleConstraintClearsItsBindingsAndDoesNotMoveCurrentSolids() {
        val s=Session(listOf(Solid(SolidType.Cube,2.0),Solid(SolidType.Cone,3.0),Solid(SolidType.Sphere,4.0)))
        s.say("Scale the selected object by 1.5");val q=s.say("Do not change the existing cube")
        s.s=s.s.copy(solids=s.s.solids.reversed());val current=s.s.solids
        val r=s.say(q.dialogue!!.suggestions.first().answer);assertTrue(r.message,r.message.contains("scene changed"));assertEquals(current,s.s.solids);assertNull(RuhiDialogueMemory.read(s.s).pending)
    }
    @Test fun noPendingActionOrNoCubeAsksEssentialQuestionWithoutInventingEdit() {
        val s=Session(listOf(Solid(SolidType.Cube,2.0)));val before=s.s.solids
        val r=s.say("Do not change the existing cube");assertTrue(r.message,r.message.contains("Which pending action"));assertEquals(before,s.s.solids)
        val absent=Session(listOf(Solid(SolidType.Cone,2.0),Solid(SolidType.Sphere,4.0)));absent.say("Scale it by 2");val original=absent.s.solids
        val q=absent.say("Do not change the existing cube");assertTrue(q.message,q.message.contains("no cube"));assertEquals(original,absent.s.solids);assertNotNull(RuhiDialogueMemory.read(absent.s).pending)
    }
    @Test fun privateValidationRejectsBroadEditThatWouldStillChangeProtectedCube() {
        val solids=listOf(Solid(SolidType.Cube,2.0),Solid(SolidType.Sphere,4.0));val s=Session(solids)
        s.say("Scale it by 1.5")
        val memory=RuhiDialogueMemory.read(s.s)
        s.s=s.s.copy(labSessionValues=s.s.labSessionValues+(RuhiDialogueMemory.KEY to memory.copy(pending=memory.pending!!.copy(source="scale all of them by 1.5")).encode()))
        val r=s.say("Do not change the existing cube")
        assertEquals(r.message,GraphCommandStatus.CLARIFICATION,r.status);assertTrue(r.message,r.message.contains("would change the protected cube"))
        assertEquals(solids,s.s.solids);assertEquals("blue",s.s.labSessionValues["ruhi.studio.appearance.0"])
        assertFalse(s.history.forModule(s.s.module).canUndo);assertNotNull(RuhiDialogueMemory.read(s.s).pending)
    }
    @Test fun preservingCubeRetainsExistingAnimationAndAppearanceRatherThanFreezingIt() {
        val s=Session(listOf(Solid(SolidType.Cube,2.0),Solid(SolidType.Sphere,4.0)))
        val key=RuhiMotionEngine.PREFIX+"0";val motion=RuhiAngularMotion("y",30.0,0L).encode()
        s.s=s.s.copy(labSessionValues=s.s.labSessionValues+(key to motion));val pose=RuhiMotionEngine.solids(s.s,5000)[0]
        assertEquals(Vec3(0.0,150.0,0.0),pose.rotation)
        s.say("Scale it by 1.5");val r=s.say("Do not change the existing cube")
        assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertEquals(motion,s.s.labSessionValues[key])
        assertEquals(pose,RuhiMotionEngine.solids(s.s,5000)[0]);assertEquals("blue",s.s.labSessionValues["ruhi.studio.appearance.0"])
        assertEquals(3.0,s.s.solids[1].radius,0.0)
    }
}
