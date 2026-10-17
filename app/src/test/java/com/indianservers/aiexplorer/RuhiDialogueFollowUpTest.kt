package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.workspace.*
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import org.junit.Assert.*
import org.junit.Test

class RuhiDialogueFollowUpTest {
    private class Session(module:MathModule) {
        val manager=RuhiDialogueManager();val histories=WorkspaceHistoryByModule()
        var state=WorkspaceState(module=module);var context=RuhiSceneContext()
        fun say(text:String)=manager.execute(state,text,histories,context).also{state=it.state;context=it.context}
        fun restore(){state=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(state)).state!!}
    }
    @Test fun originalEquationCompletionResumesOneGraphAndKeepsStudentWords() {
        val s=Session(MathModule.Graph2D)
        assertEquals(GraphCommandStatus.CLARIFICATION,s.say("Draw a graph").status)
        s.restore()
        val r=s.say("Use x squared for the equation")
        assertEquals(r.message,GraphCommandStatus.APPLIED,r.status)
        assertEquals(1,s.state.functions.size)
        assertTrue(s.state.functions.single().expression.contains("x^2"))
        assertNull(RuhiDialogueMemory.read(s.state).pending)
        assertEquals("Use x squared for the equation",RuhiDialogueMemory.read(s.state).turns.last().user)
        s.say("Undo");assertTrue(s.state.functions.isEmpty())
    }
    @Test fun secondSuggestedAnswerUsesDisplayedCompositeChoiceAfterRestore() {
        val s=Session(MathModule.Graph2D)
        val question=s.say("Compare two functions")
        val displayed=question.dialogue!!.suggestions[1]
        // Compare has different displayed choices from the equation prompt.
        assertEquals("sin and cos",displayed.answer)
        s.restore()
        val options=s.say("What choices do I have?")
        assertEquals(question.dialogue!!.suggestions,options.dialogue!!.suggestions)
        val r=s.say("Choose the second suggested answer")
        assertEquals(r.message,GraphCommandStatus.APPLIED,r.status)
        assertEquals(2,s.state.functions.size)
        assertTrue(s.state.functions.any{it.expression.contains("sin")})
        assertTrue(s.state.functions.any{it.expression.contains("cos")})
    }
    @Test fun canonicalChoiceKeepsStableOpaqueTargetAndDoesNotLeakIntoHistory() {
        val s=Session(MathModule.Geometry2D)
        s.state=s.state.copy(points=listOf(Vec2(0.0,0.0),Vec2(1.0,0.0),Vec2(4.0,0.0),Vec2(6.0,0.0)),shapes=listOf(Shape2D("circle,/907",Shape2DType.Circle,listOf(0,1)),Shape2D("circle,/811",Shape2DType.Circle,listOf(2,3))))
        val q=s.say("Label the circle as C1")
        assertEquals(GraphCommandStatus.CLARIFICATION,q.status)
        val chosen=q.dialogue!!.suggestions[1].answer
        s.restore()
        assertEquals(chosen,s.manager.restoredView(s.state)!!.suggestions[1].answer)
        val r=s.say("Choose the second suggested answer")
        assertEquals(r.message,GraphCommandStatus.APPLIED,r.status)
        assertEquals("Choose the second suggested answer",RuhiDialogueMemory.read(s.state).turns.last().user)
        assertEquals("C1",s.state.shapes.single{it.id=="circle,/811"}.name)
        assertNotEquals("C1",s.state.shapes.single{it.id=="circle,/907"}.name)
    }
    @Test fun directChipActionStoresItsVisibleLabelInsteadOfOpaquePayload() {
        val s=Session(MathModule.Geometry2D)
        s.state=s.state.copy(points=listOf(Vec2(0.0,0.0),Vec2(1.0,0.0),Vec2(4.0,0.0),Vec2(6.0,0.0)),shapes=listOf(Shape2D("opaque,/907",Shape2DType.Circle,listOf(0,1)),Shape2D("opaque,/811",Shape2DType.Circle,listOf(2,3))))
        val chip=s.say("Label the circle as Orbit").dialogue!!.suggestions[1]
        assertTrue(chip.answer.startsWith("Use scene object "))
        assertEquals(GraphCommandStatus.APPLIED,s.say(chip.answer).status)
        assertEquals(chip.label,RuhiDialogueMemory.read(s.state).turns.last().user)
        assertEquals("Orbit",s.state.shapes.single{it.id=="opaque,/811"}.name)
    }
    @Test fun blankCanvasAliasUsesNativeUndoAndPreservesInactiveScene() {
        val s=Session(MathModule.Geometry2D)
        assertEquals(GraphCommandStatus.APPLIED,s.say("Make a triangle").status)
        assertEquals(3,s.state.points.size)
        val triangle=s.state.shapes.single();val points=s.state.points
        assertTrue(kotlin.math.abs((points[1].x-points[0].x)*(points[2].y-points[0].y)-(points[1].y-points[0].y)*(points[2].x-points[0].x))>0.1)
        val solids=listOf(Solid(SolidType.Cube,2.0));s.state=s.state.copy(solids=solids)
        assertEquals(GraphCommandStatus.APPLIED,s.say("Start again with a blank canvas").status)
        assertTrue(s.state.shapes.isEmpty());assertEquals(solids,s.state.solids)
        s.say("Undo");assertEquals(triangle,s.state.shapes.single());assertEquals(points,s.state.points)
    }
    @Test fun actualErrorReasonSurvivesRestoreAndAnInterveningSuccessfulTurn() {
        val s=Session(MathModule.Geometry3D)
        s.say("Create a cube")
        val failure=s.say("Move the cube to (abc, 2, 3)")
        assertTrue(failure.status in setOf(GraphCommandStatus.INVALID,GraphCommandStatus.UNSUPPORTED))
        s.say("Create a sphere");s.restore()
        val before=s.state.solids
        val explained=s.say("Why did that fail?")
        assertEquals(GraphCommandStatus.APPLIED,explained.status)
        assertTrue(explained.message.contains(failure.message))
        assertEquals(before,s.state.solids)
    }
    @Test fun firstAndBothUseActualPendingCandidatesWithOneUndo() {
        for(answer in listOf("Just the first one","Both of them")) {
            val s=Session(MathModule.Graph3D)
            s.state=s.state.copy(surfaceLayers=listOf(SpatialSurfaceLayer("mesh,/811","x+y"),SpatialSurfaceLayer("mesh,/907","x-y")))
            val before=s.state.surfaceLayers
            assertEquals(GraphCommandStatus.CLARIFICATION,s.say("Make it red").status)
            s.restore()
            val r=s.say(answer);assertEquals(r.message,GraphCommandStatus.APPLIED,r.status)
            assertEquals(before.map{it.id},s.state.surfaceLayers.map{it.id})
            assertEquals("#FF0000",s.state.surfaceLayers[0].colorHex)
            if(answer.startsWith("Both"))assertEquals("#FF0000",s.state.surfaceLayers[1].colorHex)
            else assertEquals(before[1],s.state.surfaceLayers[1])
            s.say("Undo");assertEquals(before,s.state.surfaceLayers)
        }
    }
    @Test fun preparationRejectionIsRecordedWithoutChangingSceneOrPendingAction() {
        val s=Session(MathModule.Graph2D)
        s.say("Draw a graph")
        val pending=RuhiDialogueMemory.read(s.state).pending
        val message="The scene or selection changed while I prepared the command. Submit it again."
        s.state=s.manager.recordPreparationFailure(s.state,"Graph x^2",message)
        s.restore()
        assertEquals(pending,RuhiDialogueMemory.read(s.state).pending)
        assertTrue(s.say("Why did that fail?").message.contains(message))
        assertTrue(s.state.functions.isEmpty())
        assertEquals(3,s.say("What choices do I have?").dialogue!!.suggestions.size)
    }
    @Test fun numericCorrectionFillsPendingHeightAndPreservesExplicitRadius() {
        val s=Session(MathModule.Geometry3D)
        val question=s.say("Find the volume of a cone with radius 2")
        assertEquals(question.message,GraphCommandStatus.CLARIFICATION,question.status)
        assertEquals("height",RuhiDialogueMemory.read(s.state).pending!!.targets.first())
        s.restore()
        val result=s.say("No, make it 7 instead")
        assertEquals(result.message,GraphCommandStatus.APPLIED,result.status)
        val query=RuhiSolidMeasureLanguage.decode(s.state.labSessionValues.getValue(RuhiSolidMeasureLanguage.MARK))
        assertEquals(2.0,query.dimensions.getValue("radius"),0.0)
        assertEquals(7.0,query.dimensions.getValue("height"),0.0)
        assertEquals(28*Math.PI/3,RuhiSolidMeasureLanguage.compute(s.state,query).entities.single().quantities.single().value,1e-9)
        assertTrue(s.state.solids.isEmpty())
        assertNull(RuhiDialogueMemory.read(s.state).pending)
    }
    @Test fun priorRadiusComesFromValidatedEngineValuesAndKeepsNewHeight() {
        for(radius in listOf(1.75,3.25,8.5)) {
            val s=Session(MathModule.Geometry3D)
            assertEquals(GraphCommandStatus.APPLIED,s.say("Create a sphere with radius $radius").status)
            val native=s.state.solids
            assertEquals(radius,RuhiDialogueMemory.read(s.state).roleValues.getValue("Geometry3D:radius"),0.0)
            val question=s.say("Find the volume of a cylinder with height 6")
            assertEquals(question.message,GraphCommandStatus.CLARIFICATION,question.status)
            assertEquals("radius",RuhiDialogueMemory.read(s.state).pending!!.targets.first())
            s.restore()
            val result=s.say("Use the same radius as before")
            assertEquals(result.message,GraphCommandStatus.APPLIED,result.status)
            val query=RuhiSolidMeasureLanguage.decode(s.state.labSessionValues.getValue(RuhiSolidMeasureLanguage.MARK))
            assertEquals(radius,query.dimensions.getValue("radius"),0.0)
            assertEquals(6.0,query.dimensions.getValue("height"),0.0)
            assertEquals(6*Math.PI*radius*radius,RuhiSolidMeasureLanguage.compute(s.state,query).entities.single().quantities.single().value,1e-9)
            assertEquals(native,s.state.solids)
        }
    }
    @Test fun missingRadiusAsksOneSlotAndInvalidCorrectionKeepsThePendingTask() {
        val s=Session(MathModule.Geometry3D)
        s.say("Find the volume of a cylinder with height 6")
        val pending=RuhiDialogueMemory.read(s.state).pending
        val question=s.say("Use the same radius as before")
        assertEquals(GraphCommandStatus.CLARIFICATION,question.status)
        assertTrue(question.message.startsWith("What radius should I use?"))
        assertEquals(pending,RuhiDialogueMemory.read(s.state).pending)
        assertEquals(2,question.dialogue!!.suggestions.size)
        val invalid=s.say("No, make it -7 instead")
        assertEquals(GraphCommandStatus.INVALID,invalid.status)
        assertEquals(pending,RuhiDialogueMemory.read(s.state).pending)
        assertTrue(s.state.solids.isEmpty())
        assertEquals(GraphCommandStatus.APPLIED,s.say("No, make it 7 instead").status)
        assertEquals(7.0,RuhiSolidMeasureLanguage.decode(s.state.labSessionValues.getValue(RuhiSolidMeasureLanguage.MARK)).dimensions.getValue("radius"),0.0)
    }
    @Test fun resumeRestoresTheActualQuestionAndBoundSceneChoices() {
        val s=Session(MathModule.Geometry2D)
        s.state=s.state.copy(points=listOf(Vec2(0.0,0.0),Vec2(1.0,0.0),Vec2(4.0,0.0),Vec2(6.0,0.0)),shapes=listOf(Shape2D("opaque,/907",Shape2DType.Circle,listOf(0,1)),Shape2D("opaque,/811",Shape2DType.Circle,listOf(2,3))))
        val question=s.say("Label the circle as Orbit")
        val pending=RuhiDialogueMemory.read(s.state).pending
        s.restore()
        val resumed=s.say("Continue where we stopped")
        assertEquals(GraphCommandStatus.CLARIFICATION,resumed.status)
        assertEquals(RuhiDialogueAction.RESUME_PENDING,resumed.dialogue!!.action)
        assertEquals(question.message,resumed.message)
        assertEquals(question.dialogue!!.suggestions,resumed.dialogue!!.suggestions)
        assertEquals(pending,RuhiDialogueMemory.read(s.state).pending)
        assertEquals(GraphCommandStatus.APPLIED,s.say("Choose the second suggested answer").status)
        assertEquals("Orbit",s.state.shapes.single{it.id=="opaque,/811"}.name)
    }
    @Test fun resumeAfterNativeSceneChangeClearsTheObsoletePendingTask() {
        val s=Session(MathModule.Geometry3D)
        s.state=s.state.copy(solids=listOf(Solid(SolidType.Cube,2.0),Solid(SolidType.Cube,3.0)))
        assertEquals(GraphCommandStatus.CLARIFICATION,s.say("Move the cube to (1,2,3)").status)
        s.state=s.state.copy(solids=s.state.solids.mapIndexed{i,q->if(i==0)q.copy(position=Vec3(7.0,8.0,9.0))else q})
        val before=s.state.solids
        val resumed=s.say("Continue where we stopped")
        assertEquals(GraphCommandStatus.CLARIFICATION,resumed.status)
        assertNull(RuhiDialogueMemory.read(s.state).pending)
        assertTrue(resumed.dialogue!!.suggestions.isEmpty())
        assertEquals(before,s.state.solids)
    }
}


