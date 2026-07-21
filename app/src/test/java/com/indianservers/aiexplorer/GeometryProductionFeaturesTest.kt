package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.DynamicGeometryDocument
import com.indianservers.aiexplorer.core.DynamicGeometryEngine
import com.indianservers.aiexplorer.core.DynamicGeometryObject
import com.indianservers.aiexplorer.core.DynamicPoint
import com.indianservers.aiexplorer.core.DynamicPointRule
import com.indianservers.aiexplorer.core.GeometryConstraint
import com.indianservers.aiexplorer.core.GeometryTransformation
import com.indianservers.aiexplorer.core.Vec2
import com.indianservers.aiexplorer.workspace.GeoGebraExchange
import com.indianservers.aiexplorer.workspace.Shape2D
import com.indianservers.aiexplorer.workspace.Shape2DType
import com.indianservers.aiexplorer.workspace.WorkspaceState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeometryProductionFeaturesTest {
    private val engine = DynamicGeometryEngine()

    private fun base(): DynamicGeometryDocument {
        var document = DynamicGeometryDocument()
        listOf("A" to Vec2(0.0, 0.0), "B" to Vec2(4.0, 0.0), "C" to Vec2(0.0, 3.0), "D" to Vec2(2.0, 2.0)).forEach { (id, point) ->
            document = engine.addPoint(document, DynamicPoint(id, DynamicPointRule.Free(point)), "fixture")
        }
        document = engine.addObject(document, DynamicGeometryObject.Line("x-axis", "A", "B"), "line")
        document = engine.addObject(document, DynamicGeometryObject.Circle("circle", "A", "C"), "circle")
        return document
    }

    @Test fun pointOnObjectIntersectionConicsAndCompositeTransformsStayDependent() {
        var document = base()
        document = engine.addPoint(document, DynamicPoint("P", DynamicPointRule.PointOnObject("circle", .25)), "on circle")
        document = engine.addPoint(document, DynamicPoint("I", DynamicPointRule.ObjectIntersection("x-axis", "circle", 1)), "intersection")
        document = engine.addObject(document, DynamicGeometryObject.Parabola("parabola", "C", "A", "B"), "focus directrix")
        document = engine.addPoint(document, DynamicPoint("Q", DynamicPointRule.PointOnObject("parabola", 2.0)), "on conic")
        val transform = GeometryTransformation.Composite(listOf(GeometryTransformation.Translation(1.0, 2.0), GeometryTransformation.Rotation("A", 90.0)))
        document = engine.addPoint(document, DynamicPoint("T", DynamicPointRule.Transform("B", transform)), "composite")
        val p = engine.resolve(document)
        assertEquals(0.0, p.getValue("P").x, 1e-8); assertEquals(3.0, p.getValue("P").y, 1e-8)
        assertEquals(3.0, kotlin.math.abs(p.getValue("I").x), 1e-8)
        assertEquals(-2.0, p.getValue("T").x, 1e-8); assertEquals(5.0, p.getValue("T").y, 1e-8)
        assertTrue(p.getValue("Q").x.isFinite())
    }

    @Test fun constraintsLocusReplayGroupsClipboardAndCascadeDeletionWorkTogether() {
        var document = base()
        document = engine.addConstraint(document, GeometryConstraint.FixedLength("length", "A", "D", 5.0), "fixed")
        assertTrue(engine.evaluateConstraints(document).single().satisfied)
        document = engine.addPoint(document, DynamicPoint("M", DynamicPointRule.Midpoint("A", "D")), "midpoint")
        document = engine.generateLocus(document, "locus", "D", "M", listOf(Vec2(2.0, 0.0), Vec2(4.0, 0.0), Vec2(6.0, 0.0)))
        assertEquals(3, (document.objects.last() as DynamicGeometryObject.Locus).samples.size)
        val inspection = engine.inspectDependency(document, "M")
        assertTrue("A" in inspection.transitive && "D" in inspection.transitive)
        assertTrue(engine.replayFrame(document, 2).blockedSteps.isNotEmpty())
        document = engine.select(document, setOf("x-axis"))
        document = engine.groupSelection(document, "g", "Axis")
        val clipboard = engine.copySelection(document)
        document = engine.pasteClipboard(document, clipboard)
        assertTrue(document.selection.any { it.startsWith("copy-") })
        document = engine.deleteSelection(document)
        assertTrue(document.points.none { it.id.startsWith("copy-") })
    }

    @Test fun geoGebraRoundTripIncludesLinesCirclesPolygonsAndEllipse() {
        val state = WorkspaceState(
            points = listOf(Vec2(0.0, 0.0), Vec2(2.0, 0.0), Vec2(0.0, 2.0), Vec2(-2.0, 0.0)),
            shapes = listOf(
                Shape2D("line", Shape2DType.Line, listOf(0, 1), "l"),
                Shape2D("circle", Shape2DType.Circle, listOf(0, 1), "c"),
                Shape2D("poly", Shape2DType.Polygon, listOf(0, 1, 2), "poly"),
                Shape2D("ellipse", Shape2DType.Ellipse, listOf(1, 3, 2), "e"),
            ),
            functions = emptyList(), solids = emptyList(), vectors3D = emptyList(),
        )
        val imported = GeoGebraExchange.importXml(GeoGebraExchange.exportXml(state).xml)
        assertEquals(4, imported.workspace.points.size)
        assertEquals(setOf(Shape2DType.Line, Shape2DType.Circle, Shape2DType.Polygon, Shape2DType.Ellipse), imported.workspace.shapes.map { it.type }.toSet())
        assertTrue(imported.coverage.imported >= 8)
    }
}
