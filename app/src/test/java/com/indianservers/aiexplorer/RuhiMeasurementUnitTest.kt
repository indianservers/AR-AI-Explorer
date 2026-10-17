package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.assistant.workspace.*
import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import com.indianservers.aiexplorer.workspace.GeometryMeasurementUnits.Length
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import kotlin.math.PI

class RuhiMeasurementUnitTest {
    private class Session(var s:WorkspaceState,var c:RuhiSceneContext=RuhiSceneContext()) {
        val manager=RuhiDialogueManager();val history=WorkspaceHistoryByModule()
        fun say(text:String)=manager.execute(s,text,history,c).also{s=it.state;c=it.context}
        fun restore(){s=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(s)).state!!}
    }
    private fun triangle()=WorkspaceState(module=MathModule.Geometry2D,points=listOf(Vec2(0.0,0.0),Vec2(3.0,0.0),Vec2(0.0,4.0)),shapes=listOf(Shape2D("unit,/907",Shape2DType.Triangle,listOf(0,1,2))))
    @Test fun originalCentimetersRequestConvertsSelectedAreaAndPerimeterWithoutMovingControls() {
        val initial=triangle();val s=Session(initial,RuhiSceneContext(selectedShapeId="unit,/907"))
        s.say("Use meters for coordinate units");s.restore();val r=s.say("Use centimeters instead")
        assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertTrue(r.message,r.message.contains("area 60000 cm²"));assertTrue(r.message,r.message.contains("perimeter 1200 cm"))
        assertEquals(initial.points,s.s.points);assertEquals(initial.shapes,s.s.shapes)
        assertEquals("cm",r.semanticFrame!!.parameters["units"]);assertEquals("current_measurements",r.semanticFrame!!.parameters["target"])
        val u=GeometryMeasurementUnits.read(s.s);assertEquals(Length.METER,u.base);assertEquals(Length.CENTIMETER,u.display)
        val label=GeometrySelectionMeasurements.label(GeometrySelectionMeasurements.measure(s.s.points,s.s.shapes,setOf(0)),u,::trim)
        assertEquals("area 60000 cm² - perimeter 1200 cm",label)
        s.say("Undo");assertEquals(Length.METER,GeometryMeasurementUnits.read(s.s).display);assertEquals(initial.points,s.s.points)
    }
    @Test fun cubicAndSquareFactorsAndDimensionSegmentsMatchNativeCube() {
        val solids=listOf(Solid(SolidType.Cube,2.0));val s=Session(WorkspaceState(module=MathModule.Geometry3D,solids=solids),RuhiSceneContext(selectedSolidIndex=0))
        s.say("Find surface area and volume of the selected solid");s.say("Use meters for coordinate units")
        val task=RuhiActionContext.read(s.s);val r=s.say("Use centimeters instead")
        assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertTrue(r.message,r.message.contains("240000 cm²"));assertTrue(r.message,r.message.contains("8000000 cm³"));assertEquals(solids,s.s.solids)
        assertEquals(task,RuhiActionContext.read(s.s))
        val d=RuhiSolidMeasureLanguage.drawing(s.s)!!;assertEquals(listOf(24.0,8.0),d.entities.single().quantities.map{it.value})
        val u=GeometryMeasurementUnits.read(s.s);assertEquals(listOf("w = 200 cm","h = 200 cm","d = 200 cm"),d.entities.single().segments.map{RuhiSolidMeasureLanguage.segmentLabel(it,u)})
        assertTrue(RuhiSolidMeasureLanguage.drawingLabel(s.s).contains("8000000 cm³"))
    }
    @Test fun missingProvenanceAsksOneQuestionAndRestoresActualBaseChoice() {
        val initial=triangle().copy(labSessionValues=mapOf(RuhiSolidMeasureLanguage.UNIT to "m"));val s=Session(initial)
        val q=s.say("Use centimeters instead");assertEquals(GraphCommandStatus.CLARIFICATION,q.status);assertEquals(3,q.dialogue!!.suggestions.size);assertFalse(GeometryMeasurementUnits.read(s.s).known)
        s.restore();assertEquals(q.dialogue!!.suggestions,s.manager.restoredView(s.s)!!.suggestions)
        val r=s.say("Choose the first suggested answer");assertEquals(r.message,GraphCommandStatus.APPLIED,r.status)
        assertEquals(Length.METER,GeometryMeasurementUnits.read(s.s).base);assertEquals(Length.CENTIMETER,GeometryMeasurementUnits.read(s.s).display)
        assertEquals(initial.points,s.s.points);assertTrue(r.message,r.message.contains("60000 cm²"))
    }
    @Test fun centimetersAreNotAssumedToBeMetersAndRoundTripChangesOnlyDisplay() {
        val s=Session(triangle());s.say("Use centimeters for coordinate units");s.say("Use centimeters instead")
        assertEquals(1.0,GeometryMeasurementUnits.read(s.s).factor(3),0.0)
        val points=s.s.points;s.say("Use meters instead");val u=GeometryMeasurementUnits.read(s.s)
        assertEquals(Length.CENTIMETER,u.base);assertEquals(.0001,u.factor(2),1e-14);val area=u.text(6.0,2,::trim);assertEquals(.0006,area.substringBefore(' ').toDouble(),1e-14);assertTrue(area.endsWith(" m²"))
        s.say("Use centimeters instead");assertEquals(points,s.s.points);assertEquals(1.0,GeometryMeasurementUnits.read(s.s).factor(2),0.0)
    }
    @Test fun slopeAndCountsStayDimensionlessWhileLengthsConvert() {
        val u=GeometryMeasurementUnits.Context(Length.METER,Length.CENTIMETER)
        val points=listOf(Vec2(0.0,0.0),Vec2(3.0,4.0));val shapes=listOf(Shape2D("line",Shape2DType.Segment,listOf(0,1)))
        val label=GeometrySelectionMeasurements.label(GeometrySelectionMeasurements.measure(points,shapes,setOf(0)),u,::trim)
        assertEquals("Δx 300 cm - Δy 400 cm - length 500 cm - slope 1.333333",label)
        assertEquals(1.0,u.factor(0),0.0);assertEquals("",u.suffix(0))
        val cube=Solid(SolidType.Cube,2.0);val topology=RuhiSolidMeasureMath.quantity(cube,"topology")
        assertEquals(topology.exact,RuhiSolidMeasureLanguage.quantityLabel(cube,topology,u))
    }
    @Test fun currentNativeEditsRecomputeConvertedValuesAndExplanations() {
        val s=Session(triangle(),RuhiSceneContext(selectedShapeId="unit,/907"));s.say("Use meters for coordinate units");s.say("Use centimeters instead")
        s.s=s.s.copy(points=s.s.points.toMutableList().apply{this[2]=Vec2(0.0,5.0)})
        val r=s.say("Explain the shape we are viewing");assertEquals(r.message,GraphCommandStatus.APPLIED,r.status);assertTrue(r.message,r.message.contains("75000.0 cm²"));assertEquals(Vec2(0.0,5.0),s.s.points[2])
        val cube=Session(WorkspaceState(module=MathModule.Geometry3D,solids=listOf(Solid(SolidType.Cube,2.0))),RuhiSceneContext(selectedSolidIndex=0))
        cube.say("Use meters for coordinate units");cube.say("Use centimeters instead");val q=cube.say("Find volume of the selected solid")
        assertTrue(q.message,q.message.contains("cm³"));assertTrue(q.message,q.message.contains("1000000"))
        cube.say("Scale the cube by 1.5");val d=RuhiSolidMeasureLanguage.drawing(cube.s)!!
        assertEquals(27.0,d.entities.single().quantities.single().value,1e-9);assertEquals(27e6,27.0*GeometryMeasurementUnits.read(cube.s).factor(3),0.0)
    }
    @Test fun stalePendingAnswerAndResumeDoNotApplyOldUnitChoice() {
        for(resume in listOf(false,true)) {
            val s=Session(triangle());val q=s.say("Use centimeters instead");s.s=s.s.copy(points=s.s.points+Vec2(9.0,9.0));val current=s.s.points
            val r=s.say(if(resume)"Resume"else q.dialogue!!.suggestions.first().answer)
            assertEquals(GraphCommandStatus.CLARIFICATION,r.status);assertNull(RuhiDialogueMemory.read(s.s).pending);assertFalse(GeometryMeasurementUnits.read(s.s).known);assertEquals(current,s.s.points)
        }
    }
    @Test fun baseAndDisplayUnitsAreScopedToGeometryModuleAndSavedTogether() {
        val s=Session(triangle());s.say("Use meters for coordinate units");s.say("Use centimeters instead");s.restore()
        assertEquals(Length.CENTIMETER,GeometryMeasurementUnits.read(s.s).display)
        val other=s.s.copy(module=MathModule.Geometry3D);assertFalse(GeometryMeasurementUnits.read(other).known)
        val graph=Session(s.s.copy(module=MathModule.Graph2D));val r=graph.say("Use centimeters instead")
        assertEquals(GraphCommandStatus.CLARIFICATION,r.status);assertEquals(s.s.points,graph.s.points)
    }
    @Test fun circleAndBaseAreaAnnotationsUseTheirOwnDimensionalPower() {
        val u=GeometryMeasurementUnits.Context(Length.METER,Length.CENTIMETER)
        val measure=GeometrySelectionMeasurements.measure(listOf(Vec2(0.0,0.0),Vec2(2.0,0.0)),listOf(Shape2D("c",Shape2DType.Circle,listOf(0,1))),setOf(0))
        assertEquals(200.0,measure.quantities[0].value!!*u.factor(1),0.0);assertEquals(40000*PI,measure.quantities[2].value!!*u.factor(2),1e-8)
        val segment=RuhiSolidMeasureLanguage.Segment(Vec3(0.0,0.0,0.0),Vec3(2.0,0.0,0.0),"base A = 4",4.0,2)
        assertEquals("base A = 40000 cm²",RuhiSolidMeasureLanguage.segmentLabel(segment,u))
    }
    @Test fun finiteMagnitudeAndTinyNonzeroConversionsDoNotBecomeInfinityOrZero() {
        val large=GeometryMeasurementUnits.Context(Length.METER,Length.CENTIMETER).number(1e308,3,::trim)
        assertTrue(BigDecimal(large)>BigDecimal("1e313"));assertFalse(large.contains("Infinity"))
        val tiny=GeometryMeasurementUnits.Context(Length.CENTIMETER,Length.METER).number(1e-320,3,::trim)
        assertTrue(BigDecimal(tiny)>BigDecimal.ZERO)
        assertEquals("1.0E-10",GeometryMeasurementUnits.Context().number(1e-10,1,::trim))
    }
    @Test fun exactMillimeterVolumeConversionNeverPrintsAZeroMultiplier() {
        val s=Session(WorkspaceState(module=MathModule.Geometry3D,solids=listOf(Solid(SolidType.Cube,2.0))),RuhiSceneContext(selectedSolidIndex=0))
        val q=s.say("Use meters instead");assertEquals(GraphCommandStatus.CLARIFICATION,q.status)
        s.say(q.dialogue!!.suggestions[2].answer);assertEquals(Length.MILLIMETER,GeometryMeasurementUnits.read(s.s).base)
        val r=s.say("Find volume of the selected solid");assertEquals(r.message,GraphCommandStatus.APPLIED,r.status)
        assertTrue(r.message,r.message.contains("* 0.000000001"));assertFalse(r.message.contains("* 0 ≈"))
        assertEquals(1e-9,GeometryMeasurementUnits.read(s.s).factor(3),0.0)
    }
}
