package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.FunctionDefinition
import com.indianservers.aiexplorer.workspace.Unified2DMathController
import com.indianservers.aiexplorer.workspace.Unified2DMutation
import com.indianservers.aiexplorer.workspace.UnifiedSpatialMathController
import com.indianservers.aiexplorer.workspace.UnifiedSpatialMutation
import com.indianservers.aiexplorer.workspace.UniversalMathPayload
import com.indianservers.aiexplorer.workspace.UniversalMathValueStatus
import com.indianservers.aiexplorer.workspace.WorkspaceProjectCodec
import com.indianservers.aiexplorer.workspace.WorkspaceState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeometryGraphStagedExpressionTest {
    @Test fun incomplete2dExpressionRemainsVisibleButUnusableUntilCompleted() {
        val controller = Unified2DMathController()
        val initial = controller.snapshot(WorkspaceState(functions = listOf(FunctionDefinition("f", "f(x)", "x^2", "cyan"))))
        val staged = controller.stageExpression(initial, "f", "x+") as Unified2DMutation.Applied
        val payload = staged.snapshot.document.objects.getValue("f").payload as UniversalMathPayload.Symbolic

        assertEquals("x+", payload.source)
        assertNull(payload.ast)
        assertEquals(UniversalMathValueStatus.ParseError, staged.snapshot.document.objects.getValue("f").valueState.status)

        val completed = controller.stageExpression(staged.snapshot, "f", "x+1") as Unified2DMutation.Applied
        assertTrue((completed.snapshot.document.objects.getValue("f").payload as UniversalMathPayload.Symbolic).ast != null)
        assertEquals(UniversalMathValueStatus.Valid, completed.snapshot.document.objects.getValue("f").valueState.status)
    }

    @Test fun incompleteSurfaceDraftSurvivesWorkspacePersistence() {
        val controller = UnifiedSpatialMathController()
        val initial = controller.snapshot(WorkspaceState(surfaceExpression = "x^2+y^2"))
        val staged = controller.stageSurface(initial, "sin(x)+") as UnifiedSpatialMutation.Applied
        val restored = WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(staged.snapshot.state), recover = false).state!!

        assertEquals("sin(x)+", restored.surfaceExpression)
        val objectState = controller.snapshot(restored).document.objects.getValue("surface-main").valueState
        assertEquals(UniversalMathValueStatus.ParseError, objectState.status)
    }
}
