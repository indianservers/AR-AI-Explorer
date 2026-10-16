package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.workspace.*
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.sin
import kotlin.math.cos

class RuhiDialogueTest {
    private class Session(module:MathModule) {
        val manager=RuhiDialogueManager();val histories=WorkspaceHistoryByModule()
        var state=WorkspaceState(module=module,surfaceLayers=emptyList())
        var context=RuhiSceneContext()
        fun say(text:String):RuhiExecutionResult=manager.execute(state,text,histories,context).also{state=it.state;context=it.context}
    }
    @Test fun incompleteGraphChipAnswerThenModifyWithoutDuplicate() {
        val s=Session(MathModule.Graph2D)
        val question=s.say("Draw a graph.")
        assertEquals(GraphCommandStatus.CLARIFICATION,question.status);assertTrue(question.dialogue!!.pending)
        assertEquals(3,question.dialogue!!.suggestions.size);assertTrue(s.state.functions.isEmpty())
        assertFalse(s.histories.forModule(MathModule.Graph2D).canUndo)
        assertEquals(GraphCommandStatus.APPLIED,s.say(question.dialogue!!.suggestions[1].answer).status)
        val id=s.state.functions.single().id
        assertEquals(GraphCommandStatus.APPLIED,s.say("Make it red.").status)
        assertEquals(id,s.state.functions.single().id);assertEquals("#FF0000",s.state.functions.single().colorKey)
        assertNull(RuhiDialogueMemory.read(s.state).pending)
    }
    @Test fun defaultsAndCylinderParametersSurviveCorrectionsAndUndo() {
        val s=Session(MathModule.Geometry3D)
        assertEquals(GraphCommandStatus.APPLIED,s.say("Create a cylinder.").status)
        assertEquals(2.0,s.state.solids.single().height,0.0)
        assertEquals(GraphCommandStatus.APPLIED,s.say("Make the height 10.").status)
        assertEquals(GraphCommandStatus.APPLIED,s.say("And the radius 3.").status)
        assertEquals(10.0,s.state.solids.single().height,0.0);assertEquals(3.0,s.state.solids.single().radius,0.0)
        val volume=s.say("What's its volume?");assertEquals(GraphCommandStatus.APPLIED,volume.status)
        assertEquals(90*Math.PI,Geometry3D.measure(s.state.solids.single()).volume,1e-8)
        assertEquals(GraphCommandStatus.APPLIED,s.say("Actually, height should be 12.").status)
        s.say("Undo that.");assertEquals(10.0,s.state.solids.single().height,0.0)
        s.say("Redo.");assertEquals(12.0,s.state.solids.single().height,0.0)
        assertEquals(1,s.state.solids.size)
    }
    @Test fun comparisonSurvivesInterruptionAndRoundTrip() {
        val s=Session(MathModule.Graph2D)
        s.say("Compare two functions.")
        val explanation=s.say("First explain what a quadratic function is.")
        assertEquals(RuhiDialogueAction.EXPLAIN,explanation.dialogue!!.action);assertTrue(s.state.functions.isEmpty())
        s.state=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(s.state)).state!!
        assertEquals("compare",RuhiDialogueMemory.read(s.state).pending!!.kind)
        assertNotNull(RuhiDialogueManager().restoredView(s.state))
        assertEquals(GraphCommandStatus.APPLIED,s.say("sin and cos.").status)
        assertEquals(2,s.state.functions.size)
        val analysis=s.say("Show where they intersect.")
        assertEquals(analysis.message,GraphCommandStatus.APPLIED,analysis.status)
        val pins=RuhiDialogueMath.markers(s.state);assertTrue(pins.isNotEmpty())
        pins.forEach{assertEquals(sin(it.x),cos(it.x),1e-5)}
    }
    @Test fun ambiguityUsesActualObjectsAndBothIsAtomic() {
        val s=Session(MathModule.Graph3D)
        s.say("Create a cube.");s.say("Create a sphere.")
        s.state=s.state.copy(labSessionValues=s.state.labSessionValues-GraphAssistant.selectionKey(MathModule.Graph3D)-RuhiDialogueMemory.KEY)
        val question=s.say("Make it red.")
        assertEquals(GraphCommandStatus.CLARIFICATION,question.status)
        assertTrue(question.dialogue!!.suggestions.any{it.label=="Both"})
        assertEquals(GraphCommandStatus.APPLIED,s.say("Both.").status)
        assertTrue(s.state.surfaceLayers.all{it.colorHex=="#FF0000"})
        s.say("Undo.");assertTrue(s.state.surfaceLayers.none{it.colorHex=="#FF0000"})
    }
    @Test fun triangleDefaultsTypesDimensionsAnglesAndRotation() {
        val s=Session(MathModule.Geometry2D)
        assertEquals(GraphCommandStatus.APPLIED,s.say("Draw a triangle.").status)
        val id=s.state.shapes.single().id
        s.say("Make it equilateral.");s.say("Side length 6.")
        val shape=s.state.shapes.single();assertEquals(id,shape.id)
        val p=shape.pointIndices.map{s.state.points[it]}
        p.indices.forEach{i->assertEquals(6.0,p[i].distanceTo(p[(i+1)%3]),1e-8)}
        assertTrue(s.say("Show the angles.").message.contains("60.00°"))
        assertEquals(GraphCommandStatus.APPLIED,s.say("Rotate it.").status)
        assertEquals(1,s.state.shapes.size)
    }
    @Test fun cancelAndInvalidValuesNeverMutateGeometry() {
        val s=Session(MathModule.Geometry3D);s.say("Create a cylinder.")
        val before=s.state.solids
        assertEquals(GraphCommandStatus.INVALID,s.say("Make the height -4.").status);assertEquals(before,s.state.solids)
        val g=Session(MathModule.Graph2D);g.say("Draw a graph.");g.say("Cancel.")
        assertNull(RuhiDialogueMemory.read(g.state).pending);assertTrue(g.state.functions.isEmpty())
        assertFalse(g.manager.accepts(g.state,"Teach me quadratic equations"))
    }
    @Test fun invalidAnswerRecoversAndEssentialDistanceIsFilled() {
        val s=Session(MathModule.Graph2D);s.say("Draw a graph.")
        assertEquals(GraphCommandStatus.CLARIFICATION,s.say("banana").status);assertNotNull(RuhiDialogueMemory.read(s.state).pending)
        assertEquals(GraphCommandStatus.APPLIED,s.say("x squared minus four").status)
        assertEquals(GraphCommandStatus.CLARIFICATION,s.say("Move it.").status)
        assertEquals(GraphCommandStatus.APPLIED,s.say("3 units right").status);assertEquals(1,s.state.functions.size)
        assertEquals(5.0,ExpressionEngine().compile(stripEquation(s.state.functions.single().expression)).eval(mapOf("x" to 0.0)),1e-8)
    }
    @Test fun copyAndNamedReferenceDimensionsPreserveOriginal() {
        val s=Session(MathModule.Geometry3D);s.say("Create a cone with height 9.")
        assertEquals(GraphCommandStatus.APPLIED,s.say("Create a cylinder with the same height as that cone.").status)
        assertEquals(9.0,s.state.solids.last().height,0.0)
        assertEquals(GraphCommandStatus.APPLIED,s.say("Make it transparent.").status)
        assertEquals(GraphCommandStatus.APPLIED,s.say("Make another one.").status)
        assertEquals(3,s.state.solids.size);assertEquals(2,s.context.selectedSolidIndex)
        assertEquals("glass",s.state.labSessionValues["ruhi.studio.appearance.2"])
        assertEquals(GraphCommandStatus.APPLIED,s.say("Make the height 12.").status)
        assertEquals(9.0,s.state.solids[1].height,0.0);assertEquals(12.0,s.state.solids[2].height,0.0)
    }
    @Test fun pendingTargetsCannotRetargetAfterNativeSceneChange() {
        val s=Session(MathModule.Graph3D);s.say("Create a cube.");s.say("Create a sphere.")
        s.state=s.state.copy(labSessionValues=s.state.labSessionValues-GraphAssistant.SELECTION-RuhiDialogueMemory.KEY)
        s.say("Make it red.");val before=s.state.surfaceLayers
        s.state=s.state.copy(surfaceLayers=before.dropLast(1))
        assertEquals(GraphCommandStatus.CLARIFICATION,s.say("Both.").status)
        assertTrue(s.state.surfaceLayers.none{it.colorHex=="#FF0000"})
    }
    @Test fun functionFamilyFillsSlotBeforeSpokenEquationAndRoots() {
        val s=Session(MathModule.Graph2D);s.say("Draw a graph.")
        assertEquals(GraphCommandStatus.CLARIFICATION,s.say("Quadratic.").status)
        assertTrue(RuhiDialogueMemory.read(s.state).pending!!.source.contains("family=quadratic"))
        assertTrue(s.state.functions.isEmpty())
        assertEquals(GraphCommandStatus.APPLIED,s.say("x squared minus four.").status)
        assertEquals(GraphCommandStatus.APPLIED,s.say("Show its roots.").status)
        val roots=RuhiDialogueMath.markers(s.state).map{it.x}.sorted();assertEquals(2,roots.size);assertEquals(-2.0,roots[0],1e-6);assertEquals(2.0,roots[1],1e-6)
    }
    @Test fun missingExplicitReferenceCannotFallBackToLastModifiedCurve() {
        val empty=Session(MathModule.Graph2D)
        assertEquals(GraphCommandStatus.CLARIFICATION,empty.say("Make the red curve red.").status)
        assertNull(RuhiDialogueMemory.read(empty.state).pending)
        assertEquals(GraphCommandStatus.APPLIED,empty.say("Plot y = x.").status)
        val s=Session(MathModule.Graph2D);s.say("Plot y = x.");s.say("Plot y = x^2.");s.say("Make it blue.")
        val before=s.state.functions
        assertEquals(GraphCommandStatus.CLARIFICATION,s.say("Make the red curve red.").status)
        assertEquals(before,s.state.functions);assertEquals("target",RuhiDialogueMemory.read(s.state).pending!!.kind)
    }
}
