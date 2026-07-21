package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.FunctionDefinition
import com.indianservers.aiexplorer.phase2.mathstudio.StudioTransform
import com.indianservers.aiexplorer.phase2.mathstudio.UnifiedMathStudioEngine
import com.indianservers.aiexplorer.phase2.mathstudio.UnifiedStudioHistory
import com.indianservers.aiexplorer.workspace.GraphSliderMetadataState
import com.indianservers.aiexplorer.workspace.GeoGebraPackageExchange
import com.indianservers.aiexplorer.workspace.WorkspaceProjectCodec
import com.indianservers.aiexplorer.workspace.WorkspaceState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase2UnifiedMathStudioTest {
    private val engine = UnifiedMathStudioEngine()
    private val base = WorkspaceState(
        functions = listOf(FunctionDefinition("f", "f(x)", "x^2", "cyan")),
        graphSliderMetadata = emptyMap(),
        modifiedAt = 100L,
    )

    @Test fun dependentExpressionRecomputesAcrossAlgebraGraphTableAndGeometry() {
        var session = engine.fromWorkspace(base)
        session = engine.add(session, "g(x) = f(x) + a")
        session = engine.parameter(session, "a", 2.0)
        var projection = engine.projection(session)
        val g = projection.expressions.first { it.name == "g(x)" }
        assertEquals(setOf("f"), g.dependencies)
        assertTrue(g.resolvedSource.contains("x^2"))
        assertEquals(6.0, projection.graph.graphObject("g(x)")!!.table.first { it.input.decimal == 2.0 }.output.decimal, 1e-9)

        session = engine.edit(session, "f", "x^2 - 1")
        projection = engine.projection(session)
        assertEquals(5.0, projection.graph.graphObject("g(x)")!!.table.first { it.input.decimal == 2.0 }.output.decimal, 1e-9)
        assertTrue(projection.keyPoints.getValue("f").any { kotlin.math.abs(it.x - 1.0) < 1e-4 })
    }

    @Test fun cycleIsRejectedWithoutCorruptingLastValidDocument() {
        var session = engine.add(engine.fromWorkspace(base), "g(x) = f(x) + 1")
        val validRevision = session.document.revision
        session = engine.edit(session, "f", "g(x) + 1")
        assertEquals(validRevision, session.document.revision)
        assertTrue(session.message.contains("dependency graph", ignoreCase = true))
        assertTrue(engine.projection(session).diagnostics.isEmpty())
    }

    @Test fun transformsVisibilityColoursParametersAndHistoryRemainCoherent() {
        val initial = engine.fromWorkspace(base)
        val history = UnifiedStudioHistory(initial)
        var session = history.apply(engine.edit(initial, "f", "(x+1)^2"))
        session = history.apply(engine.transform(session, StudioTransform.Expand))
        assertTrue(session.resultPods.first().verified)
        assertTrue(session.resultPods.first().exact.contains("x"))
        session = history.apply(engine.toggleVisible(session, "f"))
        session = history.apply(engine.cycleColor(session, "f"))
        session = history.apply(engine.parameter(session, "a", 3.25))
        val workspace = engine.toWorkspace(session)
        assertFalse(workspace.functions.single().visible)
        assertEquals("violet", workspace.functions.single().colorKey)
        assertEquals(3.25, workspace.graphSliderMetadata.getValue("a").value!!, 0.0)
        assertTrue(history.canUndo)
        assertEquals(null, history.undo().parameterValues["a"])
    }

    @Test fun persistedWorkspaceRestoresStudioParameterValues() {
        val state = base.copy(graphSliderMetadata = mapOf("a" to GraphSliderMetadataState(value = -2.5)))
        val restored = WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(state), recover = false).state!!
        assertEquals(-2.5, engine.fromWorkspace(restored).parameterValues.getValue("a"), 0.0)
    }

    @Test fun realGeoGebraPackageRoundTripImportsSupportedObjects() {
        val packed = GeoGebraPackageExchange.export(base)
        assertTrue(packed.size > 100)
        val imported = GeoGebraPackageExchange.import(packed, WorkspaceState(points = emptyList(), functions = emptyList()))
        assertEquals(1, imported.workspace.functions.size)
        assertEquals("f(x)", imported.workspace.functions.single().name)
        assertEquals("x^2", imported.workspace.functions.single().expression)
        assertTrue(imported.coverage.imported >= 1)
    }
}
