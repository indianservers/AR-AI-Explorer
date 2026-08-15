package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.workspace.UniversalDependencyIndex
import com.indianservers.aiexplorer.workspace.UniversalMathDefinition
import com.indianservers.aiexplorer.workspace.UniversalMathDocument
import com.indianservers.aiexplorer.workspace.UniversalMathKind
import com.indianservers.aiexplorer.workspace.UniversalMathObject
import com.indianservers.aiexplorer.workspace.UniversalMathPayload
import com.indianservers.aiexplorer.workspace.UniversalMathRuntime
import com.indianservers.aiexplorer.workspace.UniversalMathValueState
import com.indianservers.aiexplorer.workspace.UniversalMathValueStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UniversalMathRuntimeTest {
    private fun value(id: String, dependencies: Set<String> = emptySet(), revision: Long = 0) = UniversalMathObject(
        id, UniversalMathKind.Expression, id, UniversalMathPayload.Properties(mapOf("type" to "test")), dependencies,
        objectRevision = revision, definition = UniversalMathDefinition.Properties("test"),
    )

    @Test fun reverseIndexSelectsOnlyChangedObjectAndDescendants() {
        val document = UniversalMathDocument(objects = listOf(
            value("a"), value("b", setOf("a")), value("c", setOf("b")), value("unrelated"),
        ).associateBy { it.id })

        val affected = UniversalDependencyIndex.build(document).affectedBy(setOf("a"))

        assertEquals(setOf("a", "b", "c"), affected)
        assertFalse("unrelated" in affected)
    }

    @Test fun recomputeIsTopologicalIncrementalAndInstrumented() {
        var clock = 0L
        val visited = mutableListOf<String>()
        val runtime = UniversalMathRuntime(
            evaluator = { objectValue, _ -> visited += objectValue.id; UniversalMathValueState() },
            nanoTime = { ++clock },
        )
        val document = UniversalMathDocument(objects = listOf(value("a"), value("b", setOf("a")), value("c", setOf("b")), value("x")).associateBy { it.id })

        val first = runtime.recompute(document, setOf("a"))
        val second = runtime.recompute(first.document, setOf("a"))

        assertEquals(listOf("a", "b", "c"), first.evaluationOrder)
        assertEquals(listOf("a", "b", "c"), visited)
        assertTrue(first.evaluations.all { it.durationNanos > 0 })
        assertTrue(second.evaluations.all { it.cacheHit })
        assertFalse("x" in first.affectedObjects)
    }

    @Test fun missingAndCyclicObjectsRemainVisibleWithExplicitStates() {
        val document = UniversalMathDocument(objects = listOf(
            value("missing", setOf("absent")),
            value("a", setOf("b")), value("b", setOf("a")),
        ).associateBy { it.id })

        val report = UniversalMathRuntime().recompute(document)

        assertEquals(UniversalMathValueStatus.MissingDependency, report.document.objects.getValue("missing").valueState.status)
        assertEquals(UniversalMathValueStatus.CyclicDependency, report.document.objects.getValue("a").valueState.status)
        assertEquals(UniversalMathValueStatus.CyclicDependency, report.document.objects.getValue("b").valueState.status)
        assertTrue(report.diagnostics.any { it.contains("Missing dependencies") })
        assertTrue(report.diagnostics.any { it.contains("Dependency cycle") })
    }

    @Test fun failedEvaluationBecomesNumericallyUnstableInsteadOfCrashing() {
        val runtime = UniversalMathRuntime(evaluator = { _, _ -> error("singular construction") })

        val report = runtime.recompute(UniversalMathDocument(objects = mapOf("a" to value("a"))))

        assertEquals(UniversalMathValueStatus.NumericallyUnstable, report.document.objects.getValue("a").valueState.status)
        assertEquals("singular construction", report.evaluations.single().diagnostic)
        assertFalse(report.successful)
    }
}
