package com.indianservers.aiexplorer.arengine.measurement

import com.indianservers.aiexplorer.arengine.contract.ArColor
import com.indianservers.aiexplorer.arengine.contract.ArConstructionObjectType
import com.indianservers.aiexplorer.arengine.contract.ArConstructionState
import com.indianservers.aiexplorer.arengine.contract.ArLocalTransform
import com.indianservers.aiexplorer.arengine.contract.ArQuaternion
import com.indianservers.aiexplorer.arengine.contract.ArVector3
import com.indianservers.aiexplorer.arengine.labs.ArLabRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sqrt

class ArMeasurementToolkitTest {
    private val epsilon = 1e-8

    @Test
    fun unitsConvertLengthAreaVolumeAndDescribeScale() {
        val scale = ArPhysicalScale(.05)
        assertEquals(2.0, scale.lengthFromMeters(.1, ArLengthUnit.MathematicalUnit), epsilon)
        assertEquals(10.0, scale.lengthFromMeters(.1, ArLengthUnit.Centimeter), epsilon)
        assertEquals(10_000.0, scale.areaFromSquareMeters(1.0, ArLengthUnit.Centimeter), epsilon)
        assertEquals(1_000_000.0, scale.volumeFromCubicMeters(1.0, ArLengthUnit.Centimeter), epsilon)
        assertEquals("1 mathematical unit = 5.0 cm", scale.relationshipLabel())
        assertEquals(.045454545, scale.calibrated(.11, 10.0, ArLengthUnit.Centimeter).metersPerMathUnit, 1e-8)
    }

    @Test
    fun distanceAngleAndPolylineUseWorldSpace() {
        val a = point("A", 0.0, 0.0, 0.0)
        val b = point("B", 3.0, 4.0, 0.0)
        val c = point("C", 3.0, 4.0, 4.0)
        val distance = ArMeasurementMath.distance(a, b)
        assertEquals(5.0, distance.distanceMeters, epsilon)
        assertEquals(3.0, distance.horizontalMeters, epsilon)
        assertEquals(4.0, distance.verticalMeters, epsilon)
        assertEquals(9.0, ArMeasurementMath.polyline(listOf(a, b, c)).distanceMeters, epsilon)
        assertEquals(90.0, ArMeasurementMath.angle(a, b, c).degrees, epsilon)
    }

    @Test
    fun polygonReportsAreaPerimeterAndPlanarity() {
        val square = listOf(
            point("A", 0.0, 0.0, 0.0), point("B", 2.0, 0.0, 0.0),
            point("C", 2.0, 0.0, 2.0), point("D", 0.0, 0.0, 2.0),
        )
        val result = ArMeasurementMath.polygon(square)
        assertEquals(4.0, result.areaSquareMeters, epsilon)
        assertEquals(8.0, result.perimeterMeters, epsilon)
        assertTrue(result.planar)
        val warped = ArMeasurementMath.polygon(square.dropLast(1) + point("D", 0.0, .2, 2.0))
        assertFalse(warped.planar)
    }

    @Test
    fun circleFitReturnsCircumcenterRadiusAndDiameter() {
        val fit = ArMeasurementMath.circleThrough(
            point("A", 1.0, 0.0, 0.0),
            point("B", 0.0, 1.0, 0.0),
            point("C", -1.0, 0.0, 0.0),
        )
        assertEquals(0.0, fit.centerMeters.x, epsilon)
        assertEquals(0.0, fit.centerMeters.y, epsilon)
        assertEquals(1.0, fit.radiusMeters, epsilon)
        assertEquals(2.0, fit.diameterMeters, epsilon)
    }

