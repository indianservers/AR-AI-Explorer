package com.indianservers.aiexplorer.solver

import com.indianservers.aiexplorer.solver.domain.input.ExternalMathInput
import com.indianservers.aiexplorer.solver.domain.input.NormalizedTextSolverInputAdapter
import com.indianservers.aiexplorer.solver.domain.model.SolverInputResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class Phase4OfflineAdapterTest {
    @Test
    fun futureAdapterNormalizesTextWithoutConnectingAnyProducer() {
        val result = NormalizedTextSolverInputAdapter().convert(ExternalMathInput.RecognizedText("future-test", " 3 × x + 5 = 20 "))
        assertTrue(result is SolverInputResult.Success)
        result as SolverInputResult.Success
        assertEquals("keyboard", result.input.sourceName)
        assertTrue(result.input.normalized.contains("*"))
    }

    @Test
    fun solverProductionSourceHasNoRemoteClientOrRuntimeDownload() {
        val root = File("src/main/java/com/indianservers/aiexplorer/solver")
        val forbidden = listOf("java.net", "okhttp", "retrofit", "wolfram", "openai", "gemini", "runtime download")
        val violations = root.walkTopDown().filter { it.extension == "kt" }.flatMap { file ->
            val text = file.readText().lowercase()
            forbidden.filter(text::contains).map { "${file.name}:$it" }
        }.toList()
        assertTrue("Offline violations: $violations", violations.isEmpty())
    }

    @Test
    fun externalAdapterIsNotReferencedByApplicationNavigation() {
        val main = File("src/main/java/com/indianservers/aiexplorer/MainActivity.kt").readText()
        assertTrue(!main.contains("NormalizedTextSolverInputAdapter"))
        assertTrue(!main.contains("ExternalMathInput"))
    }
}

