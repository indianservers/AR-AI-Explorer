package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.exp
import kotlin.math.ln

class GraphProductionTest {
    @Test fun everyGraphFormUsesOneTypedExpressionModel() {
        val definitions = listOf(
            TypedGraphExpressionParser.parse("y=a*x^2+b"),
            TypedGraphExpressionParser.parse("x^2+y^2=4"),
            TypedGraphExpressionParser.parse("r=2+a*cos(theta)"),
            TypedGraphExpressionParser.parse("x(t)=cos(t); y(t)=sin(t)"),
            TypedGraphExpressionParser.parse("piecewise{x<0:-x; x>=0:x}"),
            TypedGraphExpressionParser.parse("y<=a*x+1"),
        )

        assertEquals(listOf("Explicit", "Implicit", "Polar", "Parametric", "Piecewise", "Inequality"), definitions.map { it::class.simpleName })
        assertEquals(setOf("a", "b"), definitions.first().parameters)
        assertEquals(setOf("a"), definitions[2].parameters)
        val engine = TypedGraphEngine()
        assertTrue(engine.sample(definitions[0], parameterValues = mapOf("a" to 1.0, "b" to 0.0)).curves.first().points.size > 20)
        assertTrue(engine.sample(definitions[1], samples = 160).implicitSegments.isNotEmpty())
        assertTrue(engine.sample(definitions[4]).curves.flatMap { it.points }.isNotEmpty())
        assertTrue(engine.sample(definitions[5], parameterValues = mapOf("a" to 1.0), samples = 100).inequalityCells.isNotEmpty())
    }

    @Test fun canvasHandlesCoverEveryFreeAndLinkedParameter() {
        val reducer = GraphWorkspaceReducer()
        var state = reducer.reduce(GraphWorkspaceState(), GraphWorkspaceAction.AddExpression("a*x^2+b*x+c"))
        state = reducer.reduce(state, GraphWorkspaceAction.SetLinkedVariable("d", "a+b"))
        state = reducer.reduce(state, GraphWorkspaceAction.AddExpression("d*sin(x)"))

        assertEquals(setOf("a", "b", "c", "d"), state.parameters.keys)
        state = reducer.reduce(state, GraphWorkspaceAction.SetParameter("a", 2.0))
        state = reducer.reduce(state, GraphWorkspaceAction.SetParameter("b", 3.0))
        assertEquals(5.0, state.parameters.getValue("d").value, 1e-12)
        val handles = GraphParameterHandleEngine.handles(state)
        assertEquals(state.parameters.size, handles.size)
        assertTrue(handles.first { it.parameter.name == "a" }.affectedRows.isNotEmpty())
        val dragged = GraphParameterHandleEngine.drag(state, "c", .75)
        assertTrue(dragged.parameters.getValue("c").value > 0)
    }

    @Test fun professionalTablesAcceptThousandsOfCsvTsvRowsAndCalculatedColumns() {
        val pasted = buildString {
            appendLine("x\ty")
            repeat(5_000) { appendLine("$it\t${2 * it + 1}") }
        }
        val engine = ProfessionalGraphTableEngine()
        val table = engine.paste(pasted)
        val calculated = engine.calculatedColumn(table, "residual", "y-(2*x+1)")

        assertEquals(5_000, calculated.rowCount)
        assertEquals(3, calculated.columns.size)
        assertTrue(calculated.columns.last().values.all { it == 0.0 })
        assertEquals(Vec2(4_999.0, 9_999.0), engine.series(calculated, "x", "y").last())
        val csv = engine.paste("name,value\n\"1\",2\n3,4")
        assertEquals(2, csv.rowCount)
    }

