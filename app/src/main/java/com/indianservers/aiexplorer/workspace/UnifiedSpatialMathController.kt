package com.indianservers.aiexplorer.workspace

import com.indianservers.aiexplorer.core.ExpressionEngine
import com.indianservers.aiexplorer.core.InteractiveParameterEngine
import com.indianservers.aiexplorer.core.Solid
import com.indianservers.aiexplorer.core.SpatialSurfaceLayer
import com.indianservers.aiexplorer.core.Vec3
import com.indianservers.aiexplorer.core.Vector3D
import com.indianservers.aiexplorer.spatial.SurfaceDefinition3D

data class SurfaceTableCell(val x: Double, val y: Double, val z: Double?, val diagnostic: String? = null)

data class SpatialSolverHandoff(
    val sourceObjectIds: Set<String>,
    val query: String,
    val assumptions: List<String>,
    val provenance: String,
)

data class UnifiedSpatialSnapshot(
    val state: WorkspaceState,
    val document: UniversalMathDocument,
    val recompute: UniversalRecomputeReport,
    val parameters: List<MathParameterRow>,
    val parameterValues: Map<String, Double>,
    val surfaceTable: List<SurfaceTableCell>,
    val spatialSchema: SharedSpatialMathSchema = SharedSpatialMathSchema(),
)

sealed interface UnifiedSpatialMutation {
    data class Applied(val snapshot: UnifiedSpatialSnapshot, val affectedObjects: Set<String>) : UnifiedSpatialMutation
    data class Rejected(val message: String) : UnifiedSpatialMutation
}

