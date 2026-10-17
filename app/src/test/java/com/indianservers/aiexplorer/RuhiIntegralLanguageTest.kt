package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.workspace.*
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class RuhiIntegralLanguageTest {
    private fun empty()=WorkspaceState(module=MathModule.Graph2D,surfaceLayers=emptyList())
    private fun query(s:WorkspaceState,t:String,h:WorkspaceHistoryByModule=WorkspaceHistoryByModule())=RuhiDialogueManager().execute(s,t,h)
    @Test fun original25RowsKeepTheirSlotsAndIndependentlyCheckedAreasOrRequiredBoundsQuestions() {
        val root=requireNotNull(generateSequence(java.io.File(requireNotNull(System.getProperty("user.dir")))){it.parentFile}.firstOrNull{java.io.File(it,"docs/qa/ruhi-500/replay.tsv").exists()})
        fun decode(v:String)=String(java.util.Base64.getDecoder().decode(v),Charsets.UTF_8)
        val rows=java.io.File(root,"docs/qa/ruhi-500/replay.tsv").readLines().map{it.split('\t')}.filter{it[2]=="INTEGRATE"};assertEquals(25,rows.size);val errors=mutableListOf<String>()
        rows.forEach{row->val s=RuhiSeed500Fixture.state(row[3],row[0].toInt());val c=RuhiSeed500Fixture.context(row[0].toInt());val r=RuhiDialogueManager().execute(s,decode(row[1]),WorkspaceHistoryByModule(),c);val check=RuhiIntegralOracle.check(RuhiSeedJson.objectValue(decode(row[6])),s,r,c);if(!check.verified)errors+="${row[0]} ${check.diagnostics}"};assertTrue(errors.joinToString("\n"),errors.isEmpty())
    }
    @Test fun newCoefficientsReversedBoundsAndUndefinedDomainsUseNativeMathematics() {
        for(a in listOf(.375,1.75,4.2)){val f=RuhiIntegralMath.function("y=$a*x^2+3*x-2");val result=RuhiIntegralMath.integrate(f,-1.5,2.25);val p:(Double)->Double={x->a*x*x*x/3+1.5*x*x-2*x};assertEquals(p(2.25)-p(-1.5),result.signed,1e-7);val reverse=RuhiIntegralMath.integrate(f,2.25,-1.5);assertEquals(-result.signed,reverse.signed,1e-7);assertEquals(result.absolute,reverse.absolute,1e-7)}
        assertTrue(runCatching{RuhiIntegralMath.integrate(RuhiIntegralMath.function("y=1/x"),-1.0,1.0)}.isFailure)
        assertTrue(runCatching{RuhiIntegralMath.integrate(RuhiIntegralMath.function("y=1/(x-0.12345)"),-1.0,1.0)}.isFailure)
        assertTrue(runCatching{RuhiIntegralMath.integrate(RuhiIntegralMath.function("y=sin(512*pi*x)"),0.0,1.0)}.isFailure)
        assertTrue(runCatching{RuhiIntegralMath.integrate(RuhiIntegralMath.function("y=sqrt(x)"),-1.0,1.0)}.isFailure)
        assertTrue(runCatching{RuhiIntegralMath.integrate(RuhiIntegralMath.function("y=x {x>0}"),-1.0,1.0)}.isFailure)
        val sine=RuhiIntegralMath.integrate(RuhiIntegralMath.function("y=sin(x)"),0.0,2*PI);assertEquals(0.0,sine.signed,1e-7);assertEquals(4.0,sine.absolute,1e-7)
    }
    @Test fun boundsQuestionRestoresResumesCustomAndViewportChoicesWithoutInventingAnInterval() {
        val s=empty().copy(functions=listOf(FunctionDefinition("id,/907","Velocity_907","y=x^2","cyan")));val h=WorkspaceHistoryByModule();val r=query(s,"Integrate the selected function",h)
        assertEquals(r.message,GraphCommandStatus.CLARIFICATION,r.status);assertEquals(0,h.forModule(s.module).protocol.size);assertEquals(listOf("Visible x-range","0 to 1","Custom bounds"),r.dialogue!!.suggestions.map{it.label})
        val loaded=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(r.state),recover=false).state!!;assertEquals(r.dialogue,RuhiDialogueManager().restoredView(loaded))
        val out=query(loaded,"Use integral bounds 0 to 2",h);assertEquals(out.message,GraphCommandStatus.APPLIED,out.status);assertEquals(8.0/3,RuhiIntegralLanguage.drawing(out.state)!!.values!!.signed,1e-7);assertEquals(1,h.forModule(s.module).protocol.size)
        val custom=query(r.state,r.dialogue!!.suggestions.last().answer);assertEquals(GraphCommandStatus.CLARIFICATION,custom.status);assertTrue(custom.dialogue!!.pending)
        val stale=query(r.state.copy(functions=listOf(s.functions.single().copy(expression="y=3*x^2"))),"Use integral bounds 0 to 2");assertEquals(GraphCommandStatus.CLARIFICATION,stale.status);assertNull(RuhiDialogueMemory.read(stale.state).pending)
        assertNull(RuhiDialogueMemory.read(query(r.state,"Never mind").state).pending)
    }
    @Test fun selectedRenamedFunctionsFollowEditsAndAnnotationsPersistUndoAndRedo() {
        val s=empty().copy(functions=listOf(FunctionDefinition("id,/907","Velocity_907","y=x^2","cyan")));val h=WorkspaceHistoryByModule();val r=query(s,"Shade area under the currently selected curve between 1 and 4",h);assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertEquals(21.0,RuhiIntegralLanguage.drawing(r.state)!!.values!!.signed,1e-7)
        val changed=r.state.copy(functions=listOf(s.functions.single().copy(expression="y=3*x^2")));assertEquals(63.0,RuhiIntegralLanguage.drawing(changed)!!.values!!.signed,1e-7);assertEquals(s.functions,r.state.functions)
        val loaded=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(changed),recover=false).state!!;assertEquals(RuhiIntegralLanguage.drawing(changed),RuhiIntegralLanguage.drawing(loaded))
        val undo=query(r.state,"Undo",h);assertNull(RuhiIntegralLanguage.drawing(undo.state));assertNotNull(RuhiIntegralLanguage.drawing(query(undo.state,"Redo",h).state))
    }
    @Test fun ambiguousCurveChipsBindStableIdsAndAnUnrelatedPlotCanInterruptTheQuestion() {
        val s=empty().copy(functions=listOf(FunctionDefinition("id,/907","Velocity_907","y=x^2","cyan"),FunctionDefinition("id,/811","Velocity_811","y=x","red")))
        val r=query(s,"Integrate the selected function");assertEquals(GraphCommandStatus.CLARIFICATION,r.status);assertEquals(2,r.dialogue!!.suggestions.size)
        val chosen=query(r.state,r.dialogue!!.suggestions.first().answer);assertEquals(GraphCommandStatus.CLARIFICATION,chosen.status);assertEquals("integral_bounds",RuhiDialogueMemory.read(chosen.state).pending!!.kind)
        val done=query(chosen.state,"0 to 3");assertEquals(done.message,GraphCommandStatus.APPLIED,done.status);assertEquals(9.0,RuhiIntegralLanguage.drawing(done.state)!!.values!!.signed,1e-7)
        val plot=query(r.state,"Plot y = x cubed");assertEquals(plot.message,GraphCommandStatus.APPLIED,plot.status);assertNull(RuhiDialogueMemory.read(plot.state).pending)
    }
}
