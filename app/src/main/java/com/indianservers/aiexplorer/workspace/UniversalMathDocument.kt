package com.indianservers.aiexplorer.workspace

import com.indianservers.aiexplorer.core.ExactRational
import com.indianservers.aiexplorer.core.MathAssumptionSet
import com.indianservers.aiexplorer.core.MathNumberDomain
import com.indianservers.aiexplorer.core.MathNotebookDocument
import com.indianservers.aiexplorer.core.ManipulativeScene
import com.indianservers.aiexplorer.core.Solid
import com.indianservers.aiexplorer.core.SymbolicCasEngine
import com.indianservers.aiexplorer.core.SymbolicExpression
import com.indianservers.aiexplorer.core.VariableAssumption
import com.indianservers.aiexplorer.core.Vec2
import com.indianservers.aiexplorer.core.Vec3
import com.indianservers.aiexplorer.core.Vector3D
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64

enum class UniversalMathKind {
    Scalar, Parameter, Expression, Equation, EquationSystem, ParametricEquation, Inequality, Relation, Boolean, Text,
    Function, PiecewiseFunction, Point2D, Point3D, Vector,
    Line, Ray, Segment, Circle, Conic, Plane,
    Matrix, DataList, Sequence, Measurement, Angle,
    GeometryConstruction, ProbabilityModel, UnitMeasurement, Surface, Solid, Manipulative, NotebookCell, SpatialScene,
}

sealed interface UniversalMathPayload {
    /** A null AST represents a staged editor value; it remains visible but cannot enter verified computation. */
    data class Symbolic(val source: String, val ast: SymbolicExpression?, val parseError: String? = null) : UniversalMathPayload
    data class Coordinates(val values: List<Double>, val labels: List<String> = emptyList(), val definition: String? = null) : UniversalMathPayload
    data class Dataset(val values: List<Double>, val missingIndices: Set<Int> = emptySet()) : UniversalMathPayload
    data class Properties(val entries: Map<String, String>) : UniversalMathPayload
}

data class UniversalMathObject(
    val id: String,
    val kind: UniversalMathKind,
    val name: String,
    val payload: UniversalMathPayload,
    val dependencies: Set<String> = emptySet(),
    val assumptions: MathAssumptionSet = MathAssumptionSet(),
    val objectRevision: Long = 0,
    val sourceView: String = "workspace",
) {
    init {
        require(id.isNotBlank() && name.isNotBlank())
        require(id !in dependencies) { "Object $id cannot depend on itself" }
    }
}

/** Typed constructors used by calculator, solver, notebook, statistics, manipulatives and future subjects. */
object UniversalMathObjectFactory {
    private val cas = SymbolicCasEngine()

    fun scalar(id: String, name: String, exact: ExactRational, unit: String? = null) = UniversalMathObject(
        id, if (unit == null) UniversalMathKind.Scalar else UniversalMathKind.UnitMeasurement, name,
        UniversalMathPayload.Properties(mapOf("exact" to exact.toString(), "decimal" to exact.toDouble().toString()) + (unit?.let { mapOf("unit" to it) } ?: emptyMap())), sourceView = "calculator/CAS",
    )

    fun symbolic(id: String, kind: UniversalMathKind, name: String, source: String, assumptions: MathAssumptionSet = MathAssumptionSet(), dependencies: Set<String> = emptySet(), sourceView: String = "CAS"):
        UniversalMathObject {
        require(kind in setOf(UniversalMathKind.Expression, UniversalMathKind.Equation, UniversalMathKind.Inequality, UniversalMathKind.Boolean,
            UniversalMathKind.Function, UniversalMathKind.PiecewiseFunction, UniversalMathKind.Surface, UniversalMathKind.NotebookCell))
        val parsed = runCatching { cas.parse(source) }
        return UniversalMathObject(id, kind, name, UniversalMathPayload.Symbolic(source, parsed.getOrNull(), parsed.exceptionOrNull()?.message), dependencies, assumptions, sourceView = sourceView)
    }

