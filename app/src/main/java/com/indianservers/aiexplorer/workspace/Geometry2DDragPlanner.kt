package com.indianservers.aiexplorer.workspace

/**
 * Plans a body drag without accidentally translating an unrelated shape that happens to reuse
 * the same free point record. Formal dependency and constraint points remain shared.
 */
data class Geometry2DDragPlan(
    val state: WorkspaceState,
    val selectedShapeIndices: Set<Int>,
    val movablePointIndices: List<Int>,
    val detachedPointIndices: Map<Int, Int>,
) {
    val detached: Boolean get() = detachedPointIndices.isNotEmpty()
}

object Geometry2DDragPlanner {
    fun eligibleHandleIndices(state: WorkspaceState, selectedShapeIndices: Set<Int>): Set<Int> {
        val selected = selectedShapeIndices
            .filterTo(linkedSetOf()) { it in state.shapes.indices }
            .flatMapTo(linkedSetOf()) { state.shapes[it].pointIndices }
        val owned = state.shapes.filter { it.visible }.flatMapTo(linkedSetOf()) { it.pointIndices }
        val orphaned = state.points.indices.filterNotTo(linkedSetOf(), owned::contains)
        return (selected + orphaned).filterTo(linkedSetOf()) { it in state.points.indices }
    }

    fun plan(state: WorkspaceState, pressedShapeIndex: Int, currentSelection: Set<Int>): Geometry2DDragPlan {
        val pressed = state.shapes.getOrNull(pressedShapeIndex)
            ?: return Geometry2DDragPlan(state, emptySet(), emptyList(), emptyMap())
        if (pressed.locked) return Geometry2DDragPlan(state, setOf(pressedShapeIndex), emptyList(), emptyMap())

        val initial = if (pressedShapeIndex in currentSelection) {
            currentSelection.filterTo(linkedSetOf()) { it in state.shapes.indices }
        } else {
            linkedSetOf(pressedShapeIndex)
        }
        val selectedIds = initial.mapTo(linkedSetOf()) { state.shapes[it].id }
        val matchedGroups = state.geometryGroups.filter { group -> group.shapeIds.any(selectedIds::contains) }
        val groupedIds = matchedGroups
            .flatMapTo(linkedSetOf()) { it.shapeIds }
        if (matchedGroups.any { it.locked }) {
            val groupSelection = (initial + state.shapes.indices.filter { state.shapes[it].id in groupedIds })
                .filterTo(linkedSetOf()) { it in state.shapes.indices }
            return Geometry2DDragPlan(state, groupSelection, emptyList(), emptyMap())
        }
        val selected = (initial + state.shapes.indices.filter { state.shapes[it].id in groupedIds })
            .filterTo(linkedSetOf()) { !state.shapes[it].locked }
        if (selected.isEmpty()) return Geometry2DDragPlan(state, initial, emptyList(), emptyMap())

        val selectedPointIndices = selected.flatMap { state.shapes[it].pointIndices }.distinct()
        val unselectedPointIndices = state.shapes.indices
            .filterNot(selected::contains)
            .flatMapTo(linkedSetOf()) { state.shapes[it].pointIndices }
        val protected = buildSet {
            state.pointDependencies.forEach { add(it.outputIndex); addAll(it.inputIndices) }
            state.geometryConstraints.forEach { addAll(it.pointIndices) }
        }
        val detachable = selectedPointIndices.filterTo(linkedSetOf()) {
            it in unselectedPointIndices && it !in protected && it in state.points.indices
        }
        if (detachable.isEmpty()) {
            val movable = selectedPointIndices.filter { index ->
                index in state.points.indices && state.pointDependencies.none { it.outputIndex == index }
            }
            return Geometry2DDragPlan(state, selected, movable, emptyMap())
        }

        val points = state.points.toMutableList()
        val remap = detachable.associateWith { original ->
            points.size.also { points += state.points[original] }
        }
        val shapes = state.shapes.mapIndexed { index, shape ->
            if (index !in selected) shape else shape.copy(pointIndices = shape.pointIndices.map { remap[it] ?: it })
        }
        val detachedState = state.copy(points = points, shapes = shapes, modifiedAt = System.currentTimeMillis())
        val movable = selected.flatMap { shapes[it].pointIndices }.distinct().filter { index ->
            index in detachedState.points.indices && detachedState.pointDependencies.none { it.outputIndex == index }
        }
        return Geometry2DDragPlan(detachedState, selected, movable, remap)
    }
}
