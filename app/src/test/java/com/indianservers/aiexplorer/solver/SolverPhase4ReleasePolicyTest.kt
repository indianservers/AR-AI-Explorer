package com.indianservers.aiexplorer.solver

import com.indianservers.aiexplorer.solver.presentation.SolverReleasePolicy
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SolverPhase4ReleasePolicyTest {
    @Test
    fun longRunningCalculationMessagesAreProgressiveAndActionable() {
        assertNull(SolverReleasePolicy.longRunningMessage(SolverReleasePolicy.solveWarningMillis - 1))
        val warning = SolverReleasePolicy.longRunningMessage(SolverReleasePolicy.solveWarningMillis)!!
        assertTrue(warning.contains("cancel", ignoreCase = true))
        assertTrue(warning.contains("input", ignoreCase = true))
        val extended = SolverReleasePolicy.longRunningMessage(SolverReleasePolicy.solveWarningMillis * 2)!!
        assertTrue(extended.contains("offline", ignoreCase = true))
    }

    @Test
    fun releaseDimensionsAndCacheRemainBounded() {
        assertTrue(SolverReleasePolicy.minimumSupportingTextSp >= 11)
        assertTrue(SolverReleasePolicy.recommendedTouchTargetDp >= 48)
        assertTrue(SolverReleasePolicy.maximumCachedSolutions in 8..64)
    }

    @Test
    fun solverPresentationContainsNoSubElevenSpSupportingText() {
        val root = File("src/main/java/com/indianservers/aiexplorer/solver/presentation")
        val offenders = root.walkTopDown().filter { it.extension == "kt" }.flatMap { file ->
            file.readLines().withIndex().asSequence().filter { (_, line) ->
                Regex("fontSize\\s*=\\s*(?:[1-9]|10)\\.sp").containsMatchIn(line)
            }.map { (index, line) -> "${file.name}:${index + 1}: ${line.trim()}" }
        }.toList()
        assertEquals(offenders.joinToString("\n"), emptyList<String>(), offenders)
    }
}
