package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.workspace.*
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import org.junit.Assert.*
import org.junit.Test

class RuhiFormulaReplyLanguageTest {
    private class Session {
        var state=WorkspaceState(module=MathModule.Graph2D,functions=emptyList(),surfaceLayers=emptyList())
        val history=WorkspaceHistoryByModule();val manager=RuhiDialogueManager()
        fun say(source:String)=manager.execute(state,source,history).also{state=it.state}
        fun restore(){state=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(state)).state!!}
    }
    private fun derivative(s:Session,x:Double):Double {val q=RuhiDerivativeLanguage.decode(s.state.labSessionValues.getValue(RuhiDerivativeLanguage.MARK));return RuhiDerivativeMath.slope(RuhiDerivativeLanguage.resolve(s.state,q),x).value}
    private fun integral(s:Session)=RuhiIntegralLanguage.drawing(s.state)!!.values!!.signed
    @Test fun formalAndMathematicalWordRepliesKeepBothOriginalSelectedAnalyses() {
        for(family in listOf("derivative","integral"))for(formula in listOf("y=x^2","y equals x squared","x squared","1x squared")) {
            val s=Session();s.say(if(family=="derivative")"Plot the derivative of the selected curve"else"Integrate the selected function");s.restore()
            val r=s.say(formula);assertEquals(r.message,if(family=="derivative")GraphCommandStatus.APPLIED else GraphCommandStatus.CLARIFICATION,r.status)
            if(family=="integral"){assertTrue(s.state.functions.isEmpty());s.restore();assertEquals(GraphCommandStatus.APPLIED,s.say("0 to 2").status);assertEquals(8.0/3,integral(s),1e-7)}else assertEquals(4.0,derivative(s,2.0),1e-8)
            assertEquals(1,s.state.functions.size);assertEquals(1,s.history.forModule(s.state.module).protocol.size);assertNull(RuhiDialogueMemory.read(s.state).pending)
            s.say("Undo");assertTrue(s.state.functions.isEmpty());s.say("Redo");assertEquals(1,s.state.functions.size)
        }
    }
    @Test fun variedNumericCoefficientsAndWordOperatorsUseIndependentCurrentMathematics() {
        for(a in listOf(.375,1.7,2.3))for(family in listOf("derivative","integral")) {
            val s=Session();s.say(if(family=="derivative")"Plot the derivative of the selected curve"else"Integrate the selected function");s.restore()
            val source="y equals $a x squared plus two x minus five";val r=s.say(source)
            assertEquals(r.message,if(family=="derivative")GraphCommandStatus.APPLIED else GraphCommandStatus.CLARIFICATION,r.status)
            if(family=="derivative")assertEquals(2*a*1.75+2,derivative(s,1.75),1e-8)else{s.say("0 to 2");assertEquals(a*8/3+4-10,integral(s),1e-7)}
            assertTrue(RuhiDialogueMemory.read(s.state).turns.any{it.user==source})
        }
    }
    @Test fun savedGradientXSurvivesPoliteCubicWordReply() {
        val s=Session();s.say("What is the instantaneous gradient here?");s.say("2");s.restore()
        val r=s.say("Please y equals x cubed");assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertTrue(r.message.contains("slope = 12.0"));assertEquals(listOf(Vec2(2.0,8.0)),RuhiDerivativeLanguage.drawing(s.state)!!.points)
    }
    @Test fun contextualUsePrefixKeepsExplicitIntegralBoundsAndDomainValidatedSuggestions() {
        val s=Session();s.say("Shade area under the currently selected curve between 1 and 4");s.restore();val r=s.say("Use y equals two x squared");assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertEquals(42.0,integral(s),1e-7)
        val log=Session();log.say("Integrate the selected function");val q=log.say("let y equal ln(x)");assertEquals(q.message,GraphCommandStatus.CLARIFICATION,q.status);assertFalse(q.dialogue!!.suggestions.any{it.label=="0 to 1"});log.restore();log.say("1 to 2");assertEquals(2*kotlin.math.ln(2.0)-1,integral(log),1e-7)
    }
    @Test fun invalidWordEquationsRetainTheirPendingAnalysisWithoutSceneEdits() {
        for(family in listOf("derivative","integral")) {
            val s=Session();s.say(if(family=="derivative")"Plot the derivative of the selected curve"else"Integrate the selected function")
            for(formula in listOf("y equals x squared plus banana","y equals x squared plus","y equals one over zero")) {
                val r=s.say(formula);assertEquals(r.message,GraphCommandStatus.INVALID,r.status);assertTrue(s.state.functions.isEmpty());assertTrue(s.history.forModule(s.state.module).protocol.isEmpty());assertEquals("${family}_formula",RuhiDialogueMemory.read(s.state).pending!!.kind);s.restore()
            }
        }
    }
    @Test fun unrelatedNewCommandsAreNotConsumedAsFormulaReplies() {
        for(family in listOf("derivative","integral")) {
            val s=Session();s.say(if(family=="derivative")"Plot the derivative of the selected curve"else"Integrate the selected function")
            val r=s.say("Plot y = x cubed");assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertEquals("PLOT_FUNCTION_2D",r.semanticFrame!!.intent);assertNull(RuhiDialogueMemory.read(s.state).pending);assertFalse(s.state.labSessionValues.containsKey(RuhiDerivativeLanguage.MARK));assertFalse(s.state.labSessionValues.containsKey(RuhiIntegralLanguage.MARK))
        }
    }
    @Test fun squareRootWordReplyUsesOriginalDomainForMathematicsAndRecovery() {
        val s=Session();s.say("Shade area under the currently selected curve between 1 and 4");val r=s.say("square root of x");assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertEquals(14.0/3,integral(s),1e-7)
        val d=Session();d.say("What is the instantaneous gradient here?");d.say("0");val q=d.say("y equals sqrt(x)");assertEquals(q.message,GraphCommandStatus.CLARIFICATION,q.status);assertTrue(d.state.functions.isEmpty());d.restore();val done=d.say("4");assertEquals(done.message,GraphCommandStatus.APPLIED,done.status);assertEquals(.25,derivative(d,4.0),1e-8)
    }
}
