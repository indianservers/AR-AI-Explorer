package com.indianservers.aiexplorer.workspace

import com.indianservers.aiexplorer.core.FunctionDefinition
import com.indianservers.aiexplorer.core.Geometry2D
import com.indianservers.aiexplorer.core.Solid
import com.indianservers.aiexplorer.core.SolidType
import com.indianservers.aiexplorer.core.Vec2
import com.indianservers.aiexplorer.core.Vec3
import com.indianservers.aiexplorer.core.Vector3D
import com.indianservers.aiexplorer.spatial.SpatialScenePlacement
import kotlin.math.cos
import kotlin.math.sin

enum class Shape2DType {
    Line,
    Segment,
    Ray,
    Triangle,
    Polygon,
    Rectangle,
    Square,
    Circle,
    Arc,
    Vector,
    Parallel,
    Perpendicular,
    AngleBisector,
    CircleThreePoints,
    Ellipse,
    RegularPolygon,
}

data class Shape2D(
    val id: String,
    val type: Shape2DType,
    val pointIndices: List<Int>,
    val name: String = type.name,
    val visible: Boolean = true,
    val locked: Boolean = false,
    val styleKey: String = "default",
)

enum class PointDependencyType {
    Midpoint, Centroid, Circumcenter, Incenter, Orthocenter, Intersection,
    Translate, Rotate, ReflectX, Dilate,
}

data class PointDependency(
    val outputIndex: Int,
    val inputIndices: List<Int>,
    val type: PointDependencyType,
    val name: String = type.name,
    val parameters: List<Double> = emptyList(),
)

enum class MathModule(val label: String) {
    Geometry2D("2D"),
    Geometry3D("3D"),
    Graph2D("Graph"),
    Graph3D("3D Graph"),
    Trigonometry("Trig"),
    SpatialAR("AR"),
}

data class WorkspaceState(
    val id: String = "default",
    val name: String = "Exploration Lab",
    val module: MathModule = MathModule.Graph2D,
    val points: List<Vec2> = listOf(Vec2(1.0, 2.0), Vec2(4.0, 5.0)),
    val shapes: List<Shape2D> = listOf(
        Shape2D("default-segment", Shape2DType.Segment, listOf(0, 1), "AB"),
    ),
    val pointDependencies: List<PointDependency> = emptyList(),
    val functions: List<FunctionDefinition> = listOf(
        FunctionDefinition("f", "f(x)", "x^2 - 4*x + 3", "cyan"),
        FunctionDefinition("g", "g(x)", "x - 1", "violet"),
    ),
    val solids: List<Solid> = listOf(
        Solid(SolidType.Cube, width = 2.0, position = Vec3(-2.6, 0.0, 0.0)),
        Solid(SolidType.Cylinder, width = 2.0, height = 2.2, radius = 0.9, position = Vec3(0.0, 0.0, 0.0)),
        Solid(SolidType.Cone, width = 2.0, height = 2.5, radius = 0.9, position = Vec3(2.6, 0.0, 0.0)),
    ),
    val vectors3D: List<Vector3D> = listOf(
        Vector3D("u", Vec3(-2.0, -1.0, -1.0), Vec3(1.4, 1.2, 1.0), "u"),
        Vector3D("v", Vec3(0.2, -1.3, 1.1), Vec3(2.4, .7, -1.0), "v"),
    ),
    val surfaceExpression: String = "x^2 + y^2",
    val spatialPlacement: SpatialScenePlacement = SpatialScenePlacement(),
    val modifiedAt: Long = System.currentTimeMillis(),
)

sealed interface WorkspaceCommand {
    val label: String
    fun apply(state: WorkspaceState): WorkspaceState
    fun undo(state: WorkspaceState): WorkspaceState
}

data class MovePointCommand(val index: Int, val from: Vec2, val to: Vec2) : WorkspaceCommand {
    override val label = "Move point"
    override fun apply(state: WorkspaceState) = state.copy(points = state.points.replace(index, to), modifiedAt = System.currentTimeMillis()).recomputed()
    override fun undo(state: WorkspaceState) = state.copy(points = state.points.replace(index, from), modifiedAt = System.currentTimeMillis()).recomputed()
}

