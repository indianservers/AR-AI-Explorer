package com.indianservers.aiexplorer.assistant.workspace

import com.indianservers.aiexplorer.core.Geometry2D
import com.indianservers.aiexplorer.core.Geometry3D
import com.indianservers.aiexplorer.core.SolidType
import com.indianservers.aiexplorer.workspace.MathModule
import com.indianservers.aiexplorer.workspace.Shape2DType
import com.indianservers.aiexplorer.workspace.WorkspaceState
import java.util.Locale

enum class WorkspaceAssistantActionType { EXPLAIN, DELETE, RESIZE, ROTATE, FORMULA, PRACTICE, ADD, FIT_VIEW, CLEAR_ALL }
enum class WorkspaceAssistantCommandType { ADD_2D_SHAPE, ADD_3D_SOLID, DELETE_SELECTED, ROTATE_SELECTED, RESIZE_SELECTED, CLEAR_ALL, FIT_VIEW, UNKNOWN }
enum class WorkspaceAssistantTargetKind { NONE, SHAPE_2D, SOLID_3D, VECTOR_3D, GRAPH_FUNCTION, SURFACE_3D, WORKSPACE }

data class WorkspaceAssistantContext(
    val module: MathModule,
    val selectedIndex: Int = -1,
    val selectedName: String? = null,
    val targetKind: WorkspaceAssistantTargetKind = WorkspaceAssistantTargetKind.NONE,
    val objectCount: Int = 0,
    val pointCount: Int = 0,
    val summaryFacts: List<String> = emptyList(),
)

data class WorkspaceAssistantSummary(
    val title: String,
    val description: String,
    val measurements: List<String>,
    val formulas: List<String>,
    val nextActions: List<WorkspaceAssistantAction>,
)

data class WorkspaceAssistantAction(
    val type: WorkspaceAssistantActionType,
    val label: String,
    val enabled: Boolean = true,
    val compact: Boolean = true,
)

data class WorkspaceAssistantCommand(
    val type: WorkspaceAssistantCommandType,
    val targetName: String? = null,
    val amount: Double? = null,
    val valid: Boolean = type != WorkspaceAssistantCommandType.UNKNOWN,
    val reason: String = "",
)

object WorkspaceAssistantContextFactory {
    fun from(state: WorkspaceState, selectedShapeIndex: Int = -1, selectedSolidIndex: Int = -1, selectedVectorIndex: Int = -1): WorkspaceAssistantContext = when (state.module) {
        MathModule.Geometry2D, MathModule.CoordinatePlane -> {
            val shape = state.shapes.getOrNull(selectedShapeIndex)
            WorkspaceAssistantContext(
                module = state.module,
                selectedIndex = selectedShapeIndex,
                selectedName = shape?.name,
                targetKind = if (shape != null) WorkspaceAssistantTargetKind.SHAPE_2D else WorkspaceAssistantTargetKind.WORKSPACE,
                objectCount = state.shapes.count { it.visible },
                pointCount = state.points.size,
                summaryFacts = listOf("${state.shapes.size} shapes", "${state.points.size} points", "${state.geometryConstraints.size} constraints"),
            )
        }
        MathModule.Geometry3D, MathModule.VectorLab -> {
            val solid = state.solids.getOrNull(selectedSolidIndex)
            val vector = state.vectors3D.getOrNull(selectedVectorIndex)
            WorkspaceAssistantContext(
                module = state.module,
                selectedIndex = if (solid != null) selectedSolidIndex else selectedVectorIndex,
                selectedName = solid?.type?.name ?: vector?.name,
                targetKind = when {
                    solid != null -> WorkspaceAssistantTargetKind.SOLID_3D
                    vector != null -> WorkspaceAssistantTargetKind.VECTOR_3D
                    else -> WorkspaceAssistantTargetKind.WORKSPACE
                },
                objectCount = state.solids.size + state.vectors3D.size,
                pointCount = 0,
                summaryFacts = listOf("${state.solids.size} solids", "${state.vectors3D.size} vectors"),
            )
        }
        MathModule.Graph2D -> WorkspaceAssistantContext(
            module = state.module,
            selectedIndex = -1,
            selectedName = state.functions.firstOrNull()?.name,
            targetKind = WorkspaceAssistantTargetKind.GRAPH_FUNCTION,
            objectCount = state.functions.size,
            summaryFacts = state.functions.take(3).map { "${it.name}: ${it.expression}" },
        )
        MathModule.Graph3D -> WorkspaceAssistantContext(
            module = state.module,
            selectedName = state.surfaceExpression,
            targetKind = WorkspaceAssistantTargetKind.SURFACE_3D,
            objectCount = 1,
            summaryFacts = listOf("surface z = ${state.surfaceExpression}"),
        )
        MathModule.CalculusLab -> labContext(state.module, "limits", "derivatives", "integrals", "applications")
        MathModule.MatricesLinearTransformations -> labContext(state.module, "exact matrices", "row reduction", "linear systems", "transformations")
        MathModule.PhysicsMath -> labContext(state.module, "motion", "projectiles", "force and energy", "oscillations")
        MathModule.MathematicalArt -> labContext(state.module, "polar art", "parametric art", "fractals", "symmetry")
        else -> WorkspaceAssistantContext(
            module = state.module,
            targetKind = WorkspaceAssistantTargetKind.WORKSPACE,
            objectCount = state.shapes.size + state.solids.size + state.functions.size,
            pointCount = state.points.size,
        )
    }

