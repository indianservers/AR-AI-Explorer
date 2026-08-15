package com.indianservers.aiexplorer.workspace

import com.indianservers.aiexplorer.core.ExpressionEngine
import com.indianservers.aiexplorer.core.GraphAnalysis
import com.indianservers.aiexplorer.core.Vec2

enum class Unified2DView { Algebra, Geometry, Graph, Table }

data class Unified2DSelection(val objectIds: Set<String> = emptySet()) {
    val primaryId: String? get() = objectIds.lastOrNull()
}

data class Unified2DObjectProjection(
    val id: String,
    val name: String,
    val kind: UniversalMathKind,
    val definition: UniversalMathDefinition,
    val valueState: UniversalMathValueState,
    val presentation: UniversalMathPresentation,
    val views: Set<Unified2DView>,
    val selected: Boolean,
)

data class Unified2DSnapshot(
    val state: WorkspaceState,
    val document: UniversalMathDocument,
    val objects: List<Unified2DObjectProjection>,
    val selection: Unified2DSelection,
    val recompute: UniversalRecomputeReport,
) {
    fun objectsFor(view: Unified2DView) = objects.filter { view in it.views }
}

sealed interface Unified2DMutation {
    data class Applied(val snapshot: Unified2DSnapshot, val affectedObjects: Set<String>) : Unified2DMutation
    data class Rejected(val message: String) : Unified2DMutation
}

