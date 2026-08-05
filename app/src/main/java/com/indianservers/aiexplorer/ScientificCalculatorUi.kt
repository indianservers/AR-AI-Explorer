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
internal fun ScientificCalculatorScreen(vm: ExplorerViewModel, wide: Boolean) {
    val calculator = remember { ScientificCalculator() }
    val smartCalculator = remember { SmartScientificCalculator(calculator) }
    val professionalCalculator = remember { ProfessionalScientificCalculator() }
    val haptic = LocalHapticFeedback.current
    val editorHistory = remember { CalculatorEditorHistory("sin(30)+log(1000)") }
    var editor by remember { mutableStateOf(TextFieldValue("sin(30)+log(1000)", selection = TextRange(17))) }
    val expression = editor.text
    var angleMode by remember { mutableStateOf(AngleMode.Degrees) }
    var outcome by remember { mutableStateOf<SmartCalculatorOutcome?>(runCatching { smartCalculator.evaluate(expression, angleMode) }.getOrNull()) }
    var evaluatedExpression by remember { mutableStateOf(expression) }
    var error by remember { mutableStateOf<String?>(null) }
    var keyboardLayer by remember { mutableStateOf(CalculatorKeyboardLayer.Basic) }
    var advancedMode by remember { mutableStateOf(AdvancedCalculatorMode.Scientific) }
    var professionalMode by remember { mutableStateOf(ProfessionalCalculatorMode.Matrix) }
    var calculatorPrecision by remember { mutableIntStateOf(8) }
    val advancedCatalog = remember { AdvancedScientificCalculator().examples }
    val professionalCatalog = remember { professionalCalculator.examples }
    val favourites = remember { CalculatorFavourites(listOf("sin(", "cos(", "sqrt(", "pi")) }
    var favouriteKeys by remember { mutableStateOf(favourites.all()) }
    var oneHanded by remember { mutableStateOf(!wide) }
    var calculatorHaptics by remember { mutableStateOf(true) }
    var showRecognition by remember { mutableStateOf(false) }
    var recognitionInput by remember { mutableStateOf("") }
    var recognitionMessage by remember { mutableStateOf<String?>(null) }
    var showHistory by remember { mutableStateOf(false) }
    var showAdvancedTools by remember { mutableStateOf(false) }
    var shiftMode by remember { mutableStateOf(false) }
    var alphaMode by remember { mutableStateOf(false) }
    var conversionValue by remember { mutableStateOf("72") }
    var selectedConversion by remember { mutableStateOf(calculator.conversions.first()) }
    fun setExpression(text: String, cursor: Int = text.length) {
        editorHistory.edit(text); editor = TextFieldValue(text, selection = TextRange(cursor.coerceIn(0, text.length)))
    }
    val appendToken: (String) -> Unit = { token ->
        val edit = CalculatorInputIntelligence.smartInsert(expression, editor.selection.min, editor.selection.max, token)
        setExpression(edit.text, edit.cursor)
    }
    fun evaluate() {
        outcome = runCatching { smartCalculator.evaluate(expression, angleMode, calculatorPrecision) }
            .onSuccess { error = null; evaluatedExpression = expression }
            .onFailure { error = it.message ?: "Expression could not be evaluated" }
            .getOrNull()
    }
    fun moveCursor(delta: Int) {
        val position = (editor.selection.start + delta).coerceIn(0, expression.length)
        editor = editor.copy(selection = TextRange(position))
    }
    fun deleteBackward() {
        val edit = CalculatorInputIntelligence.smartBackspace(expression, editor.selection.min, editor.selection.max)
        setExpression(edit.text, edit.cursor)
    }
    fun toggleCurrentSign() {
        val edit = CalculatorInputIntelligence.toggleSign(expression, editor.selection.min, editor.selection.max)
        setExpression(edit.text, edit.cursor)
    }
    val liveDiagnostics = remember(expression) { CalculatorInputIntelligence.diagnostics(expression) }
    val livePreview = remember(expression, angleMode, calculatorPrecision) {
        if (expression.isBlank() || liveDiagnostics.any { it.error }) null
        else runCatching { calculator.evaluate(CalculatorInputIntelligence.interpret(expression), angleMode, calculatorPrecision) }.getOrNull()
    }
    val shownOutcome = outcome?.takeIf { evaluatedExpression == expression }
    val editorSuggestions = remember(expression, editor.selection.start) { CalculatorInputIntelligence.suggestions(expression, editor.selection.start) }
    fun applySuggestion(suggestion: String) {
        val cursor = editor.selection.start.coerceIn(0, expression.length)
        val prefixStart = expression.substring(0, cursor).indexOfLast { !it.isLetter() } + 1
        val edit = CalculatorInputIntelligence.smartInsert(expression, prefixStart, cursor, suggestion)
        setExpression(edit.text, edit.cursor)
    }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = if (wide) 24.dp else 4.dp, vertical = 6.dp)
            .semantics { contentDescription = "Scientific calculator module" },
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            GlowButton("Back", icon = "←", iconOnly = true, onClick = vm::returnToMathMenu)
            Text("SCIENTIFIC", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Light, letterSpacing = 3.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                GlowButton("History", icon = "↶", iconOnly = true) { showHistory = !showHistory; if (showHistory) showAdvancedTools = true }
                GlowButton("Settings", icon = "⚙", iconOnly = true) { showAdvancedTools = !showAdvancedTools }
            }
        }
        Column(
            Modifier.fillMaxWidth().heightIn(min = 205.dp).shadow(12.dp, RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF152B56), Color(0xFF071426), Color(0xFF07101D))))
                .border(1.dp, Color(0xFF5D9DFF), RoundedCornerShape(28.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Text(
                    angleMode.label.uppercase(), color = Color(0xFF8CB7FF), fontSize = 12.sp,
                    modifier = Modifier.clip(RoundedCornerShape(9.dp)).background(Color(0x334D80D8)).padding(horizontal = 10.dp, vertical = 7.dp),
                )
                BasicTextField(
                    value = editor,
                    onValueChange = { next -> editorHistory.edit(next.text); editor = next },
                    modifier = Modifier.weight(1f).padding(start = 12.dp).semantics { contentDescription = "Smart scientific calculator expression editor" },
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFFA8CAFF), fontSize = if (wide) 24.sp else 19.sp, textAlign = TextAlign.End, fontFamily = FontFamily.Monospace),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(Color(0xFF6BA7FF)),
                    visualTransformation = MathSyntaxVisualTransformation(),
                    singleLine = true,
                )
            }
            Text(
                shownOutcome?.exact?.let { "= $it" } ?: livePreview?.exactHint?.let { "= $it" } ?: "Live preview",
                color = Color(0xFF8296BC), fontSize = 14.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth(),
            )
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x335D9DFF)))
            Text(
                shownOutcome?.primary ?: livePreview?.decimal ?: error ?: "0",
                color = if (liveDiagnostics.none { it.error }) Color(0xFFF4F7FF) else Amber,
                fontSize = if (wide) 60.sp else 50.sp,
                fontWeight = FontWeight.Light,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
            )
            liveDiagnostics.firstOrNull()?.let { diagnostic ->
                Text(diagnostic.message, color = if (diagnostic.error) Amber else Muted, fontSize = 10.sp, maxLines = 1)
            }
        }
        AnimatedVisibility(editorSuggestions.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Complete:", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 9.dp))
                editorSuggestions.forEach { suggestion -> GlowButton(suggestion) { applySuggestion(suggestion) } }
            }
        }
        ScientificReferenceKeypad(
            shiftActive = shiftMode,
            alphaActive = alphaMode,
            onShift = { shiftMode = !shiftMode },
            onAlpha = { alphaMode = !alphaMode },
            onPrevious = { moveCursor(-1) },
            onNext = { moveCursor(1) },
            onMode = { angleMode = if (angleMode == AngleMode.Degrees) AngleMode.Radians else AngleMode.Degrees; evaluate() },
            onToken = { token -> if (calculatorHaptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress); appendToken(token) },
            onClear = { setExpression(""); outcome = null; error = null },
            onDelete = ::deleteBackward,
            onToggleSign = ::toggleCurrentSign,
            onCalculate = ::evaluate,
            onSolve = vm::openProblemSolver,
            answer = shownOutcome?.primary ?: livePreview?.decimal ?: "0",
        )
        AnimatedVisibility(showAdvancedTools) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Professional smart tools: exact maths, matrices, vectors, data, probability, units, programmer and finance workflows.", color = Muted, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            AngleMode.entries.forEach { mode -> GlowButton(if (angleMode == mode) "${mode.label} active" else mode.label, onClick = { angleMode = mode; evaluate() }) }
            GlowButton("Clear") { setExpression(""); outcome = null; error = null }
            GlowButton("Undo") { setExpression(editorHistory.undo()) }
            GlowButton("Redo") { setExpression(editorHistory.redo()) }
            GlowButton(if (showHistory) "Hide history" else "History") { showHistory = !showHistory }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            TogglePill(if (oneHanded) "One-handed" else "Full width", oneHanded) { oneHanded = !oneHanded }
            TogglePill(if (calculatorHaptics) "Haptics on" else "Haptics off", calculatorHaptics) { calculatorHaptics = !calculatorHaptics }
            TogglePill(if (showRecognition) "Hide voice input" else "Voice input", showRecognition) { showRecognition = !showRecognition }
        }
        if (showRecognition) {
            IntentAwareMathField(recognitionInput, { recognitionInput = it }, "Voice transcript", Modifier.fillMaxWidth(), singleLine = false, minLines = 2)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                GlowButton("Normalize voice", enabled = recognitionInput.isNotBlank()) {
                    val recognized = CalculatorRecognitionAdapters.voice(recognitionInput)
                    setExpression(recognized.normalized); recognitionMessage = recognized.warnings.firstOrNull() ?: "Voice text normalized. Confirm it, then calculate."
                }
            }
            recognitionMessage?.let { Text(it, color = Muted, fontSize = 11.sp) }
        }
        IntentAwareMathValueField(
            value = editor, onValueChange = { next -> editorHistory.edit(next.text); editor = next }, label = "Expression",
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Scientific calculator expression input" },
            singleLine = false, minLines = 2,
        )
        val diagnostics = CalculatorInputIntelligence.diagnostics(expression)
        diagnostics.forEach { Text("${it.position?.let { position -> "At ${position + 1}: " }.orEmpty()}${it.message}", color = if (it.error) Amber else Muted, fontSize = 11.sp) }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { CalculatorKeyboardLayer.entries.forEach { layer -> TogglePill(layer.name, keyboardLayer == layer) { keyboardLayer = layer } } }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { AdvancedCalculatorMode.entries.forEach { mode -> TogglePill(mode.name, advancedMode == mode) { advancedMode = mode } } }
        Text("Professional modes", color = Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { ProfessionalCalculatorMode.entries.forEach { mode -> TogglePill(mode.name, professionalMode == mode) { professionalMode = mode } } }
        Row(verticalAlignment = Alignment.CenterVertically) { Text("Precision $calculatorPrecision", color = Muted, modifier = Modifier.width(110.dp)); Slider(calculatorPrecision.toFloat(), { calculatorPrecision = it.roundToInt().coerceIn(2, 12) }, valueRange = 2f..12f, steps = 9, modifier = Modifier.weight(1f)) }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            CalculatorInputIntelligence.templates[keyboardLayer].orEmpty().forEach { template -> GlowButton(template.label, onClick = {
                val start = editor.selection.min; val text = expression.replaceRange(start, editor.selection.max, template.source); setExpression(text, start + template.source.length - template.cursorBack)
            }) }
        }
        val keypadTokens = when (keyboardLayer) {
            CalculatorKeyboardLayer.Basic -> listOf("7", "8", "9", "/", "4", "5", "6", "*", "1", "2", "3", "-", "0", ".", "pi", "+", "(", ")", "^", "%")
            CalculatorKeyboardLayer.Scientific -> listOf("sin(", "cos(", "tan(", "sec(", "csc(", "cot(", "asin(", "acos(", "atan(", "sinh(", "cosh(", "tanh(", "sqrt(", "ln(", "log(", "exp(", "!", "e", "pi", "min(", "max(")
            CalculatorKeyboardLayer.Structural -> listOf("x", "y", "a", "b", "=", ",", "[", "]", "f(x)=", "integrate ", " from ", " to ")
        }
        if (favouriteKeys.isNotEmpty()) {
            Text("Favourite keys", color = Muted, fontSize = 11.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { favouriteKeys.forEach { token -> GlowButton("★ $token") { if (calculatorHaptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress); appendToken(token) } } }
        }
        FlowRow(
            modifier = if (oneHanded) Modifier.widthIn(max = 360.dp).align(Alignment.End) else Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            keypadTokens.forEach { token ->
                GlowButton(token) { if (calculatorHaptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress); appendToken(token) }
            }
        }
        Text("Customize favourite keys", color = Muted, fontSize = 11.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("sin(", "cos(", "tan(", "sqrt(", "ln(", "pi", "x", "integrate ").forEach { token ->
                TogglePill(token, token in favouriteKeys) { favouriteKeys = favourites.toggle(token).toList() }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlowButton("Calculate") { evaluate() }
            GlowButton("Open Solver") { vm.openProblemSolver() }
        }
        Text("${advancedMode.name} examples", color = Cyan, fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            advancedCatalog[advancedMode].orEmpty().forEach { example -> GlowButton(example, onClick = { setExpression(example); outcome = runCatching { smartCalculator.evaluate(example, angleMode, calculatorPrecision) }.getOrNull() }) }
        }
        Text("${professionalMode.name} examples", color = Cyan, fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            professionalCatalog[professionalMode].orEmpty().forEach { example -> GlowButton(example, onClick = { setExpression(example); outcome = runCatching { smartCalculator.evaluate(example, angleMode, calculatorPrecision) }.getOrNull() }) }
        }
        error?.let { Text(it, color = Amber, fontSize = 12.sp) }
        outcome?.let {
            SmartCalculatorResultCard(it)
            Text("Send result", color = Cyan, fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                GlowButton("Graph", enabled = expression.contains("x")) { vm.addFunction(expression); vm.open(MathModule.Graph2D) }
                GlowButton("Solver") { vm.openProblemSolver() }
                GlowButton("Notebook") { vm.submitNotebook(expression); vm.openMathNotebook() }
                GlowButton("Spreadsheet") { vm.openSpreadsheetLab() }
                GlowButton("Probability Lab") { vm.openProbabilityLab() }
            }
        }
        if (showHistory) {
            Text("Editable calculation branches", color = Cyan, fontWeight = FontWeight.Bold)
            smartCalculator.history.asReversed().take(12).forEachIndexed { reverseIndex, entry ->
                val index = smartCalculator.history.lastIndex - reverseIndex
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(Color(0x22101824)).clickable { setExpression(smartCalculator.branchFrom(index)) }.padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${entry.input}  ->  ${entry.primary}", color = Ink, fontFamily = FontFamily.Monospace, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    Text("branch ${entry.branch}", color = Muted, fontSize = 9.sp)
                }
            }
        }
        if (wide) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CalculatorConstantsPanel(calculator, onInsert = appendToken)
                CalculatorConversionsPanel(calculator, conversionValue, { conversionValue = it }, selectedConversion, { selectedConversion = it })
            }
        } else {
            CalculatorConstantsPanel(calculator, onInsert = appendToken)
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
                    MathFormulaText(card.expression, color = Cyan, fontSize = 14.sp)
                }
                Text(card.description, color = Muted, fontSize = 11.sp)
                if (showAdvancedTools) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        card.examples.forEach { example -> GlowButton(example) { setExpression(example); outcome = runCatching { smartCalculator.evaluate(example, angleMode, calculatorPrecision) }.getOrNull() } }
                    }
                }
            }
            }
        }
    }
}

}

