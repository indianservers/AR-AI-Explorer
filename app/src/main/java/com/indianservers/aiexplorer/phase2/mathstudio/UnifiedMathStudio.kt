package com.indianservers.aiexplorer.phase2.mathstudio

import com.indianservers.aiexplorer.core.CasRow
import com.indianservers.aiexplorer.core.FunctionDefinition
import com.indianservers.aiexplorer.core.MathSolverTutor
import com.indianservers.aiexplorer.core.SymbolicCalculusEngine
import com.indianservers.aiexplorer.core.SymbolicCasEngine
import com.indianservers.aiexplorer.core.Vec2
import com.indianservers.aiexplorer.workspace.MathGraphObject
import com.indianservers.aiexplorer.workspace.GraphSliderMetadataState
import com.indianservers.aiexplorer.workspace.MathObjectGraph
import com.indianservers.aiexplorer.workspace.MathObjectGraphSnapshot
import com.indianservers.aiexplorer.workspace.UniversalMathDocument
import com.indianservers.aiexplorer.workspace.UniversalMathDocumentEngine
import com.indianservers.aiexplorer.workspace.UniversalMathKind
import com.indianservers.aiexplorer.workspace.UniversalMathObject
import com.indianservers.aiexplorer.workspace.UniversalMathObjectFactory
import com.indianservers.aiexplorer.workspace.UniversalMathPayload
import com.indianservers.aiexplorer.workspace.UniversalMutationResult
import com.indianservers.aiexplorer.workspace.UniversalWorkspaceBridge
import com.indianservers.aiexplorer.workspace.WorkspaceState

enum class StudioTransform(val label: String) {
    Simplify("Simplify"), Expand("Expand"), Factor("Factor"), PartialFractions("Partial fractions"),
    Derivative("Derivative"), Integral("Integral"), Limit("Limit x→0")
}

data class StudioResultPod(
    val title: String,
    val exact: String,
    val explanation: String,
    val steps: List<String>,
    val verified: Boolean,
)

data class StudioExpression(
    val id: String,
    val name: String,
    val source: String,
    val resolvedSource: String,
    val colorKey: String,
    val visible: Boolean,
    val dependencies: Set<String>,
    val error: String? = null,
)

data class StudioProjection(
    val expressions: List<StudioExpression>,
    val graph: MathObjectGraphSnapshot,
    val keyPoints: Map<String, List<Vec2>>,
    val diagnostics: List<String>,
)

data class UnifiedStudioSession(
    val document: UniversalMathDocument,
    val baseWorkspace: WorkspaceState,
    val selectedId: String? = null,
    val parameterValues: Map<String, Double> = emptyMap(),
    val hiddenIds: Set<String> = emptySet(),
    val colorKeys: Map<String, String> = emptyMap(),
    val resultPods: List<StudioResultPod> = emptyList(),
    val message: String = "Every view is linked to one maths document.",
)