/** Shared write and projection boundary for 3D Geometry, 3D Graphing, tables, sliders and Solver. */
class UnifiedSpatialMathController(
    private val engine: UniversalMathDocumentEngine = UniversalMathDocumentEngine(),
    private val runtime: UniversalMathRuntime = UniversalMathRuntime(),
    private val expressions: ExpressionEngine = ExpressionEngine(),
    private val spatialMath: SharedSpatialMathEngine = SharedSpatialMathEngine(expressions),
) {
    fun snapshot(
        state: WorkspaceState,
        parameterValues: Map<String, Double> = emptyMap(),
        axes: List<Double> = listOf(-2.0, -1.0, 0.0, 1.0, 2.0),
    ): UnifiedSpatialSnapshot {
        val document = UniversalWorkspaceBridge.fromWorkspace(state)
        val report = runtime.recompute(document)
        return build(UniversalWorkspaceBridge.applyToWorkspace(report.document, state), report.document, report, parameterValues, axes)
    }

    fun editSurface(snapshot: UnifiedSpatialSnapshot, expression: String): UnifiedSpatialMutation =
        apply(snapshot, engine.editSymbolic(snapshot.document, "surface-main", expression))

    fun stageSurface(snapshot: UnifiedSpatialSnapshot, expression: String): UnifiedSpatialMutation =
        apply(snapshot, engine.stageSymbolicEdit(snapshot.document, "surface-main", expression))

    fun replaceSurfaceLayers(snapshot: UnifiedSpatialSnapshot, layers: List<SpatialSurfaceLayer>): UnifiedSpatialMutation {
        val normalized = layers.mapIndexed { index, layer ->
            val id = if (index == 0) "surface-main" else layer.id.trim().ifBlank { "surface-layer-$index" }
            layer.copy(id = id, opacity = layer.opacity.coerceIn(0.0, 1.0))
        }
        if (normalized.map { it.id }.distinct().size != normalized.size) {
            return UnifiedSpatialMutation.Rejected("Every 3D surface layer needs a unique ID")
        }
        val now = System.currentTimeMillis()
        val stagedState = snapshot.state.copy(
            surfaceExpression = normalized.firstOrNull()?.expression ?: "0",
            surfaceLayers = normalized,
            modifiedAt = now,
            universalMathDocument = null,
        )
        val currentProjection = UniversalWorkspaceBridge.fromWorkspace(snapshot.state.copy(universalMathDocument = null))
        val nextProjection = UniversalWorkspaceBridge.fromWorkspace(stagedState)
        val customObjects = snapshot.document.objects.filterKeys { it !in currentProjection.objects }
        val document = nextProjection.copy(
            revision = maxOf(snapshot.document.revision + 1, now),
            objects = nextProjection.objects + customObjects,
            modifiedAt = now,
        )
        val validation = engine.validate(document)
        if (!validation.valid) return UnifiedSpatialMutation.Rejected(validation.diagnostics.joinToString("; "))
        val report = runtime.recompute(document)
        val state = UniversalWorkspaceBridge.applyToWorkspace(report.document, stagedState)
        val changed = (currentProjection.objects.keys + nextProjection.objects.keys)
            .filterTo(linkedSetOf()) { id -> id.startsWith("surface-") || id == "spatial-scene" }
        return UnifiedSpatialMutation.Applied(rebuild(state, report.document, report, snapshot.parameterValues), changed)
    }

    fun setParameter(snapshot: UnifiedSpatialSnapshot, name: String, value: Double): UnifiedSpatialMutation {
        val parameter = snapshot.parameters.firstOrNull { it.name == name } ?: return UnifiedSpatialMutation.Rejected("Unknown surface parameter $name")
        if (!value.isFinite() || value !in parameter.min..parameter.max) return UnifiedSpatialMutation.Rejected("$name must be between ${parameter.min} and ${parameter.max}")
        val values = snapshot.parameterValues + (name to value)
        return UnifiedSpatialMutation.Applied(rebuild(snapshot.state, snapshot.document, snapshot.recompute, values), setOf("surface-main"))
    }

    fun updateSolid(snapshot: UnifiedSpatialSnapshot, index: Int, transform: (Solid) -> Solid): UnifiedSpatialMutation {
        val currentSolid = snapshot.state.solids.getOrNull(index) ?: return UnifiedSpatialMutation.Rejected("Unknown solid $index")
        val updated = transform(currentSolid)
        if (listOf(updated.width, updated.height, updated.depth, updated.radius, updated.topRadius).any { !it.isFinite() || it <= 0.0 }) {
            return UnifiedSpatialMutation.Rejected("Solid dimensions must be positive and finite")
        }
        val id = "solid-$index"
        val current = snapshot.document.objects[id] ?: return UnifiedSpatialMutation.Rejected("Missing authoritative object $id")
        val payload = UniversalMathPayload.Properties(mapOf(
            "type" to updated.type.name, "width" to updated.width.toString(), "height" to updated.height.toString(),
            "depth" to updated.depth.toString(), "radius" to updated.radius.toString(), "topRadius" to updated.topRadius.toString(),
            "position" to updated.position.csv(), "rotation" to updated.rotation.csv(),
        ))
        return apply(snapshot, engine.upsert(snapshot.document, current.copy(payload = payload, definition = UniversalMathDefinition.Properties(updated.type.name))))
    }

    fun updateVector(snapshot: UnifiedSpatialSnapshot, vectorId: String, transform: (Vector3D) -> Vector3D): UnifiedSpatialMutation {
        val vector = snapshot.state.vectors3D.firstOrNull { it.id == vectorId } ?: return UnifiedSpatialMutation.Rejected("Unknown vector $vectorId")
        val updated = transform(vector)
        val values = listOf(updated.start.x, updated.start.y, updated.start.z, updated.end.x, updated.end.y, updated.end.z)
        if (values.any { !it.isFinite() }) return UnifiedSpatialMutation.Rejected("Vector coordinates must be finite")
        val id = "vector-$vectorId"
        val current = snapshot.document.objects[id] ?: return UnifiedSpatialMutation.Rejected("Missing authoritative object $id")
        return apply(snapshot, engine.upsert(snapshot.document, current.copy(
            payload = UniversalMathPayload.Coordinates(values, listOf("x1", "y1", "z1", "x2", "y2", "z2")),
            definition = UniversalMathDefinition.Coordinates(6),
            valueState = UniversalMathValueState(values = mapOf(
                "magnitude" to UniversalExactApproxValue(decimal = updated.magnitude, provenance = "3D vector components", verification = UniversalVerificationStatus.Exact),
            )),
        )))
    }

    fun solverHandoff(snapshot: UnifiedSpatialSnapshot, objectIds: Set<String>, operation: String): Result<SpatialSolverHandoff> = runCatching {
        require(objectIds.isNotEmpty()) { "Select at least one 3D maths object" }
        val objects = objectIds.map { snapshot.document.objects[it] ?: error("Unknown maths object $it") }
        val definitions = objects.joinToString("; ") { value -> when (val definition = value.definition) {
            is UniversalMathDefinition.Symbolic -> definition.source
            is UniversalMathDefinition.Construction -> "${definition.command}(${definition.arguments.joinToString()})"
            is UniversalMathDefinition.Coordinates -> (value.payload as? UniversalMathPayload.Coordinates)?.values?.joinToString(prefix = "(", postfix = ")") ?: value.name
            is UniversalMathDefinition.Properties -> "${value.name}{${(value.payload as? UniversalMathPayload.Properties)?.entries?.entries?.joinToString { "${it.key}=${it.value}" }}}"
        } }
        SpatialSolverHandoff(
            sourceObjectIds = objectIds,
            query = "$operation $definitions",
            assumptions = objects.flatMap { it.assumptions.variables.values }.map { assumption -> "${assumption.variable} is ${assumption.domain.name.lowercase()}" }.distinct(),
            provenance = "UnifiedMathDocument ${snapshot.document.id} revision ${snapshot.document.revision}",
        )
    }

    private fun apply(snapshot: UnifiedSpatialSnapshot, mutation: UniversalMutationResult): UnifiedSpatialMutation = when (mutation) {
        is UniversalMutationResult.Applied -> {
            val report = runtime.recompute(mutation.document, mutation.affectedObjects)
            val state = UniversalWorkspaceBridge.applyToWorkspace(report.document, snapshot.state)
            UnifiedSpatialMutation.Applied(rebuild(state, report.document, report, snapshot.parameterValues), mutation.affectedObjects)
        }
        is UniversalMutationResult.Conflict -> UnifiedSpatialMutation.Rejected(mutation.message)
        is UniversalMutationResult.Rejected -> UnifiedSpatialMutation.Rejected((listOf(mutation.message) + mutation.diagnostics).joinToString("; "))
    }

    private fun rebuild(state: WorkspaceState, document: UniversalMathDocument, report: UniversalRecomputeReport, values: Map<String, Double>) =
        build(state, document, report, values, listOf(-2.0, -1.0, 0.0, 1.0, 2.0))

    private fun build(state: WorkspaceState, document: UniversalMathDocument, report: UniversalRecomputeReport, values: Map<String, Double>, axes: List<Double>): UnifiedSpatialSnapshot {
        val surface = (document.objects["surface-main"]?.payload as? UniversalMathPayload.Symbolic)?.source.orEmpty()
        val parameters = InteractiveParameterEngine.discover(listOf(surface), values, independentVariables = setOf("x", "y")).map {
            MathParameterRow("param-${it.name}", it.name, it.value, it.minimum, it.maximum, it.step)
        }
        val resolvedValues = parameters.associate { it.name to it.value }
        val resolved = InteractiveParameterEngine.resolve(surface, resolvedValues, independentVariables = setOf("x", "y"))
        val compiled = runCatching { expressions.compile(resolved) }.getOrNull()
        val table = axes.flatMap { x -> axes.map { y ->
            val z = compiled?.let { runCatching { it.eval(mapOf("x" to x, "y" to y)) }.getOrNull() }?.takeIf(Double::isFinite)
            SurfaceTableCell(x, y, z, if (z == null) "Undefined or outside the surface domain" else null)
        } }
        val definition = SurfaceDefinition3D.Explicit("surface-main", resolved)
        return UnifiedSpatialSnapshot(state, document, report, parameters, resolvedValues, table, spatialMath.schema(listOf(definition)))
    }

    private fun Vec3.csv() = "$x,$y,$z"
}
