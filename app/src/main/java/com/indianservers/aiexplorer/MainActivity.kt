package com.indianservers.aiexplorer

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.ContextCompat
import com.indianservers.aiexplorer.core.ExpressionEngine
import com.indianservers.aiexplorer.core.Geometry2D
import com.indianservers.aiexplorer.core.Geometry3D
import com.indianservers.aiexplorer.core.CrossSection3D
import com.indianservers.aiexplorer.core.Graph3D
import com.indianservers.aiexplorer.core.GraphAnalysis
import com.indianservers.aiexplorer.core.GraphDefinitionKind
import com.indianservers.aiexplorer.core.StatisticsEngine
import com.indianservers.aiexplorer.core.ProbabilityEngine
import com.indianservers.aiexplorer.core.MathProblemSolver
import com.indianservers.aiexplorer.core.ProblemSolution
import com.indianservers.aiexplorer.core.SolutionStepRole
import com.indianservers.aiexplorer.core.Solid
import com.indianservers.aiexplorer.core.SolidType
import com.indianservers.aiexplorer.core.SolidMeshFactory
import com.indianservers.aiexplorer.core.Vec2
import com.indianservers.aiexplorer.core.Vec3
import com.indianservers.aiexplorer.core.Vector3D
import com.indianservers.aiexplorer.core.stripEquation
import com.indianservers.aiexplorer.core.trim
import com.indianservers.aiexplorer.learning.Assignment
import com.indianservers.aiexplorer.learning.ClassroomEngine
import com.indianservers.aiexplorer.learning.LearnerProgress
import com.indianservers.aiexplorer.learning.LearningActivity
import com.indianservers.aiexplorer.learning.LearningCatalog
import com.indianservers.aiexplorer.learning.LearningEvaluator
import com.indianservers.aiexplorer.learning.LearningPackage
import com.indianservers.aiexplorer.learning.LearningOperation
import com.indianservers.aiexplorer.learning.LearningOperationType
import com.indianservers.aiexplorer.learning.LearningRole
import com.indianservers.aiexplorer.learning.LearningValidation
import com.indianservers.aiexplorer.learning.OfflineLearningQueue
import com.indianservers.aiexplorer.learning.PackageValidation
import com.indianservers.aiexplorer.learning.ProgressStatus
import com.indianservers.aiexplorer.workspace.AddVector3DCommand
import com.indianservers.aiexplorer.workspace.AddConstructionCommand
import com.indianservers.aiexplorer.workspace.AddDependentPointCommand
import com.indianservers.aiexplorer.workspace.AddFunctionCommand
import com.indianservers.aiexplorer.workspace.AddPointCommand
import com.indianservers.aiexplorer.workspace.AddSolidCommand
import com.indianservers.aiexplorer.workspace.CommandHistory
import com.indianservers.aiexplorer.workspace.EditExpressionCommand
import com.indianservers.aiexplorer.workspace.DeleteShapeCommand
import com.indianservers.aiexplorer.workspace.DeleteFunctionCommand
import com.indianservers.aiexplorer.workspace.MathModule
import com.indianservers.aiexplorer.workspace.MovePointCommand
import com.indianservers.aiexplorer.workspace.MovePointsCommand
import com.indianservers.aiexplorer.workspace.MoveSolidCommand
import com.indianservers.aiexplorer.workspace.MoveVector3DCommand
import com.indianservers.aiexplorer.workspace.Shape2D
import com.indianservers.aiexplorer.workspace.Shape2DType
import com.indianservers.aiexplorer.workspace.PointDependencyType
import com.indianservers.aiexplorer.workspace.TransformSolidCommand
import com.indianservers.aiexplorer.workspace.TransformVector3DCommand
import com.indianservers.aiexplorer.workspace.TransformSpatialPlacementCommand
import com.indianservers.aiexplorer.workspace.TransformShape2DCommand
import com.indianservers.aiexplorer.workspace.UpdateShapeCommand
import com.indianservers.aiexplorer.workspace.UpdateFunctionCommand
import com.indianservers.aiexplorer.workspace.WorkspaceJson
import com.indianservers.aiexplorer.workspace.WorkspaceState
import com.indianservers.aiexplorer.workspace.recomputed
import com.indianservers.aiexplorer.workspace.resolvePointDependency
import com.indianservers.aiexplorer.spatial.ARScaleMode
import com.indianservers.aiexplorer.spatial.ARAvailability
import com.indianservers.aiexplorer.spatial.ARCapabilities
import com.indianservers.aiexplorer.spatial.ARCoreSessionController
import com.indianservers.aiexplorer.spatial.SpatialSafety
import com.indianservers.aiexplorer.spatial.TrackingQuality
import com.indianservers.aiexplorer.spatial.SpatialPlacementEngine
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.tan
import kotlin.math.round

private val Ink = Color(0xFFEAF5FF)
private val Muted = Color(0xFFB8C4D8)
private val Background = Color(0xFF030507)
private val SurfaceA = Color(0xDD07101A)
private val SurfaceB = Color(0xBB0B1017)
private val Cyan = Color(0xFF20D9FF)
private val Violet = Color(0xFF985DFF)
private val Green = Color(0xFF48E0A4)
private val Amber = Color(0xFFFFC857)
private val Grid = Color(0x3341C8F5)

enum class PanelSlot { Left, Right, Bottom, Chrome }
enum class GeometryTool {
    Select, Point, Midpoint, Intersection, Centroid, Circumcenter, Incenter, Orthocenter,
    Line, Segment, Ray, Vector, Parallel, Perpendicular, AngleBisector,
    Triangle, Polygon, RegularPolygon, Rectangle, Square, Circle, CircleThreePoints, Arc, Ellipse, Measure,
}
enum class GraphTool { Plot, Trace, Tangent, Normal, Derivative, Integral, AreaBetween, Intersections, Extrema, Table, Data, Probability }
enum class SurfaceTool { Surface, Wireframe, Contours, Slice, Gradient, BoundingBox, Trace }
enum class SurfaceViewPreset { Isometric, X, Y, Z, XY, XZ, YZ }
enum class Transform3DMode { Move, Rotate, Scale }
enum class Selection3DMode { Object, Vertex, Edge, Face }
enum class CameraProjection { Perspective, Orthographic }
private data class SubObjectSelection(val solidIndex: Int, val mode: Selection3DMode, val index: Int)

data class AppSettings(
    val haptics: Boolean = true,
    val snap: Boolean = true,
    val highContrast: Boolean = false,
    val reducedMotion: Boolean = false,
    val decimalPrecision: Int = 2,
)

private data class SubjectOption(val title: String, val description: String, val symbol: String, val enabled: Boolean)
private data class MathMenuOption(val title: String, val description: String, val available: Boolean = false)

private val SubjectOptions = listOf(
    SubjectOption("Maths", "Interactive mathematics laboratory", "∑", true),
    SubjectOption("Physics", "Mechanics, waves and fields", "F", false),
    SubjectOption("Chemistry", "Molecules, reactions and matter", "⚗", false),
    SubjectOption("Biology", "Cells, systems and life", "DNA", false),
    SubjectOption("Astro Physics", "Stars, space and cosmology", "✦", false),
    SubjectOption("IQ Labs", "Logic, patterns and reasoning", "IQ", false),
)

private val MathMenuOptions = listOf(
    MathMenuOption("Explore Workspaces", "2D, 3D, graphing, trigonometry and spatial AR", true),
    MathMenuOption("Problem Solver", "Explainable, step-by-step answers with verification", true),
    MathMenuOption("Formulas", "Searchable formula reference"),
    MathMenuOption("MCQs", "Practice questions and explanations"),
    MathMenuOption("Visualize Formulas", "Turn formulas into interactive scenes"),
    MathMenuOption("Theorems", "Statements, conditions and applications"),
    MathMenuOption("Visual Proofs", "Manipulable visual demonstrations"),
    MathMenuOption("Maths Dictionary", "Terms, notation and examples"),
    MathMenuOption("Probability & Statistics", "Data, distributions and probability labs"),
)

private data class PointGesture(
    val indices: List<Int>,
    val from: List<Vec2>,
)

private data class SolidGesture(val index: Int, val from: Solid)
private data class VectorGesture(val index: Int, val from: Vector3D)

data class SavedWorkspace(
    val id: String,
    val name: String,
    val module: MathModule,
    val snapshot: WorkspaceState,
    val json: String,
    val updatedAt: Long,
)

private val LearningActivities = LearningCatalog.lessons

private fun GeometryTool.requiredTapCount(): Int = when (this) {
    GeometryTool.Point -> 1
    GeometryTool.Midpoint -> 2
    GeometryTool.Line, GeometryTool.Segment, GeometryTool.Ray, GeometryTool.Vector,
    GeometryTool.Rectangle, GeometryTool.Square, GeometryTool.Circle, GeometryTool.RegularPolygon -> 2
    GeometryTool.Centroid, GeometryTool.Circumcenter, GeometryTool.Incenter, GeometryTool.Orthocenter,
    GeometryTool.Parallel, GeometryTool.Perpendicular, GeometryTool.AngleBisector,
    GeometryTool.Triangle, GeometryTool.CircleThreePoints, GeometryTool.Arc, GeometryTool.Ellipse -> 3
    GeometryTool.Polygon -> 4
    GeometryTool.Intersection -> 4
    GeometryTool.Select, GeometryTool.Measure -> 0
}

private fun GeometryTool.toShape2DType(): Shape2DType? = when (this) {
    GeometryTool.Line -> Shape2DType.Line
    GeometryTool.Segment -> Shape2DType.Segment
    GeometryTool.Ray -> Shape2DType.Ray
    GeometryTool.Vector -> Shape2DType.Vector
    GeometryTool.Parallel -> Shape2DType.Parallel
    GeometryTool.Perpendicular -> Shape2DType.Perpendicular
    GeometryTool.AngleBisector -> Shape2DType.AngleBisector
    GeometryTool.Triangle -> Shape2DType.Triangle
    GeometryTool.Polygon -> Shape2DType.Polygon
    GeometryTool.RegularPolygon -> Shape2DType.RegularPolygon
    GeometryTool.Rectangle -> Shape2DType.Rectangle
    GeometryTool.Square -> Shape2DType.Square
    GeometryTool.Circle -> Shape2DType.Circle
    GeometryTool.CircleThreePoints -> Shape2DType.CircleThreePoints
    GeometryTool.Arc -> Shape2DType.Arc
    GeometryTool.Ellipse -> Shape2DType.Ellipse
    else -> null
}

private fun GeometryTool.toPointDependencyType(): PointDependencyType? = when (this) {
    GeometryTool.Midpoint -> PointDependencyType.Midpoint
    GeometryTool.Centroid -> PointDependencyType.Centroid
    GeometryTool.Circumcenter -> PointDependencyType.Circumcenter
    GeometryTool.Incenter -> PointDependencyType.Incenter
    GeometryTool.Orthocenter -> PointDependencyType.Orthocenter
    GeometryTool.Intersection -> PointDependencyType.Intersection
    else -> null
}

private fun defaultSolid(type: SolidType): Solid = when (type) {
    SolidType.Cube -> Solid(type, width = 2.0)
    SolidType.Cuboid -> Solid(type, width = 2.4, height = 1.6, depth = 1.4, radius = .8)
    SolidType.Sphere -> Solid(type, width = 2.0, height = 2.0, depth = 2.0, radius = 1.0)
    SolidType.Hemisphere -> Solid(type, width = 2.0, height = 1.0, depth = 2.0, radius = 1.0)
    SolidType.Cylinder -> Solid(type, width = 2.0, height = 2.4, depth = 2.0, radius = .9)
    SolidType.Cone -> Solid(type, width = 2.0, height = 2.5, depth = 2.0, radius = .9)
    SolidType.Frustum -> Solid(type, width = 2.0, height = 2.4, depth = 2.0, radius = 1.0, topRadius = .55)
    SolidType.Pyramid -> Solid(type, width = 2.2, height = 2.4, depth = 2.2, radius = .9)
    SolidType.TriangularPrism -> Solid(type, width = 2.2, height = 2.0, depth = 2.6)
    SolidType.Tetrahedron -> Solid(type, width = 2.4)
    SolidType.Octahedron -> Solid(type, width = 2.4)
    SolidType.Torus -> Solid(type, width = 1.15, height = 1.0, depth = 1.0, radius = .42)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AIExplorerApp() }
    }
}

class ExplorerViewModel : ViewModel() {
    private val history = CommandHistory()
    private val learningQueue = OfflineLearningQueue()
    private var pointGesture: PointGesture? = null
    private var solidGesture: SolidGesture? = null
    private var vectorGesture: VectorGesture? = null
    private var spatialGestureFrom: com.indianservers.aiexplorer.spatial.SpatialScenePlacement? = null
    var state by mutableStateOf(WorkspaceState())
        private set
    var selectedPoint by mutableIntStateOf(1)
        private set
    var selectedShape by mutableIntStateOf(-1)
        private set
    var status by mutableStateOf("Ready")
        private set
    var showLeftPanel by mutableStateOf(false)
        private set
    var showRightPanel by mutableStateOf(false)
        private set
    var showBottomPanel by mutableStateOf(false)
        private set
    var showChrome by mutableStateOf(true)
        private set
    var showLearningPanel by mutableStateOf(false)
        private set
    var showSubjectHub by mutableStateOf(true)
        private set
    var showMathMenu by mutableStateOf(false)
        private set
    var showProblemSolver by mutableStateOf(false)
        private set
    var showActionDock by mutableStateOf(false)
        private set
    var geometryTool by mutableStateOf(GeometryTool.Select)
        private set
    var selectedSolid by mutableIntStateOf(0)
        private set
    var selectedVector3D by mutableIntStateOf(0)
        private set
    var pendingConstruction by mutableStateOf<List<Vec2>>(emptyList())
        private set
    private var pendingPointIndices by mutableStateOf<List<Int?>>(emptyList())
    var activeActivityId by mutableStateOf(LearningActivities.first().id)
        private set
    var lessonProgress by mutableStateOf<Map<String, LearnerProgress>>(emptyMap())
        private set
    var learningRole by mutableStateOf(LearningRole.Learner)
        private set
    var assignments by mutableStateOf<List<Assignment>>(LearningCatalog.defaultAssignments)
        private set
    var savedWorkspaces by mutableStateOf<List<SavedWorkspace>>(emptyList())
        private set
    var settings by mutableStateOf(AppSettings())
        private set
    var lastValidation by mutableStateOf(LearningValidation(false, "Start an activity and validate your construction."))
        private set
    var lastPackageValidation by mutableStateOf(PackageValidation(true, "Package ready for validation.", LearningPackage.schemaVersion))
        private set

    val activeActivity: LearningActivity
        get() = LearningActivities.firstOrNull { it.id == activeActivityId } ?: LearningActivities.first()
    val completedActivities: Set<String>
        get() = lessonProgress.filterValues { it.status == ProgressStatus.Completed }.keys
    val activeProgress: LearnerProgress?
        get() = lessonProgress[activeActivityId]
    val teacherSummary
        get() = ClassroomEngine.summarize(assignments.first(), LearningActivities, lessonProgress)
    val pendingLearningOperations: Int
        get() = learningQueue.pending().size

    val constructionProtocol: List<String> get() = history.protocol

    fun open(module: MathModule) {
        state = state.copy(module = module)
        showProblemSolver = false
        showMathMenu = false
        hidePanels()
    }

    fun enterMaths() {
        showSubjectHub = false
        showProblemSolver = false
        showMathMenu = false
        status = "Mathematics Explorer"
    }

    fun openProblemSolver() {
        showSubjectHub = false
        showProblemSolver = true
        showMathMenu = false
        showActionDock = false
        hidePanels()
        status = "Explainable Problem Solver"
    }

    fun openSubjectHub() {
        showSubjectHub = true
        showProblemSolver = false
        showMathMenu = false
        hidePanels()
        status = "Choose a subject"
    }

    fun toggleMathMenu() {
        showMathMenu = !showMathMenu
        if (showMathMenu) hideWorkspacePanelsOnly()
    }

    fun toggleActionDock() {
        showActionDock = !showActionDock
    }

    fun hideActionDock() {
        showActionDock = false
    }

    fun togglePanel(slot: PanelSlot) {
        when (slot) {
            PanelSlot.Left -> showLeftPanel = !showLeftPanel
            PanelSlot.Right -> showRightPanel = !showRightPanel
            PanelSlot.Bottom -> showBottomPanel = !showBottomPanel
            PanelSlot.Chrome -> showChrome = !showChrome
        }
    }

    fun hidePanels() {
        showLeftPanel = false
        showRightPanel = false
        showBottomPanel = false
        showLearningPanel = false
    }

    fun toggleLearningPanel() {
        showLearningPanel = !showLearningPanel
        if (showLearningPanel) hideWorkspacePanelsOnly()
    }

    private fun hideWorkspacePanelsOnly() {
        showLeftPanel = false
        showRightPanel = false
        showBottomPanel = false
    }

    fun startActivity(activity: LearningActivity) {
        activeActivityId = activity.id
        state = state.copy(module = activity.module)
        showLearningPanel = true
        hideWorkspacePanelsOnly()
        geometryTool = if (activity.id == "triangle-angle-sum") GeometryTool.Triangle else geometryTool
        val now = System.currentTimeMillis()
        if (lessonProgress[activity.id] == null) {
            lessonProgress = lessonProgress + (activity.id to LearnerProgress(activity.id, ProgressStatus.InProgress, startedAt = now, updatedAt = now))
        }
        lastValidation = LearningValidation(false, activity.checkpoints.firstOrNull()?.instruction ?: activity.objective)
        status = "Activity: ${activity.title}"
    }

    fun validateActiveActivity(): LearningValidation {
        val result = validateActivity(activeActivity)
        val now = System.currentTimeMillis()
        learningQueue.enqueue(LearningOperation("attempt-$activeActivityId-$now", activeActivityId, LearningOperationType.Attempt, now))
        lessonProgress = lessonProgress + (
            activeActivityId to LearningEvaluator.recordAttempt(activeActivity, lessonProgress[activeActivityId], result, now)
        )
        lastValidation = result
        status = if (result.passed) "Validation passed" else result.message
        return result
    }

    fun completeActiveActivity() {
        val result = validateActiveActivity()
        if (result.passed) {
            val now = System.currentTimeMillis()
            learningQueue.enqueue(LearningOperation("complete-$activeActivityId-$now", activeActivityId, LearningOperationType.Complete, now))
            status = "Completed ${activeActivity.title}"
        }
    }

    private fun validateActivity(activity: LearningActivity): LearningValidation = LearningEvaluator.evaluate(activity, state)

    fun revealHint() {
        val current = lessonProgress[activeActivityId] ?: LearnerProgress(activeActivityId, ProgressStatus.InProgress)
        val hint = LearningEvaluator.nextHint(activeActivity, lastValidation, current.hintsUsed)
        val now = System.currentTimeMillis()
        lessonProgress = lessonProgress + (activeActivityId to current.copy(hintsUsed = current.hintsUsed + 1, updatedAt = now))
        learningQueue.enqueue(LearningOperation("hint-$activeActivityId-$now", activeActivityId, LearningOperationType.HintUsed, now))
        lastValidation = lastValidation.copy(message = hint)
        status = "Hint ${current.hintsUsed + 1} revealed"
    }

    fun switchLearningRole(role: LearningRole) {
        learningRole = role
        status = "${role.name} view"
    }

    fun selectGeometryTool(tool: GeometryTool) {
        geometryTool = tool
        pendingConstruction = emptyList()
        pendingPointIndices = emptyList()
        status = "${tool.name.lowercase().replaceFirstChar { it.uppercase() }} tool selected"
    }

    fun addPoint(point: Vec2) {
        state = history.execute(state, AddPointCommand(point))
        selectedPoint = state.points.lastIndex
        status = "Added point ${selectedPoint + 1}"
    }

    fun handleGeometryTap(point: Vec2, hitPointIndex: Int?) {
        val snapped = hitPointIndex?.let(state.points::getOrNull) ?: if (settings.snap) {
            Vec2(round(point.x * 2.0) / 2.0, round(point.y * 2.0) / 2.0)
        } else point
        when (geometryTool) {
            GeometryTool.Select, GeometryTool.Measure -> return
            GeometryTool.Point -> {
                if (hitPointIndex != null) {
                    selectedPoint = hitPointIndex
                    status = "Selected point ${hitPointIndex + 1}"
                    return
                }
                state = history.execute(state, AddConstructionCommand(listOf(snapped), null))
                selectedPoint = state.points.lastIndex
                status = "Point placed"
            }
            else -> {
                val dependencyType = geometryTool.toPointDependencyType()
                if (dependencyType != null && hitPointIndex == null) {
                    status = "${geometryTool.name}: tap an existing point"
                    return
                }
                val next = pendingConstruction + snapped
                val nextIndices = pendingPointIndices + hitPointIndex
                val required = geometryTool.requiredTapCount()
                if (next.size >= required) {
                    if (dependencyType != null) {
                        val inputs = nextIndices.take(required).filterNotNull()
                        state = history.execute(state, AddDependentPointCommand(inputs, dependencyType))
                        selectedPoint = state.points.lastIndex
                    } else {
                        val shapeType = geometryTool.toShape2DType()
                        state = history.execute(
                            state,
                            AddConstructionCommand(next.take(required), shapeType, nextIndices.take(required)),
                        )
                        selectedShape = state.shapes.lastIndex
                    }
                    pendingConstruction = emptyList()
                    pendingPointIndices = emptyList()
                    status = "${geometryTool.name.lowercase().replaceFirstChar { it.uppercase() }} created"
                } else {
                    pendingConstruction = next
                    pendingPointIndices = nextIndices
                    status = "${geometryTool.name}: tap ${next.size + 1} of $required"
                }
            }
        }
    }

    fun movePoint(index: Int, point: Vec2) {
        val from = state.points[index]
        state = history.execute(state, MovePointCommand(index, from, point))
        selectedPoint = index
        status = "Moved point ${index + 1}"
    }

