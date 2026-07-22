package com.indianservers.aiexplorer.phase2.mathstudio

import com.indianservers.aiexplorer.workspace.UniversalMathKind
import com.indianservers.aiexplorer.workspace.UniversalMathObject
import com.indianservers.aiexplorer.core.MathStudioView
import com.indianservers.aiexplorer.core.UnifiedConstructionEngine

/** Every mathematical representation that can participate in the same live document. */
enum class SharedMathView(val label: String) {
    Algebra("Algebra"), Graph("Graph"), Table("Table"), Geometry2D("2D"), Spatial3D("3D"),
    Cas("CAS"), Proof("Proof"), Timeline("Timeline"), Inspector("Inspector"), Labs("Advanced labs"),
}

enum class SharedWorkspaceMode(val label: String, val description: String) {
    Explore("Explore", "Direct manipulation with the main linked views."),
    Learn("Learn", "Guidance, explanations and checkpoints stay beside the construction."),
    Proof("Proof", "Dependencies, claims and verification are foregrounded."),
    Exam("Exam", "A focused, auditable workspace with restricted assistance."),
    Research("Research", "More simultaneous views, provenance and diagnostic detail."),
    Presentation("Present", "Large canvas, simplified controls and replay support."),
}

data class SharedStudioLayout(
    val activeViews: List<SharedMathView>,
    val focusedView: SharedMathView = activeViews.first(),
) {
    init { require(activeViews.isNotEmpty() && activeViews.size <= 4 && focusedView in activeViews) }

    fun toggle(view: SharedMathView): SharedStudioLayout {
        if (view in activeViews) {
            if (activeViews.size == 1) return this
            val next = activeViews - view
            return copy(activeViews = next, focusedView = focusedView.takeIf { it in next } ?: next.first())
        }
        val next = (activeViews + view).takeLast(4)
        return copy(activeViews = next, focusedView = view)
    }

    fun focus(view: SharedMathView) = if (view in activeViews) copy(focusedView = view) else this

    companion object {
        fun forMode(mode: SharedWorkspaceMode) = when (mode) {
            SharedWorkspaceMode.Explore -> SharedStudioLayout(listOf(SharedMathView.Algebra, SharedMathView.Graph, SharedMathView.Table))
            SharedWorkspaceMode.Learn -> SharedStudioLayout(listOf(SharedMathView.Algebra, SharedMathView.Graph, SharedMathView.Cas))
            SharedWorkspaceMode.Proof -> SharedStudioLayout(listOf(SharedMathView.Geometry2D, SharedMathView.Proof, SharedMathView.Inspector))
            SharedWorkspaceMode.Exam -> SharedStudioLayout(listOf(SharedMathView.Algebra, SharedMathView.Graph))
            SharedWorkspaceMode.Research -> SharedStudioLayout(listOf(SharedMathView.Graph, SharedMathView.Spatial3D, SharedMathView.Cas, SharedMathView.Labs))
            SharedWorkspaceMode.Presentation -> SharedStudioLayout(listOf(SharedMathView.Graph, SharedMathView.Timeline), SharedMathView.Graph)
        }
    }
}

data class SharedSelection(
    val anchorId: String? = null,
    val canonicalIds: Set<String> = emptySet(),
    val representationIds: Set<String> = emptySet(),
    val announcement: String = "Nothing selected.",
)

data class SharedExperienceState(
    val mode: SharedWorkspaceMode = SharedWorkspaceMode.Explore,
    val layout: SharedStudioLayout = SharedStudioLayout.forMode(SharedWorkspaceMode.Explore),
    val selection: SharedSelection = SharedSelection(),
)

enum class SharedNodeRole { Canonical, Representation, Derived }

data class SharedMathNode(
    val id: String,
    val canonicalId: String,
    val name: String,
    val view: SharedMathView,
    val role: SharedNodeRole,
    val dependencies: Set<String>,
    val source: String,
    val accessibleLabel: String,
)

