package com.indianservers.aiexplorer.phase2.mathstudio

import com.indianservers.aiexplorer.core.CasRow
import com.indianservers.aiexplorer.core.FunctionDefinition
import com.indianservers.aiexplorer.core.MathSolverTutor
import com.indianservers.aiexplorer.core.SymbolicCalculusEngine
import com.indianservers.aiexplorer.core.SymbolicCasEngine
import com.indianservers.aiexplorer.core.UnifiedConstructionEngine
import com.indianservers.aiexplorer.core.UnifiedConstructionSession
import com.indianservers.aiexplorer.core.DynamicGeometryEngine
import com.indianservers.aiexplorer.core.DynamicPointRule
import com.indianservers.aiexplorer.core.StudioTokenKind
import com.indianservers.aiexplorer.core.StudioObjectToken
import com.indianservers.aiexplorer.core.Vec2
import com.indianservers.aiexplorer.spatial.SpatialConstruction3D
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
import com.indianservers.aiexplorer.workspace.Shape2D
import com.indianservers.aiexplorer.workspace.Shape2DType

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
    val algebraEntries: List<AlgebraEntry>,
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
    val construction: UnifiedConstructionSession = UnifiedConstructionSession(),
    val experience: SharedExperienceState = SharedExperienceState(),
    val algebraSort: AlgebraSortMode = AlgebraSortMode.Type,
    val algebraDisplay: AlgebraDisplayMode = AlgebraDisplayMode.Definition,
    val algebraFilter: AlgebraFilter = AlgebraFilter.All,
    val algebraSelection: Set<String> = emptySet(),
    val algebraStyles: Map<String, AlgebraObjectStyle> = emptyMap(),
    val collapsedAlgebraGroups: Set<String> = emptySet(),
    val showAuxiliary: Boolean = false,
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
        val parsed = runCatching { UniversalAlgebraParser.parse(input, session.document, session.document.objects.size + 1) }
            .getOrElse { return session.copy(message = it.message ?: "The mathematical object could not be interpreted.") }
        val value = parsed.value
        return when (val inserted = documents.upsert(session.document, value)) {
            is UniversalMutationResult.Applied -> session.copy(document = inserted.document, selectedId = value.id, algebraSelection = setOf(value.id),
                message = "${value.name} added as ${value.kind.name}; compatible views updated.")
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
        is UniversalMutationResult.Applied -> session.copy(document = result.document, selectedId = result.document.objects.values.firstOrNull()?.id,
            algebraSelection = session.algebraSelection - result.affectedObjects, message = "Removed ${result.affectedObjects.size} linked object(s).")
        is UniversalMutationResult.Rejected -> session.copy(message = result.message)
        is UniversalMutationResult.Conflict -> session.copy(message = result.message)
    }

    fun select(session: UnifiedStudioSession, id: String): UnifiedStudioSession {
        val selected = SharedExperienceEngine().select(session, id)
        return selected.copy(algebraSelection = setOf(selected.selectedId ?: id))
    }
    fun construct(session: UnifiedStudioSession, command: String): UnifiedStudioSession {
        val next = UnifiedConstructionEngine().execute(session.construction, command)
        val token = UnifiedConstructionEngine().tokens(next).last()
        val value = constructionAlgebraObject(session, next, token, command)
        val document = when (val inserted = documents.upsert(session.document, value)) {
            is UniversalMutationResult.Applied -> inserted.document
            else -> session.document
        }
        return session.copy(construction = next, document = document, selectedId = value.id, algebraSelection = setOf(value.id),
            message = "Created ${value.name} in Geometry and Algebra with ${value.dependencies.size} live dependency link(s).")
    }
    fun parameter(session: UnifiedStudioSession, name: String, value: Double) = session.copy(parameterValues = session.parameterValues + (name to value), message = "$name = ${format(value)}; all views updated.")
    fun toggleVisible(session: UnifiedStudioSession, id: String) = session.copy(hiddenIds = if (id in session.hiddenIds) session.hiddenIds - id else session.hiddenIds + id, message = "Visibility synchronized across views.")
    fun cycleColor(session: UnifiedStudioSession, id: String): UnifiedStudioSession {
        val current = session.colorKeys[id] ?: "cyan"; val next = palette[(palette.indexOf(current).takeIf { it >= 0 } ?: 0).plus(1) % palette.size]
        return session.copy(colorKeys = session.colorKeys + (id to next), message = "Object colour synchronized across views.")
    }

    fun configureAlgebra(session: UnifiedStudioSession, sort: AlgebraSortMode? = null, display: AlgebraDisplayMode? = null, filter: AlgebraFilter? = null) =
        session.copy(algebraSort = sort ?: session.algebraSort, algebraDisplay = display ?: session.algebraDisplay, algebraFilter = filter ?: session.algebraFilter)

    fun selectAlgebra(session: UnifiedStudioSession, id: String, additive: Boolean = false): UnifiedStudioSession {
        val selection = if (!additive) setOf(id) else if (id in session.algebraSelection) session.algebraSelection - id else session.algebraSelection + id
        return select(session.copy(algebraSelection = selection), id).copy(algebraSelection = selection)
    }

    fun setAlgebraStyle(session: UnifiedStudioSession, id: String, transform: (AlgebraObjectStyle) -> AlgebraObjectStyle): UnifiedStudioSession {
        val current = session.algebraStyles[id] ?: AlgebraObjectStyle(visible = id !in session.hiddenIds)
        val next = transform(current)
        return session.copy(algebraStyles = session.algebraStyles + (id to next), hiddenIds = if (next.visible) session.hiddenIds - id else session.hiddenIds + id,
            message = "Updated ${session.document.objects[id]?.name ?: id} properties across linked views.")
    }

    fun styleSelection(session: UnifiedStudioSession, transform: (AlgebraObjectStyle) -> AlgebraObjectStyle): UnifiedStudioSession {
        var next = session
        session.algebraSelection.forEach { id -> next = setAlgebraStyle(next, id, transform) }
        return next.copy(message = "Updated ${session.algebraSelection.size} selected Algebra object(s).")
    }

    fun selectAllAlgebra(session: UnifiedStudioSession, entries: List<AlgebraEntry>) = session.copy(algebraSelection = entries.mapTo(linkedSetOf()) { it.id }, message = "Selected ${entries.size} Algebra objects.")

    fun toggleAlgebraGroup(session: UnifiedStudioSession, group: String) = session.copy(collapsedAlgebraGroups =
        if (group in session.collapsedAlgebraGroups) session.collapsedAlgebraGroups - group else session.collapsedAlgebraGroups + group)

    fun setSelectionFolder(session: UnifiedStudioSession, folder: String): UnifiedStudioSession = styleSelection(session) { it.copy(folder = folder.trim().ifBlank { "Workspace" }) }

    fun duplicateSelection(session: UnifiedStudioSession): UnifiedStudioSession {
        var next = session
        val created = linkedSetOf<String>()
        session.algebraSelection.forEach { id ->
            val before = next.document.objects.keys
            next = duplicate(next, id)
            created += next.document.objects.keys - before
        }
        return next.copy(algebraSelection = created, message = "Duplicated ${created.size} Algebra object(s) with dependencies.")
    }

    fun copySelection(session: UnifiedStudioSession): String = UniversalAlgebraProjection.entries(session)
        .filter { it.id in session.algebraSelection }.joinToString("\n") { "${it.name} = ${it.definition}" }

    fun rename(session: UnifiedStudioSession, id: String, newName: String): UnifiedStudioSession {
        val value = session.document.objects[id] ?: return session.copy(message = "Unknown object $id")
        if (!Regex("[A-Za-z][A-Za-z0-9_]*(?:\\([^)]*\\))?").matches(newName.trim())) return session.copy(message = "Names begin with a letter and may contain digits or underscores.")
        if (session.document.objects.values.any { it.id != id && it.name == newName.trim() }) return session.copy(message = "${newName.trim()} is already in use.")
        return when (val result = documents.upsert(session.document, value.copy(name = newName.trim()))) {
            is UniversalMutationResult.Applied -> session.copy(document = result.document, message = "Renamed ${value.name} to ${newName.trim()}; references remain linked by object identity.")
            is UniversalMutationResult.Rejected -> session.copy(message = result.message)
            is UniversalMutationResult.Conflict -> session.copy(message = result.message)
        }
    }

    fun duplicate(session: UnifiedStudioSession, id: String): UnifiedStudioSession {
        val value = session.document.objects[id] ?: return session.copy(message = "Unknown object $id")
        val base = "${id}-copy"; val copyId = generateSequence(base) { "$it-copy" }.first { it !in session.document.objects }
        val names = session.document.objects.values.map { it.name }.toSet()
        val copyName = generateSequence("${value.name}_copy") { "${it}_copy" }.first { it !in names }
        return when (val result = documents.upsert(session.document, value.copy(id = copyId, name = copyName, objectRevision = 0))) {
            is UniversalMutationResult.Applied -> session.copy(document = result.document, selectedId = copyId, algebraSelection = setOf(copyId), message = "Duplicated ${value.name} with live dependencies.")
            is UniversalMutationResult.Rejected -> session.copy(message = result.message)
            is UniversalMutationResult.Conflict -> session.copy(message = result.message)
        }
    }

    fun makeIndependent(session: UnifiedStudioSession, id: String): UnifiedStudioSession {
        val current = session.document.objects[id] ?: return session.copy(message = "Unknown object $id")
        if (current.dependencies.isEmpty()) return session.copy(message = "${current.name} is already independent.")
        val resolved = resolvedDefinition(session, id)
        val detached = current.copy(payload = when (val payload = current.payload) {
            is UniversalMathPayload.Symbolic -> {
                val parsed = runCatching { cas.parse(resolved) }
                if (parsed.isFailure) payload else UniversalMathPayload.Symbolic(resolved, parsed.getOrNull())
            }
            is UniversalMathPayload.Coordinates -> payload
            is UniversalMathPayload.Dataset -> payload
            is UniversalMathPayload.Properties -> payload.copy(entries = payload.entries + ("definition" to resolved) + ("detachedFrom" to current.dependencies.joinToString()))
        }, dependencies = emptySet(), sourceView = "algebra/free")
        return when (val result = documents.upsert(session.document, detached)) {
            is UniversalMutationResult.Applied -> session.copy(document = result.document, selectedId = id,
                algebraSelection = if (session.algebraSelection.isEmpty()) setOf(id) else session.algebraSelection,
                message = "${current.name} is now independent; ${result.affectedObjects.size - 1} downstream object(s) retained their links to it.")
            is UniversalMutationResult.Rejected -> session.copy(message = result.message)
            is UniversalMutationResult.Conflict -> session.copy(message = result.message)
        }
    }

    fun duplicateAsFree(session: UnifiedStudioSession, id: String): UnifiedStudioSession {
        val current = session.document.objects[id] ?: return session.copy(message = "Unknown object $id")
        val base = "${id}-free"; val copyId = generateSequence(base) { "$it-copy" }.first { it !in session.document.objects }
        val names = session.document.objects.values.map { it.name }.toSet()
        val copyName = generateSequence("${current.name.substringBefore('(')}_free") { "${it}_copy" }.first { candidate -> names.none { it.substringBefore('(') == candidate } }
        val resolved = resolvedDefinition(session, id)
        val freePayload = when (val payload = current.payload) {
            is UniversalMathPayload.Symbolic -> runCatching { cas.parse(resolved) }.fold(
                onSuccess = { UniversalMathPayload.Symbolic(resolved, it) },
                onFailure = { UniversalMathPayload.Properties(mapOf("definition" to resolved, "copiedFrom" to id)) },
            )
            is UniversalMathPayload.Coordinates -> payload.copy()
            is UniversalMathPayload.Dataset -> payload.copy()
            is UniversalMathPayload.Properties -> payload.copy(entries = payload.entries + ("definition" to resolved) + ("copiedFrom" to id))
        }
        val copy = current.copy(id = copyId, name = if ('(' in current.name) "$copyName(x)" else copyName, payload = freePayload,
            dependencies = emptySet(), objectRevision = 0, sourceView = "algebra/free-copy")
        return when (val result = documents.upsert(session.document, copy)) {
            is UniversalMutationResult.Applied -> session.copy(document = result.document, selectedId = copyId, algebraSelection = setOf(copyId),
                algebraStyles = session.algebraStyles + (copyId to (session.algebraStyles[id] ?: AlgebraObjectStyle())), message = "Created independent copy ${copy.name}.")
            is UniversalMutationResult.Rejected -> session.copy(message = result.message)
            is UniversalMutationResult.Conflict -> session.copy(message = result.message)
        }
    }

    fun replaceReferences(session: UnifiedStudioSession, oldId: String, replacementId: String): UnifiedStudioSession {
        if (oldId == replacementId) return session.copy(message = "Choose two different objects.")
        val old = session.document.objects[oldId] ?: return session.copy(message = "Unknown object $oldId")
        val replacement = session.document.objects[replacementId] ?: return session.copy(message = "Unknown replacement $replacementId")
        val targets = session.document.dependentsOf(oldId, transitive = false).filter { it != replacementId }
        if (targets.isEmpty()) return session.copy(message = "${old.name} has no references to replace.")
        val oldSymbol = old.name.substringBefore('('); val newSymbol = replacement.name.substringBefore('(')
        val pattern = Regex("\\b${Regex.escape(oldSymbol)}\\b")
        var candidate = session.document
        for (targetId in targets.sortedBy { candidate.objects[it]?.objectRevision ?: 0 }) {
            val target = candidate.objects[targetId] ?: continue
            val updatedPayload = when (val payload = target.payload) {
                is UniversalMathPayload.Symbolic -> {
                    val source = payload.source.replace(pattern, newSymbol)
                    val parsed = runCatching { cas.parse(source) }.getOrElse { return session.copy(message = "Reference replacement would invalidate ${target.name}: ${it.message}") }
                    UniversalMathPayload.Symbolic(source, parsed)
                }
                is UniversalMathPayload.Properties -> payload.copy(entries = payload.entries.mapValues { (key, value) -> if (key in setOf("definition", "command", "result")) value.replace(pattern, newSymbol) else value })
                else -> payload
            }
            val updated = target.copy(payload = updatedPayload, dependencies = (target.dependencies - oldId) + replacementId)
            when (val result = documents.upsert(candidate, updated)) {
                is UniversalMutationResult.Applied -> candidate = result.document
                is UniversalMutationResult.Rejected -> return session.copy(message = "Reference replacement rejected: ${result.message}")
                is UniversalMutationResult.Conflict -> return session.copy(message = result.message)
            }
        }
        return session.copy(document = candidate, message = "Replaced ${old.name} with ${replacement.name} in ${targets.size} dependent object(s); styles and selections were preserved.")
    }

    fun redefinitionPreview(session: UnifiedStudioSession, id: String, source: String): AlgebraRedefinitionPreview {
        val current = session.document.objects[id] ?: return AlgebraRedefinitionPreview(id, false, "", source, emptySet(), emptySet(), "Unknown object $id")
        val affected = session.document.dependentsOf(id)
        val old = UniversalAlgebraProjection.entries(session).first { it.id == id }.definition
        val candidate = runCatching { UniversalAlgebraParser.parseRedefinition(source, current, session.document).value }
            .getOrElse { error ->
                val detail = error.message ?: "The definition is invalid."
                return AlgebraRedefinitionPreview(id, false, old, source, emptySet(), affected,
                    if (detail.contains("depend on itself", ignoreCase = true)) "Rejected: this definition would create a circular dependency." else detail)
            }
        return when (val validation = documents.upsert(session.document, candidate)) {
            is UniversalMutationResult.Applied -> AlgebraRedefinitionPreview(
                id, true, old, source, candidate.dependencies, affected,
                buildString {
                    if (candidate.kind != current.kind) append("Type changes from ${current.kind.name} to ${candidate.kind.name}. ")
                    append("Will recompute ${affected.size} downstream object(s).")
                },
            )
            is UniversalMutationResult.Rejected -> AlgebraRedefinitionPreview(id, false, old, source, candidate.dependencies, affected,
                if (validation.diagnostics.any { it.contains("cycle", ignoreCase = true) }) "Rejected: this definition would create a circular dependency."
                else validation.diagnostics.firstOrNull() ?: validation.message)
            is UniversalMutationResult.Conflict -> AlgebraRedefinitionPreview(id, false, old, source, candidate.dependencies, affected, validation.message)
        }
    }

    private fun resolvedDefinition(session: UnifiedStudioSession, id: String): String {
        val entry = UniversalAlgebraProjection.entries(session).firstOrNull { it.id == id } ?: return ""
        var resolved = entry.definition
        val visited = linkedSetOf<String>()
        fun substitute(dependencyId: String) {
            if (!visited.add(dependencyId)) return
            val dependency = session.document.objects[dependencyId] ?: return
            dependency.dependencies.forEach(::substitute)
            val dependencyEntry = UniversalAlgebraProjection.entries(session).firstOrNull { it.id == dependencyId } ?: return
            val name = dependency.name.substringBefore('(')
            val call = Regex("\\b${Regex.escape(name)}\\s*\\(\\s*x\\s*\\)")
            resolved = if ('(' in dependency.name && call.containsMatchIn(resolved)) resolved.replace(call, "(${dependencyEntry.value})")
            else resolved.replace(Regex("\\b${Regex.escape(name)}\\b"), "(${dependencyEntry.value})")
        }
        entry.dependencies.forEach(::substitute)
        return resolved
    }

    private fun constructionAlgebraObject(session: UnifiedStudioSession, construction: UnifiedConstructionSession, token: StudioObjectToken, source: String): UniversalMathObject {
        val dependencies = token.dependencies.mapTo(linkedSetOf()) { reference ->
            session.document.objects[reference]?.id ?: session.document.objects.values.firstOrNull { it.name.substringBefore('(') == reference }?.id ?: reference
        }
        val definition = constructionDefinition(source)
        val kind = when (token.kind) {
            StudioTokenKind.GraphExpression -> UniversalMathKind.Function
            StudioTokenKind.Point -> if (token.view.name == "Spatial3D") UniversalMathKind.Point3D else UniversalMathKind.Point2D
            StudioTokenKind.Line -> UniversalMathKind.Line
            StudioTokenKind.Circle -> UniversalMathKind.Circle
            StudioTokenKind.Conic -> UniversalMathKind.Conic
            StudioTokenKind.Vector -> UniversalMathKind.Vector
            StudioTokenKind.Plane -> UniversalMathKind.Plane
            StudioTokenKind.Surface -> UniversalMathKind.Surface
            StudioTokenKind.Constraint -> UniversalMathKind.Relation
            StudioTokenKind.Measurement -> UniversalMathKind.Measurement
        }
        val payload = when {
            token.kind == StudioTokenKind.Point && token.view.name == "Geometry2D" -> {
                val point = DynamicGeometryEngine().resolve(construction.geometry).getValue(token.id)
                UniversalMathPayload.Coordinates(listOf(point.x, point.y), listOf("x", "y"), definition.takeUnless { source.startsWith("point2d", true) || source.startsWith("point(", true) && dependencies.isEmpty() })
            }
            token.kind == StudioTokenKind.Point && token.view.name == "Spatial3D" -> {
                val point = (construction.spatial.entities[token.id] as? SpatialConstruction3D.Point)?.position
                UniversalMathPayload.Coordinates(listOfNotNull(point?.x, point?.y, point?.z), listOf("x", "y", "z"), definition)
            }
            token.kind == StudioTokenKind.GraphExpression -> {
                val expression = source.substringAfter(',', token.algebra).substringBeforeLast(')').trim()
                val parsed = runCatching { cas.parse(expression) }
                UniversalMathPayload.Symbolic(expression, parsed.getOrNull(), parsed.exceptionOrNull()?.message)
            }
            else -> UniversalMathPayload.Properties(mapOf("definition" to definition, "command" to definition, "algebra" to token.algebra))
        }
        val name = if (kind == UniversalMathKind.Function) "${token.id}(x)" else token.id
        return UniversalMathObject(token.id, kind, name, payload, dependencies, sourceView = when (token.view.name) {
            "Geometry2D" -> "geometry/algebra"; "Spatial3D" -> "3D/algebra"; else -> "graph/algebra"
        })
    }

    private fun constructionDefinition(source: String): String {
        val command = source.substringBefore('(').trim().lowercase()
        val arguments = source.substringAfter('(', "").substringBeforeLast(')', "").split(',').map(String::trim)
        val values = arguments.drop(1)
        val name = when (command) {
            "point2d", "point3d" -> "Point"
            "pointon" -> "Point"
            "intersection" -> "Intersect"
            "line2d", "line3d" -> "Line"
            "plane3d" -> "Plane"
            "vector3d" -> "Vector"
            "implicitsurface" -> "ImplicitSurface"
            "parametricsurface" -> "ParametricSurface"
            else -> command.replaceFirstChar { it.uppercase() }
        }
        return if (command in setOf("point2d", "point3d")) "(${values.joinToString()})" else "$name(${values.joinToString()})"
    }

    private fun syncConstructionCoordinates(session: UnifiedStudioSession): UnifiedStudioSession {
        val resolved = runCatching { DynamicGeometryEngine().resolve(session.construction.geometry) }.getOrElse { return session.copy(message = it.message ?: session.message) }
        var document = session.document
        resolved.forEach { (id, point) ->
            val current = document.objects[id] ?: return@forEach
            val payload = current.payload as? UniversalMathPayload.Coordinates ?: return@forEach
            val values = listOf(point.x, point.y)
            if (payload.values != values) when (val result = documents.upsert(document, current.copy(payload = payload.copy(values = values)))) {
                is UniversalMutationResult.Applied -> document = result.document
                else -> Unit
            }
        }
        return session.copy(document = document)
    }

    private fun addCasResult(session: UnifiedStudioSession, entry: AlgebraEntry, operation: String, result: CasRow): UnifiedStudioSession {
        val idBase = "cas-${operation.lowercase()}-${entry.id}"
        val id = generateSequence(idBase) { "$it-copy" }.first { it !in session.document.objects }
        val value = UniversalMathObject(id, UniversalMathKind.NotebookCell, "$operation(${entry.name})",
            UniversalMathPayload.Properties(mapOf("definition" to "$operation(${entry.name})", "result" to result.exact, "verified" to result.supported.toString())), setOf(entry.id), sourceView = "CAS")
        return when (val inserted = documents.upsert(session.document, value)) {
            is UniversalMutationResult.Applied -> session.copy(document = inserted.document, selectedId = id, algebraSelection = setOf(id), message = "$operation result added to Algebra: ${result.exact}")
            else -> session.copy(message = "$operation result could not be attached to the document.")
        }
    }

    fun redefine(session: UnifiedStudioSession, id: String, source: String): UnifiedStudioSession {
        val preview = redefinitionPreview(session, id, source)
        if (!preview.accepted) return session.copy(message = preview.message)
        val current = session.document.objects[id] ?: return session
        val candidate = runCatching { UniversalAlgebraParser.parseRedefinition(source, current, session.document).value }
            .getOrElse { return session.copy(message = it.message ?: "The new definition is invalid.") }
        val result = documents.upsert(session.document, candidate)
        return when (result) {
            is UniversalMutationResult.Applied -> {
                val values = (result.document.objects[id]?.payload as? UniversalMathPayload.Coordinates)?.values
                val construction = if (values != null && values.size == 2 && session.construction.geometry.points.any { it.id == id && it.rule is DynamicPointRule.Free }) {
                    DynamicGeometryEngine().moveFreePoint(session.construction.geometry, id, Vec2(values[0], values[1])).let { session.construction.copy(geometry = it, revision = session.construction.revision + 1) }
                } else session.construction
                syncConstructionCoordinates(session.copy(document = result.document, construction = construction, message = "Redefined ${current.name}. ${preview.message}"))
            }
            is UniversalMutationResult.Rejected -> session.copy(message = result.message)
            is UniversalMutationResult.Conflict -> session.copy(message = result.message)
        }
    }

    fun moveGeometryPoint(session: UnifiedStudioSession, id: String, position: Vec2): UnifiedStudioSession {
        val geometry = runCatching { DynamicGeometryEngine().moveFreePoint(session.construction.geometry, id, position) }
            .getOrElse { return session.copy(message = it.message ?: "Only free points can be dragged.") }
        return syncConstructionCoordinates(session.copy(construction = session.construction.copy(geometry = geometry, revision = session.construction.revision + 1),
            message = "$id moved to (${format(position.x)}, ${format(position.y)}); Algebra and dependents updated."))
    }

    fun applyEquationAction(session: UnifiedStudioSession, id: String, action: AlgebraEquationAction, argument: String = ""): UnifiedStudioSession {
        val entry = UniversalAlgebraProjection.entries(session).firstOrNull { it.id == id } ?: return session.copy(message = "Unknown equation $id")
        if (action !in AlgebraEquationActions.available(entry)) return session.copy(message = "${action.label} is not applicable to ${entry.kind.name}.")
        val variables = AlgebraEquationActions.variables(entry)
        return when (action) {
            AlgebraEquationAction.InspectVariables -> session.copy(message = "Variables in ${entry.name}: ${variables.joinToString().ifBlank { "none" }}. Dependencies: ${entry.dependencies.joinToString().ifBlank { "none" }}.")
            AlgebraEquationAction.Graph -> setAlgebraStyle(session, id) { it.copy(visible = true) }.copy(message = "${entry.name} is visible in every compatible graph or geometry view.")
            AlgebraEquationAction.Rearrange -> {
                val left = entry.definition.substringBefore('=').trim(); val right = entry.definition.substringAfter('=').trim()
                redefine(session, id, "$right = $left")
            }
            AlgebraEquationAction.Substitute -> {
                val pair = argument.split('=', limit = 2).map(String::trim)
                if (pair.size != 2 || pair[0].isBlank()) session.copy(message = "Enter substitution as variable=value.")
                else redefine(session, id, entry.definition.replace(Regex("\\b${Regex.escape(pair[0])}\\b"), "(${pair[1]})"))
            }
            AlgebraEquationAction.ConvertForm -> session.copy(message = when (entry.kind) {
                UniversalMathKind.Line -> "${entry.name}: standard coordinate form ${entry.definition}; select Rearrange to change the isolated side."
                UniversalMathKind.Circle, UniversalMathKind.Conic -> "${entry.name}: center/standard conic form preserved; exact coefficients are available in Definition mode."
                else -> "${entry.name}: exact relation form ${entry.definition}."
            })
            AlgebraEquationAction.CreateGeometry -> when (val linked = documents.upsert(session.document, session.document.objects.getValue(id).copy(sourceView = "algebra/geometry"))) {
                is UniversalMutationResult.Applied -> session.copy(document = linked.document, message = "${entry.name} is now linked as dependent geometry without changing its equation identity.")
                is UniversalMutationResult.Rejected -> session.copy(message = linked.message)
                is UniversalMutationResult.Conflict -> session.copy(message = linked.message)
            }
            AlgebraEquationAction.Solve -> {
                val equations = when (entry.kind) {
                    UniversalMathKind.EquationSystem -> entry.definition.removePrefix("System(").removeSuffix(")").split(';', ',').filter { '=' in it }
                    else -> listOf(entry.definition)
                }
                val result = runCatching { cas.solveSystem(equations, variables.ifEmpty { listOf("x") }) }.getOrNull()
                if (result == null || !result.supported) session.copy(message = "Keep the typed equation; open CAS to choose a numerical or symbolic method.")
                else addCasResult(session, entry, "Solve", result)
            }
        }
    }

    fun removeSelection(session: UnifiedStudioSession, cascade: Boolean = false): UnifiedStudioSession {
        var next = session
        val ordered = session.algebraSelection.sortedByDescending { session.document.dependentsOf(it).size }
        ordered.forEach { id -> if (id in next.document.objects) next = remove(next, id, cascade) }
        return next.copy(message = "Deleted ${session.algebraSelection.size - next.algebraSelection.size} selected Algebra object(s).")
    }

    fun constructFromSelection(session: UnifiedStudioSession, command: String): UnifiedStudioSession {
        val names = session.algebraSelection.mapNotNull { session.document.objects[it]?.name }
        if (names.isEmpty()) return session.copy(message = "Select compatible Algebra objects first.")
        return add(session, "${command.lowercase()}${session.document.objects.size + 1} = $command(${names.joinToString()})")
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
        val resultIdBase = "cas-${operation.name.lowercase()}-${selected.first.id}"
        val resultId = generateSequence(resultIdBase) { "$it-copy" }.first { it !in session.document.objects }
        val casObject = UniversalMathObject(resultId, UniversalMathKind.NotebookCell, "${operation.label}(${selected.first.name})",
            UniversalMathPayload.Properties(mapOf("definition" to "${operation.label}(${selected.first.name})", "result" to result.exact,
                "verified" to result.supported.toString(), "steps" to result.steps.size.toString())), setOf(selected.first.id), sourceView = "CAS")
        val document = (documents.upsert(session.document, casObject) as? UniversalMutationResult.Applied)?.document ?: session.document
        return session.copy(document = document, resultPods = listOf(pod) + session.resultPods.take(4), selectedId = resultId,
            algebraSelection = setOf(resultId), message = if (result.supported) "${operation.label} result verified and added to Algebra." else "${operation.label} is outside the supported safe domain.")
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
        val symbolic = validation.topologicalOrder.mapNotNull(session.document.objects::get).filter {
            it.kind in setOf(UniversalMathKind.Function, UniversalMathKind.PiecewiseFunction) && it.payload is UniversalMathPayload.Symbolic
        }
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
        return StudioProjection(decorated, UniversalAlgebraProjection.entries(session), graph, keyPoints, validation.diagnostics + graph.diagnostics)
    }

    fun toWorkspace(session: UnifiedStudioSession): WorkspaceState {
        val projection = projection(session)
        val extraPointObjects = session.document.objects.values.filter { it.kind == UniversalMathKind.Point2D && !it.id.startsWith("point-") }
        val points = session.baseWorkspace.points + extraPointObjects.mapNotNull { (it.payload as? UniversalMathPayload.Coordinates)?.values?.takeIf { values -> values.size >= 2 }?.let { values -> Vec2(values[0], values[1]) } }
        val pointIndices = buildMap {
            session.baseWorkspace.points.indices.forEach { put("point-$it", it) }
            extraPointObjects.forEachIndexed { index, value -> put(value.id, session.baseWorkspace.points.size + index) }
        }
        val existingShapeIds = session.baseWorkspace.shapes.mapTo(hashSetOf()) { it.id }
        val extraShapes = session.document.objects.values.filter { it.id !in existingShapeIds }.mapNotNull { value ->
            val type = when (value.kind) {
                UniversalMathKind.Line -> Shape2DType.Line
                UniversalMathKind.Ray -> Shape2DType.Ray
                UniversalMathKind.Segment -> Shape2DType.Segment
                UniversalMathKind.Circle -> Shape2DType.Circle
                UniversalMathKind.Conic -> Shape2DType.Ellipse
                else -> null
            } ?: return@mapNotNull null
            val indices = value.dependencies.mapNotNull(pointIndices::get)
            if (indices.isEmpty()) null else Shape2D(value.id, type, indices, value.name, value.id !in session.hiddenIds, session.algebraStyles[value.id]?.locked ?: false, session.colorKeys[value.id] ?: "default")
        }
        return session.baseWorkspace.copy(
            points = points,
            shapes = session.baseWorkspace.shapes + extraShapes,
            functions = projection.expressions.map { FunctionDefinition(it.id, it.name, it.source, it.colorKey, it.visible) },
            graphSliderMetadata = session.baseWorkspace.graphSliderMetadata + session.parameterValues.mapValues { (name, value) ->
                (session.baseWorkspace.graphSliderMetadata[name] ?: GraphSliderMetadataState()).copy(value = value)
            },
            universalMathDocument = session.document,
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
