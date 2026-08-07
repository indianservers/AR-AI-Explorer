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
internal fun MathKnowledgeScreen(vm: ExplorerViewModel, wide: Boolean) {
    var query by remember { mutableStateOf("") }
    var topic by remember { mutableStateOf<KnowledgeTopic?>(null) }
	    var level by remember { mutableStateOf<KnowledgeLevel?>(null) }
	    var formulaCategory by remember { mutableStateOf<FormulaCategory?>(null) }
    var formulaTag by remember { mutableStateOf<String?>(null) }
    var theoremCategory by remember { mutableStateOf<String?>(null) }
    var dictionaryInitial by rememberSaveable { mutableStateOf<Char?>(null) }
    var dictionaryClassBand by rememberSaveable { mutableStateOf<DictionaryClassBand?>(null) }
    var dictionaryDifficulty by rememberSaveable { mutableStateOf<DictionaryDifficulty?>(null) }
    var answers by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var quizSubject by remember { mutableStateOf(QuizSubject.Maths) }
    var quizLevel by remember { mutableStateOf(QuizLevel.Basic) }
    var quizSession by remember { mutableStateOf<QuizSession?>(null) }
    val visualProofEngine = remember { VisualProofEngine() }
    var visualProofPlayback by remember { mutableStateOf(visualProofEngine.start(VisualProofCatalog.labs.first().id)) }
    var visualProofCategory by remember { mutableStateOf("Geometry") }
    var proofWorkspaceOpen by remember { mutableStateOf(false) }
    var proofCategoriesOpen by remember { mutableStateOf(true) }
    var proofFormulasOpen by remember { mutableStateOf(true) }
    var proofExplanationOpen by remember { mutableStateOf(false) }
    var proofControlsOpen by remember { mutableStateOf(false) }
    var proofResultsOpen by remember { mutableStateOf(false) }
    var proofCompareMode by rememberSaveable { mutableStateOf(false) }
    var proofCompareIds by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var visualProofError by remember { mutableStateOf<String?>(null) }
    var proofZoom by rememberSaveable { mutableFloatStateOf(1f) }
    var knowledgeLoading by remember { mutableStateOf(true) }
    if (vm.activeKnowledgeSection == KnowledgeSection.Visualize) {
        VisualFormulaDiscoveryModule(vm, wide)
        return
    }
    val knowledgeDetailOpen =
        (vm.activeKnowledgeSection == KnowledgeSection.Formulas &&
            formulaCategory != null) ||
            (vm.activeKnowledgeSection == KnowledgeSection.Theorems && theoremCategory != null) ||
            (vm.activeKnowledgeSection == KnowledgeSection.Proofs &&
                (proofWorkspaceOpen || proofFormulasOpen)) ||
            (vm.activeKnowledgeSection == KnowledgeSection.Mcqs && quizSession != null)
    BackHandler(enabled = knowledgeDetailOpen) {
        when {
            vm.activeKnowledgeSection == KnowledgeSection.Proofs && proofWorkspaceOpen -> {
                if (visualProofPlayback.playing) {
                    visualProofPlayback = visualProofEngine.togglePlaying(visualProofPlayback)
                }
                proofWorkspaceOpen = false
                proofFormulasOpen = true
                proofExplanationOpen = false
                proofControlsOpen = false
                proofResultsOpen = false
                visualProofError = null
            }
            vm.activeKnowledgeSection == KnowledgeSection.Proofs && proofFormulasOpen -> {
                proofFormulasOpen = false
                proofCategoriesOpen = true
            }
            vm.activeKnowledgeSection == KnowledgeSection.Formulas && formulaCategory != null -> {
                formulaCategory = null
                formulaTag = null
            }
            vm.activeKnowledgeSection == KnowledgeSection.Theorems && theoremCategory != null -> {
                theoremCategory = null
            }
            vm.activeKnowledgeSection == KnowledgeSection.Mcqs && quizSession != null -> {
                quizSession = null
            }
        }
    }
    LaunchedEffect(vm.activeKnowledgeSection) {
        if (vm.activeKnowledgeSection == KnowledgeSection.Formulas) {
            formulaCategory = null
            formulaTag = null
        }
        if (vm.activeKnowledgeSection == KnowledgeSection.Theorems) {
            theoremCategory = null
        }
        if (vm.activeKnowledgeSection == KnowledgeSection.Proofs) {
            proofCategoriesOpen = true
            proofFormulasOpen = false
            proofWorkspaceOpen = false
        }
    }
    LaunchedEffect(visualProofPlayback.playing) {
        while (visualProofPlayback.playing) {
            delay(850)
            visualProofPlayback = visualProofEngine.next(visualProofPlayback)
        }
    }
	    val result = remember(query, topic, level, formulaCategory, dictionaryInitial, dictionaryClassBand, dictionaryDifficulty, vm.activeKnowledgeSection) {
        if (vm.activeKnowledgeSection == KnowledgeSection.Dictionary) {
            KnowledgeSearchResult(
                formulas = emptyList(),
                theorems = emptyList(),
                visualProofs = emptyList(),
                dictionary = MathDictionaryCatalog.search(query, topic, level, dictionaryInitial, dictionaryClassBand, dictionaryDifficulty),
                mcqs = emptyList(),
            )
        } else {
            MathKnowledgeCatalog.search(query, topic, level, formulaCategory)
        }
	    }
    val formulaResults = remember(result.formulas, formulaTag) {
        result.formulas.filter { formulaTag == null || formulaTag in it.tags }
    }
    val theoremResults = remember(result.theorems, theoremCategory) {
        result.theorems.filter { theoremCategory == null || it.category == theoremCategory }
    }
	    LaunchedEffect(query, topic, level, formulaCategory, formulaTag, theoremCategory, dictionaryInitial, dictionaryClassBand, dictionaryDifficulty, vm.activeKnowledgeSection) {
        knowledgeLoading = true
        // Keep at least one composed frame so a destination/filter change never flashes blank.
        delay(90)
        knowledgeLoading = false
    }

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        GlassPanel(modifier.fillMaxSize().semantics { contentDescription = "Maths knowledge content" }) {
            if (vm.activeKnowledgeSection != KnowledgeSection.Formulas || formulaCategory != null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(vm.activeKnowledgeSection.title, color = Violet, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        if (vm.activeKnowledgeSection == KnowledgeSection.Formulas) {
                            Text(formulaCategory?.label.orEmpty(), color = Muted, fontSize = 10.sp)
                        }
                    }
                    Text(
                        when (vm.activeKnowledgeSection) {
                            KnowledgeSection.Formulas -> "${formulaResults.size} formulas"
                            KnowledgeSection.Theorems -> "${theoremResults.size} theorems"
                            KnowledgeSection.Proofs -> {
                                val count = VisualProofCatalog.labs.count { it.matchesProofQuery(query) }
                                "$count Visual Proofs"
                            }
                            else -> "${result.total} found"
                        },
                        color = Muted,
                        fontSize = 11.sp,
                    )
                }
            }
            if (
                vm.activeKnowledgeSection in setOf(KnowledgeSection.Formulas, KnowledgeSection.Theorems, KnowledgeSection.Proofs) &&
                !(vm.activeKnowledgeSection == KnowledgeSection.Formulas && formulaCategory == null)
            ) {
                KnowledgeSearchField(
                    value = query,
                    label = when (vm.activeKnowledgeSection) {
                        KnowledgeSection.Formulas -> "Search formulas or tags"
                        KnowledgeSection.Theorems -> "Search theorems or tags"
                        else -> "Search Visual Proofs"
                    },
                    onValueChange = { query = it },
                )
            }
            if (knowledgeLoading) {
                KnowledgeLoadingSkeleton(vm.activeKnowledgeSection.title)
	            } else when (vm.activeKnowledgeSection) {
	                KnowledgeSection.Formulas -> when {
                        formulaCategory == null -> FormulaCategoryGallery(
                            formulas = result.formulas,
                            query = query,
                            onQueryChange = { query = it },
                            onOpenCategory = {
                                formulaCategory = it
                                formulaTag = null
                            },
                        )
                        else -> FormulaDirectCategoryLibrary(
                            category = formulaCategory!!,
                            formulas = result.formulas,
                            onBack = { formulaCategory = null },
                            selectedTag = formulaTag,
                            onTag = { formulaTag = it },
                            onOpenWorkspace = { formula ->
                                when (formula.topic) {
                                    KnowledgeTopic.Geometry -> vm.open(
                                        if ("3d" in formula.tags || "volume" in formula.tags) MathModule.Graph3D else MathModule.Geometry2D,
                                    )
                                    KnowledgeTopic.Calculus, KnowledgeTopic.Algebra -> vm.open(MathModule.Graph2D)
                                    KnowledgeTopic.Statistics, KnowledgeTopic.Probability -> vm.openProbabilityLab()
                                }
                            },
                        )
                    }
                KnowledgeSection.Theorems -> if (theoremCategory == null) {
                    TheoremCategoryGallery(
                        categories = theoremCategories,
                        theorems = result.theorems,
                        onOpen = { theoremCategory = it },
                    )
                } else {
                    TheoremCategoryLibrary(
                        category = theoremCategory!!,
                        theorems = theoremResults,
                        onBack = { theoremCategory = null },
                    )
                }
                KnowledgeSection.Visualize -> {
                    result.formulas.take(5).forEach { formula ->
                        KnowledgeCard(
                            formula.title,
                            formula.expression,
                            "Interactive visual formula",
                            "Tap to open a live workspace. Variables: ${formula.variables.joinToString()}",
                            Green,
                        ) {
                            when (formula.topic) {
                                KnowledgeTopic.Geometry -> vm.open(MathModule.Geometry2D)
                                KnowledgeTopic.Calculus, KnowledgeTopic.Algebra -> vm.open(MathModule.Graph2D)
                                KnowledgeTopic.Statistics, KnowledgeTopic.Probability -> vm.openProbabilityLab()
                            }
                        }
                    }
                    result.visualProofs.forEach { proof ->
                        KnowledgeCard(proof.title, proof.invariant, "${proof.workspace.label} · TAP TO EXPLORE", proof.learnerPrompt, Cyan) {
                            vm.openKnowledgeHub(KnowledgeSection.Proofs)
                        }
                    }
                }
                KnowledgeSection.Proofs -> {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Visual Proofs", color = Cyan, fontWeight = FontWeight.Bold)
                            Text("${VisualProofCatalog.categories.size} categories · ${VisualProofCatalog.labs.size} interactive Visual Proofs", color = Green, fontSize = 11.sp)
                        }
                        if (!proofCategoriesOpen) GlowButton("All categories") {
                            proofCategoriesOpen = true
                            proofFormulasOpen = false
                            proofWorkspaceOpen = false
                        }
                    }
                    AnimatedVisibility(proofCategoriesOpen) {
                        VisualProofCategoryPicker(query) { category ->
                            visualProofCategory = category
                            proofCategoriesOpen = false
                            proofFormulasOpen = true
                            proofWorkspaceOpen = false
                        }
                    }
                    AnimatedVisibility(proofFormulasOpen) {
                        VisualProofList(
                            category = visualProofCategory,
                            query = query,
                            compareMode = proofCompareMode,
                            compareIds = proofCompareIds,
                            onCategories = {
                                proofCategoriesOpen = true
                                proofFormulasOpen = false
                            },
                            onCompareMode = { enabled ->
                                proofCompareMode = enabled
                                if (!enabled) proofCompareIds = emptyList()
                            },
                            onCompareSelection = { id ->
                                proofCompareIds = if (id in proofCompareIds) {
                                    proofCompareIds - id
                                } else {
                                    (proofCompareIds + id).takeLast(2)
                                }
                            },
                            onOpen = { lab ->
                                runCatching { visualProofEngine.start(lab.id) }
                                    .onSuccess {
                                        visualProofPlayback = it
                                        visualProofError = null
                                        proofWorkspaceOpen = true
                                        proofFormulasOpen = false
                                        proofExplanationOpen = false
                                        proofControlsOpen = true
                                        proofResultsOpen = true
                                    }
                                    .onFailure {
                                        visualProofError = "This proof could not start. Choose another proof or try again."
                                        proofWorkspaceOpen = false
                                    }
                            }
                        )
                    }
                    visualProofError?.let { message ->
                        Insight("Visual proof unavailable", message, Amber)
                    }
                    if (!proofWorkspaceOpen && !proofFormulasOpen && !proofCategoriesOpen) {
                        Text("Choose a Visual Proof category to continue.", color = Green, fontSize = 12.sp)
                    }
                    AnimatedVisibility(proofWorkspaceOpen) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            val proofCertificate = VisualProofCatalog.certificateFor(visualProofPlayback.frame.lab.id)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("$visualProofCategory › Visual Proof", color = Muted, fontSize = 10.sp)
                                    MathFormulaText(visualProofPlayback.frame.lab.formalResult, color = Cyan, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                                GlowButton("Close proof") { proofWorkspaceOpen = false; proofFormulasOpen = true }
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                GlowButton("Zoom out", enabled = proofZoom > .5f) { proofZoom = (proofZoom - .15f).coerceAtLeast(.5f) }
                                Text("${(proofZoom * 100).toInt()}%", color = Muted, fontSize = 10.sp)
                                GlowButton("Zoom in", enabled = proofZoom < 2.5f) { proofZoom = (proofZoom + .15f).coerceAtMost(2.5f) }
                                GlowButton("100%") { proofZoom = 1f }
                                GlowButton("Fit") { proofZoom = 1f }
                            }
                            InteractiveVisualProofCanvas(visualProofPlayback, zoom = proofZoom) { name, value ->
                                visualProofPlayback = visualProofEngine.setParameter(visualProofPlayback, name, value)
                            }
                            visualProofPlayback.frame.lab.dataSet?.let { dataSet ->
                                Insight(
                                    "Verified dataset",
                                    "${dataSet.title} · ${dataSet.rows.size} observations · ${dataSet.sourceLabel} · ${dataSet.sourceUrl}",
                                    Cyan,
                                )
                            }
                            ProofLearningCycle(visualProofPlayback.frame)
                            KnowledgeCard(visualProofPlayback.frame.lab.title, visualProofPlayback.frame.lab.formalResult, visualProofPlayback.frame.lab.topic, visualProofPlayback.frame.lab.steps[visualProofPlayback.frame.step], Green)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                GlowButton(if (visualProofPlayback.playing) "Pause" else "Play") { visualProofPlayback = visualProofEngine.togglePlaying(visualProofPlayback) }
                                GlowButton("Previous", enabled = visualProofPlayback.frame.step > 0) { visualProofPlayback = visualProofEngine.previous(visualProofPlayback) }
                                GlowButton("Next", enabled = visualProofPlayback.frame.step < visualProofPlayback.frame.lab.steps.lastIndex) { visualProofPlayback = visualProofEngine.advance(visualProofPlayback) }
                                GlowButton("Reveal") { visualProofPlayback = visualProofEngine.reveal(visualProofPlayback) }
                                GlowButton("Reset", icon = "reset") { visualProofPlayback = visualProofEngine.reset(visualProofPlayback) }
                                GlowButton(if (proofExplanationOpen) "Close reasoning" else "Why it works") { proofExplanationOpen = !proofExplanationOpen }
                                GlowButton(if (proofControlsOpen) "Hide controls" else "Show controls") { proofControlsOpen = !proofControlsOpen }
                                GlowButton(if (proofResultsOpen) "Hide results" else "Show results") { proofResultsOpen = !proofResultsOpen }
                            }
                            AnimatedVisibility(proofExplanationOpen) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    KnowledgeCard("Why this proves it", proofCertificate.argument.joinToString(" "), proofCertificate.method, "Assumptions: ${proofCertificate.assumptions.joinToString()}", Violet)
                                    Text("What changes? ${visualProofPlayback.frame.lab.changesPrompt}", color = Cyan, fontSize = 12.sp)
                                    Text("What stays same? ${visualProofPlayback.frame.lab.invariantPrompt}", color = Green, fontSize = 12.sp)
                                }
                            }
                            AnimatedVisibility(proofControlsOpen) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    visualProofPlayback.frame.lab.parameters.forEach { parameter ->
                                        AxisSlider(parameter.name, visualProofPlayback.frame.parameters.getValue(parameter.name).toFloat(), parameter.minimum.toFloat()..parameter.maximum.toFloat()) {
                                            visualProofPlayback = visualProofEngine.setParameter(visualProofPlayback, parameter.name, it.toDouble())
                                        }
                                    }
                                }
                            }
                            AnimatedVisibility(proofResultsOpen) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    visualProofPlayback.frame.measurements.forEach { (name, value) -> Insight(name, trim(value), Violet) }
                                    Insight("Invariant", "${visualProofPlayback.frame.invariant} · residual ${trim(visualProofPlayback.frame.residual)}", if (visualProofPlayback.frame.holds) Green else Amber)
                                }
                            }
                        }
                    }
                }
                KnowledgeSection.Dictionary -> result.dictionary.forEach { term ->
                    DictionaryTermDiagram(term)
                    KnowledgeCard(term.term, term.definition, "${term.topic.label} · ${term.classBands.joinToString { it.label }} · ${term.difficulty.label}", "${term.notation}\n✓ Example: ${term.example}\n✕ Non-example: ${term.nonExample}", Amber)
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
            if (!knowledgeLoading && when (vm.activeKnowledgeSection) {
                    KnowledgeSection.Formulas -> formulaResults.isEmpty()
                    KnowledgeSection.Theorems -> theoremResults.isEmpty()
                    KnowledgeSection.Proofs -> VisualProofCatalog.labs.none { it.matchesProofQuery(query) }
                    else -> result.total == 0
                }
            ) Text("No matches yet. Clear filters or search a broader term.", color = Amber)
        }
    }

    Content(
        Modifier
            .fillMaxSize()
            .padding(
                top = if (wide) 74.dp else 70.dp,
                bottom = if (wide) 72.dp else 70.dp,
                start = if (wide) 12.dp else 0.dp,
                end = if (wide) 12.dp else 0.dp,
            ),
    )
}