data class SharedMathEdge(val from: String, val to: String, val relation: String)

data class SharedObjectGraph(
    val nodes: Map<String, SharedMathNode>,
    val edges: List<SharedMathEdge>,
) {
    fun representations(canonicalId: String) = nodes.values.filter { it.canonicalId == canonicalId }
    fun dependents(canonicalId: String) = edges.filter { it.from == canonicalId && it.relation == "depends-on" }.map { it.to }.toSet()
    fun dependencies(canonicalId: String) = edges.filter { it.to == canonicalId && it.relation == "depends-on" }.map { it.from }.toSet()
}

data class VariableProvenance(
    val objectId: String,
    val name: String,
    val sourceView: String,
    val directDependencies: List<String>,
    val dependencyChain: List<String>,
    val dependentObjects: List<String>,
    val assumptions: List<String>,
    val explanation: String,
)

/** Builds view representations from the canonical document; views never own divergent copies. */
object SharedObjectGraphBuilder {
    fun build(session: UnifiedStudioSession): SharedObjectGraph {
        val document = session.document
        val nodes = linkedMapOf<String, SharedMathNode>()
        val edges = mutableListOf<SharedMathEdge>()
        document.objects.values.forEach { value ->
            val canonicalView = primaryView(value)
            nodes[value.id] = node(value.id, value, canonicalView, SharedNodeRole.Canonical)
            value.dependencies.forEach { dependency -> edges += SharedMathEdge(dependency, value.id, "depends-on") }
            representations(value).forEach { view ->
                val id = "${value.id}@${view.name.lowercase()}"
                nodes[id] = node(id, value, view, SharedNodeRole.Representation)
                edges += SharedMathEdge(value.id, id, "represented-as")
            }
        }
        UnifiedConstructionEngine().tokens(session.construction).forEach { token ->
            val canonical = "construction:${token.id}"
            val view = when (token.view) {
                MathStudioView.Graph -> SharedMathView.Graph
                MathStudioView.Geometry2D -> SharedMathView.Geometry2D
                MathStudioView.Spatial3D -> SharedMathView.Spatial3D
            }
            val dependencies = token.dependencies.mapTo(linkedSetOf()) { "construction:$it" }
            nodes[canonical] = SharedMathNode(canonical, canonical, token.id, view, SharedNodeRole.Canonical, dependencies, token.algebra, token.accessibleLabel)
            val representationId = "$canonical@${view.name.lowercase()}"
            nodes[representationId] = SharedMathNode(representationId, canonical, token.id, view, SharedNodeRole.Representation, dependencies, token.algebra, token.accessibleLabel)
            dependencies.forEach { edges += SharedMathEdge(it, canonical, "depends-on") }
            edges += SharedMathEdge(canonical, representationId, "represented-as")
        }
        session.resultPods.forEachIndexed { index, pod ->
            val canonical = session.selectedId ?: return@forEachIndexed
            val id = "$canonical@cas-result-$index"
            nodes[id] = SharedMathNode(id, canonical, pod.title, SharedMathView.Cas, SharedNodeRole.Derived, setOf(canonical), pod.exact, "CAS result ${pod.title}: ${pod.exact}")
            edges += SharedMathEdge(canonical, id, "derived-as")
        }
        return SharedObjectGraph(nodes, edges)
    }

    private fun node(id: String, value: UniversalMathObject, view: SharedMathView, role: SharedNodeRole) = SharedMathNode(
        id, value.id, value.name, view, role, value.dependencies, value.payload.toString(),
        "${view.label} ${value.kind.name.lowercase()} ${value.name}; ${value.dependencies.size} dependencies",
    )

