package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.workspace.*
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import org.junit.Assert.*
import org.junit.Test

class RuhiDerivativePendingSlotsTest {
    private class Session(var state:WorkspaceState) {
        val h=WorkspaceHistoryByModule();val manager=RuhiDialogueManager()
        fun say(text:String,c:RuhiSceneContext=RuhiSceneContext())=manager.execute(state,text,h,c).also{state=it.state}
        fun restore(){state=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(state)).state!!}
    }
    private fun scene()=WorkspaceState(module=MathModule.Graph2D,functions=listOf(FunctionDefinition("curve,/907","Square907","y=x^2","cyan"),FunctionDefinition("curve,/811","Cube811","y=x^3","red")))
    @Test fun suppliedXSurvivesNextCurveQuestionAndSavedSession() {
        val s=Session(scene());val q=s.say("Show the tangent slope at the selected point")
        assertEquals(GraphCommandStatus.CLARIFICATION,q.status)
        s.restore();val curve=s.say(q.dialogue!!.suggestions.last().answer)
        assertEquals(GraphCommandStatus.CLARIFICATION,curve.status);assertEquals("derivative_graph",RuhiDialogueMemory.read(s.state).pending!!.kind)
        s.restore();val out=s.say(curve.dialogue!!.suggestions.last().answer)
        assertEquals(out.message,GraphCommandStatus.APPLIED,out.status);assertTrue(out.message.contains("slope = 3.0"))
        assertEquals(Vec2(1.0,1.0),RuhiDerivativeLanguage.drawing(s.state)!!.points.single())
        assertEquals(1,s.h.forModule(s.state.module).protocol.size)
    }
    @Test fun selectedPointSurvivesCurveQuestionWithoutReselectingAfterRestore() {
        val s=Session(scene().copy(points=listOf(Vec2(2.0,4.0))))
        val q=s.say("What is the instantaneous gradient here?",RuhiSceneContext(selectedPointIndex=0))
        assertEquals(GraphCommandStatus.CLARIFICATION,q.status);assertEquals("derivative_graph",RuhiDialogueMemory.read(s.state).pending!!.kind)
        s.restore();val out=s.say(q.dialogue!!.suggestions.first().answer)
        assertEquals(out.message,GraphCommandStatus.APPLIED,out.status);assertTrue(out.message.contains("slope = 4.0"))
        val key=RuhiDerivativeLanguage.decode(s.state.labSessionValues.getValue(RuhiDerivativeLanguage.MARK)).pointKey
        assertTrue(key.isNotEmpty());assertEquals("0",s.state.labSessionValues[key])
    }
    @Test fun plainNumericAndNamedRepliesResumeBothOriginalRequestsWithoutLosingX() {
        for(command in listOf("Show the tangent slope at the selected point","What is the instantaneous gradient here?"))for(answer in listOf("2.5","x = -2",".375")) {
            val x=answer.substringAfter('=').trim().toDouble();val s=Session(scene());s.say(command);s.restore()
            val q=s.say(answer);assertEquals(q.message,GraphCommandStatus.CLARIFICATION,q.status);assertEquals("derivative_graph",RuhiDialogueMemory.read(s.state).pending!!.kind)
            s.restore();val out=s.say("Cube811");assertEquals(out.message,GraphCommandStatus.APPLIED,out.status)
            val slope=Regex("slope = ([-+0-9.eE]+)").find(out.message)!!.groupValues[1].toDouble();assertEquals(3*x*x,slope,1e-8)
            assertEquals(scene().functions,s.state.functions);assertTrue(s.state.points.isEmpty());assertEquals(1,s.h.forModule(s.state.module).protocol.size)
        }
    }
    @Test fun invalidXRetainsQuestionAndValidCurveBindingWithoutAddingHistory() {
        val initial=scene().let{it.copy(labSessionValues=mapOf(GraphAssistant.selectionKey(it.module) to it.functions.first().id))};val s=Session(initial)
        s.say("Show the tangent slope at the selected point")
        for(answer in listOf("Use derivative x NaN","Use derivative x Infinity","Use derivative x 10001")) {
            val out=s.say(answer);assertEquals(out.message,GraphCommandStatus.INVALID,out.status);assertEquals("derivative_x",RuhiDialogueMemory.read(s.state).pending!!.kind);assertTrue(s.h.forModule(s.state.module).protocol.isEmpty());s.restore()
        }
        val out=s.say("x = 3");assertEquals(out.message,GraphCommandStatus.APPLIED,out.status);assertTrue(out.message.contains("slope = 6.0"));assertEquals(initial.functions,s.state.functions)
    }
    @Test fun selectionChangesDoNotReplaceTheCurveAlreadyBoundToPendingQuestion() {
        val initial=scene().let{it.copy(labSessionValues=mapOf(GraphAssistant.selectionKey(it.module) to it.functions.first().id))};val s=Session(initial)
        s.say("What is the instantaneous gradient here?");s.restore()
        s.state=s.state.copy(labSessionValues=s.state.labSessionValues+(GraphAssistant.selectionKey(s.state.module) to initial.functions.last().id))
        val out=s.say("2");assertEquals(out.message,GraphCommandStatus.APPLIED,out.status);assertTrue(out.message.contains("slope = 4.0"))
        assertEquals(initial.functions.first().id,RuhiDerivativeLanguage.decode(s.state.labSessionValues.getValue(RuhiDerivativeLanguage.MARK)).source)
    }
    @Test fun sceneEditsInvalidateSavedSlotsAndOldChoicesWithoutAnEdit() {
        val s=Session(scene());val first=s.say("Show the tangent slope at the selected point");val q=s.say(first.dialogue!!.suggestions.last().answer)
        s.restore();s.state=s.state.copy(functions=s.state.functions.reversed());val current=s.state
        val out=s.say(q.dialogue!!.suggestions.first().answer)
        assertEquals(GraphCommandStatus.CLARIFICATION,out.status);assertTrue(out.message.contains("scene changed",ignoreCase=true));assertNull(RuhiDialogueMemory.read(s.state).pending);assertEquals(current.functions,s.state.functions);assertTrue(s.h.forModule(s.state.module).protocol.isEmpty())
    }
    @Test fun cancellationAndNewDerivativeRequestDoNotReuseAbandonedX() {
        val s=Session(scene());val first=s.say("What is the instantaneous gradient here?");s.say(first.dialogue!!.suggestions.last().answer)
        s.say("Never mind");assertNull(RuhiDialogueMemory.read(s.state).pending)
        val q=s.say("Show the tangent slope at the selected point");assertEquals("derivative_x",RuhiDialogueMemory.read(s.state).pending!!.kind);assertTrue(RuhiDialogueMemory.read(s.state).pending!!.slots["x"]==null)
        assertEquals(GraphCommandStatus.CLARIFICATION,q.status);assertTrue(s.h.forModule(s.state.module).protocol.isEmpty())
    }
    @Test fun versionSixPendingRecordsStillReadWithNoSavedSlots() {
        val initial=scene();val bytes=java.io.ByteArrayOutputStream();java.io.DataOutputStream(bytes).use{o->
            o.writeInt(6);listOf(initial.id,initial.module.name,"","","","","").forEach(o::writeUTF)
            o.writeUTF("");o.writeInt(0);o.writeInt(0);o.writeInt(0);o.writeUTF("");o.writeInt(0)
            o.writeBoolean(true);listOf("derivative_x","Show the tangent slope at the selected point",initial.module.name,"old-scene").forEach(o::writeUTF);o.writeInt(0);o.writeInt(0)
        }
        val saved=RuhiDialogueMemory.read(initial.copy(labSessionValues=mapOf(RuhiDialogueMemory.KEY to java.util.Base64.getEncoder().encodeToString(bytes.toByteArray()))))
        assertEquals("derivative_x",saved.pending!!.kind);assertTrue(saved.pending!!.slots.isEmpty())
        val reloaded=RuhiDialogueMemory.read(initial.copy(labSessionValues=mapOf(RuhiDialogueMemory.KEY to saved.encode())));assertEquals(saved.pending,reloaded.pending)
    }
    @Test fun choosingAnOffCurveFunctionRetainsThatChoiceDuringTheNextXQuestion() {
        val s=Session(scene().copy(points=listOf(Vec2(2.0,4.0))))
        val first=s.say("Show the tangent slope at the selected point",RuhiSceneContext(selectedPointIndex=0))
        s.restore();val q=s.say(first.dialogue!!.suggestions.last().answer)
        assertEquals(GraphCommandStatus.CLARIFICATION,q.status);assertTrue(q.message.contains("not on"));assertEquals("derivative_x",RuhiDialogueMemory.read(s.state).pending!!.kind);assertTrue(s.h.forModule(s.state.module).protocol.isEmpty())
        s.restore();val out=s.say("3")
        assertEquals(out.message,GraphCommandStatus.APPLIED,out.status);assertTrue(out.message.contains("slope = 27.0"));assertEquals(Vec2(3.0,27.0),RuhiDerivativeLanguage.drawing(s.state)!!.points.single());assertEquals(listOf(Vec2(2.0,4.0)),s.state.points)
    }
}