data class MovePointsCommand(
    val indices: List<Int>,
    val from: List<Vec2>,
    val to: List<Vec2>,
) : WorkspaceCommand {
    init {
        require(indices.size == from.size && from.size == to.size)
    }

    override val label = "Move object"

    override fun apply(state: WorkspaceState) = state.copy(
        points = state.points.replaceMany(indices, to),
        modifiedAt = System.currentTimeMillis(),
    ).recomputed()

    override fun undo(state: WorkspaceState) = state.copy(
        points = state.points.replaceMany(indices, from),
        modifiedAt = System.currentTimeMillis(),
    ).recomputed()
}

data class AddPointCommand(val point: Vec2) : WorkspaceCommand {
    override val label = "Add point"
    override fun apply(state: WorkspaceState) = state.copy(points = state.points + point, modifiedAt = System.currentTimeMillis())
    override fun undo(state: WorkspaceState) = state.copy(points = state.points.dropLast(1), modifiedAt = System.currentTimeMillis())
}

data class AddConstructionCommand(
    val points: List<Vec2>,
    val shapeType: Shape2DType?,
    val existingPointIndices: List<Int?> = emptyList(),
) : WorkspaceCommand {
    override val label = if (shapeType == null) "Add point" else "Add ${shapeType.name.lowercase()}"

    override fun apply(state: WorkspaceState): WorkspaceState {
        val startIndex = state.points.size
        val addedPoints = mutableListOf<Vec2>()
        val pointIndices = points.indices.map { index ->
            existingPointIndices.getOrNull(index)?.takeIf { it in state.points.indices }
                ?: (startIndex + addedPoints.size).also { addedPoints += points[index] }
        }
        val addedShape = shapeType?.let {
            Shape2D(
                id = "${it.name.lowercase()}-${System.currentTimeMillis()}",
                type = it,
                pointIndices = pointIndices,
            )
        }
        return state.copy(
            points = state.points + addedPoints,
            shapes = if (addedShape == null) state.shapes else state.shapes + addedShape,
            modifiedAt = System.currentTimeMillis(),
        )
    }

    override fun undo(state: WorkspaceState): WorkspaceState {
        val removeCount = points.indices.count { existingPointIndices.getOrNull(it) == null }
        return state.copy(
            points = state.points.dropLast(removeCount),
            shapes = if (shapeType == null) state.shapes else state.shapes.dropLast(1),
            modifiedAt = System.currentTimeMillis(),
        )
    }
}

data class AddDependentPointCommand(
    val inputIndices: List<Int>,
    val dependencyType: PointDependencyType,
    val name: String = dependencyType.name,
) : WorkspaceCommand {
    override val label = "Add ${dependencyType.name.lowercase()}"

    override fun apply(state: WorkspaceState): WorkspaceState {
        val outputIndex = state.points.size
        val position = resolvePointDependency(state.points, inputIndices, dependencyType)
            ?: inputIndices.mapNotNull(state.points::getOrNull).firstOrNull()
            ?: Vec2(0.0, 0.0)
        return state.copy(
            points = state.points + position,
            pointDependencies = state.pointDependencies + PointDependency(outputIndex, inputIndices, dependencyType, name),
            modifiedAt = System.currentTimeMillis(),
        ).recomputed()
    }

    override fun undo(state: WorkspaceState) = state.copy(
        points = state.points.dropLast(1),
        pointDependencies = state.pointDependencies.dropLast(1),
        modifiedAt = System.currentTimeMillis(),
    ).recomputed()
}

data class AddShapeFromPointsCommand(
    val type: Shape2DType,
    val pointIndices: List<Int>,
    val name: String = type.name,
) : WorkspaceCommand {
    override val label = "Add ${type.name.lowercase()}"
    override fun apply(state: WorkspaceState) = state.copy(
        shapes = state.shapes + Shape2D(
            id = "${type.name.lowercase()}-${System.currentTimeMillis()}",
            type = type,
            pointIndices = pointIndices,
            name = name,
        ),
        modifiedAt = System.currentTimeMillis(),
    )

    override fun undo(state: WorkspaceState) = state.copy(shapes = state.shapes.dropLast(1), modifiedAt = System.currentTimeMillis())
}

