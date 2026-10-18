package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.workspace.*
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import org.junit.Assert.*
import org.junit.Test

class RuhiMissingPriorContextTest {
    private val sources=linkedMapOf(480 to "Use the same radius as before",481 to "No, make it 7 instead",483 to "Both of them",486 to "Continue where we stopped",490 to "Use x squared for the equation",491 to "Choose the second suggested answer")
    private fun empty(id:Int)=WorkspaceState(module=if(id in setOf(480,481,483))MathModule.Geometry3D else MathModule.Graph2D,surfaceLayers=emptyList())
    private fun question(id:Int) {
        val s=empty(id);val h=WorkspaceHistoryByModule();val m=RuhiDialogueManager()
        assertTrue(m.accepts(s,sources.getValue(id)))
        val q=m.execute(s,sources.getValue(id),h)
        assertEquals(q.message,GraphCommandStatus.CLARIFICATION,q.status)
        assertEquals(RuhiDialogueAction.ASK_CLARIFICATION,q.dialogue!!.action)
        assertTrue(q.dialogue!!.pending);assertTrue(q.dialogue!!.suggestions.isEmpty())
        assertEquals(s.solids,q.state.solids);assertEquals(s.functions,q.state.functions);assertEquals(s.points,q.state.points)
        assertEquals(0,h.forModule(s.module).protocol.size)
        val saved=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(q.state),recover=false).state!!
        val resume=RuhiDialogueManager().execute(saved,"Continue",h)
        assertEquals(GraphCommandStatus.CLARIFICATION,resume.status);assertEquals(q.message,resume.message)
        assertEquals(q.dialogue!!.suggestions,RuhiDialogueManager().restoredView(saved)!!.suggestions)
        val command=if(s.module==MathModule.Geometry3D)"Create a sphere with radius 3" else "Plot y = x^2"
        val applied=RuhiDialogueManager().execute(resume.state,command,h)
        assertEquals(applied.message,GraphCommandStatus.APPLIED,applied.status)
        assertNull(RuhiDialogueMemory.read(applied.state).pending);assertEquals(1,h.forModule(s.module).protocol.size)
        if(s.module==MathModule.Geometry3D){assertEquals(1,applied.state.solids.size);assertEquals(3.0,applied.state.solids.single().radius,0.0)}
        else {assertEquals(1,applied.state.functions.size);assertEquals(4.0,ExpressionEngine().compile(applied.state.functions.single().expression.substringAfter('=')).eval(mapOf("x" to 2.0)),1e-9)}
    }
    @Test fun absentRadiusContextAsksPurposeBeforeCreatingAnything()=question(480)
    @Test fun absentNumericSlotDoesNotGuessWhichDimensionToChange()=question(481)
    @Test fun absentPairDoesNotInventTwoObjectsOrTheirAction()=question(483)
    @Test fun absentPendingTaskAsksWhatToResume()=question(486)
    @Test fun absentEquationTaskDoesNotInventAnExistingGraph()=question(490)
    @Test fun absentSuggestionDoesNotInventAnAnswerChoice()=question(491)
    @Test fun partialAnswersCancellationAndStaleWorkspaceNeverChangeGeometry() {
        val s=empty(481);val h=WorkspaceHistoryByModule();val m=RuhiDialogueManager()
        val q=m.execute(s,sources.getValue(481),h)
        assertEquals(GraphCommandStatus.CLARIFICATION,q.status)
        for(reply in listOf("7","both","sphere")) {
            val r=m.execute(q.state,reply,h);assertEquals(GraphCommandStatus.CLARIFICATION,r.status)
            assertTrue(r.state.solids.isEmpty());assertEquals(0,h.forModule(s.module).protocol.size)
        }
        val cancelled=m.execute(q.state,"Never mind",h);assertNull(RuhiDialogueMemory.read(cancelled.state).pending)
        val changed=q.state.copy(module=MathModule.Graph2D)
        val resumed=m.execute(changed,"Continue",h)
        assertEquals(GraphCommandStatus.CLARIFICATION,resumed.status);assertNull(RuhiDialogueMemory.read(resumed.state).pending)
    }
    @Test fun actualPendingTasksStillUseTheirRealRolesChoicesAndSavedQuestion() {
        val fixture=RuhiDialogueSeedFixture
        for((id,text) in sources) {
            val s=fixture.state(id,0);val r=RuhiDialogueManager().execute(s,text,WorkspaceHistoryByModule(),fixture.context(s))
            assertEquals("Original valid pending context $id: ${r.message}",if(id==486)GraphCommandStatus.CLARIFICATION else GraphCommandStatus.APPLIED,r.status)
            if(id==486)assertEquals(RuhiDialogueMemory.read(s).pendingPrompt,r.message)
        }
    }
}