@Composable
private fun ScientificReferenceKeypad(
    shiftActive: Boolean,
    alphaActive: Boolean,
    onShift: () -> Unit,
    onAlpha: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onMode: () -> Unit,
    onToken: (String) -> Unit,
    onClear: () -> Unit,
    onDelete: () -> Unit,
    onToggleSign: () -> Unit,
    onCalculate: () -> Unit,
    onSolve: () -> Unit,
    answer: String,
) {
    @Composable
    fun KeyRow(vararg keys: Triple<String, String, Color>) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            keys.forEach { (label, token, accent) ->
                ScientificCalculatorKey(label, accent, Modifier.weight(1f)) {
                    when (token) {
                        "#shift" -> onShift()
                        "#alpha" -> onAlpha()
                        "#left" -> onPrevious()
                        "#right" -> onNext()
                        "#mode" -> onMode()
                        "#clear" -> onClear()
                        "#delete" -> onDelete()
                        "#sign" -> onToggleSign()
                        "#calculate" -> onCalculate()
                        "#solve" -> onSolve()
                        "#answer" -> onToken(answer)
                        else -> onToken(token)
                    }
                }
            }
        }
    }
    val normal = Color(0xFFE1E4EB)
    val operatorBlue = Color(0xFF4595FF)
    val red = Color(0xFFFF5A70)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        KeyRow(
            Triple(if (shiftActive) "SHIFT •" else "SHIFT", "#shift", Color(0xFFFFB52E)),
            Triple(if (alphaActive) "ALPHA •" else "ALPHA", "#alpha", Violet),
            Triple("◀", "#left", normal), Triple("▶", "#right", normal), Triple("MODE", "#mode", normal),
        )
        KeyRow(Triple("CALC", "#calculate", normal), Triple("∫dx", "integrate ", normal), Triple("x!", "!", normal), Triple("(", "(", normal), Triple(")", ")", normal), Triple("AC", "#clear", red))
        KeyRow(Triple("SOLVE", "#solve", normal), Triple("d/dx", "differentiate ", normal), Triple("√", "sqrt(", normal), Triple("xʸ", "^", normal), Triple("log", "log(", normal), Triple("DEL", "#delete", red))
        KeyRow(Triple("sin⁻¹", "asin(", normal), Triple("cos⁻¹", "acos(", normal), Triple("tan⁻¹", "atan(", normal), Triple("log₁₀", "log(", normal), Triple("ln", "ln(", normal), Triple("%", "%", normal))
        KeyRow(Triple("sin", "sin(", normal), Triple("cos", "cos(", normal), Triple("tan", "tan(", normal), Triple("x²", "^2", normal), Triple("x³", "^3", normal), Triple("xʸ", "^", normal))
        KeyRow(Triple("π", "pi", normal), Triple("e", "e", normal), Triple("^", "^", normal), Triple("10ˣ", "10^(", normal), Triple("Ans", "#answer", normal), Triple("EXP", "e^(", normal))
        KeyRow(Triple("7", "7", normal), Triple("8", "8", normal), Triple("9", "9", normal), Triple("÷", "/", operatorBlue), Triple("×", "*", operatorBlue))
        KeyRow(Triple("4", "4", normal), Triple("5", "5", normal), Triple("6", "6", normal), Triple("−", "-", operatorBlue), Triple("+", "+", operatorBlue))
        Row(Modifier.fillMaxWidth().height(118.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Column(Modifier.weight(4f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                KeyRow(Triple("1", "1", normal), Triple("2", "2", normal), Triple("3", "3", normal))
                KeyRow(Triple("0", "0", normal), Triple(".", ".", normal), Triple("±", "#sign", normal))
            }
            ScientificCalculatorKey("=", operatorBlue, Modifier.weight(1f).fillMaxHeight(), onCalculate)
        }
    }
}

