package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.workspace.*
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import org.junit.Assert.*
import org.junit.Test

class RuhiErrorContextTest {
    private fun scene()=WorkspaceState(module=MathModule.Geometry3D,solids=listOf(Solid(SolidType.Cube,2.0)),surfaceLayers=emptyList())
    private val selected=RuhiSceneContext(selectedSolidIndex=0)
    private fun ask(m:RuhiDialogueManager,s:WorkspaceState,h:WorkspaceHistoryByModule):RuhiExecutionResult {
        val r=m.execute(s,"Why did that fail?",h,selected)
        assertEquals(r.message,GraphCommandStatus.CLARIFICATION,r.status)
        assertEquals(RuhiDialogueAction.ASK_CLARIFICATION,r.dialogue!!.action)
        assertTrue(r.dialogue!!.pending);assertTrue(r.dialogue!!.suggestions.isEmpty())
        sameScene(s,r.state);return r
    }
    private fun sameScene(a:WorkspaceState,b:WorkspaceState) {
        assertEquals(a.module,b.module);assertEquals(a.points,b.points);assertEquals(a.shapes,b.shapes)
        assertEquals(a.solids,b.solids);assertEquals(a.functions,b.functions);assertEquals(a.surfaceLayers,b.surfaceLayers)
        assertEquals(a.points3D,b.points3D);assertEquals(a.vectors3D,b.vectors3D);assertEquals(a.planes3D,b.planes3D);assertEquals(a.linears3D,b.linears3D)
        assertEquals(a.labSessionValues[RuhiActionContext.KEY],b.labSessionValues[RuhiActionContext.KEY])
    }
    @Test fun missingFailedCommandAsksOneSavedQuestionInEveryNativeStudio() {
        for(module in listOf(MathModule.Geometry2D,MathModule.Geometry3D,MathModule.Graph2D,MathModule.Graph3D)) {
            val h=WorkspaceHistoryByModule();val q=ask(RuhiDialogueManager(),WorkspaceState(module=module,surfaceLayers=emptyList()),h)
            assertEquals(0,h.forModule(module).protocol.size);assertNotNull(RuhiDialogueMemory.read(q.state).pending)
        }
    }
    @Test fun savedDestructiveReferencesNeverClearTheActualScene() {
        val m=RuhiDialogueManager();val s=scene();val h=WorkspaceHistoryByModule();val q=ask(m,s,h)
        val loaded=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(q.state),recover=false).state!!
        assertTrue(m.restoredView(loaded)!!.pending)
        for(command in listOf("Clear the canvas","Start again with a blank canvas","Move the cube to (3, 2, 1)","Create a sphere with radius 3")) {
            val r=m.execute(loaded,command,h,selected)
            assertEquals(r.message,GraphCommandStatus.CLARIFICATION,r.status)
            sameScene(s,r.state);assertEquals(0,h.forModule(s.module).protocol.size)
            assertNotNull(RuhiDialogueMemory.read(r.state).pending)
        }
    }
    @Test fun reproducibleInvalidReferenceExplainsValidationWithoutApplyingIt() {
        val m=RuhiDialogueManager();val s=scene();val h=WorkspaceHistoryByModule();val q=ask(m,s,h)
        val r=m.execute(q.state,"Create a sphere with radius -3",h,selected)
        assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertEquals(RuhiDialogueAction.EXPLAIN,r.dialogue!!.action)
        assertTrue(r.message.contains("positive",true));sameScene(s,r.state)
        assertEquals(0,h.forModule(s.module).protocol.size);assertNull(RuhiDialogueMemory.read(r.state).pending)
    }
    @Test fun reportedErrorTextAlsoRemainsReferenceMaterial() {
        val m=RuhiDialogueManager();val s=scene();val h=WorkspaceHistoryByModule();val q=ask(m,s,h)
        val reference=m.execute(q.state,"Move the cube to (3, 2, 1)",h,selected)
        assertEquals(GraphCommandStatus.CLARIFICATION,reference.status);sameScene(s,reference.state)
        val explained=m.execute(reference.state,"Clear the canvas",h,selected)
        assertEquals(explained.message,GraphCommandStatus.APPLIED,explained.status)
        assertEquals(RuhiDialogueAction.EXPLAIN,explained.dialogue!!.action)
        sameScene(s,explained.state);assertEquals(0,h.forModule(s.module).protocol.size)
    }
    @Test fun existingNumericQuestionIsRestoredAfterAnErrorExplanation() {
        val s=RuhiDialogueSeedFixture.state(480,0);val old=RuhiDialogueMemory.read(s)
        assertTrue(old.lastError.isBlank());assertNotNull(old.pending)
        val m=RuhiDialogueManager();val h=WorkspaceHistoryByModule();val q=ask(m,s,h)
        val r=m.execute(q.state,"Create a sphere with radius -3",h,selected)
        assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);sameScene(s,r.state)
        assertEquals(old.pending,RuhiDialogueMemory.read(r.state).pending)
        assertEquals(old.pendingPrompt,RuhiDialogueMemory.read(r.state).pendingPrompt)
        val resumed=m.execute(r.state,"Use measurement value 2",h,selected)
        assertEquals(resumed.message,GraphCommandStatus.APPLIED,resumed.status)
        assertEquals(s.solids,resumed.state.solids);assertNull(RuhiDialogueMemory.read(resumed.state).pending)
        assertEquals(1,h.forModule(s.module).protocol.size)
    }
    @Test fun resumeCancelAndChangedStudioDoNotApplyReferenceCommands() {
        val m=RuhiDialogueManager();val s=scene();val h=WorkspaceHistoryByModule();val q=ask(m,s,h)
        val resumed=m.execute(q.state,"Continue",h,selected)
        assertEquals(q.message,resumed.message);sameScene(s,resumed.state)
        val cancelled=m.execute(q.state,"Never mind",h,selected)
        assertNull(RuhiDialogueMemory.read(cancelled.state).pending);sameScene(s,cancelled.state)
        val changed=q.state.copy(module=MathModule.Graph2D)
        val stale=m.execute(changed,"Continue",h)
        assertEquals(GraphCommandStatus.CLARIFICATION,stale.status)
        assertNull(RuhiDialogueMemory.read(stale.state).pending);sameScene(changed,stale.state)
    }
    @Test fun pronounCommandAliasesCannotBypassErrorReferenceMode() {
        val m=RuhiDialogueManager();val h=WorkspaceHistoryByModule()
        val scaled=m.execute(scene(),"Scale the selected object by 1.5",h,selected)
        assertEquals(GraphCommandStatus.APPLIED,scaled.status)
        assertNotNull(scaled.state.labSessionValues[RuhiActionContext.KEY])
        val q=ask(m,scaled.state,h)
        val r=m.execute(q.state,"Make it larger",h,selected)
        assertEquals(r.message,GraphCommandStatus.CLARIFICATION,r.status)
        sameScene(scaled.state,r.state);assertEquals(1,h.forModule(scaled.state.module).protocol.size)
    }
    @Test fun actuallyRecordedErrorStillExplainsItsRealDiagnostic() {
        val m=RuhiDialogueManager();val s=scene();val h=WorkspaceHistoryByModule()
        val invalid=m.execute(s,"Create a sphere with radius -3",h,selected)
        assertEquals(GraphCommandStatus.INVALID,invalid.status)
        val error=RuhiDialogueMemory.read(invalid.state).lastError;assertTrue(error.isNotBlank())
        val r=m.execute(invalid.state,"Why did that fail?",h,selected)
        assertEquals(GraphCommandStatus.APPLIED,r.status);assertEquals(RuhiDialogueAction.EXPLAIN,r.dialogue!!.action)
        assertTrue(r.message.contains(error));sameScene(s,r.state)
        assertEquals(0,h.forModule(s.module).protocol.size)
    }
}
