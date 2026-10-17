package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.workspace.*
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import org.junit.Assert.*
import org.junit.Test

class RuhiContextualGraphTest {
    private class Session {
        val manager=RuhiDialogueManager();val histories=WorkspaceHistoryByModule()
        var state=WorkspaceState(module=MathModule.Graph2D);var context=RuhiSceneContext()
        fun say(text:String)=manager.execute(state,text,histories,context).also{state=it.state;context=it.context}
        fun restore(){state=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(state)).state!!}
    }
    @Test fun previousGraphUsesCreationHistoryAfterRenamingReorderingAndFormulaEdits() {
        val s=Session()
        assertEquals(GraphCommandStatus.APPLIED,s.say("Plot y = x^2").status)
        val first=s.state.functions.single().id
        assertEquals(GraphCommandStatus.APPLIED,s.say("Plot y = 2*x+3").status)
        val second=s.state.functions.last().id
        assertEquals(listOf(first,second),RuhiDialogueMemory.read(s.state).graphTimeline)
        s.state=s.state.copy(functions=s.state.functions.map{if(it.id==first)it.copy(name="Old_907",expression="y=x^2+7")else it.copy(name="Current_811")}.reversed())
        s.context=RuhiSceneContext(selectedFunctionIds=listOf(second));s.restore()
        val before=s.state.functions
        val result=s.say("Compare this with the previous graph")
        assertEquals(result.message,GraphCommandStatus.APPLIED,result.status)
        assertEquals(listOf(second,first),result.semanticFrame!!.parameters["resolved_graph_ids"])
        assertTrue(result.message.contains("Old_907"));assertTrue(result.message.contains("Current_811"))
        assertTrue(result.message.contains("first minus second at x = 0 is -4.0"))
        assertEquals(before,s.state.functions)
    }
    @Test fun comparisonQueryDoesNotAddUndoOrCreateReplacementGraphs() {
        val s=Session();s.say("Plot y = x^2");s.say("Plot y = x+5")
        val first=s.state.functions.first()
        assertEquals(GraphCommandStatus.APPLIED,s.say("Compare this with the previous graph").status)
        assertEquals(2,s.state.functions.size)
        s.say("Undo")
        assertEquals(listOf(first),s.state.functions)
    }
    @Test fun unknownHistoryAsksForActualPairsAndRestoredOrdinalSelectsDisplayedPair() {
        val s=Session()
        s.state=s.state.copy(functions=listOf(FunctionDefinition("curve,/907","Rise_907","y=x+1","#00FFFF"),FunctionDefinition("curve,/811","Bowl_811","y=x^2+4","#00FFFF"),FunctionDefinition("curve,/662","Wave_662","y=cos(x)","#00FFFF")))
        val before=s.state.functions
        val q=s.say("Compare this with the previous graph")
        assertEquals(GraphCommandStatus.CLARIFICATION,q.status)
        assertEquals(3,q.dialogue!!.suggestions.size)
        assertEquals(before,s.state.functions)
        s.restore()
        assertEquals(q.dialogue!!.suggestions,s.manager.restoredView(s.state)!!.suggestions)
        val result=s.say("Choose the second suggested answer")
        assertEquals(result.message,GraphCommandStatus.APPLIED,result.status)
        assertEquals(listOf("curve,/907","curve,/662"),result.semanticFrame!!.parameters["resolved_graph_ids"])
        assertEquals(before,s.state.functions)
        assertEquals("Choose the second suggested answer",RuhiDialogueMemory.read(s.state).turns.last().user)
    }
    @Test fun changedGraphRejectsBoundAnswerAndCancelClearsPendingComparison() {
        val s=Session()
        s.state=s.state.copy(functions=listOf(FunctionDefinition("opaque,/907","Line_907","y=x+1","#00FFFF"),FunctionDefinition("opaque,/811","Line_811","y=2*x+3","#00FFFF")))
        val q=s.say("Compare this with the previous graph")
        assertEquals(GraphCommandStatus.CLARIFICATION,q.status)
        assertEquals(RuhiDialogueMemory.read(s.state).pending,s.say("Continue where we stopped").let{RuhiDialogueMemory.read(it.state).pending})
        s.state=s.state.copy(functions=s.state.functions.map{if(it.id=="opaque,/907")it.copy(expression="y=x+9")else it})
        val before=s.state.functions
        assertEquals(GraphCommandStatus.CLARIFICATION,s.say(q.dialogue!!.suggestions.first().answer).status)
        assertEquals(before,s.state.functions)
        s.say("Compare this with the previous graph");s.say("Cancel that")
        assertNull(RuhiDialogueMemory.read(s.state).pending)
        assertEquals(before,s.state.functions)
    }
    @Test fun restrictedDomainIsReportedWithoutInventingAValue() {
        val s=Session();s.say("Plot y = x+1");s.say("Draw y = 1/x only for x between 1 and 5")
        val before=s.state.functions
        val r=s.say("Compare this with the previous graph")
        assertEquals(r.message,GraphCommandStatus.APPLIED,r.status)
        assertTrue(r.message.contains("no finite y value is defined"))
        assertFalse(r.message.contains("first minus second"))
        assertEquals(before,s.state.functions)
    }
    @Test fun deleteAndUndoRetainOriginalCreationOrder() {
        val s=Session();s.say("Plot y = x+1");s.say("Plot y = x^2+4")
        val original=s.state.functions;val ids=original.map{it.id}
        s.say("Delete the last object");assertEquals(1,s.state.functions.size)
        assertEquals(ids,RuhiDialogueMemory.read(s.state).graphTimeline)
        s.restore();s.say("Undo")
        assertEquals(original,s.state.functions)
        assertEquals(ids,RuhiDialogueMemory.read(s.state).graphTimeline)
        s.context=RuhiSceneContext(selectedFunctionIds=listOf(ids[1]))
        val result=s.say("Compare this with the previous graph")
        assertEquals(result.message,GraphCommandStatus.APPLIED,result.status)
        assertEquals(listOf(ids[1],ids[0]),result.semanticFrame!!.parameters["resolved_graph_ids"])
    }
}
