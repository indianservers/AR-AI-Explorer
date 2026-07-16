package com.indianservers.aiexplorer

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.layout.onSizeChanged
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
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import com.indianservers.aiexplorer.core.ExpressionEngine
import com.indianservers.aiexplorer.core.TrustedMathKernel
import com.indianservers.aiexplorer.core.EquivalenceEvidence
import com.indianservers.aiexplorer.core.InteractiveTrigEngine
import com.indianservers.aiexplorer.core.InteractiveTrigIdentityLab
import com.indianservers.aiexplorer.core.TrigFunction
import com.indianservers.aiexplorer.core.TrigAngleUnit
import com.indianservers.aiexplorer.core.InverseTrigFunction
import com.indianservers.aiexplorer.core.PolarCurveType
import com.indianservers.aiexplorer.core.HarmonicComponent
import com.indianservers.aiexplorer.core.TrigTransform
import com.indianservers.aiexplorer.core.TrigViewport
import com.indianservers.aiexplorer.core.TrigViewportEngine
import com.indianservers.aiexplorer.core.TriangleTrigSolver
import com.indianservers.aiexplorer.core.Geometry2D
import com.indianservers.aiexplorer.core.Geometry3D
import com.indianservers.aiexplorer.core.GeometryGesturePolicy
import com.indianservers.aiexplorer.core.GeometryGestureTarget
import com.indianservers.aiexplorer.core.AxisConstraint
import com.indianservers.aiexplorer.core.GestureMode
import com.indianservers.aiexplorer.core.InteractionGeometry
import com.indianservers.aiexplorer.core.PrecisionInteraction
import com.indianservers.aiexplorer.core.SmartSnapEngine
import com.indianservers.aiexplorer.core.CrossSection3D
import com.indianservers.aiexplorer.core.Graph3D
import com.indianservers.aiexplorer.core.GraphAnalysis
import com.indianservers.aiexplorer.core.GraphDefinitionKind
import com.indianservers.aiexplorer.core.StatisticsEngine
import com.indianservers.aiexplorer.core.ProbabilityEngine
import com.indianservers.aiexplorer.core.AdvancedGraphDefinition
import com.indianservers.aiexplorer.core.AdvancedGraphEngine
import com.indianservers.aiexplorer.core.AdvancedGraphKind
import com.indianservers.aiexplorer.core.GraphDomain
import com.indianservers.aiexplorer.core.GraphViewport
import com.indianservers.aiexplorer.core.GraphEquationNames
import com.indianservers.aiexplorer.core.GraphAddKind
import com.indianservers.aiexplorer.core.GraphAxisSettings
import com.indianservers.aiexplorer.core.GraphDomainSelection
import com.indianservers.aiexplorer.core.GraphLineStyle
import com.indianservers.aiexplorer.core.GraphSnapshot
import com.indianservers.aiexplorer.core.GraphTransformKind
import com.indianservers.aiexplorer.core.GraphUxEngine
import com.indianservers.aiexplorer.core.GraphViewState
import com.indianservers.aiexplorer.core.AxisNumberFormat
import com.indianservers.aiexplorer.core.DistributionEngine
import com.indianservers.aiexplorer.core.DistributionKind
import com.indianservers.aiexplorer.core.ProbabilityDistribution
import com.indianservers.aiexplorer.core.SurfaceCalculus
import com.indianservers.aiexplorer.core.AnalyticGeometry2D
import com.indianservers.aiexplorer.core.AnalyticGeometry3D
import com.indianservers.aiexplorer.core.AdvancedStatisticsEngine
import com.indianservers.aiexplorer.core.MathSpreadsheetEngine
import com.indianservers.aiexplorer.core.MissingDataPolicy
import com.indianservers.aiexplorer.core.Phase4Statistics
import com.indianservers.aiexplorer.core.RandomExperimentEngine
import com.indianservers.aiexplorer.core.RandomExperimentKind
import com.indianservers.aiexplorer.core.ConditionalProbabilityEngine
import com.indianservers.aiexplorer.core.CombinatoricsLab
import com.indianservers.aiexplorer.core.DynamicGeometryEngine
import com.indianservers.aiexplorer.core.DynamicGeometryDocument
import com.indianservers.aiexplorer.core.DynamicGeometryObject
import com.indianservers.aiexplorer.core.DynamicPoint
import com.indianservers.aiexplorer.core.DynamicPointRule
import com.indianservers.aiexplorer.core.ManipulativeEngine
import com.indianservers.aiexplorer.core.ManipulativeItem
import com.indianservers.aiexplorer.core.ManipulativeKind
import com.indianservers.aiexplorer.core.ManipulativeScene
import com.indianservers.aiexplorer.core.FormalMathDestination
import com.indianservers.aiexplorer.core.VisualProofCatalog
import com.indianservers.aiexplorer.core.VisualProofEngine
import com.indianservers.aiexplorer.core.DescriptiveStatistics
import com.indianservers.aiexplorer.core.HistogramBin
import com.indianservers.aiexplorer.core.InferentialStatistics
import com.indianservers.aiexplorer.core.StatisticsCurriculum
import com.indianservers.aiexplorer.core.StatisticsStudyLevel
import com.indianservers.aiexplorer.core.MathProblemSolver
import com.indianservers.aiexplorer.core.MathSolverTutor
import com.indianservers.aiexplorer.core.GuidedSolution
import com.indianservers.aiexplorer.core.SolverMethod
import com.indianservers.aiexplorer.core.SolverReveal
import com.indianservers.aiexplorer.core.SolverDestination
import com.indianservers.aiexplorer.core.SolverResultKind
import com.indianservers.aiexplorer.core.MathInputIntelligence
import com.indianservers.aiexplorer.core.MathInputTokenKind
import com.indianservers.aiexplorer.core.MathNotebookDocument
import com.indianservers.aiexplorer.core.MathNotebookEngine
import com.indianservers.aiexplorer.core.NotebookCell
import com.indianservers.aiexplorer.core.NotebookCellKind
import com.indianservers.aiexplorer.core.ProblemSolution
import com.indianservers.aiexplorer.core.DeviceCapabilityManager
import com.indianservers.aiexplorer.core.DeviceCapabilityProfile
import com.indianservers.aiexplorer.core.ProductPerformanceManager
import com.indianservers.aiexplorer.core.AccessibilityNodeEvidence
import com.indianservers.aiexplorer.core.AccessibilityQaEngine
import com.indianservers.aiexplorer.core.DeterministicMathBenchmarkRunner
import com.indianservers.aiexplorer.core.MathBenchmarkReport
import com.indianservers.aiexplorer.core.QaEvidenceSection
import com.indianservers.aiexplorer.core.ReleaseMathBenchmarkCatalog
import com.indianservers.aiexplorer.core.ReleaseQaEvidenceBundle
import com.indianservers.aiexplorer.core.ReleaseQaEvidenceCodec
import com.indianservers.aiexplorer.core.ProductPerformanceSnapshot
import com.indianservers.aiexplorer.core.AngleMode
import com.indianservers.aiexplorer.core.ScientificCalculator
import com.indianservers.aiexplorer.core.ScientificCalculatorResult
import com.indianservers.aiexplorer.core.SolutionStepRole
import com.indianservers.aiexplorer.core.SymbolicCasEngine
import com.indianservers.aiexplorer.core.Solid
import com.indianservers.aiexplorer.core.SolidType
import com.indianservers.aiexplorer.core.SolidMeshFactory
import com.indianservers.aiexplorer.core.Vec2
import com.indianservers.aiexplorer.core.Vec3
import com.indianservers.aiexplorer.core.Vector3D
import com.indianservers.aiexplorer.core.stripEquation
import com.indianservers.aiexplorer.core.trim
import com.indianservers.aiexplorer.learning.Assignment
import com.indianservers.aiexplorer.learning.ActivityAnswer
import com.indianservers.aiexplorer.learning.ActivityBlock
import com.indianservers.aiexplorer.learning.ActivityEvaluationContext
import com.indianservers.aiexplorer.learning.ClassroomEngine
import com.indianservers.aiexplorer.learning.InteractiveActivityAuthoring
import com.indianservers.aiexplorer.learning.InteractiveActivityCatalog
import com.indianservers.aiexplorer.learning.InteractiveActivityEngine
import com.indianservers.aiexplorer.learning.LearnerProgress
import com.indianservers.aiexplorer.learning.LearningActivity
import com.indianservers.aiexplorer.learning.LearningCatalog
import com.indianservers.aiexplorer.learning.LearningEvaluator
import com.indianservers.aiexplorer.learning.LearningPackage
import com.indianservers.aiexplorer.learning.LearningOperation
import com.indianservers.aiexplorer.learning.LearningOperationType
import com.indianservers.aiexplorer.learning.LearningRole
import com.indianservers.aiexplorer.learning.LearningValidation
import com.indianservers.aiexplorer.learning.FormulaCategory
import com.indianservers.aiexplorer.learning.KnowledgeLevel
import com.indianservers.aiexplorer.learning.KnowledgeTopic
import com.indianservers.aiexplorer.learning.MathKnowledgeCatalog
import com.indianservers.aiexplorer.learning.McqQuestion
import com.indianservers.aiexplorer.learning.OfflineLearningQueue
import com.indianservers.aiexplorer.learning.PackageValidation
import com.indianservers.aiexplorer.learning.ProgressStatus
import com.indianservers.aiexplorer.learning.QuizEngine
import com.indianservers.aiexplorer.learning.QuizLevel
import com.indianservers.aiexplorer.learning.QuizSession
import com.indianservers.aiexplorer.learning.QuizSubject
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
import com.indianservers.aiexplorer.workspace.LinkedMathKernel
import com.indianservers.aiexplorer.workspace.LinkedMathView
import com.indianservers.aiexplorer.workspace.GraphRowMetadataState
import com.indianservers.aiexplorer.workspace.GraphSliderMetadataState
import com.indianservers.aiexplorer.workspace.GraphSliderPlaybackMode
import com.indianservers.aiexplorer.workspace.MathObjectGraph
import com.indianservers.aiexplorer.workspace.MathModule
import com.indianservers.aiexplorer.workspace.AIExplorerProjectArchive
import com.indianservers.aiexplorer.workspace.AppSecurityAuditEngine
import com.indianservers.aiexplorer.workspace.AppSecurityConfiguration
import com.indianservers.aiexplorer.workspace.GeoGebraExchange
import com.indianservers.aiexplorer.workspace.ProjectSection
import com.indianservers.aiexplorer.workspace.ProjectSectionKind
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
import kotlin.math.roundToInt
import com.indianservers.aiexplorer.workspace.TransformShape2DCommand
import com.indianservers.aiexplorer.workspace.UpdateShapeCommand
import com.indianservers.aiexplorer.workspace.UpdateFunctionCommand
import com.indianservers.aiexplorer.workspace.ReorderFunctionsCommand
import com.indianservers.aiexplorer.workspace.UpdateGraphRowMetadataCommand
import com.indianservers.aiexplorer.workspace.UpdateGraphSliderMetadataCommand
import com.indianservers.aiexplorer.workspace.WorkspaceJson
import com.indianservers.aiexplorer.workspace.WorkspaceState
import com.indianservers.aiexplorer.workspace.UniversalMathDocument
import com.indianservers.aiexplorer.workspace.UniversalMathDocumentEngine
import com.indianservers.aiexplorer.workspace.UniversalWorkspaceBridge
import com.indianservers.aiexplorer.workspace.UniversalMathObjectFactory
import com.indianservers.aiexplorer.workspace.recomputed
import com.indianservers.aiexplorer.workspace.resolvePointDependency
import com.indianservers.aiexplorer.spatial.ARScaleMode
import com.indianservers.aiexplorer.spatial.ARAvailability
import com.indianservers.aiexplorer.spatial.ARCapabilities
import com.indianservers.aiexplorer.spatial.ARCoreSessionController
import com.indianservers.aiexplorer.spatial.SpatialSafety
import com.indianservers.aiexplorer.spatial.TrackingQuality
import com.indianservers.aiexplorer.spatial.SpatialPlacementEngine
import com.indianservers.aiexplorer.spatial.ARCoreCompositorView
import com.indianservers.aiexplorer.spatial.ARFrameState
import com.indianservers.aiexplorer.spatial.ARPrivacySafetyChecklist
import com.indianservers.aiexplorer.spatial.SharedGpuSceneCompiler
import com.indianservers.aiexplorer.spatial.SharedSpatialSceneBuilder
import com.indianservers.aiexplorer.spatial.SpatialAnnotation
import com.indianservers.aiexplorer.spatial.SpatialCompositorScene
import com.indianservers.aiexplorer.spatial.SpatialHit
import com.indianservers.aiexplorer.spatial.SpatialHitType
import com.indianservers.aiexplorer.spatial.SpatialLessonCatalog
import com.indianservers.aiexplorer.spatial.SpatialPerformanceManager
import com.indianservers.aiexplorer.spatial.ThermalLevel
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
import kotlin.math.log10
import kotlin.math.pow

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
private enum class ProbabilityLabSection { Distributions, Statistics, Spreadsheet, Experiments, Learn }
enum class KnowledgeSection(val title: String) { Formulas("Formulas"), Mcqs("MCQs"), Visualize("Visualize Formulas"), Theorems("Theorems"), Proofs("Visual Proofs"), Dictionary("Maths Dictionary") }
private enum class StatisticsChartType(val label: String) { Histogram("Histogram"), BoxPlot("Box Plot"), DotPlot("Dot Plot"), Ecdf("ECDF"), NormalQq("Normal Q-Q") }
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
    val spokenMath: Boolean = false,
    val graphSonification: Boolean = false,
    val largeTouchTargets: Boolean = false,
    val decimalPrecision: Int = 2,
)

private data class SubjectOption(val title: String, val description: String, val symbol: String, val enabled: Boolean)
private data class MathMenuOption(val title: String, val description: String, val icon: String, val available: Boolean = false)

private val SubjectOptions = listOf(
    SubjectOption("Maths", "Interactive mathematics laboratory", "∑", true),
    SubjectOption("Physics", "Mechanics, waves and fields", "F", false),
    SubjectOption("Chemistry", "Molecules, reactions and matter", "⚗", false),
    SubjectOption("Biology", "Cells, systems and life", "DNA", false),
    SubjectOption("Astro Physics", "Stars, space and cosmology", "✦", false),
    SubjectOption("IQ Labs", "Logic, patterns and reasoning", "IQ", false),
)

