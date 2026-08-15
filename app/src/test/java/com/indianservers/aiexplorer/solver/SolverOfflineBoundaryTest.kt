package com.indianservers.aiexplorer.solver

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SolverOfflineBoundaryTest {
    @Test
    fun solverProductionSourcesContainNoNetworkIntegration() {
        val root = File("src/main/java/com/indianservers/aiexplorer/solver")
        assertTrue("Solver source package must exist", root.isDirectory)
        val forbidden = listOf(
            "java.net.",
            "okhttp",
            "retrofit",
            "firebase",
            "openai",
            "gemini",
            "wolfram",
        )
        root.walkTopDown().filter(File::isFile).forEach { file ->
            val source = file.readText().lowercase()
            forbidden.forEach { token ->
                assertFalse("${file.name} crosses the offline keyboard-only boundary with '$token'", token in source)
            }
        }
    }

    @Test
    fun keyboardRemainsTheOnlyDirectSolverInputSource() {
        val root = File("src/main/java/com/indianservers/aiexplorer/solver")
        val sourceNames = root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { file -> Regex("""class\s+(\w+SolverInputSource)""").findAll(file.readText()).map { it.groupValues[1] } }
            .toList()
        assertEquals(listOf("KeyboardSolverInputSource"), sourceNames)
    }

    private fun assertEquals(expected: Any, actual: Any) {
        org.junit.Assert.assertEquals(expected, actual)
    }
}
