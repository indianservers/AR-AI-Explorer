package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class GeometrySelectionMeasurementsTest {
    private fun values(points:List<Vec2>,type:Shape2DType,indices:List<Int> = points.indices.toList())=GeometrySelectionMeasurements.measure(points,listOf(Shape2D("opaque,/907",type,indices)),setOf(0))
    private fun quantities(r:GeometrySelectionMeasurements.Result)=r.quantities.associateBy{it.label}
    @Test fun triangleLabelUsesAreaAndPerimeterRatherThanAnUnrelatedEdge() {
        val r=values(listOf(Vec2(0.0,0.0),Vec2(6.0,0.0),Vec2(3.0,3.0)),Shape2DType.Triangle)
        val q=quantities(r);assertEquals(setOf("area","perimeter"),q.keys)
        assertEquals(9.0,q.getValue("area").value!!,1e-9);assertEquals(6+2*sqrt(18.0),q.getValue("perimeter").value!!,1e-9)
        assertEquals(2,q.getValue("area").power);assertEquals(1,q.getValue("perimeter").power)
        val label=GeometrySelectionMeasurements.label(r){it.toString()};assertFalse(label.contains("Δx"));assertFalse(label.contains("slope"))
    }
    @Test fun quantitiesFollowEditedControlsAndAreInvariantUnderTranslation() {
        val p=listOf(Vec2(0.0,0.0),Vec2(6.0,0.0),Vec2(3.0,5.0))
        val r=values(p,Shape2DType.Triangle);val q=quantities(r)
        assertEquals(15.0,q.getValue("area").value!!,1e-9);assertEquals(6+2*sqrt(34.0),q.getValue("perimeter").value!!,1e-9)
        val translated=values(p.map{it+Vec2(-17.0,23.0)},Shape2DType.Triangle)
        r.quantities.zip(translated.quantities).forEach{(a,b)->assertEquals(a.value!!,b.value!!,1e-9)}
    }
    @Test fun segmentDeltasLengthAndSlopeAllUseTheSameOrderedEndpoints() {
        val q=quantities(values(listOf(Vec2(4.0,5.0),Vec2(-2.0,2.0)),Shape2DType.Segment))
        assertEquals(-6.0,q.getValue("Δx").value!!,0.0);assertEquals(-3.0,q.getValue("Δy").value!!,0.0)
        assertEquals(sqrt(45.0),q.getValue("length").value!!,1e-9);assertEquals(.5,q.getValue("slope").value!!,1e-9)
        assertEquals(0,q.getValue("slope").power)
    }
    @Test fun verticalSlopeIsUndefinedAndNearVerticalSlopeIsFinite() {
        val vertical=values(listOf(Vec2(0.0,0.0),Vec2(0.0,4.0)),Shape2DType.Segment)
        assertNull(quantities(vertical).getValue("slope").value)
        assertTrue(GeometrySelectionMeasurements.label(vertical){it.toString()}.contains("slope undefined"))
        val near=quantities(values(listOf(Vec2(0.0,0.0),Vec2(1e-10,4.0)),Shape2DType.Segment))
        assertEquals(4e10,near.getValue("slope").value!!,1e-3)
    }
    @Test fun circlesUseTheirNativeRadiusAndThreePointCircumcenter() {
        val cases=listOf(values(listOf(Vec2(2.0,3.0),Vec2(5.0,3.0)),Shape2DType.Circle) to 3.0,values(listOf(Vec2(0.0,0.0),Vec2(4.0,0.0),Vec2(0.0,3.0)),Shape2DType.CircleThreePoints) to 2.5)
        cases.forEach{(r,radius)->val q=quantities(r);assertEquals(radius,q.getValue("radius").value!!,1e-9);assertEquals(2*PI*radius,q.getValue("circumference").value!!,1e-9);assertEquals(PI*radius*radius,q.getValue("area").value!!,1e-9)}
    }
    @Test fun unboundedLinesDoNotClaimAFiniteTotalLength() {
        for(type in listOf(Shape2DType.Line,Shape2DType.Ray)) {
            val q=quantities(values(listOf(Vec2(1.0,2.0),Vec2(4.0,6.0)),type))
            assertFalse(q.containsKey("length"));assertEquals(3.0,q.getValue("direction x").value!!,0.0);assertEquals(4.0,q.getValue("direction y").value!!,0.0);assertEquals(4.0/3,q.getValue("slope").value!!,1e-9)
        }
    }
    @Test fun groupsDescribeTheDrawnControlBoxInsteadOfAnArbitraryCrossObjectSegment() {
        val p=listOf(Vec2(-4.0,-2.0),Vec2(-1.0,1.0),Vec2(3.0,5.0),Vec2(7.0,8.0))
        val shapes=listOf(Shape2D("a",Shape2DType.Segment,listOf(0,1)),Shape2D("b",Shape2DType.Segment,listOf(2,3)))
        val q=quantities(GeometrySelectionMeasurements.measure(p,shapes,setOf(0,1)))
        assertEquals(setOf("box width","box height"),q.keys);assertEquals(11.0,q.getValue("box width").value!!,0.0);assertEquals(10.0,q.getValue("box height").value!!,0.0)
    }
    @Test fun undefinedCirclesAndMissingControlsDoNotProduceInventedMeasurements() {
        val flat=values(listOf(Vec2(0.0,0.0),Vec2(1.0,0.0),Vec2(2.0,0.0)),Shape2DType.CircleThreePoints)
        assertTrue(flat.quantities.isEmpty());assertEquals("Undefined circle",flat.note)
        val missing=values(listOf(Vec2(0.0,0.0)),Shape2DType.Segment,listOf(0,5))
        assertTrue(missing.quantities.isEmpty());assertEquals("Missing control point",missing.note)
    }
    @Test fun displayRoundingDoesNotTurnNonzeroDeltasIntoZeroOrFiniteValuesIntoInfinity() {
        val near=values(listOf(Vec2(0.0,0.0),Vec2(1e-10,4.0)),Shape2DType.Segment)
        val text=GeometrySelectionMeasurements.label(near,::trim)
        assertTrue(text.contains("Δx 1.0E-10"));assertTrue(text.contains("slope 40000000000"))
        val large=GeometrySelectionMeasurements.Result(listOf(GeometrySelectionMeasurements.Quantity("length",1e308,1)))
        val rendered=GeometrySelectionMeasurements.label(large,::trim)
        assertTrue(rendered.contains("1.0E308"));assertFalse(rendered.contains("Infinity"))
    }
}
