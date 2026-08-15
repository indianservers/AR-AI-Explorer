package com.indianservers.aiexplorer.solver.presentation

/** Release invariants shared by UI, background execution, and regression tests. */
object SolverReleasePolicy {
    const val minimumSupportingTextSp = 11
    const val recommendedTouchTargetDp = 48
    const val solveWarningMillis = 8_000L
    const val maximumCachedSolutions = 24

    fun longRunningMessage(elapsedMillis: Long): String? = when {
        elapsedMillis < solveWarningMillis -> null
        elapsedMillis < solveWarningMillis * 2 -> "This calculation is taking longer than usual. You can cancel without losing your input."
        else -> "The calculation is still running offline. Cancel and simplify the input if you do not want to wait."
    }
}
