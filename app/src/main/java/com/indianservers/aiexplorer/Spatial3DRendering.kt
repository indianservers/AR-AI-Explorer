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
internal fun Projected3DCanvas(
    modifier: Modifier,
    solids: List<Solid>,
    vectors: List<Vector3D>,
    selectedIndex: Int,
    visibleSolidIndices: Set<Int>,
    selectedVectorIndex: Int,
    rx: Float,
    ry: Float,
    rz: Float,
    zoom: Float,
    cameraPan: Offset,
    transformMode: Transform3DMode,
    showGrid: Boolean,
    gridSize: Float,
    visualMode: SpatialVisualMode,
    solidAppearances: Map<Int, WorkspaceAppearance> = emptyMap(),
    defaultAppearance: WorkspaceAppearance = WorkspaceAppearance(),
    axisStyle: WorkspaceAxisStyle = WorkspaceVisualStyles.Spectral.axes,
    perspective: Boolean,
    selectionMode: Selection3DMode,
    subSelection: SubObjectSelection?,
    sectionEnabled: Boolean,
    sectionPlane: EditableSectionPlane,
    clipSection: Boolean,
    onSelect: (Int) -> Unit,
    onSubSelect: (SubObjectSelection?) -> Unit,
    onSelectVector: (Int) -> Unit,
    onSolidDragStart: (Int) -> Unit,
    onSolidMove: (Int, Vec3) -> Unit,
    onSolidRotate: (Int, Vec3) -> Unit,
    onSolidScale: (Int, Double) -> Unit,
    onSolidAxisScale: (Int, TransformGizmoAxis, Double) -> Unit,
    onSectionPlaneMove: (Double) -> Unit,
    onSolidDragEnd: () -> Unit,
    onSolidDragCancel: () -> Unit,
    onSolidDropDelete: (Int) -> Unit,
    onVectorDragStart: (Int) -> Unit,
    onVectorMove: (Int, Vec3) -> Unit,
    onVectorDragEnd: () -> Unit,
    onVectorDragCancel: () -> Unit,
    onOrbit: (Float, Float) -> Unit,
    onPan: (Offset) -> Unit,
    onZoom: (Float) -> Unit,
    onResetCamera: () -> Unit,
    onEmptyTap: () -> Unit,
    onGestureModeChange: (GestureMode) -> Unit,
) {
    var lastTapAt by remember { mutableStateOf(0L) }
    var lastSubPickAt by remember { mutableStateOf(0L) }
    var activeGizmoAxis by remember { mutableStateOf<TransformGizmoAxis?>(null) }
    var stylusHoverSolid by remember { mutableStateOf<Int?>(null) }
    val visualEffects = LocalAppVisualEffects.current
    val currentSolids by rememberUpdatedState(solids)
    val currentVectors by rememberUpdatedState(vectors)
    val currentRx by rememberUpdatedState(rx)
    val currentRy by rememberUpdatedState(ry)
    val currentRz by rememberUpdatedState(rz)
    val currentZoom by rememberUpdatedState(zoom)
    val currentPan by rememberUpdatedState(cameraPan)
    val currentPerspective by rememberUpdatedState(perspective)
    val currentSelectionMode by rememberUpdatedState(selectionMode)
    val currentSubSelection by rememberUpdatedState(subSelection)
    val currentSectionEnabled by rememberUpdatedState(sectionEnabled)
    val currentClipSection by rememberUpdatedState(clipSection)
    val currentSectionPlane by rememberUpdatedState(sectionPlane)
    val currentSelectedIndex by rememberUpdatedState(selectedIndex)
    val currentVisibleSolidIndices by rememberUpdatedState(visibleSolidIndices)
    val structuredDescription = remember(solids, subSelection, selectedIndex) {
        val selectedHit = subSelection?.let { selection ->
            solids.getOrNull(selection.solidIndex)?.let { solid ->
                val mode = when (selection.mode) { Selection3DMode.Vertex -> SpatialSubObjectType.Vertex; Selection3DMode.Edge -> SpatialSubObjectType.Edge; Selection3DMode.Face -> SpatialSubObjectType.Face; Selection3DMode.Object -> SpatialSubObjectType.Face }
                com.indianservers.aiexplorer.core.SpatialSubObjectHit(selection.solidIndex, mode, selection.index, 0.0, 0.0, subObjectAnchorWorld(solid, selection))
            }
        }
        com.indianservers.aiexplorer.core.SpatialAccessibilityEngine.describe(solids, selectedHit).joinToString(". ") { node -> node.description + ". " + node.measurements.joinToString() }
    }
    Canvas(
        modifier
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && (event.key == Key.Delete || event.key == Key.Backspace)) {
                    onSolidDropDelete(currentSelectedIndex); true
                } else false
            }
            .focusable()
            .pointerInput(solids, rx, ry, rz, zoom, cameraPan, visibleSolidIndices) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val hover = event.changes.firstOrNull { it.type == androidx.compose.ui.input.pointer.PointerType.Stylus && !it.pressed }
                        stylusHoverSolid = hover?.let { change ->
                            val center = Offset(size.width * .52f, size.height * .45f) + cameraPan
                            val scale = 74f * zoom
                            visibleSolidIndices.minByOrNull { index ->
                                val point = project(rotate(solids[index].position, rx, ry, rz), center, scale, perspective)
                                (point - change.position).getDistance()
                            }?.takeIf { index ->
                                val point = project(rotate(solids[index].position, rx, ry, rz), center, scale, perspective)
                                (point - change.position).getDistance() < 120f
                            }
                        }
                    }
                }
            }
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
                    val projectedMeshes = gestureSolids.mapIndexedNotNull { solidIndex, solid ->
                        if (solidIndex !in currentVisibleSolidIndices) return@mapIndexedNotNull null
                        val mesh = SolidMeshFactory.create(solid)
                        ProjectedSpatialMesh(solidIndex, mesh.vertices.map { vertex ->
                            val world = solidLocalToWorld(solid, vertex)
                            val camera = rotate(world, gestureRx, gestureRy, gestureRz)
                            val screen = project(camera, center, scale, currentPerspective)
                            ProjectedSpatialPoint(Vec2(screen.x.toDouble(), screen.y.toDouble()), camera.z, world)
                        }, mesh.edges, mesh.faces)
                    }
                    fun vectorDistance(index: Int, target: Offset): Float {
                        val vector = gestureVectors[index]
                        val a = project(rotate(vector.start, gestureRx, gestureRy, gestureRz), center, scale, currentPerspective)
                        val b = project(rotate(vector.end, gestureRx, gestureRy, gestureRz), center, scale, currentPerspective)
                        return pointSegmentDistance(target, a, b)
                    }
                    fun solidDistance(index: Int, target: Offset): Float {
                        val mesh = projectedMeshes.firstOrNull { it.solidIndex == index } ?: return Float.MAX_VALUE
                        return SpatialSubObjectPicker.pick(
                            listOf(mesh),
                            Vec2(target.x.toDouble(), target.y.toDouble()),
                            SpatialSubObjectType.Face,
                            Double.MAX_VALUE,
                        )?.screenDistance?.toFloat() ?: Float.MAX_VALUE
                    }

                    fun pickSubObject(target: Offset): SubObjectSelection? {
                        val mode = currentSelectionMode
                        if (mode == Selection3DMode.Object) return null
                        val type = when (mode) {
                            Selection3DMode.Vertex -> SpatialSubObjectType.Vertex
                            Selection3DMode.Edge -> SpatialSubObjectType.Edge
                            Selection3DMode.Face -> SpatialSubObjectType.Face
                            Selection3DMode.Object -> return null
                        }
                        val tolerance = when (mode) { Selection3DMode.Vertex -> 28.0; Selection3DMode.Edge -> 22.0; Selection3DMode.Face -> 18.0; Selection3DMode.Object -> 0.0 }
                        val hits = SpatialSubObjectPicker.pickAll(projectedMeshes, Vec2(target.x.toDouble(), target.y.toDouble()), type, tolerance)
                        val now = System.currentTimeMillis()
                        val current = currentSubSelection
                        val currentHit = hits.indexOfFirst { current != null && it.solidIndex == current.solidIndex && it.index == current.index }
                        val hit = if (now - lastSubPickAt < 520L && currentHit >= 0) hits.getOrNull((currentHit + 1) % hits.size.coerceAtLeast(1)) else hits.firstOrNull()
                        lastSubPickAt = now
                        return hit?.let { SubObjectSelection(it.solidIndex, mode, it.index) }
                    }

                    val selectedForGizmo = gestureSolids.getOrNull(currentSelectedIndex)?.takeIf { currentSelectedIndex in currentVisibleSolidIndices }
                    val gizmoHandles = selectedForGizmo?.let { solid -> projectedGizmoHandles(solid, currentSubSelection?.takeIf { it.solidIndex == currentSelectedIndex }, gestureRx, gestureRy, gestureRz, center, scale, currentPerspective) }.orEmpty()
                    val gizmoKind = when (transformMode) { Transform3DMode.Move -> TransformGizmoKind.Move; Transform3DMode.Rotate -> TransformGizmoKind.Rotate; Transform3DMode.Scale -> TransformGizmoKind.Scale }
                    val interactiveGizmoHandles = if (gizmoKind == TransformGizmoKind.Scale) gizmoHandles else gizmoHandles.filter { it.axis != TransformGizmoAxis.Uniform }
                    val gizmoHit = TransformGizmoEngine.hitTest(Vec2(down.position.x.toDouble(), down.position.y.toDouble()), gizmoKind, interactiveGizmoHandles)
                    val sectionHandle = if ((currentSectionEnabled || currentClipSection) && selectedForGizmo != null) projectedSectionHandle(selectedForGizmo, currentSectionPlane, gestureRx, gestureRy, gestureRz, center, scale, currentPerspective) else null
                    val sectionHit = sectionHandle?.let { (it.end - down.position).getDistance() <= 24f || pointSegmentDistance(down.position, it.start, it.end) <= 12f } == true
                    if (gizmoHit != null && !sectionHit) {
                        activeGizmoAxis = gizmoHit.axis
                        onSolidDragStart(currentSelectedIndex)
                    }

                    val subHit = if (gizmoHit == null && !sectionHit) pickSubObject(down.position) else null
                    subHit?.let {
                        onSelect(it.solidIndex)
                        onSubSelect(it)
                    }
                    var vectorIndex = if (gizmoHit == null && !sectionHit && subHit == null && currentSelectionMode == Selection3DMode.Object) gestureVectors.indices.minByOrNull { vectorDistance(it, down.position) }
                        ?.takeIf { vectorDistance(it, down.position) < 42f }
                    else null
                    var solidIndex = if (gizmoHit != null && !sectionHit) currentSelectedIndex else if (!sectionHit && vectorIndex == null) {
                        gestureSolids.indices.filter(currentVisibleSolidIndices::contains).minByOrNull { solidDistance(it, down.position) }
                            ?.takeIf { solidDistance(it, down.position) < 48f }
                    } else null
                    if (currentSelectionMode != Selection3DMode.Object || subHit != null) solidIndex = null
                    vectorIndex?.let {
                        onSelectVector(it)
                        onVectorDragStart(it)
                    }
                    solidIndex?.takeIf { gizmoHit == null }?.let {
                        onSelect(it)
                        onSolidDragStart(it)
                    }
                    onGestureModeChange(when {
                        sectionHit -> GestureMode.Moving
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
                    var multiTouchScale = 1f
                    var multiTouchRotation = 0f
                    var multiTouchPan = Offset.Zero
                    var latestPosition = down.position
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.firstOrNull()?.let { latestPosition = it.position }
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.size >= 2) {
                            val gesturePan = event.calculatePan()
                            val gestureZoom = event.calculateZoom()
                            val gestureRotation = event.calculateRotation()
                            if (solidIndex != null) {
                                multiTouchPan += gesturePan
                                multiTouchScale *= gestureZoom
                                multiTouchRotation += gestureRotation
                                when (transformMode) {
                                    Transform3DMode.Move -> onSolidMove(
                                        solidIndex,
                                        screenDragToWorld(Vec2(multiTouchPan.x.toDouble(), multiTouchPan.y.toDouble()), scale.toDouble(), gestureRx, gestureRy, gestureRz),
                                    )
                                    Transform3DMode.Rotate -> onSolidRotate(
                                        solidIndex,
                                        Vec3(0.0, multiTouchRotation.toDouble(), 0.0),
                                    )
                                    Transform3DMode.Scale -> onSolidScale(solidIndex, multiTouchScale.toDouble())
                                }
                                onGestureModeChange(
                                    when (transformMode) {
                                        Transform3DMode.Move -> GestureMode.Moving
                                        Transform3DMode.Rotate -> GestureMode.Rotating
                                        Transform3DMode.Scale -> GestureMode.Resizing
                                    },
                                )
                            } else {
                                if (!objectCancelled) {
                                    if (vectorIndex != null) onVectorDragCancel()
                                    vectorIndex = null
                                    objectCancelled = true
                                }
                                onPan(gesturePan)
                                onZoom(gestureZoom)
                                onGestureModeChange(if (abs(gestureZoom - 1f) > .002f) GestureMode.Zooming else GestureMode.Panning)
                            }
                            transformed = true
                            event.changes.forEach { it.consume() }
                        } else {
                            val change = event.changes.firstOrNull()
                            val delta = change?.positionChange() ?: Offset.Zero
                            if (delta.getDistance() > 0f) {
                                total += delta
                                moved = moved || total.getDistance() > 8f
                                when {
                                    sectionHit -> {
                                        val handle = requireNotNull(sectionHandle)
                                        val direction = handle.end - handle.start
                                        val length = direction.getDistance().coerceAtLeast(1f)
                                        val unit = direction / length
                                        onSectionPlaneMove(((delta.x * unit.x + delta.y * unit.y) / length).toDouble())
                                    }
                                    vectorIndex != null -> onVectorMove(
                                        vectorIndex,
                                        Vec3((total.x / scale).toDouble(), 0.0, (total.y / scale).toDouble()),
                                    )
                                    solidIndex != null && gizmoHit != null -> when (transformMode) {
                                        Transform3DMode.Move -> onSolidMove(
                                            solidIndex,
                                            TransformGizmoEngine.translationDelta(
                                                Vec2(total.x.toDouble(), total.y.toDouble()),
                                                interactiveGizmoHandles.first { it.axis == gizmoHit.axis },
                                            ),
                                        )
                                        Transform3DMode.Rotate -> onSolidRotate(solidIndex, TransformGizmoEngine.rotationDelta(gizmoHit.axis, (total.x - total.y) * .35))
                                        Transform3DMode.Scale -> {
                                            val factor = (1.0 + (total.x - total.y) / 260.0).coerceAtLeast(.2)
                                            if (gizmoHit.axis == TransformGizmoAxis.Uniform) onSolidScale(solidIndex, factor) else onSolidAxisScale(solidIndex, gizmoHit.axis, factor)
                                        }
                                    }
                                    solidIndex != null -> when (transformMode) {
                                        Transform3DMode.Move -> onSolidMove(
                                            solidIndex,
                                            screenDragToWorld(Vec2(total.x.toDouble(), total.y.toDouble()), scale.toDouble(), gestureRx, gestureRy, gestureRz),
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

                    if (solidIndex != null) {
                        val overDelete = latestPosition.x in (size.width * .32f)..(size.width * .68f) && latestPosition.y >= size.height * .78f
                        if (overDelete) {
                            onSolidDragCancel()
                            onSolidDropDelete(solidIndex)
                        } else onSolidDragEnd()
                    }
                    activeGizmoAxis = null
                    if (vectorIndex != null) onVectorDragEnd()
                    if (!moved && !transformed && solidIndex == null && vectorIndex == null && subHit == null && !sectionHit && gizmoHit == null) {
                        onEmptyTap()
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
            .semantics { contentDescription = "Interactive 3D workspace with object, vertex, edge and face selection. $structuredDescription" },
    ) {
        val center = Offset(size.width * .52f, size.height * .45f) + cameraPan
        if (showGrid) drawPerspectiveGrid(center, gridSize, visualEffects, axisStyle)
        vectors.forEachIndexed { index, vector ->
            val vectorColor = if (index == selectedVectorIndex) {
                WorkspaceVisualStyles.ReferenceYellow
            } else {
                WorkspaceVisualStyles.spectralColor(index)
            }
            drawVector3D(vector, rx, ry, rz, center, 74f * zoom, vectorColor, index == selectedVectorIndex, perspective)
        }
        solids.forEachIndexed { index, solid ->
            if (index !in visibleSolidIndices) return@forEachIndexed
            val appearance = solidAppearances[index] ?: defaultAppearance.copy(colorIndex = index)
            val color = if (index == stylusHoverSolid) WorkspaceVisualStyles.ReferenceYellow else appearance.color
            drawSolidProjection(
                solid, solid.position, rx, ry, rz, center, 74f * zoom, color, visualMode, index == selectedIndex,
                perspective, subSelection?.takeIf { it.solidIndex == index }, sectionEnabled && index == selectedIndex,
                sectionPlane, clipSection && index == selectedIndex, appearance,
            )
            if (index == stylusHoverSolid) {
                val hover = project(rotate(solid.position, rx, ry, rz), center, 74f * zoom, perspective)
                drawGraphLabel("Stylus preview · ${solid.type.name}", hover + Offset(14f, -22f), Amber)
            }
            if (index == selectedIndex) {
                val handles = projectedGizmoHandles(solid, subSelection?.takeIf { it.solidIndex == index }, rx, ry, rz, center, 74f * zoom, perspective)
                drawTransformGizmo(handles, transformMode, activeGizmoAxis)
                if (sectionEnabled || clipSection) projectedSectionHandle(solid, sectionPlane, rx, ry, rz, center, 74f * zoom, perspective)?.let { handle ->
                    drawLine(Amber, handle.start, handle.end, 4f, cap = StrokeCap.Round)
                    drawCircle(Color.White, 9f, handle.end)
                    drawGraphLabel("drag plane", handle.end + Offset(10f, -10f), Amber)
                }
            }
        }
    }
}

@Composable
internal fun SurfaceCanvas3D(
    modifier: Modifier,
    expression: String,
    mesh: com.indianservers.aiexplorer.core.SurfaceMesh?,
    appearance: WorkspaceAppearance = WorkspaceAppearance(),
    additionalMeshes: List<StyledSurfaceMesh>,
    axisStyle: WorkspaceAxisStyle = appearance.palette.axes,
    surfaceOpacity: Float,
    selectableMeshes: List<Pair<Int, com.indianservers.aiexplorer.core.SurfaceMesh>>,
    gradientPath: List<Vec3>,
    gradientPathIndex: Int,
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
    onSelectSurface: (Int) -> Unit,
) {
    val engine = remember { ExpressionEngine() }
    val calculus = remember { SurfaceCalculus() }
    val visualEffects = LocalAppVisualEffects.current
    val analysis = remember(expression, trace) { runCatching { calculus.analyze(expression, trace.x, trace.y) }.getOrNull() }
    var lastTapAt by remember { mutableStateOf(0L) }
    val currentZoom by rememberUpdatedState(zoom)
    val currentPan by rememberUpdatedState(cameraPan)
    val currentMesh by rememberUpdatedState(mesh)
    val currentRotation by rememberUpdatedState(rotation)
    val currentTilt by rememberUpdatedState(tilt)
    val currentRoll by rememberUpdatedState(roll)
    val currentAnalysis by rememberUpdatedState(analysis)
    val currentSelectableMeshes by rememberUpdatedState(selectableMeshes)
    val currentOnSelectSurface by rememberUpdatedState(onSelectSurface)
    Canvas(
        modifier
            .pointerInput(activeTool) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val center = Offset(size.width * .5f, size.height * .52f) + currentPan
                    val scale = 82f * currentZoom
                    fun screen(value: Vec3) = project(rotate(value, currentTilt, currentRotation, currentRoll), center, scale)
                    val projected = currentMesh?.vertices?.map { value -> screen(value).let { Vec2(it.x.toDouble(), it.y.toDouble()) } }.orEmpty()
                    fun surfaceAt(position: Offset, tolerance: Double = 52.0): Vec2? = currentMesh?.let { surfaceMesh ->
                        SurfaceAnalysisHandleEngine.pick(surfaceMesh, projected, Vec2(position.x.toDouble(), position.y.toDouble()), tolerance)?.let { Vec2(it.x, it.y) }
                    }
                    val handleScreen = currentAnalysis?.point?.let(::screen)
                    val draggingHandle = handleScreen?.let { (it - down.position).getDistance() <= 34f } == true
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
                                if (draggingHandle || activeTool in setOf(SurfaceTool.Trace, SurfaceTool.Gradient)) {
                                    surfaceAt(change!!.position)?.let(onTrace)
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
                            if (activeTool in setOf(SurfaceTool.Trace, SurfaceTool.Gradient)) {
                                surfaceAt(down.position)?.let(onTrace)
                            } else {
                                currentSelectableMeshes
                                    .mapNotNull { (index, selectableMesh) ->
                                        selectableMesh.vertices.minOfOrNull { vertex ->
                                            (screen(vertex) - down.position).getDistance()
                                        }?.let { distance -> index to distance }
                                    }
                                    .minByOrNull { it.second }
                                    ?.takeIf { it.second <= 48f }
                                    ?.let { currentOnSelectSurface(it.first) }
                            }
                            lastTapAt = now
                        }
                    }
                }
            }
            .semantics { contentDescription = "Interactive 3D graph: tap a surface to select it, drag to orbit, two fingers pan, pinch zoom, and twist roll" },
    ) {
        val center = Offset(size.width * .5f, size.height * .52f) + cameraPan
        val scale = 82f * zoom
        fun map(v: Vec3) = project(rotate(v, tilt, rotation, roll), center, scale)
        drawPerspectiveGrid(center, effects = visualEffects, axisStyle = axisStyle)
        drawCoordinatePlanes3D(::map, axisStyle)
        if (showBox) drawSurfaceBox(::map, axisStyle)
        val meshRows = mesh?.vertices?.chunked(mesh.columns).orEmpty()
        drawStyledSurface(meshRows, ::map, appearance, surfaceOpacity)
        meshRows.forEachIndexed { rowIndex, row ->
            val rowColor = appearance.palette.sample(rowIndex.toFloat() / meshRows.lastIndex.coerceAtLeast(1))
            row.zipWithNext().forEachIndexed { columnIndex, (a, b) ->
                val textureVisible = appearance.texture == WorkspaceTexture.Mesh ||
                    (appearance.texture == WorkspaceTexture.Contour && rowIndex % 3 == 0) ||
                    (appearance.texture == WorkspaceTexture.Faceted && (rowIndex + columnIndex) % 2 == 0)
                val alpha = (if (showWireframe || textureVisible) .78f else .18f) * surfaceOpacity
                if (appearance.glow && (showWireframe || textureVisible)) {
                    drawLine(rowColor.copy(alpha = alpha * .16f), map(a), map(b), 7f)
                }
                drawLine(rowColor.copy(alpha = alpha), map(a), map(b), if (showWireframe) 1.8f else 1.05f)
            }
        }
        val meshColumns = mesh?.vertices?.groupBy { it.y }?.values.orEmpty()
        meshColumns.forEachIndexed { index, col ->
            val columnColor = appearance.palette.sample(1f - index.toFloat() / (meshColumns.size - 1).coerceAtLeast(1))
            col.zipWithNext().forEach { (a, b) ->
                val alpha = (if (showWireframe || index % 3 == 0) .58f else .22f) * surfaceOpacity
                drawLine(columnColor.copy(alpha = alpha), map(a), map(b), if (showWireframe) 1.3f else .9f)
            }
        }
        additionalMeshes.forEach { styled ->
            val rows = styled.mesh.vertices.chunked(styled.mesh.columns)
            drawStyledSurface(rows, ::map, styled.appearance, styled.opacity)
            rows.forEachIndexed { rowIndex, row ->
                val color = styled.appearance.palette.sample(rowIndex.toFloat() / rows.lastIndex.coerceAtLeast(1))
                row.zipWithNext().forEach { (a, b) -> drawLine(color.copy(alpha = styled.opacity * .68f), map(a), map(b), 1.35f) }
            }
        }
        if (gradientPath.size >= 2) {
            val visiblePath = gradientPath.take((gradientPathIndex + 1).coerceAtLeast(2))
            visiblePath.zipWithNext().forEach { (a, b) -> drawLine(Green, map(a), map(b), 4f, cap = StrokeCap.Round) }
            visiblePath.lastOrNull()?.let { drawRadiantPoint(map(it), Green, "gradient path") }
        }
        if (showContours) drawSurfaceContours(mesh, ::map)
        if (showSlice) drawSurfaceSlice(mesh, sliceZ, ::map)
        val compiled = runCatching { engine.compile(stripEquation(expression).replace("y", "yy")) }.getOrNull()
        compiled?.takeIf { activeTool in setOf(SurfaceTool.Trace, SurfaceTool.Gradient) || showGradient }?.let {
            val z = runCatching { it.eval(mapOf("x" to trace.x, "yy" to trace.y)) }.getOrDefault(Double.NaN)
            if (z.isFinite()) {
                val point = Vec3(trace.x, trace.y, z.coerceIn(-8.0, 8.0))
                val screenPoint = map(point)
                drawRadiantPoint(screenPoint, Amber, "(${trim(trace.x)}, ${trim(trace.y)}, ${trim(z)})")
                if (showGradient) analysis?.let { differential -> drawSurfaceAnalysisHandle(differential, ::map) }
            }
        }
    }
}

private fun WorkspacePalette.sample(fraction: Float): Color {
    if (colors.size == 1) return colors.first()
    val position = fraction.coerceIn(0f, 1f) * colors.lastIndex
    val lower = position.toInt().coerceIn(0, colors.lastIndex)
    val upper = (lower + 1).coerceAtMost(colors.lastIndex)
    return androidx.compose.ui.graphics.lerp(colors[lower], colors[upper], position - lower)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStyledSurface(
    rows: List<List<Vec3>>,
    map: (Vec3) -> Offset,
    appearance: WorkspaceAppearance,
    opacity: Float,
) {
    if (rows.size < 2) return
    val zValues = rows.flatten().map(Vec3::z)
    val zMin = zValues.minOrNull() ?: return
    val zRange = ((zValues.maxOrNull() ?: zMin) - zMin).coerceAtLeast(1e-9)
    val materialAlpha = when (appearance.material) {
        com.indianservers.aiexplorer.core.SpatialMaterial.Matte -> .46f
        com.indianservers.aiexplorer.core.SpatialMaterial.Gloss -> .62f
        com.indianservers.aiexplorer.core.SpatialMaterial.Metal -> .72f
        com.indianservers.aiexplorer.core.SpatialMaterial.Glass -> .24f
        com.indianservers.aiexplorer.core.SpatialMaterial.XRay -> .12f
    } * opacity
    rows.zipWithNext().forEachIndexed { rowIndex, (top, bottom) ->
        val width = min(top.size, bottom.size)
        for (column in 0 until width - 1) {
            val values = listOf(top[column], top[column + 1], bottom[column + 1], bottom[column])
            val points = values.map(map)
            val normalizedHeight = (((values.sumOf(Vec3::z) / values.size) - zMin) / zRange).toFloat()
            val sweep = (
                normalizedHeight * .62f +
                    rowIndex.toFloat() / rows.lastIndex.coerceAtLeast(1) * .23f +
                    column.toFloat() / (width - 1).coerceAtLeast(1) * .15f
                ).mod(1f)
            val color = appearance.palette.sample(sweep)
            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
                close()
            }
            if (appearance.glow) drawPath(path, color.copy(alpha = materialAlpha * .12f))
            val highlight = when (appearance.material) {
                com.indianservers.aiexplorer.core.SpatialMaterial.Gloss -> Color.White.copy(alpha = .22f * opacity)
                com.indianservers.aiexplorer.core.SpatialMaterial.Metal -> Color.White.copy(alpha = .15f * opacity)
                else -> color.copy(alpha = materialAlpha * .45f)
            }
            drawPath(
                path,
                Brush.linearGradient(
                    listOf(color.copy(alpha = materialAlpha), highlight),
                    start = points.first(),
                    end = points[2],
                ),
            )
            if (appearance.texture == WorkspaceTexture.Faceted) {
                drawPath(path, color.copy(alpha = .42f * opacity), style = Stroke(.8f))
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCoordinatePlanes3D(
    map: (Vec3) -> Offset,
    axes: WorkspaceAxisStyle,
) {
    fun plane(points: List<Vec3>, color: Color) {
        val projected = points.map(map)
        val path = Path().apply {
            moveTo(projected.first().x, projected.first().y)
            projected.drop(1).forEach { lineTo(it.x, it.y) }
            close()
        }
        drawPath(path, color.copy(alpha = .018f))
        drawPath(path, color.copy(alpha = .10f), style = Stroke(1f))
    }
    plane(listOf(Vec3(-3.0, -3.0, 0.0), Vec3(3.0, -3.0, 0.0), Vec3(3.0, 3.0, 0.0), Vec3(-3.0, 3.0, 0.0)), axes.z)
    plane(listOf(Vec3(-3.0, 0.0, -1.0), Vec3(3.0, 0.0, -1.0), Vec3(3.0, 0.0, 6.0), Vec3(-3.0, 0.0, 6.0)), axes.y)
    plane(listOf(Vec3(0.0, -3.0, -1.0), Vec3(0.0, 3.0, -1.0), Vec3(0.0, 3.0, 6.0), Vec3(0.0, -3.0, 6.0)), axes.x)

    val origin = map(Vec3(0.0, 0.0, 0.0))
    val x = map(Vec3(3.7, 0.0, 0.0))
    val y = map(Vec3(0.0, 3.7, 0.0))
    val z = map(Vec3(0.0, 0.0, 4.8))
    drawLine(axes.x.copy(alpha = .18f), origin, x, 12f, cap = StrokeCap.Round)
    drawLine(axes.y.copy(alpha = .18f), origin, y, 12f, cap = StrokeCap.Round)
    drawLine(axes.z.copy(alpha = .18f), origin, z, 12f, cap = StrokeCap.Round)
    drawLine(axes.x, origin, x, 4f, cap = StrokeCap.Round)
    drawLine(axes.y, origin, y, 4f, cap = StrokeCap.Round)
    drawLine(axes.z, origin, z, 4f, cap = StrokeCap.Round)
    drawCircle(Color.White, 5f, origin)
    val plateColor = if (axes.label.red + axes.label.green + axes.label.blue < 1.5f) {
        Color.White.copy(alpha = .88f)
    } else {
        SurfaceA
    }
    drawGraphLabel("X", x + Offset(8f, 0f), axes.label, plateColor, axes.x)
    drawGraphLabel("Y", y + Offset(8f, 0f), axes.label, plateColor, axes.y)
    drawGraphLabel("Z", z + Offset(8f, 0f), axes.label, plateColor, axes.z)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSurfaceBox(
    map: (Vec3) -> Offset,
    axes: WorkspaceAxisStyle,
) {
    val corners = listOf(
        Vec3(-3.0, -3.0, -1.0), Vec3(3.0, -3.0, -1.0), Vec3(3.0, 3.0, -1.0), Vec3(-3.0, 3.0, -1.0),
        Vec3(-3.0, -3.0, 7.0), Vec3(3.0, -3.0, 7.0), Vec3(3.0, 3.0, 7.0), Vec3(-3.0, 3.0, 7.0),
    )
    listOf(0 to 1, 1 to 2, 2 to 3, 3 to 0, 4 to 5, 5 to 6, 6 to 7, 7 to 4, 0 to 4, 1 to 5, 2 to 6, 3 to 7).forEach { (a, b) ->
        drawLine(axes.grid.copy(alpha = .45f), map(corners[a]), map(corners[b]), 1.6f)
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

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSurfaceAnalysisHandle(
    differential: com.indianservers.aiexplorer.core.SurfaceDifferential,
    map: (Vec3) -> Offset,
) {
    val point = differential.point
    val fx = differential.gradient.x; val fy = differential.gradient.y
    val tangentX = Vec3(1.0, 0.0, fx).normalized()
    val tangentY = Vec3(0.0, 1.0, fy).normalized()
    val extent = .62
    val corners = listOf(
        point - tangentX * extent - tangentY * extent,
        point + tangentX * extent - tangentY * extent,
        point + tangentX * extent + tangentY * extent,
        point - tangentX * extent + tangentY * extent,
    ).map(map)
    val plane = Path().apply { moveTo(corners[0].x, corners[0].y); corners.drop(1).forEach { lineTo(it.x, it.y) }; close() }
    drawPath(plane, Violet.copy(.18f)); drawPath(plane, Violet.copy(.78f), style = Stroke(2.5f))
    val normalEnd = point + differential.unitNormal * .9
    drawLine(Cyan, map(point), map(normalEnd), 4f, cap = StrokeCap.Round)
    drawCircle(Cyan, 8f, map(normalEnd)); drawGraphLabel("normal", map(normalEnd) + Offset(8f, -8f), Cyan)
    val gradientMagnitude = hypot(fx, fy)
    if (gradientMagnitude > 1e-10) {
        val dx = fx / gradientMagnitude; val dy = fy / gradientMagnitude
        val gradientEnd = point + Vec3(dx, dy, fx * dx + fy * dy).normalized() * .95
        drawLine(Green, map(point), map(gradientEnd), 5f, cap = StrokeCap.Round)
        drawCircle(Green, 9f, map(gradientEnd)); drawGraphLabel("∇f", map(gradientEnd) + Offset(8f, 14f), Green)
    }
    drawCircle(Amber.copy(.22f), 28f, map(point)); drawCircle(Color.White, 6f, map(point))
}

internal fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGrid(
    origin: Offset,
    scale: Float,
    settings: GraphAxisSettings = GraphAxisSettings(),
    effects: AppVisualEffects = AppVisualEffects.Standard,
    axisStyle: WorkspaceAxisStyle = WorkspaceVisualStyles.Spectral.axes,
) {
    if (!scale.isFinite() || scale <= 0f) return
    val minX = (-origin.x / scale).toDouble()
    val maxX = ((size.width - origin.x) / scale).toDouble()
    val minY = ((origin.y - size.height) / scale).toDouble()
    val maxY = (origin.y / scale).toDouble()
    val xTicks = GraphViewport.ticks(minX, maxX)
    val yTicks = GraphViewport.ticks(minY, maxY)
    xTicks.forEach { value ->
        val x = origin.x + value.toFloat() * scale
        if (settings.gridVisible || value == 0.0) {
            if (value == 0.0 && effects.enhanced) drawLine(axisStyle.y.copy(alpha = effects.gridGlowAlpha), Offset(x, 0f), Offset(x, size.height), 7f)
            val gridColor = if (value == 0.0) axisStyle.y.copy(.9f) else axisStyle.grid
            drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), if (value == 0.0) 2f else 1f)
        }
    }
    yTicks.forEach { value ->
        val y = origin.y - value.toFloat() * scale
        if (settings.gridVisible || value == 0.0) {
            if (value == 0.0 && effects.enhanced) drawLine(axisStyle.x.copy(alpha = effects.gridGlowAlpha), Offset(0f, y), Offset(size.width, y), 7f)
            val gridColor = if (value == 0.0) axisStyle.x.copy(.9f) else axisStyle.grid
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), if (value == 0.0) 2f else 1f)
        }
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

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPerspectiveGrid(
    center: Offset,
    spacingScale: Float = 1f,
    effects: AppVisualEffects = AppVisualEffects.Standard,
    axisStyle: WorkspaceAxisStyle = WorkspaceVisualStyles.Spectral.axes,
) {
    val safeScale = spacingScale.coerceIn(.5f, 3f)
    val gridColor = if (effects.enhanced) axisStyle.grid.copy(alpha = effects.gridGlowAlpha * .8f) else axisStyle.grid
    for (i in -8..8) {
        drawLine(gridColor, Offset(center.x + i * 48f * safeScale, center.y - 330f), Offset(center.x + i * 78f * safeScale, center.y + 330f), 1f)
        drawLine(gridColor, Offset(center.x - 420f, center.y + i * 34f * safeScale), Offset(center.x + 420f, center.y + i * 34f * safeScale), 1f)
    }
    if (effects.enhanced) {
        drawLine(axisStyle.x.copy(alpha = effects.gridGlowAlpha), center, center + Offset(260f, 110f), 8f)
        drawLine(axisStyle.y.copy(alpha = effects.gridGlowAlpha), center, center + Offset(-220f, 140f), 8f)
        drawLine(axisStyle.z.copy(alpha = effects.gridGlowAlpha), center, center + Offset(0f, -260f), 8f)
    }
    drawLine(axisStyle.x, center, center + Offset(260f, 110f), 3f)
    drawLine(axisStyle.y, center, center + Offset(-220f, 140f), 3f)
    drawLine(axisStyle.z, center, center + Offset(0f, -260f), 3f)
}

internal fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRadiantPoint(position: Offset, color: Color, label: String) {
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

internal fun androidx.compose.ui.graphics.drawscope.DrawScope.drawConstructionPreview(
    pending: List<Vec2>,
    tool: GeometryTool,
    tx: (Vec2) -> Offset,
) {
    if (pending.isEmpty()) return
    pending.forEachIndexed { index, point -> drawRadiantPoint(tx(point), Amber, "tap ${index + 1}") }
    val type = tool.toShape2DType() ?: return
    drawShape2D(type, pending, tx, Amber, filled = false)
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

internal fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSolidProjection(
    solid: Solid,
    offset: Vec3,
    rx: Float,
    ry: Float,
    rz: Float,
    center: Offset,
    scale: Float,
    color: Color,
    visualMode: SpatialVisualMode,
    selected: Boolean,
    perspective: Boolean,
    subSelection: SubObjectSelection?,
    sectionEnabled: Boolean,
    sectionPlane: EditableSectionPlane,
    clipSection: Boolean,
    appearance: WorkspaceAppearance = WorkspaceAppearance(),
) {
    fun p(v: Vec3): Offset {
        return project(rotate(solidLocalToWorld(solid.copy(position = offset), v), rx, ry, rz), center, scale, perspective)
    }
    val strokeWidth = when (visualMode) {
        SpatialVisualMode.Wireframe -> 2.2f
        SpatialVisualMode.XRay -> 1.5f
        SpatialVisualMode.Solid -> if (selected) 1.8f else 1.1f
    }
    val anchor = project(rotate(offset, rx, ry, rz), center, scale, perspective)
    if (selected && appearance.glow) {
        drawCircle(Brush.radialGradient(listOf(color.copy(.28f), Color.Transparent), anchor, 112f), radius = 112f, center = anchor)
        drawCircle(color.copy(.80f), radius = 64f, center = anchor, style = Stroke(2.4f))
    }
    val mesh = SolidMeshFactory.create(solid)
    val rotatedVertices = mesh.vertices.map { rotate(solidLocalToWorld(solid.copy(position = offset), it), rx, ry, rz) }
    val vertices = rotatedVertices.map { project(it, center, scale, perspective) }
    if (visualMode != SpatialVisualMode.Wireframe && !clipSection) {
        val lightDirection = Vec3(-.35, .65, -.68).normalized()
        mesh.faces.withIndex()
            .filter { it.value.size >= 3 }
            .sortedByDescending { indexed -> indexed.value.map { rotatedVertices[it].z }.average() }
            .forEach { (index, face) ->
                val path = Path().apply {
                    moveTo(vertices[face.first()].x, vertices[face.first()].y)
                    face.drop(1).forEach { lineTo(vertices[it].x, vertices[it].y) }
                    close()
                }
                val normal = AnalyticGeometry3D.cross(
                    rotatedVertices[face[1]] - rotatedVertices[face[0]],
                    rotatedVertices[face[2]] - rotatedVertices[face[0]],
                ).normalized()
                val light = (.28 + .72 * kotlin.math.abs(normal.dot(lightDirection))).coerceIn(.28, 1.0).toFloat()
                val litColor = Color(
                    red = (color.red * (.55f + light * .45f)).coerceIn(0f, 1f),
                    green = (color.green * (.55f + light * .45f)).coerceIn(0f, 1f),
                    blue = (color.blue * (.55f + light * .45f)).coerceIn(0f, 1f),
                    alpha = 1f,
                )
                val selectedFace = subSelection?.mode == Selection3DMode.Face && subSelection.index == index
                val materialAlpha = when (appearance.material) {
                    com.indianservers.aiexplorer.core.SpatialMaterial.Matte -> .72f
                    com.indianservers.aiexplorer.core.SpatialMaterial.Gloss -> .86f
                    com.indianservers.aiexplorer.core.SpatialMaterial.Metal -> .92f
                    com.indianservers.aiexplorer.core.SpatialMaterial.Glass -> .24f
                    com.indianservers.aiexplorer.core.SpatialMaterial.XRay -> .12f
                }
                val faceAlpha = when {
                    selectedFace -> .88f
                    visualMode == SpatialVisualMode.XRay -> .13f
                    selected -> materialAlpha
                    else -> materialAlpha * .72f
                }
                drawPath(
                    path,
                    Brush.linearGradient(
                        listOf(
                            (if (selectedFace) Amber else litColor).copy(alpha = faceAlpha),
                            Color.White.copy(
                                alpha = if (visualMode == SpatialVisualMode.XRay) .025f
                                else if (appearance.material == com.indianservers.aiexplorer.core.SpatialMaterial.Gloss) .22f
                                else .10f,
                            ),
                        ),
                    ),
                )
                if (appearance.texture == WorkspaceTexture.Faceted) {
                    drawPath(path, color.copy(alpha = .5f), style = Stroke(.8f))
                }
            }
    }
    mesh.edges.forEachIndexed { index, (a, b) ->
        var start = mesh.vertices[a]
        var end = mesh.vertices[b]
        val startSide = sectionPlane.unitNormal.dot(start) - sectionPlane.offset
        val endSide = sectionPlane.unitNormal.dot(end) - sectionPlane.offset
        if (!clipSection || startSide >= 0.0 || endSide >= 0.0) {
            if (clipSection && startSide < 0.0) {
                val t = startSide / (startSide - endSide)
                start += (end - start) * t
            }
            if (clipSection && endSide < 0.0) {
                val t = endSide / (endSide - startSide)
                end += (start - end) * t
            }
            val picked = subSelection?.mode == Selection3DMode.Edge && subSelection.index == index
            val edgeColor = when {
                picked -> Amber
                visualMode == SpatialVisualMode.Wireframe -> color.copy(alpha = if (selected) .80f else .30f)
                visualMode == SpatialVisualMode.XRay -> color.copy(alpha = .58f)
                selected -> color.copy(alpha = .80f)
                else -> color.copy(alpha = .30f)
            }
            drawLine(edgeColor, p(start), p(end), if (picked) 7f else strokeWidth)
        }
    }
    if (visualMode == SpatialVisualMode.Wireframe || subSelection?.mode == Selection3DMode.Vertex) {
        vertices.forEachIndexed { index, vertex ->
            if (!clipSection || sectionPlane.unitNormal.dot(mesh.vertices[index]) - sectionPlane.offset >= 0.0) {
                val picked = subSelection?.mode == Selection3DMode.Vertex && subSelection.index == index
                drawCircle(if (picked) Amber else color.copy(alpha = if (selected) .80f else .30f), if (picked) 10f else 4.5f, vertex)
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
        val (basisA, basisB) = sectionPlane.basis()
        val extent = maxOf(solid.width, solid.height, solid.depth, solid.radius * 2.0).coerceAtLeast(1.0) * .7
        val planeCorners = listOf(
            sectionPlane.origin - basisA * extent - basisB * extent,
            sectionPlane.origin + basisA * extent - basisB * extent,
            sectionPlane.origin + basisA * extent + basisB * extent,
            sectionPlane.origin - basisA * extent + basisB * extent,
        ).map(::p)
        val planePath = Path().apply { moveTo(planeCorners[0].x, planeCorners[0].y); planeCorners.drop(1).forEach { lineTo(it.x, it.y) }; close() }
        drawPath(planePath, Amber.copy(.10f)); drawPath(planePath, Amber.copy(.45f), style = Stroke(2f))
        val section = CrossSection3D.intersect(mesh, sectionPlane.unitNormal, sectionPlane.offset)
        if (section.size >= 2) {
            val sectionPath = Path().apply {
                val first = p(section.first())
                moveTo(first.x, first.y)
                section.drop(1).map(::p).forEach { lineTo(it.x, it.y) }
                if (section.size >= 3) close()
            }
            if (section.size >= 3) drawPath(sectionPath, Amber.copy(.28f))
            drawPath(sectionPath, Amber, style = Stroke(5f))
            drawGraphLabel("section n·p=${trim(sectionPlane.offset)}", p(section.first()) + Offset(12f, -18f), Amber)
        }
    }
    if (selected) {
        drawGraphLabel("${solid.type.name} selected", anchor + Offset(20f, -72f), color)
    }
}

private data class ProjectedSectionHandle(val start: Offset, val end: Offset)

internal fun subObjectAnchorWorld(solid: Solid, selection: SubObjectSelection?): Vec3 {
    val mesh = SolidMeshFactory.create(solid)
    val local = when (selection?.mode) {
        Selection3DMode.Vertex -> mesh.vertices.getOrNull(selection.index)
        Selection3DMode.Edge -> mesh.edges.getOrNull(selection.index)?.let { (a, b) -> (mesh.vertices[a] + mesh.vertices[b]) * .5 }
        Selection3DMode.Face -> mesh.faces.getOrNull(selection.index)?.mapNotNull(mesh.vertices::getOrNull)?.takeIf { it.isNotEmpty() }?.reduce(Vec3::plus)?.let { it * (1.0 / mesh.faces[selection.index].size) }
        else -> null
    }
    return local?.let { solidLocalToWorld(solid, it) } ?: solid.position
}

private fun projectedGizmoHandles(
    solid: Solid,
    selection: SubObjectSelection?,
    rx: Float,
    ry: Float,
    rz: Float,
    center: Offset,
    scale: Float,
    perspective: Boolean,
): List<TransformGizmoHandle> {
    val anchorWorld = subObjectAnchorWorld(solid, selection)
    fun screen(value: Vec3) = project(rotate(value, rx, ry, rz), center, scale, perspective)
    val anchor = screen(anchorWorld)
    val extent = 1.05
    val axes = listOf(
        TransformGizmoAxis.X to Vec3(extent, 0.0, 0.0),
        TransformGizmoAxis.Y to Vec3(0.0, extent, 0.0),
        TransformGizmoAxis.Z to Vec3(0.0, 0.0, extent),
    )
    return axes.map { (axis, vector) ->
        val end = screen(anchorWorld + vector)
        TransformGizmoHandle(axis, Vec2(anchor.x.toDouble(), anchor.y.toDouble()), Vec2(end.x.toDouble(), end.y.toDouble()))
    } + TransformGizmoHandle(TransformGizmoAxis.Uniform, Vec2(anchor.x.toDouble(), anchor.y.toDouble()), Vec2((anchor.x + 24f).toDouble(), (anchor.y + 24f).toDouble()))
}

private fun projectedSectionHandle(
    solid: Solid,
    plane: EditableSectionPlane,
    rx: Float,
    ry: Float,
    rz: Float,
    center: Offset,
    scale: Float,
    perspective: Boolean,
): ProjectedSectionHandle? {
    val startWorld = solidLocalToWorld(solid, plane.origin)
    val endWorld = solidLocalToWorld(solid, plane.origin + plane.unitNormal)
    val start = project(rotate(startWorld, rx, ry, rz), center, scale, perspective)
    val end = project(rotate(endWorld, rx, ry, rz), center, scale, perspective)
    return ProjectedSectionHandle(start, end).takeIf { (end - start).getDistance() > 2f }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTransformGizmo(handles: List<TransformGizmoHandle>, mode: Transform3DMode, active: TransformGizmoAxis?) {
    if (handles.isEmpty()) return
    fun p(value: Vec2) = Offset(value.x.toFloat(), value.y.toFloat())
    val anchor = p(handles.first().start)
    fun color(axis: TransformGizmoAxis) = when (axis) {
        TransformGizmoAxis.X -> WorkspaceVisualStyles.ReferenceBlue
        TransformGizmoAxis.Y -> WorkspaceVisualStyles.ReferenceMagenta
        TransformGizmoAxis.Z -> WorkspaceVisualStyles.ReferenceCyan
        TransformGizmoAxis.Uniform -> Color.White
    }
    when (mode) {
        Transform3DMode.Move -> {
            handles.filter { it.axis != TransformGizmoAxis.Uniform }.forEach { handle ->
                val accent = color(handle.axis); drawLine(accent, p(handle.start), p(handle.end), if (active == handle.axis) 9f else 5f, cap = StrokeCap.Round); drawCircle(accent, 8f, p(handle.end)); drawGraphLabel(handle.axis.label, p(handle.end) + Offset(6f, -6f), accent)
            }
            drawCircle(Color.White, 7f, anchor)
        }
        Transform3DMode.Rotate -> {
            handles.filter { it.axis != TransformGizmoAxis.Uniform }.forEach { handle ->
                val radius = (p(handle.end) - anchor).getDistance(); drawCircle(color(handle.axis).copy(.85f), radius, anchor, style = Stroke(if (active == handle.axis) 8f else 4f))
            }
        }
        Transform3DMode.Scale -> {
            handles.forEach { handle ->
                val end = p(handle.end); val accent = color(handle.axis); drawLine(accent, anchor, end, if (active == handle.axis) 8f else 4f); drawRect(accent, topLeft = end - Offset(7f, 7f), size = Size(14f, 14f)); if (handle.axis != TransformGizmoAxis.Uniform) drawGraphLabel(handle.axis.label, end + Offset(6f, -6f), accent)
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

internal fun screenDragToWorld(delta: Vec2, pixelsPerUnit: Double, rx: Float, ry: Float, rz: Float): Vec3 {
    if (pixelsPerUnit <= 1e-9) return Vec3(0.0, 0.0, 0.0)
    val ax = Math.toRadians(rx.toDouble())
    val ay = Math.toRadians(ry.toDouble())
    val az = Math.toRadians(rz.toDouble())
    val cameraX = delta.x / pixelsPerUnit
    val cameraY = -delta.y / pixelsPerUnit
    val xAfterZ = cameraX * cos(az) + cameraY * sin(az)
    val yAfterZ = -cameraX * sin(az) + cameraY * cos(az)
    val xAfterY = xAfterZ * cos(ay)
    val zAfterY = xAfterZ * sin(ay)
    return Vec3(
        xAfterY,
        yAfterZ * cos(ax) + zAfterY * sin(ax),
        -yAfterZ * sin(ax) + zAfterY * cos(ax),
    )
}

internal fun solidLocalToWorld(solid: Solid, vertex: Vec3): Vec3 = rotate(
    vertex,
    solid.rotation.x.toFloat(),
    solid.rotation.y.toFloat(),
    solid.rotation.z.toFloat(),
) + solid.position

internal fun Float.wrapDegrees(): Float {
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

internal fun snapAngle(value: Float): Float = (kotlin.math.round(value / 1f) * 1f).coerceIn(-180f, 180f)

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

internal fun radianLabel(angle: Double): String = when (kotlin.math.round(angle).toInt()) {
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