    fun beginPointDrag(index: Int) {
        val point = state.points.getOrNull(index) ?: return
        state.shapes.firstOrNull { it.locked && index in it.pointIndices }?.let {
            selectedPoint = index
            status = "${it.name} is locked"
            return
        }
        if (state.pointDependencies.any { it.outputIndex == index }) {
            selectedPoint = index
            status = "Dependent point: move its parent points"
            return
        }
        pointGesture = PointGesture(listOf(index), listOf(point))
        selectedPoint = index
        selectedShape = -1
    }

    fun beginShapeDrag(shapeIndex: Int) {
        val shape = state.shapes.getOrNull(shapeIndex) ?: return
        selectedShape = shapeIndex
        if (shape.locked) {
            status = "${shape.name} is locked"
            return
        }
        val indices = shape.pointIndices.distinct().filterNot { index -> state.pointDependencies.any { it.outputIndex == index } }
        if (indices.isEmpty()) return
        pointGesture = PointGesture(indices, indices.mapNotNull(state.points::getOrNull))
    }

    fun previewPointDrag(index: Int, point: Vec2) {
        val gesture = pointGesture ?: return
        if (index !in gesture.indices) return
        state = state.copy(
            points = state.points.mapIndexed { i, old -> if (i == index) point else old },
            modifiedAt = System.currentTimeMillis(),
        ).recomputed()
        selectedPoint = index
    }

    fun previewShapeDrag(delta: Vec2) {
        val gesture = pointGesture ?: return
        val replacements = gesture.indices.zip(gesture.from.map { it + delta }).toMap()
        state = state.copy(
            points = state.points.mapIndexed { index, old -> replacements[index] ?: old },
            modifiedAt = System.currentTimeMillis(),
        ).recomputed()
    }

    fun endPointDrag() {
        val gesture = pointGesture ?: return
        val final = gesture.indices.mapNotNull(state.points::getOrNull)
        if (final != gesture.from) history.recordApplied(MovePointsCommand(gesture.indices, gesture.from, final))
        pointGesture = null
        status = "Moved object"
    }

    fun cancelPointDrag() {
        val gesture = pointGesture ?: return
        val replacements = gesture.indices.zip(gesture.from).toMap()
        state = state.copy(points = state.points.mapIndexed { index, old -> replacements[index] ?: old }).recomputed()
        pointGesture = null
        status = "Move cancelled"
    }

    fun selectShape(index: Int) {
        if (index !in state.shapes.indices) return
        selectedShape = index
        status = "Selected ${state.shapes[index].name}"
    }

    fun updateSelectedShape(transform: (Shape2D) -> Shape2D) {
        val index = selectedShape.takeIf { it in state.shapes.indices } ?: return
        val from = state.shapes[index]
        val to = transform(from)
        if (from == to) return
        state = history.execute(state, UpdateShapeCommand(index, from, to))
        status = "Updated ${to.name}"
    }

    fun deleteSelectedShape() {
        val index = selectedShape.takeIf { it in state.shapes.indices } ?: return
        val shape = state.shapes[index]
        state = history.execute(state, DeleteShapeCommand(index, shape))
        selectedShape = (index - 1).coerceAtMost(state.shapes.lastIndex)
        status = "Deleted ${shape.name}"
    }

    fun transformSelectedShape(type: PointDependencyType, parameters: List<Double> = emptyList()) {
        val index = selectedShape.takeIf { it in state.shapes.indices } ?: return
        state = history.execute(state, TransformShape2DCommand(index, type, parameters))
        selectedShape = state.shapes.lastIndex
        status = "${type.name} created"
    }

    fun editExpression(index: Int, expression: String) {
        val from = state.functions[index].expression
        state = history.execute(state, EditExpressionCommand(index, from, expression))
        status = "Expression updated"
    }

    fun addFunction(expression: String = "sin(x)") {
        val index = state.functions.size
        val name = ('f'.code + index).toChar().toString() + "(x)"
        val color = listOf("cyan", "violet", "green", "amber")[index % 4]
        state = history.execute(
            state,
            AddFunctionCommand(
                com.indianservers.aiexplorer.core.FunctionDefinition(
                    id = "function-${System.currentTimeMillis()}",
                    name = name,
                    expression = expression,
                    colorKey = color,
                ),
            ),
        )
        status = "Added $name"
    }

    fun updateFunction(index: Int, transform: (com.indianservers.aiexplorer.core.FunctionDefinition) -> com.indianservers.aiexplorer.core.FunctionDefinition) {
        val from = state.functions.getOrNull(index) ?: return
        val to = transform(from)
        if (from == to) return
        state = history.execute(state, UpdateFunctionCommand(index, from, to))
        status = "Updated ${to.name}"
    }

    fun deleteFunction(index: Int) {
        val function = state.functions.getOrNull(index) ?: return
        state = history.execute(state, DeleteFunctionCommand(index, function))
        status = "Deleted ${function.name}"
    }

    fun addSolid(type: SolidType) {
        val position = Vec3(((state.solids.size % 5) - 2) * 1.7, 0.0, ((state.solids.size / 5) - 1) * 1.6)
        val solid = defaultSolid(type).copy(position = position)
        state = history.execute(state, AddSolidCommand(solid))
        selectedSolid = state.solids.lastIndex
        status = "Added ${type.name}"
    }

    fun selectSolid(index: Int) {
        selectedSolid = index.coerceIn(0, state.solids.lastIndex.coerceAtLeast(0))
        status = "Selected ${state.solids.getOrNull(selectedSolid)?.type?.name ?: "solid"}"
    }

    fun moveSolid(index: Int, to: Vec3) {
        val solid = state.solids.getOrNull(index) ?: return
        state = history.execute(state, MoveSolidCommand(index, solid.position, to))
        selectedSolid = index
        status = "Moved ${solid.type.name}"
    }

    fun beginSolidDrag(index: Int) {
        val solid = state.solids.getOrNull(index) ?: return
        solidGesture = SolidGesture(index, solid)
        selectSolid(index)
    }

    fun previewSolidDrag(index: Int, delta: Vec3) {
        val gesture = solidGesture?.takeIf { it.index == index } ?: return
        state = state.copy(
            solids = state.solids.mapIndexed { i, old -> if (i == index) gesture.from.copy(position = gesture.from.position + delta) else old },
            modifiedAt = System.currentTimeMillis(),
        )
    }

    fun previewSolidRotation(index: Int, deltaDegrees: Vec3) {
        val gesture = solidGesture?.takeIf { it.index == index } ?: return
        val base = gesture.from.rotation
        val rotated = gesture.from.copy(rotation = base + deltaDegrees)
        state = state.copy(
            solids = state.solids.mapIndexed { i, old -> if (i == index) rotated else old },
            modifiedAt = System.currentTimeMillis(),
        )
    }

    fun previewSolidScale(index: Int, factor: Double) {
        val gesture = solidGesture?.takeIf { it.index == index } ?: return
        val f = factor.coerceIn(.2, 5.0)
        val scaled = gesture.from.copy(
            width = (gesture.from.width * f).coerceIn(.2, 12.0),
            height = (gesture.from.height * f).coerceIn(.2, 12.0),
            depth = (gesture.from.depth * f).coerceIn(.2, 12.0),
            radius = (gesture.from.radius * f).coerceIn(.1, 6.0),
            topRadius = (gesture.from.topRadius * f).coerceIn(.05, 6.0),
        )
        state = state.copy(
            solids = state.solids.mapIndexed { i, old -> if (i == index) scaled else old },
            modifiedAt = System.currentTimeMillis(),
        )
    }

    fun endSolidDrag() {
        val gesture = solidGesture ?: return
        val final = state.solids.getOrNull(gesture.index)
        if (final != null && final != gesture.from) history.recordApplied(TransformSolidCommand(gesture.index, gesture.from, final))
        solidGesture = null
        status = "Moved ${gesture.from.type.name}"
    }

    fun cancelSolidDrag() {
        val gesture = solidGesture ?: return
        state = state.copy(solids = state.solids.mapIndexed { i, old -> if (i == gesture.index) gesture.from else old })
        solidGesture = null
        status = "Move cancelled"
    }

    fun transformSolid(index: Int, transform: (Solid) -> Solid) {
        val from = state.solids.getOrNull(index) ?: return
        val to = transform(from)
        state = history.execute(state, TransformSolidCommand(index, from, to))
        selectedSolid = index
        status = "Transformed ${to.type.name}"
    }

    fun addVector3D() {
        val n = state.vectors3D.size + 1
        val offset = (n - 2) * .55
        val vector = Vector3D(
            id = "w$n",
            name = "w$n",
            start = Vec3(-1.5 + offset, -1.0, -1.0),
            end = Vec3(1.4 + offset, 1.0, 1.2),
        )
        state = history.execute(state, AddVector3DCommand(vector))
        selectedVector3D = state.vectors3D.lastIndex
        status = "Added 3D vector ${vector.name}"
    }

    fun selectVector3D(index: Int) {
        selectedVector3D = index.coerceIn(0, state.vectors3D.lastIndex.coerceAtLeast(0))
        status = "Selected vector ${state.vectors3D.getOrNull(selectedVector3D)?.name ?: ""}"
    }

    fun moveVector3D(index: Int, delta: Vec3) {
        val from = state.vectors3D.getOrNull(index) ?: return
        val to = from.copy(start = from.start + delta, end = from.end + delta)
        state = history.execute(state, MoveVector3DCommand(index, from, to))
        selectedVector3D = index
        status = "Moved vector ${from.name}"
    }

    fun beginVectorDrag(index: Int) {
        val vector = state.vectors3D.getOrNull(index) ?: return
        vectorGesture = VectorGesture(index, vector)
        selectVector3D(index)
    }

    fun previewVectorDrag(index: Int, delta: Vec3) {
        val gesture = vectorGesture?.takeIf { it.index == index } ?: return
        val moved = gesture.from.copy(start = gesture.from.start + delta, end = gesture.from.end + delta)
        state = state.copy(
            vectors3D = state.vectors3D.mapIndexed { i, old -> if (i == index) moved else old },
            modifiedAt = System.currentTimeMillis(),
        )
    }

    fun endVectorDrag() {
        val gesture = vectorGesture ?: return
        val final = state.vectors3D.getOrNull(gesture.index)
        if (final != null && final != gesture.from) history.recordApplied(TransformVector3DCommand(gesture.index, gesture.from, final))
        vectorGesture = null
        status = "Moved vector ${gesture.from.name}"
    }

    fun cancelVectorDrag() {
        val gesture = vectorGesture ?: return
        state = state.copy(vectors3D = state.vectors3D.mapIndexed { i, old -> if (i == gesture.index) gesture.from else old })
        vectorGesture = null
        status = "Move cancelled"
    }

    fun transformVector3D(index: Int, transform: (Vector3D) -> Vector3D) {
        val from = state.vectors3D.getOrNull(index) ?: return
        val to = transform(from)
        state = history.execute(state, TransformVector3DCommand(index, from, to))
        selectedVector3D = index
        status = "Transformed vector ${to.name}"
    }

    fun setSurfaceExpression(value: String) {
        state = state.copy(surfaceExpression = value)
    }

    fun transformSpatialPlacement(label: String = "Transform spatial scene", transform: (com.indianservers.aiexplorer.spatial.SpatialScenePlacement) -> com.indianservers.aiexplorer.spatial.SpatialScenePlacement) {
        val from = state.spatialPlacement
        val to = transform(from)
        if (from == to) return
        state = history.execute(state, TransformSpatialPlacementCommand(from, to, label))
        status = label
    }

    fun placeSpatialScene() = transformSpatialPlacement("Place scene in space") {
        SpatialPlacementEngine.place(it, Vec3(0.0, 0.0, -1.2), System.currentTimeMillis())
            .copy(trackingQuality = TrackingQuality.Stopped, estimated = true)
    }

    fun resetSpatialScene() = transformSpatialPlacement("Reset spatial anchor") {
        com.indianservers.aiexplorer.spatial.SpatialScenePlacement()
    }

    fun setSpatialScaleMode(mode: ARScaleMode) = transformSpatialPlacement("Set ${mode.name} scale") {
        SpatialPlacementEngine.setScaleMode(it, mode)
    }

    fun setDepthOcclusion(enabled: Boolean) = transformSpatialPlacement("${if (enabled) "Enable" else "Disable"} depth occlusion") {
        it.copy(depthOcclusionEnabled = enabled)
    }

    fun beginSpatialGesture() {
        spatialGestureFrom = state.spatialPlacement
    }

    fun previewSpatialGesture(panPixels: Offset, rotationDegrees: Float, scaleFactor: Float) {
        val from = spatialGestureFrom ?: return
        val moved = SpatialPlacementEngine.move(from, Vec3((panPixels.x / 520f).toDouble(), 0.0, (panPixels.y / 520f).toDouble()))
        val rotated = SpatialPlacementEngine.rotate(moved, Vec3(0.0, rotationDegrees.toDouble(), 0.0))
        val transformed = if (kotlin.math.abs(scaleFactor - 1f) > .001f) SpatialPlacementEngine.scale(rotated, scaleFactor.toDouble()) else rotated
        state = state.copy(spatialPlacement = transformed)
    }

    fun endSpatialGesture() {
        val from = spatialGestureFrom ?: return
        val to = state.spatialPlacement
        spatialGestureFrom = null
        if (from != to) history.recordApplied(TransformSpatialPlacementCommand(from, to, "Manipulate spatial scene"))
    }

    fun cancelSpatialGesture() {
        val from = spatialGestureFrom ?: return
        state = state.copy(spatialPlacement = from)
        spatialGestureFrom = null
    }

    fun undo() {
        state = history.undo(state)
        status = "Undo"
    }

    fun redo() {
        state = history.redo(state)
        status = "Redo"
    }

    fun reset() {
        state = WorkspaceState(module = state.module)
        status = "Workspace reset"
    }

    fun exportJson(): String {
        status = "Workspace JSON generated"
        return WorkspaceJson.export(state)
    }

    fun saveWorkspace() {
        val saved = SavedWorkspace(
            id = "workspace-${System.currentTimeMillis()}",
            name = "${state.name} ${savedWorkspaces.size + 1}",
            module = state.module,
            snapshot = state,
            json = WorkspaceJson.export(state),
            updatedAt = System.currentTimeMillis(),
        )
        savedWorkspaces = listOf(saved) + savedWorkspaces.take(7)
        status = "Workspace saved"
    }

    fun duplicateWorkspace(saved: SavedWorkspace) {
        val copy = saved.copy(
            id = "workspace-${System.currentTimeMillis()}",
            name = "${saved.name} Copy",
            updatedAt = System.currentTimeMillis(),
        )
        savedWorkspaces = listOf(copy) + savedWorkspaces
        status = "Workspace duplicated"
    }

    fun deleteWorkspace(saved: SavedWorkspace) {
        savedWorkspaces = savedWorkspaces.filterNot { it.id == saved.id }
        status = "Workspace deleted"
    }

    fun restoreWorkspaceShell(saved: SavedWorkspace) {
        state = saved.snapshot.copy(name = saved.name, module = saved.module, modifiedAt = System.currentTimeMillis())
        status = "Opened ${saved.name}"
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        settings = transform(settings)
        status = "Settings updated"
    }

    fun exportLearningPackage(): String {
        val source = learningPackageJson()
        lastPackageValidation = LearningPackage.validate(source)
        status = lastPackageValidation.message
        return source
    }

    fun exportLearningPackagePreview(): String = learningPackageJson().lineSequence().take(8).joinToString("\n")

    fun validateLearningPackage(source: String): Boolean {
        lastPackageValidation = LearningPackage.validate(source)
        status = lastPackageValidation.message
        return lastPackageValidation.valid
    }

    private fun learningPackageJson(): String = LearningPackage.export(state, activeActivityId, lessonProgress, assignments)
}

