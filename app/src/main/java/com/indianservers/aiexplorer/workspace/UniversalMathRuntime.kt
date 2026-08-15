package com.indianservers.aiexplorer.workspace

/** Reverse edges are kept separately so a parent edit can invalidate descendants in O(affected edges). */
data class UniversalDependencyIndex(
    val dependencies: Map<String, Set<String>>,
    val dependents: Map<String, Set<String>>,
) {
    fun affectedBy(ids: Set<String>): Set<String> {
        val affected = linkedSetOf<String>()
        val queue = ArrayDeque(ids.filter(dependencies::containsKey))
        while (queue.isNotEmpty()) {
            val id = queue.removeFirst()
            if (!affected.add(id)) continue
            dependents[id].orEmpty().sorted().forEach(queue::addLast)
        }
        return affected
    }

    companion object {
        fun build(document: UniversalMathDocument): UniversalDependencyIndex {
            val dependencies = document.objects.mapValues { it.value.dependencies }
            val dependents = document.objects.keys.associateWith { linkedSetOf<String>() }.toMutableMap()
            dependencies.forEach { (child, parents) -> parents.forEach { parent -> dependents.getOrPut(parent) { linkedSetOf() } += child } }
            return UniversalDependencyIndex(dependencies, dependents.mapValues { it.value.toSet() })
        }
    }
}

fun interface UniversalMathObjectEvaluator {
    fun evaluate(value: UniversalMathObject, dependencies: Map<String, UniversalMathValueState>): UniversalMathValueState
}

data class UniversalObjectEvaluation(
    val objectId: String,
    val status: UniversalMathValueStatus,
    val durationNanos: Long,
    val cacheHit: Boolean,
    val diagnostic: String? = null,
)

data class UniversalRecomputeReport(
    val document: UniversalMathDocument,
    val requestedObjects: Set<String>,
    val affectedObjects: Set<String>,
    val evaluationOrder: List<String>,
    val evaluations: List<UniversalObjectEvaluation>,
    val diagnostics: List<String>,
    val totalDurationNanos: Long,
) {
    val successful: Boolean get() = evaluations.none { it.status in setOf(
        UniversalMathValueStatus.MissingDependency,
        UniversalMathValueStatus.CyclicDependency,
        UniversalMathValueStatus.NumericallyUnstable,
    ) }
}

/**
 * Incremental, view-independent Geometry/Graph runtime. It never hides invalid objects: failures are
 * persisted as value states so every linked view can display the same diagnostic and last definition.
 */