    @Test
    fun cylinderConeSphereAndCuboidFitsExposeCorrectFormulaValues() {
        val base = point("A", 0.0, 0.0, 0.0)
        val rim = point("B", 2.0, 0.0, 0.0)
        val top = point("C", 0.0, 3.0, 0.0)
        val cylinder = ArPrimitiveFitter.cylinder(base, rim, top)
        assertEquals(2.0, cylinder.radiusMeters, epsilon)
        assertEquals(3.0, cylinder.heightMeters, epsilon)
        assertEquals(12.0 * PI, cylinder.volumeCubicMeters, epsilon)
        assertEquals(20.0 * PI, cylinder.surfaceAreaSquareMeters, epsilon)

        val cone = ArPrimitiveFitter.cone(base, rim, top)
        assertEquals(4.0 * PI, cone.volumeCubicMeters, epsilon)
        assertEquals(sqrt(13.0), cone.slantHeightMeters, epsilon)

        val sphere = ArPrimitiveFitter.sphere(base, listOf(rim, point("C", 0.0, 2.0, 0.0), point("D", 0.0, 0.0, 2.0)))
        assertEquals(16.0 * PI, sphere.surfaceAreaSquareMeters, epsilon)
        assertEquals(32.0 * PI / 3.0, sphere.volumeCubicMeters, epsilon)

        val cuboid = ArPrimitiveFitter.cuboid(base, point("B", 2.0, 0.0, 0.0), point("C", 0.0, 0.0, 3.0), point("D", 0.0, 4.0, 0.0))
        assertEquals(24.0, cuboid.volumeCubicMeters, epsilon)
        assertEquals(52.0, cuboid.surfaceAreaSquareMeters, epsilon)
        assertEquals(sqrt(29.0), cuboid.spaceDiagonalMeters, epsilon)
        assertEquals(0.0, cuboid.baseRightAngleErrorDegrees, epsilon)
    }

    @Test
    fun formatterAvoidsFalsePrecision() {
        assertEquals("1.2", ArMeasurementFormatter.number(1.23456, .08))
        assertEquals("1.23", ArMeasurementFormatter.number(1.23456, .008))
        assertTrue(ArMeasurementFormatter.approximate(1.234, .03, "m").startsWith("Approximate AR measurement"))
        assertTrue(ArMeasurementFormatter.confidence(.3).contains("move slowly"))
    }

    @Test
    fun capabilityRegistryCoversEveryCataloguedLabWithLessonSpecificFlags() {
        assertEquals(18, ArLabRegistry.definitions.size)
        assertEquals(18, ArLabRegistry.definitions.map { it.id }.distinct().size)
        assertTrue(ArLabRegistry.require("ar-3d-shapes").capabilities.supportsPrimitiveFitting)
        assertFalse(ArLabRegistry.require("ar-statistics").capabilities.supportsPrimitiveFitting)
        assertTrue(ArLabRegistry.require("ar-coordinate-plane").capabilities.supportsPointSelection)
    }

    @Test
    fun constructionStateRoundTripsWithoutLosing3dArParameters() {
        val state = ArConstructionState(
            id = "cylinder-1",
            labId = "ar-3d-shapes",
            objectType = ArConstructionObjectType.Solid,
            parameters = mapOf("radius" to .2, "height" to .7),
            transform = ArLocalTransform(
                offsetMeters = ArVector3(.1, .2, -.3),
                orientation = ArQuaternion.fromEulerDegrees(10.0, 20.0, 30.0),
                uniformScale = 1.4,
            ),
            metersPerMathUnit = .05,
            color = ArColor(.2f, .4f, .8f, .6f),
            opacity = .6f,
            wireframe = false,
            labConfiguration = mapOf("unit" to "cm"),
        )
        assertEquals(state, ArConstructionState.fromPersistedMap(state.toPersistedMap()))
    }

    @Test
    fun guidedFitWorkflowReportsProgressSupportsUndoAndProducesResult() {
        var workflow = ArPrimitiveFitWorkflow(ArFitShape.Cylinder)
        assertEquals("Tap the centre of the base.", workflow.instruction)
        workflow = workflow.add(point("A", 0.0, 0.0, 0.0)).add(point("B", 2.0, 0.0, 0.0))
        assertEquals(2.0 / 3.0, workflow.progress, epsilon)
        workflow = workflow.undo().add(point("B", 1.0, 0.0, 0.0)).add(point("C", 0.0, 3.0, 0.0))
        assertTrue(workflow.complete)
        assertTrue(workflow.result() is ArCylinderFit)
        assertTrue(workflow.lock().locked)
        assertFalse(workflow.restart().complete)
    }

    @Test(expected = IllegalArgumentException::class)
    fun angleRejectsCoincidentPoints() {
        val a = point("A", 0.0, 0.0, 0.0)
        ArMeasurementMath.angle(a, a, point("C", 1.0, 0.0, 0.0))
    }

    @Test(expected = IllegalArgumentException::class)
    fun circleRejectsCollinearPoints() {
        ArMeasurementMath.circleThrough(point("A", 0.0, 0.0, 0.0), point("B", 1.0, 0.0, 0.0), point("C", 2.0, 0.0, 0.0))
    }

    private fun point(label: String, x: Double, y: Double, z: Double) =
        ArMeasuredPoint(label, ArVector3(x, y, z), uncertaintyMeters = .01, confidence = .9)
}
