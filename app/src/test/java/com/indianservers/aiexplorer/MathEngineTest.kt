package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.ExpressionEngine
import com.indianservers.aiexplorer.core.Geometry2D
import com.indianservers.aiexplorer.core.Geometry3D
import com.indianservers.aiexplorer.core.CrossSection3D
import com.indianservers.aiexplorer.core.Graph3D
import com.indianservers.aiexplorer.core.GraphAnalysis
import com.indianservers.aiexplorer.core.StatisticsEngine
import com.indianservers.aiexplorer.core.ProbabilityEngine
import com.indianservers.aiexplorer.core.GraphDefinitionKind
import com.indianservers.aiexplorer.core.Solid
import com.indianservers.aiexplorer.core.SolidType
import com.indianservers.aiexplorer.core.SolidMeshFactory
import com.indianservers.aiexplorer.core.Vec2
import com.indianservers.aiexplorer.core.Vec3
import com.indianservers.aiexplorer.workspace.WorkspaceJson
import com.indianservers.aiexplorer.workspace.WorkspaceState
import com.indianservers.aiexplorer.workspace.CommandHistory
import com.indianservers.aiexplorer.workspace.MovePointsCommand
import com.indianservers.aiexplorer.workspace.AddConstructionCommand
import com.indianservers.aiexplorer.workspace.AddDependentPointCommand
import com.indianservers.aiexplorer.workspace.MovePointCommand
import com.indianservers.aiexplorer.workspace.PointDependencyType
import com.indianservers.aiexplorer.workspace.Shape2DType
import com.indianservers.aiexplorer.workspace.Shape2D
import com.indianservers.aiexplorer.workspace.TransformShape2DCommand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
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
        assertEquals(4.0, engine.compile("if(x<0,-x,x)").eval(mapOf("x" to -4.0)), epsilon)
        assertEquals(5.0, engine.compile("max(2,5,3)").eval(), epsilon)
        assertEquals(1.0, engine.compile("3>=2").eval(), epsilon)
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
    fun advancedSolidMeasurementsAreMathematicallyCorrect() {
        val hemisphere = Geometry3D.measure(Solid(SolidType.Hemisphere, width = 4.0, radius = 2.0))
        val frustum = Geometry3D.measure(Solid(SolidType.Frustum, width = 2.0, height = 2.0, radius = 1.0, topRadius = .5))
        val tetrahedron = Geometry3D.measure(Solid(SolidType.Tetrahedron, width = 3.0))

        assertEquals(16.0 * PI / 3.0, hemisphere.volume, epsilon)
        assertEquals(12.0 * PI, hemisphere.surfaceArea, epsilon)
        assertEquals(7.0 * PI / 6.0, frustum.volume, epsilon)
        assertEquals(27.0 / (6.0 * sqrt(2.0)), tetrahedron.volume, epsilon)
        assertEquals(4, tetrahedron.faces)
    }

    @Test
    fun solidMeshSupportsSubObjectsAndCrossSections() {
        val mesh = SolidMeshFactory.create(Solid(SolidType.Cube, width = 2.0))
        val section = CrossSection3D.intersect(mesh, Vec3(0.0, 1.0, 0.0), 0.0)

        assertEquals(8, mesh.vertices.size)
        assertEquals(12, mesh.edges.size)
        assertEquals(6, mesh.faces.size)
        assertEquals(4, section.size)
        assertTrue(section.all { abs(it.y) < epsilon })
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
        assertTrue(json.contains("\"pointDependencies\""))
        assertTrue(json.contains("x^2 - 4*x + 3"))
        assertTrue(json.contains("x^2 + y^2"))
        assertTrue(json.contains("\"rotation\""))
        assertTrue(json.contains("\"topRadius\""))
    }

    @Test
    fun completedDragIsRecordedAsOneAtomicUndoStep() {
        val history = CommandHistory()
        val initial = WorkspaceState()
        val indices = listOf(0, 1)
        val from = indices.map(initial.points::get)
        val delta = Vec2(2.0, -1.0)
        val to = from.map { it + delta }
        val preview = initial.copy(
            points = initial.points.mapIndexed { index, point ->
                indices.indexOf(index).takeIf { it >= 0 }?.let(to::get) ?: point
            },
        )

        history.recordApplied(MovePointsCommand(indices, from, to))

        val undone = history.undo(preview)
        assertEquals(from, indices.map(undone.points::get))
        assertEquals(to, indices.map(history.redo(undone).points::get))
    }

    @Test
    fun triangleCentersSatisfyGeometricInvariants() {
        val a = Vec2(0.0, 0.0)
        val b = Vec2(6.0, 0.0)
        val c = Vec2(0.0, 8.0)

        val centroid = Geometry2D.centroid(a, b, c)
        val circumcenter = Geometry2D.circumcenter(a, b, c)!!
        val incenter = Geometry2D.incenter(a, b, c)!!
        val orthocenter = Geometry2D.orthocenter(a, b, c)!!

        assertEquals(2.0, centroid.x, epsilon)
        assertEquals(8.0 / 3.0, centroid.y, epsilon)
        assertEquals(circumcenter.distanceTo(a), circumcenter.distanceTo(b), epsilon)
        assertEquals(circumcenter.distanceTo(b), circumcenter.distanceTo(c), epsilon)
        assertEquals(2.0, incenter.x, epsilon)
        assertEquals(2.0, incenter.y, epsilon)
        assertEquals(a.x, orthocenter.x, epsilon)
        assertEquals(a.y, orthocenter.y, epsilon)
    }

    @Test
    fun dependentMidpointRecomputesWhenParentMovesAndUndoRestoresIt() {
        val history = CommandHistory()
        val initial = WorkspaceState(points = listOf(Vec2(0.0, 0.0), Vec2(4.0, 2.0)), shapes = emptyList())
        val withMidpoint = history.execute(
            initial,
            AddDependentPointCommand(listOf(0, 1), PointDependencyType.Midpoint, "M"),
        )
        assertEquals(Vec2(2.0, 1.0), withMidpoint.points[2])

        val moved = history.execute(withMidpoint, MovePointCommand(1, Vec2(4.0, 2.0), Vec2(8.0, 4.0)))
        assertEquals(Vec2(4.0, 2.0), moved.points[2])

        val undone = history.undo(moved)
        assertEquals(Vec2(2.0, 1.0), undone.points[2])
    }

    @Test
    fun constructionCanReuseExistingPointsWithoutCreatingDuplicates() {
        val initial = WorkspaceState(points = listOf(Vec2(0.0, 0.0), Vec2(3.0, 2.0)), shapes = emptyList())
        val command = AddConstructionCommand(
            points = initial.points,
            shapeType = Shape2DType.Segment,
            existingPointIndices = listOf(0, 1),
        )

        val result = command.apply(initial)
        assertEquals(2, result.points.size)
        assertEquals(listOf(0, 1), result.shapes.single().pointIndices)
        assertEquals(initial, command.undo(result).copy(modifiedAt = initial.modifiedAt))
    }

    @Test
    fun lineIntersectionDetectsParallelAndCrossingLines() {
        val crossing = Geometry2D.lineIntersection(
            Vec2(0.0, 0.0), Vec2(4.0, 4.0),
            Vec2(0.0, 4.0), Vec2(4.0, 0.0),
        )!!
        assertEquals(2.0, crossing.x, epsilon)
        assertEquals(2.0, crossing.y, epsilon)
        assertTrue(Geometry2D.lineIntersection(Vec2(0.0, 0.0), Vec2(1.0, 0.0), Vec2(0.0, 1.0), Vec2(1.0, 1.0)) == null)
    }

    @Test
    fun transformedShapeRemainsDependentOnItsSource() {
        val source = WorkspaceState(
            points = listOf(Vec2(0.0, 0.0), Vec2(2.0, 0.0)),
            shapes = listOf(Shape2D("s", Shape2DType.Segment, listOf(0, 1), "AB")),
        )
        val transformed = TransformShape2DCommand(0, PointDependencyType.Translate, listOf(1.0, 2.0)).apply(source)
        assertEquals(Vec2(1.0, 2.0), transformed.points[2])
        assertEquals(Vec2(3.0, 2.0), transformed.points[3])

        val moved = MovePointCommand(1, Vec2(2.0, 0.0), Vec2(5.0, 1.0)).apply(transformed)
        assertEquals(Vec2(6.0, 3.0), moved.points[3])
    }

    @Test
    fun advancedGraphDefinitionsProduceRealSamples() {
        val graph = GraphAnalysis()
        assertEquals(GraphDefinitionKind.Polar, graph.definitionKind("r = 2*cos(3*t)"))
        assertEquals(GraphDefinitionKind.Parametric, graph.definitionKind("x(t)=3*cos(t); y(t)=2*sin(t)"))
        assertEquals(GraphDefinitionKind.Implicit, graph.definitionKind("x^2+y^2=9"))

        val polar = graph.samplePolar("r=2", steps = 24)
        assertEquals(2.0, polar.points.first().x, epsilon)
        assertEquals(0.0, polar.points.first().y, epsilon)

        val parametric = graph.sampleParametric("x(t)=3*cos(t); y(t)=2*sin(t)", steps = 24)
        assertTrue(parametric.points.any { abs(it.x - 3.0) < 1e-6 && abs(it.y) < 1e-6 })
        assertTrue(graph.implicitSegments("x^2+y^2=9", -4.0, 4.0, -4.0, 4.0, density = 24).isNotEmpty())
    }

    @Test
    fun generalCalculusAnalysisMatchesReferenceValues() {
        val graph = GraphAnalysis()
        assertEquals(12.0, graph.derivative("x^3", 2.0), 1e-3)
        assertEquals(9.0, graph.integral("x^2", 0.0, 3.0), 1e-5)
        assertEquals(listOf(-2.0, 2.0), graph.roots("x^2-4", -4.0, 4.0).map { kotlin.math.round(it * 1000) / 1000 })
        assertTrue(graph.extrema("x^2", -3.0, 3.0).any { abs(it.x) < .02 && abs(it.y) < .01 })
    }

    @Test
    fun statisticsAndProbabilityEnginesMatchReferenceValues() {
        val summary = StatisticsEngine.summarize(listOf(Vec2(0.0, 1.0), Vec2(1.0, 3.0), Vec2(2.0, 5.0)))
        val regression = summary.regression!!
        assertEquals(3, summary.count)
        assertEquals(2.0, regression.slope, epsilon)
        assertEquals(1.0, regression.intercept, epsilon)
        assertEquals(1.0, regression.correlation, epsilon)
        assertEquals(1.0 / sqrt(2.0 * PI), ProbabilityEngine.normalPdf(0.0), epsilon)
        assertEquals(0.1171875, ProbabilityEngine.binomialPmf(3, 10, .5), epsilon)
    }
}
