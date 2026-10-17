package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.workspace.*
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import org.junit.Assert.*
import org.junit.Test

class RuhiConstructionConversationTest {
    private class Session(module:MathModule) {
        val manager=RuhiDialogueManager();val histories=WorkspaceHistoryByModule()
        var state=WorkspaceState(module=module);var context=RuhiSceneContext()
        fun say(text:String)=manager.execute(state,text,histories,context).also{state=it.state;context=it.context}
        fun restore(){state=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(state)).state!!}
    }
    private fun triangle()=WorkspaceState(module=MathModule.Geometry2D,points=listOf(Vec2(0.0,0.0),Vec2(4.0,0.0),Vec2(0.0,3.0),Vec2(20.0,30.0)),shapes=listOf(Shape2D("roof,/907",Shape2DType.Triangle,listOf(0,1,2),"Roof_811")))
    @Test fun originalExplanationUsesCurrentTriangleControlsWithoutHistory() {
        val s=Session(MathModule.Geometry2D);s.state=triangle();val before=s.state
        val r=s.say("Explain the shape we are viewing")
        assertEquals(r.message,GraphCommandStatus.APPLIED,r.status)
        assertTrue(r.message.contains("Roof_811"));assertTrue(r.message.contains("area 6.0 square units"));assertTrue(r.message.contains("perimeter 12.0 units"))
        assertEquals(before.points,s.state.points);assertEquals(before.shapes,s.state.shapes)
        assertFalse(s.histories.forModule(MathModule.Geometry2D).canUndo)
        s.state=s.state.copy(points=s.state.points.mapIndexed{i,p->if(i==1)Vec2(8.0,0.0)else p})
        assertTrue(s.say("Explain the shape we are viewing").message.contains("area 12.0 square units"))
    }
    @Test fun nativeAnisotropicCubeExplanationUsesAllThreeDimensions() {
        val s=Session(MathModule.Geometry3D);s.state=s.state.copy(solids=listOf(Solid(SolidType.Cube,2.0,3.0,4.0),Solid(SolidType.Sphere,2.0)));s.context=RuhiSceneContext(selectedSolidIndex=0)
        val before=s.state.solids;val r=s.say("Explain the shape we are viewing")
        assertEquals(r.message,GraphCommandStatus.APPLIED,r.status)
        assertTrue(r.message.contains("volume 24.0 cubic units"));assertTrue(r.message.contains("surface area 52.0 square units"))
        assertEquals(before,s.state.solids);assertFalse(s.histories.forModule(MathModule.Geometry3D).canUndo)
    }
    @Test fun stepsWalkActualParentsBeforeDependentControlsAndExcludeUnrelatedPoints() {
        val s=Session(MathModule.Geometry2D)
        s.state=s.state.copy(points=listOf(Vec2(0.0,0.0),Vec2(4.0,2.0),Vec2(2.0,1.0),Vec2(30.0,40.0)),shapes=listOf(Shape2D("half,/907",Shape2DType.Segment,listOf(0,2),"HalfEdge_811")),pointDependencies=listOf(PointDependency(2,listOf(0,1),PointDependencyType.Midpoint)),labSessionValues=mapOf("ruhi.studio.point.Start_907" to "0","ruhi.studio.point.End_811" to "1","ruhi.studio.point.Half_662" to "2","ruhi.studio.point.Unrelated_553" to "3"))
        val before=s.state;val r=s.say("Show me the drawing step by step")
        assertEquals(r.message,GraphCommandStatus.APPLIED,r.status)
        val steps=r.message.lines();assertEquals(4,steps.size)
        assertTrue(steps[0].contains("Start_907"));assertTrue(steps[1].contains("End_811"));assertTrue(steps[2].contains("midpoint of Start_907, End_811"));assertTrue(steps[2].contains("(2.0, 1.0)"))
        assertFalse(r.message.contains("Unrelated_553"));assertEquals(before.points,s.state.points);assertEquals(before.pointDependencies,s.state.pointDependencies)
        s.state=s.state.copy(points=s.state.points.mapIndexed{i,p->if(i==1)Vec2(8.0,4.0)else p}).recomputed();s.restore()
        assertTrue(s.say("Show me the drawing step by step").message.contains("(4.0, 2.0)"))
        assertFalse(s.histories.forModule(MathModule.Geometry2D).canUndo)
    }
    @Test fun measurementRequestRestoresNativeShapeOverlayWithOneUndo() {
        val s=Session(MathModule.Geometry2D);s.state=triangle().let{it.copy(labSessionValues=mapOf(RuhiSceneEditLanguage.PREFIX+"measure.roof,/907" to "false"))}
        val before=s.state;val r=s.say("Now show the measurements")
        assertEquals(r.message,GraphCommandStatus.APPLIED,r.status)
        assertTrue(RuhiSceneEditLanguage.measurements(s.state,"roof,/907"));assertEquals("roof,/907",r.context.selectedShapeId)
        assertEquals(before.points,s.state.points);assertEquals(before.shapes,s.state.shapes)
        s.say("Undo");assertFalse(RuhiSceneEditLanguage.measurements(s.state,"roof,/907"))
    }
    @Test fun measurementRequestBindsLiveSelectedSolidAndPreservesUnrelatedSolid() {
        val s=Session(MathModule.Geometry3D);s.state=s.state.copy(solids=listOf(Solid(SolidType.Cube,2.0,3.0,4.0),Solid(SolidType.Sphere,3.0)));s.context=RuhiSceneContext(selectedSolidIndex=0)
        val before=s.state.solids;val r=s.say("Now show the measurements")
        assertEquals(r.message,GraphCommandStatus.APPLIED,r.status)
        val q=RuhiSolidMeasureLanguage.decode(s.state.labSessionValues.getValue(RuhiSolidMeasureLanguage.MARK))
        assertNull(q.shape);assertEquals(listOf(0),q.indices)
        val values=RuhiSolidMeasureLanguage.compute(s.state,q).entities.single().quantities.associate{it.name to it.value}
        assertEquals(24.0,values.getValue("volume"),0.0);assertEquals(52.0,values.getValue("surface_area"),0.0)
        assertEquals(before,s.state.solids)
    }
    @Test fun actualConstructionChoicesRestoreResumeAndUseStableOpaqueIds() {
        val s=Session(MathModule.Geometry2D)
        s.state=s.state.copy(points=listOf(Vec2(0.0,0.0),Vec2(1.0,0.0),Vec2(4.0,0.0),Vec2(7.0,0.0)),shapes=listOf(Shape2D("orb,/907",Shape2DType.Circle,listOf(0,1),"Small_907"),Shape2D("orb,/811",Shape2DType.Circle,listOf(2,3),"Large_811")))
        val q=s.say("Explain the shape we are viewing");assertEquals(GraphCommandStatus.CLARIFICATION,q.status);assertEquals(2,q.dialogue!!.suggestions.size)
        s.restore();val resumed=s.say("Continue where we stopped");assertEquals(q.message,resumed.message);assertEquals(q.dialogue!!.suggestions,resumed.dialogue!!.suggestions)
        val r=s.say("Choose the second suggested answer");assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertTrue(r.message.contains("Large_811"));assertTrue(r.message.contains("radius 3.0 units"));assertEquals("orb,/811",r.context.selectedShapeId)
        assertNull(RuhiDialogueMemory.read(s.state).pending)
    }
    @Test fun nativeParentEditRetainsTheLastCreatedStableShapeForSteps() {
        val s=Session(MathModule.Geometry2D)
        s.state=s.state.copy(points=listOf(Vec2(20.0,20.0),Vec2(21.0,20.0)),shapes=listOf(Shape2D("unrelated,/553",Shape2DType.Circle,listOf(0,1))))
        s.say("Make a triangle")
        val created=s.state.shapes.single{it.type==Shape2DType.Triangle};val id=created.id;val first=created.pointIndices.first()
        s.state=s.state.copy(points=s.state.points.mapIndexed{i,p->if(i==first)p+Vec2(2.25,1.75)else p}).recomputed();s.context=RuhiSceneContext();s.restore()
        val r=s.say("Show me the drawing step by step")
        assertEquals(r.message,GraphCommandStatus.APPLIED,r.status)
        assertEquals("s:$id",r.semanticFrame!!.parameters["resolved_object_id"])
        assertEquals(id,RuhiDialogueMemory.read(s.state).lastCreated)
        assertTrue(r.message.contains("(${s.state.points[first].x}, ${s.state.points[first].y})"))
    }
    @Test fun dependencyCyclesAndStaleAnswersDoNotChangeTheConstruction() {
        val s=Session(MathModule.Geometry2D);s.state=triangle().copy(pointDependencies=listOf(PointDependency(0,listOf(1,2),PointDependencyType.Midpoint),PointDependency(1,listOf(0,2),PointDependencyType.Midpoint)))
        val before=s.state.points;val r=s.say("Show me the drawing step by step")
        assertEquals(GraphCommandStatus.INVALID,r.status);assertEquals(before,s.state.points)
        assertFalse(s.histories.forModule(MathModule.Geometry2D).canUndo)
        val many=Session(MathModule.Geometry3D);many.state=many.state.copy(solids=listOf(Solid(SolidType.Cube,2.0),Solid(SolidType.Sphere,2.0)))
        val q=many.say("Explain the shape we are viewing");assertEquals(GraphCommandStatus.CLARIFICATION,q.status)
        many.state=many.state.copy(solids=many.state.solids.mapIndexed{i,solid->if(i==1)solid.copy(radius=4.0)else solid})
        val current=many.state.solids
        assertEquals(GraphCommandStatus.CLARIFICATION,many.say(q.dialogue!!.suggestions.first().answer).status);assertEquals(current,many.state.solids)
    }
    @Test fun deicticDrawingAsksForCoordinatesThenCreatesAnIndependentCircleAtTheAnswer() {
        val s=Session(MathModule.Geometry2D)
        s.state=s.state.copy(points=listOf(Vec2(2.0,3.0),Vec2(4.0,3.0)),shapes=listOf(Shape2D("orbit,/907",Shape2DType.Circle,listOf(0,1),"Orbit_811",styleKey="#0000FF")))
        val before=s.state
        val q=s.say("Draw it over there")
        assertEquals(GraphCommandStatus.CLARIFICATION,q.status);assertTrue(q.message.contains("copy's center"));assertEquals(3,q.dialogue!!.suggestions.size)
        assertEquals(before.points,s.state.points);assertEquals(before.shapes,s.state.shapes)
        s.restore();val resumed=s.say("Continue where we stopped");assertEquals(q.dialogue!!.suggestions,resumed.dialogue!!.suggestions)
        val r=s.say("(7, -4)");assertEquals(r.message,GraphCommandStatus.APPLIED,r.status)
        assertEquals(2,s.state.shapes.size);assertEquals(before.shapes.single(),s.state.shapes.first());assertEquals(before.points,s.state.points.take(2))
        val copy=s.state.shapes.last();assertNotEquals(before.shapes.single().id,copy.id);assertTrue(copy.pointIndices.none{it in before.shapes.single().pointIndices})
        val circle=RuhiCircleLanguage.describe(s.state,copy)!!;assertEquals(Vec2(7.0,-4.0),circle.center);assertEquals(2.0,circle.radius,1e-9);assertEquals("#0000FF",copy.styleKey)
        s.say("Undo");assertEquals(before.points,s.state.points);assertEquals(before.shapes,s.state.shapes)
    }
    @Test fun deicticSolidCopyPreservesOriginalPoseDimensionsAndAppearance() {
        val s=Session(MathModule.Geometry3D)
        val original=Solid(SolidType.Cone,4.0,7.0,4.0,2.0,position=Vec3(2.0,3.0,4.0),rotation=Vec3(17.0,22.0,31.0),mirrorX=true)
        s.state=s.state.copy(solids=listOf(original),labSessionValues=mapOf("ruhi.studio.appearance.0" to "blue",RuhiTransform3DLanguage.PREFIX+"opacity.0" to ".3"))
        assertEquals(GraphCommandStatus.CLARIFICATION,s.say("Draw it over there").status)
        val r=s.say("(-5, 2, 9)");assertEquals(r.message,GraphCommandStatus.APPLIED,r.status)
        assertEquals(original,s.state.solids.first());assertEquals(original.copy(position=Vec3(-5.0,2.0,9.0)),s.state.solids.last())
        assertEquals("blue",s.state.labSessionValues["ruhi.studio.appearance.1"]);assertEquals(.3,RuhiTransform3DLanguage.opacity(s.state,1),0.0)
        assertEquals(1,r.context.selectedSolidIndex)
    }
    @Test fun invalidAndStalePlacementAnswersDoNotMoveOrDuplicateObjects() {
        val s=Session(MathModule.Geometry2D);s.state=triangle();val original=s.state
        s.say("Draw it over there")
        val pending=RuhiDialogueMemory.read(s.state).pending
        for(answer in listOf("(abc,2)","(10001,2)","(1,2,3)")) {
            assertEquals(answer,GraphCommandStatus.INVALID,s.say(answer).status)
            assertEquals(pending,RuhiDialogueMemory.read(s.state).pending);assertEquals(original.points,s.state.points);assertEquals(original.shapes,s.state.shapes)
        }
        s.state=s.state.copy(points=s.state.points.mapIndexed{i,p->if(i==1)Vec2(7.0,0.0)else p})
        val changed=s.state.points
        assertEquals(GraphCommandStatus.CLARIFICATION,s.say("(3,4)").status);assertEquals(changed,s.state.points);assertEquals(1,s.state.shapes.size)
    }
    @Test fun ambiguousDeicticTargetThenLocationAreTwoBoundSubtasksOfOneRequest() {
        val s=Session(MathModule.Geometry2D)
        s.state=s.state.copy(points=listOf(Vec2(0.0,0.0),Vec2(1.0,0.0),Vec2(4.0,0.0),Vec2(7.0,0.0)),shapes=listOf(Shape2D("a,/907",Shape2DType.Circle,listOf(0,1),"Small_907"),Shape2D("b,/811",Shape2DType.Circle,listOf(2,3),"Large_811")))
        val q=s.say("Draw it over there");assertEquals(GraphCommandStatus.CLARIFICATION,q.status);assertEquals(2,q.dialogue!!.suggestions.size)
        assertEquals(GraphCommandStatus.CLARIFICATION,s.say("Choose the second suggested answer").status)
        assertEquals("Draw it over there",RuhiDialogueMemory.read(s.state).pending!!.source)
        s.restore();assertEquals(GraphCommandStatus.APPLIED,s.say("(8,9)").status)
        assertEquals(3,s.state.shapes.size);assertEquals(3.0,RuhiCircleLanguage.describe(s.state,s.state.shapes.last())!!.radius,1e-9)
    }
    @Test fun largerAndRotateThatReuseTheKnownLastSolidAfterSelectionIsRestored() {
        val s=Session(MathModule.Geometry3D);s.say("Create a cube");s.say("Create a sphere");s.say("Create a cube")
        val before=s.state.solids;s.restore();s.context=RuhiSceneContext()
        val larger=s.say("Make it larger");assertEquals(larger.message,GraphCommandStatus.APPLIED,larger.status)
        assertEquals(before.take(2),s.state.solids.take(2));assertEquals(before[2].width*1.25,s.state.solids[2].width,1e-9)
        s.restore();s.context=RuhiSceneContext()
        val rotated=s.say("Rotate that");assertEquals(rotated.message,GraphCommandStatus.APPLIED,rotated.status)
        assertEquals(before.take(2),s.state.solids.take(2));assertEquals(before[2].rotation+Vec3(0.0,45.0,0.0),s.state.solids[2].rotation)
        s.say("Move the selected object 2 units right")
        assertEquals(GraphCommandStatus.APPLIED,s.say("Undo that last 3D rotation").status)
        assertEquals(before[2].rotation,s.state.solids[2].rotation);assertEquals(2.0,s.state.solids[2].position.x,1e-9);assertEquals(before[2].width*1.25,s.state.solids[2].width,1e-9)
    }
    @Test fun largerKeepsAStableLastShapeAfterNativePointEditsWithOtherShapesPresent() {
        val s=Session(MathModule.Geometry2D)
        s.state=s.state.copy(points=listOf(Vec2(20.0,20.0),Vec2(21.0,20.0)),shapes=listOf(Shape2D("untouched,/907",Shape2DType.Circle,listOf(0,1))))
        s.say("Make a triangle");val shape=s.state.shapes.single{it.type==Shape2DType.Triangle};val ids=shape.pointIndices
        s.state=s.state.copy(points=s.state.points.mapIndexed{i,p->if(i==ids[0])p+Vec2(1.0,.5)else p}).recomputed();s.context=RuhiSceneContext();s.restore()
        val before=s.state;val length=before.points[ids[0]].distanceTo(before.points[ids[1]])
        val r=s.say("Make it larger");assertEquals(r.message,GraphCommandStatus.APPLIED,r.status)
        assertEquals(before.points.take(2),s.state.points.take(2));assertEquals(length*1.25,s.state.points[ids[0]].distanceTo(s.state.points[ids[1]]),1e-8)
        assertEquals(shape.id,r.context.selectedShapeId)
    }
    @Test fun drawingACopyPreservesAnExistingGroupsAnimationAndUndoRestoresItsGuard() {
        val s=Session(MathModule.Geometry3D)
        val initial=WorkspaceState(module=MathModule.Geometry3D,solids=listOf(Solid(SolidType.Cube,2.0,position=Vec3(3.0,0.0,1.0)),Solid(SolidType.Cone,2.0,3.0,position=Vec3(-2.0,1.0,0.0))),labSessionValues=mapOf("ruhi.geometry3d.selection" to "0,1"))
        val group=RuhiTransform3DLanguage.execute(initial,"Rotate both solids together around z",WorkspaceHistoryByModule(),RuhiSceneContext(),RuhiInputModality.TEXT,null,100)!!.result
        assertEquals(group.message,GraphCommandStatus.APPLIED,group.status)
        s.state=group.state;s.context=RuhiSceneContext(selectedSolidIndex=0)
        val before=RuhiMotionEngine.solids(s.state,3100)
        assertEquals(GraphCommandStatus.CLARIFICATION,s.say("Draw it over there").status)
        assertEquals(GraphCommandStatus.APPLIED,s.say("(8,9,10)").status)
        assertEquals(listOf(0,1),RuhiTransform3DLanguage.groupIds(s.state))
        assertEquals(before,RuhiMotionEngine.solids(s.state,3100).take(2))
        assertEquals(Vec3(8.0,9.0,10.0),RuhiMotionEngine.solids(s.state,3100)[2].position)
        assertEquals("2",s.state.labSessionValues["ruhi.studio.selected.solid"])
        s.say("Undo")
        assertEquals(2,s.state.solids.size);assertEquals(before,RuhiMotionEngine.solids(s.state,3100))
    }
    @Test fun explicitlyResolvedExplanationTargetRemainsTheTopicAfterRestoration() {
        val s=Session(MathModule.Geometry2D)
        s.state=s.state.copy(points=listOf(Vec2(0.0,0.0),Vec2(1.0,0.0),Vec2(4.0,0.0),Vec2(7.0,0.0)),shapes=listOf(Shape2D("a,/907",Shape2DType.Circle,listOf(0,1),"Small_907"),Shape2D("b,/811",Shape2DType.Circle,listOf(2,3),"Large_811")))
        s.say("Explain the shape we are viewing");assertEquals(GraphCommandStatus.APPLIED,s.say("Choose the second suggested answer").status)
        assertEquals("b,/811",RuhiDialogueMemory.read(s.state).lastModified)
        assertFalse(s.histories.forModule(MathModule.Geometry2D).canUndo)
        s.restore();s.context=RuhiSceneContext()
        val q=s.say("Draw it over there")
        assertEquals(GraphCommandStatus.CLARIFICATION,q.status);assertTrue(q.message.contains("copy's center"))
        val r=s.say("(8,9)");assertEquals(r.message,GraphCommandStatus.APPLIED,r.status)
        assertEquals(3.0,RuhiCircleLanguage.describe(s.state,s.state.shapes.last())!!.radius,1e-9)
    }
    @Test fun successfulExplanationPreservesAnInterruptedGraphOrPlacementRequest() {
        val s=Session(MathModule.Geometry2D);s.state=triangle()
        val question=s.say("Draw a graph");val pending=RuhiDialogueMemory.read(s.state).pending
        val explained=s.say("Explain the shape we are viewing")
        assertEquals(GraphCommandStatus.APPLIED,explained.status);assertEquals(RuhiDialogueAction.EXPLAIN,explained.dialogue!!.action)
        assertEquals(pending,RuhiDialogueMemory.read(s.state).pending)
        s.restore();val resumed=s.say("Continue where we stopped")
        assertEquals(question.message,resumed.message);assertEquals(question.dialogue!!.suggestions,resumed.dialogue!!.suggestions)
        assertEquals(GraphCommandStatus.APPLIED,s.say("Use x squared for the equation").status)
        assertEquals(1,s.state.functions.size);assertEquals(triangle().shapes,s.state.shapes)
        val placement=Session(MathModule.Geometry2D);placement.state=triangle()
        val where=placement.say("Draw it over there");val plan=RuhiDialogueMemory.read(placement.state).pending
        val aside=placement.say("Explain the shape we are viewing")
        assertEquals(GraphCommandStatus.APPLIED,aside.status);assertEquals(RuhiDialogueAction.EXPLAIN,aside.dialogue!!.action)
        assertEquals(plan,RuhiDialogueMemory.read(placement.state).pending)
        assertEquals(where.message,placement.say("Continue where we stopped").message)
        assertEquals(GraphCommandStatus.APPLIED,placement.say("(8,4)").status)
        assertEquals(2,placement.state.shapes.size)
    }
}