    @Test fun regressionFamiliesExposeResidualsBandsAndModelGuidance() {
        val engine = ProfessionalRegressionEngine()
        val linearData = (0..20).map { x -> Vec2(x.toDouble(), 1.0 + 2.0 * x + if (x % 2 == 0) .05 else -.05) }
        val linear = engine.fit(linearData, GraphRegressionKind.Linear)
        val polynomial = engine.fit((0..20).map { x -> Vec2(x / 4.0, 1.0 + 2.0 * (x / 4.0) + 3.0 * (x / 4.0) * (x / 4.0)) }, GraphRegressionKind.Polynomial, degree = 2)
        val exponential = engine.fit((0..15).map { x -> Vec2(x / 5.0, 3.0 * exp(.4 * x / 5.0)) }, GraphRegressionKind.Exponential)
        val logarithmic = engine.fit((1..20).map { x -> Vec2(x.toDouble(), 2.0 + 4.0 * ln(x.toDouble())) }, GraphRegressionKind.Logarithmic)
        val logistic = engine.fit((-10..10).map { x -> Vec2(x / 2.0, 10.0 / (1 + exp(-1.2 * (x / 2.0 - .5)))) }, GraphRegressionKind.Logistic)
        val custom = engine.fit(linearData, GraphRegressionKind.Custom, customExpression = "m*x+b", customParameters = listOf("m", "b"))

        listOf(linear, polynomial, exponential, logarithmic, logistic, custom).forEach {
            assertEquals(it.fitted.size, it.residuals.size)
            assertEquals(it.fitted.size, it.confidenceBand.size)
            assertTrue(it.rmse.isFinite() && it.aic.isFinite() && it.bic.isFinite())
        }
        assertTrue(linear.rSquared > .999)
        assertTrue(polynomial.rSquared > .999999)
        assertTrue(exponential.rSquared > .999999)
        assertTrue(logarithmic.rSquared > .999999)
        assertTrue(logistic.rSquared > .95)
        val comparison = engine.compare(listOf(linear, custom))
        assertEquals(2, comparison.ranked.size)
        assertTrue(comparison.guidance.any { it.contains("causation", true) })
    }

    @Test fun pointsOfInterestAreDeterministicSortedAndKeyboardReachable() {
        val reducer = GraphWorkspaceReducer()
        var state = reducer.reduce(GraphWorkspaceState(), GraphWorkspaceAction.AddExpression("x^2-1"))
        state = reducer.reduce(state, GraphWorkspaceAction.AddExpression("x"))
        val engine = DeterministicGraphInterestEngine()
        val first = engine.points(state.rows, GraphDomain(-3.0, 3.0), state.parameters.mapValues { it.value.value })
        val second = engine.points(state.rows, GraphDomain(-3.0, 3.0), state.parameters.mapValues { it.value.value })

        assertEquals(first, second)
        assertEquals(first.sortedBy { it.point.x }, first)
        assertTrue(first.any { it.kind == GraphPointKind.Root })
        assertTrue(first.any { it.kind == GraphPointKind.Intersection })
        val navigation = GraphKeyboardNavigator(first)
        assertNotNull(navigation.current())
        assertNotNull(navigation.next(GraphPointKind.Root))
        assertTrue(navigation.shortcuts().containsKey("ArrowRight"))
    }

    @Test fun transformationsFoldersNotesAndAudioSummariesReuseGraphState() {
        val reducer = GraphWorkspaceReducer()
        var state = reducer.reduce(GraphWorkspaceState(), GraphWorkspaceAction.AddFolder("Quadratics"))
        state = reducer.reduce(state, GraphWorkspaceAction.AddExpression("x^2", folderId = state.folders.first().id))
        val row = state.rows.single()
        state = reducer.reduce(state, GraphWorkspaceAction.SetNote(row.id, "Vertex form exploration"))
        assertEquals("Vertex form exploration", state.rows.single().note)
        val transform = CompositeGraphTransformation("shift and reflect", listOf(GraphTransformation(GraphTransformKind.TranslateX, 2.0), GraphTransformation(GraphTransformKind.ReflectX, 1.0)))
        val transformed = ReusableGraphTransformationEngine.apply(state.rows, setOf(row.id), transform).single()
        assertTrue(transformed.source.contains("x-2"))

        val sample = TypedGraphEngine().sample(transformed.typed!!)
        val points = DeterministicGraphInterestEngine().points(listOf(transformed), GraphDomain(-5.0, 5.0), emptyMap())
        val accessible = AccessibleGraphDescriptionEngine.build(transformed, sample, points)
        assertTrue(accessible.notes.size > 20)
        assertTrue(accessible.notes.all { it.pitch.isFinite() && it.pan in -1.0..1.0 })
        assertTrue(accessible.summary.navigationHints.any { it.contains("Audio pitch") })
    }
}
