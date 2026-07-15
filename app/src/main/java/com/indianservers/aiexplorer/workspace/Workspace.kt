package com.indianservers.aiexplorer.workspace

import com.indianservers.aiexplorer.core.FunctionDefinition
import com.indianservers.aiexplorer.core.Solid
import com.indianservers.aiexplorer.core.SolidType
import com.indianservers.aiexplorer.core.Vec2
import com.indianservers.aiexplorer.core.Vec3
import com.indianservers.aiexplorer.core.Vector3D

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
}

data class Shape2D(
    val id: String,
    val type: Shape2DType,
    val pointIndices: List<Int>,
    val name: String = type.name,
)

enum class MathModule(val label: String) {
    Geometry2D("2D"),
    Geometry3D("3D"),
    Graph2D("Graph"),
    Graph3D("3D Graph"),
    Trigonometry("Trig"),
}

data class WorkspaceState(
    val id: String = "default",
    val name: String = "Exploration Lab",
    val module: MathModule = MathModule.Graph2D,
    val points: List<Vec2> = listOf(Vec2(1.0, 2.0), Vec2(4.0, 5.0)),
    val shapes: List<Shape2D> = listOf(
        Shape2D("default-segment", Shape2DType.Segment, listOf(0, 1), "AB"),
    ),
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
    val modifiedAt: Long = System.currentTimeMillis(),
)

sealed interface WorkspaceCommand {
    val label: String
    fun apply(state: WorkspaceState): WorkspaceState
    fun undo(state: WorkspaceState): WorkspaceState
}

data class MovePointCommand(val index: Int, val from: Vec2, val to: Vec2) : WorkspaceCommand {
    override val label = "Move point"
    override fun apply(state: WorkspaceState) = state.copy(points = state.points.replace(index, to), modifiedAt = System.currentTimeMillis())
    override fun undo(state: WorkspaceState) = state.copy(points = state.points.replace(index, from), modifiedAt = System.currentTimeMillis())
}

data class AddPointCommand(val point: Vec2) : WorkspaceCommand {
    override val label = "Add point"
    override fun apply(state: WorkspaceState) = state.copy(points = state.points + point, modifiedAt = System.currentTimeMillis())
    override fun undo(state: WorkspaceState) = state.copy(points = state.points.dropLast(1), modifiedAt = System.currentTimeMillis())
}

data class AddConstructionCommand(val points: List<Vec2>, val shapeType: Shape2DType?) : WorkspaceCommand {
    override val label = if (shapeType == null) "Add point" else "Add ${shapeType.name.lowercase()}"

    override fun apply(state: WorkspaceState): WorkspaceState {
        val startIndex = state.points.size
        val addedShape = shapeType?.let {
            Shape2D(
                id = "${it.name.lowercase()}-${System.currentTimeMillis()}",
                type = it,
                pointIndices = points.indices.map { offset -> startIndex + offset },
            )
        }
        return state.copy(
            points = state.points + points,
            shapes = if (addedShape == null) state.shapes else state.shapes + addedShape,
            modifiedAt = System.currentTimeMillis(),
        )
    }

    override fun undo(state: WorkspaceState): WorkspaceState {
        val removeCount = points.size
        return state.copy(
            points = state.points.dropLast(removeCount),
            shapes = if (shapeType == null) state.shapes else state.shapes.dropLast(1),
            modifiedAt = System.currentTimeMillis(),
        )
    }
}

data class EditExpressionCommand(val index: Int, val from: String, val to: String) : WorkspaceCommand {
    override val label = "Edit expression"
    override fun apply(state: WorkspaceState) = state.copy(functions = state.functions.replace(index, state.functions[index].copy(expression = to)), modifiedAt = System.currentTimeMillis())
    override fun undo(state: WorkspaceState) = state.copy(functions = state.functions.replace(index, state.functions[index].copy(expression = from)), modifiedAt = System.currentTimeMillis())
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

class CommandHistory(private val limit: Int = 80) {
    private val undoStack = ArrayDeque<WorkspaceCommand>()
    private val redoStack = ArrayDeque<WorkspaceCommand>()

    fun execute(state: WorkspaceState, command: WorkspaceCommand): WorkspaceState {
        undoStack.addLast(command)
        while (undoStack.size > limit) undoStack.removeFirst()
        redoStack.clear()
        return command.apply(state)
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
}

object WorkspaceJson {
    fun export(state: WorkspaceState): String = buildString {
        appendLine("{")
        appendLine("  \"schemaVersion\": 1,")
        appendLine("  \"id\": \"${state.id}\",")
        appendLine("  \"name\": \"${state.name}\",")
        appendLine("  \"module\": \"${state.module.name}\",")
        appendLine("  \"points\": [${state.points.joinToString { "{\"x\":${it.x},\"y\":${it.y}}" }}],")
        appendLine("  \"shapes\": [${state.shapes.joinToString { "{\"id\":\"${it.id}\",\"type\":\"${it.type}\",\"points\":[${it.pointIndices.joinToString()}]}" }}],")
        appendLine("  \"functions\": [${state.functions.joinToString { "{\"id\":\"${it.id}\",\"name\":\"${it.name}\",\"expression\":\"${it.expression}\"}" }}],")
        appendLine("  \"solids\": [${state.solids.joinToString { "{\"type\":\"${it.type}\",\"width\":${it.width},\"height\":${it.height},\"depth\":${it.depth},\"radius\":${it.radius},\"position\":{\"x\":${it.position.x},\"y\":${it.position.y},\"z\":${it.position.z}}}" }}],")
        appendLine("  \"vectors3D\": [${state.vectors3D.joinToString { "{\"id\":\"${it.id}\",\"name\":\"${it.name}\",\"start\":{\"x\":${it.start.x},\"y\":${it.start.y},\"z\":${it.start.z}},\"end\":{\"x\":${it.end.x},\"y\":${it.end.y},\"z\":${it.end.z}}}" }}],")
        appendLine("  \"surfaceExpression\": \"${state.surfaceExpression}\",")
        appendLine("  \"modifiedAt\": ${state.modifiedAt}")
        appendLine("}")
    }
}

private fun <T> List<T>.replace(index: Int, value: T): List<T> = mapIndexed { i, old -> if (i == index) value else old }
