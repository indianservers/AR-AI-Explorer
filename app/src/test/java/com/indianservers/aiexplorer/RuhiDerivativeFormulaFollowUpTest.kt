package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.workspace.*
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import org.junit.Assert.*
import org.junit.Test

class RuhiDerivativeFormulaFollowUpTest {
    private class Session(var state:WorkspaceState=WorkspaceState(module=MathModule.Graph2D,functions=emptyList(),surfaceLayers=emptyList())) {
        val history=WorkspaceHistoryByModule();val manager=RuhiDialogueManager()
        fun say(text:String,c:RuhiSceneContext=RuhiSceneContext())=manager.execute(state,text,history,c).also{state=it.state}
        fun restore(){state=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(state)).state!!}
    }
    @Test fun originalSelectedDerivativeRequestOffersFormulaExamplesAndResumesAfterSave() {
        val s=Session();val q=s.say("Plot the derivative of the selected curve")
        assertEquals(GraphCommandStatus.CLARIFICATION,q.status);assertEquals("derivative_formula",RuhiDialogueMemory.read(s.state).pending!!.kind)
        assertTrue(q.dialogue!!.suggestions.size in 3..5);assertTrue(s.state.functions.isEmpty());assertTrue(s.history.forModule(s.state.module).protocol.isEmpty())
        s.restore();val out=s.say("y = 3*x^2 + 2*x")
        assertEquals(out.message,GraphCommandStatus.APPLIED,out.status);assertEquals(1,s.state.functions.size);assertNull(RuhiDialogueMemory.read(s.state).pending)
        val query=RuhiDerivativeLanguage.decode(s.state.labSessionValues.getValue(RuhiDerivativeLanguage.MARK));assertEquals("function",query.kind);assertEquals(s.state.functions.single().id,query.source)
        val math=RuhiDerivativeLanguage.resolve(s.state,query)
        assertEquals(14.0,RuhiDerivativeMath.slope(math,2.0).value,1e-8)
        assertEquals(1,s.history.forModule(s.state.module).protocol.size)
        s.say("Undo");assertTrue(s.state.functions.isEmpty());assertFalse(s.state.labSessionValues.containsKey(RuhiDerivativeLanguage.MARK))
        s.say("Redo");assertEquals(1,s.state.functions.size);assertNotNull(RuhiDerivativeLanguage.drawing(s.state))
    }
    @Test fun originalGradientRequestsRetainNumericReplyAcrossFormulaQuestionAndRestore() {
        for(command in listOf("Show the tangent slope at the selected point","What is the instantaneous gradient here?")) {
            val s=Session();s.say(command);s.restore();val formula=s.say("2")
            assertEquals(formula.message,GraphCommandStatus.CLARIFICATION,formula.status);assertEquals("derivative_formula",RuhiDialogueMemory.read(s.state).pending!!.kind)
            s.restore();val out=s.say("y=x^3");assertEquals(out.message,GraphCommandStatus.APPLIED,out.status);assertTrue(out.message.contains("slope = 12.0"));assertEquals(listOf(Vec2(2.0,8.0)),RuhiDerivativeLanguage.drawing(s.state)!!.points)
        }
    }
    @Test fun invalidAndNonExplicitFormulasKeepTheQuestionWithoutCreatingCurves() {
        val s=Session();s.say("Plot the derivative of the selected curve")
        for(formula in listOf("Use derivative formula y=banana(x)","Use derivative formula x^2+y^2=9","Use derivative formula y=1/0","Use derivative formula y=a*x")) {
            val r=s.say(formula);assertEquals(r.message,GraphCommandStatus.INVALID,r.status);assertTrue(s.state.functions.isEmpty());assertTrue(s.history.forModule(s.state.module).protocol.isEmpty());assertEquals("derivative_formula",RuhiDialogueMemory.read(s.state).pending!!.kind);s.restore()
        }
        assertEquals(GraphCommandStatus.APPLIED,s.say("y=x^2").status)
    }
    @Test fun formulaWithUndefinedSavedXAsksForValidXBeforeAtomicCreation() {
        val s=Session();s.say("What is the instantaneous gradient here?");s.say("0");s.restore()
        val q=s.say("y=ln(x)");assertEquals(q.message,GraphCommandStatus.CLARIFICATION,q.status);assertTrue(s.state.functions.isEmpty());assertTrue(s.history.forModule(s.state.module).protocol.isEmpty())
        assertEquals("derivative_x",RuhiDialogueMemory.read(s.state).pending!!.kind)
        assertTrue(q.dialogue!!.suggestions.isNotEmpty());q.dialogue!!.suggestions.forEach{assertTrue(it.label.substringAfter('=').trim().toDouble()>0)}
        s.restore();val r=s.say("2");assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertTrue(r.message.contains("slope = 0.5"));assertEquals(1,s.state.functions.size);assertEquals(1,s.history.forModule(s.state.module).protocol.size)
    }
    @Test fun cancellationSceneChangesAndNewLiteralRequestsDoNotConsumeFormulaSlots() {
        val s=Session();s.say("Plot the derivative of the selected curve");s.restore();s.say("Cancel that");assertNull(RuhiDialogueMemory.read(s.state).pending);assertTrue(s.state.functions.isEmpty())
        s.say("Plot the derivative of the selected curve");s.state=s.state.copy(functions=listOf(FunctionDefinition("external","External","y=x^4","red")))
        val stale=s.say("Use derivative formula y=x^2");assertEquals(GraphCommandStatus.CLARIFICATION,stale.status);assertEquals("external",s.state.functions.single().id);assertNull(RuhiDialogueMemory.read(s.state).pending)
        val fresh=Session();fresh.say("What is the instantaneous gradient here?");fresh.say("3");fresh.say("Differentiate y=x^2");assertTrue(fresh.state.functions.isEmpty());assertNull(RuhiDialogueMemory.read(fresh.state).pending)
    }
    @Test fun originalStationaryAndSliderRequestsKeepTheirModesAndCurrentFunctionIdentity() {
        for((command,mode) in listOf("Where is the slope zero on this curve?" to "stationary","Show how the tangent changes as x moves" to "slider")) {
            val s=Session();val q=s.say(command);s.restore();val r=s.say(q.dialogue!!.suggestions.first().answer)
            assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);val query=RuhiDerivativeLanguage.decode(s.state.labSessionValues.getValue(RuhiDerivativeLanguage.MARK));assertEquals(mode,query.mode);assertEquals(s.state.functions.single().id,query.source)
            val edited=s.state.copy(functions=s.state.functions.map{it.copy(expression="y=x^3")});val d=RuhiDerivativeLanguage.drawing(edited,2.0)!!;assertTrue(d.diagnostic,d.diagnostic.isEmpty())
            if(mode=="slider")assertEquals(listOf(Vec2(2.0,8.0)),d.points)
        }
    }
    @Test fun validFormulaIsNotRejectedBecauseItsDomainMissesTheDefaultSampleCoordinates() {
        val s=Session();s.say("Plot the derivative of the selected curve")
        val r=s.say("y=ln(x-100)");assertEquals(r.message,GraphCommandStatus.APPLIED,r.status)
        val q=RuhiDerivativeLanguage.decode(s.state.labSessionValues.getValue(RuhiDerivativeLanguage.MARK))
        assertEquals(.5,RuhiDerivativeMath.slope(RuhiDerivativeLanguage.resolve(s.state,q),102.0).value,1e-8)
    }

}
