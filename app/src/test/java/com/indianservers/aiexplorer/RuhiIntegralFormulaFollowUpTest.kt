package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.workspace.*
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import org.junit.Assert.*
import org.junit.Test

class RuhiIntegralFormulaFollowUpTest {
    private class Session(var state:WorkspaceState=WorkspaceState(module=MathModule.Graph2D,functions=emptyList(),surfaceLayers=emptyList())) {
        val h=WorkspaceHistoryByModule();val m=RuhiDialogueManager()
        fun say(text:String)=m.execute(state,text,h).also{state=it.state}
        fun restore(){state=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(state)).state!!}
    }
    private fun value(s:Session)=RuhiIntegralLanguage.drawing(s.state)!!.values!!.signed
    @Test fun originalRow364RetainsFormulaAcrossSavedRequiredBoundsAndCommitsAtomically() {
        val s=Session();val q=s.say("Integrate the selected function")
        assertEquals(GraphCommandStatus.CLARIFICATION,q.status);assertEquals("integral_formula",RuhiDialogueMemory.read(s.state).pending!!.kind);assertEquals(3,q.dialogue!!.suggestions.size)
        s.restore();val bounds=s.say("y=x^2");assertEquals(bounds.message,GraphCommandStatus.CLARIFICATION,bounds.status);assertEquals("integral_bounds",RuhiDialogueMemory.read(s.state).pending!!.kind);assertTrue(s.state.functions.isEmpty());assertTrue(s.h.forModule(s.state.module).protocol.isEmpty())
        s.restore();val out=s.say("0 to 2");assertEquals(out.message,GraphCommandStatus.APPLIED,out.status);assertEquals("INTEGRATE",out.semanticFrame!!.intent);assertEquals(8.0/3,value(s),1e-7);assertEquals(listOf(0.0,2.0),out.semanticFrame!!.parameters["bounds"]);assertEquals(1,s.state.functions.size);assertEquals(1,s.h.forModule(s.state.module).protocol.size)
        assertEquals(s.state.functions.single().id,RuhiIntegralLanguage.decode(s.state.labSessionValues.getValue(RuhiIntegralLanguage.MARK)).refs.single().value)
        s.say("Undo");assertTrue(s.state.functions.isEmpty());assertNull(RuhiIntegralLanguage.drawing(s.state));s.say("Redo");assertEquals(8.0/3,value(s),1e-7)
    }
    @Test fun originalRow372KeepsExplicitBoundsAndNativeShadingAcrossSavedFormulaReply() {
        for(a in listOf(.375,1.75,4.2)) {
            val s=Session();s.say("Shade area under the currently selected curve between 1 and 4");s.restore();val r=s.say("y=$a*x^2")
            assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertEquals(21*a,value(s),1e-7);val d=RuhiIntegralLanguage.drawing(s.state)!!;assertEquals(1.0 to 4.0,d.bounds);assertEquals(256,d.bands.size);assertTrue(d.bands.all{it.sign==1});assertTrue(r.semanticFrame!!.parameters["shade"]==true);assertNull(RuhiDialogueMemory.read(s.state).pending)
        }
    }
    @Test fun restoredFormulaExampleIsExecutableAndBoundsExamplesUseTheProposedDomain() {
        val s=Session();val q=s.say("Integrate the selected function");s.restore();val b=s.say(q.dialogue!!.suggestions.first().answer);assertEquals(GraphCommandStatus.CLARIFICATION,b.status);s.restore();assertEquals(GraphCommandStatus.APPLIED,s.say("2 to 0").status);assertEquals(-8.0/3,value(s),1e-7)
        val log=Session();log.say("Integrate the selected function");val bounds=log.say("y=ln(x)");assertTrue(log.state.functions.isEmpty());assertTrue(bounds.dialogue!!.suggestions.any{it.label=="1 to 2"});assertFalse(bounds.dialogue!!.suggestions.any{it.label=="0 to 1"});log.restore();assertEquals(GraphCommandStatus.APPLIED,log.say("1 to 2").status);assertEquals(2*kotlin.math.ln(2.0)-1,value(log),1e-7)
    }
    @Test fun invalidFormulasPreserveQuestionAndDoNotCreateSceneOrHistoryEntries() {
        val s=Session();s.say("Integrate the selected function")
        for(formula in listOf("y=banana(x)","x^2+y^2=9","y=a*x","y=1/0")) {
            val r=s.say("Use integral formula $formula");assertEquals(r.message,GraphCommandStatus.INVALID,r.status);assertTrue(s.state.functions.isEmpty());assertTrue(s.h.forModule(s.state.module).protocol.isEmpty());assertEquals("integral_formula",RuhiDialogueMemory.read(s.state).pending!!.kind);s.restore()
        }
        assertEquals(GraphCommandStatus.CLARIFICATION,s.say("y=x^2").status);assertEquals(GraphCommandStatus.APPLIED,s.say("0 to 2").status)
    }
    @Test fun invalidBoundsAndUndefinedIntervalsRetainValidatedProposalUntilCorrected() {
        val s=Session();s.say("Integrate the selected function");s.say("y=1/x");s.restore()
        for(bounds in listOf("NaN to 2","0 to 2","-1 to 1","10001 to 2")) {
            val r=s.say(bounds);assertEquals(r.message,GraphCommandStatus.INVALID,r.status);assertEquals("integral_bounds",RuhiDialogueMemory.read(s.state).pending!!.kind);assertTrue(s.state.functions.isEmpty());assertTrue(s.h.forModule(s.state.module).protocol.isEmpty());s.restore()
        }
        assertEquals(GraphCommandStatus.APPLIED,s.say("1 to 2").status);assertEquals(kotlin.math.ln(2.0),value(s),1e-7)
        val fixed=Session();fixed.say("Shade area under the currently selected curve between -1 and 1");val bad=fixed.say("y=1/x");assertEquals(GraphCommandStatus.INVALID,bad.status);assertTrue(fixed.state.functions.isEmpty());fixed.restore();assertEquals(GraphCommandStatus.APPLIED,fixed.say("1 to 2").status);assertEquals(kotlin.math.ln(2.0),value(fixed),1e-7)
    }
    @Test fun cancellationNewRequestsAndSceneChangesCannotReuseStagedFormula() {
        val s=Session();s.say("Integrate the selected function");s.say("y=x^2");s.restore();s.say("Cancel that");assertNull(RuhiDialogueMemory.read(s.state).pending);assertTrue(s.state.functions.isEmpty())
        s.say("Integrate the selected function");s.state=s.state.copy(functions=listOf(FunctionDefinition("external","External","y=x^3","red")));val stale=s.say("Use integral formula y=x^2");assertEquals(GraphCommandStatus.CLARIFICATION,stale.status);assertNull(RuhiDialogueMemory.read(s.state).pending);assertEquals("external",s.state.functions.single().id)
        val fresh=Session();fresh.say("Integrate the selected function");fresh.say("y=x^2");val direct=fresh.say("Integrate x from 0 to 2");assertEquals(GraphCommandStatus.APPLIED,direct.status);assertTrue(fresh.state.functions.isEmpty());assertEquals(2.0,value(fresh),1e-7)
    }
    @Test fun savedAnalysisTracksCurrentFunctionEditsAndLeavesExistingHiddenCurvesUntouched() {
        val hidden=FunctionDefinition("old","Function1","y=x^4","red",visible=false);val s=Session(WorkspaceState(module=MathModule.Graph2D,functions=listOf(hidden),surfaceLayers=emptyList()))
        s.say("Shade area under the currently selected curve between 1 and 4");s.say("y=x^2");s.restore();assertEquals(hidden,s.state.functions.first());assertEquals("Function2",s.state.functions.last().name)
        s.state=s.state.copy(functions=s.state.functions.map{if(it.id==hidden.id)it else it.copy(name="Velocity907",expression="y=3*x^2")});assertEquals(63.0,value(s),1e-7);assertEquals(hidden,s.state.functions.first());assertTrue(s.state.points.isEmpty())
    }
}
