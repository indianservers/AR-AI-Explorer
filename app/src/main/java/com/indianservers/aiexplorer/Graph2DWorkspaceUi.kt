package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.adaptive.LocalAdaptiveDeviceProfile
import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import com.indianservers.aiexplorer.core.ExpressionEngine
import com.indianservers.aiexplorer.persistence.DurableMathStore
import com.indianservers.aiexplorer.persistence.LocalReliabilityMonitor
import com.indianservers.aiexplorer.persistence.MathFileExchange
import com.indianservers.aiexplorer.phase2.mathstudio.UnifiedMathStudioScreen
import com.indianservers.aiexplorer.phase3.mathlearning.AdaptiveMathLearningScreen
import com.indianservers.aiexplorer.probabilitystats.ProbabilityStatisticsPhase1Screen
import com.indianservers.aiexplorer.probabilitystats.ProbabilityStatisticsPhase2Screen
import com.indianservers.aiexplorer.probabilitystats.ProbabilityStatisticsPhase3Screen
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
import com.indianservers.aiexplorer.core.EditableSectionPlane
import com.indianservers.aiexplorer.core.ProjectedSpatialMesh
import com.indianservers.aiexplorer.core.ProjectedSpatialPoint
import com.indianservers.aiexplorer.core.SpatialSubObjectPicker
import com.indianservers.aiexplorer.core.SpatialSubObjectType
import com.indianservers.aiexplorer.core.SurfaceAnalysisHandleEngine
import com.indianservers.aiexplorer.core.TransformGizmoAxis
import com.indianservers.aiexplorer.core.TransformGizmoEngine
import com.indianservers.aiexplorer.core.TransformGizmoHandle
import com.indianservers.aiexplorer.core.TransformGizmoKind
import com.indianservers.aiexplorer.core.Graph3D
import com.indianservers.aiexplorer.core.GraphAnalysis
import com.indianservers.aiexplorer.core.GraphDefinitionKind
import com.indianservers.aiexplorer.core.CompareModeEngine
import com.indianservers.aiexplorer.core.ComparisonAttribute
import com.indianservers.aiexplorer.core.ComparisonItem
import com.indianservers.aiexplorer.core.StatisticsEngine
import com.indianservers.aiexplorer.core.ProbabilityEngine
import com.indianservers.aiexplorer.core.AdvancedGraphDefinition
import com.indianservers.aiexplorer.core.AdvancedGraphEngine
import com.indianservers.aiexplorer.core.TypedGraphEngine
import com.indianservers.aiexplorer.core.TypedGraphExpression
import com.indianservers.aiexplorer.core.TypedGraphExpressionParser
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
import com.indianservers.aiexplorer.core.GraphDirectManipulationEngine
import com.indianservers.aiexplorer.core.GraphFitResult
import com.indianservers.aiexplorer.core.ProfessionalGraphTable
import com.indianservers.aiexplorer.core.ProfessionalGraphTableEngine
import com.indianservers.aiexplorer.core.AdvancedSpatialInteractionEngine
import com.indianservers.aiexplorer.core.SpatialAlignment
import com.indianservers.aiexplorer.core.ConstraintAwareSpatialSnap
import com.indianservers.aiexplorer.core.SpatialCameraBookmark
import com.indianservers.aiexplorer.core.SpatialDragPlane
import com.indianservers.aiexplorer.core.SpatialTransformSpace
import com.indianservers.aiexplorer.core.SpatialVisualMode
import com.indianservers.aiexplorer.core.GraphViewState
import com.indianservers.aiexplorer.core.AxisNumberFormat
import com.indianservers.aiexplorer.core.DistributionEngine
import com.indianservers.aiexplorer.core.DistributionKind
import com.indianservers.aiexplorer.core.ProbabilityDistribution
import com.indianservers.aiexplorer.core.SurfaceCalculus
import com.indianservers.aiexplorer.core.SurfaceInputInterpreter
import com.indianservers.aiexplorer.core.AnalyticGeometry3D
import com.indianservers.aiexplorer.core.AdvancedStatisticsEngine
import com.indianservers.aiexplorer.core.MathSpreadsheetEngine
import com.indianservers.aiexplorer.core.MissingDataPolicy
import com.indianservers.aiexplorer.core.Phase4Statistics
import com.indianservers.aiexplorer.core.RandomExperimentEngine
import com.indianservers.aiexplorer.core.RandomExperimentKind
import com.indianservers.aiexplorer.core.ConditionalProbabilityEngine
import com.indianservers.aiexplorer.core.CombinatoricsLab
import com.indianservers.aiexplorer.core.ManipulativeEngine
import com.indianservers.aiexplorer.core.ManipulativeItem
import com.indianservers.aiexplorer.core.ManipulativeKind
import com.indianservers.aiexplorer.core.ManipulativeScene
import com.indianservers.aiexplorer.core.FormalMathDestination
import com.indianservers.aiexplorer.core.VisualProofCatalog
import com.indianservers.aiexplorer.core.VisualProofEngine
import com.indianservers.aiexplorer.core.BooleanFormulaLaw
import com.indianservers.aiexplorer.core.SetLogicCatalog
import com.indianservers.aiexplorer.core.SetLogicEngine
import com.indianservers.aiexplorer.core.SetStudioTool
import com.indianservers.aiexplorer.core.SetTheoryLearningCatalog
import com.indianservers.aiexplorer.core.SetTheoryStudioEngine
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
import com.indianservers.aiexplorer.core.CasAssumptionDraft
import com.indianservers.aiexplorer.core.CasInteractionEngine
import com.indianservers.aiexplorer.core.CasKeyboardCatalog
import com.indianservers.aiexplorer.core.CasKeyboardLayer
import com.indianservers.aiexplorer.core.CasSolutionMethod
import com.indianservers.aiexplorer.core.CasNotebookInteractionEngine
import com.indianservers.aiexplorer.core.CasNotebookSession
import com.indianservers.aiexplorer.core.CasInterpretationResolver
import com.indianservers.aiexplorer.core.CasInterpretationOption
import com.indianservers.aiexplorer.core.CasStructuredImportEngine
import com.indianservers.aiexplorer.core.CasDimensionalAnalyzer
import com.indianservers.aiexplorer.core.CasExportEngine
import com.indianservers.aiexplorer.core.CasExportFormat
import com.indianservers.aiexplorer.core.CasExpressionTransfer
import com.indianservers.aiexplorer.core.CasTargetAction
import com.indianservers.aiexplorer.core.CasCaptureNormalizer
import com.indianservers.aiexplorer.core.CasInputModality
import com.indianservers.aiexplorer.core.CasScopedVariable
import com.indianservers.aiexplorer.core.CasVariableScope
import com.indianservers.aiexplorer.core.CasStepDisclosureEngine
import com.indianservers.aiexplorer.core.CasDirectManipulationEngine
import com.indianservers.aiexplorer.core.CasManipulationState
import com.indianservers.aiexplorer.core.MathAssumptionSet
import com.indianservers.aiexplorer.core.MathNumberDomain
import com.indianservers.aiexplorer.input.IntentAwareMathField
import com.indianservers.aiexplorer.input.IntentAwareMathValueField
import com.indianservers.aiexplorer.input.CompactMathField
import com.indianservers.aiexplorer.input.MathKeyboardContext
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
import com.indianservers.aiexplorer.core.SmartScientificCalculator
import com.indianservers.aiexplorer.core.SmartCalculatorOutcome
import com.indianservers.aiexplorer.core.CalculatorInputIntelligence
import com.indianservers.aiexplorer.core.CalculatorKeyboardLayer
import com.indianservers.aiexplorer.core.CalculatorEditorHistory
import com.indianservers.aiexplorer.core.AdvancedCalculatorMode
import com.indianservers.aiexplorer.core.AdvancedScientificCalculator
import com.indianservers.aiexplorer.core.CalculatorFavourites
import com.indianservers.aiexplorer.core.CalculatorRecognitionAdapters
import com.indianservers.aiexplorer.core.ProfessionalCalculatorMode
import com.indianservers.aiexplorer.core.ProfessionalScientificCalculator
import com.indianservers.aiexplorer.learningintelligence.ui.LearningIntelligenceFeatureRoot
import com.indianservers.aiexplorer.core.SolutionStepRole
import com.indianservers.aiexplorer.core.SymbolicCasEngine
import com.indianservers.aiexplorer.core.Solid
import com.indianservers.aiexplorer.core.SolidType
import com.indianservers.aiexplorer.core.SmartSolidPlacementEngine
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
import com.indianservers.aiexplorer.learning.icon
import com.indianservers.aiexplorer.learning.KnowledgeLevel
import com.indianservers.aiexplorer.learning.KnowledgeTopic
import com.indianservers.aiexplorer.learning.DictionaryClassBand
import com.indianservers.aiexplorer.learning.DictionaryDifficulty
import com.indianservers.aiexplorer.learning.DictionaryTerm
import com.indianservers.aiexplorer.learning.MathKnowledgeCatalog
import com.indianservers.aiexplorer.learning.MathDictionaryCatalog
import com.indianservers.aiexplorer.learning.KnowledgeSearchResult
import com.indianservers.aiexplorer.learning.McqQuestion
import com.indianservers.aiexplorer.learning.OfflineLearningQueue
import com.indianservers.aiexplorer.learning.PackageValidation
import com.indianservers.aiexplorer.input.HandwritingMathInput
import com.indianservers.aiexplorer.learning.ProgressStatus
import com.indianservers.aiexplorer.learning.QuizEngine
import com.indianservers.aiexplorer.learning.QuizLevel
import com.indianservers.aiexplorer.learning.QuizSession
import com.indianservers.aiexplorer.learning.QuizSubject
import com.indianservers.aiexplorer.learning.theoremCategories
import com.indianservers.aiexplorer.workspace.AddVector3DCommand
import com.indianservers.aiexplorer.workspace.DeleteVector3DCommand
import com.indianservers.aiexplorer.workspace.AddConstructionCommand
import com.indianservers.aiexplorer.workspace.AddDependentPointCommand
import com.indianservers.aiexplorer.workspace.AddGeometryConstraint2DCommand
import com.indianservers.aiexplorer.workspace.AddShapeFromPointsCommand
import com.indianservers.aiexplorer.workspace.AddFunctionCommand
import com.indianservers.aiexplorer.workspace.AddPointCommand
import com.indianservers.aiexplorer.workspace.AddSolidCommand
import com.indianservers.aiexplorer.workspace.CommandHistory
import com.indianservers.aiexplorer.workspace.EditExpressionCommand
import com.indianservers.aiexplorer.workspace.DeleteShapeCommand
import com.indianservers.aiexplorer.workspace.DeleteSolidCommand
import com.indianservers.aiexplorer.workspace.DeleteShapesCommand
import com.indianservers.aiexplorer.workspace.DeleteSolidsCommand
import com.indianservers.aiexplorer.workspace.ReplaceSolidsCommand
import com.indianservers.aiexplorer.workspace.DeleteFunctionCommand
import com.indianservers.aiexplorer.workspace.LinkedMathKernel
import com.indianservers.aiexplorer.workspace.LinkedMathView
import com.indianservers.aiexplorer.workspace.GraphRowMetadataState
import com.indianservers.aiexplorer.workspace.GraphSliderMetadataState
import com.indianservers.aiexplorer.workspace.GraphSliderPlaybackMode
import com.indianservers.aiexplorer.workspace.Geometry2DInteractionEngine
import com.indianservers.aiexplorer.workspace.Geometry2DDirectManipulation
import com.indianservers.aiexplorer.workspace.Geometry2DDragPlanner
import com.indianservers.aiexplorer.workspace.ConstraintFeedbackLevel
import com.indianservers.aiexplorer.workspace.GeometryProtocolStatus
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
import com.indianservers.aiexplorer.workspace.ReplaceGeometry2DCommand
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
import com.indianservers.aiexplorer.workspace.WorkspaceProjectCodec
import com.indianservers.aiexplorer.workspace.WorkspaceState
import com.indianservers.aiexplorer.workspace.Geometry2DSnapshot
import com.indianservers.aiexplorer.workspace.geometry2DSnapshot
import com.indianservers.aiexplorer.workspace.UniversalMathDocument
import com.indianservers.aiexplorer.workspace.UniversalMathDocumentEngine
import com.indianservers.aiexplorer.workspace.UniversalWorkspaceBridge
import com.indianservers.aiexplorer.workspace.UniversalMathObjectFactory
import com.indianservers.aiexplorer.workspace.recomputed
import com.indianservers.aiexplorer.workspace.resolvePointDependency
import com.indianservers.aiexplorer.spatial.ARScaleMode
import com.indianservers.aiexplorer.spatial.ARAvailability
import com.indianservers.aiexplorer.spatial.ARCapabilities
import com.indianservers.aiexplorer.arengine.arcore.ArCoreRuntime
import com.indianservers.aiexplorer.arengine.contract.ArVector2
import com.indianservers.aiexplorer.arengine.contract.ArVector3
import com.indianservers.aiexplorer.arengine.contract.ArFrameSnapshot
import com.indianservers.aiexplorer.arengine.contract.ArHitCandidate
import com.indianservers.aiexplorer.arengine.contract.ArHitPolicy
import com.indianservers.aiexplorer.arengine.contract.ArRuntimeState
import com.indianservers.aiexplorer.arengine.contract.ArTrackingState
import com.indianservers.aiexplorer.arengine.interaction.ArGizmoAxis
import com.indianservers.aiexplorer.arengine.interaction.ArGizmoMode
import com.indianservers.aiexplorer.arengine.interaction.ArPickHit
import com.indianservers.aiexplorer.arengine.interaction.ArSelectionEngine
import com.indianservers.aiexplorer.arengine.interaction.ArSelectionState
import com.indianservers.aiexplorer.arengine.interaction.ArSubObjectKind
import com.indianservers.aiexplorer.spatial.SpatialSafety
import com.indianservers.aiexplorer.spatial.TrackingQuality
import com.indianservers.aiexplorer.spatial.SpatialPlacementEngine
import com.indianservers.aiexplorer.spatial.ARCoreCompositorView
import com.indianservers.aiexplorer.spatial.ARFrameState
import com.indianservers.aiexplorer.spatial.toSpatialCapabilities
import com.indianservers.aiexplorer.spatial.toSpatialFrame
import com.indianservers.aiexplorer.spatial.toSpatialHit
import com.indianservers.aiexplorer.spatial.previewSpatialPlacement
import com.indianservers.aiexplorer.spatial.ArPhase4SpatialBridge
import com.indianservers.aiexplorer.spatial.ArPhase5AnalysisBridge
import com.indianservers.aiexplorer.spatial.ArPhase5AnalysisOptions
import com.indianservers.aiexplorer.spatial.ArMathWorkspaceBridge
import com.indianservers.aiexplorer.spatial.ArMathWorkspaceMode
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
import com.indianservers.aiexplorer.gamifymaths.GamifyMathsRoot
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
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

