package com.indianservers.aiexplorer.solver

import com.indianservers.aiexplorer.input.MathOcrNormalizer
import com.indianservers.aiexplorer.solver.domain.input.CaptureKind
import com.indianservers.aiexplorer.solver.domain.input.ExternalMathInput
import com.indianservers.aiexplorer.solver.domain.input.NormalizedTextSolverInputAdapter
import com.indianservers.aiexplorer.solver.domain.model.SolverInputResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SolverPhase2CaptureConvenienceTest {
    @Test
    fun ocrCleanupPreservesLinesAndRepairsOnlySafeTypography() {
        val normalized = MathOcrNormalizer.normalize("2x² − 7x + 3 = 0\nx ≥ −2")
        assertEquals("2x ^ 2 - 7x + 3 = 0\nx >= - 2", normalized.editableText)
        assertTrue(normalized.changes.any { it.contains("squared", true) })
        assertTrue(normalized.editableText.lines().size == 2)
    }

    @Test
    fun unconfirmedCameraRecognitionCannotCrossSolverBoundary() {
        val result = NormalizedTextSolverInputAdapter().convert(
            ExternalMathInput.ConfirmedCapture("camera", "2x + 3 = 9", CaptureKind.CameraOcr, learnerConfirmed = false, confidence = .94),
        )
        assertTrue(result is SolverInputResult.Error)
        assertTrue((result as SolverInputResult.Error).message.contains("confirm", true))
    }

    @Test
    fun confirmedCameraAndHandwritingTextRemainSourceLabelled() {
        listOf(CaptureKind.CameraOcr, CaptureKind.SelectedImageOcr, CaptureKind.Handwriting).forEach { kind ->
            val result = NormalizedTextSolverInputAdapter().convert(
                ExternalMathInput.ConfirmedCapture(kind.name, "3x + 5 = 20", kind, learnerConfirmed = true, confidence = .82, lineCount = 1),
            ) as SolverInputResult.Success
            assertEquals("3x + 5 = 20", result.input.normalized)
        }
    }
}