class UniversalMathRuntime(
    private val evaluator: UniversalMathObjectEvaluator = UniversalMathObjectEvaluator(::defaultEvaluation),
    private val nanoTime: () -> Long = System::nanoTime,
) {
    private val evaluatedSignatures = mutableMapOf<String, String>()

    fun recompute(document: UniversalMathDocument, changedIds: Set<String> = document.objects.keys): UniversalRecomputeReport {
        val started = nanoTime()
        val index = UniversalDependencyIndex.build(document)
        val validation = UniversalMathDocumentEngine().validate(document)
        val requested = changedIds.intersect(document.objects.keys)
        val affected = index.affectedBy(requested)
        val cycleIds = validation.cycles.flatten().toSet()
        val states = document.objects.mapValues { it.value.valueState }.toMutableMap()
        val evaluations = mutableListOf<UniversalObjectEvaluation>()
        val order = validation.topologicalOrder.filter(affected::contains)

        order.forEach { id ->
            val value = document.objects.getValue(id)
            val before = nanoTime()
            val missing = validation.missingDependencies[id].orEmpty()
            val state: UniversalMathValueState
            val cacheHit: Boolean
            val signature = signature(document, value, states)
            when {
                missing.isNotEmpty() -> {
                    state = UniversalMathValueState(
                        UniversalMathValueStatus.MissingDependency,
                        diagnostic = "Missing dependencies: ${missing.sorted().joinToString()}",
                    )
                    cacheHit = false
                }
                id in cycleIds -> {
                    val cycle = validation.cycles.firstOrNull { id in it }.orEmpty()
                    state = UniversalMathValueState(
                        UniversalMathValueStatus.CyclicDependency,
                        diagnostic = "Dependency cycle: ${cycle.joinToString(" -> ")}",
                    )
                    cacheHit = false
                }
                else -> {
                    val unavailable = value.dependencies.mapNotNull { dependency ->
                        states[dependency]?.takeUnless(UniversalMathValueState::usable)?.let { dependency to it.status }
                    }
                    if (unavailable.isNotEmpty()) {
                        state = UniversalMathValueState(
                            UniversalMathValueStatus.Undefined,
                            diagnostic = "Unavailable dependencies: ${unavailable.joinToString { "${it.first} (${it.second.name})" }}",
                        )
                        cacheHit = false
                    } else if (evaluatedSignatures[id] == signature) {
                        state = states.getValue(id)
                        cacheHit = true
                    } else {
                        state = runCatching { evaluator.evaluate(value, value.dependencies.associateWith { states.getValue(it) }) }
                            .getOrElse { error -> UniversalMathValueState(UniversalMathValueStatus.NumericallyUnstable, diagnostic = error.message ?: "Evaluation failed") }
                        cacheHit = false
                    }
                }
            }
            states[id] = state
            evaluatedSignatures[id] = signature
            evaluations += UniversalObjectEvaluation(id, state.status, nanoTime() - before, cacheHit, state.diagnostic)
        }

        val updatedObjects = document.objects.mapValues { (id, value) -> value.copy(valueState = states.getValue(id)) }
        return UniversalRecomputeReport(
            document = document.copy(objects = updatedObjects),
            requestedObjects = requested,
            affectedObjects = affected,
            evaluationOrder = order,
            evaluations = evaluations,
            diagnostics = evaluations.mapNotNull { record -> record.diagnostic?.let { "${record.objectId}: $it" } },
            totalDurationNanos = nanoTime() - started,
        )
    }

    fun clearCache() = evaluatedSignatures.clear()

    private fun signature(document: UniversalMathDocument, value: UniversalMathObject, states: Map<String, UniversalMathValueState>): String = buildString {
        append(document.id).append('|').append(value.id).append('|').append(value.objectRevision).append('|').append(value.definition)
        value.dependencies.sorted().forEach { dependency ->
            val parent = document.objects[dependency]
            append('|').append(dependency).append(':').append(parent?.objectRevision).append(':').append(states[dependency])
        }
    }

    companion object {
        private fun defaultEvaluation(value: UniversalMathObject, dependencies: Map<String, UniversalMathValueState>): UniversalMathValueState {
            require(dependencies.values.all(UniversalMathValueState::usable))
            return when (val payload = value.payload) {
                is UniversalMathPayload.Symbolic -> if (payload.ast == null) {
                    UniversalMathValueState(UniversalMathValueStatus.ParseError, diagnostic = payload.parseError ?: "Expression could not be parsed")
                } else value.valueState.takeIf { it.usable } ?: UniversalMathValueState()
                is UniversalMathPayload.Coordinates -> if (payload.values.all(Double::isFinite)) value.valueState.copy(status = UniversalMathValueStatus.Valid, diagnostic = null)
                    else UniversalMathValueState(UniversalMathValueStatus.NumericallyUnstable, diagnostic = "Coordinates must be finite")
                is UniversalMathPayload.Dataset -> if (payload.values.withIndex().all { (index, number) -> number.isFinite() || index in payload.missingIndices }) value.valueState.copy(status = UniversalMathValueStatus.Valid, diagnostic = null)
                    else UniversalMathValueState(UniversalMathValueStatus.NumericallyUnstable, diagnostic = "Dataset contains an unmarked non-finite value")
                is UniversalMathPayload.Properties -> value.valueState.takeIf { it.usable } ?: UniversalMathValueState()
            }
        }
    }
}