    fun point2D(id: String, name: String, x: Double, y: Double, dependencies: Set<String> = emptySet()) = UniversalMathObject(id, UniversalMathKind.Point2D, name, UniversalMathPayload.Coordinates(listOf(x, y), listOf("x", "y")), dependencies, sourceView = "geometry")
    fun point3D(id: String, name: String, x: Double, y: Double, z: Double, dependencies: Set<String> = emptySet()) = UniversalMathObject(id, UniversalMathKind.Point3D, name, UniversalMathPayload.Coordinates(listOf(x, y, z), listOf("x", "y", "z")), dependencies, sourceView = "3D/AR")
    fun vector(id: String, name: String, components: List<Double>) = UniversalMathObject(id, UniversalMathKind.Vector, name, UniversalMathPayload.Coordinates(components, components.indices.map { "v$it" }), sourceView = "vector")

    fun matrix(id: String, name: String, rows: List<List<Double>>): UniversalMathObject {
        require(rows.isNotEmpty() && rows.map { it.size }.distinct().size == 1) { "Matrix must be non-empty and rectangular" }
        return UniversalMathObject(id, UniversalMathKind.Matrix, name, UniversalMathPayload.Properties(mapOf("rows" to rows.size.toString(), "columns" to rows.first().size.toString(), "values" to rows.flatten().joinToString(","))), sourceView = "CAS/matrix")
    }

    fun dataList(id: String, name: String, values: List<Double>, missing: Set<Int> = emptySet()) = UniversalMathObject(id, UniversalMathKind.DataList, name, UniversalMathPayload.Dataset(values, missing), sourceView = "spreadsheet/statistics")
    fun probability(id: String, name: String, distribution: String, parameters: Map<String, Double>) = UniversalMathObject(id, UniversalMathKind.ProbabilityModel, name, UniversalMathPayload.Properties(mapOf("distribution" to distribution) + parameters.mapValues { it.value.toString() }), sourceView = "probability")
    fun unitMeasurement(id: String, name: String, exact: ExactRational, unit: String, uncertainty: Double? = null) = UniversalMathObject(id, UniversalMathKind.UnitMeasurement, name, UniversalMathPayload.Properties(mapOf("exact" to exact.toString(), "unit" to unit) + (uncertainty?.let { mapOf("uncertainty" to it.toString()) } ?: emptyMap())), sourceView = "measurement")

    fun fromNotebook(document: MathNotebookDocument): List<UniversalMathObject> {
        val symbolIds = document.cells.mapNotNull { cell -> cell.symbol?.let { it to "notebook-${cell.id}" } }.toMap()
        return document.cells.map { cell ->
            symbolic("notebook-${cell.id}", UniversalMathKind.NotebookCell, cell.symbol ?: cell.id, cell.expression,
                dependencies = cell.dependencies.mapNotNull(symbolIds::get).toSet(), sourceView = "notebook")
        }
    }

    fun fromManipulatives(scene: ManipulativeScene): List<UniversalMathObject> = scene.items.map { item ->
        UniversalMathObject("manipulative-${item.id}", UniversalMathKind.Manipulative, item.label,
            UniversalMathPayload.Properties(mapOf("kind" to item.kind.name, "x" to item.position.x.toString(), "y" to item.position.y.toString(), "rotation" to item.rotationDegrees.toString(), "scale" to item.scale.toString(), "value" to item.value.toString())), sourceView = "manipulatives")
    }
}

data class UniversalMathDocument(
    val schemaVersion: Int = CURRENT_SCHEMA,
    val id: String = "universal-math-document",
    val revision: Long = 0,
    val objects: Map<String, UniversalMathObject> = emptyMap(),
    val modifiedAt: Long = 0,
) {
    fun objectByName(name: String) = objects.values.firstOrNull { it.name == name }
    fun dependentsOf(id: String, transitive: Boolean = true): Set<String> {
        val result = linkedSetOf<String>()
        var frontier = setOf(id)
        while (frontier.isNotEmpty()) {
            val next = objects.values.filter { candidate -> candidate.id !in result && candidate.dependencies.any(frontier::contains) }.map { it.id }.toSet()
            result += next
            if (!transitive) break
            frontier = next
        }
        return result
    }

    companion object { const val CURRENT_SCHEMA = 2 }
}