@Composable
fun AIExplorerApp(vm: ExplorerViewModel = viewModel()) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Background,
            surface = Color(0xFF070A0F),
            primary = Cyan,
            secondary = Violet,
            onBackground = Ink,
            onSurface = Ink,
        ),
    ) {
        Surface(Modifier.fillMaxSize(), color = Background) {
            BoxWithConstraints(
                Modifier
                    .fillMaxSize()
                    .background(radialBackdrop())
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(8.dp),
            ) {
                val compact = maxWidth < 520.dp
                val wide = maxWidth >= 760.dp
                if (vm.showSubjectHub) {
                    SubjectHubScreen(
                        modifier = Modifier.fillMaxSize(),
                        wide = wide,
                        onOpenMaths = vm::enterMaths,
                    )
                } else {
                    if (vm.showProblemSolver) {
                        ProblemSolverScreen(vm, wide = wide)
                    } else {
                        when (vm.state.module) {
                            MathModule.Geometry2D -> Geometry2DScreen(vm)
                            MathModule.Geometry3D -> Geometry3DScreen(vm)
                            MathModule.Graph2D -> Graph2DScreen(vm)
                            MathModule.Graph3D -> Graph3DScreen(vm)
                            MathModule.Trigonometry -> TrigonometryScreen(vm)
                            MathModule.SpatialAR -> SpatialARScreen(vm)
                        }
                    }
                    if (vm.showChrome) TopShell(vm, compact, Modifier.align(Alignment.TopCenter))
                    if (vm.showLearningPanel && !vm.showProblemSolver) LearningCoachPanel(vm, Modifier.align(Alignment.CenterEnd))
                    BottomModeSelector(vm.state.module, vm::open, Modifier.align(Alignment.BottomCenter))
                    if (vm.showActionDock && !vm.showProblemSolver) MiniDock(
                        modifier = Modifier.align(Alignment.TopEnd).padding(top = if (compact) 74.dp else 82.dp),
                        items = listOf("Focus", "Learn", "Tools", "Info", "Panel", "Export", "Close"),
                        onClick = {
                            when (it) {
                                "Focus" -> vm.togglePanel(PanelSlot.Chrome)
                                "Learn" -> vm.toggleLearningPanel()
                                "Tools" -> vm.togglePanel(PanelSlot.Left)
                                "Info" -> vm.togglePanel(PanelSlot.Right)
                                "Panel" -> vm.togglePanel(PanelSlot.Bottom)
                                "Export" -> vm.exportJson()
                                "Close" -> vm.hideActionDock()
                            }
                        },
                    )
                    if (vm.showMathMenu) MathematicsMenuPanel(
                        vm = vm,
                        modifier = Modifier
                            .align(if (wide) Alignment.CenterStart else Alignment.Center)
                            .widthIn(max = if (wide) 460.dp else 390.dp)
                            .fillMaxWidth(if (wide) .46f else .94f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SubjectHubScreen(modifier: Modifier = Modifier, wide: Boolean, onOpenMaths: () -> Unit) {
    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = if (wide) 42.dp else 12.dp, vertical = if (wide) 28.dp else 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("AI Explorer", color = Ink, fontSize = if (wide) 42.sp else 30.sp, fontWeight = FontWeight.ExtraBold)
        Text("Choose a learning laboratory", color = Muted, fontSize = if (wide) 20.sp else 15.sp)
        Text("Maths is available now · more sciences are being prepared", color = Cyan, fontSize = 12.sp, textAlign = TextAlign.Center)
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SubjectOptions.forEach { subject ->
                Column(
                    Modifier
                        .width(if (wide) 250.dp else 158.dp)
                        .heightIn(min = if (wide) 170.dp else 142.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (subject.enabled) Brush.linearGradient(listOf(Color(0x6630D9FF), Color(0x5538E6B0))) else Brush.linearGradient(listOf(SurfaceA, SurfaceB)))
                        .border(1.5.dp, if (subject.enabled) Cyan else Muted.copy(.35f), RoundedCornerShape(24.dp))
                        .clickable(enabled = subject.enabled) { onOpenMaths() }
                        .focusable()
                        .semantics { contentDescription = if (subject.enabled) "Open ${subject.title} laboratory" else "${subject.title}, coming soon" }
                        .padding(18.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(subject.symbol, color = if (subject.enabled) Color.White else Muted, fontSize = if (wide) 34.sp else 27.sp, fontWeight = FontWeight.Bold)
                    Column {
                        Text(subject.title, color = if (subject.enabled) Ink else Muted, fontSize = if (wide) 22.sp else 17.sp, fontWeight = FontWeight.Bold)
                        Text(subject.description, color = Muted, fontSize = 11.sp)
                        Text(if (subject.enabled) "OPEN" else "COMING SOON", color = if (subject.enabled) Green else Amber.copy(.75f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Text("Touch, mouse, keyboard and TV remote ready", color = Muted, fontSize = 11.sp)
    }
}

@Composable
private fun MathematicsMenuPanel(vm: ExplorerViewModel, modifier: Modifier = Modifier) {
    GlassPanel(modifier) {
        PanelHeader("Mathematics Menu", vm::toggleMathMenu, Cyan)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlowButton("Subjects", onClick = vm::openSubjectHub)
            GlowButton("Current Workspace", onClick = vm::toggleMathMenu)
        }
        MathMenuOptions.forEach { option ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (option.available) Color(0x4430D9FF) else Color(0x22101824))
                    .border(1.dp, if (option.available) Cyan else Muted.copy(.28f), RoundedCornerShape(16.dp))
                    .clickable(enabled = option.available) {
                        if (option.title == "Problem Solver") vm.openProblemSolver() else vm.toggleMathMenu()
                    }
                    .focusable()
                    .semantics { contentDescription = "${option.title}, ${if (option.available) "available" else "planned for a future update"}" }
                    .padding(13.dp),
            ) {
                Text(option.title, color = if (option.available) Ink else Muted, fontWeight = FontWeight.SemiBold)
                Text(option.description, color = Muted, fontSize = 11.sp)
                Text(if (option.available) "AVAILABLE" else "PLANNED", color = if (option.available) Green else Amber, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
        Text("Workspaces", color = Ink, fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            MathModule.entries.forEach { module -> GlowButton(module.label, onClick = { vm.open(module) }) }
        }
    }
}

private fun radialBackdrop() = Brush.radialGradient(
    colors = listOf(Color(0x3320D9FF), Background, Background),
    radius = 1100f,
    center = Offset(420f, 220f),
)

@Composable
private fun TopShell(vm: ExplorerViewModel, compact: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlowButton("Menu", onClick = vm::toggleMathMenu)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("AI Explorer", color = Ink, fontSize = if (compact) 20.sp else 27.sp, fontWeight = FontWeight.ExtraBold)
            Text("Maths · ${vm.state.module.label}", color = Muted, fontSize = if (compact) 10.sp else 12.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 8.dp)) {
            GlowButton(if (compact) "↶" else "Undo", enabled = true, onClick = vm::undo)
            GlowButton(if (compact) "↷" else "Redo", enabled = true, onClick = vm::redo)
            GlowButton(if (compact) "⋮" else "More", enabled = true, onClick = vm::toggleActionDock)
        }
    }
}

@Composable
private fun LearningCoachPanel(vm: ExplorerViewModel, modifier: Modifier = Modifier) {
    val activity = vm.activeActivity
    val progress = "${vm.completedActivities.size}/${LearningActivities.size}"
    GlassPanel(
        modifier
            .width(320.dp)
            .padding(top = 112.dp, end = 8.dp),
    ) {
        PanelHeader("Learning Coach", vm::hidePanels, Green)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlowButton(if (vm.learningRole == LearningRole.Learner) "• Learner" else "Learner", onClick = { vm.switchLearningRole(LearningRole.Learner) })
            GlowButton(if (vm.learningRole == LearningRole.Teacher) "• Teacher" else "Teacher", onClick = { vm.switchLearningRole(LearningRole.Teacher) })
        }
        Text("Progress $progress · ${vm.activeProgress?.percent(activity) ?: 0}% current", color = Cyan, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text(activity.title, color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(activity.objective, color = Muted, fontSize = 13.sp)
        Insight("Allowed", activity.allowedTools.joinToString().ifBlank { "Open exploration" }, Cyan)
        Insight("Proof", activity.proof, Violet)
        Insight("Validation", vm.lastValidation.message, if (vm.lastValidation.passed) Green else Amber)
        activity.checkpoints.forEach { checkpoint ->
            val result = vm.lastValidation.checkpoints.firstOrNull { it.checkpointId == checkpoint.id }
            val completed = checkpoint.id in vm.activeProgress?.completedCheckpointIds.orEmpty()
            Insight(
                if (completed || result?.passed == true) "✓ ${checkpoint.title}" else "○ ${checkpoint.title}",
                result?.message ?: checkpoint.instruction,
                if (completed || result?.passed == true) Green else Cyan,
            )
            result?.misconception?.let { Insight("Why not yet", it, Amber) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlowButton("Go", onClick = { vm.startActivity(activity) })
            GlowButton("Check", onClick = { vm.validateActiveActivity() })
            GlowButton("Hint", onClick = vm::revealHint)
            GlowButton("Done", onClick = vm::completeActiveActivity)
            GlowButton("Save", onClick = vm::saveWorkspace)
            GlowButton("Package", onClick = { vm.exportLearningPackage() })
        }
        vm.activeProgress?.let {
            Text("Attempts ${it.attempts} · hints ${it.hintsUsed} · offline changes ${vm.pendingLearningOperations}", color = Muted, fontSize = 11.sp)
        }
        if (vm.learningRole == LearningRole.Teacher) {
            val summary = vm.teacherSummary
            Text("Teacher dashboard", color = Ink, fontWeight = FontWeight.SemiBold)
            Insight("Assignment", vm.assignments.first().title, Violet)
            Insight("Completion", "${summary.completedLessons}/${summary.assignedLessons} lessons · ${summary.checkpointsCompleted} checkpoints", Green)
            Insight("Support", "${summary.attempts} attempts · ${summary.hintsUsed} hints", Amber)
            Insight("Needs attention", summary.needsAttention.joinToString().ifBlank { "No learner flags" }, summary.needsAttention.takeIf { it.isNotEmpty() }?.let { Amber } ?: Green)
        }
        Text("Settings", color = Ink, fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TogglePill("Haptics", vm.settings.haptics) { value -> vm.updateSettings { it.copy(haptics = value) } }
            TogglePill("Snap", vm.settings.snap) { value -> vm.updateSettings { it.copy(snap = value) } }
            TogglePill("High contrast", vm.settings.highContrast) { value -> vm.updateSettings { it.copy(highContrast = value) } }
            TogglePill("Reduced motion", vm.settings.reducedMotion) { value -> vm.updateSettings { it.copy(reducedMotion = value) } }
        }
        Text("Activities", color = Ink, fontWeight = FontWeight.SemiBold)
        Column(
            Modifier
                .height(220.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LearningActivities.forEach { item ->
                val done = item.id in vm.completedActivities
                val active = item.id == activity.id
                LearningActivityRow(
                    activity = item,
                    active = active,
                    completed = done,
                    onClick = { vm.startActivity(item) },
                )
            }
        }
        if (vm.savedWorkspaces.isNotEmpty()) {
            Text("Saved Workspaces", color = Ink, fontWeight = FontWeight.SemiBold)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                vm.savedWorkspaces.take(3).forEach { saved ->
                    SavedWorkspaceRow(
                        saved = saved,
                        onOpen = { vm.restoreWorkspaceShell(saved) },
                        onDuplicate = { vm.duplicateWorkspace(saved) },
                        onDelete = { vm.deleteWorkspace(saved) },
                    )
                }
            }
        }
        Text("Workspace package", color = Ink, fontWeight = FontWeight.SemiBold)
        Insight("Package validation", vm.lastPackageValidation.message, if (vm.lastPackageValidation.valid) Green else Muted)
        Text(
            text = vm.exportLearningPackagePreview(),
            color = Muted,
            fontSize = 11.sp,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x33101824))
                .padding(10.dp),
        )
    }
}

@Composable
private fun LearningActivityRow(
    activity: LearningActivity,
    active: Boolean,
    completed: Boolean,
    onClick: () -> Unit,
) {
    val color = when {
        completed -> Green
        active -> Cyan
        else -> Muted
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (active) Color(0x3320D9FF) else Color(0x22101824))
            .border(1.dp, color.copy(.55f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
    ) {
        Text("${if (completed) "Done" else activity.module.label} - ${activity.title}", color = color, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Text(activity.target, color = Muted, fontSize = 11.sp)
    }
}

@Composable
private fun SavedWorkspaceRow(
    saved: SavedWorkspace,
    onOpen: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x22101824))
            .border(1.dp, Cyan.copy(.38f), RoundedCornerShape(14.dp))
            .padding(10.dp),
    ) {
        Text(saved.name, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Text("${saved.module.label} workspace", color = Muted, fontSize = 11.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            GlowButton("Open", onClick = onOpen)
            GlowButton("Copy", onClick = onDuplicate)
            GlowButton("Delete", onClick = onDelete)
        }
    }
}

@Composable
private fun BottomModeSelector(active: MathModule, onSelect: (MathModule) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth(.98f)
            .widthIn(max = 980.dp)
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(SurfaceA.copy(alpha = .96f))
            .border(1.dp, Color(0x5548BFFF), RoundedCornerShape(26.dp))
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        MathModule.entries.forEach { module ->
            val selected = module == active
            val compactLabel = when (module) {
                MathModule.Geometry2D -> "2D"
                MathModule.Geometry3D -> "3D"
                MathModule.Graph2D -> "Graph"
                MathModule.Graph3D -> "G3D"
                MathModule.Trigonometry -> "Trig"
                MathModule.SpatialAR -> "AR"
            }
            Text(
                text = compactLabel,
                color = if (selected) Color.White else Muted,
                textAlign = TextAlign.Center,
                maxLines = 1,
                fontSize = 12.sp,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (selected) Brush.horizontalGradient(listOf(Violet, Cyan)) else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)))
                    .clickable { onSelect(module) }
                    .focusable()
                    .padding(horizontal = 3.dp, vertical = 10.dp)
                    .semantics { contentDescription = "Open ${module.label} workspace" },
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun Geometry2DScreen(vm: ExplorerViewModel) {
    val haptic = LocalHapticFeedback.current
    val a = vm.state.points[0]
    val b = vm.state.points[1]
    val m = Geometry2D.segment(a, b)
    val third = Vec2(4.0, -1.5)
    val selectedShape = vm.state.shapes.getOrNull(vm.selectedShape)
    val dependenciesByOutput = vm.state.pointDependencies.associateBy { it.outputIndex }
    val invalidDependencyOutputs = vm.state.pointDependencies.filter {
        resolvePointDependency(vm.state.points, it.inputIndices, it.type, it.parameters) == null
    }.mapTo(mutableSetOf()) { it.outputIndex }
    Box(Modifier.fillMaxSize()) {
        CoordinateCanvas(
            modifier = Modifier.fillMaxSize().semantics { contentDescription = "Interactive coordinate geometry canvas" },
            shapes = vm.state.shapes,
            interactionEnabled = vm.geometryTool == GeometryTool.Select,
            onPointDragStart = {
                if (vm.settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.beginPointDrag(it)
            },
            onPointDrag = vm::previewPointDrag,
            onShapeDragStart = {
                if (vm.settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.beginShapeDrag(it)
            },
            onShapeDrag = vm::previewShapeDrag,
            onDragEnd = vm::endPointDrag,
            onDragCancel = vm::cancelPointDrag,
            onCanvasTap = vm::handleGeometryTap,
            points = vm.state.points,
        ) { tx ->
            val pa = tx(a)
            val pb = tx(b)
            val pm = tx(m.midpoint)
            drawLine(Violet, pa, pb, 5f, cap = StrokeCap.Round)
            drawStoredShapes(vm.state.points, vm.state.shapes, vm.selectedShape, tx)
            drawConstructionPreview(vm.pendingConstruction, vm.geometryTool, tx)
            vm.state.points.drop(2).forEachIndexed { index, point ->
                val pointIndex = index + 2
                val dependency = dependenciesByOutput[pointIndex]
                val invalid = pointIndex in invalidDependencyOutputs
                drawRadiantPoint(
                    tx(point),
                    if (invalid) Color.Red else if (dependency == null) Green else Amber,
                    if (invalid) "${dependency?.name} undefined" else dependency?.name ?: "P${pointIndex + 1}",
                )
            }
            drawLine(Cyan, tx(Vec2(a.x, a.y)), tx(Vec2(b.x, a.y)), 2f, pathEffect = null)
            drawLine(Cyan.copy(alpha = .8f), tx(Vec2(b.x, a.y)), pb, 2f)
            drawRadiantPoint(pa, Cyan, "A (${trim(a.x)}, ${trim(a.y)})")
            drawRadiantPoint(pb, Violet, "B (${trim(b.x)}, ${trim(b.y)})")
            drawRadiantPoint(pm, Violet, "M (${trim(m.midpoint.x)}, ${trim(m.midpoint.y)})")
            drawCircle(Cyan.copy(alpha = .8f), radius = a.distanceTo(third).toFloat() * 42f, center = tx(Vec2(1.5, 1.0)), style = Stroke(2f))
        }
        InteractionHint(
            "Drag points or shapes · two fingers pan/zoom · double-tap fit",
            Modifier.align(Alignment.BottomEnd),
        )
        FloatingPanelLaunchers(
            modifier = Modifier.align(Alignment.CenterStart),
            leftLabel = "Tools",
            rightLabel = "Measure",
            bottomLabel = "Construct",
            onLeft = { vm.togglePanel(PanelSlot.Left) },
            onRight = { vm.togglePanel(PanelSlot.Right) },
            onBottom = { vm.togglePanel(PanelSlot.Bottom) },
        )
        if (vm.showLeftPanel) GlassPanel(Modifier.align(Alignment.TopStart).width(270.dp)) {
            PanelHeader("Dynamic Geometry", vm::hidePanels, Cyan)
            Text("Tap flow: ${vm.geometryTool.name} needs ${vm.geometryTool.requiredTapCount()} tap(s)", color = Muted, fontSize = 12.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                GeometryTool.entries.filterNot { it == GeometryTool.Measure }.forEach {
                    GlowButton(if (vm.geometryTool == it) "• ${it.name}" else it.name, onClick = { vm.selectGeometryTool(it) })
                }
            }
            Text("Objects", color = Ink, fontWeight = FontWeight.SemiBold)
            vm.state.shapes.forEachIndexed { index, shape ->
                Text(
                    text = "${if (index == vm.selectedShape) "• " else ""}${shape.name}${if (!shape.visible) " (hidden)" else ""}${if (shape.locked) " 🔒" else ""}",
                    color = if (index == vm.selectedShape) Amber else Muted,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { vm.selectShape(index) }
                        .padding(7.dp),
                )
            }
        }
        if (vm.showRightPanel) GlassPanel(Modifier.align(Alignment.TopEnd).width(260.dp)) {
            PanelHeader("Object Inspector", vm::hidePanels, Violet)
            Insight("Active tool", vm.geometryTool.name, Green)
            Insight("Pending taps", "${vm.pendingConstruction.size}/${vm.geometryTool.requiredTapCount()}", Cyan)
            Insight("Objects", "${vm.state.shapes.size}", Violet)
            Insight("Dependencies", "${vm.state.pointDependencies.size}", Amber)
            Insight("Undefined", "${invalidDependencyOutputs.size}", if (invalidDependencyOutputs.isEmpty()) Green else Color.Red)
            Insight("Midpoint", "(${trim(m.midpoint.x)}, ${trim(m.midpoint.y)})", Cyan)
            Insight("Slope", m.slope?.let(::trim) ?: "undefined", Violet)
            Insight("Distance AB", "${m.exactDistance} ≈ ${trim(m.distance)}", Cyan)
            Insight("Triangle area", "${trim(Geometry2D.polygonArea(listOf(a, b, third)))} u²", Violet)
            selectedShape?.let { shape ->
                OutlinedTextField(
                    value = shape.name,
                    onValueChange = { name -> vm.updateSelectedShape { it.copy(name = name.take(32)) } },
                    label = { Text("Object name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    GlowButton(if (shape.visible) "Hide" else "Show", onClick = { vm.updateSelectedShape { it.copy(visible = !it.visible) } })
                    GlowButton(if (shape.locked) "Unlock" else "Lock", onClick = { vm.updateSelectedShape { it.copy(locked = !it.locked) } })
                    GlowButton("Style", onClick = {
                        vm.updateSelectedShape {
                            val next = when (it.styleKey) { "default" -> "cyan"; "cyan" -> "violet"; "violet" -> "green"; else -> "default" }
                            it.copy(styleKey = next)
                        }
                    })
                    GlowButton("Delete", onClick = vm::deleteSelectedShape)
                    GlowButton("Translate", onClick = { vm.transformSelectedShape(PointDependencyType.Translate, listOf(1.0, 1.0)) })
                    GlowButton("Rotate 30°", onClick = { vm.transformSelectedShape(PointDependencyType.Rotate, listOf(30.0, 0.0, 0.0)) })
                    GlowButton("Reflect X", onClick = { vm.transformSelectedShape(PointDependencyType.ReflectX) })
                    GlowButton("Dilate", onClick = { vm.transformSelectedShape(PointDependencyType.Dilate, listOf(1.25, 0.0, 0.0)) })
                }
            }
        }
        if (vm.showBottomPanel) GlassPanel(Modifier.align(Alignment.BottomStart).fillMaxWidth()) {
            PanelHeader("Geometry Controls", vm::hidePanels, Ink)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    GeometryTool.Point,
                    GeometryTool.Line,
                    GeometryTool.Segment,
                    GeometryTool.Ray,
                    GeometryTool.Triangle,
                    GeometryTool.Polygon,
                    GeometryTool.Rectangle,
                    GeometryTool.Square,
                    GeometryTool.Circle,
                    GeometryTool.Arc,
                    GeometryTool.Measure,
                ).forEach {
                    GlowButton(if (vm.geometryTool == it) "• ${it.name}" else it.name, onClick = { vm.selectGeometryTool(it) })
                }
                GlowButton("Reset", onClick = { vm.reset() })
            }
            if (vm.constructionProtocol.isNotEmpty()) {
                Text("Construction Protocol", color = Ink, fontWeight = FontWeight.SemiBold)
                Text(vm.constructionProtocol.takeLast(8).mapIndexed { index, label -> "${index + 1}. $label" }.joinToString("  ·  "), color = Muted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun Graph2DScreen(vm: ExplorerViewModel) {
    val graph = remember { GraphAnalysis() }
    val engine = remember { ExpressionEngine() }
    var traceX by remember { mutableFloatStateOf(2f) }
    var graphTool by remember { mutableStateOf(GraphTool.Trace) }
    var parameterA by remember { mutableFloatStateOf(1f) }
    var dataText by remember { mutableStateOf("-2,4; -1,1; 0,0; 1,1; 2,4") }
    val liveFunctions = vm.state.functions.map { function ->
        function.copy(expression = function.expression.replace(Regex("\\ba\\b"), trim(parameterA.toDouble())))
    }
    val visibleFunctions = liveFunctions.filter { it.visible }
    val explicitFunctions = visibleFunctions.filter { graph.definitionKind(it.expression) == GraphDefinitionKind.Explicit }
    val primaryExpression = explicitFunctions.firstOrNull()?.expression
    val roots = remember(primaryExpression) {
        primaryExpression?.let { runCatching { graph.roots(it, -10.0, 10.0) }.getOrDefault(emptyList()) }.orEmpty()
    }
    val extrema = remember(primaryExpression) {
        primaryExpression?.let { runCatching { graph.extrema(it, -10.0, 10.0) }.getOrDefault(emptyList()) }.orEmpty()
    }
    val intersections = remember(liveFunctions) {
        if (explicitFunctions.size < 2) emptyList() else runCatching {
            graph.intersections(engine.compile(explicitFunctions[0].expression), engine.compile(explicitFunctions[1].expression), -10.0, 10.0)
        }.getOrDefault(emptyList())
    }
    val dataPoints = remember(dataText) { parseDataPoints(dataText) }
    val dataSummary = remember(dataPoints) { StatisticsEngine.summarize(dataPoints) }
    Box(Modifier.fillMaxSize()) {
        GraphCanvas(
            modifier = Modifier.fillMaxSize(),
            functions = liveFunctions,
            dataPoints = if (graphTool == GraphTool.Data) dataPoints else emptyList(),
            traceX = traceX.toDouble(),
            graphTool = graphTool,
            onTraceChange = { traceX = it.toFloat().coerceIn(-1_000f, 1_000f) },
        )
        InteractionHint(
            "Trace mode drags the trace · Plot pans · pinch zooms",
            Modifier.align(Alignment.BottomEnd),
        )
        EquationChips(Modifier.align(Alignment.TopCenter), visibleFunctions.map { "${it.name} = ${it.expression}" })
        GraphToolChips(
            modifier = Modifier.align(Alignment.TopCenter),
            active = graphTool,
            onSelect = { graphTool = it },
        )
        FloatingPanelLaunchers(
            modifier = Modifier.align(Alignment.CenterStart),
            leftLabel = "Fx",
            rightLabel = "Analysis",
            bottomLabel = "Trace",
            onLeft = { vm.togglePanel(PanelSlot.Left) },
            onRight = { vm.togglePanel(PanelSlot.Right) },
            onBottom = { vm.togglePanel(PanelSlot.Bottom) },
        )
        if (vm.showLeftPanel) GlassPanel(Modifier.align(Alignment.TopStart).width(300.dp)) {
            PanelHeader("Equations & Definitions", vm::hidePanels, Cyan)
            vm.state.functions.forEachIndexed { index, fn ->
                OutlinedTextField(
                    value = fn.expression,
                    onValueChange = { vm.editExpression(index, it) },
                    label = { Text(fn.name) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GlowButton(if (fn.visible) "Hide" else "Show", onClick = { vm.updateFunction(index) { it.copy(visible = !it.visible) } })
                    GlowButton("Delete", onClick = { vm.deleteFunction(index) })
                }
            }
            Text("Add definition", color = Ink, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                GlowButton("Function", onClick = { vm.addFunction("sin(x)") })
                GlowButton("Parameter", onClick = { vm.addFunction("a*sin(x)") })
                GlowButton("Piecewise", onClick = { vm.addFunction("if(x<0,-x,x)") })
                GlowButton("Polar", onClick = { vm.addFunction("r = 2*cos(3*t)") })
                GlowButton("Parametric", onClick = { vm.addFunction("x(t)=3*cos(t); y(t)=2*sin(t)") })
                GlowButton("Implicit", onClick = { vm.addFunction("x^2 + y^2 = 9") })
            }
        }
        if (vm.showRightPanel) GlassPanel(Modifier.align(Alignment.TopEnd).width(270.dp)) {
            PanelHeader("Graph Insights", vm::hidePanels, Violet)
            Insight("Tool", graphTool.name, Green)
            Insight("Definitions", "${visibleFunctions.size} visible", Cyan)
            Insight("Kinds", visibleFunctions.map { graph.definitionKind(it.expression).name }.distinct().joinToString(), Violet)
            Insight("Roots", roots.joinToString { trim(it) }.ifBlank { "none detected" }, Cyan)
            Insight("Extrema", extrema.joinToString { "(${trim(it.x)}, ${trim(it.y)})" }.ifBlank { "none detected" }, Green)
            primaryExpression?.let {
                Insight("Derivative", runCatching { trim(graph.derivative(it, traceX.toDouble())) }.getOrDefault("undefined"), Amber)
                Insight("Integral 0→x", runCatching { trim(graph.integral(it, 0.0, traceX.toDouble())) }.getOrDefault("undefined"), Cyan)
            }
            Insight("Intersections", intersections.joinToString { "(${trim(it.x)}, ${trim(it.y)})" }, Violet)
            if (graphTool == GraphTool.Data) {
                Insight("Data", "${dataSummary.count} points", Cyan)
                Insight("Mean", "(${trim(dataSummary.meanX)}, ${trim(dataSummary.meanY)})", Green)
                Insight("σ y", trim(dataSummary.standardDeviationY), Violet)
                dataSummary.regression?.let { Insight("Regression", "y=${trim(it.slope)}x+${trim(it.intercept)} · r=${trim(it.correlation)}", Amber) }
            }
        }
        if (vm.showBottomPanel) GlassPanel(Modifier.align(Alignment.BottomStart).fillMaxWidth()) {
            PanelHeader("Graph Controls", vm::hidePanels, Ink)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Trace x = ${trim(traceX.toDouble())}", color = Muted, modifier = Modifier.width(120.dp))
                Slider(value = traceX, onValueChange = { traceX = it }, valueRange = -6f..6f, modifier = Modifier.weight(1f))
            }
            if (vm.state.functions.any { Regex("\\ba\\b").containsMatchIn(it.expression) }) {
                AxisSlider("Parameter a", parameterA, -8f..8f) { parameterA = it }
            }
            if (graphTool == GraphTool.Table) {
                Text("Value table", color = Ink, fontWeight = FontWeight.SemiBold)
                val rows = (-4..4).joinToString("\n") { x ->
                    val values = explicitFunctions.joinToString("  ") { fn ->
                        val y = runCatching { engine.compile(fn.expression).eval(mapOf("x" to x.toDouble())) }.getOrDefault(Double.NaN)
                        "${fn.name}:${trim(y)}"
                    }
                    "x=$x  $values"
                }
                Text(rows, color = Muted, fontSize = 12.sp)
            }
            if (graphTool == GraphTool.Data) {
                OutlinedTextField(
                    value = dataText,
                    onValueChange = { dataText = it },
                    label = { Text("Data points: x,y; x,y") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (graphTool == GraphTool.Probability) {
                Text("Normal PDF at x=${trim(traceX.toDouble())}: ${trim(ProbabilityEngine.normalPdf(traceX.toDouble()))}", color = Cyan)
                Text("Binomial P(X=3), n=10, p=.5: ${trim(ProbabilityEngine.binomialPmf(3, 10, .5))}", color = Violet)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GraphTool.entries.forEach { tool ->
                    GlowButton(if (graphTool == tool) "• ${tool.name}" else tool.name, onClick = { graphTool = tool })
                }
                GlowButton("Export JSON", onClick = { vm.exportJson() })
            }
        }
    }
}

private fun parseDataPoints(source: String): List<Vec2> = source
    .split(';', '\n')
    .mapNotNull { entry ->
        val values = entry.trim().split(',').mapNotNull { it.trim().toDoubleOrNull() }
        if (values.size >= 2) Vec2(values[0], values[1]) else null
    }

@Composable
private fun Geometry3DScreen(vm: ExplorerViewModel) {
    val haptic = LocalHapticFeedback.current
    var rotateX by remember { mutableFloatStateOf(25f) }
    var rotateY by remember { mutableFloatStateOf(-35f) }
    var rotateZ by remember { mutableFloatStateOf(15f) }
    var zoom by remember { mutableFloatStateOf(1.0f) }
    var cameraPan by remember { mutableStateOf(Offset.Zero) }
    var transformMode by remember { mutableStateOf(Transform3DMode.Move) }
    var wire by remember { mutableStateOf(true) }
    var projection by remember { mutableStateOf(CameraProjection.Perspective) }
    var selectionMode by remember { mutableStateOf(Selection3DMode.Object) }
    var subSelection by remember { mutableStateOf<SubObjectSelection?>(null) }
    var sectionEnabled by remember { mutableStateOf(false) }
    var clipSection by remember { mutableStateOf(false) }
    var sectionY by remember { mutableFloatStateOf(0f) }
    val selectedIndex = vm.selectedSolid.coerceIn(0, vm.state.solids.lastIndex.coerceAtLeast(0))
    val selectedSolid = vm.state.solids.getOrNull(selectedIndex)
    val selectedVectorIndex = vm.selectedVector3D.coerceIn(0, vm.state.vectors3D.lastIndex.coerceAtLeast(0))
    val selectedVector = vm.state.vectors3D.getOrNull(selectedVectorIndex)
    Box(Modifier.fillMaxSize()) {
        Projected3DCanvas(
            modifier = Modifier.fillMaxSize(),
            solids = vm.state.solids,
            vectors = vm.state.vectors3D,
            selectedIndex = selectedIndex,
            selectedVectorIndex = selectedVectorIndex,
            rx = rotateX,
            ry = rotateY,
            rz = rotateZ,
            zoom = zoom,
            cameraPan = cameraPan,
            transformMode = transformMode,
            wire = wire,
            perspective = projection == CameraProjection.Perspective,
            selectionMode = selectionMode,
            subSelection = subSelection,
            sectionEnabled = sectionEnabled,
            sectionY = sectionY.toDouble(),
            clipSection = clipSection,
            onSelect = vm::selectSolid,
            onSubSelect = { subSelection = it },
            onSelectVector = vm::selectVector3D,
            onSolidDragStart = {
                if (vm.settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.beginSolidDrag(it)
            },
            onSolidMove = vm::previewSolidDrag,
            onSolidRotate = vm::previewSolidRotation,
            onSolidScale = vm::previewSolidScale,
            onSolidDragEnd = vm::endSolidDrag,
            onSolidDragCancel = vm::cancelSolidDrag,
            onVectorDragStart = {
                if (vm.settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.beginVectorDrag(it)
            },
            onVectorMove = vm::previewVectorDrag,
            onVectorDragEnd = vm::endVectorDrag,
            onVectorDragCancel = vm::cancelVectorDrag,
            onOrbit = { dx, dy ->
                rotateY = (rotateY + dx).wrapDegrees()
                rotateX = (rotateX + dy).coerceIn(-89f, 89f)
            },
            onPan = { delta -> cameraPan += delta },
            onZoom = { factor -> zoom = (zoom * factor).coerceIn(.35f, 4f) },
            onResetCamera = {
                rotateX = 25f
                rotateY = -35f
                rotateZ = 15f
                zoom = 1f
                cameraPan = Offset.Zero
            },
        )
        SolidAddStrip(Modifier.align(Alignment.TopCenter), vm::addSolid, vm::addVector3D)
        Transform3DChips(
            modifier = Modifier.align(Alignment.BottomCenter),
            active = transformMode,
            onSelect = { transformMode = it },
        )
        Selection3DChips(
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 62.dp, end = 12.dp),
            active = selectionMode,
            onSelect = {
                selectionMode = it
                subSelection = null
            },
        )
        InteractionHint(
            "Drag empty space to orbit · two fingers pan/zoom · drag object to transform",
            Modifier.align(Alignment.BottomEnd),
        )
        FloatingPanelLaunchers(
            modifier = Modifier.align(Alignment.CenterStart),
            leftLabel = "Solids",
            rightLabel = "3D Tools",
            bottomLabel = "Transform",
            onLeft = { vm.togglePanel(PanelSlot.Left) },
            onRight = { vm.togglePanel(PanelSlot.Right) },
            onBottom = { vm.togglePanel(PanelSlot.Bottom) },
        )
        if (vm.showLeftPanel) GlassPanel(Modifier.align(Alignment.TopStart).width(230.dp)) {
            PanelHeader("3D Objects", vm::hidePanels, Cyan)
            vm.state.solids.forEachIndexed { index, solid ->
                val measure = Geometry3D.measure(solid)
                val accent = if (index == selectedIndex) Cyan else if (solid.type == SolidType.Cube) Cyan else Violet
                Text(
                    text = "${if (index == selectedIndex) "• " else ""}${solid.type.name}",
                    color = accent,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { vm.selectSolid(index) }
                        .padding(8.dp),
                )
                Insight("Measure", "V ${trim(measure.volume)} · A ${trim(measure.surfaceArea)}", accent)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SolidType.entries.forEach { type -> GlowButton(type.name.take(6), onClick = { vm.addSolid(type) }) }
                GlowButton("Vector", onClick = vm::addVector3D)
            }
            if (vm.state.vectors3D.isNotEmpty()) {
                Text("Vectors", color = Ink, fontWeight = FontWeight.SemiBold)
                vm.state.vectors3D.forEachIndexed { index, vector ->
                    val accent = if (index == selectedVectorIndex) Amber else Green
                    Text(
                        text = "${if (index == selectedVectorIndex) "• " else ""}${vector.name} = <${trim(vector.components.x)}, ${trim(vector.components.y)}, ${trim(vector.components.z)}>",
                        color = accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { vm.selectVector3D(index) }
                            .padding(8.dp),
                    )
                    Insight("Magnitude", trim(vector.magnitude), accent)
                }
            }
        }
        if (vm.showRightPanel) GlassPanel(Modifier.align(Alignment.TopEnd).width(170.dp)) {
            PanelHeader("Tools", vm::hidePanels, Violet)
            listOf("Zoom +", "Zoom -", "Scale +", "Scale -", "Wireframe", "Reset").forEach {
                GlowButton(it, onClick = {
                    if (it == "Zoom +") zoom = (zoom + .1f).coerceAtMost(1.8f)
                    if (it == "Zoom -") zoom = (zoom - .1f).coerceAtLeast(.6f)
                    if (it == "Scale +") selectedSolid?.let { solid ->
                        vm.transformSolid(selectedIndex) { solid.copy(width = solid.width + .2, height = solid.height + .2, depth = solid.depth + .2, radius = solid.radius + .1) }
                    }
                    if (it == "Scale -") selectedSolid?.let { solid ->
                        vm.transformSolid(selectedIndex) {
                            solid.copy(
                                width = (solid.width - .2).coerceAtLeast(.4),
                                height = (solid.height - .2).coerceAtLeast(.4),
                                depth = (solid.depth - .2).coerceAtLeast(.4),
                                radius = (solid.radius - .1).coerceAtLeast(.2),
                            )
                        }
                    }
                    if (it == "Wireframe") wire = !wire
                    if (it == "Reset") vm.reset()
                })
            }
            selectedSolid?.let {
                val measure = Geometry3D.measure(it)
                Insight("Selected", it.type.name, Cyan)
                Insight("Faces", measure.faces.toString(), Violet)
                Insight("Edges", measure.edges.toString(), Violet)
                Insight("Vertices", measure.vertices.toString(), Violet)
                subSelection?.takeIf { selection -> selection.solidIndex == selectedIndex }?.let { selection ->
                    Insight("Sub-object", "${selection.mode.name} ${selection.index + 1}", Amber)
                }
            }
            selectedVector?.let {
                Insight("Vector", it.name, Amber)
                Insight("Components", "<${trim(it.components.x)}, ${trim(it.components.y)}, ${trim(it.components.z)}>", Green)
                Insight("Magnitude", trim(it.magnitude), Amber)
            }
        }
        if (vm.showBottomPanel) GlassPanel(Modifier.align(Alignment.BottomStart).fillMaxWidth()) {
            PanelHeader("3D Controls", vm::hidePanels, Ink)
            AxisSlider("Rotate X", rotateX, -180f..180f) { rotateX = it }
            AxisSlider("Rotate Y", rotateY, -180f..180f) { rotateY = it }
            AxisSlider("Rotate Z", rotateZ, -180f..180f) { rotateZ = it }
            AxisSlider("Zoom", zoom, .6f..1.8f) { zoom = it }
            TogglePill("Perspective", projection == CameraProjection.Perspective) {
                projection = if (it) CameraProjection.Perspective else CameraProjection.Orthographic
            }
            TogglePill("Wireframe", wire) { wire = it }
            TogglePill("Cross-section", sectionEnabled) { sectionEnabled = it }
            TogglePill("Clip below plane", clipSection) { clipSection = it }
            if (sectionEnabled || clipSection) AxisSlider("Section Y", sectionY, -3f..3f) { sectionY = it }
            selectedSolid?.let { solid ->
                Text("Selected ${solid.type.name}", color = Cyan, fontWeight = FontWeight.SemiBold)
                AxisSlider("Width", solid.width.toFloat(), .4f..4f) { value ->
                    vm.transformSolid(selectedIndex) { it.copy(width = value.toDouble()) }
                }
                AxisSlider("Height", solid.height.toFloat(), .4f..4f) { value ->
                    vm.transformSolid(selectedIndex) { it.copy(height = value.toDouble()) }
                }
                AxisSlider("Depth", solid.depth.toFloat(), .4f..4f) { value ->
                    vm.transformSolid(selectedIndex) { it.copy(depth = value.toDouble()) }
                }
                AxisSlider("Radius", solid.radius.toFloat(), .2f..2f) { value ->
                    vm.transformSolid(selectedIndex) { it.copy(radius = value.toDouble()) }
                }
                if (solid.type == SolidType.Frustum) AxisSlider("Top radius", solid.topRadius.toFloat(), .1f..2f) { value ->
                    vm.transformSolid(selectedIndex) { it.copy(topRadius = value.toDouble()) }
                }
            }
            selectedVector?.let { vector ->
                Text("Selected vector ${vector.name}", color = Amber, fontWeight = FontWeight.SemiBold)
                AxisSlider("dx", vector.components.x.toFloat(), -5f..5f) { value ->
                    vm.transformVector3D(selectedVectorIndex) { it.copy(end = it.start + Vec3(value.toDouble(), it.components.y, it.components.z)) }
                }
                AxisSlider("dy", vector.components.y.toFloat(), -5f..5f) { value ->
                    vm.transformVector3D(selectedVectorIndex) { it.copy(end = it.start + Vec3(it.components.x, value.toDouble(), it.components.z)) }
                }
                AxisSlider("dz", vector.components.z.toFloat(), -5f..5f) { value ->
                    vm.transformVector3D(selectedVectorIndex) { it.copy(end = it.start + Vec3(it.components.x, it.components.y, value.toDouble())) }
                }
            }
        }
    }
}

@Composable
private fun SpatialARScreen(vm: ExplorerViewModel) {
    val context = LocalContext.current
    val activity = context as? Activity
    val controller = remember { ARCoreSessionController() }
    var capabilities by remember { mutableStateOf(ARCapabilities(ARAvailability.Checking)) }
    var cameraGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        cameraGranted = granted
        capabilities = if (granted && activity != null) controller.prepare(activity, true) else {
            capabilities.copy(message = "Camera permission was not granted; the spatial simulator remains fully available.")
        }
    }
    LaunchedEffect(activity) {
        if (activity != null) capabilities = controller.checkAvailability(activity)
    }
    DisposableEffect(controller) {
        onDispose { controller.pause(); controller.close() }
    }

    val placement = vm.state.spatialPlacement
    val guidance = SpatialSafety.guidance(placement.trackingQuality)
    Box(Modifier.fillMaxSize()) {
        SpatialPreviewCanvas(
            modifier = Modifier.fillMaxSize(),
            solids = vm.state.solids,
            placement = placement,
            onGestureStart = vm::beginSpatialGesture,
            onGesture = vm::previewSpatialGesture,
            onGestureEnd = vm::endSpatialGesture,
        )
        GlassPanel(Modifier.align(Alignment.TopStart).width(285.dp).padding(top = 105.dp)) {
            PanelHeader("AR Spatial Lab", { vm.open(MathModule.Geometry3D) }, Cyan)
            Insight("Mode", if (capabilities.availability == ARAvailability.Ready && cameraGranted) "ARCore ready · simulator preview" else "Accessible spatial simulator", Cyan)
            Insight("ARCore", capabilities.message, if (capabilities.availability == ARAvailability.Unsupported) Amber else Green)
            Insight("Tracking", guidance.title, if (guidance.safeToPlace) Green else Amber)
            Text(guidance.instruction, color = Muted, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlowButton("Enable ARCore", onClick = {
                    if (activity == null) capabilities = capabilities.copy(message = "ARCore requires an Android activity.")
                    else if (!cameraGranted) cameraPermission.launch(Manifest.permission.CAMERA)
                    else capabilities = controller.prepare(activity, true)
                })
                GlowButton(if (placement.isPlaced) "Re-place" else "Place", onClick = vm::placeSpatialScene)
                GlowButton("Reset", onClick = vm::resetSpatialScene)
            }
            Insight("Scale", placement.visibleScale, Violet)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GlowButton(if (placement.scaleMode == ARScaleMode.OneToOne) "• 1:1" else "1:1", onClick = { vm.setSpatialScaleMode(ARScaleMode.OneToOne) })
                GlowButton(if (placement.scaleMode == ARScaleMode.FitToSpace) "• Fit" else "Fit", onClick = { vm.setSpatialScaleMode(ARScaleMode.FitToSpace) })
                TogglePill("Depth occlusion", placement.depthOcclusionEnabled) {
                    vm.setDepthOcclusion(it && capabilities.depthSupported)
                }
            }
            Insight("Depth", when {
                capabilities.depthSupported -> "Supported; occlusion can be enabled."
                capabilities.availability == ARAvailability.Ready -> "Unavailable; objects remain outlined."
                else -> "Checked when an ARCore session is prepared."
            }, if (capabilities.depthSupported) Green else Muted)
            Text("Placement and measurements are educational estimates, not certified physical measurements.", color = Amber, fontSize = 11.sp)
        }
        InteractionHint(
            "Drag to move the anchored scene · pinch to scale · twist to rotate · Undo restores the last gesture",
            Modifier.align(Alignment.BottomEnd),
        )
    }
}

@Composable
private fun SpatialPreviewCanvas(
    modifier: Modifier,
    solids: List<Solid>,
    placement: com.indianservers.aiexplorer.spatial.SpatialScenePlacement,
    onGestureStart: () -> Unit,
    onGesture: (Offset, Float, Float) -> Unit,
    onGestureEnd: () -> Unit,
) {
    val currentPlacement by rememberUpdatedState(placement)
    Canvas(
        modifier
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    onGestureStart()
                    var totalPan = Offset.Zero
                    var totalRotation = 0f
                    var totalScale = 1f
                    while (true) {
                        val event = awaitPointerEvent()
                        totalPan += event.calculatePan()
                        totalRotation += event.calculateRotation()
                        totalScale *= event.calculateZoom()
                        onGesture(totalPan, totalRotation, totalScale)
                        event.changes.forEach { if (it.pressed) it.consume() }
                        if (event.changes.none { it.pressed }) break
                    }
                    onGestureEnd()
                }
            }
            .semantics { contentDescription = "AR spatial mathematics preview with direct move, rotate and scale gestures" },
    ) {
        drawRect(Brush.verticalGradient(listOf(Color(0xFF08131B), Color(0xFF18242A), Color(0xFF101317))))
        val scene = currentPlacement
        val center = Offset(
            size.width * .56f + scene.pose.positionMeters.x.toFloat() * 220f,
            size.height * .53f + scene.pose.positionMeters.z.toFloat() * 34f,
        )
        drawPerspectiveGrid(center.copy(y = center.y + 110f))
        drawCircle(if (scene.isPlaced) Green else Amber, 20f, center, style = Stroke(3f))
        drawLine(Color.White.copy(.7f), center - Offset(32f, 0f), center + Offset(32f, 0f), 2f)
        drawLine(Color.White.copy(.7f), center - Offset(0f, 32f), center + Offset(0f, 32f), 2f)
        solids.forEachIndexed { index, solid ->
            drawSolidProjection(
                solid = solid,
                offset = solid.position,
                rx = 24f + scene.pose.rotationDegrees.x.toFloat(),
                ry = -34f + scene.pose.rotationDegrees.y.toFloat(),
                rz = scene.pose.rotationDegrees.z.toFloat(),
                center = center,
                scale = 58f * scene.pose.uniformScale.toFloat(),
                color = if (index % 2 == 0) Cyan else Violet,
                wire = !scene.depthOcclusionEnabled,
                selected = index == 0,
                perspective = true,
                subSelection = null,
                sectionEnabled = false,
                sectionY = 0.0,
                clipSection = false,
            )
        }
        drawGraphLabel(if (scene.isPlaced) "Anchored · ${scene.visibleScale}" else "Placement preview · tap Place", center + Offset(28f, -130f), if (scene.isPlaced) Green else Amber)
    }
}

@Composable
private fun Graph3DScreen(vm: ExplorerViewModel) {
    val graph3D = remember { Graph3D() }
    var density by remember { mutableFloatStateOf(26f) }
    var rotation by remember { mutableFloatStateOf(35f) }
    var roll by remember { mutableFloatStateOf(0f) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var cameraPan by remember { mutableStateOf(Offset.Zero) }
    var tilt by remember { mutableFloatStateOf(55f) }
    var sliceZ by remember { mutableFloatStateOf(2f) }
    var traceX by remember { mutableFloatStateOf(1f) }
    var traceY by remember { mutableFloatStateOf(1f) }
    var showWireframe by remember { mutableStateOf(true) }
    var showContours by remember { mutableStateOf(true) }
    var showSlice by remember { mutableStateOf(true) }
    var showGradient by remember { mutableStateOf(true) }
    var showBox by remember { mutableStateOf(true) }
    var activeTool by remember { mutableStateOf(SurfaceTool.Surface) }
    var viewPreset by remember { mutableStateOf(SurfaceViewPreset.Isometric) }
    fun applyView(preset: SurfaceViewPreset) {
        viewPreset = preset
        when (preset) {
            SurfaceViewPreset.Isometric -> { tilt = 55f; rotation = 35f; roll = 0f }
            SurfaceViewPreset.X, SurfaceViewPreset.YZ -> { tilt = 0f; rotation = 90f; roll = 0f }
            SurfaceViewPreset.Y, SurfaceViewPreset.XZ -> { tilt = 90f; rotation = 0f; roll = 0f }
            SurfaceViewPreset.Z, SurfaceViewPreset.XY -> { tilt = 0f; rotation = 0f; roll = 0f }
        }
        cameraPan = Offset.Zero
    }
    val insight = remember(vm.state.surfaceExpression) { graph3D.insight(vm.state.surfaceExpression) }
    val mesh = remember(vm.state.surfaceExpression, density) {
        runCatching { graph3D.mesh(vm.state.surfaceExpression, density = density.toInt()) }.getOrNull()
    }
    Box(Modifier.fillMaxSize()) {
        SurfaceCanvas3D(
            modifier = Modifier.fillMaxSize(),
            expression = vm.state.surfaceExpression,
            mesh = mesh,
            rotation = rotation,
            tilt = tilt,
            roll = roll,
            zoom = zoom,
            cameraPan = cameraPan,
            sliceZ = sliceZ.toDouble(),
            trace = Vec2(traceX.toDouble(), traceY.toDouble()),
            showWireframe = showWireframe,
            showContours = showContours,
            showSlice = showSlice,
            showGradient = showGradient,
            showBox = showBox,
            activeTool = activeTool,
            onRotate = { delta -> rotation = (rotation + delta).coerceIn(-180f, 180f) },
            onTilt = { delta -> tilt = (tilt + delta).coerceIn(-89f, 89f) },
            onRoll = { delta -> roll = (roll + delta).wrapDegrees() },
            onPan = { delta -> cameraPan += delta },
            onZoom = { factor -> zoom = (zoom * factor).coerceIn(.35f, 4f) },
            onResetCamera = {
                zoom = 1f
                rotation = 35f
                tilt = 55f
                roll = 0f
                viewPreset = SurfaceViewPreset.Isometric
                cameraPan = Offset.Zero
            },
            onTrace = { point ->
                traceX = point.x.toFloat().coerceIn(-3f, 3f)
                traceY = point.y.toFloat().coerceIn(-3f, 3f)
            },
        )
        SurfaceExampleChips(
            modifier = Modifier.align(Alignment.TopCenter),
            onSelect = vm::setSurfaceExpression,
        )
        SurfaceViewChips(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 120.dp),
            active = viewPreset,
            onSelect = { applyView(it) },
        )
        InteractionHint(
            "One finger orbits · two fingers pan · pinch zooms · twist rolls · tap X/Y/Z or a plane",
            Modifier.align(Alignment.BottomEnd),
        )
        FloatingPanelLaunchers(
            modifier = Modifier.align(Alignment.CenterStart),
            leftLabel = "Equation",
            rightLabel = "Surface",
            bottomLabel = "3D Ctrl",
            onLeft = { vm.togglePanel(PanelSlot.Left) },
            onRight = { vm.togglePanel(PanelSlot.Right) },
            onBottom = { vm.togglePanel(PanelSlot.Bottom) },
        )
        if (vm.showLeftPanel) GlassPanel(Modifier.align(Alignment.TopStart).width(280.dp)) {
            PanelHeader("3D Graph Workspace", vm::hidePanels, Cyan)
            OutlinedTextField(
                value = vm.state.surfaceExpression,
                onValueChange = vm::setSurfaceExpression,
                label = { Text("z = f(x, y)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            AxisSlider("Mesh density", density, 8f..48f) { density = it }
            AxisSlider("Rotation", rotation, -180f..180f) { rotation = it }
            AxisSlider("Tilt", tilt, -89f..89f) { tilt = it }
            AxisSlider("Roll", roll, -180f..180f) { roll = it }
        }
        if (vm.showRightPanel) GlassPanel(Modifier.align(Alignment.TopEnd).width(250.dp)) {
            PanelHeader("Surface Insights", vm::hidePanels, Violet)
            Insight("Surface", insight.classification, Cyan)
            Insight("Vertex", insight.vertex?.let { "(${trim(it.x)}, ${trim(it.y)}, ${trim(it.z)})" } ?: "sampled", Violet)
            Insight("Range", insight.range, Cyan)
            Insight("Symmetry", insight.symmetry, Violet)
            Insight("Trace", "(${trim(traceX.toDouble())}, ${trim(traceY.toDouble())})", Green)
            Insight("Slice", "z = ${trim(sliceZ.toDouble())}", Violet)
            Insight("Checks", "z(0,0)=0 · z(1,1)=2", Green)
        }
        if (vm.showBottomPanel) GlassPanel(Modifier.align(Alignment.BottomStart).fillMaxWidth()) {
            PanelHeader("3D Graph Controls", vm::hidePanels, Ink)
            AxisSlider("Rotation", rotation, -180f..180f) { rotation = it }
            AxisSlider("Tilt", tilt, -89f..89f) { tilt = it }
            AxisSlider("Roll", roll, -180f..180f) { roll = it }
            AxisSlider("Zoom", zoom, .55f..1.7f) { zoom = it }
            AxisSlider("Mesh", density, 8f..56f) { density = it }
            AxisSlider("Slice z", sliceZ, -4f..6f) { sliceZ = it }
            AxisSlider("Trace x", traceX, -3f..3f) { traceX = it }
            AxisSlider("Trace y", traceY, -3f..3f) { traceY = it }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SurfaceTool.entries.forEach { tool ->
                    GlowButton(if (activeTool == tool) "• ${tool.name}" else tool.name, onClick = {
                        activeTool = tool
                        if (tool == SurfaceTool.Wireframe) showWireframe = !showWireframe
                        if (tool == SurfaceTool.Contours) showContours = !showContours
                        if (tool == SurfaceTool.Slice) showSlice = !showSlice
                        if (tool == SurfaceTool.Gradient) showGradient = !showGradient
                        if (tool == SurfaceTool.BoundingBox) showBox = !showBox
                    })
                }
                GlowButton("Zoom +", onClick = { zoom = (zoom + .1f).coerceAtMost(1.7f) })
                GlowButton("Zoom -", onClick = { zoom = (zoom - .1f).coerceAtLeast(.55f) })
                GlowButton("Fit", onClick = { zoom = 1f; applyView(SurfaceViewPreset.Isometric) })
            }
        }
    }
}

@Composable
private fun TrigonometryScreen(vm: ExplorerViewModel) {
    var angle by remember { mutableFloatStateOf(45f) }
    var showTangents by remember { mutableStateOf(true) }
    var showProjections by remember { mutableStateOf(true) }
    var showWave by remember { mutableStateOf(true) }
    val radians = Math.toRadians(angle.toDouble())
    val sinValue = sin(radians)
    val cosValue = cos(radians)
    val tanValue = tan(radians)
    Box(Modifier.fillMaxSize()) {
        TrigCanvas(
            modifier = Modifier.fillMaxSize(),
            angleDegrees = angle,
            showTangents = showTangents,
            showProjections = showProjections,
            showWave = showWave,
        )
        FloatingPanelLaunchers(
            modifier = Modifier.align(Alignment.CenterStart),
            leftLabel = "Trig Tools",
            rightLabel = "Insights",
            bottomLabel = "Controls",
            onLeft = { vm.togglePanel(PanelSlot.Left) },
            onRight = { vm.togglePanel(PanelSlot.Right) },
            onBottom = { vm.togglePanel(PanelSlot.Bottom) },
        )
        if (vm.showLeftPanel) GlassPanel(Modifier.align(Alignment.TopStart).width(210.dp)) {
            PanelHeader("Trig Tools", vm::hidePanels, Cyan)
            listOf("Angle", "Unit Circle", "Triangle", "Identities", "Special Angles", "Graphs").forEach {
                GlowButton(it, onClick = {})
            }
        }
        if (vm.showRightPanel) GlassPanel(Modifier.align(Alignment.TopEnd).width(250.dp)) {
            PanelHeader("Angle Insights", vm::hidePanels, Violet)
            Insight("Angle", "${trim(angle.toDouble())} deg", Cyan)
            Insight("Radians", "${radianLabel(angle.toDouble())} rad", Violet)
            Insight("Quadrant", quadrantLabel(angle.toDouble()), Cyan)
            Insight("sin theta", trim(sinValue), Violet)
            Insight("cos theta", trim(cosValue), Cyan)
            Insight("tan theta", if (tanValue.isFinite()) trim(tanValue) else "undefined", Green)
            Insight("Point P", "(${trim(cosValue)}, ${trim(sinValue)})", Violet)
            Insight("Identity", "sin^2 theta + cos^2 theta = ${trim(sinValue * sinValue + cosValue * cosValue)}", Cyan)
        }
        if (vm.showBottomPanel) GlassPanel(Modifier.align(Alignment.BottomStart).fillMaxWidth()) {
            PanelHeader("Trigonometry Controls", vm::hidePanels, Ink)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Angle ${trim(angle.toDouble())} deg", color = Ink, modifier = Modifier.width(126.dp))
                Slider(value = angle, onValueChange = { angle = snapAngle(it) }, valueRange = -180f..180f, modifier = Modifier.weight(1f))
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(-180f, -90f, 0f, 30f, 45f, 60f, 90f, 180f).forEach {
                    GlowButton("${trim(it.toDouble())} deg", onClick = { angle = it })
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TogglePill("Tangents", showTangents) { showTangents = it }
                TogglePill("Projections", showProjections) { showProjections = it }
                TogglePill("Sine wave", showWave) { showWave = it }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GlowButton("sin theta", onClick = { showWave = true })
                GlowButton("cos theta", onClick = { showProjections = true })
                GlowButton("tan theta", onClick = { showTangents = true })
                GlowButton("Close Panels", onClick = vm::hidePanels)
            }
        }
    }
}

@Composable
private fun SurfaceExampleChips(modifier: Modifier = Modifier, onSelect: (String) -> Unit) {
    val examples = listOf(
        "Paraboloid" to "x^2 + y^2",
        "Saddle" to "x^2 - y^2",
        "Wave" to "sin(x) + cos(y)",
        "Plane" to "0.5*x + y",
        "Cone" to "sqrt(x^2 + y^2)",
        "Ripple" to "sin(x^2 + y^2)",
    )
    FlowRow(
        modifier.padding(top = 88.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        examples.forEachIndexed { index, (label, expression) ->
            Text(
                text = label,
                color = Ink,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (index % 2 == 0) Color(0x5520D9FF) else Color(0x55985DFF))
                    .border(1.dp, if (index % 2 == 0) Cyan else Violet, RoundedCornerShape(16.dp))
                    .clickable { onSelect(expression) }
                    .padding(horizontal = 11.dp, vertical = 7.dp),
            )
        }
    }
}

@Composable
private fun SurfaceViewChips(
    modifier: Modifier = Modifier,
    active: SurfaceViewPreset,
    onSelect: (SurfaceViewPreset) -> Unit,
) {
    FlowRow(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceA.copy(alpha = .92f))
            .border(1.dp, Cyan.copy(.45f), RoundedCornerShape(18.dp))
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SurfaceViewPreset.entries.forEach { preset ->
            Text(
                text = if (preset == active) "• ${preset.name}" else preset.name,
                color = if (preset == active) Color.White else Muted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (preset == active) Color(0x6630D9FF) else Color.Transparent)
                    .clickable { onSelect(preset) }
                    .semantics { contentDescription = "View 3D graph from ${preset.name} axis or plane" }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun CoordinateCanvas(
    modifier: Modifier,
    points: List<Vec2>,
    shapes: List<Shape2D>,
    interactionEnabled: Boolean,
    onPointDragStart: (Int) -> Unit,
    onPointDrag: (Int, Vec2) -> Unit,
    onShapeDragStart: (Int) -> Unit,
    onShapeDrag: (Vec2) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onCanvasTap: (Vec2, Int?) -> Unit,
    content: androidx.compose.ui.graphics.drawscope.DrawScope.(toScreen: (Vec2) -> Offset) -> Unit,
) {
    var cameraCenter by remember { mutableStateOf(Vec2(0.0, 0.0)) }
    var cameraZoom by remember { mutableFloatStateOf(1f) }
    var lastTapAt by remember { mutableStateOf(0L) }
    val currentPoints by rememberUpdatedState(points)
    val currentShapes by rememberUpdatedState(shapes)
    Canvas(
        modifier
            .pointerInput(interactionEnabled) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val baseScale = size.width / 14f
                    fun scale() = baseScale * cameraZoom
                    fun origin() = Offset(
                        size.width / 2f - cameraCenter.x.toFloat() * scale(),
                        size.height / 2f + cameraCenter.y.toFloat() * scale(),
                    )
                    fun world(screen: Offset): Vec2 {
                        val o = origin()
                        return Vec2(((screen.x - o.x) / scale()).toDouble(), ((o.y - screen.y) / scale()).toDouble())
                    }
                    fun screen(point: Vec2): Offset {
                        val o = origin()
                        return Offset(o.x + point.x.toFloat() * scale(), o.y - point.y.toFloat() * scale())
                    }

                    val startWorld = world(down.position)
                    val gesturePoints = currentPoints
                    val gestureShapes = currentShapes
                    val tappedPointIndex = gesturePoints.indices
                        .minByOrNull { (screen(gesturePoints[it]) - down.position).getDistance() }
                        ?.takeIf { (screen(gesturePoints[it]) - down.position).getDistance() <= 38f }
                    var pointIndex: Int? = null
                    var shapeIndex: Int? = null
                    if (interactionEnabled) {
                        pointIndex = tappedPointIndex
                        if (pointIndex == null) {
                            shapeIndex = gestureShapes.indices.filter { gestureShapes[it].visible }
                                .minByOrNull { shapeScreenDistance(gestureShapes[it], gesturePoints, down.position, ::screen) }
                                ?.takeIf { shapeScreenDistance(gestureShapes[it], gesturePoints, down.position, ::screen) <= 42f }
                        }
                        pointIndex?.let(onPointDragStart)
                        shapeIndex?.let(onShapeDragStart)
                    }

                    var moved = false
                    var transformed = false
                    var cancelledObjectDrag = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.size >= 2) {
                            if (!cancelledObjectDrag && (pointIndex != null || shapeIndex != null)) {
                                onDragCancel()
                                pointIndex = null
                                shapeIndex = null
                                cancelledObjectDrag = true
                            }
                            val pan = event.calculatePan()
                            val centroid = event.calculateCentroid()
                            val oldScale = scale()
                            cameraCenter = Vec2(cameraCenter.x - pan.x / oldScale, cameraCenter.y + pan.y / oldScale)
                            val beforeZoom = world(centroid)
                            cameraZoom = (cameraZoom * event.calculateZoom()).coerceIn(.35f, 5f)
                            val afterZoom = world(centroid)
                            cameraCenter += beforeZoom - afterZoom
                            transformed = true
                            event.changes.forEach { it.consume() }
                        } else {
                            val change = event.changes.firstOrNull()
                            val delta = change?.positionChange() ?: Offset.Zero
                            if (delta.getDistance() > 0f) {
                                moved = moved || (change!!.position - down.position).getDistance() > 8f
                                when {
                                    pointIndex != null -> onPointDrag(pointIndex, world(change!!.position))
                                    shapeIndex != null -> onShapeDrag(world(change!!.position) - startWorld)
                                    interactionEnabled -> cameraCenter = Vec2(cameraCenter.x - delta.x / scale(), cameraCenter.y + delta.y / scale())
                                }
                                change!!.consume()
                            }
                        }
                        if (event.changes.none { it.pressed }) break
                    }

                    when {
                        pointIndex != null || shapeIndex != null -> onDragEnd()
                        !moved && !transformed -> {
                            val now = System.currentTimeMillis()
                            if (interactionEnabled && now - lastTapAt < 320L) {
                                cameraCenter = Vec2(0.0, 0.0)
                                cameraZoom = 1f
                                lastTapAt = 0L
                            } else {
                                onCanvasTap(startWorld, tappedPointIndex)
                                lastTapAt = if (interactionEnabled) now else 0L
                            }
                        }
                    }
                }
            },
    ) {
        val scale = size.width / 14f * cameraZoom
        val origin = Offset(size.width / 2f - cameraCenter.x.toFloat() * scale, size.height / 2f + cameraCenter.y.toFloat() * scale)
        val tx: (Vec2) -> Offset = { Offset(origin.x + it.x.toFloat() * scale, origin.y - it.y.toFloat() * scale) }
        drawGrid(origin, scale)
        content(tx)
    }
}

private fun shapeScreenDistance(shape: Shape2D, points: List<Vec2>, target: Offset, screen: (Vec2) -> Offset): Float {
    val worldPoints = shape.pointIndices.mapNotNull(points::getOrNull)
    val vertices = worldPoints.map(screen)
    if (vertices.isEmpty()) return Float.MAX_VALUE
    if (shape.type in setOf(Shape2DType.Circle, Shape2DType.Arc) && vertices.size >= 2) {
        val radius = (vertices[1] - vertices[0]).getDistance()
        return abs((target - vertices[0]).getDistance() - radius)
    }
    if (shape.type == Shape2DType.CircleThreePoints && worldPoints.size >= 3) {
        val centerWorld = Geometry2D.circumcenter(worldPoints[0], worldPoints[1], worldPoints[2]) ?: return Float.MAX_VALUE
        val center = screen(centerWorld)
        return abs((target - center).getDistance() - (vertices[0] - center).getDistance())
    }
    if (shape.type in setOf(Shape2DType.Parallel, Shape2DType.Perpendicular) && worldPoints.size >= 3) {
        val base = worldPoints[1] - worldPoints[0]
        val direction = if (shape.type == Shape2DType.Parallel) base else Vec2(-base.y, base.x)
        val a = screen(worldPoints[2] - direction * 100.0)
        val b = screen(worldPoints[2] + direction * 100.0)
        return pointSegmentDistance(target, a, b)
    }
    if (shape.type == Shape2DType.AngleBisector && worldPoints.size >= 3) {
        val u = worldPoints[0] - worldPoints[1]
        val v = worldPoints[2] - worldPoints[1]
        val direction = u * (1.0 / u.distanceTo(Vec2(0.0, 0.0)).coerceAtLeast(1e-9)) +
            v * (1.0 / v.distanceTo(Vec2(0.0, 0.0)).coerceAtLeast(1e-9))
        return pointSegmentDistance(target, screen(worldPoints[1]), screen(worldPoints[1] + direction * 100.0))
    }
    val edges = when {
        vertices.size == 1 -> emptyList()
        shape.type in setOf(Shape2DType.Triangle, Shape2DType.Polygon, Shape2DType.Rectangle, Shape2DType.Square) ->
            vertices.indices.map { vertices[it] to vertices[(it + 1) % vertices.size] }
        else -> vertices.zipWithNext()
    }
    return edges.minOfOrNull { (a, b) -> pointSegmentDistance(target, a, b) }
        ?: (vertices.first() - target).getDistance()
}

private fun pointSegmentDistance(point: Offset, start: Offset, end: Offset): Float {
    val dx = end.x - start.x
    val dy = end.y - start.y
    val lengthSquared = dx * dx + dy * dy
    if (lengthSquared <= 1e-6f) return (point - start).getDistance()
    val t = (((point.x - start.x) * dx + (point.y - start.y) * dy) / lengthSquared).coerceIn(0f, 1f)
    return hypot(point.x - (start.x + t * dx), point.y - (start.y + t * dy))
}

@Composable
private fun SolidAddStrip(modifier: Modifier = Modifier, onAdd: (SolidType) -> Unit, onAddVector: () -> Unit) {
    FlowRow(
        modifier.padding(top = 132.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SolidType.entries.forEach { type ->
            Text(
                text = type.name,
                color = Ink,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x33101824))
                    .border(1.dp, Color(0x5548BFFF), RoundedCornerShape(16.dp))
                    .clickable { onAdd(type) }
                    .padding(horizontal = 11.dp, vertical = 7.dp),
            )
        }
        Text(
            text = "Vector",
            color = Ink,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0x55FFC857))
                .border(1.dp, Amber, RoundedCornerShape(16.dp))
                .clickable { onAddVector() }
                .padding(horizontal = 11.dp, vertical = 7.dp),
        )
    }
}

@Composable
private fun Transform3DChips(modifier: Modifier = Modifier, active: Transform3DMode, onSelect: (Transform3DMode) -> Unit) {
    Row(
        modifier = modifier
            .padding(bottom = 108.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(SurfaceA)
            .border(1.dp, Color(0x5548BFFF), RoundedCornerShape(22.dp))
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Transform3DMode.entries.forEach { mode ->
            Text(
                text = mode.name,
                color = if (mode == active) Ink else Muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (mode == active) Color(0x6630D9FF) else Color.Transparent)
                    .clickable { onSelect(mode) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun Selection3DChips(modifier: Modifier = Modifier, active: Selection3DMode, onSelect: (Selection3DMode) -> Unit) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceA)
            .border(1.dp, Color(0x5548BFFF), RoundedCornerShape(18.dp))
            .padding(5.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Selection3DMode.entries.forEach { mode ->
            Text(
                text = mode.name,
                color = if (mode == active) Ink else Muted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(13.dp))
                    .background(if (mode == active) Color(0x6630D9FF) else Color.Transparent)
                    .clickable { onSelect(mode) }
                    .padding(horizontal = 9.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun InteractionHint(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = Muted,
        fontSize = 11.sp,
        modifier = modifier
            .padding(end = 16.dp, bottom = 68.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceB)
            .border(1.dp, Color(0x3348BFFF), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp),
    )
}

@Composable
private fun GraphCanvas(
    modifier: Modifier,
    functions: List<com.indianservers.aiexplorer.core.FunctionDefinition>,
    dataPoints: List<Vec2>,
    traceX: Double,
    graphTool: GraphTool,
    onTraceChange: (Double) -> Unit,
) {
    val graph = remember { GraphAnalysis() }
    val engine = remember { ExpressionEngine() }
    var cameraCenter by remember { mutableStateOf(Vec2(0.0, 0.0)) }
    var cameraZoom by remember { mutableFloatStateOf(1f) }
    var lastTapAt by remember { mutableStateOf(0L) }
    val currentFunctions by rememberUpdatedState(functions)
    Canvas(
        modifier
            .pointerInput(graphTool) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val baseScale = size.width / 14f
                    fun scale() = baseScale * cameraZoom
                    fun origin() = Offset(
                        size.width / 2f - cameraCenter.x.toFloat() * scale(),
                        size.height / 2f + cameraCenter.y.toFloat() * scale(),
                    )
                    fun world(screen: Offset): Vec2 {
                        val o = origin()
                        return Vec2(((screen.x - o.x) / scale()).toDouble(), ((o.y - screen.y) / scale()).toDouble())
                    }

                    val traceScreen = currentFunctions.firstOrNull { it.visible && graph.definitionKind(it.expression) == GraphDefinitionKind.Explicit }?.let { fn ->
                        runCatching {
                            val y = engine.compile(stripEquation(fn.expression)).eval(mapOf("x" to traceX))
                            val o = origin()
                            Offset(o.x + traceX.toFloat() * scale(), o.y - y.toFloat() * scale())
                        }.getOrNull()
                    }
                    val traceDrag = graphTool == GraphTool.Trace || (traceScreen != null && (traceScreen - down.position).getDistance() < 44f)
                    var moved = false
                    var transformed = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.size >= 2) {
                            val pan = event.calculatePan()
                            val centroid = event.calculateCentroid()
                            cameraCenter = Vec2(cameraCenter.x - pan.x / scale(), cameraCenter.y + pan.y / scale())
                            val beforeZoom = world(centroid)
                            cameraZoom = (cameraZoom * event.calculateZoom()).coerceIn(.2f, 8f)
                            val afterZoom = world(centroid)
                            cameraCenter += beforeZoom - afterZoom
                            transformed = true
                            event.changes.forEach { it.consume() }
                        } else {
                            val change = event.changes.firstOrNull()
                            val delta = change?.positionChange() ?: Offset.Zero
                            if (delta.getDistance() > 0f) {
                                moved = moved || (change!!.position - down.position).getDistance() > 8f
                                if (traceDrag) {
                                    onTraceChange(world(change!!.position).x)
                                } else {
                                    cameraCenter = Vec2(cameraCenter.x - delta.x / scale(), cameraCenter.y + delta.y / scale())
                                }
                                change!!.consume()
                            }
                        }
                        if (event.changes.none { it.pressed }) break
                    }
                    if (!moved && !transformed) {
                        val now = System.currentTimeMillis()
                        if (now - lastTapAt < 320L) {
                            cameraCenter = Vec2(0.0, 0.0)
                            cameraZoom = 1f
                            lastTapAt = 0L
                        } else {
                            onTraceChange(world(down.position).x)
                            lastTapAt = now
                        }
                    }
                }
            }
            .semantics { contentDescription = "Interactive graphing canvas with axes, curves, trace point, and annotations" },
    ) {
        val scale = size.width / 14f * cameraZoom
        val origin = Offset(size.width / 2f - cameraCenter.x.toFloat() * scale, size.height / 2f + cameraCenter.y.toFloat() * scale)
        val tx: (Vec2) -> Offset = { Offset(origin.x + it.x.toFloat() * scale, origin.y - it.y.toFloat() * scale) }
        drawGrid(origin, scale)
        val halfWidth = size.width / (2f * scale)
        val halfHeight = size.height / (2f * scale)
        val minX = cameraCenter.x - halfWidth
        val maxX = cameraCenter.x + halfWidth
        val minY = cameraCenter.y - halfHeight
        val maxY = cameraCenter.y + halfHeight
        functions.forEachIndexed { index, fn ->
            if (!fn.visible) return@forEachIndexed
            val color = when (fn.colorKey) { "cyan" -> Cyan; "green" -> Green; "amber" -> Amber; else -> Violet }
            val kind = graph.definitionKind(fn.expression)
            if (kind == GraphDefinitionKind.Implicit) {
                val segments = runCatching { graph.implicitSegments(fn.expression, minX, maxX, minY, maxY) }.getOrDefault(emptyList())
                segments.forEach { drawLine(color, tx(it.start), tx(it.end), 3.2f, cap = StrokeCap.Round) }
            } else {
                val sample = runCatching { graph.sampleDefinition(fn.expression, minX, maxX, steps = 520) }.getOrNull()
                sample?.points?.zipWithNext()?.forEachIndexed { i, pair ->
                    if (!sample.breaks.contains(i)) drawLine(color, tx(pair.first), tx(pair.second), 4.2f, cap = StrokeCap.Round)
                }
            }
            val trace = if (kind == GraphDefinitionKind.Explicit) runCatching {
                val y = engine.compile(stripEquation(fn.expression)).eval(mapOf("x" to traceX))
                Vec2(traceX, y)
            }.getOrNull() else null
            trace?.takeIf { abs(tx(it).y - size.height / 2f) <= size.height }?.let {
                if (graphTool in setOf(GraphTool.Trace, GraphTool.Tangent, GraphTool.Normal, GraphTool.Derivative, GraphTool.Integral, GraphTool.AreaBetween) || index == 0) {
                    drawRadiantPoint(tx(it), color, "${fn.name}: (${trim(it.x)}, ${trim(it.y)})")
                }
            }
        }
        if (dataPoints.isNotEmpty()) {
            dataPoints.forEachIndexed { index, point -> drawRadiantPoint(tx(point), Amber, "D${index + 1}") }
            StatisticsEngine.summarize(dataPoints).regression?.let { regression ->
                drawLine(
                    Green,
                    tx(Vec2(minX, regression.slope * minX + regression.intercept)),
                    tx(Vec2(maxX, regression.slope * maxX + regression.intercept)),
                    3f,
                )
            }
        }
        val explicit = functions.filter { it.visible && graph.definitionKind(it.expression) == GraphDefinitionKind.Explicit }
        drawGraphAnalysisOverlay(
            explicit.firstOrNull()?.expression,
            explicit.getOrNull(1)?.expression,
            traceX,
            graphTool,
            engine,
            tx,
            origin,
            scale,
        )
    }
}

@Composable
private fun GraphToolChips(modifier: Modifier = Modifier, active: GraphTool, onSelect: (GraphTool) -> Unit) {
    FlowRow(
        modifier.padding(top = 132.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        GraphTool.entries.forEach {
            Text(
                text = it.name,
                color = if (active == it) Ink else Muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (active == it) Color(0x6630D9FF) else Color(0x33101824))
                    .border(1.dp, if (active == it) Cyan else Color(0x5548BFFF), RoundedCornerShape(16.dp))
                    .clickable { onSelect(it) }
                    .padding(horizontal = 11.dp, vertical = 7.dp),
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGraphAnalysisOverlay(
    expression: String?,
    secondExpression: String?,
    traceX: Double,
    graphTool: GraphTool,
    engine: ExpressionEngine,
    tx: (Vec2) -> Offset,
    origin: Offset,
    scale: Float,
) {
    if (expression == null) return
    val compiled = runCatching { engine.compile(stripEquation(expression)) }.getOrNull() ?: return
    fun f(x: Double) = runCatching { compiled.eval(mapOf("x" to x)) }.getOrDefault(Double.NaN)
    val y = f(traceX)
    if (!y.isFinite()) return
    val point = Vec2(traceX, y)
    val h = 0.001
    val slope = (f(traceX + h) - f(traceX - h)) / (2.0 * h)
    val leftWorld = ((0f - origin.x) / scale).toDouble()
    val rightWorld = ((size.width - origin.x) / scale).toDouble()
    when (graphTool) {
        GraphTool.Tangent -> {
            val tangent = { x: Double -> y + slope * (x - traceX) }
            drawLine(Amber, tx(Vec2(leftWorld, tangent(leftWorld))), tx(Vec2(rightWorld, tangent(rightWorld))), 3f, cap = StrokeCap.Round)
            drawGraphLabel("tangent slope ${trim(slope)}", tx(point) + Offset(18f, -54f), Amber)
        }
        GraphTool.Normal -> {
            val normalSlope = if (abs(slope) < 1e-9) 1e6 else -1.0 / slope
            val normal = { x: Double -> y + normalSlope * (x - traceX) }
            drawLine(Violet, tx(Vec2(leftWorld, normal(leftWorld))), tx(Vec2(rightWorld, normal(rightWorld))), 3f, cap = StrokeCap.Round)
            drawGraphLabel("normal slope ${trim(normalSlope)}", tx(point) + Offset(18f, -54f), Violet)
        }
        GraphTool.Derivative -> {
            val derivativePoints = (0..360).mapNotNull { i ->
                val x = leftWorld + (rightWorld - leftWorld) * i / 360.0
                val d = (f(x + h) - f(x - h)) / (2.0 * h)
                if (d.isFinite()) Vec2(x, d) else null
            }
            derivativePoints.zipWithNext().forEach { (a, b) -> drawLine(Green, tx(a), tx(b), 3f, cap = StrokeCap.Round) }
            drawGraphLabel("f'(${trim(traceX)}) = ${trim(slope)}", tx(point) + Offset(18f, -54f), Green)
        }
        GraphTool.Integral -> {
            val start = 0.0.coerceAtMost(traceX)
            val end = 0.0.coerceAtLeast(traceX)
            val steps = 80
            val path = Path()
            path.moveTo(tx(Vec2(start, 0.0)).x, tx(Vec2(start, 0.0)).y)
            var area = 0.0
            var lastX = start
            var lastY = f(start)
            for (i in 0..steps) {
                val x = start + (end - start) * i / steps
                val yy = f(x)
                if (yy.isFinite()) {
                    val p = tx(Vec2(x, yy))
                    path.lineTo(p.x, p.y)
                    if (i > 0) area += (yy + lastY) * .5 * (x - lastX)
                    lastX = x
                    lastY = yy
                }
            }
            path.lineTo(tx(Vec2(end, 0.0)).x, tx(Vec2(end, 0.0)).y)
            path.close()
            drawPath(path, Brush.verticalGradient(listOf(Cyan.copy(.33f), Violet.copy(.18f), Color.Transparent)))
            drawPath(path, Cyan.copy(.8f), style = Stroke(2f))
            drawGraphLabel("area ${trim(area)}", tx(point) + Offset(18f, -54f), Cyan)
        }
        GraphTool.AreaBetween -> {
            val other = secondExpression?.let { runCatching { engine.compile(stripEquation(it)) }.getOrNull() } ?: return
            val start = min(0.0, traceX)
            val end = max(0.0, traceX)
            val steps = 100
            val top = (0..steps).mapNotNull { i ->
                val x = start + (end - start) * i / steps
                val yy = f(x)
                if (yy.isFinite()) Vec2(x, yy) else null
            }
            val bottom = (steps downTo 0).mapNotNull { i ->
                val x = start + (end - start) * i / steps
                val yy = runCatching { other.eval(mapOf("x" to x)) }.getOrDefault(Double.NaN)
                if (yy.isFinite()) Vec2(x, yy) else null
            }
            if (top.isNotEmpty() && bottom.isNotEmpty()) {
                val path = Path().apply {
                    val first = tx(top.first()); moveTo(first.x, first.y)
                    (top.drop(1) + bottom).forEach { val p = tx(it); lineTo(p.x, p.y) }
                    close()
                }
                drawPath(path, Brush.verticalGradient(listOf(Amber.copy(.30f), Violet.copy(.18f))))
                drawPath(path, Amber, style = Stroke(2f))
            }
        }
        GraphTool.Intersections -> {
            val graph = GraphAnalysis()
            val other = secondExpression?.let { runCatching { engine.compile(stripEquation(it)) }.getOrNull() }
            val points = if (other == null) graph.roots(expression, leftWorld, rightWorld).map { Vec2(it, f(it)) }
            else graph.intersections(compiled, other, leftWorld, rightWorld)
            points.forEach { drawRadiantPoint(tx(it), Amber, "(${trim(it.x)}, ${trim(it.y)})") }
        }
        GraphTool.Extrema -> {
            GraphAnalysis().extrema(expression, leftWorld, rightWorld).forEach {
                drawRadiantPoint(tx(it), Green, "extremum (${trim(it.x)}, ${trim(it.y)})")
            }
        }
        GraphTool.Plot, GraphTool.Trace, GraphTool.Table, GraphTool.Data, GraphTool.Probability -> Unit
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGraphLabel(text: String, position: Offset, color: Color) {
    drawRoundRect(SurfaceA, topLeft = position, size = Size(190f, 42f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f))
    drawRoundRect(color.copy(.75f), topLeft = position, size = Size(190f, 42f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f), style = Stroke(1.4f))
    drawTrigText(text, position.x + 12f, position.y + 27f, color)
}

@Composable
private fun TrigCanvas(
    modifier: Modifier,
    angleDegrees: Float,
    showTangents: Boolean,
    showProjections: Boolean,
    showWave: Boolean,
) {
    Canvas(modifier.semantics { contentDescription = "Interactive trigonometry workspace with unit circle and sine wave" }) {
        val angle = Math.toRadians(angleDegrees.toDouble())
        val center = Offset(size.width * .47f, size.height * .42f)
        val radius = size.minDimension * .235f
        fun circlePoint(theta: Double) = Offset(
            center.x + cos(theta).toFloat() * radius,
            center.y - sin(theta).toFloat() * radius,
        )
        drawTrigGrid()
        drawLine(Color.White.copy(.85f), Offset(center.x - radius * 1.35f, center.y), Offset(center.x + radius * 1.38f, center.y), 2.4f)
        drawLine(Color.White.copy(.85f), Offset(center.x, center.y + radius * 1.35f), Offset(center.x, center.y - radius * 1.38f), 2.4f)
        drawCircle(Cyan.copy(alpha = .88f), radius = radius, center = center, style = Stroke(3.2f))
        val p = circlePoint(angle)
        val cosPoint = Offset(p.x, center.y)
        val sinPoint = Offset(center.x, p.y)
        drawLine(Violet, center, p, 4f, cap = StrokeCap.Round)
        if (showProjections) {
            drawLine(Cyan.copy(alpha = .9f), p, cosPoint, 2.8f)
            drawLine(Violet.copy(alpha = .8f), p, sinPoint, 2.4f)
            drawLine(Cyan, center, cosPoint, 4f, cap = StrokeCap.Round)
            drawLine(Violet, center, sinPoint, 3.4f, cap = StrokeCap.Round)
        }
        if (showTangents) {
            val tangentX = center.x + radius
            val tangentY = center.y - tan(angle).toFloat().coerceIn(-3f, 3f) * radius
            drawLine(Cyan.copy(alpha = .75f), Offset(tangentX, center.y - radius * 1.55f), Offset(tangentX, center.y + radius * 1.55f), 2f)
            drawLine(Cyan.copy(alpha = .75f), p, Offset(tangentX, tangentY), 2.8f)
        }
        drawArc(Violet, startAngle = -angleDegrees, sweepAngle = angleDegrees, useCenter = false, topLeft = Offset(center.x - 64f, center.y - 64f), size = Size(128f, 128f), style = Stroke(4f, cap = StrokeCap.Round))
        listOf(0.0, PI / 2, PI, 3 * PI / 2).forEach { theta ->
            drawRadiantPoint(circlePoint(theta), Cyan, when (theta) {
                0.0 -> "(1, 0)"
                PI / 2 -> "(0, 1)"
                PI -> "(-1, 0)"
                else -> "(0, -1)"
            })
        }
        drawRadiantPoint(center, Color.White, "O")
        drawRadiantPoint(p, Violet, "P (${trim(cos(angle))}, ${trim(sin(angle))})")
        drawTrigText("theta", center.x + 68f, center.y - 24f, Violet)
        drawTrigText("cos theta", (center.x + cosPoint.x) / 2f, center.y + 34f, Cyan)
        drawTrigText("sin theta", p.x + 12f, (center.y + p.y) / 2f, Violet)
        drawTrigText("tan theta", center.x + radius + 22f, center.y - 82f, Cyan)
        drawQuadrantCards(center, radius)
        drawRightTriangleCard(Offset(size.width * .76f, size.height * .48f), angleDegrees)
        drawIdentitiesCard(Offset(size.width * .76f, size.height * .64f))
        if (showWave) drawSineWavePane(Offset(size.width * .15f, size.height * .68f), Size(size.width * .56f, size.height * .2f), angleDegrees)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTrigGrid() {
    val step = 38f
    var x = 0f
    while (x <= size.width) {
        drawLine(Grid.copy(alpha = .35f), Offset(x, 0f), Offset(x, size.height), 1f)
        x += step
    }
    var y = 0f
    while (y <= size.height) {
        drawLine(Grid.copy(alpha = .35f), Offset(0f, y), Offset(size.width, y), 1f)
        y += step
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawQuadrantCards(center: Offset, radius: Float) {
    val items = listOf(
        "QI\nsin +\ncos +\ntan +" to Offset(center.x + radius * .92f, center.y - radius * 1.06f),
        "QII\nsin +\ncos -\ntan -" to Offset(center.x - radius * .95f, center.y - radius * 1.05f),
        "QIII\nsin -\ncos -\ntan +" to Offset(center.x - radius * 1.04f, center.y + radius * .9f),
        "QIV\nsin -\ncos +\ntan -" to Offset(center.x + radius * .93f, center.y + radius * .92f),
    )
    items.forEach { (text, p) ->
        drawRoundRect(SurfaceA, topLeft = p, size = Size(86f, 104f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f))
        drawRoundRect(Color(0x5548BFFF), topLeft = p, size = Size(86f, 104f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f), style = Stroke(1.5f))
        text.lines().forEachIndexed { index, line -> drawTrigText(line, p.x + 14f, p.y + 24f + index * 22f, if (index == 0) Ink else Muted) }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRightTriangleCard(topLeft: Offset, angleDegrees: Float) {
    drawRoundRect(SurfaceA, topLeft = topLeft, size = Size(250f, 170f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f))
    drawRoundRect(Color(0x66985DFF), topLeft = topLeft, size = Size(250f, 170f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f), style = Stroke(1.8f))
    val a = Offset(topLeft.x + 40f, topLeft.y + 130f)
    val b = Offset(topLeft.x + 190f, topLeft.y + 130f)
    val c = Offset(topLeft.x + 190f, topLeft.y + 35f)
    drawLine(Violet, a, b, 3f)
    drawLine(Violet, b, c, 3f)
    drawLine(Violet, a, c, 3f)
    drawTrigText("Right Triangle", topLeft.x + 18f, topLeft.y + 28f, Cyan)
    drawTrigText("${trim(angleDegrees.toDouble())} deg", topLeft.x + 118f, topLeft.y + 102f, Ink)
    drawTrigText("1", topLeft.x + 112f, topLeft.y + 154f, Ink)
    drawTrigText("tan theta", topLeft.x + 198f, topLeft.y + 85f, Ink)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawIdentitiesCard(topLeft: Offset) {
    val lines = listOf(
        "Trig Identities",
        "sin^2 theta + cos^2 theta = 1",
        "tan theta = sin theta / cos theta",
        "1 + tan^2 theta = sec^2 theta",
        "sin(-theta) = -sin theta",
        "cos(-theta) = cos theta",
    )
    drawRoundRect(SurfaceA, topLeft = topLeft, size = Size(310f, 170f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f))
    drawRoundRect(Color(0x66985DFF), topLeft = topLeft, size = Size(310f, 170f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f), style = Stroke(1.8f))
    lines.forEachIndexed { index, line -> drawTrigText(line, topLeft.x + 18f, topLeft.y + 28f + index * 24f, if (index == 0) Cyan else Ink) }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSineWavePane(topLeft: Offset, paneSize: Size, angleDegrees: Float) {
    drawRoundRect(SurfaceA, topLeft = topLeft, size = paneSize, cornerRadius = androidx.compose.ui.geometry.CornerRadius(20f, 20f))
    drawRoundRect(Color(0x5548BFFF), topLeft = topLeft, size = paneSize, cornerRadius = androidx.compose.ui.geometry.CornerRadius(20f, 20f), style = Stroke(1.8f))
    val origin = Offset(topLeft.x + 42f, topLeft.y + paneSize.height / 2f)
    val width = paneSize.width - 70f
    val amp = paneSize.height * .34f
    drawLine(Color.White.copy(.8f), origin, Offset(topLeft.x + paneSize.width - 20f, origin.y), 2f)
    drawLine(Color.White.copy(.8f), Offset(origin.x, topLeft.y + 18f), Offset(origin.x, topLeft.y + paneSize.height - 18f), 2f)
    val path = Path()
    for (i in 0..240) {
        val t = i / 240f
        val x = origin.x + t * width
        val y = origin.y - sin(t * Math.PI * 2).toFloat() * amp
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(path, Violet, style = Stroke(3f, cap = StrokeCap.Round))
    val t = ((angleDegrees % 360f) + 360f) % 360f / 360f
    val px = origin.x + t * width
    val py = origin.y - sin(Math.toRadians(angleDegrees.toDouble())).toFloat() * amp
    drawLine(Violet.copy(alpha = .7f), Offset(px, origin.y), Offset(px, py), 2f)
    drawRadiantPoint(Offset(px, py), Violet, "${trim(angleDegrees.toDouble())} deg")
    drawTrigText("Sine wave: y = sin theta", topLeft.x + 22f, topLeft.y + 28f, Ink)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTrigText(text: String, x: Float, y: Float, color: Color) {
    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            this.color = android.graphics.Color.rgb((color.red * 255).toInt(), (color.green * 255).toInt(), (color.blue * 255).toInt())
            textSize = 24f
            isAntiAlias = true
        }
        drawText(text, x, y, paint)
    }
}

@Composable
private fun Projected3DCanvas(
    modifier: Modifier,
    solids: List<Solid>,
    vectors: List<Vector3D>,
    selectedIndex: Int,
    selectedVectorIndex: Int,
    rx: Float,
    ry: Float,
    rz: Float,
    zoom: Float,
    cameraPan: Offset,
    transformMode: Transform3DMode,
    wire: Boolean,
    perspective: Boolean,
    selectionMode: Selection3DMode,
    subSelection: SubObjectSelection?,
    sectionEnabled: Boolean,
    sectionY: Double,
    clipSection: Boolean,
    onSelect: (Int) -> Unit,
    onSubSelect: (SubObjectSelection?) -> Unit,
    onSelectVector: (Int) -> Unit,
    onSolidDragStart: (Int) -> Unit,
    onSolidMove: (Int, Vec3) -> Unit,
    onSolidRotate: (Int, Vec3) -> Unit,
    onSolidScale: (Int, Double) -> Unit,
    onSolidDragEnd: () -> Unit,
    onSolidDragCancel: () -> Unit,
    onVectorDragStart: (Int) -> Unit,
    onVectorMove: (Int, Vec3) -> Unit,
    onVectorDragEnd: () -> Unit,
    onVectorDragCancel: () -> Unit,
    onOrbit: (Float, Float) -> Unit,
    onPan: (Offset) -> Unit,
    onZoom: (Float) -> Unit,
    onResetCamera: () -> Unit,
) {
    var lastTapAt by remember { mutableStateOf(0L) }
    val currentSolids by rememberUpdatedState(solids)
    val currentVectors by rememberUpdatedState(vectors)
    val currentRx by rememberUpdatedState(rx)
    val currentRy by rememberUpdatedState(ry)
    val currentRz by rememberUpdatedState(rz)
    val currentZoom by rememberUpdatedState(zoom)
    val currentPan by rememberUpdatedState(cameraPan)
    val currentPerspective by rememberUpdatedState(perspective)
    val currentSelectionMode by rememberUpdatedState(selectionMode)
    Canvas(
        modifier
            .pointerInput(transformMode) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val gestureSolids = currentSolids
                    val gestureVectors = currentVectors
                    val gestureRx = currentRx
                    val gestureRy = currentRy
                    val gestureRz = currentRz
                    val center = Offset(size.width * .52f, size.height * .45f) + currentPan
                    val scale = 74f * currentZoom
                    fun vectorDistance(index: Int, target: Offset): Float {
                        val vector = gestureVectors[index]
                        val a = project(rotate(vector.start, gestureRx, gestureRy, gestureRz), center, scale, currentPerspective)
                        val b = project(rotate(vector.end, gestureRx, gestureRy, gestureRz), center, scale, currentPerspective)
                        return pointSegmentDistance(target, a, b)
                    }
                    fun solidDistance(index: Int, target: Offset): Float {
                        val projected = project(rotate(gestureSolids[index].position, gestureRx, gestureRy, gestureRz), center, scale, currentPerspective)
                        return (projected - target).getDistance()
                    }

                    fun projectVertex(solid: Solid, vertex: Vec3): Offset = project(
                        rotate(solidLocalToWorld(solid, vertex), gestureRx, gestureRy, gestureRz),
                        center,
                        scale,
                        currentPerspective,
                    )

                    fun pickSubObject(target: Offset): SubObjectSelection? {
                        val mode = currentSelectionMode
                        if (mode == Selection3DMode.Object) return null
                        var best: SubObjectSelection? = null
                        var bestDistance = Float.MAX_VALUE
                        gestureSolids.forEachIndexed { solidIndex, solid ->
                            val mesh = SolidMeshFactory.create(solid)
                            val points = mesh.vertices.map { projectVertex(solid, it) }
                            when (mode) {
                                Selection3DMode.Vertex -> points.forEachIndexed { index, point ->
                                    val distance = (point - target).getDistance()
                                    if (distance < bestDistance) { bestDistance = distance; best = SubObjectSelection(solidIndex, mode, index) }
                                }
                                Selection3DMode.Edge -> mesh.edges.forEachIndexed { index, edge ->
                                    val distance = pointSegmentDistance(target, points[edge.first], points[edge.second])
                                    if (distance < bestDistance) { bestDistance = distance; best = SubObjectSelection(solidIndex, mode, index) }
                                }
                                Selection3DMode.Face -> mesh.faces.forEachIndexed { index, face ->
                                    val centroid = face.map { points[it] }.reduce(Offset::plus) / face.size.toFloat()
                                    val distance = (centroid - target).getDistance()
                                    if (distance < bestDistance) { bestDistance = distance; best = SubObjectSelection(solidIndex, mode, index) }
                                }
                                Selection3DMode.Object -> Unit
                            }
                        }
                        val limit = when (mode) {
                            Selection3DMode.Vertex -> 28f
                            Selection3DMode.Edge -> 22f
                            Selection3DMode.Face -> 70f
                            Selection3DMode.Object -> 0f
                        }
                        return best?.takeIf { bestDistance <= limit }
                    }

                    val subHit = pickSubObject(down.position)
                    subHit?.let {
                        onSelect(it.solidIndex)
                        onSubSelect(it)
                    }
                    var vectorIndex = if (subHit == null && currentSelectionMode == Selection3DMode.Object) gestureVectors.indices.minByOrNull { vectorDistance(it, down.position) }
                        ?.takeIf { vectorDistance(it, down.position) < 42f }
                    else null
                    var solidIndex = if (vectorIndex == null) {
                        gestureSolids.indices.minByOrNull { solidDistance(it, down.position) }
                            ?.takeIf { solidDistance(it, down.position) < 104f }
                    } else null
                    if (currentSelectionMode != Selection3DMode.Object || subHit != null) solidIndex = null
                    vectorIndex?.let {
                        onSelectVector(it)
                        onVectorDragStart(it)
                    }
                    solidIndex?.let {
                        onSelect(it)
                        onSolidDragStart(it)
                    }

                    var total = Offset.Zero
                    var moved = false
                    var transformed = false
                    var objectCancelled = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.size >= 2) {
                            if (!objectCancelled) {
                                if (solidIndex != null) onSolidDragCancel()
                                if (vectorIndex != null) onVectorDragCancel()
                                solidIndex = null
                                vectorIndex = null
                                objectCancelled = true
                            }
                            onPan(event.calculatePan())
                            onZoom(event.calculateZoom())
                            transformed = true
                            event.changes.forEach { it.consume() }
                        } else {
                            val change = event.changes.firstOrNull()
                            val delta = change?.positionChange() ?: Offset.Zero
                            if (delta.getDistance() > 0f) {
                                total += delta
                                moved = moved || total.getDistance() > 8f
                                when {
                                    vectorIndex != null -> onVectorMove(
                                        vectorIndex,
                                        Vec3((total.x / scale).toDouble(), 0.0, (total.y / scale).toDouble()),
                                    )
                                    solidIndex != null -> when (transformMode) {
                                        Transform3DMode.Move -> onSolidMove(
                                            solidIndex,
                                            Vec3((total.x / scale).toDouble(), 0.0, (total.y / scale).toDouble()),
                                        )
                                        Transform3DMode.Rotate -> onSolidRotate(
                                            solidIndex,
                                            Vec3((-total.y * .35f).toDouble(), (total.x * .35f).toDouble(), 0.0),
                                        )
                                        Transform3DMode.Scale -> onSolidScale(
                                            solidIndex,
                                            (1.0 + (total.x - total.y) / 260.0).coerceAtLeast(.2),
                                        )
                                    }
                                    subHit != null -> Unit
                                    else -> onOrbit(delta.x * .35f, -delta.y * .25f)
                                }
                                change!!.consume()
                            }
                        }
                        if (event.changes.none { it.pressed }) break
                    }

                    if (solidIndex != null) onSolidDragEnd()
                    if (vectorIndex != null) onVectorDragEnd()
                    if (!moved && !transformed && solidIndex == null && vectorIndex == null && subHit == null) {
                        val now = System.currentTimeMillis()
                        if (now - lastTapAt < 320L) {
                            onResetCamera()
                            lastTapAt = 0L
                        } else {
                            lastTapAt = now
                        }
                    }
                }
            }
            .semantics { contentDescription = "Interactive 3D workspace with object, vertex, edge and face selection" },
    ) {
        val center = Offset(size.width * .52f, size.height * .45f) + cameraPan
        drawPerspectiveGrid(center)
        vectors.forEachIndexed { index, vector ->
            drawVector3D(vector, rx, ry, rz, center, 74f * zoom, if (index == selectedVectorIndex) Amber else Green, index == selectedVectorIndex, perspective)
        }
        solids.forEachIndexed { index, solid ->
            val color = if (index == selectedIndex) Cyan else if (index % 2 == 0) Violet else Green
            drawSolidProjection(
                solid, solid.position, rx, ry, rz, center, 74f * zoom, color, wire, index == selectedIndex,
                perspective, subSelection?.takeIf { it.solidIndex == index }, sectionEnabled && index == selectedIndex,
                sectionY, clipSection && index == selectedIndex,
            )
            if (index == selectedIndex) {
                val anchor = project(rotate(solid.position, rx, ry, rz), center, 74f * zoom, perspective)
                drawTransformGizmo(anchor, transformMode)
            }
        }
    }
}

@Composable
private fun SurfaceCanvas3D(
    modifier: Modifier,
    expression: String,
    mesh: com.indianservers.aiexplorer.core.SurfaceMesh?,
    rotation: Float,
    tilt: Float,
    roll: Float,
    zoom: Float,
    cameraPan: Offset,
    sliceZ: Double,
    trace: Vec2,
    showWireframe: Boolean,
    showContours: Boolean,
    showSlice: Boolean,
    showGradient: Boolean,
    showBox: Boolean,
    activeTool: SurfaceTool,
    onRotate: (Float) -> Unit,
    onTilt: (Float) -> Unit,
    onRoll: (Float) -> Unit,
    onPan: (Offset) -> Unit,
    onZoom: (Float) -> Unit,
    onResetCamera: () -> Unit,
    onTrace: (Vec2) -> Unit,
) {
    val engine = remember { ExpressionEngine() }
    var lastTapAt by remember { mutableStateOf(0L) }
    val currentZoom by rememberUpdatedState(zoom)
    val currentPan by rememberUpdatedState(cameraPan)
    Canvas(
        modifier
            .pointerInput(activeTool) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val center = Offset(size.width * .5f, size.height * .5f) + currentPan
                    val scale = 54f * currentZoom
                    fun traceAt(position: Offset) = Vec2(
                        ((position.x - center.x) / scale).toDouble().coerceIn(-3.0, 3.0),
                        ((position.y - center.y) / scale).toDouble().coerceIn(-3.0, 3.0),
                    )
                    var moved = false
                    var transformed = false
                    var total = Offset.Zero
                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.size >= 2) {
                            onPan(event.calculatePan())
                            onZoom(event.calculateZoom())
                            onRoll(event.calculateRotation())
                            transformed = true
                            event.changes.forEach { it.consume() }
                        } else {
                            val change = event.changes.firstOrNull()
                            val delta = change?.positionChange() ?: Offset.Zero
                            if (delta.getDistance() > 0f) {
                                total += delta
                                moved = moved || total.getDistance() > 8f
                                if (activeTool == SurfaceTool.Trace) {
                                    onTrace(traceAt(change!!.position))
                                } else {
                                    onRotate(delta.x * .35f)
                                    onTilt(-delta.y * .18f)
                                }
                                change!!.consume()
                            }
                        }
                        if (event.changes.none { it.pressed }) break
                    }
                    if (!moved && !transformed) {
                        val now = System.currentTimeMillis()
                        if (now - lastTapAt < 320L) {
                            onResetCamera()
                            lastTapAt = 0L
                        } else {
                            if (activeTool == SurfaceTool.Trace) onTrace(traceAt(down.position))
                            lastTapAt = now
                        }
                    }
                }
            }
            .semantics { contentDescription = "Interactive 3D graph: drag to orbit, two fingers pan, pinch zoom, twist roll, and tap axis or plane views" },
    ) {
        val center = Offset(size.width * .5f, size.height * .5f) + cameraPan
        val scale = 54f * zoom
        fun map(v: Vec3) = project(rotate(v, tilt, rotation, roll), center, scale)
        drawPerspectiveGrid(center)
        drawCoordinatePlanes3D(::map)
        if (showBox) drawSurfaceBox(::map)
        mesh?.vertices?.chunked(mesh.columns)?.forEachIndexed { rowIndex, row ->
            row.zipWithNext().forEachIndexed { columnIndex, (a, b) ->
                val alpha = if (showWireframe || rowIndex % 3 == 0 || columnIndex % 3 == 0) .78f else .28f
                drawLine(Cyan.copy(alpha = alpha), map(a), map(b), if (showWireframe) 1.8f else 1.1f)
            }
        }
        mesh?.vertices?.groupBy { it.y }?.values?.forEachIndexed { index, col ->
            col.zipWithNext().forEach { (a, b) ->
                val alpha = if (showWireframe || index % 3 == 0) .58f else .22f
                drawLine(Violet.copy(alpha = alpha), map(a), map(b), if (showWireframe) 1.3f else .9f)
            }
        }
        if (showContours) drawSurfaceContours(mesh, ::map)
        if (showSlice) drawSurfaceSlice(mesh, sliceZ, ::map)
        val compiled = runCatching { engine.compile(stripEquation(expression).replace("y", "yy")) }.getOrNull()
        compiled?.let {
            val z = runCatching { it.eval(mapOf("x" to trace.x, "yy" to trace.y)) }.getOrDefault(Double.NaN)
            if (z.isFinite()) {
                val point = Vec3(trace.x, trace.y, z.coerceIn(-8.0, 8.0))
                val screenPoint = map(point)
                drawRadiantPoint(screenPoint, Amber, "(${trim(trace.x)}, ${trim(trace.y)}, ${trim(z)})")
                if (showGradient) drawGradientVector(it, trace, z, ::map)
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCoordinatePlanes3D(map: (Vec3) -> Offset) {
    fun plane(points: List<Vec3>, color: Color) {
        val projected = points.map(map)
        val path = Path().apply {
            moveTo(projected.first().x, projected.first().y)
            projected.drop(1).forEach { lineTo(it.x, it.y) }
            close()
        }
        drawPath(path, color.copy(alpha = .045f))
        drawPath(path, color.copy(alpha = .22f), style = Stroke(1.2f))
    }
    plane(listOf(Vec3(-3.0, -3.0, 0.0), Vec3(3.0, -3.0, 0.0), Vec3(3.0, 3.0, 0.0), Vec3(-3.0, 3.0, 0.0)), Cyan)
    plane(listOf(Vec3(-3.0, 0.0, -1.0), Vec3(3.0, 0.0, -1.0), Vec3(3.0, 0.0, 6.0), Vec3(-3.0, 0.0, 6.0)), Green)
    plane(listOf(Vec3(0.0, -3.0, -1.0), Vec3(0.0, 3.0, -1.0), Vec3(0.0, 3.0, 6.0), Vec3(0.0, -3.0, 6.0)), Violet)

    val origin = map(Vec3(0.0, 0.0, 0.0))
    val x = map(Vec3(3.7, 0.0, 0.0))
    val y = map(Vec3(0.0, 3.7, 0.0))
    val z = map(Vec3(0.0, 0.0, 4.8))
    drawLine(Color(0xFFFF5B68), origin, x, 4f, cap = StrokeCap.Round)
    drawLine(Green, origin, y, 4f, cap = StrokeCap.Round)
    drawLine(Cyan, origin, z, 4f, cap = StrokeCap.Round)
    drawCircle(Color.White, 5f, origin)
    drawGraphLabel("X", x + Offset(8f, 0f), Color(0xFFFF5B68))
    drawGraphLabel("Y", y + Offset(8f, 0f), Green)
    drawGraphLabel("Z", z + Offset(8f, 0f), Cyan)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSurfaceBox(map: (Vec3) -> Offset) {
    val corners = listOf(
        Vec3(-3.0, -3.0, -1.0), Vec3(3.0, -3.0, -1.0), Vec3(3.0, 3.0, -1.0), Vec3(-3.0, 3.0, -1.0),
        Vec3(-3.0, -3.0, 7.0), Vec3(3.0, -3.0, 7.0), Vec3(3.0, 3.0, 7.0), Vec3(-3.0, 3.0, 7.0),
    )
    listOf(0 to 1, 1 to 2, 2 to 3, 3 to 0, 4 to 5, 5 to 6, 6 to 7, 7 to 4, 0 to 4, 1 to 5, 2 to 6, 3 to 7).forEach { (a, b) ->
        drawLine(Cyan.copy(.33f), map(corners[a]), map(corners[b]), 1.6f)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSurfaceContours(
    mesh: com.indianservers.aiexplorer.core.SurfaceMesh?,
    map: (Vec3) -> Offset,
) {
    val rows = mesh?.vertices?.chunked(mesh.columns) ?: return
    val levels = listOf(0.0, 1.0, 2.0, 4.0, 6.0)
    levels.forEachIndexed { levelIndex, level ->
        val color = if (levelIndex % 2 == 0) Cyan else Violet
        rows.forEach { row ->
            row.zipWithNext().forEach { (a, b) ->
                if ((a.z - level) * (b.z - level) <= 0.0 && abs(a.z - b.z) > 1e-6) {
                    val t = ((level - a.z) / (b.z - a.z)).coerceIn(0.0, 1.0)
                    val p = Vec3(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t, level)
                    drawCircle(color.copy(.72f), 2.4f, map(p))
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSurfaceSlice(
    mesh: com.indianservers.aiexplorer.core.SurfaceMesh?,
    sliceZ: Double,
    map: (Vec3) -> Offset,
) {
    val rows = mesh?.vertices?.chunked(mesh.columns) ?: return
    rows.forEach { row ->
        val slicePoints = row.zipWithNext().mapNotNull { (a, b) ->
            if ((a.z - sliceZ) * (b.z - sliceZ) <= 0.0 && abs(a.z - b.z) > 1e-6) {
                val t = ((sliceZ - a.z) / (b.z - a.z)).coerceIn(0.0, 1.0)
                Vec3(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t, sliceZ)
            } else {
                null
            }
        }
        slicePoints.zipWithNext().forEach { (a, b) -> drawLine(Amber, map(a), map(b), 3f, cap = StrokeCap.Round) }
    }
    drawGraphLabel("slice z = ${trim(sliceZ)}", Offset(size.width * .08f, size.height * .22f), Amber)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGradientVector(
    expression: com.indianservers.aiexplorer.core.Expression,
    trace: Vec2,
    z: Double,
    map: (Vec3) -> Offset,
) {
    val h = .01
    fun f(x: Double, y: Double) = runCatching { expression.eval(mapOf("x" to x, "yy" to y)) }.getOrDefault(Double.NaN)
    val dx = (f(trace.x + h, trace.y) - f(trace.x - h, trace.y)) / (2.0 * h)
    val dy = (f(trace.x, trace.y + h) - f(trace.x, trace.y - h)) / (2.0 * h)
    if (!dx.isFinite() || !dy.isFinite()) return
    val start = Vec3(trace.x, trace.y, z.coerceIn(-8.0, 8.0))
    val end = Vec3((trace.x + dx * .35).coerceIn(-3.0, 3.0), (trace.y + dy * .35).coerceIn(-3.0, 3.0), (z + .55).coerceIn(-8.0, 8.0))
    drawLine(Green, map(start), map(end), 4f, cap = StrokeCap.Round)
    drawRadiantPoint(map(end), Green, "grad <${trim(dx)}, ${trim(dy)}>")
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGrid(origin: Offset, scale: Float) {
    val firstX = floor(-origin.x / scale).toInt() - 1
    val lastX = ceil((size.width - origin.x) / scale).toInt() + 1
    val firstY = floor(-origin.y / scale).toInt() - 1
    val lastY = ceil((size.height - origin.y) / scale).toInt() + 1
    for (i in firstX..lastX) {
        val x = origin.x + i * scale
        drawLine(Grid, Offset(x, 0f), Offset(x, size.height), if (i == 0) 3f else 1f)
    }
    for (i in firstY..lastY) {
        val y = origin.y + i * scale
        drawLine(Grid, Offset(0f, y), Offset(size.width, y), if (i == 0) 3f else 1f)
    }
    drawLine(Color.White.copy(.85f), Offset(0f, origin.y), Offset(size.width, origin.y), 2f)
    drawLine(Color.White.copy(.85f), Offset(origin.x, 0f), Offset(origin.x, size.height), 2f)
    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 28f
            isAntiAlias = true
        }
        drawText("x", size.width - 30f, origin.y - 14f, paint)
        drawText("y", origin.x + 14f, 30f, paint)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPerspectiveGrid(center: Offset) {
    for (i in -8..8) {
        drawLine(Grid, Offset(center.x + i * 48f, center.y - 330f), Offset(center.x + i * 78f, center.y + 330f), 1f)
        drawLine(Grid, Offset(center.x - 420f, center.y + i * 34f), Offset(center.x + 420f, center.y + i * 34f), 1f)
    }
    drawLine(Cyan, center, center + Offset(260f, 110f), 3f)
    drawLine(Cyan, center, center + Offset(-220f, 140f), 3f)
    drawLine(Cyan, center, center + Offset(0f, -260f), 3f)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRadiantPoint(position: Offset, color: Color, label: String) {
    drawCircle(color.copy(alpha = .18f), 26f, position)
    drawCircle(color, 11f, position)
    drawCircle(Color.White, 5f, position)
    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            this.color = android.graphics.Color.rgb((color.red * 255).toInt(), (color.green * 255).toInt(), (color.blue * 255).toInt())
            textSize = 28f
            isAntiAlias = true
        }
        drawText(label, position.x + 14f, position.y - 14f, paint)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawConstructedShapes(
    points: List<Vec2>,
    activeTool: GeometryTool,
    tx: (Vec2) -> Offset,
) {
    if (points.size >= 2) {
        val a = tx(points[0])
        val b = tx(points[1])
        when (activeTool) {
            GeometryTool.Line -> {
                val direction = b - a
                val length = direction.getDistance().coerceAtLeast(1f)
                val unit = Offset(direction.x / length, direction.y / length)
                drawLine(Cyan.copy(alpha = .85f), a - unit * 2000f, a + unit * 2000f, 3.5f)
            }
            GeometryTool.Ray -> {
                val direction = b - a
                val length = direction.getDistance().coerceAtLeast(1f)
                val unit = Offset(direction.x / length, direction.y / length)
                drawLine(Cyan.copy(alpha = .85f), a, a + unit * 2000f, 3.5f)
            }
            GeometryTool.Segment, GeometryTool.Select, GeometryTool.Point, GeometryTool.Measure -> {
                drawLine(Violet.copy(alpha = .9f), a, b, 4f, cap = StrokeCap.Round)
            }
            GeometryTool.Circle -> {
                drawCircle(Cyan.copy(alpha = .8f), radius = (b - a).getDistance(), center = a, style = Stroke(3f))
            }
            GeometryTool.Rectangle, GeometryTool.Square -> {
                val width = b.x - a.x
                val height = if (activeTool == GeometryTool.Square) width else b.y - a.y
                val path = Path().apply {
                    moveTo(a.x, a.y)
                    lineTo(a.x + width, a.y)
                    lineTo(a.x + width, a.y + height)
                    lineTo(a.x, a.y + height)
                    close()
                }
                drawPath(path, Violet.copy(alpha = .18f))
                drawPath(path, Violet, style = Stroke(3f))
            }
            GeometryTool.Arc -> {
                drawArc(Cyan, startAngle = 200f, sweepAngle = 110f, useCenter = false, topLeft = Offset(a.x - 90f, a.y - 90f), size = Size(180f, 180f), style = Stroke(4f, cap = StrokeCap.Round))
            }
            else -> Unit
        }
    }

    if (points.size >= 3 && activeTool in setOf(GeometryTool.Triangle, GeometryTool.Polygon, GeometryTool.Select, GeometryTool.Measure)) {
        val shapePoints = if (activeTool == GeometryTool.Triangle) points.take(3) else points.takeLast(points.size.coerceAtMost(8))
        val path = Path().apply {
            val first = tx(shapePoints.first())
            moveTo(first.x, first.y)
            shapePoints.drop(1).forEach {
                val p = tx(it)
                lineTo(p.x, p.y)
            }
            close()
        }
        drawPath(path, Violet.copy(alpha = .14f))
        drawPath(path, Violet.copy(alpha = .9f), style = Stroke(3f))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStoredShapes(
    points: List<Vec2>,
    shapes: List<Shape2D>,
    selectedShape: Int,
    tx: (Vec2) -> Offset,
) {
    shapes.forEachIndexed { index, shape ->
        if (!shape.visible) return@forEachIndexed
        val shapePoints = shape.pointIndices.mapNotNull { points.getOrNull(it) }
        val styled = when (shape.styleKey) {
            "cyan" -> Cyan
            "violet" -> Violet
            "green" -> Green
            else -> if (index % 2 == 0) Violet else Cyan
        }
        val accent = if (index == selectedShape) Amber else styled
        drawShape2D(shape.type, shapePoints, tx, accent, filled = true)
        if (index == selectedShape) {
            shapePoints.forEach { point ->
                drawCircle(Amber.copy(.22f), 20f, tx(point))
                drawCircle(Color.White, 7f, tx(point))
                drawCircle(Amber, 4f, tx(point))
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawConstructionPreview(
    pending: List<Vec2>,
    tool: GeometryTool,
    tx: (Vec2) -> Offset,
) {
    if (pending.isEmpty()) return
    pending.forEachIndexed { index, point -> drawRadiantPoint(tx(point), Amber, "tap ${index + 1}") }
    val type = tool.toShape2DType() ?: return
    drawShape2D(type, pending, tx, Amber, filled = false)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawShape2D(
    type: Shape2DType,
    points: List<Vec2>,
    tx: (Vec2) -> Offset,
    accent: Color,
    filled: Boolean,
) {
    if (points.isEmpty()) return
    fun offset(index: Int) = tx(points[index])
    val stroke = Stroke(if (filled) 3.5f else 2.4f, cap = StrokeCap.Round)
    when (type) {
        Shape2DType.Line, Shape2DType.Ray, Shape2DType.Segment, Shape2DType.Vector -> {
            if (points.size < 2) return
            val a = offset(0)
            val b = offset(1)
            val direction = b - a
            val length = direction.getDistance().coerceAtLeast(1f)
            val unit = Offset(direction.x / length, direction.y / length)
            val start = when (type) {
                Shape2DType.Line -> a - unit * 2000f
                else -> a
            }
            val end = when (type) {
                Shape2DType.Line, Shape2DType.Ray -> a + unit * 2000f
                else -> b
            }
            drawLine(accent, start, end, stroke.width, cap = StrokeCap.Round)
            if (type == Shape2DType.Vector) {
                val normal = Offset(-unit.y, unit.x)
                val head = 20f
                drawPath(
                    Path().apply {
                        moveTo(end.x, end.y)
                        lineTo((end - unit * head + normal * head * .55f).x, (end - unit * head + normal * head * .55f).y)
                        lineTo((end - unit * head - normal * head * .55f).x, (end - unit * head - normal * head * .55f).y)
                        close()
                    },
                    accent,
                )
            }
        }
        Shape2DType.Parallel, Shape2DType.Perpendicular -> {
            if (points.size < 3) return
            val base = points[1] - points[0]
            val direction = if (type == Shape2DType.Parallel) base else Vec2(-base.y, base.x)
            val length = direction.distanceTo(Vec2(0.0, 0.0)).coerceAtLeast(1e-9)
            val unit = direction * (1.0 / length)
            drawLine(accent, tx(points[2] - unit * 100.0), tx(points[2] + unit * 100.0), stroke.width, cap = StrokeCap.Round)
        }
        Shape2DType.AngleBisector -> {
            if (points.size < 3) return
            val u = points[0] - points[1]
            val v = points[2] - points[1]
            val um = u.distanceTo(Vec2(0.0, 0.0)).coerceAtLeast(1e-9)
            val vm = v.distanceTo(Vec2(0.0, 0.0)).coerceAtLeast(1e-9)
            val direction = u * (1.0 / um) + v * (1.0 / vm)
            drawLine(accent, tx(points[1]), tx(points[1] + direction * 100.0), stroke.width, cap = StrokeCap.Round)
        }
        Shape2DType.Circle -> {
            if (points.size < 2) return
            val center = offset(0)
            val radius = (offset(1) - center).getDistance()
            if (filled) drawCircle(Brush.radialGradient(listOf(accent.copy(.22f), Color.Transparent), center, radius), radius, center)
            drawCircle(accent, radius, center, style = stroke)
        }
        Shape2DType.CircleThreePoints -> {
            if (points.size < 3) return
            val centerWorld = Geometry2D.circumcenter(points[0], points[1], points[2]) ?: return
            val center = tx(centerWorld)
            val radius = (tx(points[0]) - center).getDistance()
            if (filled) drawCircle(Brush.radialGradient(listOf(accent.copy(.18f), Color.Transparent), center, radius), radius, center)
            drawCircle(accent, radius, center, style = stroke)
        }
        Shape2DType.Ellipse -> {
            if (points.size < 3) return
            val center = tx(points[0])
            val rx = (tx(points[1]) - center).getDistance().coerceAtLeast(1f)
            val ry = (tx(points[2]) - center).getDistance().coerceAtLeast(1f)
            drawOval(
                color = accent,
                topLeft = Offset(center.x - rx, center.y - ry),
                size = Size(rx * 2f, ry * 2f),
                style = stroke,
            )
        }
        Shape2DType.Rectangle, Shape2DType.Square -> {
            if (points.size < 2) return
            val a = offset(0)
            val b = offset(1)
            val width = b.x - a.x
            val height = if (type == Shape2DType.Square) width else b.y - a.y
            val path = Path().apply {
                moveTo(a.x, a.y)
                lineTo(a.x + width, a.y)
                lineTo(a.x + width, a.y + height)
                lineTo(a.x, a.y + height)
                close()
            }
            if (filled) drawPath(path, Brush.linearGradient(listOf(accent.copy(.28f), Cyan.copy(.10f))))
            drawPath(path, accent, style = stroke)
        }
        Shape2DType.Triangle, Shape2DType.Polygon -> {
            if (points.size < 3) return
            val path = Path().apply {
                val first = offset(0)
                moveTo(first.x, first.y)
                points.indices.drop(1).forEach {
                    val p = offset(it)
                    lineTo(p.x, p.y)
                }
                close()
            }
            if (filled) drawPath(path, Brush.linearGradient(listOf(Violet.copy(.26f), Cyan.copy(.12f), Color.Transparent)))
            drawPath(path, accent, style = stroke)
        }
        Shape2DType.RegularPolygon -> {
            if (points.size < 2) return
            val center = points[0]
            val radiusVector = points[1] - center
            val startAngle = kotlin.math.atan2(radiusVector.y, radiusVector.x)
            val radius = radiusVector.distanceTo(Vec2(0.0, 0.0))
            val vertices = (0 until 5).map { i ->
                val angle = startAngle + i * 2.0 * PI / 5.0
                Vec2(center.x + cos(angle) * radius, center.y + sin(angle) * radius)
            }
            val path = Path().apply {
                val first = tx(vertices.first())
                moveTo(first.x, first.y)
                vertices.drop(1).forEach { val p = tx(it); lineTo(p.x, p.y) }
                close()
            }
            if (filled) drawPath(path, accent.copy(.18f))
            drawPath(path, accent, style = stroke)
        }
        Shape2DType.Arc -> {
            if (points.size < 2) return
            val a = offset(0)
            val b = offset(points.lastIndex)
            val radius = (b - a).getDistance().coerceAtLeast(70f)
            drawArc(accent, startAngle = 205f, sweepAngle = 115f, useCenter = false, topLeft = Offset(a.x - radius, a.y - radius), size = Size(radius * 2f, radius * 2f), style = stroke)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawVector3D(
    vector: Vector3D,
    rx: Float,
    ry: Float,
    rz: Float,
    center: Offset,
    scale: Float,
    color: Color,
    selected: Boolean,
    perspective: Boolean,
) {
    val start = project(rotate(vector.start, rx, ry, rz), center, scale, perspective)
    val end = project(rotate(vector.end, rx, ry, rz), center, scale, perspective)
    val direction = end - start
    val length = direction.getDistance().coerceAtLeast(1f)
    val unit = Offset(direction.x / length, direction.y / length)
    val normal = Offset(-unit.y, unit.x)
    val head = 22f
    val left = end - unit * head + normal * (head * .55f)
    val right = end - unit * head - normal * (head * .55f)
    if (selected) {
        drawLine(color.copy(.18f), start, end, 18f, cap = StrokeCap.Round)
        drawCircle(color.copy(.20f), 46f, end)
    }
    drawLine(color.copy(.38f), Offset(center.x, center.y), start, 1.6f)
    drawLine(color, start, end, if (selected) 5.5f else 4f, cap = StrokeCap.Round)
    drawPath(
        Path().apply {
            moveTo(end.x, end.y)
            lineTo(left.x, left.y)
            lineTo(right.x, right.y)
            close()
        },
        Brush.linearGradient(listOf(color, Color.White.copy(.75f))),
    )
    drawCircle(color, 7f, start)
    drawCircle(Color.White, 3f, start)
    drawGraphLabel("${vector.name} |v|=${trim(vector.magnitude)}", end + Offset(16f, -54f), color)
    if (selected) {
        drawGraphLabel("<${trim(vector.components.x)}, ${trim(vector.components.y)}, ${trim(vector.components.z)}>", start + Offset(14f, 16f), color)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSolidProjection(
    solid: Solid,
    offset: Vec3,
    rx: Float,
    ry: Float,
    rz: Float,
    center: Offset,
    scale: Float,
    color: Color,
    wire: Boolean,
    selected: Boolean,
    perspective: Boolean,
    subSelection: SubObjectSelection?,
    sectionEnabled: Boolean,
    sectionY: Double,
    clipSection: Boolean,
) {
    fun p(v: Vec3): Offset {
        return project(rotate(solidLocalToWorld(solid.copy(position = offset), v), rx, ry, rz), center, scale, perspective)
    }
    val strokeWidth = if (wire) 2.2f else 4.2f
    val anchor = project(rotate(offset, rx, ry, rz), center, scale, perspective)
    if (selected) {
        drawCircle(Brush.radialGradient(listOf(color.copy(.34f), Color.Transparent), anchor, 112f), radius = 112f, center = anchor)
        drawCircle(color.copy(.85f), radius = 64f, center = anchor, style = Stroke(2.4f))
    }
    val mesh = SolidMeshFactory.create(solid)
    val vertices = mesh.vertices.map(::p)
    if (!wire && !clipSection) {
        mesh.faces.forEachIndexed { index, face ->
                if (face.size >= 3) {
                    val path = Path().apply {
                        moveTo(vertices[face.first()].x, vertices[face.first()].y)
                        face.drop(1).forEach { lineTo(vertices[it].x, vertices[it].y) }
                        close()
                    }
                    val selectedFace = subSelection?.mode == Selection3DMode.Face && subSelection.index == index
                    drawPath(path, if (selectedFace) Amber.copy(.5f) else color.copy(.12f))
                }
        }
    }
    mesh.edges.forEachIndexed { index, (a, b) ->
        var start = mesh.vertices[a]
        var end = mesh.vertices[b]
        if (!clipSection || start.y >= sectionY || end.y >= sectionY) {
            if (clipSection && start.y < sectionY) {
                val t = (sectionY - start.y) / (end.y - start.y)
                start += (end - start) * t
            }
            if (clipSection && end.y < sectionY) {
                val t = (sectionY - end.y) / (start.y - end.y)
                end += (start - end) * t
            }
            val picked = subSelection?.mode == Selection3DMode.Edge && subSelection.index == index
            drawLine(if (picked) Amber else color, p(start), p(end), if (picked) 7f else strokeWidth)
        }
    }
    if (mesh.vertices.size <= 16 || subSelection?.mode == Selection3DMode.Vertex) {
        vertices.forEachIndexed { index, vertex ->
            if (!clipSection || mesh.vertices[index].y >= sectionY) {
                val picked = subSelection?.mode == Selection3DMode.Vertex && subSelection.index == index
                drawCircle(if (picked) Amber else color, if (picked) 10f else 4.5f, vertex)
            }
        }
    }
    if (subSelection?.mode == Selection3DMode.Face) {
        mesh.faces.getOrNull(subSelection.index)?.let { face ->
            val path = Path().apply {
                if (face.isNotEmpty()) moveTo(vertices[face.first()].x, vertices[face.first()].y)
                face.drop(1).forEach { lineTo(vertices[it].x, vertices[it].y) }
                close()
            }
            drawPath(path, Amber.copy(.32f), style = Stroke(6f))
        }
    }
    if (sectionEnabled || clipSection) {
        val section = CrossSection3D.intersect(mesh, Vec3(0.0, 1.0, 0.0), sectionY)
        if (section.size >= 2) {
            val sectionPath = Path().apply {
                val first = p(section.first())
                moveTo(first.x, first.y)
                section.drop(1).map(::p).forEach { lineTo(it.x, it.y) }
                if (section.size >= 3) close()
            }
            if (section.size >= 3) drawPath(sectionPath, Amber.copy(.28f))
            drawPath(sectionPath, Amber, style = Stroke(5f))
            drawGraphLabel("section y=${trim(sectionY)}", p(section.first()) + Offset(12f, -18f), Amber)
        }
    }
    if (selected) {
        drawGraphLabel("${solid.type.name} selected", anchor + Offset(20f, -72f), color)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTransformGizmo(anchor: Offset, mode: Transform3DMode) {
    when (mode) {
        Transform3DMode.Move -> {
            drawLine(Color(0xFFFF5B68), anchor, anchor + Offset(72f, 0f), 5f, cap = StrokeCap.Round)
            drawLine(Green, anchor, anchor + Offset(0f, -72f), 5f, cap = StrokeCap.Round)
            drawLine(Cyan, anchor, anchor + Offset(-46f, 48f), 5f, cap = StrokeCap.Round)
            drawCircle(Color.White, 7f, anchor)
        }
        Transform3DMode.Rotate -> {
            drawCircle(Cyan.copy(.9f), 76f, anchor, style = Stroke(5f))
            drawArc(Violet, 205f, 115f, false, anchor - Offset(62f, 62f), Size(124f, 124f), style = Stroke(7f, cap = StrokeCap.Round))
        }
        Transform3DMode.Scale -> {
            drawRect(Cyan.copy(.9f), topLeft = anchor - Offset(55f, 55f), size = Size(110f, 110f), style = Stroke(4f))
            listOf(Offset(-55f, -55f), Offset(55f, -55f), Offset(55f, 55f), Offset(-55f, 55f)).forEach {
                drawRect(Color.White, topLeft = anchor + it - Offset(7f, 7f), size = Size(14f, 14f))
            }
        }
    }
}

private fun cubeVertices(w: Double, h: Double, d: Double): List<Vec3> {
    val x = w / 2
    val y = h / 2
    val z = d / 2
    return listOf(
        Vec3(-x, -y, -z), Vec3(x, -y, -z), Vec3(x, y, -z), Vec3(-x, y, -z),
        Vec3(-x, -y, z), Vec3(x, -y, z), Vec3(x, y, z), Vec3(-x, y, z),
    )
}

private fun rotate(p: Vec3, rx: Float, ry: Float, rz: Float): Vec3 {
    val ax = Math.toRadians(rx.toDouble())
    val ay = Math.toRadians(ry.toDouble())
    val az = Math.toRadians(rz.toDouble())
    var y = p.y * cos(ax) - p.z * sin(ax)
    var z = p.y * sin(ax) + p.z * cos(ax)
    var x = p.x
    val x2 = x * cos(ay) + z * sin(ay)
    z = -x * sin(ay) + z * cos(ay)
    x = x2
    val x3 = x * cos(az) - y * sin(az)
    y = x * sin(az) + y * cos(az)
    return Vec3(x3, y, z)
}

private fun solidLocalToWorld(solid: Solid, vertex: Vec3): Vec3 = rotate(
    vertex,
    solid.rotation.x.toFloat(),
    solid.rotation.y.toFloat(),
    solid.rotation.z.toFloat(),
) + solid.position

private fun Float.wrapDegrees(): Float {
    var value = this
    while (value > 180f) value -= 360f
    while (value < -180f) value += 360f
    return value
}

private fun project(p: Vec3, center: Offset, scale: Float, perspective: Boolean = true): Offset {
    val depthScale = if (perspective) 1.0 / (1.0 + (p.z + 5.0) * 0.08) else 1.0
    val depthLift = if (perspective) p.z * 18 else p.z * 10
    return Offset((center.x + p.x * scale * depthScale).toFloat(), (center.y - p.y * scale * depthScale + depthLift).toFloat())
}

private fun snapAngle(value: Float): Float = (kotlin.math.round(value / 1f) * 1f).coerceIn(-180f, 180f)

private fun quadrantLabel(angle: Double): String {
    val normalized = ((angle % 360.0) + 360.0) % 360.0
    return when {
        normalized == 0.0 || normalized == 90.0 || normalized == 180.0 || normalized == 270.0 -> "Axis"
        normalized < 90.0 -> "I"
        normalized < 180.0 -> "II"
        normalized < 270.0 -> "III"
        else -> "IV"
    }
}

private fun radianLabel(angle: Double): String = when (kotlin.math.round(angle).toInt()) {
    0 -> "0"
    30 -> "pi/6"
    45 -> "pi/4"
    60 -> "pi/3"
    90 -> "pi/2"
    120 -> "2pi/3"
    135 -> "3pi/4"
    150 -> "5pi/6"
    180 -> "pi"
    -30 -> "-pi/6"
    -45 -> "-pi/4"
    -60 -> "-pi/3"
    -90 -> "-pi/2"
    -120 -> "-2pi/3"
    -135 -> "-3pi/4"
    -150 -> "-5pi/6"
    -180 -> "-pi"
    else -> "${trim(Math.toRadians(angle))}"
}

@Composable
private fun MiniDock(modifier: Modifier = Modifier, items: List<String>, onClick: (String) -> Unit) {
    Column(
        modifier
            .padding(top = 58.dp, end = 4.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceA)
            .border(1.dp, Color(0x5548BFFF), RoundedCornerShape(18.dp))
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items.forEach {
            Text(
                text = it,
                color = Ink,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onClick(it) }
                    .focusable()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun FloatingPanelLaunchers(
    modifier: Modifier = Modifier,
    leftLabel: String,
    rightLabel: String,
    bottomLabel: String,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    onBottom: () -> Unit,
) {
    Column(
        modifier.padding(start = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        GlowButton(leftLabel, onClick = onLeft)
        GlowButton(rightLabel, onClick = onRight)
        GlowButton(bottomLabel, onClick = onBottom)
    }
}

@Composable
private fun EquationChips(modifier: Modifier = Modifier, labels: List<String>) {
    FlowRow(
        modifier.padding(top = 88.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        labels.forEachIndexed { index, label ->
            Text(
                text = label,
                color = Ink,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (index % 2 == 0) Color(0x6620D9FF) else Color(0x66985DFF))
                    .border(1.dp, if (index % 2 == 0) Cyan else Violet, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun GlassPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .padding(8.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(SurfaceA, SurfaceB)))
            .border(1.dp, Color(0x6645CFFF), RoundedCornerShape(18.dp))
            .heightIn(max = 560.dp)
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

@Composable
private fun PanelHeader(title: String, onClose: () -> Unit, accent: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = accent, fontWeight = FontWeight.Bold)
        Text(
            text = "Close",
            color = Muted,
            fontSize = 12.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onClose)
                .padding(horizontal = 8.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun GlowButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF101824), contentColor = Ink),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.height(44.dp),
    ) {
        Text(label, fontSize = 13.sp)
    }
}

@Composable
private fun Insight(label: String, value: String, color: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(9.dp).clip(RoundedCornerShape(9.dp)).background(color))
            Spacer(Modifier.width(8.dp))
            Text(label, color = Muted, fontSize = 13.sp)
        }
        Text(value, color = Ink, fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Muted)
        Switch(checked = checked, onCheckedChange = {})
    }
}

@Composable
private fun TogglePill(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Text(
        text = if (checked) "On: $label" else "Off: $label",
        color = if (checked) Ink else Muted,
        fontSize = 13.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (checked) Color(0x6630D9FF) else Color(0x33101824))
            .border(1.dp, if (checked) Cyan else Color(0x5548BFFF), RoundedCornerShape(14.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 12.dp, vertical = 10.dp),
    )
}

@Composable
private fun AxisSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValue: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("$label ${trim(value.toDouble())}", color = Muted, modifier = Modifier.width(112.dp))
        Slider(value = value, onValueChange = onValue, valueRange = range, modifier = Modifier.weight(1f))
    }
}
