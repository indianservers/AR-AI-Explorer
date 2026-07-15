package com.indianservers.aiexplorer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.indianservers.aiexplorer.core.ExpressionEngine
import com.indianservers.aiexplorer.core.Geometry2D
import com.indianservers.aiexplorer.core.Geometry3D
import com.indianservers.aiexplorer.core.Graph3D
import com.indianservers.aiexplorer.core.GraphAnalysis
import com.indianservers.aiexplorer.core.Solid
import com.indianservers.aiexplorer.core.SolidType
import com.indianservers.aiexplorer.core.Vec2
import com.indianservers.aiexplorer.core.Vec3
import com.indianservers.aiexplorer.core.Vector3D
import com.indianservers.aiexplorer.core.stripEquation
import com.indianservers.aiexplorer.core.trim
import com.indianservers.aiexplorer.workspace.AddVector3DCommand
import com.indianservers.aiexplorer.workspace.AddConstructionCommand
import com.indianservers.aiexplorer.workspace.AddPointCommand
import com.indianservers.aiexplorer.workspace.AddSolidCommand
import com.indianservers.aiexplorer.workspace.CommandHistory
import com.indianservers.aiexplorer.workspace.EditExpressionCommand
import com.indianservers.aiexplorer.workspace.MathModule
import com.indianservers.aiexplorer.workspace.MovePointCommand
import com.indianservers.aiexplorer.workspace.MoveSolidCommand
import com.indianservers.aiexplorer.workspace.MoveVector3DCommand
import com.indianservers.aiexplorer.workspace.Shape2D
import com.indianservers.aiexplorer.workspace.Shape2DType
import com.indianservers.aiexplorer.workspace.TransformSolidCommand
import com.indianservers.aiexplorer.workspace.TransformVector3DCommand
import com.indianservers.aiexplorer.workspace.WorkspaceJson
import com.indianservers.aiexplorer.workspace.WorkspaceState
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

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
enum class GeometryTool { Select, Point, Line, Segment, Ray, Triangle, Polygon, Rectangle, Square, Circle, Arc, Measure }
enum class GraphTool { Plot, Trace, Tangent, Derivative, Integral, Intersections }
enum class SurfaceTool { Surface, Wireframe, Contours, Slice, Gradient, BoundingBox, Trace }

data class LearningActivity(
    val id: String,
    val title: String,
    val module: MathModule,
    val objective: String,
    val target: String,
    val hint: String,
    val proof: String,
)

data class LearningValidation(val passed: Boolean, val message: String)

data class AppSettings(
    val haptics: Boolean = true,
    val snap: Boolean = true,
    val highContrast: Boolean = false,
    val reducedMotion: Boolean = false,
    val decimalPrecision: Int = 2,
)

data class SavedWorkspace(
    val id: String,
    val name: String,
    val module: MathModule,
    val snapshot: WorkspaceState,
    val json: String,
    val updatedAt: Long,
)

private val LearningActivities = listOf(
    LearningActivity(
        id = "midpoint-distance",
        title = "Midpoint And Distance",
        module = MathModule.Geometry2D,
        objective = "Drag two points and explain midpoint, slope, and distance from the same construction.",
        target = "Create or move A and B, then inspect M, slope, and distance.",
        hint = "The midpoint is the average of x-values and y-values.",
        proof = "M = ((x1 + x2) / 2, (y1 + y2) / 2); distance follows Pythagoras on dx and dy.",
    ),
    LearningActivity(
        id = "triangle-angle-sum",
        title = "Triangle Angle Sum",
        module = MathModule.Geometry2D,
        objective = "Construct a triangle and verify its side lengths, altitude, and angle relationships.",
        target = "Use Triangle: 3 taps. Drag vertices and watch measurements update.",
        hint = "A triangle can be compared with a parallel-line construction to see why angles sum to 180 degrees.",
        proof = "Translate the base line through the top vertex; alternate interior angles complete a straight angle.",
    ),
    LearningActivity(
        id = "quadratic-roots",
        title = "Quadratic Roots And Vertex",
        module = MathModule.Graph2D,
        objective = "Use sliders to see how a, b, and c move roots, vertex, and intersections.",
        target = "Set f(x)=x^2-4x+3 and trace roots at x=1 and x=3.",
        hint = "The vertex x-coordinate is -b / 2a.",
        proof = "Completing the square transforms ax^2+bx+c into vertex form and reveals the minimum or maximum.",
    ),
    LearningActivity(
        id = "unit-circle",
        title = "Unit Circle Trigonometry",
        module = MathModule.Trigonometry,
        objective = "Connect angle, radians, sin, cos, tan, projections, and the sine wave.",
        target = "Snap to 30, 45, 60, and 90 degrees and compare values.",
        hint = "On the unit circle, the point is (cos theta, sin theta).",
        proof = "Since radius is 1, x^2 + y^2 = 1 becomes cos^2(theta) + sin^2(theta) = 1.",
    ),
    LearningActivity(
        id = "solid-volume",
        title = "3D Volume Lab",
        module = MathModule.Geometry3D,
        objective = "Place solids, resize them, and compare volume and surface area.",
        target = "Add a cube, cylinder, cone, and sphere. Drag and resize each selected solid.",
        hint = "Volume changes cubically when every dimension scales together.",
        proof = "A prism volume is base area times height; cones and pyramids are one third of the related prism.",
    ),
    LearningActivity(
        id = "paraboloid-slices",
        title = "Paraboloid Slices",
        module = MathModule.Graph3D,
        objective = "Explore z = x^2 + y^2 with contours, trace point, gradient, and z-slices.",
        target = "Move the slice slider and trace point to see circular level curves.",
        hint = "For z = x^2 + y^2, each horizontal slice is x^2 + y^2 = constant.",
        proof = "A fixed z level gives a circle with radius sqrt(z), proving rotational symmetry.",
    ),
)

