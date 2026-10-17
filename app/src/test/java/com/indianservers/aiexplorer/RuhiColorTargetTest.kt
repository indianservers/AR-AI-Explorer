package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.workspace.*
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import org.junit.Assert.*
import org.junit.Test

class RuhiColorTargetTest {
    private class Session(var state:WorkspaceState) {
        val manager=RuhiDialogueManager();val history=WorkspaceHistoryByModule()
        fun say(text:String)=manager.execute(state,text,history).also{state=it.state}
        fun restore(){state=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(state)).state!!}
    }
    private fun circles(colors:List<String>)=WorkspaceState(module=MathModule.Geometry2D,
        points=listOf(Vec2(0.0,0.0),Vec2(1.0,0.0),Vec2(5.0,0.0),Vec2(7.0,0.0)),
        shapes=colors.mapIndexed{i,color->Shape2D("opaque,/$i",Shape2DType.Circle,listOf(i*2,i*2+1),name="Orbit_$i",styleKey=color)})
    @Test fun originalCorrectionBindsNativeColorAndOpaqueIdAfterRestoreWithOneUndo() {
        for(colors in listOf(listOf("#0000FF","#FF0000"),listOf("red","blue"))) {
            val initial=circles(colors);val s=Session(initial)
            assertEquals(GraphCommandStatus.CLARIFICATION,s.say("Label the circle as Target").status)
            s.restore();val r=s.say("Actually I meant the blue one")
            assertEquals(r.message,GraphCommandStatus.APPLIED,r.status)
            val blue=colors.indexOfFirst{it.lowercase() in setOf("blue","#0000ff")}
            assertEquals("Target",s.state.shapes[blue].name)
            assertEquals(initial.shapes[1-blue],s.state.shapes[1-blue])
            assertEquals(initial.points,s.state.points)
            assertEquals("Actually I meant the blue one",RuhiDialogueMemory.read(s.state).turns.last().user)
            assertNull(RuhiDialogueMemory.read(s.state).pending)
            s.say("Undo");assertEquals(initial.shapes,s.state.shapes)
        }
    }
    @Test fun solidCorrectionUsesAppearanceAndValidatedTransformNotArrayDefault() {
        val initial=WorkspaceState(module=MathModule.Geometry3D,solids=listOf(Solid(SolidType.Cube,2.0),Solid(SolidType.Cube,3.0)),labSessionValues=mapOf("ruhi.studio.appearance.0" to "red","ruhi.studio.appearance.1" to "#0000FF"))
        val s=Session(initial)
        assertEquals(GraphCommandStatus.CLARIFICATION,s.say("Move the cube to (3, 2, 1)").status)
        s.restore();val r=s.say("Actually, I meant the blue one.")
        assertEquals(r.message,GraphCommandStatus.APPLIED,r.status)
        assertEquals(initial.solids[0],s.state.solids[0]);assertEquals(Vec3(3.0,2.0,1.0),s.state.solids[1].position)
        s.say("Undo");assertEquals(initial.solids,s.state.solids)
    }
    @Test fun multipleBlueObjectsAskAndDisplayedAnswerStillCompletesOriginalAction() {
        val initial=circles(listOf("blue","#0000FF"));val s=Session(initial)
        s.say("Label the circle as Chosen");s.restore()
        val q=s.say("Actually I meant the blue one")
        assertEquals(GraphCommandStatus.CLARIFICATION,q.status);assertTrue(q.message.contains("More than one"))
        assertEquals(initial.shapes,s.state.shapes);assertEquals(2,q.dialogue!!.suggestions.size)
        s.restore();val r=s.say("Choose the second suggested answer")
        assertEquals(r.message,GraphCommandStatus.APPLIED,r.status)
        assertEquals(initial.shapes[0],s.state.shapes[0]);assertEquals("Chosen",s.state.shapes[1].name)
    }
    @Test fun absentBlueAndStaleSceneDoNotInventAnEditOrLoseOrdinaryPendingChoice() {
        val initial=circles(listOf("red","green"));val s=Session(initial)
        s.say("Label the circle as Chosen")
        val q=s.say("Actually I meant the blue one")
        assertEquals(GraphCommandStatus.CLARIFICATION,q.status);assertTrue(q.message.contains("No blue"))
        assertNotNull(RuhiDialogueMemory.read(s.state).pending);assertEquals(initial.shapes,s.state.shapes)
        s.state=s.state.copy(points=s.state.points.map{it+Vec2(2.0,0.0)})
        val moved=s.state.points;val stale=s.say("Actually I meant the blue one")
        assertEquals(GraphCommandStatus.CLARIFICATION,stale.status);assertTrue(stale.message.contains("scene changed"))
        assertNull(RuhiDialogueMemory.read(s.state).pending);assertEquals(moved,s.state.points);assertEquals(initial.shapes,s.state.shapes)
        val missing=s.say("Actually I meant the blue one")
        assertEquals(GraphCommandStatus.CLARIFICATION,missing.status);assertTrue(missing.message.contains("Which action"))
    }
}