    private fun labContext(module: MathModule, vararg capabilities: String) = WorkspaceAssistantContext(
        module = module,
        targetKind = WorkspaceAssistantTargetKind.WORKSPACE,
        objectCount = 0,
        summaryFacts = capabilities.map { "supports $it" },
    )
}

object WorkspaceAssistantSummarizer {
    fun summarize(state: WorkspaceState, selectedShapeIndex: Int = -1, selectedSolidIndex: Int = -1, selectedVectorIndex: Int = -1): WorkspaceAssistantSummary {
        val context = WorkspaceAssistantContextFactory.from(state, selectedShapeIndex, selectedSolidIndex, selectedVectorIndex)
        return when (context.targetKind) {
            WorkspaceAssistantTargetKind.SHAPE_2D -> summarizeShape2D(state, selectedShapeIndex, context)
            WorkspaceAssistantTargetKind.SOLID_3D -> summarizeSolid3D(state, selectedSolidIndex, context)
            WorkspaceAssistantTargetKind.VECTOR_3D -> summarizeVector3D(state, selectedVectorIndex, context)
            WorkspaceAssistantTargetKind.GRAPH_FUNCTION -> WorkspaceAssistantSummary(
                "2D graph workspace",
                "There are ${state.functions.size} graph row(s).",
                state.functions.take(3).map { "${it.name} = ${it.expression}" },
                emptyList(),
                commonActions(selected = false),
            )
            WorkspaceAssistantTargetKind.SURFACE_3D -> WorkspaceAssistantSummary(
                "3D graph surface",
                "The active surface expression is z = ${state.surfaceExpression}.",
                listOf("Surface expression: ${state.surfaceExpression}"),
                emptyList(),
                commonActions(selected = false),
            )
            else -> WorkspaceAssistantSummary(
                "${state.module.label} workspace",
                "No object is selected. ${context.summaryFacts.joinToString(", ")}.",
                context.summaryFacts,
                emptyList(),
                commonActions(selected = false),
            )
        }
    }

    private fun summarizeShape2D(state: WorkspaceState, index: Int, context: WorkspaceAssistantContext): WorkspaceAssistantSummary {
        val shape = state.shapes.getOrNull(index) ?: return summarize(state)
        val points = shape.pointIndices.mapNotNull(state.points::getOrNull)
        val measurements = buildList {
            add("${shape.type.name.readable()} with ${points.size} control point(s)")
            if (points.size >= 2) add("First segment ${format(points[0].distanceTo(points[1]))} units")
            if (points.size >= 3) add("Area ${format(kotlin.math.abs(Geometry2D.polygonArea(points)))} square units")
            if (shape.locked) add("Locked")
        }
        return WorkspaceAssistantSummary(
            shape.name,
            "Selected ${shape.type.name.readable()} in the 2D geometry workspace.",
            measurements,
            formulaHints(shape.type),
            commonActions(selected = true, locked = shape.locked),
        )
    }

    private fun summarizeSolid3D(state: WorkspaceState, index: Int, context: WorkspaceAssistantContext): WorkspaceAssistantSummary {
        val solid = state.solids.getOrNull(index) ?: return summarize(state)
        val measure = Geometry3D.measure(solid)
        return WorkspaceAssistantSummary(
            solid.type.name,
            "Selected ${solid.type.name.readable()} in the 3D geometry workspace.",
            listOf(
                "Width ${format(solid.width)}, height ${format(solid.height)}, depth ${format(solid.depth)}",
                "Surface area ${format(measure.surfaceArea)}",
                "Volume ${format(measure.volume)}",
                "${measure.faces} faces, ${measure.edges} edges, ${measure.vertices} vertices",
            ),
            Geometry3D.formulas(solid.type).take(3).map { "${it.first}: ${it.second}" },
            commonActions(selected = true),
        )
    }

    private fun summarizeVector3D(state: WorkspaceState, index: Int, context: WorkspaceAssistantContext): WorkspaceAssistantSummary {
        val vector = state.vectors3D.getOrNull(index) ?: return summarize(state)
        return WorkspaceAssistantSummary(
            vector.name,
            "Selected 3D vector.",
            listOf("Components <${format(vector.components.x)}, ${format(vector.components.y)}, ${format(vector.components.z)}>", "Magnitude ${format(vector.magnitude)}"),
            listOf("|v| = sqrt(x^2 + y^2 + z^2)"),
            commonActions(selected = true),
        )
    }

