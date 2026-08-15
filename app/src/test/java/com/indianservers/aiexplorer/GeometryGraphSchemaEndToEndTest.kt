package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.FunctionDefinition
import com.indianservers.aiexplorer.workspace.MathObjectGraph
import com.indianservers.aiexplorer.workspace.SpatialEquationForm
import com.indianservers.aiexplorer.workspace.UnifiedSpatialMathController
import com.indianservers.aiexplorer.workspace.WorkspaceProjectCodec
import com.indianservers.aiexplorer.workspace.WorkspaceState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeometryGraphSchemaEndToEndTest {
    @Test fun persistedWorkspaceRebuildsShared2dAnd3dAnalysisWithoutDivergence() {
        val original = WorkspaceState(
            functions = listOf(FunctionDefinition("f", "f(x)", "1/(x-2)", "cyan")),
            surfaceExpression = "x^2+y^2",
        )
        val restored = WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(original), recover = false).state!!

        val graph = MathObjectGraph().snapshot(restored).graphObject("f(x)")!!.advancedFeatures!!
        val spatial = UnifiedSpatialMathController().snapshot(restored)

        assertTrue(graph.asymptotes.any { it.kind == "vertical" && it.equation == "x = 2" })
        assertEquals(SpatialEquationForm.ExplicitSurface, spatial.spatialSchema.equations.single().form)
        assertEquals("z=x^2+y^2", spatial.spatialSchema.equations.single().equations.single())
    }

    @Test fun spatialSelectionHandsTheSamePersistedDefinitionToSolver() {
        val controller = UnifiedSpatialMathController()
        val snapshot = controller.snapshot(WorkspaceState(surfaceExpression = "sin(x)+cos(y)"))
        val handoff = controller.solverHandoff(snapshot, setOf("surface-main"), "find tangent plane").getOrThrow()

        assertTrue(handoff.query.contains("sin(x)+cos(y)"))
        assertTrue(handoff.sourceObjectIds.contains("surface-main"))
        assertTrue(handoff.provenance.contains(snapshot.document.id))
    }
}