data class UniversalDocumentValidation(
    val valid: Boolean,
    val topologicalOrder: List<String>,
    val missingDependencies: Map<String, Set<String>>,
    val cycles: List<List<String>>,
    val diagnostics: List<String>,
)

sealed interface UniversalMutationResult {
    data class Applied(val document: UniversalMathDocument, val affectedObjects: Set<String>) : UniversalMutationResult
    data class Conflict(val expectedRevision: Long, val actualRevision: Long, val message: String) : UniversalMutationResult
    data class Rejected(val message: String, val diagnostics: List<String> = emptyList()) : UniversalMutationResult
}

class UniversalMathDocumentEngine(private val cas: SymbolicCasEngine = SymbolicCasEngine()) {
    fun create(id: String, objects: Iterable<UniversalMathObject>, now: Long = System.currentTimeMillis()): UniversalMathDocument {
        val map = objects.associateBy { it.id }
        val document = UniversalMathDocument(id = id, objects = map, modifiedAt = now)
        val validation = validate(document)
        require(validation.valid) { validation.diagnostics.joinToString("; ") }
        return document
    }

    fun upsert(document: UniversalMathDocument, value: UniversalMathObject, expectedRevision: Long = document.revision, now: Long = System.currentTimeMillis()): UniversalMutationResult {
        if (expectedRevision != document.revision) return UniversalMutationResult.Conflict(expectedRevision, document.revision, "The document changed; recompute the edit from revision ${document.revision}.")
        val current = document.objects[value.id]
        val candidateObject = value.copy(objectRevision = (current?.objectRevision ?: -1) + 1)
        val candidate = document.copy(revision = document.revision + 1, objects = document.objects + (value.id to candidateObject), modifiedAt = now)
        val validation = validate(candidate)
        if (!validation.valid) return UniversalMutationResult.Rejected("The edit would make the maths dependency graph invalid.", validation.diagnostics)
        return UniversalMutationResult.Applied(candidate, setOf(value.id) + candidate.dependentsOf(value.id))
    }

    fun editSymbolic(document: UniversalMathDocument, id: String, source: String, expectedRevision: Long = document.revision, now: Long = System.currentTimeMillis()): UniversalMutationResult {
        val current = document.objects[id] ?: return UniversalMutationResult.Rejected("Unknown maths object $id")
        if (current.payload !is UniversalMathPayload.Symbolic) return UniversalMutationResult.Rejected("$id is not a symbolic object")
        val parsed = runCatching { cas.parse(source) }.getOrElse { return UniversalMutationResult.Rejected(it.message ?: "Invalid mathematical expression") }
        val referencedNames = identifiers(parsed)
        val dependencies = document.objects.values.filter { it.id != id && (it.id in referencedNames || it.name.substringBefore('(') in referencedNames) }.map { it.id }.toSet()
        return upsert(document, current.copy(payload = UniversalMathPayload.Symbolic(source, parsed), dependencies = dependencies), expectedRevision, now)
    }

    fun editCoordinates(document: UniversalMathDocument, id: String, values: List<Double>, expectedRevision: Long = document.revision, now: Long = System.currentTimeMillis()): UniversalMutationResult {
        val current = document.objects[id] ?: return UniversalMutationResult.Rejected("Unknown maths object $id")
        val payload = current.payload as? UniversalMathPayload.Coordinates ?: return UniversalMutationResult.Rejected("$id does not contain coordinates")
        if (values.any { !it.isFinite() }) return UniversalMutationResult.Rejected("Coordinates must be finite")
        return upsert(document, current.copy(payload = payload.copy(values = values)), expectedRevision, now)
    }