data class UpdateShapeCommand(val index: Int, val from: Shape2D, val to: Shape2D) : WorkspaceCommand {
    override val label = "Update object"
    override fun apply(state: WorkspaceState) = state.copy(shapes = state.shapes.replace(index, to), modifiedAt = System.currentTimeMillis())
    override fun undo(state: WorkspaceState) = state.copy(shapes = state.shapes.replace(index, from), modifiedAt = System.currentTimeMillis())
}

data class DeleteShapeCommand(val index: Int, val shape: Shape2D) : WorkspaceCommand {
    override val label = "Delete ${shape.name}"
    override fun apply(state: WorkspaceState) = state.copy(
        shapes = state.shapes.filterIndexed { i, _ -> i != index },
        modifiedAt = System.currentTimeMillis(),
    )

    override fun undo(state: WorkspaceState) = state.copy(
        shapes = state.shapes.toMutableList().apply { add(index.coerceIn(0, size), shape) },
        modifiedAt = System.currentTimeMillis(),
    )
}

data class TransformShape2DCommand(
    val sourceIndex: Int,
    val dependencyType: PointDependencyType,
    val parameters: List<Double> = emptyList(),
) : WorkspaceCommand {
    override val label = "${dependencyType.name.lowercase()} object"

    override fun apply(state: WorkspaceState): WorkspaceState {
        val source = state.shapes.getOrNull(sourceIndex) ?: return state
        val distinctInputs = source.pointIndices.distinct()
        val outputByInput = mutableMapOf<Int, Int>()
        val addedPoints = mutableListOf<Vec2>()
        val addedDependencies = mutableListOf<PointDependency>()
        distinctInputs.forEach { inputIndex ->
            val outputIndex = state.points.size + addedPoints.size
            val position = resolvePointDependency(state.points, listOf(inputIndex), dependencyType, parameters)
                ?: state.points.getOrNull(inputIndex)
                ?: Vec2(0.0, 0.0)
            outputByInput[inputIndex] = outputIndex
            addedPoints += position
            addedDependencies += PointDependency(
                outputIndex = outputIndex,
                inputIndices = listOf(inputIndex),
                type = dependencyType,
                name = "${source.name} ${dependencyType.name}",
                parameters = parameters,
            )
        }
        val transformed = source.copy(
            id = "${source.id}-${dependencyType.name.lowercase()}-${System.currentTimeMillis()}",
            pointIndices = source.pointIndices.map { outputByInput.getValue(it) },
            name = "${source.name} ${dependencyType.name.lowercase()}",
            locked = false,
        )
        return state.copy(
            points = state.points + addedPoints,
            pointDependencies = state.pointDependencies + addedDependencies,
            shapes = state.shapes + transformed,
            modifiedAt = System.currentTimeMillis(),
        ).recomputed()
    }

    override fun undo(state: WorkspaceState): WorkspaceState {
        val source = state.shapes.getOrNull(sourceIndex) ?: return state
        val count = source.pointIndices.distinct().size
        return state.copy(
            points = state.points.dropLast(count),
            pointDependencies = state.pointDependencies.dropLast(count),
            shapes = state.shapes.dropLast(1),
            modifiedAt = System.currentTimeMillis(),
        ).recomputed()
    }
}

data class EditExpressionCommand(val index: Int, val from: String, val to: String) : WorkspaceCommand {
    override val label = "Edit expression"
    override fun apply(state: WorkspaceState) = state.copy(functions = state.functions.replace(index, state.functions[index].copy(expression = to)), modifiedAt = System.currentTimeMillis())
    override fun undo(state: WorkspaceState) = state.copy(functions = state.functions.replace(index, state.functions[index].copy(expression = from)), modifiedAt = System.currentTimeMillis())
}

data class AddFunctionCommand(val function: FunctionDefinition) : WorkspaceCommand {
    override val label = "Add function"
    override fun apply(state: WorkspaceState) = state.copy(functions = state.functions + function, modifiedAt = System.currentTimeMillis())
    override fun undo(state: WorkspaceState) = state.copy(functions = state.functions.dropLast(1), modifiedAt = System.currentTimeMillis())
}