@Composable
private fun ScientificCalculatorKey(label: String, accent: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 54.dp).border(1.dp, Color(0xFF334867), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (label == "=") Color(0xFF135EF1) else Color(0xFF111C30),
            contentColor = if (label == "=") Color.White else accent,
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 8.dp),
    ) {
        Text(label, fontSize = if (label.length > 6) 10.sp else 15.sp, maxLines = 1, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SmartCalculatorResultCard(outcome: SmartCalculatorOutcome) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0x5520D9FF)).border(1.dp, Cyan.copy(.45f), RoundedCornerShape(16.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(outcome.primary, color = Ink, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
        outcome.exact?.let { Insight("Exact", it, Amber) }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) { outcome.alternatives.forEach { (label, value) -> Insight(label, value, when (label) { "Scientific" -> Violet; "Engineering" -> Green; else -> Cyan }) } }
        Text("Interpreted: ${outcome.interpretedInput}", color = Muted, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        outcome.diagnostics.forEach { Text(it.message, color = if (it.error) Amber else Muted, fontSize = 11.sp) }
        outcome.steps.take(7).forEachIndexed { index, step -> Text("${index + 1}. $step", color = Muted, fontSize = 11.sp) }
        Text("Verification: ${outcome.verification}", color = Green, fontSize = 11.sp)
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