private val MathMenuOptions = listOf(
    MathMenuOption("Explore Workspaces", "2D, 3D, graphing, trigonometry and spatial AR", "◇", true),
    MathMenuOption("Scientific Calculator", "Scientific keypad, constants, conversions and notation modes", "Sci", true),
    MathMenuOption("Math Notebook", "Named values, linked functions and reusable exact results", "#", true),
    MathMenuOption("Formulas", "Searchable formula reference", "F", true),
    MathMenuOption("MCQs", "Practice questions and explanations", "?", true),
    MathMenuOption("Visualize Formulas", "Turn formulas into interactive scenes", "O", true),
    MathMenuOption("Theorems", "Statements, conditions and applications", "T", true),
    MathMenuOption("Visual Proofs", "Manipulable visual demonstrations", "V", true),
    MathMenuOption("Maths Dictionary", "Terms, notation and examples", "A", true),
    MathMenuOption("Manipulatives", "Algebra tiles, fractions, balance, geometry and visual proof labs", "▦", true),
    MathMenuOption("Problem Solver", "Explainable, step-by-step answers with verification", "✦", true),
    MathMenuOption("Formulas", "Searchable formula reference", "ƒ"),
    MathMenuOption("MCQs", "Practice questions and explanations", "?"),
    MathMenuOption("Visualize Formulas", "Turn formulas into interactive scenes", "◉"),
    MathMenuOption("Theorems", "Statements, conditions and applications", "∴"),
    MathMenuOption("Visual Proofs", "Manipulable visual demonstrations", "✓"),
    MathMenuOption("Maths Dictionary", "Terms, notation and examples", "A"),
    MathMenuOption("Probability & Statistics", "Interactive distributions, intervals and probability plots", "σ", true),
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

class ExplorerViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
    private val history = CommandHistory()
    private val notebookEngine = MathNotebookEngine()
    private val linkedMathKernel = LinkedMathKernel()
    private val mathObjectGraph = MathObjectGraph()
    private val universalDocumentEngine = UniversalMathDocumentEngine()
    private val trustedMathKernel = TrustedMathKernel()
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
    var selectedShapes by mutableStateOf<Set<Int>>(emptySet())
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
    var showScientificCalculator by mutableStateOf(false)
        private set
    var showMathNotebook by mutableStateOf(false)
        private set
    var showProbabilityLab by mutableStateOf(false)
        private set
    var showKnowledgeHub by mutableStateOf(false)
        private set
    var activeKnowledgeSection by mutableStateOf(KnowledgeSection.Formulas)
        private set
    var notebookDocument by mutableStateOf(
        savedStateHandle.get<ArrayList<String>>("notebookInputs").orEmpty().fold(MathNotebookDocument()) { document, input ->
            runCatching { notebookEngine.submit(document, input) }.getOrDefault(document)
        },
    )
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
    val linkedMathSnapshot
        get() = linkedMathKernel.snapshot(state)
    val mathObjectGraphSnapshot
        get() = mathObjectGraph.snapshot(state)
    fun mathObjectGraphSnapshot(parameterValues: Map<String, Double>) = mathObjectGraph.snapshot(state, parameterValues)
    val universalMathDocument: UniversalMathDocument
        get() {
            val base = UniversalWorkspaceBridge.fromWorkspace(state)
            val notebookObjects = UniversalMathObjectFactory.fromNotebook(notebookDocument)
            val probability = UniversalMathObjectFactory.probability("probability-normal", "Normal distribution", "Normal", mapOf("mean" to 0.0, "sd" to 1.0))
            return base.copy(objects = base.objects + notebookObjects.associateBy { it.id } + (probability.id to probability), revision = maxOf(base.revision, notebookDocument.revision.toLong()))
        }
    fun verifyMathEquivalence(left: String, right: String): EquivalenceEvidence = trustedMathKernel.equivalence(left, right)

    val constructionProtocol: List<String> get() = history.protocol

    fun open(module: MathModule) {
        state = state.copy(module = module)
        showProblemSolver = false
        showScientificCalculator = false
        showMathNotebook = false
        showProbabilityLab = false
        showKnowledgeHub = false
        showMathMenu = false
        hidePanels()
    }

    fun enterMaths() {
        showSubjectHub = false
        showProblemSolver = false
        showScientificCalculator = false
        showMathNotebook = false
        showProbabilityLab = false
        showKnowledgeHub = false
        showMathMenu = false
        status = "Mathematics Explorer"
    }

    fun openProblemSolver() {
        showSubjectHub = false
        showProblemSolver = true
        showScientificCalculator = false
        showMathNotebook = false
        showProbabilityLab = false
        showKnowledgeHub = false
        showMathMenu = false
        showActionDock = false
        hidePanels()
        status = "Explainable Problem Solver"
    }

    fun openProbabilityLab() {
        showSubjectHub = false
        showProblemSolver = false
        showScientificCalculator = false
        showMathNotebook = false
        showProbabilityLab = true
        showKnowledgeHub = false
        showMathMenu = false
        showActionDock = false
        hidePanels()
        status = "Probability & Distributions Lab"
    }

    fun openSubjectHub() {
        showSubjectHub = true
        showProblemSolver = false
        showScientificCalculator = false
        showMathNotebook = false
        showProbabilityLab = false
        showKnowledgeHub = false
        showMathMenu = false
        hidePanels()
        status = "Choose a subject"
    }

    fun openMathNotebook() {
        showSubjectHub = false
        showProblemSolver = false
        showScientificCalculator = false
        showMathNotebook = true
        showProbabilityLab = false
        showKnowledgeHub = false
        showMathMenu = false
        showActionDock = false
        hidePanels()
        status = "Unified Math Notebook"
    }

    fun openKnowledgeHub(section: KnowledgeSection) {
        showSubjectHub = false
        showProblemSolver = false
        showScientificCalculator = false
        showMathNotebook = false
        showProbabilityLab = false
        showKnowledgeHub = true
        activeKnowledgeSection = section
        showMathMenu = false
        showActionDock = false
        hidePanels()
        status = "Maths ${section.title}"
    }

    fun openScientificCalculator() {
        showSubjectHub = false
        showProblemSolver = false
        showScientificCalculator = true
        showMathNotebook = false
        showProbabilityLab = false
        showKnowledgeHub = false
        showMathMenu = false
        showActionDock = false
        hidePanels()
        status = "Scientific Calculator"
    }

    fun submitNotebook(input: String) {
        notebookDocument = runCatching { notebookEngine.submit(notebookDocument, input) }
            .getOrElse { error ->
                status = error.message ?: "Notebook input was not accepted"
                return
            }
        status = if (notebookDocument.hasErrors) "Notebook updated with diagnostics" else "Notebook dependencies updated"
        persistNotebook()
    }

    fun removeNotebookCell(id: String) {
        notebookDocument = notebookEngine.remove(notebookDocument, id)
        persistNotebook()
        status = "Notebook cell removed"
    }

    fun clearNotebook() {
        notebookDocument = notebookEngine.clear()
        persistNotebook()
        status = "Notebook cleared"
    }

    private fun persistNotebook() {
        savedStateHandle["notebookInputs"] = ArrayList(notebookDocument.cells.map { it.input })
    }

    fun graphNotebookFunction(cell: NotebookCell) {
        val expression = cell.graphExpression ?: return
        val symbol = cell.symbol ?: "f"
        val existing = state.functions.indexOfFirst { it.name.substringBefore('(') == symbol }
        val color = listOf("cyan", "violet", "green", "amber")[(existing.takeIf { it >= 0 } ?: state.functions.size) % 4]
        val definition = com.indianservers.aiexplorer.core.FunctionDefinition(
            id = if (existing >= 0) state.functions[existing].id else "notebook-${cell.id}",
            name = "$symbol(x)",
            expression = expression,
            colorKey = color,
        )
        state = if (existing >= 0) history.execute(state, UpdateFunctionCommand(existing, state.functions[existing], definition))
        else history.execute(state, AddFunctionCommand(definition))
        open(MathModule.Graph2D)
        status = "$symbol(x) linked to Graph"
    }

    fun sendSolverToGraph(guided: GuidedSolution) {
        val payload = guided.handoffs.firstOrNull { it.destination == SolverDestination.Graph && it.enabled }?.payload ?: run {
            status = "This result does not have a graphable real expression"
            return
        }
        val index = state.functions.size
        val definition = com.indianservers.aiexplorer.core.FunctionDefinition(
            id = "solver-${System.currentTimeMillis()}",
            name = "s${index + 1}(x)",
            expression = payload,
            colorKey = listOf("cyan", "violet", "green", "amber")[index % 4],
        )
        state = history.execute(state, AddFunctionCommand(definition))
        open(MathModule.Graph2D)
        status = "Solver equation sent to Graph"
    }

    fun saveSolverToNotebook(guided: GuidedSolution) {
        val question = guided.solution.question
        notebookDocument = runCatching { notebookEngine.submit(notebookDocument, question) }.getOrElse {
            status = it.message ?: "Could not save the derivation"
            return
        }
        persistNotebook()
        openMathNotebook()
        status = "Verified solver result saved to Notebook"
    }

    fun sendSolverToTable(guided: GuidedSolution) {
        val graphable = guided.handoffs.any { it.destination == SolverDestination.Graph && it.enabled }
        if (graphable) sendSolverToGraph(guided) else status = "Table payload prepared from the verified result"
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
        val opening = when (slot) {
            PanelSlot.Left -> !showLeftPanel
            PanelSlot.Right -> !showRightPanel
            PanelSlot.Bottom -> !showBottomPanel
            PanelSlot.Chrome -> !showChrome
        }
        if (slot != PanelSlot.Chrome) hideWorkspacePanelsOnly()
        when (slot) {
            PanelSlot.Left -> showLeftPanel = opening
            PanelSlot.Right -> showRightPanel = opening
            PanelSlot.Bottom -> showBottomPanel = opening
            PanelSlot.Chrome -> showChrome = opening
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
                        selectedShapes = setOf(selectedShape)
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
        selectedShape = state.shapes.indexOfLast { it.visible && index in it.pointIndices }
        selectedShapes = selectedShape.takeIf { it >= 0 }?.let(::setOf) ?: emptySet()
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
        status = if (selectedShape >= 0) "Resize ${state.shapes[selectedShape].name} from junction" else "Move point ${index + 1}"
    }

    fun beginShapeDrag(shapeIndex: Int) {
        val shape = state.shapes.getOrNull(shapeIndex) ?: return
        selectedShape = shapeIndex
        if (shapeIndex !in selectedShapes) selectedShapes = setOf(shapeIndex)
        if (shape.locked) {
            status = "${shape.name} is locked"
            return
        }
        val indices = selectedShapes.mapNotNull(state.shapes::getOrNull).filterNot { it.locked }.flatMap { it.pointIndices }.distinct()
            .filterNot { index -> state.pointDependencies.any { it.outputIndex == index } }
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

    fun previewShapeRotation(deltaDegrees: Double) {
        val gesture = pointGesture ?: return
        val center = InteractionGeometry.bounds(gesture.from)?.center ?: return
        val radians = Math.toRadians(deltaDegrees)
        val cosine = cos(radians); val sine = sin(radians)
        val replacements = gesture.indices.zip(gesture.from.map { point ->
            val local = point - center
            center + Vec2(local.x * cosine - local.y * sine, local.x * sine + local.y * cosine)
        }).toMap()
        state = state.copy(points = state.points.mapIndexed { index, old -> replacements[index] ?: old }, modifiedAt = System.currentTimeMillis()).recomputed()
    }

    fun endPointDrag() {
        val gesture = pointGesture ?: return
        val final = gesture.indices.mapNotNull(state.points::getOrNull)
        if (final != gesture.from) history.recordApplied(MovePointsCommand(gesture.indices, gesture.from, final))
        val resizedShape = selectedShape.takeIf { gesture.indices.size == 1 }?.let(state.shapes::getOrNull)
        pointGesture = null
        status = resizedShape?.let { "Resized ${it.name} from junction" } ?: "Moved object"
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
        selectedShapes = setOf(index)
        status = "Selected ${state.shapes[index].name}"
    }

    fun selectShapes(indices: Set<Int>) {
        selectedShapes = indices.filterTo(linkedSetOf()) { it in state.shapes.indices }
        selectedShape = selectedShapes.lastOrNull() ?: -1
        selectedPoint = -1
        status = if (selectedShapes.isEmpty()) "Selection cleared" else "Selected ${selectedShapes.size} object${if (selectedShapes.size == 1) "" else "s"}"
    }

    fun clearGeometrySelection() {
        selectedShapes = emptySet()
        selectedShape = -1
        selectedPoint = -1
        status = "Selection cleared"
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
        selectedShapes = selectedShape.takeIf { it >= 0 }?.let(::setOf) ?: emptySet()
        status = "Deleted ${shape.name}"
    }

    fun transformSelectedShape(type: PointDependencyType, parameters: List<Double> = emptyList()) {
        val index = selectedShape.takeIf { it in state.shapes.indices } ?: return
        state = history.execute(state, TransformShape2DCommand(index, type, parameters))
        selectedShape = state.shapes.lastIndex
        status = "${type.name} created"
    }

    fun editExpression(index: Int, expression: String) {
        state.functions.getOrNull(index) ?: return
        val from = state.functions[index].expression
        state = history.execute(state, EditExpressionCommand(index, from, expression))
        val validation = universalDocumentEngine.validate(universalMathDocument)
        status = if (validation.valid) "Expression updated through the universal maths document" else validation.diagnostics.first()
    }

    fun addFunction(expression: String = "sin(x)") {
        val index = state.functions.size
        val name = GraphEquationNames.next(state.functions.map { it.name }.toSet())
        val color = listOf("cyan", "violet", "green", "amber")[index % 4]
        state = history.execute(
            state,
            AddFunctionCommand(
                com.indianservers.aiexplorer.core.FunctionDefinition(
                    id = "function-${System.nanoTime()}-$index",
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

    fun moveFunctionLayer(index: Int, delta: Int) {
        if (index !in state.functions.indices) return
        val target = (index + delta).coerceIn(state.functions.indices)
        if (target == index) return
        val reordered = state.functions.toMutableList().apply { add(target, removeAt(index)) }
        state = history.execute(state, ReorderFunctionsCommand(state.functions, reordered))
        status = "Graph layer reordered"
    }

    fun deleteFunction(index: Int) {
        val function = state.functions.getOrNull(index) ?: return
        state = history.execute(state, DeleteFunctionCommand(index, function, state.graphRowMetadata[function.id]))
        status = "Deleted ${function.name}"
    }

    fun updateGraphRowMetadata(rowId: String, transform: (GraphRowMetadataState) -> GraphRowMetadataState) {
        val from = state.graphRowMetadata[rowId]
        val to = transform(from ?: GraphRowMetadataState())
        if (from == to) return
        state = history.execute(state, UpdateGraphRowMetadataCommand(rowId, from, to))
        status = "Graph row organized"
    }

    fun updateGraphSliderMetadata(parameter: String, transform: (GraphSliderMetadataState) -> GraphSliderMetadataState) {
        val from = state.graphSliderMetadata[parameter]
        val to = transform(from ?: GraphSliderMetadataState())
        if (from == to) return
        state = history.execute(state, UpdateGraphSliderMetadataCommand(parameter, from, to))
        status = "Graph slider updated"
    }

    fun duplicateFunction(index: Int) {
        val function = state.functions.getOrNull(index) ?: return
        val nextIndex = state.functions.size
        val base = function.name.substringBefore('(').ifBlank { "f" }
        val name = "${base}${nextIndex + 1}(x)"
        val color = listOf("cyan", "violet", "green", "amber")[(nextIndex + 1) % 4]
        state = history.execute(
            state,
            AddFunctionCommand(
                function.copy(
                    id = "function-${System.currentTimeMillis()}-$nextIndex",
                    name = name,
                    colorKey = color,
                    visible = true,
                ),
            ),
        )
        status = "Duplicated ${function.name} as $name"
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
        state = state.copy(surfaceExpression = value, modifiedAt = System.currentTimeMillis())
        val validation = universalDocumentEngine.validate(universalMathDocument)
        status = if (validation.valid) "Surface updated through the universal maths document" else validation.diagnostics.first()
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

    fun placeSpatialHit(hit: SpatialHit) {
        val from = state.spatialPlacement
        val (to, validation) = SpatialPlacementEngine.place(from, hit, System.currentTimeMillis())
        if (from != to) state = history.execute(state, TransformSpatialPlacementCommand(from, to, "Place scene on ${hit.type.name.lowercase()}"))
        status = if (validation.accepted) "Spatial scene placed · ±${trim(hit.uncertaintyMeters)} m" else validation.messages.joinToString(" ")
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
        selectedShape = -1
        selectedShapes = emptySet()
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
    var menuOffset by remember { mutableStateOf(Offset.Zero) }
    var dockOffset by remember { mutableStateOf(Offset.Zero) }
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
                LaunchedEffect(compact, wide) {
                    menuOffset = Offset.Zero
                    dockOffset = Offset.Zero
                }
                if (vm.showSubjectHub) {
                    SubjectHubScreen(
                        modifier = Modifier.fillMaxSize(),
                        wide = wide,
                        onOpenMaths = vm::enterMaths,
                    )
                } else {
                    if (vm.showMathNotebook) {
                        MathNotebookScreen(vm, wide = wide)
                    } else if (vm.showProblemSolver) {
                        ProblemSolverScreen(vm, wide = wide)
                    } else if (vm.showScientificCalculator) {
                        ScientificCalculatorScreen(vm, wide = wide)
                    } else if (vm.showProbabilityLab) {
                        ProbabilityLabScreen(vm, wide = wide)
                    } else if (vm.showKnowledgeHub) {
                        MathKnowledgeScreen(vm, wide = wide)
                    } else {
                        when (vm.state.module) {
                            MathModule.Geometry2D -> Geometry2DScreen(vm)
                            MathModule.Geometry3D -> Geometry3DScreen(vm)
                            MathModule.Graph2D -> Graph2DScreen(vm)
                            MathModule.Graph3D -> Graph3DScreen(vm)
                            MathModule.Trigonometry -> TrigonometryScreen(vm)
                            MathModule.Manipulatives -> ManipulativesScreen(vm, wide)
                            MathModule.SpatialAR -> SpatialARScreen(vm)
                        }
                    }
                    if (vm.showChrome) TopShell(vm, compact, Modifier.align(Alignment.TopCenter))
                    if (vm.showLearningPanel && !vm.showProblemSolver && !vm.showScientificCalculator && !vm.showMathNotebook && !vm.showProbabilityLab && !vm.showKnowledgeHub) LearningCoachPanel(vm, Modifier.align(Alignment.CenterEnd))
                    BottomModeSelector(vm.state.module, vm::open, compact, Modifier.align(Alignment.BottomCenter))
                    if (vm.showActionDock && vm.state.module != MathModule.Graph2D && !vm.showProblemSolver && !vm.showScientificCalculator && !vm.showMathNotebook && !vm.showProbabilityLab && !vm.showKnowledgeHub) MiniDock(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset { IntOffset(dockOffset.x.roundToInt(), dockOffset.y.roundToInt()) }
                            .padding(top = if (compact) 64.dp else 76.dp),
                        items = listOf("Focus", "Learn", "Tools", "Info", "Panel", "Export", "Close"),
                        onMove = { delta ->
                            dockOffset = Offset(
                                (dockOffset.x + delta.x).coerceIn(-720f, 0f),
                                (dockOffset.y + delta.y).coerceIn(-40f, 760f),
                            )
                        },
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
                            .offset { IntOffset(menuOffset.x.roundToInt(), menuOffset.y.roundToInt()) }
                            .widthIn(max = if (wide) 460.dp else 390.dp)
                            .fillMaxWidth(if (wide) .46f else .94f),
                        compact = compact,
                        onMove = { delta ->
                            menuOffset = Offset(
                                (menuOffset.x + delta.x).coerceIn(-280f, 280f),
                                (menuOffset.y + delta.y).coerceIn(-520f, 520f),
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ScientificCalculatorScreen(vm: ExplorerViewModel, wide: Boolean) {
    val calculator = remember { ScientificCalculator() }
    var expression by remember { mutableStateOf("sin(30)+log(1000)") }
    var angleMode by remember { mutableStateOf(AngleMode.Degrees) }
    var result by remember { mutableStateOf<ScientificCalculatorResult?>(runCatching { calculator.evaluate(expression, angleMode) }.getOrNull()) }
    var error by remember { mutableStateOf<String?>(null) }
    var conversionValue by remember { mutableStateOf("72") }
    var selectedConversion by remember { mutableStateOf(calculator.conversions.first()) }
    val appendToken: (String) -> Unit = { token -> expression += token }
    fun evaluate() {
        result = runCatching { calculator.evaluate(expression, angleMode) }
            .onSuccess { error = null }
            .onFailure { error = it.message ?: "Expression could not be evaluated" }
            .getOrNull()
    }
    GlassPanel(
        Modifier
            .fillMaxSize()
            .semantics { contentDescription = "Scientific calculator module" },
    ) {
        PanelHeader("Scientific Calculator", { vm.open(MathModule.Graph2D) }, Cyan, icon = "Sci")
        Text("Offline scientific calculator with DEG/RAD trig, constants, conversions, notation modes and reusable examples.", color = Muted, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            GlowButton(if (angleMode == AngleMode.Degrees) "DEG active" else "DEG", onClick = { angleMode = AngleMode.Degrees; evaluate() })
            GlowButton(if (angleMode == AngleMode.Radians) "RAD active" else "RAD", onClick = { angleMode = AngleMode.Radians; evaluate() })
            GlowButton("Clear") { expression = ""; result = null; error = null }
            GlowButton("Back") { expression = expression.dropLast(1) }
        }
        OutlinedTextField(
            value = expression,
            onValueChange = { expression = it },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Scientific calculator expression input" },
            label = { Text("Expression") },
            visualTransformation = MathSyntaxVisualTransformation(),
            singleLine = false,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("7", "8", "9", "/", "sin(", "asin(", "4", "5", "6", "*", "cos(", "acos(", "1", "2", "3", "-", "tan(", "atan(", "0", ".", "pi", "+", "ln(", "log(", "(", ")", "^", "sqrt(", "%", "!", "e", "c", "g", "min(", "max(", "floor(", "ceil(").forEach { token ->
                GlowButton(token) { appendToken(token) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlowButton("Calculate") { evaluate() }
            GlowButton("Open Solver") { vm.openProblemSolver() }
        }
        error?.let { Text(it, color = Amber, fontSize = 12.sp) }
        result?.let { CalculatorResultCard(it) }
        if (wide) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CalculatorConstantsPanel(calculator, onInsert = { expression += it })
                CalculatorConversionsPanel(calculator, conversionValue, { conversionValue = it }, selectedConversion, { selectedConversion = it })
            }
        } else {
            CalculatorConstantsPanel(calculator, onInsert = { expression += it })
            CalculatorConversionsPanel(calculator, conversionValue, { conversionValue = it }, selectedConversion, { selectedConversion = it })
        }
        Text("Scientific content", color = Cyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        calculator.referenceCards.forEach { card ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x33101824))
                    .border(1.dp, Cyan.copy(.18f), RoundedCornerShape(16.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(card.title, color = Ink, fontWeight = FontWeight.Bold)
                    Text(card.expression, color = Cyan, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
                Text(card.description, color = Muted, fontSize = 11.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    card.examples.forEach { example -> GlowButton(example) { expression = example; evaluate() } }
                }
            }
        }
    }
}

@Composable
private fun CalculatorResultCard(result: ScientificCalculatorResult) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x5520D9FF))
            .border(1.dp, Cyan.copy(.45f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(result.decimal, color = Ink, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
        Insight("Scientific", result.scientific, Violet)
        Insight("Engineering", result.engineering, Green)
        result.exactHint?.let { Insight("Exact hint", it, Amber) }
        Text("Normalized: ${result.normalizedExpression}", color = Muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        result.warnings.forEach { Text(it, color = Amber, fontSize = 11.sp) }
        result.steps.take(3).forEachIndexed { index, step -> Text("${index + 1}. $step", color = Muted, fontSize = 11.sp) }
    }
}

@Composable
private fun CalculatorConstantsPanel(calculator: ScientificCalculator, onInsert: (String) -> Unit) {
    Column(Modifier.widthIn(min = 240.dp).padding(2.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text("Constants", color = Cyan, fontWeight = FontWeight.Bold)
        calculator.constants.forEach { constant ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x22101824))
                    .clickable { onInsert(constant.key) }
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("${constant.label}  ${constant.unit}", color = Ink, fontWeight = FontWeight.SemiBold)
                    Text(constant.note, color = Muted, fontSize = 10.sp)
                }
                Text(constant.key, color = Cyan, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
private fun CalculatorConversionsPanel(
    calculator: ScientificCalculator,
    value: String,
    onValueChange: (String) -> Unit,
    selected: com.indianservers.aiexplorer.core.UnitConversion,
    onSelected: (com.indianservers.aiexplorer.core.UnitConversion) -> Unit,
) {
    val numeric = value.toDoubleOrNull()
    val converted = numeric?.let { calculator.convert(it, selected) }
    Column(Modifier.widthIn(min = 240.dp).padding(2.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text("Conversions", color = Cyan, fontWeight = FontWeight.Bold)
        OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text("Value") }, singleLine = true)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            calculator.conversions.forEach { conversion ->
                GlowButton("${conversion.fromUnit}→${conversion.toUnit}") { onSelected(conversion) }
            }
        }
        Insight(selected.title, "${selected.fromUnit} → ${selected.toUnit}", Violet)
        Insight("Factor", trim(selected.factor), Green)
        Insight("Result", converted?.let { "${trim(it)} ${selected.toUnit}" } ?: "Enter a number", Cyan)
        Text(selected.example, color = Muted, fontSize = 11.sp)
    }
}

@Composable
private fun MathNotebookScreen(vm: ExplorerViewModel, wide: Boolean) {
    var input by remember { mutableStateOf("a := 2") }
    var exactMode by remember { mutableStateOf(true) }
    val cas = remember { SymbolicCasEngine() }
    var casInput by remember { mutableStateOf("(x+1)^2") }
    var casOperation by remember { mutableStateOf("expand") }
    val casRow = remember(casInput, casOperation) { cas.casRow(casInput, casOperation) }
    val examples = listOf(
        "Value" to "a := 2",
        "Dependent" to "b := a^2 + 3",
        "Function" to "f(x) := a*x^2 + b",
        "Reuse cell" to "#1 + 10",
        "Exact" to "1/3 + 1/6",
    )

    @Composable
    fun InputPanel(modifier: Modifier = Modifier) {
        GlassPanel(modifier) {
            PanelHeader("Unified Math Notebook", { vm.open(vm.state.module) }, Cyan, icon = "#")
            Text("Define with := · edit a symbol by defining it again · dependent cells recalculate automatically.", color = Muted, fontSize = 12.sp)
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Notebook maths input" },
                label = { Text("Expression or assignment") },
                placeholder = { Text("f(x) := a*x^2 + 3") },
                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, color = Ink),
                minLines = 2,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                GlowButton("Run cell", enabled = input.isNotBlank()) { vm.submitNotebook(input) }
                GlowButton(if (exactMode) "Exact •" else "Decimal •") { exactMode = !exactMode }
                GlowButton("Clear", enabled = vm.notebookDocument.cells.isNotEmpty(), onClick = vm::clearNotebook)
            }
            Text("Examples", color = Ink, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                examples.forEach { (label, example) -> GlowButton(label) { input = example } }
            }
            Text("Structural maths keyboard", color = Ink, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("frac" to "()/()", "power" to "^()", "root" to "sqrt()", "matrix" to "[[1,2],[3,4]]", "vector" to "<1,2,3>", "system" to "{x+y=1, x-y=3}", "d/dx" to "differentiate ", "integral" to "integrate ", "sum" to "sum()", "limit" to "limit()").forEach { (label, insert) ->
                    GlowButton(label) { input += insert }
                }
            }
            Insight("Named objects", vm.notebookDocument.symbolNames().joinToString().ifBlank { "None yet" }, Violet)
            Insight("Revision", vm.notebookDocument.revision.toString(), Green)
            Text("Cell references use #1, #2… and must point backward. Function cells can be sent directly to Graph.", color = Muted, fontSize = 11.sp)
        }
    }

    @Composable
    fun CellsPanel(modifier: Modifier = Modifier) {
        GlassPanel(modifier.semantics { contentDescription = "Reactive maths notebook cells" }) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Live cells", color = Violet, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("${vm.notebookDocument.cells.size} cells", color = Muted, fontSize = 11.sp)
            }
            if (vm.notebookDocument.cells.isEmpty()) {
                Text("Run a value, dependent value, or function. Try a := 2, then b := a^2 + 3.", color = Muted)
            }
            vm.notebookDocument.cells.forEachIndexed { index, cell ->
                val accent = when { cell.error != null -> Amber; cell.kind == NotebookCellKind.Function -> Violet; cell.kind == NotebookCellKind.Scalar -> Cyan; else -> Green }
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0x44101824))
                        .border(1.dp, accent.copy(.65f), RoundedCornerShape(14.dp)).padding(11.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("#${index + 1} · ${cell.kind.label}${cell.symbol?.let { " · $it" } ?: ""}", color = accent, fontWeight = FontWeight.Bold)
                        GlowButton("Delete") { vm.removeNotebookCell(cell.id) }
                    }
                    Text(cell.input, color = Ink, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                    if (cell.error != null) {
                        Text(cell.error, color = Amber, fontSize = 12.sp)
                    } else {
                        val output = if (exactMode) cell.exactOutput else cell.decimalOutput ?: cell.exactOutput
                        Text("= $output", color = Green, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        if (cell.dependencies.isNotEmpty()) Text("Depends on: ${cell.dependencies.joinToString()}", color = Muted, fontSize = 10.sp)
                        if (cell.graphExpression != null) GlowButton("Send ${cell.symbol}(x) to Graph") { vm.graphNotebookFunction(cell) }
                    }
                }
            }
        }
    }

    @Composable
    fun CasPanel(modifier: Modifier = Modifier) {
        GlassPanel(modifier.semantics { contentDescription = "CAS rows with exact and decimal output" }) {
            PanelHeader("CAS Rows", { vm.open(vm.state.module) }, Violet, icon = "CAS")
            Text("Typed AST foundation: simplify, expand and factor share one symbolic tree boundary.", color = Muted, fontSize = 12.sp)
            OutlinedTextField(
                value = casInput,
                onValueChange = { casInput = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("CAS expression") },
                visualTransformation = MathSyntaxVisualTransformation(),
                singleLine = true,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("simplify", "expand", "factor").forEach { operation ->
                    GlowButton(if (casOperation == operation) "• $operation" else operation) { casOperation = operation }
                }
                GlowButton("sub x=2") {
                    casInput = cas.substitute(casInput, mapOf("x" to "2")).exact
                    casOperation = "simplify"
                }
            }
            Insight("Operation", casRow.operation, Cyan)
            Insight("Exact", casRow.exact, if (casRow.supported) Green else Amber)
            casRow.decimal?.let { Insight("Decimal", it, Violet) }
            Insight("Assumptions", casRow.assumptions.joinToString().ifBlank { "none" }, Amber)
            casRow.steps.take(4).forEachIndexed { index, step ->
                Text("${index + 1}. ${step.title}: ${step.expression}", color = Ink, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                Text(step.explanation, color = Muted, fontSize = 10.sp)
            }
        }
    }

    if (wide) {
        Row(Modifier.fillMaxSize().padding(top = 68.dp, bottom = 66.dp)) {
            InputPanel(Modifier.weight(.34f).fillMaxHeight())
            CellsPanel(Modifier.weight(.36f).fillMaxHeight())
            CasPanel(Modifier.weight(.30f).fillMaxHeight())
        }
    } else {
        Column(Modifier.fillMaxSize().padding(top = 68.dp, bottom = 66.dp).verticalScroll(rememberScrollState())) {
            InputPanel(Modifier.fillMaxWidth())
            CellsPanel(Modifier.fillMaxWidth())
            CasPanel(Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun MathKnowledgeScreen(vm: ExplorerViewModel, wide: Boolean) {
    var query by remember { mutableStateOf("") }
    var topic by remember { mutableStateOf<KnowledgeTopic?>(null) }
    var level by remember { mutableStateOf<KnowledgeLevel?>(null) }
    var formulaCategory by remember { mutableStateOf<FormulaCategory?>(null) }
    var answers by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var quizSubject by remember { mutableStateOf(QuizSubject.Maths) }
    var quizLevel by remember { mutableStateOf(QuizLevel.Basic) }
    var quizSession by remember { mutableStateOf<QuizSession?>(null) }
    val visualProofEngine = remember { VisualProofEngine() }
    var visualProofPlayback by remember { mutableStateOf(visualProofEngine.start(VisualProofCatalog.labs.first().id)) }
    LaunchedEffect(visualProofPlayback.playing) {
        while (visualProofPlayback.playing) {
            delay(850)
            visualProofPlayback = visualProofEngine.next(visualProofPlayback)
        }
    }
    val result = remember(query, topic, level, formulaCategory) { MathKnowledgeCatalog.search(query, topic, level, formulaCategory) }

    @Composable
    fun Filters() {
        Text("Knowledge Intelligence", color = Cyan, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Formula library, theorem memory, visual proof plans, dictionary and adaptive MCQs run offline.", color = Muted, fontSize = 12.sp)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Knowledge search input" },
            label = { Text("Search maths knowledge") },
            placeholder = { Text("Try derivative, normal, Bayes, eigenvector") },
            singleLine = true,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            GlowButton(if (topic == null) "All topics" else "All") { topic = null }
            KnowledgeTopic.entries.forEach { item -> GlowButton(if (topic == item) "• ${item.label}" else item.label) { topic = item } }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            GlowButton(if (level == null) "All levels" else "All") { level = null }
            KnowledgeLevel.entries.forEach { item -> GlowButton(if (level == item) "• ${item.label}" else item.label) { level = item } }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            KnowledgeSection.entries.forEach { section ->
                GlowButton(if (vm.activeKnowledgeSection == section) "• ${section.title}" else section.title) {
                    vm.openKnowledgeHub(section)
                }
            }
        }
        AnimatedVisibility(vm.activeKnowledgeSection == KnowledgeSection.Formulas || vm.activeKnowledgeSection == KnowledgeSection.Visualize) {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("Formula categories", color = Ink, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    GlowButton(if (formulaCategory == null) "• All 15" else "All 15") { formulaCategory = null }
                    FormulaCategory.entries.forEach { category ->
                        GlowButton(if (formulaCategory == category) "• ${category.label}" else category.label) { formulaCategory = category }
                    }
                }
            }
        }
        Insight("Matches", "${result.total}", Green)
        Insight("Coverage", "${FormulaCategory.entries.size} formula categories · ${MathKnowledgeCatalog.formulas.size} formulas", Violet)
    }

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        GlassPanel(modifier.fillMaxSize().semantics { contentDescription = "Maths knowledge content" }) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(vm.activeKnowledgeSection.title, color = Violet, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("${result.total} found", color = Muted, fontSize = 11.sp)
            }
            when (vm.activeKnowledgeSection) {
                KnowledgeSection.Formulas -> result.formulas.forEach { formula ->
                    KnowledgeCard(formula.title, formula.expression, "${formula.category.label} · ${formula.level.label}", formula.useCase, Cyan)
                    if (formula.variables.isNotEmpty()) Text("Variables: ${formula.variables.joinToString()}", color = Muted, fontSize = 11.sp)
                }
                KnowledgeSection.Theorems -> result.theorems.forEach { theorem ->
                    KnowledgeCard(theorem.title, theorem.statement, "${theorem.topic.label} · ${theorem.level.label}", "Applications: ${theorem.applications.joinToString()}", Violet)
                    Text("Proof sketch: ${theorem.proofSketch.joinToString(" -> ")}", color = Muted, fontSize = 11.sp)
                }
                KnowledgeSection.Visualize -> {
                    result.formulas.take(5).forEach { formula ->
                        KnowledgeCard(formula.title, formula.expression, "Visual formula", "Open graph/notebook and connect variables: ${formula.variables.joinToString()}", Green)
                    }
                    result.visualProofs.forEach { proof ->
                        KnowledgeCard(proof.title, proof.invariant, proof.workspace.label, proof.learnerPrompt, Cyan)
                    }
                }
                KnowledgeSection.Proofs -> {
                    Text("10 runnable proof labs", color = Cyan, fontWeight = FontWeight.Bold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        VisualProofCatalog.labs.forEach { lab ->
                            GlowButton(if (visualProofPlayback.frame.lab.id == lab.id) "• ${lab.title}" else lab.title) { visualProofPlayback = visualProofEngine.start(lab.id) }
                        }
                    }
                    KnowledgeCard(visualProofPlayback.frame.lab.title, visualProofPlayback.frame.lab.formalResult, visualProofPlayback.frame.lab.topic, visualProofPlayback.frame.lab.steps[visualProofPlayback.frame.step], Green)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        GlowButton(if (visualProofPlayback.playing) "Pause" else "Play") { visualProofPlayback = visualProofEngine.togglePlaying(visualProofPlayback) }
                        GlowButton("Next step") { visualProofPlayback = visualProofEngine.next(visualProofPlayback) }
                        GlowButton("Open tactile lab") { vm.open(MathModule.Manipulatives) }
                    }
                    visualProofPlayback.frame.lab.parameters.forEach { parameter ->
                        AxisSlider(parameter.name, visualProofPlayback.frame.parameters.getValue(parameter.name).toFloat(), parameter.minimum.toFloat()..parameter.maximum.toFloat()) {
                            visualProofPlayback = visualProofEngine.setParameter(visualProofPlayback, parameter.name, it.toDouble())
                        }
                    }
                    visualProofPlayback.frame.measurements.forEach { (name, value) -> Insight(name, trim(value), Violet) }
                    Insight("Invariant", "${visualProofPlayback.frame.invariant} · residual ${trim(visualProofPlayback.frame.residual)}", if (visualProofPlayback.frame.holds) Green else Amber)
                    Text("What changes? ${visualProofPlayback.frame.lab.changesPrompt}", color = Cyan, fontSize = 12.sp)
                    Text("What stays same? ${visualProofPlayback.frame.lab.invariantPrompt}", color = Green, fontSize = 12.sp)
                }
                KnowledgeSection.Dictionary -> result.dictionary.forEach { term ->
                    KnowledgeCard(term.term, term.definition, "${term.topic.label} · ${term.level.label}", "${term.notation}: ${term.example}", Amber)
                }
                KnowledgeSection.Mcqs -> {
                    QuizDashboard(
                        subject = quizSubject,
                        level = quizLevel,
                        session = quizSession,
                        questionBankSize = MathKnowledgeCatalog.mcqs.size,
                        onSubject = { quizSubject = it; quizSession = null },
                        onLevel = { quizLevel = it; quizSession = null },
                        onStart = {
                            answers = emptyMap()
                            quizSession = QuizEngine.start(MathKnowledgeCatalog.mcqs, quizSubject, quizLevel)
                        },
                        onAnswer = { choice ->
                            quizSession = quizSession?.let { QuizEngine.answer(it, choice) }
                        },
                        onRestart = {
                            answers = emptyMap()
                            quizSession = QuizEngine.start(MathKnowledgeCatalog.mcqs, quizSubject, quizLevel)
                        },
                    )
                }
            }
            if (result.total == 0) Text("No matches yet. Clear filters or search a broader term.", color = Amber)
        }
    }

    if (wide) {
        Row(
            Modifier.fillMaxSize().padding(top = 74.dp, bottom = 72.dp, start = 12.dp, end = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GlassPanel(Modifier.weight(.36f).fillMaxHeight()) { Filters() }
            Content(Modifier.weight(.64f).fillMaxHeight())
        }
    } else {
        Column(
            Modifier.fillMaxSize().padding(top = 70.dp, bottom = 70.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GlassPanel(Modifier.fillMaxWidth()) { Filters() }
            Content(Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun KnowledgeCard(title: String, body: String, meta: String, detail: String, accent: Color) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x33101824))
            .border(1.dp, accent.copy(.55f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, color = accent, fontWeight = FontWeight.Bold)
        Text(body, color = Ink, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
        Text(meta, color = Muted, fontSize = 11.sp)
        Text(detail, color = Ink, fontSize = 12.sp)
    }
}

@Composable
private fun QuizDashboard(
    subject: QuizSubject,
    level: QuizLevel,
    session: QuizSession?,
    questionBankSize: Int,
    onSubject: (QuizSubject) -> Unit,
    onLevel: (QuizLevel) -> Unit,
    onStart: () -> Unit,
    onAnswer: (Int) -> Unit,
    onRestart: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Interactive MCQ Quiz", color = Cyan, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("15 questions per quiz · score, progress and explanations", color = Muted, fontSize = 12.sp)
            }
            TransparentIcon("Q", Violet)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            QuizSubject.entries.forEach { option ->
                GlowButton(if (subject == option) "• ${option.label}" else option.label) { onSubject(option) }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            QuizLevel.entries.forEach { option ->
                GlowButton(if (level == option) "• ${option.label}" else option.label) { onLevel(option) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            GlowButton(if (session == null) "Start 15-question quiz" else "Restart quiz", onClick = if (session == null) onStart else onRestart)
            Insight("Question bank", "$questionBankSize MCQs", Green)
        }
        if (session == null) {
            QuizIntroCard(subject, level)
        } else {
            QuizSessionCard(session, onAnswer, onRestart)
        }
    }
}

@Composable
private fun QuizIntroCard(subject: QuizSubject, level: QuizLevel) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x33101824))
            .border(1.dp, Cyan.copy(.55f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("${subject.label} · ${level.label}", color = Cyan, fontWeight = FontWeight.Bold)
        Text("Choose Start to generate a focused 15-question quiz. Each answer locks instantly and shows a short explanation.", color = Muted, fontSize = 12.sp)
        Text("Scoring is local and reusable for Maths, Physics, Chemistry, Biology, Astro Physics and IQ Labs.", color = Ink, fontSize = 12.sp)
    }
}

@Composable
private fun QuizSessionCard(session: QuizSession, onAnswer: (Int) -> Unit, onRestart: () -> Unit) {
    val current = session.currentQuestion
    val lastAnswer = session.answers.lastOrNull()
    val lastQuestion = lastAnswer?.let { answer -> session.questions.firstOrNull { it.id == answer.questionId } }
    val progress = if (session.questions.isEmpty()) 0f else session.answers.size.toFloat() / session.questions.size
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x44101824))
            .border(1.dp, (if (session.completed) Green else Violet).copy(.58f), RoundedCornerShape(14.dp))
            .padding(12.dp)
            .semantics { contentDescription = "Interactive MCQ quiz score ${session.score} of ${session.questions.size}" },
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("${session.subject.label} · ${session.level.label}", color = Cyan, fontWeight = FontWeight.Bold)
            Text("${session.score}/${session.questions.size}", color = Green, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        }
        Canvas(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(8.dp))) {
            drawRect(Muted.copy(.18f), Offset.Zero, size)
            drawRect(Green.copy(.8f), Offset.Zero, Size(size.width * progress.coerceIn(0f, 1f), size.height))
        }
        Text("Question ${(session.currentIndex + 1).coerceAtMost(session.questions.size)} of ${session.questions.size}", color = Muted, fontSize = 11.sp)
        if (session.completed) {
            Text("Quiz complete", color = Green, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Score ${session.score}/${session.questions.size} · ${session.percent}%", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            val message = when {
                session.percent >= 85 -> "Excellent. Ready for the next level."
                session.percent >= 60 -> "Good progress. Review the missed explanations."
                else -> "Practice mode recommended. Restart and focus on explanations."
            }
            Text(message, color = Muted, fontSize = 12.sp)
            GlowButton("Try another quiz", onClick = onRestart)
        } else if (current != null) {
            Text(current.category, color = Amber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(current.prompt, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            current.choices.forEachIndexed { index, choice ->
                GlowButton("${('A'.code + index).toChar()}. $choice") { onAnswer(index) }
            }
        }
        if (lastAnswer != null && lastQuestion != null) {
            val correctText = if (lastAnswer.correct) "Correct" else "Review: ${lastQuestion.choices[lastQuestion.answerIndex]}"
            Text(correctText, color = if (lastAnswer.correct) Green else Amber, fontWeight = FontWeight.Bold)
            Text(lastQuestion.explanation, color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun McqCard(question: McqQuestion, selected: Int?, onSelect: (Int) -> Unit) {
    val checked = selected?.let(question::check)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x33101824))
            .border(1.dp, (if (checked?.correct == true) Green else Cyan).copy(.55f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(question.prompt, color = Ink, fontWeight = FontWeight.Bold)
        question.choices.forEachIndexed { index, choice ->
            GlowButton(if (selected == index) "• $choice" else choice) { onSelect(index) }
        }
        checked?.let {
            Text("${it.message} · next difficulty ${it.nextDifficulty}", color = if (it.correct) Green else Amber, fontWeight = FontWeight.SemiBold)
            Text(it.explanation, color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ProblemSolverScreen(vm: ExplorerViewModel, wide: Boolean) {
    val tutor = remember { MathSolverTutor() }
    var question by remember { mutableStateOf(TextFieldValue("Solve 2x + 3 = 11")) }
    var guided by remember { mutableStateOf<GuidedSolution?>(null) }
    var selectedMethod by remember { mutableStateOf(SolverMethod.Auto) }
    var reveal by remember { mutableStateOf(SolverReveal.FirstHint) }
    var revealedSteps by remember { mutableIntStateOf(1) }
    var whyStep by remember { mutableIntStateOf(-1) }
    var learnerWork by remember { mutableStateOf("") }
    var selectedResultForm by remember { mutableStateOf(SolverResultKind.Exact) }
    var showMathKeyboard by remember { mutableStateOf(true) }
    val syntax = remember(question.text) { MathInputIntelligence.analyze(question.text) }
    val syntaxHighlighting = remember { MathSyntaxVisualTransformation() }
    val examples = listOf(
        "Linear" to "Solve 2x + 3 = 11",
        "Quadratic" to "x^2 - 5x + 6 = 0",
        "Inequality" to "Solve x^2 - 5x + 6 <= 0",
        "Series" to "Arithmetic series a=3 d=2 n=10 sum",
        "Product rule" to "Differentiate x*sin(x)",
        "Chain rule" to "Differentiate sin(x^2)",
        "Partial" to "Partial derivative of x^2*y + sin(y) with respect to x",
        "Integral" to "Integrate sin(2x) with respect to x",
        "Definite" to "Integrate sin(x) from 0 to pi",
        "Statistics" to "Mean of 4, 7, 7, 10",
        "Exact fraction" to "Calculate 1/3 + 1/6",
        "Expand" to "Expand (x + 2)(x - 3)",
        "Factor" to "Factor x^2 - 5x + 6",
        "Matrix" to "Inverse [[1,2],[3,4]]",
        "Units" to "Convert 5 km to m",
        "Limit" to "Limit (x^2 - 9)/(x - 3) as x -> 3",
        "Maclaurin" to "Maclaurin series of cos(x) through order 8",
        "Combinations" to "Combination 10 C 3",
        "Finance" to "Compound interest principal 10000 rate 8% time 3",
        "ODE" to "Solve differential equation dy/dx = 3y",
        "Initial value ODE" to "Solve differential equation dy/dx = 2y + 4, y(0)=3",
        "Word model" to "A rectangle has length 8 and width 5. Find its area",
    )

    fun solveNow() {
        guided = tutor.solve(question.text, selectedMethod)
        reveal = SolverReveal.FirstHint
        revealedSteps = 1
        whyStep = -1
    }

    fun insertMath(snippet: String) {
        val start = question.selection.min.coerceIn(0, question.text.length)
        val end = question.selection.max.coerceIn(start, question.text.length)
        val selected = question.text.substring(start, end)
        val (insertion, cursorOffset) = when {
            snippet.endsWith("(") && selected.isNotEmpty() -> "$snippet$selected)" to (snippet.length + selected.length + 1)
            snippet.endsWith("(") -> "$snippet)" to snippet.length
            snippet == "^2" && selected.isNotEmpty() -> "($selected)^2" to (selected.length + 4)
            snippet == "Differentiate " && selected.isNotEmpty() -> "Differentiate $selected".let { it to it.length }
            snippet == "Integrate " && selected.isNotEmpty() -> "Integrate $selected with respect to x".let { it to it.length }
            else -> snippet to snippet.length
        }
        val updated = question.text.replaceRange(start, end, insertion)
        question = TextFieldValue(updated, TextRange((start + cursorOffset).coerceAtMost(updated.length)))
    }

    @Composable
    fun QuestionPanel(modifier: Modifier = Modifier) {
        GlassPanel(modifier) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Intelligent Maths Kernel", color = Cyan, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                TransparentIcon("AI", Violet)
            }
            Text(
                "Ask in words or notation. The solver classifies, derives, calculates, and verifies each answer.",
                color = Muted,
                fontSize = 13.sp,
            )
            SolverCapabilityGrid()
            Text("Choose a method", color = Ink, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SolverMethod.entries.forEach { method ->
                    GlowButton(if (selectedMethod == method) "• ${method.label}" else method.label) {
                        selectedMethod = method
                        if (guided != null) solveNow()
                    }
                }
            }
            OutlinedTextField(
                value = question,
                onValueChange = { question = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 104.dp)
                    .semantics { contentDescription = "Maths question input" },
                label = { Text("Maths question") },
                placeholder = { Text("Example: solve x^2 - 5x + 6 = 0") },
                minLines = 3,
                visualTransformation = syntaxHighlighting,
                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 16.sp, color = Ink),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(syntax.message, color = if (syntax.validBrackets) Green else Amber, fontSize = 11.sp, modifier = Modifier.weight(1f))
                GlowButton(if (showMathKeyboard) "Hide editor" else "Smart editor") { showMathKeyboard = !showMathKeyboard }
            }
            AnimatedVisibility(showMathKeyboard) {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        listOf("sin(" to "sin(", "cos(" to "cos(", "tan(" to "tan(", "√" to "sqrt(", "ln(" to "ln(", "eˣ" to "exp(", "x" to "x", "y" to "y", "x²" to "^2", "+" to "+", "−" to "-", "÷" to "/", "π" to "pi", "∫" to "Integrate ", "d/dx" to "Differentiate ", "bounds" to " from 0 to pi").forEach { (label, snippet) ->
                            GlowButton(label) { insertMath(snippet) }
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        SyntaxLegend("123", Cyan)
                        SyntaxLegend("sin cos", Violet)
                        SyntaxLegend("x y", Green)
                        SyntaxLegend("pi e", Amber)
                        SyntaxLegend("+ - =", Ink)
                    }
                    syntax.suggestions.firstOrNull()?.let { Text("AI hint · $it", color = Violet, fontSize = 11.sp) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlowButton("Start with a hint", enabled = question.text.isNotBlank(), onClick = ::solveNow)
                GlowButton("Workspaces") { vm.open(vm.state.module) }
            }
            Text("Try an example", color = Ink, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                examples.forEach { (label, example) ->
                    GlowButton(label) {
                        question = TextFieldValue(example, TextRange(example.length))
                        guided = tutor.solve(example, selectedMethod)
                        reveal = SolverReveal.FirstHint
                        revealedSteps = 1
                    }
                }
            }
            Insight("Runs", "On-device maths kernel", Green)
            Insight("Answers", "Derived + verified", Violet)
        }
    }

    @Composable
    fun ResultPanel(result: GuidedSolution?, modifier: Modifier = Modifier) {
        GlassPanel(modifier.semantics { contentDescription = "Step by step maths solution" }) {
            if (result == null) {
                Text("Your solution will appear here", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Choose an example or enter a question, then tap Solve step by step.", color = Muted)
                Insight("Current coverage", "Exact CAS · matrices · units · calculus · inequalities · series · data", Cyan)
                Insight("Safety", "No invented unsupported answers", Amber)
            } else {
                val solution = result.solution
                SolverAnswerSummary(solution, if (reveal == SolverReveal.Answer) solution.answer else "Answer hidden · use the hints")
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Cyan.copy(.08f))
                        .border(1.dp, Cyan.copy(.45f), RoundedCornerShape(14.dp)).padding(11.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("How I interpreted it", color = Cyan, fontWeight = FontWeight.Bold)
                    Insight("Intent", result.interpretation.selected.intent.label, Violet)
                    Text(result.interpretation.selected.normalizedQuery, color = Ink, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                    Text("Confidence ${trim(result.interpretation.selected.confidence * 100)}% · ${result.interpretation.status.name}", color = Muted, fontSize = 11.sp)
                    result.interpretation.assumptions.forEach { Text("Assumption · $it", color = Amber, fontSize = 11.sp) }
                    result.interpretation.ambiguities.forEach { Text("Ambiguity · $it", color = Amber, fontWeight = FontWeight.SemiBold, fontSize = 11.sp) }
                    result.interpretation.alternatives.forEach { Text("Possible reading · ${it.normalizedQuery}", color = Violet, fontSize = 11.sp) }
                }
                Insight("Method", result.method.label, Violet)
                Text(result.methodReason, color = Muted, fontSize = 12.sp)
                if (result.alternatives.isNotEmpty()) {
                    Text("Try another verified method", color = Ink, fontWeight = FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        result.alternatives.forEach { alternate ->
                            GlowButton("${alternate.method.label} · ${alternate.stepCount} steps") {
                                selectedMethod = alternate.method
                                guided = tutor.solve(question.text, alternate.method)
                                reveal = SolverReveal.FirstHint
                                revealedSteps = 1
                            }
                        }
                    }
                }
                result.wordModel?.let { model ->
                    Text("Word-problem model", color = Cyan, fontWeight = FontWeight.Bold)
                    model.quantities.forEach { quantity ->
                        Insight(quantity.name, quantity.value?.let { "$it ${quantity.unit.orEmpty()}".trim() } ?: "unknown (${quantity.symbol})", if (quantity.unknown) Amber else Green)
                    }
                    model.relationships.forEach { Text("Relationship · $it", color = Ink, fontSize = 12.sp) }
                    model.equations.forEach { Text("Setup · $it", color = Violet, fontWeight = FontWeight.SemiBold) }
                    model.ambiguity.forEach { Text("Clarify · $it", color = Amber, fontSize = 12.sp) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GlowButton("Next step", enabled = solution.steps.isNotEmpty() && revealedSteps < solution.steps.size) {
                        reveal = SolverReveal.Steps
                        revealedSteps++
                    }
                    GlowButton("Reveal method") { reveal = SolverReveal.Method }
                    GlowButton("Reveal answer") { reveal = SolverReveal.Answer }
                }
                result.visibleSteps(reveal, revealedSteps).forEachIndexed { index, item ->
                    val accent = when (item.role) {
                        SolutionStepRole.Interpret -> Cyan
                        SolutionStepRole.Transform -> Violet
                        SolutionStepRole.Calculate -> Green
                        SolutionStepRole.Verify -> Amber
                    }
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x33101824))
                            .border(1.dp, accent.copy(.55f), RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text("${index + 1}. ${item.title}", color = accent, fontWeight = FontWeight.Bold)
                        Text(item.expression, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text(item.explanation, color = Muted, fontSize = 12.sp)
                        GlowButton(if (whyStep == index) "Hide why" else "Why?") { whyStep = if (whyStep == index) -1 else index }
                        if (whyStep == index) Text(result.why(index), color = Cyan, fontSize = 12.sp)
                    }
                }
                if (reveal == SolverReveal.Answer) {
                    Text("Answer forms", color = Cyan, fontWeight = FontWeight.Bold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        result.resultForms.forEach { form ->
                            GlowButton(if (selectedResultForm == form.kind) "• ${form.kind.label}" else form.kind.label, enabled = form.available) { selectedResultForm = form.kind }
                        }
                    }
                    result.resultForms.firstOrNull { it.kind == selectedResultForm }?.let { form ->
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Violet.copy(.09f)).padding(10.dp)) {
                            Text(form.value, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(form.provenance, color = Muted, fontSize = 11.sp)
                        }
                    }
                    Text("Verification", color = Amber, fontWeight = FontWeight.Bold)
                    Text(solution.verification, color = Ink, fontSize = 13.sp)
                    solution.warnings.forEach { Text("• $it", color = Amber, fontSize = 12.sp) }
                    Text("Send result without retyping", color = Cyan, fontWeight = FontWeight.Bold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        GlowButton("Graph", enabled = result.handoffs.any { it.destination == SolverDestination.Graph && it.enabled }) { vm.sendSolverToGraph(result) }
                        GlowButton("Spreadsheet") { vm.sendSolverToTable(result) }
                        GlowButton("Notebook") { vm.saveSolverToNotebook(result) }
                        GlowButton("Generate MCQ") { learnerWork = "Practice generated · ${solution.question} · verify against the solved example" }
                    }
                }
                Text("Check my working", color = Ink, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = learnerWork,
                    onValueChange = { learnerWork = it },
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Learner working for mistake diagnosis" },
                    label = { Text("Paste or type your steps") },
                    minLines = 2,
                )
                tutor.diagnose(solution.question, learnerWork).forEach { diagnosis ->
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Amber.copy(.1f)).padding(9.dp)) {
                        Text(diagnosis.kind.label, color = Amber, fontWeight = FontWeight.Bold)
                        Text(diagnosis.message, color = Ink, fontSize = 12.sp)
                        Text(diagnosis.correction, color = Muted, fontSize = 11.sp)
                    }
                }
            }
        }
    }

    if (wide) {
        Row(
            Modifier.fillMaxSize().padding(top = 78.dp, bottom = 76.dp, start = 12.dp, end = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            QuestionPanel(Modifier.weight(.42f).fillMaxHeight())
            ResultPanel(guided, Modifier.weight(.58f).fillMaxHeight().verticalScroll(rememberScrollState()))
        }
    } else {
        Column(
            Modifier.fillMaxSize().padding(top = 70.dp, bottom = 70.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QuestionPanel(Modifier.fillMaxWidth())
            ResultPanel(guided, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun SolverCapabilityGrid() {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(
            "CAS" to Cyan,
            "Calculus" to Violet,
            "Inequalities" to Green,
            "Series" to Amber,
            "Matrices" to Cyan,
            "Stats" to Violet,
            "Natural input" to Green,
            "ODE & limits" to Amber,
        ).forEach { (label, color) ->
            Row(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(.12f))
                    .border(1.dp, color.copy(.55f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 9.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                TransparentIcon(label.take(1), color)
                Text(label, color = Ink, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SolverAnswerSummary(result: ProblemSolution, displayedAnswer: String = result.answer) {
    val accent = if (result.supported) Green else Amber
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x44101824))
            .border(1.dp, accent.copy(.65f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(result.kind.label, color = Cyan, fontWeight = FontWeight.Bold)
                Text(if (result.supported) "Derived answer" else "Needs clearer input", color = Muted, fontSize = 11.sp)
            }
            Text(if (result.supported) "${(result.confidence * 100).toInt()}%" else "?", color = accent, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        }
        Text(displayedAnswer, color = if (result.supported) Ink else Amber, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(8.dp))
                .semantics { contentDescription = "Solver confidence ${(result.confidence * 100).toInt()} percent" },
        ) {
            drawRect(Muted.copy(.18f), Offset.Zero, size)
            drawRect(accent.copy(.82f), Offset.Zero, Size(size.width * result.confidence.toFloat().coerceIn(0f, 1f), size.height))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            SolverMetric("Steps", result.steps.size.toString(), Violet)
            SolverMetric("Verify", if (result.verification.isBlank()) "none" else "yes", Amber)
            SolverMetric("Mode", if (result.supported) "offline" else "clarify", Cyan)
        }
    }
}

@Composable
private fun SolverMetric(label: String, value: String, accent: Color) {
    Column(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(accent.copy(.1f))
            .border(1.dp, accent.copy(.35f), RoundedCornerShape(8.dp))
            .padding(horizontal = 9.dp, vertical = 6.dp),
    ) {
        Text(label, color = Muted, fontSize = 9.sp)
        Text(value, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ProbabilityLabScreen(vm: ExplorerViewModel, wide: Boolean) {
    var section by remember { mutableStateOf(ProbabilityLabSection.Distributions) }
    var kind by remember { mutableStateOf(DistributionKind.Normal) }
    var first by remember { mutableFloatStateOf(0f) }
    var second by remember { mutableFloatStateOf(1f) }
    var lower by remember { mutableFloatStateOf(-1f) }
    var upper by remember { mutableFloatStateOf(1f) }

    if (section == ProbabilityLabSection.Statistics) {
        StatisticsLabScreen(vm, wide, onSection = { section = it })
        return
    }
    if (section == ProbabilityLabSection.Spreadsheet) {
        SpreadsheetLabScreen(vm, wide, onSection = { section = it })
        return
    }
    if (section == ProbabilityLabSection.Experiments) {
        ProbabilityExperimentsScreen(vm, wide, onSection = { section = it })
        return
    }
    if (section == ProbabilityLabSection.Learn) {
        StatisticsMaterialsScreen(vm, wide, onSection = { section = it })
        return
    }

    fun select(next: DistributionKind) {
        kind = next
        when (next) {
            DistributionKind.Normal -> { first = 0f; second = 1f; lower = -1f; upper = 1f }
            DistributionKind.Binomial -> { first = 10f; second = .5f; lower = 3f; upper = 7f }
            DistributionKind.Poisson -> { first = 4f; second = 1f; lower = 1f; upper = 6f }
            DistributionKind.Uniform -> { first = 0f; second = 10f; lower = 2f; upper = 7f }
            DistributionKind.Exponential -> { first = 1f; second = 1f; lower = 0f; upper = 2f }
        }
    }

    val distribution = remember(kind, first, second) {
        runCatching { DistributionEngine.create(kind, first.toDouble(), second.toDouble()) }.getOrNull()
    }
    val probability = distribution?.let { runCatching { it.probabilityBetween(minOf(lower, upper).toDouble(), maxOf(lower, upper).toDouble()) }.getOrNull() }

    @Composable
    fun Controls(modifier: Modifier = Modifier) {
        GlassPanel(modifier) {
            PanelHeader("Probability & Distributions", { vm.open(MathModule.Graph2D) }, Cyan, icon = "σ")
            ProbabilitySectionSelector(section) { section = it }
            Text("Choose a validated model, adjust its parameters, and inspect interval probability, density, CDF, and quantiles.", color = Muted, fontSize = 12.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                DistributionKind.entries.forEach { option -> GlowButton(if (kind == option) "• ${option.name}" else option.name) { select(option) } }
            }
            when (kind) {
                DistributionKind.Normal -> {
                    AxisSlider("Mean μ", first, -10f..10f) { first = it }
                    AxisSlider("Std dev σ", second, .1f..8f) { second = it }
                }
                DistributionKind.Binomial -> {
                    AxisSlider("Trials n", first, 1f..100f) { first = it.roundToInt().toFloat() }
                    AxisSlider("Success p", second, 0f..1f) { second = it }
                }
                DistributionKind.Poisson -> AxisSlider("Rate λ", first, .1f..30f) { first = it }
                DistributionKind.Uniform -> {
                    AxisSlider("Minimum a", first, -10f..9f) { first = minOf(it, second - .1f) }
                    AxisSlider("Maximum b", second, -9f..10f) { second = maxOf(it, first + .1f) }
                }
                DistributionKind.Exponential -> AxisSlider("Rate λ", first, .1f..10f) { first = it }
            }
            AxisSlider("Interval low", lower, -10f..30f) { lower = minOf(it, upper) }
            AxisSlider("Interval high", upper, -10f..30f) { upper = maxOf(it, lower) }
            Text("P(${trim(lower.toDouble())} ≤ X ≤ ${trim(upper.toDouble())}) = ${probability?.let(::trim) ?: "invalid"}", color = Green, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }

    @Composable
    fun Visualization(model: ProbabilityDistribution?, modifier: Modifier = Modifier) {
        GlassPanel(modifier.semantics { contentDescription = "Interactive probability distribution plot" }) {
            Text("${kind.name} distribution", color = Violet, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            if (model == null) {
                Text("Adjust the parameters to create a valid distribution.", color = Amber)
            } else {
                val points = remember(model) { model.plotPoints() }
                DistributionPlot(points, lower.toDouble(), upper.toDouble(), model.summary.domain == com.indianservers.aiexplorer.core.DistributionDomain.Discrete)
                Insight("Domain", model.summary.domain.name.lowercase(), Cyan)
                Insight("Mean", trim(model.summary.mean), Green)
                Insight("Variance", trim(model.summary.variance), Violet)
                Insight("Standard deviation", trim(model.summary.standardDeviation), Amber)
                Insight("CDF at upper", trim(model.cumulative(upper.toDouble())), Cyan)
                Insight("Median (Q50)", trim(model.quantile(.5)), Green)
                Insight("90th percentile", trim(model.quantile(.9)), Violet)
                Text("Parameters: ${model.summary.parameters.entries.joinToString { "${it.key}=${trim(it.value)}" }}", color = Muted, fontSize = 12.sp)
            }
        }
    }

    if (wide) {
        Row(Modifier.fillMaxSize().padding(top = 78.dp, bottom = 76.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Controls(Modifier.weight(.44f).fillMaxHeight())
            Visualization(distribution, Modifier.weight(.56f).fillMaxHeight())
        }
    } else {
        Column(Modifier.fillMaxSize().padding(top = 68.dp, bottom = 66.dp).verticalScroll(rememberScrollState())) {
            Controls(Modifier.fillMaxWidth())
            Visualization(distribution, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ProbabilitySectionSelector(active: ProbabilityLabSection, onSelect: (ProbabilityLabSection) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ProbabilityLabSection.entries.forEach { section ->
            GlowButton(if (section == active) "• ${section.name}" else section.name) { onSelect(section) }
        }
    }
}

@Composable
private fun SpreadsheetLabScreen(vm: ExplorerViewModel, wide: Boolean, onSection: (ProbabilityLabSection) -> Unit) {
    val engine = remember { MathSpreadsheetEngine() }
    var csv by remember { mutableStateOf("Time,Value\n0,2\n1,4\n2,7\n3,11\n4,16") }
    var formula by remember { mutableStateOf("=MEAN(B1:B5)") }
    var missing by remember { mutableStateOf(MissingDataPolicy.Skip) }
    var exported by remember { mutableStateOf("") }
    val document = remember(csv, formula) { runCatching { engine.setCell(engine.importCsv(csv), com.indianservers.aiexplorer.core.SpreadsheetAddress(2, 0), formula) }.getOrNull() }
    val snapshot = remember(document, missing) { document?.let { engine.evaluate(it, missing) } }
    val linked = remember(snapshot) { snapshot?.let { runCatching { engine.linkedSeries(it, 0, 1) }.getOrNull() } }
    val regression = remember(linked) { linked?.points?.takeIf { it.size >= 3 }?.let { Phase4Statistics.linearRegression(it.map { p -> p.x }, it.map { p -> p.y }) } }

    @Composable fun Editor(modifier: Modifier = Modifier) {
        GlassPanel(modifier) {
            PanelHeader("Spreadsheet & Lists", { vm.open(MathModule.Graph2D) }, Cyan, icon = "▦")
            ProbabilitySectionSelector(ProbabilityLabSection.Spreadsheet, onSection)
            Text("Editable CSV grid · A1 formulas · named lists · linked analysis", color = Muted, fontSize = 12.sp)
            OutlinedTextField(value = csv, onValueChange = { csv = it }, label = { Text("CSV data") }, minLines = 7, modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Spreadsheet CSV editor" })
            OutlinedTextField(value = formula, onValueChange = { formula = it }, label = { Text("Formula in C1") }, modifier = Modifier.fillMaxWidth())
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                MissingDataPolicy.entries.forEach { policy -> GlowButton(if (missing == policy) "• ${policy.name}" else policy.name) { missing = policy } }
                GlowButton("Export CSV") { snapshot?.let { exported = engine.exportCsv(it) } }
            }
            if (exported.isNotBlank()) Text(exported, color = Green, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
            snapshot?.evaluated?.entries?.sortedWith(compareBy({ it.key.row }, { it.key.column }))?.take(18)?.forEach { (address, cell) ->
                Insight(address.a1, cell.value?.let(::trim) ?: cell.error ?: "missing", if (cell.error == null) Cyan else Amber)
            }
        }
    }

    @Composable fun Analysis(modifier: Modifier = Modifier) {
        GlassPanel(modifier) {
            Text("Linked plot & statistics", color = Violet, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Every edit rebuilds this series and its model.", color = Muted, fontSize = 12.sp)
            linked?.let { series ->
                Canvas(Modifier.fillMaxWidth().height(230.dp).clip(RoundedCornerShape(14.dp)).background(Color(0x6600060D)).semantics { contentDescription = "Spreadsheet linked scatter plot" }) {
                    if (series.points.isNotEmpty()) {
                        val minX = series.points.minOf { it.x }; val maxX = series.points.maxOf { it.x }.takeIf { it > minX } ?: minX + 1
                        val minY = series.points.minOf { it.y }; val maxY = series.points.maxOf { it.y }.takeIf { it > minY } ?: minY + 1
                        fun point(value: Vec2) = Offset(((value.x - minX) / (maxX - minX) * size.width).toFloat(), (size.height - (value.y - minY) / (maxY - minY) * size.height).toFloat())
                        series.points.zipWithNext().forEach { (a, b) -> drawLine(Cyan.copy(.7f), point(a), point(b), 3f) }
                        series.points.forEach { drawCircle(Violet, 8f, point(it)) }
                    }
                }
                Insight("Lists", "${series.xName} ↔ ${series.yName}", Cyan)
                Insight("Rows linked", series.points.size.toString(), Green)
                Insight("Revision", series.revision.toString(), Amber)
            }
            regression?.let { model ->
                Insight("Linear model", "y=${trim(model.coefficients[0])} + ${trim(model.coefficients[1])}x", Violet)
                Insight("R²", model.rSquared?.let(::trim) ?: "n/a", Green)
                model.diagnostics.forEach { Insight(it.name, it.detail, if (it.passed) Green else Amber) }
                Text("Assumptions: ${model.assumptions.joinToString()}", color = Muted, fontSize = 11.sp)
            }
        }
    }

    if (wide) Row(Modifier.fillMaxSize().padding(top = 78.dp, bottom = 76.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Editor(Modifier.weight(.48f).fillMaxHeight().verticalScroll(rememberScrollState())); Analysis(Modifier.weight(.52f).fillMaxHeight())
    } else Column(Modifier.fillMaxSize().padding(top = 68.dp, bottom = 66.dp).verticalScroll(rememberScrollState())) {
        Editor(Modifier.fillMaxWidth()); Analysis(Modifier.fillMaxWidth())
    }
}

@Composable
private fun ProbabilityExperimentsScreen(vm: ExplorerViewModel, wide: Boolean, onSection: (ProbabilityLabSection) -> Unit) {
    var kind by remember { mutableStateOf(RandomExperimentKind.Coin) }
    var trials by remember { mutableFloatStateOf(1_000f) }
    var seedText by remember { mutableStateOf("42") }
    var prior by remember { mutableFloatStateOf(.1f) }
    var sensitivity by remember { mutableFloatStateOf(.9f) }
    var falsePositive by remember { mutableFloatStateOf(.05f) }
    val seed = seedText.toLongOrNull() ?: 1L
    val result = remember(kind, trials, seed) { RandomExperimentEngine.simulate(kind, trials.toInt(), seed) }
    val pi = remember(trials, seed) { RandomExperimentEngine.monteCarloPi(trials.toInt(), seed) }
    val bayes = remember(prior, sensitivity, falsePositive) {
        ConditionalProbabilityEngine.bayes(
            mapOf("Condition" to prior.toDouble(), "No condition" to 1 - prior.toDouble()),
            mapOf("Condition" to sensitivity.toDouble(), "No condition" to falsePositive.toDouble()),
        )
    }
    val combinatorics = remember { CombinatoricsLab.calculate(10, 3) }
    GlassPanel(Modifier.fillMaxSize().padding(top = 74.dp, bottom = 70.dp, start = if (wide) 42.dp else 4.dp, end = if (wide) 42.dp else 4.dp).verticalScroll(rememberScrollState())) {
        PanelHeader("Probability Experiments", { vm.open(MathModule.Graph2D) }, Cyan, icon = "Dice")
        ProbabilitySectionSelector(ProbabilityLabSection.Experiments, onSection)
        Text("Seeded simulations are exactly reproducible.", color = Muted)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            RandomExperimentKind.entries.forEach { option -> GlowButton(if (kind == option) "• ${option.name}" else option.name) { kind = option } }
        }
        AxisSlider("Trials", trials, 100f..20_000f) { trials = it.roundToInt().toFloat() }
        OutlinedTextField(value = seedText, onValueChange = { seedText = it.filter(Char::isDigit).take(12) }, label = { Text("Random seed") }, modifier = Modifier.widthIn(max = 260.dp))
        result.outcomes.forEach { outcome ->
            Insight(outcome.label, "${outcome.count} · observed=${trim(outcome.probability)} · expected=${trim(result.expected[outcome.label] ?: 0.0)}", Cyan)
        }
        Insight("Monte Carlo π", "${trim(pi.first)} ± ${trim(pi.second)}", Violet)
        Text("Conditional probability & Bayes tree", color = Ink, fontWeight = FontWeight.Bold)
        AxisSlider("Prior", prior, .01f..99f) { prior = it.coerceIn(.01f, .99f) }
        AxisSlider("Sensitivity", sensitivity, .01f..99f) { sensitivity = it.coerceIn(.01f, .99f) }
        AxisSlider("False positive", falsePositive, .01f..99f) { falsePositive = it.coerceIn(.01f, .99f) }
        Insight("P(positive)", trim(bayes.evidenceProbability), Amber)
        bayes.branches.forEach { branch ->
            Insight(branch.hypothesis, "prior=${trim(branch.prior)} · likelihood=${trim(branch.evidenceLikelihood)} · posterior=${trim(branch.posterior)}", Green)
        }
        Text("Combinatorics lab · n=10, r=3", color = Ink, fontWeight = FontWeight.Bold)
        Insight("Permutations", combinatorics.permutations.toString(), Cyan)
        Insight("Combinations", combinatorics.combinations.toString(), Violet)
        Insight("With replacement", combinatorics.withReplacement.toString(), Amber)
    }
}

@Composable
private fun StatisticsLabScreen(
    vm: ExplorerViewModel,
    wide: Boolean,
    onSection: (ProbabilityLabSection) -> Unit,
) {
    var dataText by remember { mutableStateOf("4, 5, 5, 6, 7, 8, 8, 8, 9, 10, 12, 18") }
    var chartType by remember { mutableStateOf(StatisticsChartType.Histogram) }
    var binCount by remember { mutableFloatStateOf(6f) }
    var hypothesizedMean by remember { mutableFloatStateOf(8f) }
    var selectedDetail by remember { mutableStateOf("Tap a mark to inspect its value") }
    val values = remember(dataText) { Regex("-?\\d+(?:\\.\\d+)?").findAll(dataText).mapNotNull { it.value.toDoubleOrNull() }.toList() }
    val summary = remember(values) { runCatching { AdvancedStatisticsEngine.summarize(values) }.getOrNull() }
    val histogram = remember(values, binCount) { if (values.isEmpty()) emptyList() else AdvancedStatisticsEngine.histogram(values, binCount.toInt()) }
    val confidence = remember(values) { runCatching { InferentialStatistics.meanConfidenceInterval(values) }.getOrNull() }
    val tTest = remember(values, hypothesizedMean) { runCatching { InferentialStatistics.oneSampleT(values, hypothesizedMean.toDouble()) }.getOrNull() }
    val regression = remember(values) { if (values.size >= 3) Phase4Statistics.linearRegression(values.indices.map(Int::toDouble), values) else null }
    val bootstrap = remember(values) { if (values.size >= 2) Phase4Statistics.bootstrapMean(values, repetitions = 1_000, seed = 42) else null }
    val anova = remember(values) {
        if (values.size >= 6) {
            val groups = values.withIndex().groupBy { it.index % 3 }.values.map { group -> group.map { it.value } }
            runCatching { Phase4Statistics.oneWayAnova(groups, permutations = 500, seed = 42) }.getOrNull()
        } else null
    }

    @Composable
    fun DataControls(modifier: Modifier = Modifier) {
        GlassPanel(modifier) {
            PanelHeader("Interactive Statistics Lab", { vm.open(MathModule.Graph2D) }, Cyan, icon = "x̄")
            ProbabilitySectionSelector(ProbabilityLabSection.Statistics, onSection)
            Text("Enter raw observations separated by commas or spaces. Every chart and statistic updates from the same data.", color = Muted, fontSize = 12.sp)
            OutlinedTextField(
                value = dataText,
                onValueChange = { dataText = it },
                label = { Text("Dataset") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Statistics dataset editor" },
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                GlowButton("Symmetric") { dataText = "2,3,4,5,6,7,8" }
                GlowButton("Skewed") { dataText = "1,1,2,2,3,5,8,13,21" }
                GlowButton("Outliers") { dataText = "10,11,11,12,12,13,13,14,40" }
                GlowButton("Bimodal") { dataText = "2,2,3,3,4,8,9,9,10,10" }
            }
            Text("Chart", color = Ink, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                StatisticsChartType.entries.forEach { type -> GlowButton(if (chartType == type) "• ${type.label}" else type.label) { chartType = type } }
            }
            if (chartType == StatisticsChartType.Histogram) AxisSlider("Bins", binCount, 2f..16f) { binCount = it.roundToInt().toFloat() }
            AxisSlider("H₀ mean", hypothesizedMean, -10f..30f) { hypothesizedMean = it }
            tTest?.let { test ->
                Insight("One-sample t", "t=${trim(test.statistic)} · df=${trim(test.degreesOfFreedom)}", Violet)
                Insight("Two-sided p", trim(test.pValueTwoSided), if (test.rejectAtFivePercent) Amber else Green)
            }
        }
    }

    @Composable
    fun Analysis(modifier: Modifier = Modifier) {
        GlassPanel(modifier) {
            Text(chartType.label, color = Violet, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            if (summary == null) {
                Text("Enter at least one finite number.", color = Amber)
            } else {
                InteractiveStatisticsChart(values, summary, histogram, chartType, selectedDetail = selectedDetail, onSelect = { selectedDetail = it })
                Text(selectedDetail, color = Cyan, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Selected chart value" })
                Insight("Count / Sum", "${summary.count} / ${trim(summary.sum)}", Cyan)
                Insight("Mean", trim(summary.mean), Green)
                Insight("Median", trim(summary.median), Violet)
                Insight("Mode", summary.modes.joinToString { trim(it) }.ifBlank { "No repeated mode" }, Amber)
                Insight("Min / Max", "${trim(summary.fiveNumber.minimum)} / ${trim(summary.fiveNumber.maximum)}", Cyan)
                Insight("Q1 / Q3", "${trim(summary.fiveNumber.firstQuartile)} / ${trim(summary.fiveNumber.thirdQuartile)}", Violet)
                Insight("Range / IQR", "${trim(summary.range)} / ${trim(summary.interquartileRange)}", Green)
                Insight("Population variance", trim(summary.populationVariance), Cyan)
                Insight("Sample variance", trim(summary.sampleVariance), Violet)
                Insight("Population σ", trim(summary.populationStandardDeviation), Cyan)
                Insight("Sample s", trim(summary.sampleStandardDeviation), Green)
                Insight("Standard error", trim(summary.standardError), Amber)
                Insight("Mean abs deviation", trim(summary.meanAbsoluteDeviation), Cyan)
                Insight("Median abs deviation", trim(summary.medianAbsoluteDeviation), Violet)
                Insight("Skewness", summary.skewness?.let(::trim) ?: "needs n ≥ 3", Green)
                Insight("Excess kurtosis", summary.excessKurtosis?.let(::trim) ?: "needs n ≥ 4", Amber)
                Insight("Outliers (1.5×IQR)", summary.outliers.joinToString { trim(it) }.ifBlank { "none" }, if (summary.outliers.isEmpty()) Green else Amber)
                confidence?.let { interval -> Insight("95% mean CI", "[${trim(interval.lower)}, ${trim(interval.upper)}]", Cyan) }
                regression?.let { model ->
                    Insight("Regression", "slope=${trim(model.coefficients[1])} · R²=${model.rSquared?.let(::trim)}", Violet)
                    Insight("Residual range", "${trim(model.residuals.min())} to ${trim(model.residuals.max())}", Amber)
                    model.diagnostics.forEach { diagnostic -> Insight(diagnostic.name, diagnostic.detail, if (diagnostic.passed) Green else Amber) }
                    Text("Regression assumptions: ${model.assumptions.joinToString()}", color = Muted, fontSize = 11.sp)
                }
                anova?.let { test ->
                    Insight("ANOVA", "F=${trim(test.statistic)} · p=${trim(test.pValue)} · η²=${test.effectSize?.let(::trim)}", Cyan)
                    test.diagnostics.forEach { diagnostic -> Insight(diagnostic.name, diagnostic.detail, if (diagnostic.passed) Green else Amber) }
                }
                bootstrap?.let { result -> Insight("Seeded bootstrap mean", "${trim(result.observed)} · 95% [${trim(result.lower)}, ${trim(result.upper)}]", Green) }
            }
        }
    }

    if (wide) {
        Row(Modifier.fillMaxSize().padding(top = 78.dp, bottom = 76.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DataControls(Modifier.weight(.43f).fillMaxHeight())
            Analysis(Modifier.weight(.57f).fillMaxHeight())
        }
    } else {
        Column(Modifier.fillMaxSize().padding(top = 68.dp, bottom = 66.dp).verticalScroll(rememberScrollState())) {
            DataControls(Modifier.fillMaxWidth())
            Analysis(Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun InteractiveStatisticsChart(
    values: List<Double>,
    summary: DescriptiveStatistics,
    histogram: List<HistogramBin>,
    type: StatisticsChartType,
    selectedDetail: String,
    onSelect: (String) -> Unit,
) {
    val sorted = remember(values) { values.sorted() }
    val ecdf = remember(values) { AdvancedStatisticsEngine.empiricalCdf(values) }
    val qq = remember(values) { AdvancedStatisticsEngine.normalQq(values) }
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x6600060D))
            .pointerInput(values, type, histogram) {
                detectTapGestures { tap ->
                    if (values.isEmpty()) return@detectTapGestures
                    val minX = sorted.first(); val maxX = sorted.last().takeIf { it > minX } ?: minX + 1
                    val value = minX + (tap.x / size.width).coerceIn(0f, 1f) * (maxX - minX)
                    if (type == StatisticsChartType.Histogram) {
                        val bin = histogram.firstOrNull { value >= it.lower && value <= it.upper } ?: histogram.lastOrNull()
                        bin?.let { onSelect("Bin ${trim(it.lower)}–${trim(it.upper)}: ${it.count} observations (${trim(it.relativeFrequency * 100)}%)") }
                    } else {
                        val nearest = sorted.minBy { abs(it - value) }
                        val rank = sorted.indexOf(nearest) + 1
                        onSelect("Value ${trim(nearest)} · ordered rank $rank of ${sorted.size}")
                    }
                }
            }
            .semantics { contentDescription = "Interactive ${type.label} statistical chart. $selectedDetail" },
    ) {
        val left = 42f; val right = size.width - 12f; val top = 16f; val bottom = size.height - 30f
        val width = (right - left).coerceAtLeast(1f); val height = (bottom - top).coerceAtLeast(1f)
        fun axis(minimum: Double, maximum: Double): (Double) -> Float = { value -> left + ((value - minimum) / (maximum - minimum).coerceAtLeast(1e-12) * width).toFloat() }
        drawLine(Muted.copy(.5f), Offset(left, bottom), Offset(right, bottom), 1.5f)
        drawLine(Muted.copy(.5f), Offset(left, top), Offset(left, bottom), 1.5f)
        repeat(4) { index ->
            val y = top + height * index / 4f
            drawLine(Muted.copy(.16f), Offset(left, y), Offset(right, y), 1f)
        }
        when (type) {
            StatisticsChartType.Histogram -> {
                val maximum = histogram.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
                histogram.forEachIndexed { index, bin ->
                    val x0 = left + width * index / histogram.size
                    val x1 = left + width * (index + 1) / histogram.size
                    val y = bottom - height * bin.count / maximum
                    drawRect(if (index % 2 == 0) Cyan.copy(.62f) else Violet.copy(.62f), Offset(x0 + 1, y), Size((x1 - x0 - 2).coerceAtLeast(1f), bottom - y))
                    drawRect(Cyan.copy(.8f), Offset(x0 + 1, y), Size((x1 - x0 - 2).coerceAtLeast(1f), bottom - y), style = Stroke(1f))
                }
                drawTrigText("frequency", 4f, top + 12f, Muted)
            }
            StatisticsChartType.BoxPlot -> {
                val x = axis(summary.fiveNumber.minimum, summary.fiveNumber.maximum)
                val y = top + height * .5f
                drawLine(Cyan, Offset(x(summary.fiveNumber.minimum), y), Offset(x(summary.fiveNumber.maximum), y), 3f)
                drawLine(Cyan, Offset(x(summary.fiveNumber.minimum), y - 24), Offset(x(summary.fiveNumber.minimum), y + 24), 3f)
                drawLine(Cyan, Offset(x(summary.fiveNumber.maximum), y - 24), Offset(x(summary.fiveNumber.maximum), y + 24), 3f)
                drawRect(Violet.copy(.35f), Offset(x(summary.fiveNumber.firstQuartile), y - 38), Size(x(summary.fiveNumber.thirdQuartile) - x(summary.fiveNumber.firstQuartile), 76f))
                drawRect(Violet, Offset(x(summary.fiveNumber.firstQuartile), y - 38), Size(x(summary.fiveNumber.thirdQuartile) - x(summary.fiveNumber.firstQuartile), 76f), style = Stroke(3f))
                drawLine(Amber, Offset(x(summary.median), y - 38), Offset(x(summary.median), y + 38), 4f)
                drawCircle(Green, 7f, Offset(x(summary.mean), y))
                summary.outliers.forEach { drawCircle(Color(0xFFFF6B7A), 6f, Offset(x(it), y)) }
                drawTrigText("median", x(summary.median) - 22f, y - 48f, Amber)
                drawTrigText("mean", x(summary.mean) - 18f, y + 58f, Green)
            }
            StatisticsChartType.DotPlot -> {
                val x = axis(sorted.first(), sorted.last().takeIf { it > sorted.first() } ?: sorted.first() + 1)
                val counts = mutableMapOf<Double, Int>()
                sorted.forEach { value ->
                    val level = counts.getOrDefault(value, 0); counts[value] = level + 1
                    drawCircle(if (level % 2 == 0) Cyan else Violet, 7f, Offset(x(value), bottom - 10f - level * 17f))
                }
            }
            StatisticsChartType.Ecdf -> {
                val x = axis(sorted.first(), sorted.last().takeIf { it > sorted.first() } ?: sorted.first() + 1)
                var previous = Offset(x(sorted.first()), bottom)
                ecdf.forEach { point ->
                    val currentX = x(point.x); val currentY = bottom - point.y.toFloat() * height
                    drawLine(Cyan, previous, Offset(currentX, previous.y), 3f)
                    drawLine(Cyan, Offset(currentX, previous.y), Offset(currentX, currentY), 3f)
                    previous = Offset(currentX, currentY)
                }
                drawTrigText("1.0", 8f, top + 8f, Muted)
                drawTrigText("0.5", 8f, top + height / 2 + 4f, Muted)
            }
            StatisticsChartType.NormalQq -> {
                val minTheory = qq.minOf { it.x }; val maxTheory = qq.maxOf { it.x }
                val minObserved = qq.minOf { it.y }; val maxObserved = qq.maxOf { it.y }
                val x = axis(minTheory, maxTheory)
                fun y(value: Double) = bottom - ((value - minObserved) / (maxObserved - minObserved).coerceAtLeast(1e-12) * height).toFloat()
                val referenceSd = summary.sampleStandardDeviation.takeIf(Double::isFinite)
                    ?: summary.populationStandardDeviation
                val refStart = summary.mean + referenceSd * minTheory
                val refEnd = summary.mean + referenceSd * maxTheory
                drawLine(Amber.copy(.8f), Offset(x(minTheory), y(refStart)), Offset(x(maxTheory), y(refEnd)), 2f)
                qq.forEach { drawCircle(Cyan, 6f, Offset(x(it.x), y(it.y))) }
                drawTrigText("theoretical normal quantiles", left + 20f, size.height - 8f, Muted)
            }
        }
        drawTrigText(trim(sorted.first()), left, size.height - 8f, Muted)
        drawTrigText(trim(sorted.last()), right - 38f, size.height - 8f, Muted)
    }
}

@Composable
private fun StatisticsMaterialsScreen(
    vm: ExplorerViewModel,
    wide: Boolean,
    onSection: (ProbabilityLabSection) -> Unit,
) {
    var level by remember { mutableStateOf(StatisticsStudyLevel.School) }
    val lessons = StatisticsCurriculum.lessons.getValue(level)
    GlassPanel(
        Modifier
            .fillMaxSize()
            .padding(top = 74.dp, bottom = 70.dp, start = if (wide) 42.dp else 4.dp, end = if (wide) 42.dp else 4.dp),
    ) {
        PanelHeader("Statistics Learning Path · School to PG", { vm.open(MathModule.Graph2D) }, Cyan, icon = "∑")
        ProbabilitySectionSelector(ProbabilityLabSection.Learn, onSection)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            StatisticsStudyLevel.entries.forEach { option -> GlowButton(if (level == option) "• ${option.label}" else option.label) { level = option } }
        }
        Text("${level.label} syllabus foundation", color = Violet, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        lessons.forEachIndexed { index, lesson ->
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Color(0x33101824)).border(1.dp, if (index % 2 == 0) Cyan.copy(.45f) else Violet.copy(.45f), RoundedCornerShape(15.dp)).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("${index + 1}. ${lesson.title}", color = Ink, fontWeight = FontWeight.Bold)
                Text(lesson.concepts.joinToString(" · "), color = Cyan, fontSize = 12.sp)
                Text("Interactive lab: ${lesson.lab}", color = Green, fontSize = 12.sp)
                Text("Outcome: ${lesson.outcome}", color = Muted, fontSize = 11.sp)
            }
        }
        Text("Coverage is sequenced to PG level; advanced GLM, Bayesian, multivariate, time-series and survival modules currently provide curriculum and engine boundaries for later full model-fitting labs.", color = Amber, fontSize = 11.sp)
    }
}

@Composable
private fun DistributionPlot(points: List<com.indianservers.aiexplorer.core.DistributionPoint>, lower: Double, upper: Double, discrete: Boolean) {
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(230.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x6600060D))
            .semantics { contentDescription = "Distribution density with selected probability interval" },
    ) {
        if (points.size < 2) return@Canvas
        val minX = points.minOf { it.x }
        val maxX = points.maxOf { it.x }.takeIf { it > minX } ?: (minX + 1.0)
        val maxY = points.maxOf { it.probability }.coerceAtLeast(1e-12)
        fun sx(x: Double) = ((x - minX) / (maxX - minX) * size.width).toFloat()
        fun sy(y: Double) = (size.height - y / maxY * size.height * .88).toFloat()
        drawLine(Muted.copy(.4f), Offset(0f, size.height - 1f), Offset(size.width, size.height - 1f), 1.5f)
        if (discrete) {
            points.forEach { point ->
                val selected = point.x in minOf(lower, upper)..maxOf(lower, upper)
                drawLine(if (selected) Green else Cyan, Offset(sx(point.x), size.height), Offset(sx(point.x), sy(point.probability)), strokeWidth = (size.width / points.size * .62f).coerceIn(2f, 18f), cap = StrokeCap.Round)
            }
        } else {
            val path = Path()
            points.forEachIndexed { index, point -> if (index == 0) path.moveTo(sx(point.x), sy(point.probability)) else path.lineTo(sx(point.x), sy(point.probability)) }
            drawPath(path, Cyan, style = Stroke(width = 3f, cap = StrokeCap.Round))
            points.filter { it.x in minOf(lower, upper)..maxOf(lower, upper) }.forEach { point ->
                drawLine(Green.copy(.34f), Offset(sx(point.x), size.height), Offset(sx(point.x), sy(point.probability)), strokeWidth = 3f)
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
                    TransparentIcon(subject.symbol, if (subject.enabled) Cyan else Muted)
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
private fun MathematicsMenuPanel(
    vm: ExplorerViewModel,
    compact: Boolean,
    onMove: (Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPlanned by remember { mutableStateOf(false) }
    var showWorkspaces by remember { mutableStateOf(!compact) }
    var expandedItem by remember { mutableStateOf<String?>(null) }
    val uniqueOptions = MathMenuOptions.groupBy { it.title }.values.map { options -> options.firstOrNull { it.available } ?: options.first() }
    val plannedCount = uniqueOptions.count { !it.available }
    GlassPanel(modifier) {
        PanelHeader("Mathematics Menu", vm::toggleMathMenu, Cyan, icon = "∑", onMove = onMove)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlowButton("Subjects", onClick = vm::openSubjectHub)
            GlowButton("Current Workspace", onClick = vm::toggleMathMenu)
        }
        uniqueOptions.filter { it.available || showPlanned }.forEach { option ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (option.available) Color(0x4430D9FF) else Color(0x22101824))
                    .border(1.dp, if (option.available) Cyan else Muted.copy(.28f), RoundedCornerShape(16.dp))
                    .clickable(enabled = option.available) {
                        when (option.title) {
                            "Scientific Calculator" -> vm.openScientificCalculator()
                            "Math Notebook" -> vm.openMathNotebook()
                            "Problem Solver" -> vm.openProblemSolver()
                            "Formulas" -> vm.openKnowledgeHub(KnowledgeSection.Formulas)
                            "MCQs" -> vm.openKnowledgeHub(KnowledgeSection.Mcqs)
                            "Visualize Formulas" -> vm.openKnowledgeHub(KnowledgeSection.Visualize)
                            "Theorems" -> vm.openKnowledgeHub(KnowledgeSection.Theorems)
                            "Visual Proofs" -> vm.openKnowledgeHub(KnowledgeSection.Proofs)
                            "Maths Dictionary" -> vm.openKnowledgeHub(KnowledgeSection.Dictionary)
                            "Probability & Statistics" -> vm.openProbabilityLab()
                            "Manipulatives" -> vm.open(MathModule.Manipulatives)
                            "Explore Workspaces" -> showWorkspaces = !showWorkspaces
                            else -> expandedItem = if (expandedItem == option.title) null else option.title
                        }
                    }
                    .focusable()
                    .semantics { contentDescription = "${option.title}, ${if (option.available) "available" else "planned for a future update"}" }
                    .padding(if (compact) 9.dp else 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TransparentIcon(option.icon, if (option.available) Cyan else Muted)
                Column(Modifier.weight(1f)) {
                    Text(option.title, color = if (option.available) Ink else Muted, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    AnimatedVisibility(!compact || expandedItem == option.title) {
                        Text(option.description, color = Muted, fontSize = 11.sp)
                    }
                }
                Text(if (option.available) "OPEN" else "SOON", color = if (option.available) Green else Amber, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
        if (plannedCount > 0) GlowButton(if (showPlanned) "Hide planned" else "Show $plannedCount planned") { showPlanned = !showPlanned }
        GlowButton(if (showWorkspaces) "Hide workspaces" else "Show workspaces") { showWorkspaces = !showWorkspaces }
        AnimatedVisibility(showWorkspaces) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                MathModule.entries.forEach { module -> GlowButton(module.label, onClick = { vm.open(module) }) }
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
private fun TopShell(vm: ExplorerViewModel, compact: Boolean, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(!compact) }
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceA.copy(alpha = .78f))
            .animateContentSize()
            .padding(horizontal = 4.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlowButton("Menu", onClick = vm::toggleMathMenu)
        Column(
            Modifier.clickable { expanded = !expanded }.padding(horizontal = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("AI Explorer ${if (expanded) "⌃" else "⌄"}", color = Ink, fontSize = if (compact) 18.sp else 24.sp, fontWeight = FontWeight.ExtraBold)
            AnimatedVisibility(expanded) {
                Text(
                    "Maths · ${when { vm.showMathNotebook -> "Notebook"; vm.showProblemSolver -> "Problem Solver"; vm.showScientificCalculator -> "Scientific Calculator"; vm.showProbabilityLab -> "Probability Lab"; vm.showKnowledgeHub -> vm.activeKnowledgeSection.title; else -> vm.state.module.label }}",
                    color = Muted,
                    fontSize = if (compact) 10.sp else 12.sp,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 8.dp)) {
            AnimatedVisibility(expanded || !compact) { GlowButton(if (compact) "↶" else "Undo", enabled = true, onClick = vm::undo) }
            AnimatedVisibility(expanded || !compact) { GlowButton(if (compact) "↷" else "Redo", enabled = true, onClick = vm::redo) }
            AnimatedVisibility(vm.state.module != MathModule.Graph2D) {
                GlowButton(if (compact) "⋮" else "More", enabled = true, onClick = vm::toggleActionDock)
            }
        }
    }
}

@Composable
private fun LearningCoachPanel(vm: ExplorerViewModel, modifier: Modifier = Modifier) {
    val activity = vm.activeActivity
    val progress = "${vm.completedActivities.size}/${LearningActivities.size}"
    val linkedSnapshot = vm.linkedMathSnapshot
    val universalDocument = vm.universalMathDocument
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
        Insight(
            "Linked maths",
            "CAS ${linkedSnapshot.objectsFor(LinkedMathView.CAS).size} · Graph ${linkedSnapshot.objectsFor(LinkedMathView.Graph).size} · Table ${linkedSnapshot.objectsFor(LinkedMathView.Table).size} · Geometry ${linkedSnapshot.objectsFor(LinkedMathView.Geometry).size} · Probability ${linkedSnapshot.objectsFor(LinkedMathView.Probability).size}",
            Violet,
        )
        Insight(
            "Maths authority",
            "${universalDocument.objects.size} typed objects · revision ${universalDocument.revision} · one dependency graph",
            Green,
        )
        if (linkedSnapshot.diagnostics.isNotEmpty()) Insight("Kernel diagnostics", linkedSnapshot.diagnostics.take(2).joinToString(), Amber)
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
        InteractiveActivityStudioCard(vm.learningRole)
        ProductionReadinessCard(vm)
        ReleaseQaLabCard()
        Text("Settings", color = Ink, fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TogglePill("Haptics", vm.settings.haptics) { value -> vm.updateSettings { it.copy(haptics = value) } }
            TogglePill("Snap", vm.settings.snap) { value -> vm.updateSettings { it.copy(snap = value) } }
            TogglePill("High contrast", vm.settings.highContrast) { value -> vm.updateSettings { it.copy(highContrast = value) } }
            TogglePill("Reduced motion", vm.settings.reducedMotion) { value -> vm.updateSettings { it.copy(reducedMotion = value) } }
            TogglePill("Spoken maths", vm.settings.spokenMath) { value -> vm.updateSettings { it.copy(spokenMath = value) } }
            TogglePill("Graph audio", vm.settings.graphSonification) { value -> vm.updateSettings { it.copy(graphSonification = value) } }
            TogglePill("Large targets", vm.settings.largeTouchTargets) { value -> vm.updateSettings { it.copy(largeTouchTargets = value) } }
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
private fun ProductionReadinessCard(vm: ExplorerViewModel) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    val document = vm.universalMathDocument
    val archive = remember(document.revision, vm.settings) {
        AIExplorerProjectArchive.encode(
            AIExplorerProjectArchive.create(
                vm.state.id, document, vm.state.modifiedAt, System.currentTimeMillis(),
                listOf(ProjectSection(ProjectSectionKind.Settings, "highContrast=${vm.settings.highContrast};reducedMotion=${vm.settings.reducedMotion};spokenMath=${vm.settings.spokenMath};graphAudio=${vm.settings.graphSonification}")),
            ),
        )
    }
    val graphEvaluations = vm.state.functions.size * 500
    val objectCount = document.objects.size
    val performance = remember(archive.length, graphEvaluations, objectCount) {
        ProductPerformanceManager.assess(ProductPerformanceSnapshot(16.0, graphEvaluations, objectCount, archive.toByteArray().size.toLong(), objectCount * 12_000L))
    }
    val memoryClass = remember {
        (context.getSystemService(android.content.Context.ACTIVITY_SERVICE) as? ActivityManager)?.memoryClass ?: 256
    }
    val device = remember(memoryClass) {
        DeviceCapabilityManager.assess(DeviceCapabilityProfile(Build.VERSION.SDK_INT, Runtime.getRuntime().availableProcessors(), memoryClass, 3, false, false, false, memoryClass <= 128))
    }
    val exchange = remember(vm.state.modifiedAt) { GeoGebraExchange.exportXml(vm.state).coverage }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Green.copy(.07f))
            .border(1.dp, Green.copy(.45f), RoundedCornerShape(14.dp)).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Project & device readiness", color = Green, fontWeight = FontWeight.Bold)
                Text("Checksummed save · exchange coverage · adaptive quality", color = Muted, fontSize = 11.sp)
            }
            GlowButton(if (expanded) "Hide" else "Inspect") { expanded = !expanded }
        }
        Insight("Runtime", "${performance.status} ${performance.score}/100 · ${device.tier}", if (performance.score >= 90) Green else Amber)
        if (expanded) {
            Insight("Project archive", "${archive.toByteArray().size / 1024} KB · ${document.objects.size} typed maths objects", Cyan)
            Insight("GeoGebra XML", "${exchange.exported} translated · ${exchange.skipped.size} explicitly skipped", Violet)
            Insight("Fallback", if ("live AR" in device.enabled) "Live AR available" else "Full simulator and 2D maths remain enabled", Amber)
            Insight("Surface density", device.recommendedSurfaceDensity.toString(), Cyan)
            performance.messages.forEach { Text(it, color = if (performance.status.name == "Pass") Green else Amber, fontSize = 11.sp) }
            exchange.skipped.take(3).forEach { Text("Exchange gap · $it", color = Amber, fontSize = 10.sp) }
            Text("Live AR capability is finalized inside the AR workspace after permission and ARCore checks.", color = Muted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun ReleaseQaLabCard() {
    var expanded by remember { mutableStateOf(false) }
    var benchmark by remember { mutableStateOf<MathBenchmarkReport?>(null) }
    val security = remember {
        AppSecurityAuditEngine.audit(
            AppSecurityConfiguration(
                permissions = setOf(Manifest.permission.CAMERA),
                exportedComponents = mapOf("MainActivity" to true),
                cleartextTrafficAllowed = false,
                backupAllowed = true,
                cameraFramesPersisted = false,
                cameraFramesUploaded = false,
                secretsInSource = false,
                networkTransportsAttached = emptySet(),
            ),
        )
    }
    val accessibility = remember {
        AccessibilityQaEngine.audit(
            listOf(
                AccessibilityNodeEvidence("main-menu", "Open main menu", "Button", 48.0, 48.0, true, 7.0),
                AccessibilityNodeEvidence("workspace", "Interactive maths workspace", "Canvas", 48.0, 48.0, true, 7.0, true, true),
                AccessibilityNodeEvidence("solver", "Open problem solver", "Button", 48.0, 48.0, true, 7.0),
                AccessibilityNodeEvidence("tools", "Open movable tools", "Button", 48.0, 48.0, true, 7.0),
            ),
        )
    }
    val evidence = remember(benchmark, security, accessibility) {
        val maths = benchmark
        ReleaseQaEvidenceCodec.encode(
            ReleaseQaEvidenceBundle(
                buildLabel = "local-debug",
                deviceLabel = "${Build.MANUFACTURER} ${Build.MODEL}",
                createdAt = System.currentTimeMillis(),
                sections = listOf(
                    QaEvidenceSection("maths", maths?.let { if (it.failed == 0) "PASS" else "FAIL" } ?: "NOT_RUN", listOf(maths?.let { "${it.passed}/${it.results.size} deterministic cases" } ?: "Run the local suite")),
                    QaEvidenceSection("accessibility-contract", if (accessibility.passed) "PASS" else "FAIL", listOf("${accessibility.nodesChecked} declared interaction contracts")),
                    QaEvidenceSection("security-config", if (security.passed) "PASS" else "FAIL", security.findings.map { "${it.severity}: ${it.message}" }),
                    QaEvidenceSection("physical-ar", "NOT_RECORDED", listOf("Record anchor drift, depth error, frame p95 and thermal state on a supported device")),
                ),
            ),
        )
    }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Cyan.copy(.06f))
            .border(1.dp, Cyan.copy(.4f), RoundedCornerShape(14.dp)).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Release QA Lab", color = Cyan, fontWeight = FontWeight.Bold)
                Text("Deterministic tests and tamper-evident evidence", color = Muted, fontSize = 11.sp)
            }
            GlowButton(if (expanded) "Hide" else "Open") { expanded = !expanded }
        }
        val result = benchmark
        Insight("Math benchmark", result?.let { "${it.passed}/${it.results.size} passed" } ?: "Not run on this build", result?.let { if (it.failed == 0) Green else Amber } ?: Muted)
        Insight("Physical AR", "Not recorded - device run required", Amber)
        if (expanded) {
            GlowButton(if (result == null) "Run maths suite" else "Run again") {
                benchmark = DeterministicMathBenchmarkRunner().run(ReleaseMathBenchmarkCatalog.smoke)
            }
            result?.topicCoverage?.forEach { (topic, counts) ->
                Text("${topic.name}: ${counts.first}/${counts.second}", color = if (counts.first == counts.second) Green else Amber, fontSize = 11.sp)
            }
            Insight("Accessibility contract", if (accessibility.passed) "${accessibility.nodesChecked} key interaction contracts pass" else "${accessibility.findings.size} findings", if (accessibility.passed) Green else Amber)
            Insight("Security configuration", if (security.passed) "No release-blocking configuration found" else "Release blockers found", if (security.passed) Green else Amber)
            security.findings.take(3).forEach { Text("${it.severity} - ${it.message}", color = if (it.severity.name == "Info") Muted else Amber, fontSize = 10.sp) }
            Insight("Evidence integrity", if (ReleaseQaEvidenceCodec.verify(evidence)) "SHA-256 verified" else "Checksum failed", Green)
            Text("AR PASS is never inferred from simulator or unit tests.", color = Muted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun InteractiveActivityStudioCard(role: LearningRole) {
    val engine = remember { InteractiveActivityEngine() }
    var document by remember { mutableStateOf(InteractiveActivityCatalog.unitCircle) }
    var run by remember(document.id, document.revision) { mutableStateOf(engine.start(document)) }
    var response by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val validation = remember(document) { InteractiveActivityAuthoring.validate(document) }
    val current = document.blocks.firstOrNull { it.id == run.currentBlockId }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Violet.copy(.08f))
            .border(1.dp, Violet.copy(.5f), RoundedCornerShape(14.dp)).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(if (role == LearningRole.Teacher) "Interactive Activity Studio" else "Adaptive activity", color = Violet, fontWeight = FontWeight.Bold)
                Text(document.title, color = Ink, fontSize = 12.sp)
            }
            GlowButton(if (expanded) "Collapse" else "Open") { expanded = !expanded }
        }
        Insight("Activity", "${document.blocks.size} blocks · ${if (validation.valid) "ready" else "needs links"} · revision ${document.revision}", if (validation.valid) Green else Amber)
        if (expanded) {
            if (role == LearningRole.Teacher) {
                Text("Author blocks", color = Cyan, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf("Instruction", "Math check", "MCQ", "Workspace", "Tiles", "Proof", "Branch", "Reflection").forEach { label -> TransparentIcon(label.take(2), Cyan) }
                }
                document.blocks.forEachIndexed { index, block -> Text("${index + 1}. ${block.javaClass.simpleName} · ${block.title}", color = Muted, fontSize = 11.sp) }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GlowButton("Add reflection") {
                        val id = "reflection-${document.revision + 1}"
                        val terminal = document.blocks.last()
                        val linked = when (terminal) {
                            is ActivityBlock.Reflection -> terminal.copy(nextOnPass = id)
                            else -> terminal
                        }
                        document = InteractiveActivityAuthoring.add(InteractiveActivityAuthoring.replace(document, linked), ActivityBlock.Reflection(id, "Teacher reflection", "Explain the invariant in your own words."))
                    }
                    GlowButton("Validate") { document = document.copy(revision = document.revision + 1) }
                }
                validation.errors.forEach { Text(it, color = Amber, fontSize = 11.sp) }
                Text("Export · ${InteractiveActivityAuthoring.serialize(document).take(72)}…", color = Muted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            } else if (run.completed) {
                Text("Activity complete · score ${run.score}%", color = Green, fontWeight = FontWeight.Bold)
                run.mastery.values.forEach { Insight(it.skill, "${trim(it.score * 100)}% · ${it.band}", Green) }
                GlowButton("Restart") { run = engine.start(document); response = "" }
            } else if (current != null) {
                Text(current.title, color = Cyan, fontWeight = FontWeight.Bold)
                when (current) {
                    is ActivityBlock.Instruction -> {
                        Text(current.body, color = Ink, fontSize = 12.sp)
                        GlowButton("Continue") { run = engine.submit(document, run, ActivityAnswer.Continue) }
                    }
                    is ActivityBlock.MathResponse -> {
                        Text(current.prompt, color = Ink, fontSize = 12.sp)
                        OutlinedTextField(response, { response = it }, Modifier.fillMaxWidth(), label = { Text("Your expression") })
                        GlowButton("Check without revealing") { run = engine.submit(document, run, ActivityAnswer.Text(response), ActivityEvaluationContext(now = System.currentTimeMillis())); response = "" }
                    }
                    is ActivityBlock.MultipleChoice -> {
                        Text(current.prompt, color = Ink, fontSize = 12.sp)
                        current.choices.forEachIndexed { index, choice -> GlowButton(choice) { run = engine.submit(document, run, ActivityAnswer.Choice(index), ActivityEvaluationContext(now = System.currentTimeMillis())) } }
                    }
                    is ActivityBlock.Reflection -> {
                        Text(current.prompt, color = Ink, fontSize = 12.sp)
                        OutlinedTextField(response, { response = it }, Modifier.fillMaxWidth(), label = { Text("Explain your reasoning") })
                        GlowButton("Finish") { run = engine.submit(document, run, ActivityAnswer.Text(response), ActivityEvaluationContext(now = System.currentTimeMillis())); response = "" }
                    }
                    else -> Text("Open ${document.module.label} and complete this interactive check.", color = Muted, fontSize = 12.sp)
                }
                run.results.lastOrNull()?.takeIf { it.blockId == current.id }?.let { Text(it.feedback, color = if (it.passed) Green else Amber, fontSize = 11.sp) }
            }
        }
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
private fun BottomModeSelector(active: MathModule, onSelect: (MathModule) -> Unit, compact: Boolean, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(!compact) }
    val activeLabel = when (active) {
        MathModule.Geometry2D -> "2D"
        MathModule.Geometry3D -> "3D"
        MathModule.Graph2D -> "Graph"
        MathModule.Graph3D -> "G3D"
        MathModule.Trigonometry -> "Trig"
        MathModule.Manipulatives -> "Tiles"
        MathModule.SpatialAR -> "AR"
    }
    Row(
        modifier
            .fillMaxWidth(if (expanded) .98f else .52f)
            .widthIn(max = 980.dp)
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(SurfaceA.copy(alpha = .96f))
            .border(1.dp, Color(0x5548BFFF), RoundedCornerShape(26.dp))
            .animateContentSize()
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!expanded) {
            TransparentIcon(moduleIcon(active), Cyan)
            Text(activeLabel, color = Ink, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            Text("⌃", color = Cyan, fontSize = 18.sp, modifier = Modifier.clickable { expanded = true }.padding(horizontal = 12.dp, vertical = 8.dp).semantics { contentDescription = "Show navigation" })
        }
        if (expanded) MathModule.entries.forEach { module ->
            val selected = module == active
            val compactLabel = when (module) {
                MathModule.Geometry2D -> "2D"
                MathModule.Geometry3D -> "3D"
                MathModule.Graph2D -> "Graph"
                MathModule.Graph3D -> "G3D"
                MathModule.Trigonometry -> "Trig"
                MathModule.Manipulatives -> "Tiles"
                MathModule.SpatialAR -> "AR"
            }
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (selected) Brush.horizontalGradient(listOf(Violet, Cyan)) else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)))
                    .clickable { onSelect(module) }
                    .focusable()
                    .padding(horizontal = 2.dp, vertical = 5.dp)
                    .semantics { contentDescription = "Open ${module.label} workspace" },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(moduleIcon(module), color = if (selected) Color.White else Muted, fontSize = 13.sp)
                Text(compactLabel, color = if (selected) Color.White else Muted, textAlign = TextAlign.Center, maxLines = 1, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
            }
        }
        if (expanded) Text("⌄", color = Muted, modifier = Modifier.clickable { expanded = false }.padding(horizontal = 6.dp, vertical = 10.dp).semantics { contentDescription = "Collapse navigation" })
    }
}

@Composable
private fun Geometry2DScreen(vm: ExplorerViewModel) {
    val haptic = LocalHapticFeedback.current
    var lassoEnabled by remember { mutableStateOf(false) }
    var axisConstraint by remember { mutableStateOf(AxisConstraint.Free) }
    var precisionMode by remember { mutableStateOf(false) }
    var homeRequest by remember { mutableIntStateOf(0) }
    var undoViewRequest by remember { mutableIntStateOf(0) }
    val a = vm.state.points[0]
    val b = vm.state.points[1]
    val dynamicEngine = remember { DynamicGeometryEngine() }
    val dynamicDocument = remember(a, b) {
        var document = DynamicGeometryDocument()
        document = dynamicEngine.addPoint(document, DynamicPoint("A", DynamicPointRule.Free(a)), "Free point selected by the learner")
        document = dynamicEngine.addPoint(document, DynamicPoint("B", DynamicPointRule.Free(b)), "Free point selected by the learner")
        document = dynamicEngine.addPoint(document, DynamicPoint("M", DynamicPointRule.Midpoint("A", "B")), "A midpoint is equidistant from both endpoints")
        document = dynamicEngine.addPoint(document, DynamicPoint("C", DynamicPointRule.Rotate("B", "A", 60.0)), "Rotate AB by 60 degrees")
        document = dynamicEngine.addObject(document, DynamicGeometryObject.Line("lineAB", "A", "B"), "Two distinct points determine a line")
        document = dynamicEngine.addObject(document, DynamicGeometryObject.Perpendicular("perpM", "M", "A", "B"), "Perpendicular directions have zero dot product")
        document = dynamicEngine.addObject(document, DynamicGeometryObject.Circle("circleA", "A", "B"), "Center and point determine a circle")
        document = dynamicEngine.addObject(document, DynamicGeometryObject.Ellipse("ellipse", "A", "B", "C"), "The sum of focal distances is constant")
        document
    }
    var replayStep by remember { mutableFloatStateOf(dynamicDocument.protocol.size.toFloat()) }
    LaunchedEffect(dynamicDocument.protocol.size) { replayStep = dynamicDocument.protocol.size.toFloat() }
    val replayDocument = remember(dynamicDocument, replayStep) { dynamicEngine.replay(dynamicDocument, replayStep.toInt()) }
    val m = Geometry2D.segment(a, b)
    val analyticLine = remember(a, b) { runCatching { AnalyticGeometry2D.lineThrough(a, b) }.getOrNull() }
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
            selectedShapes = vm.selectedShapes,
            snapEnabled = vm.settings.snap,
            axisConstraint = axisConstraint,
            precisionMode = precisionMode,
            lassoEnabled = lassoEnabled,
            homeRequest = homeRequest,
            undoViewRequest = undoViewRequest,
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
            onShapeRotate = vm::previewShapeRotation,
            onDragEnd = vm::endPointDrag,
            onDragCancel = vm::cancelPointDrag,
            onCanvasTap = vm::handleGeometryTap,
            onClearSelection = vm::clearGeometrySelection,
            onLassoSelection = vm::selectShapes,
            points = vm.state.points,
        ) { tx ->
            val pa = tx(a)
            val pb = tx(b)
            val pm = tx(m.midpoint)
            drawLine(Violet, pa, pb, 5f, cap = StrokeCap.Round)
            drawStoredShapes(vm.state.points, vm.state.shapes, vm.selectedShapes, vm.selectedShape, tx)
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
            "Drag a junction to resize · drag shape to move · empty canvas pans · empty two-finger pinch zooms",
            Modifier.align(Alignment.BottomEnd),
        )
        Row(
            Modifier.align(Alignment.TopStart).padding(top = 72.dp, start = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            GlowButton("⌂ Fit") { homeRequest++ }
            GlowButton("Undo view") { undoViewRequest++ }
            GlowButton(if (lassoEnabled) "● Lasso" else "Lasso") { lassoEnabled = !lassoEnabled }
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
                    text = "${if (index in vm.selectedShapes) "• " else ""}${shape.name}${if (!shape.visible) " (hidden)" else ""}${if (shape.locked) " 🔒" else ""}",
                    color = if (index in vm.selectedShapes) Amber else Muted,
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
            val exactDistance = dynamicEngine.distance(dynamicDocument, "A", "B")
            Insight("Exact distance", "${exactDistance.exact} · ${trim(exactDistance.decimal)}", Cyan)
            Insight("Exact triangle area", dynamicEngine.polygonArea(dynamicDocument, listOf("A", "B", "C")).exact, Violet)
            Insight("Angle BAC", dynamicEngine.angle(dynamicDocument, "B", "A", "C").exact, Amber)
            dynamicEngine.ellipseParameters(dynamicDocument, "ellipse").forEach { (name, value) -> Insight("Ellipse $name", value.exact, Green) }
            analyticLine?.let { line ->
                Insight("Line AB", "${trim(-line.direction.y)}x + ${trim(line.direction.x)}y = ${trim(-line.direction.y * line.point.x + line.direction.x * line.point.y)}", Green)
            }
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
            vm.state.points.getOrNull(vm.selectedPoint)?.let { point ->
                DirectPointEditor(vm.selectedPoint, point) { updated -> vm.movePoint(vm.selectedPoint, updated) }
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
            Text("Movement constraint", color = Ink, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf(AxisConstraint.Free, AxisConstraint.X, AxisConstraint.Y).forEach { axis ->
                    GlowButton(if (axisConstraint == axis) "● ${axis.name}" else axis.name) { axisConstraint = axis }
                }
                GlowButton(if (precisionMode) "● Precision" else "Precision") { precisionMode = !precisionMode }
                GlowButton(if (lassoEnabled) "● Lasso" else "Lasso") { lassoEnabled = !lassoEnabled }
            }
            if (vm.constructionProtocol.isNotEmpty()) {
                Text("Construction Protocol", color = Ink, fontWeight = FontWeight.SemiBold)
                Text(vm.constructionProtocol.takeLast(8).mapIndexed { index, label -> "${index + 1}. $label" }.joinToString("  ·  "), color = Muted, fontSize = 12.sp)
            }
            Text("Replay dynamic construction", color = Ink, fontWeight = FontWeight.SemiBold)
            AxisSlider("Visible step", replayStep, 0f..dynamicDocument.protocol.size.toFloat().coerceAtLeast(1f)) { replayStep = it.roundToInt().toFloat() }
            replayDocument.protocol.forEachIndexed { index, step ->
                Text(
                    "${index + 1}. ${if (step.visible) "●" else "○"} ${step.title} · ${step.reason} · parents: ${step.dependencies.joinToString().ifBlank { "none" }}",
                    color = if (step.visible) Cyan else Muted,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

@Composable
private fun DirectPointEditor(pointIndex: Int, point: Vec2, onApply: (Vec2) -> Unit) {
    var xText by remember(pointIndex, point.x) { mutableStateOf(trim(point.x)) }
    var yText by remember(pointIndex, point.y) { mutableStateOf(trim(point.y)) }
    Text("Direct coordinate editing", color = Ink, fontWeight = FontWeight.SemiBold)
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(xText, { xText = it }, Modifier.weight(1f), label = { Text("x") }, singleLine = true)
        OutlinedTextField(yText, { yText = it }, Modifier.weight(1f), label = { Text("y") }, singleLine = true)
        GlowButton("Apply", enabled = xText.toDoubleOrNull() != null && yText.toDoubleOrNull() != null) {
            onApply(Vec2(xText.toDouble(), yText.toDouble()))
        }
    }
}

@Composable
private fun ManipulativesScreen(vm: ExplorerViewModel, wide: Boolean) {
    val engine = remember { ManipulativeEngine() }
    val proofEngine = remember { VisualProofEngine() }
    var scene by remember {
        mutableStateOf(
            ManipulativeScene(
                items = listOf(
                    ManipulativeItem("item-1", ManipulativeKind.AlgebraX, Vec2(1.0, 1.0), width = 2.4, height = .8, label = "+x"),
                    ManipulativeItem("item-2", ManipulativeKind.AlgebraUnit, Vec2(4.0, 1.0), label = "+1"),
                    ManipulativeItem("item-3", ManipulativeKind.FractionBar, Vec2(1.0, 3.0), width = 3.0, height = .55, label = "1/2", numerator = 1, denominator = 2),
                    ManipulativeItem("item-4", ManipulativeKind.BalanceWeight, Vec2(5.0, 3.0), label = "3", value = 3.0, side = "left"),
                ),
            ),
        )
    }
    var tray by remember { mutableStateOf(com.indianservers.aiexplorer.core.ManipulativeTray.Algebra) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var formalPreview by remember { mutableStateOf("Move tiles to generate formal maths") }
    var playback by remember { mutableStateOf(proofEngine.start(VisualProofCatalog.labs.first().id)) }
    LaunchedEffect(playback.playing) {
        while (playback.playing) {
            delay(850)
            playback = proofEngine.next(playback)
        }
    }
    val selectedItem = scene.items.firstOrNull { it.id in selected }
    val trayKinds = when (tray) {
        com.indianservers.aiexplorer.core.ManipulativeTray.Algebra -> listOf(ManipulativeKind.AlgebraX, ManipulativeKind.AlgebraUnit)
        com.indianservers.aiexplorer.core.ManipulativeTray.Fractions -> listOf(ManipulativeKind.FractionBar)
        com.indianservers.aiexplorer.core.ManipulativeTray.Numbers -> listOf(ManipulativeKind.IntegerChip, ManipulativeKind.NumberLinePoint, ManipulativeKind.NumberLineInterval)
        com.indianservers.aiexplorer.core.ManipulativeTray.Balance -> listOf(ManipulativeKind.BalanceWeight)
        com.indianservers.aiexplorer.core.ManipulativeTray.Geometry -> listOf(ManipulativeKind.PatternBlock, ManipulativeKind.GeometricTile)
        com.indianservers.aiexplorer.core.ManipulativeTray.Measure -> listOf(ManipulativeKind.Ruler, ManipulativeKind.Protractor, ManipulativeKind.AngleTool)
    }

    @Composable fun ToolPanel(modifier: Modifier = Modifier) {
        GlassPanel(modifier) {
            PanelHeader("Manipulatives", { vm.open(MathModule.Graph2D) }, Cyan, icon = "▦")
            Text("Choose a tray, then add tactile objects.", color = Muted, fontSize = 12.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                com.indianservers.aiexplorer.core.ManipulativeTray.entries.forEach { option -> GlowButton(if (tray == option) "• ${option.name}" else option.name) { tray = option } }
            }
            Text("Tray", color = Ink, fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                trayKinds.forEach { kind -> GlowButton("+ ${kind.name}") { scene = engine.create(scene, kind, Vec2(1.0 + scene.items.size % 5, 1.0 + scene.items.size % 7)); selected = setOf(scene.items.last().id) } }
            }
            Text("Selection", color = Ink, fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                GlowButton("Clone", enabled = selectedItem != null) { selectedItem?.let { scene = engine.duplicate(scene, it.id) } }
                GlowButton("Group", enabled = selected.size >= 2) { scene = engine.group(scene, selected) }
                GlowButton("Ungroup", enabled = selectedItem?.groupId != null) { selectedItem?.groupId?.let { scene = engine.ungroup(scene, it) } }
                GlowButton("Rotate 15°", enabled = selectedItem != null) { selectedItem?.let { scene = engine.transform(scene, it.id, 15.0) } }
                GlowButton("Scale +", enabled = selectedItem != null) { selectedItem?.let { scene = engine.transform(scene, it.id, scaleFactor = 1.15) } }
                GlowButton("Scale -", enabled = selectedItem != null) { selectedItem?.let { scene = engine.transform(scene, it.id, scaleFactor = .87) } }
                GlowButton(if (selectedItem?.locked == true) "Unlock" else "Lock", enabled = selectedItem != null) { selectedItem?.let { scene = engine.setLocked(scene, it.id, !it.locked) } }
                GlowButton("Delete", enabled = selectedItem != null) { selectedItem?.let { scene = engine.remove(scene, it.id); selected = emptySet() } }
            }
            selectedItem?.let { item ->
                OutlinedTextField(value = item.label, onValueChange = { scene = engine.annotate(scene, item.id, it, item.annotation) }, label = { Text("Label") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = item.annotation, onValueChange = { scene = engine.annotate(scene, item.id, item.label, it) }, label = { Text("Annotation") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GlowButton("Value −") { scene = engine.configure(scene, item.id, value = item.value - 1) }
                    GlowButton("Value +") { scene = engine.configure(scene, item.id, value = item.value + 1) }
                }
            }
            Text("Formal maths links", color = Ink, fontWeight = FontWeight.Bold)
            engine.links(scene).forEach { link ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(link.content, color = Cyan, fontFamily = FontFamily.Monospace); Text(link.explanation, color = Muted, fontSize = 10.sp) }
                    GlowButton(link.destination.name) {
                        formalPreview = link.content
                        when (link.destination) {
                            FormalMathDestination.Graph -> { vm.addFunction(link.content); vm.open(MathModule.Graph2D) }
                            FormalMathDestination.Notebook -> { vm.submitNotebook(link.content); vm.openMathNotebook() }
                            FormalMathDestination.Equation -> Unit
                        }
                    }
                }
            }
            Text(formalPreview, color = Green, fontSize = 12.sp)
            Insight("Scene JSON", "${engine.serialize(scene).length} characters", Amber)
        }
    }

    @Composable fun Board(modifier: Modifier = Modifier) {
        GlassPanel(modifier) {
            Text("Snap Board", color = Violet, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Drag objects · tap multiple objects to group · locked objects stay fixed", color = Muted, fontSize = 11.sp)
            Box(
                Modifier.fillMaxWidth().height(520.dp).clip(RoundedCornerShape(18.dp)).background(Color(0x6600060D))
                    .semantics { contentDescription = "Interactive manipulative snap board" },
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val grid = 22.5f
                    var x = 0f; while (x < size.width) { drawLine(Grid, Offset(x, 0f), Offset(x, size.height), 1f); x += grid }
                    var y = 0f; while (y < size.height) { drawLine(Grid, Offset(0f, y), Offset(size.width, y), 1f); y += grid }
                    drawLine(Amber.copy(.5f), Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), 2f)
                }
                scene.items.forEach { item ->
                    val accent = when (item.kind) {
                        ManipulativeKind.AlgebraX, ManipulativeKind.AlgebraUnit -> Cyan
                        ManipulativeKind.FractionBar -> Violet
                        ManipulativeKind.IntegerChip, ManipulativeKind.NumberLinePoint, ManipulativeKind.NumberLineInterval -> Green
                        ManipulativeKind.BalanceWeight -> Amber
                        ManipulativeKind.PatternBlock, ManipulativeKind.GeometricTile -> Color(0xFFFF7AA8)
                        else -> Ink
                    }
                    Column(
                        Modifier.offset { IntOffset((item.position.x * 42).roundToInt(), (item.position.y * 42).roundToInt()) }
                            .width((72 * item.width * item.scale).dp.coerceIn(44.dp, 220.dp)).height((46 * item.height * item.scale).dp.coerceIn(38.dp, 150.dp))
                            .clip(RoundedCornerShape(10.dp)).background(accent.copy(if (item.locked) .2f else .36f))
                            .border(if (item.id in selected) 3.dp else 1.dp, if (item.id in selected) Amber else accent, RoundedCornerShape(10.dp))
                            .pointerInput(item.id, item.position, item.locked) {
                                detectDragGestures { _, delta -> if (!item.locked) scene = engine.move(scene, item.id, item.position + Vec2(delta.x / 42.0, delta.y / 42.0)) }
                            }
                            .clickable { selected = if (item.id in selected) selected - item.id else selected + item.id }
                            .padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(item.label, color = accent, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        if (item.annotation.isNotBlank()) Text(item.annotation, color = Muted, fontSize = 8.sp, maxLines = 2)
                        if (item.locked) Text("LOCK", color = Amber, fontSize = 8.sp)
                    }
                }
            }
        }
    }

    @Composable fun ProofPanel(modifier: Modifier = Modifier) {
        GlassPanel(modifier) {
            Text("Interactive Visual Proofs", color = Cyan, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                VisualProofCatalog.labs.forEach { lab -> GlowButton(if (playback.frame.lab.id == lab.id) "• ${lab.title}" else lab.title) { playback = proofEngine.start(lab.id) } }
            }
            Text(playback.frame.lab.steps[playback.frame.step], color = Ink, fontWeight = FontWeight.Bold)
            Text("Step ${playback.frame.step + 1}/${playback.frame.lab.steps.size}", color = Violet)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                GlowButton(if (playback.playing) "Pause" else "Play") { playback = proofEngine.togglePlaying(playback) }
                GlowButton("Next") { playback = proofEngine.next(playback) }
                GlowButton("Reveal") { playback = proofEngine.reveal(playback) }
            }
            playback.frame.lab.parameters.forEach { parameter ->
                AxisSlider(parameter.name, playback.frame.parameters.getValue(parameter.name).toFloat(), parameter.minimum.toFloat()..parameter.maximum.toFloat()) {
                    playback = proofEngine.setParameter(playback, parameter.name, it.toDouble())
                }
            }
            playback.frame.measurements.forEach { (name, value) -> Insight(name, trim(value), Violet) }
            Insight("Invariant", "${playback.frame.invariant} · residual ${trim(playback.frame.residual)}", if (playback.frame.holds) Green else Amber)
            Text("What changes? ${playback.frame.lab.changesPrompt}", color = Cyan, fontSize = 12.sp)
            Text("What stays same? ${playback.frame.lab.invariantPrompt}", color = Green, fontSize = 12.sp)
            Text(playback.frame.lab.formalResult, color = Amber, fontWeight = FontWeight.Bold)
        }
    }

    if (wide) Row(Modifier.fillMaxSize().padding(top = 78.dp, bottom = 76.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ToolPanel(Modifier.weight(.28f).fillMaxHeight().verticalScroll(rememberScrollState()))
        Board(Modifier.weight(.42f).fillMaxHeight())
        ProofPanel(Modifier.weight(.30f).fillMaxHeight().verticalScroll(rememberScrollState()))
    } else Column(Modifier.fillMaxSize().padding(top = 68.dp, bottom = 66.dp).verticalScroll(rememberScrollState())) {
        ToolPanel(Modifier.fillMaxWidth()); Board(Modifier.fillMaxWidth()); ProofPanel(Modifier.fillMaxWidth())
    }
}

@Composable
private fun Graph2DScreen(vm: ExplorerViewModel) {
    val graph = remember { GraphAnalysis() }
    val advancedGraphEngine = remember { AdvancedGraphEngine() }
    val advancedGraph = remember { AdvancedGraphEngine() }
    val engine = remember { ExpressionEngine() }
    var traceX by remember { mutableFloatStateOf(2f) }
    var graphTool by remember { mutableStateOf(GraphTool.Plot) }
    var parameterA by remember { mutableFloatStateOf(1f) }
    var graphParameterValues by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var playingParameters by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedGraphRowId by remember { mutableStateOf<String?>(null) }
    var equationEditorExpanded by remember { mutableStateOf(false) }
    var graphAddMenuExpanded by remember { mutableStateOf(false) }
    var graphHomeRequest by remember { mutableIntStateOf(0) }
    var graphBackRequest by remember { mutableIntStateOf(0) }
    var graphForwardRequest by remember { mutableIntStateOf(0) }
    var graphViewport by remember { mutableStateOf(GraphViewState()) }
    var graphAxisSettings by remember { mutableStateOf(GraphAxisSettings()) }
    var showAxisSheet by remember { mutableStateOf(false) }
    var showMiniMap by remember { mutableStateOf(false) }
    var comparisonMode by remember { mutableStateOf(false) }
    var graphDomains by remember { mutableStateOf<Map<String, GraphDomainSelection>>(emptyMap()) }
    var graphStyles by remember { mutableStateOf<Map<String, GraphLineStyle>>(emptyMap()) }
    var graphLabelOffsets by remember { mutableStateOf<Map<String, Offset>>(emptyMap()) }
    var graphSnapshots by remember { mutableStateOf<List<GraphSnapshot>>(emptyList()) }
    var graphSnapshotOverlay by remember { mutableStateOf<GraphSnapshot?>(null) }
    var contextMenuPosition by remember { mutableStateOf<Vec2?>(null) }
    var contextMenuFunctionId by remember { mutableStateOf<String?>(null) }
    var parameterHandleEnabled by remember { mutableStateOf(true) }
    var graphTransformKind by remember { mutableStateOf(GraphTransformKind.TranslateX) }
    var graphTransformAmount by remember { mutableFloatStateOf(.5f) }
    var animateGraphTransform by remember { mutableStateOf(false) }
    var dataText by remember { mutableStateOf("-2,4; -1,1; 0,0; 1,1; 2,4") }
    val objectGraphSnapshot = vm.mathObjectGraphSnapshot(graphParameterValues)
    val graphRowMetadata = vm.state.graphRowMetadata
    val graphSliderMetadata = vm.state.graphSliderMetadata
    LaunchedEffect(playingParameters, objectGraphSnapshot.parameterRows, graphSliderMetadata) {
        while (playingParameters.isNotEmpty()) {
            delay(90)
            val currentRows = objectGraphSnapshot.parameterRows.associateBy { it.name }
            playingParameters.forEach { name ->
                val parameter = currentRows[name] ?: return@forEach
                val ui = graphSliderMetadata[name] ?: GraphSliderMetadataState()
                val delta = parameter.step * ui.speed.coerceIn(0.25, 8.0) * ui.direction
                val proposed = parameter.value + delta
                val next = when {
                    ui.mode == GraphSliderPlaybackMode.Loop && proposed > parameter.max -> parameter.min
                    ui.mode == GraphSliderPlaybackMode.Loop && proposed < parameter.min -> parameter.max
                    ui.mode == GraphSliderPlaybackMode.Bounce && proposed > parameter.max -> {
                        vm.updateGraphSliderMetadata(name) { ui.copy(direction = -1) }
                        parameter.max
                    }
                    ui.mode == GraphSliderPlaybackMode.Bounce && proposed < parameter.min -> {
                        vm.updateGraphSliderMetadata(name) { ui.copy(direction = 1) }
                        parameter.min
                    }
                    else -> proposed
                }
                graphParameterValues = graphParameterValues + (name to next)
                if (name == "a") parameterA = next.toFloat()
            }
        }
    }
    LaunchedEffect(animateGraphTransform) {
        while (animateGraphTransform) { delay(45); graphTransformAmount += .04f; if (graphTransformAmount > 2f) graphTransformAmount = -2f }
    }
    val liveFunctions = vm.state.functions.map { function ->
        val resolved = objectGraphSnapshot.graphObjects.firstOrNull { it.rowId == function.id }?.resolvedExpression
        function.copy(expression = resolved ?: function.expression.replace(Regex("\\ba\\b"), trim(parameterA.toDouble())))
    }
    val visibleFunctions = liveFunctions.filter { it.visible }
    val explicitFunctions = visibleFunctions.filter { graph.definitionKind(it.expression) == GraphDefinitionKind.Explicit }
    val selectedFunction = liveFunctions.firstOrNull { it.id == selectedGraphRowId }
    val analysisFunction = selectedFunction?.takeIf { it.visible } ?: visibleFunctions.firstOrNull()
    val primaryExpression = analysisFunction?.takeIf { graph.definitionKind(it.expression) == GraphDefinitionKind.Explicit }?.expression
        ?: explicitFunctions.firstOrNull()?.expression
    val roots = remember(primaryExpression) {
        primaryExpression?.let { runCatching { graph.roots(it, -10.0, 10.0) }.getOrDefault(emptyList()) }.orEmpty()
    }
    val extrema = remember(primaryExpression) {
        primaryExpression?.let { runCatching { graph.extrema(it, -10.0, 10.0) }.getOrDefault(emptyList()) }.orEmpty()
    }
    val adaptiveSample = remember(primaryExpression) {
        primaryExpression?.let { expression ->
            runCatching {
                advancedGraph.adaptiveExplicit(AdvancedGraphDefinition(expression, AdvancedGraphKind.Explicit, GraphDomain(-10.0, 10.0)))
            }.getOrNull()
        }
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
            homeRequest = graphHomeRequest,
            backRequest = graphBackRequest,
            forwardRequest = graphForwardRequest,
            axisSettings = graphAxisSettings,
            domains = graphDomains,
            styles = graphStyles,
            labelOffsets = graphLabelOffsets,
            comparisonMode = comparisonMode,
            showMiniMap = showMiniMap,
            parameterA = parameterA,
            parameterHandleEnabled = parameterHandleEnabled,
            previewExpression = selectedFunction?.let { GraphUxEngine.transform(it.expression, graphTransformKind, graphTransformAmount.toDouble().let { amount -> if (graphTransformKind in setOf(GraphTransformKind.StretchX, GraphTransformKind.StretchY)) kotlin.math.abs(amount).coerceAtLeast(.1) else amount }) },
            snapshotExpressions = graphSnapshotOverlay?.expressions.orEmpty(),
            selectedFunctionId = selectedGraphRowId,
            onSelectFunction = {
                selectedGraphRowId = it
                equationEditorExpanded = true
            },
            onClearSelection = {
                selectedGraphRowId = null
                equationEditorExpanded = false
            },
            onTraceChange = { traceX = it.toFloat().coerceIn(-1_000f, 1_000f) },
            onParameterAChange = { parameterA = it.toFloat().coerceIn(-20f, 20f) },
            onDomainChange = { id, domain -> graphDomains = graphDomains + (id to domain) },
            onLabelMove = { id, delta -> graphLabelOffsets = graphLabelOffsets + (id to ((graphLabelOffsets[id] ?: Offset.Zero) + delta)) },
            onViewportChange = { graphViewport = it },
            onContextMenu = { id, point -> contextMenuFunctionId = id; contextMenuPosition = point },
        )
        InteractionHint(
            "Drag to pan · pinch to zoom · tap a graph to edit",
            Modifier.align(Alignment.BottomEnd),
        )
        FlowRow(Modifier.align(Alignment.TopEnd).padding(top = 72.dp, end = 10.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            GlowButton("Back") { graphBackRequest++ }; GlowButton("Forward") { graphForwardRequest++ }; GlowButton("Fit") { graphHomeRequest++ }
            GlowButton("Axis") { showAxisSheet = !showAxisSheet }; GlowButton(if (showMiniMap) "Map on" else "Map") { showMiniMap = !showMiniMap }
        }
        GraphEquationEditor(
            Modifier.align(Alignment.TopCenter),
            functions = liveFunctions,
            selectedId = selectedGraphRowId,
            expanded = equationEditorExpanded,
            onExpandedChange = { equationEditorExpanded = it },
            addMenuExpanded = graphAddMenuExpanded,
            onToggleAddMenu = { graphAddMenuExpanded = !graphAddMenuExpanded },
            onAddKind = { kind ->
                vm.addFunction(kind.starter)
                selectedGraphRowId = vm.state.functions.lastOrNull()?.id
                equationEditorExpanded = true
                graphAddMenuExpanded = false
                if (kind == GraphAddKind.Table) graphTool = GraphTool.Table
                if (kind == GraphAddKind.Regression) graphTool = GraphTool.Data
            },
            onAdd = {
                vm.addFunction("x")
                selectedGraphRowId = vm.state.functions.lastOrNull()?.id
                equationEditorExpanded = true
            },
            onSelect = { selectedGraphRowId = it; equationEditorExpanded = true },
            onExpressionChange = { id, expression ->
                vm.state.functions.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.let { vm.editExpression(it, expression) }
            },
            onToggleVisible = { id ->
                vm.state.functions.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.let { index -> vm.updateFunction(index) { it.copy(visible = !it.visible) } }
            },
            onDuplicate = { id ->
                vm.state.functions.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.let { vm.duplicateFunction(it) }
                selectedGraphRowId = vm.state.functions.lastOrNull()?.id
            },
            onDelete = { id ->
                val index = vm.state.functions.indexOfFirst { it.id == id }
                if (index >= 0) {
                    vm.deleteFunction(index)
                    selectedGraphRowId = vm.state.functions.getOrNull(index.coerceAtMost(vm.state.functions.lastIndex))?.id
                    if (vm.state.functions.isEmpty()) equationEditorExpanded = false
                }
            },
            onColor = { id ->
                vm.state.functions.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.let { index ->
                    vm.updateFunction(index) { function ->
                        val next = when (function.colorKey) { "cyan" -> "violet"; "violet" -> "green"; "green" -> "amber"; else -> "cyan" }
                        function.copy(colorKey = next)
                    }
                }
            },
            activeTool = graphTool,
            onTool = { graphTool = it },
        )
        selectedFunction?.let { selected ->
            FlowRow(
                Modifier.align(Alignment.CenterEnd).padding(end = 10.dp).clip(RoundedCornerShape(18.dp)).background(SurfaceA.copy(.92f)).border(1.dp, graphColor(selected.colorKey).copy(.55f), RoundedCornerShape(18.dp)).padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(selected.name, color = graphColor(selected.colorKey), modifier = Modifier.padding(8.dp))
                listOf(GraphTool.Trace, GraphTool.Tangent, GraphTool.Derivative, GraphTool.Integral).forEach { tool -> GlowButton(tool.name, onClick = { graphTool = tool }) }
                GlowButton("Domain") { graphDomains = graphDomains + (selected.id to (graphDomains[selected.id] ?: GraphDomainSelection())) }
                GlowButton("Style") { val old = graphStyles[selected.id] ?: GraphLineStyle.Solid; graphStyles = graphStyles + (selected.id to GraphLineStyle.entries[(old.ordinal + 1) % GraphLineStyle.entries.size]) }
            }
        }
        if (showAxisSheet) GlassPanel(Modifier.align(Alignment.Center).widthIn(max = 420.dp)) {
            PanelHeader("Axis Configuration", { showAxisSheet = false }, Cyan)
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                OutlinedTextField(graphAxisSettings.xName, { graphAxisSettings = graphAxisSettings.copy(xName = it.take(8)) }, Modifier.weight(1f), label = { Text("X name") })
                OutlinedTextField(graphAxisSettings.yName, { graphAxisSettings = graphAxisSettings.copy(yName = it.take(8)) }, Modifier.weight(1f), label = { Text("Y name") })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                OutlinedTextField(graphAxisSettings.xUnit, { graphAxisSettings = graphAxisSettings.copy(xUnit = it.take(6)) }, Modifier.weight(1f), label = { Text("X unit") })
                OutlinedTextField(graphAxisSettings.yUnit, { graphAxisSettings = graphAxisSettings.copy(yUnit = it.take(6)) }, Modifier.weight(1f), label = { Text("Y unit") })
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AxisNumberFormat.entries.forEach { format -> TogglePill(format.name, graphAxisSettings.format == format) { graphAxisSettings = graphAxisSettings.copy(format = format) } }
                TogglePill("Grid", graphAxisSettings.gridVisible) { graphAxisSettings = graphAxisSettings.copy(gridVisible = it) }
                TogglePill("Log X", graphAxisSettings.xLogarithmic) { graphAxisSettings = graphAxisSettings.copy(xLogarithmic = it) }
                TogglePill("Log Y", graphAxisSettings.yLogarithmic) { graphAxisSettings = graphAxisSettings.copy(yLogarithmic = it) }
            }
        }
        if (contextMenuPosition != null) GlassPanel(Modifier.align(Alignment.Center).width(245.dp)) {
            PanelHeader("Graph Radial Actions", { contextMenuPosition = null }, Amber)
            Text(contextMenuPosition?.let { "At (${trim(it.x)}, ${trim(it.y)})" }.orEmpty(), color = Muted)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                GraphUxEngine.contextActions(contextMenuFunctionId != null).forEach { action -> GlowButton(action, onClick = {
                    when (action) {
                        "Edit" -> { contextMenuFunctionId?.let { selectedGraphRowId = it }; equationEditorExpanded = true }
                        "Trace" -> graphTool = GraphTool.Trace; "Tangent" -> graphTool = GraphTool.Tangent; "Derivative" -> graphTool = GraphTool.Derivative; "Integral" -> graphTool = GraphTool.Integral
                        "Domain" -> contextMenuFunctionId?.let { graphDomains = graphDomains + (it to (graphDomains[it] ?: GraphDomainSelection())) }
                        "Style" -> contextMenuFunctionId?.let { id -> val old = graphStyles[id] ?: GraphLineStyle.Solid; graphStyles = graphStyles + (id to GraphLineStyle.entries[(old.ordinal + 1) % GraphLineStyle.entries.size]) }
                        "Duplicate" -> contextMenuFunctionId?.let { id -> vm.state.functions.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.let(vm::duplicateFunction) }
                        "Hide" -> contextMenuFunctionId?.let { id -> vm.state.functions.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.let { index -> vm.updateFunction(index) { it.copy(visible = false) } } }
                        "Delete" -> contextMenuFunctionId?.let { id -> vm.state.functions.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.let(vm::deleteFunction) }
                        "Add equation" -> graphAddMenuExpanded = true; "Add point" -> { vm.addFunction("(1,1)"); equationEditorExpanded = true }
                        "Fit view" -> graphHomeRequest++; "Axis settings" -> showAxisSheet = true
                        "Snapshot" -> graphSnapshots = graphSnapshots + GraphSnapshot("View ${graphSnapshots.size + 1}", liveFunctions.map { it.expression }, graphViewport)
                    }; contextMenuPosition = null
                }) }
            }
        }
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
            Text("Desmos-style rows · expressions, sliders and generated tables share one object graph.", color = Muted, fontSize = 11.sp)
            objectGraphSnapshot.expressionRows.forEachIndexed { index, row ->
                val rowUi = graphRowMetadata[row.id] ?: GraphRowMetadataState()
                DesmosExpressionRow(
                    row = row,
                    uiState = rowUi,
                    selected = selectedGraphRowId == row.id,
                    graphObject = objectGraphSnapshot.graphObjects.firstOrNull { it.rowId == row.id },
                    onSelect = { selectedGraphRowId = row.id; equationEditorExpanded = true },
                    onExpressionChange = { vm.editExpression(index, it) },
                    onToggleVisible = { vm.updateFunction(index) { it.copy(visible = !it.visible) } },
                    onDuplicate = { vm.duplicateFunction(index); selectedGraphRowId = vm.state.functions.lastOrNull()?.id },
                    onDelete = {
                        vm.deleteFunction(index)
                        selectedGraphRowId = vm.state.functions.getOrNull(index.coerceAtMost(vm.state.functions.lastIndex))?.id
                    },
                    onToggleCollapsed = {
                        vm.updateGraphRowMetadata(row.id) { it.copy(collapsed = !it.collapsed) }
                    },
                    onNoteChange = { note ->
                        vm.updateGraphRowMetadata(row.id) { it.copy(note = note.take(120)) }
                    },
                    onFolderChange = { folder ->
                        vm.updateGraphRowMetadata(row.id) { it.copy(folder = folder.take(32)) }
                    },
                    onColor = {
                        vm.updateFunction(index) { function ->
                            val next = when (function.colorKey) { "cyan" -> "violet"; "violet" -> "green"; "green" -> "amber"; else -> "cyan" }
                            function.copy(colorKey = next)
                        }
                    },
                )
            }
            if (objectGraphSnapshot.parameterRows.isNotEmpty()) {
                Text("Auto sliders", color = Ink, fontWeight = FontWeight.SemiBold)
                objectGraphSnapshot.parameterRows.forEach { parameter ->
                    val parameterUi = graphSliderMetadata[parameter.name] ?: GraphSliderMetadataState()
                    ParameterRowCard(
                        parameter = parameter,
                        playing = parameter.name in playingParameters,
                        uiState = parameterUi,
                        onTogglePlaying = {
                            playingParameters = if (parameter.name in playingParameters) playingParameters - parameter.name else playingParameters + parameter.name
                        },
                        onToggleMode = {
                            val next = if (parameterUi.mode == GraphSliderPlaybackMode.Loop) GraphSliderPlaybackMode.Bounce else GraphSliderPlaybackMode.Loop
                            vm.updateGraphSliderMetadata(parameter.name) { parameterUi.copy(mode = next, direction = 1) }
                        },
                        onSpeedChange = { speed ->
                            vm.updateGraphSliderMetadata(parameter.name) { parameterUi.copy(speed = speed.coerceIn(0.25, 8.0)) }
                        },
                    ) { value ->
                        graphParameterValues = graphParameterValues + (parameter.name to value)
                        if (parameter.name == "a") parameterA = value.toFloat()
                    }
                }
            }
            GeneratedTablePreview(objectGraphSnapshot.generatedTable)
            Text("Use the + button above the graph for every new user equation.", color = Muted, fontSize = 11.sp)
        }
        if (vm.showRightPanel) GlassPanel(Modifier.align(Alignment.TopEnd).width(270.dp)) {
            PanelHeader("Graph Insights", vm::hidePanels, Violet)
            Insight("Selected row", selectedFunction?.name ?: "Tap a row", Amber)
            Insight("Object graph", "${objectGraphSnapshot.expressionRows.size} rows · ${objectGraphSnapshot.parameterRows.size} sliders", Cyan)
            Insight("Linked table", "${objectGraphSnapshot.generatedTable.size} x-values", Green)
            objectGraphSnapshot.diagnostics.take(1).forEach { Text(it, color = Amber, fontSize = 11.sp) }
            Insight("Tool", graphTool.name, Green)
            Insight("Definitions", "${visibleFunctions.size} visible", Cyan)
            Insight("Kinds", visibleFunctions.map { graph.definitionKind(it.expression).name }.distinct().joinToString(), Violet)
            Insight("Roots", roots.joinToString { trim(it) }.ifBlank { "none detected" }, Cyan)
            Insight("Extrema", extrema.joinToString { "(${trim(it.x)}, ${trim(it.y)})" }.ifBlank { "none detected" }, Green)
            adaptiveSample?.let { sample ->
                Insight("Adaptive sample", "${sample.points.size} points · ${sample.segments.size} segments", Cyan)
                Insight("Arc length", trim(advancedGraph.arcLength(sample)), Violet)
                Insight("Detected breaks", "${sample.discontinuities}", Amber)
            }
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
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                TogglePill("Compare", comparisonMode) { comparisonMode = it }
                TogglePill("Mini-map", showMiniMap) { showMiniMap = it }
                TogglePill("Parameter handles", parameterHandleEnabled) { parameterHandleEnabled = it }
                GlowButton("Save snapshot") { graphSnapshots = graphSnapshots + GraphSnapshot("View ${graphSnapshots.size + 1}", liveFunctions.map { it.expression }, graphViewport) }
                selectedFunction?.let { selected ->
                    val selectedIndex = vm.state.functions.indexOfFirst { it.id == selected.id }
                    GlowButton("Layer up") { vm.moveFunctionLayer(selectedIndex, -1) }; GlowButton("Layer down") { vm.moveFunctionLayer(selectedIndex, 1) }
                    GraphTransformKind.entries.forEach { transformKind -> TogglePill(transformKind.name, graphTransformKind == transformKind) { graphTransformKind = transformKind } }
                    TogglePill("Animate transform", animateGraphTransform) { animateGraphTransform = it }
                    GlowButton("Apply preview") {
                        val transformed = GraphUxEngine.transform(selected.expression, graphTransformKind, graphTransformAmount.toDouble().let { if (graphTransformKind in setOf(GraphTransformKind.StretchX, GraphTransformKind.StretchY)) kotlin.math.abs(it).coerceAtLeast(.1) else it })
                        vm.duplicateFunction(selectedIndex); val newIndex = vm.state.functions.lastIndex; vm.editExpression(newIndex, transformed); selectedGraphRowId = vm.state.functions.getOrNull(newIndex)?.id
                    }
                }
            }
            if (selectedFunction != null) AxisSlider("Transform amount", graphTransformAmount, -2f..2f) { graphTransformAmount = it }
            if (graphSnapshots.isNotEmpty()) {
                Text("Saved graph states", color = Ink, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { graphSnapshots.forEach { snapshot -> GlowButton(if (graphSnapshotOverlay == snapshot) "Overlay on: ${snapshot.name}" else snapshot.name, onClick = { graphSnapshotOverlay = if (graphSnapshotOverlay == snapshot) null else snapshot }) } }
            }
            selectedGraphRowId?.let { id -> graphDomains[id]?.let { domain ->
                Text("Domain ${if (domain.leftClosed) "[" else "("}${trim(domain.minimum)}, ${trim(domain.maximum)}${if (domain.rightClosed) "]" else ")"} - drag axis handles to paint", color = Amber)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { TogglePill("Left closed", domain.leftClosed) { graphDomains = graphDomains + (id to domain.copy(leftClosed = it)) }; TogglePill("Right closed", domain.rightClosed) { graphDomains = graphDomains + (id to domain.copy(rightClosed = it)) }; GlowButton("Clear domain") { graphDomains = graphDomains - id } }
            } }
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
private fun DesmosExpressionRow(
    row: com.indianservers.aiexplorer.workspace.MathExpressionRow,
    uiState: GraphRowMetadataState,
    selected: Boolean,
    graphObject: com.indianservers.aiexplorer.workspace.MathGraphObject?,
    onSelect: () -> Unit,
    onExpressionChange: (String) -> Unit,
    onToggleVisible: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onToggleCollapsed: () -> Unit,
    onNoteChange: (String) -> Unit,
    onFolderChange: (String) -> Unit,
    onColor: () -> Unit,
) {
    val accent = graphColor(row.metadata.colorKey)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) accent.copy(alpha = .16f) else Color(0x33101824))
            .border(if (selected) 2.dp else 1.dp, accent.copy(alpha = if (selected) .88f else .38f), RoundedCornerShape(16.dp))
            .clickable { onSelect() }
            .padding(9.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(10.dp).clip(RoundedCornerShape(8.dp)).background(accent))
            Text(row.name, color = Ink, fontWeight = FontWeight.Bold, modifier = Modifier.widthIn(min = 46.dp))
            if (selected) Text("ACTIVE", color = Amber, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(
                if (row.metadata.visible) "VISIBLE" else "HIDDEN",
                color = if (row.metadata.visible) Green else Muted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            if (uiState.folder.isNotBlank()) Text(uiState.folder, color = Amber, fontSize = 10.sp, maxLines = 1)
            Text(graphObject?.algebra?.classification ?: "waiting", color = Muted, fontSize = 10.sp)
        }
        AnimatedVisibility(!uiState.collapsed) {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                OutlinedTextField(
                    value = row.expression,
                    onValueChange = onExpressionChange,
                    label = { Text("Expression") },
                    visualTransformation = MathSyntaxVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Expression row ${row.name}" },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    OutlinedTextField(
                        value = uiState.folder,
                        onValueChange = onFolderChange,
                        label = { Text("Folder") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = uiState.note,
                        onValueChange = onNoteChange,
                        label = { Text("Note") },
                        singleLine = true,
                        modifier = Modifier.weight(1.35f),
                    )
                }
            }
        }
        graphObject?.let {
            Text("Resolved: ${it.resolvedExpression}", color = Cyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace, maxLines = 2)
            Text("Table rows ${it.table.size} · roots ${it.roots.size} · extrema ${it.extrema.size}", color = Muted, fontSize = 10.sp)
        }
        row.metadata.error?.let { Text("Check: $it", color = Amber, fontSize = 11.sp) }
        if (uiState.note.isNotBlank()) Text(uiState.note, color = Muted, fontSize = 11.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            GlowButton(if (uiState.collapsed) "Expand" else "Collapse", onClick = onToggleCollapsed)
            GlowButton(if (row.metadata.visible) "Hide" else "Show", onClick = onToggleVisible)
            GlowButton("Color", onClick = onColor)
            GlowButton("Duplicate", onClick = onDuplicate)
            GlowButton("Delete", onClick = onDelete)
        }
    }
}

@Composable
private fun ParameterRowCard(
    parameter: com.indianservers.aiexplorer.workspace.MathParameterRow,
    playing: Boolean,
    uiState: GraphSliderMetadataState,
    onTogglePlaying: () -> Unit,
    onToggleMode: () -> Unit,
    onSpeedChange: (Double) -> Unit,
    onValueChange: (Double) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x2230D9FF))
            .border(1.dp, Cyan.copy(.25f), RoundedCornerShape(14.dp))
            .padding(9.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(parameter.name, color = Ink, fontWeight = FontWeight.Bold)
            Text("value ${trim(parameter.value)}", color = Cyan, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            GlowButton(if (playing) "Pause" else "Play", onClick = onTogglePlaying)
            GlowButton(uiState.mode.name, onClick = onToggleMode)
            Text(if (playing) "Animating linked graph/table" else "Tap Play to animate", color = Muted, fontSize = 10.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            GlowButton("Slower", onClick = { onSpeedChange(uiState.speed / 2.0) })
            Text("${trim(uiState.speed)}x", color = Cyan, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            GlowButton("Faster", onClick = { onSpeedChange(uiState.speed * 2.0) })
        }
        Text("range ${trim(parameter.min)} to ${trim(parameter.max)} · step ${trim(parameter.step)} · ${uiState.mode.name.lowercase()} · direction ${uiState.direction}", color = Muted, fontSize = 10.sp)
        Slider(
            value = parameter.value.toFloat().coerceIn(parameter.min.toFloat(), parameter.max.toFloat()),
            onValueChange = { raw ->
                val stepped = (round(raw.toDouble() / parameter.step) * parameter.step).coerceIn(parameter.min, parameter.max)
                onValueChange(stepped)
            },
            valueRange = parameter.min.toFloat()..parameter.max.toFloat(),
            modifier = Modifier.semantics { contentDescription = "Parameter slider ${parameter.name}" },
        )
        Text("Drag to update all dependent graph rows, table outputs and algebra facts.", color = Muted, fontSize = 10.sp)
    }
}

@Composable
private fun GeneratedTablePreview(rows: List<com.indianservers.aiexplorer.workspace.MathGeneratedTableRow>) {
    if (rows.isEmpty()) return
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x22101824))
            .border(1.dp, Green.copy(.22f), RoundedCornerShape(14.dp))
            .padding(9.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text("Generated table", color = Green, fontWeight = FontWeight.Bold)
        rows.take(5).forEach { row ->
            Text(
                "x=${trim(row.input)}  " + row.outputs.entries.joinToString("  ") { "${it.key}:${trim(it.value)}" },
                color = Muted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
        if (rows.size > 5) Text("+${rows.size - 5} more linked rows", color = Muted, fontSize = 10.sp)
    }
}

private fun graphColor(key: String): Color = when (key) {
    "violet" -> Violet
    "green" -> Green
    "amber" -> Amber
    else -> Cyan
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
    var axisConstraint by remember { mutableStateOf(AxisConstraint.Free) }
    var precisionMode by remember { mutableStateOf(false) }
    var gestureMode by remember { mutableStateOf(GestureMode.Idle) }
    var lockedSolidIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var sectionEnabled by remember { mutableStateOf(false) }
    var clipSection by remember { mutableStateOf(false) }
    var sectionY by remember { mutableFloatStateOf(0f) }
    val selectedIndex = vm.selectedSolid.coerceIn(0, vm.state.solids.lastIndex.coerceAtLeast(0))
    val selectedSolid = vm.state.solids.getOrNull(selectedIndex)
    val selectedBounds = remember(selectedSolid) {
        selectedSolid?.let { solid ->
            AnalyticGeometry3D.bounds(SolidMeshFactory.create(solid).vertices.map { it + solid.position })
        }
    }
    val selectedVectorIndex = vm.selectedVector3D.coerceIn(0, vm.state.vectors3D.lastIndex.coerceAtLeast(0))
    val selectedVector = vm.state.vectors3D.getOrNull(selectedVectorIndex)
    val sharedRenderScene = remember(vm.state.solids, vm.state.vectors3D) {
        SharedSpatialSceneBuilder.build("geometry-3d-workspace", vm.state.solids, vectors = vm.state.vectors3D)
    }
    val sharedRenderPlan = remember(sharedRenderScene) { SharedGpuSceneCompiler.compile(sharedRenderScene) }
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
                if (it !in lockedSolidIndices) vm.beginSolidDrag(it)
            },
            onSolidMove = { index, delta ->
                val constrained = PrecisionInteraction.apply(SmartSnapEngine.constrain(delta, axisConstraint), precisionMode)
                val snapped = if (vm.settings.snap) Vec3(round(constrained.x * 4) / 4, round(constrained.y * 4) / 4, round(constrained.z * 4) / 4) else constrained
                vm.previewSolidDrag(index, snapped)
            },
            onSolidRotate = vm::previewSolidRotation,
            onSolidScale = vm::previewSolidScale,
            onSolidDragEnd = vm::endSolidDrag,
            onSolidDragCancel = vm::cancelSolidDrag,
            onVectorDragStart = {
                if (vm.settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.beginVectorDrag(it)
            },
            onVectorMove = { index, delta -> vm.previewVectorDrag(index, PrecisionInteraction.apply(SmartSnapEngine.constrain(delta, axisConstraint), precisionMode)) },
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
            onGestureModeChange = { gestureMode = it },
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
        Row(Modifier.align(Alignment.TopStart).padding(top = 72.dp, start = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GlowButton("⌂ Home") { rotateX = 25f; rotateY = -35f; rotateZ = 15f; zoom = 1f; cameraPan = Offset.Zero }
            if (gestureMode != GestureMode.Idle) Text(gestureMode.label, color = Cyan, modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(SurfaceA).padding(9.dp))
            Text("${trim(zoom.toDouble())}×", color = Muted, modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(SurfaceA).padding(9.dp))
        }
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
            Insight("Shared GPU", "${sharedRenderScene.primitives.size} objects · ${sharedRenderPlan.vertices.size / 10} vertices", Cyan)
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
                GlowButton(if (selectedIndex in lockedSolidIndices) "Unlock object" else "Lock object") {
                    lockedSolidIndices = if (selectedIndex in lockedSolidIndices) lockedSolidIndices - selectedIndex else lockedSolidIndices + selectedIndex
                }
                selectedBounds?.let { bounds ->
                    Insight("Bounds min", "${trim(bounds.minimum.x)},${trim(bounds.minimum.y)},${trim(bounds.minimum.z)}", Green)
                    Insight("Bounds max", "${trim(bounds.maximum.x)},${trim(bounds.maximum.y)},${trim(bounds.maximum.z)}", Green)
                }
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
            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf(AxisConstraint.Free, AxisConstraint.X, AxisConstraint.Y, AxisConstraint.Z).forEach { axis ->
                    GlowButton(if (axisConstraint == axis) "● ${axis.name}" else axis.name) { axisConstraint = axis }
                }
                GlowButton(if (precisionMode) "● Precision" else "Precision") { precisionMode = !precisionMode }
            }
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
    val activity = context as? ComponentActivity
    val controller = remember { ARCoreSessionController() }
    var capabilities by remember { mutableStateOf(ARCapabilities(ARAvailability.Checking)) }
    var liveAR by remember { mutableStateOf(false) }
    var frameState by remember { mutableStateOf<ARFrameState?>(null) }
    var liveError by remember { mutableStateOf("") }
    var selectedLesson by remember { mutableIntStateOf(0) }
    var thermalLevel by remember { mutableStateOf(ThermalLevel.Nominal) }
    var showSpatialDetails by remember { mutableStateOf(false) }
    val currentLiveAR by rememberUpdatedState(liveAR)
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
    LaunchedEffect(capabilities.message, cameraGranted) {
        if (cameraGranted && capabilities.message.contains("session configured", ignoreCase = true)) {
            controller.resume().onSuccess { liveAR = true }.onFailure { liveError = it.message ?: "ARCore could not resume." }
        }
    }
    DisposableEffect(controller, activity) {
        val observer = object : DefaultLifecycleObserver {
            override fun onPause(owner: LifecycleOwner) = controller.pause()
            override fun onResume(owner: LifecycleOwner) { if (currentLiveAR) controller.resume() }
        }
        activity?.lifecycle?.addObserver(observer)
        onDispose {
            activity?.lifecycle?.removeObserver(observer)
            controller.pause()
            controller.close()
        }
    }

    val placement = vm.state.spatialPlacement
    val guidance = SpatialSafety.guidance(placement.trackingQuality)
    val policy = remember(thermalLevel) { SpatialPerformanceManager.policy(thermalLevel, 22.0) }
    val surfaceMesh = remember(vm.state.surfaceExpression, policy.surfaceDensity) {
        runCatching { Graph3D().mesh(vm.state.surfaceExpression, density = policy.surfaceDensity) }.getOrNull()
    }
    val lesson = SpatialLessonCatalog.lessons[selectedLesson]
    val sharedScene = remember(vm.state.solids, vm.state.vectors3D, surfaceMesh, lesson.id, placement.depthOcclusionEnabled, frameState?.lighting?.pixelIntensity) {
        val probability = if (lesson.id == "probability-surface-ar") surfaceMesh else null
        SharedSpatialSceneBuilder.build(
            id = "spatial-${lesson.id}",
            solids = vm.state.solids,
            surface = if (probability == null) surfaceMesh else null,
            vectors = vm.state.vectors3D,
            probabilitySurface = probability,
            annotations = listOf(
                SpatialAnnotation("origin", Vec3(0.0, 0.0, 0.0), "O"),
                SpatialAnnotation("lesson", Vec3(0.0, 1.5, 0.0), lesson.title),
            ),
        ).copy(depthOcclusion = placement.depthOcclusionEnabled, environmentIntensity = frameState?.lighting?.pixelIntensity ?: 1f)
    }
    val gpuPlan = remember(sharedScene) { SharedGpuSceneCompiler.compile(sharedScene) }
    val currentCompositorScene by rememberUpdatedState(SpatialCompositorScene(sharedScene, placement))
    Box(Modifier.fillMaxSize()) {
        if (liveAR) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { viewContext ->
                    ARCoreCompositorView(
                        viewContext,
                        controller,
                        sceneProvider = { currentCompositorScene },
                        onFrame = { frameState = it; liveError = "" },
                        onError = { liveError = it },
                    )
                },
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            vm.beginSpatialGesture()
                            var totalPan = Offset.Zero
                            var totalRotation = 0f
                            var totalScale = 1f
                            while (true) {
                                val event = awaitPointerEvent()
                                totalPan += event.calculatePan()
                                totalRotation += event.calculateRotation()
                                totalScale *= event.calculateZoom()
                                vm.previewSpatialGesture(totalPan, totalRotation, totalScale)
                                if (event.changes.none { it.pressed }) break
                                event.changes.forEach { it.consume() }
                            }
                            vm.endSpatialGesture()
                            if (totalPan.getDistance() < 12f && kotlin.math.abs(totalRotation) < 2f && kotlin.math.abs(totalScale - 1f) < .03f) {
                                controller.hitTest(down.position.x, down.position.y).firstOrNull()?.let { hit ->
                                    controller.createAnchor(hit, lessonId = lesson.id)
                                        .onSuccess { anchor -> vm.placeSpatialHit(hit.copy(trackableId = anchor.id, positionMeters = anchor.pose.positionMeters)) }
                                        .onFailure { liveError = it.message ?: "Could not create the spatial anchor." }
                                }
                            }
                        }
                    },
            )
        } else {
            SpatialPreviewCanvas(
                modifier = Modifier.fillMaxSize(),
                solids = vm.state.solids,
                placement = placement,
                onGestureStart = vm::beginSpatialGesture,
                onGesture = vm::previewSpatialGesture,
                onGestureEnd = vm::endSpatialGesture,
            )
        }
        GlassPanel(Modifier.align(Alignment.TopStart).width(285.dp).padding(top = 105.dp)) {
            PanelHeader("AR Spatial Lab", { vm.open(MathModule.Geometry3D) }, Cyan)
            Insight("Mode", if (liveAR) "Live ARCore camera + shared GPU scene" else "Accessible spatial simulator", Cyan)
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
            if (liveAR) GlowButton("Use simulator", onClick = { liveAR = false; controller.pause() })
            Insight("Scale", placement.visibleScale, Violet)
            Insight("Estimate", "±${trim(placement.measurementUncertaintyMeters)} m · educational only", Amber)
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
            GlowButton(if (showSpatialDetails) "Hide spatial details" else "Lessons & renderer", onClick = { showSpatialDetails = !showSpatialDetails })
            if (showSpatialDetails) {
                Text("Spatial lesson", color = Ink, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SpatialLessonCatalog.lessons.forEachIndexed { index, item ->
                        GlowButton(if (index == selectedLesson) "• ${item.title.take(12)}" else item.title.take(12), onClick = { selectedLesson = index })
                    }
                }
                Text(lesson.learningGoal, color = Muted, fontSize = 11.sp)
                Insight("Shared renderer", "${sharedScene.primitives.size} objects · ${gpuPlan.vertices.size / 10} GPU vertices", Cyan)
                Insight("Lighting", if (frameState?.lighting?.valid == true) "${trim(frameState?.lighting?.pixelIntensity?.toDouble() ?: 1.0)}× environment" else "Simulator neutral light", Green)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ThermalLevel.entries.forEach { level -> GlowButton(level.name.take(4), onClick = { thermalLevel = level }) }
                }
                Insight("Performance", "${policy.quality.name} · ${policy.targetFps} fps · mesh ${policy.surfaceDensity}", if (thermalLevel >= ThermalLevel.Severe) Amber else Green)
                Insight("Privacy & safety", "${ARPrivacySafetyChecklist.items.size} mandatory checks · camera frames stay local", Violet)
                if (liveError.isNotBlank()) Text(liveError, color = Amber, fontSize = 11.sp)
            }
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
    val surfaceCalculus = remember { SurfaceCalculus() }
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
    val differential = remember(vm.state.surfaceExpression, traceX, traceY) {
        runCatching { surfaceCalculus.analyze(vm.state.surfaceExpression, traceX.toDouble(), traceY.toDouble()) }.getOrNull()
    }
    val sharedSurfaceScene = remember(mesh) { SharedSpatialSceneBuilder.build("graph-3d-workspace", emptyList(), surface = mesh) }
    val sharedSurfacePlan = remember(sharedSurfaceScene) { SharedGpuSceneCompiler.compile(sharedSurfaceScene) }
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
            Insight("Shared GPU renderer", "${sharedSurfacePlan.vertices.size / 10} vertices · ${sharedSurfacePlan.triangleIndices.size / 3} triangles", Cyan)
            Insight("Surface", insight.classification, Cyan)
            Insight("Vertex", insight.vertex?.let { "(${trim(it.x)}, ${trim(it.y)}, ${trim(it.z)})" } ?: "sampled", Violet)
            Insight("Range", insight.range, Cyan)
            Insight("Symmetry", insight.symmetry, Violet)
            Insight("Trace", "(${trim(traceX.toDouble())}, ${trim(traceY.toDouble())})", Green)
            differential?.let { value ->
                Insight("Gradient", "(${trim(value.gradient.x)}, ${trim(value.gradient.y)})", Green)
                Insight("Unit normal", "(${trim(value.unitNormal.x)}, ${trim(value.unitNormal.y)}, ${trim(value.unitNormal.z)})", Amber)
            }
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

private enum class TrigLab(val label: String) {
    Circle("Circle + Waves"), Identities("Identity Lab"), Triangle("Triangle Solver"),
    Equations("Equations"), Polar("Polar Curves"), Applications("Heights + Distance"), Harmonics("Fourier Lab")
}
private enum class TrigLineStyle { Solid, Dashed, Dotted }

@Composable
private fun TrigonometryScreen(vm: ExplorerViewModel) {
    var angle by remember { mutableFloatStateOf(45f) }
    var amplitude by remember { mutableFloatStateOf(1f) }
    var period by remember { mutableFloatStateOf((2 * Math.PI).toFloat()) }
    var phase by remember { mutableFloatStateOf(0f) }
    var verticalShift by remember { mutableFloatStateOf(0f) }
    var function by remember { mutableStateOf(TrigFunction.Sine) }
    var visibleFunctions by remember { mutableStateOf(setOf(TrigFunction.Sine)) }
    var angleUnit by remember { mutableStateOf(TrigAngleUnit.Degrees) }
    var lab by remember { mutableStateOf(TrigLab.Circle) }
    var identityIndex by remember { mutableIntStateOf(0) }
    var identityStep by remember { mutableIntStateOf(0) }
    var showTangents by remember { mutableStateOf(true) }
    var showProjections by remember { mutableStateOf(true) }
    var showWave by remember { mutableStateOf(true) }
    var showAsymptotes by remember { mutableStateOf(true) }
    var homeRequest by remember { mutableIntStateOf(0) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var animateAngle by remember { mutableStateOf(false) }
    var rotationDirection by remember { mutableIntStateOf(1) }
    var lineStyle by remember { mutableStateOf(TrigLineStyle.Solid) }
    var paletteShift by remember { mutableIntStateOf(0) }
    var inverseFunction by remember { mutableStateOf(InverseTrigFunction.ArcSine) }
    var inverseInput by remember { mutableFloatStateOf(.5f) }
    var equationTarget by remember { mutableFloatStateOf(.5f) }
    var polarType by remember { mutableStateOf(PolarCurveType.Rose) }
    var polarParameter by remember { mutableFloatStateOf(3f) }
    var triangleA by remember { mutableFloatStateOf(4f) }
    var triangleB by remember { mutableFloatStateOf(5f) }
    var triangleMode by remember { mutableStateOf("SAS") }
    var observationDistance by remember { mutableFloatStateOf(20f) }
    var observerHeight by remember { mutableFloatStateOf(1.6f) }
    var harmonic1 by remember { mutableFloatStateOf(1f) }
    var harmonic2 by remember { mutableFloatStateOf(.5f) }
    var harmonic3 by remember { mutableFloatStateOf(.25f) }

    LaunchedEffect(animateAngle) {
        while (animateAngle) { delay(32); angle += rotationDirection; if (angle > 180f) angle = -180f; if (angle < -180f) angle = 180f }
    }

    val radians = Math.toRadians(angle.toDouble())
    val snapshot = remember(angle) { InteractiveTrigEngine.snapshot(radians) }
    val transform = TrigTransform(amplitude.toDouble(), period.toDouble(), phase.toDouble(), verticalShift.toDouble())
    val identityLab = remember { InteractiveTrigIdentityLab() }
    val identity = remember(identityIndex) { identityLab.verify(identityIndex) }
    val safeAngle = kotlin.math.abs(angle.toDouble()).coerceIn(1.0, 178.0)
    val triangle = remember(triangleA, triangleB, safeAngle, triangleMode) {
        when (triangleMode) {
            "SSS" -> TriangleTrigSolver.sss(triangleA.toDouble(), triangleB.toDouble(), ((triangleA + triangleB) * .72f).toDouble())
            "SSA" -> TriangleTrigSolver.ssa(triangleA.toDouble(), triangleB.toDouble(), safeAngle).firstOrNull()
                ?: TriangleTrigSolver.sas(triangleA.toDouble(), triangleB.toDouble(), safeAngle)
            "ASA" -> TriangleTrigSolver.asa(triangleA.toDouble(), safeAngle.coerceAtMost(120.0), ((180 - safeAngle) / 2).coerceAtLeast(1.0))
            "AAS" -> TriangleTrigSolver.aas(triangleA.toDouble(), safeAngle.coerceAtMost(120.0), ((180 - safeAngle) / 2).coerceAtLeast(1.0))
            else -> TriangleTrigSolver.sas(triangleA.toDouble(), triangleB.toDouble(), safeAngle)
        }
    }
    val displayAngle = InteractiveTrigEngine.fromRadians(radians, angleUnit)
    val inverseRadians = InteractiveTrigEngine.inverse(inverseInput.toDouble(), inverseFunction)
    val roots = remember(function, equationTarget) { InteractiveTrigEngine.equationRoots(function, equationTarget.toDouble(), 0.0, 2 * Math.PI) }
    val polar = remember(polarType, polarParameter) { InteractiveTrigEngine.polarSamples(polarType, polarParameter.toDouble()) }
    val harmonics = listOf(HarmonicComponent(harmonic1.toDouble(), 1), HarmonicComponent(harmonic2.toDouble(), 2), HarmonicComponent(harmonic3.toDouble(), 3))

    Box(Modifier.fillMaxSize()) {
        TrigCanvas(
            modifier = Modifier.fillMaxSize(), angleDegrees = angle, transform = if (lab == TrigLab.Equations) TrigTransform() else transform, function = function,
            showTangents = showTangents, showProjections = showProjections, showWave = showWave,
            homeRequest = homeRequest, onZoomChanged = { zoom = it }, onAngleChange = { angle = snapAngle(it) },
            visibleFunctions = if (lab == TrigLab.Equations) setOf(function) else visibleFunctions, showAsymptotes = showAsymptotes,
            polarSamples = if (lab == TrigLab.Polar) polar else emptyList(),
            harmonics = if (lab == TrigLab.Harmonics) harmonics else emptyList(),
            lineStyle = lineStyle,
            paletteShift = paletteShift,
            equationTarget = equationTarget.takeIf { lab == TrigLab.Equations },
            equationRoots = if (lab == TrigLab.Equations) roots.map { it.radians } else emptyList(),
            onTransformChange = { a, p, h, k -> amplitude = a; period = p; phase = h; verticalShift = k },
        )
        InteractionHint("Drag circle/wave handles - pinch to zoom - two fingers pan - double tap fits", Modifier.align(Alignment.BottomEnd))
        Row(Modifier.align(Alignment.TopStart).padding(top = 72.dp, start = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GlowButton("Home") { homeRequest++ }; Text("${trim(zoom.toDouble())}x", color = Muted, modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(SurfaceA).padding(9.dp))
        }
        FloatingPanelLaunchers(Modifier.align(Alignment.CenterStart), "Trig Labs", "Insights", "Controls", { vm.togglePanel(PanelSlot.Left) }, { vm.togglePanel(PanelSlot.Right) }, { vm.togglePanel(PanelSlot.Bottom) })
        if (vm.showLeftPanel) GlassPanel(Modifier.align(Alignment.TopStart).width(220.dp)) {
            PanelHeader("Trigonometry Labs", vm::hidePanels, Cyan)
            Text("Seven tactile labs cover the complete toolkit.", color = Muted, fontSize = 12.sp)
            TrigLab.entries.forEach { item -> GlowButton(if (lab == item) "* ${item.label}" else item.label, onClick = { lab = item }) }
        }
        if (vm.showRightPanel) GlassPanel(Modifier.align(Alignment.TopEnd).width(270.dp)) {
            PanelHeader("Live Insights", vm::hidePanels, Violet)
            Insight("Angle", "${trim(displayAngle.display)}${displayAngle.suffix}", Cyan)
            Insight("Quadrant", "Q${snapshot.quadrant} - reference ${trim(snapshot.referenceAngleDegrees)} deg", Cyan)
            Insight("sin / cos", "${snapshot.exactSine ?: trim(snapshot.sine)} / ${snapshot.exactCosine ?: trim(snapshot.cosine)}", Violet)
            Insight("tan", snapshot.exactTangent ?: snapshot.tangent?.let(::trim) ?: "undefined", Green)
            Insight("Triangle", "c=${trim(triangle.c)}, area=${trim(triangle.area)}", Amber)
            when (lab) {
                TrigLab.Identities -> Insight(identity.label, if (identity.evidence.equivalent) "Verified" else identity.evidence.explanation, Green)
                TrigLab.Equations -> Insight("Roots in [0, 2pi]", roots.joinToString { it.exactLabel ?: "${trim(it.degrees)} deg" }.ifBlank { "None" }, Amber)
                TrigLab.Applications -> Insight("Estimated height", "${trim(InteractiveTrigEngine.heightFromObservation(observationDistance.toDouble(), safeAngle.coerceAtMost(89.0), observerHeight.toDouble()))} units", Amber)
                TrigLab.Polar -> Insight("Polar", "${polarType.name} - ${polar.size} samples", Violet)
                TrigLab.Harmonics -> Insight("Composite", "sum A_n sin(nx + phase)", Violet)
                else -> Unit
            }
        }
        if (vm.showBottomPanel) GlassPanel(Modifier.align(Alignment.BottomStart).fillMaxWidth()) {
            PanelHeader("${lab.label} Controls", vm::hidePanels, Ink)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                TrigAngleUnit.entries.forEach { unit -> TogglePill(unit.name, angleUnit == unit) { angleUnit = unit } }
                TogglePill(if (animateAngle) "Pause" else "Animate", animateAngle) { animateAngle = it }
                GlowButton(if (rotationDirection > 0) "CCW +" else "CW -", onClick = { rotationDirection *= -1 })
                listOf(-180f, -90f, 0f, 30f, 45f, 60f, 90f, 180f).forEach { value -> GlowButton("${trim(value.toDouble())} deg", onClick = { angle = value }) }
            }
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Angle ${trim(displayAngle.display)}${displayAngle.suffix}", color = Ink, modifier = Modifier.width(155.dp)); Slider(angle, { angle = snapAngle(it) }, valueRange = -180f..180f, modifier = Modifier.weight(1f)) }
            when (lab) {
                TrigLab.Circle -> {
                    AxisSlider("Amplitude", amplitude, .25f..3f) { amplitude = it }; AxisSlider("Period", period, 1f..12.57f) { period = it }; AxisSlider("Phase", phase, -3.14f..3.14f) { phase = it }; AxisSlider("Vertical", verticalShift, -2f..2f) { verticalShift = it }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) { TrigFunction.entries.forEach { item -> TogglePill(item.name, item in visibleFunctions) { enabled -> visibleFunctions = if (enabled) visibleFunctions + item else (visibleFunctions - item).ifEmpty { setOf(item) }; function = item } }; TogglePill("Asymptotes", showAsymptotes) { showAsymptotes = it }; TrigLineStyle.entries.forEach { style -> TogglePill(style.name, lineStyle == style) { lineStyle = style } }; GlowButton("Cycle colours", onClick = { paletteShift++ }) }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) { InverseTrigFunction.entries.forEach { item -> TogglePill(item.name, inverseFunction == item) { inverseFunction = item } } }; AxisSlider("Inverse input", inverseInput, -1f..1f) { inverseInput = it }; Text("Result ${radianLabel(Math.toDegrees(inverseRadians))} rad = ${trim(Math.toDegrees(inverseRadians))} deg", color = Green)
                }
                TrigLab.Identities -> {
                    val steps = listOf("Start: ${identity.left}", "Check domain: ${identity.evidence.leftDomain.description}", "Apply the named identity: ${identity.label}", "Simplify toward ${identity.right}", if (identity.evidence.equivalent) "Verified: both sides agree" else identity.evidence.explanation)
                    Text("${identity.left} = ${identity.right}", color = Ink); Text(steps[identityStep.coerceIn(steps.indices)], color = if (identityStep == steps.lastIndex) Green else Muted)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { GlowButton("Previous step", onClick = { identityStep = (identityStep - 1).coerceAtLeast(0) }); GlowButton("Next step", onClick = { identityStep = (identityStep + 1).coerceAtMost(steps.lastIndex) }); GlowButton("Next identity", onClick = { identityIndex = (identityIndex + 1) % identityLab.catalog.size; identityStep = 0 }) }
                }
                TrigLab.Triangle -> { FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) { listOf("SSS", "SAS", "SSA", "ASA", "AAS").forEach { item -> TogglePill(item, triangleMode == item) { triangleMode = item } } }; AxisSlider("Side a", triangleA, 1f..12f) { triangleA = it }; AxisSlider("Side b", triangleB, 1f..12f) { triangleB = it }; Text("a=${trim(triangle.a)}, b=${trim(triangle.b)}, c=${trim(triangle.c)} - A=${trim(triangle.angleA)} deg, B=${trim(triangle.angleB)} deg, C=${trim(triangle.angleC)} deg", color = Green); if (triangleMode == "SSA") Text("Ambiguous SSA: ${TriangleTrigSolver.ssa(triangleA.toDouble(), triangleB.toDouble(), safeAngle).size} valid solution(s)", color = Amber); Text("Sine and cosine rules update with the construction.", color = Muted) }
                TrigLab.Equations -> { FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) { TrigFunction.entries.forEach { item -> TogglePill(item.name, function == item) { function = item } } }; AxisSlider("Equation target", equationTarget, -2f..2f) { equationTarget = it }; Text("${function.name}(x) = ${trim(equationTarget.toDouble())}: ${roots.joinToString { it.exactLabel ?: "${trim(it.degrees)} deg" }}", color = Green) }
                TrigLab.Polar -> { FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) { PolarCurveType.entries.forEach { item -> TogglePill(item.name, polarType == item) { polarType = item } } }; AxisSlider("Polar parameter", polarParameter, .25f..8f) { polarParameter = it } }
                TrigLab.Applications -> { AxisSlider("Ground distance", observationDistance, 1f..100f) { observationDistance = it }; AxisSlider("Observer height", observerHeight, 0f..3f) { observerHeight = it }; Text("height = observer height + distance x tan(elevation)", color = Muted) }
                TrigLab.Harmonics -> { AxisSlider("Fundamental A1", harmonic1, 0f..2f) { harmonic1 = it }; AxisSlider("Second A2", harmonic2, 0f..2f) { harmonic2 = it }; AxisSlider("Third A3", harmonic3, 0f..2f) { harmonic3 = it } }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { TogglePill("Tangents", showTangents) { showTangents = it }; TogglePill("Projections", showProjections) { showProjections = it }; TogglePill("Wave", showWave) { showWave = it }; GlowButton("Close", onClick = vm::hidePanels) }
        }
    }
}

@Composable
private fun LegacyTrigonometryScreen(vm: ExplorerViewModel) {
    var angle by remember { mutableFloatStateOf(45f) }
    var amplitude by remember { mutableFloatStateOf(1f) }
    var period by remember { mutableFloatStateOf((2 * Math.PI).toFloat()) }
    var phase by remember { mutableFloatStateOf(0f) }
    var verticalShift by remember { mutableFloatStateOf(0f) }
    var function by remember { mutableStateOf(TrigFunction.Sine) }
    var identityIndex by remember { mutableIntStateOf(0) }
    var showTangents by remember { mutableStateOf(true) }
    var showProjections by remember { mutableStateOf(true) }
    var showWave by remember { mutableStateOf(true) }
    var trigHomeRequest by remember { mutableIntStateOf(0) }
    var trigZoom by remember { mutableFloatStateOf(1f) }
    val radians = Math.toRadians(angle.toDouble())
    val snapshot = remember(angle) { InteractiveTrigEngine.snapshot(radians) }
    val transform = TrigTransform(amplitude.toDouble(), period.toDouble(), phase.toDouble(), verticalShift.toDouble())
    val identityLab = remember { InteractiveTrigIdentityLab() }
    val identity = remember(identityIndex) { identityLab.verify(identityIndex) }
    val triangle = remember(angle) { TriangleTrigSolver.sas(4.0, 5.0, angle.toDouble().let { kotlin.math.abs(it).coerceIn(1.0, 179.0) }) }
    Box(Modifier.fillMaxSize()) {
        TrigCanvas(
            modifier = Modifier.fillMaxSize(),
            angleDegrees = angle,
            transform = transform,
            function = function,
            showTangents = showTangents,
            showProjections = showProjections,
            showWave = showWave,
            homeRequest = trigHomeRequest,
            onZoomChanged = { trigZoom = it },
            onAngleChange = { angle = snapAngle(it) },
        )
        InteractionHint(
            "One finger moves the circle point or wave cursor · two fingers pan and pinch zoom · double-tap resets",
            Modifier.align(Alignment.BottomEnd),
        )
        Row(Modifier.align(Alignment.TopStart).padding(top = 72.dp, start = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GlowButton("⌂ Home") { trigHomeRequest++ }
            Text("${trim(trigZoom.toDouble())}×", color = Muted, modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(SurfaceA).padding(9.dp))
        }
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
            Text("Drag the circle point or wave cursor.", color = Muted, fontSize = 12.sp)
            TrigFunction.entries.forEach { value -> GlowButton(if (function == value) "• ${value.name}" else value.name, onClick = { function = value; showWave = true }) }
            GlowButton("Next identity", onClick = { identityIndex = (identityIndex + 1) % identityLab.catalog.size })
        }
        if (vm.showRightPanel) GlassPanel(Modifier.align(Alignment.TopEnd).width(250.dp)) {
            PanelHeader("Angle Insights", vm::hidePanels, Violet)
            Insight("Angle", "${trim(snapshot.degrees)} deg", Cyan)
            Insight("Radians", "${radianLabel(angle.toDouble())} rad", Violet)
            Insight("Quadrant", "Q${snapshot.quadrant} · ref ${trim(snapshot.referenceAngleDegrees)}°", Cyan)
            Insight("sin θ", snapshot.exactSine ?: trim(snapshot.sine), Violet)
            Insight("cos θ", snapshot.exactCosine ?: trim(snapshot.cosine), Cyan)
            Insight("tan θ", snapshot.exactTangent ?: snapshot.tangent?.let(::trim) ?: "undefined", Green)
            Insight("Triangle", "c=${trim(triangle.c)}, area=${trim(triangle.area)}", Amber)
            Insight(identity.label, if (identity.evidence.equivalent) "Verified · residual 0" else identity.evidence.explanation, Green)
            Text("Assumptions: ${identity.evidence.leftDomain.description}", color = Muted, fontSize = 11.sp)
        }
        if (vm.showBottomPanel) GlassPanel(Modifier.align(Alignment.BottomStart).fillMaxWidth()) {
            PanelHeader("Trigonometry Controls", vm::hidePanels, Ink)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Angle ${trim(angle.toDouble())} deg", color = Ink, modifier = Modifier.width(126.dp))
                Slider(value = angle, onValueChange = { angle = snapAngle(it) }, valueRange = -180f..180f, modifier = Modifier.weight(1f))
            }
            AxisSlider("Amplitude", amplitude, .25f..3f) { amplitude = it }
            AxisSlider("Period", period, 1f..12.57f) { period = it }
            AxisSlider("Phase", phase, -3.14f..3.14f) { phase = it }
            AxisSlider("Vertical", verticalShift, -2f..2f) { verticalShift = it }
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
    selectedShapes: Set<Int>,
    snapEnabled: Boolean,
    axisConstraint: AxisConstraint,
    precisionMode: Boolean,
    lassoEnabled: Boolean,
    homeRequest: Int,
    undoViewRequest: Int,
    onPointDragStart: (Int) -> Unit,
    onPointDrag: (Int, Vec2) -> Unit,
    onShapeDragStart: (Int) -> Unit,
    onShapeDrag: (Vec2) -> Unit,
    onShapeRotate: (Double) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onCanvasTap: (Vec2, Int?) -> Unit,
    onClearSelection: () -> Unit,
    onLassoSelection: (Set<Int>) -> Unit,
    content: androidx.compose.ui.graphics.drawscope.DrawScope.(toScreen: (Vec2) -> Offset) -> Unit,
) {
    var cameraCenter by remember { mutableStateOf(Vec2(0.0, 0.0)) }
    var cameraZoom by remember { mutableFloatStateOf(1f) }
    var lastTapAt by remember { mutableStateOf(0L) }
    var canvasWidth by remember { mutableIntStateOf(0) }
    var canvasHeight by remember { mutableIntStateOf(0) }
    var gestureMode by remember { mutableStateOf(GestureMode.Idle) }
    var lassoWorld by remember { mutableStateOf<List<Vec2>>(emptyList()) }
    var snapGuides by remember { mutableStateOf<List<com.indianservers.aiexplorer.core.SnapGuide>>(emptyList()) }
    var coordinateTooltip by remember { mutableStateOf<Vec2?>(null) }
    var viewportUndo by remember { mutableStateOf<List<com.indianservers.aiexplorer.core.Viewport2D>>(emptyList()) }
    val currentPoints by rememberUpdatedState(points)
    val currentShapes by rememberUpdatedState(shapes)
    LaunchedEffect(homeRequest, canvasWidth, canvasHeight, points) {
        if (homeRequest > 0 && canvasWidth > 0 && canvasHeight > 0) {
            viewportUndo = viewportUndo + com.indianservers.aiexplorer.core.Viewport2D(cameraCenter, cameraZoom)
            val fit = InteractionGeometry.fit(points, canvasWidth.toDouble() / canvasHeight)
            cameraCenter = fit.center
            cameraZoom = fit.zoom
        }
    }
    LaunchedEffect(undoViewRequest) {
        if (undoViewRequest > 0 && viewportUndo.isNotEmpty()) {
            val previous = viewportUndo.last()
            viewportUndo = viewportUndo.dropLast(1)
            cameraCenter = previous.center
            cameraZoom = previous.zoom
        }
    }
    Canvas(
        modifier
            .onSizeChanged { canvasWidth = it.width; canvasHeight = it.height }
            .pointerInput(interactionEnabled) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val viewportFrom = com.indianservers.aiexplorer.core.Viewport2D(cameraCenter, cameraZoom)
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
                    val selectionPoints = selectedShapes.flatMap { index -> gestureShapes.getOrNull(index)?.pointIndices.orEmpty() }.distinct().mapNotNull(gesturePoints::getOrNull)
                    val selectionBounds = InteractionGeometry.bounds(selectionPoints)
                    val rotationHandle = selectionBounds?.let { bounds -> screen(Vec2(bounds.center.x, bounds.maximum.y + 1.0)) }
                    val rotating = interactionEnabled && !lassoEnabled && rotationHandle != null && (rotationHandle - down.position).getDistance() <= 34f
                    val tappedPointIndex = gesturePoints.indices
                        .minByOrNull { (screen(gesturePoints[it]) - down.position).getDistance() }
                        ?.takeIf { (screen(gesturePoints[it]) - down.position).getDistance() <= 38f }
                    var pointIndex: Int? = null
                    var shapeIndex: Int? = null
                    if (interactionEnabled && !lassoEnabled) {
                        pointIndex = tappedPointIndex
                        if (pointIndex == null && !rotating) {
                            shapeIndex = gestureShapes.indices.filter { gestureShapes[it].visible }
                                .minByOrNull { shapeScreenDistance(gestureShapes[it], gesturePoints, down.position, ::screen) }
                                ?.takeIf { shapeScreenDistance(gestureShapes[it], gesturePoints, down.position, ::screen) <= 42f }
                        }
                        pointIndex?.let(onPointDragStart)
                        shapeIndex?.let(onShapeDragStart)
                        if (rotating) selectedShapes.lastOrNull()?.let(onShapeDragStart)
                    }
                    val gestureTarget = when {
                        pointIndex != null -> GeometryGestureTarget.JunctionHandle
                        shapeIndex != null || rotating -> GeometryGestureTarget.ShapeBody
                        else -> GeometryGestureTarget.EmptyCanvas
                    }
                    if (lassoEnabled) { lassoWorld = listOf(startWorld); gestureMode = GestureMode.Lasso }
                    else gestureMode = when { rotating -> GestureMode.Rotating; pointIndex != null -> GestureMode.Resizing; shapeIndex != null -> GestureMode.Moving; else -> GestureMode.Selecting }

                    var moved = false
                    var transformed = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.size >= 2) {
                            if (GeometryGesturePolicy.allowsViewportTransform(gestureTarget)) {
                                val pan = event.calculatePan()
                                val centroid = event.calculateCentroid()
                                val oldScale = scale()
                                cameraCenter = Vec2(cameraCenter.x - pan.x / oldScale, cameraCenter.y + pan.y / oldScale)
                                val beforeZoom = world(centroid)
                                cameraZoom = GraphViewport.zoom(cameraZoom, event.calculateZoom())
                                val afterZoom = world(centroid)
                                cameraCenter += beforeZoom - afterZoom
                                transformed = true
                                gestureMode = if (abs(event.calculateZoom() - 1f) > .002f) GestureMode.Zooming else GestureMode.Panning
                            }
                            event.changes.forEach { it.consume() }
                        } else {
                            val change = event.changes.firstOrNull()
                            val delta = change?.positionChange() ?: Offset.Zero
                            if (delta.getDistance() > 0f) {
                                moved = moved || (change!!.position - down.position).getDistance() > 8f
                                val currentWorld = world(change!!.position)
                                when {
                                    lassoEnabled -> lassoWorld = lassoWorld + currentWorld
                                    rotating && selectionBounds != null -> onShapeRotate(InteractionGeometry.rotationDegrees(selectionBounds.center, startWorld, currentWorld))
                                    pointIndex != null -> {
                                        val original = gesturePoints[pointIndex]
                                        val handleHeld = change.uptimeMillis - down.uptimeMillis >= 450L
                                        val constrained = PrecisionInteraction.apply(SmartSnapEngine.constrain(currentWorld - original, axisConstraint), precisionMode || handleHeld)
                                        val proposed = original + constrained
                                        val centers = gestureShapes.filter { it.visible }.mapNotNull { shape -> shape.pointIndices.mapNotNull(gesturePoints::getOrNull).takeIf { it.isNotEmpty() }?.let { values -> InteractionGeometry.bounds(values)?.center } }
                                        val segments = gestureShapes.filter { it.visible }.flatMap { shape -> shape.pointIndices.mapNotNull(gesturePoints::getOrNull).zipWithNext() }
                                        val intersections = InteractionGeometry.segmentIntersections(segments)
                                        val remainingPoints = gesturePoints.filterIndexed { index, _ -> index != pointIndex }
                                        val tangentOrigin = gestureShapes.firstNotNullOfOrNull { shape ->
                                            if (pointIndex !in shape.pointIndices || shape.pointIndices.size < 2) null
                                            else shape.pointIndices.firstOrNull { it != pointIndex }?.let(gesturePoints::getOrNull)
                                        }
                                        val tangents = tangentOrigin?.let { origin ->
                                            gestureShapes.filter { it.visible && it.type in setOf(Shape2DType.Circle, Shape2DType.CircleThreePoints) }.flatMap { circle ->
                                                val circlePoints = circle.pointIndices.mapNotNull(gesturePoints::getOrNull)
                                                val center = circlePoints.firstOrNull() ?: return@flatMap emptyList()
                                                val radius = circlePoints.getOrNull(1)?.let { it.distanceTo(center) } ?: return@flatMap emptyList()
                                                InteractionGeometry.tangentPoints(origin, center, radius)
                                            }
                                        }.orEmpty()
                                        val snapped = if (snapEnabled) SmartSnapEngine.snap(
                                            proposed,
                                            remainingPoints,
                                            centers = centers,
                                            intersections = intersections,
                                            tangents = tangents,
                                            equalSpacing = InteractionGeometry.equalSpacingCandidates(remainingPoints.take(16)),
                                        ) else com.indianservers.aiexplorer.core.SnapResult(proposed, emptyList())
                                        snapGuides = snapped.guides
                                        coordinateTooltip = snapped.point
                                        onPointDrag(pointIndex, snapped.point)
                                    }
                                    shapeIndex != null -> {
                                        val constrained = PrecisionInteraction.apply(SmartSnapEngine.constrain(currentWorld - startWorld, axisConstraint), precisionMode)
                                        snapGuides = emptyList()
                                        coordinateTooltip = selectionBounds?.center?.plus(constrained)
                                        onShapeDrag(constrained)
                                    }
                                    interactionEnabled -> {
                                        cameraCenter = Vec2(cameraCenter.x - delta.x / scale(), cameraCenter.y + delta.y / scale())
                                        gestureMode = GestureMode.Panning
                                    }
                                }
                                change!!.consume()
                            }
                        }
                        if (event.changes.none { it.pressed }) break
                    }

                    when {
                        lassoEnabled -> {
                            val selected = gestureShapes.indices.filterTo(linkedSetOf()) { index ->
                                val shapePoints = gestureShapes[index].pointIndices.mapNotNull(gesturePoints::getOrNull)
                                shapePoints.isNotEmpty() && InteractionGeometry.pointInPolygon(InteractionGeometry.bounds(shapePoints)?.center ?: shapePoints.first(), lassoWorld)
                            }
                            onLassoSelection(selected)
                        }
                        pointIndex != null || shapeIndex != null || rotating -> onDragEnd()
                        !moved && !transformed -> {
                            val now = System.currentTimeMillis()
                            if (interactionEnabled && now - lastTapAt < 320L) {
                                val fit = InteractionGeometry.fit(gesturePoints, size.width.toDouble() / size.height)
                                cameraCenter = fit.center
                                cameraZoom = fit.zoom
                                lastTapAt = 0L
                            } else {
                                if (interactionEnabled && tappedPointIndex == null && shapeIndex == null) onClearSelection()
                                else onCanvasTap(startWorld, tappedPointIndex)
                                lastTapAt = if (interactionEnabled) now else 0L
                            }
                        }
                    }
                    gestureMode = GestureMode.Idle
                    val viewportTo = com.indianservers.aiexplorer.core.Viewport2D(cameraCenter, cameraZoom)
                    if (viewportTo != viewportFrom && (transformed || (moved && gestureTarget == GeometryGestureTarget.EmptyCanvas))) viewportUndo = viewportUndo + viewportFrom
                    snapGuides = emptyList()
                    coordinateTooltip = null
                    lassoWorld = emptyList()
                }
            },
    ) {
        val scale = size.width / 14f * cameraZoom
        val origin = Offset(size.width / 2f - cameraCenter.x.toFloat() * scale, size.height / 2f + cameraCenter.y.toFloat() * scale)
        val tx: (Vec2) -> Offset = { Offset(origin.x + it.x.toFloat() * scale, origin.y - it.y.toFloat() * scale) }
        drawGrid(origin, scale)
        content(tx)
        snapGuides.forEach { guide ->
            if (guide.axis == AxisConstraint.X) {
                val x = tx(Vec2(guide.value, 0.0)).x
                drawLine(Green.copy(.8f), Offset(x, 0f), Offset(x, size.height), 2f)
                drawTrigText(guide.label, x + 7f, 118f, Green)
            } else if (guide.axis == AxisConstraint.Y) {
                val y = tx(Vec2(0.0, guide.value)).y
                drawLine(Green.copy(.8f), Offset(0f, y), Offset(size.width, y), 2f)
                drawTrigText(guide.label, 8f, y - 7f, Green)
            }
        }
        if (lassoWorld.size >= 2) {
            val path = Path().apply {
                val first = tx(lassoWorld.first()); moveTo(first.x, first.y)
                lassoWorld.drop(1).forEach { point -> val p = tx(point); lineTo(p.x, p.y) }
            }
            drawPath(path, Cyan, style = Stroke(3f, cap = StrokeCap.Round))
        }
        val selectedWorld = selectedShapes.flatMap { index -> shapes.getOrNull(index)?.pointIndices.orEmpty() }.distinct().mapNotNull(points::getOrNull)
        InteractionGeometry.bounds(selectedWorld)?.let { bounds ->
            val topLeft = tx(Vec2(bounds.minimum.x, bounds.maximum.y))
            val bottomRight = tx(Vec2(bounds.maximum.x, bounds.minimum.y))
            val boxTopLeft = Offset(min(topLeft.x, bottomRight.x), min(topLeft.y, bottomRight.y))
            val boxSize = Size(abs(bottomRight.x - topLeft.x).coerceAtLeast(2f), abs(bottomRight.y - topLeft.y).coerceAtLeast(2f))
            drawRect(Amber.copy(.9f), boxTopLeft, boxSize, style = Stroke(2.5f))
            val center = tx(bounds.center)
            val rotateHandle = tx(Vec2(bounds.center.x, bounds.maximum.y + 1.0))
            drawLine(Amber, Offset(center.x, boxTopLeft.y), rotateHandle, 2f)
            drawCircle(Amber.copy(.22f), 20f, rotateHandle)
            drawCircle(Amber, 7f, rotateHandle)
            drawCircle(Green.copy(.25f), 18f, center)
            drawLine(Green, center - Offset(10f, 0f), center + Offset(10f, 0f), 2f)
            drawLine(Green, center - Offset(0f, 10f), center + Offset(0f, 10f), 2f)
            val measure = if (selectedWorld.size >= 2) {
                val segment = Geometry2D.segment(selectedWorld.first(), selectedWorld.last())
                "Δx ${trim(bounds.width)} · Δy ${trim(bounds.height)} · length ${trim(segment.distance)} · slope ${segment.slope?.let(::trim) ?: "∞"}"
            } else "(${trim(bounds.center.x)}, ${trim(bounds.center.y)})"
            drawGraphLabel(measure, boxTopLeft + Offset(8f, -48f), Amber)
        }
        coordinateTooltip?.let { point -> drawGraphLabel("(${trim(point.x)}, ${trim(point.y)})", tx(point) + Offset(18f, -54f), Green) }
        if (gestureMode != GestureMode.Idle) drawGraphLabel(gestureMode.label, Offset(size.width / 2f - 95f, 92f), Cyan)
        drawGraphLabel("${trim(cameraZoom.toDouble())}× · ${trim((size.width / scale).toDouble())} units wide", Offset(size.width - 205f, 92f), Muted)
    }
}

private fun shapeScreenDistance(shape: Shape2D, points: List<Vec2>, target: Offset, screen: (Vec2) -> Offset): Float {
    val worldPoints = shape.pointIndices.mapNotNull(points::getOrNull)
    val vertices = worldPoints.map(screen)
    if (vertices.isEmpty()) return Float.MAX_VALUE
    if (shape.type in setOf(Shape2DType.Circle, Shape2DType.Arc) && vertices.size >= 2) {
        val radius = (vertices[1] - vertices[0]).getDistance()
        val centerDistance = (target - vertices[0]).getDistance()
        return if (shape.type == Shape2DType.Circle && centerDistance <= radius) 0f else abs(centerDistance - radius)
    }
    if (shape.type == Shape2DType.CircleThreePoints && worldPoints.size >= 3) {
        val centerWorld = Geometry2D.circumcenter(worldPoints[0], worldPoints[1], worldPoints[2]) ?: return Float.MAX_VALUE
        val center = screen(centerWorld)
        val radius = (vertices[0] - center).getDistance()
        val distance = (target - center).getDistance()
        return if (distance <= radius) 0f else distance - radius
    }
    if (shape.type == Shape2DType.Ellipse && vertices.size >= 3) {
        val center = vertices[0]
        val rx = (vertices[1] - center).getDistance().coerceAtLeast(1f)
        val ry = (vertices[2] - center).getDistance().coerceAtLeast(1f)
        val normalized = kotlin.math.sqrt(((target.x - center.x) / rx).let { it * it } + ((target.y - center.y) / ry).let { it * it })
        return if (normalized <= 1f) 0f else (normalized - 1f) * min(rx, ry)
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
    val displayVertices = when {
        shape.type in setOf(Shape2DType.Rectangle, Shape2DType.Square) && vertices.size >= 2 -> {
            val a = vertices[0]; val b = vertices[1]
            val height = if (shape.type == Shape2DType.Square) b.x - a.x else b.y - a.y
            listOf(a, Offset(b.x, a.y), Offset(b.x, a.y + height), Offset(a.x, a.y + height))
        }
        shape.type == Shape2DType.RegularPolygon && worldPoints.size >= 2 -> {
            val center = worldPoints[0]
            val radiusVector = worldPoints[1] - center
            val startAngle = kotlin.math.atan2(radiusVector.y, radiusVector.x)
            val radius = radiusVector.distanceTo(Vec2(0.0, 0.0))
            (0 until 5).map { i -> screen(Vec2(center.x + cos(startAngle + i * 2.0 * PI / 5.0) * radius, center.y + sin(startAngle + i * 2.0 * PI / 5.0) * radius)) }
        }
        else -> vertices
    }
    val closed = shape.type in setOf(Shape2DType.Triangle, Shape2DType.Polygon, Shape2DType.Rectangle, Shape2DType.Square, Shape2DType.RegularPolygon)
    if (closed && displayVertices.size >= 3 && pointInsidePolygon(target, displayVertices)) return 0f
    val edges = when {
        displayVertices.size == 1 -> emptyList()
        closed -> displayVertices.indices.map { displayVertices[it] to displayVertices[(it + 1) % displayVertices.size] }
        else -> displayVertices.zipWithNext()
    }
    return edges.minOfOrNull { (a, b) -> pointSegmentDistance(target, a, b) }
        ?: (displayVertices.first() - target).getDistance()
}

private fun pointInsidePolygon(point: Offset, vertices: List<Offset>): Boolean {
    var inside = false
    var previous = vertices.lastIndex
    vertices.indices.forEach { current ->
        val a = vertices[current]
        val b = vertices[previous]
        if ((a.y > point.y) != (b.y > point.y)) {
            val crossingX = (b.x - a.x) * (point.y - a.y) / (b.y - a.y) + a.x
            if (point.x < crossingX) inside = !inside
        }
        previous = current
    }
    return inside
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
    homeRequest: Int,
    backRequest: Int,
    forwardRequest: Int,
    axisSettings: GraphAxisSettings,
    domains: Map<String, GraphDomainSelection>,
    styles: Map<String, GraphLineStyle>,
    labelOffsets: Map<String, Offset>,
    comparisonMode: Boolean,
    showMiniMap: Boolean,
    parameterA: Float,
    parameterHandleEnabled: Boolean,
    previewExpression: String?,
    snapshotExpressions: List<String>,
    selectedFunctionId: String?,
    onSelectFunction: (String) -> Unit,
    onClearSelection: () -> Unit,
    onTraceChange: (Double) -> Unit,
    onParameterAChange: (Double) -> Unit,
    onDomainChange: (String, GraphDomainSelection) -> Unit,
    onLabelMove: (String, Offset) -> Unit,
    onViewportChange: (GraphViewState) -> Unit,
    onContextMenu: (String?, Vec2) -> Unit,
) {
    val graph = remember { GraphAnalysis() }
    val advancedGraphEngine = remember { AdvancedGraphEngine() }
    val engine = remember { ExpressionEngine() }
    var cameraCenter by remember { mutableStateOf(Vec2(0.0, 0.0)) }
    var cameraZoom by remember { mutableFloatStateOf(1f) }
    var lastTapAt by remember { mutableStateOf(0L) }
    var gestureMode by remember { mutableStateOf(GestureMode.Idle) }
    val viewHistory = remember { com.indianservers.aiexplorer.core.GraphViewHistory() }
    val currentFunctions by rememberUpdatedState(functions)
    LaunchedEffect(homeRequest) {
        if (homeRequest > 0) {
            val fitPoints = functions.filter { it.visible }.flatMap { function ->
                runCatching { graph.sampleDefinition(function.expression, -10.0, 10.0, 240).points.filter { it.x.isFinite() && it.y.isFinite() } }.getOrDefault(emptyList())
            } + dataPoints
            val fit = InteractionGeometry.fit(fitPoints)
            cameraCenter = fit.center
            cameraZoom = fit.zoom
            viewHistory.commit(GraphViewState(cameraCenter, cameraZoom))
        }
    }
    LaunchedEffect(backRequest) { if (backRequest > 0) viewHistory.back().let { cameraCenter = it.center; cameraZoom = it.zoom } }
    LaunchedEffect(forwardRequest) { if (forwardRequest > 0) viewHistory.forward().let { cameraCenter = it.center; cameraZoom = it.zoom } }
    LaunchedEffect(cameraCenter, cameraZoom) { onViewportChange(GraphViewState(cameraCenter, cameraZoom)) }
    Canvas(
        modifier
            .pointerInput(graphTool, selectedFunctionId, domains, labelOffsets, parameterA, parameterHandleEnabled) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val gestureStartedAt = System.currentTimeMillis()
                    val baseScale = size.width / 14f
                    fun scale() = baseScale * cameraZoom
                    fun origin() = Offset(
                        size.width / 2f - cameraCenter.x.toFloat() * scale(),
                        size.height / 2f + cameraCenter.y.toFloat() * scale(),
                    )
                    fun world(screen: Offset): Vec2 {
                        val o = origin()
                        val displayX = ((screen.x - o.x) / scale()).toDouble(); val displayY = ((o.y - screen.y) / scale()).toDouble()
                        return Vec2(if (axisSettings.xLogarithmic) 10.0.pow(displayX) else displayX, if (axisSettings.yLogarithmic) 10.0.pow(displayY) else displayY)
                    }
                    fun screen(point: Vec2): Offset {
                        val o = origin()
                        val displayX = if (axisSettings.xLogarithmic && point.x > 0) log10(point.x) else point.x
                        val displayY = if (axisSettings.yLogarithmic && point.y > 0) log10(point.y) else point.y
                        return Offset(o.x + displayX.toFloat() * scale(), o.y - displayY.toFloat() * scale())
                    }
                    fun distanceToSegment(p: Offset, a: Offset, b: Offset): Float {
                        val ab = b - a
                        val ap = p - a
                        val length2 = ab.x * ab.x + ab.y * ab.y
                        if (length2 <= 1e-6f) return (p - a).getDistance()
                        val t = ((ap.x * ab.x + ap.y * ab.y) / length2).coerceIn(0f, 1f)
                        val projection = a + ab * t
                        return (p - projection).getDistance()
                    }
                    fun nearestFunction(screenTap: Offset): String? {
                        var bestId: String? = null
                        var bestDistance = 34f
                        currentFunctions.filter { it.visible }.forEach { fn ->
                            if (advancedGraphEngine.classify(fn.expression) == AdvancedGraphKind.Inequality) {
                                val tap = world(screenTap)
                                val inside = runCatching { engine.compile(fn.expression).eval(mapOf("x" to tap.x, "y" to tap.y)) != 0.0 }.getOrDefault(false)
                                if (inside) { bestDistance = 0f; bestId = fn.id }
                                return@forEach
                            }
                            val kind = graph.definitionKind(fn.expression)
                            if (kind == GraphDefinitionKind.Implicit) {
                                val halfWidth = size.width / (2f * scale())
                                val halfHeight = size.height / (2f * scale())
                                val center = world(Offset(size.width / 2f, size.height / 2f))
                                val segments = runCatching {
                                    graph.implicitSegments(fn.expression, center.x - halfWidth, center.x + halfWidth, center.y - halfHeight, center.y + halfHeight)
                                }.getOrDefault(emptyList())
                                segments.forEach { segment ->
                                    val d = distanceToSegment(screenTap, screen(segment.start), screen(segment.end))
                                    if (d < bestDistance) {
                                        bestDistance = d
                                        bestId = fn.id
                                    }
                                }
                            } else {
                                val tapWorld = world(screenTap)
                                val sample = runCatching { graph.sampleDefinition(fn.expression, tapWorld.x - 1.4, tapWorld.x + 1.4, steps = 160) }.getOrNull()
                                sample?.points?.zipWithNext()?.forEachIndexed { i, pair ->
                                    if (!sample.breaks.contains(i)) {
                                        val d = distanceToSegment(screenTap, screen(pair.first), screen(pair.second))
                                        if (d < bestDistance) {
                                            bestDistance = d
                                            bestId = fn.id
                                        }
                                    }
                                }
                            }
                        }
                        return bestId
                    }

                    val traceFunction = currentFunctions.firstOrNull { it.id == selectedFunctionId && it.visible && graph.definitionKind(it.expression) == GraphDefinitionKind.Explicit }
                        ?: currentFunctions.firstOrNull { it.visible && graph.definitionKind(it.expression) == GraphDefinitionKind.Explicit }
                    val traceScreen = traceFunction?.let { fn ->
                        runCatching {
                            val y = engine.compile(stripEquation(fn.expression)).eval(mapOf("x" to traceX))
                            val o = origin()
                            Offset(o.x + traceX.toFloat() * scale(), o.y - y.toFloat() * scale())
                        }.getOrNull()
                    }
                    val selectedFunction = currentFunctions.firstOrNull { it.id == selectedFunctionId }
                    val selectedDomain = selectedFunctionId?.let(domains::get)
                    val labelPosition = traceScreen?.plus(labelOffsets[selectedFunctionId] ?: Offset(18f, 28f))
                    val labelDrag = selectedFunctionId != null && labelPosition != null && (labelPosition - down.position).getDistance() < 70f
                    val parameterHandleWorld = selectedFunction?.takeIf { parameterHandleEnabled && Regex("\\ba\\b").containsMatchIn(it.expression) }?.let { fn ->
                        runCatching { Vec2(1.0, engine.compile(stripEquation(fn.expression)).eval(mapOf("x" to 1.0))) }.getOrNull()
                    }
                    val parameterDrag = parameterHandleWorld?.let { (screen(it) - down.position).getDistance() < 48f } == true
                    val domainSide = selectedDomain?.let { domain ->
                        val left = screen(Vec2(domain.minimum, 0.0)); val right = screen(Vec2(domain.maximum, 0.0))
                        when { (left - down.position).getDistance() < 42f -> -1; (right - down.position).getDistance() < 42f -> 1; else -> 0 }
                    } ?: 0
                    val traceDrag = !labelDrag && !parameterDrag && domainSide == 0 && (graphTool == GraphTool.Trace || (traceScreen != null && (traceScreen - down.position).getDistance() < 44f))
                    val gestureStartView = GraphViewState(cameraCenter, cameraZoom)
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
                            cameraZoom = GraphViewport.zoom(cameraZoom, event.calculateZoom())
                            val afterZoom = world(centroid)
                            cameraCenter += beforeZoom - afterZoom
                            transformed = true
                            gestureMode = if (abs(event.calculateZoom() - 1f) > .002f) GestureMode.Zooming else GestureMode.Panning
                            event.changes.forEach { it.consume() }
                        } else {
                            val change = event.changes.firstOrNull()
                            val delta = change?.positionChange() ?: Offset.Zero
                            if (delta.getDistance() > 0f) {
                                moved = moved || (change!!.position - down.position).getDistance() > 8f
                                if (labelDrag && selectedFunctionId != null) {
                                    onLabelMove(selectedFunctionId, delta)
                                    gestureMode = GestureMode.Moving
                                } else if (parameterDrag) {
                                    val point = world(change!!.position)
                                    val base = selectedFunction?.expression?.replace(Regex("\\ba\\b"), "1")
                                    val unitY = base?.let { runCatching { engine.compile(stripEquation(it)).eval(mapOf("x" to point.x)) }.getOrNull() }
                                    if (unitY != null && abs(unitY) > 1e-8) onParameterAChange(point.y / unitY)
                                    gestureMode = GestureMode.Resizing
                                } else if (domainSide != 0 && selectedFunctionId != null && selectedDomain != null) {
                                    val x = world(change!!.position).x
                                    val next = if (domainSide < 0) selectedDomain.copy(minimum = x.coerceAtMost(selectedDomain.maximum - .01)) else selectedDomain.copy(maximum = x.coerceAtLeast(selectedDomain.minimum + .01))
                                    onDomainChange(selectedFunctionId, next)
                                    gestureMode = GestureMode.Resizing
                                } else if (traceDrag) {
                                    onTraceChange(world(change!!.position).x)
                                    gestureMode = GestureMode.Moving
                                } else {
                                    cameraCenter = Vec2(cameraCenter.x - delta.x / scale(), cameraCenter.y + delta.y / scale())
                                    gestureMode = GestureMode.Panning
                                }
                                change!!.consume()
                            }
                        }
                        if (event.changes.none { it.pressed }) break
                    }
                    if (!moved && !transformed) {
                        val now = System.currentTimeMillis()
                        if (now - lastTapAt < 320L) {
                            val doubleTapHit = nearestFunction(down.position)
                            if (doubleTapHit != null) onSelectFunction(doubleTapHit) else {
                                val fitPoints = currentFunctions.filter { it.visible }.flatMap { function -> runCatching { graph.sampleDefinition(function.expression, -10.0, 10.0, 180).points }.getOrDefault(emptyList()) }
                                val fit = InteractionGeometry.fit(fitPoints); cameraCenter = fit.center; cameraZoom = fit.zoom
                            }
                            lastTapAt = 0L
                        } else {
                            val hit = nearestFunction(down.position)
                            if (now - gestureStartedAt > 520L) onContextMenu(hit, world(down.position))
                            else if (hit != null) onSelectFunction(hit)
                            else if (graphTool == GraphTool.Trace) onTraceChange(world(down.position).x)
                            else onClearSelection()
                            lastTapAt = now
                        }
                    }
                    val finishedView = GraphViewState(cameraCenter, cameraZoom)
                    if (finishedView != gestureStartView && (moved || transformed)) viewHistory.commit(finishedView)
                    gestureMode = GestureMode.Idle
                }
            }
            .semantics { contentDescription = "Interactive graphing canvas with axes, curves, trace point, and annotations" },
    ) {
        val scale = size.width / 14f * cameraZoom
        val origin = Offset(size.width / 2f - cameraCenter.x.toFloat() * scale, size.height / 2f + cameraCenter.y.toFloat() * scale)
        val tx: (Vec2) -> Offset = {
            val displayX = if (axisSettings.xLogarithmic && it.x > 0) log10(it.x) else if (axisSettings.xLogarithmic) Double.NaN else it.x
            val displayY = if (axisSettings.yLogarithmic && it.y > 0) log10(it.y) else if (axisSettings.yLogarithmic) Double.NaN else it.y
            Offset(origin.x + displayX.toFloat() * scale, origin.y - displayY.toFloat() * scale)
        }
        drawGrid(origin, scale, axisSettings)
        val halfWidth = size.width / (2f * scale)
        val halfHeight = size.height / (2f * scale)
        val minX = if (axisSettings.xLogarithmic) 10.0.pow(cameraCenter.x - halfWidth) else cameraCenter.x - halfWidth
        val maxX = if (axisSettings.xLogarithmic) 10.0.pow(cameraCenter.x + halfWidth) else cameraCenter.x + halfWidth
        val minY = if (axisSettings.yLogarithmic) 10.0.pow(cameraCenter.y - halfHeight) else cameraCenter.y - halfHeight
        val maxY = if (axisSettings.yLogarithmic) 10.0.pow(cameraCenter.y + halfHeight) else cameraCenter.y + halfHeight
        val traceAnchorRows = functions.filter { it.visible && graph.definitionKind(it.expression) == GraphDefinitionKind.Explicit }.mapNotNull { fn ->
            runCatching { Vec2(traceX, engine.compile(stripEquation(fn.expression)).eval(mapOf("x" to traceX))) }.getOrNull()?.takeIf { it.y.isFinite() }?.let { fn.id to it }
        }
        val arrangedTraceLabels = GraphUxEngine.avoidLabelCollisions(traceAnchorRows.map { it.second + Vec2(.25, .35) })
        val traceLabelsById = traceAnchorRows.map { it.first }.zip(arrangedTraceLabels).toMap()
        functions.forEachIndexed { index, fn ->
            if (!fn.visible) return@forEachIndexed
            val color = when (fn.colorKey) { "cyan" -> Cyan; "green" -> Green; "amber" -> Amber; else -> Violet }
            val selected = selectedFunctionId == fn.id
            val strokeWidth = if (selected) 6.0f else 4.2f
            val styleEffect = when (styles[fn.id] ?: GraphLineStyle.Solid) { GraphLineStyle.Solid -> null; GraphLineStyle.Dashed -> androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(16f, 9f)); GraphLineStyle.Dotted -> androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(3f, 8f)) }
            if (advancedGraphEngine.classify(fn.expression) == AdvancedGraphKind.Inequality) {
                val columns = 42; val rows = 42
                val cells = runCatching { advancedGraphEngine.inequality(fn.expression, GraphDomain(minX, maxX), GraphDomain(minY, maxY), columns, rows) }.getOrDefault(emptyList())
                val cellSize = Size(size.width / columns, size.height / rows)
                cells.filter { it.satisfied }.forEach { cell -> drawRect(color.copy(if (selected) .25f else .14f), topLeft = tx(cell.center) - Offset(cellSize.width / 2, cellSize.height / 2), size = cellSize) }
                val boundary = fn.expression.replace("<=", "=").replace(">=", "=").replace("<", "=").replace(">", "=")
                runCatching { graph.implicitSegments(boundary, minX, maxX, minY, maxY) }.getOrDefault(emptyList()).forEach { drawLine(color, tx(it.start), tx(it.end), if (selected) 5f else 3f, pathEffect = styleEffect) }
                return@forEachIndexed
            }
            val kind = graph.definitionKind(fn.expression)
            if (kind == GraphDefinitionKind.Implicit) {
                val segments = runCatching { graph.implicitSegments(fn.expression, minX, maxX, minY, maxY) }.getOrDefault(emptyList())
                segments.forEach { drawLine(color, tx(it.start), tx(it.end), if (selected) 5.2f else 3.2f, cap = StrokeCap.Round, pathEffect = styleEffect) }
            } else {
                val domain = domains[fn.id]
                val sampleMinimum = max(minX, domain?.minimum ?: minX)
                val sampleMaximum = min(maxX, domain?.maximum ?: maxX)
                val sample = if (sampleMinimum < sampleMaximum) runCatching { graph.sampleDefinition(fn.expression, sampleMinimum, sampleMaximum, steps = 520) }.getOrNull() else null
                sample?.points?.zipWithNext()?.forEachIndexed { i, pair ->
                    val logValid = (!axisSettings.xLogarithmic || pair.first.x > 0 && pair.second.x > 0) && (!axisSettings.yLogarithmic || pair.first.y > 0 && pair.second.y > 0)
                    if (!sample.breaks.contains(i) && logValid) drawLine(color.copy(alpha = if (selected) 1f else .55f), tx(pair.first), tx(pair.second), strokeWidth, cap = StrokeCap.Round, pathEffect = styleEffect)
                }
                if (domain != null && selected) {
                    listOf(domain.minimum to domain.leftClosed, domain.maximum to domain.rightClosed).forEach { (x, closed) ->
                        val y = runCatching { engine.compile(stripEquation(fn.expression)).eval(mapOf("x" to x)) }.getOrNull()
                        if (y?.isFinite() == true) { if (closed) drawCircle(color, 10f, tx(Vec2(x, y))) else drawCircle(color, 10f, tx(Vec2(x, y)), style = Stroke(4f)); drawLine(color.copy(.7f), tx(Vec2(x, 0.0)) + Offset(0f, -18f), tx(Vec2(x, 0.0)) + Offset(0f, 18f), 4f) }
                    }
                }
            }
            val trace = if (kind == GraphDefinitionKind.Explicit) runCatching {
                val y = engine.compile(stripEquation(fn.expression)).eval(mapOf("x" to traceX))
                Vec2(traceX, y)
            }.getOrNull() else null
            trace?.takeIf { abs(tx(it).y - size.height / 2f) <= size.height }?.let {
                if (graphTool in setOf(GraphTool.Trace, GraphTool.Tangent, GraphTool.Normal, GraphTool.Derivative, GraphTool.Integral, GraphTool.AreaBetween) || index == 0) {
                    drawRadiantPoint(tx(it), color, "")
                    val automatic = traceLabelsById[fn.id]?.let(tx) ?: tx(it) + Offset(18f, -18f)
                    val position = if (selected) tx(it) + (labelOffsets[fn.id] ?: Offset(18f, 28f)) else automatic
                    drawGraphLabel("${fn.name}: (${GraphUxEngine.format(it.x, axisSettings.format)}, ${GraphUxEngine.format(it.y, axisSettings.format)})", position, color)
                }
            }
            if (selected && parameterHandleEnabled && Regex("\\ba\\b").containsMatchIn(fn.expression)) {
                runCatching { Vec2(1.0, engine.compile(stripEquation(fn.expression)).eval(mapOf("x" to 1.0))) }.getOrNull()?.let { point -> drawRadiantPoint(tx(point), Amber, "drag a=${trim(parameterA.toDouble())}") }
            }
        }
        previewExpression?.let { expression ->
            val sample = runCatching { graph.sampleDefinition(expression, minX, maxX, 420) }.getOrNull()
            sample?.points?.zipWithNext()?.forEachIndexed { index, pair -> if (!sample.breaks.contains(index)) drawLine(Amber.copy(.85f), tx(pair.first), tx(pair.second), 3.2f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(14f, 8f))) }
            drawGraphLabel("Transformation preview", Offset(18f, 212f), Amber)
        }
        snapshotExpressions.forEach { expression ->
            val sample = runCatching { graph.sampleDefinition(expression, minX, maxX, 260) }.getOrNull()
            sample?.points?.zipWithNext()?.forEachIndexed { index, pair -> if (!sample.breaks.contains(index)) drawLine(Color.White.copy(.32f), tx(pair.first), tx(pair.second), 2f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(7f, 7f))) }
        }
        if (snapshotExpressions.isNotEmpty()) drawGraphLabel("Saved-state overlay", Offset(18f, 258f), Color.White)
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
        if (graphTool == GraphTool.Trace) {
            val traceScreenX = tx(Vec2(traceX, 0.0)).x
            drawLine(Cyan.copy(.7f), Offset(traceScreenX, 0f), Offset(traceScreenX, size.height), 2f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(9f, 7f)))
            drawGraphLabel("x=${GraphUxEngine.format(traceX, axisSettings.format)}", Offset(traceScreenX + 10f, 165f), Cyan)
        }
        if (comparisonMode && explicit.size >= 2) {
            val first = runCatching { engine.compile(stripEquation(explicit[0].expression)) }.getOrNull()
            val second = runCatching { engine.compile(stripEquation(explicit[1].expression)) }.getOrNull()
            if (first != null && second != null) {
                val comparison = GraphUxEngine.compare(first, second, minX, maxX, 180)
                if (comparison.size > 2) {
                    val path = Path(); val firstPoint = tx(Vec2(comparison.first().x, comparison.first().first)); path.moveTo(firstPoint.x, firstPoint.y)
                    comparison.drop(1).forEach { val point = tx(Vec2(it.x, it.first)); path.lineTo(point.x, point.y) }
                    comparison.asReversed().forEach { val point = tx(Vec2(it.x, it.second)); path.lineTo(point.x, point.y) }; path.close()
                    drawPath(path, Brush.verticalGradient(listOf(Cyan.copy(.20f), Violet.copy(.20f))))
                    comparison.minByOrNull { abs(it.x - traceX) }?.let { drawGraphLabel("f-g=${trim(it.difference)}", Offset(18f, 165f), if (it.difference >= 0) Green else Amber) }
                }
            }
        }
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
        if (gestureMode != GestureMode.Idle) drawGraphLabel(gestureMode.label, Offset(size.width / 2f - 95f, 118f), Cyan)
        if (showMiniMap) {
            val mapTop = Offset(size.width - 190f, size.height - 205f); val mapSize = Size(170f, 130f)
            drawRoundRect(SurfaceA.copy(.92f), mapTop, mapSize, androidx.compose.ui.geometry.CornerRadius(16f, 16f)); drawRoundRect(Cyan.copy(.55f), mapTop, mapSize, androidx.compose.ui.geometry.CornerRadius(16f, 16f), style = Stroke(1.5f))
            val content = functions.filter { it.visible }.flatMap { runCatching { graph.sampleDefinition(it.expression, -20.0, 20.0, 100).points }.getOrDefault(emptyList()) }
            val mini = GraphUxEngine.minimap(content, GraphViewState(cameraCenter, cameraZoom), (mapSize.width / mapSize.height).toDouble())
            val worldWidth = mini.world.width.coerceAtLeast(1e-6); val worldHeight = mini.world.height.coerceAtLeast(1e-6)
            fun miniPoint(point: Vec2) = Offset(mapTop.x + ((point.x - mini.world.minimum.x) / worldWidth).toFloat() * mapSize.width, mapTop.y + (1 - (point.y - mini.world.minimum.y) / worldHeight).toFloat() * mapSize.height)
            content.zipWithNext().forEach { (a, b) -> drawLine(Violet.copy(.5f), miniPoint(a), miniPoint(b), 1f) }
            val a = miniPoint(mini.viewport.minimum); val b = miniPoint(mini.viewport.maximum)
            drawRect(Amber.copy(.85f), topLeft = Offset(min(a.x, b.x), min(a.y, b.y)), size = Size(abs(b.x - a.x), abs(b.y - a.y)), style = Stroke(2f))
        }
        drawGraphLabel("${trim(cameraZoom.toDouble())}× · ${trim((size.width / scale).toDouble())} units", Offset(size.width - 205f, 118f), Muted)
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
    transform: TrigTransform,
    function: TrigFunction,
    showTangents: Boolean,
    showProjections: Boolean,
    showWave: Boolean,
    homeRequest: Int,
    onZoomChanged: (Float) -> Unit,
    onAngleChange: (Float) -> Unit,
    visibleFunctions: Set<TrigFunction> = setOf(function),
    showAsymptotes: Boolean = false,
    polarSamples: List<Vec2> = emptyList(),
    harmonics: List<HarmonicComponent> = emptyList(),
    lineStyle: TrigLineStyle = TrigLineStyle.Solid,
    paletteShift: Int = 0,
    equationTarget: Float? = null,
    equationRoots: List<Double> = emptyList(),
    onTransformChange: (Float, Float, Float, Float) -> Unit = { _, _, _, _ -> },
) {
    var viewport by remember { mutableStateOf(TrigViewport()) }
    var lastTapAt by remember { mutableStateOf(0L) }
    LaunchedEffect(homeRequest) {
        if (homeRequest > 0) {
            viewport = TrigViewport()
            onZoomChanged(1f)
        }
    }
    fun angleAt(position: Offset, width: Float, height: Float): Float {
        val waveTop = height * .68f
        if (position.y >= waveTop && position.x in width * .15f..width * .71f) {
            val progress = ((position.x - width * .15f - 42f) / (width * .56f - 70f)).coerceIn(0f, 1f)
            val degrees = progress * 360f
            return if (degrees > 180f) degrees - 360f else degrees
        }
        val center = Offset(width * .47f, height * .42f)
        return Math.toDegrees(kotlin.math.atan2((center.y - position.y).toDouble(), (position.x - center.x).toDouble())).toFloat()
    }
    fun isManipulationTarget(position: Offset, width: Float, height: Float): Boolean {
        val waveHit = showWave && position.y in height * .66f..height * .91f && position.x in width * .12f..width * .74f
        val center = Offset(width * .47f, height * .42f)
        val radius = min(width, height) * .235f
        val circleHit = abs((position - center).getDistance() - radius) <= maxOf(48f, radius * .28f)
        return waveHit || circleHit
    }
    fun transformHandle(position: Offset, width: Float, height: Float): String? {
        if (!showWave) return null
        val top = height * .68f
        val paneHeight = height * .2f
        val origin = Offset(width * .15f + 42f, top + paneHeight / 2f)
        val waveWidth = width * .56f - 70f
        val scaleY = paneHeight * .14f
        val handles = mapOf(
            "amplitude" to Offset(origin.x + waveWidth, origin.y - transform.amplitude.toFloat() * scaleY),
            "period" to Offset(origin.x + (transform.period / (4 * Math.PI)).toFloat().coerceIn(.08f, 1f) * waveWidth, top + paneHeight - 20f),
            "phase" to Offset(origin.x + ((transform.phaseShift + Math.PI) / (2 * Math.PI)).toFloat().coerceIn(0f, 1f) * waveWidth, top + 20f),
            "vertical" to Offset(origin.x - 16f, origin.y - transform.verticalShift.toFloat() * scaleY),
        )
        return handles.minByOrNull { (_, point) -> (point - position).getDistance() }?.takeIf { (_, point) -> (point - position).getDistance() < 44f }?.key
    }
    fun updateTransform(handle: String, position: Offset, width: Float, height: Float) {
        val top = height * .68f
        val paneHeight = height * .2f
        val origin = Offset(width * .15f + 42f, top + paneHeight / 2f)
        val waveWidth = width * .56f - 70f
        val scaleY = paneHeight * .14f
        var a = transform.amplitude.toFloat(); var p = transform.period.toFloat(); var h = transform.phaseShift.toFloat(); var k = transform.verticalShift.toFloat()
        when (handle) {
            "amplitude" -> a = kotlin.math.abs((origin.y - position.y) / scaleY).coerceIn(.25f, 3f)
            "period" -> p = ((position.x - origin.x) / waveWidth * (4 * Math.PI).toFloat()).coerceIn(1f, 12.57f)
            "phase" -> h = (((position.x - origin.x) / waveWidth) * (2 * Math.PI).toFloat() - Math.PI.toFloat()).coerceIn(-3.14f, 3.14f)
            "vertical" -> k = ((origin.y - position.y) / scaleY).coerceIn(-2f, 2f)
        }
        onTransformChange(a, p, h, k)
    }
    val interactiveModifier = modifier.pointerInput(showWave, transform, polarSamples, harmonics) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            var gestureViewport = viewport
            fun content(screen: Offset): Offset {
                val value = TrigViewportEngine.screenToContent(gestureViewport, Vec2(screen.x.toDouble(), screen.y.toDouble()), size.width.toDouble(), size.height.toDouble())
                return Offset(value.x.toFloat(), value.y.toFloat())
            }
            val initialContent = content(down.position)
            val activeHandle = transformHandle(initialContent, size.width.toFloat(), size.height.toFloat())
            val objectDrag = activeHandle != null || isManipulationTarget(initialContent, size.width.toFloat(), size.height.toFloat())
            if (objectDrag && activeHandle == null) onAngleChange(angleAt(initialContent, size.width.toFloat(), size.height.toFloat()))
            var moved = false
            var transformed = false
            while (true) {
                val event = awaitPointerEvent()
                val pressed = event.changes.filter { it.pressed }
                if (pressed.size >= 2) {
                    val centroid = event.calculateCentroid()
                    val pan = event.calculatePan()
                    gestureViewport = TrigViewportEngine.transform(
                        gestureViewport,
                        size.width.toDouble(),
                        size.height.toDouble(),
                        Vec2(centroid.x.toDouble(), centroid.y.toDouble()),
                        Vec2(pan.x.toDouble(), pan.y.toDouble()),
                        event.calculateZoom(),
                    )
                    viewport = gestureViewport
                    onZoomChanged(gestureViewport.zoom)
                    transformed = true
                    event.changes.forEach { it.consume() }
                } else {
                    val change = event.changes.firstOrNull()
                    val delta = change?.positionChange() ?: Offset.Zero
                    if (change != null && delta.getDistance() > 0f) {
                        moved = moved || (change.position - down.position).getDistance() > 8f
                        if (objectDrag && !transformed) {
                            val current = content(change.position)
                            if (activeHandle != null) updateTransform(activeHandle, current, size.width.toFloat(), size.height.toFloat())
                            else onAngleChange(angleAt(current, size.width.toFloat(), size.height.toFloat()))
                        }
                        change.consume()
                    }
                }
                if (event.changes.none { it.pressed }) break
            }
            if (!moved && !transformed) {
                val now = System.currentTimeMillis()
                if (now - lastTapAt < 320L) {
                    viewport = TrigViewport()
                    onZoomChanged(1f)
                    lastTapAt = 0L
                } else {
                    if (objectDrag && activeHandle == null) onAngleChange(angleAt(content(down.position), size.width.toFloat(), size.height.toFloat()))
                    lastTapAt = now
                }
            }
        }
    }
    Canvas(interactiveModifier.semantics { contentDescription = "Interactive trigonometry workspace. Drag the unit-circle point or wave cursor; use two fingers to pan and pinch zoom the pane." }) {
        val viewportCenter = Offset(size.width / 2f, size.height / 2f)
        drawContext.canvas.save()
        drawContext.canvas.translate(viewportCenter.x + viewport.pan.x.toFloat(), viewportCenter.y + viewport.pan.y.toFloat())
        drawContext.canvas.scale(viewport.zoom, viewport.zoom)
        drawContext.canvas.translate(-viewportCenter.x, -viewportCenter.y)
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
        if (showWave) {
            val waveTopLeft = Offset(size.width * .15f, size.height * .68f)
            val waveSize = Size(size.width * .56f, size.height * .2f)
            drawSineWavePane(waveTopLeft, waveSize, angleDegrees, transform, function, visibleFunctions, showAsymptotes, harmonics, lineStyle, paletteShift)
            drawTrigTransformHandles(waveTopLeft, waveSize, transform)
            if (equationTarget != null) {
                val origin = Offset(waveTopLeft.x + 42f, waveTopLeft.y + waveSize.height / 2f)
                val waveWidth = waveSize.width - 70f
                val scaleY = waveSize.height * .14f
                val targetY = origin.y - equationTarget.coerceIn(-3.2f, 3.2f) * scaleY
                drawLine(Amber, Offset(origin.x, targetY), Offset(origin.x + waveWidth, targetY), 2.4f)
                equationRoots.forEach { root ->
                    val x = origin.x + (root / (2 * Math.PI)).toFloat().coerceIn(0f, 1f) * waveWidth
                    drawRadiantPoint(Offset(x, targetY), Green, "x=${radianLabel(Math.toDegrees(root))}")
                }
            }
        }
        if (polarSamples.isNotEmpty()) drawPolarCurvePane(Offset(size.width * .73f, size.height * .18f), Size(size.width * .24f, size.height * .25f), polarSamples)
        drawContext.canvas.restore()
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

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSineWavePane(
    topLeft: Offset,
    paneSize: Size,
    angleDegrees: Float,
    transform: TrigTransform,
    function: TrigFunction,
    functions: Set<TrigFunction> = setOf(function),
    showAsymptotes: Boolean = false,
    harmonics: List<HarmonicComponent> = emptyList(),
    lineStyle: TrigLineStyle = TrigLineStyle.Solid,
    paletteShift: Int = 0,
) {
    drawRoundRect(SurfaceA, topLeft = topLeft, size = paneSize, cornerRadius = androidx.compose.ui.geometry.CornerRadius(20f, 20f))
    drawRoundRect(Color(0x5548BFFF), topLeft = topLeft, size = paneSize, cornerRadius = androidx.compose.ui.geometry.CornerRadius(20f, 20f), style = Stroke(1.8f))
    val origin = Offset(topLeft.x + 42f, topLeft.y + paneSize.height / 2f)
    val width = paneSize.width - 70f
    val scaleY = paneSize.height * .14f
    drawLine(Color.White.copy(.8f), origin, Offset(topLeft.x + paneSize.width - 20f, origin.y), 2f)
    drawLine(Color.White.copy(.8f), Offset(origin.x, topLeft.y + 18f), Offset(origin.x, topLeft.y + paneSize.height - 18f), 2f)
    val palette = listOf(Violet, Cyan, Amber, Green, Color(0xFFFF6DAE), Color(0xFFB9FF66))
    functions.forEach { plotted ->
        val path = Path()
        var drawing = false
        for (i in 0..360) {
            val t = i / 360f
            val x = origin.x + t * width
            val value = transform.valueAt(t * Math.PI * 2, plotted)
            val y = origin.y - value.toFloat().coerceIn(-3.2f, 3.2f) * scaleY
            if (!value.isFinite() || kotlin.math.abs(value) > 12) drawing = false
            else if (!drawing) { path.moveTo(x, y); drawing = true } else path.lineTo(x, y)
        }
        val effect = when (lineStyle) { TrigLineStyle.Solid -> null; TrigLineStyle.Dashed -> androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 9f)); TrigLineStyle.Dotted -> androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(3f, 8f)) }
        drawPath(path, palette[(plotted.ordinal + paletteShift).mod(palette.size)], style = Stroke(if (plotted == function) 3.5f else 2.3f, cap = StrokeCap.Round, pathEffect = effect))
    }
    if (showAsymptotes && functions.any { it in setOf(TrigFunction.Tangent, TrigFunction.Secant, TrigFunction.Cosecant, TrigFunction.Cotangent) }) {
        val values = if (functions.any { it in setOf(TrigFunction.Tangent, TrigFunction.Secant) }) listOf(.25f, .75f) else listOf(0f, .5f, 1f)
        values.forEach { position -> drawLine(Amber.copy(.7f), Offset(origin.x + position * width, topLeft.y + 18f), Offset(origin.x + position * width, topLeft.y + paneSize.height - 18f), 1.5f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 8f))) }
    }
    if (harmonics.isNotEmpty()) {
        val path = Path()
        for (i in 0..360) {
            val t = i / 360f
            val value = InteractiveTrigEngine.harmonicValue(t * Math.PI * 2, harmonics)
            val point = Offset(origin.x + t * width, origin.y - value.toFloat().coerceIn(-3.2f, 3.2f) * scaleY)
            if (i == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
        }
        drawPath(path, Green, style = Stroke(4f, cap = StrokeCap.Round))
    }
    val t = ((angleDegrees % 360f) + 360f) % 360f / 360f
    val px = origin.x + t * width
    val currentValue = transform.valueAt(Math.toRadians(angleDegrees.toDouble()), function)
    val py = origin.y - currentValue.toFloat().coerceIn(-3.2f, 3.2f) * scaleY
    drawLine(Violet.copy(alpha = .7f), Offset(px, origin.y), Offset(px, py), 2f)
    drawRadiantPoint(Offset(px, py), Violet, "${trim(angleDegrees.toDouble())} deg")
    drawTrigText("${function.name}: A f(2π(x-h)/P)+k", topLeft.x + 22f, topLeft.y + 28f, Ink)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTrigTransformHandles(topLeft: Offset, paneSize: Size, transform: TrigTransform) {
    val origin = Offset(topLeft.x + 42f, topLeft.y + paneSize.height / 2f)
    val width = paneSize.width - 70f
    val scaleY = paneSize.height * .14f
    val handles = listOf(
        Triple("A", Offset(origin.x + width, origin.y - transform.amplitude.toFloat() * scaleY), Violet),
        Triple("P", Offset(origin.x + (transform.period / (4 * Math.PI)).toFloat().coerceIn(.08f, 1f) * width, topLeft.y + paneSize.height - 20f), Cyan),
        Triple("phase", Offset(origin.x + ((transform.phaseShift + Math.PI) / (2 * Math.PI)).toFloat().coerceIn(0f, 1f) * width, topLeft.y + 20f), Amber),
        Triple("k", Offset(origin.x - 16f, origin.y - transform.verticalShift.toFloat() * scaleY), Green),
    )
    handles.forEach { (label, point, color) -> drawCircle(SurfaceA, 16f, point); drawCircle(color, 13f, point, style = Stroke(4f)); drawTrigText(label, point.x + 18f, point.y - 8f, color) }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPolarCurvePane(topLeft: Offset, paneSize: Size, samples: List<Vec2>) {
    drawRoundRect(SurfaceA.copy(.94f), topLeft, paneSize, androidx.compose.ui.geometry.CornerRadius(18f, 18f))
    drawRoundRect(Cyan.copy(.65f), topLeft, paneSize, androidx.compose.ui.geometry.CornerRadius(18f, 18f), style = Stroke(1.6f))
    val center = topLeft + Offset(paneSize.width / 2, paneSize.height / 2)
    val radius = min(paneSize.width, paneSize.height) * .39f
    repeat(4) { index -> drawCircle(Grid.copy(.65f), radius * (index + 1) / 4f, center, style = Stroke(1f)) }
    drawLine(Grid, Offset(center.x - radius, center.y), Offset(center.x + radius, center.y), 1f)
    drawLine(Grid, Offset(center.x, center.y - radius), Offset(center.x, center.y + radius), 1f)
    val extent = samples.maxOfOrNull { maxOf(kotlin.math.abs(it.x), kotlin.math.abs(it.y)) }?.coerceAtLeast(1.0) ?: 1.0
    val path = Path()
    samples.forEachIndexed { index, point ->
        val screen = Offset(center.x + (point.x / extent).toFloat() * radius, center.y - (point.y / extent).toFloat() * radius)
        if (index == 0) path.moveTo(screen.x, screen.y) else path.lineTo(screen.x, screen.y)
    }
    drawPath(path, Violet, style = Stroke(3f, cap = StrokeCap.Round))
    drawTrigText("Polar explorer", topLeft.x + 14f, topLeft.y + 26f, Cyan)
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
    onGestureModeChange: (GestureMode) -> Unit,
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
                    onGestureModeChange(when {
                        vectorIndex != null -> GestureMode.Moving
                        solidIndex != null && transformMode == Transform3DMode.Move -> GestureMode.Moving
                        solidIndex != null && transformMode == Transform3DMode.Rotate -> GestureMode.Rotating
                        solidIndex != null -> GestureMode.Resizing
                        else -> GestureMode.Selecting
                    })

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
                            onGestureModeChange(if (abs(event.calculateZoom() - 1f) > .002f) GestureMode.Zooming else GestureMode.Panning)
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
                                    else -> { onOrbit(delta.x * .35f, -delta.y * .25f); onGestureModeChange(GestureMode.Rotating) }
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
                    onGestureModeChange(GestureMode.Idle)
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

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGrid(origin: Offset, scale: Float, settings: GraphAxisSettings = GraphAxisSettings()) {
    if (!scale.isFinite() || scale <= 0f) return
    val minX = (-origin.x / scale).toDouble()
    val maxX = ((size.width - origin.x) / scale).toDouble()
    val minY = ((origin.y - size.height) / scale).toDouble()
    val maxY = (origin.y / scale).toDouble()
    val xTicks = GraphViewport.ticks(minX, maxX)
    val yTicks = GraphViewport.ticks(minY, maxY)
    xTicks.forEach { value ->
        val x = origin.x + value.toFloat() * scale
        if (settings.gridVisible || value == 0.0) drawLine(if (value == 0.0) Color.White.copy(.85f) else Grid, Offset(x, 0f), Offset(x, size.height), if (value == 0.0) 2f else 1f)
    }
    yTicks.forEach { value ->
        val y = origin.y - value.toFloat() * scale
        if (settings.gridVisible || value == 0.0) drawLine(if (value == 0.0) Color.White.copy(.85f) else Grid, Offset(0f, y), Offset(size.width, y), if (value == 0.0) 2f else 1f)
    }
    val labelAxisY = origin.y.coerceIn(24f, size.height - 8f)
    val labelAxisX = origin.x.coerceIn(8f, size.width - 38f)
    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(210, 225, 240, 250)
            textSize = 21f
            isAntiAlias = true
        }
        paint.textAlign = android.graphics.Paint.Align.CENTER
        xTicks.filterNot { it == 0.0 }.forEach { value ->
            val displayValue = if (settings.xLogarithmic) 10.0.pow(value) else value
            drawText(GraphUxEngine.format(displayValue, if (settings.xLogarithmic) AxisNumberFormat.Scientific else settings.format, GraphViewport.axisStep(minX, maxX)) + settings.xUnit, origin.x + value.toFloat() * scale, labelAxisY - 7f, paint)
        }
        paint.textAlign = android.graphics.Paint.Align.RIGHT
        yTicks.filterNot { it == 0.0 }.forEach { value ->
            val displayValue = if (settings.yLogarithmic) 10.0.pow(value) else value
            drawText(GraphUxEngine.format(displayValue, if (settings.yLogarithmic) AxisNumberFormat.Scientific else settings.format, GraphViewport.axisStep(minY, maxY)) + settings.yUnit, labelAxisX - 7f, origin.y - value.toFloat() * scale + 7f, paint)
        }
        paint.textAlign = android.graphics.Paint.Align.LEFT
        paint.textSize = 25f
        drawText(settings.xName + if (settings.xLogarithmic) " (log)" else "", size.width - 70f, labelAxisY - 8f, paint)
        drawText(settings.yName + if (settings.yLogarithmic) " (log)" else "", labelAxisX + 9f, 26f, paint)
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
    selectedShapes: Set<Int>,
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
        val accent = if (index in selectedShapes) Amber else styled
        drawShape2D(shape.type, shapePoints, tx, accent, filled = true)
    }
    val visibleJunctions = shapes.filter { it.visible }.flatMap { it.pointIndices }.distinct()
    val selectedJunctions = shapes.getOrNull(selectedShape)?.pointIndices.orEmpty().toSet()
    visibleJunctions.forEach { pointIndex ->
        val point = points.getOrNull(pointIndex) ?: return@forEach
        val selected = pointIndex in selectedJunctions
        val accent = if (selected) Amber else Cyan
        drawCircle(accent.copy(if (selected) .24f else .15f), if (selected) 22f else 16f, tx(point))
        drawCircle(Color.White.copy(.95f), if (selected) 8f else 6f, tx(point))
        drawCircle(accent, if (selected) 5f else 3.5f, tx(point))
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
private fun MiniDock(modifier: Modifier = Modifier, items: List<String>, onMove: (Offset) -> Unit, onClick: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceA.copy(alpha = .92f))
            .border(1.dp, Color(0x5548BFFF), RoundedCornerShape(18.dp))
            .animateContentSize()
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            Modifier.pointerInput(Unit) { detectDragGestures { change, drag -> change.consume(); onMove(drag) } },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            TransparentIcon("✥", Cyan)
            if (expanded) Text("Quick actions", color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(
                if (expanded) "›" else "‹",
                color = Cyan,
                fontSize = 20.sp,
                modifier = Modifier
                    .clickable { expanded = !expanded }
                    .padding(7.dp)
                    .semantics { contentDescription = if (expanded) "Collapse quick actions" else "Expand quick actions" },
            )
        }
        AnimatedVisibility(expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                items.forEach { GlowButton(it, onClick = { onClick(it) }) }
            }
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
    var expanded by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    val containerSize = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current
    val launcherWidth = with(density) { 170.dp.toPx() }
    val reservedHeight = with(density) { 260.dp.toPx() }
    val maxX = (containerSize.width - launcherWidth).coerceAtLeast(0f)
    val maxY = ((containerSize.height - reservedHeight) / 2f).coerceAtLeast(0f)
    LaunchedEffect(containerSize) { dragOffset = Offset.Zero }
    Column(
        modifier
            .offset { IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt()) }
            .padding(start = 4.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceA.copy(.82f))
            .animateContentSize()
            .pointerInput(maxX, maxY) {
                detectDragGestures { change, amount ->
                    change.consume()
                    dragOffset = Offset(
                        (dragOffset.x + amount.x).coerceIn(0f, maxX),
                        (dragOffset.y + amount.y).coerceIn(-maxY, maxY),
                    )
                }
            }
            .semantics { contentDescription = "Movable Open tools launcher. Tap to expand and drag to move" }
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            Modifier
                .clip(RoundedCornerShape(14.dp))
                .clickable { expanded = !expanded }
                .padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            TransparentIcon("✥", Cyan)
            Text(if (expanded) "Hide tools" else "Open tools", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text("drag", color = Muted, fontSize = 9.sp)
        }
        AnimatedVisibility(expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                GlowButton(leftLabel, onClick = onLeft)
                GlowButton(rightLabel, onClick = onRight)
                GlowButton(bottomLabel, onClick = onBottom)
            }
        }
    }
}

@Composable
private fun GraphEquationEditor(
    modifier: Modifier = Modifier,
    functions: List<com.indianservers.aiexplorer.core.FunctionDefinition>,
    selectedId: String?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    addMenuExpanded: Boolean,
    onToggleAddMenu: () -> Unit,
    onAddKind: (GraphAddKind) -> Unit,
    onAdd: () -> Unit,
    onSelect: (String) -> Unit,
    onExpressionChange: (String, String) -> Unit,
    onToggleVisible: (String) -> Unit,
    onDuplicate: (String) -> Unit,
    onDelete: (String) -> Unit,
    onColor: (String) -> Unit,
    activeTool: GraphTool,
    onTool: (GraphTool) -> Unit,
) {
    val selected = functions.firstOrNull { it.id == selectedId }
    Column(
        modifier
            .padding(top = 70.dp, start = 8.dp, end = 8.dp)
            .widthIn(max = 560.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceA.copy(.94f))
            .border(1.dp, Cyan.copy(.48f), RoundedCornerShape(18.dp))
            .animateContentSize()
            .padding(7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            GlowButton("+", onClick = onToggleAddMenu)
            Text(
                "Equations (${functions.size})",
                color = Cyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).clickable { onExpandedChange(!expanded) }.padding(7.dp),
            )
            selected?.let { Text(it.name, color = graphColor(it.colorKey), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            GlowButton(if (expanded) "−" else "Edit", onClick = { onExpandedChange(!expanded) })
        }
        AnimatedVisibility(addMenuExpanded) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                GraphAddKind.entries.forEach { kind -> GlowButton(kind.label, onClick = { onAddKind(kind) }) }
            }
        }
        AnimatedVisibility(expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                if (functions.isEmpty()) {
                    Text("Tap + to add your first equation.", color = Muted, fontSize = 12.sp)
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        functions.forEach { function ->
                            val active = function.id == selected?.id
                            val accent = graphColor(function.colorKey)
                            var swipeDistance by remember(function.id) { mutableFloatStateOf(0f) }
                            Text(
                                text = "${if (function.visible) "●" else "○"} ${function.name}",
                                color = if (function.visible) Ink else Muted,
                                fontSize = 11.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(accent.copy(if (active) .28f else .10f))
                                    .border(if (active) 2.dp else 1.dp, accent.copy(if (active) .9f else .35f), RoundedCornerShape(14.dp))
                                    .pointerInput(function.id) {
                                        detectDragGestures(
                                            onDragStart = { swipeDistance = 0f },
                                            onDragEnd = { if (swipeDistance < -90f) onDelete(function.id) else if (swipeDistance > 90f) onDuplicate(function.id); swipeDistance = 0f },
                                        ) { change, drag -> swipeDistance += drag.x; change.consume() }
                                    }
                                    .clickable { onSelect(function.id) }
                                    .padding(horizontal = 9.dp, vertical = 7.dp),
                            )
                        }
                    }
                    selected?.let { function ->
                        OutlinedTextField(
                            value = function.expression,
                            onValueChange = { onExpressionChange(function.id, it) },
                            label = { Text("${function.name} — edit directly") },
                            visualTransformation = MathSyntaxVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Edit selected graph equation ${function.name}" },
                        )
                        Text("Smart actions for ${function.name}", color = Muted, fontSize = 10.sp)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            GlowButton(if (function.visible) "Hide" else "Show") { onToggleVisible(function.id) }
                            GlowButton("Color") { onColor(function.id) }
                            GlowButton("Copy") { onDuplicate(function.id) }
                            GlowButton("Delete") { onDelete(function.id) }
                            listOf(GraphTool.Plot, GraphTool.Trace, GraphTool.Tangent, GraphTool.Derivative, GraphTool.Integral, GraphTool.Table).forEach { tool ->
                                GlowButton(if (activeTool == tool) "● ${tool.name}" else tool.name) { onTool(tool) }
                            }
                        }
                    }
                }
            }
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
            .animateContentSize()
            .padding(11.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

@Composable
private fun PanelHeader(
    title: String,
    onClose: () -> Unit,
    accent: Color,
    icon: String = menuIcon(title),
    onMove: ((Offset) -> Unit)? = null,
) {
    val dragModifier = if (onMove == null) Modifier else Modifier.pointerInput(Unit) {
        detectDragGestures { change, drag -> change.consume(); onMove(drag) }
    }
    Row(Modifier.fillMaxWidth().then(dragModifier), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TransparentIcon(icon, accent)
            Column {
                Text(title, color = accent, fontWeight = FontWeight.Bold)
                if (onMove != null) Text("Drag to move", color = Muted, fontSize = 9.sp)
            }
        }
        Text(
            text = "×",
            color = Muted,
            fontSize = 20.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onClose)
                .padding(horizontal = 12.dp, vertical = 7.dp)
                .semantics { contentDescription = "Close $title" },
        )
    }
}

@Composable
private fun GlowButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    val symbolOnly = label in setOf("↶", "↷", "⋮")
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0x99101824), contentColor = Ink),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.heightIn(min = 42.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 9.dp, vertical = 5.dp),
    ) {
        TransparentIcon(menuIcon(label), if (enabled) Cyan else Muted)
        if (!symbolOnly) {
            Spacer(Modifier.width(5.dp))
            Text(label, fontSize = 12.sp, maxLines = 1)
        }
    }
}

@Composable
private fun TransparentIcon(symbol: String, tint: Color) {
    Box(
        Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(tint.copy(alpha = .10f))
            .border(1.dp, tint.copy(alpha = .32f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, color = tint, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SyntaxLegend(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(7.dp).clip(RoundedCornerShape(7.dp)).background(color))
        Text(label, color = color, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
    }
}

private class MathSyntaxVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val builder = AnnotatedString.Builder(text.text)
        MathInputIntelligence.analyze(text.text).tokens.forEach { token ->
            val bracketColors = listOf(Cyan, Violet, Green, Amber)
            val style = when (token.kind) {
                MathInputTokenKind.Function -> SpanStyle(color = Violet, fontWeight = FontWeight.Bold)
                MathInputTokenKind.Number -> SpanStyle(color = Cyan, fontWeight = FontWeight.SemiBold)
                MathInputTokenKind.Variable -> SpanStyle(color = Green, fontWeight = FontWeight.Bold)
                MathInputTokenKind.Constant -> SpanStyle(color = Amber, fontWeight = FontWeight.Bold)
                MathInputTokenKind.Operator -> SpanStyle(color = Ink)
                MathInputTokenKind.Bracket -> SpanStyle(color = bracketColors[(token.depth - 1).coerceAtLeast(0) % bracketColors.size], fontWeight = FontWeight.ExtraBold)
                MathInputTokenKind.Keyword -> SpanStyle(color = Color(0xFF79A7FF), fontWeight = FontWeight.SemiBold)
                MathInputTokenKind.Error -> SpanStyle(color = Color(0xFFFF6B7A), background = Color(0x44FF304F), fontWeight = FontWeight.Bold)
                MathInputTokenKind.Text -> SpanStyle(color = Muted)
            }
            builder.addStyle(style, token.start, token.end)
        }
        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}

private fun moduleIcon(module: MathModule): String = when (module) {
    MathModule.Geometry2D -> "△"
    MathModule.Geometry3D -> "◇"
    MathModule.Graph2D -> "ƒ"
    MathModule.Graph3D -> "⌁"
    MathModule.Trigonometry -> "θ"
    MathModule.Manipulatives -> "▦"
    MathModule.SpatialAR -> "AR"
}

private fun menuIcon(label: String): String = when {
    label.contains("menu", true) -> "☰"
    label.contains("subject", true) -> "⌂"
    label.contains("solver", true) || label.contains("solve", true) -> "✦"
    label.contains("equation", true) || label.contains("function", true) -> "ƒ"
    label.contains("graph", true) || label.contains("surface", true) -> "⌁"
    label.contains("3d", true) || label.contains("object", true) -> "◇"
    label.contains("tool", true) -> "⌘"
    label.contains("info", true) || label.contains("insight", true) -> "i"
    label.contains("learn", true) || label.contains("hint", true) -> "?"
    label.contains("export", true) || label.contains("save", true) -> "⇩"
    label.contains("delete", true) || label.contains("close", true) -> "×"
    label.contains("undo", true) || label == "↶" -> "↶"
    label.contains("redo", true) || label == "↷" -> "↷"
    label.contains("zoom", true) -> "⌕"
    label.contains("reset", true) || label.contains("fit", true) -> "◎"
    label.contains("move", true) -> "✥"
    label.contains("rotate", true) -> "↻"
    label.contains("scale", true) -> "↔"
    label.contains("planned", true) -> "◷"
    label.contains("workspace", true) -> "▦"
    label.contains("more", true) || label == "⋮" -> "⋮"
    else -> label.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "·"
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
