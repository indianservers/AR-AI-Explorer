package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.workspace.*
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class RuhiDerivativeLanguageTest {
    private fun empty()=WorkspaceState(module=MathModule.Graph2D,surfaceLayers=emptyList())
    private fun query(s:WorkspaceState,t:String,c:RuhiSceneContext=RuhiSceneContext(),h:WorkspaceHistoryByModule=WorkspaceHistoryByModule())=RuhiDialogueManager().execute(s,t,h,c)
    @Test fun originalRowsKeepSourceSlotsAndIndependentDerivativeIdentities() {
        val root=requireNotNull(generateSequence(java.io.File(requireNotNull(System.getProperty("user.dir")))){it.parentFile}.firstOrNull{java.io.File(it,"docs/qa/ruhi-500/replay.tsv").exists()})
        fun decode(v:String)=String(java.util.Base64.getDecoder().decode(v),Charsets.UTF_8)
        val rows=java.io.File(root,"docs/qa/ruhi-500/replay.tsv").readLines().map{it.split('\t')}.filter{it[2]=="DIFFERENTIATE"};assertEquals(25,rows.size)
        val errors=mutableListOf<String>()
        rows.forEach{row->val s=RuhiSeed500Fixture.state(row[3],row[0].toInt());val c=RuhiSeed500Fixture.context(row[0].toInt());val r=query(s,decode(row[1]),c);val check=RuhiDerivativeOracle.check(RuhiSeedJson.objectValue(decode(row[6])),s,r,c);if(!check.verified)errors+="${row[0]} ${check.diagnostics}"}
        assertTrue(errors.joinToString("\n"),errors.isEmpty())
    }
    @Test fun changedCoefficientsAndDomainsProduceComputedSlopesAndNormals() {
        for(a in listOf(.375,1.75,4.2)){val r=RuhiDerivativeMath.derive("y=$a*x^2+3*x-2");for(x in listOf(-2.5,.5,3.0)){val slope=RuhiDerivativeMath.slope(r,x);assertEquals(2*a*x+3,slope.value,1e-7);assertEquals(a*x*x+3*x-2,slope.point.y,1e-8)}}
        val normal=RuhiDerivativeMath.line(RuhiDerivativeMath.slope(RuhiDerivativeMath.derive("y=x^2"),0.0),true);assertEquals(RuhiIntersectionMath.Curve.Vertical(0.0),normal)
        assertTrue(runCatching{RuhiDerivativeMath.slope(RuhiDerivativeMath.derive("y=abs(x)"),0.0)}.isFailure)
        assertTrue(runCatching{RuhiDerivativeMath.slope(RuhiDerivativeMath.derive("y=1/x"),0.0)}.isFailure)
        assertTrue(runCatching{RuhiDerivativeMath.slope(RuhiDerivativeMath.derive("y=sqrt(x)"),0.0)}.isFailure)
    }
    @Test fun stableCurveChoicesRestoreResumeAndRejectStaleReplies() {
        val s=empty().copy(functions=listOf(FunctionDefinition("id,/907","Motion_907","y=x^2","cyan"),FunctionDefinition("id,/811","Motion_811","y=x^3","red")))
        val r=query(s,"Plot the derivative of the selected curve");assertEquals(r.message,GraphCommandStatus.CLARIFICATION,r.status);assertEquals(2,r.dialogue!!.suggestions.size)
        val loaded=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(r.state),recover=false).state!!;assertEquals(r.dialogue,RuhiDialogueManager().restoredView(loaded))
        val out=query(loaded,r.dialogue!!.suggestions.first().answer);assertEquals(out.message,GraphCommandStatus.APPLIED,out.status);assertEquals(s.functions,out.state.functions)
        val edited=out.state.copy(functions=out.state.functions.mapIndexed{i,f->if(i==0)f.copy(expression="y=3*x^2")else f});val derivative=RuhiDerivativeLanguage.drawing(edited)!!.helpers.single{it.name=="Derivative"}.curve as RuhiIntersectionMath.Curve.Scalar;assertEquals(12.0,RuhiIntersectionMath.y(derivative,2.0)!!,1e-7)
        val stale=query(r.state.copy(functions=s.functions.drop(1)),r.dialogue!!.suggestions.first().answer);assertEquals(GraphCommandStatus.CLARIFICATION,stale.status);assertNull(RuhiDialogueMemory.read(stale.state).pending)
        assertNull(RuhiDialogueMemory.read(query(r.state,"Never mind").state).pending)
    }
    @Test fun annotationsPersistAndUndoWithoutChangingOtherObjects() {
        val s=empty();val h=WorkspaceHistoryByModule();val r=query(s,"Draw a tangent to y = x squared at x = 2",h=h);assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertEquals(1,h.forModule(s.module).protocol.size)
        val loaded=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(r.state),recover=false).state!!;assertEquals(RuhiDerivativeLanguage.drawing(r.state),RuhiDerivativeLanguage.drawing(loaded))
        val undo=query(r.state,"Undo",h=h);assertNull(RuhiDerivativeLanguage.drawing(undo.state));val redo=query(undo.state,"Redo",h=h);assertNotNull(RuhiDerivativeLanguage.drawing(redo.state))
    }
}
