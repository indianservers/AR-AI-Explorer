package com.indianservers.aiexplorer.core

data class CasManipulationPreview(
    val before: String,
    val after: String,
    val target: CasSubexpressionTarget,
    val action: CasTargetAction,
    val domain: CasDomainBranchReport,
)

data class CasManipulationState(
    val history: List<String>,
    val historyIndex: Int,
    val selectedTarget: CasSubexpressionTarget? = null,
    val preview: CasManipulationPreview? = null,
) {
    constructor(source: String) : this(listOf(source), 0)
    val expression get() = history[historyIndex]
    val canUndo get() = historyIndex > 0
    val canRedo get() = historyIndex < history.lastIndex
}

data class CasParameterHandle(val variable: String, val minimum: Double = -10.0, val maximum: Double = 10.0, val value: Double = 0.0)
data class CasParameterScrubResult(val expression: String, val substituted: String, val result: CasRow)

class CasDirectManipulationEngine(
    private val interactions: CasNotebookInteractionEngine = CasNotebookInteractionEngine(),
    private val cas: SymbolicCasEngine = SymbolicCasEngine(),
) {
    fun select(state: CasManipulationState, target: CasSubexpressionTarget?) = state.copy(selectedTarget = target, preview = null)

    fun preview(state: CasManipulationState, action: CasTargetAction, variable: String = "x", replacement: String = "0"): CasManipulationState {
        val target = state.selectedTarget ?: error("Select a subexpression first.")
        val after = interactions.applyTarget(state.expression, target, action, variable, replacement)
        return state.copy(preview = CasManipulationPreview(state.expression, after, target, action, CasDomainBranchAnalyzer.analyze(after)))
    }

    fun commit(state: CasManipulationState): CasManipulationState {
        val preview = state.preview ?: return state
        if (preview.after == state.expression) return state.copy(preview = null)
        val retained = state.history.take(state.historyIndex + 1)
        return CasManipulationState(retained + preview.after, retained.size, selectedTarget = null)
    }

    fun cancel(state: CasManipulationState) = state.copy(preview = null)
    fun undo(state: CasManipulationState) = if (state.canUndo) state.copy(historyIndex = state.historyIndex - 1, selectedTarget = null, preview = null) else state
    fun redo(state: CasManipulationState) = if (state.canRedo) state.copy(historyIndex = state.historyIndex + 1, selectedTarget = null, preview = null) else state

    fun targets(state: CasManipulationState) = interactions.subexpressions(state.expression)

    fun handles(source: String): List<CasParameterHandle> = Regex("[A-Za-z][A-Za-z0-9_]*").findAll(source).map { it.value }
        .filterNot { it.lowercase() in setOf("sin", "cos", "tan", "sqrt", "ln", "log", "exp", "abs", "pi", "e", "infinity") }
        .distinct().map { CasParameterHandle(it) }.toList()

    fun scrub(source: String, variable: String, value: Double): CasParameterScrubResult {
        val formatted = if (value % 1.0 == 0.0) value.toInt().toString() else "%.3f".format(value).trimEnd('0').trimEnd('.')
        val substituted = cas.substitute(source, mapOf(variable to formatted)).exact
        return CasParameterScrubResult(source, substituted, cas.simplify(substituted))
    }

    fun drop(state: CasManipulationState, transfer: CasExpressionTransfer, replaceSelection: Boolean): CasManipulationState {
        val next = if (replaceSelection && state.selectedTarget != null) {
            val target = state.selectedTarget
            state.expression.replaceRange(target.start, target.endExclusive, "(${transfer.expression})")
        } else "${state.expression} + (${transfer.expression})"
        val retained = state.history.take(state.historyIndex + 1)
        return CasManipulationState(retained + next, retained.size)
    }
}