@Composable
internal fun Graph2DScreen(vm: ExplorerViewModel) {
    val adaptiveProfile = LocalAdaptiveDeviceProfile.current
    val workspaceToolTop = if (adaptiveProfile.isTelevision) {
        adaptiveProfile.workspacePolicy.topChromeClearance
    } else {
        72.dp
    }
    val workspacePanelWidth = if (adaptiveProfile.isTelevision) {
        adaptiveProfile.workspacePolicy.sidePanelWidth
    } else {
        300.dp
    }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val graphScope = rememberCoroutineScope()
    val graph = remember { GraphAnalysis() }
    val advancedGraphEngine = remember { AdvancedGraphEngine() }
    val advancedGraph = remember { AdvancedGraphEngine() }
    val engine = remember { ExpressionEngine() }
    val dataTableEngine = remember { ProfessionalGraphTableEngine(engine) }
    var traceX by remember { mutableFloatStateOf(2f) }
    var graphTool by remember { mutableStateOf(GraphTool.Plot) }
    var parameterA by remember { mutableFloatStateOf(1f) }
    var graphParameterValues by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var playingParameters by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedGraphRowId by remember { mutableStateOf<String?>(null) }
    var equationEditorExpanded by remember { mutableStateOf(false) }
    var graphTypingMode by remember { mutableStateOf(false) }
    var graphAddMenuExpanded by remember { mutableStateOf(false) }
    var graphViewToolsExpanded by remember { mutableStateOf(false) }
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
    var functionTableStart by rememberSaveable { mutableStateOf("-4") }
    var functionTableEnd by rememberSaveable { mutableStateOf("4") }
    var functionTableStep by rememberSaveable { mutableStateOf("1") }
    var csvSource by rememberSaveable { mutableStateOf("x,y\n0,0\n1,1\n2,4") }
    var csvHasHeader by rememberSaveable { mutableStateOf(true) }
    var importedTable by remember { mutableStateOf<ProfessionalGraphTable?>(null) }
    var selectedCsvX by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedCsvY by rememberSaveable { mutableStateOf<String?>(null) }
    var csvMessage by remember { mutableStateOf("Choose a CSV/TSV file or paste data.") }
    var csvImportOpen by rememberSaveable { mutableStateOf(false) }
    var brushInterval by remember { mutableStateOf<ClosedFloatingPointRange<Double>?>(null) }
    var sketchPoints by remember { mutableStateOf<List<Vec2>>(emptyList()) }
    var latestSketchFit by remember { mutableStateOf<GraphFitResult?>(null) }
    var pinnedTracePoints by remember { mutableStateOf<List<Vec2>>(emptyList()) }
    var directCurveDelta by remember { mutableStateOf<Vec2?>(null) }
    var splitComparison by remember { mutableStateOf(false) }
    var showResidualPlot by remember { mutableStateOf(false) }
    var presentationMode by remember { mutableStateOf(false) }
    var graphLayerQuery by rememberSaveable { mutableStateOf("") }
    var showPointsOfInterest by remember { mutableStateOf(true) }
    var showSplitTable by remember { mutableStateOf(false) }
    var snapshotOpacity by remember { mutableFloatStateOf(.45f) }
    var accessibilityMode by remember { mutableStateOf(false) }
    var clearEpochSeen by remember { mutableIntStateOf(vm.workspaceClearEpoch) }
    BackHandler(
        enabled = equationEditorExpanded || graphAddMenuExpanded || graphViewToolsExpanded || showAxisSheet || contextMenuPosition != null || vm.showBottomPanel,
    ) {
        when {
            graphAddMenuExpanded -> graphAddMenuExpanded = false
            equationEditorExpanded -> {
                equationEditorExpanded = false
                graphTypingMode = false
            }
            showAxisSheet -> showAxisSheet = false
            contextMenuPosition != null -> contextMenuPosition = null
            vm.showBottomPanel -> vm.hidePanels()
            graphViewToolsExpanded -> graphViewToolsExpanded = false
        }
    }
    LaunchedEffect(vm.workspaceClearEpoch) {
        if (vm.workspaceClearEpoch != clearEpochSeen) {
            playingParameters = emptySet()
            selectedGraphRowId = null
            graphParameterValues = emptyMap()
            graphDomains = emptyMap()
            graphStyles = emptyMap()
            graphLabelOffsets = emptyMap()
            graphSnapshots = emptyList()
            graphSnapshotOverlay = null
            dataText = ""
            importedTable = null
            selectedCsvX = null
            selectedCsvY = null
            csvMessage = "Choose a CSV/TSV file or paste data."
            csvImportOpen = false
            brushInterval = null
            sketchPoints = emptyList()
            latestSketchFit = null
            pinnedTracePoints = emptyList()
            directCurveDelta = null
            clearEpochSeen = vm.workspaceClearEpoch
        }
    }
    val graphRowMetadata = vm.state.graphRowMetadata
    val graphSliderMetadata = vm.state.graphSliderMetadata
    val persistedParameterValues = graphSliderMetadata.mapNotNull { (name, metadata) ->
        metadata.value?.let { name to it }
    }.toMap()
    val functionTableInputs = remember(functionTableStart, functionTableEnd, functionTableStep) {
        val start = functionTableStart.toDoubleOrNull()
        val end = functionTableEnd.toDoubleOrNull()
        val step = functionTableStep.toDoubleOrNull()
        if (start == null || end == null || step == null) emptyList()
        else runCatching { dataTableEngine.functionInputs(start, end, step) }.getOrDefault(emptyList())
    }
    val objectGraphSnapshot = vm.mathObjectGraphSnapshot(
        persistedParameterValues + graphParameterValues,
        functionTableInputs,
    )
    fun readCsv(source: String, label: String) {
        runCatching {
            val table = dataTableEngine.paste(source, csvHasHeader)
            require(table.columns.count { column -> column.values.any { it != null } } >= 2) {
                "CSV needs at least two numeric columns."
            }
            table
        }
            .onSuccess { table ->
                importedTable = table
                val numeric = table.columns.filter { column -> column.values.any { it != null } }
                selectedCsvX = numeric.first().name
                selectedCsvY = numeric.getOrElse(1) { numeric.first() }.name
                csvMessage = "$label · ${table.rowCount} rows · ${table.columns.size} columns"
            }
            .onFailure { error ->
                importedTable = null
                selectedCsvX = null
                selectedCsvY = null
                csvMessage = error.message ?: "Could not read this CSV data."
            }
    }
    val csvPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: error("The selected file could not be opened.")
            require(bytes.size <= 12 * 1024 * 1024) { "Choose a CSV file smaller than 12 MB." }
            bytes.toString(Charsets.UTF_8)
        }.onSuccess { source ->
            csvSource = source
            readCsv(source, uri.lastPathSegment?.substringAfterLast('/') ?: "Imported file")
        }.onFailure { error ->
            csvMessage = error.message ?: "Could not open this CSV file."
        }
    }
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
    val traceY = remember(primaryExpression, traceX, graphParameterValues) {
        primaryExpression?.let { expression ->
            runCatching {
                engine.compile(stripEquation(expression)).eval(graphParameterValues + ("x" to traceX.toDouble()))
            }.getOrNull()?.takeIf(Double::isFinite)
        }
    }
    val traceSlope = remember(primaryExpression, traceX) {
        primaryExpression?.let { runCatching { graph.derivative(it, traceX.toDouble()) }.getOrNull() }
    }
    val pointsOfInterest = remember(roots, extrema, intersections) {
        buildList {
            roots.forEach { add("Root" to Vec2(it, 0.0)) }
            extrema.forEach { add("Extremum" to it) }
            intersections.forEach { add("Intersection" to it) }
        }.sortedBy { it.second.x }
    }
    LaunchedEffect(presentationMode) {
        if (presentationMode) {
            equationEditorExpanded = false
            graphAddMenuExpanded = false
            graphViewToolsExpanded = false
            showAxisSheet = false
            vm.hidePanels()
        }
    }
    Box(Modifier.fillMaxSize()) {
        GraphCanvas(
            modifier = Modifier.fillMaxSize()
                .background(WorkspaceVisualStyles.ReferenceNavy)
                .appWorkspaceTreatment(0.dp, WorkspaceVisualStyles.ReferenceCyan, WorkspaceVisualStyles.ReferenceMagenta),
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
            parameterValues = objectGraphSnapshot.parameterRows.associate { it.name to it.value },
            previewExpression = directCurveDelta?.let { delta -> selectedFunction?.let { GraphDirectManipulationEngine.translate(it.expression, delta) } }
                ?: selectedFunction?.let { GraphUxEngine.transform(it.expression, graphTransformKind, graphTransformAmount.toDouble().let { amount -> if (graphTransformKind in setOf(GraphTransformKind.StretchX, GraphTransformKind.StretchY)) kotlin.math.abs(amount).coerceAtLeast(.1) else amount }) },
            brushInterval = brushInterval,
            sketchPoints = sketchPoints,
            pinnedTracePoints = pinnedTracePoints,
            showResidualPlot = showResidualPlot,
            splitComparison = splitComparison,
            snapshotExpressions = graphSnapshotOverlay?.expressions.orEmpty(),
            snapshotOpacity = snapshotOpacity,
            selectedFunctionId = selectedGraphRowId,
            onSelectFunction = {
                selectedGraphRowId = it
                equationEditorExpanded = true
            },
            onClearSelection = {
                selectedGraphRowId = null
                equationEditorExpanded = false
                vm.dismissAllMenusAndPanels()
            },
            onTraceChange = { traceX = it.toFloat().coerceIn(-1_000f, 1_000f) },
            onParameterAChange = { parameterA = it.toFloat().coerceIn(-20f, 20f) },
            onParameterChange = { name, value ->
                graphParameterValues = graphParameterValues + (name to value.coerceIn(-20.0, 20.0))
                if (name == "a") parameterA = value.toFloat().coerceIn(-20f, 20f)
            },
            onDomainChange = { id, domain -> graphDomains = graphDomains + (id to domain) },
            onLabelMove = { id, delta -> graphLabelOffsets = graphLabelOffsets + (id to ((graphLabelOffsets[id] ?: Offset.Zero) + delta)) },
            onCurveDrag = { id, delta ->
                selectedGraphRowId = id
                directCurveDelta = delta
            },
            onCurveDragEnd = { id, delta ->
                val index = vm.state.functions.indexOfFirst { it.id == id }
                if (index >= 0 && (abs(delta.x) > .01 || abs(delta.y) > .01)) vm.editExpression(index, GraphDirectManipulationEngine.translate(vm.state.functions[index].expression, delta))
                directCurveDelta = null
            },
            onBrushChange = { start, end -> brushInterval = min(start, end)..max(start, end) },
            onSketchChange = { sketchPoints = it },
            onSketchEnd = { points ->
                latestSketchFit = GraphDirectManipulationEngine.fit(points)
                latestSketchFit?.let { fit -> vm.addFunction(fit.expression); selectedGraphRowId = vm.state.functions.lastOrNull()?.id }
                sketchPoints = emptyList()
            },
            onDataPointMove = { index, point ->
                val updated = dataPoints.toMutableList()
                if (index in updated.indices) {
                    updated[index] = point
                    dataText = updated.joinToString("; ") { "${trim(it.x)},${trim(it.y)}" }
                }
            },
            onViewportChange = { graphViewport = it },
            onContextMenu = { id, point -> contextMenuFunctionId = id; contextMenuPosition = point },
        )
        if (presentationMode) {
            GlowButton("Exit presentation") { presentationMode = false }
        }
        if (!presentationMode && showPointsOfInterest && pointsOfInterest.isNotEmpty() && !graphTypingMode) {
            Row(
                Modifier.align(Alignment.TopCenter).padding(
                    top = if (adaptiveProfile.isTelevision) workspaceToolTop + 54.dp else 142.dp,
                    start = 10.dp,
                    end = 10.dp,
                )
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                pointsOfInterest.take(12).forEach { (kind, point) ->
                    GlowButton("$kind (${trim(point.x)}, ${trim(point.y)})") {
                        traceX = point.x.toFloat()
                        graphTool = GraphTool.Trace
                        if (vm.settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }
            }
        }
        if (graphTool in setOf(GraphTool.Trace, GraphTool.Tangent, GraphTool.Derivative) && traceY != null) {
            Column(
                Modifier.align(Alignment.BottomCenter).padding(bottom = if (presentationMode) 24.dp else 72.dp)
                    .clip(RoundedCornerShape(14.dp)).background(SurfaceA.copy(.9f))
                    .border(1.dp, Green.copy(.55f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Trace (${trim(traceX.toDouble())}, ${trim(traceY)})", color = Green, fontWeight = FontWeight.Bold)
                Text("slope ${traceSlope?.let(::trim) ?: "undefined"} · ${analysisFunction?.name ?: "function"}", color = Muted, fontSize = 10.sp)
            }
        }
        if (!presentationMode && showSplitTable && objectGraphSnapshot.generatedTable.isNotEmpty()) {
            GlassPanel(Modifier.align(Alignment.CenterEnd).width(260.dp)) {
                PanelHeader("Linked Table", { showSplitTable = false }, Green)
                GeneratedTablePreview(objectGraphSnapshot.generatedTable)
            }
        }
        if (!presentationMode && objectGraphSnapshot.parameterRows.isNotEmpty() && !vm.showLeftPanel && !graphTypingMode) {
            Column(
                Modifier.align(Alignment.BottomStart).padding(start = 12.dp, bottom = 78.dp)
                    .widthIn(max = 300.dp).clip(RoundedCornerShape(14.dp))
                    .background(SurfaceA.copy(.88f)).padding(7.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("Parameters", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                objectGraphSnapshot.parameterRows.take(3).forEach { parameter ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${parameter.name}=${trim(parameter.value)}", color = Ink, fontSize = 10.sp, modifier = Modifier.width(72.dp))
                        Slider(
                            value = parameter.value.toFloat(),
                            onValueChange = { value ->
                                graphParameterValues = graphParameterValues + (parameter.name to value.toDouble())
                                if (parameter.name == "a") parameterA = value
                            },
                            valueRange = parameter.min.toFloat()..parameter.max.toFloat(),
                            modifier = Modifier.weight(1f),
                        )
                        GlowButton(if (parameter.name in playingParameters) "Pause" else "Play") {
                            playingParameters = if (parameter.name in playingParameters) playingParameters - parameter.name else playingParameters + parameter.name
                        }
                    }
                }
            }
        }
        if (!presentationMode && accessibilityMode && !graphTypingMode) {
            val spokenSummary = buildString {
                append("Graph workspace. ${visibleFunctions.size} visible functions. ")
                append("${roots.size} roots, ${extrema.size} extrema, and ${intersections.size} intersections. ")
                traceY?.let { append("Trace x ${trim(traceX.toDouble())}, y ${trim(it)}.") }
            }
            Column(
                Modifier.align(Alignment.CenterStart).padding(start = 12.dp).widthIn(max = 270.dp)
                    .clip(RoundedCornerShape(14.dp)).background(SurfaceA.copy(.92f))
                    .border(1.dp, Amber.copy(.55f), RoundedCornerShape(14.dp)).padding(9.dp),
            ) {
                Text("Accessible graph", color = Amber, fontWeight = FontWeight.Bold)
                Text(spokenSummary, color = Ink, fontSize = 10.sp)
                GlowButton("Read description") {
                    (context as? Activity)?.window?.decorView?.announceForAccessibility(spokenSummary)
                }
            }
        }
        if (!graphTypingMode && !presentationMode && !vm.hasDismissibleOverlay()) Row(
            Modifier.align(Alignment.BottomCenter).padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AddShapeTarget(
                onAdd = {
                    vm.addFunction("x")
                    selectedGraphRowId = vm.state.functions.lastOrNull()?.id
                    equationEditorExpanded = true
                    graphAddMenuExpanded = false
                },
                label = "+ Graph",
                contentDescription = "Add a graph equation to the workspace",
            )
            DeleteDropTarget(
                enabled = selectedFunction != null,
                onDelete = {
                    selectedFunction?.let { function ->
                        val index = vm.state.functions.indexOfFirst { it.id == function.id }
                        if (index >= 0) {
                            vm.deleteFunction(index)
                            selectedGraphRowId = vm.state.functions.getOrNull(index.coerceAtMost(vm.state.functions.lastIndex))?.id
                            if (vm.state.functions.isEmpty()) equationEditorExpanded = false
                        }
                    }
                },
            )
            DestructiveGlowButton(
                "Clear all",
                enabled = vm.state.functions.isNotEmpty() || dataText.isNotBlank() || graphSnapshots.isNotEmpty() || pinnedTracePoints.isNotEmpty() || sketchPoints.isNotEmpty(),
                icon = "Ã—",
                onClick = vm::clearCurrentWorkspace,
            )
        }
        if (!graphTypingMode && !presentationMode && selectedFunction != null) SmartSelectionHud(
            title = selectedFunction.name,
            instruction = if (Regex("\\ba\\b").containsMatchIn(selectedFunction.expression)) {
                "Drag the curve handle to change a · drag endpoint handles to edit its domain"
            } else {
                "Tap/hold the curve for actions · drag its label or domain endpoints directly"
            },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 76.dp),
        ) {
            GlowButton("Trace") { graphTool = GraphTool.Trace }
            GlowButton("Pin point") {
                runCatching { engine.compile(stripEquation(selectedFunction.expression)).eval(mapOf("x" to traceX.toDouble())) }.getOrNull()?.takeIf(Double::isFinite)?.let { y ->
                    pinnedTracePoints = (pinnedTracePoints + Vec2(traceX.toDouble(), y)).takeLast(8)
                }
            }
            GlowButton("Brush area") { graphTool = GraphTool.BrushArea; brushInterval = null }
            GlowButton("Sketch fit") { graphTool = GraphTool.SketchFit; sketchPoints = emptyList() }
            GlowButton("Domain") {
                graphDomains = graphDomains + (selectedFunction.id to (graphDomains[selectedFunction.id] ?: GraphDomainSelection()))
            }
            DestructiveGlowButton("Delete", icon = "×") {
                val index = vm.state.functions.indexOfFirst { it.id == selectedFunction.id }
                if (index >= 0) {
                    vm.deleteFunction(index)
                    selectedGraphRowId = vm.state.functions.getOrNull(index.coerceAtMost(vm.state.functions.lastIndex))?.id
                }
            }
        }
        if (!graphTypingMode && !presentationMode) Column(
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = workspaceToolTop, end = 10.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceA.copy(.82f))
                .animateContentSize()
                .padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { graphViewToolsExpanded = !graphViewToolsExpanded }
                    .padding(horizontal = 8.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                TransparentIcon("V", Cyan)
                Text("View", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(if (graphViewToolsExpanded) "Collapse ▲" else "Expand ▼", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            AnimatedVisibility(graphViewToolsExpanded) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    GlowButton("Undo", enabled = vm.canUndo, onClick = vm::undo)
                    GlowButton("Redo", enabled = vm.canRedo, onClick = vm::redo)
                    GlowButton("Back") { graphBackRequest++ }
                    GlowButton("Forward") { graphForwardRequest++ }
                    GlowButton("Fit") { graphHomeRequest++ }
                    GlowButton("Axis") { showAxisSheet = !showAxisSheet }
                    TogglePill("Mini map", showMiniMap) { showMiniMap = it }
                    TogglePill("POI", showPointsOfInterest) { showPointsOfInterest = it }
                    TogglePill("Table split", showSplitTable) { showSplitTable = it }
                    TogglePill("Accessible", accessibilityMode) { accessibilityMode = it }
                    GlowButton("Present") { presentationMode = true }
                }
            }
        }
        if (!presentationMode && (equationEditorExpanded || graphAddMenuExpanded)) {
            DimmedWorkspaceScrim {
                equationEditorExpanded = false
                graphAddMenuExpanded = false
                graphTypingMode = false
            }
        }
        if (!presentationMode) GraphEquationEditor(
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
            onTypingChange = { typing ->
                graphTypingMode = typing
                if (typing) {
                    graphAddMenuExpanded = false
                    contextMenuPosition = null
                    showAxisSheet = false
                    vm.hidePanels()
                }
            },
        )
        if (showAxisSheet) {
            DimmedWorkspaceScrim { showAxisSheet = false }
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
        if (contextMenuPosition != null) {
            DimmedWorkspaceScrim { contextMenuPosition = null }
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
        if (vm.showLeftPanel) GlassPanel(Modifier.align(Alignment.TopStart).width(workspacePanelWidth)) {
            PanelHeader("Equations & Definitions", vm::hidePanels, Cyan)
            Text("Desmos-style rows · expressions, sliders and generated tables share one object graph.", color = Muted, fontSize = 11.sp)
            OutlinedTextField(
                value = graphLayerQuery,
                onValueChange = { graphLayerQuery = it },
                label = { Text("Search equations, notes or folders") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            objectGraphSnapshot.expressionRows.mapIndexed { index, row -> index to row }
                .filter { (_, row) ->
                    val metadata = graphRowMetadata[row.id] ?: GraphRowMetadataState()
                    graphLayerQuery.isBlank() || listOf(row.name, row.expression, metadata.note, metadata.folder)
                        .any { it.contains(graphLayerQuery.trim(), ignoreCase = true) }
                }
                .forEach { (index, row) ->
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
                        onValueChangeFinished = {
                            val value = graphParameterValues[parameter.name] ?: parameter.value
                            vm.updateGraphSliderMetadata(parameter.name) { parameterUi.copy(value = value) }
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
        if (vm.showRightPanel) GlassPanel(
            Modifier.align(Alignment.TopEnd).width(
                if (adaptiveProfile.isTelevision) adaptiveProfile.workspacePolicy.sidePanelWidth else 270.dp,
            ),
        ) {
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
                FunctionDataTablePanel(
                    rows = objectGraphSnapshot.generatedTable,
                    start = functionTableStart,
                    end = functionTableEnd,
                    step = functionTableStep,
                    onStart = { functionTableStart = it },
                    onEnd = { functionTableEnd = it },
                    onStep = { functionTableStep = it },
                )
            }
            if (graphTool == GraphTool.Data) {
                OutlinedTextField(
                    value = dataText,
                    onValueChange = { dataText = it },
                    label = { Text("Data points: x,y; x,y") },
                    modifier = Modifier.fillMaxWidth(),
                )
                GlowButton(if (csvImportOpen) "Close CSV import" else "Import CSV", icon = "table") {
                    csvImportOpen = !csvImportOpen
                }
                if (csvImportOpen) {
                    CsvDataImportPanel(
                        source = csvSource,
                        hasHeader = csvHasHeader,
                        table = importedTable,
                        selectedX = selectedCsvX,
                        selectedY = selectedCsvY,
                        message = csvMessage,
                        onSource = { csvSource = it },
                        onToggleHeader = {
                            csvHasHeader = !csvHasHeader
                            importedTable = null
                            csvMessage = "Header setting changed. Read the data again."
                        },
                        onChooseFile = {
                            csvPicker.launch(arrayOf("text/csv", "text/tab-separated-values", "text/plain", "application/vnd.ms-excel"))
                        },
                        onParse = { readCsv(csvSource, "Pasted data") },
                        onSelectX = { selectedCsvX = it },
                        onSelectY = { selectedCsvY = it },
                        onPlot = {
                            val table = importedTable
                            val x = selectedCsvX
                            val y = selectedCsvY
                            if (table != null && x != null && y != null) {
                                val points = runCatching { dataTableEngine.series(table, x, y) }.getOrDefault(emptyList())
                                dataText = points.joinToString("; ") { "${trim(it.x)},${trim(it.y)}" }
                                csvMessage = "Plotted ${points.size} complete rows: $x vs $y"
                                graphTool = GraphTool.Data
                            }
                        },
                    )
                }
            }
            if (graphTool == GraphTool.Probability) {
                Text("Normal PDF at x=${trim(traceX.toDouble())}: ${trim(ProbabilityEngine.normalPdf(traceX.toDouble()))}", color = Cyan)
                Text("Binomial P(X=3), n=10, p=.5: ${trim(ProbabilityEngine.binomialPmf(3, 10, .5))}", color = Violet)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                TogglePill("Compare", comparisonMode) { comparisonMode = it }
                TogglePill("Split compare", splitComparison) { splitComparison = it; comparisonMode = it || comparisonMode }
                TogglePill("Residual plot", showResidualPlot) { showResidualPlot = it }
                TogglePill("Parameter handles", parameterHandleEnabled) { parameterHandleEnabled = it }
                if (pinnedTracePoints.isNotEmpty()) GlowButton("Clear ${pinnedTracePoints.size} pins") { pinnedTracePoints = emptyList() }
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
            if (comparisonMode) {
                val pair = visibleFunctions.take(2)
                if (pair.size == 2) {
                    val items = pair.mapIndexed { index, function ->
                        ComparisonItem(
                            id = function.id,
                            title = function.name,
                            primary = function.expression,
                            attributes = listOf(
                                ComparisonAttribute("Definition", graph.definitionKind(function.expression).name),
                                ComparisonAttribute("Layer", (index + 1).toString()),
                                ComparisonAttribute("Visible", if (function.visible) "Yes" else "No"),
                                ComparisonAttribute("Color", function.colorKey),
                            ),
                        )
                    }
                    SideBySideComparePanel(CompareModeEngine.compare(items[0], items[1]))
                } else {
                    Text("Add or show a second equation to compare.", color = Amber, fontSize = 11.sp)
                }
            }
            if (selectedFunction != null) AxisSlider("Transform amount", graphTransformAmount, -2f..2f) { graphTransformAmount = it }
            brushInterval?.let { interval ->
                val area = primaryExpression?.let { expression -> runCatching { GraphDirectManipulationEngine.signedArea(engine.compile(stripEquation(expression)), interval.start, interval.endInclusive) }.getOrNull() }
                Insight("Brushed interval", "${trim(interval.start)} to ${trim(interval.endInclusive)} · signed area ${area?.let(::trim) ?: "undefined"}", Cyan)
            }
            latestSketchFit?.let { fit -> Insight("Sketch fit", "${fit.expression} · R² ${trim(fit.rSquared)}", Green) }
            if (graphSnapshots.isNotEmpty()) {
                Text("Saved graph states", color = Ink, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { graphSnapshots.forEach { snapshot -> GlowButton(if (graphSnapshotOverlay == snapshot) "Overlay on: ${snapshot.name}" else snapshot.name, onClick = { graphSnapshotOverlay = if (graphSnapshotOverlay == snapshot) null else snapshot }) } }
                if (graphSnapshotOverlay != null) AxisSlider("Overlay opacity", snapshotOpacity, .1f..1f) { snapshotOpacity = it }
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
                GlowButton("Share graph") {
                    (context as? Activity)?.let { activity ->
                        graphScope.launch {
                            runCatching { MathFileExchange.shareProject(activity, vm.state) }
                                .onFailure { vm.reportStatus("Share failed: ${it.message}") }
                        }
                    }
                }
                GlowButton("Export image") {
                    (context as? Activity)?.let { activity ->
                        graphScope.launch {
                            runCatching { MathFileExchange.sharePng(activity, vm.state) }
                                .onFailure { vm.reportStatus("Image export failed: ${it.message}") }
                        }
                    }
                }
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
                IntentAwareMathField(
                    value = row.expression, onValueChange = onExpressionChange, label = "Expression",
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Expression row ${row.name}" },
                    placeholder = "y=sin(x)", showLegend = selected,
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
            DestructiveGlowButton("Delete", onClick = onDelete)
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
    onValueChangeFinished: () -> Unit,
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
            onValueChangeFinished = onValueChangeFinished,
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

internal fun graphColor(key: String): Color = when (key) {
    "violet" -> WorkspaceVisualStyles.ReferenceViolet
    "green" -> WorkspaceVisualStyles.ReferenceBlue
    "amber" -> WorkspaceVisualStyles.ReferenceYellow
    "magenta" -> WorkspaceVisualStyles.ReferenceMagenta
    "coral" -> WorkspaceVisualStyles.ReferenceCoral
    "orange" -> WorkspaceVisualStyles.ReferenceOrange
    else -> WorkspaceVisualStyles.ReferenceCyan
}

private fun parseDataPoints(source: String): List<Vec2> = source
    .split(';', '\n')
    .mapNotNull { entry ->
        val values = entry.trim().split(',').mapNotNull { it.trim().toDoubleOrNull() }
        if (values.size >= 2) Vec2(values[0], values[1]) else null
    }

internal fun SpatialVisualMode.displayLabel() = when (this) {
    SpatialVisualMode.Solid -> "Solid"
    SpatialVisualMode.XRay -> "Transparent"
    SpatialVisualMode.Wireframe -> "Wireframe"
}

internal fun SpatialVisualMode.displayIcon() = when (this) {
    SpatialVisualMode.Solid -> "◉"
    SpatialVisualMode.XRay -> "◐"
    SpatialVisualMode.Wireframe -> "◇"
}

internal fun SpatialVisualMode.nextDisplayMode() = when (this) {
    SpatialVisualMode.Solid -> SpatialVisualMode.XRay
    SpatialVisualMode.XRay -> SpatialVisualMode.Wireframe
    SpatialVisualMode.Wireframe -> SpatialVisualMode.Solid
}
