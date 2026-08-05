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
internal fun GraphCanvas(
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
    parameterValues: Map<String, Double>,
    previewExpression: String?,
    brushInterval: ClosedFloatingPointRange<Double>?,
    sketchPoints: List<Vec2>,
    pinnedTracePoints: List<Vec2>,
    showResidualPlot: Boolean,
    splitComparison: Boolean,
    snapshotExpressions: List<String>,
    snapshotOpacity: Float,
    selectedFunctionId: String?,
    onSelectFunction: (String) -> Unit,
    onClearSelection: () -> Unit,
    onTraceChange: (Double) -> Unit,
    onParameterAChange: (Double) -> Unit,
    onParameterChange: (String, Double) -> Unit,
    onDomainChange: (String, GraphDomainSelection) -> Unit,
    onLabelMove: (String, Offset) -> Unit,
    onCurveDrag: (String, Vec2) -> Unit,
    onCurveDragEnd: (String, Vec2) -> Unit,
    onBrushChange: (Double, Double) -> Unit,
    onSketchChange: (List<Vec2>) -> Unit,
    onSketchEnd: (List<Vec2>) -> Unit,
    onDataPointMove: (Int, Vec2) -> Unit,
    onViewportChange: (GraphViewState) -> Unit,
    onContextMenu: (String?, Vec2) -> Unit,
) {
    val graph = remember { GraphAnalysis() }
    val advancedGraphEngine = remember { AdvancedGraphEngine() }
    val typedGraphEngine = remember { TypedGraphEngine() }
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
            .pointerInput(graphTool, selectedFunctionId, domains, labelOffsets, parameterA, parameterHandleEnabled, parameterValues) {
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
                    val canvasParameterColumns = maxOf(1, ((size.width - 80f) / 92f).toInt())
                    fun canvasParameterAnchor(index: Int) = Offset(80f + (index % canvasParameterColumns) * 92f, 120f + (index / canvasParameterColumns) * 54f)
                    val canvasParameter = if (parameterHandleEnabled) parameterValues.keys.withIndex().minByOrNull { (index, _) ->
                        (canvasParameterAnchor(index) - down.position).getDistance()
                    }?.takeIf { (index, _) -> (canvasParameterAnchor(index) - down.position).getDistance() < 38f }?.value else null
                    val domainSide = selectedDomain?.let { domain ->
                        val left = screen(Vec2(domain.minimum, 0.0)); val right = screen(Vec2(domain.maximum, 0.0))
                        when { (left - down.position).getDistance() < 42f -> -1; (right - down.position).getDistance() < 42f -> 1; else -> 0 }
                    } ?: 0
                    val traceDrag = !labelDrag && !parameterDrag && domainSide == 0 && (graphTool == GraphTool.Trace || (traceScreen != null && (traceScreen - down.position).getDistance() < 44f))
                    val startWorld = world(down.position)
                    val curveDragId = nearestFunction(down.position)?.takeIf { graphTool == GraphTool.Plot && it == selectedFunctionId }
                    val dataPointIndex = if (graphTool == GraphTool.Data) dataPoints.indices.minByOrNull { (screen(dataPoints[it]) - down.position).getDistance() }
                        ?.takeIf { (screen(dataPoints[it]) - down.position).getDistance() < 42f } else null
                    val brushing = graphTool == GraphTool.BrushArea
                    val sketching = graphTool == GraphTool.SketchFit
                    var activeSketch = if (sketching) listOf(startWorld) else emptyList()
                    var latestWorld = startWorld
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
                            if (change != null) latestWorld = world(change.position)
                            if (delta.getDistance() > 0f) {
                                moved = moved || (change!!.position - down.position).getDistance() > 8f
                                if (canvasParameter != null) {
                                    onParameterChange(canvasParameter, ((change!!.position.x - 40f) / (size.width - 80f) * 20f - 10f).toDouble())
                                    gestureMode = GestureMode.Resizing
                                } else if (dataPointIndex != null) {
                                    onDataPointMove(dataPointIndex, world(change!!.position))
                                    gestureMode = GestureMode.Moving
                                } else if (brushing) {
                                    onBrushChange(startWorld.x, world(change!!.position).x)
                                    gestureMode = GestureMode.Resizing
                                } else if (sketching) {
                                    activeSketch = (activeSketch + world(change!!.position)).takeLast(800)
                                    onSketchChange(activeSketch)
                                    gestureMode = GestureMode.Moving
                                } else if (curveDragId != null) {
                                    onCurveDrag(curveDragId, world(change!!.position) - startWorld)
                                    gestureMode = GestureMode.Moving
                                } else if (labelDrag && selectedFunctionId != null) {
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
                    if (moved && curveDragId != null) onCurveDragEnd(curveDragId, latestWorld - startWorld)
                    if (sketching && activeSketch.size >= 3) onSketchEnd(activeSketch)
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
        if (parameterHandleEnabled) parameterValues.entries.forEachIndexed { index, entry ->
            val columns = maxOf(1, ((size.width - 80f) / 92f).toInt())
            val anchor = Offset(80f + (index % columns) * 92f, 120f + (index / columns) * 54f)
            drawCircle(Cyan.copy(.2f), 20f, anchor)
            drawCircle(Cyan, 7f, anchor)
            drawGraphLabel("${entry.key}=${trim(entry.value)}", anchor + Offset(-34f, -28f), Cyan)
        }
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
            val typedDefinition = runCatching { TypedGraphExpressionParser.parse(fn.expression) }.getOrNull()
            if (typedDefinition is TypedGraphExpression.Inequality) {
                val columns = 42; val rows = 42
                val cells = runCatching { typedGraphEngine.sample(typedDefinition, GraphDomain(minX, maxX), GraphDomain(minY, maxY, "y"), parameterValues, 168).inequalityCells }.getOrDefault(emptyList())
                val cellSize = Size(size.width / columns, size.height / rows)
                val fillAlpha = if (selected) .25f else if (selectedFunctionId == null) .14f else .06f
                cells.filter { it.satisfied }.forEach { cell -> drawRect(color.copy(fillAlpha), topLeft = tx(cell.center) - Offset(cellSize.width / 2, cellSize.height / 2), size = cellSize) }
                return@forEachIndexed
            }
            val kind = graph.definitionKind(fn.expression)
            if (typedDefinition is TypedGraphExpression.Implicit) {
                val segments = runCatching { typedGraphEngine.sample(typedDefinition, GraphDomain(minX, maxX), GraphDomain(minY, maxY, "y"), parameterValues, 520).implicitSegments }.getOrDefault(emptyList())
                val curveColor = color.copy(alpha = if (selected || selectedFunctionId == null) 1f else .28f)
                segments.forEach { drawLine(curveColor, tx(it.start), tx(it.end), if (selected) 5.2f else 3.2f, cap = StrokeCap.Round, pathEffect = styleEffect) }
            } else {
                val domain = domains[fn.id]
                val sampleMinimum = max(minX, domain?.minimum ?: minX)
                val sampleMaximum = min(maxX, domain?.maximum ?: maxX)
                val sample = if (sampleMinimum < sampleMaximum && typedDefinition != null) runCatching {
                    typedGraphEngine.sample(typedDefinition, GraphDomain(sampleMinimum, sampleMaximum), GraphDomain(minY, maxY, "y"), parameterValues, 520)
                }.getOrNull() else null
                sample?.curves?.forEach { segment ->
                    segment.points.zipWithNext().forEach { pair ->
                        val logValid = (!axisSettings.xLogarithmic || pair.first.x > 0 && pair.second.x > 0) && (!axisSettings.yLogarithmic || pair.first.y > 0 && pair.second.y > 0)
                        if (logValid) drawLine(color.copy(alpha = if (selected || selectedFunctionId == null) 1f else .28f), tx(pair.first), tx(pair.second), strokeWidth, cap = StrokeCap.Round, pathEffect = styleEffect)
                    }
                }
                if (domain != null && selected) {
                    listOf(domain.minimum to domain.leftClosed, domain.maximum to domain.rightClosed).forEach { (x, closed) ->
                        val y = (typedDefinition as? TypedGraphExpression.Explicit)?.let { runCatching { engine.compile(it.expression).eval(parameterValues + ("x" to x)) }.getOrNull() }
                        if (y?.isFinite() == true) { if (closed) drawCircle(color, 10f, tx(Vec2(x, y))) else drawCircle(color, 10f, tx(Vec2(x, y)), style = Stroke(4f)); drawLine(color.copy(.7f), tx(Vec2(x, 0.0)) + Offset(0f, -18f), tx(Vec2(x, 0.0)) + Offset(0f, 18f), 4f) }
                    }
                }
            }
            val trace = if (kind == GraphDefinitionKind.Explicit) runCatching {
                val y = engine.compile(stripEquation(fn.expression)).eval(parameterValues + ("x" to traceX))
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
        brushInterval?.let { interval ->
            functions.firstOrNull { it.visible && graph.definitionKind(it.expression) == GraphDefinitionKind.Explicit }?.let { function ->
                val compiled = runCatching { engine.compile(stripEquation(function.expression)) }.getOrNull()
                if (compiled != null) {
                    val start = max(minX, interval.start); val end = min(maxX, interval.endInclusive)
                    if (start < end) {
                        val areaPath = Path().apply {
                            val base = tx(Vec2(start, 0.0)); moveTo(base.x, base.y)
                            (0..120).forEach { i ->
                                val x = start + (end - start) * i / 120.0
                                val y = runCatching { compiled.eval(mapOf("x" to x)) }.getOrDefault(Double.NaN)
                                if (y.isFinite()) { val point = tx(Vec2(x, y)); lineTo(point.x, point.y) }
                            }
                            val finish = tx(Vec2(end, 0.0)); lineTo(finish.x, finish.y); close()
                        }
                        drawPath(areaPath, Brush.verticalGradient(listOf(Cyan.copy(.38f), Violet.copy(.12f))))
                        drawGraphLabel("Brushed area", tx(Vec2(start, 0.0)) + Offset(10f, -35f), Cyan)
                    }
                }
            }
        }
        if (sketchPoints.size >= 2) {
            sketchPoints.zipWithNext().forEach { (a, b) -> drawLine(Amber, tx(a), tx(b), 5f, StrokeCap.Round) }
            drawGraphLabel("Release to fit a quadratic", tx(sketchPoints.last()) + Offset(12f, -30f), Amber)
        }
        pinnedTracePoints.forEachIndexed { index, point ->
            val anchor = tx(point)
            drawLine(Green.copy(.45f), Offset(anchor.x, 0f), Offset(anchor.x, size.height), 1.5f)
            drawLine(Green.copy(.45f), Offset(0f, anchor.y), Offset(size.width, anchor.y), 1.5f)
            drawCircle(Green.copy(.24f), 14f, anchor); drawCircle(Green, 5f, anchor)
            drawGraphLabel("P${index + 1} (${trim(point.x)}, ${trim(point.y)})", anchor + Offset(10f, -34f), Green)
        }
        if (splitComparison) {
            drawLine(Amber.copy(.8f), Offset(size.width / 2f, 96f), Offset(size.width / 2f, size.height - 70f), 2f)
            drawGraphLabel("A", Offset(size.width * .25f, 130f), Cyan); drawGraphLabel("B", Offset(size.width * .75f, 130f), Violet)
        }
        if (showResidualPlot && dataPoints.size >= 3) {
            StatisticsEngine.summarize(dataPoints).regression?.let { regression ->
                val top = size.height * .76f
                drawRect(SurfaceA.copy(.88f), Offset(0f, top), Size(size.width, size.height - top))
                drawLine(Muted, Offset(0f, (top + size.height) / 2f), Offset(size.width, (top + size.height) / 2f), 1.5f)
                dataPoints.forEach { point ->
                    val residual = point.y - (regression.slope * point.x + regression.intercept)
                    val anchor = Offset(tx(point).x, (top + size.height) / 2f - residual.toFloat() * scale * .35f)
                    drawCircle(Amber, 5f, anchor)
                }
                drawGraphLabel("Residual plot", Offset(14f, top + 26f), Amber)
            }
        }
        if (graphTool == GraphTool.ComplexPlane) {
            val originPoint = tx(Vec2(0.0, 0.0))
            drawCircle(Violet.copy(.18f), scale, originPoint)
            drawCircle(Violet, scale, originPoint, style = Stroke(2.5f))
            listOf(Vec2(0.0, 1.0) to "i", Vec2(0.0, -1.0) to "−i", Vec2(1.0, 0.0) to "1", Vec2(-1.0, 0.0) to "−1").forEach { (point, label) ->
                drawRadiantPoint(tx(point), Amber, label)
            }
            drawGraphLabel("Complex plane · horizontal Re(z), vertical Im(z)", Offset(18f, 165f), Violet)
        }
        previewExpression?.let { expression ->
            val sample = runCatching { graph.sampleDefinition(expression, minX, maxX, 420) }.getOrNull()
            sample?.points?.zipWithNext()?.forEachIndexed { index, pair -> if (!sample.breaks.contains(index)) drawLine(Amber.copy(.85f), tx(pair.first), tx(pair.second), 3.2f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(14f, 8f))) }
            drawGraphLabel("Transformation preview", Offset(18f, 212f), Amber)
        }
        snapshotExpressions.forEach { expression ->
            val sample = runCatching { graph.sampleDefinition(expression, minX, maxX, 260) }.getOrNull()
            sample?.points?.zipWithNext()?.forEachIndexed { index, pair -> if (!sample.breaks.contains(index)) drawLine(Color.White.copy(snapshotOpacity.coerceIn(.1f, 1f)), tx(pair.first), tx(pair.second), 2f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(7f, 7f))) }
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
            explicit.firstOrNull()?.let { function ->
                runCatching { engine.compile(stripEquation(function.expression)).eval(mapOf("x" to traceX)) }.getOrNull()?.takeIf(Double::isFinite)?.let { y ->
                    val traceScreenY = tx(Vec2(traceX, y)).y
                    drawLine(Cyan.copy(.5f), Offset(0f, traceScreenY), Offset(size.width, traceScreenY), 1.5f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(7f, 7f)))
                }
            }
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
        GraphTool.Plot, GraphTool.Trace, GraphTool.BrushArea, GraphTool.SketchFit, GraphTool.Table, GraphTool.Data, GraphTool.Probability, GraphTool.ComplexPlane -> Unit
    }
}

internal fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGraphLabel(text: String, position: Offset, color: Color) {
    drawRoundRect(SurfaceA, topLeft = position, size = Size(190f, 42f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f))
    drawRoundRect(color.copy(.75f), topLeft = position, size = Size(190f, 42f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f), style = Stroke(1.4f))
    drawTrigText(text, position.x + 12f, position.y + 27f, color)
}

@Composable
internal fun TrigCanvas(
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

internal fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTrigText(text: String, x: Float, y: Float, color: Color) {
    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            this.color = android.graphics.Color.rgb((color.red * 255).toInt(), (color.green * 255).toInt(), (color.blue * 255).toInt())
            textSize = 24f
            isAntiAlias = true
        }
        drawText(text, x, y, paint)
    }
}