/** Single write boundary shared by the 2D Geometry and Graph workspaces. */
class Unified2DMathController(
    private val documentEngine: UniversalMathDocumentEngine = UniversalMathDocumentEngine(),
    private val runtime: UniversalMathRuntime = UniversalMathRuntime(),
    private val expressions: ExpressionEngine = ExpressionEngine(),
) {
    private val graph = GraphAnalysis(expressions)

    fun snapshot(state: WorkspaceState, selection: Unified2DSelection = Unified2DSelection()): Unified2DSnapshot {
        val document = GeometryGraphEquationProjector.project(state, UniversalWorkspaceBridge.fromWorkspace(state))
        val recompute = runtime.recompute(document)
        val projected = UniversalWorkspaceBridge.applyToWorkspace(recompute.document, state)
        return buildSnapshot(projected, recompute.document, selection, recompute)
    }

    fun select(snapshot: Unified2DSnapshot, id: String?, additive: Boolean = false): Unified2DSnapshot {
        val valid = id?.takeIf(snapshot.document.objects::containsKey)
        val ids = when {
            valid == null -> emptySet()
            additive && valid in snapshot.selection.objectIds -> snapshot.selection.objectIds - valid
            additive -> snapshot.selection.objectIds + valid
            else -> setOf(valid)
        }
        return snapshot.copy(
            selection = Unified2DSelection(ids),
            objects = snapshot.objects.map { it.copy(selected = it.id in ids) },
        )
    }

    fun editCoordinates(snapshot: Unified2DSnapshot, pointId: String, position: Vec2): Unified2DMutation {
        val pointIndex = pointId.removePrefix("point-").toIntOrNull()
            ?: return Unified2DMutation.Rejected("$pointId is not a 2D workspace point")
        if (snapshot.state.pointDependencies.any { it.outputIndex == pointIndex }) {
            return Unified2DMutation.Rejected("Dependent points must be edited through their construction")
        }
        return applyMutation(snapshot, documentEngine.editCoordinates(snapshot.document, pointId, listOf(position.x, position.y)))
    }

    /** Commits a completed multi-point drag/transform atomically after preview rendering finishes. */
    fun transformPoints(snapshot: Unified2DSnapshot, positions: Map<String, Vec2>): Unified2DMutation {
        if (positions.isEmpty()) return Unified2DMutation.Rejected("Select at least one point to transform")
        val updates = positions.map { (pointId, position) ->
            val pointIndex = pointId.removePrefix("point-").toIntOrNull()
                ?: return Unified2DMutation.Rejected("$pointId is not a 2D workspace point")
            if (!position.x.isFinite() || !position.y.isFinite()) return Unified2DMutation.Rejected("Point coordinates must be finite")
            if (snapshot.state.pointDependencies.any { it.outputIndex == pointIndex }) {
                return Unified2DMutation.Rejected("Dependent point $pointId must be transformed through its construction")
            }
            val current = snapshot.document.objects[pointId]
                ?: return Unified2DMutation.Rejected("Unknown maths object $pointId")
            val payload = current.payload as? UniversalMathPayload.Coordinates
                ?: return Unified2DMutation.Rejected("$pointId does not contain coordinates")
            current.copy(
                payload = payload.copy(values = listOf(position.x, position.y)),
                definition = UniversalMathDefinition.Coordinates(2, payload.definition),
                valueState = UniversalMathValueState(),
            )
        }
        return applyMutation(snapshot, documentEngine.upsertBatch(snapshot.document, updates))
    }

    fun editExpression(snapshot: Unified2DSnapshot, functionId: String, source: String): Unified2DMutation =
        applyMutation(snapshot, documentEngine.editSymbolic(snapshot.document, functionId, source))

    fun stageExpression(snapshot: Unified2DSnapshot, functionId: String, source: String): Unified2DMutation =
        applyMutation(snapshot, documentEngine.stageSymbolicEdit(snapshot.document, functionId, source))

    fun updatePresentation(
        snapshot: Unified2DSnapshot,
        objectId: String,
        transform: (UniversalMathPresentation) -> UniversalMathPresentation,
    ): Unified2DMutation {
        val current = snapshot.document.objects[objectId] ?: return Unified2DMutation.Rejected("Unknown maths object $objectId")
        return applyMutation(snapshot, documentEngine.upsert(snapshot.document, current.copy(presentation = transform(current.presentation))))
    }

    fun createGraphIntersectionPoint(
        snapshot: Unified2DSnapshot,
        firstFunctionId: String,
        secondFunctionId: String,
        branch: Int = 0,
        minimumX: Double = -10.0,
        maximumX: Double = 10.0,
    ): Unified2DMutation {
        val first = symbolicExpression(snapshot.document, firstFunctionId) ?: return Unified2DMutation.Rejected("Unknown graph function $firstFunctionId")
        val second = symbolicExpression(snapshot.document, secondFunctionId) ?: return Unified2DMutation.Rejected("Unknown graph function $secondFunctionId")
        val intersections = runCatching { graph.intersections(expressions.compile(first), expressions.compile(second), minimumX, maximumX) }
            .getOrElse { return Unified2DMutation.Rejected(it.message ?: "Could not calculate graph intersections") }
        val point = intersections.getOrNull(branch) ?: return Unified2DMutation.Rejected("No intersection exists on the selected branch")
        val pointIndex = snapshot.state.points.size
        val pointId = "point-$pointIndex"
        val addedState = snapshot.state.copy(points = snapshot.state.points + point)
        val baseDocument = UniversalWorkspaceBridge.fromWorkspace(addedState)
        val basePoint = baseDocument.objects.getValue(pointId)
        val linkedPoint = basePoint.copy(
            name = "I${pointIndex + 1}",
            dependencies = setOf(firstFunctionId, secondFunctionId),
            definition = UniversalMathDefinition.Construction("Intersect", listOf(firstFunctionId, secondFunctionId, branch.toString())),
            valueState = UniversalMathValueState(values = mapOf(
                "x" to UniversalExactApproxValue(decimal = point.x, provenance = "graph intersection", verification = UniversalVerificationStatus.Numerical),
                "y" to UniversalExactApproxValue(decimal = point.y, provenance = "graph intersection", verification = UniversalVerificationStatus.Numerical),
            )),
        )
        val mutation = documentEngine.upsert(baseDocument, linkedPoint)
        val baseSnapshot = snapshot.copy(state = addedState, document = baseDocument)
        return applyMutation(baseSnapshot, mutation, selection = Unified2DSelection(setOf(pointId)))
    }

    private fun applyMutation(
        snapshot: Unified2DSnapshot,
        mutation: UniversalMutationResult,
        selection: Unified2DSelection = snapshot.selection,
    ): Unified2DMutation = when (mutation) {
        is UniversalMutationResult.Applied -> {
            val report = runtime.recompute(mutation.document, mutation.affectedObjects)
            val state = UniversalWorkspaceBridge.applyToWorkspace(report.document, snapshot.state)
            val refreshed = GeometryGraphEquationProjector.project(state, report.document)
            val derivedIds = refreshed.objects.values.filter { it.dependencies.any(mutation.affectedObjects::contains) }.map { it.id }.toSet()
            val refreshedReport = runtime.recompute(refreshed, mutation.affectedObjects + derivedIds)
            val projectedState = UniversalWorkspaceBridge.applyToWorkspace(refreshedReport.document, state)
            Unified2DMutation.Applied(buildSnapshot(projectedState, refreshedReport.document, selection, refreshedReport), mutation.affectedObjects + derivedIds)
        }
        is UniversalMutationResult.Conflict -> Unified2DMutation.Rejected(mutation.message)
        is UniversalMutationResult.Rejected -> Unified2DMutation.Rejected((listOf(mutation.message) + mutation.diagnostics).joinToString("; "))
    }

    private fun buildSnapshot(
        state: WorkspaceState,
        document: UniversalMathDocument,
        selection: Unified2DSelection,
        report: UniversalRecomputeReport,
    ) = Unified2DSnapshot(
        state, document,
        document.objects.values.filter { it.kind in supportedKinds }.sortedBy { it.id }.map { value ->
            Unified2DObjectProjection(
                value.id, value.name, value.kind, value.definition, value.valueState, value.presentation,
                views = viewsFor(value.kind), selected = value.id in selection.objectIds,
            )
        },
        selection, report,
    )

    private fun symbolicExpression(document: UniversalMathDocument, id: String) =
        (document.objects[id]?.payload as? UniversalMathPayload.Symbolic)?.source

    companion object {
        private val supportedKinds = setOf(
            UniversalMathKind.Function, UniversalMathKind.PiecewiseFunction, UniversalMathKind.Equation, UniversalMathKind.Relation, UniversalMathKind.Point2D,
            UniversalMathKind.Line, UniversalMathKind.Ray, UniversalMathKind.Segment, UniversalMathKind.Circle,
            UniversalMathKind.Conic, UniversalMathKind.GeometryConstruction, UniversalMathKind.Measurement,
            UniversalMathKind.Angle, UniversalMathKind.Boolean,
        )

        private fun viewsFor(kind: UniversalMathKind): Set<Unified2DView> = when (kind) {
            UniversalMathKind.Function, UniversalMathKind.PiecewiseFunction -> setOf(Unified2DView.Algebra, Unified2DView.Graph, Unified2DView.Table)
            UniversalMathKind.Equation -> setOf(Unified2DView.Algebra, Unified2DView.Graph)
            UniversalMathKind.Relation -> setOf(Unified2DView.Algebra, Unified2DView.Geometry, Unified2DView.Graph)
            UniversalMathKind.Point2D, UniversalMathKind.Line, UniversalMathKind.Ray, UniversalMathKind.Segment,
            UniversalMathKind.Circle, UniversalMathKind.Conic -> setOf(Unified2DView.Algebra, Unified2DView.Geometry, Unified2DView.Graph)
            UniversalMathKind.GeometryConstruction, UniversalMathKind.Measurement, UniversalMathKind.Angle, UniversalMathKind.Boolean -> setOf(Unified2DView.Algebra, Unified2DView.Geometry)
            else -> emptySet()
        }
    }
}
