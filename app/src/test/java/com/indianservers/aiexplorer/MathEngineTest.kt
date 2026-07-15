package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.ExpressionEngine
import com.indianservers.aiexplorer.core.Geometry2D
import com.indianservers.aiexplorer.core.Geometry3D
import com.indianservers.aiexplorer.core.Graph3D
import com.indianservers.aiexplorer.core.GraphAnalysis
import com.indianservers.aiexplorer.core.Solid
import com.indianservers.aiexplorer.core.SolidType
import com.indianservers.aiexplorer.core.Vec2
import com.indianservers.aiexplorer.workspace.WorkspaceJson
import com.indianservers.aiexplorer.workspace.WorkspaceState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sqrt

class MathEngineTest {
    private val epsilon = 1e-6

    @Test
    fun expressionParserHandlesPrecedenceFunctionsConstantsAndImplicitMultiplication() {
        val engine = ExpressionEngine()

        assertEquals(14.0, engine.compile("2 + 3 * 4").eval(), epsilon)
        assertEquals(20.0, engine.compile("(2 + 3) * 4").eval(), epsilon)
        assertEquals(-9.0, engine.compile("-3^2").eval(), epsilon)
        assertEquals(6.0, engine.compile("2x + 1").eval(mapOf("x" to 2.5)), epsilon)
        assertEquals(1.0, engine.compile("sin(pi / 2)").eval(), epsilon)
        assertEquals(3.0, engine.compile("sqrt(9)").eval(), epsilon)
    }

    @Test
    fun coordinateGeometryMatchesRequiredExample() {
        val measurement = Geometry2D.segment(Vec2(1.0, 2.0), Vec2(4.0, 5.0))

        assertEquals(2.5, measurement.midpoint.x, epsilon)
        assertEquals(3.5, measurement.midpoint.y, epsilon)
        assertEquals(1.0, measurement.slope!!, epsilon)
        assertEquals(sqrt(18.0), measurement.distance, epsilon)
        assertEquals("sqrt(18)", measurement.exactDistance)
    }

    @Test
    fun graphInsightsMatchDefaultWorkspace() {
        val graph = GraphAnalysis()
        val f = graph.quadratic(1.0, -4.0, 3.0)
        val g = graph.linear(1.0, -1.0)

        assertEquals(2.0, f.vertex.x, epsilon)
        assertEquals(-1.0, f.vertex.y, epsilon)
        assertEquals(listOf(1.0, 3.0), f.roots)
        assertEquals(3.0, f.yIntercept, epsilon)
        assertEquals(2.0, f.axis, epsilon)
        assertEquals(1.0, g.slope, epsilon)
        assertEquals(1.0, g.xIntercept!!, epsilon)
        assertEquals(-1.0, g.yIntercept, epsilon)
    }

    @Test
    fun graphIntersectionsMatchRequiredDefaultWorkspace() {
        val engine = ExpressionEngine()
        val intersections = GraphAnalysis().intersections(
            engine.compile("x^2 - 4*x + 3"),
            engine.compile("x - 1"),
            -2.0,
            6.0,
        )

        assertEquals(2, intersections.size)
        assertEquals(1.0, intersections[0].x, 1e-4)
        assertEquals(0.0, intersections[0].y, 1e-4)
        assertEquals(4.0, intersections[1].x, 1e-4)
        assertEquals(3.0, intersections[1].y, 1e-4)
    }

    @Test
    fun solidMeasurementsAreMathematicallyCorrect() {
        val cube = Geometry3D.measure(Solid(SolidType.Cube, width = 3.0))
        val cylinder = Geometry3D.measure(Solid(SolidType.Cylinder, width = 2.0, radius = 2.0, height = 5.0))
        val cone = Geometry3D.measure(Solid(SolidType.Cone, width = 2.0, radius = 2.0, height = 6.0))

        assertEquals(27.0, cube.volume, epsilon)
        assertEquals(54.0, cube.surfaceArea, epsilon)
        assertEquals(20.0 * PI, cylinder.volume, epsilon)
        assertEquals(8.0 * PI, cone.volume, epsilon)
    }

    @Test
    fun surfaceInsightMatchesRequiredParaboloidCase() {
        val graph3D = Graph3D()
        val insight = graph3D.insight("z = x^2 + y^2")
        val mesh = graph3D.mesh("z = x^2 + y^2", density = 4)

        assertEquals("Elliptic paraboloid", insight.classification)
        assertEquals("z >= 0", insight.range)
        assertEquals(0.0, insight.vertex!!.z, epsilon)
        assertTrue(mesh.vertices.any { it.x == 1.5 && it.y == 1.5 && it.z == 4.5 })
    }

    @Test
    fun workspaceJsonContainsMathematicalDefinitions() {
        val json = WorkspaceJson.export(WorkspaceState())

        assertTrue(json.contains("\"schemaVersion\": 1"))
        assertTrue(json.contains("\"points\""))
        assertTrue(json.contains("\"functions\""))
        assertTrue(json.contains("x^2 - 4*x + 3"))
        assertTrue(json.contains("x^2 + y^2"))
    }
}