data class UpdateFunctionCommand(val index: Int, val from: FunctionDefinition, val to: FunctionDefinition) : WorkspaceCommand {
    override val label = "Update function"
    override fun apply(state: WorkspaceState) = state.copy(functions = state.functions.replace(index, to), modifiedAt = System.currentTimeMillis())
    override fun undo(state: WorkspaceState) = state.copy(functions = state.functions.replace(index, from), modifiedAt = System.currentTimeMillis())
}

data class DeleteFunctionCommand(val index: Int, val function: FunctionDefinition) : WorkspaceCommand {
    override val label = "Delete function"
    override fun apply(state: WorkspaceState) = state.copy(
        functions = state.functions.filterIndexed { i, _ -> i != index },
        modifiedAt = System.currentTimeMillis(),
    )
    override fun undo(state: WorkspaceState) = state.copy(
        functions = state.functions.toMutableList().apply { add(index.coerceIn(0, size), function) },
        modifiedAt = System.currentTimeMillis(),
    )
}

data class AddSolidCommand(val solid: Solid) : WorkspaceCommand {
    override val label = "Add solid"
    override fun apply(state: WorkspaceState) = state.copy(solids = state.solids + solid, modifiedAt = System.currentTimeMillis())
    override fun undo(state: WorkspaceState) = state.copy(solids = state.solids.dropLast(1), modifiedAt = System.currentTimeMillis())
}

data class MoveSolidCommand(val index: Int, val from: Vec3, val to: Vec3) : WorkspaceCommand {
    override val label = "Move solid"
    override fun apply(state: WorkspaceState) = state.copy(
        solids = state.solids.replace(index, state.solids[index].copy(position = to)),
        modifiedAt = System.currentTimeMillis(),
    )

    override fun undo(state: WorkspaceState) = state.copy(
        solids = state.solids.replace(index, state.solids[index].copy(position = from)),
        modifiedAt = System.currentTimeMillis(),
    )
}

data class TransformSolidCommand(val index: Int, val from: Solid, val to: Solid) : WorkspaceCommand {
    override val label = "Transform solid"
    override fun apply(state: WorkspaceState) = state.copy(solids = state.solids.replace(index, to), modifiedAt = System.currentTimeMillis())
    override fun undo(state: WorkspaceState) = state.copy(solids = state.solids.replace(index, from), modifiedAt = System.currentTimeMillis())
}

data class AddVector3DCommand(val vector: Vector3D) : WorkspaceCommand {
    override val label = "Add 3D vector"
    override fun apply(state: WorkspaceState) = state.copy(vectors3D = state.vectors3D + vector, modifiedAt = System.currentTimeMillis())
    override fun undo(state: WorkspaceState) = state.copy(vectors3D = state.vectors3D.dropLast(1), modifiedAt = System.currentTimeMillis())
}

data class MoveVector3DCommand(val index: Int, val from: Vector3D, val to: Vector3D) : WorkspaceCommand {
    override val label = "Move 3D vector"
    override fun apply(state: WorkspaceState) = state.copy(vectors3D = state.vectors3D.replace(index, to), modifiedAt = System.currentTimeMillis())
    override fun undo(state: WorkspaceState) = state.copy(vectors3D = state.vectors3D.replace(index, from), modifiedAt = System.currentTimeMillis())
}

data class TransformVector3DCommand(val index: Int, val from: Vector3D, val to: Vector3D) : WorkspaceCommand {
    override val label = "Transform 3D vector"
    override fun apply(state: WorkspaceState) = state.copy(vectors3D = state.vectors3D.replace(index, to), modifiedAt = System.currentTimeMillis())
    override fun undo(state: WorkspaceState) = state.copy(vectors3D = state.vectors3D.replace(index, from), modifiedAt = System.currentTimeMillis())
}

data class TransformSpatialPlacementCommand(
    val from: SpatialScenePlacement,
    val to: SpatialScenePlacement,
    override val label: String = "Transform spatial scene",
) : WorkspaceCommand {
    override fun apply(state: WorkspaceState) = state.copy(spatialPlacement = to, modifiedAt = System.currentTimeMillis())
    override fun undo(state: WorkspaceState) = state.copy(spatialPlacement = from, modifiedAt = System.currentTimeMillis())
}

