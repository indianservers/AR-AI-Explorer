package com.indianservers.aiexplorer.solver

import com.indianservers.aiexplorer.solver.domain.catalogue.SolverCalculatorCatalogue
import com.indianservers.aiexplorer.solver.domain.engine.Phase3SolverEngine
import com.indianservers.aiexplorer.solver.domain.practice.SolverDifficultyModel
import com.indianservers.aiexplorer.solver.domain.practice.SolverPracticeGenerator
import com.indianservers.aiexplorer.solver.domain.tutor.DifficultyLevel
import com.indianservers.aiexplorer.solver.domain.tutor.PracticeMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase4PracticeCatalogueTest {
    private val engine = Phase3SolverEngine()
    private val generator = SolverPracticeGenerator(engine)

    @Test
    fun everyPracticeModeProducesAValidatedProblemForCoreSkills() {
        val sources = listOf("23+17", "3x+5=20", "x^2-5x+6=0")
        sources.forEach { source ->
            val solution = engine.solve(source)
            PracticeMode.entries.forEachIndexed { index, mode ->
                val problem = generator.generate(solution, mode, seed = 1000 + index)
                assertNotNull("$source / $mode", problem)
                assertTrue(problem!!.expectedAnswer.isNotBlank())
                assertTrue(problem.validationMessage.startsWith("Validated offline"))
                assertTrue(problem.difficulty.explanation.contains("large numbers alone"))
            }
        }
    }

    @Test
    fun generatedPracticeAnswersAreRevalidatedByTheCentralEngine() {
        val solution = engine.solve("3x+5=20")
        repeat(100) { seed ->
            val problem = generator.generate(solution, PracticeMode.SimilarDifficulty, seed)!!
            val solved = engine.solve(problem.prompt)
            assertTrue(solved.supported)
            assertTrue(solved.verification.status.name != "Failed")
            assertTrue(problem.expectedAnswer.replace(" ", "") == (solved.exactAnswer ?: solved.finalAnswer).orEmpty().replace(" ", ""))
        }
    }

    @Test
    fun difficultyUsesStructureRatherThanNumberMagnitude() {
        val small = SolverDifficultyModel.assess("999999+888888")
        val structural = SolverDifficultyModel.assess("sqrt((x+1)/(x-2))=3")
        assertTrue(structural.score > small.score)
        assertTrue(small.level in setOf(DifficultyLevel.Foundation, DifficultyLevel.Beginner, DifficultyLevel.Intermediate))
        val proof = SolverDifficultyModel.assess("x^2+y^2=z^2", proofRequired = true)
        assertTrue(proof.level in setOf(DifficultyLevel.OlympiadStyle, DifficultyLevel.ProofOriented))
    }

    @Test
    fun supportedCalculatorPresetsRouteThroughOneCentralEngine() {
        val unsupported = SolverCalculatorCatalogue.presets.filter { it.supported }.filterNot { preset ->
            engine.solve(preset.starterExpression).supported
        }
        assertTrue("Falsely advertised presets: ${unsupported.map { it.title }}", unsupported.isEmpty())
    }

    @Test
    fun catalogueIsGroupedAndSearchableWithoutDuplicateIds() {
        assertTrue(SolverCalculatorCatalogue.presets.map { it.id }.distinct().size == SolverCalculatorCatalogue.presets.size)
        assertTrue(SolverCalculatorCatalogue.presets.groupBy { it.group }.all { it.value.isNotEmpty() })
        assertFalse(SolverCalculatorCatalogue.search("matrix").isEmpty())
        assertTrue(SolverCalculatorCatalogue.search("no-such-calculator").isEmpty())
    }
}