    private fun primaryView(value: UniversalMathObject) = when (value.kind) {
        UniversalMathKind.Function -> SharedMathView.Algebra
        UniversalMathKind.Point2D, UniversalMathKind.GeometryConstruction -> SharedMathView.Geometry2D
        UniversalMathKind.Point3D, UniversalMathKind.Vector, UniversalMathKind.Solid,
        UniversalMathKind.Surface, UniversalMathKind.SpatialScene -> SharedMathView.Spatial3D
        UniversalMathKind.NotebookCell, UniversalMathKind.Matrix -> SharedMathView.Cas
        UniversalMathKind.DataList -> SharedMathView.Table
        else -> SharedMathView.Inspector
    }

    private fun representations(value: UniversalMathObject): Set<SharedMathView> = when (value.kind) {
        UniversalMathKind.Function -> setOf(SharedMathView.Algebra, SharedMathView.Graph, SharedMathView.Table, SharedMathView.Cas)
        UniversalMathKind.Point2D, UniversalMathKind.GeometryConstruction -> setOf(SharedMathView.Geometry2D, SharedMathView.Algebra, SharedMathView.Inspector)
        UniversalMathKind.Point3D, UniversalMathKind.Vector, UniversalMathKind.Solid,
        UniversalMathKind.Surface, UniversalMathKind.SpatialScene -> setOf(SharedMathView.Spatial3D, SharedMathView.Algebra, SharedMathView.Inspector)
        UniversalMathKind.NotebookCell, UniversalMathKind.Matrix -> setOf(SharedMathView.Cas, SharedMathView.Algebra, SharedMathView.Inspector)
        UniversalMathKind.DataList -> setOf(SharedMathView.Table, SharedMathView.Graph, SharedMathView.Inspector)
        UniversalMathKind.UnitMeasurement -> setOf(SharedMathView.Algebra, SharedMathView.Geometry2D, SharedMathView.Spatial3D, SharedMathView.Inspector)
        else -> setOf(primaryView(value), SharedMathView.Inspector)
    }
}

class SharedExperienceEngine {
    fun setMode(session: UnifiedStudioSession, mode: SharedWorkspaceMode) = session.copy(
        experience = session.experience.copy(mode = mode, layout = SharedStudioLayout.forMode(mode)),
        message = "${mode.label} workspace: ${mode.description}",
    )

    fun toggleView(session: UnifiedStudioSession, view: SharedMathView) = session.copy(
        experience = session.experience.copy(layout = session.experience.layout.toggle(view)),
        message = "Linked workspace layout updated.",
    )

    fun focusView(session: UnifiedStudioSession, view: SharedMathView) = session.copy(
        experience = session.experience.copy(layout = session.experience.layout.focus(view)),
    )

    fun select(session: UnifiedStudioSession, requestedId: String, additive: Boolean = false): UnifiedStudioSession {
        val graph = SharedObjectGraphBuilder.build(session)
        val requested = graph.nodes[requestedId] ?: graph.nodes.values.firstOrNull { it.canonicalId == requestedId }
            ?: return session.copy(message = "Unknown linked object $requestedId.")
        val canonical = if (additive) session.experience.selection.canonicalIds + requested.canonicalId else setOf(requested.canonicalId)
        val representations = graph.nodes.values.filter { it.canonicalId in canonical }.mapTo(linkedSetOf()) { it.id }
        val views = graph.nodes.values.filter { it.id in representations }.map { it.view.label }.distinct()
        val selection = SharedSelection(requested.id, canonical, representations, "Selected ${canonical.joinToString()} across ${views.joinToString()}.")
        return session.copy(selectedId = requested.canonicalId, experience = session.experience.copy(selection = selection), message = selection.announcement)
    }