class CommandHistory(private val limit: Int = 80) {
    private val undoStack = ArrayDeque<WorkspaceCommand>()
    private val redoStack = ArrayDeque<WorkspaceCommand>()

    fun execute(state: WorkspaceState, command: WorkspaceCommand): WorkspaceState {
        recordApplied(command)
        return command.apply(state)
    }

    /** Records a gesture command after its live preview has already reached [WorkspaceState]. */
    fun recordApplied(command: WorkspaceCommand) {
        undoStack.addLast(command)
        while (undoStack.size > limit) undoStack.removeFirst()
        redoStack.clear()
    }

    fun undo(state: WorkspaceState): WorkspaceState {
        val command = undoStack.removeLastOrNull() ?: return state
        redoStack.addLast(command)
        return command.undo(state)
    }

    fun redo(state: WorkspaceState): WorkspaceState {
        val command = redoStack.removeLastOrNull() ?: return state
        undoStack.addLast(command)
        return command.apply(state)
    }

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()
    val protocol: List<String> get() = undoStack.map { it.label }
}

object WorkspaceJson {
    fun export(state: WorkspaceState): String = buildString {
        appendLine("{")
        appendLine("  \"schemaVersion\": 1,")
        appendLine("  \"id\": \"${state.id.jsonEscaped()}\",")
        appendLine("  \"name\": \"${state.name.jsonEscaped()}\",")
        appendLine("  \"module\": \"${state.module.name}\",")
        appendLine("  \"points\": [${state.points.joinToString { "{\"x\":${it.x},\"y\":${it.y}}" }}],")
        appendLine("  \"shapes\": [${state.shapes.joinToString { "{\"id\":\"${it.id.jsonEscaped()}\",\"type\":\"${it.type}\",\"name\":\"${it.name.jsonEscaped()}\",\"points\":[${it.pointIndices.joinToString()}],\"visible\":${it.visible},\"locked\":${it.locked},\"style\":\"${it.styleKey.jsonEscaped()}\"}" }}],")
        appendLine("  \"pointDependencies\": [${state.pointDependencies.joinToString { "{\"output\":${it.outputIndex},\"inputs\":[${it.inputIndices.joinToString()}],\"type\":\"${it.type}\",\"name\":\"${it.name.jsonEscaped()}\",\"parameters\":[${it.parameters.joinToString()}]}" }}],")
        appendLine("  \"functions\": [${state.functions.joinToString { "{\"id\":\"${it.id.jsonEscaped()}\",\"name\":\"${it.name.jsonEscaped()}\",\"expression\":\"${it.expression.jsonEscaped()}\",\"color\":\"${it.colorKey.jsonEscaped()}\",\"visible\":${it.visible}}" }}],")
        appendLine("  \"solids\": [${state.solids.joinToString { "{\"type\":\"${it.type}\",\"width\":${it.width},\"height\":${it.height},\"depth\":${it.depth},\"radius\":${it.radius},\"topRadius\":${it.topRadius},\"position\":{\"x\":${it.position.x},\"y\":${it.position.y},\"z\":${it.position.z}},\"rotation\":{\"x\":${it.rotation.x},\"y\":${it.rotation.y},\"z\":${it.rotation.z}}}" }}],")
        appendLine("  \"vectors3D\": [${state.vectors3D.joinToString { "{\"id\":\"${it.id.jsonEscaped()}\",\"name\":\"${it.name.jsonEscaped()}\",\"start\":{\"x\":${it.start.x},\"y\":${it.start.y},\"z\":${it.start.z}},\"end\":{\"x\":${it.end.x},\"y\":${it.end.y},\"z\":${it.end.z}}}" }}],")
        appendLine("  \"surfaceExpression\": \"${state.surfaceExpression.jsonEscaped()}\",")
        appendLine("  \"spatialPlacement\": {\"anchorId\":\"${state.spatialPlacement.anchorId.jsonEscaped()}\",\"positionMeters\":{\"x\":${state.spatialPlacement.pose.positionMeters.x},\"y\":${state.spatialPlacement.pose.positionMeters.y},\"z\":${state.spatialPlacement.pose.positionMeters.z}},\"rotationDegrees\":{\"x\":${state.spatialPlacement.pose.rotationDegrees.x},\"y\":${state.spatialPlacement.pose.rotationDegrees.y},\"z\":${state.spatialPlacement.pose.rotationDegrees.z}},\"uniformScale\":${state.spatialPlacement.pose.uniformScale},\"scaleMode\":\"${state.spatialPlacement.scaleMode}\",\"metersPerMathUnit\":${state.spatialPlacement.metersPerMathUnit},\"estimated\":${state.spatialPlacement.estimated},\"depthOcclusionEnabled\":${state.spatialPlacement.depthOcclusionEnabled}},")
        appendLine("  \"modifiedAt\": ${state.modifiedAt}")
        appendLine("}")
    }
}

