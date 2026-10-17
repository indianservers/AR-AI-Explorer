package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.workspace.*
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import org.junit.Assert.*
import org.junit.Test

class RuhiDerivativeDomainFollowUpTest {
    private class Session(var state:WorkspaceState) {
        val h=WorkspaceHistoryByModule();val manager=RuhiDialogueManager()
        fun say(text:String,c:RuhiSceneContext=RuhiSceneContext())=manager.execute(state,text,h,c).also{state=it.state}
        fun restore(){state=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(state)).state!!}
    }
    private fun scene(expressions:List<String>)=WorkspaceState(module=MathModule.Graph2D,functions=expressions.mapIndexed{i,e->FunctionDefinition("domain,/$i","Curve_$i",e,"cyan")})
    @Test fun logarithmQuestionOffersOnlyPositiveFiniteDerivativeExamples() {
        val s=Session(scene(listOf("y=ln(x)")));val r=s.say("Show the tangent slope at the selected point")
        assertEquals(GraphCommandStatus.CLARIFICATION,r.status);assertTrue(r.dialogue!!.suggestions.isNotEmpty())
        r.dialogue!!.suggestions.forEach{val x=it.answer.substringAfterLast(' ').toDouble();assertTrue("Invalid logarithm suggestion $x",x>0&&x.isFinite())}
    }
    @Test fun choosingCurveWithUndefinedEarlierXAsksNewXAndRetainsCurveAfterRestore() {
        val s=Session(scene(listOf("y=x^2","y=ln(x)")))
        s.say("What is the instantaneous gradient here?");val q=s.say("0")
        assertEquals(GraphCommandStatus.CLARIFICATION,q.status);s.restore();val r=s.say(q.dialogue!!.suggestions.last().answer)
        assertEquals(r.message,GraphCommandStatus.CLARIFICATION,r.status);val pending=RuhiDialogueMemory.read(s.state).pending!!
        assertEquals("derivative_x",pending.kind);assertEquals("domain,/1",pending.slots["curve_id"]);assertNull(pending.slots["x"]);assertTrue(s.h.forModule(s.state.module).protocol.isEmpty())
        s.restore();val out=s.say("2");assertEquals(out.message,GraphCommandStatus.APPLIED,out.status);assertTrue(out.message.contains("slope = 0.5"));assertEquals("domain,/1",RuhiDerivativeLanguage.decode(s.state.labSessionValues.getValue(RuhiDerivativeLanguage.MARK)).source)
    }
    @Test fun examplesRespectLogSquareRootReciprocalAndAbsoluteValueDomains() {
        for(expression in listOf("y=ln(x)","y=sqrt(x)","y=1/x","y=abs(x)")) {
            val initial=scene(listOf(expression));val q=Session(initial).say("What is the instantaneous gradient here?")
            assertEquals(GraphCommandStatus.CLARIFICATION,q.status);assertTrue(q.dialogue!!.suggestions.size in 1..3)
            q.dialogue!!.suggestions.forEach{chip->
                val x=chip.answer.substringAfterLast(' ').toDouble()
                assertTrue("$expression x=$x",if(expression.contains("ln")||expression.contains("sqrt"))x>0 else x!=0.0)
                val out=Session(q.state).say(chip.answer);assertEquals(out.message,GraphCommandStatus.APPLIED,out.status)
                val slope=Regex("slope = ([-+0-9.eE]+)").find(out.message)!!.groupValues[1].toDouble()
                val expected=when(expression){"y=ln(x)"->1/x;"y=sqrt(x)"->1/(2*kotlin.math.sqrt(x));"y=1/x"->-1/(x*x);else->if(x>0)1.0 else -1.0}
                assertEquals(expected,slope,1e-8);assertEquals(initial.functions,out.state.functions);assertEquals(initial.points,out.state.points)
            }
        }
    }
    @Test fun unavailableSampleLocationsProduceNoFabricatedValidChips() {
        val s=Session(scene(listOf("y=ln(x-100)")));val q=s.say("Show the tangent slope at the selected point")
        assertEquals(q.message,GraphCommandStatus.CLARIFICATION,q.status);assertTrue(q.dialogue!!.suggestions.isEmpty());s.restore()
        val out=s.say("101");assertEquals(out.message,GraphCommandStatus.APPLIED,out.status);assertTrue(out.message.contains("slope = 1.0"))
    }
    @Test fun nonsmoothSelectedPointAsksForAnotherLocationWithoutProjectingOrAddingHistory() {
        val initial=scene(listOf("y=abs(x)")).copy(points=listOf(Vec2(0.0,0.0)));val s=Session(initial)
        val q=s.say("Show the tangent slope at the selected point",RuhiSceneContext(selectedPointIndex=0))
        assertEquals(q.message,GraphCommandStatus.CLARIFICATION,q.status);assertEquals("derivative_x",RuhiDialogueMemory.read(s.state).pending!!.kind);assertTrue(s.h.forModule(s.state.module).protocol.isEmpty());assertEquals(initial.points,s.state.points)
        assertTrue(q.dialogue!!.suggestions.none{it.answer.substringAfterLast(' ').toDouble()==0.0});s.restore()
        val out=s.say("-1");assertEquals(out.message,GraphCommandStatus.APPLIED,out.status);assertTrue(out.message.contains("slope = -1.0"));assertEquals(initial.points,s.state.points)
    }
    @Test fun repeatedUndefinedXKeepsCurveAndDoesNotCreateUndoEntries() {
        val s=Session(scene(listOf("y=ln(x)")));s.say("What is the instantaneous gradient here?")
        for(x in listOf("0","-1","-2")) {
            val q=s.say(x);assertEquals(q.message,GraphCommandStatus.CLARIFICATION,q.status);assertEquals("domain,/0",RuhiDialogueMemory.read(s.state).pending!!.slots["curve_id"]);assertNull(RuhiDialogueMemory.read(s.state).pending!!.slots["x"]);assertTrue(s.h.forModule(s.state.module).protocol.isEmpty());s.restore()
        }
        val out=s.say("2");assertEquals(GraphCommandStatus.APPLIED,out.status);assertEquals(1,s.h.forModule(s.state.module).protocol.size)
    }
    @Test fun literalUndefinedEvaluationRemainsAnHonestRejection() {
        val initial=scene(emptyList());val s=Session(initial)
        val out=s.say("Calculate derivative at x = 0 for y = ln(x)")
        assertEquals(out.message,GraphCommandStatus.INVALID,out.status);assertTrue(out.message.contains("undefined"));assertNull(RuhiDialogueMemory.read(s.state).pending);assertTrue(s.h.forModule(s.state.module).protocol.isEmpty());assertEquals(initial.functions,s.state.functions)
    }
    @Test fun curveEditsInvalidateSavedDomainChoicesInsteadOfUsingStaleExamples() {
        val s=Session(scene(listOf("y=ln(x)")));val q=s.say("Show the tangent slope at the selected point");s.restore()
        s.state=s.state.copy(functions=s.state.functions.map{it.copy(expression="y=x^2")});val out=s.say(q.dialogue!!.suggestions.first().answer)
        assertEquals(GraphCommandStatus.CLARIFICATION,out.status);assertTrue(out.message.contains("scene changed",true));assertNull(RuhiDialogueMemory.read(s.state).pending);assertTrue(s.h.forModule(s.state.module).protocol.isEmpty())
    }
}
