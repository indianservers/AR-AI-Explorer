package com.indianservers.aiexplorer.solver

import com.indianservers.aiexplorer.solver.domain.engine.Phase3SolverEngine
import com.indianservers.aiexplorer.solver.domain.input.RecognitionReadiness
import com.indianservers.aiexplorer.solver.domain.input.SchoolMathInputRecognizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SchoolMathInputRecognizerTest {
    @Test
    fun commonClassroomWordingBecomesReviewableCanonicalMath() {
        val cases = mapOf(
            "What is 25 percent of 80?" to "25% of 80",
            "Solve for x: 3x plus 5 equals 20" to "3x + 5 = 20",
            "Find the derivative of x^3" to "differentiate x^3",
            "Find the integral of x^2" to "integrate x^2",
            "Calculate the mean of 2, 4, 6, 8" to "mean(2, 4, 6, 8)",
        )
        cases.forEach { (source, expected) ->
            val result = SchoolMathInputRecognizer.recognize(source)!!
            assertEquals(source, RecognitionReadiness.Ready, result.readiness)
            assertEquals(source, expected, result.canonicalInput)
            assertTrue(result.confidencePercent >= 90)
        }
    }

    @Test
    fun typographicOperatorsNormalizeWithoutChangingMeaning() {
        assertEquals("2*x - 3 <= 7", SchoolMathInputRecognizer.canonicalize("2×x − 3 ≤ 7"))
    }

    @Test
    fun incompleteInputGetsSpecificGuidanceBeforeSolve() {
        listOf("differentiate", "2x +", "(2 + 3").forEach { source ->
            val result = SchoolMathInputRecognizer.recognize(source)!!
            assertEquals(source, RecognitionReadiness.NeedsDetail, result.readiness)
            assertTrue(result.guidance.isNotBlank())
        }
    }

    @Test
    fun previewClassifiesMajorSchoolTopicsAndSurfacesAssumptions() {
        assertEquals("Quadratic equation", SchoolMathInputRecognizer.recognize("2x^2 - 7x + 3 = 0")!!.topic)
        assertEquals("Trigonometry", SchoolMathInputRecognizer.recognize("sin(x) = 1/2")!!.topic)
        assertEquals("Matrices", SchoolMathInputRecognizer.recognize("det([[1,2],[3,4]])")!!.topic)
        assertTrue(SchoolMathInputRecognizer.recognize("2x + 3 = 9")!!.assumptions.isNotEmpty())
    }

    @Test
    fun canonicalFormsSolveThroughExistingVerifiedEngine() {
        val engine = Phase3SolverEngine()
        listOf(
            "What is 25 percent of 80?" to "20",
            "Solve for x: 3x plus 5 equals 20" to "x = 5",
            "Calculate the mean of 2, 4, 6, 8" to "Mean = 5",
        ).forEach { (natural, answer) ->
            val solved = engine.solve(SchoolMathInputRecognizer.canonicalize(natural))
            assertTrue("$natural: ${solved.message}", solved.supported)
            assertEquals(answer, solved.finalAnswer)
            assertFalse(solved.verification.checks.any { !it.passed })
        }
    }
}
