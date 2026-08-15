package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.FunctionDefinition
import com.indianservers.aiexplorer.core.Vec2
import com.indianservers.aiexplorer.workspace.CommandHistory
import com.indianservers.aiexplorer.workspace.ReplaceWorkspaceCommand
import com.indianservers.aiexplorer.workspace.Unified2DMathController
import com.indianservers.aiexplorer.workspace.Unified2DMutation
import com.indianservers.aiexplorer.workspace.UniversalMathDefinition
import com.indianservers.aiexplorer.workspace.UniversalMathDocument
import com.indianservers.aiexplorer.workspace.UniversalMathDocumentCodec
import com.indianservers.aiexplorer.workspace.UniversalMathKind
import com.indianservers.aiexplorer.workspace.UniversalMathObject
import com.indianservers.aiexplorer.workspace.UniversalMathPayload
import com.indianservers.aiexplorer.workspace.UniversalMathRuntime
import com.indianservers.aiexplorer.workspace.WorkspaceState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.system.measureNanoTime

class GeometryGraphPhase1HardeningTest {
    @Test fun crossViewEditUndoRedoRestoresDocumentAndLegacyProjectionAtomically() {
        val controller = Unified2DMathController()
        val history = CommandHistory()
        val before = WorkspaceState(
            points = listOf(Vec2(1.0, 2.0)),
            functions = listOf(FunctionDefinition("f", "f(x)", "x^2", "cyan")),
        )
        val moved = controller.editCoordinates(controller.snapshot(before), "point-0", Vec2(4.0, 5.0)) as Unified2DMutation.Applied
        val after = history.execute(before, ReplaceWorkspaceCommand(before, moved.snapshot.state, "Move linked point"))

        assertEquals(Vec2(4.0, 5.0), after.points.single())
        assertEquals(listOf(4.0, 5.0), (after.universalMathDocument!!.objects.getValue("point-0").payload as UniversalMathPayload.Coordinates).values)

        val undone = history.undo(after)
        assertEquals(Vec2(1.0, 2.0), undone.points.single())
        val redone = history.redo(undone)
        assertEquals(Vec2(4.0, 5.0), redone.points.single())
        assertEquals(moved.snapshot.document.objects.getValue("point-0").definition, redone.universalMathDocument!!.objects.getValue("point-0").definition)
    }

    @Test fun schemaThreePersistenceIsDeterministicAndRecoversRecordDamage() {
        val source = Unified2DMathController().snapshot(WorkspaceState(points = listOf(Vec2(2.0, 3.0)))).document
        val first = UniversalMathDocumentCodec.encode(source)
        val decoded = UniversalMathDocumentCodec.decode(first).document
        val second = UniversalMathDocumentCodec.encode(decoded!!)

        assertEquals(first, second)
        val damaged = first.replaceFirst(Regex("\\\"checksum\\\":\\\"."), "\"checksum\":\"0")
        val recovered = UniversalMathDocumentCodec.decode(damaged)
        assertTrue(recovered.recovered)
        assertFalse(recovered.checksumValid)
        assertNotNull(recovered.document)
        assertEquals(source.objects.keys, recovered.document!!.objects.keys)
    }

    @Test fun fiveHundredObjectInteractiveChainMeetsCommonUpdateGate() {
        val document = chainDocument(500)
        val runtime = UniversalMathRuntime()
        runtime.recompute(document)
        lateinit var report: com.indianservers.aiexplorer.workspace.UniversalRecomputeReport
        val duration = measureNanoTime { report = runtime.recompute(document, setOf("n-490")) }

        assertEquals(10, report.affectedObjects.size)
        assertTrue("Incremental 10-object tail took ${duration / 1_000_000.0} ms", duration < 100_000_000L)
        assertTrue(report.evaluations.all { it.cacheHit })
    }

    @Test fun fiveThousandObjectStressDocumentRecomputesWithoutCorruption() {
        val document = chainDocument(5_000)
        val runtime = UniversalMathRuntime()
        lateinit var report: com.indianservers.aiexplorer.workspace.UniversalRecomputeReport
        val duration = measureNanoTime { report = runtime.recompute(document) }

        assertEquals(5_000, report.evaluationOrder.size)
        assertEquals(5_000, report.document.objects.size)
        assertTrue(report.successful)
        assertTrue("5,000-object recompute took ${duration / 1_000_000.0} ms", duration < 8_000_000_000L)
    }

    private fun chainDocument(size: Int): UniversalMathDocument {
        val objects = (0 until size).associate { index ->
            val id = "n-$index"
            id to UniversalMathObject(
                id = id,
                kind = UniversalMathKind.Measurement,
                name = id,
                payload = UniversalMathPayload.Properties(mapOf("type" to "benchmark", "value" to index.toString())),
                dependencies = if (index == 0) emptySet() else setOf("n-${index - 1}"),
                definition = UniversalMathDefinition.Properties("benchmark"),
            )
        }
        return UniversalMathDocument(id = "geometry-graph-benchmark-$size", objects = objects)
    }
}
