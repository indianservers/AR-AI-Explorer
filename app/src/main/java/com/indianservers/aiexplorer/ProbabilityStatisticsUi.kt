package com.indianservers.aiexplorer

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
import com.indianservers.aiexplorer.features.probabilitystatistics.presentation.ProbabilityStatisticsScreen
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
internal fun ProbabilityLabScreen(vm: ExplorerViewModel, wide: Boolean) {
    var showLearningUniverse by rememberSaveable { mutableStateOf(true) }
    var showPhase1 by rememberSaveable(vm.requestedProbabilitySection) {
        mutableStateOf(vm.requestedProbabilitySection == ProbabilityLabSection.Distributions.ordinal)
    }
    var modernPhase by rememberSaveable { mutableIntStateOf(1) }
    var section by remember(vm.requestedProbabilitySection) { mutableStateOf(ProbabilityLabSection.entries.getOrElse(vm.requestedProbabilitySection) { ProbabilityLabSection.Distributions }) }
    var kind by remember { mutableStateOf(DistributionKind.Normal) }
    var first by remember { mutableFloatStateOf(0f) }
    var second by remember { mutableFloatStateOf(1f) }
    var lower by remember { mutableFloatStateOf(-1f) }
    var upper by remember { mutableFloatStateOf(1f) }

    if (showLearningUniverse) {
        ProbabilityStatisticsScreen(
            onExit = vm::returnToMathMenu,
            onOpenClassicLabs = { showLearningUniverse = false },
        )
        return
    }

    if (showPhase1) {
        if (modernPhase == 3) {
            ProbabilityStatisticsPhase3Screen(
                onBackToPhase2 = { modernPhase = 2 },
                onExit = vm::returnToMathMenu,
            )
        } else if (modernPhase == 2) {
            ProbabilityStatisticsPhase2Screen(
                onBackToPhase1 = { modernPhase = 1 },
                onOpenPhase3 = { modernPhase = 3 },
                onExit = vm::returnToMathMenu,
            )
        } else {
            ProbabilityStatisticsPhase1Screen(
                onExit = vm::returnToMathMenu,
                onOpenPhase2 = { modernPhase = 2 },
                onOpenLegacy = { showPhase1 = false },
            )
        }
        return
    }

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
            PanelHeader("Probability & Distributions", vm::returnToMathMenu, Cyan, icon = "σ")
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
            PanelHeader("Spreadsheet & Lists", vm::returnToMathMenu, Cyan, icon = "▦")
            ProbabilitySectionSelector(ProbabilityLabSection.Spreadsheet, onSection)
            Text("Editable CSV grid · A1 formulas · named lists · linked analysis", color = Muted, fontSize = 12.sp)
            OutlinedTextField(value = csv, onValueChange = { csv = it }, label = { Text("CSV data") }, minLines = 7, modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Spreadsheet CSV editor" })
            IntentAwareMathField(value = formula, onValueChange = { formula = it }, label = "Formula in C1", modifier = Modifier.fillMaxWidth(), placeholder = "=A1+B1", showLegend = false)
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
                Canvas(Modifier.fillMaxWidth().height(230.dp).clip(RoundedCornerShape(14.dp)).background(Color(0x6600060D)).appWorkspaceTreatment(14.dp, Cyan, Violet).semantics { contentDescription = "Spreadsheet linked scatter plot" }) {
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
        PanelHeader("Probability Experiments", vm::returnToMathMenu, Cyan, icon = "Dice")
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
            PanelHeader("Interactive Statistics Lab", vm::returnToMathMenu, Cyan, icon = "x̄")
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
            .appWorkspaceTreatment(14.dp, Cyan, Violet)
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
        PanelHeader("Statistics Learning Path · School to PG", vm::returnToMathMenu, Cyan, icon = "∑")
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
            .appWorkspaceTreatment(14.dp, Cyan, Violet)
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
