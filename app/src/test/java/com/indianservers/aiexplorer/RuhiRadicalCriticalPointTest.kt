package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.workspace.*
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.pow
import kotlin.math.sqrt

class RuhiRadicalCriticalPointTest {
    private fun analyze(expression:String,stationary:Boolean=false):List<Vec2> {
        val s=WorkspaceState(module=MathModule.Graph2D);val r=RuhiDialogueManager().execute(s,if(stationary)"Find the stationary points of $expression"else"Find critical points of y = $expression",WorkspaceHistoryByModule())
        assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertEquals(if(stationary)"stationary"else"critical",r.semanticFrame!!.parameters["analysis"]);assertEquals(s.functions,r.state.functions);assertEquals(s.points,r.state.points)
        return RuhiDerivativeLanguage.drawing(r.state)!!.points
    }
    private fun points(actual:List<Vec2>,vararg expected:Vec2) {
        assertEquals(actual.toString(),expected.size,actual.size);expected.forEach{e->assertTrue("Expected $e in $actual",actual.any{it.distanceTo(e)<1e-6})}
    }
    @Test fun squareRadicalsHaveTheSameFiniteCornersAsTheirRealAbsoluteValueIdentities() {
        points(analyze("sqrt(x^2)"),Vec2(0.0,0.0));points(analyze("sqrt(x^2)",true))
        for(a in listOf(-1.25,.375,1.7,2.125))for(c in listOf("1","-0.75","0.000000001","0.000000000000001")) {
            val e="($c)*sqrt((x-($a))^2)+2*($c)*x";points(analyze(e),Vec2(a,2*c.toDouble()*a));points(analyze(e,true))
        }
    }
    @Test fun higherEvenPowersRetainSmoothStationaryOriginsAndShiftedParents() {
        for(n in listOf(4,6,8))for(a in listOf(0.0,.375,1.7)) {
            val e="sqrt((x-($a))^$n)";val critical=analyze(e);assertEquals("$e critical: $critical",1,critical.size);points(critical,Vec2(a,0.0));val stationary=analyze(e,true);assertEquals("$e stationary: $stationary",1,stationary.size);points(stationary,Vec2(a,0.0))
        }
    }
    @Test fun halfPowerSpellingAndNestedSquareRootsPreserveTheirRealBranches() {
        for(e in listOf("(x^2)^0.5","(x^2)^(1/2)","sqrt((sqrt(x^2))^2)")){points(analyze(e),Vec2(0.0,0.0));points(analyze(e,true))}
        points(analyze("(x^4)^0.5",true),Vec2(0.0,0.0));points(analyze("(x^4)^(1/2)",true),Vec2(0.0,0.0))
    }
    @Test fun positiveFactorsAndProductsRetainBothCornersAndInteriorSmoothZeros() {
        points(analyze("sqrt(4*x^2)"),Vec2(0.0,0.0));points(analyze("sqrt(4*x^2)",true))
        points(analyze("sqrt((x-1)^2*(x+1)^2)"),Vec2(-1.0,0.0),Vec2(0.0,1.0),Vec2(1.0,0.0))
        points(analyze("sqrt((x-1)^2*(x+1)^2)",true),Vec2(0.0,1.0))
    }
    @Test fun cancellationAndReciprocalPolesDoNotInventCriticalOrStationaryPoints() {
        for(e in listOf("sqrt(x^2)-sqrt(x^2)+x","sqrt(x^2)-abs(x)+x","1/sqrt(x^2)","sqrt((1/x)^2)","sqrt(x^(-2))")){points(analyze(e));points(analyze(e,true))}
    }
    @Test fun originalDomainsRemainAuthoritativeAfterEvenPowerIdentities() {
        val r=RuhiDerivativeMath.derive("y=sqrt((sqrt(x))^2)");assertNull(RuhiDerivativeMath.value(r,-1.0));assertEquals(.5,RuhiDerivativeMath.slope(r,1.0).value,1e-10);assertTrue(runCatching{RuhiDerivativeMath.slope(r,0.0)}.isFailure)
        points(analyze("sqrt((sqrt(x))^2)"));points(analyze("sqrt((sqrt(x))^2)",true))
        val pole=RuhiDerivativeMath.derive("y=sqrt((1/x)^2)");assertNull(RuhiDerivativeMath.value(pole,0.0));assertTrue(runCatching{RuhiDerivativeMath.slope(pole,0.0)}.isFailure)
    }
    @Test fun oneSidedSlopesStayExactNearCornersAndFiniteAtSmoothRemovableFormulaHoles() {
        val corner=RuhiDerivativeMath.derive("y=sqrt(x^2)");for(x in listOf(1e-9,-1e-9,2.0,-3.0))assertEquals(if(x>0)1.0 else -1.0,RuhiDerivativeMath.slope(corner,x).value,0.0)
        assertTrue(runCatching{RuhiDerivativeMath.slope(corner,0.0)}.isFailure)
        assertEquals(0.0,RuhiDerivativeMath.slope(RuhiDerivativeMath.derive("y=sqrt(x^4)"),0.0).value,0.0)
        assertEquals(1.0,RuhiDerivativeMath.slope(RuhiDerivativeMath.derive("y=sqrt(x^2)-abs(x)+x"),0.0).value,0.0)
        val x=.375+1e-5;assertEquals(4*(x-.375).pow(3),RuhiDerivativeMath.slope(RuhiDerivativeMath.derive("y=sqrt((x-0.375)^8)"),x).value,1e-25)
    }
    @Test fun savedCurrentCurveAnalysisFollowsRadicalEditsAndKeepsAtomicAnnotationHistory() {
        val initial=WorkspaceState(module=MathModule.Graph2D,functions=listOf(FunctionDefinition("radical,/907","Root907","y=sqrt(x^2)","cyan")))
        val manager=RuhiDialogueManager();val h=WorkspaceHistoryByModule();val r=manager.execute(initial,"Find critical points of the selected curve",h)
        assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);points(RuhiDerivativeLanguage.drawing(r.state)!!.points,Vec2(0.0,0.0));assertEquals(1,h.forModule(initial.module).protocol.size)
        val changed=r.state.copy(functions=r.state.functions.map{it.copy(expression="y=sqrt((x-2)^2)+2*x")});val restored=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(changed)).state!!
        points(RuhiDerivativeLanguage.drawing(restored)!!.points,Vec2(2.0,4.0));assertEquals(initial.points,restored.points)
        val undone=manager.execute(restored,"Undo",h).state;assertFalse(undone.labSessionValues.containsKey(RuhiDerivativeLanguage.MARK));assertEquals(restored.functions,undone.functions)
        val redone=manager.execute(undone,"Redo",h).state;points(RuhiDerivativeLanguage.drawing(redone)!!.points,Vec2(2.0,4.0))
    }
    @Test fun rationalPowersRetainIndependentNumericMeaningThroughCasRendering() {
        val cas=SymbolicCasEngine();val engine=ExpressionEngine()
        val cases=listOf<Pair<String,(Double)->Double>>("x^(1/2)" to {x->sqrt(x)},"x^(-1/2)" to {x->1/sqrt(x)},"(1/2)^x" to {x->.5.pow(x)},"(2/3)^x" to {x->(2.0/3).pow(x)},"(x+1)^(3/2)" to {x->(x+1).pow(1.5)})
        for((source,expected) in cases)for(x in listOf(.25,1.0,4.0))for(tree in listOf(cas.parse(source),cas.simplify(cas.parse(source)))) {
            val rendered=cas.render(tree);assertEquals("$source -> $rendered at $x",expected(x),engine.compile(rendered).eval(mapOf("x" to x)),1e-10)
        }
        val nested=listOf<Pair<String,(Double)->Double>>("(x^(-1))^2" to {x->1/(x*x)},"(-x)^2" to {x->x*x},"(-2)^2" to {_->4.0},"((x+1)^2)^3" to {x->(x+1).pow(6)})
        for((source,expected) in nested)for(x in listOf(-2.0,.25,2.0))for(tree in listOf(cas.parse(source),cas.simplify(cas.parse(source)))){val rendered=cas.render(tree);assertEquals("$source -> $rendered at $x",expected(x),engine.compile(rendered).eval(mapOf("x" to x)),1e-10)}
    }
    @Test fun exactCasRadicalDerivativesAgreeWithElementarySlopesOnTheirRealDomain() {
        val cas=SymbolicCasEngine();val engine=ExpressionEngine()
        val cases=listOf<Pair<String,(Double)->Double>>("sqrt(x)" to {x->1/(2*sqrt(x))},"1/sqrt(x)" to {x->-.5*x.pow(-1.5)})
        for((source,expected) in cases) {
            val derivative=cas.derivative(source);assertTrue(derivative.toString(),derivative.supported)
            for(x in listOf(.25,1.0,4.0,9.0))assertEquals("$source -> ${derivative.exact} at $x",expected(x),engine.compile(derivative.exact).eval(mapOf("x" to x)),1e-10)
        }
    }
}
