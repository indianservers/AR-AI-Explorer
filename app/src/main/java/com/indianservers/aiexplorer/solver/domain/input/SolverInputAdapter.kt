package com.indianservers.aiexplorer.solver.domain.input

import com.indianservers.aiexplorer.solver.domain.model.SolverInputResult

sealed interface ExternalMathInput {
    val sourceId: String
    val payload: String

    data class RecognizedText(
        override val sourceId: String,
        override val payload: String,
    ) : ExternalMathInput

    data class ConfirmedCapture(
        override val sourceId: String,
        override val payload: String,
        val kind: CaptureKind,
        val learnerConfirmed: Boolean,
        val confidence: Double? = null,
        val lineCount: Int = 1,
    ) : ExternalMathInput
}

enum class CaptureKind { CameraOcr, SelectedImageOcr, Handwriting }

interface SolverInputAdapter {
    fun convert(input: ExternalMathInput): SolverInputResult
}

/**
 * Future integration boundary only. No external input producer is connected to
 * this adapter in Phase 4.
 */
class NormalizedTextSolverInputAdapter : SolverInputAdapter {
    override fun convert(input: ExternalMathInput): SolverInputResult {
        if (input is ExternalMathInput.ConfirmedCapture && !input.learnerConfirmed) {
            return SolverInputResult.Error("Review and confirm the recognized mathematics before inserting it into Solver.")
        }
        return KeyboardSolverInputSource(input.payload).getExpression()
    }
}