    fun inspectProvenance(session: UnifiedStudioSession, objectId: String): VariableProvenance {
        val graph = SharedObjectGraphBuilder.build(session)
        val node = graph.nodes[objectId] ?: graph.nodes.values.firstOrNull { it.canonicalId == objectId } ?: error("Unknown linked object $objectId")
        val value = session.document.objects[node.canonicalId]
        if (value == null) {
            val dependencies = node.dependencies.sorted()
            val dependents = graph.dependents(node.canonicalId).sorted()
            return VariableProvenance(node.canonicalId, node.name, node.view.label, dependencies, dependencies, dependents, emptyList(),
                "${node.name} is a live ${node.view.label} construction; it depends on ${dependencies.joinToString().ifBlank { "no other objects" }} and updates ${dependents.joinToString().ifBlank { "no dependents" }}.")
        }
        val chain = linkedSetOf<String>()
        fun visit(id: String) { session.document.objects[id]?.dependencies.orEmpty().forEach { if (chain.add(it)) visit(it) } }
        visit(value.id)
        val dependents = session.document.dependentsOf(value.id).sorted()
        return VariableProvenance(value.id, value.name, value.sourceView, value.dependencies.sorted(), chain.toList(), dependents, value.assumptions.descriptions,
            "${value.name} originates in ${value.sourceView}; it depends on ${chain.joinToString().ifBlank { "no other objects" }} and updates ${dependents.joinToString().ifBlank { "no dependents" }}.")
    }
}

data class SemanticStudioAction(
    val id: Long,
    val label: String,
    val beforeRevision: Long,
    val afterRevision: Long,
    val selectedIds: Set<String>,
    val timestampMillis: Long,
)

data class StudioHistoryBranch(val id: String, val name: String, val headActionId: Long, val actionCount: Int)
data class StudioBranchComparison(
    val first: StudioHistoryBranch,
    val second: StudioHistoryBranch,
    val changedObjects: Set<String>,
    val firstRevision: Long,
    val secondRevision: Long,
)

/** A semantic, branchable history. Undo followed by an edit preserves the abandoned future as a branch. */
class SharedStudioHistory(initial: UnifiedStudioSession, private val limit: Int = 120) {
    private data class Entry(val action: SemanticStudioAction?, val session: UnifiedStudioSession, var parent: Entry?, val children: MutableList<Entry> = mutableListOf(), val branchName: String? = null)
    private var sequence = 0L
    private var root = Entry(null, initial, null)
    private var cursor = root

    val current get() = cursor.session
    val canUndo get() = cursor.parent != null
    val canRedo get() = cursor.children.isNotEmpty()

    fun apply(next: UnifiedStudioSession, label: String): UnifiedStudioSession {
        if (next == cursor.session) return current
        val action = SemanticStudioAction(++sequence, label, cursor.session.document.revision, next.document.revision,
            next.experience.selection.canonicalIds.ifEmpty { setOfNotNull(next.selectedId) }, System.currentTimeMillis())
        val branchName = if (cursor.children.isNotEmpty()) "Alternative ${cursor.children.size + 1}" else null
        val entry = Entry(action, next, cursor, branchName = branchName)
        cursor.children += entry
        cursor = entry
        trim()
        return current
    }

    fun undo(): UnifiedStudioSession { cursor.parent?.let { cursor = it }; return current }
    fun redo(branchIndex: Int = cursor.children.lastIndex): UnifiedStudioSession {
        cursor.children.getOrNull(branchIndex.coerceAtLeast(0))?.let { cursor = it }
        return current
    }

    fun timeline(): List<SemanticStudioAction> = generateSequence(cursor) { it.parent }.mapNotNull { it.action }.toList().asReversed()

    fun branches(): List<StudioHistoryBranch> = cursor.children.mapIndexed { index, entry ->
        val actions = generateSequence(entry) { it.children.lastOrNull() }.toList()
        StudioHistoryBranch("branch-${entry.action?.id ?: index}", entry.branchName ?: "Continuation ${index + 1}", actions.last().action?.id ?: 0, actions.size)
    }

    fun checkout(branchId: String): UnifiedStudioSession {
        val entry = cursor.children.firstOrNull { "branch-${it.action?.id}" == branchId } ?: return current
        cursor = generateSequence(entry) { it.children.lastOrNull() }.last()
        return current
    }

