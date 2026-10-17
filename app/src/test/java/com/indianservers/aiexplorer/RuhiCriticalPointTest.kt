package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.workspace.*
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import org.junit.Assert.*
import org.junit.Test

class RuhiCriticalPointTest {
    private fun analyze(expression:String,stationary:Boolean=false):Pair<RuhiExecutionResult,List<Vec2>> {
        val s=WorkspaceState(module=MathModule.Graph2D);val text=if(stationary)"Find the stationary points of $expression"else"Find critical points of y = $expression"
        val r=RuhiDialogueManager().execute(s,text,WorkspaceHistoryByModule())
        assertEquals(r.message,GraphCommandStatus.APPLIED,r.status)
        assertEquals(if(stationary)"stationary"else"critical",r.semanticFrame!!.parameters["analysis"])
        assertEquals(s.functions,r.state.functions);assertEquals(s.points,r.state.points)
        return r to RuhiDerivativeLanguage.drawing(r.state)!!.points
    }
    private fun points(actual:List<Vec2>,vararg expected:Vec2) {
        assertEquals(actual.toString(),expected.size,actual.size)
        expected.forEach{e->assertTrue("Expected $e in $actual",actual.any{it.distanceTo(e)<1e-6})}
    }
    @Test fun criticalIncludesOriginalPolynomialFiniteCornersAndSmoothAbsSquare() {
        points(analyze("x^4-2*x^2").second,Vec2(-1.0,-1.0),Vec2(0.0,0.0),Vec2(1.0,-1.0))
        points(analyze("abs(x)").second,Vec2(0.0,0.0))
        points(analyze("abs(x-2)+2*x").second,Vec2(2.0,4.0))
        points(analyze("abs(x^2)").second,Vec2(0.0,0.0))
        assertTrue(analyze("abs(x)").first.message.startsWith("Critical points"))
    }
    @Test fun stationaryExcludesCornersEvenAtSmallAmplitudeAndIncludesSmoothBranchZeros() {
        for(e in listOf("abs(x)","0.000000001*abs(x)","0.000000000000001*abs(x)","abs(x-2)+2*x","abs(x)-abs(x)+x","abs(x)-abs(x)+0.000000000000001*x"))points(analyze(e,true).second)
        points(analyze("abs(x^2)",true).second,Vec2(0.0,0.0))
        points(analyze("abs(x^3)",true).second,Vec2(0.0,0.0))
    }
    @Test fun shiftedAndScaledNonExtremumCornersUseCurrentExpressions() {
        for(a in listOf(.375,1.7,2.3))for(c in listOf(-1.25,.75,2.125)) {
            points(analyze("($a)*abs(x-($c))+2*($a)*x-1").second,Vec2(c,2*a*c-1))
        }
        points(analyze("0.000000001*abs(x)").second,Vec2(0.0,0.0))
        points(analyze("0.000000000000001*abs(x)").second,Vec2(0.0,0.0))
    }
    @Test fun canceledBranchesDoNotInventCriticalPointsAndNestedBranchesStaySmooth() {
        points(analyze("abs(x)-abs(x)+x").second)
        points(analyze("abs(abs(x)^2)").second,Vec2(0.0,0.0))
        points(analyze("abs(x^3)").second,Vec2(0.0,0.0))
    }
    @Test fun domainPolesAreExcludedWhileFiniteRationalCornersRemain() {
        points(analyze("1/x").second)
        points(analyze("abs(x)/x+x").second)
        points(analyze("abs(x-2)/(x-1)").second,Vec2(2.0,0.0))
    }
    @Test fun savedSelectedAnalysisFollowsFormulaEditsAndAnnotationUndoRedo() {
        val initial=WorkspaceState(module=MathModule.Graph2D,functions=listOf(FunctionDefinition("critical,/907","Corner907","y=abs(x)","cyan")))
        val h=WorkspaceHistoryByModule();val manager=RuhiDialogueManager()
        val r=manager.execute(initial,"Find critical points of the selected curve",h)
        assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);points(RuhiDerivativeLanguage.drawing(r.state)!!.points,Vec2(0.0,0.0))
        val changed=r.state.copy(functions=r.state.functions.map{it.copy(expression="y=abs(x-2)+2*x")})
        val saved=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(changed)).state!!
        points(RuhiDerivativeLanguage.drawing(saved)!!.points,Vec2(2.0,4.0));assertEquals(1,h.forModule(saved.module).protocol.size)
        val undone=manager.execute(saved,"Undo",h).state;assertFalse(undone.labSessionValues.containsKey(RuhiDerivativeLanguage.MARK));assertEquals(saved.functions,undone.functions)
        val redone=manager.execute(undone,"Redo",h).state;points(RuhiDerivativeLanguage.drawing(redone)!!.points,Vec2(2.0,4.0))
    }
    @Test fun branchAwareSlopesAcceptSmoothZerosAndNearCornerCoordinatesButRejectCorners() {
        assertEquals(0.0,RuhiDerivativeMath.slope(RuhiDerivativeMath.derive("y=abs(x^2)"),0.0).value,0.0)
        assertEquals(1.0,RuhiDerivativeMath.slope(RuhiDerivativeMath.derive("y=abs(x)-abs(x)+x"),0.0).value,0.0)
        for(x in listOf(1e-9,-1e-9))assertEquals(if(x>0)1.0 else -1.0,RuhiDerivativeMath.slope(RuhiDerivativeMath.derive("y=abs(x)"),x).value,0.0)
        assertTrue(runCatching{RuhiDerivativeMath.slope(RuhiDerivativeMath.derive("y=abs(x)"),0.0)}.isFailure)
    }
}