private fun GeometryTool.requiredTapCount(): Int = when (this) {
    GeometryTool.Point -> 1
    GeometryTool.Line, GeometryTool.Segment, GeometryTool.Ray, GeometryTool.Rectangle, GeometryTool.Square, GeometryTool.Circle -> 2
    GeometryTool.Triangle, GeometryTool.Arc -> 3
    GeometryTool.Polygon -> 4
    GeometryTool.Select, GeometryTool.Measure -> 0
}

private fun GeometryTool.toShape2DType(): Shape2DType? = when (this) {
    GeometryTool.Line -> Shape2DType.Line
    GeometryTool.Segment -> Shape2DType.Segment
    GeometryTool.Ray -> Shape2DType.Ray
    GeometryTool.Triangle -> Shape2DType.Triangle
    GeometryTool.Polygon -> Shape2DType.Polygon
    GeometryTool.Rectangle -> Shape2DType.Rectangle
    GeometryTool.Square -> Shape2DType.Square
    GeometryTool.Circle -> Shape2DType.Circle
    GeometryTool.Arc -> Shape2DType.Arc
    else -> null
}

private fun defaultSolid(type: SolidType): Solid = when (type) {
    SolidType.Cube -> Solid(type, width = 2.0)
    SolidType.Cuboid -> Solid(type, width = 2.4, height = 1.6, depth = 1.4, radius = .8)
    SolidType.Sphere -> Solid(type, width = 2.0, height = 2.0, depth = 2.0, radius = 1.0)
    SolidType.Cylinder -> Solid(type, width = 2.0, height = 2.4, depth = 2.0, radius = .9)
    SolidType.Cone -> Solid(type, width = 2.0, height = 2.5, depth = 2.0, radius = .9)
    SolidType.Pyramid -> Solid(type, width = 2.2, height = 2.4, depth = 2.2, radius = .9)
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
    var state by mutableStateOf(WorkspaceState())
        private set
    var selectedPoint by mutableIntStateOf(1)
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
    var geometryTool by mutableStateOf(GeometryTool.Select)
        private set
    var selectedSolid by mutableIntStateOf(0)
        private set
    var selectedVector3D by mutableIntStateOf(0)
        private set
    var pendingConstruction by mutableStateOf<List<Vec2>>(emptyList())
        private set
    var activeActivityId by mutableStateOf(LearningActivities.first().id)
        private set
    var completedActivities by mutableStateOf<Set<String>>(emptySet())
        private set
    var savedWorkspaces by mutableStateOf<List<SavedWorkspace>>(emptyList())
        private set
    var settings by mutableStateOf(AppSettings())
        private set
    var lastValidation by mutableStateOf(LearningValidation(false, "Start an activity and validate your construction."))
        private set

    val activeActivity: LearningActivity
        get() = LearningActivities.firstOrNull { it.id == activeActivityId } ?: LearningActivities.first()

    fun open(module: MathModule) {
        state = state.copy(module = module)
        hidePanels()
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
        status = "Activity: ${activity.title}"
    }

    fun validateActiveActivity(): LearningValidation {
        val result = validateActivity(activeActivity)
        lastValidation = result
        status = if (result.passed) "Validation passed" else result.message
        return result
    }

    fun completeActiveActivity() {
        val result = validateActiveActivity()
        if (result.passed) {
            completedActivities = completedActivities + activeActivityId
            status = "Completed ${activeActivity.title}"
        }
    }

    private fun validateActivity(activity: LearningActivity): LearningValidation = when (activity.id) {
        "midpoint-distance" -> {
            val ok = state.points.size >= 2 && state.points[0].distanceTo(state.points[1]) > .1
            LearningValidation(ok, if (ok) "Two movable points define midpoint, slope, and distance." else "Place or separate two points first.")
        }
        "triangle-angle-sum" -> {
            val triangle = state.shapes.any { it.type == Shape2DType.Triangle } || state.points.size >= 3
            val ok = triangle && state.points.size >= 3
            LearningValidation(ok, if (ok) "Triangle construction is ready for angle-sum proof." else "Use Triangle: tap three vertices.")
        }
        "quadratic-roots" -> {
            val expression = state.functions.firstOrNull()?.expression?.replace(" ", "").orEmpty()
            val ok = "x^2" in expression && "-4" in expression && "+3" in expression
            LearningValidation(ok, if (ok) "Quadratic target detected. Trace roots and vertex." else "Set f(x) close to x^2 - 4x + 3.")
        }
        "unit-circle" -> LearningValidation(state.module == MathModule.Trigonometry, "Unit circle workspace is open; compare sin, cos, tan.")
        "solid-volume" -> {
            val ok = state.solids.size >= 4 || state.solids.any { it.type == SolidType.Sphere }
            LearningValidation(ok, if (ok) "Multiple solids are ready for volume comparison." else "Add one more solid, ideally a sphere.")
        }
        "paraboloid-slices" -> {
            val normalized = stripEquation(state.surfaceExpression).replace(" ", "")
            val ok = normalized in setOf("x^2+y^2", "x*x+y*y")
            LearningValidation(ok, if (ok) "Paraboloid target detected. Use contours, trace, and slices." else "Choose Paraboloid or enter x^2 + y^2.")
        }
        else -> LearningValidation(false, "No validator for this activity yet.")
    }

    fun selectGeometryTool(tool: GeometryTool) {
        geometryTool = tool
        pendingConstruction = emptyList()
        status = "${tool.name.lowercase().replaceFirstChar { it.uppercase() }} tool selected"
    }

    fun addPoint(point: Vec2) {
        state = history.execute(state, AddPointCommand(point))
        selectedPoint = state.points.lastIndex
        status = "Added point ${selectedPoint + 1}"
    }

    fun handleGeometryTap(point: Vec2) {
        when (geometryTool) {
            GeometryTool.Select, GeometryTool.Measure -> return
            GeometryTool.Point -> {
                state = history.execute(state, AddConstructionCommand(listOf(point), null))
                selectedPoint = state.points.lastIndex
                status = "Point placed"
            }
            else -> {
                val next = pendingConstruction + point
                val required = geometryTool.requiredTapCount()
                if (next.size >= required) {
                    val shapeType = geometryTool.toShape2DType()
                    state = history.execute(state, AddConstructionCommand(next.take(required), shapeType))
                    selectedPoint = state.points.lastIndex
                    pendingConstruction = emptyList()
                    status = "${geometryTool.name.lowercase().replaceFirstChar { it.uppercase() }} created"
                } else {
                    pendingConstruction = next
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

    fun editExpression(index: Int, expression: String) {
        val from = state.functions[index].expression
        state = history.execute(state, EditExpressionCommand(index, from, expression))
        status = "Expression updated"
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
        status = "Learning package generated"
        return learningPackageJson()
    }

    fun exportLearningPackagePreview(): String = learningPackageJson().lineSequence().take(8).joinToString("\n")

    private fun learningPackageJson(): String {
        return buildString {
            appendLine("{")
            appendLine("  \"schemaVersion\": 2,")
            appendLine("  \"workspace\": ${WorkspaceJson.export(state).prependIndent("  ").trim()},")
            appendLine("  \"learning\": {")
            appendLine("    \"activeActivityId\": \"$activeActivityId\",")
            appendLine("    \"completedActivityIds\": [${completedActivities.joinToString { "\"$it\"" }}],")
            appendLine("    \"lastValidation\": {\"passed\": ${lastValidation.passed}, \"message\": \"${lastValidation.message}\"}")
            appendLine("  },")
            appendLine("  \"settings\": {")
            appendLine("    \"haptics\": ${settings.haptics},")
            appendLine("    \"snap\": ${settings.snap},")
            appendLine("    \"highContrast\": ${settings.highContrast},")
            appendLine("    \"reducedMotion\": ${settings.reducedMotion},")
            appendLine("    \"decimalPrecision\": ${settings.decimalPrecision}")
            appendLine("  }")
            appendLine("}")
        }
    }
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
            Box(
                Modifier
                    .fillMaxSize()
                    .background(radialBackdrop())
                    .padding(8.dp),
            ) {
                when (vm.state.module) {
                    MathModule.Geometry2D -> Geometry2DScreen(vm)
                    MathModule.Geometry3D -> Geometry3DScreen(vm)
                    MathModule.Graph2D -> Graph2DScreen(vm)
                    MathModule.Graph3D -> Graph3DScreen(vm)
                    MathModule.Trigonometry -> TrigonometryScreen(vm)
                }
                if (vm.showChrome) TopShell(vm, Modifier.align(Alignment.TopCenter))
                if (vm.showLearningPanel) LearningCoachPanel(vm, Modifier.align(Alignment.CenterEnd))
                BottomModeSelector(vm.state.module, vm::open, Modifier.align(Alignment.BottomCenter))
                MiniDock(
                    modifier = Modifier.align(Alignment.TopEnd),
                    items = listOf("Focus", "Learn", "Tools", "Info", "Panel", "Export", "Close"),
                    onClick = {
                        when (it) {
                            "Focus" -> vm.togglePanel(PanelSlot.Chrome)
                            "Learn" -> vm.toggleLearningPanel()
                            "Tools" -> vm.togglePanel(PanelSlot.Left)
                            "Info" -> vm.togglePanel(PanelSlot.Right)
                            "Panel" -> vm.togglePanel(PanelSlot.Bottom)
                            "Export" -> vm.exportJson()
                            "Close" -> vm.hidePanels()
                        }
                    },
                )
            }
        }
    }
}

