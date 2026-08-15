package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.Vec2
import com.indianservers.aiexplorer.workspace.Unified2DMathController
import com.indianservers.aiexplorer.workspace.Unified2DMutation
import com.indianservers.aiexplorer.workspace.UniversalMathDocumentEngine
import com.indianservers.aiexplorer.workspace.UniversalMathObjectFactory
import com.indianservers.aiexplorer.workspace.UniversalMutationResult
import com.indianservers.aiexplorer.workspace.WorkspaceState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeometryGraphBatchTransactionTest {
    @Test fun multiPointTransformUsesOneRevisionAndOneRecomputeBoundary() {
        val controller = Unified2DMathController()
        val initial = controller.snapshot(WorkspaceState(points = listOf(Vec2(0.0, 0.0), Vec2(1.0, 1.0))))
        val result = controller.transformPoints(initial, mapOf("point-0" to Vec2(4.0, 5.0), "point-1" to Vec2(6.0, 7.0))) as Unified2DMutation.Applied

        assertEquals(initial.document.revision + 1, result.snapshot.document.revision)
        assertEquals(listOf(Vec2(4.0, 5.0), Vec2(6.0, 7.0)), result.snapshot.state.points)
        assertTrue(result.affectedObjects.containsAll(setOf("point-0", "point-1")))
    }

    @Test fun invalidMemberRejectsWholeBatchWithoutChangingDocument() {
        val controller = Unified2DMathController()
        val initial = controller.snapshot(WorkspaceState(points = listOf(Vec2(0.0, 0.0), Vec2(1.0, 1.0))))
        val result = controller.transformPoints(initial, mapOf("point-0" to Vec2(9.0, 9.0), "missing" to Vec2(2.0, 2.0)))

        assertTrue(result is Unified2DMutation.Rejected)
        assertEquals(listOf(Vec2(0.0, 0.0), Vec2(1.0, 1.0)), initial.state.points)
    }

    @Test fun batchEngineRejectsDuplicateAndStaleTransactions() {
        val engine = UniversalMathDocumentEngine()
        val point = UniversalMathObjectFactory.point2D("A", "A", 0.0, 0.0)
        val document = engine.create("batch", listOf(point), now = 1)

        assertTrue(engine.upsertBatch(document, listOf(point, point)) is UniversalMutationResult.Rejected)
        assertTrue(engine.upsertBatch(document, listOf(point), expectedRevision = document.revision + 1) is UniversalMutationResult.Conflict)
    }
}