    fun remove(document: UniversalMathDocument, id: String, expectedRevision: Long = document.revision, cascade: Boolean = false, now: Long = System.currentTimeMillis()): UniversalMutationResult {
        if (expectedRevision != document.revision) return UniversalMutationResult.Conflict(expectedRevision, document.revision, "The document changed before deletion.")
        if (id !in document.objects) return UniversalMutationResult.Rejected("Unknown maths object $id")
        val dependents = document.dependentsOf(id)
        if (dependents.isNotEmpty() && !cascade) return UniversalMutationResult.Rejected("$id is required by ${dependents.joinToString()}")
        val removed = setOf(id) + dependents.takeIf { cascade }.orEmpty()
        return UniversalMutationResult.Applied(document.copy(revision = document.revision + 1, objects = document.objects - removed, modifiedAt = now), removed)
    }

    fun validate(document: UniversalMathDocument): UniversalDocumentValidation {
        val missing = document.objects.values.mapNotNull { value ->
            val absent = value.dependencies - document.objects.keys
            if (absent.isEmpty()) null else value.id to absent
        }.toMap()
        val visiting = linkedSetOf<String>(); val visited = linkedSetOf<String>(); val order = mutableListOf<String>(); val cycles = mutableListOf<List<String>>()
        fun visit(id: String, path: List<String>) {
            if (id in visiting) { cycles += path.dropWhile { it != id } + id; return }
            if (id in visited) return
            visiting += id
            document.objects[id]?.dependencies?.filter(document.objects::containsKey)?.sorted()?.forEach { visit(it, path + id) }
            visiting -= id; visited += id; order += id
        }
        document.objects.keys.sorted().forEach { visit(it, emptyList()) }
        val diagnostics = buildList {
            missing.forEach { (id, deps) -> add("$id references missing objects: ${deps.joinToString()}") }
            cycles.forEach { add("Dependency cycle: ${it.joinToString(" → ")}") }
            document.objects.values.forEach { value ->
                val symbolic = value.payload as? UniversalMathPayload.Symbolic
                if (symbolic != null && symbolic.ast == null) add("${value.id} has staged invalid input: ${symbolic.parseError ?: "parse failed"}")
            }
            if (document.schemaVersion > UniversalMathDocument.CURRENT_SCHEMA) add("Document schema ${document.schemaVersion} is newer than supported schema ${UniversalMathDocument.CURRENT_SCHEMA}.")
        }
        return UniversalDocumentValidation(diagnostics.isEmpty(), order, missing, cycles, diagnostics)
    }

    private fun identifiers(expression: SymbolicExpression): Set<String> = buildSet {
        fun visit(node: SymbolicExpression) {
            when (node) {
                is SymbolicExpression.Variable -> add(node.name)
                is SymbolicExpression.UnaryMinus -> visit(node.value)
                is SymbolicExpression.Sum -> node.terms.forEach(::visit)
                is SymbolicExpression.Product -> node.factors.forEach(::visit)
                is SymbolicExpression.Power -> { visit(node.base); visit(node.exponent) }
                is SymbolicExpression.Function -> { add(node.name); node.arguments.forEach(::visit) }
                is SymbolicExpression.Number -> Unit
            }
        }
        visit(expression)
    }
}

/** Converts every current workspace object into one canonical typed document and applies supported edits back. */
object UniversalWorkspaceBridge {
    private val cas = SymbolicCasEngine()