private fun radialBackdrop() = Brush.radialGradient(
    colors = listOf(Color(0x3320D9FF), Background, Background),
    radius = 1100f,
    center = Offset(420f, 220f),
)

@Composable
private fun TopShell(vm: ExplorerViewModel, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlowButton("Menu", onClick = { vm.togglePanel(PanelSlot.Left) })
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("AI Explorer", color = Ink, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
            Text("Learn · Visualize · Explore · ${vm.state.module.label}", color = Muted, fontSize = 12.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlowButton("Undo", enabled = true, onClick = vm::undo)
            GlowButton("Redo", enabled = true, onClick = vm::redo)
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
        Text("Progress $progress", color = Cyan, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text(activity.title, color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(activity.objective, color = Muted, fontSize = 13.sp)
        Insight("Target", activity.target, Cyan)
        Insight("Hint", activity.hint, Amber)
        Insight("Proof", activity.proof, Violet)
        Insight("Validation", vm.lastValidation.message, if (vm.lastValidation.passed) Green else Amber)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlowButton("Go", onClick = { vm.startActivity(activity) })
            GlowButton("Check", onClick = { vm.validateActiveActivity() })
            GlowButton("Done", onClick = vm::completeActiveActivity)
            GlowButton("Save", onClick = vm::saveWorkspace)
            GlowButton("Package", onClick = { vm.exportLearningPackage() })
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
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Row(
            Modifier
                .clip(RoundedCornerShape(30.dp))
                .background(SurfaceA)
                .border(1.dp, Color(0x5548BFFF), RoundedCornerShape(30.dp))
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            MathModule.entries.forEach { module ->
                val selected = module == active
                Text(
                    text = module.label,
                    color = if (selected) Color.White else Muted,
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (selected) Brush.horizontalGradient(listOf(Violet, Cyan)) else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)))
                        .clickable { onSelect(module) }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .semantics { contentDescription = "Open ${module.label} workspace" },
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun Geometry2DScreen(vm: ExplorerViewModel) {
    val a = vm.state.points[0]
    val b = vm.state.points[1]
    val m = Geometry2D.segment(a, b)
    val third = Vec2(4.0, -1.5)
    Box(Modifier.fillMaxSize()) {
        CoordinateCanvas(
            modifier = Modifier.fillMaxSize().semantics { contentDescription = "Interactive coordinate geometry canvas" },
            onPointDrag = vm::movePoint,
            onCanvasTap = vm::handleGeometryTap,
            points = vm.state.points,
        ) { tx ->
            val pa = tx(a)
            val pb = tx(b)
            val pm = tx(m.midpoint)
            drawLine(Violet, pa, pb, 5f, cap = StrokeCap.Round)
            drawStoredShapes(vm.state.points, vm.state.shapes, tx)
            drawConstructionPreview(vm.pendingConstruction, vm.geometryTool, tx)
            vm.state.points.drop(2).forEachIndexed { index, point -> drawRadiantPoint(tx(point), Green, "P${index + 3}") }
            drawLine(Cyan, tx(Vec2(a.x, a.y)), tx(Vec2(b.x, a.y)), 2f, pathEffect = null)
            drawLine(Cyan.copy(alpha = .8f), tx(Vec2(b.x, a.y)), pb, 2f)
            drawRadiantPoint(pa, Cyan, "A (${trim(a.x)}, ${trim(a.y)})")
            drawRadiantPoint(pb, Violet, "B (${trim(b.x)}, ${trim(b.y)})")
            drawRadiantPoint(pm, Violet, "M (${trim(m.midpoint.x)}, ${trim(m.midpoint.y)})")
            drawCircle(Cyan.copy(alpha = .8f), radius = a.distanceTo(third).toFloat() * 42f, center = tx(Vec2(1.5, 1.0)), style = Stroke(2f))
        }
        FloatingPanelLaunchers(
            modifier = Modifier.align(Alignment.CenterStart),
            leftLabel = "Tools",
            rightLabel = "Measure",
            bottomLabel = "Construct",
            onLeft = { vm.togglePanel(PanelSlot.Left) },
            onRight = { vm.togglePanel(PanelSlot.Right) },
            onBottom = { vm.togglePanel(PanelSlot.Bottom) },
        )
        if (vm.showLeftPanel) GlassPanel(Modifier.align(Alignment.TopStart).width(190.dp)) {
            PanelHeader("Point Tools", vm::hidePanels, Cyan)
            Text("Tap flow: ${vm.geometryTool.name} needs ${vm.geometryTool.requiredTapCount()} tap(s)", color = Muted, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlowButton("A", onClick = { vm.movePoint(0, a + Vec2(.5, .0)) })
                GlowButton("B", onClick = { vm.movePoint(1, b + Vec2(.0, -.5)) })
            }
            listOf(
                GeometryTool.Select,
                GeometryTool.Point,
                GeometryTool.Line,
                GeometryTool.Segment,
                GeometryTool.Ray,
                GeometryTool.Triangle,
                GeometryTool.Circle,
                GeometryTool.Arc,
            ).forEach {
                GlowButton(if (vm.geometryTool == it) "• ${it.name}" else it.name, onClick = { vm.selectGeometryTool(it) })
            }
            ToggleRow("Snap", true)
            ToggleRow("Grid", true)
            ToggleRow("Labels", true)
        }
        if (vm.showRightPanel) GlassPanel(Modifier.align(Alignment.TopEnd).width(220.dp)) {
            PanelHeader("Geometry Insights", vm::hidePanels, Violet)
            Insight("Active tool", vm.geometryTool.name, Green)
            Insight("Pending taps", "${vm.pendingConstruction.size}/${vm.geometryTool.requiredTapCount()}", Cyan)
            Insight("Objects", "${vm.state.shapes.size}", Violet)
            Insight("Midpoint", "(${trim(m.midpoint.x)}, ${trim(m.midpoint.y)})", Cyan)
            Insight("Slope", m.slope?.let(::trim) ?: "undefined", Violet)
            Insight("Distance AB", "${m.exactDistance} ≈ ${trim(m.distance)}", Cyan)
            Insight("Triangle area", "${trim(Geometry2D.polygonArea(listOf(a, b, third)))} u²", Violet)
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
        }
    }
}

@Composable
private fun Graph2DScreen(vm: ExplorerViewModel) {
    val graph = remember { GraphAnalysis() }
    val engine = remember { ExpressionEngine() }
    var traceX by remember { mutableFloatStateOf(2f) }
    var graphTool by remember { mutableStateOf(GraphTool.Trace) }
    var qa by remember { mutableFloatStateOf(1f) }
    var qb by remember { mutableFloatStateOf(-4f) }
    var qc by remember { mutableFloatStateOf(3f) }
    var lm by remember { mutableFloatStateOf(1f) }
    var lb by remember { mutableFloatStateOf(-1f) }
    val liveFunctions = remember(qa, qb, qc, lm, lb) {
        listOf(
            vm.state.functions[0].copy(expression = "${trim(qa.toDouble())}*x^2 + ${trim(qb.toDouble())}*x + ${trim(qc.toDouble())}"),
            vm.state.functions[1].copy(expression = "${trim(lm.toDouble())}*x + ${trim(lb.toDouble())}"),
        )
    }
    val fInsight = graph.quadratic(qa.toDouble(), qb.toDouble(), qc.toDouble())
    val gInsight = graph.linear(lm.toDouble(), lb.toDouble())
    val intersections = remember(liveFunctions) {
        runCatching {
            graph.intersections(
                engine.compile(liveFunctions[0].expression),
                engine.compile(liveFunctions[1].expression),
                -6.0,
                6.0,
            )
        }.getOrDefault(emptyList())
    }
    Box(Modifier.fillMaxSize()) {
        GraphCanvas(
            modifier = Modifier.fillMaxSize(),
            functions = liveFunctions,
            traceX = traceX.toDouble(),
            graphTool = graphTool,
            onTraceChange = { traceX = it.toFloat().coerceIn(-6f, 6f) },
        )
        EquationChips(Modifier.align(Alignment.TopCenter), liveFunctions.map { "${it.name} = ${it.expression}" })
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
        if (vm.showLeftPanel) GlassPanel(Modifier.align(Alignment.TopStart).width(260.dp)) {
            PanelHeader("Equations", vm::hidePanels, Cyan)
            liveFunctions.forEachIndexed { index, fn ->
                OutlinedTextField(
                    value = fn.expression,
                    onValueChange = { vm.editExpression(index, it) },
                    label = { Text(fn.name) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (vm.showRightPanel) GlassPanel(Modifier.align(Alignment.TopEnd).width(240.dp)) {
            PanelHeader("Graph Insights", vm::hidePanels, Violet)
            Insight("Tool", graphTool.name, Green)
            Insight("Vertex", "(${trim(fInsight.vertex.x)}, ${trim(fInsight.vertex.y)})", Cyan)
            Insight("Axis", "x = ${trim(fInsight.axis)}", Violet)
            Insight("Roots", fInsight.roots.joinToString { "x = ${trim(it)}" }, Cyan)
            Insight("f y-intercept", "(0, ${trim(fInsight.yIntercept)})", Cyan)
            Insight("g slope", trim(gInsight.slope), Violet)
            Insight("Intersections", intersections.joinToString { "(${trim(it.x)}, ${trim(it.y)})" }, Violet)
        }
        if (vm.showBottomPanel) GlassPanel(Modifier.align(Alignment.BottomStart).fillMaxWidth()) {
            PanelHeader("Graph Controls", vm::hidePanels, Ink)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Trace x = ${trim(traceX.toDouble())}", color = Muted, modifier = Modifier.width(120.dp))
                Slider(value = traceX, onValueChange = { traceX = it }, valueRange = -6f..6f, modifier = Modifier.weight(1f))
            }
            AxisSlider("a", qa, -5f..5f) { qa = it.coerceAtLeast(.1f) }
            AxisSlider("b", qb, -10f..10f) { qb = it }
            AxisSlider("c", qc, -10f..10f) { qc = it }
            AxisSlider("m", lm, -5f..5f) { lm = it }
            AxisSlider("line b", lb, -10f..10f) { lb = it }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GraphTool.entries.forEach { tool ->
                    GlowButton(if (graphTool == tool) "• ${tool.name}" else tool.name, onClick = { graphTool = tool })
                }
                GlowButton("Export JSON", onClick = { vm.exportJson() })
            }
        }
    }
}

@Composable
private fun Geometry3DScreen(vm: ExplorerViewModel) {
    var rotateX by remember { mutableFloatStateOf(25f) }
    var rotateY by remember { mutableFloatStateOf(-35f) }
    var rotateZ by remember { mutableFloatStateOf(15f) }
    var zoom by remember { mutableFloatStateOf(1.0f) }
    var wire by remember { mutableStateOf(true) }
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
            wire = wire,
            onSelect = vm::selectSolid,
            onMove = { index, position -> vm.moveSolid(index, position) },
            onSelectVector = vm::selectVector3D,
            onMoveVector = { index, delta -> vm.moveVector3D(index, delta) },
        )
        SolidAddStrip(Modifier.align(Alignment.TopCenter), vm::addSolid, vm::addVector3D)
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
            ToggleRow("Perspective", true)
            ToggleRow("Wireframe", wire)
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
private fun Graph3DScreen(vm: ExplorerViewModel) {
    val graph3D = remember { Graph3D() }
    var density by remember { mutableFloatStateOf(26f) }
    var rotation by remember { mutableFloatStateOf(35f) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var tilt by remember { mutableFloatStateOf(55f) }
    var sliceZ by remember { mutableFloatStateOf(2f) }
    var traceX by remember { mutableFloatStateOf(1f) }
    var traceY by remember { mutableFloatStateOf(1f) }
    var showWireframe by remember { mutableStateOf(true) }
    var showContours by remember { mutableStateOf(true) }
    var showSlice by remember { mutableStateOf(true) }
    var showGradient by remember { mutableStateOf(true) }
    var showBox by remember { mutableStateOf(true) }
    var activeTool by remember { mutableStateOf(SurfaceTool.Trace) }
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
            zoom = zoom,
            sliceZ = sliceZ.toDouble(),
            trace = Vec2(traceX.toDouble(), traceY.toDouble()),
            showWireframe = showWireframe,
            showContours = showContours,
            showSlice = showSlice,
            showGradient = showGradient,
            showBox = showBox,
            activeTool = activeTool,
            onRotate = { delta -> rotation = (rotation + delta).coerceIn(-180f, 180f) },
            onTilt = { delta -> tilt = (tilt + delta).coerceIn(25f, 78f) },
            onTrace = { point ->
                traceX = point.x.toFloat().coerceIn(-3f, 3f)
                traceY = point.y.toFloat().coerceIn(-3f, 3f)
            },
        )
        SurfaceExampleChips(
            modifier = Modifier.align(Alignment.TopCenter),
            onSelect = vm::setSurfaceExpression,
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
            AxisSlider("Tilt", tilt, 25f..78f) { tilt = it }
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
            AxisSlider("Tilt", tilt, 25f..78f) { tilt = it }
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
                GlowButton("Fit", onClick = { zoom = 1f; rotation = 35f; tilt = 55f })
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
private fun CoordinateCanvas(
    modifier: Modifier,
    points: List<Vec2>,
    onPointDrag: (Int, Vec2) -> Unit,
    onCanvasTap: (Vec2) -> Unit,
    content: androidx.compose.ui.graphics.drawscope.DrawScope.(toScreen: (Vec2) -> Offset) -> Unit,
) {
    var dragIndex by remember { mutableStateOf<Int?>(null) }
    Canvas(
        modifier
            .pointerInput(points) {
                detectTapGestures { tap ->
                    val scale = size.width / 14f
                    val origin = Offset(size.width / 2f, size.height / 2f)
                    onCanvasTap(Vec2(((tap.x - origin.x) / scale).toDouble(), ((origin.y - tap.y) / scale).toDouble()))
                }
            }
            .pointerInput(points) {
                detectDragGestures(
                    onDragStart = { start ->
                        val scale = size.width / 14f
                        val origin = Offset(size.width / 2f, size.height / 2f)
                        dragIndex = points.indexOfFirst {
                            val p = Offset(origin.x + it.x.toFloat() * scale, origin.y - it.y.toFloat() * scale)
                            (p - start).getDistance() < 36f
                        }.takeIf { it >= 0 }
                    },
                    onDragEnd = { dragIndex = null },
                ) { change, _ ->
                    val scale = size.width / 14f
                    val origin = Offset(size.width / 2f, size.height / 2f)
                    dragIndex?.let {
                        onPointDrag(it, Vec2(((change.position.x - origin.x) / scale).toDouble(), ((origin.y - change.position.y) / scale).toDouble()))
                    }
                }
            },
    ) {
        val scale = size.width / 14f
        val origin = Offset(size.width / 2f, size.height / 2f)
        val tx: (Vec2) -> Offset = { Offset(origin.x + it.x.toFloat() * scale, origin.y - it.y.toFloat() * scale) }
        drawGrid(origin, scale)
        content(tx)
    }
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
private fun GraphCanvas(
    modifier: Modifier,
    functions: List<com.indianservers.aiexplorer.core.FunctionDefinition>,
    traceX: Double,
    graphTool: GraphTool,
    onTraceChange: (Double) -> Unit,
) {
    val graph = remember { GraphAnalysis() }
    val engine = remember { ExpressionEngine() }
    Canvas(
        modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val scale = size.width / 14f
                    val originX = size.width / 2f
                    onTraceChange(((offset.x - originX) / scale).toDouble().coerceIn(-6.0, 6.0))
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val scale = size.width / 14f
                    val originX = size.width / 2f
                    onTraceChange(((change.position.x - originX) / scale).toDouble().coerceIn(-6.0, 6.0))
                }
            }
            .semantics { contentDescription = "Interactive graphing canvas with axes, curves, trace point, and annotations" },
    ) {
        val scale = size.width / 14f
        val origin = Offset(size.width / 2f, size.height / 2f)
        val tx: (Vec2) -> Offset = { Offset(origin.x + it.x.toFloat() * scale, origin.y - it.y.toFloat() * scale) }
        drawGrid(origin, scale)
        functions.forEachIndexed { index, fn ->
            val color = if (fn.colorKey == "cyan") Cyan else Violet
            val sample = runCatching { graph.sample(fn.expression, -7.0, 7.0) }.getOrNull()
            sample?.points?.zipWithNext()?.forEachIndexed { i, pair ->
                if (!sample.breaks.contains(i)) drawLine(color, tx(pair.first), tx(pair.second), 4.5f, cap = StrokeCap.Round)
            }
            val trace = runCatching {
                val y = engine.compile(fn.expression).eval(mapOf("x" to traceX))
                Vec2(traceX, y)
            }.getOrNull()
            trace?.takeIf { it.y in -7.0..7.0 }?.let {
                if (graphTool in setOf(GraphTool.Trace, GraphTool.Tangent, GraphTool.Derivative, GraphTool.Integral) || index == 0) {
                    drawRadiantPoint(tx(it), color, "${fn.name}: (${trim(it.x)}, ${trim(it.y)})")
                }
            }
        }
        drawGraphAnalysisOverlay(functions.firstOrNull()?.expression, traceX, graphTool, engine, tx, origin, scale)
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
    traceX: Double,
    graphTool: GraphTool,
    engine: ExpressionEngine,
    tx: (Vec2) -> Offset,
    origin: Offset,
    scale: Float,
) {
    if (expression == null) return
    val compiled = runCatching { engine.compile(expression) }.getOrNull() ?: return
    fun f(x: Double) = runCatching { compiled.eval(mapOf("x" to x)) }.getOrDefault(Double.NaN)
    val y = f(traceX)
    if (!y.isFinite()) return
    val point = Vec2(traceX, y)
    val h = 0.001
    val slope = (f(traceX + h) - f(traceX - h)) / (2.0 * h)
    when (graphTool) {
        GraphTool.Tangent -> {
            val left = -7.0
            val right = 7.0
            val tangent = { x: Double -> y + slope * (x - traceX) }
            drawLine(Amber, tx(Vec2(left, tangent(left))), tx(Vec2(right, tangent(right))), 3f, cap = StrokeCap.Round)
            drawGraphLabel("tangent slope ${trim(slope)}", tx(point) + Offset(18f, -54f), Amber)
        }
        GraphTool.Derivative -> {
            val derivativePoints = (-180..180).mapNotNull { i ->
                val x = i / 20.0
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
        GraphTool.Intersections -> {
            drawLine(Amber.copy(.7f), Offset(0f, origin.y), Offset(size.width, origin.y), 2f)
            drawGraphLabel("roots/intersections highlighted", Offset(origin.x + 20f, origin.y + 42f), Amber)
        }
        GraphTool.Plot, GraphTool.Trace -> Unit
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
    wire: Boolean,
    onSelect: (Int) -> Unit,
    onMove: (Int, Vec3) -> Unit,
    onSelectVector: (Int) -> Unit,
    onMoveVector: (Int, Vec3) -> Unit,
) {
    var dragSolidIndex by remember { mutableStateOf<Int?>(null) }
    var dragVectorIndex by remember { mutableStateOf<Int?>(null) }
    Canvas(
        modifier
            .pointerInput(solids, vectors, rx, ry, rz, zoom) {
                detectTapGestures { tap ->
                    val center = Offset(size.width * .52f, size.height * .45f)
                    val scale = 74f * zoom
                    val nearestVector = vectors.indices.minByOrNull { index ->
                        val vector = vectors[index]
                        val a = project(rotate(vector.start, rx, ry, rz), center, scale)
                        val b = project(rotate(vector.end, rx, ry, rz), center, scale)
                        minOf((a - tap).getDistance(), (b - tap).getDistance(), (Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f) - tap).getDistance())
                    }
                    val nearestVectorDistance = nearestVector?.let { index ->
                        val vector = vectors[index]
                        val a = project(rotate(vector.start, rx, ry, rz), center, scale)
                        val b = project(rotate(vector.end, rx, ry, rz), center, scale)
                        minOf((a - tap).getDistance(), (b - tap).getDistance(), (Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f) - tap).getDistance())
                    } ?: Float.MAX_VALUE
                    val nearestSolid = solids.indices.minByOrNull { index ->
                        val projected = project(rotate(solids[index].position, rx, ry, rz), center, scale)
                        (projected - tap).getDistance()
                    }
                    val nearestSolidDistance = nearestSolid?.let { index ->
                        val projected = project(rotate(solids[index].position, rx, ry, rz), center, scale)
                        (projected - tap).getDistance()
                    } ?: Float.MAX_VALUE
                    if (nearestVector != null && nearestVectorDistance < nearestSolidDistance && nearestVectorDistance < 96f) {
                        onSelectVector(nearestVector)
                    } else {
                        nearestSolid?.let { index ->
                            if (nearestSolidDistance < 96f) onSelect(index)
                        }
                    }
                }
            }
            .pointerInput(solids, vectors, selectedIndex, selectedVectorIndex, zoom) {
                detectDragGestures(
                    onDragStart = { start ->
                        val center = Offset(size.width * .52f, size.height * .45f)
                        val scale = 74f * zoom
                        dragVectorIndex = vectors.indices.minByOrNull { index ->
                            val vector = vectors[index]
                            val a = project(rotate(vector.start, rx, ry, rz), center, scale)
                            val b = project(rotate(vector.end, rx, ry, rz), center, scale)
                            minOf((a - start).getDistance(), (b - start).getDistance())
                        }?.takeIf { index ->
                            val vector = vectors[index]
                            val a = project(rotate(vector.start, rx, ry, rz), center, scale)
                            val b = project(rotate(vector.end, rx, ry, rz), center, scale)
                            minOf((a - start).getDistance(), (b - start).getDistance()) < 110f
                        }
                        dragSolidIndex = if (dragVectorIndex == null) solids.indices.minByOrNull { index ->
                            val projected = project(rotate(solids[index].position, rx, ry, rz), center, scale)
                            (projected - start).getDistance()
                        }?.takeIf { index ->
                            val projected = project(rotate(solids[index].position, rx, ry, rz), center, scale)
                            (projected - start).getDistance() < 110f
                        } ?: selectedIndex else null
                        dragVectorIndex?.let(onSelectVector)
                        dragSolidIndex?.let(onSelect)
                    },
                    onDragEnd = {
                        dragSolidIndex = null
                        dragVectorIndex = null
                    },
                ) { change, dragAmount ->
                    val scale = (74f * zoom).coerceAtLeast(36f)
                    val delta = Vec3((dragAmount.x / scale).toDouble(), 0.0, (dragAmount.y / scale).toDouble())
                    dragVectorIndex?.let { onMoveVector(it, delta) }
                    dragSolidIndex?.let { index ->
                        val solid = solids.getOrNull(index) ?: return@let
                        onMove(index, solid.position + delta)
                    }
                    change.consume()
                }
            }
            .semantics { contentDescription = "3D solids workspace with projected solid geometry" },
    ) {
        val center = Offset(size.width * .52f, size.height * .45f)
        drawPerspectiveGrid(center)
        vectors.forEachIndexed { index, vector ->
            drawVector3D(vector, rx, ry, rz, center, 74f * zoom, if (index == selectedVectorIndex) Amber else Green, index == selectedVectorIndex)
        }
        solids.forEachIndexed { index, solid ->
            val color = if (index == selectedIndex) Cyan else if (index % 2 == 0) Violet else Green
            drawSolidProjection(solid, solid.position, rx, ry, rz, center, 74f * zoom, color, wire, index == selectedIndex)
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
    zoom: Float,
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
    onTrace: (Vec2) -> Unit,
) {
    val engine = remember { ExpressionEngine() }
    Canvas(
        modifier
            .pointerInput(activeTool) {
                detectTapGestures { tap ->
                    val center = Offset(size.width * .5f, size.height * .5f)
                    val scale = 54f * zoom
                    val x = ((tap.x - center.x) / scale).toDouble().coerceIn(-3.0, 3.0)
                    val y = ((tap.y - center.y) / scale).toDouble().coerceIn(-3.0, 3.0)
                    onTrace(Vec2(x, y))
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    if (activeTool == SurfaceTool.Trace) {
                        val center = Offset(size.width * .5f, size.height * .5f)
                        val scale = 54f * zoom
                        onTrace(
                            Vec2(
                                ((change.position.x - center.x) / scale).toDouble().coerceIn(-3.0, 3.0),
                                ((change.position.y - center.y) / scale).toDouble().coerceIn(-3.0, 3.0),
                            ),
                        )
                    } else {
                        onRotate(dragAmount.x * .35f)
                        onTilt(-dragAmount.y * .18f)
                    }
                    change.consume()
                }
            }
            .semantics { contentDescription = "3D graphing surface generated from equation mesh" },
    ) {
        val center = Offset(size.width * .5f, size.height * .5f)
        val scale = 54f * zoom
        fun map(v: Vec3) = project(rotate(v, tilt, rotation, 0f), center, scale)
        drawPerspectiveGrid(center)
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
    for (i in -14..14) {
        val x = origin.x + i * scale
        val y = origin.y + i * scale
        drawLine(Grid, Offset(x, 0f), Offset(x, size.height), if (i == 0) 3f else 1f)
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
    tx: (Vec2) -> Offset,
) {
    shapes.forEachIndexed { index, shape ->
        val shapePoints = shape.pointIndices.mapNotNull { points.getOrNull(it) }
        val accent = if (index % 2 == 0) Violet else Cyan
        drawShape2D(shape.type, shapePoints, tx, accent, filled = true)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawConstructionPreview(
    pending: List<Vec2>,
    tool: GeometryTool,
    tx: (Vec2) -> Offset,
) {
    val type = tool.toShape2DType() ?: return
    if (pending.isEmpty()) return
    pending.forEachIndexed { index, point -> drawRadiantPoint(tx(point), Amber, "tap ${index + 1}") }
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
        Shape2DType.Line, Shape2DType.Ray, Shape2DType.Segment -> {
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
        }
        Shape2DType.Circle -> {
            if (points.size < 2) return
            val center = offset(0)
            val radius = (offset(1) - center).getDistance()
            if (filled) drawCircle(Brush.radialGradient(listOf(accent.copy(.22f), Color.Transparent), center, radius), radius, center)
            drawCircle(accent, radius, center, style = stroke)
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
) {
    val start = project(rotate(vector.start, rx, ry, rz), center, scale)
    val end = project(rotate(vector.end, rx, ry, rz), center, scale)
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
) {
    fun p(v: Vec3) = project(rotate(v + offset, rx, ry, rz), center, scale)
    val strokeWidth = if (wire) 2.2f else 4.2f
    val anchor = project(rotate(offset, rx, ry, rz), center, scale)
    if (selected) {
        drawCircle(Brush.radialGradient(listOf(color.copy(.34f), Color.Transparent), anchor, 112f), radius = 112f, center = anchor)
        drawCircle(color.copy(.85f), radius = 64f, center = anchor, style = Stroke(2.4f))
    }
    when (solid.type) {
        SolidType.Cube, SolidType.Cuboid -> {
            val vertices = cubeVertices(solid.width, solid.height, solid.depth).map(::p)
            val edges = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 0, 4 to 5, 5 to 6, 6 to 7, 7 to 4, 0 to 4, 1 to 5, 2 to 6, 3 to 7)
            edges.forEach { (a, b) -> drawLine(color, vertices[a], vertices[b], strokeWidth) }
            vertices.forEach { drawCircle(color, 4.5f, it) }
        }
        SolidType.Pyramid -> {
            val base = listOf(
                Vec3(-solid.width / 2, -solid.height / 2, -solid.depth / 2),
                Vec3(solid.width / 2, -solid.height / 2, -solid.depth / 2),
                Vec3(solid.width / 2, -solid.height / 2, solid.depth / 2),
                Vec3(-solid.width / 2, -solid.height / 2, solid.depth / 2),
            )
            val apex = Vec3(0.0, solid.height / 2, 0.0)
            base.zipWithNext().forEach { (a, b) -> drawLine(color, p(a), p(b), strokeWidth) }
            drawLine(color, p(base.last()), p(base.first()), strokeWidth)
            base.forEach { drawLine(color, p(it), p(apex), strokeWidth) }
        }
        SolidType.Cylinder, SolidType.Cone, SolidType.Sphere, SolidType.Torus -> {
            val steps = 36
            val radius = solid.radius.coerceAtLeast(0.35)
            val bottom = (0..steps).map {
                val t = it / steps.toDouble() * Math.PI * 2.0
                Vec3(cos(t) * radius, -solid.height / 2, sin(t) * radius)
            }
            val top = (0..steps).map {
                val t = it / steps.toDouble() * Math.PI * 2.0
                Vec3(cos(t) * radius, solid.height / 2, sin(t) * radius)
            }
            when (solid.type) {
                SolidType.Cone -> {
                    val apex = Vec3(0.0, solid.height / 2, 0.0)
                    bottom.zipWithNext().forEach { (a, b) -> drawLine(color, p(a), p(b), strokeWidth) }
                    listOf(0, 9, 18, 27).forEach { drawLine(color, p(bottom[it]), p(apex), strokeWidth) }
                }
                SolidType.Sphere -> {
                    bottom.zipWithNext().forEach { (a, b) -> drawLine(color, p(a.copy(y = 0.0)), p(b.copy(y = 0.0)), strokeWidth) }
                    top.zipWithNext().forEach { (a, b) -> drawLine(color.copy(alpha = .7f), p(Vec3(a.x, a.z, 0.0)), p(Vec3(b.x, b.z, 0.0)), 1.6f) }
                    bottom.zipWithNext().forEach { (a, b) -> drawLine(color.copy(alpha = .5f), p(Vec3(0.0, a.x, a.z)), p(Vec3(0.0, b.x, b.z)), 1.4f) }
                }
                SolidType.Torus -> {
                    bottom.zipWithNext().forEach { (a, b) -> drawLine(color, p(a.copy(y = 0.0) * 1.35), p(b.copy(y = 0.0) * 1.35), strokeWidth) }
                    bottom.zipWithNext().forEach { (a, b) -> drawLine(color.copy(alpha = .55f), p(a.copy(y = 0.0) * .75), p(b.copy(y = 0.0) * .75), 1.6f) }
                }
                else -> {
                    bottom.zipWithNext().forEach { (a, b) -> drawLine(color, p(a), p(b), strokeWidth) }
                    top.zipWithNext().forEach { (a, b) -> drawLine(color, p(a), p(b), strokeWidth) }
                    listOf(0, 9, 18, 27).forEach { drawLine(color.copy(alpha = .7f), p(bottom[it]), p(top[it]), strokeWidth) }
                }
            }
        }
    }
    if (selected) {
        drawGraphLabel("${solid.type.name} selected", anchor + Offset(20f, -72f), color)
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

private fun project(p: Vec3, center: Offset, scale: Float): Offset {
    val perspective = 1.0 / (1.0 + (p.z + 5.0) * 0.08)
    return Offset((center.x + p.x * scale * perspective).toFloat(), (center.y - p.y * scale * perspective + p.z * 18).toFloat())
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
