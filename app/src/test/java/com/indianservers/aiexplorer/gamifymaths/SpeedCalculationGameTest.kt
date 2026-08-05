package com.indianservers.aiexplorer.gamifymaths

import com.indianservers.aiexplorer.displayLatexFormula
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SpeedCalculationGameTest {
    @Test
    fun aSprintDoesNotRepeatQuestions() {
        val prompts = linkedSetOf<String>()
        val random = Random(42)
        val generated = List(80) {
            nextUniqueSpeedProblem(
                mode = SpeedCalculationMode.Basic,
                digits = 1,
                selectedBasic = setOf("Addition"),
                selectedAdvanced = emptySet(),
                usedPrompts = prompts,
                random = random,
            )
        }

        assertEquals(generated.size, generated.map { it.prompt }.distinct().size)
        assertEquals(generated.size, prompts.size)
    }

    @Test
    fun advancedQuestionsAreUniqueAndUseLatexNotation() {
        val prompts = linkedSetOf<String>()
        val random = Random(7)
        val generated = List(80) {
            nextUniqueSpeedProblem(
                mode = SpeedCalculationMode.Advanced,
                digits = 1,
                selectedBasic = emptySet(),
                selectedAdvanced = setOf("Trigonometry", "Advanced Calculation"),
                usedPrompts = prompts,
                random = random,
            )
        }

        assertEquals(generated.size, generated.map { it.prompt }.distinct().size)
        assertTrue(generated.any { "\\times" in it.prompt || "^" in it.prompt || "\\sqrt" in it.prompt })
        generated.forEach {
            val rendered = displayLatexFormula(it.prompt)
            assertFalse(rendered.contains("\\times"))
            assertFalse(rendered.contains("\\circ"))
        }
    }
}