    private fun commonActions(selected: Boolean, locked: Boolean = false): List<WorkspaceAssistantAction> = buildList {
        add(WorkspaceAssistantAction(WorkspaceAssistantActionType.EXPLAIN, "Explain"))
        add(WorkspaceAssistantAction(WorkspaceAssistantActionType.FORMULA, "Formula"))
        add(WorkspaceAssistantAction(WorkspaceAssistantActionType.PRACTICE, "Practice"))
        if (selected) {
            add(WorkspaceAssistantAction(WorkspaceAssistantActionType.RESIZE, "Size", enabled = !locked))
            add(WorkspaceAssistantAction(WorkspaceAssistantActionType.ROTATE, "Rotate", enabled = !locked))
            add(WorkspaceAssistantAction(WorkspaceAssistantActionType.DELETE, "Delete", enabled = !locked))
        } else {
            add(WorkspaceAssistantAction(WorkspaceAssistantActionType.ADD, "Add"))
            add(WorkspaceAssistantAction(WorkspaceAssistantActionType.FIT_VIEW, "Fit"))
            add(WorkspaceAssistantAction(WorkspaceAssistantActionType.CLEAR_ALL, "Clear"))
        }
    }

    private fun formulaHints(type: Shape2DType): List<String> = when (type) {
        Shape2DType.Triangle -> listOf("A = 1/2 x base x height", "Interior angle sum = 180 deg")
        Shape2DType.Rectangle -> listOf("A = length x width", "P = 2(length + width)")
        Shape2DType.Square -> listOf("A = side^2", "P = 4 x side")
        Shape2DType.Circle -> listOf("A = pi r^2", "C = 2 pi r")
        else -> emptyList()
    }
}

object WorkspaceAssistantCommandParser {
    fun parse(text: String, state: WorkspaceState): WorkspaceAssistantCommand {
        val normalized = text.lowercase(Locale.US)
        if (normalized.isBlank()) return WorkspaceAssistantCommand(WorkspaceAssistantCommandType.UNKNOWN, reason = "No command text.")
        if ("clear" in normalized && "all" in normalized) return WorkspaceAssistantCommand(WorkspaceAssistantCommandType.CLEAR_ALL)
        if ("fit" in normalized || "home view" in normalized) return WorkspaceAssistantCommand(WorkspaceAssistantCommandType.FIT_VIEW)
        if ("delete" in normalized || "remove" in normalized) return WorkspaceAssistantCommand(WorkspaceAssistantCommandType.DELETE_SELECTED)
        if ("rotate" in normalized || "rot " in normalized) return WorkspaceAssistantCommand(WorkspaceAssistantCommandType.ROTATE_SELECTED, amount = firstNumber(normalized) ?: 15.0)
        if ("resize" in normalized || "scale" in normalized || "size" in normalized || "smaller" in normalized || "bigger" in normalized) {
            val amount = when {
                "smaller" in normalized || "decrease" in normalized || "minus" in normalized -> .9
                "bigger" in normalized || "increase" in normalized || "plus" in normalized -> 1.1
                else -> firstNumber(normalized)?.let { if (it > 10) it / 100.0 else it } ?: 1.1
            }
            return WorkspaceAssistantCommand(WorkspaceAssistantCommandType.RESIZE_SELECTED, amount = amount)
        }
        if ("add" in normalized || "draw" in normalized || "construct" in normalized) {
            Shape2DType.entries.firstOrNull { it.name.lowercase(Locale.US) in normalized }?.let {
                return WorkspaceAssistantCommand(WorkspaceAssistantCommandType.ADD_2D_SHAPE, targetName = it.name)
            }
            SolidType.entries.firstOrNull { it.name.lowercase(Locale.US) in normalized }?.let {
                return WorkspaceAssistantCommand(WorkspaceAssistantCommandType.ADD_3D_SOLID, targetName = it.name)
            }
            return when (state.module) {
                MathModule.Geometry2D, MathModule.CoordinatePlane -> WorkspaceAssistantCommand(WorkspaceAssistantCommandType.ADD_2D_SHAPE, reason = "No shape name supplied.")
                MathModule.Geometry3D -> WorkspaceAssistantCommand(WorkspaceAssistantCommandType.ADD_3D_SOLID, reason = "No solid name supplied.")
                else -> WorkspaceAssistantCommand(WorkspaceAssistantCommandType.UNKNOWN, reason = "Add command is only supported in geometry workspaces.")
            }
        }
        return WorkspaceAssistantCommand(WorkspaceAssistantCommandType.UNKNOWN, reason = "No supported offline workspace command matched.")
    }

    private fun firstNumber(text: String): Double? = Regex("""-?\d+(?:\.\d+)?""").find(text)?.value?.toDoubleOrNull()
}

private fun String.readable(): String = replace(Regex("([a-z])([A-Z])"), "$1 $2").replace('_', ' ').lowercase(Locale.US)
    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }

private fun format(value: Double): String = "%.2f".format(Locale.US, value).trimEnd('0').trimEnd('.')