    fun fromWorkspace(state: WorkspaceState): UniversalMathDocument {
        val objects = mutableListOf<UniversalMathObject>()
        val pointDependencyByOutput = state.pointDependencies.associateBy { it.outputIndex }
        state.points.forEachIndexed { index, point ->
            val dependency = pointDependencyByOutput[index]
            objects += UniversalMathObject(
                id = "point-$index", kind = UniversalMathKind.Point2D, name = dependency?.name ?: "P$index",
                payload = UniversalMathPayload.Coordinates(listOf(point.x, point.y), listOf("x", "y"), dependency?.let { pointDefinition(it, state) }),
                dependencies = dependency?.inputIndices?.map { "point-$it" }?.toSet().orEmpty(), sourceView = "geometry",
            )
        }
        val functionNames = state.functions.associate { it.name.substringBefore('(') to it.id }
        state.functions.forEach { function ->
            val parsed = runCatching { cas.parse(function.expression) }
            val ast = parsed.getOrNull()
            val dependencies = ast?.let(::symbolicIdentifiers).orEmpty().mapNotNull(functionNames::get).filterNot { it == function.id }.toSet()
            objects += UniversalMathObject(function.id, UniversalMathKind.Function, function.name, UniversalMathPayload.Symbolic(function.expression, ast, parsed.exceptionOrNull()?.message), dependencies, sourceView = "graph/CAS/table")
        }
        state.shapes.forEach { shape ->
            objects += UniversalMathObject(shape.id, shape.type.algebraKind(), shape.name,
                UniversalMathPayload.Properties(mapOf("definition" to shapeDefinition(shape, state), "type" to shape.type.name, "visible" to shape.visible.toString(), "locked" to shape.locked.toString(), "style" to shape.styleKey)),
                shape.pointIndices.map { "point-$it" }.toSet(), sourceView = "geometry")
        }
        state.geometryConstraints.forEach { constraint ->
            val kind = when (constraint.type) {
                GeometryConstraint2DType.FixedAngle -> UniversalMathKind.Angle
                GeometryConstraint2DType.FixedLength -> UniversalMathKind.Measurement
                else -> UniversalMathKind.Boolean
            }
            val definition = "${constraint.type.label}(${(constraint.shapeIds + constraint.pointIndices.map { "P$it" }).joinToString()})"
            objects += UniversalMathObject(constraint.id, kind, constraint.type.label,
                UniversalMathPayload.Properties(mapOf("definition" to definition, "value" to (constraint.target?.toString() ?: "satisfied"), "constraint" to constraint.type.name)),
                (constraint.shapeIds + constraint.pointIndices.map { "point-$it" }).toSet(), sourceView = "geometry/measurement")
        }
        state.solids.forEachIndexed { index, solid -> objects += solidObject(index, solid) }
        state.vectors3D.forEach { vector -> objects += vectorObject(vector) }
        val parsedSurface = runCatching { cas.parse(state.surfaceExpression) }
        objects += UniversalMathObject("surface-main", UniversalMathKind.Surface, "z=f(x,y)", UniversalMathPayload.Symbolic(state.surfaceExpression, parsedSurface.getOrNull(), parsedSurface.exceptionOrNull()?.message), sourceView = "3D graph")
        objects += UniversalMathObject("spatial-scene", UniversalMathKind.SpatialScene, "AR spatial scene", UniversalMathPayload.Properties(mapOf(
            "anchorId" to state.spatialPlacement.anchorId, "scale" to state.spatialPlacement.pose.uniformScale.toString(), "metersPerUnit" to state.spatialPlacement.metersPerMathUnit.toString(),
        )), dependencies = (state.solids.indices.map { "solid-$it" } + state.vectors3D.map { "vector-${it.id}" } + "surface-main").toSet(), sourceView = "AR")
        val generated = UniversalMathDocument(id = "math-${state.id}", revision = state.modifiedAt.coerceAtLeast(0), objects = objects.associateBy { it.id }, modifiedAt = state.modifiedAt)
        val stored = state.universalMathDocument ?: return generated
        return stored.copy(revision = maxOf(stored.revision, generated.revision), objects = stored.objects + generated.objects, modifiedAt = maxOf(stored.modifiedAt, generated.modifiedAt))
    }

    fun applyToWorkspace(document: UniversalMathDocument, state: WorkspaceState): WorkspaceState {
        val validation = UniversalMathDocumentEngine().validate(document)
        require(validation.valid) { validation.diagnostics.joinToString("; ") }
        val functions = state.functions.map { function ->
            val payload = document.objects[function.id]?.payload as? UniversalMathPayload.Symbolic
            if (payload == null) function else function.copy(expression = payload.source)
        }
        val points = state.points.mapIndexed { index, point ->
            val coordinates = document.objects["point-$index"]?.payload as? UniversalMathPayload.Coordinates
            coordinates?.values?.takeIf { it.size >= 2 }?.let { Vec2(it[0], it[1]) } ?: point
        }
        val surface = (document.objects["surface-main"]?.payload as? UniversalMathPayload.Symbolic)?.source ?: state.surfaceExpression
        return state.copy(functions = functions, points = points, surfaceExpression = surface, universalMathDocument = document, modifiedAt = document.modifiedAt).recomputed()
    }

