package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.workspace.*
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs
import kotlin.math.pow

class RuhiRadicalDerivativePlotTest {
    private val engine=ExpressionEngine()
    private fun helper(s:WorkspaceState)=RuhiDerivativeLanguage.drawing(s)!!.helpers.single{it.name=="Derivative"}.curve as RuhiIntersectionMath.Curve.Scalar
    private fun value(curve:RuhiIntersectionMath.Curve.Scalar,x:Double):Double?=runCatching {
        val vars=mapOf("x" to x)
        curve.restriction?.let{val allowed=engine.compile(it).eval(vars);require(allowed.isFinite()&&allowed!=0.0)}
        engine.compile(curve.expression).eval(vars).also{require(it.isFinite())}
    }.getOrNull()
    private fun analyze(expression:String):RuhiExecutionResult {
        val initial=WorkspaceState(module=MathModule.Graph2D,functions=emptyList(),surfaceLayers=emptyList())
        val r=RuhiDialogueManager().execute(initial,"Differentiate $expression",WorkspaceHistoryByModule())
        assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertEquals("DIFFERENTIATE",r.semanticFrame!!.intent);assertEquals(initial.functions,r.state.functions);assertEquals(initial.points,r.state.points)
        assertEquals("",RuhiDerivativeLanguage.drawing(r.state)!!.diagnostic)
        return r
    }
    @Test fun originalDevelopmentProbeRepairsFiniteHolesButPreservesTheTrueCusp() {
        val cases=listOf(Triple("x^2",0.0,0.0),Triple("sqrt(x^4)",0.0,0.0),Triple("sqrt(x^4)",2.0,4.0),Triple("sqrt((x-1.25)^4)",1.25,0.0),Triple("sqrt(x^2)-abs(x)+x",0.0,1.0),Triple("sqrt(x^6)",0.0,0.0),Triple("sqrt(x^2)",-2.0,-1.0))
        for((formula,x,expected) in cases)assertEquals("$formula at $x",expected,value(helper(analyze(formula).state),x)!!,1e-10)
        assertNull(value(helper(analyze("sqrt(x^2)").state),0.0))
    }
    @Test fun plottedSmoothEvenRadicalsAgreeWithTheirIndependentRealDerivatives() {
        for(n in listOf(4,6,8))for(a in listOf(-1.25,.375,1.7)) {
            val source="sqrt((x-($a))^$n)";val curve=helper(analyze(source).state);val k=n/2
            for(x in listOf(a-2,a-1e-6,a,a+1e-6,a+2)) {
                val u=x-a;val expected=if(u==0.0)0.0 else k*u*abs(u).pow(k-2)
                assertEquals("$source at $x",expected,value(curve,x)!!,1e-10)
            }
        }
        val nested=helper(analyze("sqrt((abs(x))^6)").state);for(x in listOf(-2.0,0.0,2.0))assertEquals(3*x*abs(x),value(nested,x)!!,1e-10)
    }
    @Test fun canceledEquivalentAbsoluteValuesLeaveNoInventedHole() {
        for(source in listOf("sqrt(x^2)-abs(x)+x","sqrt(4*x^2)-2*abs(x)+x","sqrt(9*x^2)-3*abs(-x)+x","sqrt(0.25*x^2)-0.5*abs(x)+x")) {
            val curve=helper(analyze(source).state)
            for(x in listOf(-2.0,-1e-9,0.0,1e-9,3.0))assertEquals("$source at $x",1.0,value(curve,x)!!,1e-10)
        }
    }
    @Test fun negativeTinyAndFractionalCoefficientsKeepTheirActualSlopeValues() {
        for(coefficient in listOf("-2","0.5","0.000000000000001")) {
            val source="($coefficient)*sqrt(x^4)";val curve=helper(analyze(source).state)
            for(x in listOf(-2.0,0.0,3.0))assertEquals("$source at $x",2*coefficient.toDouble()*x,value(curve,x)!!,1e-25)
        }
    }
    @Test fun originalPolesAndCanceledSourceDomainsAreStillExcluded() {
        val cases=listOf<Pair<String,(Double)->Double>>("sqrt((1/x)^4)" to {x->-2/x.pow(3)},"sqrt((1/x)^6)" to {x->-3*(if(x>0)1 else -1)/abs(x).pow(4)},"x/x" to {_->0.0},"sqrt(x^2)/x" to {_->0.0})
        for((source,expected) in cases){val curve=helper(analyze(source).state);assertNull(source,value(curve,0.0));for(x in listOf(-2.0,2.0))assertEquals("$source at $x",expected(x),value(curve,x)!!,1e-10)}
        val restricted=helper(analyze("sqrt((sqrt(x))^2)").state);assertNull(value(restricted,-1.0));assertNull(value(restricted,0.0));assertEquals(.5,value(restricted,1.0)!!,1e-10)
    }
    @Test fun trueAbsoluteValueCornersKeepUndefinedDerivativesAndTheirTwoDifferentSides() {
        for(source in listOf("abs(x)","sqrt(x^2)")) {
            val curve=helper(analyze(source).state);assertNull(source,value(curve,0.0));assertEquals(-1.0,value(curve,-1e-9)!!,1e-10);assertEquals(1.0,value(curve,1e-9)!!,1e-10)
        }
        val shifted=helper(analyze("sqrt((x-1.25)^2)+2*x").state);assertNull(value(shifted,1.25));assertEquals(1.0,value(shifted,1.25-1e-9)!!,1e-10);assertEquals(3.0,value(shifted,1.25+1e-9)!!,1e-10)
    }
    @Test fun firstAndSecondAbsolutePowerRulesHaveFiniteSmoothZeros() {
        for(n in 2..5)for(order in 1..2) {
            val r=RuhiDerivativeMath.derive("y=abs(x)^$n",order)
            for(x in listOf(-2.0,0.0,2.0)) {
                val expected=if(order==1)n*x*abs(x).pow(n-2)else n*(n-1)*abs(x).pow(n-2)
                assertEquals("n=$n order=$order x=$x",expected,engine.compile(r.derivative).eval(mapOf("x" to x)),1e-10)
            }
        }
    }
    @Test fun matchingAbsoluteProductsUseValidSmoothRulesWithoutFusingDifferentArguments() {
        val calculus=SymbolicCalculusEngine()
        for(m in 1..3)for(k in 1..3)for(source in listOf("x^$m*abs(x)^$k","abs(x)^$k*x^$m")) {
            val derivative=calculus.differentiate(source).expression
            for(x in listOf(-2.0,0.0,2.0))assertEquals("$source at $x",(m+k)*x.pow(m-1)*abs(x).pow(k),engine.compile(derivative).eval(mapOf("x" to x)),1e-10)
        }
        val control=calculus.differentiate("x*abs(x+1)").expression;assertEquals(1.0,engine.compile(control).eval(mapOf("x" to 0.0)),1e-10);assertFalse(engine.compile(control).eval(mapOf("x" to -1.0)).isFinite())
    }
    @Test fun savedSelectedCurveUsesTheSameIdentityAndLiveFormulaAfterReorderAndUndo() {
        val initial=WorkspaceState(module=MathModule.Graph2D,functions=listOf(FunctionDefinition("root,/907","Root907","y=sqrt(x^4)","cyan"),FunctionDefinition("other-811","Other811","y=x^2+1","green")),surfaceLayers=emptyList())
        val h=WorkspaceHistoryByModule();val manager=RuhiDialogueManager();val r=manager.execute(initial,"Plot the derivative of the selected curve",h,RuhiSceneContext(selectedFunctionIds=listOf("root,/907")))
        assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertEquals(0.0,value(helper(r.state),0.0)!!,0.0);assertEquals(1,h.forModule(initial.module).protocol.size)
        val edited=r.state.copy(functions=r.state.functions.reversed().map{if(it.id=="root,/907")it.copy(expression="y=sqrt((x-1.25)^4)",name="Renamed662")else it});val restored=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(edited)).state!!
        assertEquals(0.0,value(helper(restored),1.25)!!,0.0);assertEquals(1.5,value(helper(restored),2.0)!!,1e-10);assertEquals(initial.functions[1],restored.functions[0])
        val undone=manager.execute(restored,"Undo",h).state;assertFalse(undone.labSessionValues.containsKey(RuhiDerivativeLanguage.MARK));assertEquals(restored.functions,undone.functions)
        val redone=manager.execute(undone,"Redo",h).state;assertEquals(0.0,value(helper(redone),1.25)!!,0.0)
    }
    @Test fun hiddenOrDeletedReferencedCurveCannotSilentlyRebindToAnotherCurve() {
        val initial=WorkspaceState(module=MathModule.Graph2D,functions=listOf(FunctionDefinition("root-907","Root907","y=sqrt(x^4)","cyan"),FunctionDefinition("other-811","Other811","y=x^2+1","green")),surfaceLayers=emptyList())
        val r=RuhiDialogueManager().execute(initial,"Plot the derivative of the selected curve",WorkspaceHistoryByModule(),RuhiSceneContext(selectedFunctionIds=listOf("root-907")))
        for(changed in listOf(r.state.copy(functions=r.state.functions.map{if(it.id=="root-907")it.copy(visible=false)else it}),r.state.copy(functions=r.state.functions.filter{it.id!="root-907"}))) {
            val drawing=RuhiDerivativeLanguage.drawing(WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(changed)).state!!)!!;assertTrue(drawing.helpers.isEmpty());assertTrue(drawing.diagnostic.isNotEmpty());assertEquals(initial.functions[1],changed.functions.last())
        }
    }
    @Test fun symbolicNestedPowerDerivativesKeepTheirIndependentNumericMeaning() {
        val calculus=SymbolicCalculusEngine()
        val cases=listOf<Pair<String,(Double)->Double>>("(x^-1)^3" to {x->-3/x.pow(4)},"((x+1)^2)^4" to {x->8*(x+1).pow(7)},"(-x)^3" to {x->-3*x*x})
        for((source,expected) in cases)for(x in listOf(-2.0,.25,2.0)){val derivative=calculus.differentiate(source).expression;assertEquals("$source -> $derivative at $x",expected(x),engine.compile(derivative).eval(mapOf("x" to x)),1e-10)}
    }
    @Test fun unaryPowersAndRadicalCanonicalizationPreserveConventionalPrecedence() {
        val cas=SymbolicCasEngine();val cases=listOf<Pair<String,(Double)->Double>>("-x^2" to {x->-x*x},"(-x)^2" to {x->x*x},"-2^2" to {_->-4.0},"2^-2" to {_->.25},"2^3^2" to {_->512.0},"x^-2" to {x->1/(x*x)})
        for((source,expected) in cases)for(x in listOf(-2.0,2.0)){val rendered=cas.render(cas.parse(source));assertEquals("$source -> $rendered at $x",expected(x),engine.compile(rendered).eval(mapOf("x" to x)),1e-10)}
        val curve=helper(analyze("-x^2+sqrt(x^4)+x").state);for(x in listOf(-2.0,0.0,2.0))assertEquals(1.0,value(curve,x)!!,1e-10)
    }
}