    fun compare(firstId: String, secondId: String): StudioBranchComparison {
        val entries = cursor.children.associateBy { "branch-${it.action?.id}" }
        val firstEntry = entries[firstId] ?: error("Unknown branch $firstId")
        val secondEntry = entries[secondId] ?: error("Unknown branch $secondId")
        fun head(entry: Entry) = generateSequence(entry) { it.children.lastOrNull() }.last()
        fun metadata(entry: Entry): StudioHistoryBranch {
            val actions = generateSequence(entry) { it.children.lastOrNull() }.toList()
            return StudioHistoryBranch("branch-${entry.action?.id}", entry.branchName ?: "Continuation", actions.last().action?.id ?: 0, actions.size)
        }
        val first = head(firstEntry); val second = head(secondEntry)
        val firstObjects = first.session.document.objects; val secondObjects = second.session.document.objects
        val changed = (firstObjects.keys + secondObjects.keys).filterTo(linkedSetOf()) { firstObjects[it] != secondObjects[it] }
        (first.session.parameterValues.keys + second.session.parameterValues.keys).filterTo(changed) {
            first.session.parameterValues[it] != second.session.parameterValues[it]
        }
        val labeledChanges = changed.mapTo(linkedSetOf()) { if (it in firstObjects || it in secondObjects) it else "parameter:$it" }
        return StudioBranchComparison(metadata(firstEntry), metadata(secondEntry), labeledChanges, first.session.document.revision, second.session.document.revision)
    }

    fun merge(branchId: String, label: String = "Merge preferred exploration branch"): UnifiedStudioSession {
        val preferred = cursor.children.firstOrNull { "branch-${it.action?.id}" == branchId } ?: return current
        val head = generateSequence(preferred) { it.children.lastOrNull() }.last()
        return apply(head.session.copy(message = label), label)
    }

    private fun trim() {
        // Preserve branch topology; only detach the oldest linear prefix when a very long session is produced.
        if (sequence <= limit) return
        var ancestor = cursor
        repeat(limit) { ancestor = ancestor.parent ?: return }
        ancestor.parent?.children?.remove(ancestor)
        ancestor.parent = null
        root = ancestor
    }
}

data class RecordedStudioEvent(val elapsedMillis: Long, val label: String, val session: UnifiedStudioSession)
data class RecordedStudioSession(val title: String, val mode: SharedWorkspaceMode, val events: List<RecordedStudioEvent>, val durationMillis: Long)

/** Records state-changing manipulations, so playback remains interactive and can be resumed from any event. */
class StudioSessionRecorder(private val clock: () -> Long = System::currentTimeMillis) {
    private var startedAt: Long? = null
    private var title = "Untitled exploration"
    private val events = mutableListOf<RecordedStudioEvent>()

    val isRecording get() = startedAt != null

    fun start(title: String, initial: UnifiedStudioSession) {
        require(!isRecording)
        this.title = title.ifBlank { "Untitled exploration" }
        startedAt = clock(); events.clear()
        capture("Session started", initial)
    }

    fun capture(label: String, session: UnifiedStudioSession) {
        val start = startedAt ?: return
        events += RecordedStudioEvent((clock() - start).coerceAtLeast(0), label, session)
    }

    fun stop(): RecordedStudioSession {
        val start = startedAt ?: error("Recorder is not running")
        val duration = (clock() - start).coerceAtLeast(events.lastOrNull()?.elapsedMillis ?: 0)
        startedAt = null
        return RecordedStudioSession(title, events.firstOrNull()?.session?.experience?.mode ?: SharedWorkspaceMode.Explore, events.toList(), duration)
    }

    fun stateAt(recording: RecordedStudioSession, elapsedMillis: Long): UnifiedStudioSession =
        recording.events.lastOrNull { it.elapsedMillis <= elapsedMillis }?.session
            ?: recording.events.firstOrNull()?.session ?: error("Recording contains no events")
}
