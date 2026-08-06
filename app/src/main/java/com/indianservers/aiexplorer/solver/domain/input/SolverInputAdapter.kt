package com.indianservers.aiexplorer.solver.domain.input

import com.indianservers.aiexplorer.solver.domain.model.SolverInputResult

sealed interface ExternalMathInput {
    val sourceId: String
    val payload: String

    data class RecognizedText(
        override val sourceId: String,
        override val payload: String,
    ) : ExternalMathInput
}

interface SolverInputAdapter {
    fun convert(input: ExternalMathInput): SolverInputResult
}

/**
 * Future integration boundary only. No external input producer is connected to
 * this adapter in Phase 4.
 */
class NormalizedTextSolverInputAdapter : SolverInputAdapter {
    override fun convert(input: ExternalMathInput): SolverInputResult =
        KeyboardSolverInputSource(input.payload).getExpression()
}

