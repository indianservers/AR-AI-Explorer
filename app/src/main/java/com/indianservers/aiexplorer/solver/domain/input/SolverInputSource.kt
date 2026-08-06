package com.indianservers.aiexplorer.solver.domain.input

import com.indianservers.aiexplorer.solver.domain.model.SolverInput
import com.indianservers.aiexplorer.solver.domain.model.SolverInputResult
import com.indianservers.aiexplorer.solver.domain.parser.SolverInputNormalizer

fun interface SolverInputSource {
    fun getExpression(): SolverInputResult
}

class KeyboardSolverInputSource(private val text: String) : SolverInputSource {
    override fun getExpression(): SolverInputResult {
        val normalized = SolverInputNormalizer.normalize(text)
        return if (normalized.isBlank()) SolverInputResult.Error("Enter a mathematical problem using the keyboard.")
        else SolverInputResult.Success(SolverInput(text, normalized))
    }
}

