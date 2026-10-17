package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.workspace.*
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class RuhiAbsoluteValueExplanationTest {
    private class Session(var state:WorkspaceState=WorkspaceState(module=MathModule.Graph2D)) {
        val history=WorkspaceHistoryByModule();private val manager=RuhiDialogueManager()
        fun say(text:String)=manager.execute(state,text,history).also{state=it.state}
        fun restore(){state=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(state)).state!!}
    }
    private fun check(s:Session,r:RuhiExecutionResult,x:Double) {
        assertEquals(r.message,GraphCommandStatus.APPLIED,r.status)
        assertEquals(x,(r.semanticFrame!!.parameters["at_x"] as Number).toDouble(),0.0)
        assertEquals(listOf(Vec2(x,abs(x))),RuhiDerivativeLanguage.drawing(s.state)!!.points)
        assertEquals(listOf(x),RuhiDerivativeLanguage.decode(s.state.labSessionValues.getValue(RuhiDerivativeLanguage.MARK)).xs)
        assertNull(RuhiDialogueMemory.read(s.state).pending)
    }
    @Test fun originalZeroExplanationKeepsUnequalOneSidedSlopesAndSavedMarker() {
        val s=Session();val r=s.say("Explain why absolute value is not differentiable at zero");check(s,r,0.0)
        assertTrue(r.message.contains("left-hand slope is -1"));assertTrue(r.message.contains("right-hand slope is 1"));s.restore()
        assertEquals(listOf(Vec2(0.0,0.0)),RuhiDerivativeLanguage.drawing(s.state)!!.points)
    }
    @Test fun explicitCoordinatesCorrectTheFalsePremiseAndUseTheirOwnMarker() {
        for(x in listOf(2.0,-3.0,.125,-2.75,1e-9,-1e-9,Double.MIN_VALUE,-Double.MIN_VALUE)) {
            val s=Session();val r=s.say("Explain why absolute value is not differentiable at x = $x");check(s,r,x)
            assertTrue(r.message,r.message.contains("is differentiable at x=$x"));assertTrue(r.message.contains("derivative is ${if(x>0)1.0 else -1.0}"))
            assertFalse(r.message.contains("not differentiable at zero"))
        }
    }
    @Test fun plainAndSpokenCoordinatesAreEntityValuesRatherThanTheZeroTemplate() {
        for((text,x) in listOf("at 2" to 2.0,"at minus three" to -3.0,"at x equals two" to 2.0)) {
            val s=Session();check(s,s.say("Explain why absolute value is not differentiable $text"),x)
        }
    }
    @Test fun missingCoordinateAsksOneQuestionAndRestoredNumericRepliesUseLiteralAbs() {
        for(x in listOf(-1.0,0.0,2.0)) {
            val s=Session(WorkspaceState(module=MathModule.Graph2D,functions=listOf(FunctionDefinition("unrelated-log","Log","y=ln(x)","cyan"))))
            val q=s.say("Explain why absolute value is not differentiable")
            assertEquals(GraphCommandStatus.CLARIFICATION,q.status);assertFalse(s.state.labSessionValues.containsKey(RuhiDerivativeLanguage.MARK));assertTrue(s.history.forModule(s.state.module).protocol.isEmpty())
            assertTrue(q.dialogue!!.suggestions.any{it.label=="x = 0"});s.restore()
            if(x==2.0)s.state=s.state.copy(functions=s.state.functions.map{it.copy(expression="y=sqrt(x)")})
            check(s,s.say("$x"),x)
            assertEquals(if(x==2.0)"y=sqrt(x)"else"y=ln(x)",s.state.functions.single().expression)
            assertNull(s.state.labSessionValues[GraphAssistant.selectionKey(MathModule.Graph2D)])
        }
    }
    @Test fun invalidCoordinatesAndCancellationLeaveAnalysisAndHistoryUntouched() {
        for(value in listOf("abc","NaN","infinity","10001","(2,3)")) {
            val s=Session();val before=s.state;val r=s.say("Explain why absolute value is not differentiable at x = $value")
            assertEquals(value,GraphCommandStatus.INVALID,r.status);assertFalse(s.state.labSessionValues.containsKey(RuhiDerivativeLanguage.MARK));assertEquals(before.functions,s.state.functions);assertTrue(s.history.forModule(s.state.module).protocol.isEmpty())
        }
        val s=Session();s.say("Explain why absolute value is not differentiable");s.restore();s.say("Cancel that")
        assertNull(RuhiDialogueMemory.read(s.state).pending);assertFalse(s.state.labSessionValues.containsKey(RuhiDerivativeLanguage.MARK));assertTrue(s.history.forModule(s.state.module).protocol.isEmpty())
    }
    @Test fun savedExplanationAndUndoRedoRetainTheRequestedCoordinate() {
        val s=Session();check(s,s.say("Explain why absolute value is not differentiable at x = -3"),-3.0);s.restore()
        assertEquals(listOf(Vec2(-3.0,3.0)),RuhiDerivativeLanguage.drawing(s.state)!!.points);assertEquals(1,s.history.forModule(s.state.module).protocol.size)
        s.say("Undo");assertFalse(s.state.labSessionValues.containsKey(RuhiDerivativeLanguage.MARK));s.say("Redo")
        assertEquals(listOf(Vec2(-3.0,3.0)),RuhiDerivativeLanguage.drawing(s.state)!!.points)
    }
}
