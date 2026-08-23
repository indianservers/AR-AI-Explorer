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
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.runtime.mutableDoubleStateOf
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import com.indianservers.aiexplorer.adaptive.AdaptiveAppScaffold
import com.indianservers.aiexplorer.adaptive.AdaptiveOverlayEdge
import com.indianservers.aiexplorer.adaptive.LocalAdaptiveDeviceProfile
import com.indianservers.aiexplorer.adaptive.adaptiveFocusGroup
import com.indianservers.aiexplorer.adaptive.adaptiveFocusRing
import com.indianservers.aiexplorer.adaptive.adaptiveDialogWidth
import com.indianservers.aiexplorer.adaptive.rememberAdaptiveDeviceProfile
import com.indianservers.aiexplorer.adaptive.tvRemoteScrollable
import com.indianservers.aiexplorer.ar3dgraph.presentation.AR3DGraphScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import com.indianservers.aiexplorer.core.ExpressionEngine
import com.indianservers.aiexplorer.persistence.DurableMathStore
import com.indianservers.aiexplorer.persistence.LocalReliabilityMonitor
import com.indianservers.aiexplorer.persistence.MathFileExchange
import com.indianservers.aiexplorer.solver.presentation.SolverScreen
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
import com.indianservers.aiexplorer.core.InteractiveParameterEngine
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
import com.indianservers.aiexplorer.core.CompareModeEngine
import com.indianservers.aiexplorer.core.ComparisonAttribute
import com.indianservers.aiexplorer.core.ComparisonItem
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
import com.indianservers.aiexplorer.core.LearnerStepStatus
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
import com.indianservers.aiexplorer.core.GuidedSolution
import com.indianservers.aiexplorer.learningintelligence.ui.LearningIntelligenceFeatureRoot
import com.indianservers.aiexplorer.learnall.MathsHomeSearchKind
import com.indianservers.aiexplorer.learnall.MathsHomeSearchResult
import com.indianservers.aiexplorer.learnall.MathsLearnAllMode
import com.indianservers.aiexplorer.learnall.MathsLearnAllRepository
import com.indianservers.aiexplorer.learnall.OfflineLearningCoachResponse
import com.indianservers.aiexplorer.learnall.MathsLearnAllScreen
import com.indianservers.aiexplorer.mathdictionary.MathDictionaryRepository
import com.indianservers.aiexplorer.mathdictionary.MathDictionaryScreen
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
import com.indianservers.aiexplorer.workspace.AddPoint3DCommand
import com.indianservers.aiexplorer.workspace.DeleteVector3DCommand
import com.indianservers.aiexplorer.workspace.DeletePoint3DCommand
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
import com.indianservers.aiexplorer.workspace.ReplaceWorkspaceCommand
import com.indianservers.aiexplorer.workspace.MoveSolidCommand
import com.indianservers.aiexplorer.workspace.MoveVector3DCommand
import com.indianservers.aiexplorer.workspace.Point3D
import com.indianservers.aiexplorer.workspace.Shape2D
import com.indianservers.aiexplorer.workspace.Shape2DType
import com.indianservers.aiexplorer.workspace.PointDependencyType
import com.indianservers.aiexplorer.workspace.TransformSolidCommand
import com.indianservers.aiexplorer.workspace.TransformVector3DCommand
import com.indianservers.aiexplorer.workspace.TransformPoint3DCommand
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
import com.indianservers.aiexplorer.workspace.Unified2DMathController
import com.indianservers.aiexplorer.workspace.Unified2DMutation
import com.indianservers.aiexplorer.workspace.UnifiedSpatialMathController
import com.indianservers.aiexplorer.workspace.UnifiedSpatialMutation
import com.indianservers.aiexplorer.workspace.recomputed
import com.indianservers.aiexplorer.workspace.resolvePointDependency
import com.indianservers.aiexplorer.spatial.ARScaleMode
import com.indianservers.aiexplorer.spatial.ARAvailability
import com.indianservers.aiexplorer.spatial.ARCapabilities
import com.indianservers.aiexplorer.arengine.arcore.ArCoreRuntime
import com.indianservers.aiexplorer.arengine.contract.ArVector2
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

enum class PanelSlot { Left, Right, Bottom, Chrome }
enum class GeometryTool {
    Select, Point, Midpoint, PointOnObject, Intersection, Tangent, Centroid, Circumcenter, Incenter, Orthocenter,
    Line, Segment, Ray, Vector, Parallel, Perpendicular, AngleBisector,
    Triangle, Polygon, RegularPolygon, Rectangle, Square, Circle, CircleThreePoints, Arc, Ellipse, Measure,
}
enum class GraphTool { Plot, Trace, Tangent, Normal, Derivative, Integral, AreaBetween, BrushArea, SketchFit, Intersections, Extrema, Table, Data, Probability, ComplexPlane }
internal enum class ProbabilityLabSection { Distributions, Statistics, Spreadsheet, Experiments, Learn }
enum class KnowledgeSection(val title: String) { Formulas("Formulas"), Mcqs("MCQs"), Visualize("Visualize Formulas"), Theorems("Theorems"), Proofs("Visual Proofs"), Dictionary("Maths Dictionary") }
internal enum class StatisticsChartType(val label: String) { Histogram("Histogram"), BoxPlot("Box Plot"), DotPlot("Dot Plot"), Ecdf("ECDF"), NormalQq("Normal Q-Q") }
enum class SurfaceTool { Surface, Wireframe, Contours, Slice, Gradient, BoundingBox, Trace }
enum class SurfaceViewPreset { Isometric, X, Y, Z, XY, XZ, YZ }
enum class Transform3DMode { Move, Rotate, Scale }
enum class Selection3DMode { Object, Vertex, Edge, Face }
enum class CameraProjection { Perspective, Orthographic }
internal data class SubObjectSelection(val solidIndex: Int, val mode: Selection3DMode, val index: Int)

data class AppSettings(
    val haptics: Boolean = true,
    val snap: Boolean = true,
    val highContrast: Boolean = false,
    val reducedMotion: Boolean = false,
    val spokenMath: Boolean = false,
    val graphSonification: Boolean = false,
    val largeTouchTargets: Boolean = false,
    val decimalPrecision: Int = 2,
    val colorScheme: AppColorScheme = AppColorScheme.Modern,
    val learnerName: String = "",
    val learnerClass: String = "",
    val learnerStandard: String = "",
    val learningComfort: LearningComfort = LearningComfort.Balanced,
)

enum class LearningComfort(val label: String, val description: String) {
    Gentle("Gentle", "More examples, slower pace and simpler words"),
    Balanced("Balanced", "Normal pace with clear examples"),
    Advanced("Advanced", "Deeper ideas, proofs and challenge prompts"),
}

internal data class MathWorkspaceOption(val title: String, val description: String, val icon: String)

private data class AppIntentSnapshot(
    val module: MathModule,
    val showSubjectHub: Boolean,
    val showPhysicsHub: Boolean,
    val showChemistryHub: Boolean,
    val showBiologyHub: Boolean,
    val showLearningIntelligence: Boolean,
    val showMathLanding: Boolean,
    val showShapesExplorer: Boolean,
    val shapeExplorerScene: Boolean,
    val showProblemSolver: Boolean,
    val showSolver: Boolean,
    val showScientificCalculator: Boolean,
    val showSetLogicVisualizer: Boolean,
    val showMathNotebook: Boolean,
    val showUnifiedMathStudio: Boolean,
    val showAdaptiveMathLearning: Boolean,
    val showMathsLearnAll: Boolean,
    val showGamifyMaths: Boolean,
    val showProbabilityLab: Boolean,
    val requestedProbabilitySection: Int,
    val showKnowledgeHub: Boolean,
    val showMathDictionary: Boolean,
    val activeKnowledgeSection: KnowledgeSection,
    val showConceptLibrary: Boolean,
    val selectedMathConcept: String?,
    val selectedMathSubConcept: String?,
    val selectedMathLessonId: String?,
    val showChrome: Boolean,
    val status: String,
)

internal val MathCreationTools = listOf(
    MathWorkspaceOption("Unified Math Studio", "Linked algebra, graph, table, geometry and solver views", "Live"),
    MathWorkspaceOption("Explore Workspaces", "2D, 3D, graphing, trigonometry and spatial AR", "W"),
    MathWorkspaceOption("Scientific Calculator", "Scientific keypad, constants and conversions", "Sci"),
    MathWorkspaceOption("Math Notebook", "Named values, linked functions and reusable exact results", "#"),
    MathWorkspaceOption("Solver", "Offline arithmetic and algebra with verified, traceable steps", "Solve"),
    MathWorkspaceOption("Problem Solver", "Explainable, step-by-step answers with verification", "Fx"),
    MathWorkspaceOption("Math Camera", "Scan printed or handwritten maths into the verified solver", "Scan"),
    MathWorkspaceOption("2D Geometry", "Construct points, lines, circles, polygons and constraints", "2D"),
    MathWorkspaceOption("3D Geometry", "Explore solids, vectors, sections and measurements", "3D"),
    MathWorkspaceOption("Graphs Explorer", "Plot and investigate 2D functions, curves and inequalities", "Graph"),
    MathWorkspaceOption("Shapes Explorer", "Construct and investigate interactive 2D shapes", "2D"),
    MathWorkspaceOption("Manipulatives", "Algebra tiles, fractions, balance and tactile labs", "Tiles"),
    MathWorkspaceOption("Probability & Statistics", "Distributions, intervals and probability plots", "Stat"),
)

internal val MathLearningTools = listOf(
    MathWorkspaceOption("Learn All", "SQLite-backed Class 1 to PG Maths lessons and examples", "All"),
    MathWorkspaceOption("Adaptive Math Coach", "Workspace-aware Socratic practice, proof checks and misconception repair", "AI"),
    MathWorkspaceOption("GamifyMaths", "Interactive maths worlds with drag-and-drop missions, reasoning and mastery", "PLAY"),
    MathWorkspaceOption("Formulas", "Searchable formula reference", "F"),
    MathWorkspaceOption("Visual Proofs", "Manipulable visual demonstrations", "Proof"),
    MathWorkspaceOption("Theorems", "Statements, conditions and applications", "Thm"),
    MathWorkspaceOption("Dictionary", "600+ maths words with meanings, examples and related terms", "A-Z"),
    MathWorkspaceOption("Math Concepts", "Browse the same SQLite Maths lessons by category, topic and subtopic", "All"),
    MathWorkspaceOption("Visual Dictionary", "Terms, notation, diagrams and examples", "A-Z"),
    MathWorkspaceOption("MCQs", "Practice questions with explanations", "?"),
    MathWorkspaceOption("Formula Visualizer", "Turn formulas into interactive scenes", "View"),
    MathWorkspaceOption("Set Theory & Logic", "Interactive Venn diagrams, identities and truth tables", "∪∧"),
)

private data class MathLearningCategory(
    val title: String,
    val description: String,
    val icon: String,
    val toolTitles: List<String>,
)

private val MathLearningCategories = listOf(
    MathLearningCategory("Guided Practice", "Coaching, quizzes and practice", "GO", listOf("Learn All", "Adaptive Math Coach", "MCQs")),
    MathLearningCategory("Formula Lab", "Reference and visual formulas", "Fx", listOf("Formulas", "Formula Visualizer")),
    MathLearningCategory("Proofs & Theorems", "See why mathematics works", "QED", listOf("Visual Proofs", "Theorems")),
    MathLearningCategory("Concept Library", "Browse lesson categories and maths terms", "All", listOf("Math Concepts", "Dictionary", "Visual Dictionary")),
    MathLearningCategory("Logic & Sets", "Venn diagrams and reasoning", "AND", listOf("Set Theory & Logic")),
)

private data class MathHomeCategory(
    val title: String,
    val description: String,
    val icon: String,
    val toolTitles: List<String>,
)

private val MathHomeCategories = listOf(
    MathHomeCategory("GamifyMaths", "Games, speed challenges and mastery worlds", "PLAY", listOf("GamifyMaths")),
    MathHomeCategory("Solve & Calculate", "Calculator, solver, notebook and linked studio", "Fx", listOf("Unified Math Studio", "Scientific Calculator", "Solver", "Problem Solver", "Math Camera", "Math Notebook")),
    MathHomeCategory(
        "Visual Workspaces",
        "Geometry, graphs, tiles and spatial exploration",
        "3D",
        listOf("2D Geometry", "3D Geometry", "Shapes Explorer", "Graphs Explorer", "Explore Workspaces", "Manipulatives"),
    ),
    MathHomeCategory("Data & Probability", "Statistics, distributions and probability labs", "STAT", listOf("Probability & Statistics")),
    MathHomeCategory("Learn & Practise", "Coaching, concepts and explained questions", "GO", listOf("Learn All", "Adaptive Math Coach", "Math Concepts", "Dictionary", "MCQs")),
    MathHomeCategory("Formulas & Proofs", "Formula tools, theorems and visual proofs", "QED", listOf("Formulas", "Formula Visualizer", "Visual Proofs", "Theorems")),
    MathHomeCategory("Reference & Logic", "Dictionary, notation, sets and logical reasoning", "A-Z", listOf("Dictionary", "Visual Dictionary", "Set Theory & Logic")),
    MathHomeCategory(
        "Discover More",
        "Challenges, paths, exams, history and real-world maths",
        "MORE",
        listOf("Daily Challenge", "Practice Paths", "Math Games", "Exam Prep", "Real-World Math", "Math History"),
    ),
)

internal val SuggestedMathTools = listOf(
    MathWorkspaceOption("Daily Challenge", "A fresh problem and guided solution every day", "Day"),
    MathWorkspaceOption("Practice Paths", "Adaptive practice from foundations to mastery", "Path"),
    MathWorkspaceOption("Math Games", "Puzzles, patterns and strategy challenges", "Game"),
    MathWorkspaceOption("Exam Prep", "Timed topic tests and revision plans", "Test"),
    MathWorkspaceOption("Real-World Math", "Model money, motion, data and everyday decisions", "Life"),
    MathWorkspaceOption("Math History", "Discover ideas through mathematicians and milestones", "Time"),
)

private data class PointGesture(
    val indices: List<Int>,
    val from: List<Vec2>,
    val geometryBefore: Geometry2DSnapshot? = null,
)

private data class SolidGesture(val index: Int, val from: Solid)
private data class SolidGroupGesture(val indices: Set<Int>, val from: List<Solid>)
private data class VectorGesture(val index: Int, val from: Vector3D)
private data class Point3DGesture(val index: Int, val from: Point3D)
data class SavedWorkspace(
    val id: String,
    val name: String,
    val module: MathModule,
    val snapshot: WorkspaceState,
    val json: String,
    val updatedAt: Long,
)

private val LearningActivities = LearningCatalog.lessons

class MainActivity : ComponentActivity() {
    private lateinit var reliabilityMonitor: LocalReliabilityMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        reliabilityMonitor = LocalReliabilityMonitor(this)
        val graphVerificationMode = intent.getStringExtra("verify_graph_mode")
        val graphVerificationExpression = intent.getStringExtra("verify_graph_expression_b64")?.let { encoded ->
            runCatching {
                val padded = encoded + "=".repeat((4 - encoded.length % 4) % 4)
                String(
                    android.util.Base64.decode(
                        padded,
                        android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP,
                    ),
                    Charsets.UTF_8,
                )
            }.getOrNull()
        } ?: intent.getStringExtra("verify_graph_expression")
        setContent {
            val vm: ExplorerViewModel = viewModel()
            LaunchedEffect(graphVerificationMode, graphVerificationExpression) {
                if (!graphVerificationMode.isNullOrBlank() && !graphVerificationExpression.isNullOrBlank()) {
                    vm.openGraphVerification(graphVerificationMode, graphVerificationExpression)
                }
            }
            AIExplorerApp(vm)
        }
    }

    override fun onStart() {
        super.onStart()
        reliabilityMonitor.beginSession()
    }

    override fun onStop() {
        reliabilityMonitor.endSession()
        super.onStop()
    }
}

class ExplorerViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
    private val workspaceHistories = com.indianservers.aiexplorer.workspace.WorkspaceHistoryByModule()
    private val history: CommandHistory get() = workspaceHistories.forModule(state.module)
    private val notebookEngine = MathNotebookEngine()
    private val linkedMathKernel = LinkedMathKernel()
    private val mathObjectGraph = MathObjectGraph()
    private val universalDocumentEngine = UniversalMathDocumentEngine()
    private val unified2DController = Unified2DMathController()
    private val unifiedSpatialController = UnifiedSpatialMathController()
    private val trustedMathKernel = TrustedMathKernel()
    private val learningQueue = OfflineLearningQueue()
    private var pointGesture: PointGesture? = null
    private var solidGesture: SolidGesture? = null
    private var solidGroupGesture: SolidGroupGesture? = null
    private var vectorGesture: VectorGesture? = null
    private var point3DGesture: Point3DGesture? = null
    private var spatialGestureFrom: com.indianservers.aiexplorer.spatial.SpatialScenePlacement? = null
    var state by mutableStateOf(WorkspaceState())
        private set
    var selectedPoint by mutableIntStateOf(-1)
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

    fun updateStatus(message: String) {
        status = message
    }
    var showLearningPanel by mutableStateOf(false)
        private set
    var showSubjectHub by mutableStateOf(false)
        private set
    var showPhysicsHub by mutableStateOf(false)
        private set
    var showChemistryHub by mutableStateOf(false)
        private set
    var showBiologyHub by mutableStateOf(false)
        private set
    var showLearningIntelligence by mutableStateOf(false)
        private set
    var showMathLanding by mutableStateOf(true)
        private set
    var showMathMenu by mutableStateOf(false)
        private set
    var showShapesExplorer by mutableStateOf(false)
        private set
    var shapeExplorerScene by mutableStateOf(false)
        private set
    var showProblemSolver by mutableStateOf(false)
        private set
    var showSolver by mutableStateOf(false)
        private set
    var solverCameraRequested by mutableStateOf(false)
        private set
    var showScientificCalculator by mutableStateOf(false)
        private set
    var showSetLogicVisualizer by mutableStateOf(false)
        private set
    var showMathNotebook by mutableStateOf(false)
        private set
    var showUnifiedMathStudio by mutableStateOf(false)
        private set
    var showAdaptiveMathLearning by mutableStateOf(false)
        private set
    var showMathsLearnAll by mutableStateOf(false)
        private set
    var mathsLearnAllMode by mutableStateOf(MathsLearnAllMode.Concepts)
        private set
    var showGamifyMaths by mutableStateOf(false)
        private set
    var showProbabilityLab by mutableStateOf(false)
        private set
    var requestedProbabilitySection by mutableIntStateOf(0)
        private set
    var showKnowledgeHub by mutableStateOf(false)
        private set
    var showMathDictionary by mutableStateOf(false)
        private set
    var activeKnowledgeSection by mutableStateOf(KnowledgeSection.Formulas)
        private set
    var showConceptLibrary by mutableStateOf(false)
        private set
    var selectedMathConcept by mutableStateOf<String?>(null)
        private set
    var selectedMathSubConcept by mutableStateOf<String?>(null)
        private set
    var selectedMathLessonId by mutableStateOf<String?>(null)
        private set
    var savedSetTheoryWorkspaces by mutableStateOf(savedStateHandle.get<ArrayList<String>>("savedSetTheoryWorkspaces").orEmpty().toList())
        private set
    var completedVisualFormulaIds by mutableStateOf(savedStateHandle.get<ArrayList<String>>("completedVisualFormulaIds").orEmpty().toSet())
        private set
    var favouriteVisualFormulaIds by mutableStateOf(savedStateHandle.get<ArrayList<String>>("favouriteVisualFormulaIds").orEmpty().toSet())
        private set
    var pinnedMathTools by mutableStateOf(savedStateHandle.get<ArrayList<String>>("pinnedMathTools").orEmpty().toSet())
        private set
    var recentMathTools by mutableStateOf(savedStateHandle.get<ArrayList<String>>("recentMathTools").orEmpty().toList())
        private set
    var mathToolUseCounts by mutableStateOf(
        savedStateHandle.get<ArrayList<String>>("mathToolUseCounts").orEmpty().mapNotNull { encoded ->
            val split = encoded.lastIndexOf('=')
            if (split <= 0) null else encoded.substring(0, split) to (encoded.substring(split + 1).toIntOrNull() ?: 0)
        }.toMap(),
    )
        private set
    var favoriteShapeKeys by mutableStateOf<Set<String>>(emptySet())
        private set
    var recentShapeKeys by mutableStateOf<List<String>>(emptyList())
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
    var workspaceClearEpoch by mutableIntStateOf(0)
        private set
    var selectedSolid by mutableIntStateOf(-1)
        private set
    var selectedVector3D by mutableIntStateOf(-1)
        private set
    var selectedPoint3D by mutableIntStateOf(-1)
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
    val unifiedSpatialSnapshot
        get() = unifiedSpatialController.snapshot(state)
    fun mathObjectGraphSnapshot(
        parameterValues: Map<String, Double>,
        tableInputs: List<Double> = (-4..4).map { it.toDouble() },
    ) = mathObjectGraph.snapshot(state, parameterValues, tableInputs)
    val universalMathDocument: UniversalMathDocument
        get() {
            val base = UniversalWorkspaceBridge.fromWorkspace(state)
            val notebookObjects = UniversalMathObjectFactory.fromNotebook(notebookDocument)
            val probability = UniversalMathObjectFactory.probability("probability-normal", "Normal distribution", "Normal", mapOf("mean" to 0.0, "sd" to 1.0))
            return base.copy(objects = base.objects + notebookObjects.associateBy { it.id } + (probability.id to probability), revision = maxOf(base.revision, notebookDocument.revision.toLong()))
        }
    fun verifyMathEquivalence(left: String, right: String): EquivalenceEvidence = trustedMathKernel.equivalence(left, right)

	    val constructionProtocol: List<String> get() = history.protocol

    private var intentBackStack = mutableListOf<AppIntentSnapshot>()
    private var restoringIntent = false

    private fun currentIntentSnapshot() = AppIntentSnapshot(
        module = state.module,
        showSubjectHub = showSubjectHub,
        showPhysicsHub = showPhysicsHub,
        showChemistryHub = showChemistryHub,
        showBiologyHub = showBiologyHub,
        showLearningIntelligence = showLearningIntelligence,
        showMathLanding = showMathLanding,
        showShapesExplorer = showShapesExplorer,
        shapeExplorerScene = shapeExplorerScene,
        showProblemSolver = showProblemSolver,
        showSolver = showSolver,
        showScientificCalculator = showScientificCalculator,
        showSetLogicVisualizer = showSetLogicVisualizer,
        showMathNotebook = showMathNotebook,
        showUnifiedMathStudio = showUnifiedMathStudio,
        showAdaptiveMathLearning = showAdaptiveMathLearning,
        showMathsLearnAll = showMathsLearnAll,
        showGamifyMaths = showGamifyMaths,
        showProbabilityLab = showProbabilityLab,
        requestedProbabilitySection = requestedProbabilitySection,
        showKnowledgeHub = showKnowledgeHub,
        showMathDictionary = showMathDictionary,
        activeKnowledgeSection = activeKnowledgeSection,
        showConceptLibrary = showConceptLibrary,
        selectedMathConcept = selectedMathConcept,
        selectedMathSubConcept = selectedMathSubConcept,
        selectedMathLessonId = selectedMathLessonId,
        showChrome = showChrome,
        status = status,
    )

    private fun rememberCurrentIntent() {
        if (restoringIntent) return
        val snapshot = currentIntentSnapshot()
        if (intentBackStack.lastOrNull() != snapshot) {
            intentBackStack = (intentBackStack + snapshot).takeLast(25).toMutableList()
        }
    }

    private fun restoreIntent(snapshot: AppIntentSnapshot) {
        restoringIntent = true
        state = state.copy(module = snapshot.module)
        showSubjectHub = snapshot.showSubjectHub
        showPhysicsHub = snapshot.showPhysicsHub
        showChemistryHub = snapshot.showChemistryHub
        showBiologyHub = snapshot.showBiologyHub
        showLearningIntelligence = snapshot.showLearningIntelligence
        showMathLanding = snapshot.showMathLanding
        showShapesExplorer = snapshot.showShapesExplorer
        shapeExplorerScene = snapshot.shapeExplorerScene
        showProblemSolver = snapshot.showProblemSolver
        showSolver = snapshot.showSolver
        showScientificCalculator = snapshot.showScientificCalculator
        showSetLogicVisualizer = snapshot.showSetLogicVisualizer
        showMathNotebook = snapshot.showMathNotebook
        showUnifiedMathStudio = snapshot.showUnifiedMathStudio
        showAdaptiveMathLearning = snapshot.showAdaptiveMathLearning
        showMathsLearnAll = snapshot.showMathsLearnAll
        showGamifyMaths = snapshot.showGamifyMaths
        showProbabilityLab = snapshot.showProbabilityLab
        requestedProbabilitySection = snapshot.requestedProbabilitySection
        showKnowledgeHub = snapshot.showKnowledgeHub
        showMathDictionary = snapshot.showMathDictionary
        activeKnowledgeSection = snapshot.activeKnowledgeSection
        showConceptLibrary = snapshot.showConceptLibrary
        selectedMathConcept = snapshot.selectedMathConcept
        selectedMathSubConcept = snapshot.selectedMathSubConcept
        selectedMathLessonId = snapshot.selectedMathLessonId
        showChrome = snapshot.showChrome
        showActionDock = false
        hidePanels()
        status = snapshot.status
        restoringIntent = false
    }

    private fun withoutRecordingIntent(block: () -> Unit) {
        restoringIntent = true
        try {
            block()
        } finally {
            restoringIntent = false
        }
    }

    private fun restorePreviousIntent(): Boolean {
        val previous = intentBackStack.lastOrNull() ?: return false
        intentBackStack = intentBackStack.dropLast(1).toMutableList()
        restoreIntent(previous)
        return true
    }

    fun open(module: MathModule) {
        rememberCurrentIntent()
        state = state.copy(module = module)
        showSubjectHub = false
        showMathLanding = false
        showShapesExplorer = false
        shapeExplorerScene = false
        showProblemSolver = false
        showSolver = false
        showScientificCalculator = false
        showSetLogicVisualizer = false
        showMathNotebook = false
        showUnifiedMathStudio = false
        showAdaptiveMathLearning = false
        showGamifyMaths = false
        showProbabilityLab = false
        showKnowledgeHub = false
        showMathDictionary = false
        showConceptLibrary = false
        selectedMathConcept = null
        selectedMathSubConcept = null
        showMathMenu = false
        hidePanels()
    }

    fun openGraphVerification(mode: String, expression: String) {
        withoutRecordingIntent {
            hidePanels()
            showSubjectHub = false
            showMathLanding = false
            showShapesExplorer = false
            shapeExplorerScene = false
            showProblemSolver = false
            showSolver = false
            showScientificCalculator = false
            showSetLogicVisualizer = false
            showMathNotebook = false
            showUnifiedMathStudio = false
            showAdaptiveMathLearning = false
            showGamifyMaths = false
            showProbabilityLab = false
            showKnowledgeHub = false
            showMathDictionary = false
            showConceptLibrary = false
            selectedMathConcept = null
            selectedMathSubConcept = null
            showMathMenu = false
            showChrome = true
            when (mode.lowercase()) {
                "2d", "graph", "graph2d" -> {
                    state = state.copy(
                        module = MathModule.Graph2D,
                        functions = listOf(
                            com.indianservers.aiexplorer.core.FunctionDefinition(
                                id = "verify-2d",
                                name = "f(x)",
                                expression = expression,
                                colorKey = "cyan",
                                visible = true,
                            ),
                        ),
                    )
                    status = "Verifying 2D graph: $expression"
                }
                "3d", "graph3d" -> {
                    state = state.copy(
                        module = MathModule.Graph3D,
                        surfaceExpression = expression,
                        surfaceLayers = listOf(com.indianservers.aiexplorer.core.SpatialSurfaceLayer("surface-main", expression)),
                    )
                    status = "Verifying 3D graph: $expression"
                }
            }
        }
    }

	    fun enterMaths() {
        rememberCurrentIntent()
	        returnToMathMenu()
	        status = "Mathematics Menu"
	    }

	    fun returnToMathMenu() {
        rememberCurrentIntent()
	        showSubjectHub = false
        showPhysicsHub = false
        showChemistryHub = false
        showBiologyHub = false
        showMathLanding = true
        showProblemSolver = false
        showSolver = false
        showScientificCalculator = false
        showSetLogicVisualizer = false
        showMathNotebook = false
        showUnifiedMathStudio = false
        showAdaptiveMathLearning = false
        showGamifyMaths = false
        showProbabilityLab = false
        showKnowledgeHub = false
        showMathDictionary = false
        showConceptLibrary = false
        selectedMathConcept = null
        selectedMathSubConcept = null
        showShapesExplorer = false
        shapeExplorerScene = false
        showMathMenu = false
        showChrome = true
        showActionDock = false
        hidePanels()
        status = "Mathematics Menu"
    }

	    fun openShapesExplorer() {
        rememberCurrentIntent()
	        showSubjectHub = false
        showMathLanding = false
        showProblemSolver = false
        showScientificCalculator = false
        showMathNotebook = false
        showProbabilityLab = false
        showKnowledgeHub = false
        showMathDictionary = false
        showMathMenu = false
        showShapesExplorer = true
        shapeExplorerScene = false
        showActionDock = false
        hidePanels()
        status = "Shapes Explorer"
    }

	    fun loadExplorerShape2D(id: String) {
	        val preset = ShapeExplorer2DShapes.firstOrNull { it.id == id } ?: return
        rememberCurrentIntent()
	        state = state.copy(
            name = "${preset.label} Explorer",
            module = MathModule.Geometry2D,
            points = preset.points,
            shapes = listOf(Shape2D("shape-explorer-${preset.id}", preset.type, preset.points.indices.toList(), preset.label)),
            pointDependencies = emptyList(),
            modifiedAt = System.currentTimeMillis(),
        )
        selectedPoint = preset.points.lastIndex
        selectedShape = 0
        selectedShapes = setOf(0)
        showShapesExplorer = false
        shapeExplorerScene = true
        showChrome = true
        rememberShape("2d:$id")
        status = "${preset.label} loaded - drag its handles to resize"
    }

	    fun loadExplorerShape3D(type: SolidType) {
        rememberCurrentIntent()
	        state = state.copy(
            name = "${type.name} Explorer",
            module = MathModule.Geometry3D,
            solids = listOf(defaultSolid(type)),
            vectors3D = emptyList(),
            points3D = emptyList(),
            modifiedAt = System.currentTimeMillis(),
        )
        selectedSolid = 0
        selectedPoint3D = -1
        selectedVector3D = -1
        showShapesExplorer = false
        shapeExplorerScene = true
        showChrome = true
        rememberShape("3d:${type.name}")
        status = "${type.name} loaded - pinch or drag in Scale mode to resize"
    }

    fun toggleFavoriteShape(key: String) {
        favoriteShapeKeys = if (key in favoriteShapeKeys) favoriteShapeKeys - key else favoriteShapeKeys + key
        status = if (key in favoriteShapeKeys) "Shape added to favourites" else "Shape removed from favourites"
    }

    private fun rememberShape(key: String) {
        recentShapeKeys = (listOf(key) + recentShapeKeys.filterNot { it == key }).take(8)
    }

    fun addExplorerShape2D(id: String) {
        val preset = ShapeExplorer2DShapes.firstOrNull { it.id == id } ?: return
        rememberCurrentIntent()
	        val basePoints = if (state.module == MathModule.Geometry2D) state.points else emptyList()
        val baseShapes = if (state.module == MathModule.Geometry2D) state.shapes else emptyList()
        val offset = Vec2((baseShapes.size % 3) * 1.1, (baseShapes.size % 2) * .8)
        val newPoints = preset.points.map { it + offset }
        val start = basePoints.size
        val shape = Shape2D("shape-explorer-${preset.id}-${System.nanoTime()}", preset.type, newPoints.indices.map { start + it }, preset.label)
        state = state.copy(module = MathModule.Geometry2D, points = basePoints + newPoints, shapes = baseShapes + shape, modifiedAt = System.currentTimeMillis())
        selectedShape = state.shapes.lastIndex
        selectedShapes = setOf(selectedShape)
        selectedPoint = state.points.lastIndex
        showShapesExplorer = false
        shapeExplorerScene = true
        showChrome = true
        rememberShape("2d:$id")
        status = "${preset.label} added to composite scene"
    }

    fun addConstructionShape2D(type: Shape2DType, label: String = type.name) {
        rememberCurrentIntent()
        val basePoints = if (state.module == MathModule.Geometry2D) state.points else emptyList()
        val baseShapes = if (state.module == MathModule.Geometry2D) state.shapes else emptyList()
        val offset = Vec2((baseShapes.size % 4) * .85 - .4, (baseShapes.size % 3) * .55)
        val points = defaultConstructionPoints(type).map { it + offset }
        val start = basePoints.size
        val shape = Shape2D(
            id = "construction-${type.name.lowercase()}-${System.nanoTime()}",
            type = type,
            pointIndices = points.indices.map { start + it },
            name = label,
        )
        state = state.copy(
            module = MathModule.Geometry2D,
            points = basePoints + points,
            shapes = baseShapes + shape,
            pointDependencies = if (state.module == MathModule.Geometry2D) state.pointDependencies else emptyList(),
            geometryConstraints = if (state.module == MathModule.Geometry2D) state.geometryConstraints else emptyList(),
            geometryGroups = if (state.module == MathModule.Geometry2D) state.geometryGroups else emptyList(),
            modifiedAt = System.currentTimeMillis(),
        ).recomputed()
        selectedShape = state.shapes.lastIndex
        selectedShapes = setOf(selectedShape)
        selectedPoint = -1
        showShapesExplorer = false
        shapeExplorerScene = true
        showChrome = true
        rememberShape("2d:${type.name}")
        status = "$label added"
    }

    fun addArShape2D(id: String) {
        val preset = ShapeExplorer2DShapes.firstOrNull { it.id == id } ?: return
        val offset = Vec2((state.shapes.size % 3) * 1.1, (state.shapes.size % 2) * .8)
        val newPoints = preset.points.map { it + offset }
        val start = state.points.size
        val shape = Shape2D("ar-shape-${preset.id}-${System.nanoTime()}", preset.type, newPoints.indices.map { start + it }, preset.label)
        state = state.copy(points = state.points + newPoints, shapes = state.shapes + shape, modifiedAt = System.currentTimeMillis())
        selectedShape = state.shapes.lastIndex
        selectedShapes = setOf(selectedShape)
        selectedPoint = state.points.lastIndex
        rememberShape("2d:$id")
        status = "${preset.label} added to AR"
    }

	    fun addExplorerShape3D(type: SolidType) {
        rememberCurrentIntent()
        val base = if (state.module == MathModule.Geometry3D) state.solids else emptyList()
        val position = Vec3(((base.size % 4) - 1.5) * 2.2, 0.0, (base.size / 4) * 1.8)
        state = state.copy(module = MathModule.Geometry3D, solids = base + defaultSolid(type).copy(position = position), modifiedAt = System.currentTimeMillis())
        selectedSolid = state.solids.lastIndex
        selectedPoint3D = -1
        selectedVector3D = -1
        showShapesExplorer = false
        shapeExplorerScene = true
        showChrome = true
        rememberShape("3d:${type.name}")
        status = "${type.name} added to composite scene"
    }

    fun duplicateExplorerSelection() {
        if (state.module == MathModule.Geometry2D) {
            val source = state.shapes.getOrNull(selectedShape) ?: return
            val sourcePoints = source.pointIndices.mapNotNull { state.points.getOrNull(it) }
            val start = state.points.size
            val copiedPoints = sourcePoints.map { it + Vec2(.7, .7) }
            val copy = source.copy(id = "${source.id}-copy-${System.nanoTime()}", pointIndices = copiedPoints.indices.map { start + it }, name = "${source.name} Copy")
            state = state.copy(points = state.points + copiedPoints, shapes = state.shapes + copy, modifiedAt = System.currentTimeMillis())
            selectedShape = state.shapes.lastIndex
            selectedShapes = setOf(selectedShape)
        } else {
            val source = state.solids.getOrNull(selectedSolid) ?: return
            state = state.copy(solids = state.solids + source.copy(position = source.position + Vec3(.8, 0.0, .8)), modifiedAt = System.currentTimeMillis())
            selectedSolid = state.solids.lastIndex
        }
        status = "Shape duplicated"
    }

    fun scaleExplorerShape2D(factor: Double) {
        val shape = state.shapes.getOrNull(selectedShape) ?: return
        if (shape.locked) { status = "Unlock ${shape.name} before resizing"; return }
        val indices = shape.pointIndices.filter { it in state.points.indices }
        if (indices.isEmpty()) return
        val center = indices.map { state.points[it] }.let { points -> Vec2(points.map { it.x }.average(), points.map { it.y }.average()) }
        val replacements = indices.associateWith { index -> center + (state.points[index] - center) * factor.coerceIn(.2, 5.0) }
        state = state.copy(points = state.points.mapIndexed { index, point -> replacements[index] ?: point }, modifiedAt = System.currentTimeMillis()).recomputed()
        status = "${shape.name} resized"
    }

    fun resizeExplorerShape2D(width: Double, height: Double, keepProportions: Boolean) {
        val shape = state.shapes.getOrNull(selectedShape) ?: return
        if (shape.locked) { status = "Unlock ${shape.name} before resizing"; return }
        val indices = shape.pointIndices.filter { it in state.points.indices }
        if (indices.isEmpty()) return
        val points = indices.map { state.points[it] }
        val minX = points.minOf { it.x }; val maxX = points.maxOf { it.x }
        val minY = points.minOf { it.y }; val maxY = points.maxOf { it.y }
        val currentWidth = (maxX - minX).coerceAtLeast(1e-6); val currentHeight = (maxY - minY).coerceAtLeast(1e-6)
        val sx = (width / currentWidth).coerceIn(.05, 20.0)
        val syRequested = (height / currentHeight).coerceIn(.05, 20.0)
        val sy = if (keepProportions) sx else syRequested
        val center = Vec2((minX + maxX) / 2, (minY + maxY) / 2)
        val replacements = indices.associateWith { index ->
            val delta = state.points[index] - center
            Vec2(center.x + delta.x * sx, center.y + delta.y * sy)
        }
        state = state.copy(points = state.points.mapIndexed { index, point -> replacements[index] ?: point }, modifiedAt = System.currentTimeMillis()).recomputed()
        status = "${shape.name} set to exact dimensions"
    }

    fun rotateExplorerShape2D(degrees: Double) {
        val shape = state.shapes.getOrNull(selectedShape) ?: return
        if (shape.locked) return
        val indices = shape.pointIndices.filter { it in state.points.indices }
        val center = indices.map { state.points[it] }.let { points -> Vec2(points.map { it.x }.average(), points.map { it.y }.average()) }
        val radians = Math.toRadians(degrees)
        val replacements = indices.associateWith { index ->
            val d = state.points[index] - center
            Vec2(center.x + d.x * cos(radians) - d.y * sin(radians), center.y + d.x * sin(radians) + d.y * cos(radians))
        }
        state = state.copy(points = state.points.mapIndexed { index, point -> replacements[index] ?: point }, modifiedAt = System.currentTimeMillis()).recomputed()
        status = "${shape.name} rotated"
    }

    fun renameExplorerSelection(name: String) {
        if (state.module != MathModule.Geometry2D || name.isBlank()) return
        updateSelectedShape { it.copy(name = name.trim()) }
    }

    fun resetExplorerSelection() {
        if (state.module == MathModule.Geometry2D) {
            val shape = state.shapes.getOrNull(selectedShape) ?: return
            val preset = ShapeExplorer2DShapes.firstOrNull { shape.id.contains("shape-explorer-${it.id}") } ?: return
            loadExplorerShape2D(preset.id)
        } else {
            val type = state.solids.getOrNull(selectedSolid)?.type ?: return
            loadExplorerShape3D(type)
        }
        status = "Shape reset"
    }

	    fun openCurrentShapeInAr() {
	        if (state.module != MathModule.Geometry3D || state.solids.isEmpty()) {
	            status = "Choose a 3D solid before opening AR"
	            return
	        }
        rememberCurrentIntent()
	        shapeExplorerScene = false
        showShapesExplorer = false
        state = state.copy(module = MathModule.SpatialAR)
        status = "Shape prepared for AR placement"
    }

	    fun openProblemSolver() {
        rememberCurrentIntent()
	        showSubjectHub = false
        showMathLanding = false
        showShapesExplorer = false
        shapeExplorerScene = false
        showProblemSolver = true
        showSolver = false
        showScientificCalculator = false
        showMathNotebook = false
        showUnifiedMathStudio = false
        showAdaptiveMathLearning = false
        showMathsLearnAll = false
        showGamifyMaths = false
        showProbabilityLab = false
        showKnowledgeHub = false
        showMathDictionary = false
        showMathMenu = false
        showActionDock = false
        hidePanels()
        status = "Explainable Problem Solver"
    }

    fun openSolver() {
        rememberCurrentIntent()
        showSubjectHub = false
        showMathLanding = false
        showShapesExplorer = false
        shapeExplorerScene = false
        showProblemSolver = false
        showSolver = true
        showScientificCalculator = false
        showSetLogicVisualizer = false
        showMathNotebook = false
        showUnifiedMathStudio = false
        showAdaptiveMathLearning = false
        showMathsLearnAll = false
        showGamifyMaths = false
        showProbabilityLab = false
        showKnowledgeHub = false
        showMathDictionary = false
        showConceptLibrary = false
        selectedMathConcept = null
        selectedMathSubConcept = null
        selectedMathLessonId = null
        showMathMenu = false
        showActionDock = false
        hidePanels()
        status = "Offline Solver"
    }

    fun openMathCamera() {
        openProblemSolver()
        solverCameraRequested = true
        status = "Math Camera"
    }

    fun consumeMathCameraRequest() {
        solverCameraRequested = false
    }

	    fun openProbabilityLab() {
        rememberCurrentIntent()
	        showSubjectHub = false
        showMathLanding = false
        showShapesExplorer = false
        shapeExplorerScene = false
        showProblemSolver = false
        showSolver = false
        showScientificCalculator = false
        showMathNotebook = false
        showUnifiedMathStudio = false
        showAdaptiveMathLearning = false
        showMathsLearnAll = false
        showGamifyMaths = false
        showProbabilityLab = true
        showKnowledgeHub = false
        showMathDictionary = false
        showMathMenu = false
        showActionDock = false
        hidePanels()
        status = "Probability & Distributions Lab"
    }

    fun openSpreadsheetLab() {
        requestedProbabilitySection = ProbabilityLabSection.Spreadsheet.ordinal
        openProbabilityLab()
        status = "Spreadsheet & Lists"
    }

	    fun openSubjectHub() {
        returnToMathMenu()
    }

	    fun openLearningIntelligence() {
        rememberCurrentIntent()
	        showSubjectHub = false
        showPhysicsHub = false
        showChemistryHub = false
        showBiologyHub = false
        showLearningIntelligence = true
        showMathLanding = false
        showAdaptiveMathLearning = false
        showMathsLearnAll = false
        showGamifyMaths = false
        showMathMenu = false
        showActionDock = false
        hidePanels()
        status = "Local Learning Intelligence"
    }

	    fun openMathNotebook() {
        rememberCurrentIntent()
	        showSubjectHub = false
        showMathLanding = false
        showShapesExplorer = false
        shapeExplorerScene = false
        showProblemSolver = false
        showSolver = false
        showScientificCalculator = false
        showMathNotebook = true
        showUnifiedMathStudio = false
        showAdaptiveMathLearning = false
        showMathsLearnAll = false
        showGamifyMaths = false
        showProbabilityLab = false
        showKnowledgeHub = false
        showMathDictionary = false
        showMathMenu = false
        showActionDock = false
        hidePanels()
        status = "Unified Math Notebook"
    }

	    fun openUnifiedMathStudio() {
        rememberCurrentIntent()
	        showSubjectHub = false
        showMathLanding = false
        showShapesExplorer = false
        shapeExplorerScene = false
        showProblemSolver = false
        showSolver = false
        showScientificCalculator = false
        showSetLogicVisualizer = false
        showMathNotebook = false
        showProbabilityLab = false
        showKnowledgeHub = false
        showMathDictionary = false
        showUnifiedMathStudio = true
        showAdaptiveMathLearning = false
        showMathsLearnAll = false
        showGamifyMaths = false
        showMathMenu = false
        showActionDock = false
        hidePanels()
        status = "Unified Math Studio"
    }

    fun openAdaptiveMathLearning() {
        rememberCurrentIntent()
	        showSubjectHub = false
        showMathLanding = false
        showShapesExplorer = false
        shapeExplorerScene = false
        showProblemSolver = false
        showSolver = false
        showScientificCalculator = false
        showSetLogicVisualizer = false
        showMathNotebook = false
        showProbabilityLab = false
        showKnowledgeHub = false
        showMathDictionary = false
        showUnifiedMathStudio = false
        showAdaptiveMathLearning = true
        showMathsLearnAll = false
        showGamifyMaths = false
        showMathMenu = false
        showActionDock = false
        hidePanels()
        status = "Adaptive Math Coach"
    }

    fun openMathsLearnAll() {
        rememberCurrentIntent()
        showSubjectHub = false
        showMathLanding = false
        showShapesExplorer = false
        shapeExplorerScene = false
        showProblemSolver = false
        showSolver = false
        showScientificCalculator = false
        showSetLogicVisualizer = false
        showMathNotebook = false
        showUnifiedMathStudio = false
        showAdaptiveMathLearning = false
        mathsLearnAllMode = MathsLearnAllMode.Concepts
        showMathsLearnAll = true
        showGamifyMaths = false
        showProbabilityLab = false
        showKnowledgeHub = false
        showMathDictionary = false
        showConceptLibrary = false
        selectedMathLessonId = null
        showMathMenu = false
        showActionDock = false
        hidePanels()
        status = "Learn All Maths"
    }

    fun openGamifyMaths() {
        rememberCurrentIntent()
        showSubjectHub = false
        showPhysicsHub = false
        showChemistryHub = false
        showBiologyHub = false
        showLearningIntelligence = false
        showMathLanding = false
        showShapesExplorer = false
        shapeExplorerScene = false
        showProblemSolver = false
        showSolver = false
        showScientificCalculator = false
        showSetLogicVisualizer = false
        showMathNotebook = false
        showUnifiedMathStudio = false
        showAdaptiveMathLearning = false
        showMathsLearnAll = false
        showGamifyMaths = true
        showProbabilityLab = false
        showKnowledgeHub = false
        showMathDictionary = false
        showMathMenu = false
        showChrome = false
        showActionDock = false
        hidePanels()
        status = "GamifyMaths"
    }

    fun commitUnifiedStudio(workspace: WorkspaceState) {
        state = workspace
        status = "Linked maths document autosaved"
    }

	    fun openKnowledgeHub(section: KnowledgeSection) {
        rememberCurrentIntent()
	        showSubjectHub = false
        showMathLanding = false
        showShapesExplorer = false
        shapeExplorerScene = false
        showProblemSolver = false
        showSolver = false
        showScientificCalculator = false
        showMathNotebook = false
        showUnifiedMathStudio = false
        showAdaptiveMathLearning = false
        showMathsLearnAll = false
        showGamifyMaths = false
        showProbabilityLab = false
        showKnowledgeHub = true
        showMathDictionary = false
        showConceptLibrary = false
        selectedMathConcept = null
        selectedMathSubConcept = null
        activeKnowledgeSection = section
        showMathMenu = false
        showActionDock = false
        hidePanels()
        status = "Maths ${section.title}"
    }

    fun openMathDictionary() {
        rememberCurrentIntent()
        showSubjectHub = false
        showMathLanding = false
        showShapesExplorer = false
        shapeExplorerScene = false
        showProblemSolver = false
        showSolver = false
        showScientificCalculator = false
        showSetLogicVisualizer = false
        showMathNotebook = false
        showUnifiedMathStudio = false
        showAdaptiveMathLearning = false
        showMathsLearnAll = false
        showGamifyMaths = false
        showProbabilityLab = false
        showKnowledgeHub = false
        showMathDictionary = true
        showConceptLibrary = false
        selectedMathConcept = null
        selectedMathSubConcept = null
        showMathMenu = false
        showActionDock = false
        hidePanels()
        status = "Math Dictionary"
    }

    fun openConceptLibrary(concept: String? = null, subConcept: String? = null) {
        if (concept == "Visual Formulas") {
            openKnowledgeHub(KnowledgeSection.Visualize)
            return
        }
        rememberCurrentIntent()
        showSubjectHub = false
        showMathLanding = false
        showShapesExplorer = false
        shapeExplorerScene = false
        showProblemSolver = false
        showSolver = false
        showScientificCalculator = false
        showSetLogicVisualizer = false
        showMathNotebook = false
        showUnifiedMathStudio = false
        showAdaptiveMathLearning = false
        mathsLearnAllMode = MathsLearnAllMode.ClassExplore
        showMathsLearnAll = true
        showGamifyMaths = false
        showProbabilityLab = false
        showKnowledgeHub = false
        showMathDictionary = false
        showConceptLibrary = false
        selectedMathConcept = concept
        selectedMathSubConcept = subConcept
        selectedMathLessonId = null
        showMathMenu = false
        showActionDock = false
        hidePanels()
        status = subConcept ?: concept?.let { "$it lessons" } ?: "Math Concepts"
    }

    fun selectMathConcept(concept: String?) {
        selectedMathConcept = concept
        selectedMathSubConcept = null
        selectedMathLessonId = null
        status = concept?.let { "$it concepts" } ?: "Math Concepts"
    }

    fun selectMathSubConcept(subConcept: String?) {
        selectedMathSubConcept = subConcept
        selectedMathLessonId = null
        status = if (subConcept == null) {
            selectedMathConcept?.let { "$it concepts" } ?: "Math Concepts"
        } else {
            "$subConcept lesson"
        }
    }

    fun openMathLesson(lessonId: String, title: String) {
        rememberCurrentIntent()
        showSubjectHub = false
        showMathLanding = false
        showShapesExplorer = false
        shapeExplorerScene = false
        showProblemSolver = false
        showSolver = false
        showScientificCalculator = false
        showSetLogicVisualizer = false
        showMathNotebook = false
        showUnifiedMathStudio = false
        showAdaptiveMathLearning = false
        mathsLearnAllMode = MathsLearnAllMode.ClassExplore
        showMathsLearnAll = true
        showGamifyMaths = false
        showProbabilityLab = false
        showKnowledgeHub = false
        showMathDictionary = false
        showConceptLibrary = false
        selectedMathConcept = null
        selectedMathSubConcept = title
        selectedMathLessonId = lessonId
        showMathMenu = false
        showActionDock = false
        hidePanels()
        status = title
    }



    fun saveSetTheoryWorkspace(snapshot: String) {
        savedSetTheoryWorkspaces = (listOf(snapshot) + savedSetTheoryWorkspaces.filterNot { it.substringBefore('|') == snapshot.substringBefore('|') }).take(20)
        savedStateHandle["savedSetTheoryWorkspaces"] = ArrayList(savedSetTheoryWorkspaces)
        status = "Set Theory workspace saved"
    }

    fun deleteSetTheoryWorkspace(snapshot: String) {
        savedSetTheoryWorkspaces = savedSetTheoryWorkspaces - snapshot
        savedStateHandle["savedSetTheoryWorkspaces"] = ArrayList(savedSetTheoryWorkspaces)
        status = "Set Theory workspace deleted"
    }

    fun completeVisualFormula(formulaId: String) {
        completedVisualFormulaIds = completedVisualFormulaIds + formulaId
        savedStateHandle["completedVisualFormulaIds"] = ArrayList(completedVisualFormulaIds)
        status = "Visual formula discovery completed"
    }

    fun toggleVisualFormulaFavourite(formulaId: String) {
        favouriteVisualFormulaIds = if (formulaId in favouriteVisualFormulaIds) favouriteVisualFormulaIds - formulaId else favouriteVisualFormulaIds + formulaId
        savedStateHandle["favouriteVisualFormulaIds"] = ArrayList(favouriteVisualFormulaIds)
    }

	    fun openScientificCalculator() {
        rememberCurrentIntent()
	        showSubjectHub = false
        showMathLanding = false
        showShapesExplorer = false
        shapeExplorerScene = false
        showProblemSolver = false
        showSolver = false
        showScientificCalculator = true
        showMathNotebook = false
        showUnifiedMathStudio = false
        showAdaptiveMathLearning = false
        showMathsLearnAll = false
        showGamifyMaths = false
        showProbabilityLab = false
        showKnowledgeHub = false
        showMathDictionary = false
        showMathMenu = false
        showActionDock = false
        hidePanels()
        status = "Scientific Calculator"
    }

    fun openSetLogicVisualizer() {
        rememberCurrentIntent()
	        showSubjectHub = false
        showMathLanding = false
        showConceptLibrary = false
        selectedMathConcept = null
        selectedMathSubConcept = null
        showPhysicsHub = false
        showShapesExplorer = false
        shapeExplorerScene = false
        showProblemSolver = false
        showSolver = false
        showScientificCalculator = false
        showSetLogicVisualizer = true
        showMathNotebook = false
        showUnifiedMathStudio = false
        showAdaptiveMathLearning = false
        showMathsLearnAll = false
        showGamifyMaths = false
        showProbabilityLab = false
        showKnowledgeHub = false
        showMathDictionary = false
        showMathMenu = false
        showActionDock = false
        hidePanels()
        status = "Set Theory & Logic Formula Visualizer"
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

    fun recordMathToolOpen(title: String) {
        recentMathTools = (listOf(title) + recentMathTools.filterNot { it == title }).take(6)
        mathToolUseCounts = mathToolUseCounts + (title to ((mathToolUseCounts[title] ?: 0) + 1))
        savedStateHandle["recentMathTools"] = ArrayList(recentMathTools)
        savedStateHandle["mathToolUseCounts"] = ArrayList(mathToolUseCounts.map { "${it.key}=${it.value}" })
    }

    fun togglePinnedMathTool(title: String) {
        pinnedMathTools = if (title in pinnedMathTools) pinnedMathTools - title else pinnedMathTools + title
        savedStateHandle["pinnedMathTools"] = ArrayList(pinnedMathTools.sorted())
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

    fun hasDismissibleOverlay(): Boolean =
        showMathMenu || showActionDock || showLearningPanel || showLeftPanel || showRightPanel || showBottomPanel

    fun dismissTopOverlay() {
        when {
            showMathMenu -> showMathMenu = false
            showActionDock -> showActionDock = false
            showLearningPanel -> showLearningPanel = false
            showLeftPanel || showRightPanel || showBottomPanel -> hidePanels()
        }
    }

	    fun navigateBackFromMathWorkspace() {
	        when {
	            hasDismissibleOverlay() -> dismissTopOverlay()
	            restorePreviousIntent() -> Unit
	            else -> withoutRecordingIntent { returnToMathMenu() }
	        }
	    }

    fun navigateBackIntent() {
        when {
            hasDismissibleOverlay() -> dismissTopOverlay()
            showSubjectHub || showMathLanding -> Unit
            restorePreviousIntent() -> Unit
            else -> withoutRecordingIntent { returnToMathMenu() }
        }
    }

    fun dismissAllMenusAndPanels() {
        showMathMenu = false
        showActionDock = false
        showLearningPanel = false
        hidePanels()
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

    fun applyContextualGeometryTool(toolName: String, pointIndices: List<Int>) {
        val tool = runCatching { GeometryTool.valueOf(toolName) }.getOrNull() ?: return
        val required = tool.requiredTapCount()
        val inputs = pointIndices.take(required)
        if (required == 0 || inputs.size < required || inputs.any { it !in state.points.indices }) {
            geometryTool = tool
            pendingPointIndices = inputs.map<Int, Int?> { it }
            pendingConstruction = inputs.mapNotNull(state.points::getOrNull)
            status = "${tool.name}: ${inputs.size} selected - tap ${required - inputs.size} more point${if (required - inputs.size == 1) "" else "s"}"
            return
        }
        val dependencyType = tool.toPointDependencyType()
        if (dependencyType != null) {
            state = history.execute(state, AddDependentPointCommand(inputs, dependencyType))
            selectedPoint = state.points.lastIndex
            selectedShape = -1
            selectedShapes = emptySet()
        } else {
            val shapeType = tool.toShape2DType() ?: return selectGeometryTool(tool)
            state = history.execute(state, AddShapeFromPointsCommand(shapeType, inputs, tool.name))
            selectedShape = state.shapes.lastIndex
            selectedShapes = setOf(selectedShape)
            selectedPoint = -1
        }
        geometryTool = GeometryTool.Select
        pendingConstruction = emptyList()
        pendingPointIndices = emptyList()
        status = "Created ${tool.name.lowercase()} from the current selection"
    }

    fun applyGeometryConstraint(constraint: com.indianservers.aiexplorer.workspace.GeometryConstraint2D) {
        state = history.execute(state, AddGeometryConstraint2DCommand(constraint))
        val feedback = Geometry2DInteractionEngine.evaluateConstraint(state, constraint)
        status = "${constraint.type.label}: ${feedback.level.name.lowercase().replaceFirstChar { it.uppercase() }}"
    }

    fun removeGeometryConstraint(id: String) {
        if (state.geometryConstraints.none { it.id == id }) return
        state = state.copy(geometryConstraints = state.geometryConstraints.filterNot { it.id == id }, modifiedAt = System.currentTimeMillis())
        status = "Constraint removed"
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
        state.points.getOrNull(index) ?: return
        val before = state
        when (val mutation = unified2DController.editCoordinates(unified2DController.snapshot(state), "point-$index", point)) {
            is Unified2DMutation.Applied -> {
                state = history.execute(before, ReplaceWorkspaceCommand(before, mutation.snapshot.state, "Move point ${index + 1}"))
                selectedPoint = index
                status = "Moved point ${index + 1}; ${mutation.affectedObjects.size} linked object${if (mutation.affectedObjects.size == 1) "" else "s"} updated"
            }
            is Unified2DMutation.Rejected -> status = mutation.message
        }
    }

    fun beginPointDrag(index: Int) {
        state.points.getOrNull(index) ?: return
        selectedShape = selectedShape
            .takeIf { it in state.shapes.indices && state.shapes[it].visible && index in state.shapes[it].pointIndices }
            ?: state.shapes.indexOfLast { it.visible && index in it.pointIndices }
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
        val geometryBefore = state.geometry2DSnapshot()
        val plan = Geometry2DDragPlanner.planHandleDrag(state, selectedShape, index)
        state = plan.state
        val actualIndex = plan.pointIndex
        val point = state.points.getOrNull(actualIndex) ?: return
        pointGesture = PointGesture(
            listOf(actualIndex),
            listOf(point),
            geometryBefore.takeIf { plan.detached },
        )
        selectedPoint = actualIndex
        status = state.shapes.getOrNull(selectedShape)?.let { "Resize ${it.name} from junction" } ?: "Move point ${index + 1}"
    }

    fun beginShapeDrag(shapeIndex: Int) {
        val shape = state.shapes.getOrNull(shapeIndex) ?: return
        selectedShape = shapeIndex
        selectedPoint = -1
        if (shape.locked) {
            selectedShapes = setOf(shapeIndex)
            status = "${shape.name} is locked"
            return
        }
        val geometryBefore = state.geometry2DSnapshot()
        val plan = Geometry2DDragPlanner.plan(state, shapeIndex, selectedShapes)
        selectedShapes = plan.selectedShapeIndices
        state = plan.state
        val indices = plan.movablePointIndices
        if (indices.isEmpty()) {
            status = "This object is locked or fully dependency-controlled"
            return
        }
        pointGesture = PointGesture(
            indices,
            indices.mapNotNull(state.points::getOrNull),
            geometryBefore.takeIf { plan.detached },
        )
        status = if (plan.detached) "Moving ${shape.name} independently" else "Moving ${selectedShapes.size} object${if (selectedShapes.size == 1) "" else "s"}"
    }

    fun previewPointDrag(index: Int, point: Vec2) {
        val gesture = pointGesture ?: return
        val actualIndex = if (index in gesture.indices) index else gesture.indices.singleOrNull() ?: return
        state = state.copy(
            points = state.points.mapIndexed { i, old -> if (i == actualIndex) point else old },
            modifiedAt = System.currentTimeMillis(),
        ).recomputed()
        selectedPoint = actualIndex
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

    fun previewShapeScale(factor: Double) {
        val gesture = pointGesture ?: return
        val center = InteractionGeometry.bounds(gesture.from)?.center ?: return
        val safeFactor = factor.coerceIn(.08, 12.0)
        val replacements = gesture.indices.zip(gesture.from.map { point -> center + (point - center) * safeFactor }).toMap()
        state = state.copy(
            points = state.points.mapIndexed { index, old -> replacements[index] ?: old },
            modifiedAt = System.currentTimeMillis(),
        ).recomputed()
    }

    fun rotateSelectedShapeBy(deltaDegrees: Double) {
        val index = selectedShape.takeIf { it in state.shapes.indices } ?: return
        beginShapeDrag(index)
        if (pointGesture == null) return
        previewShapeRotation(deltaDegrees)
        endPointDrag()
        status = "Rotated ${state.shapes.getOrNull(index)?.name ?: "object"} by ${trim(deltaDegrees)} degrees"
    }

    fun scaleSelectedShapeBy(factor: Double) {
        val index = selectedShape.takeIf { it in state.shapes.indices } ?: return
        beginShapeDrag(index)
        if (pointGesture == null) return
        previewShapeScale(factor)
        endPointDrag()
        status = "Resized ${state.shapes.getOrNull(index)?.name ?: "object"} × ${trim(factor)}"
    }

    fun nudgeSelectedShape(delta: Vec2) {
        val index = selectedShape.takeIf { it in state.shapes.indices } ?: return
        beginShapeDrag(index)
        if (pointGesture == null) return
        previewShapeDrag(delta)
        endPointDrag()
        status = "Moved ${state.shapes.getOrNull(index)?.name ?: "object"}"
    }

    fun endPointDrag() {
        val gesture = pointGesture ?: return
        val final = gesture.indices.mapNotNull(state.points::getOrNull)
        if (gesture.geometryBefore != null) {
            val after = state.geometry2DSnapshot()
            if (after != gesture.geometryBefore) {
                history.recordApplied(ReplaceGeometry2DCommand(gesture.geometryBefore, after, "Move object independently"))
            }
        } else if (final != gesture.from) {
            history.recordApplied(MovePointsCommand(gesture.indices, gesture.from, final))
        }
        val resizedShape = selectedShape.takeIf { gesture.indices.size == 1 }?.let(state.shapes::getOrNull)
        pointGesture = null
        status = resizedShape?.let { "Resized ${it.name} from junction" } ?: "Moved object"
    }

    fun cancelPointDrag() {
        val gesture = pointGesture ?: return
        state = if (gesture.geometryBefore != null) {
            ReplaceGeometry2DCommand(state.geometry2DSnapshot(), gesture.geometryBefore).apply(state)
        } else {
            val replacements = gesture.indices.zip(gesture.from).toMap()
            state.copy(points = state.points.mapIndexed { index, old -> replacements[index] ?: old }).recomputed()
        }
        pointGesture = null
        status = "Move cancelled"
    }

    fun selectShape(index: Int) {
        if (index !in state.shapes.indices) return
        selectedShape = index
        selectedShapes = setOf(index)
        selectedPoint = -1
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

    fun reorderSelectedShape(toFront: Boolean) {
        val index = selectedShape.takeIf { it in state.shapes.indices } ?: return
        val shape = state.shapes[index]
        val reordered = state.shapes.toMutableList().apply { removeAt(index); if (toFront) add(shape) else add(0, shape) }
        state = history.execute(state, com.indianservers.aiexplorer.workspace.ReorderShapesCommand(state.shapes, reordered))
        selectedShape = if (toFront) state.shapes.lastIndex else 0
        selectedShapes = setOf(selectedShape)
        status = if (toFront) "Brought ${shape.name} to front" else "Sent ${shape.name} behind"
    }

    fun duplicateSelectedShape() {
        transformSelectedShape(PointDependencyType.Translate, listOf(.35, -.35))
        selectedShapes = selectedShape.takeIf { it in state.shapes.indices }?.let(::setOf) ?: emptySet()
        status = state.shapes.getOrNull(selectedShape)?.let { "Duplicated ${it.name}" } ?: status
    }

    fun exportSelectedGeometry(): String = buildString {
        appendLine("AIEXPLORER_GEOMETRY_2D_V1")
        selectedShapes.sorted().mapNotNull(state.shapes::getOrNull).forEach { shape ->
            val coordinates = shape.pointIndices.mapNotNull(state.points::getOrNull).joinToString(";") { "${it.x},${it.y}" }
            appendLine(listOf(shape.type.name, java.net.URLEncoder.encode(shape.name, "UTF-8"), shape.styleKey, coordinates).joinToString("|"))
        }
    }.trim()

    fun importGeometry(text: String) {
        if (!text.startsWith("AIEXPLORER_GEOMETRY_2D_V1")) { status = "Clipboard does not contain AI Explorer geometry"; return }
        val before = state.shapes.size
        text.lineSequence().drop(1).filter(String::isNotBlank).forEach { row ->
            val fields = row.split('|'); val type = fields.getOrNull(0)?.let { runCatching { Shape2DType.valueOf(it) }.getOrNull() } ?: return@forEach
            val values = fields.getOrNull(3)?.split(';').orEmpty().mapNotNull { pair -> pair.split(',').takeIf { it.size == 2 }?.let { Vec2(it[0].toDoubleOrNull() ?: return@mapNotNull null, it[1].toDoubleOrNull() ?: return@mapNotNull null) } }
            if (values.isEmpty()) return@forEach
            state = history.execute(state, AddConstructionCommand(values.map { it + Vec2(.35, -.35) }, type))
            val index = state.shapes.lastIndex; val shape = state.shapes[index]
            val name = fields.getOrNull(1)?.let { runCatching { java.net.URLDecoder.decode(it, "UTF-8") }.getOrNull() } ?: shape.name
            val style = fields.getOrNull(2) ?: shape.styleKey
            state = history.execute(state, UpdateShapeCommand(index, shape, shape.copy(name = "$name Copy", styleKey = style)))
        }
        selectedShapes = (before until state.shapes.size).toSet(); selectedShape = selectedShapes.lastOrNull() ?: -1
        status = if (selectedShapes.isEmpty()) "No geometry pasted" else "Pasted ${selectedShapes.size} object${if (selectedShapes.size == 1) "" else "s"}"
    }

    fun moveGeometrySelectionFromKeyboard(dx: Int, dy: Int, precision: Boolean) {
        if (selectedPoint in state.points.indices) {
            Geometry2DDirectManipulation.movePointByKeyboard(state, selectedPoint, dx, dy, precision)?.let { movePoint(selectedPoint, it) }
            return
        }
        val index = selectedShape.takeIf { it in state.shapes.indices } ?: return
        beginShapeDrag(index)
        val amount = if (precision) .01 else .1
        previewShapeDrag(Vec2(dx * amount, dy * amount))
        endPointDrag()
    }

    fun cycleGeometrySelection(backwards: Boolean = false) {
        if (state.shapes.isEmpty()) return
        val delta = if (backwards) -1 else 1
        selectShape(((selectedShape.takeIf { it >= 0 } ?: if (backwards) 0 else -1) + delta).mod(state.shapes.size))
    }

    fun freeSelectedDependentPoint() {
        val dependency = state.pointDependencies.firstOrNull { it.outputIndex == selectedPoint } ?: return
        state = history.execute(state, com.indianservers.aiexplorer.workspace.RemovePointDependencyCommand(dependency))
        status = "Converted ${dependency.name} to a free point"
    }

    fun nudgeSelectedDependencyParent() {
        val dependency = state.pointDependencies.firstOrNull { it.outputIndex == selectedPoint } ?: return
        val parent = dependency.inputIndices.lastOrNull { index -> state.pointDependencies.none { it.outputIndex == index } } ?: return
        movePoint(parent, state.points[parent] + Vec2(.0, .1))
        selectedPoint = dependency.outputIndex
        status = "Nudged a parent to repair ${dependency.name}"
    }

    fun groupSelectedShapes() {
        val ids = selectedShapes.mapNotNull(state.shapes::getOrNull).mapTo(linkedSetOf()) { it.id }
        if (ids.size < 2) return
        val group = com.indianservers.aiexplorer.workspace.GeometryGroup2D("group-${System.currentTimeMillis()}", "Group ${state.geometryGroups.size + 1}", ids)
        state = history.execute(state, com.indianservers.aiexplorer.workspace.ReplaceGeometryGroupsCommand(state.geometryGroups, state.geometryGroups + group))
        status = "Grouped ${ids.size} objects"
    }

    fun ungroupSelectedShapes() {
        val ids = selectedShapes.mapNotNull(state.shapes::getOrNull).map { it.id }.toSet()
        val groups = state.geometryGroups.filterNot { it.shapeIds == ids || it.shapeIds.any(ids::contains) }
        if (groups == state.geometryGroups) return
        state = history.execute(state, com.indianservers.aiexplorer.workspace.ReplaceGeometryGroupsCommand(state.geometryGroups, groups))
        status = "Group removed"
    }

    fun deleteSelectedShape() {
        val indices = selectedShapes.filterTo(linkedSetOf()) { it in state.shapes.indices }
            .ifEmpty { selectedShape.takeIf { it in state.shapes.indices }?.let(::setOf).orEmpty() }
        if (indices.isEmpty()) return
        val names = indices.mapNotNull(state.shapes::getOrNull).map { it.name }
        val anchor = indices.minOrNull() ?: 0
        state = history.execute(state, DeleteShapesCommand(indices, state.shapes, state.points, state.pointDependencies, state.geometryGroups, state.geometryConstraints))
        selectedShape = (anchor - 1).coerceAtMost(state.shapes.lastIndex)
        selectedShapes = selectedShape.takeIf { it >= 0 }?.let(::setOf) ?: emptySet()
        selectedPoint = -1
        status = "Deleted ${names.joinToString()}"
    }

    fun transformSelectedShape(type: PointDependencyType, parameters: List<Double> = emptyList()) {
        val index = selectedShape.takeIf { it in state.shapes.indices } ?: return
        state = history.execute(state, TransformShape2DCommand(index, type, parameters))
        selectedShape = state.shapes.lastIndex
        status = "${type.name} created"
    }

    fun editExpression(index: Int, expression: String) {
        val function = state.functions.getOrNull(index) ?: return
        val before = state
        when (val mutation = unified2DController.stageExpression(unified2DController.snapshot(state), function.id, expression)) {
            is Unified2DMutation.Applied -> {
                state = history.execute(before, ReplaceWorkspaceCommand(before, mutation.snapshot.state, "Edit ${function.name}"))
                val staged = mutation.snapshot.document.objects[function.id]?.valueState?.status == com.indianservers.aiexplorer.workspace.UniversalMathValueStatus.ParseError
                status = if (staged) "Expression draft saved; complete it to resume verified computation" else "Expression updated; ${mutation.affectedObjects.size} linked object${if (mutation.affectedObjects.size == 1) "" else "s"} recomputed"
            }
            is Unified2DMutation.Rejected -> status = mutation.message
        }
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
        val template = defaultSolid(type)
        val solid = template.copy(position = SmartSolidPlacementEngine.next(state.solids, template))
        state = history.execute(state, AddSolidCommand(solid))
        selectedSolid = state.solids.lastIndex
        status = "Added ${type.name} in open scene space"
    }

    fun selectSolid(index: Int) {
        selectedSolid = index.takeIf { it in state.solids.indices } ?: -1
        if (selectedSolid >= 0) {
            selectedVector3D = -1
            selectedPoint3D = -1
        }
        status = state.solids.getOrNull(selectedSolid)?.let { "Selected ${it.type.name}" } ?: "No solid selected"
    }

    fun deleteSelectedSolid() {
        deleteSelectedSolids(setOf(selectedSolid))
    }

    fun deleteSelectedSolids(indices: Set<Int>) {
        val valid = indices.filterTo(linkedSetOf()) { it in state.solids.indices }
        if (valid.isEmpty()) return
        val names = valid.mapNotNull(state.solids::getOrNull).map { it.type.name }
        val anchor = valid.minOrNull() ?: 0
        state = history.execute(state, DeleteSolidsCommand(valid, state.solids))
        selectedSolid = when {
            state.solids.isEmpty() -> -1
            anchor <= state.solids.lastIndex -> anchor
            else -> state.solids.lastIndex
        }
        solidGesture = null
        status = "Deleted ${names.joinToString()}"
    }

    fun moveSolid(index: Int, to: Vec3) {
        val solid = state.solids.getOrNull(index) ?: return
        val before = state
        when (val mutation = unifiedSpatialController.updateSolid(unifiedSpatialController.snapshot(state), index) { it.copy(position = to) }) {
            is UnifiedSpatialMutation.Applied -> {
                state = history.execute(before, ReplaceWorkspaceCommand(before, mutation.snapshot.state, "Move ${solid.type.name}"))
                selectedSolid = index
                status = "Moved ${solid.type.name}; linked 3D views updated"
            }
            is UnifiedSpatialMutation.Rejected -> status = mutation.message
        }
    }

    fun beginSolidDrag(index: Int) {
        val solid = state.solids.getOrNull(index) ?: return
        solidGesture = SolidGesture(index, solid)
        selectSolid(index)
    }

    fun beginSolidGroupDrag(indices: Set<Int>) {
        val valid = indices.filterTo(linkedSetOf()) { it in state.solids.indices }
        if (valid.isEmpty()) return
        solidGroupGesture = SolidGroupGesture(valid, state.solids)
        selectedSolid = valid.last()
    }

    fun previewSolidGroupMove(delta: Vec3) {
        val gesture = solidGroupGesture ?: return
        state = state.copy(solids = gesture.from.mapIndexed { index, solid -> if (index in gesture.indices) solid.copy(position = solid.position + delta) else solid }, modifiedAt = System.currentTimeMillis())
    }

    fun previewSolidGroupRotation(delta: Vec3) {
        val gesture = solidGroupGesture ?: return
        state = state.copy(solids = gesture.from.mapIndexed { index, solid -> if (index in gesture.indices) solid.copy(rotation = solid.rotation + delta) else solid }, modifiedAt = System.currentTimeMillis())
    }

    fun previewSolidGroupScale(factor: Double) {
        val gesture = solidGroupGesture ?: return; val f = factor.coerceIn(.2, 5.0)
        state = state.copy(solids = gesture.from.mapIndexed { index, solid -> if (index in gesture.indices) solid.copy(width = solid.width * f, height = solid.height * f, depth = solid.depth * f, radius = solid.radius * f, topRadius = solid.topRadius * f) else solid }, modifiedAt = System.currentTimeMillis())
    }

    fun endSolidGroupDrag() {
        val gesture = solidGroupGesture ?: return
        if (state.solids != gesture.from) history.recordApplied(ReplaceSolidsCommand(gesture.from, state.solids, "Transform 3D group"))
        solidGroupGesture = null; status = "Transformed ${gesture.indices.size} objects"
    }

    fun cancelSolidGroupDrag() {
        val gesture = solidGroupGesture ?: return
        state = state.copy(solids = gesture.from); solidGroupGesture = null; status = "Group transform cancelled"
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

    fun previewSolidAxisScale(index: Int, axis: TransformGizmoAxis, factor: Double) {
        val gesture = solidGesture?.takeIf { it.index == index } ?: return
        val f = factor.coerceIn(.2, 5.0)
        val from = gesture.from
        val scaled = from.copy(
            width = (from.width * if (axis in setOf(TransformGizmoAxis.X, TransformGizmoAxis.Uniform)) f else 1.0).coerceIn(.2, 12.0),
            height = (from.height * if (axis in setOf(TransformGizmoAxis.Y, TransformGizmoAxis.Uniform)) f else 1.0).coerceIn(.2, 12.0),
            depth = (from.depth * if (axis in setOf(TransformGizmoAxis.Z, TransformGizmoAxis.Uniform)) f else 1.0).coerceIn(.2, 12.0),
            radius = (from.radius * if (axis == TransformGizmoAxis.Uniform) f else 1.0).coerceIn(.1, 6.0),
            topRadius = (from.topRadius * if (axis == TransformGizmoAxis.Uniform) f else 1.0).coerceIn(.05, 6.0),
        )
        state = state.copy(solids = state.solids.mapIndexed { i, old -> if (i == index) scaled else old }, modifiedAt = System.currentTimeMillis())
    }

    fun previewSolidFaceExtrusion(index: Int, faceIndex: Int, factor: Double) {
        val gesture = solidGesture?.takeIf { it.index == index } ?: return
        val amount = (factor - 1.0) * 2.0
        val extruded = AdvancedSpatialInteractionEngine.extrude(gesture.from, faceIndex, amount)
        state = state.copy(solids = state.solids.mapIndexed { i, old -> if (i == index) extruded else old }, modifiedAt = System.currentTimeMillis())
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

    fun replaceSolids(label: String, transform: (List<Solid>) -> List<Solid>) {
        val from = state.solids
        val to = transform(from)
        if (from == to) return
        state = history.execute(state, ReplaceSolidsCommand(from, to, label))
        selectedSolid = selectedSolid.takeIf { it in state.solids.indices } ?: state.solids.lastIndex
        status = label
    }

    fun duplicateSelectedSolid() {
        val source = state.solids.getOrNull(selectedSolid) ?: return
        state = history.execute(state, AddSolidCommand(source.copy(position = source.position + Vec3(.8, .2, .8))))
        selectedSolid = state.solids.lastIndex
        status = "Duplicated ${source.type.name}"
    }

    fun addPoint3D(position: Vec3 = Vec3(0.0, 0.0, 0.0)) {
        val n = state.points3D.size + 1
        val offset = (n - 1) * .55
        val point = Point3D(
            id = "P3$n",
            name = "P$n",
            position = position + Vec3(offset, 0.0, offset * .35),
            styleKey = listOf("green", "cyan", "amber", "violet")[(n - 1).mod(4)],
        )
        state = history.execute(state, AddPoint3DCommand(point))
        selectedPoint3D = state.points3D.lastIndex
        selectedSolid = -1
        selectedVector3D = -1
        status = "Added 3D point ${point.name} at (${trim(point.position.x)}, ${trim(point.position.y)}, ${trim(point.position.z)})"
    }

    fun selectPoint3D(index: Int) {
        selectedPoint3D = index.takeIf { it in state.points3D.indices } ?: -1
        if (selectedPoint3D >= 0) {
            selectedSolid = -1
            selectedVector3D = -1
        }
        status = state.points3D.getOrNull(selectedPoint3D)?.let { "Selected point ${it.name}" } ?: "No 3D point selected"
    }

    fun deletePoint3D(index: Int) {
        val point = state.points3D.getOrNull(index) ?: return
        state = history.execute(state, DeletePoint3DCommand(index, point))
        selectedPoint3D = when {
            state.points3D.isEmpty() -> -1
            index <= state.points3D.lastIndex -> index
            else -> state.points3D.lastIndex
        }
        status = "Deleted 3D point ${point.name}"
    }

    fun transformPoint3D(index: Int, transform: (Point3D) -> Point3D) {
        val from = state.points3D.getOrNull(index) ?: return
        val to = transform(from)
        state = history.execute(state, TransformPoint3DCommand(index, from, to))
        selectedPoint3D = index
        selectedSolid = -1
        selectedVector3D = -1
        status = "Updated point ${to.name}"
    }

    fun beginPoint3DDrag(index: Int) {
        val point = state.points3D.getOrNull(index) ?: return
        if (point.locked) {
            status = "Point ${point.name} is locked"
            return
        }
        point3DGesture = Point3DGesture(index, point)
        selectPoint3D(index)
    }

    fun previewPoint3DDrag(index: Int, delta: Vec3) {
        val gesture = point3DGesture?.takeIf { it.index == index } ?: return
        val moved = gesture.from.copy(position = gesture.from.position + delta)
        state = state.copy(
            points3D = state.points3D.mapIndexed { i, old -> if (i == index) moved else old },
            modifiedAt = System.currentTimeMillis(),
        )
    }

    fun endPoint3DDrag() {
        val gesture = point3DGesture ?: return
        val final = state.points3D.getOrNull(gesture.index)
        if (final != null && final != gesture.from) history.recordApplied(TransformPoint3DCommand(gesture.index, gesture.from, final))
        point3DGesture = null
        status = "Moved point ${gesture.from.name}"
    }

    fun cancelPoint3DDrag() {
        val gesture = point3DGesture ?: return
        state = state.copy(points3D = state.points3D.mapIndexed { i, old -> if (i == gesture.index) gesture.from else old })
        point3DGesture = null
        status = "Point move cancelled"
    }

    fun addVector3D(
        namePrefix: String = "w",
        start: Vec3? = null,
        end: Vec3? = null,
        statusLabel: String = "3D vector",
    ) {
        val n = state.vectors3D.size + 1
        val offset = (n - 2) * .55
        val vector = Vector3D(
            id = "$namePrefix$n",
            name = "$namePrefix$n",
            start = start ?: Vec3(-1.5 + offset, -1.0, -1.0),
            end = end ?: Vec3(1.4 + offset, 1.0, 1.2),
        )
        state = history.execute(state, AddVector3DCommand(vector))
        selectedVector3D = state.vectors3D.lastIndex
        selectedPoint3D = -1
        selectedSolid = -1
        status = "Added $statusLabel ${vector.name}"
    }

    fun addSegment3D() = addVector3D(
        namePrefix = "seg",
        start = Vec3(-1.2, 0.0, -0.6),
        end = Vec3(1.2, 0.0, 0.6),
        statusLabel = "3D segment construction",
    )

    fun addLine3D() = addVector3D(
        namePrefix = "line",
        start = Vec3(-2.5, 0.0, -1.25),
        end = Vec3(2.5, 0.0, 1.25),
        statusLabel = "3D line construction",
    )

    fun addRay3D() = addVector3D(
        namePrefix = "ray",
        start = Vec3(-1.4, 0.0, -0.8),
        end = Vec3(2.2, 0.0, 1.1),
        statusLabel = "3D ray construction",
    )

    fun selectVector3D(index: Int) {
        selectedVector3D = index.takeIf { it in state.vectors3D.indices } ?: -1
        if (selectedVector3D >= 0) {
            selectedPoint3D = -1
            selectedSolid = -1
        }
        status = state.vectors3D.getOrNull(selectedVector3D)?.let { "Selected vector ${it.name}" } ?: "No vector selected"
    }

    fun deleteVector3D(index: Int) {
        val vector = state.vectors3D.getOrNull(index) ?: return
        state = history.execute(state, DeleteVector3DCommand(index, vector))
        selectedVector3D = when {
            state.vectors3D.isEmpty() -> -1
            index <= state.vectors3D.lastIndex -> index
            else -> state.vectors3D.lastIndex
        }
        vectorGesture = null
        status = "Deleted vector ${vector.name}"
    }

    fun moveVector3D(index: Int, delta: Vec3) {
        val from = state.vectors3D.getOrNull(index) ?: return
        val before = state
        when (val mutation = unifiedSpatialController.updateVector(unifiedSpatialController.snapshot(state), from.id) {
            it.copy(start = it.start + delta, end = it.end + delta)
        }) {
            is UnifiedSpatialMutation.Applied -> {
                state = history.execute(before, ReplaceWorkspaceCommand(before, mutation.snapshot.state, "Move vector ${from.name}"))
                selectedVector3D = index
                status = "Moved vector ${from.name}; linked 3D views updated"
            }
            is UnifiedSpatialMutation.Rejected -> status = mutation.message
        }
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
        val before = state
        when (val mutation = unifiedSpatialController.stageSurface(unifiedSpatialController.snapshot(state), value)) {
            is UnifiedSpatialMutation.Applied -> {
                state = history.execute(before, ReplaceWorkspaceCommand(before, mutation.snapshot.state, "Edit 3D surface"))
                val staged = mutation.snapshot.document.objects["surface-main"]?.valueState?.status == com.indianservers.aiexplorer.workspace.UniversalMathValueStatus.ParseError
                status = if (staged) "Surface draft saved; complete it to resume mesh analysis" else "Surface, parameter table and linked 3D views updated"
            }
            is UnifiedSpatialMutation.Rejected -> status = mutation.message
        }
    }

    fun replaceSurfaceLayers(
        layers: List<com.indianservers.aiexplorer.core.SpatialSurfaceLayer>,
        label: String = "Update 3D surface layers",
    ): Result<Unit> {
        val before = state
        return when (val mutation = unifiedSpatialController.replaceSurfaceLayers(unifiedSpatialController.snapshot(state), layers)) {
            is UnifiedSpatialMutation.Applied -> {
                state = history.execute(before, ReplaceWorkspaceCommand(before, mutation.snapshot.state, label))
                status = "Updated ${layers.size} linked 3D surface layer${if (layers.size == 1) "" else "s"}"
                Result.success(Unit)
            }
            is UnifiedSpatialMutation.Rejected -> {
                status = mutation.message
                Result.failure(IllegalArgumentException(mutation.message))
            }
        }
    }

    fun updateGraph3DView(transform: (com.indianservers.aiexplorer.workspace.Graph3DViewState) -> com.indianservers.aiexplorer.workspace.Graph3DViewState) {
        val updated = transform(state.graph3DView)
        if (updated != state.graph3DView) state = state.copy(graph3DView = updated, modifiedAt = System.currentTimeMillis())
    }

    fun updateGraph2DView(transform: (com.indianservers.aiexplorer.workspace.Graph2DViewState) -> com.indianservers.aiexplorer.workspace.Graph2DViewState) {
        val updated = transform(state.graph2DView)
        if (updated != state.graph2DView) state = state.copy(graph2DView = updated, modifiedAt = System.currentTimeMillis())
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
        status = if (validation.accepted) "Spatial scene placed - ±${trim(hit.uncertaintyMeters)} m" else validation.messages.joinToString(" ")
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

    val canUndo: Boolean get() = history.canUndo
    val canRedo: Boolean get() = history.canRedo
    val universalHistoryDepth: Int get() = history.protocol.size

    val mathsBreadcrumb: List<String>
        get() = when {
            showSolver -> listOf("Maths", "Tools", "Solver")
            showProblemSolver -> listOf("Maths", "Algebra", "Quadratics")
            showKnowledgeHub && activeKnowledgeSection == KnowledgeSection.Dictionary -> listOf("Maths", "Reference", "Visual Dictionary")
            showKnowledgeHub -> listOf("Maths", "Knowledge", activeKnowledgeSection.title)
            showScientificCalculator -> listOf("Maths", "Tools", "Scientific Calculator")
            showMathNotebook -> listOf("Maths", "Workspace", "Notebook")
            showUnifiedMathStudio -> listOf("Maths", "Studio", "Linked Views")
            showAdaptiveMathLearning -> listOf("Maths", "Learn", "Adaptive Coach")
            showMathsLearnAll -> listOf("Maths", "Learn", "Learn All")
            showProbabilityLab -> listOf("Maths", "Data", "Probability & Statistics")
            showShapesExplorer -> listOf("Maths", "Geometry", "Shapes Explorer")
            showSetLogicVisualizer -> listOf("Maths", "Foundations", "Set Theory & Logic")
            else -> listOf("Maths", state.module.label)
        }

    fun redo() {
        state = history.redo(state)
        status = "Redo"
    }

    fun clearCurrentWorkspace() {
        val cleared = when (state.module) {
            MathModule.Geometry2D -> state.copy(
                points = emptyList(),
                shapes = emptyList(),
                pointDependencies = emptyList(),
                geometryConstraints = emptyList(),
                geometryGroups = emptyList(),
            )
            MathModule.Geometry3D, MathModule.SpatialAR -> state.copy(
                solids = emptyList(),
                vectors3D = emptyList(),
                points3D = emptyList(),
                spatialPlacement = com.indianservers.aiexplorer.spatial.SpatialScenePlacement(),
            )
            MathModule.Graph2D -> state.copy(
                functions = emptyList(),
                graphRowMetadata = emptyMap(),
                graphSliderMetadata = emptyMap(),
            )
            MathModule.Graph3D -> state.copy(surfaceExpression = "0", surfaceLayers = emptyList())
            MathModule.Trigonometry,
            MathModule.Manipulatives,
            MathModule.ProbabilityStatistics,
            MathModule.MatricesLinearTransformations,
            MathModule.DataSpreadsheet,
            MathModule.DiscreteMathematics,
            MathModule.NumberTheory,
            MathModule.ARGraph3D -> state
        }
        state = history.execute(
            state,
            com.indianservers.aiexplorer.workspace.ReplaceWorkspaceCommand(state, cleared),
        )
        selectedShape = -1
        selectedShapes = emptySet()
        selectedPoint = -1
        selectedSolid = -1
        selectedVector3D = -1
        selectedPoint3D = -1
        pendingConstruction = emptyList()
        pendingPointIndices = emptyList()
        geometryTool = GeometryTool.Select
        workspaceClearEpoch++
        dismissAllMenusAndPanels()
        status = "${state.module.label} workspace cleared - Undo is available"
    }

    fun clearArWorkspace(mode: ArMathWorkspaceMode) {
        val cleared = when (mode) {
            ArMathWorkspaceMode.Geometry2D -> state.copy(
                points = emptyList(),
                shapes = emptyList(),
                pointDependencies = emptyList(),
                geometryConstraints = emptyList(),
                geometryGroups = emptyList(),
            )
            ArMathWorkspaceMode.Geometry3D -> state.copy(
                solids = emptyList(),
                vectors3D = emptyList(),
                points3D = emptyList(),
                spatialPlacement = com.indianservers.aiexplorer.spatial.SpatialScenePlacement(),
            )
            ArMathWorkspaceMode.Graph2D -> state.copy(
                functions = emptyList(),
                graphRowMetadata = emptyMap(),
                graphSliderMetadata = emptyMap(),
            )
            ArMathWorkspaceMode.Graph3D -> state.copy(
                surfaceExpression = "0",
                surfaceLayers = emptyList(),
                spatialPlacement = com.indianservers.aiexplorer.spatial.SpatialScenePlacement(),
            )
            ArMathWorkspaceMode.CAS -> state
        }
        if (cleared == state) return
        state = history.execute(
            state,
            com.indianservers.aiexplorer.workspace.ReplaceWorkspaceCommand(state, cleared, "Clear AR ${mode.label}"),
        )
        selectedShape = -1
        selectedShapes = emptySet()
        selectedPoint = -1
        selectedSolid = -1
        selectedVector3D = -1
        selectedPoint3D = -1
        pendingConstruction = emptyList()
        pendingPointIndices = emptyList()
        geometryTool = GeometryTool.Select
        workspaceClearEpoch++
        status = "Cleared AR ${mode.label} - Undo is available"
    }

    fun reset() = clearCurrentWorkspace()

    fun exportJson(): String {
        status = "Workspace JSON generated"
        return WorkspaceJson.export(state)
    }

    fun hydrateDurableState(recovered: WorkspaceState?, projects: List<SavedWorkspace>, persistedSettings: AppSettings) {
        recovered?.let { state = it }
        savedWorkspaces = projects
        settings = persistedSettings
        selectedPoint = selectedPoint.takeIf { it in state.points.indices } ?: -1
        selectedShape = -1
        selectedShapes = emptySet()
        status = if (recovered == null) "Ready" else "Recovered your last workspace"
    }

    fun importWorkspace(imported: WorkspaceState, recovered: Boolean, diagnostics: List<String>) {
        state = imported.copy(modifiedAt = System.currentTimeMillis())
        selectedPoint = state.points.indices.firstOrNull() ?: -1
        selectedShape = -1
        selectedShapes = emptySet()
        showSubjectHub = false
        showMathLanding = false
        showMathMenu = false
        status = when {
            recovered -> "Project recovered${diagnostics.firstOrNull()?.let { ": $it" }.orEmpty()}"
            else -> "Project imported"
        }
    }

    fun reportStatus(message: String) { status = message.take(180) }

    fun saveWorkspace() {
        val saved = SavedWorkspace(
            id = "workspace-${System.currentTimeMillis()}",
            name = "${state.name} ${savedWorkspaces.size + 1}",
            module = state.module,
            snapshot = state,
            json = WorkspaceProjectCodec.encode(state),
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
@OptIn(ExperimentalLayoutApi::class)
fun AIExplorerApp(vm: ExplorerViewModel = viewModel(), durableStateEnabled: Boolean = true) {
    var menuOffset by remember { mutableStateOf(Offset.Zero) }
    val showSplash = false
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showExitConfirmation by rememberSaveable { mutableStateOf(false) }
    var showClearConfirmation by rememberSaveable { mutableStateOf(false) }
    var showAiAssistant by rememberSaveable { mutableStateOf(false) }
    val hostActivity = LocalActivity.current
    val applicationContext = LocalContext.current.applicationContext
    val durableStore = remember(applicationContext) { DurableMathStore(applicationContext) }
    var persistenceReady by remember { mutableStateOf(false) }
    LaunchedEffect(durableStore, durableStateEnabled) {
        if (!durableStateEnabled) {
            persistenceReady = true
            return@LaunchedEffect
        }
        val recovered = runCatching { durableStore.loadRecovery() }.getOrNull()
        val projects = runCatching { durableStore.loadProjects() }.getOrDefault(emptyList())
        val settings = runCatching { durableStore.loadSettings() }.getOrDefault(AppSettings())
        vm.hydrateDurableState(recovered, projects, settings)
        persistenceReady = true
    }
    LaunchedEffect(durableStore, persistenceReady, durableStateEnabled) {
        if (!persistenceReady || !durableStateEnabled) return@LaunchedEffect
        snapshotFlow { vm.state }.drop(1).conflate().collect { state ->
            runCatching { durableStore.saveRecovery(state) }
                .onFailure { vm.reportStatus("Autosave unavailable: ${it.message ?: "storage error"}") }
        }
    }

    LaunchedEffect(durableStore, persistenceReady, durableStateEnabled) {
        if (!persistenceReady || !durableStateEnabled) return@LaunchedEffect
        snapshotFlow { vm.savedWorkspaces }.drop(1).conflate().collect { projects ->
            runCatching { durableStore.replaceProjects(projects) }
                .onFailure { vm.reportStatus("Project library unavailable: ${it.message ?: "storage error"}") }
        }
    }
    LaunchedEffect(durableStore, persistenceReady, durableStateEnabled) {
        if (!persistenceReady || !durableStateEnabled) return@LaunchedEffect
        snapshotFlow { vm.settings }.drop(1).conflate().collect { settings ->
            runCatching { durableStore.saveSettings(settings) }
                .onFailure { vm.reportStatus("Settings could not be saved: ${it.message ?: "storage error"}") }
        }
    }
    LaunchedEffect(vm.settings.colorScheme) {
        applyAppPalette(vm.settings.colorScheme.palette)
    }
    BackHandler {
        when {
            showAiAssistant -> showAiAssistant = false
            showSettings -> showSettings = false
            showClearConfirmation -> showClearConfirmation = false
            showExitConfirmation -> showExitConfirmation = false
            vm.showMathLanding && !vm.hasDismissibleOverlay() -> showExitConfirmation = true
            else -> vm.navigateBackIntent()
        }
    }
    val activePalette = vm.settings.colorScheme.palette
    ProvideAppVisualEffects(vm.settings.colorScheme) {
        MaterialTheme(
            colorScheme = darkColorScheme(
                background = activePalette.background,
                surface = activePalette.surface,
                surfaceVariant = activePalette.surfaceAlt,
                primary = activePalette.primary,
                secondary = activePalette.secondary,
                tertiary = activePalette.success,
                onBackground = activePalette.ink,
                onSurface = activePalette.ink,
                onPrimary = activePalette.background,
                onSecondary = activePalette.background,
            ),
        ) {
        Surface(Modifier.fillMaxSize(), color = activePalette.background) {
            Box(Modifier.fillMaxSize()) {
                val adaptiveProfile = rememberAdaptiveDeviceProfile()
                AdaptiveAppScaffold(
                    profile = adaptiveProfile,
                    modifier = Modifier.fillMaxSize(),
                    backdrop = appBackdrop(vm.settings.colorScheme),
                ) {
                    BoxWithConstraints(Modifier.fillMaxSize()) {
                        val compact = if (adaptiveProfile.isTelevision) false else maxWidth < 520.dp
                        val wide = if (adaptiveProfile.isTelevision) true else maxWidth >= 760.dp
                    LaunchedEffect(compact, wide) {
                        menuOffset = Offset.Zero
                    }
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(
                                start = if (vm.showChrome && vm.state.module != MathModule.ARGraph3D) {
                                    adaptiveProfile.workspacePolicy.reservedNavigationWidth
                                } else {
                                    0.dp
                                },
                            ),
                    ) {
                    if (!vm.showSubjectHub) {
	                    if (vm.showLearningIntelligence) {
	                        LearningIntelligenceFeatureRoot(onExit = vm::navigateBackIntent)
                    } else if (vm.showMathLanding) {
                        MathHubUi.Screen(
                            vm = vm,
                            wide = wide,
                            onSettingsClick = { showSettings = true },
                        )
                    } else if (vm.showShapesExplorer) {
                        ShapesExplorerScreen(vm, wide = wide)
                    } else if (vm.showUnifiedMathStudio) {
                        UnifiedMathStudioScreen(vm.state, vm::commitUnifiedStudio, vm::returnToMathMenu)
                    } else if (vm.showAdaptiveMathLearning) {
                        AdaptiveMathLearningScreen(vm.state, vm::returnToMathMenu)
                    } else if (vm.showMathsLearnAll) {
                        MathsLearnAllScreen(
                            onBack = vm::returnToMathMenu,
                            initialQuery = listOfNotNull(vm.selectedMathSubConcept, vm.selectedMathConcept).firstOrNull().orEmpty(),
                            initialLessonId = vm.selectedMathLessonId,
                            mode = vm.mathsLearnAllMode,
                        )
                    } else if (vm.showGamifyMaths) {
                        GamifyMathsRoot(onExit = vm::navigateBackIntent)
                    } else if (vm.showMathNotebook) {
                        MathNotebookScreen(vm, wide = wide)
                    } else if (vm.showSolver) {
                        SolverScreen(
                            onExit = vm::navigateBackIntent,
                            wide = wide,
                            onOpenGraph = { expression -> vm.addFunction(expression); vm.open(MathModule.Graph2D) },
                            onOpenMatrices = { vm.open(MathModule.MatricesLinearTransformations) },
                            onOpenStatistics = { vm.open(MathModule.ProbabilityStatistics) },
                            onOpenGeometry = { vm.open(MathModule.Geometry2D) },
                        )
                    } else if (vm.showProblemSolver) {
                        ProblemSolverScreen(vm, wide = wide)
                    } else if (vm.showScientificCalculator) {
                        ScientificCalculatorScreen(vm, wide = wide)
                    } else if (vm.showSetLogicVisualizer) {
                        SetTheoryLogicVisualizerScreen(vm, wide = wide)
                    } else if (vm.showProbabilityLab) {
                        ProbabilityLabScreen(vm, wide = wide)
                    } else if (vm.showConceptLibrary) {
                        MathsLearnAllScreen(
                            onBack = vm::returnToMathMenu,
                            initialQuery = listOfNotNull(vm.selectedMathSubConcept, vm.selectedMathConcept).firstOrNull().orEmpty(),
                            initialLessonId = vm.selectedMathLessonId,
                            mode = MathsLearnAllMode.ClassExplore,
                        )
                    } else if (vm.showMathDictionary) {
                        MathDictionaryScreen(
                            modifier = Modifier.fillMaxSize(),
                            wide = wide,
                        )
                    } else if (vm.showKnowledgeHub) {
                        MathKnowledgeScreen(vm, wide = wide)
                    } else {
                        when (vm.state.module) {
                            MathModule.Geometry2D -> Geometry2DScreen(vm, compact, onRequestClearAll = { showClearConfirmation = true })
                            MathModule.Geometry3D -> Geometry3DScreen(vm, compact, onRequestClearAll = { showClearConfirmation = true })
                            MathModule.Graph2D -> Graph2DScreen(vm, onRequestClearAll = { showClearConfirmation = true })
                            MathModule.Graph3D -> Graph3DScreen(vm)
                            MathModule.Trigonometry -> TrigonometryScreen(vm)
                            MathModule.Manipulatives -> ManipulativesScreen(vm, wide)
                            MathModule.ProbabilityStatistics -> ProbabilityLabScreen(vm, wide = wide)
                            MathModule.MatricesLinearTransformations -> MatricesLinearTransformationsWorkspace(vm)
                            MathModule.DataSpreadsheet -> DataSpreadsheetWorkspace(vm)
                            MathModule.DiscreteMathematics -> SetTheoryLogicVisualizerScreen(vm, wide = wide)
                            MathModule.NumberTheory -> NumberTheoryWorkspace(vm)
                            MathModule.SpatialAR -> SpatialARScreen(vm)
                            MathModule.ARGraph3D -> AR3DGraphScreen(
                                onBack = vm::navigateBackIntent,
                                graphEngine = remember { Existing3DGraphEngineBridge() },
                            )
                        }
                    }
                    if (vm.showLearningPanel && !vm.showLearningIntelligence && !vm.showSolver && !vm.showProblemSolver && !vm.showScientificCalculator && !vm.showMathNotebook && !vm.showProbabilityLab && !vm.showKnowledgeHub && !vm.showMathDictionary && !vm.showMathsLearnAll) LearningCoachPanel(vm, Modifier.align(Alignment.CenterEnd))
                    }
                    if (vm.showChrome && vm.state.module != MathModule.SpatialAR && vm.state.module != MathModule.ARGraph3D && !vm.showShapesExplorer && !vm.showUnifiedMathStudio && !vm.showAdaptiveMathLearning && !vm.showMathsLearnAll && !vm.showMathDictionary && !vm.showLearningIntelligence && !vm.showBiologyHub && !vm.showChemistryHub && !vm.showPhysicsHub && !vm.showMathLanding) {
                        TopShell(
                            vm,
                            compact,
                            Modifier.align(if (adaptiveProfile.isTelevision) Alignment.CenterStart else Alignment.TopCenter),
                        )
                    }
                    if (vm.showMathMenu && !vm.showMathLanding) MathematicsMenuPanel(
                        vm = vm,
                        modifier = Modifier
                            .align(
                                when (adaptiveProfile.navigationPolicy.overlayEdge) {
                                    AdaptiveOverlayEdge.Start -> Alignment.CenterStart
                                    AdaptiveOverlayEdge.End -> Alignment.CenterEnd
                                    AdaptiveOverlayEdge.Center -> Alignment.Center
                                },
                            )
                            .offset { IntOffset(menuOffset.x.roundToInt(), menuOffset.y.roundToInt()) }
                            .padding(start = if (adaptiveProfile.isTelevision) 220.dp else 0.dp)
                            .widthIn(max = adaptiveProfile.navigationPolicy.launcherWidth)
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
                if (!showSplash && !vm.showGamifyMaths && !vm.showMathLanding) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                            .padding(12.dp),
                    ) {
                        GlowButton(
                            label = "Settings",
                            icon = "settings",
                            iconOnly = true,
                            onClick = { showSettings = true },
                        )
                    }
                }
                if (!showSplash && !vm.showGamifyMaths && (vm.showMathLanding || vm.showSubjectHub)) {
                    AppCopyrightFooter(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                            .padding(
                                start = 12.dp,
                                end = 12.dp,
                                bottom = if (vm.showMathLanding) 72.dp else 8.dp,
                            ),
                    )
                }
                if (showSettings) {
                    AppSettingsDialog(
                        settings = vm.settings,
                        onSettingsChange = { next ->
                            applyAppPalette(next.colorScheme.palette)
                            vm.updateSettings { next }
                        },
                        onDismiss = { showSettings = false },
                    )
                }
                if (showExitConfirmation) {
                    Dialog(onDismissRequest = { showExitConfirmation = false }) {
                        GlassPanel(
                            Modifier
                                .adaptiveDialogWidth()
                                .widthIn(max = if (adaptiveProfile.isTelevision) 560.dp else 360.dp)
                                .adaptiveFocusGroup(),
                        ) {
                            Text("Exit Mathematics Explorer?", color = Ink, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                            Text("Your current work is autosaved. Do you want to close the app?", color = Muted, fontSize = 12.sp)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                GlowButton("No") { showExitConfirmation = false }
                                Spacer(Modifier.width(8.dp))
                                GlowButton("Yes", icon = "X") {
                                    showExitConfirmation = false
                                    hostActivity?.finish()
                                }
                            }
                        }
                    }
                }
                if (showClearConfirmation) {
                    Dialog(onDismissRequest = { showClearConfirmation = false }) {
                        GlassPanel(
                            Modifier
                                .adaptiveDialogWidth()
                                .widthIn(max = if (adaptiveProfile.isTelevision) 560.dp else 380.dp)
                                .adaptiveFocusGroup(),
                        ) {
                            Text("Clear ${vm.state.module.label} workspace?", color = Ink, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                            Text("Only objects in the current workspace will be removed. You can restore them with Undo.", color = Muted, fontSize = 12.sp)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                GlowButton("Cancel") { showClearConfirmation = false }
                                Spacer(Modifier.width(8.dp))
                                DestructiveGlowButton("Clear all", icon = "×") {
                                    showClearConfirmation = false
                                    vm.clearCurrentWorkspace()
                                }
                            }
                        }
                    }
                }
                if (!showSplash && !vm.showMathDictionary) {
                    GlobalAiAssistantOverlay(
                        vm = vm,
                        expanded = showAiAssistant,
                        onExpandedChange = { showAiAssistant = it },
                        modifier = Modifier.align(Alignment.BottomEnd),
                    )
                }
                if (showSplash) AiExplorerSplashScreen(Modifier.fillMaxSize())
            }
        }
    }
    }
    }
}

@Composable
private fun GlobalAiAssistantOverlay(
    vm: ExplorerViewModel,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val repository = remember { MathsLearnAllRepository(context) }
    val solverTutor = remember { MathSolverTutor() }
    val scope = rememberCoroutineScope()
    var prompt by rememberSaveable { mutableStateOf("") }
    var response by remember { mutableStateOf<OfflineLearningCoachResponse?>(null) }
    var results by remember { mutableStateOf<List<MathsHomeSearchResult>>(emptyList()) }
    var solverGuidance by remember { mutableStateOf<GuidedSolution?>(null) }
    var busy by remember { mutableStateOf(false) }
    var assistantOffsetX by rememberSaveable { mutableFloatStateOf(0f) }
    var assistantOffsetY by rememberSaveable { mutableFloatStateOf(0f) }

    fun shouldUseSolver(text: String): Boolean {
        val lower = text.lowercase()
        return '=' in text ||
            Regex("""\b(solve|simplify|expand|factor|differentiate|derivative|integrate|integral|limit|gcd|lcm|mean|median|mode|determinant|inverse|rref|convert|interest|combination|permutation|dy/dx)\b""").containsMatchIn(lower) ||
            Regex("""\d+\s*[+\-*/^]\s*\d+""").containsMatchIn(text)
    }

    fun runAssistant(text: String = prompt) {
        val query = text.trim()
        if (query.isBlank()) return
        prompt = query
        busy = true
        scope.launch {
            val search = runCatching { repository.homeSearch(query, limit = 8) }.getOrDefault(emptyList())
            val coach = runCatching { repository.offlineCoach(query) }.getOrNull()
            val solved = if (shouldUseSolver(query)) runCatching { solverTutor.solve(query) }.getOrNull() else null
            results = search
            response = coach
            solverGuidance = solved?.takeIf { it.solution.supported || it.interpretation.selected.intent.name != "Unknown" }
            busy = false
        }
    }

    fun openResult(result: MathsHomeSearchResult) {
        when {
            result.lessonId != null -> vm.openMathLesson(result.lessonId, result.title)
            result.kind == MathsHomeSearchKind.Concept -> vm.openConceptLibrary(result.conceptTitle ?: result.title)
            result.kind == MathsHomeSearchKind.Topic || result.kind == MathsHomeSearchKind.Chapter -> vm.openConceptLibrary(result.conceptTitle, result.query)
            else -> openMathTool(vm, result.title)
        }
        onExpandedChange(false)
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val maxDragX = with(density) { (maxWidth - 58.dp).toPx().coerceAtLeast(0f) }
        val maxDragY = with(density) { (maxHeight - 96.dp).toPx().coerceAtLeast(0f) }
        LaunchedEffect(maxDragX, maxDragY) {
            assistantOffsetX = assistantOffsetX.coerceIn(-maxDragX, 0f)
            assistantOffsetY = assistantOffsetY.coerceIn(-maxDragY, 0f)
        }
        if (expanded) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = .82f))
                    .clickable { onExpandedChange(false) },
            )
        }
        Column(
            Modifier
                .align(Alignment.BottomEnd)
                .offset { IntOffset(assistantOffsetX.roundToInt(), assistantOffsetY.roundToInt()) }
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(end = 12.dp, bottom = 14.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
        AnimatedVisibility(expanded) {
            GlassPanel(
                Modifier
                    .widthIn(min = 292.dp, max = 380.dp)
                    .heightIn(max = 520.dp)
                    .background(Color(0xFA08111F), RoundedCornerShape(18.dp)),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RobotAssistantIcon(Modifier.size(34.dp), Cyan)
                        Column {
                            Text("AI Assistant", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                            Text("Offline Genie - lessons, search, solver", color = Ink.copy(.82f), fontSize = 9.sp)
                        }
                    }
                    GlowButton("Close", icon = "X", iconOnly = true) { onExpandedChange(false) }
                }
                Text(
                    "Ask for a lesson, concept, formula, next step, or practice idea. Online LLM memory can plug into this same local learner schema later.",
                    color = Muted,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                )
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Ask anything in maths") },
                    placeholder = { Text("Example: teach quadratic equations") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { runAssistant() }),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Resume", "Solve 2x+3=11", "Factor x^2-5x+6", "Matrix inverse", "Probability").forEach { sample ->
                        GlowButton(sample, icon = "AI") {
                            if (sample == "Resume") {
                                vm.openMathsLearnAll()
                                onExpandedChange(false)
                            } else {
                                runAssistant(sample)
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                    GlowButton(if (busy) "Thinking..." else "Ask", icon = "AI", enabled = !busy && prompt.isNotBlank()) { runAssistant() }
                    GlowButton("Dictionary", icon = "A-Z") {
                        vm.openMathDictionary()
                        onExpandedChange(false)
                    }
                    GlowButton("Solver", icon = "sum") {
                        vm.openProblemSolver()
                        onExpandedChange(false)
                    }
                }
                solverGuidance?.let { guided ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.horizontalGradient(listOf(Amber.copy(.13f), Cyan.copy(.09f))))
                            .border(1.dp, Amber.copy(.34f), RoundedCornerShape(14.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Solver intelligence", color = Amber, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                                Text("${guided.interpretation.selected.intent.label} - ${guided.method.label}", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                            Text("${(guided.solution.confidence * 100).toInt()}%", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("Answer: ${guided.solution.answer}", color = Ink.copy(.90f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                        guided.solution.steps.firstOrNull()?.let { step ->
                            Text("${step.title}: ${step.explanation}", color = Muted, fontSize = 9.sp, lineHeight = 12.sp, maxLines = 3)
                        }
                        Text("Verified: ${guided.solution.verification}", color = Muted, fontSize = 9.sp, lineHeight = 12.sp, maxLines = 2)
                        if (guided.solution.warnings.isNotEmpty()) {
                            Text(guided.solution.warnings.first(), color = Amber, fontSize = 9.sp, lineHeight = 12.sp, maxLines = 2)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                            GlowButton("Open full solver", icon = "sum") {
                                vm.openProblemSolver()
                                onExpandedChange(false)
                            }
                            guided.hint()?.let { hint ->
                                GlowButton("Hint", icon = "?") {
                                    response = OfflineLearningCoachResponse(
                                        title = "Solver hint",
                                        message = hint.explanation,
                                        suggestedQuery = prompt,
                                        suggestedLessonId = null,
                                    )
                                }
                            }
                        }
                    }
                }
                response?.let { answer ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.horizontalGradient(listOf(Cyan.copy(.13f), Violet.copy(.10f))))
                            .border(1.dp, Cyan.copy(.28f), RoundedCornerShape(14.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Text(answer.title, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(answer.message, color = Ink.copy(.86f), fontSize = 10.sp, lineHeight = 13.sp)
                        answer.suggestedLessonId?.let { lessonId ->
                            GlowButton("Open suggested lesson", icon = "L") {
                                vm.openMathLesson(lessonId, answer.title)
                                onExpandedChange(false)
                            }
                        }
                    }
                }
                if (results.isNotEmpty()) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 205.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("Matches", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        results.forEach { result ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceA.copy(.74f))
                                    .border(1.dp, Cyan.copy(.18f), RoundedCornerShape(12.dp))
                                    .clickable { openResult(result) }
                                    .padding(9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(result.kind.name.take(1), color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                Column(Modifier.weight(1f)) {
                                    Text(result.title, color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    Text(result.subtitle, color = Muted, fontSize = 9.sp, maxLines = 1)
                                }
                                Text("Open", color = Violet, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
        Box(
            Modifier
                .size(if (expanded) 46.dp else 42.dp)
                .shadow(9.dp, RoundedCornerShape(16.dp), ambientColor = Cyan.copy(.35f), spotColor = Violet.copy(.42f))
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.radialGradient(listOf(Cyan.copy(.34f), Violet.copy(.20f), SurfaceA.copy(.96f))))
                .border(1.dp, Cyan.copy(.72f), RoundedCornerShape(16.dp))
                .pointerInput(maxDragX, maxDragY) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        assistantOffsetX = (assistantOffsetX + drag.x).coerceIn(-maxDragX, 0f)
                        assistantOffsetY = (assistantOffsetY + drag.y).coerceIn(-maxDragY, 0f)
                    }
                }
                .clickable { onExpandedChange(!expanded) }
                .semantics { contentDescription = if (expanded) "Close AI Assistant" else "Open AI Assistant" },
            contentAlignment = Alignment.Center,
        ) {
            RobotAssistantIcon(Modifier.size(32.dp), Cyan)
        }
    }
    }
}

@Composable
private fun RobotAssistantIcon(modifier: Modifier = Modifier, accent: Color = Cyan) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawCircle(accent.copy(.34f), w * .18f, Offset(w * .18f, h * .48f))
        drawCircle(accent.copy(.34f), w * .18f, Offset(w * .82f, h * .48f))
        drawRoundRect(
            color = Color(0xFFE9F4FF),
            topLeft = Offset(w * .18f, h * .18f),
            size = Size(w * .64f, h * .52f),
            cornerRadius = CornerRadius(w * .16f, w * .16f),
        )
        drawRoundRect(
            color = Color(0xFF15202C),
            topLeft = Offset(w * .26f, h * .27f),
            size = Size(w * .48f, h * .30f),
            cornerRadius = CornerRadius(w * .07f, w * .07f),
        )
        drawLine(accent, Offset(w * .37f, h * .43f), Offset(w * .43f, h * .38f), strokeWidth = w * .035f, cap = StrokeCap.Round)
        drawLine(accent, Offset(w * .43f, h * .38f), Offset(w * .49f, h * .43f), strokeWidth = w * .035f, cap = StrokeCap.Round)
        drawLine(accent, Offset(w * .55f, h * .43f), Offset(w * .61f, h * .38f), strokeWidth = w * .035f, cap = StrokeCap.Round)
        drawLine(accent, Offset(w * .61f, h * .38f), Offset(w * .67f, h * .43f), strokeWidth = w * .035f, cap = StrokeCap.Round)
        drawRoundRect(
            color = Color(0xFFBFD5E5),
            topLeft = Offset(w * .33f, h * .68f),
            size = Size(w * .34f, h * .20f),
            cornerRadius = CornerRadius(w * .16f, w * .16f),
        )
        drawCircle(accent, w * .055f, Offset(w * .60f, h * .76f))
    }
}

@Composable
private fun AppCopyrightFooter(modifier: Modifier = Modifier) {
    val currentYear = remember { java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) }
    Text(
        text = "Indian Servers Pvt Ltd | Copyright 2009-$currentYear | www.IndianServers.com",
        color = Ink.copy(alpha = .82f),
        fontSize = 9.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = modifier
            .background(SurfaceA.copy(alpha = .72f), RoundedCornerShape(10.dp))
            .border(1.dp, Cyan.copy(alpha = .18f), RoundedCornerShape(10.dp))
            .padding(horizontal = 9.dp, vertical = 4.dp),
    )
}

private fun openMathTool(vm: ExplorerViewModel, title: String): Boolean {
    vm.recordMathToolOpen(title)
    when (title) {
        "Learn All" -> vm.openMathsLearnAll()
        "Unified Math Studio" -> vm.openUnifiedMathStudio()
        "Adaptive Math Coach" -> vm.openAdaptiveMathLearning()
        "GamifyMaths", "Math Games" -> vm.openGamifyMaths()
        "Scientific Calculator" -> vm.openScientificCalculator()
        "Math Notebook" -> vm.openMathNotebook()
        "Solver" -> vm.openSolver()
        "Problem Solver" -> vm.openProblemSolver()
        "Math Camera" -> vm.openMathCamera()
        "Formulas" -> vm.openKnowledgeHub(KnowledgeSection.Formulas)
        "MCQs" -> vm.openKnowledgeHub(KnowledgeSection.Mcqs)
        "Formula Visualizer" -> vm.openKnowledgeHub(KnowledgeSection.Visualize)
        "Theorems" -> vm.openKnowledgeHub(KnowledgeSection.Theorems)
        "Visual Proofs" -> vm.openKnowledgeHub(KnowledgeSection.Proofs)
        "Dictionary", "Visual Dictionary" -> vm.openMathDictionary()
        "Probability & Statistics" -> vm.openProbabilityLab()
        "2D Geometry" -> vm.open(MathModule.Geometry2D)
        "3D Geometry" -> vm.open(MathModule.Geometry3D)
        "Graphs Explorer" -> vm.open(MathModule.Graph2D)
        "Manipulatives" -> vm.open(MathModule.Manipulatives)
        "Shapes Explorer" -> vm.openShapesExplorer()
        "Set Theory & Logic" -> vm.openSetLogicVisualizer()
        "Math Concepts" -> vm.openConceptLibrary()
        "Daily Challenge", "Practice Paths", "Real-World Math" -> vm.openAdaptiveMathLearning()
        "Exam Prep" -> vm.openKnowledgeHub(KnowledgeSection.Mcqs)
        "Math History" -> vm.openConceptLibrary("Number Theory")
        else -> return false
    }
    return true
}

private fun primaryHomeCategoryToolTitle(category: MathHomeCategory): String = when (category.title) {
    "Solve & Calculate" -> "Solver"
    "Visual Workspaces" -> "Explore Workspaces"
    "Data & Probability" -> "Probability & Statistics"
    "Formulas & Proofs" -> "Formulas"
    "Reference & Logic" -> "Visual Dictionary"
    "Discover More" -> "Daily Challenge"
    else -> category.toolTitles.firstOrNull().orEmpty()
}

private fun learnerDisplayName(settings: AppSettings): String = settings.learnerName.trim().ifBlank { "Learner" }

private fun personalizedHomeMessage(settings: AppSettings, recentTools: List<String>): String {
    val name = learnerDisplayName(settings)
    val classText = settings.learnerClass.trim().ifBlank { settings.learnerStandard.trim() }
    val classTopic = when {
        classText.contains("11") || classText.contains("12") -> "calculus"
        classText.contains("10") || classText.contains("9") -> "coordinate geometry"
        classText.contains("1") || classText.contains("2") -> "number bonds"
        classText.contains("3") || classText.contains("4") -> "multiplication patterns"
        classText.contains("5") || classText.contains("6") -> "fractions"
        classText.contains("7") || classText.contains("8") -> "linear equations"
        classText.contains("pg", ignoreCase = true) || classText.contains("degree", ignoreCase = true) -> "real analysis"
        else -> "fractions"
    }
    val recent = recentTools.firstOrNull()?.takeIf { it.isNotBlank() }
    val topic = recent ?: classTopic
    val thought = when ((name.length + classText.length + java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)) % 5) {
        0 -> "Did you know a graph is just a picture of changing numbers?"
        1 -> "Did you know mistakes often show exactly what to practise next?"
        2 -> "Did you know one good example can unlock a whole formula?"
        3 -> "Did you know drawing a problem can make it much easier?"
        else -> "Did you know maths becomes calmer when we solve one small step first?"
    }
    val tone = when (settings.learningComfort) {
        LearningComfort.Gentle -> "We can go slowly and use friendly examples."
        LearningComfort.Balanced -> "We can learn with clear steps and quick practice."
        LearningComfort.Advanced -> "We can add deeper ideas when you are ready."
    }
    return "Namaste $name, happy to see you again. Shall we explore $topic now? $thought $tone"
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun AppSettingsDialog(
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember(settings) { mutableStateOf(settings) }
    val scrollState = rememberScrollState()
    Dialog(onDismissRequest = onDismiss) {
        GlassPanel(
            Modifier
                .adaptiveDialogWidth()
                .widthIn(max = 620.dp)
                .adaptiveFocusGroup()
                .tvRemoteScrollable(scrollState)
                .verticalScroll(scrollState),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Settings", color = Ink, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Text("Profile, comfort and themes stay 100% offline on this device.", color = Muted, fontSize = 11.sp)
                }
                GlowButton("Close", icon = "close", iconOnly = true, onClick = onDismiss)
            }

            Text("Learner Profile", color = Cyan, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
            OutlinedTextField(
                value = draft.learnerName,
                onValueChange = { draft = draft.copy(learnerName = it.take(32)); onSettingsChange(draft) },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = draft.learnerClass,
                    onValueChange = { draft = draft.copy(learnerClass = it.take(20)); onSettingsChange(draft) },
                    label = { Text("Class") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = draft.learnerStandard,
                    onValueChange = { draft = draft.copy(learnerStandard = it.take(20)); onSettingsChange(draft) },
                    label = { Text("Standard") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }

            Text("Learning Comfort", color = Violet, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LearningComfort.entries.forEach { comfort ->
                    val selected = draft.learningComfort == comfort
                    Column(
                        Modifier
                            .widthIn(min = 142.dp, max = 190.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) Violet.copy(.20f) else SurfaceB.copy(.62f))
                            .border(1.dp, if (selected) Violet else Muted.copy(.25f), RoundedCornerShape(8.dp))
                            .clickable {
                                draft = draft.copy(learningComfort = comfort)
                                onSettingsChange(draft)
                            }
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(if (selected) "${comfort.label} selected" else comfort.label, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(comfort.description, color = Muted, fontSize = 9.sp, lineHeight = 12.sp)
                    }
                }
            }

            Text("Comfort Controls", color = Green, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
            SettingsSwitchRow("Large touch targets", "More room for small hands and relaxed tapping.", draft.largeTouchTargets) {
                draft = draft.copy(largeTouchTargets = it); onSettingsChange(draft)
            }
            SettingsSwitchRow("Reduced motion", "Calmer screens for students who prefer less animation.", draft.reducedMotion) {
                draft = draft.copy(reducedMotion = it); onSettingsChange(draft)
            }
            SettingsSwitchRow("Spoken maths", "Prepare explanations for read-aloud support.", draft.spokenMath) {
                draft = draft.copy(spokenMath = it); onSettingsChange(draft)
            }
            SettingsSwitchRow("Haptics", "Gentle touch feedback where supported.", draft.haptics) {
                draft = draft.copy(haptics = it); onSettingsChange(draft)
            }

            Text("Theme", color = Amber, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppColorScheme.entries.forEach { scheme ->
                    val active = scheme == draft.colorScheme
                    Column(
                        Modifier
                            .widthIn(min = 132.dp, max = 190.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(scheme.palette.surfaceAlt)
                            .border(if (active) 2.dp else 1.dp, if (active) scheme.palette.primary else scheme.palette.muted.copy(.28f), RoundedCornerShape(8.dp))
                            .clickable {
                                draft = draft.copy(colorScheme = scheme)
                                onSettingsChange(draft)
                            }
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            listOf(scheme.palette.primary, scheme.palette.secondary, scheme.palette.success, scheme.palette.warning).forEach { color ->
                                Box(Modifier.size(15.dp).clip(RoundedCornerShape(3.dp)).background(color))
                            }
                        }
                        Text(if (active) "${scheme.displayName} selected" else scheme.displayName, color = scheme.palette.ink, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Text(scheme.description, color = scheme.palette.muted, fontSize = 9.sp, lineHeight = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(label: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceB.copy(.45f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(description, color = Muted, fontSize = 9.sp, lineHeight = 12.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private object MathHubUi {
@Composable
fun Screen(
    vm: ExplorerViewModel,
    wide: Boolean,
    onSettingsClick: () -> Unit,
) {
    val adaptiveProfile = LocalAdaptiveDeviceProfile.current
    val navigationPolicy = adaptiveProfile.navigationPolicy
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val learningRepository = remember { MathsLearnAllRepository(context) }
    val dictionaryRepository = remember { MathDictionaryRepository(context) }
    var query by rememberSaveable { mutableStateOf("") }
    var showWorkspaces by rememberSaveable { mutableStateOf(false) }
    var showConcepts by rememberSaveable { mutableStateOf(false) }
    var selectedLearningCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedHomeCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedPreview by rememberSaveable { mutableStateOf<String?>(null) }
    var showHomeMenu by rememberSaveable { mutableStateOf(false) }
    var workspaceOpenRequest by remember { mutableIntStateOf(0) }
    val hubScrollState = rememberScrollState()
    val workspacesRequester = remember { BringIntoViewRequester() }
    val searchRequester = remember { BringIntoViewRequester() }
    val allTools = remember { (MathCreationTools + MathLearningTools + SuggestedMathTools).distinctBy { it.title } }
    val personalMessage = remember(vm.settings, vm.recentMathTools) { personalizedHomeMessage(vm.settings, vm.recentMathTools) }
    val visibleTools = remember(query) {
        val normalized = query.trim().lowercase()
        if (normalized.isBlank()) allTools
        else allTools.filter { normalized in it.title.lowercase() || normalized in it.description.lowercase() }
    }
    val visibleConcepts = remember(query) {
        if (query.isBlank()) emptyList() else MathConceptCatalog.search(query, null)
    }
    val searchSuggestions = remember(query, allTools) {
        if (query.isBlank()) emptyList() else {
            val normalized = query.trim().lowercase()
            val toolMatches = allTools.filter {
                normalized in it.title.lowercase() || normalized in it.description.lowercase()
            }.map { HomeSearchSuggestion(it.title, it.description, concept = false) }
            val conceptMatches = MathConceptCatalog.search(query, null).map {
                HomeSearchSuggestion(it.title, it.subtopics.take(3).joinToString(" - "), concept = true)
            }
            (toolMatches + conceptMatches).distinctBy { it.label }.take(5)
        }
    }
    var learningSearchResults by remember { mutableStateOf(emptyList<com.indianservers.aiexplorer.learnall.MathsHomeSearchResult>()) }
    var dictionarySearchResults by remember { mutableStateOf(emptyList<com.indianservers.aiexplorer.mathdictionary.MathDictionaryTermSummary>()) }
    LaunchedEffect(query) {
        if (query.isBlank()) {
            learningSearchResults = emptyList()
            dictionarySearchResults = emptyList()
        } else {
            learningSearchResults = runCatching { learningRepository.homeSearch(query, limit = 10) }.getOrDefault(emptyList())
            dictionarySearchResults = runCatching {
                dictionaryRepository.seedIfNeeded()
                dictionaryRepository.searchSummaries(query, firstLetter = null, category = null, difficulty = null, bookmarksOnly = false).take(6)
            }.getOrDefault(emptyList())
        }
    }
    val enhancedSearchSuggestions = remember(searchSuggestions, learningSearchResults, dictionarySearchResults) {
        val dictionaryMatches = dictionarySearchResults.map { term ->
            HomeSearchSuggestion(
                label = term.word,
                supportingText = term.shortDefinition,
                concept = false,
                dictionaryTermKey = term.termKey,
                query = term.word,
                kindLabel = "Dictionary",
            )
        }
        val learningMatches = learningSearchResults.map { result ->
            HomeSearchSuggestion(
                label = result.title,
                supportingText = result.subtitle,
                concept = result.kind == MathsHomeSearchKind.Concept,
                lessonId = result.lessonId,
                conceptTitle = result.conceptTitle,
                query = result.query,
                kindLabel = result.kind.name,
            )
        }
        (searchSuggestions + learningMatches + dictionaryMatches)
            .distinctBy { "${it.kindLabel}:${it.label}:${it.lessonId.orEmpty()}:${it.dictionaryTermKey.orEmpty()}" }
            .take(10)
    }

    fun openOption(option: MathWorkspaceOption) {
        when (option.title) {
            "Explore Workspaces" -> {
                query = ""
                selectedHomeCategory = null
                showConcepts = false
                showWorkspaces = true
                workspaceOpenRequest++
            }
            "Math Concepts" -> {
                selectedHomeCategory = null
                showWorkspaces = false
                showConcepts = true
            }
            else -> if (!openMathTool(vm, option.title)) selectedPreview = option.title
        }
    }

    LaunchedEffect(workspaceOpenRequest) {
        if (workspaceOpenRequest > 0 && showWorkspaces) {
            workspacesRequester.bringIntoView()
        }
    }

    LaunchedEffect(selectedHomeCategory) {
        if (selectedHomeCategory != null) {
            hubScrollState.scrollTo(0)
        }
    }

    @Composable
    fun MathHubCard(
        option: MathWorkspaceOption,
        accent: Color,
        status: String = "OPEN",
        modifier: Modifier = Modifier,
    ) {
        val pinned = option.title in vm.pinnedMathTools
        Column(
            modifier
                .fillMaxWidth()
                .heightIn(min = 132.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(if (selectedPreview == option.title) accent.copy(alpha = .18f) else Color(0xCC0B1420))
                .border(
                    1.dp,
                    if (selectedPreview == option.title) accent else accent.copy(alpha = .38f),
                    RoundedCornerShape(18.dp),
                )
                .adaptiveFocusRing(shape = RoundedCornerShape(18.dp), focusColor = accent)
                .clickable { openOption(option) }
                .focusable()
                .semantics { contentDescription = "Open Maths tool ${option.title}. ${option.description}" }
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TransparentIcon(option.icon, accent)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(status, color = Green, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        if (pinned) "PINNED" else "PIN",
                        color = if (pinned) Amber else Muted,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { vm.togglePinnedMathTool(option.title) }
                            .padding(horizontal = 4.dp, vertical = 3.dp)
                            .semantics { contentDescription = "${if (pinned) "Unpin" else "Pin"} ${option.title}" },
                    )
                }
            }
            Text(
                option.title,
                color = Ink,
                fontSize = if (wide) 16.sp else 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
            )
            Text(option.description, color = Muted, fontSize = 10.sp, maxLines = 3)
        }
    }

    Box(Modifier.fillMaxSize()) {
    Column(
        Modifier
            .fillMaxSize()
            .widthIn(max = navigationPolicy.maximumContentWidth)
            .align(Alignment.TopCenter)
            .adaptiveFocusGroup()
            .tvRemoteScrollable(hubScrollState)
            .verticalScroll(hubScrollState)
            .padding(horizontal = if (wide) 34.dp else 8.dp, vertical = 12.dp)
            .padding(bottom = 104.dp)
            .semantics { contentDescription = "Mathematics Explorer menu" },
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TransparentIcon("SUM", Cyan)
                Column(Modifier.weight(1f)) {
                    Text("Namaskar ${learnerDisplayName(vm.settings)},", color = Muted, fontSize = 8.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text("Mathematics", color = Ink, fontSize = if (wide) 27.sp else 21.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Explorer", color = Cyan, fontSize = if (wide) 27.sp else 21.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Explore  -  Learn  -  Master", color = Muted, fontSize = 9.sp, maxLines = 1)
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                GlowButton(
                    label = "Home",
                    icon = "H",
                    iconOnly = !wide,
                    onClick = vm::openSubjectHub,
                )
                GlowButton(
                    label = "Menu",
                    icon = "settings",
                    iconOnly = true,
                    onClick = { showHomeMenu = !showHomeMenu },
                )
            }
        }

        AnimatedVisibility(showHomeMenu) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceA.copy(.90f))
                    .border(1.dp, Cyan.copy(.30f), RoundedCornerShape(12.dp))
                    .padding(7.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlowButton("Settings", icon = "settings") {
                    showHomeMenu = false
                    onSettingsClick()
                }
                GlowButton("Adaptive", icon = "AI") {
                    showHomeMenu = false
                    vm.openAdaptiveMathLearning()
                }
                GlowButton("Learn All", icon = "All") {
                    showHomeMenu = false
                    vm.openMathsLearnAll()
                }
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.horizontalGradient(listOf(Cyan.copy(.12f), Violet.copy(.08f), Color.Transparent)))
                .border(1.dp, Cyan.copy(.20f), RoundedCornerShape(14.dp))
                .padding(horizontal = 11.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(personalMessage, color = Ink.copy(.92f), fontSize = 10.sp, lineHeight = 13.sp)
            Text(
                "Local profile: ${vm.settings.learningComfort.label}${vm.settings.learnerClass.trim().takeIf { it.isNotBlank() }?.let { " | Class $it" }.orEmpty()}",
                color = Muted,
                fontSize = 8.sp,
                maxLines = 1,
            )
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search Mathematics") },
            leadingIcon = { Text("⌕", color = Violet, fontSize = 25.sp, fontWeight = FontWeight.Bold) },
            trailingIcon = { Text("≋", color = Cyan, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
                .bringIntoViewRequester(searchRequester)
                .semantics { contentDescription = "Global mathematics search for lessons, concepts, dictionary and tools" },
        )
        HomeSearchSuggestions(
            query = query,
            suggestions = enhancedSearchSuggestions,
            onSample = { query = it },
            onSuggestion = { suggestion ->
                when {
                    suggestion.dictionaryTermKey != null -> vm.openMathDictionary()
                    suggestion.lessonId != null -> vm.openMathLesson(suggestion.lessonId, suggestion.label)
                    suggestion.concept -> vm.openConceptLibrary(suggestion.conceptTitle ?: suggestion.label)
                    suggestion.kindLabel == MathsHomeSearchKind.Topic.name || suggestion.kindLabel == MathsHomeSearchKind.Chapter.name -> vm.openConceptLibrary(suggestion.conceptTitle, suggestion.query)
                    else -> allTools.firstOrNull { it.title == suggestion.label }?.let(::openOption) ?: run { query = suggestion.query }
                }
            },
        )

        if (query.isBlank()) {
            val gamifyCategory = MathHomeCategories.first()
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = if (wide) 124.dp else 104.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Violet.copy(.42f), Cyan.copy(.17f), Color(0xED091426)),
                        ),
                    )
                    .border(1.dp, Violet.copy(.78f), RoundedCornerShape(24.dp))
                    .adaptiveFocusRing(shape = RoundedCornerShape(24.dp), focusColor = Violet)
                    .clickable { openOption(MathLearningTools.first { it.title == "GamifyMaths" }) }
                    .focusable()
                    .semantics { contentDescription = "Play GamifyMaths games and speed challenges" }
                    .padding(horizontal = 15.dp, vertical = 13.dp),
                horizontalArrangement = Arrangement.spacedBy(13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(if (wide) 72.dp else 64.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.radialGradient(listOf(Violet, Violet.copy(.38f))))
                        .border(1.dp, Color.White.copy(.38f), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    MathHomeArtwork.Draw("GamifyMaths", Color.White, Modifier.fillMaxSize().padding(7.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("GAMIFYMATHS", color = Ink, fontSize = if (wide) 22.sp else 18.sp, fontWeight = FontWeight.ExtraBold)
                    Text(gamifyCategory.description, color = Muted, fontSize = 10.sp, maxLines = 2)
                    Text("NEW - SPEED CALCULATION", color = Green, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
                }
                Text("PLAY  >", color = Green, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
            }

            SolverHomeLaunch(
                wide = wide,
                onClick = { openOption(allTools.first { it.title == "Solver" }) },
            )

            if (selectedHomeCategory == null && !showWorkspaces) {
            Text("QUICK EXPLORE", color = Green, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                MathQuickLaunchButton("2D", "2D", Cyan, Modifier.weight(1f)) { vm.open(MathModule.Geometry2D) }
                MathQuickLaunchButton("3D", "3D", Violet, Modifier.weight(1f)) { vm.open(MathModule.Geometry3D) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                MathQuickLaunchButton("Graphs", "↗", Amber, Modifier.weight(1f)) { vm.open(MathModule.Graph2D) }
                MathQuickLaunchButton("3D Graph", "xyz", Cyan, Modifier.weight(1f)) { vm.open(MathModule.Graph3D) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                MathQuickLaunchButton("AR 3D Graph", "AR", Green, Modifier.weight(1f)) { vm.open(MathModule.ARGraph3D) }
                MathQuickLaunchButton("Trigonometry", "θ", Green, Modifier.weight(1f)) { vm.open(MathModule.Trigonometry) }
                MathQuickLaunchButton("Solver", "Fx", Violet, Modifier.weight(1f)) { vm.openSolver() }
            }
            val conceptsAccent = Color(0xFFFF67A6)
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 66.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(conceptsAccent.copy(if (showConcepts) .32f else .18f), Violet.copy(.14f), Color(0xE60A1323)),
                        ),
                    )
                    .border(1.dp, conceptsAccent.copy(if (showConcepts) .92f else .58f), RoundedCornerShape(18.dp))
                    .adaptiveFocusRing(shape = RoundedCornerShape(18.dp), focusColor = conceptsAccent)
                    .clickable {
                        showConcepts = !showConcepts
                        selectedHomeCategory = null
                        showWorkspaces = false
                    }
                    .focusable()
                    .semantics {
                        contentDescription = "${if (showConcepts) "Collapse" else "Expand"} Math Concepts with ${MathConceptCatalog.concepts.size} subjects"
                    }
                    .padding(horizontal = 13.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(13.dp))
                        .background(conceptsAccent.copy(.2f))
                        .border(1.dp, conceptsAccent.copy(.7f), RoundedCornerShape(13.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Fx", color = conceptsAccent, fontSize = 17.sp, fontWeight = FontWeight.Black)
                }
                Column(Modifier.weight(1f)) {
                    Text("MATH CONCEPTS", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                    Text("${MathConceptCatalog.concepts.size} subjects - Class 1 to PhD", color = Muted, fontSize = 9.sp)
                }
                Text(if (showConcepts) "COLLAPSE  ▲" else "EXPAND  ▼", color = conceptsAccent, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
            }

            AnimatedVisibility(showConcepts) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(19.dp))
                        .background(Brush.verticalGradient(listOf(conceptsAccent.copy(.11f), Color(0xE6091222))))
                        .border(1.dp, conceptsAccent.copy(.42f), RoundedCornerShape(19.dp))
                        .padding(9.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("EXPLORE ALL CONCEPTS", color = conceptsAccent, fontSize = 14.sp, fontWeight = FontWeight.Black)
                            Text("Choose a subject or jump directly into a topic", color = Muted, fontSize = 10.sp)
                        }
                        Text("${MathConceptCatalog.concepts.size}", color = conceptsAccent, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    }
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        val conceptColumns = when {
                            adaptiveProfile.isTelevision -> navigationPolicy.hubColumnCount
                            maxWidth >= 900.dp -> 5
                            maxWidth >= 720.dp -> 4
                            maxWidth >= 520.dp -> 3
                            maxWidth >= 340.dp -> 2
                            else -> 1
                        }
                        val conceptGap = 7.dp
                        FlowRow(
                            Modifier.fillMaxWidth(),
                            maxItemsInEachRow = conceptColumns,
                            horizontalArrangement = Arrangement.spacedBy(conceptGap),
                            verticalArrangement = Arrangement.spacedBy(conceptGap),
                        ) {
                            MathConceptCatalog.concepts.forEach { concept ->
                                val isTrig = concept.title == "Trigonometry"
                                val conceptAccent = if (isTrig) Color(0xFFFFC23E) else when (concept.title.hashCode().ushr(1) % 4) {
                                    0 -> Cyan
                                    1 -> Violet
                                    2 -> Green
                                    else -> conceptsAccent
                                }
                                Column(
                                    Modifier
                                        .weight(1f)
                                        .heightIn(min = 164.dp)
                                        .shadow(
                                            elevation = if (isTrig) 10.dp else 5.dp,
                                            shape = RoundedCornerShape(19.dp),
                                            ambientColor = conceptAccent.copy(.24f),
                                            spotColor = conceptAccent.copy(.34f),
                                        )
                                        .clip(RoundedCornerShape(19.dp))
                                        .background(
                                            if (isTrig) {
                                                Brush.linearGradient(
                                                    listOf(Color(0x70FF9D00), Color(0x3D6729A5), Color(0xF20A1323)),
                                                )
                                            } else {
                                                Brush.linearGradient(
                                                    listOf(conceptAccent.copy(.24f), Violet.copy(.10f), Color(0xF20A1323)),
                                                )
                                            },
                                        )
                                        .border(
                                            if (isTrig) 2.dp else 1.dp,
                                            conceptAccent.copy(if (isTrig) .95f else .58f),
                                            RoundedCornerShape(19.dp),
                                        )
                                        .adaptiveFocusRing(shape = RoundedCornerShape(19.dp), focusColor = conceptAccent)
                                        .clickable { vm.openConceptLibrary(concept.title) }
                                        .focusable()
                                        .semantics {
                                            contentDescription = "${if (isTrig) "Featured " else ""}${concept.title}. ${concept.summary}"
                                        }
                                        .padding(11.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                                    ) {
                                        MathConceptIconImage(concept.title, Modifier.size(44.dp), 14.dp)
                                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            if (isTrig) {
                                                Text("★ FEATURED", color = conceptAccent, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
                                            }
                                            Text(
                                                concept.title,
                                                color = Color.White,
                                                fontSize = if (isTrig) 16.sp else 14.sp,
                                                lineHeight = if (isTrig) 18.sp else 16.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                maxLines = 2,
                                            )
                                            Text(
                                                "${concept.subtopics.size} TOPICS",
                                                color = conceptAccent,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                            )
                                        }
                                    }
                                    Text(
                                        concept.summary,
                                        color = Ink.copy(.82f),
                                        fontSize = 9.sp,
                                        lineHeight = 12.sp,
                                        maxLines = 2,
                                    )
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(conceptAccent.copy(.25f)),
                                    )
                                    concept.subtopics.forEach { subConcept ->
                                        Row(
                                            Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { vm.openConceptLibrary(concept.title, subConcept) }
                                                .semantics { contentDescription = "Open ${concept.title}, $subConcept lesson" }
                                                .padding(horizontal = 5.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                                        ) {
                                            Text("›", color = conceptAccent, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                            Text(
                                                subConcept,
                                                color = Ink.copy(.88f),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                modifier = Modifier.weight(1f),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            AnimatedVisibility(!showConcepts) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                MathQuickLaunchButton("Formulas", "Fx", Violet, Modifier.weight(1f)) { vm.openKnowledgeHub(KnowledgeSection.Formulas) }
                MathQuickLaunchButton("Visual Proofs", "QED", Green, Modifier.weight(1f)) { vm.openKnowledgeHub(KnowledgeSection.Proofs) }
                MathQuickLaunchButton("Theorems", "Thm", Amber, Modifier.weight(1f)) { vm.openKnowledgeHub(KnowledgeSection.Theorems) }
            }

            Text("EXPLORE MATHEMATICS", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val columns = when {
                    adaptiveProfile.isTelevision -> navigationPolicy.hubColumnCount
                    maxWidth >= 900.dp -> 5
                    maxWidth >= 720.dp -> 4
                    maxWidth >= 520.dp -> 3
                    maxWidth >= 340.dp -> 2
                    else -> 1
                }
                val gap = 9.dp
                val categories = MathHomeCategories.drop(1).filterNot { it.title == "Learn & Practise" }
                FlowRow(
                    Modifier.fillMaxWidth(),
                    maxItemsInEachRow = columns,
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    verticalArrangement = Arrangement.spacedBy(gap),
                ) {
                    categories.forEachIndexed { index, category ->
                        val accent = when (category.title) {
                            "Solve & Calculate" -> Cyan
                            "Visual Workspaces" -> Green
                            "Data & Probability" -> Amber
                            "Formulas & Proofs" -> Color(0xFFFF67A6)
                            "Reference & Logic" -> Color(0xFF45DDCD)
                            else -> Color(0xFFFFA65C)
                        }
                        val selected = selectedHomeCategory == category.title
                        val primaryToolTitle = primaryHomeCategoryToolTitle(category)
                        Column(
                            Modifier
                                .weight(1f)
                                .heightIn(min = 154.dp)
                                .clip(RoundedCornerShape(19.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(accent.copy(if (selected) .3f else .16f), Color(0xE60A1323)),
                                    ),
                                )
                                .border(1.dp, accent.copy(if (selected) .95f else .52f), RoundedCornerShape(19.dp))
                                .adaptiveFocusRing(shape = RoundedCornerShape(19.dp), focusColor = accent)
                                .clickable {
                                    showConcepts = false
                                    selectedHomeCategory = null
                                    allTools.firstOrNull { it.title == primaryToolTitle }?.let(::openOption)
                                }
                                .focusable()
                                .semantics { contentDescription = "Open ${category.title}. ${category.description}" }
                                .padding(11.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Box(Modifier.fillMaxWidth().height(58.dp)) {
                                TransparentIcon(category.icon, accent)
                                MathHomeArtwork.Draw(
                                    category = category.title,
                                    accent = accent,
                                    modifier = Modifier.align(Alignment.CenterEnd),
                                )
                                Text(
                                    if (primaryToolTitle == "Explore Workspaces") "CHOOSE" else "OPEN",
                                    color = accent,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.align(Alignment.TopEnd),
                                )
                            }
                            Text(category.title, color = Ink, fontSize = if (wide) 15.sp else 13.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2)
                            Text(category.description, color = Muted, fontSize = 9.sp, maxLines = 2)
                        }
                    }
                }
            }
            }
            }
            }

            selectedHomeCategory?.let { selectedTitle ->
                val category = MathHomeCategories.firstOrNull { it.title == selectedTitle }
                val tools = category?.toolTitles.orEmpty().mapNotNull { title -> allTools.firstOrNull { it.title == title } }
                if (category != null && tools.isNotEmpty()) {
                    val accent = when (category.title) {
                        "Solve & Calculate" -> Cyan
                        "Visual Workspaces" -> Green
                        "Data & Probability" -> Amber
                        "Learn & Practise" -> Violet
                        "Formulas & Proofs" -> Color(0xFFFF67A6)
                        "Reference & Logic" -> Color(0xFF45DDCD)
                        else -> Color(0xFFFFA65C)
                    }
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Brush.verticalGradient(listOf(accent.copy(.12f), Color(0xD90A1323))))
                            .border(1.dp, accent.copy(.48f), RoundedCornerShape(20.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "‹  ALL CATEGORIES",
                                color = accent,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { selectedHomeCategory = null }
                                    .padding(horizontal = 7.dp, vertical = 5.dp),
                            )
                            Column(Modifier.weight(1f)) {
                                Text(category.title.uppercase(), color = accent, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                                Text("${tools.size} tools", color = Muted, fontSize = 9.sp)
                            }
                        }
                        tools.forEach { option ->
                            val pinned = option.title in vm.pinnedMathTools
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White.copy(.045f))
                                    .clickable { openOption(option) }
                                    .focusable()
                                    .padding(horizontal = 10.dp, vertical = 9.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                TransparentIcon(option.icon, accent)
                                Column(Modifier.weight(1f)) {
                                    Text(option.title, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text(option.description, color = Muted, fontSize = 9.sp, maxLines = 2)
                                }
                                Text(
                                    if (pinned) "PINNED" else "OPEN  >",
                                    color = if (pinned) Amber else Green,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                )
                            }
                        }
                    }
                }
            }
        }

        val creation = MathCreationTools.filter { it in visibleTools && it.title != "Unified Math Studio" }
        if (creation.isNotEmpty() && query.isNotBlank()) {
            Text("CREATE & SOLVE", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val columns = if (maxWidth >= 900.dp) 4 else if (maxWidth >= 600.dp) 3 else if (maxWidth >= 340.dp) 2 else 1
                FlowRow(
                    Modifier.fillMaxWidth(),
                    maxItemsInEachRow = columns,
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    creation.forEach { MathHubCard(it, Cyan, modifier = Modifier.weight(1f)) }
                }
            }
        }

        AnimatedVisibility(showWorkspaces && query.isBlank()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(workspacesRequester)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Cyan.copy(alpha = .06f))
                    .border(1.dp, Cyan.copy(alpha = .34f), RoundedCornerShape(18.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Explore Workspaces", color = Cyan, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                        Text("Choose an interactive Maths workspace", color = Muted, fontSize = 11.sp)
                    }
                    GlowButton("Collapse", icon = "collapse", iconOnly = true) { showWorkspaces = false }
                }
                Text(
                    "Your objects remain directly editable. You can switch workspace later from the bottom navigation.",
                    color = Ink,
                    fontSize = 11.sp,
                )
                val destinations = listOf(
                    Triple(MathModule.Geometry2D, "2D Geometry", "Construct and drag points, lines, circles, polygons and constraints"),
                    Triple(MathModule.Geometry3D, "3D Geometry", "Create and manipulate solids, vectors, sections and measurements"),
                    Triple(MathModule.Graph2D, "Graph", "Plot explicit, implicit, polar, parametric and inequality graphs"),
                    Triple(MathModule.Graph3D, "3D Graph", "Explore explicit, implicit and parametric surfaces"),
                    Triple(MathModule.ARGraph3D, "AR 3D Graph", "Check AR support and prepare a safe camera session for future 3D graph placement"),
                    Triple(MathModule.Trigonometry, "Trigonometry", "Use unit circles, identities, triangles and transformations"),
                    Triple(MathModule.Manipulatives, "Math Tiles", "Learn with algebra tiles, fractions, balances and tactile models"),
                    Triple(MathModule.ProbabilityStatistics, "Probability & Statistics Lab", "Simulate experiments, explore distributions and analyse samples"),
                    Triple(MathModule.MatricesLinearTransformations, "Matrices & Linear Transformations", "Edit matrices and see their geometric action on vectors and shapes"),
                    Triple(MathModule.DataSpreadsheet, "Data Table & Spreadsheet", "Calculate with linked cells, tables, summaries and graph-ready series"),
                    Triple(MathModule.DiscreteMathematics, "Discrete Mathematics Lab", "Explore sets, logic, relations, graphs and combinatorics"),
                    Triple(MathModule.NumberTheory, "Number Theory Lab", "Investigate primes, factors, divisibility, gcd and modular arithmetic"),
                    Triple(MathModule.SpatialAR, "Spatial AR", "Place existing mathematical constructions into augmented reality"),
                )
                FlowRow(
                    Modifier.fillMaxWidth(),
                    maxItemsInEachRow = if (wide) 4 else 2,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    destinations.forEachIndexed { index, (module, title, description) ->
                        val accent = listOf(Cyan, Violet, Green, Amber)[index % 4]
                        Column(
                            Modifier
                                .weight(1f)
                                .heightIn(min = 116.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(accent.copy(alpha = .09f))
                                .border(1.dp, accent.copy(alpha = .45f), RoundedCornerShape(14.dp))
                                .clickable { vm.open(module) }
                                .focusable()
                                .semantics { contentDescription = "Open $title workspace. $description" }
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                TransparentIcon(visualModuleIcon(module), accent)
                                Text("OPEN", color = Green, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                            }
                            Text(title, color = accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(description, color = Muted, fontSize = 9.sp, maxLines = 3)
                        }
                    }
                }
            }
        }

        val learning = MathLearningTools.filter { it in visibleTools }
        if (learning.isNotEmpty() && query.isNotBlank()) {
            Text("LEARN & PRACTISE", color = Violet, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
            if (query.isNotBlank()) {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val columns = if (maxWidth >= 900.dp) 4 else if (maxWidth >= 600.dp) 3 else if (maxWidth >= 340.dp) 2 else 1
                    FlowRow(
                        Modifier.fillMaxWidth(),
                        maxItemsInEachRow = columns,
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        learning.forEach { MathHubCard(it, Violet, modifier = Modifier.weight(1f)) }
                    }
                }
            } else {
                val gamify = MathLearningTools.first { it.title == "GamifyMaths" }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 104.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.horizontalGradient(listOf(Violet.copy(.34f), Cyan.copy(.15f), Color(0xCC111A31))))
                        .border(1.dp, Violet.copy(.72f), RoundedCornerShape(20.dp))
                        .clickable { openOption(gamify) }
                        .semantics { contentDescription = "Open GamifyMaths games" }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TransparentIcon("PLAY", Violet)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("GAMIFYMATHS", color = Ink, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Play interactive worlds, timed challenges and mastery missions", color = Muted, fontSize = 10.sp, maxLines = 2)
                    }
                    Text("PLAY  >", color = Green, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                }

                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val columns = if (maxWidth >= 900.dp) 4 else if (maxWidth >= 600.dp) 3 else if (maxWidth >= 340.dp) 2 else 1
                    FlowRow(
                        Modifier.fillMaxWidth(),
                        maxItemsInEachRow = columns,
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        MathLearningCategories.forEach { category ->
                            val selected = selectedLearningCategory == category.title
                            Column(
                                Modifier
                                    .weight(1f)
                                    .heightIn(min = 106.dp)
                                    .clip(RoundedCornerShape(17.dp))
                                    .background(if (selected) Violet.copy(.2f) else Color(0xCC0B1420))
                                    .border(1.dp, if (selected) Violet else Violet.copy(.38f), RoundedCornerShape(17.dp))
                                    .clickable { selectedLearningCategory = if (selected) null else category.title }
                                    .padding(11.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    TransparentIcon(category.icon, Violet)
                                    Text(if (selected) "CLOSE" else "OPEN", color = Green, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
                                }
                                Text(category.title, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                                Text(category.description, color = Muted, fontSize = 9.sp, maxLines = 2)
                            }
                        }
                    }
                }

                selectedLearningCategory?.let { selectedTitle ->
                    val category = MathLearningCategories.firstOrNull { it.title == selectedTitle }
                    val categoryTools = category?.toolTitles.orEmpty().mapNotNull { title ->
                        MathLearningTools.firstOrNull { it.title == title }
                    }
                    if (category != null && categoryTools.isNotEmpty()) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(Violet.copy(.07f))
                                .border(1.dp, Violet.copy(.42f), RoundedCornerShape(18.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(category.title.uppercase(), color = Violet, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                            categoryTools.forEach { option ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(13.dp))
                                        .background(Color.White.copy(.04f))
                                        .clickable { openOption(option) }
                                        .padding(horizontal = 11.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    TransparentIcon(option.icon, Violet)
                                    Column(Modifier.weight(1f)) {
                                        Text(option.title, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text(option.description, color = Muted, fontSize = 9.sp, maxLines = 2)
                                    }
                                    Text("OPEN >", color = Green, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (query.isNotBlank()) {
            if (visibleConcepts.isNotEmpty()) {
                Text("MATH CONCEPTS", color = Violet, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                visibleConcepts.forEach { concept ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Violet.copy(.08f))
                            .border(1.dp, Violet.copy(.35f), RoundedCornerShape(14.dp))
                            .clickable { vm.openConceptLibrary(concept.title) }.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        MathConceptIconImage(concept.title, Modifier.size(32.dp), 10.dp)
                        Column(Modifier.weight(1f)) {
                            Text(concept.title, color = Ink, fontWeight = FontWeight.Bold)
                            Text(concept.subtopics.joinToString(" - "), color = Muted, fontSize = 9.sp, maxLines = 2)
                        }
                        Text("OPEN >", color = Green, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        val suggested = SuggestedMathTools.filter { it in visibleTools }
        if (suggested.isNotEmpty() && query.isNotBlank()) {
            Text("DISCOVER MORE", color = Amber, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val columns = if (maxWidth >= 900.dp) 4 else if (maxWidth >= 600.dp) 3 else if (maxWidth >= 340.dp) 2 else 1
                FlowRow(
                    Modifier.fillMaxWidth(),
                    maxItemsInEachRow = columns,
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    suggested.forEach { MathHubCard(it, Amber, status = "PREVIEW", modifier = Modifier.weight(1f)) }
                }
            }
        }

        if (visibleTools.isEmpty() && visibleConcepts.isEmpty()) {
            Text("No Mathematics tools match this search.", color = Amber, fontWeight = FontWeight.SemiBold)
        }
    }
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xD9122350), Color(0xE00A1028), Color(0xD91C1245))))
                .border(1.dp, Violet.copy(.55f), RoundedCornerShape(22.dp))
                .padding(horizontal = 4.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MathHomeNavItem("⌂", "Home", true, Cyan) {
                selectedHomeCategory = null
                showWorkspaces = false
                showConcepts = false
            }
            MathHomeNavItem("▤", "Learn", false, Violet) {
                vm.openMathsLearnAll()
            }
            MathHomeNavItem("◇", "Explore", false, Violet) {
                vm.openConceptLibrary()
            }
            MathHomeNavItem("S", "Search", false, Cyan) {
                selectedHomeCategory = null
                showWorkspaces = false
                showConcepts = false
                query = ""
                scope.launch { searchRequester.bringIntoView() }
            }
        }
    }
}
}

@Composable
private fun SetTheoryLogicVisualizerScreen(vm: ExplorerViewModel, wide: Boolean) {
    var setMode by remember { mutableStateOf(true) }
    var selectedSetLaw by remember { mutableStateOf(SetLogicCatalog.setLaws.first()) }
    var selectedLogicLaw by remember { mutableStateOf(SetLogicCatalog.logicLaws.first()) }
    var values by remember { mutableStateOf(mapOf("A" to false, "B" to false, "C" to false, "P" to false, "Q" to false, "R" to false)) }
    var formulasOpen by remember { mutableStateOf(true) }
    var visualOpen by remember { mutableStateOf(false) }
    var tableOpen by remember { mutableStateOf(false) }
    var explanationOpen by remember { mutableStateOf(false) }
    var overlap by remember { mutableFloatStateOf(.48f) }
    var selectedPoint by remember { mutableStateOf<Offset?>(null) }
    val law = if (setMode) selectedSetLaw else selectedLogicLaw
    val live = remember(law, values) { SetLogicEngine.evaluate(law, values) }
    val rows = remember(law) { SetLogicEngine.rows(law) }

    if (setMode) {
        SetTheoryInteractiveModule(vm = vm, wide = wide, onOpenLogic = { setMode = false })
        return
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = if (wide) 26.dp else 6.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            GlowButton("Back", icon = "←", iconOnly = true, onClick = vm::returnToMathMenu)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("SET THEORY & LOGIC", color = Cyan, fontSize = if (wide) 22.sp else 17.sp, fontWeight = FontWeight.Bold)
                Text("Interactive Formula Visualizer", color = Muted, fontSize = 10.sp)
            }
            TransparentIcon("∪∧", Violet)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlowButton(if (setMode) "• Set Theory" else "Set Theory", icon = "∪") { setMode = true; formulasOpen = true; visualOpen = false; selectedPoint = null }
            GlowButton(if (!setMode) "• Logic" else "Logic", icon = "∧") { setMode = false; formulasOpen = true; visualOpen = false }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(law.title, color = Violet, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                MathFormulaText(law.formula, color = Ink, fontSize = 16.sp)
            }
            GlowButton(if (formulasOpen) "Close formulas" else "Open formulas") { formulasOpen = !formulasOpen }
        }
        AnimatedVisibility(formulasOpen) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                (if (setMode) SetLogicCatalog.setLaws else SetLogicCatalog.logicLaws).forEach { option ->
                    val formulaLabel = if (setMode) SetLogicCatalog.shortSetFormula(option) else option.formula
                    GlowButton(if (option.id == law.id && visualOpen) "• $formulaLabel" else formulaLabel) {
                        if (setMode) selectedSetLaw = option else selectedLogicLaw = option
                        formulasOpen = false
                        visualOpen = true
                        tableOpen = false
                        selectedPoint = null
                    }
                }
            }
        }
        if (setMode) {
            AnimatedVisibility(visualOpen) {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Diagram for ${SetLogicCatalog.shortSetFormula(law)}", color = Cyan, fontWeight = FontWeight.Bold)
                        GlowButton("Close diagram") { visualOpen = false; formulasOpen = true }
                    }
                    SetFormulaVennCanvas(law, overlap, selectedPoint) { point, memberships ->
                        selectedPoint = point
                        values = values + memberships
                    }
                    FlowRow(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        SetDiagramLegendChip("A", SetDiagramAColor)
                        SetDiagramLegendChip("B", SetDiagramBColor)
                        SetDiagramLegendChip("A ∪ B  (A U B)", SetDiagramUnionColor)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Overlap", color = Muted, fontSize = 11.sp, modifier = Modifier.width(70.dp))
                        Slider(overlap, { overlap = it }, valueRange = .05f..1f, modifier = Modifier.weight(1f))
                    }
                    Text("Tap anywhere in the universe to test that element's membership.", color = Muted, fontSize = 11.sp)
                }
            }
            if (!visualOpen) {
                Text("Select a formula such as A ∪ B to open its interactive diagram.", color = Green, fontSize = 12.sp)
            }
            SetTheoryLearningStudioPanel(law)
        } else {
            AnimatedVisibility(visualOpen) { LogicGateVisualizer(law, values, live.left) }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            law.variables.forEach { variable ->
                TogglePill("$variable = ${if (values[variable] == true) "TRUE" else "FALSE"}", values[variable] == true) {
                    values = values + (variable to !(values[variable] == true))
                    selectedPoint = null
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (live.equivalent) Green.copy(.09f) else Amber.copy(.1f))
                .border(1.dp, if (live.equivalent) Green.copy(.55f) else Amber, RoundedCornerShape(16.dp)).padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(law.leftLabel, color = Cyan, fontSize = 11.sp)
                Text(if (live.left) "TRUE" else "FALSE", color = if (live.left) Green else Muted, fontWeight = FontWeight.Bold)
            }
            Text(if (live.equivalent) "=" else "≠", color = if (live.equivalent) Green else Amber, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Column(horizontalAlignment = Alignment.End) {
                Text(law.rightLabel, color = Violet, fontSize = 11.sp)
                Text(if (live.right) "TRUE" else "FALSE", color = if (live.right) Green else Muted, fontWeight = FontWeight.Bold)
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            GlowButton(if (tableOpen) "Close table" else "Open full table") { tableOpen = !tableOpen }
            GlowButton(if (explanationOpen) "Close explanation" else "Why it works") { explanationOpen = !explanationOpen }
        }
        AnimatedVisibility(explanationOpen) {
            KnowledgeCard("Why it works", law.explanation, if (SetLogicEngine.verified(law)) "Verified for all ${rows.size} cases" else "Counterexample found", "Change the inputs and compare both sides.", Green)
        }
        AnimatedVisibility(tableOpen) { BooleanFormulaTable(law, rows, values) }
    }
}

@Composable
private fun SetTheoryLearningStudioPanel(activeLaw: BooleanFormulaLaw) {
    val context = LocalContext.current
    var studioOpen by remember { mutableStateOf(false) }
    var conceptsOpen by remember { mutableStateOf(false) }
    var activeTool by remember { mutableStateOf(SetStudioTool.Elements) }
    var toolMenuOpen by remember { mutableStateOf(false) }
    var conceptQuery by remember { mutableStateOf("") }
    var selectedConceptId by remember { mutableStateOf(SetTheoryLearningCatalog.concepts.first().id) }
    var favourites by remember { mutableStateOf<Set<String>>(emptySet()) }
    var recent by remember { mutableStateOf<List<String>>(emptyList()) }
    var inputA by remember { mutableStateOf("1, 2, 3") }
    var inputB by remember { mutableStateOf("2, 3, 4") }
    var universeName by remember { mutableStateOf("U") }
    var setAName by remember { mutableStateOf("A") }
    var setBName by remember { mutableStateOf("B") }
    var relation by remember { mutableStateOf(setOf("1" to "1", "2" to "2", "3" to "3")) }
    var relationMatrix by remember { mutableStateOf(false) }
    var undo by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var redo by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var saved by remember { mutableStateOf<List<String>>(emptyList()) }
    var sharePreview by remember { mutableStateOf<String?>(null) }
    var difficulty by remember { mutableIntStateOf(1) }
    var guided by remember { mutableStateOf(true) }
    var prediction by remember { mutableStateOf<Boolean?>(null) }
    var reveal by remember { mutableStateOf(false) }
    var highContrast by remember { mutableStateOf(false) }
    var spokenMath by remember { mutableStateOf(false) }
    var eulerMode by remember { mutableStateOf(false) }
    var setCount by remember { mutableIntStateOf(2) }
    val a = remember(inputA) { SetTheoryStudioEngine.parseElements(inputA) }
    val b = remember(inputB) { SetTheoryStudioEngine.parseElements(inputB) }
    val domain = a.toSet()
    fun updateInputs(nextA: String = inputA, nextB: String = inputB) {
        undo = undo + (inputA to inputB); redo = emptyList(); inputA = nextA; inputB = nextB
    }
    fun chooseConcept(id: String) { selectedConceptId = id; recent = (listOf(id) + recent.filterNot { it == id }).take(6) }

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0x330B1C2D)).border(1.dp, Violet.copy(.4f), RoundedCornerShape(16.dp)).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Set Theory Learning Studio", color = Violet, fontWeight = FontWeight.Bold)
                Text("50 concepts - 50 interactive learning features", color = Muted, fontSize = 10.sp)
            }
            GlowButton(if (studioOpen) "Close studio" else "Open studio") { studioOpen = !studioOpen }
        }
        AnimatedVisibility(studioOpen) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(activeTool.label, color = Cyan, fontWeight = FontWeight.Bold)
                    GlowButton(if (toolMenuOpen) "Hide tool list" else "Choose tool") { toolMenuOpen = !toolMenuOpen }
                }
                AnimatedVisibility(toolMenuOpen) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        SetStudioTool.entries.forEach { tool -> GlowButton(if (activeTool == tool) "• ${tool.label}" else tool.label) { activeTool = tool; toolMenuOpen = false } }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Concept Library", color = Green, fontWeight = FontWeight.Bold)
                    GlowButton(if (conceptsOpen) "Close concepts" else "Open concepts") { conceptsOpen = !conceptsOpen }
                }
                AnimatedVisibility(conceptsOpen) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(conceptQuery, { conceptQuery = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Search 50 concepts") }, singleLine = true)
                        if (recent.isNotEmpty()) Text("Recent: ${recent.mapNotNull { id -> SetTheoryLearningCatalog.concepts.firstOrNull { it.id == id }?.title }.joinToString()}", color = Muted, fontSize = 10.sp)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            SetTheoryLearningCatalog.concepts.filter { it.title.contains(conceptQuery, true) || it.category.contains(conceptQuery, true) }.forEach { concept ->
                                GlowButton(if (concept.id in favourites) "★ ${concept.title}" else concept.title) { chooseConcept(concept.id) }
                            }
                        }
                        SetTheoryLearningCatalog.concepts.firstOrNull { it.id == selectedConceptId }?.let { concept ->
                            KnowledgeCard(concept.title, concept.definition, "${concept.category} - Level ${concept.level}", concept.example, Green)
                            GlowButton(if (concept.id in favourites) "Remove favourite" else "Add favourite") { favourites = if (concept.id in favourites) favourites - concept.id else favourites + concept.id }
                        }
                    }
                }
                when (activeTool) {
                    SetStudioTool.Venn -> {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            (1..4).forEach { count -> GlowButton(if (setCount == count) "• $count sets" else "$count sets") { setCount = count } }
                            TogglePill(if (eulerMode) "Euler mode" else "Venn mode", eulerMode) { eulerMode = !eulerMode }
                            TogglePill("High contrast", highContrast) { highContrast = !highContrast }
                            TogglePill("Spoken maths", spokenMath) { spokenMath = !spokenMath }
                        }
                        Insight("Region cardinality", "|$setAName ∪ $setBName| = ${SetTheoryStudioEngine.inclusionExclusion(a.toSet(), b.toSet())}", Cyan)
                        Text("Pinch/pan, animated shading and image export controls apply to the live Venn canvas above.", color = Muted, fontSize = 11.sp)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            GlowButton("Export image") { sharePreview = runCatching { exportSetDiagramPng(context, setAName, a, setBName, b) }.getOrElse { "Export failed: ${it.message}" } }
                            GlowButton("Describe") { sharePreview = "${if (spokenMath) "Speaking: " else "Description: "}${activeLaw.formula}; ${activeLaw.explanation}" }
                        }
                    }
                    SetStudioTool.Elements -> {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(setAName, { setAName = it.take(3) }, modifier = Modifier.width(90.dp), label = { Text("Set A") }, singleLine = true)
                            OutlinedTextField(setBName, { setBName = it.take(3) }, modifier = Modifier.width(90.dp), label = { Text("Set B") }, singleLine = true)
                            OutlinedTextField(universeName, { universeName = it.take(4) }, modifier = Modifier.width(110.dp), label = { Text("Universe") }, singleLine = true)
                        }
                        OutlinedTextField(inputA, { updateInputs(nextA = it) }, modifier = Modifier.fillMaxWidth(), label = { Text("$setAName elements: text, numbers or symbols") })
                        OutlinedTextField(inputB, { updateInputs(nextB = it) }, modifier = Modifier.fillMaxWidth(), label = { Text("$setBName elements") })
                        Insight("Roster", "$setAName=${SetTheoryStudioEngine.roster(a)}  $setBName=${SetTheoryStudioEngine.roster(b)}", Cyan)
                        Insight("Set-builder", "$setAName={x ∈ $universeName | x is one of ${a.joinToString()}}", Violet)
                        Text("Duplicates are removed automatically. Tap or drag elements on the Venn canvas to change region membership.", color = Green, fontSize = 11.sp)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            GlowButton("Undo", enabled = undo.isNotEmpty()) { undo.lastOrNull()?.let { snapshot -> redo = redo + (inputA to inputB); inputA = snapshot.first; inputB = snapshot.second; undo = undo.dropLast(1) } }
                            GlowButton("Redo", enabled = redo.isNotEmpty()) { redo.lastOrNull()?.let { snapshot -> undo = undo + (inputA to inputB); inputA = snapshot.first; inputB = snapshot.second; redo = redo.dropLast(1) } }
                            GlowButton("Reset") { updateInputs("1, 2, 3", "2, 3, 4") }
                            GlowButton("Save") { saved = (saved + "$setAName=${SetTheoryStudioEngine.roster(a)}; $setBName=${SetTheoryStudioEngine.roster(b)}").distinct() }
                            GlowButton("Share data") {
                                val data = "{\"universe\":\"$universeName\",\"$setAName\":${a},\"$setBName\":${b}}"
                                sharePreview = data
                                shareSetConstruction(context, data)
                            }
                        }
                        if (saved.isNotEmpty()) Text("Saved: ${saved.joinToString(" | ")}", color = Muted, fontSize = 10.sp)
                    }
                    SetStudioTool.PowerSet -> {
                        val subsets = runCatching { SetTheoryStudioEngine.powerSet(a) }.getOrElse { emptyList() }
                        Insight("Power-set size", "2^${a.size} = ${subsets.size}", Violet)
                        Text(subsets.take(64).joinToString { SetTheoryStudioEngine.roster(it) }, color = Ink, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        Text("Subset tree: ∅ → singletons → pairs → … → $setAName", color = Green, fontSize = 11.sp)
                    }
                    SetStudioTool.Cartesian -> {
                        val product = SetTheoryStudioEngine.cartesianProduct(a, b)
                        Insight("Product", "|$setAName × $setBName| = ${product.size}", Cyan)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) { product.forEach { (x, y) -> Insight("", "($x,$y)", Violet) } }
                    }
                    SetStudioTool.Relations -> {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            domain.forEach { x -> domain.forEach { y ->
                                val pair = x to y; TogglePill(if (relationMatrix) "${if (pair in relation) 1 else 0}" else "$x → $y", pair in relation) { relation = if (pair in relation) relation - pair else relation + pair }
                            } }
                        }
                        GlowButton(if (relationMatrix) "Arrow view" else "Matrix view") { relationMatrix = !relationMatrix }
                        val analysis = SetTheoryStudioEngine.analyzeRelation(domain, relation.filter { it.first in domain && it.second in domain }.toSet())
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Insight("Reflexive", analysis.reflexive.toString(), if (analysis.reflexive) Green else Muted)
                            Insight("Symmetric", analysis.symmetric.toString(), if (analysis.symmetric) Green else Muted)
                            Insight("Antisymmetric", analysis.antisymmetric.toString(), if (analysis.antisymmetric) Green else Muted)
                            Insight("Transitive", analysis.transitive.toString(), if (analysis.transitive) Green else Muted)
                        }
                    }
                    SetStudioTool.Partitions -> {
                        val numeric = a.mapNotNull { it.toIntOrNull() }
                        val classes = numeric.groupBy { kotlin.math.abs(it % 2) }.values.map { it.toSet() }
                        Text("Equivalence classes by parity", color = Cyan, fontWeight = FontWeight.Bold)
                        classes.forEachIndexed { index, group -> Insight("Class ${index + 1}", SetTheoryStudioEngine.roster(group.map(Int::toString)), listOf(Cyan, Violet)[index % 2]) }
                        Insight("Partition valid", (classes.flatten().toSet() == numeric.toSet() && classes.all { it.isNotEmpty() }).toString(), Green)
                    }
                    SetStudioTool.Order -> {
                        val numbers = a.mapNotNull { it.toIntOrNull() }.filter { it != 0 }.toSet()
                        val covers = SetTheoryStudioEngine.hasseCovers(numbers)
                        Text("Divisibility Hasse covers", color = Cyan, fontWeight = FontWeight.Bold)
                        Text(covers.joinToString { "${it.first} ≺ ${it.second}" }.ifBlank { "Add comparable non-zero integers to set A." }, color = Ink, fontFamily = FontFamily.Monospace)
                        Text("Drag-to-order nodes use the cover pairs above; transitive edges are removed.", color = Muted, fontSize = 10.sp)
                    }
                    SetStudioTool.Functions -> {
                        val codomain = b.toSet(); val mapping = a.mapIndexedNotNull { index, item -> b.getOrNull(index % b.size.coerceAtLeast(1))?.let { item to it } }.toMap()
                        val analysis = SetTheoryStudioEngine.analyzeMapping(domain, codomain, mapping)
                        Text(mapping.entries.joinToString { "${it.key} → ${it.value}" }, color = Ink, fontFamily = FontFamily.Monospace)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Insight("Function", analysis.isFunction.toString(), Cyan); Insight("Injective", analysis.injective.toString(), Violet)
                            Insight("Surjective", analysis.surjective.toString(), Green); Insight("Bijective", analysis.bijective.toString(), Amber)
                        }
                    }
                    SetStudioTool.Proofs -> {
                        KnowledgeCard(activeLaw.title, activeLaw.formula, "Membership proof", activeLaw.explanation, Violet)
                        Text("1. Let x be arbitrary.  2. Translate set membership into Boolean conditions.  3. Apply the law.  4. Translate back to sets.", color = Ink, fontSize = 12.sp)
                        Insight("All cases", if (SetLogicEngine.verified(activeLaw)) "No counterexample" else "Counterexample found", Green)
                    }
                    SetStudioTool.Challenge -> {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            (1..4).forEach { level -> GlowButton(if (difficulty == level) "• Level $level" else "Level $level") { difficulty = level } }
                            TogglePill("Guided", guided) { guided = !guided }
                        }
                        Text("Predict: Is ${activeLaw.formula} true for every membership assignment?", color = Ink, fontWeight = FontWeight.Bold)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            GlowButton("Always true") { prediction = true; reveal = false }
                            GlowButton("Has counterexample") { prediction = false; reveal = false }
                            GlowButton(if (reveal) "Hide result" else "Reveal") { reveal = !reveal }
                        }
                        if (reveal) Insight("Result", if (prediction == SetLogicEngine.verified(activeLaw)) "Correct - mastery +1" else "Try again - inspect the full table", if (prediction == SetLogicEngine.verified(activeLaw)) Green else Amber)
                        Insight("Mastery", "${favourites.size + recent.size}/50 concepts explored", Violet)
                    }
                }
                val routedFeatures = SetTheoryLearningCatalog.features.filter { it.tool == activeTool }
                Text("Included here: ${routedFeatures.joinToString { it.title }}", color = Muted, fontSize = 9.sp)
                sharePreview?.let { preview ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Cyan.copy(.07f)).padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(preview, color = Cyan, fontSize = 10.sp, modifier = Modifier.weight(1f)); GlowButton("Close") { sharePreview = null }
                    }
                }
            }
        }
    }
}

private fun exportSetDiagramPng(
    context: android.content.Context,
    setAName: String,
    a: List<String>,
    setBName: String,
    b: List<String>,
): String {
    val bitmap = android.graphics.Bitmap.createBitmap(1200, 800, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.rgb(5, 13, 24))
    val fillA = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.argb(75, 30, 210, 255); style = android.graphics.Paint.Style.FILL }
    val fillB = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.argb(75, 145, 95, 255); style = android.graphics.Paint.Style.FILL }
    val strokeA = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(30, 210, 255); style = android.graphics.Paint.Style.STROKE; strokeWidth = 7f }
    val strokeB = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(145, 95, 255); style = android.graphics.Paint.Style.STROKE; strokeWidth = 7f }
    val text = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE; textSize = 34f }
    canvas.drawCircle(470f, 390f, 245f, fillA); canvas.drawCircle(470f, 390f, 245f, strokeA)
    canvas.drawCircle(730f, 390f, 245f, fillB); canvas.drawCircle(730f, 390f, 245f, strokeB)
    canvas.drawText("$setAName = ${SetTheoryStudioEngine.roster(a)}", 80f, 90f, text)
    canvas.drawText("$setBName = ${SetTheoryStudioEngine.roster(b)}", 650f, 90f, text)
    canvas.drawText("$setAName ∩ $setBName = ${SetTheoryStudioEngine.roster(a.toSet().intersect(b.toSet()))}", 330f, 735f, text)
    val directory = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES) ?: context.filesDir
    val file = java.io.File(directory, "set-theory-${System.currentTimeMillis()}.png")
    java.io.FileOutputStream(file).use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    bitmap.recycle()
    return "PNG exported: ${file.absolutePath}"
}

private fun shareSetConstruction(context: android.content.Context, data: String) {
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(android.content.Intent.EXTRA_TEXT, data)
    }
    context.startActivity(android.content.Intent.createChooser(intent, "Share Set Theory construction").addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
}

private val SetDiagramAColor = Color(0xFF20D7B0)
private val SetDiagramBColor = Color(0xFFFF5FA2)
private val SetDiagramUnionColor = Color(0xFFFFC857)

@Composable
private fun SetDiagramLegendChip(label: String, color: Color) {
    Row(
        Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(color.copy(alpha = .14f))
            .border(1.dp, color.copy(alpha = .72f), RoundedCornerShape(100.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(9.dp).clip(RoundedCornerShape(50)).background(color))
        Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SetFormulaVennCanvas(
    law: BooleanFormulaLaw,
    overlap: Float,
    selectedPoint: Offset?,
    onSelect: (Offset, Map<String, Boolean>) -> Unit,
) {
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    fun memberships(point: Offset, width: Float, height: Float): Map<String, Boolean> {
        val radius = minOf(width, height) * .27f * zoom
        val separation = radius * (1.65f - overlap)
        val centers = listOf(
            Offset(width / 2 - separation / 2, height * .47f) + pan,
            Offset(width / 2 + separation / 2, height * .47f) + pan,
            Offset(width / 2, height * .64f) + pan,
        )
        return law.variables.mapIndexed { index, name -> name to ((point - centers[index]).getDistance() <= radius) }.toMap()
    }
    var canvasWidth by remember { mutableFloatStateOf(1f) }
    var canvasHeight by remember { mutableFloatStateOf(1f) }
    Canvas(
        Modifier.fillMaxWidth().height(270.dp).clip(RoundedCornerShape(20.dp)).background(Color(0xFF07111F))
            .border(1.dp, Cyan.copy(.45f), RoundedCornerShape(20.dp))
            .onSizeChanged { canvasWidth = it.width.toFloat(); canvasHeight = it.height.toFloat() }
            .pointerInput(law.id, overlap, zoom, pan) { detectTapGestures(onDoubleTap = { zoom = 1f; pan = Offset.Zero }, onTap = { point -> onSelect(point, memberships(point, canvasWidth, canvasHeight)) }) }
            .pointerInput(Unit) { detectTransformGestures { _, panChange, zoomChange, _ -> zoom = (zoom * zoomChange).coerceIn(.65f, 2.4f); pan += panChange } }
            .semantics { contentDescription = "Interactive Venn diagram for ${law.title}; tap to test set membership" },
    ) {
        val radius = minOf(size.width, size.height) * .27f * zoom
        val separation = radius * (1.65f - overlap)
        val centers = listOf(Offset(size.width / 2 - separation / 2, size.height * .47f) + pan, Offset(size.width / 2 + separation / 2, size.height * .47f) + pan, Offset(size.width / 2, size.height * .64f) + pan)
        drawRect(Color(0xFF091626), style = androidx.compose.ui.graphics.drawscope.Fill)
        val colors = listOf(SetDiagramAColor, SetDiagramBColor, Color(0xFF8B7CFF))
        law.variables.forEachIndexed { index, _ ->
            drawCircle(colors[index].copy(.25f), radius, centers[index])
            drawCircle(colors[index], radius, centers[index], style = Stroke(width = 3.5f))
        }
        val columns = 17; val lines = 9
        repeat(columns) { xIndex -> repeat(lines) { yIndex ->
            val point = Offset((xIndex + 1) * size.width / (columns + 1), (yIndex + 1) * size.height / (lines + 1))
            val member = memberships(point, size.width, size.height)
            if (law.left(member)) drawCircle(SetDiagramUnionColor.copy(.92f), 3.5f, point) else drawCircle(Muted.copy(.24f), 2f, point)
        } }
        fun drawRegionBadge(label: String, center: Offset, color: Color) {
            val textPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                this.color = android.graphics.Color.rgb(
                    (color.red * 255).toInt(),
                    (color.green * 255).toInt(),
                    (color.blue * 255).toInt(),
                )
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = 15.sp.toPx()
                isFakeBoldText = true
            }
            val horizontalPadding = 9.dp.toPx()
            val verticalPadding = 5.dp.toPx()
            val badgeWidth = textPaint.measureText(label) + horizontalPadding * 2
            val badgeHeight = textPaint.fontMetrics.run { bottom - top } + verticalPadding * 2
            val badgeTopLeft = Offset(center.x - badgeWidth / 2, center.y - badgeHeight / 2)
            drawRoundRect(
                color = Color(0xE6172330),
                topLeft = badgeTopLeft,
                size = Size(badgeWidth, badgeHeight),
                cornerRadius = CornerRadius(badgeHeight / 2),
            )
            drawRoundRect(
                color = color.copy(alpha = .9f),
                topLeft = badgeTopLeft,
                size = Size(badgeWidth, badgeHeight),
                cornerRadius = CornerRadius(badgeHeight / 2),
                style = Stroke(width = 1.5.dp.toPx()),
            )
            val baseline = center.y - (textPaint.descent() + textPaint.ascent()) / 2
            drawContext.canvas.nativeCanvas.drawText(label, center.x, baseline, textPaint)
        }
        law.variables.forEachIndexed { index, variable ->
            val horizontalDirection = when (index) {
                0 -> -.42f
                1 -> .42f
                else -> 0f
            }
            val verticalDirection = if (index == 2) .48f else .05f
            drawRegionBadge(
                variable,
                centers[index] + Offset(radius * horizontalDirection, radius * verticalDirection),
                colors[index],
            )
        }
        if ("A" in law.variables && "B" in law.variables) {
            val unionY = (minOf(centers[0].y, centers[1].y) - radius - 17.dp.toPx())
                .coerceAtLeast(25.dp.toPx())
            drawRegionBadge("A ∪ B", Offset((centers[0].x + centers[1].x) / 2, unionY), SetDiagramUnionColor)
        }
        selectedPoint?.let { point ->
            drawCircle(Amber.copy(.22f), 14f, point)
            drawCircle(Amber, 7f, point)
        }
    }
}

@Composable
private fun LogicGateVisualizer(law: BooleanFormulaLaw, values: Map<String, Boolean>, result: Boolean) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color(0xFF07111F)).border(1.dp, Violet.copy(.5f), RoundedCornerShape(20.dp)).padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            law.variables.forEach { variable -> Insight(variable, if (values[variable] == true) "1" else "0", if (values[variable] == true) Cyan else Muted) }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            TransparentIcon(law.formula.take(3), Violet)
            Text(law.title, color = Violet, fontSize = 11.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("OUTPUT", color = Muted, fontSize = 9.sp)
            Text(if (result) "1" else "0", color = if (result) Green else Amber, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun BooleanFormulaTable(law: BooleanFormulaLaw, rows: List<com.indianservers.aiexplorer.core.BooleanFormulaRow>, selected: Map<String, Boolean>) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFF07111F)).border(1.dp, Cyan.copy(.32f), RoundedCornerShape(14.dp)).padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(law.variables.joinToString("  "), color = Cyan, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text("LHS  RHS  =", color = Violet, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
        rows.forEach { row ->
            val active = law.variables.all { row.inputs[it] == selected[it] }
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if (active) Violet.copy(.18f) else Color.Transparent).padding(6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(law.variables.joinToString("   ") { if (row.inputs[it] == true) "1" else "0" }, color = if (active) Ink else Muted, fontFamily = FontFamily.Monospace)
                Text(" ${if (row.left) 1 else 0}     ${if (row.right) 1 else 0}    ${if (row.equivalent) "✓" else "✗"}", color = if (row.equivalent) Green else Amber, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
private fun MathNotebookScreen(vm: ExplorerViewModel, wide: Boolean) {
    val context = LocalContext.current
    var input by remember { mutableStateOf("a := 2") }
    var exactMode by remember { mutableStateOf(true) }
    val cas = remember { SymbolicCasEngine() }
    val casInteraction = remember { CasInteractionEngine(cas) }
    val notebookCas = remember { CasNotebookInteractionEngine(casInteraction, cas) }
    val directCas = remember { CasDirectManipulationEngine(notebookCas, cas) }
    var casInput by remember { mutableStateOf(TextFieldValue("(x+1)^2")) }
    var casOperation by remember { mutableStateOf("expand") }
    var casAssumptions by remember { mutableStateOf(MathAssumptionSet()) }
    var assumptionDraft by remember { mutableStateOf(CasAssumptionDraft()) }
    var assumptionMessage by remember { mutableStateOf("Assumptions are local, explicit and removable.") }
    var showAssumptions by remember { mutableStateOf(false) }
    var casMethod by remember { mutableStateOf(CasSolutionMethod.Auto) }
    var keyboardLayer by remember { mutableStateOf(CasKeyboardLayer.Basic) }
    var expandedCasSteps by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var revealedCasSteps by remember { mutableIntStateOf(1) }
    var casStepDisclosureDepths by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    var nextStepHintVisible by remember { mutableStateOf(false) }
    var casSession by remember { mutableStateOf(CasNotebookSession()) }
    var expandedPods by remember { mutableStateOf<Set<String>>(emptySet()) }
    var revealedHints by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var rowStepDisclosureDepths by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var manipulationStates by remember { mutableStateOf<Map<String, CasManipulationState>>(emptyMap()) }
    var scrubValues by remember { mutableStateOf<Map<String, Float>>(emptyMap()) }
    var carriedExpression by remember { mutableStateOf<CasExpressionTransfer?>(null) }
    var importText by remember { mutableStateOf("") }
    var importMessage by remember { mutableStateOf("Paste CSV or TSV to create a notebook-scoped matrix.") }
    var showImport by remember { mutableStateOf(false) }
    var captureMessage by remember { mutableStateOf("Voice mathematics is always reviewed before evaluation.") }
    var variableName by remember { mutableStateOf("a") }
    var variableExpression by remember { mutableStateOf("2") }
    var variableScope by remember { mutableStateOf(CasVariableScope.FollowingRows) }
    val voiceCapture = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val candidates = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).orEmpty()
        if (result.resultCode == Activity.RESULT_OK && candidates.isNotEmpty()) {
            val capture = CasCaptureNormalizer.normalize(CasInputModality.Voice, candidates.mapIndexed { index, text -> text to (1.0 - index * .12).coerceAtLeast(.4) })
            casInput = TextFieldValue(capture.recognizedText, TextRange(capture.recognizedText.length))
            captureMessage = "Voice recognized at ${(capture.confidence * 100).toInt()}%. ${capture.alternatives.size} alternate(s) remain reviewable."
        }
    }
    val audioPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) voiceCapture.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 4)
        }) else captureMessage = "Microphone permission is needed for voice math input."
    }
    val casStructuredPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runCatching {
            val name = context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            } ?: "imported_matrix"
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader -> reader.readText() } ?: error("The selected file could not be read.")
            val parsed = CasStructuredImportEngine.structured(name, text, context.contentResolver.getType(uri))
            casSession = notebookCas.import(casSession, parsed.data)
            val expression = parsed.data.name; casInput = TextFieldValue(expression, TextRange(expression.length))
            importMessage = "Imported ${parsed.format}: ${parsed.data.rows} × ${parsed.data.columns} as ${parsed.data.name}. ${parsed.warnings.joinToString()}"
        }.onFailure { importMessage = it.message ?: "The structured file could not be imported." }
    }
    val interpretationOptions = remember(casInput.text, casOperation) { CasInterpretationResolver.options(casInput.text, casOperation) }
    var selectedInterpretationId by remember { mutableStateOf<String?>(null) }
    val selectedInterpretation = interpretationOptions.firstOrNull { it.id == selectedInterpretationId } ?: interpretationOptions.first()
    val dimensionReport = remember(selectedInterpretation.expression) { CasDimensionalAnalyzer.analyze(selectedInterpretation.expression) }
    val casPreview = remember(casInput.text, casOperation, casAssumptions) { casInteraction.interpret(casInput.text, casOperation, casAssumptions) }
    val availableCasMethods = remember(casPreview.operation) { casInteraction.availableMethods(casPreview.operation) }
    val effectiveCasMethod = casMethod.takeIf { it in availableCasMethods } ?: CasSolutionMethod.Auto
    val casRow = remember(casPreview, casAssumptions, effectiveCasMethod) { casInteraction.evaluate(casPreview, casAssumptions, effectiveCasMethod) }
    val examples = listOf(
        "Value" to "a := 2",
        "Dependent" to "b := a^2 + 3",
        "Function" to "f(x) := a*x^2 + b",
        "Reuse cell" to "#1 + 10",
        "Exact" to "1/3 + 1/6",
    )

    fun insertCasText(insertion: String, cursorBack: Int = 0) {
        val start = casInput.selection.min.coerceIn(0, casInput.text.length); val end = casInput.selection.max.coerceIn(start, casInput.text.length)
        val next = casInput.text.replaceRange(start, end, insertion); val cursor = (start + insertion.length - cursorBack).coerceIn(0, next.length)
        casInput = TextFieldValue(next, TextRange(cursor)); revealedCasSteps = 1; expandedCasSteps = emptySet(); casStepDisclosureDepths = emptyMap(); nextStepHintVisible = false
    }

    fun deleteCasBackward() {
        val start = casInput.selection.min.coerceIn(0, casInput.text.length); val end = casInput.selection.max.coerceIn(start, casInput.text.length)
        if (start != end) casInput = TextFieldValue(casInput.text.removeRange(start, end), TextRange(start))
        else if (start > 0) casInput = TextFieldValue(casInput.text.removeRange(start - 1, start), TextRange(start - 1))
    }

    @Composable
    fun InputPanel(modifier: Modifier = Modifier) {
        GlassPanel(modifier) {
            PanelHeader("Unified Math Notebook", vm::returnToMathMenu, Cyan, icon = "#")
            Text("Define with := - edit a symbol by defining it again - dependent cells recalculate automatically.", color = Muted, fontSize = 12.sp)
            IntentAwareMathField(
                value = input, onValueChange = { input = it }, label = "Expression or assignment",
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Notebook maths input" },
                placeholder = "f(x) := a*x^2 + 3", singleLine = false, minLines = 2,
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
                        Text("#${index + 1} - ${cell.kind.label}${cell.symbol?.let { " - $it" } ?: ""}", color = accent, fontWeight = FontWeight.Bold)
                            DestructiveGlowButton("Delete") { vm.removeNotebookCell(cell.id) }
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
        LaunchedEffect(casPreview.operation) { casMethod = CasSolutionMethod.Auto; revealedCasSteps = 1; expandedCasSteps = emptySet(); casStepDisclosureDepths = emptyMap(); nextStepHintVisible = false }
        GlassPanel(modifier.semantics { contentDescription = "CAS rows with exact and decimal output" }) {
            PanelHeader("CAS Rows", vm::returnToMathMenu, Violet, icon = "CAS")
            Text("Exact CAS: assumptions, algebra, systems, calculus, matrices and verified first-order ODEs share one symbolic tree.", color = Muted, fontSize = 12.sp)
            IntentAwareMathValueField(
                value = casInput,
                onValueChange = { casInput = it; revealedCasSteps = 1; expandedCasSteps = emptySet(); casStepDisclosureDepths = emptyMap(); nextStepHintVisible = false },
                label = "CAS expression or question", modifier = Modifier.fillMaxWidth(),
                placeholder = "factor x^2-5*x+6 or eigenvalues [[1,2],[3,4]]", minLines = 2,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                GlowButton("Voice math") { audioPermission.launch(Manifest.permission.RECORD_AUDIO) }
            }
            Text(captureMessage, color = Muted, fontSize = 10.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    "simplify", "expand", "factor", "partial fractions", "derivative", "integral", "limit", "system", "inequalities",
                    "series", "asymptotic", "sum", "product", "recurrence", "optimization", "exact roots", "domain",
                    "determinant", "rref", "rank", "nullspace", "matrix inverse", "transpose", "eigenvalues", "eigenvectors", "jordan form", "svd", "lu", "qr", "cholesky",
                    "ode", "nonlinear ode", "higher ode", "pde", "laplace", "inverse laplace", "fourier", "inverse fourier", "z transform",
                    "residue", "contour integral", "special functions", "number theory", "finite algebra",
                ).forEach { operation ->
                    GlowButton(if (casPreview.operation == operation) "• $operation" else operation) {
                        casOperation = operation
                        val expression = casPreview.expression
                        casInput = TextFieldValue(expression, TextRange(expression.length))
                    }
                }
                GlowButton("sub x=2") {
                    val substituted = cas.substitute(casInput.text, mapOf("x" to "2")).exact
                    casInput = TextFieldValue(substituted, TextRange(substituted.length))
                    casOperation = "simplify"
                }
            }
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0x33101B2A))
                    .border(1.dp, Cyan.copy(.35f), RoundedCornerShape(14.dp)).padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("INTERPRETATION PREVIEW", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("${(casPreview.confidence * 100).toInt()}%", color = if (casPreview.warnings.isEmpty()) Green else Amber, fontSize = 10.sp)
                }
                Text(casPreview.explanation, color = Ink, fontSize = 12.sp)
                Text("Expression: ${casPreview.expression}", color = Green, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                Text("Operation ${casPreview.operation} - variable ${casPreview.variable}", color = Violet, fontSize = 10.sp)
                casPreview.warnings.forEach { Text("Check: $it", color = Amber, fontSize = 10.sp) }
            }
            if (interpretationOptions.size > 1) {
                Text("Choose an interpretation", color = Ink, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    interpretationOptions.forEach { option ->
                        GlowButton(if (selectedInterpretation.id == option.id) "Selected: ${option.label}" else option.label) {
                            selectedInterpretationId = option.id
                        }
                    }
                }
                Text(selectedInterpretation.explanation, color = Muted, fontSize = 10.sp)
            }
            Text(dimensionReport.message, color = if (dimensionReport.valid) Green else Amber, fontSize = 10.sp)
            Text("Solution method", color = Ink, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                availableCasMethods.forEach { method ->
                    GlowButton(if (effectiveCasMethod == method) "Selected: ${method.label}" else method.label) { casMethod = method; revealedCasSteps = 1; expandedCasSteps = emptySet(); casStepDisclosureDepths = emptyMap(); nextStepHintVisible = false }
                }
            }
            Text((if (effectiveCasMethod == CasSolutionMethod.Auto) availableCasMethods.firstOrNull { it != CasSolutionMethod.Auto } else effectiveCasMethod)?.explanation.orEmpty(), color = Muted, fontSize = 10.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Assumptions", color = Ink, fontWeight = FontWeight.SemiBold)
                GlowButton(if (showAssumptions) "Close editor" else "Edit assumptions") { showAssumptions = !showAssumptions }
            }
            if (casAssumptions.variables.isEmpty()) Text("No assumptions — real variables use guarded identities only.", color = Muted, fontSize = 10.sp)
            else FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                casAssumptions.variables.values.sortedBy { it.variable }.forEach { assumption ->
                    GlowButton("${assumption.description()} ×") { casAssumptions = casAssumptions.copy(variables = casAssumptions.variables - assumption.variable) }
                }
            }
            Text("Shared variables", color = Ink, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                CasVariableScope.entries.forEach { scope -> TogglePill(scope.name, variableScope == scope) { variableScope = scope } }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                OutlinedTextField(variableName, { variableName = it.take(16) }, Modifier.weight(.35f), label = { Text("Name") }, singleLine = true)
                OutlinedTextField(variableExpression, { variableExpression = it }, Modifier.weight(.65f), label = { Text("Expression") }, singleLine = true)
            }
            GlowButton("Define scoped variable") {
                runCatching {
                    CasScopedVariable(variableName.trim(), variableExpression.trim(), variableScope, casSession.rows.lastOrNull()?.id)
                }.onSuccess { casSession = notebookCas.define(casSession, it) }.onFailure { assumptionMessage = it.message ?: "Check the variable." }
            }
            if (casSession.variables.isNotEmpty()) Text(casSession.variables.joinToString(" - ") { "${it.name}=${it.expression} [${it.scope.name}]" }, color = Green, fontSize = 10.sp)
            AnimatedVisibility(showAssumptions) {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0x331D1330)).border(1.dp, Violet.copy(.45f), RoundedCornerShape(14.dp)).padding(9.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    OutlinedTextField(assumptionDraft.variable, { assumptionDraft = assumptionDraft.copy(variable = it.take(16)) }, Modifier.fillMaxWidth(), label = { Text("Variable") }, singleLine = true)
                    Text("Domain", color = Muted, fontSize = 10.sp)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        MathNumberDomain.entries.forEach { domain -> TogglePill(domain.name, assumptionDraft.domain == domain) { assumptionDraft = assumptionDraft.copy(domain = domain) } }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        TogglePill("positive", assumptionDraft.positive) { assumptionDraft = assumptionDraft.copy(positive = it, nonNegative = if (it) false else assumptionDraft.nonNegative) }
                        TogglePill("non-negative", assumptionDraft.nonNegative) { assumptionDraft = assumptionDraft.copy(nonNegative = it, positive = if (it) false else assumptionDraft.positive) }
                        TogglePill("non-zero", assumptionDraft.nonZero) { assumptionDraft = assumptionDraft.copy(nonZero = it) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        OutlinedTextField(assumptionDraft.minimum, { assumptionDraft = assumptionDraft.copy(minimum = it) }, Modifier.weight(1f), label = { Text("Minimum") }, singleLine = true)
                        OutlinedTextField(assumptionDraft.maximum, { assumptionDraft = assumptionDraft.copy(maximum = it) }, Modifier.weight(1f), label = { Text("Maximum") }, singleLine = true)
                    }
                    GlowButton("Add or update assumption") {
                        runCatching { assumptionDraft.build() }.onSuccess { assumption ->
                            casAssumptions = casAssumptions.with(assumption); assumptionMessage = "Using ${assumption.description()}"; assumptionDraft = assumptionDraft.copy(variable = assumption.variable)
                        }.onFailure { assumptionMessage = it.message ?: "Check the assumption." }
                    }
                    Text(assumptionMessage, color = if (assumptionMessage.startsWith("Using")) Green else Muted, fontSize = 10.sp)
                }
            }
            Text("Full maths keyboard", color = Ink, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                CasKeyboardLayer.entries.forEach { layer -> GlowButton(if (keyboardLayer == layer) "${layer.label} active" else layer.label) { keyboardLayer = layer } }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                CasKeyboardCatalog.layers.getValue(keyboardLayer).forEach { key -> GlowButton(key.label) { insertCasText(key.insertion, key.cursorBack) } }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                GlowButton("←") { casInput = casInput.copy(selection = TextRange((casInput.selection.min - 1).coerceAtLeast(0))) }
                GlowButton("→") { casInput = casInput.copy(selection = TextRange((casInput.selection.max + 1).coerceAtMost(casInput.text.length))) }
                GlowButton("Backspace", onClick = ::deleteCasBackward)
                GlowButton("Clear") { casInput = TextFieldValue("") }
            }
            Text("Syntax: systems use {x+y=5; x-y=1}; inequalities use 2*x>=4 and x<5; matrices use [[1,2],[3,4]].", color = Muted, fontSize = 10.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                GlowButton("Evaluate as new CAS row", enabled = casInput.text.isNotBlank() && dimensionReport.valid) {
                    casSession = notebookCas.evaluate(
                        casSession.copy(assumptions = casAssumptions), casInput.text, selectedInterpretation,
                        effectiveCasMethod, "cas-${casSession.rows.size + 1}",
                    )
                }
                GlowButton(if (showImport) "Close import" else "Import structured data") { showImport = !showImport }
            }
            AnimatedVisibility(showImport) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("CSV - TSV - JSON arrays - whitespace matrices - Matrix Market", color = Cyan, fontSize = 10.sp)
                    GlowButton("Choose structured file") { casStructuredPicker.launch(arrayOf("text/csv", "text/tab-separated-values", "application/json", "text/plain", "application/octet-stream")) }
                    OutlinedTextField(importText, { importText = it }, Modifier.fillMaxWidth(), label = { Text("Paste structured matrix data") }, minLines = 3)
                    GlowButton("Parse pasted data", enabled = importText.isNotBlank()) {
                        runCatching { CasStructuredImportEngine.structured("import_${casSession.imports.size + 1}.txt", importText) }
                            .onSuccess { parsed -> val data = parsed.data; casSession = notebookCas.import(casSession, data); casInput = TextFieldValue(data.name, TextRange(data.name.length)); importMessage = "Imported ${parsed.format}: ${data.rows} × ${data.columns} as ${data.name}." }
                            .onFailure { importMessage = it.message ?: "Could not import this data." }
                    }
                    Text(importMessage, color = if (importMessage.startsWith("Imported")) Green else Muted, fontSize = 10.sp)
                }
            }
            Insight("Operation", casRow.operation, Cyan)
            Insight("Exact", casRow.exact, if (casRow.supported) Green else Amber)
            casRow.decimal?.let { Insight("Decimal", it, Violet) }
            Insight("Assumptions", casRow.assumptions.joinToString().ifBlank { "none" }, Amber)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Step-by-step reasoning", color = Ink, fontWeight = FontWeight.SemiBold)
                Text("${revealedCasSteps.coerceAtMost(casRow.steps.size)}/${casRow.steps.size}", color = Cyan, fontSize = 10.sp)
            }
            casRow.steps.take(revealedCasSteps).forEachIndexed { index, step ->
                val expanded = index in expandedCasSteps
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0x33101824))
                        .border(1.dp, (if (expanded) Violet else Cyan).copy(.35f), RoundedCornerShape(12.dp)).padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("${index + 1}. ${step.title}", color = if (expanded) Violet else Cyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        GlowButton(if (expanded) "Hide details" else "Explain step") { expandedCasSteps = if (expanded) expandedCasSteps - index else expandedCasSteps + index }
                    }
                    MathFormulaText(step.expression, color = Ink, fontSize = 13.sp)
                    AnimatedVisibility(expanded) {
                        val disclosures = CasStepDisclosureEngine.disclosures(step, index, casRow.steps.size, casRow.assumptions)
                        val depth = casStepDisclosureDepths[index] ?: 1
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            disclosures.take(depth).forEach { disclosure ->
                                Text("${disclosure.level.label} - ${disclosure.title}", color = if (disclosure.revealsIntermediate) Green else Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(disclosure.content, color = Muted, fontSize = 11.sp)
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                if (depth < disclosures.size) GlowButton("Show more for this step") { casStepDisclosureDepths = casStepDisclosureDepths + (index to depth + 1) }
                                if (depth > 1) GlowButton("Show less") { casStepDisclosureDepths = casStepDisclosureDepths + (index to depth - 1) }
                            }
                        }
                    }
                }
            }
            if (nextStepHintVisible && revealedCasSteps < casRow.steps.size) {
                val next = casRow.steps[revealedCasSteps]
                val hint = CasStepDisclosureEngine.disclosures(next, revealedCasSteps, casRow.steps.size, casRow.assumptions).first()
                Text("Next-step hint: ${hint.content}", color = Green, fontSize = 11.sp)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (revealedCasSteps < casRow.steps.size) GlowButton("Hint before next step") { nextStepHintVisible = true }
                if (revealedCasSteps < casRow.steps.size) GlowButton("Show next step") { revealedCasSteps++; nextStepHintVisible = false }
                if (revealedCasSteps < casRow.steps.size) GlowButton("Reveal all") { revealedCasSteps = casRow.steps.size }
                if (casRow.steps.isNotEmpty()) GlowButton(if (expandedCasSteps.size == casRow.steps.size) "Collapse details" else "Explain all") {
                    expandedCasSteps = if (expandedCasSteps.size == casRow.steps.size) emptySet() else casRow.steps.indices.toSet()
                    revealedCasSteps = casRow.steps.size
                }
            }
            if (casSession.rows.isNotEmpty()) {
                Text("Interactive CAS notebook", color = Violet, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Expressions can be reused, transformed, compared, annotated and exported without flattening the notebook.", color = Muted, fontSize = 10.sp)
            }
            casSession.rows.asReversed().forEach { interactiveRow ->
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0x44101824))
                        .border(1.dp, if (interactiveRow.pinned) Amber.copy(.7f) else Violet.copy(.4f), RoundedCornerShape(14.dp)).padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    val manipulation = manipulationStates[interactiveRow.id] ?: CasManipulationState(interactiveRow.source)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("${interactiveRow.id} - ${interactiveRow.interpretation.operation}", color = Violet, fontWeight = FontWeight.Bold)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            GlowButton(if (interactiveRow.pinned) "Unpin" else "Pin") { casSession = notebookCas.pin(casSession, interactiveRow.id) }
                            GlowButton(if (interactiveRow.id in casSession.compareRowIds) "Comparing" else "Compare") { casSession = notebookCas.compare(casSession, interactiveRow.id) }
                        }
                    }
                    Text(interactiveRow.source, color = Ink, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0x3320352D)).border(1.dp, Green.copy(.4f), RoundedCornerShape(10.dp)).padding(8.dp)) {
                        Text("LIVE EXPRESSION", color = Green, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        MathFormulaText(manipulation.preview?.after ?: manipulation.expression, color = Ink, fontSize = 14.sp)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            GlowButton("Undo", enabled = manipulation.canUndo) { manipulationStates = manipulationStates + (interactiveRow.id to directCas.undo(manipulation)) }
                            GlowButton("Redo", enabled = manipulation.canRedo) { manipulationStates = manipulationStates + (interactiveRow.id to directCas.redo(manipulation)) }
                            GlowButton("Move expression") { carriedExpression = CasExpressionTransfer(interactiveRow.id, manipulation.expression) }
                            GlowButton("Continue in input") { casInput = TextFieldValue(manipulation.expression, TextRange(manipulation.expression.length)) }
                            carriedExpression?.takeIf { it.sourceRowId != interactiveRow.id }?.let { transfer ->
                                GlowButton(if (manipulation.selectedTarget != null) "Replace selection with ${transfer.sourceRowId}" else "Compose with ${transfer.sourceRowId}") {
                                    manipulationStates = manipulationStates + (interactiveRow.id to directCas.drop(manipulation, transfer, manipulation.selectedTarget != null)); carriedExpression = null
                                }
                            }
                        }
                        directCas.handles(manipulation.expression).take(3).forEach { handle ->
                            val scrubKey = "${interactiveRow.id}:${handle.variable}"; val value = scrubValues[scrubKey] ?: 0f
                            AxisSlider("Scrub ${handle.variable}", value, -10f..10f) { scrubValues = scrubValues + (scrubKey to it) }
                            val scrubbed = directCas.scrub(manipulation.expression, handle.variable, value.toDouble())
                            Text("${handle.variable}=${trim(value.toDouble())} → ${scrubbed.result.exact}", color = Cyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                    interactiveRow.pods.forEach { pod ->
                        val key = "${interactiveRow.id}:${pod.kind.name}"; val expanded = key in expandedPods
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0x331D1330)).padding(8.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(pod.title, color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                if (pod.details.isNotEmpty()) GlowButton(if (expanded) "Less" else "Expand") { expandedPods = if (expanded) expandedPods - key else expandedPods + key }
                            }
                            MathFormulaText(pod.primary, color = Ink, fontSize = 13.sp)
                            if (pod.kind == com.indianservers.aiexplorer.core.CasPodKind.Plot) GlowButton("Open beside Graph") {
                                vm.addFunction(interactiveRow.result.exact); vm.open(MathModule.Graph2D)
                            }
                            AnimatedVisibility(expanded) { Column { pod.details.forEach { Text(it, color = Muted, fontSize = 10.sp) } } }
                        }
                    }
                    val hintCount = revealedHints[interactiveRow.id] ?: 0
                    interactiveRow.hints.take(hintCount).forEach { hint -> Text("${hint.title}: ${hint.text}", color = Green, fontSize = 10.sp) }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        if (hintCount < interactiveRow.hints.size) GlowButton("Progressive hint") { revealedHints = revealedHints + (interactiveRow.id to hintCount + 1) }
                        GlowButton("Reuse in input") { val text = notebookCas.transfer(casSession, CasExpressionTransfer(interactiveRow.id, interactiveRow.result.exact), null); casInput = TextFieldValue(text, TextRange(text.length)) }
                        CasExportFormat.entries.forEach { format -> GlowButton(format.name) {
                            if (format == CasExportFormat.Png) {
                                val file = writeShapePng(context, "CAS ${interactiveRow.id}", interactiveRow.result.exact)
                                copyShapeText(context, "CAS PNG", file.absolutePath)
                            } else copyShapeText(context, "CAS ${format.name}", CasExportEngine.export(interactiveRow, format))
                        } }
                    }
                    Text("Derivation", color = Ink, fontWeight = FontWeight.SemiBold)
                    interactiveRow.result.steps.forEachIndexed { stepIndex, step ->
                        val disclosureKey = "${interactiveRow.id}:$stepIndex"
                        val depth = rowStepDisclosureDepths[disclosureKey] ?: 0
                        val disclosures = CasStepDisclosureEngine.disclosures(step, stepIndex, interactiveRow.result.steps.size, interactiveRow.result.assumptions)
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0x33101824)).padding(7.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("${stepIndex + 1}. ${step.title}", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                GlowButton(if (depth == 0) "Hint" else if (depth < disclosures.size) "Show more" else "Collapse") {
                                    rowStepDisclosureDepths = rowStepDisclosureDepths + (disclosureKey to if (depth >= disclosures.size) 0 else depth + 1)
                                }
                            }
                            disclosures.take(depth).forEach { disclosure ->
                                Text(disclosure.level.label, color = if (disclosure.revealsIntermediate) Green else Violet, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text(disclosure.content, color = Muted, fontSize = 10.sp)
                            }
                        }
                    }
                    Text("Try another method", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        casInteraction.availableMethods(interactiveRow.interpretation.operation).filter { it != CasSolutionMethod.Auto }.forEach { method ->
                            GlowButton(if (interactiveRow.method == method) "${method.label} - current" else method.label) {
                                if (interactiveRow.method != method) {
                                    val next = notebookCas.evaluate(casSession, interactiveRow.source, interactiveRow.interpretation, method, "cas-${casSession.rows.size + 1}")
                                    val newId = next.rows.last().id
                                    casSession = next.copy(compareRowIds = setOf(interactiveRow.id, newId))
                                }
                            }
                        }
                    }
                    if (interactiveRow.assumptionWarnings.isNotEmpty()) interactiveRow.assumptionWarnings.forEach { warning ->
                        Text("Step ${warning.stepIndex + 1}: ${warning.message}", color = Amber, fontSize = 10.sp)
                    }
                    OutlinedTextField(interactiveRow.annotation, { note -> casSession = notebookCas.annotate(casSession, interactiveRow.id, note) }, Modifier.fillMaxWidth(), label = { Text("Annotation") })
                    val targets = directCas.targets(manipulation).take(8)
                    if (targets.isNotEmpty()) {
                        Text("Select and manipulate a subexpression", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                        targets.forEach { target ->
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                GlowButton(if (manipulation.selectedTarget?.id == target.id) "Selected: ${target.text}" else target.text) {
                                    manipulationStates = manipulationStates + (interactiveRow.id to directCas.select(manipulation, target))
                                }
                            }
                        }
                        manipulation.selectedTarget?.let { selected ->
                            Text("Actions for ${selected.text}", color = Violet, fontSize = 10.sp)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                CasTargetAction.entries.forEach { action -> GlowButton("Preview ${action.name}") {
                                    manipulationStates = manipulationStates + (interactiveRow.id to directCas.preview(manipulation, action))
                                } }
                            }
                        }
                        manipulation.preview?.let { preview ->
                            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0x44251B35)).padding(8.dp)) {
                                Text("PREVIEW - ${preview.action.name}", color = Violet, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text("Before: ${preview.before}", color = Muted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                Text("After: ${preview.after}", color = Green, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                preview.domain.descriptions.forEach { Text(it, color = Amber, fontSize = 9.sp) }
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    GlowButton("Commit transformation") { manipulationStates = manipulationStates + (interactiveRow.id to directCas.commit(manipulation)) }
                                    GlowButton("Cancel preview") { manipulationStates = manipulationStates + (interactiveRow.id to directCas.cancel(manipulation)) }
                                }
                            }
                        }
                    }
                }
            }
            if (casSession.compareRowIds.size > 1) {
                val compared = casSession.rows.filter { it.id in casSession.compareRowIds }
                Insight("Pinned comparison", compared.joinToString("  ↔  ") { "${it.id}: ${it.result.exact}" }, Amber)
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
internal fun QuizDashboard(
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
                Text("15 questions per quiz - score, progress and explanations", color = Muted, fontSize = 12.sp)
            }
            TransparentIcon("Q", Violet)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(QuizSubject.Maths).forEach { option ->
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
        Text("${subject.label} - ${level.label}", color = Cyan, fontWeight = FontWeight.Bold)
        Text("Choose Start to generate a focused 15-question quiz. Each answer locks instantly and shows a short explanation.", color = Muted, fontSize = 12.sp)
        Text("Scoring is local and designed for focused mathematics practice.", color = Ink, fontSize = 12.sp)
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
            Text("${session.subject.label} - ${session.level.label}", color = Cyan, fontWeight = FontWeight.Bold)
            Text("${session.score}/${session.questions.size}", color = Green, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        }
        Canvas(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(8.dp))) {
            drawRect(Muted.copy(.18f), Offset.Zero, size)
            drawRect(Green.copy(.8f), Offset.Zero, Size(size.width * progress.coerceIn(0f, 1f), size.height))
        }
        Text("Question ${(session.currentIndex + 1).coerceAtMost(session.questions.size)} of ${session.questions.size}", color = Muted, fontSize = 11.sp)
        if (session.completed) {
            Text("Quiz complete", color = Green, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Score ${session.score}/${session.questions.size} - ${session.percent}%", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
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
            Text("${it.message} - next difficulty ${it.nextDifficulty}", color = if (it.correct) Green else Amber, fontWeight = FontWeight.SemiBold)
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
    var showHandwriting by rememberSaveable { mutableStateOf(false) }
    var showMathCamera by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(vm.solverCameraRequested) {
        if (vm.solverCameraRequested) {
            showMathCamera = true
            vm.consumeMathCameraRequest()
        }
    }
    val syntax = remember(question.text) { MathInputIntelligence.analyze(question.text) }
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
            IntentAwareMathValueField(
                value = question, onValueChange = { question = it }, label = "Maths question",
                modifier = Modifier.fillMaxWidth().heightIn(min = 104.dp).semantics { contentDescription = "Maths question input" },
                placeholder = "Example: solve x^2 - 5x + 6 = 0", minLines = 3,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(syntax.message, color = if (syntax.validBrackets) Green else Amber, fontSize = 11.sp, modifier = Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    GlowButton(if (showMathCamera) "Hide camera" else "Scan") { showMathCamera = !showMathCamera }
                    GlowButton(if (showMathKeyboard) "Hide editor" else "Smart editor") { showMathKeyboard = !showMathKeyboard }
                    GlowButton(if (showHandwriting) "Hide ink" else "Handwrite") { showHandwriting = !showHandwriting }
                }
            }
            AnimatedVisibility(showMathCamera) {
                MathCameraPanel(
                    onUseText = { recognizedText ->
                        question = TextFieldValue(recognizedText, TextRange(recognizedText.length))
                        showMathCamera = false
                        guided = tutor.solve(recognizedText, selectedMethod)
                        reveal = SolverReveal.FirstHint
                        revealedSteps = 1
                        whyStep = -1
                    },
                    onDismiss = { showMathCamera = false },
                )
            }
            AnimatedVisibility(showHandwriting) { HandwritingMathInput(onInsert = ::insertMath) }
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
                    syntax.suggestions.firstOrNull()?.let { Text("AI hint - $it", color = Violet, fontSize = 11.sp) }
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
                Insight("Current coverage", "Exact CAS - matrices - units - calculus - inequalities - series - data", Cyan)
                Insight("Safety", "No invented unsupported answers", Amber)
            } else {
                val solution = result.solution
                SolverAnswerSummary(solution, if (reveal == SolverReveal.Answer) solution.answer else "Answer hidden - use the hints")
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Cyan.copy(.08f))
                        .border(1.dp, Cyan.copy(.45f), RoundedCornerShape(14.dp)).padding(11.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("How I interpreted it", color = Cyan, fontWeight = FontWeight.Bold)
                    Insight("Intent", result.interpretation.selected.intent.label, Violet)
                    Text(result.interpretation.selected.normalizedQuery, color = Ink, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                    Text("Confidence ${trim(result.interpretation.selected.confidence * 100)}% - ${result.interpretation.status.name}", color = Muted, fontSize = 11.sp)
                    result.interpretation.assumptions.forEach { Text("Assumption - $it", color = Amber, fontSize = 11.sp) }
                    result.interpretation.ambiguities.forEach { Text("Ambiguity - $it", color = Amber, fontWeight = FontWeight.SemiBold, fontSize = 11.sp) }
                    result.interpretation.alternatives.forEach { Text("Possible reading - ${it.normalizedQuery}", color = Violet, fontSize = 11.sp) }
                }
                Insight("Method", result.method.label, Violet)
                Text(result.methodReason, color = Muted, fontSize = 12.sp)
                if (result.alternatives.isNotEmpty()) {
                    Text("Try another verified method", color = Ink, fontWeight = FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        result.alternatives.forEach { alternate ->
                            GlowButton("${alternate.method.label} - ${alternate.stepCount} steps") {
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
                    model.relationships.forEach { Text("Relationship - $it", color = Ink, fontSize = 12.sp) }
                    model.equations.forEach { Text("Setup - $it", color = Violet, fontWeight = FontWeight.SemiBold) }
                    model.ambiguity.forEach { Text("Clarify - $it", color = Amber, fontSize = 12.sp) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GlowButton(
                        if (reveal == SolverReveal.FirstHint) "Reveal first step" else "Next step",
                        enabled = solution.steps.isNotEmpty() && (reveal == SolverReveal.FirstHint || revealedSteps < solution.steps.size),
                    ) {
                        if (reveal == SolverReveal.FirstHint) {
                            reveal = SolverReveal.Steps
                            revealedSteps = 1
                        } else if (revealedSteps < solution.steps.size) {
                            revealedSteps++
                        }
                    }
                    GlowButton("Reveal method") { reveal = SolverReveal.Method }
                    GlowButton("Reveal answer") { reveal = SolverReveal.Answer }
                }
                if (reveal == SolverReveal.FirstHint) {
                    solution.steps.firstOrNull()?.let { first ->
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                .background(Violet.copy(.09f))
                                .border(1.dp, Violet.copy(.48f), RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text("Hint 1", color = Violet, fontWeight = FontWeight.Bold)
                            Text(first.explanation, color = Ink, fontSize = 13.sp)
                            Text("Try that transformation yourself before revealing the worked step.", color = Muted, fontSize = 11.sp)
                        }
                    }
                }
                val displayedSteps = if (reveal == SolverReveal.FirstHint) emptyList() else result.visibleSteps(reveal, revealedSteps)
                displayedSteps.forEachIndexed { index, item ->
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
                        GlowButton("Generate MCQ") { learnerWork = "Practice generated - ${solution.question} - verify against the solved example" }
                    }
                }
                Text("Check my working", color = Ink, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = learnerWork,
                    onValueChange = { learnerWork = it },
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Learner working for mistake diagnosis" },
                    label = { Text("One transformation per line") },
                    minLines = 3,
                )
                val workReport = remember(solution.question, learnerWork) {
                    tutor.analyzeLearnerWork(solution.question, learnerWork)
                }
                if (learnerWork.isNotBlank()) {
                    Text(
                        workReport.summary,
                        color = if (workReport.allCorrect) Green else if (workReport.firstIncorrect != null) Amber else Muted,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                    )
                }
                workReport.steps.forEach { feedback ->
                    val accent = when (feedback.status) {
                        LearnerStepStatus.Correct -> Green
                        LearnerStepStatus.Incorrect -> Amber
                        LearnerStepStatus.NeedsReview -> Violet
                    }
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .background(accent.copy(.09f))
                            .border(1.dp, accent.copy(.42f), RoundedCornerShape(10.dp))
                            .padding(9.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            "Line ${feedback.lineNumber} - ${feedback.mistakeKind?.label ?: feedback.status.name}",
                            color = accent,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(feedback.source, color = Ink, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                        Text(feedback.message, color = Ink, fontSize = 12.sp)
                        Text("Hint - ${feedback.hint}", color = Muted, fontSize = 11.sp)
                        feedback.evidence?.let { Text(it, color = accent, fontSize = 10.sp) }
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
private fun ShapesExplorerScreen(vm: ExplorerViewModel, wide: Boolean) {
    var dimension by remember { mutableIntStateOf(2) }
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Triangles") }
    var favouritesOnly by remember { mutableStateOf(false) }
    var multiAdd by remember { mutableStateOf(false) }
    var compareMode by remember { mutableStateOf(false) }
    var compareKeys by remember { mutableStateOf<List<String>>(emptyList()) }
    var formulaKey by remember { mutableStateOf<String?>(null) }
    val categories = if (dimension == 2) listOf("Triangles", "Quadrilaterals", "Polygons", "Curves", "All")
    else listOf("Polyhedra", "Round", "Prisms", "Advanced", "All")
    fun solidCategory(type: SolidType) = when (type) {
        SolidType.Cube, SolidType.Cuboid, SolidType.Tetrahedron, SolidType.TriangularPyramid, SolidType.Octahedron, SolidType.Pyramid, SolidType.Wedge -> "Polyhedra"
        SolidType.Sphere, SolidType.Hemisphere, SolidType.Cylinder, SolidType.Cone, SolidType.Frustum, SolidType.Torus, SolidType.Capsule -> "Round"
        SolidType.TriangularPrism, SolidType.PentagonalPrism, SolidType.HexagonalPrism, SolidType.OctagonalPrism -> "Prisms"
        else -> "Advanced"
    }
    fun selectKey(key: String, open: () -> Unit) {
        if (compareMode) {
            compareKeys = if (key in compareKeys) compareKeys - key else (compareKeys + key).takeLast(2)
        } else {
            open()
        }
    }
    fun dismissTopLayerOrNavigateBack() {
        when {
            formulaKey != null -> formulaKey = null
            compareKeys.isNotEmpty() -> compareKeys = emptyList()
            compareMode -> compareMode = false
            multiAdd -> multiAdd = false
            else -> vm.returnToMathMenu()
        }
    }
    BackHandler(
        enabled = formulaKey != null || compareKeys.isNotEmpty() || compareMode || multiAdd,
        onBack = ::dismissTopLayerOrNavigateBack,
    )
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        GlassPanel(
            Modifier
                .widthIn(max = if (wide) 820.dp else 520.dp)
                .fillMaxWidth(.96f),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    TransparentIcon("SE", Cyan)
                    Column {
                        Text("Shapes Explorer", color = Cyan, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                        Text("Choose a shape to load its interactive scene", color = Muted, fontSize = 11.sp)
                    }
                }
                GlowButton("Back", icon = "←", onClick = ::dismissTopLayerOrNavigateBack)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                GlowButton(if (dimension == 2) "2D - ${ShapeExplorer2DShapes.size}" else "2D", icon = "2D") {
                    dimension = 2
                    category = "Triangles"
                    if (!compareMode) compareKeys = emptyList()
                }
                GlowButton(if (dimension == 3) "3D - ${SolidType.entries.size}" else "3D", icon = "3D") {
                    dimension = 3
                    category = "Polyhedra"
                    if (!compareMode) compareKeys = emptyList()
                }
                GlowButton(if (multiAdd) "Add: ON" else "Add many", icon = "+") { multiAdd = !multiAdd; compareMode = false }
                GlowButton(if (compareMode) "Compare: ON" else "Compare", icon = "=") { compareMode = !compareMode; multiAdd = false; compareKeys = emptyList() }
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search shapes") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                categories.forEach { item -> GlowButton(if (category == item) "• $item" else item) { category = item } }
                GlowButton(if (favouritesOnly) "• Favourites" else "Favourites", icon = "★") { favouritesOnly = !favouritesOnly }
            }
            if (vm.recentShapeKeys.isNotEmpty()) {
                Text("RECENT", color = Amber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    vm.recentShapeKeys.take(5).forEach { key ->
                        val label = key.substringAfter(':').replace(Regex("([a-z])([A-Z])"), "$1 $2")
                        GlowButton(label, icon = "R") {
                            if (key.startsWith("2d:")) vm.loadExplorerShape2D(key.substringAfter(':'))
                            else runCatching { SolidType.valueOf(key.substringAfter(':')) }.getOrNull()?.let(vm::loadExplorerShape3D)
                        }
                    }
                }
            }
            Text(
                if (dimension == 2) "2D SHAPE LIBRARY" else "3D SOLID LIBRARY",
                color = if (dimension == 2) Violet else Green,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                if (dimension == 2) {
                    ShapeExplorer2DShapes.filter { preset ->
                        preset.label.contains(query, true) && (category == "All" || preset.category() == category) && (!favouritesOnly || "2d:${preset.id}" in vm.favoriteShapeKeys)
                    }.forEach { preset ->
                        val key = "2d:${preset.id}"
                        ShapeCatalogButton(preset.label, preset.label.take(2).uppercase(), key in vm.favoriteShapeKeys, compareKeys.contains(key),
                            onOpen = { selectKey(key) { if (multiAdd) vm.addExplorerShape2D(preset.id) else vm.loadExplorerShape2D(preset.id) } },
                            onFavourite = { vm.toggleFavoriteShape(key) },
                            onFormulas = { formulaKey = key })
                    }
                } else {
                    SolidType.entries.filter { type ->
                        type.name.contains(query, true) && (category == "All" || solidCategory(type) == category) && (!favouritesOnly || "3d:${type.name}" in vm.favoriteShapeKeys)
                    }.forEach { type ->
                        val key = "3d:${type.name}"
                        ShapeCatalogButton(type.name.replace(Regex("([a-z])([A-Z])"), "$1 $2"), "3D", key in vm.favoriteShapeKeys, compareKeys.contains(key),
                            onOpen = { selectKey(key) { if (multiAdd) vm.addExplorerShape3D(type) else vm.loadExplorerShape3D(type) } },
                            onFavourite = { vm.toggleFavoriteShape(key) },
                            onFormulas = { formulaKey = key })
                    }
                }
            }
            if (compareKeys.isNotEmpty()) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Violet.copy(.08f)).border(1.dp, Violet.copy(.35f), RoundedCornerShape(14.dp)).padding(9.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        if (compareKeys.size < 2) "Shape comparison - choose ${2 - compareKeys.size} more" else "Shape comparison",
                        color = Violet,
                        fontWeight = FontWeight.Bold,
                    )
                    val items = compareKeys.mapNotNull { key ->
                        val is2D = key.startsWith("2d:")
                        val preset = if (is2D) ShapeExplorer2DShapes.firstOrNull { it.id == key.substringAfter(':') } else null
                        val solid = if (!is2D) runCatching { SolidType.valueOf(key.substringAfter(':')) }.getOrNull() else null
                        val title = preset?.label ?: solid?.name?.replace(Regex("([a-z])([A-Z])"), "$1 $2") ?: return@mapNotNull null
                        val formulas = preset?.let { shape2DFormulaLibrary(it.label) } ?: solid?.let(::solidFormulaLibrary).orEmpty()
                        ComparisonItem(
                            id = key,
                            title = title,
                            primary = formulas.firstOrNull()?.expression ?: "Interactive construction",
                            attributes = listOf(
                                ComparisonAttribute("Dimension", if (is2D) "2D" else "3D"),
                                ComparisonAttribute("Category", preset?.category() ?: solid?.let(::solidCategory).orEmpty()),
                                ComparisonAttribute("Formula count", formulas.size.toString()),
                                ComparisonAttribute("Measurements", formulas.take(4).joinToString { it.name }),
                            ),
                        )
                    }
                    if (items.size == 2) {
                        SideBySideComparePanel(CompareModeEngine.compare(items[0], items[1]))
                    } else {
                        items.firstOrNull()?.let { item ->
                            MathFormulaText(item.primary, color = Cyan, fontSize = 13.sp)
                        }
                    }
                }
            }
            formulaKey?.let { key ->
                val is2D = key.startsWith("2d:")
                val preset = if (is2D) ShapeExplorer2DShapes.firstOrNull { it.id == key.substringAfter(':') } else null
                val solidType = if (!is2D) runCatching { SolidType.valueOf(key.substringAfter(':')) }.getOrNull() else null
                val title = preset?.label ?: solidType?.name?.replace(Regex("([a-z])([A-Z])"), "$1 $2") ?: "Shape"
                val formulas = preset?.let { shape2DFormulaLibrary(it.label) } ?: solidType?.let(::solidFormulaLibrary).orEmpty()
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xEE08131E)).border(1.dp, Violet.copy(.55f), RoundedCornerShape(16.dp)).padding(11.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("$title - All Formulas", color = Violet, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            Text("${formulas.size} formulas available", color = Muted, fontSize = 10.sp)
                        }
                        GlowButton("Close", icon = "X") { formulaKey = null }
                    }
                    formulas.forEachIndexed { index, formula ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp)).background(Violet.copy(.07f)).padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TransparentIcon("${index + 1}", Violet)
                            Column(Modifier.weight(1f)) {
                                Text(formula.name, color = Muted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                MathFormulaText(formula.expression, color = Ink, fontSize = 12.sp)
                            }
                        }
                    }
                    GlowButton("Open interactive scene", icon = "Open") {
                        preset?.let { vm.loadExplorerShape2D(it.id) } ?: solidType?.let(vm::loadExplorerShape3D)
                    }
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x22101824))
                    .border(1.dp, Cyan.copy(alpha = .28f), RoundedCornerShape(14.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TransparentIcon("Touch", Amber)
                Column {
                    Text("Interactive scene", color = Ink, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (dimension == 2) "Drag the glowing points to resize. Measurements and formulas update with the shape."
                        else "The solid opens in Scale mode. Drag the solid to resize; use two fingers to pan or zoom the view.",
                        color = Muted,
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun ShapeCatalogButton(label: String, icon: String, favourite: Boolean, selected: Boolean, onOpen: () -> Unit, onFavourite: () -> Unit, onFormulas: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(14.dp)).background(if (selected) Violet.copy(.20f) else Color(0x99101824)).border(1.dp, if (selected) Violet else Cyan.copy(.28f), RoundedCornerShape(14.dp)).clickable(onClick = onOpen).padding(7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        TransparentIcon(icon, if (selected) Violet else Cyan)
        Text(label, color = Ink, fontSize = 12.sp, maxLines = 1)
        Text("ƒ", color = Violet, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onFormulas).padding(5.dp).semantics { contentDescription = "Show all formulas for $label" })
        Text(if (favourite) "★" else "☆", color = if (favourite) Amber else Muted, fontSize = 18.sp, modifier = Modifier.clickable(onClick = onFavourite).padding(5.dp).semantics { contentDescription = "${if (favourite) "Remove" else "Add"} $label favourite" })
    }
}

@Composable
private fun MathematicsMenuPanel(
    vm: ExplorerViewModel,
    compact: Boolean,
    onMove: (Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    val launcherScrollState = rememberScrollState()
    var launcherQuery by rememberSaveable { mutableStateOf("") }
    var showWorkspaces by remember { mutableStateOf(false) }
    var showConcepts by remember { mutableStateOf(false) }
    var showSuggestions by remember { mutableStateOf(false) }
    var futureSelection by remember { mutableStateOf<String?>(null) }

    fun openWorkspaceOption(title: String) {
        vm.recordMathToolOpen(title)
        when (title) {
            "Learn All" -> vm.openMathsLearnAll()
            "Unified Math Studio" -> vm.openUnifiedMathStudio()
            "Adaptive Math Coach" -> vm.openAdaptiveMathLearning()
            "Scientific Calculator" -> vm.openScientificCalculator()
            "Math Notebook" -> vm.openMathNotebook()
            "Solver" -> vm.openSolver()
            "Problem Solver" -> vm.openProblemSolver()
            "Formulas" -> vm.openKnowledgeHub(KnowledgeSection.Formulas)
            "MCQs" -> vm.openKnowledgeHub(KnowledgeSection.Mcqs)
            "Formula Visualizer" -> vm.openKnowledgeHub(KnowledgeSection.Visualize)
            "Theorems" -> vm.openKnowledgeHub(KnowledgeSection.Theorems)
            "Visual Proofs" -> vm.openKnowledgeHub(KnowledgeSection.Proofs)
            "Dictionary", "Visual Dictionary" -> vm.openMathDictionary()
            "Probability & Statistics" -> vm.openProbabilityLab()
            "2D Geometry" -> vm.open(MathModule.Geometry2D)
            "3D Geometry" -> vm.open(MathModule.Geometry3D)
            "Graphs Explorer" -> vm.open(MathModule.Graph2D)
            "3D Graph" -> vm.open(MathModule.Graph3D)
            "Manipulatives" -> vm.open(MathModule.Manipulatives)
            "Shapes Explorer" -> vm.openShapesExplorer()
            "Set Theory & Logic" -> vm.openSetLogicVisualizer()
            "Explore Workspaces" -> showWorkspaces = !showWorkspaces
            "Math Concepts" -> showConcepts = !showConcepts
            else -> futureSelection = title
        }
    }

    val allTools = remember { (MathCreationTools + MathLearningTools + SuggestedMathTools).distinctBy { it.title } }
    val matchingTools = remember(launcherQuery) {
        val query = launcherQuery.trim().lowercase()
        if (query.isBlank()) allTools else allTools.filter { query in it.title.lowercase() || query in it.description.lowercase() }
    }

    @Composable
    fun LauncherTool(option: MathWorkspaceOption) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            GlowButton(option.title, icon = option.icon) { openWorkspaceOption(option.title) }
            Text(
                if (option.title in vm.pinnedMathTools) "★" else "☆",
                color = if (option.title in vm.pinnedMathTools) Amber else Muted,
                fontSize = 18.sp,
                modifier = Modifier.clickable { vm.togglePinnedMathTool(option.title) }.padding(5.dp)
                    .semantics { contentDescription = "${if (option.title in vm.pinnedMathTools) "Unpin" else "Pin"} ${option.title}" },
            )
        }
    }

    GlassPanel(
        modifier
            .adaptiveFocusGroup()
            .tvRemoteScrollable(launcherScrollState)
            .verticalScroll(launcherScrollState),
    ) {
        PanelHeader("Maths Tool Launcher", vm::toggleMathMenu, Cyan, icon = "⌕", onMove = onMove)
        Text(vm.mathsBreadcrumb.joinToString(" → "), color = Green, fontSize = 11.sp, modifier = Modifier.semantics { contentDescription = "Maths breadcrumb" })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlowButton("Home", icon = "H", onClick = vm::openSubjectHub)
            GlowButton("Current Workspace", onClick = vm::toggleMathMenu)
        }
        OutlinedTextField(
            value = launcherQuery,
            onValueChange = { launcherQuery = it },
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Search maths tools" },
            label = { Text("Search tools, topics or activities") },
            singleLine = true,
        )
        if (launcherQuery.isBlank()) {
            val pinned = allTools.filter { it.title in vm.pinnedMathTools }
            val recent = vm.recentMathTools.mapNotNull { title -> allTools.singleOrNull { it.title == title } }.take(4)
            val frequent = allTools.filter { (vm.mathToolUseCounts[it.title] ?: 0) > 0 }.sortedByDescending { vm.mathToolUseCounts[it.title] }.take(4)
            if (pinned.isNotEmpty()) {
                Text("PINNED", color = Amber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) { pinned.forEach { LauncherTool(it) } }
            }
            if (recent.isNotEmpty()) {
                Text("RECENT", color = Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) { recent.forEach { LauncherTool(it) } }
            }
            if (frequent.isNotEmpty()) {
                Text("FREQUENTLY USED", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) { frequent.forEach { LauncherTool(it) } }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (launcherQuery.isNotBlank()) Text("${matchingTools.size} matching tools", color = Green, fontSize = 11.sp)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("CREATE & SOLVE", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    MathCreationTools.filter { it in matchingTools }.forEach { LauncherTool(it) }
                }

                AnimatedVisibility(showWorkspaces) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Interactive workspaces", color = Muted, fontSize = 11.sp)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            MathModule.entries.forEach { module -> GlowButton(module.label, onClick = { vm.open(module) }) }
                        }
                    }
                }

                Text("LEARN & PRACTISE", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    MathLearningTools.filter { it in matchingTools }.forEach { LauncherTool(it) }
                }

                AnimatedVisibility(showConcepts) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x22101824))
                            .border(1.dp, Violet.copy(alpha = .38f), RoundedCornerShape(14.dp))
                            .padding(9.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Text(
                            "Math Concepts - ${MathConceptCatalog.concepts.size} subjects - ${MathConceptCatalog.concepts.sumOf { it.subtopics.size }} sub-concepts",
                            color = Violet,
                            fontWeight = FontWeight.SemiBold,
                        )
                        MathConceptCatalog.concepts.forEach { concept ->
                            Column(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                    .background(Violet.copy(alpha = .06f))
                                    .border(1.dp, Violet.copy(alpha = .22f), RoundedCornerShape(10.dp))
                                    .padding(7.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                        .background(Violet.copy(alpha = .10f))
                                        .border(1.dp, Violet.copy(alpha = .35f), RoundedCornerShape(12.dp))
                                        .clickable { vm.openConceptLibrary(concept.title) }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                                ) {
                                    MathConceptIconImage(concept.title, Modifier.size(34.dp), 10.dp)
                                    Text("${concept.title} - All", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    concept.subtopics.forEach { subConcept ->
                                        GlowButton(subConcept) {
                                            vm.openConceptLibrary(concept.title, subConcept)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                GlowButton(if (showSuggestions) "Hide more ideas" else "More workspace ideas") {
                    showSuggestions = !showSuggestions
                }
                AnimatedVisibility(showSuggestions) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("SUGGESTED NEXT", color = Amber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            SuggestedMathTools.forEach { option ->
                                GlowButton(option.title, icon = option.icon) { futureSelection = option.title }
                            }
                        }
                    }
                }

                futureSelection?.let { selection ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Amber.copy(alpha = .08f))
                            .border(1.dp, Amber.copy(alpha = .28f), RoundedCornerShape(12.dp))
                            .padding(9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TransparentIcon("+", Amber)
                        Column(Modifier.weight(1f)) {
                            Text(selection, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Text("Workspace button ready for content to be added.", color = Muted, fontSize = 10.sp)
                        }
                    }
                }
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
private fun TopShell(
    vm: ExplorerViewModel,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val adaptiveProfile = LocalAdaptiveDeviceProfile.current
    val initialTvFocus = remember { FocusRequester() }
    var expanded by remember { mutableStateOf(!compact) }
    val activity = LocalActivity.current
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val scope = rememberCoroutineScope()
    val importProject = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null || activity == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching { MathFileExchange.readProject(activity, uri) }
                .onSuccess { result ->
                    result.state?.let { vm.importWorkspace(it, result.recovered, result.diagnostics) }
                        ?: vm.reportStatus(result.diagnostics.firstOrNull() ?: "This project could not be imported")
                }
                .onFailure { vm.reportStatus("Import failed: ${it.message ?: "invalid project"}") }
        }
    }
    if (adaptiveProfile.isTelevision) {
        LaunchedEffect(Unit) {
            if (adaptiveProfile.interactionPolicy.requestInitialNavigationFocus) {
                initialTvFocus.requestFocus()
            }
        }
        Column(
            modifier
                .width(208.dp)
                .fillMaxHeight()
                .adaptiveFocusGroup()
                .clip(RoundedCornerShape(18.dp))
                .background(SurfaceA.copy(alpha = .9f))
                .border(1.dp, Cyan.copy(alpha = .38f), RoundedCornerShape(18.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
                Text("AI Maths Explorer", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Text(vm.mathsBreadcrumb.joinToString(" → "), color = Cyan, fontSize = 11.sp, maxLines = 2)
            }
            GlowButton("Back", icon = "←", modifier = Modifier.focusRequester(initialTvFocus)) {
                backDispatcher?.onBackPressed() ?: vm.navigateBackIntent()
            }
            GlowButton("Maths menu", icon = "≡", onClick = vm::toggleMathMenu)
            Box(Modifier.fillMaxWidth().height(1.dp).background(Cyan.copy(alpha = .2f)))
            GlowButton("Undo", enabled = vm.canUndo, onClick = vm::undo)
            GlowButton("Redo", enabled = vm.canRedo, onClick = vm::redo)
            GlowButton("Save", onClick = vm::saveWorkspace)
            GlowButton("Import") { importProject.launch(arrayOf("application/*", "text/plain")) }
            Spacer(Modifier.weight(1f))
            Text("Use ↑ ↓ ← → and OK", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(6.dp))
        }
        return
    }
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
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            GlowButton(
                "Back",
                icon = "←",
                iconOnly = compact,
                modifier = Modifier.semantics { contentDescription = "Back to previous screen" },
                onClick = { backDispatcher?.onBackPressed() ?: vm.navigateBackIntent() },
            )
            GlowButton(
                "Menu",
                icon = "≡",
                iconOnly = compact,
                modifier = Modifier.semantics { contentDescription = "Open Maths menu" },
                onClick = vm::toggleMathMenu,
            )
        }
        Column(
            Modifier.clickable { expanded = !expanded }.padding(horizontal = 5.dp)
                .semantics { contentDescription = "Breadcrumb ${vm.mathsBreadcrumb.joinToString(" → ")}" },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("AI Maths Explorer ${if (expanded) "⌃" else "⌄"}", color = Ink, fontSize = if (compact) 18.sp else 24.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                vm.mathsBreadcrumb.joinToString(" → "),
                color = Muted,
                fontSize = if (compact) 9.sp else 12.sp,
                maxLines = 1,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 8.dp)) {
            AnimatedVisibility(expanded || !compact) { GlowButton(if (compact) "↶" else "Undo", enabled = vm.canUndo, onClick = vm::undo) }
            AnimatedVisibility(expanded || !compact) { GlowButton(if (compact) "↷" else "Redo", enabled = vm.canRedo, onClick = vm::redo) }
            AnimatedVisibility(expanded && !compact) { GlowButton("Save", onClick = vm::saveWorkspace) }
            AnimatedVisibility(expanded && !compact) { GlowButton("Import") { importProject.launch(arrayOf("application/*", "text/plain")) } }
            AnimatedVisibility(expanded && !compact) {
                GlowButton("Share") {
                    if (activity == null) vm.reportStatus("Sharing is unavailable in this window")
                    else scope.launch { runCatching { MathFileExchange.shareProject(activity, vm.state) }.onFailure { vm.reportStatus("Share failed: ${it.message}") } }
                }
            }
            AnimatedVisibility(expanded && !compact) {
                GlowButton("PNG") {
                    if (activity == null) vm.reportStatus("Image export is unavailable in this window")
                    else scope.launch { runCatching { MathFileExchange.sharePng(activity, vm.state) }.onFailure { vm.reportStatus("Image export failed: ${it.message}") } }
                }
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
        Text("Progress $progress - ${vm.activeProgress?.percent(activity) ?: 0}% current", color = Cyan, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text(activity.title, color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(activity.objective, color = Muted, fontSize = 13.sp)
        Insight("Allowed", activity.allowedTools.joinToString().ifBlank { "Open exploration" }, Cyan)
        Insight(
            "Linked maths",
            "CAS ${linkedSnapshot.objectsFor(LinkedMathView.CAS).size} - Graph ${linkedSnapshot.objectsFor(LinkedMathView.Graph).size} - Table ${linkedSnapshot.objectsFor(LinkedMathView.Table).size} - Geometry ${linkedSnapshot.objectsFor(LinkedMathView.Geometry).size} - Probability ${linkedSnapshot.objectsFor(LinkedMathView.Probability).size}",
            Violet,
        )
        Insight(
            "Maths authority",
            "${universalDocument.objects.size} typed objects - revision ${universalDocument.revision} - one dependency graph",
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
            Text("Attempts ${it.attempts} - hints ${it.hintsUsed} - offline changes ${vm.pendingLearningOperations}", color = Muted, fontSize = 11.sp)
        }
        if (vm.learningRole == LearningRole.Teacher) {
            val summary = vm.teacherSummary
            Text("Teacher dashboard", color = Ink, fontWeight = FontWeight.SemiBold)
            Insight("Assignment", vm.assignments.first().title, Violet)
            Insight("Completion", "${summary.completedLessons}/${summary.assignedLessons} lessons - ${summary.checkpointsCompleted} checkpoints", Green)
            Insight("Support", "${summary.attempts} attempts - ${summary.hintsUsed} hints", Amber)
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
    val reliability = remember { LocalReliabilityMonitor(context).snapshot() }
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
                Text("Checksummed save - exchange coverage - adaptive quality", color = Muted, fontSize = 11.sp)
            }
            GlowButton(if (expanded) "Hide" else "Inspect") { expanded = !expanded }
        }
        Insight("Runtime", "${performance.status} ${performance.score}/100 - ${device.tier}", if (performance.score >= 90) Green else Amber)
        if (expanded) {
            Insight("Local reliability", "${"%.2f".format(reliability.cleanSessionRate * 100)}% clean - ${reliability.sessions} sessions", if (reliability.cleanSessionRate >= .998) Green else Amber)
            Insight("Project archive", "${archive.toByteArray().size / 1024} KB - ${document.objects.size} typed maths objects", Cyan)
            Insight("GeoGebra XML", "${exchange.exported} translated - ${exchange.skipped.size} explicitly skipped", Violet)
            Insight("Fallback", if ("live AR" in device.enabled) "Live AR available" else "Full simulator and 2D maths remain enabled", Amber)
            Insight("Surface density", device.recommendedSurfaceDensity.toString(), Cyan)
            performance.messages.forEach { Text(it, color = if (performance.status.name == "Pass") Green else Amber, fontSize = 11.sp) }
            exchange.skipped.take(3).forEach { Text("Exchange gap - $it", color = Amber, fontSize = 10.sp) }
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
        Insight("Activity", "${document.blocks.size} blocks - ${if (validation.valid) "ready" else "needs links"} - revision ${document.revision}", if (validation.valid) Green else Amber)
        if (expanded) {
            if (role == LearningRole.Teacher) {
                Text("Author blocks", color = Cyan, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf("Instruction", "Math check", "MCQ", "Workspace", "Tiles", "Proof", "Branch", "Reflection").forEach { label -> TransparentIcon(label.take(2), Cyan) }
                }
                document.blocks.forEachIndexed { index, block -> Text("${index + 1}. ${block.javaClass.simpleName} - ${block.title}", color = Muted, fontSize = 11.sp) }
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
                Text("Export - ${InteractiveActivityAuthoring.serialize(document).take(72)}…", color = Muted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            } else if (run.completed) {
                Text("Activity complete - score ${run.score}%", color = Green, fontWeight = FontWeight.Bold)
                run.mastery.values.forEach { Insight(it.skill, "${trim(it.score * 100)}% - ${it.band}", Green) }
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
                        IntentAwareMathField(response, { response = it }, "Your expression", Modifier.fillMaxWidth(), showLegend = false)
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
            DestructiveGlowButton("Delete", onClick = onDelete)
        }
    }
}

private data class ShapeExplorer2DDetails(val formula: String, val area: Double, val perimeter: Double)

private fun copyShapeText(context: android.content.Context, label: String, value: String) {
    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    clipboard.setPrimaryClip(android.content.ClipData.newPlainText(label, value))
}

private fun safeShapeFileName(title: String) = title.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifBlank { "shape" }

private fun writeShapeTextAsset(context: android.content.Context, title: String, extension: String, content: String): java.io.File {
    val file = java.io.File(context.cacheDir, "${safeShapeFileName(title)}.$extension")
    file.writeText(content)
    return file
}

private fun writeShapePdf(context: android.content.Context, title: String, lines: List<String>): java.io.File {
    val document = android.graphics.pdf.PdfDocument()
    val page = document.startPage(android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create())
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(15, 30, 45); textSize = 18f }
    page.canvas.drawText(title, 42f, 58f, paint.apply { textSize = 24f; isFakeBoldText = true })
    paint.isFakeBoldText = false; paint.textSize = 14f
    lines.take(42).forEachIndexed { index, line -> page.canvas.drawText(line.take(76), 42f, 92f + index * 17f, paint) }
    document.finishPage(page)
    val file = java.io.File(context.cacheDir, "${safeShapeFileName(title)}.pdf")
    java.io.FileOutputStream(file).use(document::writeTo)
    document.close()
    return file
}

private fun writeShapePng(context: android.content.Context, title: String, subtitle: String): java.io.File {
    val bitmap = android.graphics.Bitmap.createBitmap(1000, 1000, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.rgb(3, 8, 14))
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(32, 217, 255); textAlign = android.graphics.Paint.Align.CENTER; textSize = 62f; isFakeBoldText = true }
    canvas.drawText(title, 500f, 430f, paint)
    paint.color = android.graphics.Color.rgb(220, 230, 242); paint.textSize = 32f; paint.isFakeBoldText = false
    canvas.drawText(subtitle.take(54), 500f, 510f, paint)
    paint.style = android.graphics.Paint.Style.STROKE; paint.strokeWidth = 6f; paint.color = android.graphics.Color.rgb(152, 93, 255)
    canvas.drawRoundRect(90f, 90f, 910f, 910f, 42f, 42f, paint)
    val file = java.io.File(context.cacheDir, "${safeShapeFileName(title)}.png")
    java.io.FileOutputStream(file).use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    bitmap.recycle()
    return file
}

private fun shape2DSvg(shape: Shape2D, points: List<Vec2>): String {
    val shapePoints = shape.pointIndices.mapNotNull { points.getOrNull(it) }
    val body = when (shape.type) {
        Shape2DType.Circle -> if (shapePoints.size >= 2) "<circle cx='400' cy='400' r='${shapePoints[0].distanceTo(shapePoints[1]) * 80}'/>" else ""
        Shape2DType.Ellipse -> if (shapePoints.size >= 3) "<ellipse cx='400' cy='400' rx='${shapePoints[0].distanceTo(shapePoints[1]) * 80}' ry='${shapePoints[0].distanceTo(shapePoints[2]) * 80}'/>" else ""
        else -> "<polygon points='${shapePoints.joinToString(" ") { "${400 + it.x * 80},${400 - it.y * 80}" }}'/ >".replace("/ >", "/>")
    }
    return "<svg xmlns='http://www.w3.org/2000/svg' width='800' height='800' viewBox='0 0 800 800'><g fill='#20d9ff33' stroke='#20d9ff' stroke-width='5'>$body</g></svg>"
}

private fun solidObj(solid: Solid): String {
    val mesh = SolidMeshFactory.create(solid)
    return buildString {
        appendLine("o ${solid.type.name}")
        mesh.vertices.forEach { appendLine("v ${it.x} ${it.y} ${it.z}") }
        mesh.faces.forEach { face -> appendLine("f ${face.joinToString(" ") { (it + 1).toString() }}") }
    }
}

@Composable
private fun Shape2DStudioPanel(vm: ExplorerViewModel, shape: Shape2D, compact: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val preset = ShapeExplorer2DShapes.firstOrNull { shape.id.contains("shape-explorer-${it.id}") }
    val formulas = shape2DFormulaLibrary(preset?.label ?: shape.name)
    val details = shapeExplorer2DDetails(shape, vm.state.points)
    var name by remember(shape.id, shape.name) { mutableStateOf(shape.name) }
    var scaleText by remember(shape.id) { mutableStateOf("1.25") }
    val shapePoints = shape.pointIndices.mapNotNull { vm.state.points.getOrNull(it) }
    val currentWidth = (shapePoints.maxOfOrNull { it.x } ?: 1.0) - (shapePoints.minOfOrNull { it.x } ?: 0.0)
    val currentHeight = (shapePoints.maxOfOrNull { it.y } ?: 1.0) - (shapePoints.minOfOrNull { it.y } ?: 0.0)
    var widthText by remember(shape.id) { mutableStateOf(trim(currentWidth)) }
    var heightText by remember(shape.id) { mutableStateOf(trim(currentHeight)) }
    var keepProportions by remember(shape.id) { mutableStateOf(true) }
    var unit by remember { mutableStateOf("unit") }
    var lessonStep by remember { mutableIntStateOf(0) }
    val unitFactor = when (unit) { "cm" -> 100.0; "mm" -> 1000.0; "in" -> 39.3701; else -> 1.0 }
    val allShapeDetails = vm.state.shapes.map { shapeExplorer2DDetails(it, vm.state.points) }
    val panelModifier = if (compact) modifier.fillMaxWidth(.96f) else modifier.padding(top = 64.dp).width(380.dp)
    GlassPanel(panelModifier) {
        PanelHeader("2D Shape Studio", vm::hidePanels, Violet, icon = "ƒ")
        OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, label = { Text("Object name") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            GlowButton("Rename") { vm.renameExplorerSelection(name) }
            GlowButton("Duplicate", onClick = vm::duplicateExplorerSelection)
            GlowButton("Reset", onClick = vm::resetExplorerSelection)
        }
        Text("PRECISION & TRANSFORM", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CompactMathField(value = widthText, onValueChange = { widthText = it }, label = "Width", modifier = Modifier.weight(1f))
            CompactMathField(value = heightText, onValueChange = { heightText = it }, label = "Height", modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GlowButton(if (keepProportions) "• Keep ratio" else "Free ratio") { keepProportions = !keepProportions }
            GlowButton("Set dimensions") { vm.resizeExplorerShape2D(widthText.toDoubleOrNull() ?: currentWidth, heightText.toDoubleOrNull() ?: currentHeight, keepProportions) }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CompactMathField(value = scaleText, onValueChange = { scaleText = it }, label = "Scale", modifier = Modifier.width(110.dp))
            GlowButton("Apply") { scaleText.toDoubleOrNull()?.let(vm::scaleExplorerShape2D) }
            GlowButton(if (shape.locked) "Unlock" else "Lock") { vm.updateSelectedShape { it.copy(locked = !it.locked) } }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GlowButton("Rotate -15°") { vm.rotateExplorerShape2D(-15.0) }
            GlowButton("Rotate +15°") { vm.rotateExplorerShape2D(15.0) }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf("unit", "cm", "mm", "in").forEach { choice -> GlowButton(if (unit == choice) "• $choice" else choice) { unit = choice } }
        }
        Insight("Area", "${trim(details.area * unitFactor * unitFactor)} ${unit}^2", Violet)
        Insight("Perimeter", "${trim(details.perimeter * unitFactor)} $unit", Green)
        if (vm.state.shapes.size > 1) {
            Insight("Composite area", trim(allShapeDetails.sumOf { it.area }), Amber)
            Insight("Composite perimeter", trim(allShapeDetails.sumOf { it.perimeter }), Amber)
        }
        Text("ALL FORMULAS - ${formulas.size}", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        formulas.forEach { formula -> Insight(formula.name, latexStyleFormula(formula.expression), Violet) }
        GlowButton("Copy all formulas", icon = "Copy") { copyShapeText(context, "${shape.name} formulas", formulas.joinToString("\n") { "${it.name}: ${it.expression}" }) }
        Text("SUBSTITUTION & METHOD", color = Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text("Current result: A = ${trim(details.area)}, P = ${trim(details.perimeter)}. Measure the labelled control points, substitute them into the selected identity, preserve units, then simplify.", color = Ink, fontSize = 11.sp)
        Insight("Variables", "A area - P perimeter - a,b sides - h height - r radius", Cyan)
        Insight("Symmetry", when (preset?.label) { "Circle" -> "infinitely many lines"; "Square" -> "4 lines, order 4"; "Rectangle", "Rhombus" -> "2 lines, order 2"; "Equilateral Triangle" -> "3 lines, order 3"; else -> "depends on current geometry" }, Amber)
        Insight("Angles", "Interior sum = (n-2) × 180°", Amber)
        Insight("Circles", "Inradius r = A/s - circumradius R from perpendicular bisectors", Amber)
        Text("Decompose into triangles to verify area; combine multiple objects for composite figures. Resizing demonstrates that perimeter scales by k while area scales by k².", color = Muted, fontSize = 10.sp)
        Text("GUIDED CONSTRUCTION", color = Amber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        val steps = listOf("Identify defining dimensions", "Place the first control point", "Construct remaining vertices", "Check constraints and symmetry", "Verify area and perimeter")
        Text("${lessonStep + 1}/${steps.size} - ${steps[lessonStep]}", color = Ink, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GlowButton("Previous", enabled = lessonStep > 0) { lessonStep-- }
            GlowButton("Next", enabled = lessonStep < steps.lastIndex) { lessonStep++ }
            GlowButton("Challenge") { lessonStep = (lessonStep + 1) % steps.size }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GlowButton("Save scene", onClick = vm::saveWorkspace)
            GlowButton("Copy results") { copyShapeText(context, shape.name, "${shape.name}: area=${trim(details.area)}, perimeter=${trim(details.perimeter)}") }
        }
        Text("EXPORT", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            GlowButton("SVG") { val file = writeShapeTextAsset(context, shape.name, "svg", shape2DSvg(shape, vm.state.points)); copyShapeText(context, "SVG export", file.absolutePath) }
            GlowButton("PNG") { val file = writeShapePng(context, shape.name, "Area ${trim(details.area)} - Perimeter ${trim(details.perimeter)}"); copyShapeText(context, "PNG export", file.absolutePath) }
            GlowButton("PDF") { val file = writeShapePdf(context, shape.name, formulas.map { "${it.name}: ${it.expression}" } + "Area: ${trim(details.area)}" + "Perimeter: ${trim(details.perimeter)}"); copyShapeText(context, "PDF export", file.absolutePath) }
        }
    }
}

@Composable
private fun Shape3DStudioPanel(vm: ExplorerViewModel, index: Int, solid: Solid, compact: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val formulas = solidFormulaLibrary(solid.type)
    val measurements = Geometry3D.measure(solid)
    var unit by remember { mutableStateOf("unit") }
    var fold by remember { mutableFloatStateOf(0f) }
    var lessonStep by remember { mutableIntStateOf(0) }
    val factor = when (unit) { "cm" -> 100.0; "mm" -> 1000.0; "in" -> 39.3701; else -> 1.0 }
    val totals = vm.state.solids.map(Geometry3D::measure)
    val panelModifier = if (compact) modifier.fillMaxWidth(.96f) else modifier.padding(top = 64.dp).width(390.dp)
    GlassPanel(panelModifier) {
        PanelHeader("3D Shape Studio", vm::hidePanels, Violet, icon = "3D")
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            GlowButton("Duplicate", onClick = vm::duplicateExplorerSelection)
            GlowButton("Reset", onClick = vm::resetExplorerSelection)
            GlowButton("AR", icon = "AR", onClick = vm::openCurrentShapeInAr)
        }
        Text("EXACT DIMENSIONS", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        AxisSlider("Width", solid.width.toFloat(), .2f..12f) { value -> vm.transformSolid(index) { it.copy(width = value.toDouble()) } }
        AxisSlider("Height", solid.height.toFloat(), .2f..12f) { value -> vm.transformSolid(index) { it.copy(height = value.toDouble()) } }
        AxisSlider("Depth", solid.depth.toFloat(), .2f..12f) { value -> vm.transformSolid(index) { it.copy(depth = value.toDouble()) } }
        AxisSlider("Radius", solid.radius.toFloat(), .1f..6f) { value -> vm.transformSolid(index) { it.copy(radius = value.toDouble()) } }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            GlowButton("Rotate X") { vm.transformSolid(index) { it.copy(rotation = it.rotation + Vec3(15.0, 0.0, 0.0)) } }
            GlowButton("Rotate Y") { vm.transformSolid(index) { it.copy(rotation = it.rotation + Vec3(0.0, 15.0, 0.0)) } }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) { listOf("unit", "cm", "mm", "in").forEach { choice -> GlowButton(if (unit == choice) "• $choice" else choice) { unit = choice } } }
        Insight("Surface area", "${trim(measurements.surfaceArea * factor * factor)} ${unit}^2", Violet)
        Insight("Volume", "${trim(measurements.volume * factor * factor * factor)} ${unit}^3", Green)
        Insight("Topology", "${measurements.faces} faces - ${measurements.edges} edges - ${measurements.vertices} vertices", Cyan)
        if (vm.state.solids.size > 1) {
            Insight("Composite surface", trim(totals.sumOf { it.surfaceArea }), Amber)
            Insight("Composite volume", trim(totals.sumOf { it.volume }), Amber)
        }
        Text("ALL FORMULAS - ${formulas.size}", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        formulas.forEach { formula -> Insight(formula.name, latexStyleFormula(formula.expression), Violet) }
        GlowButton("Copy all formulas", icon = "Copy") { copyShapeText(context, "${solid.type} formulas", formulas.joinToString("\n") { "${it.name}: ${it.expression}" }) }
        Text("DERIVATION & VARIABLES", color = Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text("Substitute the live dimensions shown above. Base-area methods use V = Bh for prisms and V = Bh/3 for pyramids; surface area is the sum of every exposed face.", color = Ink, fontSize = 11.sp)
        Insight("Variables", "B base - p perimeter - h height - r radius - s slant", Cyan)
        Insight("Bounds", "${trim(solid.width)} × ${trim(solid.height)} × ${trim(solid.depth)}", Amber)
        Text("NET & CROSS-SECTION", color = Amber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text("Net: ${measurements.faces} connected face panels - fold ${trim(fold.toDouble() * 100)}%", color = Ink, fontSize = 11.sp)
        Slider(value = fold, onValueChange = { fold = it }, valueRange = 0f..1f)
        Text("Use the 3D Tools pane outside Shape Studio for live clipping and planar cross-sections. Scaling by k preserves shape while surface changes by k² and volume by k³.", color = Muted, fontSize = 10.sp)
        val steps = listOf("Identify the base", "Construct or inspect the net", "Fold faces around shared edges", "Measure height and radius", "Verify surface area and volume")
        Text("Lesson ${lessonStep + 1}/${steps.size} - ${steps[lessonStep]}", color = Ink, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GlowButton("Previous", enabled = lessonStep > 0) { lessonStep-- }
            GlowButton("Next", enabled = lessonStep < steps.lastIndex) { lessonStep++ }
            GlowButton("Challenge") { lessonStep = (lessonStep + 1) % steps.size }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GlowButton("Save scene", onClick = vm::saveWorkspace)
            GlowButton("Copy results") { copyShapeText(context, solid.type.name, "${solid.type}: surface=${trim(measurements.surfaceArea)}, volume=${trim(measurements.volume)}") }
        }
        Text("EXPORT", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            GlowButton("OBJ model") { val file = writeShapeTextAsset(context, solid.type.name, "obj", solidObj(solid)); copyShapeText(context, "OBJ export", file.absolutePath) }
            GlowButton("PNG") { val file = writeShapePng(context, solid.type.name, "Surface ${trim(measurements.surfaceArea)} - Volume ${trim(measurements.volume)}"); copyShapeText(context, "PNG export", file.absolutePath) }
            GlowButton("PDF") { val file = writeShapePdf(context, solid.type.name, formulas.map { "${it.name}: ${it.expression}" } + "Surface: ${trim(measurements.surfaceArea)}" + "Volume: ${trim(measurements.volume)}"); copyShapeText(context, "PDF export", file.absolutePath) }
        }
        GlowButton("Collapse properties ▲", icon = "X", onClick = vm::hidePanels)
    }
}

private fun shapeExplorer2DDetails(shape: Shape2D, allPoints: List<Vec2>): ShapeExplorer2DDetails {
    val points = shape.pointIndices.mapNotNull { index -> allPoints.getOrNull(index) }
    val preset = ShapeExplorer2DShapes.firstOrNull { shape.id == "shape-explorer-${it.id}" }
    if (shape.type == Shape2DType.Circle && points.size >= 2) {
        val radius = points[0].distanceTo(points[1])
        return ShapeExplorer2DDetails(preset?.formula.orEmpty(), PI * radius * radius, 2 * PI * radius)
    }
    if (shape.type == Shape2DType.Ellipse && points.size >= 3) {
        val a = points[0].distanceTo(points[1])
        val b = points[0].distanceTo(points[2])
        val perimeter = PI * (3 * (a + b) - kotlin.math.sqrt((3 * a + b) * (a + 3 * b)))
        return ShapeExplorer2DDetails(preset?.formula.orEmpty(), PI * a * b, perimeter)
    }
    if (shape.type in setOf(Shape2DType.Rectangle, Shape2DType.Square) && points.size >= 2) {
        val width = abs(points[1].x - points[0].x)
        val height = if (shape.type == Shape2DType.Square) width else abs(points[1].y - points[0].y)
        return ShapeExplorer2DDetails(preset?.formula.orEmpty(), width * height, 2 * (width + height))
    }
    val perimeter = if (points.size >= 2) points.indices.sumOf { index -> points[index].distanceTo(points[(index + 1) % points.size]) } else 0.0
    return ShapeExplorer2DDetails(preset?.formula.orEmpty(), Geometry2D.polygonArea(points), perimeter)
}

@Composable
private fun Geometry2DScreen(vm: ExplorerViewModel, compact: Boolean, onRequestClearAll: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val adaptiveProfile = LocalAdaptiveDeviceProfile.current
    val workspaceToolTop = if (adaptiveProfile.isTelevision) {
        adaptiveProfile.workspacePolicy.topChromeClearance
    } else {
        72.dp
    }
    var lassoEnabled by remember { mutableStateOf(false) }
    var boxSelectEnabled by remember { mutableStateOf(false) }
    var contextMenuShapeIndex by remember { mutableStateOf<Int?>(null) }
    var axisConstraint by remember { mutableStateOf(AxisConstraint.Free) }
    var precisionMode by remember { mutableStateOf(false) }
    var homeRequest by remember { mutableIntStateOf(0) }
    var undoViewRequest by remember { mutableIntStateOf(0) }
    var addShapeOpen by remember { mutableStateOf(false) }
    var contextToolsExpanded by remember { mutableStateOf(false) }
    var constraintsExpanded by remember { mutableStateOf(false) }
    var viewToolsExpanded by remember { mutableStateOf(false) }
    var layersExpanded by remember { mutableStateOf(false) }
    var manipulationMode by remember { mutableStateOf(Transform2DMode.Select) }
    var resizePolicy by remember { mutableStateOf(Geometry2DResizePolicy.Free) }
    var rotationAngle by remember { mutableDoubleStateOf(0.0) }
    var lastRotationHaptic by remember { mutableStateOf<Double?>(null) }
    var objectDetailsExpanded by remember { mutableStateOf(false) }
    BackHandler(enabled = addShapeOpen) { addShapeOpen = false }
    BackHandler(enabled = objectDetailsExpanded) { objectDetailsExpanded = false }
    val selectedShape = vm.state.shapes.getOrNull(vm.selectedShape)
    LaunchedEffect(selectedShape?.id) {
        rotationAngle = 0.0
        lastRotationHaptic = null
        objectDetailsExpanded = false
        manipulationMode = Transform2DMode.Select
    }
    val dependenciesByOutput = vm.state.pointDependencies.associateBy { it.outputIndex }
    val invalidDependencyOutputs = vm.state.pointDependencies.filter {
        resolvePointDependency(vm.state.points, it.inputIndices, it.type, it.parameters) == null
    }.mapTo(mutableSetOf()) { it.outputIndex }
    val protocolSize = vm.state.points.size + vm.state.shapes.size + vm.state.geometryConstraints.size
    var protocolStep by remember { mutableFloatStateOf(protocolSize.toFloat()) }
    var focusedProtocolId by remember { mutableStateOf<String?>(null) }
    var protocolPlaying by remember { mutableStateOf(false) }
    var macroDraft by remember { mutableStateOf(com.indianservers.aiexplorer.workspace.GeometryMacroDraft("Canvas macro")) }
    var geometryTrace by remember { mutableStateOf<com.indianservers.aiexplorer.workspace.GeometryTraceSession?>(null) }
    LaunchedEffect(protocolSize) { protocolStep = protocolSize.toFloat() }
    LaunchedEffect(protocolPlaying, protocolSize) {
        while (protocolPlaying && protocolStep < protocolSize) { delay(650); protocolStep = (protocolStep + 1f).coerceAtMost(protocolSize.toFloat()) }
        if (protocolStep >= protocolSize) protocolPlaying = false
    }
    LaunchedEffect(vm.selectedShapes, vm.selectedPoint, macroDraft.recording) {
        val ids = vm.selectedShapes.mapNotNull(vm.state.shapes::getOrNull).map { it.id } + vm.selectedPoint.takeIf { it in vm.state.points.indices }?.let { "P${it + 1}" }
        macroDraft = Geometry2DDirectManipulation.recordMacro(macroDraft, ids.filterNotNull())
    }
    LaunchedEffect(vm.state.points, geometryTrace?.recording) {
        val trace = geometryTrace ?: return@LaunchedEffect
        val point = trace.objectId.removePrefix("P").toIntOrNull()?.minus(1)?.let(vm.state.points::getOrNull) ?: return@LaunchedEffect
        geometryTrace = Geometry2DDirectManipulation.appendTrace(trace, point)
    }
    val protocolTimeline = remember(vm.state, protocolStep, focusedProtocolId) {
        Geometry2DInteractionEngine.protocolTimeline(vm.state, protocolStep.roundToInt(), focusedProtocolId)
    }
    val contextInspector = remember(vm.state, vm.selectedPoint, vm.selectedShapes) {
        Geometry2DInteractionEngine.inspect(vm.state, vm.selectedPoint, vm.selectedShapes)
    }
    val constraintSuggestions = remember(vm.state, vm.selectedPoint, vm.selectedShapes) {
        Geometry2DInteractionEngine.constraintSuggestions(vm.state, vm.selectedPoint, vm.selectedShapes)
    }
    val constraintFeedback = remember(vm.state) { Geometry2DInteractionEngine.evaluateConstraints(vm.state) }
    val constraintGlyphs = remember(vm.state) { Geometry2DDirectManipulation.constraintGlyphs(vm.state) }
    val replayingProtocol = protocolStep.roundToInt() < protocolSize
    val replayShapes = remember(vm.state.shapes, protocolTimeline.visibleIds) {
        vm.state.shapes.map { shape -> shape.copy(visible = shape.visible && shape.id in protocolTimeline.visibleIds) }
    }
    Box(Modifier.fillMaxSize()) {
        CoordinateCanvas(
            modifier = Modifier.fillMaxSize()
                .background(WorkspaceVisualStyles.ReferenceNavy)
                .appWorkspaceTreatment(0.dp, WorkspaceVisualStyles.ReferenceCyan, WorkspaceVisualStyles.ReferenceMagenta)
                .semantics {
                contentDescription = "Interactive coordinate geometry canvas. ${contextInspector.title}. Parents ${contextInspector.parentIds.joinToString().ifBlank { "none" }}. Dependents ${contextInspector.dependentIds.joinToString().ifBlank { "none" }}. Use Tab to change objects and arrow keys to move the selection."
            },
            shapes = replayShapes,
            interactionEnabled = vm.geometryTool == GeometryTool.Select && !replayingProtocol,
            manipulationMode = manipulationMode,
            resizePolicy = resizePolicy,
            selectedShapes = vm.selectedShapes,
            snapEnabled = vm.settings.snap,
            axisConstraint = axisConstraint,
            precisionMode = precisionMode,
            lassoEnabled = lassoEnabled,
            boxSelectEnabled = boxSelectEnabled,
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
            onShapeRotate = { angle ->
                rotationAngle = angle
                val snap = snapGeometryRotation(angle)
                if (snap.snapped && snap.angle != lastRotationHaptic && vm.settings.haptics) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    lastRotationHaptic = snap.angle
                }
                vm.previewShapeRotation(angle)
            },
            onShapeScale = vm::previewShapeScale,
            onDragEnd = vm::endPointDrag,
            onDragCancel = vm::cancelPointDrag,
            onDropDelete = vm::deleteSelectedShape,
            onCanvasTap = { point, hitPointIndex ->
                if (hitPointIndex == null) vm.dismissAllMenusAndPanels()
                vm.handleGeometryTap(point, hitPointIndex)
            },
            onClearSelection = vm::clearGeometrySelection,
            onLassoSelection = vm::selectShapes,
            onObjectLongPress = { shapeIndex, _, _ ->
                shapeIndex?.let(vm::selectShape)
                contextMenuShapeIndex = shapeIndex
            },
            onKeyboardMove = vm::moveGeometrySelectionFromKeyboard,
            onKeyboardCycle = vm::cycleGeometrySelection,
            points = vm.state.points,
        ) { tx ->
            drawStoredShapes(vm.state.points, replayShapes, vm.selectedShapes, vm.selectedShape, tx)
            constraintGlyphs.forEach { glyph ->
                val color = when (glyph.feedback.level) { ConstraintFeedbackLevel.Satisfied -> Green; ConstraintFeedbackLevel.NearlySatisfied -> Cyan; ConstraintFeedbackLevel.Violated -> Amber; ConstraintFeedbackLevel.Invalid -> Color.Red }
                drawGraphLabel(glyph.symbol, tx(glyph.position) + Offset(8f, -8f), color)
            }
            geometryTrace?.samples?.takeIf { it.size >= 2 }?.let { samples ->
                val path = Path().apply { val first = tx(samples.first()); moveTo(first.x, first.y); samples.drop(1).forEach { val p = tx(it); lineTo(p.x, p.y) } }
                drawPath(path, Green.copy(.8f), style = Stroke(2.5f, cap = StrokeCap.Round))
            }
            if (!vm.shapeExplorerScene) drawConstructionPreview(vm.pendingConstruction, vm.geometryTool, tx)
            val ownedPointIndices = replayShapes.filter { it.visible }.flatMapTo(mutableSetOf()) { it.pointIndices }
            vm.state.points.forEachIndexed { pointIndex, point ->
                if (pointIndex in ownedPointIndices) return@forEachIndexed
                if ("P${pointIndex + 1}" !in protocolTimeline.visibleIds) return@forEachIndexed
                val dependency = dependenciesByOutput[pointIndex]
                val invalid = pointIndex in invalidDependencyOutputs
                drawRadiantPoint(
                    tx(point),
                    if (invalid) Color.Red else if (dependency == null) Green else Amber,
                    if (invalid) "${dependency?.name} undefined" else dependency?.name ?: "P${pointIndex + 1}",
                )
            }
        }
        if (vm.state.shapes.isEmpty() && vm.state.points.isEmpty()) {
            Column(
                Modifier
                    .align(Alignment.Center)
                    .widthIn(max = 340.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(SurfaceA.copy(.94f))
                    .border(1.dp, Green.copy(.55f), RoundedCornerShape(18.dp))
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Empty 2D canvas", color = Green, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Use + Add to place shapes. Nothing is preloaded.", color = Muted, fontSize = 11.sp, textAlign = TextAlign.Center)
                GlowButton("+ Add shape", icon = "+") { addShapeOpen = true }
            }
        }
        if (false && selectedShape != null) {
            val details = shapeExplorer2DDetails(selectedShape!!, vm.state.points)
            val shapeSummaryExpanded = objectDetailsExpanded
            if (shapeSummaryExpanded) {
                DimmedWorkspaceScrim { objectDetailsExpanded = false }
            }
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 86.dp)
                    .fillMaxWidth(
                        if (shapeSummaryExpanded) {
                            if (compact) .94f else .55f
                        } else {
                            if (compact) .72f else .38f
                        },
                    )
                    .widthIn(max = 520.dp)
                    .animateContentSize()
                    .padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Geometry2DObjectBar(
                    name = selectedShape!!.name,
                    objectCount = vm.state.shapes.count { it.visible },
                    detailsExpanded = shapeSummaryExpanded,
                    onDetailsToggle = { objectDetailsExpanded = !objectDetailsExpanded },
                    modifier = Modifier.fillMaxWidth(),
                )
                Geometry2DSelectionQuickHud(
                    mode = manipulationMode,
                    resizePolicy = resizePolicy,
                    onMode = { mode ->
                        manipulationMode = mode
                        vm.selectGeometryTool(GeometryTool.Select)
                    },
                    onNudge = vm::nudgeSelectedShape,
                    onScale = vm::scaleSelectedShapeBy,
                    onRotate = { delta ->
                        vm.rotateSelectedShapeBy(delta)
                        rotationAngle += delta
                    },
                    onResizePolicy = { resizePolicy = it },
                    onClearAll = onRequestClearAll,
                    modifier = Modifier.fillMaxWidth(),
                )
                AnimatedVisibility(shapeSummaryExpanded) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceA.copy(.94f))
                            .border(1.dp, Cyan.copy(alpha = .35f), RoundedCornerShape(14.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            GlowButton("All formulas", icon = "ƒ", iconOnly = compact) { vm.togglePanel(PanelSlot.Right) }
                            GlowButton("Shapes", icon = "SE", iconOnly = compact, onClick = vm::openShapesExplorer)
                        }
                        Text(details.formula, color = Ink, fontSize = if (compact) 11.sp else 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                        Text("Area ${trim(details.area)}  -  Perimeter ${trim(details.perimeter)}", color = Green, fontSize = 12.sp, maxLines = 1)
                        Text("Drag a glowing point to resize.", color = Muted, fontSize = 10.sp, maxLines = 1)
                    }
                }
            }
        }
        if (!vm.shapeExplorerScene) Column(
            Modifier.align(Alignment.TopStart).padding(top = workspaceToolTop, start = 10.dp).clip(RoundedCornerShape(16.dp)).background(SurfaceA.copy(.82f)).animateContentSize().padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(
                Modifier.clip(RoundedCornerShape(14.dp)).clickable { viewToolsExpanded = !viewToolsExpanded }.padding(horizontal = 8.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                TransparentIcon("V", Cyan)
                Text(if (viewToolsExpanded) "Hide view tools" else "View tools", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            AnimatedVisibility(viewToolsExpanded) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    GlowButton("⌂ Fit") { homeRequest++ }
                    GlowButton("Undo view") { undoViewRequest++ }
                    GlowButton(if (lassoEnabled) "● Lasso" else "Lasso") { lassoEnabled = !lassoEnabled; if (lassoEnabled) boxSelectEnabled = false }
                    GlowButton(if (boxSelectEnabled) "● Box" else "Box") { boxSelectEnabled = !boxSelectEnabled; if (boxSelectEnabled) lassoEnabled = false }
                    GlowButton("Copy", enabled = vm.selectedShapes.isNotEmpty()) { copyShapeText(context, "AI Explorer geometry", vm.exportSelectedGeometry()) }
                    GlowButton("Paste") {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()?.let(vm::importGeometry)
                    }
                    GlowButton("Group", enabled = vm.selectedShapes.size > 1, onClick = vm::groupSelectedShapes)
                    GlowButton("Ungroup", enabled = vm.state.geometryGroups.any { group -> group.shapeIds.any { id -> vm.state.shapes.indexOfFirst { it.id == id } in vm.selectedShapes } }, onClick = vm::ungroupSelectedShapes)
                }
            }
        }
        Geometry2DBottomDock(
            mode = manipulationMode,
            selected = selectedShape != null,
            locked = selectedShape?.locked == true,
            canClear = vm.state.shapes.isNotEmpty() || vm.state.points.isNotEmpty() || vm.state.geometryConstraints.isNotEmpty(),
            rotationAngle = rotationAngle,
            resizePolicy = resizePolicy,
            onMode = { mode ->
                manipulationMode = mode
                vm.selectGeometryTool(GeometryTool.Select)
            },
            onAdd = { addShapeOpen = true },
            onLock = { vm.updateSelectedShape { it.copy(locked = !it.locked) } },
            onDuplicate = vm::duplicateSelectedShape,
            onDelete = vm::deleteSelectedShape,
            onClearAll = onRequestClearAll,
            onNudge = vm::nudgeSelectedShape,
            onScale = vm::scaleSelectedShapeBy,
            onRotateBy = { delta ->
                vm.rotateSelectedShapeBy(delta)
                rotationAngle += delta
                if (vm.settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            },
            onResetRotation = {
                vm.rotateSelectedShapeBy(-rotationAngle)
                rotationAngle = 0.0
            },
            onResizePolicy = { resizePolicy = it },
            onFit = { homeRequest++ },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
        contextMenuShapeIndex?.let { shapeIndex ->
            vm.state.shapes.getOrNull(shapeIndex)?.let { shape ->
                Column(
                    Modifier.align(Alignment.Center).widthIn(max = 360.dp).clip(RoundedCornerShape(16.dp))
                        .background(SurfaceA.copy(.98f)).border(1.dp, Amber.copy(.65f), RoundedCornerShape(16.dp)).padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(shape.name, color = Amber, fontWeight = FontWeight.Bold)
                        GlowButton("×") { contextMenuShapeIndex = null }
                    }
                    Text("Object actions at the touched geometry", color = Muted, fontSize = 10.sp)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        GlowButton(if (shape.locked) "Unlock" else "Lock") { vm.updateSelectedShape { it.copy(locked = !it.locked) }; contextMenuShapeIndex = null }
                        GlowButton(if (shape.visible) "Hide" else "Show") { vm.updateSelectedShape { it.copy(visible = !it.visible) }; contextMenuShapeIndex = null }
                        GlowButton("Duplicate") { vm.duplicateSelectedShape(); contextMenuShapeIndex = null }
                        GlowButton("Front") { vm.reorderSelectedShape(true); contextMenuShapeIndex = null }
                        GlowButton("Back") { vm.reorderSelectedShape(false); contextMenuShapeIndex = null }
                        GlowButton("Style") { vm.updateSelectedShape { it.copy(styleKey = if (it.styleKey == "default") "accent" else "default") } }
                        GlowButton("Copy") { copyShapeText(context, "AI Explorer geometry", vm.exportSelectedGeometry()) }
                        GlowButton("Rename…") { vm.togglePanel(PanelSlot.Right); contextMenuShapeIndex = null }
                        DestructiveGlowButton("Delete") { vm.deleteSelectedShape(); contextMenuShapeIndex = null }
                    }
                }
            }
        }
        val quickContextTools = contextInspector.tools.filter { it.enabled && (it.category.contains("Dependent") || it.category.contains("centre") || it.toolName in setOf("Intersection", "Midpoint", "CircleThreePoints")) }.take(6)
        if (!vm.shapeExplorerScene && quickContextTools.isNotEmpty() && !vm.showLeftPanel && !vm.showRightPanel) {
            Column(
                Modifier.align(Alignment.BottomCenter).padding(bottom = if (selectedShape == null) 202.dp else 270.dp).widthIn(max = 620.dp)
                    .clip(RoundedCornerShape(15.dp)).background(SurfaceA.copy(.90f)).border(1.dp, Amber.copy(.55f), RoundedCornerShape(15.dp))
                    .clickable { contextToolsExpanded = !contextToolsExpanded }
                    .animateContentSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("CONSTRUCTION - ${contextInspector.title.uppercase()}", color = Amber, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(if (contextToolsExpanded) "Collapse" else "${quickContextTools.size} tools", color = Muted, fontSize = 9.sp)
                }
                AnimatedVisibility(contextToolsExpanded) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        quickContextTools.forEach { tool -> GlowButton(tool.label) { vm.applyContextualGeometryTool(tool.toolName, tool.pointIndices) } }
                        GlowButton("More…") { vm.togglePanel(PanelSlot.Right) }
                    }
                }
            }
        }
        if (!vm.shapeExplorerScene && constraintFeedback.isNotEmpty() && !vm.showRightPanel && !layersExpanded) {
            val satisfied = constraintFeedback.count { it.level == ConstraintFeedbackLevel.Satisfied }
            val warning = constraintFeedback.any { it.level == ConstraintFeedbackLevel.Violated || it.level == ConstraintFeedbackLevel.Invalid }
            Column(
                Modifier.align(Alignment.TopEnd).padding(top = 72.dp, end = 10.dp).width(210.dp)
                    .clip(RoundedCornerShape(14.dp)).background(SurfaceA.copy(.90f)).border(1.dp, (if (warning) Amber else Green).copy(.55f), RoundedCornerShape(14.dp))
                    .clickable { constraintsExpanded = !constraintsExpanded }
                    .animateContentSize()
                    .padding(8.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("CONSTRAINTS  $satisfied/${constraintFeedback.size}", color = if (warning) Amber else Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(if (constraintsExpanded) "Hide" else "Show", color = Muted, fontSize = 9.sp)
                }
                AnimatedVisibility(constraintsExpanded) {
                    Text(constraintFeedback.firstOrNull { it.level != ConstraintFeedbackLevel.Satisfied }?.statement ?: "All monitored relations are satisfied", color = Ink, fontSize = 10.sp, maxLines = 2)
                }
            }
        }
        if (replayingProtocol) {
            Text(
                "Protocol replay - ${protocolStep.roundToInt()}/$protocolSize - editing paused",
                color = Amber,
                fontSize = 11.sp,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 72.dp).clip(RoundedCornerShape(12.dp)).background(SurfaceA.copy(.94f)).padding(horizontal = 10.dp, vertical = 7.dp),
            )
        }
        if (vm.showLeftPanel) GlassPanel(Modifier.align(Alignment.TopStart).padding(top = 64.dp).width(270.dp)) {
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
        if (vm.showRightPanel && vm.shapeExplorerScene && selectedShape != null) Shape2DStudioPanel(vm, selectedShape, compact, Modifier.align(if (compact) Alignment.Center else Alignment.TopEnd))
        if (vm.showRightPanel && !vm.shapeExplorerScene) GlassPanel(Modifier.align(Alignment.TopEnd).padding(top = 64.dp).width(260.dp)) {
            PanelHeader("Context Inspector", vm::hidePanels, Violet)
            Text(contextInspector.title, color = Violet, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(contextInspector.kind, color = Muted, fontSize = 11.sp)
            contextInspector.properties.forEach { (name, value) -> Insight(name, value, when (name) { "State" -> Green; "Coordinates" -> Cyan; else -> Violet }) }
            Insight("Parents", contextInspector.parentIds.joinToString().ifBlank { "none — directly defined" }, Cyan)
            Insight("Drives", contextInspector.dependentIds.joinToString().ifBlank { "no dependent objects" }, Amber)
            Text("Available from this selection", color = Ink, fontWeight = FontWeight.SemiBold)
            contextInspector.tools.groupBy { it.category }.forEach { (category, tools) ->
                Text(category, color = Muted, fontSize = 9.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    tools.forEach { tool -> GlowButton(tool.label, enabled = tool.enabled) { vm.applyContextualGeometryTool(tool.toolName, tool.pointIndices) } }
                }
            }
            if (constraintSuggestions.isNotEmpty()) {
                Text("Suggested constraints", color = Ink, fontWeight = FontWeight.SemiBold)
                constraintSuggestions.forEach { suggestion ->
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp)).background(Color(0x33101824)).padding(7.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(suggestion.label, color = when (suggestion.preview.level) { ConstraintFeedbackLevel.Satisfied -> Green; ConstraintFeedbackLevel.NearlySatisfied -> Cyan; else -> Amber }, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            GlowButton("Monitor") { vm.applyGeometryConstraint(suggestion.constraint) }
                        }
                        Text(suggestion.preview.statement, color = Muted, fontSize = 9.sp)
                    }
                }
            }
            if (constraintFeedback.isNotEmpty()) {
                Text("Live constraint feedback", color = Ink, fontWeight = FontWeight.SemiBold)
                constraintFeedback.forEach { feedback ->
                    val color = when (feedback.level) { ConstraintFeedbackLevel.Satisfied -> Green; ConstraintFeedbackLevel.NearlySatisfied -> Cyan; ConstraintFeedbackLevel.Violated -> Amber; ConstraintFeedbackLevel.Invalid -> Color.Red }
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp)).border(1.dp, color.copy(.45f), RoundedCornerShape(11.dp)).padding(7.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${feedback.constraint.type.label} - ${feedback.level.name}", color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            GlowButton("×") { vm.removeGeometryConstraint(feedback.constraint.id) }
                        }
                        Text(feedback.statement, color = Ink, fontSize = 9.sp)
                        Text(feedback.guidance, color = Muted, fontSize = 9.sp)
                    }
                }
            }
            Insight("Undefined dependencies", "${invalidDependencyOutputs.size}", if (invalidDependencyOutputs.isEmpty()) Green else Color.Red)
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
                    DestructiveGlowButton("Delete", onClick = vm::deleteSelectedShape)
                    GlowButton("Translate", onClick = { vm.transformSelectedShape(PointDependencyType.Translate, listOf(1.0, 1.0)) })
                    GlowButton("Rotate 30°", onClick = { vm.transformSelectedShape(PointDependencyType.Rotate, listOf(30.0, 0.0, 0.0)) })
                    GlowButton("Reflect X", onClick = { vm.transformSelectedShape(PointDependencyType.ReflectX) })
                    GlowButton("Dilate", onClick = { vm.transformSelectedShape(PointDependencyType.Dilate, listOf(1.25, 0.0, 0.0)) })
                }
            }
            vm.state.points.getOrNull(vm.selectedPoint)?.let { point ->
                DirectPointEditor(vm.selectedPoint, point) { updated -> vm.movePoint(vm.selectedPoint, updated) }
                val dependency = vm.state.pointDependencies.firstOrNull { it.outputIndex == vm.selectedPoint }
                if (dependency != null && resolvePointDependency(vm.state.points, dependency.inputIndices, dependency.type, dependency.parameters) == null) {
                    Text("Construction needs repair", color = Color.Red, fontWeight = FontWeight.Bold)
                    Geometry2DDirectManipulation.recoverDegenerate(vm.state, dependency.inputIndices).forEach { action ->
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0x332A1018)).padding(7.dp)) {
                            Text(action.label, color = Amber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(action.explanation, color = Muted, fontSize = 9.sp)
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        GlowButton("Nudge parent", onClick = vm::nudgeSelectedDependencyParent)
                        GlowButton("Make free", onClick = vm::freeSelectedDependentPoint)
                    }
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
            Text("Movement constraint", color = Ink, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf(AxisConstraint.Free, AxisConstraint.X, AxisConstraint.Y).forEach { axis ->
                    GlowButton(if (axisConstraint == axis) "● ${axis.name}" else axis.name) { axisConstraint = axis }
                }
                GlowButton(if (precisionMode) "● Precision" else "Precision") { precisionMode = !precisionMode }
                GlowButton(if (lassoEnabled) "● Lasso" else "Lasso") { lassoEnabled = !lassoEnabled; if (lassoEnabled) boxSelectEnabled = false }
                GlowButton(if (boxSelectEnabled) "● Box" else "Box") { boxSelectEnabled = !boxSelectEnabled; if (boxSelectEnabled) lassoEnabled = false }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Construction protocol timeline", color = Ink, fontWeight = FontWeight.SemiBold)
                    Text("Replay the actual workspace and inspect dependency chains.", color = Muted, fontSize = 10.sp)
                }
                Text("${protocolStep.roundToInt()}/$protocolSize", color = Cyan, fontWeight = FontWeight.Bold)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                GlowButton("|←") { protocolStep = 0f; focusedProtocolId = null }
                GlowButton("← Step", enabled = protocolStep > 0f) { protocolStep = (protocolStep - 1f).coerceAtLeast(0f) }
                GlowButton(if (protocolPlaying) "Pause" else "Play") { protocolPlaying = !protocolPlaying }
                GlowButton("Step →", enabled = protocolStep < protocolSize) { protocolStep = (protocolStep + 1f).coerceAtMost(protocolSize.toFloat()) }
                GlowButton("Live") { protocolStep = protocolSize.toFloat(); focusedProtocolId = null }
            }
            Text("Macros and traces", color = Ink, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                GlowButton(if (macroDraft.recording) "Pause macro" else "Record macro") { macroDraft = macroDraft.copy(recording = !macroDraft.recording) }
                GlowButton("Replay macro", enabled = macroDraft.recordedIds.isNotEmpty()) {
                    val indices = macroDraft.recordedIds.mapNotNull { id -> vm.state.shapes.indexOfFirst { it.id == id }.takeIf { it >= 0 } }.toSet()
                    vm.selectShapes(indices)
                }
                GlowButton("Clear macro") { macroDraft = com.indianservers.aiexplorer.workspace.GeometryMacroDraft("Canvas macro") }
                GlowButton(if (geometryTrace?.recording == true) "Stop trace" else "Trace selected point", enabled = vm.selectedPoint in vm.state.points.indices || geometryTrace?.recording == true) {
                    geometryTrace = if (geometryTrace?.recording == true) geometryTrace?.copy(recording = false) else com.indianservers.aiexplorer.workspace.GeometryTraceSession("P${vm.selectedPoint + 1}", recording = true)
                }
                Text("${macroDraft.recordedIds.size} macro objects - ${geometryTrace?.samples?.size ?: 0} trace samples", color = Muted, fontSize = 9.sp)
            }
            AxisSlider("Timeline position", protocolStep, 0f..protocolSize.toFloat().coerceAtLeast(1f)) { protocolStep = it.roundToInt().toFloat() }
            protocolTimeline.entries.forEachIndexed { index, entry ->
                val inDependencyChain = entry.id in protocolTimeline.dependencyChain
                val accent = when (entry.status) {
                    GeometryProtocolStatus.Complete -> Green
                    GeometryProtocolStatus.Current -> Cyan
                    GeometryProtocolStatus.Future -> Muted
                    GeometryProtocolStatus.Blocked -> Color.Red
                }
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp))
                        .background(if (entry.id == focusedProtocolId) Violet.copy(.18f) else if (inDependencyChain) Cyan.copy(.10f) else Color(0x22101824))
                        .border(1.dp, (if (entry.id == focusedProtocolId) Violet else accent).copy(.42f), RoundedCornerShape(11.dp))
                        .clickable { focusedProtocolId = entry.id; protocolStep = (index + 1).toFloat() }.padding(7.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${index + 1}. ${entry.title}", color = if (inDependencyChain) Cyan else accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(entry.status.name, color = accent, fontSize = 8.sp)
                    }
                    Text(entry.detail, color = Ink, fontSize = 9.sp)
                    Text("Depends on: ${entry.parentIds.joinToString().ifBlank { "none" }}", color = Muted, fontSize = 9.sp)
                    protocolTimeline.blockedBy[entry.id]?.let { missing -> Text("Blocked by missing: ${missing.joinToString()}", color = Color.Red, fontSize = 9.sp) }
                }
            }
        }
        if (addShapeOpen) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = .58f))
                    .clickable { addShapeOpen = false },
                contentAlignment = Alignment.Center,
            ) {
                GlassPanel(
                    Modifier
                        .fillMaxWidth(.94f)
                        .widthIn(max = 680.dp)
                        .heightIn(max = 560.dp)
                        .clickable(enabled = false) {},
                ) {
                    PanelHeader("Add 2D Shape", onClose = { addShapeOpen = false }, accent = Green)
                    Add2DShapeLibrary(
                        onAddPoint = {
                            val n = vm.state.points.size
                            vm.addPoint(Vec2((n % 5) - 2.0, (n / 5) + 1.0))
                            addShapeOpen = false
                            vm.togglePanel(PanelSlot.Right)
                        },
                        onAddPreset = { id ->
                            vm.addExplorerShape2D(id)
                            addShapeOpen = false
                        },
                        onAddConstruction = { type, label ->
                            vm.addConstructionShape2D(type, label)
                            addShapeOpen = false
                        },
                    )
                    }
                }
            }
        }
    }

@Composable
private fun Geometry2DSelectionQuickHud(
    mode: Transform2DMode,
    resizePolicy: Geometry2DResizePolicy,
    onMode: (Transform2DMode) -> Unit,
    onNudge: (Vec2) -> Unit,
    onScale: (Double) -> Unit,
    onRotate: (Double) -> Unit,
    onResizePolicy: (Geometry2DResizePolicy) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceA.copy(.94f))
            .border(1.dp, Cyan.copy(.38f), RoundedCornerShape(14.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Selected object tools", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("drag handles or use buttons", color = Muted, fontSize = 9.sp)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            GlowButton(if (mode == Transform2DMode.Move) "● Move ↕↔" else "Move ↕↔", icon = "↕") { onMode(Transform2DMode.Move) }
            GlowButton(if (mode == Transform2DMode.Resize) "● Resize ⤢" else "Resize ⤢", icon = "⤢") { onMode(Transform2DMode.Resize) }
            GlowButton(if (mode == Transform2DMode.Rotate) "● Rotate ⟳" else "Rotate ⟳", icon = "⟳") { onMode(Transform2DMode.Rotate) }
            GlowButton(if (resizePolicy == Geometry2DResizePolicy.Proportional) "Ratio locked" else "Free size") {
                onResizePolicy(if (resizePolicy == Geometry2DResizePolicy.Proportional) Geometry2DResizePolicy.Free else Geometry2DResizePolicy.Proportional)
                onMode(Transform2DMode.Resize)
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            GlowButton("←", icon = "←") { onMode(Transform2DMode.Move); onNudge(Vec2(-.25, 0.0)) }
            GlowButton("↑", icon = "↑") { onMode(Transform2DMode.Move); onNudge(Vec2(0.0, .25)) }
            GlowButton("↓", icon = "↓") { onMode(Transform2DMode.Move); onNudge(Vec2(0.0, -.25)) }
            GlowButton("→", icon = "→") { onMode(Transform2DMode.Move); onNudge(Vec2(.25, 0.0)) }
            GlowButton("Size −", icon = "−") { onMode(Transform2DMode.Resize); onScale(.9) }
            GlowButton("Size +", icon = "+") { onMode(Transform2DMode.Resize); onScale(1.1) }
            GlowButton("Rot −15°", icon = "⟲") { onMode(Transform2DMode.Rotate); onRotate(-15.0) }
            GlowButton("Rot +15°", icon = "⟳") { onMode(Transform2DMode.Rotate); onRotate(15.0) }
            DestructiveGlowButton("Clear all", icon = "×", onClick = onClearAll)
        }
    }
}

private data class Add2DShapeOption(
    val category: String,
    val label: String,
    val icon: String,
    val detail: String,
    val presetId: String? = null,
    val constructionType: Shape2DType? = null,
    val point: Boolean = false,
)

@Composable
private fun Add2DShapeLibrary(
    onAddPoint: () -> Unit,
    onAddPreset: (String) -> Unit,
    onAddConstruction: (Shape2DType, String) -> Unit,
) {
    var search by remember { mutableStateOf("") }
    var expanded by remember {
        mutableStateOf(setOf("Basics", "Lines & Rays", "Triangles", "Quadrilaterals", "Curves", "Polygons", "Advanced constructions"))
    }
    val constructionOptions = remember {
        listOf(
            Add2DShapeOption("Basics", "Point", "P", "Free point with editable x/y coordinates", point = true),
            Add2DShapeOption("Lines & Rays", "Line", "L", "Two-point infinite line", constructionType = Shape2DType.Line),
            Add2DShapeOption("Lines & Rays", "Segment", "S", "Two endpoints, measured length", constructionType = Shape2DType.Segment),
            Add2DShapeOption("Lines & Rays", "Ray", "R", "Starts at first point and passes through second", constructionType = Shape2DType.Ray),
            Add2DShapeOption("Lines & Rays", "Vector", "V", "Directed segment with components", constructionType = Shape2DType.Vector),
            Add2DShapeOption("Advanced constructions", "Parallel Line", "∥", "Line through a point parallel to a base line", constructionType = Shape2DType.Parallel),
            Add2DShapeOption("Advanced constructions", "Perpendicular Line", "⊥", "Line through a point perpendicular to a base line", constructionType = Shape2DType.Perpendicular),
            Add2DShapeOption("Advanced constructions", "Angle Bisector", "∠", "Bisects an angle from three points", constructionType = Shape2DType.AngleBisector),
            Add2DShapeOption("Curves", "Circle Through 3", "C3", "Circumcircle through three points", constructionType = Shape2DType.CircleThreePoints),
            Add2DShapeOption("Curves", "Arc", "⌒", "Circular arc from three control points", constructionType = Shape2DType.Arc),
            Add2DShapeOption("Polygons", "Regular Polygon", "RG", "Editable regular pentagon template", constructionType = Shape2DType.RegularPolygon),
        )
    }
    val presetOptions = remember {
        ShapeExplorer2DShapes.map { preset ->
            Add2DShapeOption(
                category = preset.category(),
                label = preset.label,
                icon = preset.label.take(2).uppercase(),
                detail = preset.formula,
                presetId = preset.id,
            )
        }
    }
    val allOptions = constructionOptions + presetOptions
    val normalizedSearch = search.trim()
    val filtered = allOptions.filter { option ->
        normalizedSearch.isBlank() ||
            option.label.contains(normalizedSearch, ignoreCase = true) ||
            option.category.contains(normalizedSearch, ignoreCase = true) ||
            option.detail.contains(normalizedSearch, ignoreCase = true) ||
            option.constructionType?.name?.contains(normalizedSearch, ignoreCase = true) == true
    }
    Text("Choose one object to add. Existing shapes stay independent.", color = Muted, fontSize = 11.sp)
    OutlinedTextField(
        value = search,
        onValueChange = { search = it },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Search shapes and tools") },
        placeholder = { Text("point, line, triangle, tangent, polygon…") },
        singleLine = true,
    )
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(max = 410.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val grouped = filtered.groupBy { it.category }.toSortedMap(compareBy { category ->
            listOf("Basics", "Lines & Rays", "Triangles", "Quadrilaterals", "Curves", "Polygons", "Advanced constructions").indexOf(category).let { if (it < 0) 99 else it }
        })
        if (filtered.isEmpty()) {
            Text("No matching 2D shape. Try a broader search.", color = Amber, fontSize = 12.sp)
        }
        grouped.forEach { (category, options) ->
            val open = category in expanded
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(13.dp))
                    .background(SurfaceB.copy(.54f))
                    .border(1.dp, Green.copy(.22f), RoundedCornerShape(13.dp))
                    .animateContentSize()
                    .padding(7.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth().clickable {
                        expanded = if (open) expanded - category else expanded + category
                    },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("$category (${options.size})", color = Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(if (open) "▲" else "▼", color = Green, fontSize = 10.sp)
                }
                AnimatedVisibility(open) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        options.forEach { option ->
                            Column(
                                Modifier
                                    .widthIn(min = 142.dp, max = 210.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceA.copy(.80f))
                                    .border(1.dp, Cyan.copy(.26f), RoundedCornerShape(12.dp))
                                    .clickable {
                                        when {
                                            option.point -> onAddPoint()
                                            option.presetId != null -> onAddPreset(option.presetId)
                                            option.constructionType != null -> onAddConstruction(option.constructionType, option.label)
                                        }
                                    }
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Text(option.icon, color = Cyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(option.label, color = Ink, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                Text(option.detail, color = Muted, fontSize = 8.sp, maxLines = 2)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun defaultConstructionPoints(type: Shape2DType): List<Vec2> = when (type) {
    Shape2DType.Line -> listOf(Vec2(-2.5, -1.0), Vec2(2.5, 1.0))
    Shape2DType.Segment -> listOf(Vec2(-2.2, 0.0), Vec2(2.2, 0.0))
    Shape2DType.Ray -> listOf(Vec2(-2.2, -.8), Vec2(2.1, .9))
    Shape2DType.Vector -> listOf(Vec2(-2.0, -1.0), Vec2(2.0, 1.2))
    Shape2DType.Parallel -> listOf(Vec2(-2.4, -1.2), Vec2(1.8, -1.2), Vec2(-1.5, 1.2))
    Shape2DType.Perpendicular -> listOf(Vec2(-2.2, -1.0), Vec2(2.0, -1.0), Vec2(.2, 1.4))
    Shape2DType.AngleBisector -> listOf(Vec2(-2.1, -1.1), Vec2(0.0, 0.0), Vec2(2.1, -1.1))
    Shape2DType.CircleThreePoints -> listOf(Vec2(-1.6, -.8), Vec2(1.6, -.8), Vec2(0.0, 1.6))
    Shape2DType.Arc -> listOf(Vec2(-1.8, -.8), Vec2(0.0, 1.5), Vec2(1.8, -.8))
    Shape2DType.RegularPolygon -> listOf(Vec2(0.0, 0.0), Vec2(2.2, 0.0))
    Shape2DType.Circle -> listOf(Vec2(0.0, 0.0), Vec2(2.0, 0.0))
    Shape2DType.Ellipse -> listOf(Vec2(0.0, 0.0), Vec2(2.4, 0.0), Vec2(0.0, 1.4))
    Shape2DType.Rectangle -> listOf(Vec2(-2.3, -1.3), Vec2(2.3, 1.3))
    Shape2DType.Square -> listOf(Vec2(-1.7, -1.7), Vec2(1.7, 1.7))
    Shape2DType.Triangle -> listOf(Vec2(-2.2, -1.4), Vec2(2.2, -1.4), Vec2(0.0, 1.8))
    Shape2DType.Polygon -> listOf(Vec2(-2.2, -1.2), Vec2(.8, -1.5), Vec2(2.2, .7), Vec2(-.8, 1.6))
}

@Composable
private fun DirectPointEditor(pointIndex: Int, point: Vec2, onApply: (Vec2) -> Unit) {
    var xText by remember(pointIndex, point.x) { mutableStateOf(trim(point.x)) }
    var yText by remember(pointIndex, point.y) { mutableStateOf(trim(point.y)) }
    Text("Direct coordinate editing", color = Ink, fontWeight = FontWeight.SemiBold)
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
        CompactMathField(xText, { xText = it }, "x", Modifier.weight(1f))
        CompactMathField(yText, { yText = it }, "y", Modifier.weight(1f))
        GlowButton("Apply", enabled = xText.toDoubleOrNull() != null && yText.toDoubleOrNull() != null) {
            onApply(Vec2(xText.toDouble(), yText.toDouble()))
        }
    }
}

@Composable
private fun DirectVec3Editor(title: String, value: Vec3, onApply: (Vec3) -> Unit) {
    var x by remember(title, value.x) { mutableStateOf(trim(value.x)) }
    var y by remember(title, value.y) { mutableStateOf(trim(value.y)) }
    var z by remember(title, value.z) { mutableStateOf(trim(value.z)) }
    Text(title, color = Ink, fontWeight = FontWeight.SemiBold)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        CompactMathField(x, { x = it }, "x", Modifier.weight(1f), MathKeyboardContext.GRAPH_3D)
        CompactMathField(y, { y = it }, "y", Modifier.weight(1f), MathKeyboardContext.GRAPH_3D)
        CompactMathField(z, { z = it }, "z", Modifier.weight(1f), MathKeyboardContext.GRAPH_3D)
        GlowButton("Apply", enabled = listOf(x, y, z).all { it.toDoubleOrNull() != null }) { onApply(Vec3(x.toDouble(), y.toDouble(), z.toDouble())) }
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
    var clearEpochSeen by remember { mutableIntStateOf(vm.workspaceClearEpoch) }
    LaunchedEffect(vm.workspaceClearEpoch) {
        if (vm.workspaceClearEpoch != clearEpochSeen) {
            scene = ManipulativeScene()
            selected = emptySet()
            formalPreview = "Workspace cleared - add an object from a tray"
            clearEpochSeen = vm.workspaceClearEpoch
        }
    }
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
            PanelHeader("Manipulatives", vm::returnToMathMenu, Cyan, icon = "▦")
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
                DestructiveGlowButton("Delete", enabled = selectedItem != null) { selectedItem?.let { scene = engine.remove(scene, it.id); selected = emptySet() } }
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
            Text("Drag objects - tap multiple objects to group - locked objects stay fixed", color = Muted, fontSize = 11.sp)
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
            Insight("Invariant", "${playback.frame.invariant} - residual ${trim(playback.frame.residual)}", if (playback.frame.holds) Green else Amber)
            Text("What changes? ${playback.frame.lab.changesPrompt}", color = Cyan, fontSize = 12.sp)
            Text("What stays same? ${playback.frame.lab.invariantPrompt}", color = Green, fontSize = 12.sp)
            MathFormulaText(playback.frame.lab.formalResult, color = Amber, fontSize = 17.sp, fontWeight = FontWeight.Bold)
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
private fun Geometry3DScreen(vm: ExplorerViewModel, compact: Boolean, onRequestClearAll: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val adaptiveProfile = LocalAdaptiveDeviceProfile.current
    val workspaceTop = if (adaptiveProfile.isTelevision) {
        adaptiveProfile.workspacePolicy.topChromeClearance
    } else {
        if (compact) 70.dp else 78.dp
    }
    val workspaceToolTop = if (adaptiveProfile.isTelevision) {
        adaptiveProfile.workspacePolicy.topChromeClearance
    } else {
        72.dp
    }
    var rotateX by remember { mutableFloatStateOf(25f) }
    var rotateY by remember { mutableFloatStateOf(-35f) }
    var rotateZ by remember { mutableFloatStateOf(15f) }
    var zoom by remember { mutableFloatStateOf(1.0f) }
    var cameraPan by remember { mutableStateOf(Offset.Zero) }
    var transformMode by remember(vm.shapeExplorerScene) {
        mutableStateOf(if (vm.shapeExplorerScene) Transform3DMode.Scale else Transform3DMode.Move)
    }
    var projection by remember { mutableStateOf(CameraProjection.Perspective) }
    var selectionMode by remember { mutableStateOf(Selection3DMode.Object) }
    var subSelection by remember { mutableStateOf<SubObjectSelection?>(null) }
    var axisConstraint by remember { mutableStateOf(AxisConstraint.Free) }
    var precisionMode by remember { mutableStateOf(false) }
    var gestureMode by remember { mutableStateOf(GestureMode.Idle) }
    var lockedSolidIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var sectionEnabled by remember { mutableStateOf(false) }
    var clipSection by remember { mutableStateOf(false) }
    var sectionPlane by remember { mutableStateOf(EditableSectionPlane()) }
    var transformSpace by remember { mutableStateOf(SpatialTransformSpace.World) }
    var dragPlane by remember { mutableStateOf(SpatialDragPlane.Free) }
    var visualMode by remember { mutableStateOf(SpatialVisualMode.Solid) }
    var sceneAppearance by remember { mutableStateOf(WorkspaceAppearance()) }
    var sceneAxisStyle by remember { mutableStateOf(WorkspaceVisualStyles.Spectral.axes) }
    var solidAppearances by remember { mutableStateOf<Map<Int, WorkspaceAppearance>>(emptyMap()) }
    var explodeAmount by remember { mutableFloatStateOf(0f) }
    var multiSelectEnabled by remember { mutableStateOf(false) }
    var editMode by remember(vm.shapeExplorerScene) {
        mutableStateOf(if (vm.shapeExplorerScene) SpatialEditMode.Resize else SpatialEditMode.Select)
    }
    var layersExpanded by remember { mutableStateOf(false) }
    var showWorkspaceGrid by remember { mutableStateOf(true) }
    var workspaceGridSize by remember { mutableFloatStateOf(1f) }
    var selectedSolidIndices by remember(vm.shapeExplorerScene) {
        mutableStateOf(
            if (vm.shapeExplorerScene) vm.selectedSolid.takeIf { it in vm.state.solids.indices }?.let(::setOf) ?: emptySet()
            else emptySet(),
        )
    }
    var hiddenSolidIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var isolatedSolidIndices by remember { mutableStateOf<Set<Int>?>(null) }
    var solidGroups by remember { mutableStateOf<List<Set<Int>>>(emptyList()) }
    var cameraBookmarks by remember { mutableStateOf<List<SpatialCameraBookmark>>(emptyList()) }
    var showSceneNavigator by remember { mutableStateOf(false) }
    var showMotionTrails by remember { mutableStateOf(false) }
    var motionTrail by remember { mutableStateOf<List<Vec3>>(emptyList()) }
    var extrusionPreview by remember { mutableStateOf<com.indianservers.aiexplorer.core.ExtrusionPreview3D?>(null) }
    var meshEditHistory by remember { mutableStateOf(com.indianservers.aiexplorer.core.MeshEditHistory3D()) }
    var booleanResult by remember { mutableStateOf<com.indianservers.aiexplorer.core.BooleanMeshResult?>(null) }
    var spatialMeasurements by remember { mutableStateOf<List<com.indianservers.aiexplorer.core.SpatialMeasurement3D>>(emptyList()) }
    var addShapeOpen by remember { mutableStateOf(false) }
    var spatialViewToolsExpanded by remember { mutableStateOf(false) }
    var formulaInspectorOpen by remember { mutableStateOf(false) }
    fun selectTransformMode(mode: Transform3DMode) {
        transformMode = mode
        editMode = when (mode) {
            Transform3DMode.Move -> SpatialEditMode.Move
            Transform3DMode.Rotate -> SpatialEditMode.Rotate
            Transform3DMode.Scale -> SpatialEditMode.Resize
        }
    }
    fun selectEditMode(mode: SpatialEditMode) {
        editMode = mode
        transformMode = when (mode) {
            SpatialEditMode.Select, SpatialEditMode.Move -> Transform3DMode.Move
            SpatialEditMode.Resize -> Transform3DMode.Scale
            SpatialEditMode.Rotate -> Transform3DMode.Rotate
        }
    }
    BackHandler(enabled = addShapeOpen) { addShapeOpen = false }
    BackHandler(enabled = spatialViewToolsExpanded && !formulaInspectorOpen) { spatialViewToolsExpanded = false }
    BackHandler(enabled = formulaInspectorOpen) { formulaInspectorOpen = false }
    val selectedIndex = selectedSolidIndices.lastOrNull()?.takeIf { it in vm.state.solids.indices } ?: -1
    val selectedSolid = vm.state.solids.getOrNull(selectedIndex)
    LaunchedEffect(selectedIndex) {
        if (selectedIndex == -1) formulaInspectorOpen = false
    }
    val visibleSolidIndices = remember(vm.state.solids.size, hiddenSolidIndices, isolatedSolidIndices) {
        (vm.state.solids.indices).filterTo(linkedSetOf()) { it !in hiddenSolidIndices && (isolatedSolidIndices == null || it in isolatedSolidIndices.orEmpty()) }
    }
    val renderedSolids = remember(vm.state.solids, explodeAmount, extrusionPreview) {
        val values = extrusionPreview?.let { preview -> vm.state.solids.mapIndexed { index, solid -> if (index == preview.solidIndex) preview.preview else solid } } ?: vm.state.solids
        AdvancedSpatialInteractionEngine.exploded(values, explodeAmount.toDouble())
    }
    val collisions = remember(renderedSolids) { AdvancedSpatialInteractionEngine.collisions(renderedSolids) }
    val selectedBounds = remember(selectedSolid) {
        selectedSolid?.let { solid ->
            AnalyticGeometry3D.bounds(SolidMeshFactory.create(solid).vertices.map { it + solid.position })
        }
    }
    val selectedVectorIndex = vm.selectedVector3D.takeIf { it in vm.state.vectors3D.indices } ?: -1
    val selectedVector = vm.state.vectors3D.getOrNull(selectedVectorIndex)
    val selectedPoint3DIndex = vm.selectedPoint3D.takeIf { it in vm.state.points3D.indices } ?: -1
    val selectedPoint3D = vm.state.points3D.getOrNull(selectedPoint3DIndex)
    val sharedRenderScene = remember(vm.state.solids, vm.state.vectors3D) {
        SharedSpatialSceneBuilder.build("geometry-3d-workspace", vm.state.solids, vectors = vm.state.vectors3D)
    }
    val sharedRenderPlan = remember(sharedRenderScene) { SharedGpuSceneCompiler.compile(sharedRenderScene) }
    fun deleteCurrent3DSelection() {
        val targets = selectedSolidIndices.filterTo(linkedSetOf()) { it in vm.state.solids.indices }
            .ifEmpty { selectedSolid?.let { setOf(selectedIndex) } ?: emptySet() }
        if (targets.isEmpty()) {
            when {
                selectedVectorIndex >= 0 -> vm.deleteVector3D(selectedVectorIndex)
                selectedPoint3DIndex >= 0 -> vm.deletePoint3D(selectedPoint3DIndex)
            }
            return
        }
        vm.deleteSelectedSolids(targets)
        selectedSolidIndices = emptySet()
        subSelection = null; extrusionPreview = null; hiddenSolidIndices = emptySet(); isolatedSolidIndices = null; lockedSolidIndices = emptySet(); solidGroups = emptyList()
    }
    fun transformSelected3DSolids(label: String, transform: (Solid) -> Solid) {
        val targets = selectedSolidIndices.filterTo(linkedSetOf()) { it in vm.state.solids.indices }
            .ifEmpty { selectedSolid?.let { setOf(selectedIndex) } ?: emptySet() }
        if (targets.isEmpty()) return
        vm.replaceSolids(label) { solids ->
            solids.mapIndexed { index, solid ->
                if (index in targets && index !in lockedSolidIndices) transform(solid) else solid
            }
        }
    }
    fun resizeSelected3D(factor: Double) {
        selectTransformMode(Transform3DMode.Scale)
        transformSelected3DSolids("Resized 3D selection") { solid ->
            solid.copy(
                width = (solid.width * factor).coerceIn(.2, 12.0),
                height = (solid.height * factor).coerceIn(.2, 12.0),
                depth = (solid.depth * factor).coerceIn(.2, 12.0),
                radius = (solid.radius * factor).coerceIn(.1, 6.0),
                topRadius = (solid.topRadius * factor).coerceIn(.05, 6.0),
            )
        }
    }
    fun rotateSelected3D(delta: Vec3) {
        selectTransformMode(Transform3DMode.Rotate)
        transformSelected3DSolids("Rotated 3D selection") { solid -> solid.copy(rotation = solid.rotation + delta) }
    }
    Box(Modifier.fillMaxSize()) {
        Projected3DCanvas(
            modifier = Modifier.fillMaxSize()
                .background(sceneAppearance.palette.background)
                .appWorkspaceTreatment(0.dp, sceneAppearance.palette.axes.z, sceneAppearance.palette.axes.y),
            solids = renderedSolids,
            vectors = vm.state.vectors3D,
            points3D = vm.state.points3D,
            selectedIndex = selectedIndex,
            visibleSolidIndices = visibleSolidIndices,
            selectedVectorIndex = selectedVectorIndex,
            selectedPoint3DIndex = selectedPoint3DIndex,
            rx = rotateX,
            ry = rotateY,
            rz = rotateZ,
            zoom = zoom,
            cameraPan = cameraPan,
            transformMode = transformMode,
            showGrid = showWorkspaceGrid,
            gridSize = workspaceGridSize,
            visualMode = visualMode,
            solidAppearances = solidAppearances,
            defaultAppearance = sceneAppearance,
            axisStyle = sceneAxisStyle,
            perspective = projection == CameraProjection.Perspective,
            selectionMode = selectionMode,
            subSelection = subSelection,
            sectionEnabled = sectionEnabled,
            sectionPlane = sectionPlane,
            clipSection = clipSection,
            onSelect = { index ->
                vm.selectSolid(index)
                selectedSolidIndices = if (multiSelectEnabled) {
                    if (index in selectedSolidIndices) selectedSolidIndices - index else selectedSolidIndices + index
                } else setOf(index)
            },
            onSubSelect = { subSelection = it },
            onSelectVector = vm::selectVector3D,
            onSelectPoint3D = vm::selectPoint3D,
            onSolidDragStart = {
                if (vm.settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                if (editMode != SpatialEditMode.Select && it !in lockedSolidIndices) {
                    if (it in selectedSolidIndices && selectedSolidIndices.size > 1) vm.beginSolidGroupDrag(selectedSolidIndices) else vm.beginSolidDrag(it)
                }
            },
            onSolidMove = { index, delta ->
                val spatialDelta = AdvancedSpatialInteractionEngine.transformSpace(delta, vm.state.solids.getOrNull(index)?.rotation ?: Vec3(0.0, 0.0, 0.0), transformSpace)
                val planeConstrained = AdvancedSpatialInteractionEngine.constrain(spatialDelta, dragPlane)
                val constrained = PrecisionInteraction.apply(SmartSnapEngine.constrain(planeConstrained, axisConstraint), precisionMode)
                val proposed = vm.state.solids.getOrNull(index)?.position?.plus(constrained) ?: constrained
                val snappedPosition = if (vm.settings.snap) {
                    val targets = vm.state.solids.mapIndexedNotNull { otherIndex, solid -> solid.takeIf { otherIndex != index } }.flatMap { solid ->
                        com.indianservers.aiexplorer.core.ConstraintAwareSpatialSnap.targets(SolidMeshFactory.create(solid), solid.position)
                    }.toMutableList()
                    if (sectionEnabled || clipSection) {
                        val signed = sectionPlane.unitNormal.dot(proposed) - sectionPlane.offset
                        targets += com.indianservers.aiexplorer.core.SpatialSnapTarget(com.indianservers.aiexplorer.core.SpatialSnapKind.Plane, proposed - sectionPlane.unitNormal * signed, sectionPlane.unitNormal, "section plane")
                    }
                    val preview = com.indianservers.aiexplorer.core.ConstraintAwareSpatialSnap.snap(proposed, targets)
                    if (preview.target != null) preview.point else AdvancedSpatialInteractionEngine.snap(proposed, vm.state.solids.filterIndexed { i, _ -> i != index }.map { it.position })
                } else proposed
                val snapped = snappedPosition - (vm.state.solids.getOrNull(index)?.position ?: Vec3(0.0, 0.0, 0.0))
                if (editMode == SpatialEditMode.Move) {
                    if (index in selectedSolidIndices && selectedSolidIndices.size > 1) vm.previewSolidGroupMove(snapped) else vm.previewSolidDrag(index, snapped)
                }
                if (showMotionTrails) motionTrail = (motionTrail + snappedPosition).takeLast(24)
            },
            onSolidRotate = { index, delta ->
                if (editMode == SpatialEditMode.Rotate) {
                    if (index in selectedSolidIndices && selectedSolidIndices.size > 1) vm.previewSolidGroupRotation(delta) else vm.previewSolidRotation(index, delta)
                }
            },
            onSolidScale = { index, factor ->
                val face = subSelection?.takeIf { it.solidIndex == index && it.mode == Selection3DMode.Face }
                if (editMode == SpatialEditMode.Resize) {
                    if (index in selectedSolidIndices && selectedSolidIndices.size > 1) vm.previewSolidGroupScale(factor)
                    else if (face != null) vm.previewSolidFaceExtrusion(index, face.index, factor) else vm.previewSolidScale(index, factor)
                }
            },
            onSolidAxisScale = vm::previewSolidAxisScale,
            onSectionPlaneMove = { distance -> sectionPlane = sectionPlane.moved(distance) },
            onSolidDragEnd = { if (selectedSolidIndices.size > 1) vm.endSolidGroupDrag() else vm.endSolidDrag() },
            onSolidDragCancel = { if (selectedSolidIndices.size > 1) vm.cancelSolidGroupDrag() else vm.cancelSolidDrag() },
            onSolidDropDelete = { deleteCurrent3DSelection() },
            onVectorDragStart = {
                if (vm.settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.beginVectorDrag(it)
            },
            onVectorMove = { index, delta -> vm.previewVectorDrag(index, PrecisionInteraction.apply(SmartSnapEngine.constrain(delta, axisConstraint), precisionMode)) },
            onVectorDragEnd = vm::endVectorDrag,
            onVectorDragCancel = vm::cancelVectorDrag,
            onPoint3DDragStart = vm::beginPoint3DDrag,
            onPoint3DMove = { index, delta -> vm.previewPoint3DDrag(index, PrecisionInteraction.apply(SmartSnapEngine.constrain(delta, axisConstraint), precisionMode)) },
            onPoint3DDragEnd = vm::endPoint3DDrag,
            onPoint3DDragCancel = vm::cancelPoint3DDrag,
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
            onEmptyTap = vm::dismissAllMenusAndPanels,
            onGestureModeChange = { gestureMode = it },
        )
        WorkspaceThemeButton(
            appearance = sceneAppearance,
            onSelect = { palette ->
                sceneAppearance = sceneAppearance.switchPalette(palette)
                sceneAxisStyle = palette.axes
                solidAppearances = solidAppearances.mapValues { (_, appearance) ->
                    appearance.switchPalette(palette)
                }
            },
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 10.dp),
        )
        if (vm.state.solids.isEmpty() && vm.state.vectors3D.isEmpty() && vm.state.points3D.isEmpty()) {
            Column(
                Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(18.dp))
                    .background(SurfaceA.copy(alpha = .92f))
                    .border(1.dp, Cyan.copy(alpha = .4f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 22.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Empty 3D canvas", color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Add a point, solid, or vector to begin.", color = Muted, fontSize = 12.sp)
                GlowButton("+ Add 3D object", onClick = { addShapeOpen = true })
            }
        }
        if (!vm.shapeExplorerScene) {
            SpatialManipulationBar(
                current = editMode,
                selectedAvailable = selectedSolid != null,
                onSelect = ::selectEditMode,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 144.dp),
                formulasVisible = formulaInspectorOpen,
                onFormulas = { formulaInspectorOpen = !formulaInspectorOpen },
            )
        }
        if (!vm.shapeExplorerScene) {
            SpatialLayerPanel(
                solids = vm.state.solids,
                vectors = vm.state.vectors3D,
                points3D = vm.state.points3D,
                selectedSolids = selectedSolidIndices,
                selectedVector = vm.selectedVector3D,
                selectedPoint = vm.selectedPoint3D,
                locked = lockedSolidIndices,
                hidden = hiddenSolidIndices,
                groups = solidGroups,
                expanded = layersExpanded,
                onExpandedChange = { layersExpanded = it },
                onSelectSolid = { index ->
                    vm.selectSolid(index)
                    selectedSolidIndices = if (multiSelectEnabled) {
                        if (index in selectedSolidIndices) selectedSolidIndices - index else selectedSolidIndices + index
                    } else setOf(index)
                },
                onSelectVector = vm::selectVector3D,
                onSelectPoint = vm::selectPoint3D,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = workspaceToolTop, end = 10.dp),
            )
        }
        if (!vm.shapeExplorerScene && showSceneNavigator) SpatialSceneNavigator(
            solids = renderedSolids,
            selected = selectedSolidIndices,
            collisions = collisions.map { it.first to it.second },
            trail = if (showMotionTrails) motionTrail else emptyList(),
            modifier = Modifier.align(Alignment.TopEnd).padding(top = if (layersExpanded) 286.dp else 116.dp, end = 10.dp),
        )
        if (!vm.shapeExplorerScene && !vm.showRightPanel) OrientationCube(
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp),
            onPreset = { preset ->
                when (preset) {
                    "Front" -> { rotateX = 0f; rotateY = 0f; rotateZ = 0f }
                    "Top" -> { rotateX = 90f; rotateY = 0f; rotateZ = 0f }
                    "Side" -> { rotateX = 0f; rotateY = 90f; rotateZ = 0f }
                    else -> { rotateX = 25f; rotateY = -35f; rotateZ = 15f }
                }
            },
        )
        if (collisions.isNotEmpty()) Text(
            "⚠ ${collisions.size} overlap${if (collisions.size == 1) "" else "s"}",
            color = Amber,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 76.dp).clip(RoundedCornerShape(12.dp)).background(SurfaceA).padding(8.dp),
        )
        if (vm.shapeExplorerScene && selectedSolid != null) {
            val measurements = Geometry3D.measure(selectedSolid)
            Column(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = workspaceTop)
                    .fillMaxWidth(if (compact) .94f else .55f)
                    .widthIn(max = 540.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(SurfaceA.copy(.96f), SurfaceB.copy(.94f))))
                    .border(1.dp, Cyan.copy(alpha = .45f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(selectedSolid.type.name, color = Cyan, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        GlowButton(visualMode.displayLabel(), icon = visualMode.displayIcon(), iconOnly = compact) {
                            visualMode = visualMode.nextDisplayMode()
                        }
                        GlowButton("All formulas", icon = "ƒ", iconOnly = compact) { vm.togglePanel(PanelSlot.Right) }
                        GlowButton("Shapes", icon = "SE", iconOnly = compact, onClick = vm::openShapesExplorer)
                    }
                }
                Text(Geometry3D.formula(selectedSolid.type), color = Ink, fontSize = if (compact) 11.sp else 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                Text("Surface ${trim(measurements.surfaceArea)}  -  Volume ${trim(measurements.volume)}", color = Green, fontSize = 12.sp, maxLines = 1)
                Text("Drag the solid to resize.", color = Muted, fontSize = 10.sp, maxLines = 1)
            }
        }
        if (!vm.shapeExplorerScene) SolidTrackballPalette(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 126.dp),
            selectedSolid = selectedSolid,
            transformMode = transformMode,
            selectionMode = selectionMode,
            onAdd = { type ->
                vm.addSolid(type)
                selectedSolidIndices = setOf(vm.selectedSolid)
            },
            onAddVector = vm::addVector3D,
            onTransformMode = ::selectTransformMode,
            onSelectionMode = {
                selectionMode = it
                subSelection = null
            },
        )
        Column(
            Modifier.align(Alignment.BottomCenter).padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            if (selectedSolid != null) {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceA.copy(.90f))
                        .border(1.dp, Cyan.copy(.38f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 7.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val controlsEnabled = selectedIndex !in lockedSolidIndices
                    GlowButton("Size -", icon = "-", iconOnly = compact, enabled = controlsEnabled) { resizeSelected3D(.9) }
                    GlowButton("Size +", icon = "+", iconOnly = compact, enabled = controlsEnabled) { resizeSelected3D(1.1) }
                    GlowButton("Rot X", icon = "RX", iconOnly = compact, enabled = controlsEnabled) { rotateSelected3D(Vec3(15.0, 0.0, 0.0)) }
                    GlowButton("Rot Y", icon = "RY", iconOnly = compact, enabled = controlsEnabled) { rotateSelected3D(Vec3(0.0, 15.0, 0.0)) }
                    GlowButton("Rot Z", icon = "RZ", iconOnly = compact, enabled = controlsEnabled) { rotateSelected3D(Vec3(0.0, 0.0, 15.0)) }
                }
            }
            if (selectedVector != null || selectedPoint3D != null) {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceA.copy(.90f))
                        .border(1.dp, Cyan.copy(.38f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 7.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val locked = selectedPoint3D?.locked == true
                    Text(
                        text = selectedPoint3D?.let { "Point ${it.name}" } ?: selectedVector?.let { "Vector ${it.name}" }.orEmpty(),
                        color = if (selectedPoint3D != null) Cyan else Amber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    GlowButton("X-", icon = "←", iconOnly = compact, enabled = !locked) {
                        selectedPoint3D?.let { vm.transformPoint3D(selectedPoint3DIndex) { p -> p.copy(position = p.position + Vec3(-.25, 0.0, 0.0)) } }
                        selectedVector?.let { vm.moveVector3D(selectedVectorIndex, Vec3(-.25, 0.0, 0.0)) }
                    }
                    GlowButton("X+", icon = "→", iconOnly = compact, enabled = !locked) {
                        selectedPoint3D?.let { vm.transformPoint3D(selectedPoint3DIndex) { p -> p.copy(position = p.position + Vec3(.25, 0.0, 0.0)) } }
                        selectedVector?.let { vm.moveVector3D(selectedVectorIndex, Vec3(.25, 0.0, 0.0)) }
                    }
                    GlowButton("Y+", icon = "↑", iconOnly = compact, enabled = !locked) {
                        selectedPoint3D?.let { vm.transformPoint3D(selectedPoint3DIndex) { p -> p.copy(position = p.position + Vec3(0.0, .25, 0.0)) } }
                        selectedVector?.let { vm.moveVector3D(selectedVectorIndex, Vec3(0.0, .25, 0.0)) }
                    }
                    GlowButton("Z+", icon = "↗", iconOnly = compact, enabled = !locked) {
                        selectedPoint3D?.let { vm.transformPoint3D(selectedPoint3DIndex) { p -> p.copy(position = p.position + Vec3(0.0, 0.0, .25)) } }
                        selectedVector?.let { vm.moveVector3D(selectedVectorIndex, Vec3(0.0, 0.0, .25)) }
                    }
                    DestructiveGlowButton("Delete", icon = "×", iconOnly = compact, onClick = ::deleteCurrent3DSelection)
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
            AddShapeTarget(
                onAdd = { addShapeOpen = true },
                label = "+ Add",
                contentDescription = "Add a 3D solid to the workspace",
            )
            DeleteDropTarget(
                enabled = selectedSolid != null || selectedVector != null || selectedPoint3D != null,
                onDelete = ::deleteCurrent3DSelection,
            )
            DestructiveGlowButton(
                "Clear all",
                enabled = vm.state.solids.isNotEmpty() || vm.state.vectors3D.isNotEmpty() || vm.state.points3D.isNotEmpty(),
                icon = "×",
                onClick = onRequestClearAll,
            )
            }
        }
        if (!vm.shapeExplorerScene && selectedSolid != null) SmartSelectionHud(
            title = subSelection?.let { "${selectedSolid.type.name} - ${it.mode.name} ${it.index + 1}" } ?: selectedSolid.type.name,
            instruction = if (subSelection != null) "Sub-object selected - use coloured gizmo handles - empty space orbits" else "Drag a coloured gizmo handle to ${transformMode.name.lowercase()} on one axis",
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 206.dp),
        ) {
            WorkspaceAppearancePicker(
                appearance = solidAppearances[selectedIndex] ?: sceneAppearance.copy(colorIndex = selectedIndex),
                onChange = { updated ->
                    solidAppearances = solidAppearances + (selectedIndex to updated)
                    if (updated.paletteId != sceneAppearance.paletteId) sceneAxisStyle = updated.palette.axes
                    sceneAppearance = updated
                },
                modifier = Modifier.fillMaxWidth(),
            )
            WorkspaceAxisPicker(
                axes = sceneAxisStyle,
                palette = sceneAppearance.palette,
                onChange = { sceneAxisStyle = it },
            )
            SolidObjectCombo(
                solids = vm.state.solids,
                selectedIndex = selectedIndex,
                onSelect = { index ->
                    vm.selectSolid(index)
                    selectedSolidIndices = setOf(index)
                    subSelection = null
                },
            )
            SelectedSolidDetails(selectedSolid)
            Transform3DMode.entries.forEach { mode ->
                GlowButton(if (mode == transformMode) "• ${mode.name}" else mode.name) {
                    selectTransformMode(mode)
                }
            }
            GlowButton("Size −") {
                vm.transformSolid(selectedIndex) { solid ->
                    solid.copy(
                        width = (solid.width * .9).coerceAtLeast(.2),
                        height = (solid.height * .9).coerceAtLeast(.2),
                        depth = (solid.depth * .9).coerceAtLeast(.2),
                        radius = (solid.radius * .9).coerceAtLeast(.1),
                        topRadius = (solid.topRadius * .9).coerceAtLeast(.05),
                    )
                }
            }
            GlowButton("Size +") {
                vm.transformSolid(selectedIndex) { solid ->
                    solid.copy(width = solid.width * 1.1, height = solid.height * 1.1, depth = solid.depth * 1.1, radius = solid.radius * 1.1, topRadius = solid.topRadius * 1.1)
                }
            }
            listOf(
                "X−" to Vec3(-.25, 0.0, 0.0), "X+" to Vec3(.25, 0.0, 0.0),
                "Y−" to Vec3(0.0, -.25, 0.0), "Y+" to Vec3(0.0, .25, 0.0),
                "Z−" to Vec3(0.0, 0.0, -.25), "Z+" to Vec3(0.0, 0.0, .25),
            ).forEach { (label, delta) ->
                GlowButton("Move $label") { vm.transformSolid(selectedIndex) { it.copy(position = it.position + delta) } }
            }
            listOf("Rotate X" to Vec3(15.0, 0.0, 0.0), "Rotate Y" to Vec3(0.0, 15.0, 0.0), "Rotate Z" to Vec3(0.0, 0.0, 15.0)).forEach { (label, delta) ->
                GlowButton(label) { vm.transformSolid(selectedIndex) { it.copy(rotation = it.rotation + delta) } }
            }
            GlowButton(if (selectedIndex in lockedSolidIndices) "Unlock" else "Lock") {
                lockedSolidIndices = if (selectedIndex in lockedSolidIndices) lockedSolidIndices - selectedIndex else lockedSolidIndices + selectedIndex
            }
            GlowButton("Copy") { vm.duplicateSelectedSolid(); selectedSolidIndices = setOf(vm.selectedSolid) }
            subSelection?.takeIf { it.mode == Selection3DMode.Face }?.let { face ->
                GlowButton("Preview extrusion") {
                    selectedSolid?.let { extrusionPreview = com.indianservers.aiexplorer.core.SpatialExtrusionEngine.preview(selectedIndex, face.index, .25, it) }
                }
            }
            GlowButton("Mirror X") { vm.transformSolid(selectedIndex) { AdvancedSpatialInteractionEngine.reflect(it, SpatialAlignment.X) } }
            DestructiveGlowButton("Delete", icon = "×", onClick = ::deleteCurrent3DSelection)
        }
        if (!vm.shapeExplorerScene && formulaInspectorOpen && selectedSolid != null) {
            DimmedWorkspaceScrim { formulaInspectorOpen = false }
            Selected3DFormulaInspector(
                solid = selectedSolid,
                index = selectedIndex,
                bounds = selectedBounds,
                subSelection = subSelection?.takeIf { it.solidIndex == selectedIndex },
                onClose = { formulaInspectorOpen = false },
                onCopy = {
                    copyShapeText(
                        context,
                        "${selectedSolid.type.name} formulas",
                        buildSelected3DFormulaReport(selectedSolid, selectedBounds),
                    )
                },
                modifier = Modifier
                    .align(if (compact) Alignment.Center else Alignment.TopStart)
                    .padding(top = if (compact) 0.dp else 150.dp, start = if (compact) 0.dp else 10.dp)
                    .fillMaxWidth(if (compact) .94f else .36f)
                    .widthIn(min = 280.dp, max = 390.dp),
            )
        }
        extrusionPreview?.let { preview ->
            Column(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 142.dp).clip(RoundedCornerShape(14.dp))
                    .background(SurfaceA.copy(.97f)).border(1.dp, Amber.copy(.65f), RoundedCornerShape(14.dp)).padding(9.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("Extrusion preview - face ${preview.faceIndex + 1} - ${trim(preview.amount)} units", color = Amber, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GlowButton("Commit", enabled = preview.valid) {
                        vm.transformSolid(preview.solidIndex) { preview.preview }
                        meshEditHistory = meshEditHistory.record(com.indianservers.aiexplorer.core.MeshEdit3D("Extrude face", preview.solidIndex, preview.original, preview.preview, SpatialSubObjectType.Face, preview.faceIndex))
                        extrusionPreview = null
                    }
                    GlowButton("Cancel") { extrusionPreview = null }
                }
            }
        }
        if (!vm.shapeExplorerScene) Row(
            Modifier
                .align(Alignment.TopStart)
                .padding(top = workspaceToolTop, start = 10.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceA.copy(.82f))
                .clickable { spatialViewToolsExpanded = !spatialViewToolsExpanded }
                .padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            TransparentIcon("V", Cyan)
            Text(if (spatialViewToolsExpanded) "Hide view" else "View", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text("${trim(zoom.toDouble())}x", color = Muted, fontSize = 10.sp)
        }
        if (!vm.shapeExplorerScene && spatialViewToolsExpanded) Row(
            Modifier.align(Alignment.TopStart).padding(top = 116.dp, start = 10.dp).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            GlowButton("⌂ Home") { rotateX = 25f; rotateY = -35f; rotateZ = 15f; zoom = 1f; cameraPan = Offset.Zero }
            GlowButton(visualMode.displayLabel(), icon = visualMode.displayIcon()) {
                visualMode = visualMode.nextDisplayMode()
            }
            GlowButton("Cycle overlap", enabled = visibleSolidIndices.isNotEmpty()) {
                val ordered = visibleSolidIndices.sorted(); val next = ordered.firstOrNull { it > selectedIndex } ?: ordered.firstOrNull()
                next?.let { vm.selectSolid(it); selectedSolidIndices = setOf(it) }
            }
            GlowButton("Hide", enabled = selectedSolidIndices.isNotEmpty()) { hiddenSolidIndices += selectedSolidIndices; selectedSolidIndices = emptySet() }
            GlowButton(if (isolatedSolidIndices == null) "Isolate" else "End isolate", enabled = selectedSolidIndices.isNotEmpty() || isolatedSolidIndices != null) {
                isolatedSolidIndices = if (isolatedSolidIndices == null) selectedSolidIndices.takeIf { it.isNotEmpty() } else null
            }
            GlowButton("Show all", enabled = hiddenSolidIndices.isNotEmpty()) { hiddenSolidIndices = emptySet(); isolatedSolidIndices = null }
            if (gestureMode != GestureMode.Idle) Text(gestureMode.label, color = Cyan, modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(SurfaceA).padding(9.dp))
            Text("${trim(zoom.toDouble())}×", color = Muted, modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(SurfaceA).padding(9.dp))
        }
        if (vm.showLeftPanel) GlassPanel(Modifier.align(Alignment.TopStart).padding(top = 64.dp).width(230.dp)) {
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
                        .clickable { vm.selectSolid(index); selectedSolidIndices = setOf(index) }
                        .padding(8.dp),
                )
                Insight("Measure", "V ${trim(measure.volume)} - A ${trim(measure.surfaceArea)}", accent)
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
            if (vm.state.points3D.isNotEmpty()) {
                Text("Points", color = Ink, fontWeight = FontWeight.SemiBold)
                vm.state.points3D.forEachIndexed { index, point ->
                    val accent = if (index == selectedPoint3DIndex) Cyan else Amber
                    Text(
                        text = "${if (index == selectedPoint3DIndex) "• " else ""}${point.name} = (${trim(point.position.x)}, ${trim(point.position.y)}, ${trim(point.position.z)})",
                        color = accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { vm.selectPoint3D(index) }
                            .padding(8.dp),
                    )
                    Insight("Distance from origin", trim(point.distanceFromOrigin), accent)
                }
            }
        }
        if (vm.showRightPanel && vm.shapeExplorerScene && selectedSolid != null) Shape3DStudioPanel(vm, selectedIndex, selectedSolid, compact, Modifier.align(if (compact) Alignment.Center else Alignment.TopEnd))
        if (vm.showRightPanel && !vm.shapeExplorerScene) GlassPanel(Modifier.align(Alignment.TopEnd).padding(top = 64.dp).width(260.dp)) {
            PanelHeader("3D Context Inspector", vm::hidePanels, Violet)
            Insight("Shared GPU", "${sharedRenderScene.primitives.size} objects - ${sharedRenderPlan.vertices.size / 10} vertices", Cyan)
            listOf("Zoom +", "Zoom -", "Scale +", "Scale -", "Reset view").forEach {
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
                    if (it == "Reset view") {
                        rotateX = 25f
                        rotateY = -35f
                        rotateZ = 15f
                        zoom = 1f
                        cameraPan = Offset.Zero
                    }
                })
            }
            selectedSolid?.let {
                val measure = Geometry3D.measure(it)
                Insight("Selected", it.type.name, Cyan)
                Insight("Faces", measure.faces.toString(), Violet)
                Insight("Edges", measure.edges.toString(), Violet)
                Insight("Vertices", measure.vertices.toString(), Violet)
                Geometry3D.formulas(it.type).forEach { (name, formula) -> Insight(name, formula, Green) }
                GlowButton(if (selectedIndex in lockedSolidIndices) "Unlock object" else "Lock object") {
                    lockedSolidIndices = if (selectedIndex in lockedSolidIndices) lockedSolidIndices - selectedIndex else lockedSolidIndices + selectedIndex
                }
                DestructiveGlowButton("Delete", onClick = ::deleteCurrent3DSelection)
                selectedBounds?.let { bounds ->
                    Insight("Bounds min", "${trim(bounds.minimum.x)},${trim(bounds.minimum.y)},${trim(bounds.minimum.z)}", Green)
                    Insight("Bounds max", "${trim(bounds.maximum.x)},${trim(bounds.maximum.y)},${trim(bounds.maximum.z)}", Green)
                }
                subSelection?.takeIf { selection -> selection.solidIndex == selectedIndex }?.let { selection ->
                    Insight("Sub-object", "${selection.mode.name} ${selection.index + 1}", Amber)
                    val anchor = subObjectAnchorWorld(it, selection)
                    Insight("World position", "${trim(anchor.x)}, ${trim(anchor.y)}, ${trim(anchor.z)}", Cyan)
                    Text("The transform gizmo is anchored to this ${selection.mode.name.lowercase()}. Drag a coloured axis or ring for a constrained edit.", color = Muted, fontSize = 10.sp)
                }
                if (sectionEnabled || clipSection) {
                    Insight("Section normal", "${trim(sectionPlane.unitNormal.x)}, ${trim(sectionPlane.unitNormal.y)}, ${trim(sectionPlane.unitNormal.z)}", Amber)
                    Insight("Plane offset", trim(sectionPlane.offset), Amber)
                }
            }
            selectedVector?.let {
                Insight("Vector", it.name, Amber)
                Insight("Components", "<${trim(it.components.x)}, ${trim(it.components.y)}, ${trim(it.components.z)}>", Green)
                Insight("Magnitude", trim(it.magnitude), Amber)
            }
            selectedPoint3D?.let {
                Insight("Point", it.name, Cyan)
                Insight("Coordinates", "(${trim(it.position.x)}, ${trim(it.position.y)}, ${trim(it.position.z)})", Green)
                Insight("Distance from origin", trim(it.distanceFromOrigin), Amber)
                Insight("Properties", "${if (it.visible) "visible" else "hidden"} · ${if (it.locked) "locked" else "editable"} · ${it.styleKey}", Violet)
            }
        }
        if (vm.showBottomPanel) GlassPanel(Modifier.align(Alignment.BottomStart).fillMaxWidth()) {
            PanelHeader("3D View & Advanced Controls", vm::hidePanels, Ink)
            AxisSlider("Camera X", rotateX, -180f..180f) { rotateX = it }
            AxisSlider("Camera Y", rotateY, -180f..180f) { rotateY = it }
            AxisSlider("Camera Z", rotateZ, -180f..180f) { rotateZ = it }
            AxisSlider("Camera zoom", zoom, .6f..1.8f) { zoom = it }
            TogglePill("Grid", showWorkspaceGrid) { showWorkspaceGrid = it }
            AxisSlider("Grid size", workspaceGridSize, .5f..3f) { workspaceGridSize = it }
            TogglePill("Perspective", projection == CameraProjection.Perspective) {
                projection = if (it) CameraProjection.Perspective else CameraProjection.Orthographic
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf(AxisConstraint.Free, AxisConstraint.X, AxisConstraint.Y, AxisConstraint.Z).forEach { axis ->
                    GlowButton(if (axisConstraint == axis) "● ${axis.name}" else axis.name) { axisConstraint = axis }
                }
                GlowButton(if (precisionMode) "● Precision" else "Precision") { precisionMode = !precisionMode }
            }
            Text("Professional direct manipulation", color = Cyan, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SpatialTransformSpace.entries.forEach { space -> GlowButton(if (transformSpace == space) "● ${space.name}" else space.name) { transformSpace = space } }
                SpatialDragPlane.entries.forEach { plane -> GlowButton(if (dragPlane == plane) "● ${plane.name}" else plane.name) { dragPlane = plane } }
                SpatialVisualMode.entries.forEach { mode ->
                    GlowButton(if (visualMode == mode) "● ${mode.displayLabel()}" else mode.displayLabel(), icon = mode.displayIcon()) {
                        visualMode = mode
                    }
                }
            }
            AxisSlider("Exploded view", explodeAmount, 0f..3f) { explodeAmount = it }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                TogglePill("Multi-select", multiSelectEnabled) {
                    multiSelectEnabled = it
                    if (!it) selectedSolidIndices = selectedSolid?.let { setOf(selectedIndex) } ?: emptySet()
                }
                TogglePill("Scene navigator", showSceneNavigator) { showSceneNavigator = it }
                TogglePill("Motion trails", showMotionTrails) { showMotionTrails = it; if (!it) motionTrail = emptyList() }
                GlowButton("Group", enabled = selectedSolidIndices.size >= 2) { solidGroups = solidGroups + listOf(selectedSolidIndices) }
                GlowButton("Ungroup", enabled = solidGroups.any { it == selectedSolidIndices }) { solidGroups = solidGroups.filterNot { it == selectedSolidIndices } }
            }
            if (selectedSolidIndices.size >= 2) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SpatialAlignment.entries.forEach { alignment ->
                    GlowButton("Align ${alignment.name}") { vm.replaceSolids("Aligned objects on ${alignment.name}") { AdvancedSpatialInteractionEngine.align(it, selectedSolidIndices, alignment) } }
                    GlowButton("Space ${alignment.name}", enabled = selectedSolidIndices.size >= 3) { vm.replaceSolids("Distributed objects on ${alignment.name}") { AdvancedSpatialInteractionEngine.distribute(it, selectedSolidIndices, alignment) } }
                }
                GlowButton("Concentric") {
                    vm.replaceSolids("Applied concentric constraint") { solids ->
                        val anchor = selectedSolidIndices.firstOrNull()?.let(solids::getOrNull)?.position ?: return@replaceSolids solids
                        solids.mapIndexed { index, solid -> if (index in selectedSolidIndices) solid.copy(position = anchor) else solid }
                    }
                }
                GlowButton("Parallel orientation") {
                    vm.replaceSolids("Applied parallel-orientation constraint") { solids ->
                        val rotation = selectedSolidIndices.firstOrNull()?.let(solids::getOrNull)?.rotation ?: return@replaceSolids solids
                        solids.mapIndexed { index, solid -> if (index in selectedSolidIndices) solid.copy(rotation = rotation) else solid }
                    }
                }
                com.indianservers.aiexplorer.core.BooleanMeshOperation.entries.forEach { operation ->
                    GlowButton(operation.name) {
                        val pair = selectedSolidIndices.take(2).mapNotNull(vm.state.solids::getOrNull)
                        if (pair.size == 2) booleanResult = runCatching {
                            fun worldMesh(solid: Solid): com.indianservers.aiexplorer.core.SolidMesh {
                                val mesh = SolidMeshFactory.create(solid)
                                return mesh.copy(vertices = mesh.vertices.map { solidLocalToWorld(solid, it) })
                            }
                            com.indianservers.aiexplorer.core.BooleanMeshEngine.apply(worldMesh(pair[0]), worldMesh(pair[1]), operation)
                        }.getOrNull()
                    }
                }
            }
            booleanResult?.let { result ->
                Insight("Boolean ${result.operation.name.lowercase()}", "${result.mesh.vertices.size} vertices - ${result.mesh.faces.size} faces - ${result.diagnostic}", if (result.verified) Green else Amber)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GlowButton("Copy Boolean OBJ") { copyShapeText(context, "boolean.obj", com.indianservers.aiexplorer.core.SpatialExportEngine.obj(result.mesh, "Boolean${result.operation.name}")) }
                    GlowButton("Copy Boolean STL") { copyShapeText(context, "boolean.stl", com.indianservers.aiexplorer.core.SpatialExportEngine.stl(result.mesh, "Boolean${result.operation.name}")) }
                    GlowButton("Clear Boolean") { booleanResult = null }
                }
            }
            if (meshEditHistory.undo.isNotEmpty() || meshEditHistory.redo.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                GlowButton("Undo mesh", enabled = meshEditHistory.undo.isNotEmpty()) {
                    val (solids, history) = meshEditHistory.undo(vm.state.solids); vm.replaceSolids("Undo sub-object edit") { solids }; meshEditHistory = history
                }
                GlowButton("Redo mesh", enabled = meshEditHistory.redo.isNotEmpty()) {
                    val (solids, history) = meshEditHistory.redo(vm.state.solids); vm.replaceSolids("Redo sub-object edit") { solids }; meshEditHistory = history
                }
            }
            if (spatialMeasurements.isNotEmpty()) {
                Text("Pinned measurements", color = Ink, fontWeight = FontWeight.SemiBold)
                spatialMeasurements.forEach { value -> Insight(value.label, "${trim(value.value)} ${value.unit}", Green) }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GlowButton("Export measurements") { copyShapeText(context, "measurements.csv", com.indianservers.aiexplorer.core.SpatialExportEngine.measurementsCsv(spatialMeasurements)) }
                    GlowButton("Clear measurements") { spatialMeasurements = emptyList() }
                }
            }
            if (selectedSolidIndices.size >= 2) {
                val pair = selectedSolidIndices.take(2).mapNotNull(vm.state.solids::getOrNull)
                if (pair.size == 2) {
                    Insight("Measurement anchor", "distance ${trim((pair[0].position - pair[1].position).magnitude())}", Green)
                    GlowButton("Pin distance") { spatialMeasurements += com.indianservers.aiexplorer.core.SpatialMeasurementEngine.distance(pair[0].position, pair[1].position) }
                }
                val triple = selectedSolidIndices.take(3).mapNotNull(vm.state.solids::getOrNull)
                if (triple.size == 3) GlowButton("Pin angle") { spatialMeasurements += com.indianservers.aiexplorer.core.SpatialMeasurementEngine.angle(triple[0].position, triple[1].position, triple[2].position) }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                GlowButton("Front") { rotateX = 0f; rotateY = 0f; rotateZ = 0f }
                GlowButton("Top") { rotateX = 90f; rotateY = 0f; rotateZ = 0f }
                GlowButton("Side") { rotateX = 0f; rotateY = 90f; rotateZ = 0f }
                GlowButton("Isometric") { rotateX = 25f; rotateY = -35f; rotateZ = 15f }
                GlowButton("Save camera") { cameraBookmarks = (cameraBookmarks + SpatialCameraBookmark("View ${cameraBookmarks.size + 1}", Vec3(rotateX.toDouble(), rotateY.toDouble(), rotateZ.toDouble()), zoom, cameraPan.x, cameraPan.y)).takeLast(6) }
            }
            if (cameraBookmarks.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                cameraBookmarks.forEach { bookmark -> GlowButton(bookmark.name) { rotateX = bookmark.rotation.x.toFloat(); rotateY = bookmark.rotation.y.toFloat(); rotateZ = bookmark.rotation.z.toFloat(); zoom = bookmark.zoom; cameraPan = Offset(bookmark.panX, bookmark.panY) } }
            }
            TogglePill("Cross-section", sectionEnabled) { sectionEnabled = it }
            TogglePill("Clip below plane", clipSection) { clipSection = it }
            if (sectionEnabled || clipSection) {
                Text("Editable section plane", color = Amber, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("X" to Vec3(1.0, 0.0, 0.0), "Y" to Vec3(0.0, 1.0, 0.0), "Z" to Vec3(0.0, 0.0, 1.0), "Diagonal" to Vec3(1.0, 1.0, 1.0)).forEach { (label, normal) ->
                        GlowButton(label) { sectionPlane = sectionPlane.withNormal(normal) }
                    }
                }
                AxisSlider("Plane offset", sectionPlane.offset.toFloat(), -3f..3f) { value -> sectionPlane = sectionPlane.copy(origin = sectionPlane.unitNormal * value.toDouble()) }
                AxisSlider("Normal X", sectionPlane.unitNormal.x.toFloat(), -1f..1f) { value -> sectionPlane = sectionPlane.withNormal(Vec3(value.toDouble(), sectionPlane.unitNormal.y, sectionPlane.unitNormal.z)) }
                AxisSlider("Normal Y", sectionPlane.unitNormal.y.toFloat(), -1f..1f) { value -> sectionPlane = sectionPlane.withNormal(Vec3(sectionPlane.unitNormal.x, value.toDouble(), sectionPlane.unitNormal.z)) }
                AxisSlider("Normal Z", sectionPlane.unitNormal.z.toFloat(), -1f..1f) { value -> sectionPlane = sectionPlane.withNormal(Vec3(sectionPlane.unitNormal.x, sectionPlane.unitNormal.y, value.toDouble())) }
                DirectVec3Editor("Plane origin", sectionPlane.origin) { sectionPlane = sectionPlane.copy(origin = it) }
                DirectVec3Editor("Plane normal", sectionPlane.unitNormal) { sectionPlane = sectionPlane.withNormal(it) }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GlowButton("Measure section", enabled = selectedSolid != null) {
                        selectedSolid?.let { solid ->
                            val mesh = SolidMeshFactory.create(solid).let { value -> value.copy(vertices = value.vertices.map { solidLocalToWorld(solid, it) }) }
                            val section = CrossSection3D.intersect(mesh, sectionPlane.unitNormal, sectionPlane.offset)
                            if (section.size >= 2) spatialMeasurements += com.indianservers.aiexplorer.core.SpatialMeasurementEngine.sectionPerimeter(section)
                        }
                    }
                    GlowButton("Export section", enabled = selectedSolid != null) {
                        selectedSolid?.let { solid ->
                            val mesh = SolidMeshFactory.create(solid).let { value -> value.copy(vertices = value.vertices.map { solidLocalToWorld(solid, it) }) }
                            val section = CrossSection3D.intersect(mesh, sectionPlane.unitNormal, sectionPlane.offset)
                            copyShapeText(context, "section.csv", com.indianservers.aiexplorer.core.SpatialExportEngine.contoursCsv(section.map { sectionPlane.offset to it }))
                        }
                    }
                }
                Text("Drag the amber normal handle directly on the canvas to move the plane.", color = Muted, fontSize = 10.sp)
            }
            selectedSolid?.let { solid ->
                Text("Selected ${solid.type.name}", color = Cyan, fontWeight = FontWeight.SemiBold)
                DirectVec3Editor("Position", solid.position) { value -> vm.transformSolid(selectedIndex) { it.copy(position = value) } }
                DirectVec3Editor("Rotation (degrees)", solid.rotation) { value -> vm.transformSolid(selectedIndex) { it.copy(rotation = value) } }
                DirectVec3Editor("Scale / dimensions", Vec3(solid.width, solid.height, solid.depth)) { value ->
                    vm.transformSolid(selectedIndex) { it.copy(width = value.x.coerceAtLeast(.1), height = value.y.coerceAtLeast(.1), depth = value.z.coerceAtLeast(.1)) }
                }
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
                val mesh = SolidMeshFactory.create(solid)
                val euler = com.indianservers.aiexplorer.core.SolidInteractionLab.euler(mesh)
                Insight("Euler proof", "V ${euler.vertices} - E ${euler.edges} + F ${euler.faces} = ${euler.value}${if (euler.holds) " ✓" else ""}", Green)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    GlowButton("Measure volume") { spatialMeasurements += com.indianservers.aiexplorer.core.SpatialMeasurementEngine.volume(solid) }
                    subSelection?.takeIf { it.solidIndex == selectedIndex && it.mode == Selection3DMode.Face }?.let { selection ->
                        GlowButton("Measure face") { runCatching { com.indianservers.aiexplorer.core.SpatialMeasurementEngine.faceArea(mesh, selection.index) }.getOrNull()?.let { spatialMeasurements += it } }
                    }
                    GlowButton("Unfold net") {
                        val net = com.indianservers.aiexplorer.core.SolidInteractionLab.net(mesh)
                        copyShapeText(context, "${solid.type.name} net", net.joinToString("\n") { face -> face.joinToString(";") { "${it.x},${it.y}" } })
                    }
                    GlowButton("Revolve profile") {
                        val revolved = com.indianservers.aiexplorer.core.SolidInteractionLab.solidOfRevolution(listOf(Vec2(-solid.height / 2, 0.0), Vec2(-solid.height / 2, solid.radius), Vec2(solid.height / 2, solid.radius), Vec2(solid.height / 2, 0.0)))
                        copyShapeText(context, "solid-of-revolution.obj", com.indianservers.aiexplorer.core.SpatialExportEngine.obj(revolved, "SolidOfRevolution"))
                    }
                    GlowButton("Export OBJ") { copyShapeText(context, "${solid.type.name}.obj", com.indianservers.aiexplorer.core.SpatialExportEngine.obj(mesh, solid.type.name)) }
                    GlowButton("Export STL") { copyShapeText(context, "${solid.type.name}.stl", com.indianservers.aiexplorer.core.SpatialExportEngine.stl(mesh, solid.type.name)) }
                }
            }
            selectedVector?.let { vector ->
                Text("Selected vector ${vector.name}", color = Amber, fontWeight = FontWeight.SemiBold)
                DirectVec3Editor("Vector start", vector.start) { value -> vm.transformVector3D(selectedVectorIndex) { it.copy(start = value) } }
                DirectVec3Editor("Vector end", vector.end) { value -> vm.transformVector3D(selectedVectorIndex) { it.copy(end = value) } }
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
            selectedPoint3D?.let { point ->
                Text("Selected point ${point.name}", color = Cyan, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = point.name,
                    onValueChange = { name -> vm.transformPoint3D(selectedPoint3DIndex) { it.copy(name = name) } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Point name") },
                    singleLine = true,
                )
                DirectVec3Editor("Point coordinates", point.position) { value ->
                    vm.transformPoint3D(selectedPoint3DIndex) { it.copy(position = value) }
                }
                Insight("Distance from origin", trim(point.distanceFromOrigin), Amber)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    GlowButton(if (point.visible) "Hide point" else "Show point") {
                        vm.transformPoint3D(selectedPoint3DIndex) { it.copy(visible = !it.visible) }
                    }
                    GlowButton(if (point.locked) "Unlock point" else "Lock point") {
                        vm.transformPoint3D(selectedPoint3DIndex) { it.copy(locked = !it.locked) }
                    }
                    GlowButton("Cycle style") {
                        val next = when (point.styleKey) {
                            "default" -> "reference"
                            "reference" -> "highlight"
                            else -> "default"
                        }
                        vm.transformPoint3D(selectedPoint3DIndex) { it.copy(styleKey = next) }
                    }
                    DestructiveGlowButton("Delete point", icon = "×") { vm.deletePoint3D(selectedPoint3DIndex) }
                }
            }
        }
        if (addShapeOpen) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = .62f))
                    .clickable { addShapeOpen = false },
                contentAlignment = Alignment.Center,
            ) {
                BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = if (compact) Alignment.BottomCenter else Alignment.Center) {
                    val sheetModifier = if (compact) {
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = maxHeight * .72f)
                    } else {
                        Modifier
                            .fillMaxWidth(.95f)
                            .widthIn(max = 760.dp)
                            .heightIn(max = 590.dp)
                    }
                    SolidShapeLibrary(
                        sceneObjectCount = vm.state.solids.size + vm.state.vectors3D.size + vm.state.points3D.size,
                        onDismiss = { addShapeOpen = false },
                        onAdd = { type ->
                            vm.addSolid(type)
                            selectedSolidIndices = setOf(vm.selectedSolid)
                            hiddenSolidIndices -= vm.selectedSolid
                            isolatedSolidIndices = null
                            addShapeOpen = false
                        },
                        onAddVector = {
                            vm.addVector3D()
                            addShapeOpen = false
                        },
                        onAddSegment = {
                            vm.addSegment3D()
                            addShapeOpen = false
                        },
                        onAddLine = {
                            vm.addLine3D()
                            addShapeOpen = false
                        },
                        onAddRay = {
                            vm.addRay3D()
                            addShapeOpen = false
                        },
                        onAddPoint = {
                            vm.addPoint3D()
                            addShapeOpen = false
                        },
                        modifier = sheetModifier.clickable(enabled = false) {},
                    )
                }
            }
        }
    }
}

@Composable
private fun SpatialARScreen(vm: ExplorerViewModel) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val runtime = remember(activity) { activity?.let(::ArCoreRuntime) }
    var compositorView by remember { mutableStateOf<ARCoreCompositorView?>(null) }
    var capabilities by remember { mutableStateOf(ARCapabilities(ARAvailability.Checking)) }
    var liveAR by remember { mutableStateOf(false) }
    var frameState by remember { mutableStateOf<ARFrameState?>(null) }
    var arFrame by remember { mutableStateOf<ArFrameSnapshot?>(null) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var placementMode by remember { mutableStateOf(false) }
    var reticleHit by remember { mutableStateOf<ArHitCandidate?>(null) }
    var arSelection by remember { mutableStateOf(ArSelectionState()) }
    var overlapHits by remember { mutableStateOf<List<ArPickHit>>(emptyList()) }
    var stylusHoverHit by remember { mutableStateOf<ArPickHit?>(null) }
    var gizmoMode by remember { mutableStateOf(ArGizmoMode.Translate) }
    var gizmoAxis by remember { mutableStateOf(ArGizmoAxis.X) }
    var arMultiSelect by remember { mutableStateOf(false) }
    var subObjectKind by remember { mutableStateOf(ArSubObjectKind.Whole) }
    var snapEnabled by remember { mutableStateOf(true) }
    var precisionMode by remember { mutableStateOf(false) }
    var arClipboard by remember { mutableStateOf<List<Solid>>(emptyList()) }
    var arGroups by remember { mutableStateOf<List<Set<String>>>(emptyList()) }
    var numericPosition by remember { mutableStateOf("0, 0, 0") }
    var numericRotation by remember { mutableStateOf("0, 0, 0") }
    var numericScale by remember { mutableStateOf("1") }
    var numericPlaneNormal by remember { mutableStateOf("0, 1, 0") }
    var numericPlaneOffset by remember { mutableStateOf("0") }
    var arAnalysisEnabled by remember { mutableStateOf(false) }
    var arTraceX by remember { mutableFloatStateOf(0f) }
    var arTraceY by remember { mutableFloatStateOf(0f) }
    var arContourLevel by remember { mutableFloatStateOf(0f) }
    var arGradientAscending by remember { mutableStateOf(true) }
    var arGradientStep by remember { mutableIntStateOf(0) }
    var arGradientPlaying by remember { mutableStateOf(false) }
    var liveError by remember { mutableStateOf("") }
    var selectedLesson by remember { mutableIntStateOf(0) }
    var arWorkspaceMode by remember {
        mutableStateOf(
            when {
                vm.state.surfaceExpression.isNotBlank() -> ArMathWorkspaceMode.Graph3D
                vm.state.functions.any { it.visible } -> ArMathWorkspaceMode.Graph2D
                vm.state.solids.isNotEmpty() || vm.state.vectors3D.isNotEmpty() -> ArMathWorkspaceMode.Geometry3D
                vm.state.points.isNotEmpty() || vm.state.shapes.isNotEmpty() -> ArMathWorkspaceMode.Geometry2D
                else -> ArMathWorkspaceMode.Graph3D
            },
        )
    }
    var thermalLevel by remember { mutableStateOf(ThermalLevel.Nominal) }
    var showSpatialDetails by remember { mutableStateOf(false) }
    var showAdvancedTools by remember { mutableStateOf(false) }
    var showAnalysisTools by remember { mutableStateOf(false) }
    var arHudExpanded by remember { mutableStateOf(false) }
    var arHudHidden by remember { mutableStateOf(false) }
    val graphArWorkspace = arWorkspaceMode == ArMathWorkspaceMode.Graph2D || arWorkspaceMode == ArMathWorkspaceMode.Graph3D
    var displayFirstMode by remember { mutableStateOf(!graphArWorkspace) }
    var arPlacementMode by rememberSaveable { mutableStateOf(if (graphArWorkspace) ArPlacementMode.FloorTable else ArPlacementMode.Viewer) }
    var showArAddOptions by remember { mutableStateOf(false) }
    var arGraphExpressionDraft by rememberSaveable { mutableStateOf("x^2") }
    var arSurfaceExpressionDraft by rememberSaveable { mutableStateOf("z = x^2 + y^2") }
    var arOverlayPan by remember { mutableStateOf(Offset.Zero) }
    var arOverlayScale by remember { mutableFloatStateOf(.42f) }
    var arOverlayRotationX by remember { mutableFloatStateOf(-18f) }
    var arOverlayRotationY by remember { mutableFloatStateOf(0f) }
    var arOverlayRotationZ by remember { mutableFloatStateOf(0f) }
    var arObjectOpacity by remember { mutableFloatStateOf(.72f) }
    var arShowWireframe by rememberSaveable { mutableStateOf(true) }
    var arShowGrid by rememberSaveable { mutableStateOf(true) }
    var arShowAxes by rememberSaveable { mutableStateOf(true) }
    var arVisualTheme by rememberSaveable { mutableStateOf(ArVisualTheme.NeonGlass) }
    var arSurfaceQuality by rememberSaveable { mutableStateOf(ArSurfaceQuality.Balanced) }
    var autoStartAttempted by remember { mutableStateOf(false) }
    var requestCameraPermission by remember { mutableStateOf(false) }
    val currentLiveAR by rememberUpdatedState(liveAR)
    val currentCompositorView by rememberUpdatedState(compositorView)
    var cameraGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    fun startLiveAr(userRequestedInstall: Boolean = true) {
        if (activity == null) {
            capabilities = capabilities.copy(message = "ARCore requires an Android activity.")
            return
        }
        if (runtime == null) {
            capabilities = capabilities.copy(availability = ARAvailability.Error, message = "ARCore runtime is unavailable; use the spatial simulator.")
            return
        }
        if (!cameraGranted) {
            requestCameraPermission = true
            return
        }
        val prepared = runtime.prepare(cameraPermissionGranted = true, userRequestedInstall = userRequestedInstall)
        capabilities = prepared.toSpatialCapabilities()
        if (prepared is ArRuntimeState.Ready || prepared is ArRuntimeState.Paused || prepared is ArRuntimeState.Running) {
            runtime.resume()
                .onSuccess {
                    capabilities = it.toSpatialCapabilities()
                    liveAR = true
                    liveError = ""
                }
                .onFailure {
                    liveError = it.message ?: "ARCore could not open the camera."
                    liveAR = false
                }
        }
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        cameraGranted = granted
        if (granted) startLiveAr(userRequestedInstall = true)
        else capabilities = capabilities.copy(message = "Camera permission was not granted; the spatial simulator remains fully available.")
    }
    LaunchedEffect(requestCameraPermission) {
        if (requestCameraPermission) {
            requestCameraPermission = false
            cameraPermission.launch(Manifest.permission.CAMERA)
        }
    }
    LaunchedEffect(runtime) {
        if (runtime != null) capabilities = runtime.checkAvailability().toSpatialCapabilities()
    }
    LaunchedEffect(runtime, cameraGranted) {
        if (!autoStartAttempted && runtime != null) {
            autoStartAttempted = true
            startLiveAr(userRequestedInstall = false)
        }
    }
    DisposableEffect(runtime, activity) {
        val observer = object : DefaultLifecycleObserver {
            override fun onPause(owner: LifecycleOwner) {
                currentCompositorView?.onPause()
                runtime?.pause()
            }

            override fun onResume(owner: LifecycleOwner) {
                if (currentLiveAR) {
                    runtime?.resume()
                    currentCompositorView?.onResume()
                }
            }
        }
        activity?.lifecycle?.addObserver(observer)
        onDispose {
            activity?.lifecycle?.removeObserver(observer)
            currentCompositorView?.releaseRenderer()
            currentCompositorView?.onPause()
            runtime?.pause()
            runtime?.close()
        }
    }

    val placement = vm.state.spatialPlacement
    LaunchedEffect(placement.isPlaced, displayFirstMode, arPlacementMode, graphArWorkspace) {
        when {
            displayFirstMode && !placement.isPlaced -> {
                vm.placeSpatialScene()
                placementMode = false
                reticleHit = null
            }
            displayFirstMode -> {
                placementMode = false
                reticleHit = null
            }
            arPlacementMode != ArPlacementMode.Viewer && !placement.isPlaced -> placementMode = true
        }
    }
    LaunchedEffect(liveAR, displayFirstMode, graphArWorkspace) {
        if (displayFirstMode && !graphArWorkspace && liveAR && !vm.state.spatialPlacement.isPlaced) {
            vm.placeSpatialScene()
            placementMode = false
            reticleHit = null
        }
    }
    LaunchedEffect(arWorkspaceMode) {
        arSelection = ArSelectionState()
        overlapHits = emptyList()
        stylusHoverHit = null
        arAnalysisEnabled = arWorkspaceMode == ArMathWorkspaceMode.Graph3D
        if (arWorkspaceMode == ArMathWorkspaceMode.Graph2D || arWorkspaceMode == ArMathWorkspaceMode.Graph3D) {
            arPlacementMode = ArPlacementMode.FloorTable
            displayFirstMode = false
            placementMode = !vm.state.spatialPlacement.isPlaced
            reticleHit = null
        }
    }
    val guidance = SpatialSafety.guidance(placement.trackingQuality)
    val policy = remember(thermalLevel) { SpatialPerformanceManager.policy(thermalLevel, 22.0) }
    val arSurfaceDensity = arSurfaceQuality.density
    val surfaceMesh = remember(arWorkspaceMode, vm.state.surfaceExpression, arSurfaceDensity) {
        if (arWorkspaceMode == ArMathWorkspaceMode.Graph3D) {
            runCatching { Graph3D().mesh(vm.state.surfaceExpression, density = arSurfaceDensity) }.getOrNull()
        } else {
            null
        }
    }
    val lesson = SpatialLessonCatalog.lessons[selectedLesson]
    val linkedArWorkspace = remember(
        arWorkspaceMode,
        vm.state.points,
        vm.state.shapes,
        vm.state.functions,
        vm.state.solids,
        vm.state.vectors3D,
        vm.state.surfaceExpression,
        vm.state.surfaceLayers,
        vm.universalMathDocument,
        arSurfaceDensity,
    ) {
        ArMathWorkspaceBridge.build(
            mode = arWorkspaceMode,
            workspace = vm.state,
            universalDocument = vm.universalMathDocument,
            surfaceDensity = arSurfaceDensity,
        )
    }
    val sharedScene = remember(linkedArWorkspace, lesson.id, placement.depthOcclusionEnabled, frameState?.lighting?.pixelIntensity) {
        linkedArWorkspace.scene.copy(
            annotations = linkedArWorkspace.scene.annotations + SpatialAnnotation(
                "ar-workspace-mode",
                Vec3(0.0, 1.5, 0.0),
                arWorkspaceMode.label,
            ),
            depthOcclusion = placement.depthOcclusionEnabled,
            environmentIntensity = frameState?.lighting?.pixelIntensity ?: 1f,
        )
    }
    val styledSharedScene = remember(sharedScene, arWorkspaceMode, arVisualTheme, arObjectOpacity, arShowWireframe, arShowGrid, arShowAxes) {
        sharedScene.smartArStyle(
            mode = arWorkspaceMode,
            theme = arVisualTheme,
            opacity = arObjectOpacity,
            showWireframe = arShowWireframe,
            showGrid = arShowGrid,
            showAxes = arShowAxes,
        )
    }
    val selectedSolidIndices = arSelection.objectIds.mapNotNullTo(linkedSetOf()) {
        it.removePrefix("solid-").toIntOrNull()?.takeIf(vm.state.solids.indices::contains)
    }
    val selectedVectorIndex = arSelection.primaryObjectId
        ?.removePrefix("vector-")
        ?.toIntOrNull()
        ?.takeIf(vm.state.vectors3D.indices::contains)
    val activeAnchor = runtime?.anchors()?.firstOrNull { it.id == placement.anchorId }
    val anchorPlacementMode = arPlacementMode != ArPlacementMode.Viewer && !displayFirstMode && (placementMode || activeAnchor == null)
    val objectManipulationMode = !displayFirstMode && !anchorPlacementMode && activeAnchor != null
    val phase4Scene = remember(styledSharedScene, arSelection, anchorPlacementMode, reticleHit, gizmoMode, stylusHoverHit) {
        phase4DisplayScene(styledSharedScene, arSelection, anchorPlacementMode && reticleHit != null, vm.state.solids, gizmoMode, stylusHoverHit?.objectId)
    }
    val phase5Analysis = remember(
        phase4Scene,
        surfaceMesh,
        vm.state.surfaceExpression,
        vm.state.solids,
        arSelection.objectIds,
        arAnalysisEnabled,
        arTraceX,
        arTraceY,
        arContourLevel,
        numericPlaneNormal,
        numericPlaneOffset,
        arGradientAscending,
        arGradientStep,
        placement.metersPerMathUnit,
        placement.measurementUncertaintyMeters,
        arFrame?.depth,
    ) {
        ArPhase5AnalysisBridge.enrich(
            source = phase4Scene,
            surface = surfaceMesh,
            expression = vm.state.surfaceExpression,
            solids = vm.state.solids,
            selectedObjectIds = arSelection.objectIds,
            options = ArPhase5AnalysisOptions(
                enabled = arAnalysisEnabled,
                traceX = arTraceX.toDouble(),
                traceY = arTraceY.toDouble(),
                contourLevel = arContourLevel.toDouble(),
                sectionNormal = parseSpatialTriple(numericPlaneNormal) ?: Vec3(0.0, 1.0, 0.0),
                sectionOffset = numericPlaneOffset.toDoubleOrNull() ?: 0.0,
                ascending = arGradientAscending,
                gradientStep = arGradientStep,
            ),
            metersPerMathUnit = placement.metersPerMathUnit,
            poseUncertaintyMeters = placement.measurementUncertaintyMeters,
            depthAvailable = arFrame?.depth != null,
        )
    }
    val interactiveScene = phase5Analysis.scene
    LaunchedEffect(arGradientPlaying, phase5Analysis.gradientSteps) {
        while (arGradientPlaying && phase5Analysis.gradientSteps > 0) {
            delay(100)
            arGradientStep = if (arGradientStep + 1 >= phase5Analysis.gradientSteps) 0 else arGradientStep + 1
        }
    }
    val previewPlacement = if (anchorPlacementMode) reticleHit?.previewSpatialPlacement(placement) ?: placement else placement
    val screenLockedPlacement = previewPlacement.copy(
        anchorId = "",
        pose = previewPlacement.pose.copy(
            positionMeters = Vec3(
                (arOverlayPan.x / 420f).toDouble().coerceIn(-1.25, 1.25),
                (-arOverlayPan.y / 420f).toDouble().coerceIn(-1.45, 1.45),
                -2.8,
            ),
            rotationDegrees = Vec3(
                arOverlayRotationX.toDouble(),
                arOverlayRotationY.toDouble(),
                arOverlayRotationZ.toDouble(),
            ),
            uniformScale = arOverlayScale.toDouble().coerceIn(.18, 1.35),
        ),
        depthOcclusionEnabled = false,
    )
    val canonicalArScene = remember(interactiveScene, placement, activeAnchor, arSelection) {
        ArPhase4SpatialBridge.scene(interactiveScene, placement, activeAnchor, arSelection)
    }
    val trackingAllowsDirectManipulation = !liveAR || arFrame?.camera?.trackingState == ArTrackingState.Tracking
    val gpuPlan = remember(interactiveScene) { SharedGpuSceneCompiler.compile(interactiveScene) }
    val currentCompositorScene by rememberUpdatedState(
        SpatialCompositorScene(
            scene = interactiveScene,
            placement = if (displayFirstMode) screenLockedPlacement else previewPlacement,
            screenLocked = displayFirstMode,
        ),
    )
    fun resetArDisplayNow(forMode: ArMathWorkspaceMode = arWorkspaceMode) {
        arPlacementMode = ArPlacementMode.Viewer
        displayFirstMode = true
        placementMode = false
        reticleHit = null
        arOverlayPan = Offset.Zero
        arOverlayScale = if (forMode == ArMathWorkspaceMode.Graph3D) .50f else .42f
        arOverlayRotationX = if (forMode == ArMathWorkspaceMode.Graph3D) -18f else 0f
        arOverlayRotationY = 0f
        arOverlayRotationZ = 0f
    }
    fun plotArGraphExpression() {
        val expression = arGraphExpressionDraft.trim()
        if (expression.isBlank()) return
        val target = vm.state.functions.lastIndex
        if (target >= 0) {
            vm.updateFunction(target) { it.copy(expression = expression, visible = true) }
        } else {
            vm.addFunction(expression)
        }
        arWorkspaceMode = ArMathWorkspaceMode.Graph2D
        showArAddOptions = false
        resetArDisplayNow(ArMathWorkspaceMode.Graph2D)
    }
    fun plotArSurfaceExpression() {
        val expression = arSurfaceExpressionDraft.trim()
        if (expression.isBlank()) return
        vm.setSurfaceExpression(expression)
        arWorkspaceMode = ArMathWorkspaceMode.Graph3D
        showArAddOptions = false
        resetArDisplayNow(ArMathWorkspaceMode.Graph3D)
    }
    fun clearCurrentArWorkspace() {
        placement.anchorId.takeIf(String::isNotBlank)?.let { runtime?.detachAnchor(it) }
        vm.clearArWorkspace(arWorkspaceMode)
        arSelection = ArSelectionState()
        arGroups = emptyList()
        showArAddOptions = false
        resetArDisplayNow(arWorkspaceMode)
    }
    fun deleteCurrentArItem() {
        when (arWorkspaceMode) {
            ArMathWorkspaceMode.Geometry2D -> {
                if (vm.state.shapes.isNotEmpty()) {
                    if (vm.selectedShape !in vm.state.shapes.indices) vm.selectShape(vm.state.shapes.lastIndex)
                    vm.deleteSelectedShape()
                }
            }
            ArMathWorkspaceMode.Geometry3D -> {
                val target = selectedSolidIndices.ifEmpty {
                    vm.selectedSolid.takeIf { it in vm.state.solids.indices }?.let(::setOf).orEmpty()
                        .ifEmpty { vm.state.solids.lastIndex.takeIf { it >= 0 }?.let(::setOf).orEmpty() }
                }
                if (target.isNotEmpty()) {
                    vm.deleteSelectedSolids(target)
                    arSelection = ArSelectionState()
                } else {
                    selectedVectorIndex?.let(vm::deleteVector3D)
                }
            }
            ArMathWorkspaceMode.Graph2D -> {
                vm.state.functions.lastIndex.takeIf { it >= 0 }?.let(vm::deleteFunction)
            }
            ArMathWorkspaceMode.Graph3D -> {
                vm.setSurfaceExpression("0")
            }
            ArMathWorkspaceMode.CAS -> vm.openMathNotebook()
        }
    }
    fun duplicateCurrentArItem() {
        when (arWorkspaceMode) {
            ArMathWorkspaceMode.Geometry2D -> {
                if (vm.selectedShape !in vm.state.shapes.indices && vm.state.shapes.isNotEmpty()) {
                    vm.selectShape(vm.state.shapes.lastIndex)
                }
                if (vm.selectedShape in vm.state.shapes.indices) vm.duplicateSelectedShape() else vm.open(MathModule.Geometry2D)
            }
            ArMathWorkspaceMode.Geometry3D -> {
                val target = selectedSolidIndices.singleOrNull()
                    ?: vm.selectedSolid.takeIf { it in vm.state.solids.indices }
                target?.let {
                    vm.selectSolid(it)
                    vm.duplicateSelectedSolid()
                    arSelection = ArSelectionState(setOf("solid-${vm.selectedSolid}"), "solid-${vm.selectedSolid}")
                } ?: selectedVectorIndex?.let { index ->
                    val source = vm.state.vectors3D[index]
                    vm.addVector3D()
                    vm.transformVector3D(vm.state.vectors3D.lastIndex) {
                        source.copy(
                            name = "${source.name} copy",
                            start = source.start + Vec3(.35, .15, .35),
                            end = source.end + Vec3(.35, .15, .35),
                        )
                    }
                } ?: vm.open(MathModule.Geometry3D)
            }
            ArMathWorkspaceMode.Graph2D -> vm.state.functions.lastIndex.takeIf { it >= 0 }?.let(vm::duplicateFunction)
            ArMathWorkspaceMode.Graph3D -> {
                val expression = vm.state.surfaceExpression.takeIf { it.isNotBlank() && it != "0" } ?: arSurfaceExpressionDraft
                arSurfaceExpressionDraft = expression
                vm.setSurfaceExpression(expression)
            }
            ArMathWorkspaceMode.CAS -> vm.openMathNotebook()
        }
    }
    fun openCurrentArWorkspaceEditor() {
        when (arWorkspaceMode) {
            ArMathWorkspaceMode.Geometry2D -> vm.open(MathModule.Geometry2D)
            ArMathWorkspaceMode.Geometry3D -> vm.open(MathModule.Geometry3D)
            ArMathWorkspaceMode.Graph2D -> vm.open(MathModule.Graph2D)
            ArMathWorkspaceMode.Graph3D -> vm.open(MathModule.Graph3D)
            ArMathWorkspaceMode.CAS -> vm.openMathNotebook()
        }
    }
    fun resetArDisplay(forMode: ArMathWorkspaceMode = arWorkspaceMode) {
        arPlacementMode = ArPlacementMode.Viewer
        displayFirstMode = true
        placementMode = false
        reticleHit = null
        arOverlayPan = Offset.Zero
        arOverlayScale = if (forMode == ArMathWorkspaceMode.Graph3D) .50f else .42f
        arOverlayRotationX = if (forMode == ArMathWorkspaceMode.Graph3D) -18f else 0f
        arOverlayRotationY = 0f
        arOverlayRotationZ = 0f
    }
    fun rankedHitAt(point: Offset): ArHitCandidate? = runtime
        ?.hitTest(ArVector2(point.x, point.y))
        ?.let(ArHitPolicy::rank)
        ?.firstOrNull(arPlacementMode::accepts)
    LaunchedEffect(arSelection.primaryObjectId, vm.state.solids, vm.state.vectors3D) {
        val solid = arSelection.primaryObjectId?.removePrefix("solid-")?.toIntOrNull()?.let(vm.state.solids::getOrNull)
        val vector = arSelection.primaryObjectId?.removePrefix("vector-")?.toIntOrNull()?.let(vm.state.vectors3D::getOrNull)
        when {
            solid != null -> {
                numericPosition = "${trim(solid.position.x)}, ${trim(solid.position.y)}, ${trim(solid.position.z)}"
                numericRotation = "${trim(solid.rotation.x)}, ${trim(solid.rotation.y)}, ${trim(solid.rotation.z)}"
                numericScale = "1"
            }
            vector != null -> {
                numericPosition = "${trim(vector.start.x)}, ${trim(vector.start.y)}, ${trim(vector.start.z)}"
                numericRotation = "${trim(vector.end.x)}, ${trim(vector.end.y)}, ${trim(vector.end.z)}"
                numericScale = "1"
            }
        }
    }
    Box(Modifier.fillMaxSize().onSizeChanged { viewportSize = it }) {
        if (liveAR && runtime != null && !displayFirstMode) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { viewContext ->
                    ARCoreCompositorView(
                        viewContext,
                        runtime,
                        sceneProvider = { currentCompositorScene },
                        onFrame = { snapshot ->
                            arFrame = snapshot
                            frameState = snapshot.toSpatialFrame()
                            reticleHit = if (anchorPlacementMode && viewportSize.width > 0 && viewportSize.height > 0) {
                                rankedHitAt(Offset(viewportSize.width / 2f, viewportSize.height / 2f))
                            } else {
                                null
                            }
                            liveError = ""
                        },
                        onError = { liveError = it },
                    ).also { compositorView = it }
                },
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(arSelection, gizmoMode, gizmoAxis, anchorPlacementMode, objectManipulationMode, subObjectKind, arMultiSelect, snapEnabled, precisionMode, numericPlaneNormal, numericPlaneOffset, canonicalArScene, arFrame, trackingAllowsDirectManipulation) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            if (!anchorPlacementMode && !objectManipulationMode) {
                                var totalPan = Offset.Zero
                                var totalScale = 1f
                                var totalRotation = 0f
                                while (true) {
                                    val event = awaitPointerEvent()
                                    totalPan += event.calculatePan()
                                    totalScale *= event.calculateZoom()
                                    totalRotation += event.calculateRotation()
                                    arOverlayPan += event.calculatePan()
                                    arOverlayScale = (arOverlayScale * event.calculateZoom()).coerceIn(.18f, 1.35f)
                                    arOverlayRotationZ = (arOverlayRotationZ + event.calculateRotation()).wrapDegrees()
                                    event.changes.forEach { it.consume() }
                                    if (event.changes.none { it.pressed }) break
                                }
                                if (totalPan.getDistance() < 12f && kotlin.math.abs(totalScale - 1f) < .03f && kotlin.math.abs(totalRotation) < 2f) {
                                    showArAddOptions = false
                                }
                                return@awaitEachGesture
                            }
                            if (!trackingAllowsDirectManipulation) {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    event.changes.forEach { it.consume() }
                                    if (event.changes.none { it.pressed }) break
                                }
                                return@awaitEachGesture
                            }
                            val precisionMultiplier = when {
                                down.type == PointerType.Stylus && precisionMode -> .2
                                down.type == PointerType.Stylus -> .35
                                precisionMode -> .4
                                else -> 1.0
                            }
                            val gestureIndices = arSelection.objectIds.mapNotNullTo(linkedSetOf()) {
                                it.removePrefix("solid-").toIntOrNull()?.takeIf(vm.state.solids.indices::contains)
                            }
                            val editableIndices = gestureIndices.filterTo(linkedSetOf()) { "solid-$it" !in arSelection.lockedObjectIds }
                            val vectorGestureIndex = arSelection.primaryObjectId
                                ?.removePrefix("vector-")
                                ?.toIntOrNull()
                                ?.takeIf(vm.state.vectors3D.indices::contains)
                                ?.takeIf { "vector-$it" !in arSelection.lockedObjectIds }
                            val objectGesture = objectManipulationMode && (editableIndices.isNotEmpty() || vectorGestureIndex != null)
                            val basePosition = editableIndices.singleOrNull()?.let(vm.state.solids::getOrNull)?.position
                            val snapTargets = if (snapEnabled && basePosition != null) {
                                vm.state.solids.flatMapIndexed { index, solid ->
                                    if (index in editableIndices) emptyList()
                                    else ConstraintAwareSpatialSnap.targets(SolidMeshFactory.create(solid), solid.position)
                                }
                            } else {
                                emptyList()
                            }
                            if (objectGesture) {
                                when {
                                    vectorGestureIndex != null -> vm.beginVectorDrag(vectorGestureIndex)
                                    editableIndices.size > 1 -> vm.beginSolidGroupDrag(editableIndices)
                                    else -> vm.beginSolidDrag(editableIndices.single())
                                }
                            } else {
                                vm.beginSpatialGesture()
                            }
                            var totalPan = Offset.Zero
                            var totalRotation = 0f
                            var totalScale = 1f
                            while (true) {
                                val event = awaitPointerEvent()
                                totalPan += event.calculatePan()
                                totalRotation += event.calculateRotation()
                                totalScale *= event.calculateZoom()
                                if (objectGesture) {
                                    val raw = Vec3(
                                        totalPan.x / 420.0 * precisionMultiplier,
                                        -totalPan.y / 420.0 * precisionMultiplier,
                                        totalPan.y / 620.0 * precisionMultiplier,
                                    )
                                    val axisDelta = when (gizmoAxis) {
                                        ArGizmoAxis.X -> Vec3(raw.x, 0.0, 0.0)
                                        ArGizmoAxis.Y -> Vec3(0.0, raw.y, 0.0)
                                        ArGizmoAxis.Z -> Vec3(0.0, 0.0, raw.z)
                                        ArGizmoAxis.Uniform -> raw
                                    }
                                    val snappedDelta = if (snapEnabled && basePosition != null) {
                                        val proposed = basePosition + axisDelta
                                        val planeNormal = parseSpatialTriple(numericPlaneNormal)?.takeIf { it.magnitude() > 1e-9 }?.normalized()
                                        val planeOffset = numericPlaneOffset.toDoubleOrNull() ?: 0.0
                                        val projectedPlane = planeNormal?.let { normal ->
                                            proposed - normal * (normal.dot(proposed) - planeOffset)
                                        }
                                        val planeDistance = projectedPlane?.let { (proposed - it).magnitude() } ?: Double.POSITIVE_INFINITY
                                        val geometric = ConstraintAwareSpatialSnap.snap(proposed, snapTargets, .18)
                                        when {
                                            projectedPlane != null && planeDistance <= .18 -> projectedPlane - basePosition
                                            geometric.target != null -> geometric.point - basePosition
                                            else -> Vec3(
                                                (axisDelta.x * 10.0).roundToInt() / 10.0,
                                                (axisDelta.y * 10.0).roundToInt() / 10.0,
                                                (axisDelta.z * 10.0).roundToInt() / 10.0,
                                            )
                                        }
                                    } else axisDelta
                                    if (vectorGestureIndex != null) {
                                        vm.previewVectorDrag(vectorGestureIndex, snappedDelta)
                                    } else if (editableIndices.size > 1) {
                                        when (gizmoMode) {
                                            ArGizmoMode.Translate -> vm.previewSolidGroupMove(snappedDelta)
                                            ArGizmoMode.Rotate -> vm.previewSolidGroupRotation(
                                                when (gizmoAxis) {
                                                    ArGizmoAxis.X -> Vec3(totalRotation.toDouble(), 0.0, 0.0)
                                                    ArGizmoAxis.Y -> Vec3(0.0, totalRotation.toDouble(), 0.0)
                                                    ArGizmoAxis.Z -> Vec3(0.0, 0.0, totalRotation.toDouble())
                                                    ArGizmoAxis.Uniform -> Vec3(totalRotation.toDouble(), totalRotation.toDouble(), totalRotation.toDouble())
                                                },
                                            )
                                            ArGizmoMode.Scale -> vm.previewSolidGroupScale(totalScale.toDouble())
                                        }
                                    } else {
                                        val index = editableIndices.single()
                                        when (gizmoMode) {
                                            ArGizmoMode.Translate -> vm.previewSolidDrag(index, snappedDelta)
                                            ArGizmoMode.Rotate -> vm.previewSolidRotation(
                                                index,
                                                when (gizmoAxis) {
                                                    ArGizmoAxis.X -> Vec3(totalRotation.toDouble(), 0.0, 0.0)
                                                    ArGizmoAxis.Y -> Vec3(0.0, totalRotation.toDouble(), 0.0)
                                                    ArGizmoAxis.Z -> Vec3(0.0, 0.0, totalRotation.toDouble())
                                                    ArGizmoAxis.Uniform -> Vec3(totalRotation.toDouble(), totalRotation.toDouble(), totalRotation.toDouble())
                                                },
                                            )
                                            ArGizmoMode.Scale -> vm.previewSolidAxisScale(
                                                index,
                                                when (gizmoAxis) {
                                                    ArGizmoAxis.X -> TransformGizmoAxis.X
                                                    ArGizmoAxis.Y -> TransformGizmoAxis.Y
                                                    ArGizmoAxis.Z -> TransformGizmoAxis.Z
                                                    ArGizmoAxis.Uniform -> TransformGizmoAxis.Uniform
                                                },
                                                totalScale.toDouble(),
                                            )
                                        }
                                    }
                                } else {
                                    vm.previewSpatialGesture(totalPan, totalRotation, totalScale)
                                }
                                if (event.changes.none { it.pressed }) break
                                event.changes.forEach { it.consume() }
                            }
                            if (objectGesture) {
                                when {
                                    vectorGestureIndex != null -> vm.endVectorDrag()
                                    editableIndices.size > 1 -> vm.endSolidGroupDrag()
                                    else -> vm.endSolidDrag()
                                }
                            } else {
                                vm.endSpatialGesture()
                            }
                            if (totalPan.getDistance() < 12f && kotlin.math.abs(totalRotation) < 2f && kotlin.math.abs(totalScale - 1f) < .03f) {
                                if (anchorPlacementMode) {
                                    rankedHitAt(down.position)?.let { hit ->
                                        runtime.createAnchor(hit.id, System.currentTimeMillis())
                                            .onSuccess { anchor ->
                                                placement.anchorId.takeIf(String::isNotBlank)?.let(runtime::detachAnchor)
                                                vm.placeSpatialHit(
                                                    hit.toSpatialHit().copy(
                                                        trackableId = anchor.id,
                                                        positionMeters = Vec3(
                                                            anchor.pose.positionMeters.x,
                                                            anchor.pose.positionMeters.y,
                                                            anchor.pose.positionMeters.z,
                                                        ),
                                                    ),
                                                )
                                                displayFirstMode = false
                                                placementMode = false
                                                reticleHit = null
                                            }
                                            .onFailure {
                                                liveError = it.message ?: "Could not create the spatial anchor; scan the selected surface and tap again."
                                            }
                                    } ?: run {
                                        liveError = when (arPlacementMode) {
                                            ArPlacementMode.FloorTable -> "No tracked floor/table plane at the tap point. Move slowly to scan, then tap a highlighted horizontal surface."
                                            ArPlacementMode.Wall -> "No tracked wall plane at the tap point. Scan a textured wall, then tap the detected vertical surface."
                                            ArPlacementMode.Viewer -> "3D Viewer does not create AR anchors. Switch to Floor/Table or Wall for full AR placement."
                                        }
                                    }
                                } else {
                                    val snapshot = arFrame
                                    if (snapshot != null) {
                                        val hits = ArPhase4SpatialBridge.pick(
                                            ArVector2(down.position.x, down.position.y),
                                            viewportSize.width,
                                            viewportSize.height,
                                            snapshot,
                                            canonicalArScene,
                                        ).filter { it.kind == subObjectKind }
                                        overlapHits = hits
                                        val hit = if (
                                            hits.firstOrNull()?.objectId == arSelection.primaryObjectId &&
                                            overlapHits.isNotEmpty()
                                        ) {
                                            ArSelectionEngine.cycle(hits, arSelection.subObject)
                                        } else {
                                            hits.firstOrNull()
                                        }
                                        if (hit != null) {
                                            arSelection = ArSelectionEngine.select(arSelection, hit, arMultiSelect)
                                            hit.objectId.removePrefix("solid-").toIntOrNull()?.let(vm::selectSolid)
                                        } else if (!arMultiSelect) {
                                            arSelection = arSelection.copy(objectIds = emptySet(), primaryObjectId = null, subObject = null)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    .pointerInput(arFrame, canonicalArScene, viewportSize, anchorPlacementMode, objectManipulationMode, trackingAllowsDirectManipulation, subObjectKind) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val hover = event.changes.firstOrNull { it.type == PointerType.Stylus && !it.pressed }
                                stylusHoverHit = if (
                                    hover != null &&
                                    objectManipulationMode &&
                                    trackingAllowsDirectManipulation &&
                                    arFrame != null
                                ) {
                                    ArPhase4SpatialBridge.pick(
                                        ArVector2(hover.position.x, hover.position.y),
                                        viewportSize.width,
                                        viewportSize.height,
                                        arFrame!!,
                                        canonicalArScene,
                                    ).firstOrNull { it.kind == subObjectKind }
                                } else {
                                    null
                                }
                            }
                        }
                    },
            )
            if (anchorPlacementMode) {
                Canvas(Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val color = if (reticleHit != null) Green else Amber
                    drawCircle(color.copy(alpha = .18f), radius = 28f, center = center)
                    drawCircle(color, radius = 18f, center = center, style = Stroke(width = 3f))
                    drawLine(color, center - Offset(30f, 0f), center + Offset(30f, 0f), strokeWidth = 2f)
                    drawLine(color, center - Offset(0f, 30f), center + Offset(0f, 30f), strokeWidth = 2f)
                }
                Text(
                    reticleHit?.let { "${it.type.name} - ${(it.confidence * 100).roundToInt()}% - ±${trim(it.uncertaintyMeters)} m" }
                        ?: when (arPlacementMode) {
                            ArPlacementMode.FloorTable -> "Move your phone slowly to scan a floor or table."
                            ArPlacementMode.Wall -> "Move your phone slowly to scan a wall."
                            ArPlacementMode.Viewer -> "3D Viewer is not live AR."
                        },
                    color = Ink,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.Center).offset(y = 48.dp).clip(RoundedCornerShape(10.dp)).background(SurfaceA.copy(.88f)).padding(8.dp),
                )
            }
        } else {
            SpatialPreviewCanvas(
                modifier = Modifier.fillMaxSize(),
                solids = if (arWorkspaceMode == ArMathWorkspaceMode.Geometry3D) vm.state.solids else emptyList(),
                spatialScene = interactiveScene,
                placement = previewPlacement,
                onGestureStart = vm::beginSpatialGesture,
                onGesture = vm::previewSpatialGesture,
                onGestureEnd = vm::endSpatialGesture,
            )
        }
        if (!arHudHidden) GlassPanel(
            Modifier
                .align(Alignment.TopStart)
                .padding(start = 10.dp, top = 10.dp)
                .widthIn(max = if (arHudExpanded) 300.dp else 292.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    TransparentIcon("AR", Cyan)
                    Column {
                        Text("AR", color = Cyan, fontWeight = FontWeight.Bold, fontSize = if (arHudExpanded) 18.sp else 15.sp)
                        Text(if (liveAR) guidance.title else "Camera ready", color = Muted, fontSize = 10.sp, maxLines = 1)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    GlowButton("Hide", icon = "⌄", iconOnly = true) {
                        arHudHidden = true
                        arHudExpanded = false
                        showAdvancedTools = false
                        showAnalysisTools = false
                        showSpatialDetails = false
                    }
                    GlowButton(if (arHudExpanded) "Collapse" else "Open", icon = if (arHudExpanded) "−" else "⌃", iconOnly = true) {
                        arHudExpanded = !arHudExpanded
                    }
                    GlowButton("Home", icon = "H", iconOnly = true, onClick = vm::returnToMathMenu)
                }
            }
            if (!arHudExpanded) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf(
                        ArMathWorkspaceMode.Geometry2D,
                        ArMathWorkspaceMode.Geometry3D,
                        ArMathWorkspaceMode.Graph2D,
                        ArMathWorkspaceMode.Graph3D,
                    ).forEach { mode ->
                        GlowButton(
                            if (arWorkspaceMode == mode) "• ${mode.shortLabel}" else mode.shortLabel,
                            iconOnly = false,
                            onClick = {
                                arWorkspaceMode = mode
                                resetArDisplay(mode)
                            },
                        )
                    }
                    GlowButton(if (liveAR && !displayFirstMode) "AR On" else "Full AR", icon = if (liveAR && !displayFirstMode) "AR" else "6D", iconOnly = true) {
                        arPlacementMode = ArPlacementMode.FloorTable
                        displayFirstMode = false
                        placementMode = activeAnchor == null
                        startLiveAr(userRequestedInstall = true)
                    }
                    GlowButton("Add ${arWorkspaceMode.shortLabel}", icon = "+", iconOnly = true) {
                        showArAddOptions = true
                        arHudExpanded = false
                    }
                    GlowButton("Edit ${arWorkspaceMode.shortLabel}", icon = "E", iconOnly = true, onClick = ::openCurrentArWorkspaceEditor)
                    GlowButton("Copy ${arWorkspaceMode.shortLabel}", icon = "C", iconOnly = true, onClick = ::duplicateCurrentArItem)
                    DestructiveGlowButton("Delete ${arWorkspaceMode.shortLabel}", icon = "-", iconOnly = true, onClick = ::deleteCurrentArItem)
                    DestructiveGlowButton("Clear all", icon = "X", iconOnly = true, onClick = ::clearCurrentArWorkspace)
                    GlowButton(
                        "Center display",
                        icon = "D",
                        iconOnly = true,
                        onClick = {
                            resetArDisplay()
                        },
                    )
                    GlowButton("Tools", icon = "⋮", iconOnly = true) {
                        arHudExpanded = true
                        showAdvancedTools = true
                    }
                }
                if (liveError.isNotBlank()) {
                    Text(liveError, color = Amber, fontSize = 10.sp, maxLines = 2)
                    GlowButton("Open non-AR ${arWorkspaceMode.shortLabel}", icon = "E", onClick = ::openCurrentArWorkspaceEditor)
                }
            } else {
            Text(
                if (liveAR) "Camera is live. The graph displays first; use Anchor only when you want to pin it to a real surface."
                else "Smart AR opens the camera when supported; simulator remains ready as a fallback.",
                color = Muted,
                fontSize = 11.sp,
            )
            GlowButton(if (showAdvancedTools) "Hide advanced tools" else "Advanced tools", onClick = { showAdvancedTools = !showAdvancedTools })
            if (showAdvancedTools) {
            Text(arWorkspaceMode.description, color = Muted, fontSize = 10.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ArMathWorkspaceMode.entries.forEach { mode ->
                    GlowButton(if (arWorkspaceMode == mode) "• ${mode.shortLabel}" else mode.shortLabel) {
                        arWorkspaceMode = mode
                    }
                }
            }
            ArSmartEnhancementsPanel(
                vm = vm,
                arWorkspaceMode = arWorkspaceMode,
                linkedStatus = linkedArWorkspace.status,
                diagnostics = linkedArWorkspace.diagnostics,
                visualizedObjectCount = linkedArWorkspace.visualizedObjectCount,
                arSelection = arSelection,
                onSelectionChange = { arSelection = it },
                onAdd = { showArAddOptions = true },
                onDelete = ::deleteCurrentArItem,
                arVisualTheme = arVisualTheme,
                onVisualTheme = { arVisualTheme = it },
                arObjectOpacity = arObjectOpacity,
                onObjectOpacity = { arObjectOpacity = it },
                arShowWireframe = arShowWireframe,
                onShowWireframe = { arShowWireframe = it },
                arShowGrid = arShowGrid,
                onShowGrid = { arShowGrid = it },
                arShowAxes = arShowAxes,
                onShowAxes = { arShowAxes = it },
                arSurfaceQuality = arSurfaceQuality,
                onSurfaceQuality = { arSurfaceQuality = it },
                onOverlayScale = { arOverlayScale = it },
                onFitDisplay = {
                    arOverlayPan = Offset.Zero
                    arOverlayScale = if (arWorkspaceMode == ArMathWorkspaceMode.Graph3D) .50f else .56f
                    arOverlayRotationX = if (arWorkspaceMode == ArMathWorkspaceMode.Graph3D) -18f else 0f
                    arOverlayRotationY = 0f
                    arOverlayRotationZ = 0f
                },
                arOverlayRotationX = arOverlayRotationX,
                onOverlayRotationX = { arOverlayRotationX = it },
                arOverlayRotationY = arOverlayRotationY,
                onOverlayRotationY = { arOverlayRotationY = it },
                arOverlayRotationZ = arOverlayRotationZ,
                onOverlayRotationZ = { arOverlayRotationZ = it },
            )
            GlowButton("Open full ${arWorkspaceMode.label} editor", onClick = ::openCurrentArWorkspaceEditor)
            Insight("AR mode", if (liveAR) "Live ARCore camera + shared GPU scene" else "Accessible spatial simulator", Cyan)
            }
            Insight("ARCore", capabilities.message, if (capabilities.availability == ARAvailability.Unsupported) Amber else Green)
            Insight("Display", arPlacementMode.label, if (objectManipulationMode) Green else if (displayFirstMode) Cyan else Amber)
            Insight("Tracking", when {
                displayFirstMode -> "3D Viewer fallback"
                objectManipulationMode -> guidance.title
                else -> guidance.title
            }, if (displayFirstMode || guidance.safeToPlace) Green else Amber)
            Text(
                when {
                    displayFirstMode -> "3D Viewer fallback: orbit, scale, and edit without claiming camera-based AR tracking."
                    objectManipulationMode -> "Graph anchored. Move around it to explore; use transform tools to move, rotate, or scale the object intentionally."
                    anchorPlacementMode -> "Surface detected only after a valid tracked ${arPlacementMode.label.lowercase()} plane hit. Tap to create a persistent AR anchor."
                    else -> guidance.instruction
                },
                color = Muted,
                fontSize = 12.sp,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GlowButton(if (liveAR) "Camera on" else "Open camera", onClick = { startLiveAr(userRequestedInstall = true) })
                GlowButton("Edit current", onClick = ::openCurrentArWorkspaceEditor)
                GlowButton("Copy current", onClick = ::duplicateCurrentArItem)
                DestructiveGlowButton("Clear all", onClick = ::clearCurrentArWorkspace)
                GlowButton(if (arPlacementMode == ArPlacementMode.Viewer) "• Viewer" else "3D Viewer") {
                    arPlacementMode = ArPlacementMode.Viewer
                    displayFirstMode = true
                    placementMode = false
                    reticleHit = null
                    if (!placement.isPlaced) vm.placeSpatialScene()
                }
                GlowButton(if (arPlacementMode == ArPlacementMode.FloorTable && anchorPlacementMode) "• Floor/Table" else "Floor/Table") {
                    arPlacementMode = ArPlacementMode.FloorTable
                    displayFirstMode = false
                    placementMode = true
                    reticleHit = null
                    startLiveAr(userRequestedInstall = true)
                }
                GlowButton(if (arPlacementMode == ArPlacementMode.Wall && anchorPlacementMode) "• Wall" else "Wall") {
                    arPlacementMode = ArPlacementMode.Wall
                    displayFirstMode = false
                    placementMode = true
                    reticleHit = null
                    startLiveAr(userRequestedInstall = true)
                }
                GlowButton("Reset", onClick = {
                    placement.anchorId.takeIf(String::isNotBlank)?.let { runtime?.detachAnchor(it) }
                    vm.resetSpatialScene()
                    resetArDisplay()
                    arSelection = ArSelectionState()
                })
            }
            if (liveAR) GlowButton("Use simulator", onClick = {
                compositorView?.onPause()
                liveAR = false
                runtime?.pause()
            })
            Insight("Scale", placement.visibleScale, Violet)
            if (showAdvancedTools) {
            Insight("Estimate", "±${trim(placement.measurementUncertaintyMeters)} m - educational only", Amber)
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
            }
            reticleHit?.takeIf { anchorPlacementMode }?.let {
                Insight("Placement preview", "${it.type.name} - ${(it.confidence * 100).roundToInt()}% confidence", Green)
                Insight("Uncertainty", "±${trim(it.uncertaintyMeters)} m - ${placement.visibleScale}", Amber)
            }
            if (!anchorPlacementMode) {
                Text("Direct manipulation", color = Ink, fontWeight = FontWeight.Bold)
                if (!trackingAllowsDirectManipulation) {
                    Text("Tracking paused - object selection and gizmos are temporarily frozen; mathematical state and selection are preserved.", color = Amber, fontSize = 11.sp)
                }
                if (showAdvancedTools || arSelection.primaryObjectId != null) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    ArGizmoMode.entries.forEach { mode ->
                        GlowButton(if (gizmoMode == mode) "• ${mode.name}" else mode.name) { gizmoMode = mode }
                    }
                    ArGizmoAxis.entries.forEach { axis ->
                        GlowButton(if (gizmoAxis == axis) "• ${axis.name}" else axis.name) { gizmoAxis = axis }
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    TogglePill("Snap", snapEnabled) { snapEnabled = it }
                    TogglePill("Precision", precisionMode) { precisionMode = it }
                    TogglePill("Multi", arMultiSelect) {
                        arMultiSelect = it
                        if (!it) arSelection.primaryObjectId?.let { id ->
                            arSelection = arSelection.copy(objectIds = setOf(id))
                        }
                    }
                    ArSubObjectKind.entries.forEach { kind ->
                        GlowButton(if (subObjectKind == kind) "• ${kind.name}" else kind.name) { subObjectKind = kind }
                    }
                }
                if (snapEnabled) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            numericPlaneNormal,
                            { numericPlaneNormal = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Plane normal") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            numericPlaneOffset,
                            { numericPlaneOffset = it },
                            modifier = Modifier.width(82.dp),
                            label = { Text("Offset") },
                            singleLine = true,
                        )
                    }
                }
                } else {
                    Text("Tap an object to reveal transform controls.", color = Muted, fontSize = 11.sp)
                }
                arSelection.primaryObjectId?.let { selectedId ->
                    Insight(
                        "Selected",
                        buildString {
                            append(selectedId)
                            arSelection.subObject?.takeIf { it.kind != ArSubObjectKind.Whole }?.let {
                                append(" - ${it.kind.name} ${(it.subObjectIndex ?: 0) + 1}")
                            }
                        },
                        Cyan,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        GlowButton(if (selectedId in arSelection.lockedObjectIds) "Unlock" else "Lock") {
                            arSelection = ArSelectionEngine.toggleLock(arSelection)
                        }
                        GlowButton("Hide") { arSelection = ArSelectionEngine.hideSelected(arSelection) }
                        GlowButton(if (arSelection.isolatedObjectIds == null) "Isolate" else "End isolate") {
                            arSelection = if (arSelection.isolatedObjectIds == null) ArSelectionEngine.isolate(arSelection) else ArSelectionEngine.showAll(arSelection)
                        }
                        GlowButton("Cycle", enabled = overlapHits.isNotEmpty()) {
                            ArSelectionEngine.cycle(overlapHits, arSelection.subObject)?.let {
                                arSelection = ArSelectionEngine.select(arSelection, it, false)
                                it.objectId.removePrefix("solid-").toIntOrNull()?.let(vm::selectSolid)
                            }
                        }
                        GlowButton("Copy", enabled = selectedSolidIndices.isNotEmpty()) {
                            arClipboard = selectedSolidIndices.mapNotNull(vm.state.solids::getOrNull)
                            context.getSystemService(android.content.ClipboardManager::class.java)?.setPrimaryClip(
                                android.content.ClipData.newPlainText(
                                    "AI Explorer AR objects",
                                    arClipboard.joinToString("\n") {
                                        "${it.type.name} @ (${trim(it.position.x)}, ${trim(it.position.y)}, ${trim(it.position.z)})"
                                    },
                                ),
                            )
                        }
                        GlowButton("Paste", enabled = arClipboard.isNotEmpty()) {
                            val start = vm.state.solids.size
                            vm.replaceSolids("Paste AR objects") { solids ->
                                solids + arClipboard.mapIndexed { index, solid ->
                                    solid.copy(position = solid.position + Vec3(.35 + index * .12, .15, .35))
                                }
                            }
                            val ids = (start until vm.state.solids.size).mapTo(linkedSetOf()) { "solid-$it" }
                            arSelection = arSelection.copy(objectIds = ids, primaryObjectId = ids.lastOrNull())
                        }
                        GlowButton("Duplicate", enabled = selectedSolidIndices.size == 1) {
                            selectedSolidIndices.singleOrNull()?.let(vm::selectSolid)
                            vm.duplicateSelectedSolid()
                            arSelection = ArSelectionState(setOf("solid-${vm.selectedSolid}"), "solid-${vm.selectedSolid}")
                        }
                        DestructiveGlowButton("Delete", enabled = selectedSolidIndices.isNotEmpty() || selectedVectorIndex != null) {
                            selectedVectorIndex?.let(vm::deleteVector3D)
                            vm.deleteSelectedSolids(selectedSolidIndices)
                            arSelection = ArSelectionState()
                            if (vm.state.solids.isEmpty() && vm.state.vectors3D.isEmpty()) {
                                placement.anchorId.takeIf(String::isNotBlank)?.let { runtime?.detachAnchor(it) }
                                vm.resetSpatialScene()
                                displayFirstMode = true
                                placementMode = false
                            }
                        }
                        GlowButton("Front", enabled = selectedSolidIndices.size == 1) {
                            val index = selectedSolidIndices.singleOrNull() ?: return@GlowButton
                            vm.replaceSolids("Bring AR object to front") { solids ->
                                solids.getOrNull(index)?.let { selected -> solids.filterIndexed { i, _ -> i != index } + selected } ?: solids
                            }
                            val id = "solid-${vm.state.solids.lastIndex}"
                            arSelection = ArSelectionState(setOf(id), id)
                            arGroups = emptyList()
                        }
                        GlowButton("Back", enabled = selectedSolidIndices.size == 1) {
                            val index = selectedSolidIndices.singleOrNull() ?: return@GlowButton
                            vm.replaceSolids("Send AR object to back") { solids ->
                                solids.getOrNull(index)?.let { selected -> listOf(selected) + solids.filterIndexed { i, _ -> i != index } } ?: solids
                            }
                            arSelection = ArSelectionState(setOf("solid-0"), "solid-0")
                            arGroups = emptyList()
                        }
                    }
                    if (selectedSolidIndices.size > 1) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            GlowButton("Group") {
                                arGroups = arGroups + listOf(arSelection.objectIds)
                            }
                            SpatialAlignment.entries.forEach { alignment ->
                                GlowButton("Align ${alignment.name}") {
                                    vm.replaceSolids("Align AR group on ${alignment.name}") {
                                        AdvancedSpatialInteractionEngine.align(it, selectedSolidIndices, alignment)
                                    }
                                }
                            }
                            GlowButton("Distribute X", enabled = selectedSolidIndices.size >= 3) {
                                vm.replaceSolids("Distribute AR group") {
                                    AdvancedSpatialInteractionEngine.distribute(it, selectedSolidIndices, SpatialAlignment.X)
                                }
                            }
                        }
                    }
                    Text(if (selectedVectorIndex != null) "Numeric vector editor" else "Numeric transform", color = Ink, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        numericPosition,
                        { numericPosition = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(if (selectedVectorIndex != null) "Vector start x, y, z" else "Position x, y, z") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        numericRotation,
                        { numericRotation = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(if (selectedVectorIndex != null) "Vector end x, y, z" else "Rotation x°, y°, z°") },
                        singleLine = true,
                    )
                    if (selectedVectorIndex == null) OutlinedTextField(
                        numericScale,
                        { numericScale = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Scale factor") },
                        singleLine = true,
                    )
                    GlowButton("Apply exact transform", enabled = selectedSolidIndices.size == 1 || selectedVectorIndex != null) {
                        val positionValues = parseSpatialTriple(numericPosition)
                        val rotationValues = parseSpatialTriple(numericRotation)
                        val factor = numericScale.toDoubleOrNull()?.coerceIn(.05, 20.0)
                        selectedVectorIndex?.let { index ->
                            vm.transformVector3D(index) { vector ->
                                vector.copy(start = positionValues ?: vector.start, end = rotationValues ?: vector.end)
                            }
                        }
                        selectedSolidIndices.singleOrNull()?.let { index ->
                            vm.transformSolid(index) { solid ->
                                solid.copy(
                                    position = positionValues ?: solid.position,
                                    rotation = rotationValues ?: solid.rotation,
                                    width = if (factor != null) solid.width * factor else solid.width,
                                    height = if (factor != null) solid.height * factor else solid.height,
                                    depth = if (factor != null) solid.depth * factor else solid.depth,
                                    radius = if (factor != null) solid.radius * factor else solid.radius,
                                    topRadius = if (factor != null) solid.topRadius * factor else solid.topRadius,
                                )
                            }
                            numericScale = "1"
                        }
                    }
                }
                stylusHoverHit?.let {
                    Insight("Stylus hover", "${it.objectId} - ${it.kind.name.lowercase()} preview", Green)
                }
                if (arSelection.hiddenObjectIds.isNotEmpty() || arSelection.isolatedObjectIds != null) {
                    GlowButton("Show all objects") { arSelection = ArSelectionEngine.showAll(arSelection) }
                }
                if (arGroups.isNotEmpty()) Insight("Groups", "${arGroups.size} AR group(s) - shared transforms enabled", Violet)
                GlowButton(if (showAnalysisTools) "Hide analysis" else "Analysis & measurements", onClick = { showAnalysisTools = !showAnalysisTools })
                if (showAnalysisTools) {
                Text("Surface analysis & measurement", color = Ink, fontWeight = FontWeight.Bold)
                TogglePill("Live analysis", arAnalysisEnabled) {
                    arAnalysisEnabled = it
                    if (!it) arGradientPlaying = false
                }
                if (arAnalysisEnabled) {
                    AxisSlider("Trace x", arTraceX, -5f..5f) {
                        arTraceX = it
                        arGradientStep = 0
                    }
                    AxisSlider("Trace y", arTraceY, -5f..5f) {
                        arTraceY = it
                        arGradientStep = 0
                    }
                    AxisSlider("Contour z", arContourLevel, -8f..8f) { arContourLevel = it }
                    AxisSlider(
                        "Section offset",
                        (numericPlaneOffset.toFloatOrNull() ?: 0f).coerceIn(-5f, 5f),
                        -5f..5f,
                    ) { numericPlaneOffset = trim(it.toDouble()) }
                    Text(phase5Analysis.surfaceStatus, color = Muted, fontSize = 11.sp)
                    Text(phase5Analysis.sectionStatus, color = Muted, fontSize = 11.sp)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        GlowButton(if (arGradientAscending) "• Ascent" else "Ascent") {
                            arGradientAscending = true
                            arGradientStep = 0
                        }
                        GlowButton(if (!arGradientAscending) "• Descent" else "Descent") {
                            arGradientAscending = false
                            arGradientStep = 0
                        }
                        GlowButton(if (arGradientPlaying) "Pause path" else "Play path", enabled = phase5Analysis.gradientSteps > 0) {
                            arGradientPlaying = !arGradientPlaying
                        }
                        GlowButton("Previous", enabled = phase5Analysis.gradientSteps > 0) {
                            arGradientPlaying = false
                            arGradientStep = (arGradientStep - 1).coerceAtLeast(0)
                        }
                        GlowButton("Next", enabled = phase5Analysis.gradientSteps > 0) {
                            arGradientPlaying = false
                            arGradientStep = (arGradientStep + 1).coerceAtMost((phase5Analysis.gradientSteps - 1).coerceAtLeast(0))
                        }
                    }
                    if (phase5Analysis.gradientSteps > 0) {
                        AxisSlider(
                            "Path handle",
                            arGradientStep.coerceAtMost(phase5Analysis.gradientSteps - 1).toFloat(),
                            0f..(phase5Analysis.gradientSteps - 1).coerceAtLeast(1).toFloat(),
                        ) {
                            arGradientPlaying = false
                            arGradientStep = it.roundToInt()
                        }
                    }
                }
                phase5Analysis.measurements.forEach { measurement ->
                    Insight(
                        measurement.kind.name,
                        measurement.display,
                        if (measurement.truth == com.indianservers.aiexplorer.arengine.analysis.ArMeasurementTruth.ExactMathematical) Green else Amber,
                    )
                }
                if (arSelection.objectIds.isEmpty()) {
                    Text("Select one solid for exact area/volume, two for exact and physical distance, or three for angle.", color = Muted, fontSize = 11.sp)
                }
                }
            }
            GlowButton(if (showSpatialDetails) "Hide spatial details" else "Lessons & renderer", onClick = { showSpatialDetails = !showSpatialDetails })
            if (showSpatialDetails) {
                Text("Spatial lesson", color = Ink, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SpatialLessonCatalog.lessons.forEachIndexed { index, item ->
                        GlowButton(if (index == selectedLesson) "• ${item.title.take(12)}" else item.title.take(12), onClick = { selectedLesson = index })
                    }
                }
                Text(lesson.learningGoal, color = Muted, fontSize = 11.sp)
                Insight("Shared renderer", "${sharedScene.primitives.size} objects - ${gpuPlan.vertices.size / 10} GPU vertices", Cyan)
                Insight("Lighting", if (frameState?.lighting?.valid == true) "${trim(frameState?.lighting?.pixelIntensity?.toDouble() ?: 1.0)}× environment" else "Simulator neutral light", Green)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ThermalLevel.entries.forEach { level -> GlowButton(level.name.take(4), onClick = { thermalLevel = level }) }
                }
                Insight("Performance", "${policy.quality.name} - ${policy.targetFps} fps - mesh ${policy.surfaceDensity}", if (thermalLevel >= ThermalLevel.Severe) Amber else Green)
                Insight("Privacy & safety", "${ARPrivacySafetyChecklist.items.size} mandatory checks - camera frames stay local", Violet)
                if (liveError.isNotBlank()) Text(liveError, color = Amber, fontSize = 11.sp)
            }
            Text("Placement and measurements are educational estimates, not certified physical measurements.", color = Amber, fontSize = 11.sp)
            }
        }
        if (showArAddOptions) {
            GlassPanel(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 10.dp, end = 10.dp, bottom = 78.dp)
                    .widthIn(max = 360.dp)
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Add ${arWorkspaceMode.label}", color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Options follow the selected AR workspace", color = Muted, fontSize = 10.sp)
                    }
                    GlowButton("Collapse", icon = "collapse", iconOnly = true) { showArAddOptions = false }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    when (arWorkspaceMode) {
                        ArMathWorkspaceMode.Geometry2D -> {
                            ShapeExplorer2DShapes.take(12).forEach { preset ->
                                GlowButton(preset.label.take(14), icon = "+") {
                                    vm.addArShape2D(preset.id)
                                    arWorkspaceMode = ArMathWorkspaceMode.Geometry2D
                                    showArAddOptions = false
                                    resetArDisplay(ArMathWorkspaceMode.Geometry2D)
                                }
                            }
                            GlowButton("More 2D...", icon = "+") {
                                showArAddOptions = false
                                vm.open(MathModule.Geometry2D)
                            }
                        }
                        ArMathWorkspaceMode.Geometry3D -> {
                            SolidType.entries.forEach { type ->
                                GlowButton(type.name.take(10), icon = "+") {
                                    vm.addSolid(type)
                                    arWorkspaceMode = ArMathWorkspaceMode.Geometry3D
                                    showArAddOptions = false
                                    resetArDisplay(ArMathWorkspaceMode.Geometry3D)
                                }
                            }
                            GlowButton("+ Vector", icon = "+") {
                                vm.addVector3D()
                                arWorkspaceMode = ArMathWorkspaceMode.Geometry3D
                                showArAddOptions = false
                                resetArDisplay(ArMathWorkspaceMode.Geometry3D)
                            }
                        }
                        ArMathWorkspaceMode.Graph2D -> {
                            IntentAwareMathField(
                                value = arGraphExpressionDraft,
                                onValueChange = { arGraphExpressionDraft = it },
                                label = "New graph expression",
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = "y = x^2 or x^2 + y^2 = 9",
                                imeAction = ImeAction.Done,
                                onDone = ::plotArGraphExpression,
                            )
                            vm.state.functions.lastOrNull()?.let { function ->
                                Text("Current: ${function.name} = ${function.expression}", color = Muted, fontSize = 11.sp, maxLines = 2)
                            }
                            GlowButton(if (vm.state.functions.isEmpty()) "Add expression" else "Update current", icon = "+", enabled = arGraphExpressionDraft.isNotBlank(), onClick = ::plotArGraphExpression)
                            listOf(
                                "Line" to "x",
                                "Parabola" to "x^2",
                                "Sine" to "sin(x)",
                                "Cosine" to "cos(x)",
                                "Circle" to "x^2 + y^2 = 9",
                                "Absolute" to "abs(x)",
                            ).forEach { (label, expression) ->
                                GlowButton(label, icon = "+") {
                                    vm.addFunction(expression)
                                    arWorkspaceMode = ArMathWorkspaceMode.Graph2D
                                    showArAddOptions = false
                                    resetArDisplay(ArMathWorkspaceMode.Graph2D)
                                }
                            }
                            GlowButton("Full graph editor", icon = "+") {
                                showArAddOptions = false
                                vm.open(MathModule.Graph2D)
                            }
                        }
                        ArMathWorkspaceMode.Graph3D -> {
                            IntentAwareMathField(
                                value = arSurfaceExpressionDraft,
                                onValueChange = { arSurfaceExpressionDraft = it },
                                label = "New 3D surface",
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = "z = x^2 + y^2",
                                imeAction = ImeAction.Done,
                                onDone = ::plotArSurfaceExpression,
                            )
                            Text("Current: ${vm.state.surfaceExpression.ifBlank { "none" }}", color = Muted, fontSize = 11.sp, maxLines = 2)
                            GlowButton(if (vm.state.surfaceExpression.isBlank() || vm.state.surfaceExpression == "0") "Plot surface" else "Update surface", icon = "+", enabled = arSurfaceExpressionDraft.isNotBlank(), onClick = ::plotArSurfaceExpression)
                            listOf(
                                "Paraboloid" to "x^2 + y^2",
                                "Saddle" to "x^2 - y^2",
                                "Wave" to "sin(x) + cos(y)",
                                "Plane" to "x + y",
                                "Cone" to "sqrt(x^2 + y^2)",
                                "Ripple" to "sin(sqrt(x^2 + y^2))",
                            ).forEach { (label, expression) ->
                                GlowButton(label, icon = "+") {
                                    vm.setSurfaceExpression(expression)
                                    arWorkspaceMode = ArMathWorkspaceMode.Graph3D
                                    showArAddOptions = false
                                    arSurfaceExpressionDraft = expression
                                    resetArDisplay(ArMathWorkspaceMode.Graph3D)
                                }
                            }
                            GlowButton("Full 3D editor", icon = "+") {
                                showArAddOptions = false
                                vm.open(MathModule.Graph3D)
                            }
                        }
                        ArMathWorkspaceMode.CAS -> {
                            GlowButton("Open notebook", icon = "+") {
                                showArAddOptions = false
                                vm.openMathNotebook()
                            }
                        }
                    }
                }
            DestructiveGlowButton("- Delete current ${arWorkspaceMode.shortLabel}", icon = "-", onClick = {
                    deleteCurrentArItem()
                    showArAddOptions = false
                })
            }
        }
        if (arHudHidden) {
            Row(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 10.dp, top = 10.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(SurfaceA.copy(.82f))
                    .border(1.dp, Cyan.copy(.52f), RoundedCornerShape(18.dp))
                    .clickable { arHudHidden = false }
                    .semantics { contentDescription = "Show AR controls" }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                TransparentIcon("AR", Cyan)
                Text("Show", color = Ink, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private fun parseSpatialTriple(value: String): Vec3? {
    val parts = value.split(',', ';', ' ').map(String::trim).filter(String::isNotEmpty)
    if (parts.size != 3) return null
    val values = parts.map { it.toDoubleOrNull() ?: return null }
    return Vec3(values[0], values[1], values[2])
}

private fun com.indianservers.aiexplorer.core.SpatialSurfaceLayer.surfaceDefinition(): com.indianservers.aiexplorer.spatial.SurfaceDefinition3D {
    val bounds = com.indianservers.aiexplorer.spatial.SurfaceDomain(
        domain.uMin..domain.uMax, domain.vMin..domain.vMax, domain.uMin..domain.uMax,
    )
    return when (kind) {
        com.indianservers.aiexplorer.core.SpatialSurfaceKind.Explicit -> com.indianservers.aiexplorer.spatial.SurfaceDefinition3D.Explicit(id, expression, bounds)
        com.indianservers.aiexplorer.core.SpatialSurfaceKind.Implicit -> com.indianservers.aiexplorer.spatial.SurfaceDefinition3D.Implicit(id, expression, bounds)
        com.indianservers.aiexplorer.core.SpatialSurfaceKind.Parametric -> com.indianservers.aiexplorer.spatial.SurfaceDefinition3D.Parametric(id, expression, expressionY, expressionZ, domain = bounds)
    }
}

private fun com.indianservers.aiexplorer.core.SpatialSurfaceLayer.productionMesh(density: Int): com.indianservers.aiexplorer.core.SurfaceMesh =
    when (kind) {
        com.indianservers.aiexplorer.core.SpatialSurfaceKind.Explicit -> Graph3D().mesh(expression, density = density)
        else -> com.indianservers.aiexplorer.spatial.TypedSurfaceMesher().mesh(surfaceDefinition(), density.coerceIn(4, 64)).geometry.let { geometry ->
            com.indianservers.aiexplorer.core.SurfaceMesh(
                vertices = geometry.vertices,
                rows = 1,
                columns = geometry.vertices.size.coerceAtLeast(1),
                triangleIndices = geometry.triangles,
            )
        }
    }

private fun com.indianservers.aiexplorer.core.SpatialSurfaceLayer.displayEquation(): String = when (kind) {
    com.indianservers.aiexplorer.core.SpatialSurfaceKind.Explicit -> if ('=' in expression) expression else "z=$expression"
    com.indianservers.aiexplorer.core.SpatialSurfaceKind.Implicit -> expression
    com.indianservers.aiexplorer.core.SpatialSurfaceKind.Parametric -> "x=$expression; y=$expressionY; z=$expressionZ"
}

@Composable
private fun Graph3DScreen(vm: ExplorerViewModel) {
    val context = LocalContext.current
    val adaptiveProfile = LocalAdaptiveDeviceProfile.current
    val workspaceTop = if (adaptiveProfile.isTelevision) {
        adaptiveProfile.workspacePolicy.topChromeClearance
    } else {
        76.dp
    }
    val graph3D = remember { Graph3D() }
    val surfaceCalculus = remember { SurfaceCalculus() }
    val sharedSpatialMath = remember { com.indianservers.aiexplorer.workspace.SharedSpatialMathEngine() }
    val savedView = vm.state.graph3DView
    var density by remember { mutableFloatStateOf(savedView.density) }
    var rotation by remember { mutableFloatStateOf(savedView.rotation) }
    var roll by remember { mutableFloatStateOf(savedView.roll) }
    var zoom by remember { mutableFloatStateOf(savedView.zoom) }
    var cameraPan by remember { mutableStateOf(Offset(savedView.panX, savedView.panY)) }
    var tilt by remember { mutableFloatStateOf(savedView.tilt) }
    var sliceZ by remember { mutableFloatStateOf(savedView.sliceZ) }
    var traceX by remember { mutableFloatStateOf(savedView.traceX) }
    var traceY by remember { mutableFloatStateOf(savedView.traceY) }
    var showContours by remember { mutableStateOf(savedView.showContours) }
    var showSlice by remember { mutableStateOf(savedView.showSlice) }
    var showGradient by remember { mutableStateOf(savedView.showGradient) }
    var showBox by remember { mutableStateOf(savedView.showBox) }
    var showOrientationCube by remember(adaptiveProfile.isTelevision) { mutableStateOf(savedView.showOrientationCube && !adaptiveProfile.isTelevision) }
    var activeTool by remember { mutableStateOf(runCatching { SurfaceTool.valueOf(savedView.activeTool) }.getOrDefault(SurfaceTool.Surface)) }
    var graphSceneAppearance by remember { mutableStateOf(WorkspaceAppearance()) }
    var graphAxisStyle by remember { mutableStateOf(WorkspaceVisualStyles.Spectral.axes) }
    val surfaceLayers = vm.state.surfaceLayers
    var selectedSurfaceLayerIndex by remember { mutableIntStateOf(-1) }
    var selectedSurfaceLayerIndices by remember { mutableStateOf(emptySet<Int>()) }
    var gradientPlayback by remember { mutableStateOf(com.indianservers.aiexplorer.core.GradientPlayback3D(emptyList())) }
    var viewPreset by remember { mutableStateOf(runCatching { SurfaceViewPreset.valueOf(savedView.viewPreset) }.getOrDefault(SurfaceViewPreset.Isometric)) }
    var surfaceDraft by rememberSaveable { mutableStateOf(vm.state.surfaceExpression) }
    var surfaceInputMessage by remember { mutableStateOf<String?>(null) }
    var addingSurfaceEquation by remember { mutableStateOf(false) }
    var equationPanelOpen by remember { mutableStateOf(false) }
    var examplesOpen by remember { mutableStateOf(false) }
    var propertiesOpen by remember { mutableStateOf(false) }
    var insightsOpen by remember { mutableStateOf(false) }
    var controlsOpen by remember { mutableStateOf(false) }
    var panelOffset by remember { mutableStateOf(Offset.Zero) }
    var surfaceLayerQuery by rememberSaveable { mutableStateOf("") }
    var clearEpochSeen by remember { mutableIntStateOf(vm.workspaceClearEpoch) }

    fun persistView() = vm.updateGraph3DView {
        it.copy(
            density = density, rotation = rotation, tilt = tilt, roll = roll, zoom = zoom,
            panX = cameraPan.x, panY = cameraPan.y, sliceZ = sliceZ, traceX = traceX, traceY = traceY,
            showContours = showContours, showSlice = showSlice, showGradient = showGradient, showBox = showBox,
            showOrientationCube = showOrientationCube, activeTool = activeTool.name, viewPreset = viewPreset.name,
        )
    }

    BackHandler(enabled = equationPanelOpen || propertiesOpen || insightsOpen || controlsOpen || examplesOpen) {
        when {
            examplesOpen -> examplesOpen = false
            equationPanelOpen -> {
                equationPanelOpen = false
                examplesOpen = false
                addingSurfaceEquation = false
            }
            propertiesOpen -> propertiesOpen = false
            insightsOpen -> insightsOpen = false
            controlsOpen -> controlsOpen = false
        }
    }

    LaunchedEffect(vm.workspaceClearEpoch) {
        if (vm.workspaceClearEpoch != clearEpochSeen) {
            surfaceDraft = vm.state.surfaceExpression
            selectedSurfaceLayerIndex = -1
            selectedSurfaceLayerIndices = emptySet()
            addingSurfaceEquation = false
            gradientPlayback = com.indianservers.aiexplorer.core.GradientPlayback3D(emptyList())
            showBox = false
            showSlice = false
            showGradient = false
            showContours = false
            clearEpochSeen = vm.workspaceClearEpoch
        }
    }

    fun applyView(preset: SurfaceViewPreset) {
        viewPreset = preset
        when (preset) {
            SurfaceViewPreset.Isometric -> { tilt = 55f; rotation = 35f; roll = 0f }
            SurfaceViewPreset.X, SurfaceViewPreset.YZ -> { tilt = 0f; rotation = 90f; roll = 0f }
            SurfaceViewPreset.Y, SurfaceViewPreset.XZ -> { tilt = 90f; rotation = 0f; roll = 0f }
            SurfaceViewPreset.Z, SurfaceViewPreset.XY -> { tilt = 0f; rotation = 0f; roll = 0f }
        }
        cameraPan = Offset.Zero
        persistView()
    }

    val primarySurfaceExpression = surfaceLayers.firstOrNull()?.expression ?: vm.state.surfaceExpression
    val resolvedPrimaryExpression = remember(primarySurfaceExpression) {
        InteractiveParameterEngine.resolve(primarySurfaceExpression, emptyMap(), independentVariables = setOf("x", "y", "z"))
    }

    val primaryLayer = surfaceLayers.firstOrNull()
    val mesh = remember(primaryLayer, resolvedPrimaryExpression, density) {
        runCatching { primaryLayer?.copy(expression = resolvedPrimaryExpression)?.productionMesh(density.toInt().coerceIn(8, 56)) }
            .getOrNull()
            ?.takeIf { candidate ->
                (candidate.triangleIndices.isNotEmpty() || candidate.vertices.size == candidate.rows * candidate.columns) &&
                    candidate.vertices.all { point -> point.x.isFinite() && point.y.isFinite() && point.z.isFinite() }
            }
    }
    val primaryMesh = if (primaryLayer == null || primaryLayer.visible) mesh else null
    val additionalSurfaceMeshEntries = remember(surfaceLayers, density) {
        surfaceLayers.drop(1).mapIndexedNotNull { index, layer ->
            if (!layer.visible) return@mapIndexedNotNull null
            val actualIndex = index + 1
            val qualityDensity = when (layer.quality) {
                com.indianservers.aiexplorer.core.SpatialQuality.Battery -> 12
                com.indianservers.aiexplorer.core.SpatialQuality.Balanced -> 24
                com.indianservers.aiexplorer.core.SpatialQuality.High -> 36
                com.indianservers.aiexplorer.core.SpatialQuality.Ultra -> 52
            }
            runCatching { layer.productionMesh(qualityDensity) }.getOrNull()
                ?.takeIf { candidate -> candidate.vertices.all { point -> point.x.isFinite() && point.y.isFinite() && point.z.isFinite() } }
                ?.let { surfaceMesh ->
                    Triple(
                        actualIndex,
                        surfaceMesh,
                        StyledSurfaceMesh(
                            mesh = surfaceMesh,
                            appearance = layer.workspaceAppearance().copy(colorIndex = layer.colorIndex + actualIndex),
                            // Selection is indicated by the HUD/properties row. Fading other surfaces
                            // made the first graph look deleted as soon as a second graph was added.
                            opacity = layer.opacity.toFloat(),
                            renderMode = layer.renderMode,
                        ),
                    )
                }
        }
    }
    val additionalSurfaceMeshes = additionalSurfaceMeshEntries.map { it.third }
    val selectableSurfaceMeshes = buildList {
        primaryMesh?.let { add(0 to it) }
        additionalSurfaceMeshEntries.forEach { (index, surfaceMesh, _) -> add(index to surfaceMesh) }
    }
    val insight = remember(primaryLayer, resolvedPrimaryExpression) {
        if (primaryLayer?.kind == com.indianservers.aiexplorer.core.SpatialSurfaceKind.Explicit) graph3D.insight(resolvedPrimaryExpression)
        else com.indianservers.aiexplorer.core.SurfaceInsight(primaryLayer?.kind?.name ?: "No surface", null, "Sampled domain", "Inspect numerically")
    }
    val surfaceParameters = remember(surfaceLayers) {
        InteractiveParameterEngine.discover(surfaceLayers.flatMap { listOf(it.expression, it.expressionY, it.expressionZ) }, emptyMap(), independentVariables = setOf("x", "y", "z", "u", "v"))
    }
    val sharedSurfaceDefinition = remember(primaryLayer, resolvedPrimaryExpression) {
        primaryLayer?.copy(expression = resolvedPrimaryExpression)?.surfaceDefinition()
    }
    val sharedDifferential = remember(sharedSurfaceDefinition, traceX, traceY) {
        sharedSurfaceDefinition?.let { runCatching { sharedSpatialMath.differential(it, traceX.toDouble(), traceY.toDouble()) }.getOrNull() }
    }
    val sharedSurfacePlan = remember(primaryMesh) {
        SharedGpuSceneCompiler.compile(SharedSpatialSceneBuilder.build("graph-3d-workspace", emptyList(), surface = primaryMesh))
    }

    LaunchedEffect(gradientPlayback.playing) {
        while (gradientPlayback.playing) {
            delay(90)
            gradientPlayback = gradientPlayback.tick()
        }
    }

    fun plotSurfaceDraft() {
        val interpretation = SurfaceInputInterpreter.interpret(surfaceDraft).getOrElse {
            surfaceInputMessage = it.message ?: "Enter an explicit, implicit, or parametric surface."
            return
        }
        val next = interpretation.canonicalEquation
        val candidate = com.indianservers.aiexplorer.core.SpatialSurfaceLayer(
            id = surfaceLayers.getOrNull(selectedSurfaceLayerIndex)?.id ?: "surface-${System.nanoTime()}",
            expression = interpretation.expression,
            kind = interpretation.kind,
            expressionY = interpretation.expressionY,
            expressionZ = interpretation.expressionZ,
        )
        val previewResult = runCatching { candidate.productionMesh(8) }
        val preview = previewResult.getOrNull()
        if (preview == null) {
            surfaceInputMessage = "Could not plot this surface: ${previewResult.exceptionOrNull()?.message ?: "invalid expression"}"
            return
        }
        if (preview.vertices.none { it.x.isFinite() && it.y.isFinite() && it.z.isFinite() }) {
            surfaceInputMessage = "Could not plot this surface because it has no finite points in the current domain."
            return
        }
        if (addingSurfaceEquation || selectedSurfaceLayerIndex !in surfaceLayers.indices) {
            val newIndex = surfaceLayers.size
            val layer = com.indianservers.aiexplorer.core.SpatialSurfaceLayer(
                "surface-${System.nanoTime()}",
                interpretation.expression,
                kind = interpretation.kind,
                expressionY = interpretation.expressionY,
                expressionZ = interpretation.expressionZ,
            ).withWorkspaceAppearance(graphSceneAppearance.copy(colorIndex = newIndex))
            val result = vm.replaceSurfaceLayers(surfaceLayers + layer, "Add 3D surface")
            if (result.isFailure) {
                surfaceInputMessage = "Unable to add graph: ${result.exceptionOrNull()?.message ?: "workspace validation failed"}"
                return
            }
            selectedSurfaceLayerIndex = newIndex
            selectedSurfaceLayerIndices = setOf(newIndex)
            surfaceInputMessage = "Added $next"
        } else {
            val result = vm.replaceSurfaceLayers(
                surfaceLayers.mapIndexed { index, layer -> if (index == selectedSurfaceLayerIndex) layer.copy(expression = interpretation.expression, kind = interpretation.kind, expressionY = interpretation.expressionY, expressionZ = interpretation.expressionZ) else layer },
                "Edit 3D surface",
            )
            if (result.isFailure) {
                surfaceInputMessage = "Unable to update graph: ${result.exceptionOrNull()?.message ?: "workspace validation failed"}"
                return
            }
            surfaceInputMessage = "Updated $next"
        }
        surfaceDraft = next
        addingSurfaceEquation = false
        equationPanelOpen = false
        examplesOpen = false
    }

    fun selectSurfaceLayer(index: Int, additive: Boolean = false) {
        if (index !in surfaceLayers.indices) return
        val nextSelection = if (additive) {
            if (index in selectedSurfaceLayerIndices) selectedSurfaceLayerIndices - index else selectedSurfaceLayerIndices + index
        } else {
            setOf(index)
        }
        selectedSurfaceLayerIndices = nextSelection
        selectedSurfaceLayerIndex = if (index in nextSelection) index else nextSelection.lastOrNull() ?: -1
        surfaceDraft = surfaceLayers[index].displayEquation()
        addingSurfaceEquation = false
    }

    fun deleteSurfaceLayers(indices: Set<Int>) {
        val targets = indices.filterTo(linkedSetOf()) { it in surfaceLayers.indices }
        if (targets.isEmpty()) return
        vm.replaceSurfaceLayers(surfaceLayers.filterIndexed { index, _ -> index !in targets }, "Delete 3D surface layers")
        selectedSurfaceLayerIndices = emptySet()
        selectedSurfaceLayerIndex = -1
        addingSurfaceEquation = false
    }

    fun clearGraph3DWorkspace() {
        selectedSurfaceLayerIndices = emptySet()
        selectedSurfaceLayerIndex = -1
        surfaceDraft = ""
        addingSurfaceEquation = false
        gradientPlayback = com.indianservers.aiexplorer.core.GradientPlayback3D(emptyList())
        showBox = false
        showSlice = false
        showGradient = false
        showContours = false
        surfaceInputMessage = "3D graph cleared"
        persistView()
        vm.clearCurrentWorkspace()
    }

    fun updateSelectedSurfaceRenderMode(mode: com.indianservers.aiexplorer.core.SpatialSurfaceRenderMode) {
        val targets = selectedSurfaceLayerIndices.ifEmpty {
            selectedSurfaceLayerIndex.takeIf { it in surfaceLayers.indices }?.let(::setOf).orEmpty()
        }
        vm.replaceSurfaceLayers(
            surfaceLayers.mapIndexed { index, layer -> if (index in targets) layer.copy(renderMode = mode) else layer },
            "Change 3D surface render mode",
        )
    }

    Box(Modifier.fillMaxSize()) {
        SurfaceCanvas3D(
            modifier = Modifier
                .fillMaxSize()
                .background((surfaceLayers.firstOrNull()?.workspaceAppearance() ?: graphSceneAppearance).palette.background)
                .appWorkspaceTreatment(0.dp, graphAxisStyle.z, graphAxisStyle.y),
            expression = resolvedPrimaryExpression,
            mesh = primaryMesh,
            appearance = surfaceLayers.firstOrNull()?.workspaceAppearance() ?: graphSceneAppearance,
            additionalMeshes = additionalSurfaceMeshes,
            axisStyle = graphAxisStyle,
            surfaceOpacity = (surfaceLayers.firstOrNull()?.opacity ?: 1.0).toFloat(),
            selectableMeshes = selectableSurfaceMeshes,
            gradientPath = gradientPlayback.path,
            gradientPathIndex = gradientPlayback.index,
            rotation = rotation,
            tilt = tilt,
            roll = roll,
            zoom = zoom,
            cameraPan = cameraPan,
            sliceZ = sliceZ.toDouble(),
            trace = Vec2(traceX.toDouble(), traceY.toDouble()),
            renderMode = surfaceLayers.firstOrNull()?.renderMode ?: com.indianservers.aiexplorer.core.SpatialSurfaceRenderMode.SurfaceMesh,
            showContours = showContours,
            showSlice = showSlice,
            showGradient = showGradient,
            showBox = showBox,
            activeTool = activeTool,
            onRotate = { delta -> rotation = (rotation + delta).coerceIn(-180f, 180f); persistView() },
            onTilt = { delta -> tilt = (tilt + delta).coerceIn(-89f, 89f); persistView() },
            onRoll = { delta -> roll = (roll + delta).wrapDegrees(); persistView() },
            onPan = { delta -> cameraPan += delta; persistView() },
            onZoom = { factor -> zoom = (zoom * factor).coerceIn(.35f, 4f); persistView() },
            onResetCamera = {
                rotation = 35f
                tilt = 55f
                roll = 0f
                zoom = 1f
                cameraPan = Offset.Zero
                persistView()
            },
            onTrace = { point ->
                traceX = point.x.toFloat().coerceIn(-3f, 3f)
                traceY = point.y.toFloat().coerceIn(-3f, 3f)
            },
            onSelectSurface = { index -> selectSurfaceLayer(index) },
        )

        WorkspaceThemeButton(
            appearance = graphSceneAppearance,
            onSelect = { palette ->
                graphSceneAppearance = graphSceneAppearance.switchPalette(palette)
                graphAxisStyle = palette.axes
                vm.replaceSurfaceLayers(
                    surfaceLayers.map { layer -> layer.withWorkspaceAppearance(layer.workspaceAppearance().switchPalette(palette)) },
                    "Change 3D graph palette",
                )
            },
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 10.dp),
        )

        if (showOrientationCube) {
            OrientationCube(
                modifier = Modifier.align(Alignment.TopEnd).padding(top = workspaceTop + 56.dp, end = 12.dp),
                onPreset = { preset ->
                    applyView(
                        when (preset) {
                            "Top" -> SurfaceViewPreset.Z
                            "Side" -> SurfaceViewPreset.X
                            else -> SurfaceViewPreset.Y
                        },
                    )
                },
            )
        }

        Row(
            Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AddShapeTarget(
                onAdd = {
                    addingSurfaceEquation = true
                    selectedSurfaceLayerIndex = -1
                    selectedSurfaceLayerIndices = emptySet()
                    surfaceDraft = ""
                    surfaceInputMessage = "Enter an equation and press Enter to plot."
                    equationPanelOpen = true
                    propertiesOpen = false
                    insightsOpen = false
                    controlsOpen = false
                },
                label = "+ Equation",
                contentDescription = "Add a 3D graph equation to the workspace",
            )
            GlowButton("Layers", icon = "settings", enabled = surfaceLayers.isNotEmpty()) {
                propertiesOpen = true
                equationPanelOpen = false
                insightsOpen = false
                controlsOpen = false
            }
            DeleteDropTarget(
                enabled = selectedSurfaceLayerIndices.isNotEmpty(),
                onDelete = { deleteSurfaceLayers(selectedSurfaceLayerIndices) },
            )
            DestructiveGlowButton("Clear all", enabled = surfaceLayers.isNotEmpty() || showBox || showSlice || showGradient || showContours, icon = "X") {
                clearGraph3DWorkspace()
            }
        }

        val selectedLayer = surfaceLayers.getOrNull(selectedSurfaceLayerIndex)
        if (selectedLayer != null && !equationPanelOpen && !propertiesOpen && !controlsOpen && !insightsOpen) {
            SmartSelectionHud(
                title = if (selectedSurfaceLayerIndices.size > 1) "${selectedSurfaceLayerIndices.size} surfaces" else "Surface ${selectedSurfaceLayerIndex + 1}",
                instruction = "Drag to orbit, pinch to zoom, open Properties for layer controls",
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 76.dp),
                selectionKey = selectedSurfaceLayerIndex to selectedSurfaceLayerIndices,
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    GlowButton("Properties", icon = "settings") { propertiesOpen = true }
                    GlowButton("Controls", icon = "sliders") { controlsOpen = true }
                    GlowButton("Insights", icon = "Info") { insightsOpen = true }
                    com.indianservers.aiexplorer.core.SpatialSurfaceRenderMode.entries.forEach { mode ->
                        TogglePill(
                            when (mode) {
                                com.indianservers.aiexplorer.core.SpatialSurfaceRenderMode.Surface -> "Surface"
                                com.indianservers.aiexplorer.core.SpatialSurfaceRenderMode.SurfaceMesh -> "Surface + Mesh"
                                com.indianservers.aiexplorer.core.SpatialSurfaceRenderMode.Wireframe -> "Wireframe"
                            },
                            selectedLayer.renderMode == mode,
                        ) { updateSelectedSurfaceRenderMode(mode) }
                    }
                }
            }
        }

        if (equationPanelOpen || propertiesOpen || insightsOpen || controlsOpen) {
            DimmedWorkspaceScrim {
                equationPanelOpen = false
                propertiesOpen = false
                insightsOpen = false
                controlsOpen = false
            }
        }

        if (equationPanelOpen) Graph3DEquationPanel(
            surfaceDraft = surfaceDraft,
            onSurfaceDraftChange = { surfaceDraft = it },
            message = surfaceInputMessage,
            examplesOpen = examplesOpen,
            onExamplesOpenChange = { examplesOpen = it },
            onPlot = ::plotSurfaceDraft,
            onClose = {
                equationPanelOpen = false
                addingSurfaceEquation = false
            },
            onMove = { panelOffset += it },
            modifier = Modifier.align(Alignment.TopStart).padding(top = workspaceTop, start = 8.dp).offset { IntOffset(panelOffset.x.roundToInt(), panelOffset.y.roundToInt()) },
        )

        if (propertiesOpen) Graph3DPropertiesPanel(
            surfaceLayers = surfaceLayers,
            selectedSurfaceLayerIndex = selectedSurfaceLayerIndex,
            selectedSurfaceLayerIndices = selectedSurfaceLayerIndices,
            query = surfaceLayerQuery,
            onQueryChange = { surfaceLayerQuery = it },
            onSelect = ::selectSurfaceLayer,
            onUpdateLayer = { index, layer ->
                vm.replaceSurfaceLayers(
                    surfaceLayers.mapIndexed { i, old -> if (i == index) layer else old },
                    "Update 3D surface properties",
                )
            },
            onEdit = { index ->
                selectSurfaceLayer(index)
                surfaceDraft = surfaceLayers[index].displayEquation()
                addingSurfaceEquation = false
                propertiesOpen = false
                equationPanelOpen = true
            },
            onDuplicate = { index ->
                val source = surfaceLayers[index]
                val newIndex = surfaceLayers.size
                val duplicate = source.copy(
                    id = "surface-${System.nanoTime()}",
                    colorIndex = source.colorIndex + 1,
                )
                val result = vm.replaceSurfaceLayers(surfaceLayers + duplicate, "Duplicate 3D surface")
                if (result.isSuccess) {
                    selectedSurfaceLayerIndex = newIndex
                    selectedSurfaceLayerIndices = setOf(newIndex)
                    surfaceDraft = duplicate.displayEquation()
                    surfaceInputMessage = "Duplicated surface ${index + 1}"
                } else {
                    surfaceInputMessage = "Unable to duplicate graph: ${result.exceptionOrNull()?.message ?: "workspace validation failed"}"
                }
            },
            onDelete = { deleteSurfaceLayers(setOf(it)) },
            onSelectAll = {
                selectedSurfaceLayerIndices = surfaceLayers.indices.toSet()
                selectedSurfaceLayerIndex = surfaceLayers.lastIndex
            },
            onDeleteSelected = { deleteSurfaceLayers(selectedSurfaceLayerIndices) },
            onClearAll = ::clearGraph3DWorkspace,
            graphSceneAppearance = graphSceneAppearance,
            graphAxisStyle = graphAxisStyle,
            onAppearanceChange = { updated ->
                graphSceneAppearance = updated
                vm.replaceSurfaceLayers(
                    surfaceLayers.mapIndexed { index, layer ->
                        if (index in selectedSurfaceLayerIndices || index == selectedSurfaceLayerIndex) {
                            layer.withWorkspaceAppearance(updated)
                        } else {
                            layer
                        }
                    },
                    "Change 3D surface appearance",
                )
            },
            onAxisChange = { graphAxisStyle = it },
            onClose = { propertiesOpen = false },
            onMove = { panelOffset += it },
            modifier = Modifier.align(Alignment.TopStart).padding(top = workspaceTop, start = 8.dp).offset { IntOffset(panelOffset.x.roundToInt(), panelOffset.y.roundToInt()) },
        )

        if (insightsOpen) Graph3DInsightsPanel(
            classification = insight.classification,
            vertex = insight.vertex?.let { "(${trim(it.x)}, ${trim(it.y)}, ${trim(it.z)})" } ?: "sampled",
            range = insight.range,
            symmetry = insight.symmetry,
            rendererSummary = "${sharedSurfacePlan.vertices.size / 10} vertices - ${sharedSurfacePlan.triangleIndices.size / 3} triangles",
            gradient = sharedDifferential?.let { "(${trim(it.gradient.x)}, ${trim(it.gradient.y)}, ${trim(it.gradient.z)})" },
            normal = sharedDifferential?.let { "(${trim(it.unitNormal.x)}, ${trim(it.unitNormal.y)}, ${trim(it.unitNormal.z)})" },
            tangentPlane = sharedDifferential?.tangentPlaneEquation,
            trace = "(${trim(traceX.toDouble())}, ${trim(traceY.toDouble())})",
            slice = "z = ${trim(sliceZ.toDouble())}",
            parameters = surfaceParameters.joinToString { it.name },
            onClose = { insightsOpen = false },
            onMove = { panelOffset += it },
            modifier = Modifier.align(Alignment.TopEnd).padding(top = workspaceTop, end = 8.dp).offset { IntOffset(panelOffset.x.roundToInt(), panelOffset.y.roundToInt()) },
        )

        if (controlsOpen) Graph3DControlsPanel(
            density = density,
            rotation = rotation,
            tilt = tilt,
            roll = roll,
            zoom = zoom,
            sliceZ = sliceZ,
            traceX = traceX,
            traceY = traceY,
            activeTool = activeTool,
            showContours = showContours,
            showSlice = showSlice,
            showGradient = showGradient,
            showBox = showBox,
            showOrientationCube = showOrientationCube,
            viewPreset = viewPreset,
            gradientPlayback = gradientPlayback,
            onDensity = { density = it; persistView() },
            onRotation = { rotation = it; persistView() },
            onTilt = { tilt = it; persistView() },
            onRoll = { roll = it; persistView() },
            onZoom = { zoom = it; persistView() },
            onSliceZ = { sliceZ = it; persistView() },
            onTraceX = { traceX = it; persistView() },
            onTraceY = { traceY = it; persistView() },
            onTool = { tool ->
                activeTool = tool
                if (tool == SurfaceTool.Contours) showContours = !showContours
                if (tool == SurfaceTool.Slice) showSlice = !showSlice
                if (tool == SurfaceTool.Gradient) showGradient = !showGradient
                if (tool == SurfaceTool.BoundingBox) showBox = !showBox
                persistView()
            },
            onToggleOrientationCube = { showOrientationCube = !showOrientationCube; persistView() },
            onView = ::applyView,
            onGradient = { ascending ->
                val path = mutableListOf<Vec3>()
                var point = Vec2(traceX.toDouble(), traceY.toDouble())
                repeat(80) {
                    val value = runCatching { surfaceCalculus.analyze(resolvedPrimaryExpression, point.x, point.y) }.getOrNull() ?: return@repeat
                    path += value.point
                    val magnitude = hypot(value.gradient.x, value.gradient.y)
                    if (magnitude < 1e-9) return@repeat
                    val sign = if (ascending) 1.0 else -1.0
                    point += Vec2(value.gradient.x / magnitude, value.gradient.y / magnitude) * (.05 * sign)
                }
                gradientPlayback = com.indianservers.aiexplorer.core.GradientPlayback3D(path, ascending = ascending).play()
                showGradient = true
                persistView()
            },
            onToggleGradientPlayback = {
                gradientPlayback = if (gradientPlayback.playing) gradientPlayback.pause() else gradientPlayback.play()
            },
            onExportContour = {
                val values = mesh?.vertices.orEmpty().filter { abs(it.z - sliceZ.toDouble()) <= .12 }.map { sliceZ.toDouble() to it }
                copyShapeText(context, "contour.csv", com.indianservers.aiexplorer.core.SpatialExportEngine.contoursCsv(values))
            },
            onClose = { controlsOpen = false },
            onMove = { panelOffset += it },
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 8.dp, bottom = 76.dp).offset { IntOffset(panelOffset.x.roundToInt(), panelOffset.y.roundToInt()) },
        )
    }
}

@Composable
private fun Graph3DEquationPanel(
    surfaceDraft: String,
    onSurfaceDraftChange: (String) -> Unit,
    message: String?,
    examplesOpen: Boolean,
    onExamplesOpenChange: (Boolean) -> Unit,
    onPlot: () -> Unit,
    onClose: () -> Unit,
    onMove: (Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassPanel(modifier.widthIn(max = 430.dp)) {
        PanelHeader("3D Equation", onClose, Cyan, icon = "Fx", onMove = onMove)
        Text(message ?: "Explicit z=f(x,y), implicit F(x,y,z)=0, or parametric x=...; y=...; z=...", color = Muted, fontSize = 11.sp)
        IntentAwareMathField(
            value = surfaceDraft,
            onValueChange = onSurfaceDraftChange,
            label = "3D surface",
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "3D surface equation input" },
            placeholder = "z=x^2+y^2  or  x^2+y^2+z^2=4",
            imeAction = ImeAction.Done,
            onDone = onPlot,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlowButton("Plot", icon = "Fx", modifier = Modifier.weight(1f), onClick = onPlot)
            GlowButton(if (examplesOpen) "Hide examples" else "Examples", icon = "menu", modifier = Modifier.weight(1f)) {
                onExamplesOpenChange(!examplesOpen)
            }
        }
        AnimatedVisibility(examplesOpen) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    "z=x^2+y^2",
                    "x^2+y^2+z^2=4",
                    "x=cos(u)*(3+cos(v)); y=sin(u)*(3+cos(v)); z=sin(v)",
                    "z=sin(x)+cos(y)",
                    "z=x*y/3",
                ).forEach { example ->
                    GlowButton(example) { onSurfaceDraftChange(example) }
                }
            }
        }
    }
}

@Composable
private fun Graph3DPropertiesPanel(
    surfaceLayers: List<com.indianservers.aiexplorer.core.SpatialSurfaceLayer>,
    selectedSurfaceLayerIndex: Int,
    selectedSurfaceLayerIndices: Set<Int>,
    query: String,
    onQueryChange: (String) -> Unit,
    onSelect: (Int, Boolean) -> Unit,
    onUpdateLayer: (Int, com.indianservers.aiexplorer.core.SpatialSurfaceLayer) -> Unit,
    onEdit: (Int) -> Unit,
    onDuplicate: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    onSelectAll: () -> Unit,
    onDeleteSelected: () -> Unit,
    onClearAll: () -> Unit,
    graphSceneAppearance: WorkspaceAppearance,
    graphAxisStyle: WorkspaceAxisStyle,
    onAppearanceChange: (WorkspaceAppearance) -> Unit,
    onAxisChange: (WorkspaceAxisStyle) -> Unit,
    onClose: () -> Unit,
    onMove: (Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassPanel(modifier.widthIn(max = 410.dp)) {
        PanelHeader("Graph Properties", onClose, Violet, icon = "settings", onMove = onMove)
        WorkspaceAppearancePicker(graphSceneAppearance, onAppearanceChange, Modifier.fillMaxWidth())
        WorkspaceAxisPicker(graphAxisStyle, graphSceneAppearance.palette, onAxisChange, Modifier.fillMaxWidth())
        OutlinedTextField(value = query, onValueChange = onQueryChange, label = { Text("Search surfaces") }, singleLine = true, modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Search 3D surface layers" })
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            GlowButton("Select all", enabled = surfaceLayers.isNotEmpty(), onClick = onSelectAll)
            DestructiveGlowButton("Delete selected", enabled = selectedSurfaceLayerIndices.isNotEmpty(), onClick = onDeleteSelected)
            DestructiveGlowButton("Clear all", enabled = surfaceLayers.isNotEmpty(), onClick = onClearAll)
        }
        if (surfaceLayers.isEmpty()) Text("Use + Equation to add the first surface.", color = Muted, fontSize = 11.sp)
        val filteredLayers = surfaceLayers.mapIndexed { index, layer -> index to layer }
            .filter { (_, layer) -> query.isBlank() || layer.displayEquation().contains(query.trim(), ignoreCase = true) }
        Column(
            Modifier.fillMaxWidth().heightIn(max = 430.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            filteredLayers.forEach { (index, layer) ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (index in selectedSurfaceLayerIndices || index == selectedSurfaceLayerIndex) Cyan.copy(.13f) else Color(0x22101824))
                        .border(1.dp, if (index in selectedSurfaceLayerIndices || index == selectedSurfaceLayerIndex) Cyan.copy(.55f) else Color.Transparent, RoundedCornerShape(11.dp))
                        .clickable { onSelect(index, false) }
                        .padding(7.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(layer.kind.name + " surface", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Surface ${index + 1}: ${layer.displayEquation()}",
                        color = Ink,
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth().semantics {
                            contentDescription = "3D surface layer ${index + 1}: ${layer.displayEquation()}"
                        },
                        maxLines = 2,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        GlowButton(if (index in selectedSurfaceLayerIndices) "Selected" else "Select") { onSelect(index, true) }
                        GlowButton("Edit") { onEdit(index) }
                        GlowButton("Copy") { onDuplicate(index) }
                        GlowButton(if (layer.visible) "Hide" else "Show") { onUpdateLayer(index, layer.copy(visible = !layer.visible)) }
                        GlowButton(layer.material.name) {
                            val entries = com.indianservers.aiexplorer.core.SpatialMaterial.entries
                            onUpdateLayer(index, layer.copy(material = entries[(layer.material.ordinal + 1) % entries.size]))
                        }
                        GlowButton(layer.quality.name) {
                            val entries = com.indianservers.aiexplorer.core.SpatialQuality.entries
                            onUpdateLayer(index, layer.copy(quality = entries[(layer.quality.ordinal + 1) % entries.size]))
                        }
                        DestructiveGlowButton("Delete", onClick = { onDelete(index) })
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        com.indianservers.aiexplorer.core.SpatialSurfaceRenderMode.entries.forEach { mode ->
                            TogglePill(
                                when (mode) {
                                    com.indianservers.aiexplorer.core.SpatialSurfaceRenderMode.Surface -> "Surface"
                                    com.indianservers.aiexplorer.core.SpatialSurfaceRenderMode.SurfaceMesh -> "Surface + Mesh"
                                    com.indianservers.aiexplorer.core.SpatialSurfaceRenderMode.Wireframe -> "Wireframe"
                                },
                                layer.renderMode == mode,
                            ) { onUpdateLayer(index, layer.copy(renderMode = mode)) }
                        }
                    }
                    AxisSlider("Opacity", layer.opacity.toFloat(), .1f..1f) { opacity -> onUpdateLayer(index, layer.copy(opacity = opacity.toDouble())) }
                }
            }
        }
    }
}

@Composable
private fun Graph3DInsightsPanel(
    classification: String,
    vertex: String,
    range: String,
    symmetry: String,
    rendererSummary: String,
    gradient: String?,
    normal: String?,
    tangentPlane: String?,
    trace: String,
    slice: String,
    parameters: String,
    onClose: () -> Unit,
    onMove: (Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassPanel(modifier.widthIn(max = 330.dp)) {
        PanelHeader("Surface Insights", onClose, Green, icon = "Info", onMove = onMove)
        Insight("Shared GPU renderer", rendererSummary, Cyan)
        Insight("Surface", classification, Cyan)
        Insight("Vertex", vertex, Violet)
        Insight("Range", range, Cyan)
        Insight("Symmetry", symmetry, Violet)
        Insight("Trace", trace, Green)
        Insight("Slice", slice, Violet)
        if (parameters.isNotBlank()) Insight("Parameters", parameters, Amber)
        gradient?.let { Insight("Gradient", it, Green) }
        normal?.let { Insight("Unit normal", it, Amber) }
        tangentPlane?.let { MathFormulaText(it, color = Violet, fontSize = 11.sp) }
    }
}

@Composable
private fun Graph3DControlsPanel(
    density: Float,
    rotation: Float,
    tilt: Float,
    roll: Float,
    zoom: Float,
    sliceZ: Float,
    traceX: Float,
    traceY: Float,
    activeTool: SurfaceTool,
    showContours: Boolean,
    showSlice: Boolean,
    showGradient: Boolean,
    showBox: Boolean,
    showOrientationCube: Boolean,
    viewPreset: SurfaceViewPreset,
    gradientPlayback: com.indianservers.aiexplorer.core.GradientPlayback3D,
    onDensity: (Float) -> Unit,
    onRotation: (Float) -> Unit,
    onTilt: (Float) -> Unit,
    onRoll: (Float) -> Unit,
    onZoom: (Float) -> Unit,
    onSliceZ: (Float) -> Unit,
    onTraceX: (Float) -> Unit,
    onTraceY: (Float) -> Unit,
    onTool: (SurfaceTool) -> Unit,
    onToggleOrientationCube: () -> Unit,
    onView: (SurfaceViewPreset) -> Unit,
    onGradient: (Boolean) -> Unit,
    onToggleGradientPlayback: () -> Unit,
    onExportContour: () -> Unit,
    onClose: () -> Unit,
    onMove: (Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassPanel(modifier.widthIn(max = 520.dp)) {
        PanelHeader("3D Graph Controls", onClose, Cyan, icon = "sliders", onMove = onMove)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SurfaceTool.entries.forEach { tool ->
                GlowButton(if (activeTool == tool) "• ${tool.name}" else tool.name) { onTool(tool) }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            TogglePill("Contours", showContours) { onTool(SurfaceTool.Contours) }
            TogglePill("Slice", showSlice) { onTool(SurfaceTool.Slice) }
            TogglePill("Gradient", showGradient) { onTool(SurfaceTool.Gradient) }
            TogglePill("Box", showBox) { onTool(SurfaceTool.BoundingBox) }
            TogglePill("Cube", showOrientationCube) { onToggleOrientationCube() }
        }
        AxisSlider("Mesh density", density, 8f..56f, onDensity)
        AxisSlider("Rotation", rotation, -180f..180f, onRotation)
        AxisSlider("Tilt", tilt, -89f..89f, onTilt)
        AxisSlider("Roll", roll, -180f..180f, onRoll)
        AxisSlider("Zoom", zoom, .35f..4f, onZoom)
        AxisSlider("Slice plane z", sliceZ, -4f..6f, onSliceZ)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SurfaceViewPreset.entries.forEach { preset ->
                GlowButton(if (preset == viewPreset) "• ${preset.name}" else preset.name) { onView(preset) }
            }
        }
        AxisSlider("Trace x", traceX, -3f..3f, onTraceX)
        AxisSlider("Trace y", traceY, -3f..3f, onTraceY)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            GlowButton("Ascent") { onGradient(true) }
            GlowButton("Descent") { onGradient(false) }
            GlowButton(if (gradientPlayback.playing) "Pause" else "Play", enabled = gradientPlayback.path.isNotEmpty(), onClick = onToggleGradientPlayback)
            GlowButton("Export contour", onClick = onExportContour)
        }
        Text("${gradientPlayback.index.coerceAtMost(gradientPlayback.path.lastIndex) + 1}/${gradientPlayback.path.size.coerceAtLeast(1)}", color = Green, fontSize = 10.sp)
    }
}

internal enum class TrigLab(val label: String) {
    Circle("Unit Circle"),
    Graphs("Trig Graphs"),
    Transform("Transform"),
    Identities("Identities"),
    Applications("Applications"),
    Challenge("Challenge"),
    Reference("Reference"),
}
internal enum class TrigLineStyle { Solid, Dashed, Dotted }

@Composable
private fun TrigonometryScreen(vm: ExplorerViewModel) {
    val adaptiveProfile = LocalAdaptiveDeviceProfile.current
    val workspaceToolTop = if (adaptiveProfile.isTelevision) {
        adaptiveProfile.workspacePolicy.topChromeClearance
    } else {
        72.dp
    }
    var angle by rememberSaveable { mutableFloatStateOf(-135f) }
    var amplitude by rememberSaveable { mutableFloatStateOf(1.5f) }
    var frequencyB by rememberSaveable { mutableFloatStateOf(1f) }
    var phase by rememberSaveable { mutableFloatStateOf(0f) }
    var verticalShift by rememberSaveable { mutableFloatStateOf(.5f) }
    var function by remember { mutableStateOf(TrigFunction.Sine) }
    var visibleFunctions by remember { mutableStateOf(setOf(TrigFunction.Sine, TrigFunction.Cosine, TrigFunction.Tangent)) }
    var angleUnit by remember { mutableStateOf(TrigAngleUnit.Degrees) }
    var lab by rememberSaveable { mutableStateOf(TrigLab.Circle) }
    var identityIndex by remember { mutableIntStateOf(0) }
    var identityStep by remember { mutableIntStateOf(0) }
    var showTangents by remember { mutableStateOf(true) }
    var showProjections by remember { mutableStateOf(true) }
    var showWave by remember { mutableStateOf(true) }
    var showAsymptotes by remember { mutableStateOf(true) }
    var homeRequest by remember { mutableIntStateOf(0) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var animateAngle by rememberSaveable { mutableStateOf(false) }
    var rotationDirection by remember { mutableIntStateOf(1) }
    var lineStyle by remember { mutableStateOf(TrigLineStyle.Solid) }
    var paletteShift by remember { mutableIntStateOf(0) }
    var application by rememberSaveable { mutableStateOf("Ferris Wheel") }
    var appPlaying by rememberSaveable { mutableStateOf(false) }
    var appTime by rememberSaveable { mutableFloatStateOf(2.2f) }
    var appAmplitude by rememberSaveable { mutableFloatStateOf(20f) }
    var appSpeed by rememberSaveable { mutableFloatStateOf(1f) }
    var appOffset by rememberSaveable { mutableFloatStateOf(25f) }
    var referenceQuery by rememberSaveable { mutableStateOf("") }
    var tutorOpen by rememberSaveable { mutableStateOf(false) }
    var tutorPrompt by rememberSaveable { mutableStateOf("") }
    var tutorAnswer by rememberSaveable { mutableStateOf("Ask a trigonometry question. I will use the current angle and screen as context.") }
    var challengeTarget by rememberSaveable { mutableFloatStateOf(.5f) }
    var challengeScore by rememberSaveable { mutableIntStateOf(0) }
    var challengeStreak by rememberSaveable { mutableIntStateOf(0) }
    var challengeFeedback by rememberSaveable { mutableStateOf("Find an angle where sin theta = 0.5.") }
    var clearEpochSeen by remember { mutableIntStateOf(vm.workspaceClearEpoch) }
    LaunchedEffect(vm.workspaceClearEpoch) {
        if (vm.workspaceClearEpoch != clearEpochSeen) {
            angle = -135f
            amplitude = 1f
            frequencyB = 1f
            phase = 0f
            verticalShift = 0f
            function = TrigFunction.Sine
            visibleFunctions = setOf(TrigFunction.Sine, TrigFunction.Cosine, TrigFunction.Tangent)
            animateAngle = false
            showTangents = true
            showProjections = true
            showWave = true
            showAsymptotes = true
            clearEpochSeen = vm.workspaceClearEpoch
        }
    }

    LaunchedEffect(animateAngle) {
        while (animateAngle) {
            delay(32)
            angle += rotationDirection * .9f
            if (angle > 360f) angle = -360f
            if (angle < -360f) angle = 360f
        }
    }
    LaunchedEffect(appPlaying, lab) {
        while (appPlaying && lab == TrigLab.Applications) {
            delay(32)
            appTime = (appTime + .03f * appSpeed).let { if (it > 12.57f) 0f else it }
        }
    }

    val radians = Math.toRadians(angle.toDouble())
    val snapshot = remember(angle) { InteractiveTrigEngine.snapshot(radians) }
    val period = if (kotlin.math.abs(frequencyB) < .0001f) 1_000_000.0 else 2 * Math.PI / kotlin.math.abs(frequencyB.toDouble())
    val transform = TrigTransform(amplitude.toDouble(), period, phase.toDouble(), verticalShift.toDouble())
    val identityLab = remember { InteractiveTrigIdentityLab() }
    val identity = remember(identityIndex) { identityLab.verify(identityIndex) }
    val safeAngle = kotlin.math.abs(angle.toDouble()).coerceIn(1.0, 178.0)
    val triangle = remember(safeAngle) { TriangleTrigSolver.sas(4.0, 5.0, safeAngle) }
    val displayAngle = InteractiveTrigEngine.fromRadians(radians, angleUnit)
    val tanText = snapshot.tangent?.let(::trim) ?: "Undefined"
    val secText = if (kotlin.math.abs(snapshot.cosine) < 1e-8) "Undefined" else trim(1.0 / snapshot.cosine)
    val cscText = if (kotlin.math.abs(snapshot.sine) < 1e-8) "Undefined" else trim(1.0 / snapshot.sine)
    val cotText = if (kotlin.math.abs(snapshot.sine) < 1e-8) "Undefined" else trim(snapshot.cosine / snapshot.sine)
    val quadrantLabel = when {
        snapshot.degrees == 0.0 || snapshot.degrees == 90.0 || snapshot.degrees == 180.0 || snapshot.degrees == 270.0 -> "Axis"
        else -> "Q${snapshot.quadrant}"
    }
    val specialAngles = listOf(0f, 30f, 45f, 60f, 90f, 120f, 135f, 180f, 270f, 360f)
    val referenceRows = listOf(
        Triple("0 deg (0)", "0", "1"),
        Triple("30 deg (pi/6)", "1/2", "sqrt(3)/2"),
        Triple("45 deg (pi/4)", "sqrt(2)/2", "sqrt(2)/2"),
        Triple("60 deg (pi/3)", "sqrt(3)/2", "1/2"),
        Triple("90 deg (pi/2)", "1", "0"),
        Triple("120 deg (2pi/3)", "sqrt(3)/2", "-1/2"),
        Triple("135 deg (3pi/4)", "sqrt(2)/2", "-sqrt(2)/2"),
        Triple("150 deg (5pi/6)", "1/2", "-sqrt(3)/2"),
        Triple("180 deg (pi)", "0", "-1"),
        Triple("270 deg (3pi/2)", "-1", "0"),
        Triple("360 deg (2pi)", "0", "1"),
    )

    fun setAngleAnimated(value: Float) {
        angle = value.coerceIn(-360f, 360f)
    }

    fun tutorReply(question: String): String {
        val q = question.lowercase()
        return when {
            "tan" in q && ("90" in q || "undefined" in q) -> "tan theta = sin theta / cos theta. At ${trim(angle.toDouble())} deg, cos theta is ${trim(snapshot.cosine)}. When cos theta is 0, tangent is undefined rather than a huge number."
            "identity" in q || "sin2" in q || "sin^2" in q -> "On the unit circle, the point is (cos theta, sin theta). Its radius is 1, so cos^2 theta + sin^2 theta = 1 by Pythagoras."
            "radian" in q -> "A radian measures angle by arc length divided by radius. 180 deg is pi radians; the current angle is ${radianLabel(angle.toDouble())}."
            "quadrant" in q -> "The current angle is ${trim(angle.toDouble())} deg, so the terminal side is in $quadrantLabel. Sine and cosine signs follow that quadrant."
            "reference" in q -> "The reference angle is the acute angle to the x-axis. Here it is ${trim(snapshot.referenceAngleDegrees)} deg."
            "asymptote" in q -> "Tangent has vertical asymptotes where cos theta = 0, such as 90 deg and 270 deg, because tan theta divides by cosine."
            else -> "Current context: ${trim(angle.toDouble())} deg, ${radianLabel(angle.toDouble())}, $quadrantLabel, screen ${lab.label}. Try asking about tangent, radians, identities, quadrants, or reference angles."
        }
    }

    Box(Modifier.fillMaxSize()) {
        TrigCanvas(
            modifier = Modifier.fillMaxSize(), angleDegrees = angle, transform = transform, function = function,
            showTangents = showTangents, showProjections = showProjections, showWave = showWave,
            homeRequest = homeRequest, onZoomChanged = { zoom = it }, onAngleChange = { angle = snapAngle(it) },
            visibleFunctions = when (lab) {
                TrigLab.Graphs -> visibleFunctions
                TrigLab.Transform -> setOf(TrigFunction.Sine)
                else -> visibleFunctions
            },
            showAsymptotes = showAsymptotes,
            polarSamples = emptyList(),
            harmonics = emptyList(),
            lineStyle = lineStyle,
            paletteShift = paletteShift,
            equationTarget = null,
            equationRoots = emptyList(),
            onTransformChange = { a, p, h, k -> amplitude = a; frequencyB = (2 * Math.PI / p).toFloat(); phase = h; verticalShift = k },
        )

        GlassPanel(
            Modifier
                .align(Alignment.TopStart)
                .padding(top = workspaceToolTop, start = 10.dp, end = 10.dp)
                .widthIn(max = 390.dp)
                .heightIn(max = 620.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("AI Maths Explorer", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Trigonometry Lab", color = Muted, fontSize = 10.sp)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    GlowButton("Home", icon = "H", iconOnly = true) { homeRequest++ }
                    GlowButton(if (animateAngle) "Pause" else "Animate", icon = if (animateAngle) "||" else ">", iconOnly = true) { animateAngle = !animateAngle }
                    GlowButton("AI", icon = "AI", iconOnly = true) { tutorOpen = !tutorOpen }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Insight("Angle", "${trim(angle.toDouble())} deg", Cyan)
                Insight("Radians", radianLabel(angle.toDouble()), Green)
                Insight("Quadrant", quadrantLabel, Amber)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("-360", color = Muted, fontSize = 10.sp)
                Slider(angle, { angle = snapAngle(it) }, valueRange = -360f..360f, modifier = Modifier.weight(1f))
                Text("360", color = Muted, fontSize = 10.sp)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                specialAngles.forEach { value -> GlowButton("${trim(value.toDouble())} deg", onClick = { setAngleAnimated(value) }) }
            }

            when (lab) {
                TrigLab.Circle -> {
                    Text("Live Function Values", color = Cyan, fontWeight = FontWeight.Bold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Insight("sin theta", trim(snapshot.sine), Cyan)
                        Insight("cos theta", trim(snapshot.cosine), Amber)
                        Insight("tan theta", tanText, Green)
                        Insight("csc theta", cscText, Violet)
                        Insight("sec theta", secText, Violet)
                        Insight("cot theta", cotText, Violet)
                    }
                    Text("Exact Values", color = Amber, fontWeight = FontWeight.Bold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Insight("sin", snapshot.exactSine ?: "approx ${trim(snapshot.sine)}", Cyan)
                        Insight("cos", snapshot.exactCosine ?: "approx ${trim(snapshot.cosine)}", Amber)
                        Insight("tan", snapshot.exactTangent ?: tanText, Green)
                    }
                    Text("Reference triangle uses the ${trim(snapshot.referenceAngleDegrees)} deg reference angle. Opposite=${trim(kotlin.math.abs(snapshot.sine))}, adjacent=${trim(kotlin.math.abs(snapshot.cosine))}, hypotenuse=1.", color = Muted, fontSize = 11.sp)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TogglePill("Projections", showProjections) { showProjections = it }
                        TogglePill("Tangents", showTangents) { showTangents = it }
                        TogglePill("Wave", showWave) { showWave = it }
                    }
                }
                TrigLab.Graphs -> {
                    Text("Trig Graphs", color = Cyan, fontWeight = FontWeight.Bold)
                    Text("Markers stay synchronized with the unit-circle angle. Tangent breaks at undefined values.", color = Muted, fontSize = 11.sp)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(TrigFunction.Sine, TrigFunction.Cosine, TrigFunction.Tangent).forEach { item ->
                            TogglePill(item.name, item in visibleFunctions) { enabled ->
                                visibleFunctions = if (enabled) visibleFunctions + item else (visibleFunctions - item).ifEmpty { setOf(TrigFunction.Sine) }
                                function = item
                            }
                        }
                        TogglePill("Asymptotes", showAsymptotes) { showAsymptotes = it }
                        TrigLineStyle.entries.forEach { style -> TogglePill(style.name, lineStyle == style) { lineStyle = style } }
                        GlowButton("Reset view") { homeRequest++ }
                    }
                    Insight("Trace", "x=${trim(angle.toDouble())} deg, sin=${trim(snapshot.sine)}, cos=${trim(snapshot.cosine)}, tan=$tanText", Green)
                    Insight("Zoom", "${trim(zoom.toDouble())}x", Violet)
                }
                TrigLab.Transform -> {
                    Text("y = A sin(B(x - h)) + k", color = Ink, fontWeight = FontWeight.ExtraBold)
                    AxisSlider("A amplitude/reflection", amplitude, -3f..3f) { amplitude = it }
                    AxisSlider("B frequency parameter", frequencyB, -3f..3f) { frequencyB = it }
                    AxisSlider("h phase shift", phase, -3.14f..3.14f) { phase = it }
                    AxisSlider("k vertical shift", verticalShift, -2f..2f) { verticalShift = it }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Insight("Amplitude", "|A| = ${trim(kotlin.math.abs(amplitude.toDouble()))}", Cyan)
                        Insight("Period", if (kotlin.math.abs(frequencyB) < .0001f) "Undefined" else "2pi/|B| = ${trim(period)}", Amber)
                        Insight("Midline", "y = ${trim(verticalShift.toDouble())}", Green)
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        GlowButton("Reset") { amplitude = 1f; frequencyB = 1f; phase = 0f; verticalShift = 0f }
                        GlowButton("Reflection") { amplitude = -1f; frequencyB = 1f; phase = 0f; verticalShift = 0f }
                        GlowButton("Shifted wave") { amplitude = 1.5f; frequencyB = 2f; phase = .7f; verticalShift = .5f }
                    }
                }
                TrigLab.Identities -> {
                    val identities = listOf(
                        "sin^2 theta + cos^2 theta = 1" to "The unit-circle point is (cos theta, sin theta); radius^2 is always 1.",
                        "1 + tan^2 theta = sec^2 theta" to "Divide sin^2 + cos^2 = 1 by cos^2 theta. Avoid angles where cos theta = 0.",
                        "sec^2 theta - tan^2 theta = 1" to "Rearrange 1 + tan^2 theta = sec^2 theta.",
                        "sin(-theta) = -sin theta" to "Mirrored angles have opposite y-coordinates.",
                        "cos(-theta) = cos theta" to "Mirrored angles keep the same x-coordinate.",
                    )
                    val current = identities[identityIndex.coerceIn(identities.indices)]
                    Text(current.first, color = Cyan, fontWeight = FontWeight.ExtraBold)
                    Text(current.second, color = Muted, fontSize = 12.sp)
                    Text("Step ${identityStep + 1}/4: ${listOf("Place theta on the unit circle", "Highlight the changing projection", "Compare both sides numerically", "The equality follows from the geometry")[identityStep.coerceIn(0, 3)]}", color = Green, fontSize = 12.sp)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        GlowButton("Replay") { identityStep = 0 }
                        GlowButton("Next step") { identityStep = (identityStep + 1).coerceAtMost(3) }
                        GlowButton("Next identity") { identityIndex = (identityIndex + 1) % identities.size; identityStep = 0 }
                    }
                    Insight("Live check", "sin^2 + cos^2 = ${trim(snapshot.sine * snapshot.sine + snapshot.cosine * snapshot.cosine)}", Amber)
                    Insight(identity.label, if (identity.evidence.equivalent) "Existing engine verification ready" else identity.evidence.explanation, Violet)
                }
                TrigLab.Applications -> {
                    Text("Real World Applications", color = Cyan, fontWeight = FontWeight.Bold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Ferris Wheel", "Pendulum", "Sound Wave", "Satellite Orbit", "Day/Night").forEach { item -> TogglePill(item, application == item) { application = item } }
                    }
                    AxisSlider("Amplitude / radius", appAmplitude, 1f..50f) { appAmplitude = it }
                    AxisSlider("Speed / frequency", appSpeed, .1f..4f) { appSpeed = it }
                    AxisSlider("Vertical offset", appOffset, 0f..60f) { appOffset = it }
                    AxisSlider("Time", appTime, 0f..12.57f) { appTime = it }
                    val height = appAmplitude * kotlin.math.sin(appSpeed * appTime) + appOffset
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        GlowButton(if (appPlaying) "Pause" else "Play") { appPlaying = !appPlaying }
                        GlowButton("Reset") { appTime = 0f; appPlaying = false }
                    }
                    Insight(application, "model value = ${trim(height.toDouble())}", Green)
                    Text("Simplified educational sine/cosine model. It shows periodic structure, not engineering-grade physical simulation.", color = Muted, fontSize = 11.sp)
                }
                TrigLab.Challenge -> {
                    Text("Challenge Mode", color = Amber, fontWeight = FontWeight.ExtraBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Insight("Score", "$challengeScore", Cyan)
                        Insight("Streak", "$challengeStreak", Green)
                        Insight("XP", "+${challengeStreak * 5}", Amber)
                    }
                    Text("Find an angle in [-360 deg, 360 deg] where sin theta = ${trim(challengeTarget.toDouble())}.", color = Ink)
                    GlowButton("Check answer") {
                        val correct = kotlin.math.abs(snapshot.sine - challengeTarget) < .025
                        if (correct) {
                            challengeScore += 120
                            challengeStreak += 1
                            challengeFeedback = "Correct. theta=${trim(angle.toDouble())} deg works; coterminal answers also count."
                        } else {
                            challengeStreak = 0
                            challengeFeedback = "Not yet. Current sin theta=${trim(snapshot.sine)}. Try 30 deg or 150 deg for 0.5."
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        GlowButton("Hint") { challengeFeedback = "Use the y-coordinate on the unit circle. Positive sine is in quadrants I and II." }
                        GlowButton("Next") { challengeTarget = listOf(.5f, 0f, -1f, .7071f).random(); challengeFeedback = "New target ready." }
                    }
                    Text(challengeFeedback, color = if (challengeFeedback.startsWith("Correct")) Green else Amber, fontSize = 12.sp)
                }
                TrigLab.Reference -> {
                    Text("Trig Quick Reference", color = Cyan, fontWeight = FontWeight.Bold)
                    OutlinedTextField(referenceQuery, { referenceQuery = it }, label = { Text("Search angle, value, identity") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    val filtered = referenceRows.filter { row -> referenceQuery.isBlank() || listOf(row.first, row.second, row.third).any { it.contains(referenceQuery, true) } }
                    filtered.forEach { row ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable {
                                row.first.substringBefore(" ").toFloatOrNull()?.let { setAngleAnimated(it) }
                            }.padding(7.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(row.first, color = Ink, fontSize = 11.sp, modifier = Modifier.weight(1.2f))
                            Text("sin ${row.second}", color = Cyan, fontSize = 11.sp, modifier = Modifier.weight(1f))
                            val tan = when (row.first.substringBefore(" ")) { "90", "270" -> "undefined"; else -> "see table" }
                            Text("cos ${row.third} / tan $tan", color = Muted, fontSize = 11.sp, modifier = Modifier.weight(1.4f))
                        }
                    }
                    Text("Identities: reciprocal, quotient, Pythagorean, even/odd. Transformations: amplitude |A|, period 2pi/|B|, phase h, vertical shift k.", color = Muted, fontSize = 11.sp)
                }
            }
        }

        GlassPanel(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                TrigLab.entries.forEach { item ->
                    GlowButton(if (lab == item) "* ${item.label}" else item.label) {
                        lab = item
                        if (item == TrigLab.Graphs) visibleFunctions = visibleFunctions + setOf(TrigFunction.Sine, TrigFunction.Cosine, TrigFunction.Tangent)
                    }
                }
                TogglePill(if (animateAngle) "Pause" else "Animate", animateAngle) { animateAngle = it }
            }
        }

        if (tutorOpen) {
            GlassPanel(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 98.dp)
                    .widthIn(max = 340.dp)
                    .heightIn(max = 390.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("AI Tutor", color = Cyan, fontWeight = FontWeight.ExtraBold)
                    GlowButton("Collapse", icon = "collapse", iconOnly = true) { tutorOpen = false }
                }
                Text("Context: ${trim(angle.toDouble())} deg, ${radianLabel(angle.toDouble())}, $quadrantLabel, ${lab.label}", color = Muted, fontSize = 10.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf("Why is tan 90 undefined?", "What is a radian?", "Which quadrant?", "Why sin^2+cos^2=1?").forEach { sample ->
                        GlowButton(sample.take(18)) { tutorPrompt = sample; tutorAnswer = tutorReply(sample) }
                    }
                }
                OutlinedTextField(tutorPrompt, { tutorPrompt = it }, label = { Text("Ask about trigonometry") }, modifier = Modifier.fillMaxWidth(), maxLines = 2)
                GlowButton("Ask", enabled = tutorPrompt.isNotBlank()) { tutorAnswer = tutorReply(tutorPrompt) }
                Text(tutorAnswer, color = Ink, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun LegacyTrigonometryScreen(vm: ExplorerViewModel) {
    val adaptiveProfile = LocalAdaptiveDeviceProfile.current
    val workspaceToolTop = if (adaptiveProfile.isTelevision) {
        adaptiveProfile.workspacePolicy.topChromeClearance
    } else {
        72.dp
    }
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
        Row(Modifier.align(Alignment.TopStart).padding(top = workspaceToolTop, start = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GlowButton("⌂ Home") { trigHomeRequest++ }
            Text("${trim(trigZoom.toDouble())}×", color = Muted, modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(SurfaceA).padding(9.dp))
        }
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
            Insight("Quadrant", "Q${snapshot.quadrant} - ref ${trim(snapshot.referenceAngleDegrees)}°", Cyan)
            Insight("sin θ", snapshot.exactSine ?: trim(snapshot.sine), Violet)
            Insight("cos θ", snapshot.exactCosine ?: trim(snapshot.cosine), Cyan)
            Insight("tan θ", snapshot.exactTangent ?: snapshot.tangent?.let(::trim) ?: "undefined", Green)
            Insight("Triangle", "c=${trim(triangle.c)}, area=${trim(triangle.area)}", Amber)
            Insight(identity.label, if (identity.evidence.equivalent) "Verified - residual 0" else identity.evidence.explanation, Green)
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
        "Paraboloid" to "z = x^2 + y^2",
        "Saddle" to "z = x^2 - y^2",
        "Wave" to "z = sin(x) + cos(y)",
        "Plane" to "z = 0.5*x + y",
        "Cone" to "z = sqrt(x^2 + y^2)",
        "Ripple" to "z = sin(x^2 + y^2)",
    )
    FlowRow(
        modifier,
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
private fun SurfaceEquationInput(
    value: String,
    message: String?,
    onValueChange: (String) -> Unit,
    onPlot: () -> Unit,
) {
    var editorValue by remember {
        mutableStateOf(TextFieldValue(value, selection = TextRange(value.length)))
    }
    fun commitSurface() {
        onPlot()
    }
    LaunchedEffect(value) {
        if (value != editorValue.text) {
            editorValue = editorValue.copy(
                text = value,
                selection = TextRange(
                    editorValue.selection.start.coerceAtMost(value.length),
                    editorValue.selection.end.coerceAtMost(value.length),
                ),
            )
        }
    }
    Column(
        Modifier
            .widthIn(max = 760.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        IntentAwareMathValueField(
            value = editorValue,
            onValueChange = {
                editorValue = it
                onValueChange(it.text)
            },
            label = "Surface equation",
            modifier = Modifier.fillMaxWidth(),
            placeholder = "Example: z = sin(x) + cos(y)",
            singleLine = true,
            showLegend = false,
            imeAction = ImeAction.Done,
            onDone = ::commitSurface,
            compactChrome = true,
        )
        message?.let {
            Text(
                it,
                color = if (it.startsWith("Plotted") || it.startsWith("Added") || it.startsWith("Updated")) Green else Color(0xFFFF8E9E),
                fontSize = 10.sp,
                maxLines = 1,
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
    manipulationMode: Transform2DMode,
    resizePolicy: Geometry2DResizePolicy,
    selectedShapes: Set<Int>,
    snapEnabled: Boolean,
    axisConstraint: AxisConstraint,
    precisionMode: Boolean,
    lassoEnabled: Boolean,
    boxSelectEnabled: Boolean,
    homeRequest: Int,
    undoViewRequest: Int,
    onPointDragStart: (Int) -> Unit,
    onPointDrag: (Int, Vec2) -> Unit,
    onShapeDragStart: (Int) -> Unit,
    onShapeDrag: (Vec2) -> Unit,
    onShapeRotate: (Double) -> Unit,
    onShapeScale: (Double) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onDropDelete: () -> Unit,
    onCanvasTap: (Vec2, Int?) -> Unit,
    onClearSelection: () -> Unit,
    onLassoSelection: (Set<Int>) -> Unit,
    onObjectLongPress: (shapeIndex: Int?, pointIndex: Int?, position: Vec2) -> Unit,
    onKeyboardMove: (dx: Int, dy: Int, precision: Boolean) -> Unit,
    onKeyboardCycle: (backwards: Boolean) -> Unit,
    content: androidx.compose.ui.graphics.drawscope.DrawScope.(toScreen: (Vec2) -> Offset) -> Unit,
) {
    var cameraCenter by remember { mutableStateOf(Vec2(0.0, 0.0)) }
    var cameraZoom by remember { mutableFloatStateOf(1f) }
    var lastTapAt by remember { mutableStateOf(0L) }
    var canvasWidth by remember { mutableIntStateOf(0) }
    var canvasHeight by remember { mutableIntStateOf(0) }
    var gestureMode by remember { mutableStateOf(GestureMode.Idle) }
    var lassoWorld by remember { mutableStateOf<List<Vec2>>(emptyList()) }
    var boxStartWorld by remember { mutableStateOf<Vec2?>(null) }
    var boxCurrentWorld by remember { mutableStateOf<Vec2?>(null) }
    var snapGuides by remember { mutableStateOf<List<com.indianservers.aiexplorer.core.SnapGuide>>(emptyList()) }
    var coordinateTooltip by remember { mutableStateOf<Vec2?>(null) }
    var rotationFeedback by remember { mutableStateOf<GeometryRotationSnap?>(null) }
    var viewportUndo by remember { mutableStateOf<List<com.indianservers.aiexplorer.core.Viewport2D>>(emptyList()) }
    val currentPoints by rememberUpdatedState(points)
    val currentShapes by rememberUpdatedState(shapes)
    val currentInteractionEnabled by rememberUpdatedState(interactionEnabled)
    val currentManipulationMode by rememberUpdatedState(manipulationMode)
    val currentResizePolicy by rememberUpdatedState(resizePolicy)
    val currentSelectedShapes by rememberUpdatedState(selectedShapes)
    val currentSnapEnabled by rememberUpdatedState(snapEnabled)
    val currentAxisConstraint by rememberUpdatedState(axisConstraint)
    val currentPrecisionMode by rememberUpdatedState(precisionMode)
    val currentLassoEnabled by rememberUpdatedState(lassoEnabled)
    val currentBoxSelectEnabled by rememberUpdatedState(boxSelectEnabled)
    LaunchedEffect(homeRequest, canvasWidth, canvasHeight) {
        if (homeRequest > 0 && canvasWidth > 0 && canvasHeight > 0) {
            viewportUndo = viewportUndo + com.indianservers.aiexplorer.core.Viewport2D(cameraCenter, cameraZoom)
            val fit = InteractionGeometry.fit(currentPoints, canvasWidth.toDouble() / canvasHeight)
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
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) false else when (event.key) {
                    Key.DirectionLeft -> { onKeyboardMove(-1, 0, event.isShiftPressed); true }
                    Key.DirectionRight -> { onKeyboardMove(1, 0, event.isShiftPressed); true }
                    Key.DirectionUp -> { onKeyboardMove(0, 1, event.isShiftPressed); true }
                    Key.DirectionDown -> { onKeyboardMove(0, -1, event.isShiftPressed); true }
                    Key.Tab -> { onKeyboardCycle(event.isShiftPressed); true }
                    Key.Delete, Key.Backspace -> { onDropDelete(); true }
                    else -> false
                }
            }
            .focusable()
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val canManipulate = currentInteractionEnabled
                    val manipulationDuringGesture = currentManipulationMode
                    val gestureSelectedShapes = currentSelectedShapes
                    val snapDuringGesture = currentSnapEnabled
                    val axisDuringGesture = currentAxisConstraint
                    val precisionDuringGesture = currentPrecisionMode
                    val lassoDuringGesture = currentLassoEnabled
                    val boxSelectDuringGesture = currentBoxSelectEnabled
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
                    val selectionPoints = gestureSelectedShapes.flatMap { index -> gestureShapes.getOrNull(index)?.pointIndices.orEmpty() }.distinct().mapNotNull(gesturePoints::getOrNull)
                    val selectionBounds = InteractionGeometry.bounds(selectionPoints)
                    val rotationHandle = selectionBounds?.let { bounds -> screen(Vec2(bounds.center.x, bounds.maximum.y + 1.0)) }
                    val scaleHandle = selectionBounds?.let { bounds -> screen(Vec2(bounds.maximum.x + .55, bounds.minimum.y - .55)) }
                    val selectingRegion = lassoDuringGesture || boxSelectDuringGesture
                    val rotating = canManipulate && gestureSelectedShapes.isNotEmpty() &&
                        !selectingRegion && rotationHandle != null &&
                        (rotationHandle - down.position).getDistance() <= 24.dp.toPx()
                    val proportionalScaling = canManipulate && gestureSelectedShapes.isNotEmpty() &&
                        currentResizePolicy == Geometry2DResizePolicy.Proportional && !selectingRegion &&
                        scaleHandle != null && (scaleHandle - down.position).getDistance() <= 24.dp.toPx()
                    val eligiblePointIndices = Geometry2DDragPlanner.eligibleHandleIndices(
                        WorkspaceState(points = gesturePoints, shapes = gestureShapes, functions = emptyList(), solids = emptyList(), vectors3D = emptyList()),
                        gestureSelectedShapes,
                    )
                    val tappedPointIndex = eligiblePointIndices
                        .minByOrNull { (screen(gesturePoints[it]) - down.position).getDistance() }
                        ?.takeIf { (screen(gesturePoints[it]) - down.position).getDistance() <= 22.dp.toPx() }
                    var pointIndex: Int? = null
                    var shapeIndex: Int? = null
                    if (canManipulate && !selectingRegion) {
                        pointIndex = tappedPointIndex
                        if (pointIndex == null && !rotating && !proportionalScaling) {
                            shapeIndex = gestureShapes.indices.reversed().filter { gestureShapes[it].visible }
                                .minByOrNull { shapeScreenDistance(gestureShapes[it], gesturePoints, down.position, ::screen) }
                                ?.takeIf { shapeScreenDistance(gestureShapes[it], gesturePoints, down.position, ::screen) <= 42f }
                        }
                        pointIndex?.let(onPointDragStart)
                        shapeIndex?.let(onShapeDragStart)
                        if (rotating) gestureSelectedShapes.lastOrNull()?.let(onShapeDragStart)
                        if (proportionalScaling) gestureSelectedShapes.lastOrNull()?.let(onShapeDragStart)
                    }
                    val gestureTarget = when {
                        pointIndex != null -> GeometryGestureTarget.JunctionHandle
                        shapeIndex != null || rotating || proportionalScaling -> GeometryGestureTarget.ShapeBody
                        else -> GeometryGestureTarget.EmptyCanvas
                    }
                    if (lassoDuringGesture) { lassoWorld = listOf(startWorld); gestureMode = GestureMode.Lasso }
                    else if (boxSelectDuringGesture) { boxStartWorld = startWorld; boxCurrentWorld = startWorld; gestureMode = GestureMode.Lasso }
                    else gestureMode = when { rotating -> GestureMode.Rotating; proportionalScaling || pointIndex != null -> GestureMode.Resizing; shapeIndex != null -> GestureMode.Moving; else -> GestureMode.Selecting }

                    var moved = false
                    var transformed = false
                    var latestPosition = down.position
                    var latestUptime = down.uptimeMillis
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.firstOrNull()?.let { latestPosition = it.position; latestUptime = it.uptimeMillis }
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
                                    lassoDuringGesture -> lassoWorld = lassoWorld + currentWorld
                                    boxSelectDuringGesture -> boxCurrentWorld = currentWorld
                                    rotating && selectionBounds != null -> {
                                        val snap = snapGeometryRotation(InteractionGeometry.rotationDegrees(selectionBounds.center, startWorld, currentWorld))
                                        rotationFeedback = snap
                                        onShapeRotate(snap.angle)
                                    }
                                    proportionalScaling && selectionBounds != null -> {
                                        val initialDistance = selectionBounds.center.distanceTo(startWorld).coerceAtLeast(1e-6)
                                        onShapeScale(selectionBounds.center.distanceTo(currentWorld) / initialDistance)
                                    }
                                    pointIndex != null -> {
                                        val original = gesturePoints[pointIndex]
                                        val handleHeld = change.uptimeMillis - down.uptimeMillis >= 450L
                                        val constrained = PrecisionInteraction.apply(SmartSnapEngine.constrain(currentWorld - original, axisDuringGesture), precisionDuringGesture || handleHeld)
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
                                        val snapped = if (snapDuringGesture) SmartSnapEngine.snap(
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
                                        val constrained = PrecisionInteraction.apply(SmartSnapEngine.constrain(currentWorld - startWorld, axisDuringGesture), precisionDuringGesture)
                                        snapGuides = emptyList()
                                        coordinateTooltip = selectionBounds?.center?.plus(constrained)
                                        onShapeDrag(constrained)
                                    }
                                    canManipulate -> {
                                        cameraCenter = Vec2(cameraCenter.x - delta.x / scale(), cameraCenter.y + delta.y / scale())
                                        gestureMode = GestureMode.Panning
                                    }
                                }
                                change!!.consume()
                            }
                        }
                        if (event.changes.none { it.pressed }) break
                    }

                    val longPressedObject = !moved && !transformed && latestUptime - down.uptimeMillis >= 500L && (pointIndex != null || shapeIndex != null)
                    when {
                        longPressedObject -> {
                            onDragCancel()
                            onObjectLongPress(shapeIndex, pointIndex, startWorld)
                        }
                        lassoDuringGesture -> {
                            val selected = gestureShapes.indices.filterTo(linkedSetOf()) { index ->
                                val shapePoints = gestureShapes[index].pointIndices.mapNotNull(gesturePoints::getOrNull)
                                shapePoints.isNotEmpty() && InteractionGeometry.pointInPolygon(InteractionGeometry.bounds(shapePoints)?.center ?: shapePoints.first(), lassoWorld)
                            }
                            onLassoSelection(selected)
                        }
                        boxSelectDuringGesture -> {
                            val end = boxCurrentWorld ?: startWorld
                            onLassoSelection(com.indianservers.aiexplorer.workspace.Geometry2DDirectManipulation.boxSelect(
                                com.indianservers.aiexplorer.workspace.WorkspaceState(points = gesturePoints, shapes = gestureShapes, functions = emptyList(), solids = emptyList(), vectors3D = emptyList()),
                                startWorld,
                                end,
                            ))
                        }
                        pointIndex != null || shapeIndex != null || rotating || proportionalScaling -> {
                            val overDelete = latestPosition.x in (size.width * .32f)..(size.width * .68f) && latestPosition.y >= size.height * .78f
                            if (overDelete && manipulationDuringGesture != Transform2DMode.Select && (pointIndex != null || shapeIndex != null)) {
                                onDragCancel()
                                onDropDelete()
                            } else onDragEnd()
                            rotationFeedback = null
                        }
                        !moved && !transformed -> {
                            val now = System.currentTimeMillis()
                            if (canManipulate && now - lastTapAt < 320L) {
                                val fit = InteractionGeometry.fit(gesturePoints, size.width.toDouble() / size.height)
                                cameraCenter = fit.center
                                cameraZoom = fit.zoom
                                lastTapAt = 0L
                            } else {
                                if (canManipulate && tappedPointIndex == null && shapeIndex == null) onClearSelection()
                                else onCanvasTap(startWorld, tappedPointIndex)
                                lastTapAt = if (canManipulate) now else 0L
                            }
                        }
                    }
                    gestureMode = GestureMode.Idle
                    val viewportTo = com.indianservers.aiexplorer.core.Viewport2D(cameraCenter, cameraZoom)
                    if (viewportTo != viewportFrom && (transformed || (moved && gestureTarget == GeometryGestureTarget.EmptyCanvas))) viewportUndo = viewportUndo + viewportFrom
                    snapGuides = emptyList()
                    coordinateTooltip = null
                    lassoWorld = emptyList()
                    boxStartWorld = null
                    boxCurrentWorld = null
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
        val boxStart = boxStartWorld; val boxEnd = boxCurrentWorld
        if (boxStart != null && boxEnd != null) {
            val first = tx(boxStart); val second = tx(boxEnd)
            drawRect(Cyan.copy(.16f), Offset(min(first.x, second.x), min(first.y, second.y)), Size(abs(second.x - first.x), abs(second.y - first.y)))
            drawRect(Cyan, Offset(min(first.x, second.x), min(first.y, second.y)), Size(abs(second.x - first.x), abs(second.y - first.y)), style = Stroke(2.5f))
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
            drawArc(
                color = Violet,
                startAngle = -65f,
                sweepAngle = 285f,
                useCenter = false,
                topLeft = rotateHandle - Offset(13f, 13f),
                size = Size(26f, 26f),
                style = Stroke(3f, cap = StrokeCap.Round),
            )
            drawLine(Violet, rotateHandle + Offset(9f, -9f), rotateHandle + Offset(14f, -8f), 3f, cap = StrokeCap.Round)
            drawLine(Violet, rotateHandle + Offset(9f, -9f), rotateHandle + Offset(11f, -14f), 3f, cap = StrokeCap.Round)
            if (resizePolicy == Geometry2DResizePolicy.Proportional) {
                val scaleHandle = tx(Vec2(bounds.maximum.x + .55, bounds.minimum.y - .55))
                drawLine(Cyan.copy(.75f), bottomRight, scaleHandle, 2f)
                drawRect(Cyan.copy(.2f), scaleHandle - Offset(10f, 10f), Size(20f, 20f))
                drawRect(Cyan, scaleHandle - Offset(7f, 7f), Size(14f, 14f), style = Stroke(2.5f))
            }
            drawCircle(Green.copy(.25f), 18f, center)
            drawLine(Green, center - Offset(10f, 0f), center + Offset(10f, 0f), 2f)
            drawLine(Green, center - Offset(0f, 10f), center + Offset(0f, 10f), 2f)
            val measure = if (selectedWorld.size >= 2) {
                val segment = Geometry2D.segment(selectedWorld.first(), selectedWorld.last())
                "Δx ${trim(bounds.width)} - Δy ${trim(bounds.height)} - length ${trim(segment.distance)} - slope ${segment.slope?.let(::trim) ?: "∞"}"
            } else "(${trim(bounds.center.x)}, ${trim(bounds.center.y)})"
            drawGraphLabel(measure, boxTopLeft + Offset(8f, -48f), Amber)
        }
        rotationFeedback?.let { feedback ->
            drawGraphLabel(
                "Angle ${"%.1f".format(java.util.Locale.US, feedback.angle)} deg${if (feedback.snapped) " - snap" else ""}",
                Offset(size.width / 2f - 88f, 128f),
                if (feedback.snapped) Green else Violet,
            )
        }
        coordinateTooltip?.let { point -> drawGraphLabel("(${trim(point.x)}, ${trim(point.y)})", tx(point) + Offset(18f, -54f), Green) }
        if (gestureMode != GestureMode.Idle) drawGraphLabel(gestureMode.label, Offset(size.width / 2f - 95f, 92f), Cyan)
        drawGraphLabel("${trim(cameraZoom.toDouble())}× - ${trim((size.width / scale).toDouble())} units wide", Offset(size.width - 205f, 92f), Muted)
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

internal fun pointSegmentDistance(point: Offset, start: Offset, end: Offset): Float {
    val dx = end.x - start.x
    val dy = end.y - start.y
    val lengthSquared = dx * dx + dy * dy
    if (lengthSquared <= 1e-6f) return (point - start).getDistance()
    val t = (((point.x - start.x) * dx + (point.y - start.y) * dy) / lengthSquared).coerceIn(0f, 1f)
    return hypot(point.x - (start.x + t * dx), point.y - (start.y + t * dy))
}

@Composable
private fun Selected3DFormulaInspector(
    solid: Solid,
    index: Int,
    bounds: com.indianservers.aiexplorer.core.AxisAlignedBounds3D?,
    subSelection: SubObjectSelection?,
    onClose: () -> Unit,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val measurements = Geometry3D.measure(solid)
    GlassPanel(modifier) {
        PanelHeader("Object Formulas", onClose, Green, icon = "f")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Object ${index + 1}", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(solid.type.displayName(), color = Cyan, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            GlowButton("Copy", icon = "Copy", onClick = onCopy)
        }
        Text("FORMULAS", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Geometry3D.formulas(solid.type).forEach { (name, formula) ->
            Insight(name, formula, Violet)
        }
        Text("LIVE PROPERTIES", color = Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Insight("Surface area", trim(measurements.surfaceArea), Green)
        Insight("Volume", trim(measurements.volume), Green)
        Insight("Topology", "${measurements.faces} faces, ${measurements.edges} edges, ${measurements.vertices} vertices", Cyan)
        Text("DIMENSIONS", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Insight("Width height depth", "${trim(solid.width)} x ${trim(solid.height)} x ${trim(solid.depth)}", Cyan)
        Insight("Radius", trim(solid.radius), Cyan)
        if (solid.type == SolidType.Frustum) Insight("Top radius", trim(solid.topRadius), Cyan)
        Text("TRANSFORM", color = Amber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Insight("Position", "x ${trim(solid.position.x)}, y ${trim(solid.position.y)}, z ${trim(solid.position.z)}", Amber)
        Insight("Rotation", "x ${trim(solid.rotation.x)} deg, y ${trim(solid.rotation.y)} deg, z ${trim(solid.rotation.z)} deg", Amber)
        bounds?.let {
            Insight("Bounds min", "${trim(it.minimum.x)}, ${trim(it.minimum.y)}, ${trim(it.minimum.z)}", Muted)
            Insight("Bounds max", "${trim(it.maximum.x)}, ${trim(it.maximum.y)}, ${trim(it.maximum.z)}", Muted)
        }
        subSelection?.let {
            Insight("Selected part", "${it.mode.name} ${it.index + 1}", Amber)
        }
    }
}

private fun buildSelected3DFormulaReport(
    solid: Solid,
    bounds: com.indianservers.aiexplorer.core.AxisAlignedBounds3D?,
): String {
    val measurements = Geometry3D.measure(solid)
    return buildString {
        appendLine("${solid.type.displayName()} formulas")
        Geometry3D.formulas(solid.type).forEach { (name, formula) -> appendLine("$name: $formula") }
        appendLine()
        appendLine("Properties")
        appendLine("Surface area: ${trim(measurements.surfaceArea)}")
        appendLine("Volume: ${trim(measurements.volume)}")
        appendLine("Topology: ${measurements.faces} faces, ${measurements.edges} edges, ${measurements.vertices} vertices")
        appendLine("Dimensions: ${trim(solid.width)} x ${trim(solid.height)} x ${trim(solid.depth)}")
        appendLine("Radius: ${trim(solid.radius)}")
        if (solid.type == SolidType.Frustum) appendLine("Top radius: ${trim(solid.topRadius)}")
        appendLine("Position: ${trim(solid.position.x)}, ${trim(solid.position.y)}, ${trim(solid.position.z)}")
        appendLine("Rotation: ${trim(solid.rotation.x)} deg, ${trim(solid.rotation.y)} deg, ${trim(solid.rotation.z)} deg")
        bounds?.let {
            appendLine("Bounds min: ${trim(it.minimum.x)}, ${trim(it.minimum.y)}, ${trim(it.minimum.z)}")
            appendLine("Bounds max: ${trim(it.maximum.x)}, ${trim(it.maximum.y)}, ${trim(it.maximum.z)}")
        }
    }
}

@Composable
private fun SolidTrackballPalette(
    modifier: Modifier = Modifier,
    selectedSolid: Solid?,
    transformMode: Transform3DMode,
    selectionMode: Selection3DMode,
    onAdd: (SolidType) -> Unit,
    onAddVector: () -> Unit,
    onTransformMode: (Transform3DMode) -> Unit,
    onSelectionMode: (Selection3DMode) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var category by remember { mutableIntStateOf(0) }
    val groups = listOf(
        "Basic" to listOf(SolidType.Cube, SolidType.Cuboid, SolidType.Sphere, SolidType.Hemisphere),
        "Round" to listOf(SolidType.Cylinder, SolidType.Cone, SolidType.Frustum, SolidType.Torus),
        "Prisms" to listOf(SolidType.TriangularPrism, SolidType.PentagonalPrism, SolidType.HexagonalPrism, SolidType.Pyramid),
        "Advanced" to listOf(SolidType.Tetrahedron, SolidType.Octahedron, SolidType.Ellipsoid, SolidType.Paraboloid),
    )
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(58.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xCC30D9FF), Color(0xCC6C48FF), SurfaceA)))
                .border(1.dp, Cyan.copy(.75f), androidx.compose.foundation.shape.CircleShape)
                .clickable { expanded = !expanded }
                .semantics { contentDescription = if (expanded) "Close 3D trackball tools" else "Open 3D trackball tools" },
            contentAlignment = Alignment.Center,
        ) {
            Text(if (expanded) "X" else "3D", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
        }
        AnimatedVisibility(expanded) {
            GlassPanel(Modifier.width(318.dp).padding(top = 6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("3D trackball", color = Ink, fontWeight = FontWeight.Bold)
                    Text("X", color = Muted, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { expanded = false }.padding(8.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    groups.forEachIndexed { index, group ->
                        GlowButton(if (category == index) "• ${group.first}" else group.first) { category = index }
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    groups[category].second.forEach { type -> GlowButton("+ ${type.name}") { onAdd(type) } }
                    GlowButton("+ Vector", onClick = onAddVector)
                }
                selectedSolid?.let { solid ->
                    Text("Drag object: ${transformMode.name.lowercase()}", color = Muted, fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Transform3DMode.entries.forEach { mode -> GlowButton(if (mode == transformMode) "• ${mode.name}" else mode.name) { onTransformMode(mode) } }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Selection3DMode.entries.forEach { mode -> GlowButton(if (mode == selectionMode) "• ${mode.name}" else mode.name) { onSelectionMode(mode) } }
                    }
                    Text(solid.type.name, color = Cyan, fontWeight = FontWeight.Bold)
                    Text(Geometry3D.formula(solid.type), color = Ink, fontSize = 12.sp, maxLines = 2)
                    Text("Drag body to move; choose Scale then drag for proportional resize. Vertex/edge/face modes expose multiple control points.", color = Muted, fontSize = 10.sp, maxLines = 2)
                } ?: Text(
                    "Select an object to enable Move, Resize, Rotate and sub-object controls.",
                    color = Muted.copy(alpha = .3f),
                    fontSize = 11.sp,
                )
            }
        }
    }
}

@Composable
internal fun DeleteDropTarget(enabled: Boolean, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) Color(0xCC351521) else SurfaceA.copy(alpha = .55f))
            .border(1.dp, if (enabled) Color(0xFFFF6688) else Muted.copy(.35f), RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onDelete)
            .semantics { contentDescription = "Delete selected object or drag object here" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Canvas(Modifier.padding(top = 7.dp).size(22.dp)) {
            val color = if (enabled) Color(0xFFFF6688) else Muted
            drawRoundRect(color, Offset(size.width * .27f, size.height * .30f), Size(size.width * .46f, size.height * .55f), cornerRadius = CornerRadius(3f, 3f), style = Stroke(2.5f))
            drawLine(color, Offset(size.width * .20f, size.height * .23f), Offset(size.width * .80f, size.height * .23f), 2.5f)
            drawLine(color, Offset(size.width * .40f, size.height * .13f), Offset(size.width * .60f, size.height * .13f), 2.5f)
        }
        Text(if (enabled) "Delete" else "Select", color = if (enabled) Color(0xFFFF6688) else Muted, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}

@Composable
internal fun AddShapeTarget(
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "+ Add",
    contentDescription: String = "Add a shape to the scene",
) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xDD0D332C))
            .border(1.dp, Green.copy(alpha = .88f), RoundedCornerShape(16.dp))
            .clickable(onClick = onAdd)
            .semantics { this.contentDescription = contentDescription },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Canvas(Modifier.padding(top = 7.dp).size(22.dp)) {
            drawCircle(Green.copy(alpha = .18f), size.minDimension / 2)
            drawLine(Green, Offset(size.width * .20f, size.height / 2), Offset(size.width * .80f, size.height / 2), 3f, cap = StrokeCap.Round)
            drawLine(Green, Offset(size.width / 2, size.height * .20f), Offset(size.width / 2, size.height * .80f), 3f, cap = StrokeCap.Round)
        }
        Text(label, color = Green, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp))
    }
}

private fun SolidType.libraryCategory(): String = when (this) {
    SolidType.Cube, SolidType.Cuboid -> "Basic"
    SolidType.Sphere, SolidType.Hemisphere, SolidType.Cylinder, SolidType.Cone,
    SolidType.Frustum, SolidType.Torus, SolidType.Capsule, SolidType.Ellipsoid,
    SolidType.Paraboloid,
    -> "Curved"
    SolidType.TriangularPrism, SolidType.PentagonalPrism, SolidType.HexagonalPrism,
    SolidType.OctagonalPrism,
    -> "Prisms"
    SolidType.Tetrahedron, SolidType.TriangularPyramid, SolidType.Octahedron,
    SolidType.Pyramid, SolidType.Wedge,
    -> "Polyhedra"
}

private fun SolidType.displayName(): String =
    name.replace(Regex("(?<=[a-z])(?=[A-Z])"), " ")

private fun SolidType.libraryColor(): Color = when (libraryCategory()) {
    "Basic" -> Cyan
    "Curved" -> Color(0xFFFF6FAE)
    "Prisms" -> Green
    else -> Violet
}

@Composable
private fun SolidShapeLibrary(
    sceneObjectCount: Int,
    onDismiss: () -> Unit,
    onAdd: (SolidType) -> Unit,
    onAddVector: () -> Unit,
    onAddSegment: () -> Unit,
    onAddLine: () -> Unit,
    onAddRay: () -> Unit,
    onAddPoint: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var search by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("All") }
    var constructionsExpanded by remember { mutableStateOf(true) }
    var preview by remember { mutableStateOf(SolidType.Cube) }
    val categories = listOf("All", "Basic", "Curved", "Prisms", "Polyhedra")
    val normalizedSearch = search.trim()
    val constructionTools = listOf(
        Triple("Point", "Exact/free 3D coordinate with name, visible, locked and style properties", onAddPoint),
        Triple("Segment", "Finite editable construction between endpoints", onAddSegment),
        Triple("Line", "Long editable straight construction through 3D space", onAddLine),
        Triple("Ray", "Directional construction from a start point", onAddRay),
        Triple("Vector", "Start/end vector with dx, dy, dz and magnitude", onAddVector),
    )
    val visibleConstructionTools = constructionTools.filter { (label, detail, _) ->
        normalizedSearch.isBlank() || label.contains(normalizedSearch, ignoreCase = true) || detail.contains(normalizedSearch, ignoreCase = true)
    }
    val visibleTypes = SolidType.entries.filter { type ->
        (category == "All" || type.libraryCategory() == category) &&
            (normalizedSearch.isBlank() || type.displayName().contains(normalizedSearch, ignoreCase = true))
    }
    GlassPanel(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Add to 3D space", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("${SolidType.entries.size} solids - $sceneObjectCount currently in scene", color = Green, fontSize = 10.sp)
            }
            GlowButton("Close", icon = "×", onClick = onDismiss)
        }
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            GlowButton("+ Add 3D Point", icon = "P", onClick = onAddPoint)
            GlowButton("+ Segment", icon = "S", onClick = onAddSegment)
            GlowButton("+ Line", icon = "L", onClick = onAddLine)
            GlowButton("+ Ray", icon = "R", onClick = onAddRay)
            GlowButton("+ Add vector", onClick = onAddVector)
        }
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search 3D tools and solids") },
            placeholder = { Text("sphere, prism, pyramid…") },
            singleLine = true,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            categories.forEach { option ->
                GlowButton(if (category == option) "• $option" else option) { category = option }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(preview.libraryColor().copy(alpha = .10f))
                .border(1.dp, preview.libraryColor().copy(alpha = .55f), RoundedCornerShape(14.dp))
                .padding(9.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SolidLibraryThumbnail(preview, preview.libraryColor(), Modifier.size(68.dp))
            Column(Modifier.weight(1f)) {
                Text(preview.displayName(), color = preview.libraryColor(), fontWeight = FontWeight.Bold)
                Text(Geometry3D.formula(preview), color = Ink, fontSize = 10.sp, maxLines = 2)
                Text("Placed automatically in the nearest clear floor position.", color = Muted, fontSize = 9.sp)
            }
            GlowButton("+ Add ${preview.displayName()}", onClick = { onAdd(preview) })
        }
        if (visibleTypes.isEmpty()) {
            Text("No matching shapes. Try another name or choose All.", color = Amber)
        } else {
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                visibleTypes.forEach { type ->
                    val selected = type == preview
                    Column(
                        Modifier
                            .widthIn(min = 156.dp, max = 220.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(type.libraryColor().copy(alpha = if (selected) .18f else .07f))
                            .border(1.dp, type.libraryColor().copy(alpha = if (selected) .9f else .35f), RoundedCornerShape(13.dp))
                            .clickable { preview = type }
                            .padding(7.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        SolidLibraryThumbnail(type, type.libraryColor(), Modifier.size(48.dp))
                        Text(
                            type.displayName(),
                            color = if (selected) type.libraryColor() else Ink,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            "+ Add",
                            color = Green,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    preview = type
                                    onAdd(type)
                                }
                                .padding(horizontal = 12.dp, vertical = 5.dp),
                        )
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Need direction or magnitude?", color = Muted, fontSize = 10.sp)
            GlowButton("+ Add vector", onClick = onAddVector)
        }
    }
}

@Composable
private fun SolidLibraryThumbnail(type: SolidType, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.semantics { contentDescription = "${type.displayName()} preview" }) {
        drawSolidProjection(
            solid = defaultSolid(type),
            offset = Vec3(0.0, 0.0, 0.0),
            rx = 24f,
            ry = -32f,
            rz = 8f,
            center = center,
            scale = size.minDimension * .22f,
            color = color,
            visualMode = SpatialVisualMode.Solid,
            selected = false,
            perspective = true,
            subSelection = null,
            sectionEnabled = false,
            sectionPlane = EditableSectionPlane(),
            clipSection = false,
        )
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
private fun OrientationCube(modifier: Modifier = Modifier, onPreset: (String) -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(SurfaceA.copy(.94f)).border(1.dp, Violet.copy(.5f), RoundedCornerShape(14.dp)).padding(6.dp)
            .semantics { contentDescription = "Interactive 3D orientation cube" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Canvas(Modifier.size(62.dp).clickable { onPreset("Isometric") }) {
            val front = Path().apply { moveTo(10f, 22f); lineTo(38f, 30f); lineTo(38f, 57f); lineTo(10f, 48f); close() }
            val side = Path().apply { moveTo(38f, 30f); lineTo(56f, 18f); lineTo(56f, 45f); lineTo(38f, 57f); close() }
            val top = Path().apply { moveTo(10f, 22f); lineTo(29f, 9f); lineTo(56f, 18f); lineTo(38f, 30f); close() }
            drawPath(front, Cyan.copy(.35f)); drawPath(front, Cyan, style = Stroke(2f))
            drawPath(side, Violet.copy(.4f)); drawPath(side, Violet, style = Stroke(2f))
            drawPath(top, Amber.copy(.35f)); drawPath(top, Amber, style = Stroke(2f))
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            listOf("Front", "Top", "Side").forEach { preset -> Text(preset.take(1), color = Ink, fontSize = 9.sp, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onPreset(preset) }.padding(5.dp)) }
        }
    }
}

@Composable
private fun SpatialSceneNavigator(
    solids: List<Solid>,
    selected: Set<Int>,
    collisions: List<Pair<Int, Int>>,
    trail: List<Vec3>,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier.width(148.dp).height(112.dp).clip(RoundedCornerShape(15.dp))
            .background(SurfaceA.copy(.92f)).border(1.dp, Cyan.copy(.45f), RoundedCornerShape(15.dp))
            .semantics { contentDescription = "3D scene navigator with ${solids.size} objects, ${selected.size} selected, and ${collisions.size} overlaps" },
    ) {
        val all = solids.map { it.position } + trail
        val minX = all.minOfOrNull { it.x } ?: -4.0; val maxX = all.maxOfOrNull { it.x } ?: 4.0
        val minZ = all.minOfOrNull { it.z } ?: -4.0; val maxZ = all.maxOfOrNull { it.z } ?: 4.0
        fun map(point: Vec3) = Offset(
            12f + ((point.x - minX) / (maxX - minX).coerceAtLeast(1.0)).toFloat() * (size.width - 24f),
            12f + ((point.z - minZ) / (maxZ - minZ).coerceAtLeast(1.0)).toFloat() * (size.height - 24f),
        )
        for (i in 1..3) {
            drawLine(Ink.copy(.08f), Offset(size.width * i / 4f, 0f), Offset(size.width * i / 4f, size.height), 1f)
            drawLine(Ink.copy(.08f), Offset(0f, size.height * i / 4f), Offset(size.width, size.height * i / 4f), 1f)
        }
        trail.zipWithNext().forEachIndexed { index, pair -> drawLine(Amber.copy(alpha = .15f + .7f * index / trail.size.coerceAtLeast(1)), map(pair.first), map(pair.second), 2f) }
        solids.forEachIndexed { index, solid ->
            val colliding = collisions.any { index == it.first || index == it.second }
            val color = when { colliding -> Color.Red; index in selected -> Amber; else -> Cyan }
            drawCircle(color.copy(.22f), if (index in selected) 10f else 7f, map(solid.position))
            drawCircle(color, if (index in selected) 5f else 3.5f, map(solid.position))
        }
        drawGraphLabel("TOP - scene", Offset(8f, 20f), Muted)
    }
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
                    .clip(RoundedCornerShape(10.dp))
                    .adaptiveFocusRing(shape = RoundedCornerShape(10.dp))
                    .clickable { expanded = !expanded }
                    .focusable()
                    .padding(7.dp)
                    .semantics { contentDescription = if (expanded) "Collapse quick actions" else "Expand quick actions" },
            )
        }
        AnimatedVisibility(expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                GlowButton("Collapse", onClick = { expanded = false })
                items.forEach { GlowButton(it, onClick = { onClick(it); expanded = false }) }
            }
        }
    }
}

@Composable
internal fun GraphEquationEditor(
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
    onTypingChange: (Boolean) -> Unit,
) {
    val adaptiveProfile = LocalAdaptiveDeviceProfile.current
    val workspaceTop = if (adaptiveProfile.isTelevision) {
        adaptiveProfile.workspacePolicy.topChromeClearance
    } else {
        70.dp
    }
    val selected = functions.firstOrNull { it.id == selectedId }
    val focusManager = LocalFocusManager.current
    val expressionEngine = remember { ExpressionEngine() }
    var isTyping by remember(selectedId) { mutableStateOf(false) }
    var recentExpressions by remember { mutableStateOf<List<String>>(emptyList()) }
    Column(
        modifier
            .padding(top = workspaceTop, start = 8.dp, end = 8.dp)
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
            if (!isTyping) GlowButton("Add equation", icon = "+", iconOnly = true, onClick = onToggleAddMenu)
            Text(
                if (isTyping) "Editing ${selected?.name ?: "equation"}" else "Equations (${functions.size})",
                color = Cyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).clickable(enabled = !isTyping) { onExpandedChange(!expanded) }.padding(7.dp),
            )
            if (!isTyping) selected?.let { Text(it.name, color = graphColor(it.colorKey), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            if (isTyping) {
                GlowButton("Done") {
                    selected?.expression?.takeIf(String::isNotBlank)?.let { expression ->
                        recentExpressions = (listOf(expression) + recentExpressions.filterNot { it == expression }).take(8)
                    }
                    focusManager.clearFocus()
                    isTyping = false
                    onTypingChange(false)
                }
            } else {
                GlowButton(
                    if (expanded) "Collapse ▲" else "Expand ▼",
                    onClick = {
                        focusManager.clearFocus()
                        onTypingChange(false)
                        onExpandedChange(!expanded)
                        if (expanded && addMenuExpanded) onToggleAddMenu()
                    },
                )
            }
        }
        AnimatedVisibility(addMenuExpanded && !isTyping) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                GlowButton("Collapse", onClick = onToggleAddMenu)
                GraphAddKind.entries.forEach { kind -> GlowButton(kind.label, onClick = { onAddKind(kind) }) }
            }
        }
        AnimatedVisibility(expanded) {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                if (functions.isEmpty()) {
                    Text("Tap + to add your first equation.", color = Muted, fontSize = 12.sp)
                } else {
                    if (!isTyping) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                        var editorValue by remember(function.id) {
                            mutableStateOf(
                                TextFieldValue(
                                    text = function.expression,
                                    selection = TextRange(function.expression.length),
                                ),
                            )
                        }
                        LaunchedEffect(function.expression) {
                            if (function.expression != editorValue.text) {
                                editorValue = editorValue.copy(
                                    text = function.expression,
                                    selection = TextRange(
                                        editorValue.selection.start.coerceAtMost(function.expression.length),
                                        editorValue.selection.end.coerceAtMost(function.expression.length),
                                    ),
                                )
                            }
                        }
                        IntentAwareMathValueField(
                            value = editorValue,
                            onValueChange = {
                                editorValue = it
                                onExpressionChange(function.id, it.text)
                            },
                            label = "${function.name} equation",
                            placeholder = "y = sin(x)",
                            singleLine = true,
                            showLegend = false,
                            keyboardContext = MathKeyboardContext.GRAPH_2D,
                            imeAction = ImeAction.Done,
                            onDone = {
                                // Live typing stages the preview; Enter explicitly commits the plot.
                                onExpressionChange(function.id, editorValue.text)
                                editorValue.text.takeIf(String::isNotBlank)?.let { expression ->
                                    recentExpressions = (listOf(expression) + recentExpressions.filterNot { it == expression }).take(8)
                                }
                                focusManager.clearFocus()
                                isTyping = false
                                onTypingChange(false)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = "Edit selected graph equation ${function.name}" },
                            onFocusChange = { focused ->
                                if (isTyping != focused) {
                                    isTyping = focused
                                    onTypingChange(focused)
                                }
                            },
                        )
                        val normalized = remember(editorValue.text) {
                            com.indianservers.aiexplorer.core.MathExpressionNormalizer.normalize(editorValue.text)
                        }
                        val parseMessage = remember(editorValue.text) {
                            if (editorValue.text.isBlank()) "Incomplete - enter an expression"
                            else runCatching {
                                expressionEngine.compile(stripEquation(editorValue.text))
                                "Valid expression"
                            }.getOrElse { "Incomplete or invalid - ${it.message?.take(70) ?: "check notation"}" }
                        }
                        Text(
                            "$parseMessage - recognized as $normalized",
                            color = if (parseMessage.startsWith("Valid")) Green else Amber,
                            fontSize = 10.sp,
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2,
                        )
                        Text(
                            "Press Enter to plot. Use: pi or π - x^2 or x² - × - ÷ - √ - ≤ - ≥",
                            color = Muted,
                            fontSize = 10.sp,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (!isTyping && recentExpressions.isNotEmpty()) {
                            Text("Recent equations", color = Muted, fontSize = 10.sp)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                recentExpressions.take(4).forEach { expression ->
                                    GlowButton(expression.take(24)) { onExpressionChange(function.id, expression) }
                                }
                            }
                        }
                        if (!isTyping) {
                            Text("Smart actions for ${function.name}", color = Muted, fontSize = 10.sp)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                GlowButton(if (function.visible) "Hide" else "Show") { onToggleVisible(function.id) }
                                GlowButton("Color") { onColor(function.id) }
                                GlowButton("Copy") { onDuplicate(function.id) }
                                DestructiveGlowButton("Delete") { onDelete(function.id) }
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
}

@Composable
internal fun DimmedWorkspaceScrim(onDismiss: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = .78f))
            .clickable(onClick = onDismiss),
    )
}

@Composable
internal fun GlassPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val effects = LocalAppVisualEffects.current
    val panelShape = RoundedCornerShape(18.dp)
    val panelBrush = if (effects.enhanced) {
        val accent = if (effects.treatment == AppVisualTreatment.SpectralWireframe) Violet else Cyan
        Brush.linearGradient(
            listOf(
                SurfaceA,
                accent.copy(alpha = effects.surfaceTintAlpha),
                SurfaceB,
            ),
        )
    } else {
        Brush.linearGradient(listOf(SurfaceA, SurfaceB))
    }
    val panelBorder = if (effects.enhanced) {
        Brush.linearGradient(
            listOf(
                Cyan.copy(alpha = effects.borderGlowAlpha),
                Violet.copy(alpha = effects.borderGlowAlpha),
                Cyan.copy(alpha = effects.borderGlowAlpha * .72f),
            ),
        )
    } else {
        Brush.linearGradient(listOf(Color(0x6645CFFF), Color(0x6645CFFF)))
    }
    Column(
        modifier
            .padding(8.dp)
            .clip(panelShape)
            .background(panelBrush)
            .border(1.dp, panelBorder, panelShape)
            .heightIn(max = 560.dp)
            .verticalScroll(rememberScrollState())
            .animateContentSize()
            .padding(11.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

@Composable
internal fun PanelHeader(
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
            text = "X",
            color = Ink,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceB.copy(alpha = .72f))
                .border(1.dp, accent.copy(alpha = .45f), RoundedCornerShape(10.dp))
                .adaptiveFocusRing(shape = RoundedCornerShape(10.dp), focusColor = accent)
                .clickable(onClick = onClose)
                .focusable()
                .padding(horizontal = 12.dp, vertical = 7.dp)
                .semantics { contentDescription = "Close $title" },
        )
    }
}

@Composable
internal fun GlowButton(
    label: String,
    enabled: Boolean = true,
    icon: String = menuIcon(label),
    iconOnly: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val effects = LocalAppVisualEffects.current
    val adaptiveProfile = LocalAdaptiveDeviceProfile.current
    val symbolOnly = label in setOf("↶", "↷", "⋮")
    val visuallyActive = enabled && effects.enhanced && isVisuallyActiveLabel(label)
    val buttonShape = RoundedCornerShape(14.dp)
    val visualFrame = if (effects.enhanced) {
        val frameAlpha = (effects.borderGlowAlpha + if (visuallyActive) effects.activeGlowAlpha else 0f).coerceAtMost(1f)
        Modifier.border(
            1.dp,
            Brush.linearGradient(
                listOf(
                    Cyan.copy(alpha = frameAlpha),
                    Violet.copy(alpha = frameAlpha * .82f),
                ),
            ),
            buttonShape,
        )
    } else {
        Modifier
    }
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = when {
                visuallyActive -> androidx.compose.ui.graphics.lerp(SurfaceB, Cyan, effects.activeGlowAlpha * .32f)
                effects.enhanced -> SurfaceB.copy(alpha = .92f)
                else -> Color(0x99101824)
            },
            contentColor = Ink,
        ),
        shape = buttonShape,
        modifier = modifier
            .heightIn(min = if (adaptiveProfile.isTelevision) adaptiveProfile.minimumTargetSize else 42.dp)
            .then(visualFrame)
            .adaptiveFocusRing(enabled = enabled, shape = buttonShape),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 9.dp, vertical = 5.dp),
    ) {
        TransparentIcon(smartIconKey(icon, label), if (enabled) Cyan else Muted)
        if (!symbolOnly && !iconOnly) {
            Spacer(Modifier.width(5.dp))
            Text(label, fontSize = 12.sp, maxLines = 1)
        }
    }
}

@Composable
internal fun DestructiveGlowButton(label: String, enabled: Boolean = true, icon: String = "×", iconOnly: Boolean = false, onClick: () -> Unit) {
    val red = Color(0xFFFF6688)
    val symbolOnly = label in setOf("×", "-")
    val adaptiveProfile = LocalAdaptiveDeviceProfile.current
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xAA351521), contentColor = red),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .heightIn(min = if (adaptiveProfile.isTelevision) adaptiveProfile.minimumTargetSize else 42.dp)
            .border(1.dp, if (enabled) red.copy(alpha = .72f) else Muted.copy(alpha = .35f), RoundedCornerShape(14.dp))
            .adaptiveFocusRing(enabled = enabled, shape = RoundedCornerShape(14.dp), focusColor = red),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 9.dp, vertical = 5.dp),
    ) {
        TransparentIcon(smartIconKey(icon, label), if (enabled) red else Muted)
        if (!symbolOnly && !iconOnly) {
            Spacer(Modifier.width(5.dp))
            Text(label, fontSize = 12.sp, maxLines = 1)
        }
    }
}

private object MathHomeArtwork {
@Composable
fun Draw(category: String, accent: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(width = 78.dp, height = 54.dp)
            .graphicsLayer { alpha = .72f },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 1.7.dp.toPx()
            val glow = accent.copy(alpha = .2f)
            for (step in 1..3) {
                val y = size.height * step / 4f
                drawLine(glow, Offset(0f, y), Offset(size.width, y), strokeWidth = .7.dp.toPx())
            }
            when (category) {
                "GamifyMaths" -> {
                    val body = Path().apply {
                        moveTo(size.width * .2f, size.height * .7f)
                        quadraticBezierTo(size.width * .25f, size.height * .25f, size.width * .45f, size.height * .38f)
                        lineTo(size.width * .55f, size.height * .38f)
                        quadraticBezierTo(size.width * .75f, size.height * .25f, size.width * .8f, size.height * .7f)
                        quadraticBezierTo(size.width * .7f, size.height * .86f, size.width * .58f, size.height * .64f)
                        lineTo(size.width * .42f, size.height * .64f)
                        quadraticBezierTo(size.width * .3f, size.height * .86f, size.width * .2f, size.height * .7f)
                    }
                    drawPath(body, accent, style = Stroke(width = stroke))
                    drawLine(accent, Offset(size.width * .32f, size.height * .49f), Offset(size.width * .32f, size.height * .65f), strokeWidth = stroke)
                    drawLine(accent, Offset(size.width * .25f, size.height * .57f), Offset(size.width * .39f, size.height * .57f), strokeWidth = stroke)
                    drawCircle(accent, 2.3.dp.toPx(), Offset(size.width * .66f, size.height * .52f))
                    drawCircle(accent, 2.3.dp.toPx(), Offset(size.width * .72f, size.height * .61f))
                }
                "Solve & Calculate" -> {
                    drawRoundRect(accent.copy(.14f), Offset(size.width * .35f, size.height * .05f), Size(size.width * .48f, size.height * .88f), CornerRadius(6.dp.toPx()))
                    drawRoundRect(accent, Offset(size.width * .35f, size.height * .05f), Size(size.width * .48f, size.height * .88f), CornerRadius(6.dp.toPx()), style = Stroke(stroke))
                    drawRect(accent.copy(.3f), Offset(size.width * .42f, size.height * .16f), Size(size.width * .34f, size.height * .18f))
                    repeat(3) { row -> repeat(3) { col ->
                        drawCircle(accent, 1.8.dp.toPx(), Offset(size.width * (.45f + col * .13f), size.height * (.49f + row * .15f)))
                    } }
                }
                "Visual Workspaces" -> {
                    val a = Offset(size.width * .5f, size.height * .12f)
                    val b = Offset(size.width * .78f, size.height * .31f)
                    val c = Offset(size.width * .78f, size.height * .72f)
                    val d = Offset(size.width * .5f, size.height * .9f)
                    val e = Offset(size.width * .22f, size.height * .72f)
                    val f = Offset(size.width * .22f, size.height * .31f)
                    listOf(a to b, b to c, c to d, d to e, e to f, f to a, a to d, f to c, b to e).forEach {
                        drawLine(accent, it.first, it.second, strokeWidth = stroke)
                    }
                }
                "Data & Probability" -> {
                    repeat(4) { index ->
                        val h = size.height * (.22f + index * .14f)
                        drawRect(accent.copy(.38f), Offset(size.width * (.08f + index * .13f), size.height - h), Size(size.width * .08f, h))
                    }
                    val curve = Path().apply {
                        moveTo(size.width * .05f, size.height * .82f)
                        cubicTo(size.width * .28f, size.height * .78f, size.width * .32f, size.height * .12f, size.width * .55f, size.height * .16f)
                        cubicTo(size.width * .72f, size.height * .2f, size.width * .75f, size.height * .76f, size.width * .96f, size.height * .82f)
                    }
                    drawPath(curve, accent, style = Stroke(stroke))
                }
                "Reference & Logic" -> {
                    val book = Path().apply {
                        moveTo(size.width * .12f, size.height * .22f)
                        quadraticBezierTo(size.width * .34f, size.height * .12f, size.width * .5f, size.height * .34f)
                        quadraticBezierTo(size.width * .66f, size.height * .12f, size.width * .88f, size.height * .22f)
                        lineTo(size.width * .88f, size.height * .78f)
                        quadraticBezierTo(size.width * .66f, size.height * .68f, size.width * .5f, size.height * .88f)
                        quadraticBezierTo(size.width * .34f, size.height * .68f, size.width * .12f, size.height * .78f)
                        close()
                    }
                    drawPath(book, accent, style = Stroke(stroke))
                    drawLine(accent, Offset(size.width * .5f, size.height * .34f), Offset(size.width * .5f, size.height * .88f), strokeWidth = stroke)
                }
                "Discover More" -> {
                    val mountains = Path().apply {
                        moveTo(size.width * .04f, size.height * .82f)
                        lineTo(size.width * .31f, size.height * .42f)
                        lineTo(size.width * .47f, size.height * .65f)
                        lineTo(size.width * .67f, size.height * .26f)
                        lineTo(size.width * .96f, size.height * .82f)
                    }
                    drawPath(mountains, accent, style = Stroke(stroke))
                    drawLine(accent, Offset(size.width * .67f, size.height * .26f), Offset(size.width * .67f, size.height * .05f), strokeWidth = stroke)
                    drawLine(accent, Offset(size.width * .67f, size.height * .05f), Offset(size.width * .83f, size.height * .12f), strokeWidth = stroke)
                }
            }
        }
        if (category == "Formulas & Proofs") {
            Text("√x²", color = accent, fontSize = 25.sp, fontWeight = FontWeight.Light)
        }
    }
}
}

@Composable
internal fun TransparentIcon(symbol: String, tint: Color) {
    val effects = LocalAppVisualEffects.current
    val iconShape = RoundedCornerShape(10.dp)
    val iconChrome = if (effects.enhanced) {
        Modifier
            .background(
                Brush.linearGradient(
                    listOf(
                        tint.copy(alpha = effects.surfaceTintAlpha + .08f),
                        SurfaceB.copy(alpha = .72f),
                    ),
                ),
                iconShape,
            )
            .border(1.dp, tint.copy(alpha = effects.borderGlowAlpha), iconShape)
    } else {
        Modifier
            .background(tint.copy(alpha = .10f))
            .border(1.dp, tint.copy(alpha = .32f), iconShape)
    }
    Box(
        Modifier
            .size(28.dp)
            .clip(iconShape)
            .then(iconChrome),
        contentAlignment = Alignment.Center,
    ) {
        val key = smartIconKey(symbol, symbol).lowercase()
        if (key in VisualIconKeys) {
            Canvas(Modifier.size(18.dp)) {
                val stroke = 2.1.dp.toPx()
                val w = size.width
                val h = size.height
                fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
                    drawLine(tint, Offset(w * x1, h * y1), Offset(w * x2, h * y2), stroke, cap = StrokeCap.Round)
                when (key) {
                    "back" -> { line(.72f, .18f, .28f, .50f); line(.28f, .50f, .72f, .82f); line(.32f, .50f, .88f, .50f) }
                    "home" -> { line(.18f, .48f, .50f, .20f); line(.50f, .20f, .82f, .48f); line(.28f, .45f, .28f, .84f); line(.72f, .45f, .72f, .84f); line(.28f, .84f, .72f, .84f); line(.43f, .84f, .43f, .62f); line(.57f, .84f, .57f, .62f) }
                    "menu" -> { line(.22f, .30f, .78f, .30f); line(.22f, .50f, .78f, .50f); line(.22f, .70f, .78f, .70f) }
                    "expand" -> { line(.30f, .62f, .50f, .38f); line(.50f, .38f, .70f, .62f) }
                    "collapse" -> { line(.30f, .38f, .50f, .62f); line(.50f, .62f, .70f, .38f) }
                    "add" -> { line(.50f, .18f, .50f, .82f); line(.18f, .50f, .82f, .50f) }
                    "delete" -> { line(.22f, .28f, .78f, .28f); line(.32f, .38f, .36f, .82f); line(.68f, .38f, .64f, .82f); line(.36f, .82f, .64f, .82f); line(.40f, .20f, .60f, .20f) }
                    "close" -> { line(.28f, .28f, .72f, .72f); line(.72f, .28f, .28f, .72f) }
                    "reset" -> {
                        drawArc(tint, 35f, 270f, false, topLeft = Offset(w * .20f, h * .20f), size = Size(w * .62f, h * .62f), style = Stroke(stroke, cap = StrokeCap.Round))
                        line(.70f, .18f, .84f, .20f); line(.84f, .20f, .80f, .36f)
                    }
                    "save" -> { line(.28f, .18f, .72f, .18f); line(.72f, .18f, .72f, .80f); line(.72f, .80f, .28f, .80f); line(.28f, .80f, .28f, .18f); line(.38f, .18f, .38f, .42f); line(.62f, .18f, .62f, .42f); line(.36f, .62f, .64f, .62f) }
                    "graph" -> { line(.18f, .82f, .82f, .82f); line(.22f, .82f, .22f, .20f); line(.24f, .70f, .44f, .50f); line(.44f, .50f, .58f, .58f); line(.58f, .58f, .80f, .30f) }
                    "2d" -> { line(.26f, .28f, .74f, .28f); line(.74f, .28f, .74f, .76f); line(.74f, .76f, .26f, .76f); line(.26f, .76f, .26f, .28f) }
                    "3d" -> { line(.50f, .16f, .78f, .32f); line(.78f, .32f, .78f, .66f); line(.78f, .66f, .50f, .84f); line(.50f, .84f, .22f, .66f); line(.22f, .66f, .22f, .32f); line(.22f, .32f, .50f, .16f); line(.22f, .32f, .50f, .50f); line(.78f, .32f, .50f, .50f); line(.50f, .50f, .50f, .84f) }
                    "settings" -> { drawCircle(tint, w * .16f, Offset(w * .50f, h * .50f), style = Stroke(stroke)); line(.50f, .12f, .50f, .24f); line(.50f, .76f, .50f, .88f); line(.12f, .50f, .24f, .50f); line(.76f, .50f, .88f, .50f) }
                    "more" -> { drawCircle(tint, w * .055f, Offset(w * .30f, h * .50f)); drawCircle(tint, w * .055f, Offset(w * .50f, h * .50f)); drawCircle(tint, w * .055f, Offset(w * .70f, h * .50f)) }
                    "ar" -> { drawCircle(tint, w * .36f, Offset(w * .50f, h * .50f), style = Stroke(stroke)); line(.50f, .12f, .50f, .28f); line(.50f, .72f, .50f, .88f); line(.12f, .50f, .28f, .50f); line(.72f, .50f, .88f, .50f) }
                    "hide" -> { line(.24f, .26f, .76f, .74f); line(.18f, .50f, .34f, .34f); line(.66f, .66f, .82f, .50f) }
                }
            }
        } else {
            Text(symbol, color = tint, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}

private val VisualIconKeys = setOf("back", "home", "menu", "expand", "collapse", "add", "delete", "close", "reset", "save", "graph", "2d", "3d", "settings", "more", "ar", "hide")

private fun smartIconKey(icon: String, label: String): String {
    val text = "$label $icon".lowercase()
    return when {
        "back" in text || "<" in text -> "back"
        "home" in text || icon == "H" -> "home"
        "menu" in text || "tools" in text -> "menu"
        "collapse" in text -> "collapse"
        label.equals("Open", true) || "open " in text || "expand" in text -> "expand"
        "hide" in text -> "hide"
        "delete" in text || icon == "-" -> "delete"
        "close" in text || icon.equals("x", true) -> "close"
        "add" in text || icon == "+" -> "add"
        "reset" in text || "fit" in text || "center" in text -> "reset"
        "save" in text || "export" in text || "download" in text -> "save"
        "settings" in text -> "settings"
        "graph" in text || "surface" in text -> "graph"
        "3d" in text || "object" in text -> "3d"
        "2d" in text || "shape" in text -> "2d"
        "ar" in text || "camera" in text || "6d" in text -> "ar"
        "more" in text -> "more"
        icon.lowercase() in VisualIconKeys -> icon.lowercase()
        else -> icon
    }
}

@Composable
private fun SyntaxLegend(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(7.dp).clip(RoundedCornerShape(7.dp)).background(color))
        Text(label, color = color, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
    }
}

internal class MathSyntaxVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val builder = AnnotatedString.Builder(text.text)
        MathInputIntelligence.analyze(text.text).tokens.forEach { token ->
            val bracketColors = listOf(Cyan, Violet, Green, Amber)
            val style = when (token.kind) {
                MathInputTokenKind.Command -> SpanStyle(color = Color(0xFFA878FF), fontWeight = FontWeight.ExtraBold)
                MathInputTokenKind.Function -> SpanStyle(color = Violet, fontWeight = FontWeight.Bold)
                MathInputTokenKind.Number -> SpanStyle(color = Cyan, fontWeight = FontWeight.SemiBold)
                MathInputTokenKind.Variable -> SpanStyle(color = Green, fontWeight = FontWeight.Bold)
                MathInputTokenKind.Constant -> SpanStyle(color = Amber, fontWeight = FontWeight.Bold)
                MathInputTokenKind.Unit -> SpanStyle(color = Color(0xFF2DE2C5), fontWeight = FontWeight.Bold)
                MathInputTokenKind.Operator -> SpanStyle(color = Ink)
                MathInputTokenKind.Relation -> SpanStyle(color = Color(0xFFFF8A70), fontWeight = FontWeight.SemiBold)
                MathInputTokenKind.Bracket -> SpanStyle(color = bracketColors[(token.depth - 1).coerceAtLeast(0) % bracketColors.size], fontWeight = FontWeight.ExtraBold)
                MathInputTokenKind.Separator -> SpanStyle(color = Color(0xFFB4C3D8))
                MathInputTokenKind.Keyword -> SpanStyle(color = Color(0xFF79A7FF), fontWeight = FontWeight.SemiBold)
                MathInputTokenKind.Error -> SpanStyle(color = Color(0xFFFF6B7A), background = Color(0x44FF304F), fontWeight = FontWeight.Bold)
                MathInputTokenKind.Text -> SpanStyle(color = Muted)
            }
            builder.addStyle(style, token.start, token.end)
        }
        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}

private fun visualModuleIcon(module: MathModule): String = when (module) {
    MathModule.Geometry2D -> "2d"
    MathModule.Geometry3D -> "3d"
    MathModule.Graph2D -> "graph"
    MathModule.Graph3D -> "graph"
    MathModule.Trigonometry -> "T"
    MathModule.Manipulatives -> "M"
    MathModule.ProbabilityStatistics -> "P"
    MathModule.MatricesLinearTransformations -> "matrix"
    MathModule.DataSpreadsheet -> "table"
    MathModule.DiscreteMathematics -> "sets"
    MathModule.NumberTheory -> "N"
    MathModule.SpatialAR -> "ar"
    MathModule.ARGraph3D -> "ar"
}

private fun moduleIcon(module: MathModule): String = when (module) {
    MathModule.Geometry2D -> "△"
    MathModule.Geometry3D -> "◇"
    MathModule.Graph2D -> "ƒ"
    MathModule.Graph3D -> "⌁"
    MathModule.Trigonometry -> "θ"
    MathModule.Manipulatives -> "▦"
    MathModule.ProbabilityStatistics -> "P"
    MathModule.MatricesLinearTransformations -> "M×"
    MathModule.DataSpreadsheet -> "▤"
    MathModule.DiscreteMathematics -> "∪"
    MathModule.NumberTheory -> "ℕ"
    MathModule.SpatialAR -> "AR"
    MathModule.ARGraph3D -> "AR"
}

internal fun menuIcon(label: String): String = when {
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
    else -> label.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "-"
}

@Composable
internal fun Insight(label: String, value: String, color: Color) {
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
internal fun TogglePill(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
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
internal fun AxisSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValue: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("$label ${trim(value.toDouble())}", color = Muted, modifier = Modifier.width(112.dp))
        Slider(value = value, onValueChange = onValue, valueRange = range, modifier = Modifier.weight(1f))
    }
}