    private fun solidObject(index: Int, solid: Solid) = UniversalMathObject("solid-$index", UniversalMathKind.Solid, solid.type.name,
        UniversalMathPayload.Properties(mapOf("type" to solid.type.name, "width" to solid.width.toString(), "height" to solid.height.toString(), "depth" to solid.depth.toString(), "radius" to solid.radius.toString(), "position" to solid.position.csv(), "rotation" to solid.rotation.csv())), sourceView = "3D geometry")

    private fun vectorObject(vector: Vector3D) = UniversalMathObject("vector-${vector.id}", UniversalMathKind.Vector, vector.name,
        UniversalMathPayload.Coordinates(listOf(vector.start.x, vector.start.y, vector.start.z, vector.end.x, vector.end.y, vector.end.z), listOf("x1", "y1", "z1", "x2", "y2", "z2")), sourceView = "3D/vector")

    private fun symbolicIdentifiers(expression: SymbolicExpression): Set<String> = buildSet {
        fun visit(node: SymbolicExpression) { when (node) {
            is SymbolicExpression.Variable -> add(node.name)
            is SymbolicExpression.UnaryMinus -> visit(node.value)
            is SymbolicExpression.Sum -> node.terms.forEach(::visit)
            is SymbolicExpression.Product -> node.factors.forEach(::visit)
            is SymbolicExpression.Power -> { visit(node.base); visit(node.exponent) }
            is SymbolicExpression.Function -> { add(node.name); node.arguments.forEach(::visit) }
            is SymbolicExpression.Number -> Unit
        } }
        visit(expression)
    }

    private fun Vec3.csv() = "$x,$y,$z"

    private fun Shape2DType.algebraKind() = when (this) {
        Shape2DType.Line, Shape2DType.Parallel, Shape2DType.Perpendicular, Shape2DType.AngleBisector -> UniversalMathKind.Line
        Shape2DType.Ray -> UniversalMathKind.Ray
        Shape2DType.Segment -> UniversalMathKind.Segment
        Shape2DType.Circle, Shape2DType.CircleThreePoints -> UniversalMathKind.Circle
        Shape2DType.Ellipse -> UniversalMathKind.Conic
        else -> UniversalMathKind.GeometryConstruction
    }

    private fun pointDefinition(dependency: PointDependency, state: WorkspaceState): String {
        val names = dependency.inputIndices.map { index -> state.pointDependencies.firstOrNull { it.outputIndex == index }?.name ?: "P$index" }
        return when (dependency.type) {
            PointDependencyType.Midpoint -> "Midpoint(${names.joinToString()})"
            PointDependencyType.Centroid -> "Centroid(${names.joinToString()})"
            PointDependencyType.Circumcenter -> "Circumcenter(${names.joinToString()})"
            PointDependencyType.Incenter -> "Incenter(${names.joinToString()})"
            PointDependencyType.Orthocenter -> "Orthocenter(${names.joinToString()})"
            PointDependencyType.Intersection -> "Intersect(${names.joinToString()})"
            PointDependencyType.PointOnObject -> "Point(${names.joinToString()})"
            PointDependencyType.TangentPoint -> "TangentPoint(${names.joinToString()})"
            PointDependencyType.Translate -> "Translate(${names.joinToString()},${dependency.parameters.joinToString()})"
            PointDependencyType.Rotate -> "Rotate(${names.joinToString()},${dependency.parameters.joinToString()})"
            PointDependencyType.ReflectX -> "Reflect(${names.joinToString()},xAxis)"
            PointDependencyType.Dilate -> "Dilate(${names.joinToString()},${dependency.parameters.joinToString()})"
        }
    }

    private fun shapeDefinition(shape: Shape2D, state: WorkspaceState): String {
        val names = shape.pointIndices.map { index -> state.pointDependencies.firstOrNull { it.outputIndex == index }?.name ?: "P$index" }
        return "${shape.type.name}(${names.joinToString()})"
    }
}