private fun String.jsonEscaped(): String = buildString {
    this@jsonEscaped.forEach { char ->
        append(when (char) {
            '\\' -> "\\\\"
            '"' -> "\\\""
            '\n' -> "\\n"
            '\r' -> "\\r"
            '\t' -> "\\t"
            else -> char
        })
    }
}

private fun <T> List<T>.replace(index: Int, value: T): List<T> = mapIndexed { i, old -> if (i == index) value else old }

private fun <T> List<T>.replaceMany(indices: List<Int>, values: List<T>): List<T> {
    val replacements = indices.zip(values).toMap()
    return mapIndexed { index, old -> replacements[index] ?: old }
}

fun WorkspaceState.recomputed(): WorkspaceState {
    if (pointDependencies.isEmpty()) return this
    val updated = points.toMutableList()
    pointDependencies.sortedBy { it.outputIndex }.forEach { dependency ->
        if (dependency.outputIndex !in updated.indices) return@forEach
        resolvePointDependency(updated, dependency.inputIndices, dependency.type, dependency.parameters)?.let { updated[dependency.outputIndex] = it }
    }
    return copy(points = updated)
}

fun resolvePointDependency(
    points: List<Vec2>,
    inputs: List<Int>,
    type: PointDependencyType,
    parameters: List<Double> = emptyList(),
): Vec2? {
    val p = inputs.map { points.getOrNull(it) ?: return null }
    return when (type) {
        PointDependencyType.Midpoint -> if (p.size >= 2) Geometry2D.segment(p[0], p[1]).midpoint else null
        PointDependencyType.Centroid -> if (p.size >= 3) Geometry2D.centroid(p[0], p[1], p[2]) else null
        PointDependencyType.Circumcenter -> if (p.size >= 3) Geometry2D.circumcenter(p[0], p[1], p[2]) else null
        PointDependencyType.Incenter -> if (p.size >= 3) Geometry2D.incenter(p[0], p[1], p[2]) else null
        PointDependencyType.Orthocenter -> if (p.size >= 3) Geometry2D.orthocenter(p[0], p[1], p[2]) else null
        PointDependencyType.Intersection -> if (p.size >= 4) Geometry2D.lineIntersection(p[0], p[1], p[2], p[3]) else null
        PointDependencyType.Translate -> p.firstOrNull()?.let { it + Vec2(parameters.getOrElse(0) { 0.0 }, parameters.getOrElse(1) { 0.0 }) }
        PointDependencyType.Rotate -> p.firstOrNull()?.let { point ->
            val angle = Math.toRadians(parameters.getOrElse(0) { 0.0 })
            val center = Vec2(parameters.getOrElse(1) { 0.0 }, parameters.getOrElse(2) { 0.0 })
            val local = point - center
            center + Vec2(local.x * cos(angle) - local.y * sin(angle), local.x * sin(angle) + local.y * cos(angle))
        }
        PointDependencyType.ReflectX -> p.firstOrNull()?.let { Vec2(it.x, -it.y) }
        PointDependencyType.Dilate -> p.firstOrNull()?.let { point ->
            val factor = parameters.getOrElse(0) { 1.0 }
            val center = Vec2(parameters.getOrElse(1) { 0.0 }, parameters.getOrElse(2) { 0.0 })
            center + (point - center) * factor
        }
    }
}