@Composable
private fun KnowledgeLoadingSkeleton(section: String) {
    Column(
        Modifier.fillMaxWidth().semantics { contentDescription = "Loading $section content" },
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Preparing $section...", color = Muted, fontSize = 11.sp)
        repeat(3) { index ->
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(SurfaceB.copy(alpha = .55f)).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(Modifier.fillMaxWidth(if (index == 1) .55f else .72f).height(16.dp).clip(RoundedCornerShape(8.dp)).background(Cyan.copy(alpha = .16f)))
                Box(Modifier.fillMaxWidth().height(if (section == KnowledgeSection.Dictionary.title) 58.dp else 12.dp).clip(RoundedCornerShape(7.dp)).background(Ink.copy(alpha = .09f)))
                Box(Modifier.fillMaxWidth(.82f).height(10.dp).clip(RoundedCornerShape(6.dp)).background(Ink.copy(alpha = .07f)))
                Box(Modifier.fillMaxWidth(.64f).height(10.dp).clip(RoundedCornerShape(6.dp)).background(Ink.copy(alpha = .07f)))
            }
        }
    }
}

@Composable
private fun DictionaryTermDiagram(term: DictionaryTerm) {
    val explanation = when (term.term) {
        "Discriminant" -> "A parabola crossing the axis twice: a positive discriminant gives two real roots."
        "Limit" -> "Points from both sides approach the same open target point."
        "Derivative" -> "The tangent line shows the curve's instantaneous slope."
        "Median" -> "The highlighted centre point divides the ordered values."
        "Posterior" -> "Evidence updates a prior probability into a posterior."
        "Eigenvector" -> "The transformation stretches the vector but preserves its direction."
        else -> "A visual explanation of ${term.term}."
    }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(Color(0x44101824)).border(1.dp, Amber.copy(alpha = .35f), RoundedCornerShape(12.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("Picture it", color = Amber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Canvas(
            Modifier.fillMaxWidth().height(82.dp).semantics {
                contentDescription = "${term.term} explanatory diagram: $explanation"
            },
        ) {
            val w = size.width
            val h = size.height
            val grid = Ink.copy(alpha = .08f)
            for (i in 1..5) drawLine(grid, Offset(w * i / 6f, 0f), Offset(w * i / 6f, h), 1f)
            for (i in 1..3) drawLine(grid, Offset(0f, h * i / 4f), Offset(w, h * i / 4f), 1f)
            when (term.term) {
                "Discriminant" -> {
                    drawLine(Ink.copy(.55f), Offset(w * .08f, h * .72f), Offset(w * .92f, h * .72f), 2f)
                    val path = Path().apply {
                        moveTo(w * .18f, h * .18f)
                        quadraticBezierTo(w * .5f, h * 1.16f, w * .82f, h * .18f)
                    }
                    drawPath(path, Cyan, style = Stroke(4f))
                    drawCircle(Green, 6f, Offset(w * .31f, h * .72f))
                    drawCircle(Green, 6f, Offset(w * .69f, h * .72f))
                }
                "Limit" -> {
                    drawLine(Ink.copy(.5f), Offset(w * .5f, h * .12f), Offset(w * .5f, h * .88f), 2f)
                    listOf(.14f, .25f, .35f, .43f, .57f, .65f, .75f, .86f).forEach { x ->
                        val distance = kotlin.math.abs(x - .5f)
                        drawCircle(Cyan.copy(alpha = 1f - distance), 5f, Offset(w * x, h * (.48f + distance * .55f)))
                    }
                    drawCircle(Green, 8f, Offset(w * .5f, h * .48f), style = Stroke(3f))
                }
                "Derivative" -> {
                    val curve = Path().apply {
                        moveTo(w * .08f, h * .78f)
                        cubicTo(w * .34f, h * .78f, w * .48f, h * .15f, w * .9f, h * .26f)
                    }
                    drawPath(curve, Cyan, style = Stroke(4f))
                    drawLine(Green, Offset(w * .25f, h * .82f), Offset(w * .78f, h * .12f), 4f, StrokeCap.Round)
                    drawCircle(Amber, 6f, Offset(w * .5f, h * .49f))
                }
                "Median" -> {
                    drawLine(Cyan, Offset(w * .1f, h * .55f), Offset(w * .9f, h * .55f), 4f, StrokeCap.Round)
                    (1..7).forEach { i ->
                        drawCircle(if (i == 4) Green else Ink, if (i == 4) 8f else 5f, Offset(w * (.1f + i * .1f), h * .55f))
                    }
                    drawLine(Green, Offset(w * .5f, h * .22f), Offset(w * .5f, h * .82f), 3f)
                }
                "Posterior" -> {
                    drawRect(Cyan.copy(.35f), Offset(w * .08f, h * .22f), Size(w * .84f, h * .2f))
                    drawRect(Cyan, Offset(w * .08f, h * .22f), Size(w * .55f, h * .2f))
                    drawRect(Green.copy(.35f), Offset(w * .08f, h * .6f), Size(w * .84f, h * .2f))
                    drawRect(Green, Offset(w * .08f, h * .6f), Size(w * .72f, h * .2f))
                    drawLine(Amber, Offset(w * .67f, h * .38f), Offset(w * .76f, h * .58f), 4f, StrokeCap.Round)
                }
                else -> {
                    drawLine(Ink.copy(.5f), Offset(w * .12f, h * .78f), Offset(w * .88f, h * .22f), 2f)
                    drawLine(Cyan, Offset(w * .18f, h * .72f), Offset(w * .52f, h * .47f), 7f, StrokeCap.Round)
                    drawLine(Green, Offset(w * .52f, h * .47f), Offset(w * .84f, h * .24f), 7f, StrokeCap.Round)
                    drawCircle(Amber, 7f, Offset(w * .52f, h * .47f))
                }
            }
        }
        Text(explanation, color = Muted, fontSize = 10.sp)
    }
}

@Composable
internal fun KnowledgeCard(title: String, body: String, meta: String, detail: String, accent: Color, onClick: (() -> Unit)? = null) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x33101824))
            .border(1.dp, accent.copy(.55f), RoundedCornerShape(14.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick).semantics { contentDescription = "Open interactive $title" } else Modifier)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, color = accent, fontWeight = FontWeight.Bold)
        MathFormulaText(body, color = Ink, fontSize = 16.sp)
        Text(meta, color = Muted, fontSize = 11.sp)
        Text(detail, color = Ink, fontSize = 12.sp)
    }
}