class UnifiedMathStudioEngine(
    private val documents: UniversalMathDocumentEngine = UniversalMathDocumentEngine(),
    private val objectGraph: MathObjectGraph = MathObjectGraph(),
    private val cas: SymbolicCasEngine = SymbolicCasEngine(),
    private val calculus: SymbolicCalculusEngine = SymbolicCalculusEngine(),
    private val solver: MathSolverTutor = MathSolverTutor(),
) {
    fun fromWorkspace(workspace: WorkspaceState): UnifiedStudioSession {
        val document = UniversalWorkspaceBridge.fromWorkspace(workspace)
        return UnifiedStudioSession(document, workspace, document.objects.values.firstOrNull { it.kind == UniversalMathKind.Function }?.id,
            workspace.graphSliderMetadata.mapNotNull { (name, metadata) -> metadata.value?.let { name to it } }.toMap(),
            workspace.functions.filterNot { it.visible }.mapTo(linkedSetOf()) { it.id }, workspace.functions.associate { it.id to it.colorKey })
    }

    fun add(session: UnifiedStudioSession, input: String): UnifiedStudioSession {
        val parsed = parseDefinition(input, session.document.objects.values.count { it.kind == UniversalMathKind.Function } + 1)
            ?: return session.copy(message = "Enter y = expression or f(x) = expression.")
        val id = uniqueId(parsed.first, session.document)
        val value = UniversalMathObjectFactory.symbolic(id, UniversalMathKind.Function, parsed.first, parsed.second, sourceView = "unified-studio")
        return when (val inserted = documents.upsert(session.document, value)) {
            is UniversalMutationResult.Applied -> {
                val linked = documents.editSymbolic(inserted.document, id, parsed.second)
                val document = (linked as? UniversalMutationResult.Applied)?.document ?: inserted.document
                session.copy(document = document, selectedId = id, message = "${parsed.first} added to algebra, graph, table, and geometry.")
            }
            is UniversalMutationResult.Rejected -> session.copy(message = inserted.message)
            is UniversalMutationResult.Conflict -> session.copy(message = inserted.message)
        }
    }

    fun edit(session: UnifiedStudioSession, id: String, source: String): UnifiedStudioSession = when (val result = documents.editSymbolic(session.document, id, stripDefinition(source))) {
        is UniversalMutationResult.Applied -> session.copy(document = result.document, selectedId = id, message = "Linked views recomputed at revision ${result.document.revision}.")
        is UniversalMutationResult.Rejected -> session.copy(message = result.message)
        is UniversalMutationResult.Conflict -> session.copy(message = result.message)
    }

    fun remove(session: UnifiedStudioSession, id: String, cascade: Boolean = false): UnifiedStudioSession = when (val result = documents.remove(session.document, id, cascade = cascade)) {
        is UniversalMutationResult.Applied -> session.copy(document = result.document, selectedId = result.document.objects.values.firstOrNull { it.kind == UniversalMathKind.Function }?.id, message = "Removed ${result.affectedObjects.size} linked object(s).")
        is UniversalMutationResult.Rejected -> session.copy(message = result.message)
        is UniversalMutationResult.Conflict -> session.copy(message = result.message)
    }

    fun select(session: UnifiedStudioSession, id: String) = session.copy(selectedId = id)
    fun parameter(session: UnifiedStudioSession, name: String, value: Double) = session.copy(parameterValues = session.parameterValues + (name to value), message = "$name = ${format(value)}; all views updated.")
    fun toggleVisible(session: UnifiedStudioSession, id: String) = session.copy(hiddenIds = if (id in session.hiddenIds) session.hiddenIds - id else session.hiddenIds + id, message = "Visibility synchronized across views.")
    fun cycleColor(session: UnifiedStudioSession, id: String): UnifiedStudioSession {
        val current = session.colorKeys[id] ?: "cyan"; val next = palette[(palette.indexOf(current).takeIf { it >= 0 } ?: 0).plus(1) % palette.size]
        return session.copy(colorKeys = session.colorKeys + (id to next), message = "Object colour synchronized across views.")
    }

    fun transform(session: UnifiedStudioSession, operation: StudioTransform): UnifiedStudioSession {
        val selected = session.selectedSymbolic() ?: return session.copy(message = "Select an expression first.")
        val result = when (operation) {
            StudioTransform.Simplify -> cas.simplify(selected.second)
            StudioTransform.Expand -> cas.expand(selected.second)
            StudioTransform.Factor -> cas.factor(selected.second)
            StudioTransform.PartialFractions -> cas.partialFractions(selected.second)
            StudioTransform.Derivative -> cas.derivative(selected.second)
            StudioTransform.Integral -> cas.integral(selected.second)
            StudioTransform.Limit -> cas.limit(selected.second, approaching = "0")
        }
        val pod = StudioResultPod(operation.label, result.exact, result.assumptions.joinToString().ifBlank { "Deterministic local transformation with explicit rule steps." }, result.steps.map { "${it.title}: ${it.expression} — ${it.explanation}" }, result.supported)
        return session.copy(resultPods = listOf(pod) + session.resultPods.take(4), message = if (result.supported) "${operation.label} result verified locally." else "${operation.label} is outside the supported safe domain.")
    }

    fun explain(session: UnifiedStudioSession): UnifiedStudioSession {
        val selected = session.selectedSymbolic() ?: return session.copy(message = "Select an expression first.")
        val guided = solver.solve("Analyse ${selected.second}")
        val solution = guided.solution
        val pod = StudioResultPod("Explain", solution.answer, solution.verification, solution.steps.map { "${it.title}: ${it.expression} — ${it.explanation}" }, solution.supported)
        return session.copy(resultPods = listOf(pod) + session.resultPods.take(4), message = if (solution.supported) "Explanation linked to the selected object." else "Use a transform or inspect the live graph facts for this expression.")
    }

    fun projection(session: UnifiedStudioSession): StudioProjection {
        val validation = documents.validate(session.document)
        val symbolic = validation.topologicalOrder.mapNotNull(session.document.objects::get).filter { it.kind == UniversalMathKind.Function }
        val resolved = linkedMapOf<String, String>()
        val expressions = symbolic.mapIndexed { index, value ->
            val source = (value.payload as UniversalMathPayload.Symbolic).source
            var expression = source
            symbolic.take(index).forEach { dependency ->
                val dependencyName = dependency.name.substringBefore('(')
                val replacement = resolved[dependency.id] ?: return@forEach
                expression = expression.replace(Regex("\\b${Regex.escape(dependencyName)}\\s*\\(\\s*x\\s*\\)"), "($replacement)")
            }
            resolved[value.id] = expression
            val original = session.baseWorkspace.functions.firstOrNull { it.id == value.id }
            StudioExpression(value.id, value.name, source, expression, session.colorKeys[value.id] ?: original?.colorKey ?: palette[index % palette.size], value.id !in session.hiddenIds, value.dependencies)
        }
        val workspace = session.baseWorkspace.copy(functions = expressions.map { FunctionDefinition(it.id, it.name, it.resolvedSource, it.colorKey, it.visible) })
        val graph = objectGraph.snapshot(workspace, session.parameterValues)
        val errors = graph.expressionRows.associate { it.id to it.metadata.error }
        val decorated = expressions.map { it.copy(error = errors[it.id]) }
        val keyPoints = graph.graphObjects.associate { objectValue -> objectValue.rowId to (objectValue.roots.map { Vec2(it, 0.0) } + objectValue.extrema) }
        return StudioProjection(decorated, graph, keyPoints, validation.diagnostics + graph.diagnostics)
    }

    fun toWorkspace(session: UnifiedStudioSession): WorkspaceState {
        val projection = projection(session)
        return session.baseWorkspace.copy(
            functions = projection.expressions.map { FunctionDefinition(it.id, it.name, it.source, it.colorKey, it.visible) },
            graphSliderMetadata = session.baseWorkspace.graphSliderMetadata + session.parameterValues.mapValues { (name, value) ->
                (session.baseWorkspace.graphSliderMetadata[name] ?: GraphSliderMetadataState()).copy(value = value)
            },
            modifiedAt = System.currentTimeMillis(),
        )
    }

    private fun UnifiedStudioSession.selectedSymbolic(): Pair<UniversalMathObject, String>? {
        val value = selectedId?.let(document.objects::get) ?: return null
        val source = (value.payload as? UniversalMathPayload.Symbolic)?.source ?: return null
        return value to source
    }

    private fun parseDefinition(input: String, number: Int): Pair<String, String>? {
        val clean = input.trim()
        if (clean.isBlank()) return null
        val match = Regex("^([A-Za-z][A-Za-z0-9_]*)(?:\\(x\\))?\\s*=\\s*(.+)$").matchEntire(clean)
        return if (match != null) "${match.groupValues[1]}(x)" to match.groupValues[2] else "f$number(x)" to stripDefinition(clean)
    }

    private fun stripDefinition(source: String) = source.substringAfter('=', source).trim()
    private fun uniqueId(name: String, document: UniversalMathDocument): String {
        val base = "studio-" + name.substringBefore('(').lowercase().replace(Regex("[^a-z0-9]+"), "-")
        return generateSequence(base) { candidate -> "$base-${candidate.substringAfterLast('-', "1").toIntOrNull()?.plus(1) ?: 2}" }.first { it !in document.objects }
    }
    private fun format(value: Double) = String.format(java.util.Locale.US, "%.3f", value).trimEnd('0').trimEnd('.')
    private companion object { val palette = listOf("cyan", "violet", "green", "amber", "pink") }
}

class UnifiedStudioHistory(initial: UnifiedStudioSession, private val limit: Int = 60) {
    private val undo = ArrayDeque<UnifiedStudioSession>(); private val redo = ArrayDeque<UnifiedStudioSession>()
    var current: UnifiedStudioSession = initial; private set
    fun apply(next: UnifiedStudioSession): UnifiedStudioSession { if (next != current) { undo.addLast(current); while (undo.size > limit) undo.removeFirst(); redo.clear(); current = next }; return current }
    fun undo(): UnifiedStudioSession { undo.removeLastOrNull()?.let { redo.addLast(current); current = it }; return current }
    fun redo(): UnifiedStudioSession { redo.removeLastOrNull()?.let { undo.addLast(current); current = it }; return current }
    val canUndo get() = undo.isNotEmpty(); val canRedo get() = redo.isNotEmpty()
}
