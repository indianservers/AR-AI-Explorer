package com.indianservers.aiexplorer.solver

import com.indianservers.aiexplorer.solver.domain.engine.Phase3SolverEngine
import com.indianservers.aiexplorer.solver.domain.input.SchoolMathInputRecognizer
import com.indianservers.aiexplorer.solver.domain.input.SchoolRecognitionDeviceQa
import com.indianservers.aiexplorer.solver.domain.input.SchoolRecognitionEvaluationCorpus
import com.indianservers.aiexplorer.solver.domain.model.VerificationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SolverPhase3RecognitionQualityTest {
    @Test
    fun multilingualClassroomPhrasesCanonicalizeWithoutCloudInterpretation() {
        val cases = mapOf(
            "80 ka 25 pratishat" to "25% of 80",
            "80 का 25 प्रतिशत" to "25% of 80",
            "3x plus 5 barabar 20 hal karo" to "3x + 5 = 20",
            "3x जमा 5 बराबर 20 हल करो" to "3x + 5 = 20",
        )
        cases.forEach { (source, canonical) -> assertEquals(source, canonical, SchoolMathInputRecognizer.canonicalize(source)) }
    }

    @Test
    fun ambiguousNotationRequiresChoiceAndIsNeverSilentlyCanonicalized() {
        listOf("sin 30", "log 8", "2/3x").forEach { source ->
            val recognition = SchoolMathInputRecognizer.recognize(source)!!
            assertTrue(recognition.choices.size >= 2)
            assertEquals(null, recognition.canonicalInput)
            assertEquals(source, SchoolMathInputRecognizer.canonicalize(source))
        }
    }

    @Test
    fun expandedAppliedSchemasAreVerifiedByReverseOperations() {
        val engine = Phase3SolverEngine()
        val cases = mapOf(
            "A car travels at 60 km/h for 2 hours. How far does it travel?" to "120 km",
            "A bus covers 150 km at 50 km/h. How long does it take?" to "3 h",
            "5 notebooks cost 200. What is the cost of one notebook?" to "40 per notebook",
            "An item costs 800 and has 15% discount. Find the sale price." to "680",
        )
        cases.forEach { (source, answer) ->
            val solution = engine.solve(source)
            assertTrue("$source: ${solution.message}", solution.supported)
            assertEquals(answer, solution.finalAnswer)
            assertEquals(VerificationStatus.Verified, solution.verification.status)
            assertTrue(solution.verification.checks.all { it.passed })
        }
    }

    @Test
    fun telemetryFreeCorpusMeetsReleaseThresholds() {
        val report = SchoolRecognitionEvaluationCorpus.evaluate()
        assertTrue(report.failures.joinToString("\n"), report.releaseReady)
    }

    @Test
    fun deviceQaMatrixCoversPhoneTabletFoldableAndTv() {
        val matrix = SchoolRecognitionDeviceQa.matrix
        assertTrue(matrix.size >= 5)
        assertTrue(matrix.all { it.minimumTargetDp >= 48 && it.expectedFallback.isNotBlank() })
        assertTrue(matrix.any { it.device.contains("Tablet") })
        assertTrue(matrix.any { it.device.contains("Foldable") })
        assertTrue(matrix.any { it.device.contains("Television") })
    }
}
