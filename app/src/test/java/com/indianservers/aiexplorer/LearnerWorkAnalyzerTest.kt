package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.core.LearnerStepStatus
import com.indianservers.aiexplorer.core.LearnerWorkAnalyzer
import com.indianservers.aiexplorer.core.MistakeKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LearnerWorkAnalyzerTest {
    private val analyzer = LearnerWorkAnalyzer()

    @Test
    fun verifiesEquivalentEquationStepsLineByLine() {
        val report = analyzer.analyze(
            "Solve 2*x + 3 = 11",
            """
            2*x + 3 = 11
            2*x = 8
            x = 4
            """.trimIndent(),
        )

        assertEquals(3, report.steps.size)
        assertTrue(report.allCorrect)
        assertTrue(report.steps.all { it.status == LearnerStepStatus.Correct })
    }

    @Test
    fun identifiesTheFirstIncorrectLineAndPreservesEarlierWork() {
        val report = analyzer.analyze(
            "Solve 2*x + 3 = 11",
            """
            2*x + 3 = 11
            2*x = 14
            x = 7
            """.trimIndent(),
        )

        assertEquals(LearnerStepStatus.Correct, report.steps.first().status)
        assertEquals(2, report.firstIncorrect?.lineNumber)
        assertNotNull(report.firstIncorrect?.evidence)
    }

    @Test
    fun reportsBracketAndDomainMistakesOnTheirExactLines() {
        val bracket = analyzer.analyze("Solve x + 1 = 2", "x + (1 = 2").firstIncorrect
        val domain = analyzer.analyze("Simplify x", "ln(-2)").firstIncorrect

        assertEquals(MistakeKind.Bracket, bracket?.mistakeKind)
        assertEquals(1, bracket?.lineNumber)
        assertEquals(MistakeKind.Domain, domain?.mistakeKind)
        assertEquals(1, domain?.lineNumber)
    }
}