data class UniversalDocumentRecovery(
    val document: UniversalMathDocument?,
    val recovered: Boolean,
    val diagnostics: List<String>,
    val checksumValid: Boolean,
)

/** Versioned, checksummed persistence with record-level salvage for damaged documents. */
object UniversalMathDocumentCodec {
    private val cas = SymbolicCasEngine()

    fun encode(document: UniversalMathDocument): String {
        val records = document.objects.values.sortedBy { it.id }.map(::encodeObject)
        val checksum = sha256(records.joinToString("\n"))
        return buildString {
            append("{\n  \"schemaVersion\":${UniversalMathDocument.CURRENT_SCHEMA},\n")
            append("  \"id\":\"").append(pack(document.id)).append("\",\n")
            append("  \"revision\":").append(document.revision).append(",\n")
            append("  \"modifiedAt\":").append(document.modifiedAt).append(",\n")
            append("  \"checksum\":\"").append(checksum).append("\",\n")
            append("  \"records\":[").append(records.joinToString(",") { "\"$it\"" }).append("]\n}")
        }
    }

    fun decode(source: String, recover: Boolean = true): UniversalDocumentRecovery {
        val diagnostics = mutableListOf<String>()
        val schema = Regex("\\\"schemaVersion\\\"\\s*:\\s*(\\d+)").find(source)?.groupValues?.get(1)?.toIntOrNull()
            ?: return UniversalDocumentRecovery(null, false, listOf("Missing schemaVersion"), false)
        if (schema > UniversalMathDocument.CURRENT_SCHEMA) return UniversalDocumentRecovery(null, false, listOf("Schema $schema is newer than supported schema ${UniversalMathDocument.CURRENT_SCHEMA}"), false)
        val id = Regex("\\\"id\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"").find(source)?.groupValues?.get(1)?.let(::unpack) ?: "recovered-document"
        val revision = Regex("\\\"revision\\\"\\s*:\\s*(\\d+)").find(source)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
        val modifiedAt = Regex("\\\"modifiedAt\\\"\\s*:\\s*(\\d+)").find(source)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
        val checksum = Regex("\\\"checksum\\\"\\s*:\\s*\\\"([a-f0-9]+)\\\"").find(source)?.groupValues?.get(1)
        val recordsBlock = Regex("\\\"records\\\"\\s*:\\s*\\[(.*?)]", RegexOption.DOT_MATCHES_ALL).find(source)?.groupValues?.get(1).orEmpty()
        val records = Regex("\\\"([^\\\"]+)\\\"").findAll(recordsBlock).map { it.groupValues[1] }.toList()
        val checksumValid = checksum != null && checksum == sha256(records.joinToString("\n"))
        if (!checksumValid) {
            diagnostics += if (checksum == null && schema == 1) "Migrated legacy schema without checksum." else "Checksum mismatch: attempting record-level recovery."
            if (!recover && schema != 1) return UniversalDocumentRecovery(null, false, diagnostics, false)
        }
        val objects = linkedMapOf<String, UniversalMathObject>()
        records.forEachIndexed { index, record ->
            runCatching { decodeObject(record) }
                .onSuccess { objects[it.id] = it }
                .onFailure { diagnostics += "Skipped damaged record $index: ${it.message ?: "invalid data"}" }
        }
        if (objects.isEmpty() && records.isNotEmpty()) return UniversalDocumentRecovery(null, true, diagnostics + "No valid records could be recovered.", checksumValid)
        val document = UniversalMathDocument(schemaVersion = UniversalMathDocument.CURRENT_SCHEMA, id = id, revision = revision, objects = objects, modifiedAt = modifiedAt)
        val validation = UniversalMathDocumentEngine().validate(document)
        diagnostics += validation.diagnostics
        return UniversalDocumentRecovery(document.takeIf { validation.valid }, !checksumValid || diagnostics.isNotEmpty() || schema < UniversalMathDocument.CURRENT_SCHEMA, diagnostics, checksumValid)
    }

    private fun encodeObject(value: UniversalMathObject): String = listOf(
        pack(value.id), value.kind.name, pack(value.name), pack(encodePayload(value.payload)),
        pack(value.dependencies.sorted().joinToString(",")), pack(encodeAssumptions(value.assumptions)),
        value.objectRevision.toString(), pack(value.sourceView), "2",
    ).joinToString(".")

    private fun decodeObject(record: String): UniversalMathObject {
        val fields = record.split('.')
        require(fields.size >= 8) { "record has ${fields.size} fields" }
        val id = unpack(fields[0]); val kind = UniversalMathKind.valueOf(fields[1]); val name = unpack(fields[2])
        val payload = decodePayload(unpack(fields[3])); val dependencies = unpack(fields[4]).split(',').filter(String::isNotBlank).toSet()
        val assumptions = decodeAssumptions(unpack(fields[5])); val revision = fields[6].toLong(); val sourceView = unpack(fields[7])
        return UniversalMathObject(id, kind, name, payload, dependencies, assumptions, revision, sourceView)
    }

    private fun encodePayload(payload: UniversalMathPayload): String = when (payload) {
        is UniversalMathPayload.Symbolic -> "S|${pack(payload.source)}"
        is UniversalMathPayload.Coordinates -> "C|${payload.values.joinToString(",")}|${pack(payload.labels.joinToString(","))}|${pack(payload.definition.orEmpty())}"
        is UniversalMathPayload.Dataset -> "D|${payload.values.joinToString(",")}|${payload.missingIndices.sorted().joinToString(",")}"
        is UniversalMathPayload.Properties -> "P|" + payload.entries.toSortedMap().entries.joinToString(",") { "${pack(it.key)}=${pack(it.value)}" }
    }

    private fun decodePayload(source: String): UniversalMathPayload {
        val parts = source.split('|', limit = 4)
        return when (parts.firstOrNull()) {
            "S" -> unpack(parts.getOrElse(1) { error("missing symbolic source") }).let { sourceValue ->
                val parsed = runCatching { cas.parse(sourceValue) }
                UniversalMathPayload.Symbolic(sourceValue, parsed.getOrNull(), parsed.exceptionOrNull()?.message)
            }
            "C" -> UniversalMathPayload.Coordinates(parts.getOrElse(1) { "" }.csvDoubles(), parts.getOrElse(2) { "" }.let(::unpack).split(',').filter(String::isNotBlank),
                parts.getOrNull(3)?.let(::unpack)?.takeIf(String::isNotBlank))
            "D" -> UniversalMathPayload.Dataset(parts.getOrElse(1) { "" }.csvDoubles(), parts.getOrElse(2) { "" }.split(',').mapNotNull(String::toIntOrNull).toSet())
            "P" -> UniversalMathPayload.Properties(parts.getOrElse(1) { "" }.split(',').filter(String::isNotBlank).associate { entry -> entry.substringBefore('=').let(::unpack) to entry.substringAfter('=').let(::unpack) })
            else -> error("unknown payload type")
        }
    }

    private fun encodeAssumptions(set: MathAssumptionSet): String = set.variables.values.sortedBy { it.variable }.joinToString(";") { a ->
        listOf(pack(a.variable), a.domain.name, a.positive, a.nonNegative, a.nonZero, a.minimum ?: "", a.maximum ?: "").joinToString(",")
    }

    private fun decodeAssumptions(source: String): MathAssumptionSet = MathAssumptionSet(source.split(';').filter(String::isNotBlank).associate { row ->
        val f = row.split(','); val name = unpack(f[0]); name to VariableAssumption(name, MathNumberDomain.valueOf(f[1]), f[2].toBoolean(), f[3].toBoolean(), f[4].toBoolean(), f.getOrNull(5)?.toDoubleOrNull(), f.getOrNull(6)?.toDoubleOrNull())
    })

    private fun String.csvDoubles() = split(',').filter(String::isNotBlank).map(String::toDouble)
    private fun pack(value: String): String = Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(StandardCharsets.UTF_8))
    private fun unpack(value: String): String = String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(StandardCharsets.UTF_8)).joinToString("") { "%02x".format(it) }
}
